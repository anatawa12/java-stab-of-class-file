package com.anatawa12.javaStabGen

import com.squareup.javapoet.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode

class MethodSignatureVisitor(classNode: ClassNode) : TypeParametersSignatureVisitor(classNode) {
    val parameters = mutableListOf<TypeName?>()
    var returns: TypeName? = null
        private set
    val throws = mutableListOf<TypeName?>()

    override fun visitParameterType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode) { typeName ->
            parameters += typeName
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode) { typeName ->
            returns = typeName
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { typeName ->
            throws += typeName
        }
    }
}

abstract class TypeParametersSignatureVisitor(val classNode: ClassNode) : SignatureVisitor(Opcodes.ASM9) {
    private var typeVariableName: String? = null
    private var bounds = mutableListOf<TypeName?>()
    protected var typeParameters = mutableListOf<TypeVariableName?>()

    override fun visitFormalTypeParameter(name: String) {
        typeVariableName = name
    }

    override fun visitClassBound(): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { typeName ->
            bounds.add(typeName)
        }
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { typeName ->
            bounds.add(typeName)
        }
    }

    protected fun finishVisitParameter() {
        val typeVariableName = typeVariableName ?: return
        if (bounds.any { it == null }) {
            typeParameters.add(null)
        } else {
            typeParameters.add(TypeVariableName.get(typeVariableName, *bounds.toTypedArray()))
        }
        this.typeVariableName = null
        bounds = mutableListOf()
    }
}

class TypeNameSignatureVisitor(val classNode: ClassNode, val process: (TypeName?) -> Unit) : SignatureVisitor(Opcodes.ASM9) {
    var dimensions = 0

    private fun TypeName.computeArray(): TypeName {
        var type = this
        repeat(dimensions) {
            type = ArrayTypeName.of(type)
        }
        return type
    }

    override fun visitArrayType(): SignatureVisitor {
        dimensions++
        return this
    }

    override fun visitBaseType(descriptor: Char) {
        process(primitiveTypeFromDescriptor(descriptor).computeArray())
    }

    override fun visitTypeVariable(name: String) {
        process(name.asJavaIdentifierOrNull()?.let(TypeVariableName::get)?.computeArray())
    }

    sealed class TypeNameBuilder {
        abstract fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder?
        abstract fun nested(name: String): TypeNameBuilder?
        abstract fun toTypeName(): TypeName

        class Class(val className: ClassName): TypeNameBuilder() {
            override fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder? {
                if (typeVariables == null) return this
                if (typeVariables.any { it == null }) return null
                return Parameterized(ParameterizedTypeName.get(className, *typeVariables.toTypedArray()))
            }

            override fun nested(name: String): TypeNameBuilder? =
                name.asJavaIdentifierOrNull()
                    ?.let { className.nestedClass(it) }
                    ?.let(::Class)

            override fun toTypeName(): ClassName = className
        }

        class Parameterized(val className: ParameterizedTypeName, val name: String? = null): TypeNameBuilder() {
            override fun computeTypeVariables(typeVariables: MutableList<TypeName?>?): TypeNameBuilder? {
                if (typeVariables == null) return this
                if (typeVariables.any { it == null }) return null
                val name = checkNotNull(name) { "must be nested parameterized type" }
                return Parameterized(className.nestedClass(name, typeVariables))
            }

            override fun nested(name: String): TypeNameBuilder? {
                val className = toTypeName()
                return name.asJavaIdentifierOrNull()
                    ?.let { Parameterized(className, it) }
            }

            override fun toTypeName(): ParameterizedTypeName =
                this.name?.let { thisName -> className.nestedClass(thisName) } ?: className
        }
    }

    // classType
    var baseType: TypeNameBuilder? = null
    var typeVariables: MutableList<TypeName?>? = null

    override fun visitClassType(name: String) {
        baseType = classNameFromInternalName(name, classNode)?.let(TypeNameBuilder::Class)
    }

    override fun visitInnerClassType(name: String) {
        baseType = baseType?.computeTypeVariables(typeVariables)?.nested(name)
    }

    private fun typeVariables(): MutableList<TypeName?> = typeVariables ?: mutableListOf<TypeName?>().also { typeVariables = it }

    override fun visitTypeArgument() {
        typeVariables().add(WildcardTypeName.subtypeOf(TypeName.OBJECT))
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { name ->
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
        process(baseType?.computeTypeVariables(typeVariables)?.toTypeName()?.computeArray())
    }
}
