package com.anatawa12.javaStabGen

import com.squareup.javapoet.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypeReference.*
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.TypeAnnotationNode

class MethodSignatureVisitor(classNode: ClassNode, typeAnnotations: List<TypeAnnotationNode>)
    : TypeParametersSignatureVisitor(classNode, typeAnnotations, METHOD_TYPE_PARAMETER) {
    val parameters = mutableListOf<TypeName?>()
    var returns: TypeName? = null
        private set
    val throws = mutableListOf<TypeName?>()

    override fun visitParameterType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode,
            typeAnnotations.filterWith(newFormalParameterReference(parameters.size), classNode)) { typeName ->
            parameters += typeName
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode,
            typeAnnotations.filterWith(newTypeReference(METHOD_RETURN), classNode)) { typeName ->
            returns = typeName
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode,
            typeAnnotations.filterWith(newExceptionReference(throws.size), classNode)) { typeName ->
            throws += typeName
        }
    }
}

class ClassSignatureVisitor(classNode: ClassNode, typeAnnotations: List<TypeAnnotationNode>)
    : TypeParametersSignatureVisitor(classNode, typeAnnotations, CLASS_TYPE_PARAMETER) {
    var superClass: TypeName? = null
        private set
    val superInterfaces = mutableListOf<TypeName?>()

    override fun visitSuperclass(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode,
            typeAnnotations.filterWith(newSuperTypeReference(-1), classNode)) { typeName ->
            superClass = typeName
        }
    }
    override fun visitParameterType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode,
            typeAnnotations.filterWith(newSuperTypeReference(superInterfaces.size), classNode)) { typeName ->
            superInterfaces += typeName
        }
    }
}

internal class FieldSignatureVisitor(classNode: ClassNode, typeAnnotations: List<TypeAnnotationNode>)
    : TypeNameSignatureVisitor(classNode, typeAnnotations.filterWith(newTypeReference(FIELD), classNode), {}) {
    var type: TypeName? = null
        private set
    init {
        process = { type = it }
    }
}

abstract class TypeParametersSignatureVisitor(
    val classNode: ClassNode,
    val typeAnnotations: List<TypeAnnotationNode>,
    private val typeParamSort: Int
) : SignatureVisitor(Opcodes.ASM9) {
    private var typeVariableName: String? = null
    private var bounds = mutableListOf<TypeName?>()
    private var boundCount = 0
    var typeParameters = mutableListOf<TypeVariableName?>()

    override fun visitFormalTypeParameter(name: String) {
        typeVariableName = name
    }

    override fun visitClassBound(): SignatureVisitor {
        val annotations = typeAnnotations.filterWith(
            newTypeParameterBoundReference(CLASS_TYPE_PARAMETER_BOUND + typeParamSort,
                typeParameters.size, boundCount++), classNode)
        return TypeNameSignatureVisitor(classNode, annotations) { typeName ->
            bounds.add(typeName)
        }
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        if (boundCount == 0) boundCount++
        val annotations = typeAnnotations.filterWith(
            newTypeParameterBoundReference(CLASS_TYPE_PARAMETER_BOUND + typeParamSort,
                typeParameters.size, boundCount++), classNode)
        return TypeNameSignatureVisitor(classNode, annotations) { typeName ->
            bounds.add(typeName)
        }
    }

    protected fun finishVisitParameter() {
        val typeVariableName = typeVariableName ?: return
        if (bounds.any { it == null }) {
            typeParameters.add(null)
        } else {
            typeParameters.add(TypeVariableName.get(typeVariableName, *bounds.toTypedArray())
                ?.let { typeAnnotations.filterWith(newTypeParameterReference(typeParamSort, typeParameters.size),
                    classNode).annotate(it) }
            )
        }
        this.typeVariableName = null
        bounds = mutableListOf()
        boundCount = 0
    }
}

