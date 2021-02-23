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
        return TypeNameSignatureVisitor(classNode) { typeName ->
            parameters += typeName?.asTypeName(typeAnnotations
                .filterWith(newFormalParameterReference(parameters.size), classNode))
        }
    }

    override fun visitReturnType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode) { typeName ->
            returns = typeName?.asTypeName(typeAnnotations
                .filterWith(newTypeReference(METHOD_RETURN), classNode))
        }
    }

    override fun visitExceptionType(): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { typeName ->
            throws += typeName?.asTypeName(typeAnnotations
                .filterWith(newExceptionReference(throws.size), classNode))
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
        return TypeNameSignatureVisitor(classNode) { typeName ->
            superClass = typeName?.asTypeName(typeAnnotations
                .filterWith(newSuperTypeReference(-1), classNode))
        }
    }
    override fun visitParameterType(): SignatureVisitor {
        finishVisitParameter()
        return TypeNameSignatureVisitor(classNode) { typeName ->
            superInterfaces += typeName?.asTypeName(typeAnnotations
                .filterWith(newSuperTypeReference(superInterfaces.size), classNode))
        }
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
    protected var typeParameters = mutableListOf<TypeVariableName?>()

    override fun visitFormalTypeParameter(name: String) {
        typeVariableName = name
    }

    override fun visitClassBound(): SignatureVisitor {
        val annotations = typeAnnotations.filterWith(
            newTypeParameterBoundReference(CLASS_TYPE_PARAMETER_BOUND + typeParamSort,
                typeParameters.size, boundCount++), classNode)
        return TypeNameSignatureVisitor(classNode) { typeName ->
            bounds.add(typeName?.asTypeName(annotations))
        }
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        if (boundCount == 0) boundCount++
        val annotations = typeAnnotations.filterWith(
            newTypeParameterBoundReference(CLASS_TYPE_PARAMETER_BOUND + typeParamSort,
                typeParameters.size, boundCount++), classNode)
        return TypeNameSignatureVisitor(classNode) { typeName ->
            bounds.add(typeName?.asTypeName(annotations))
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

private class TypeNameSignatureVisitor(
    val classNode: ClassNode,
    val process: (SignatureType?) -> Unit,
) : SignatureVisitor(Opcodes.ASM9) {
    var dimensions = 0

    private fun SignatureType.computeArray(): SignatureType {
        var type = this
        repeat(dimensions) {
            type = SignatureArrayType(type)
        }
        return type
    }

    override fun visitArrayType(): SignatureVisitor {
        dimensions++
        return this
    }

    override fun visitBaseType(descriptor: Char) {
        process(PrimitiveSignatureType.fromDescriptor(descriptor).computeArray())
    }

    override fun visitTypeVariable(name: String) {
        process(name.asJavaIdentifierOrNull()?.let(::TypeParam)?.computeArray())
    }

    // classType
    private var type: ObjectSignatureType? = null

    override fun visitClassType(name: String) {
        type = SimpleSignatureType(name)
    }

    override fun visitInnerClassType(name: String) {
        type = type?.let { InnerSignatureType(it, name) }
    }

    override fun visitTypeArgument() {
        type?.params?.add(FullWildcard)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        return TypeNameSignatureVisitor(classNode) { name ->
            if (name == null) {
                type = null
                return@TypeNameSignatureVisitor
            }
            val type = type ?: return@TypeNameSignatureVisitor
            name as ObjectSignatureType
            when (wildcard) {
                '=' -> type.params.add(Wildcard(name, kind = BoundKind.Exactry))
                // +: ? extends TYPE
                '+' -> type.params.add(Wildcard(name, kind = BoundKind.SubType))
                // -: ? super TYPE
                '-' -> type.params.add(Wildcard(name, kind = BoundKind.SuperType))
            }
        }
    }

    override fun visitEnd() {
        process(type?.computeArray())
    }
}

private enum class BoundKind {
    Exactry,
    SubType,
    SuperType,
}
private sealed class SignatureTypeParam {
    abstract fun asTypeName(annotations: TypeAnnotations): TypeName?
}

private object FullWildcard : SignatureTypeParam() {
    private val typeName = WildcardTypeName.subtypeOf(TypeName.OBJECT)
    override fun asTypeName(annotations: TypeAnnotations) = typeName.let(annotations::annotate)
}

private class Wildcard(val base: ReferenceSignatureType, val kind: BoundKind) : SignatureTypeParam() {
    override fun asTypeName(annotations: TypeAnnotations): TypeName? = when (kind) {
        BoundKind.Exactry -> base.asTypeName(annotations)
        BoundKind.SubType -> base.asTypeName(annotations.wildcardBound())
            ?.let(WildcardTypeName::subtypeOf)
            ?.let(annotations::annotate)
        BoundKind.SuperType -> base.asTypeName(annotations.wildcardBound())
            ?.let(WildcardTypeName::supertypeOf)
            ?.let(annotations::annotate)
    }
}

private sealed class SignatureType {
    abstract fun asTypeName(annotations: TypeAnnotations): TypeName?
}

private sealed class PrimitiveSignatureType(val typeName: TypeName) : SignatureType() {
    override fun asTypeName(annotations: TypeAnnotations): TypeName? = typeName.let(annotations::annotate)

    companion object {
        fun fromDescriptor(descriptor: Char): PrimitiveSignatureType = when (descriptor) {
            'V' -> PrimitiveSignatureTypeVoid
            'Z' -> PrimitiveSignatureTypeBoolean
            'B' -> PrimitiveSignatureTypeByte
            'S' -> PrimitiveSignatureTypeShort
            'I' -> PrimitiveSignatureTypeInt
            'J' -> PrimitiveSignatureTypeLong
            'C' -> PrimitiveSignatureTypeChar
            'F' -> PrimitiveSignatureTypeFloat
            'D' -> PrimitiveSignatureTypeDouble
            else -> error("invalid or unsupported first char of descriptor")
        }
    }
}
// 9
private object PrimitiveSignatureTypeVoid : PrimitiveSignatureType(TypeName.VOID)
private object PrimitiveSignatureTypeBoolean : PrimitiveSignatureType(TypeName.BOOLEAN)
private object PrimitiveSignatureTypeByte : PrimitiveSignatureType(TypeName.BYTE)
private object PrimitiveSignatureTypeShort : PrimitiveSignatureType(TypeName.SHORT)
private object PrimitiveSignatureTypeInt : PrimitiveSignatureType(TypeName.INT)
private object PrimitiveSignatureTypeLong : PrimitiveSignatureType(TypeName.LONG)
private object PrimitiveSignatureTypeChar : PrimitiveSignatureType(TypeName.CHAR)
private object PrimitiveSignatureTypeFloat : PrimitiveSignatureType(TypeName.FLOAT)
private object PrimitiveSignatureTypeDouble : PrimitiveSignatureType(TypeName.DOUBLE)
private sealed class ReferenceSignatureType : SignatureType()
private data class SignatureArrayType(val type: SignatureType) : ReferenceSignatureType() {
    override fun asTypeName(annotations: TypeAnnotations): TypeName?
    = type.asTypeName(annotations.inArray())
        ?.let(ArrayTypeName::of)
        ?.let(annotations::annotate)

}
private data class TypeParam(val name: String) : ReferenceSignatureType() {
    override fun asTypeName(annotations: TypeAnnotations): TypeName =
        TypeVariableName.get(name).let(annotations::annotate)
}

private sealed class ObjectSignatureType : ReferenceSignatureType() {
    abstract val params: MutableList<SignatureTypeParam>
    abstract fun asTypeNameInternal(annotations: TypeAnnotations): Pair<TypeName, TypeAnnotations>?
    override final fun asTypeName(annotations: TypeAnnotations): TypeName? = asTypeNameInternal(annotations)?.first
}
private data class SimpleSignatureType(
    val internalName: String,
    override val params: MutableList<SignatureTypeParam> = mutableListOf(),
) : ObjectSignatureType() {
    override fun asTypeNameInternal(annotations: TypeAnnotations): Pair<TypeName, TypeAnnotations>? {
        val (type, ann) = classNameFromInternalName(internalName, annotations.classNode, annotations) ?: return null
        return type.parameterized(params, ann.outer())?.let { it to ann.nested() }
    }
}

private data class InnerSignatureType(
    val outer: ObjectSignatureType,
    val name: String,
    override val params: MutableList<SignatureTypeParam> = mutableListOf(),
) : ObjectSignatureType() {
    override fun asTypeNameInternal(annotations: TypeAnnotations): Pair<TypeName, TypeAnnotations>? {
        val (type, ann) = outer.asTypeNameInternal(annotations) ?: return null
        return when (type) {
            is ClassName -> type.nestedClass(name).parameterized(params, ann.outer())?.let(ann::annotate)
            is ParameterizedTypeName -> type
                .nestedClass(name, params.mapIndexed { i, param -> param.asTypeName(ann.outer().typeParam(i)) })
                .let(ann::annotate)
            else -> error("unsupported ObjectSignatureType of inner class")
        }?.let { it to ann.nested() }
    }
}

private fun ClassName.parameterized(params: MutableList<SignatureTypeParam>, annotations: TypeAnnotations): TypeName? {
    if (params.isNotEmpty())
        return ParameterizedTypeName.get(this,
            *params.mapIndexedArrayOrNull { i, param -> param.asTypeName(annotations.typeParam(i)) }
                ?: return null)
    return this
}