internal open class TypeNameSignatureVisitor(
    val classNode: ClassNode,
    var typeAnnotations: TypeAnnotations,
    var process: (TypeName?) -> Unit,
) : SignatureVisitor(Opcodes.ASM9) {
    var dimensions = 0

    private fun TypeName.computeArray(): TypeName {
        var type = this
        repeat(dimensions) {
            type = ArrayTypeName.of(type)
            typeAnnotations = typeAnnotations.outArray()
            type = typeAnnotations.annotate(type)
        }
        return type
    }

    override fun visitArrayType(): SignatureVisitor {
        dimensions++
        typeAnnotations = typeAnnotations.inArray()
        return this
    }

    override fun visitBaseType(descriptor: Char) {
        process(primitiveTypeFromDescriptor(descriptor)
            .let(typeAnnotations::annotate)
            .computeArray())
    }

    override fun visitTypeVariable(name: String) {
        process(name.asJavaIdentifierOrNull()
            ?.let(TypeVariableName::get)
            ?.let(typeAnnotations::annotate)
            ?.computeArray())
    }

    // classType
    var baseType: TypeNameBuilder? = null
    var typeVariables: MutableList<TypeName?>? = null
    var rootAnnotations: TypeAnnotations? = null

    override fun visitClassType(name: String) {
        val (className, annotations) = classNameFromInternalName(name, classNode, typeAnnotations) ?: kotlin.run {
            baseType = null
            return
        }
        rootAnnotations = typeAnnotations
        baseType = TypeNameBuilder.Class(className)
        typeAnnotations = annotations
    }

    override fun visitInnerClassType(name: String) {
        baseType = baseType?.computeTypeVariables(typeVariables)?.nested(name)
            ?.also { it.typeAnnotations = typeAnnotations }
        typeAnnotations = typeAnnotations.nested()
        typeVariables = null
    }

    private fun typeVariables(): MutableList<TypeName?> = typeVariables ?: mutableListOf<TypeName?>().also { typeVariables = it }

    override fun visitTypeArgument() {
        typeVariables().add(WildcardTypeName.subtypeOf(TypeName.OBJECT))
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        var annotations = typeAnnotations.outer().typeParam(typeVariables().size)
        if (wildcard != '=') annotations = annotations.wildcardBound()
        return TypeNameSignatureVisitor(classNode, annotations) { name ->
            when (wildcard) {
                '=' -> typeVariables().add(name)
                // +: ? extends TYPE
                '+' -> typeVariables().add(WildcardTypeName.subtypeOf(name))
                // -: ? super TYPE
                '-' -> typeVariables().add(WildcardTypeName.supertypeOf(name))
            }
        }
    }

    override fun visitEnd() {
        typeAnnotations = rootAnnotations!!
        process(baseType?.computeTypeVariables(typeVariables)?.toTypeName()?.computeArray())
    }
}

sealed class TypeNameBuilder {
    var typeAnnotations: TypeAnnotations? = null

    abstract fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder?
    abstract fun nested(name: String): TypeNameBuilder?
    abstract fun toTypeName(): TypeName

    class Class(val className: ClassName): TypeNameBuilder() {
        override fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder? {
            if (typeVariables == null) return this
            if (typeVariables.any { it == null }) return null
            return Parameterized(ParameterizedTypeName.get(toTypeName(), *typeVariables.toTypedArray()))
        }

        override fun nested(name: String): TypeNameBuilder? =
            name.asJavaIdentifierOrNull()
                ?.let { toTypeName().nestedClass(it) }
                ?.let(::Class)

        override fun toTypeName(): ClassName = typeAnnotations?.annotate(className) ?: className
    }

    class Parameterized(val className: ParameterizedTypeName, val name: String? = null): TypeNameBuilder() {
        override fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder? {
            if (typeVariables == null) return this
            if (typeVariables.any { it == null }) return null
            val name = checkNotNull(name) { "must be nested parameterized type" }
            return Parameterized(className.nestedClass(name, typeVariables).let { typeAnnotations?.annotate(it) ?: it })
        }

        override fun nested(name: String): TypeNameBuilder? {
            val className = toTypeName()
            return name.asJavaIdentifierOrNull()
                ?.let { Parameterized(className, it) }
        }

        override fun toTypeName(): ParameterizedTypeName =
            this.name?.let { thisName -> className.nestedClass(thisName) }
                ?.let { typeAnnotations?.annotate(it) ?: it }
                ?: className
    }
}
