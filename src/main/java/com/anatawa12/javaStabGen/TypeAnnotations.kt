package com.anatawa12.javaStabGen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.objectweb.asm.TypePath
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.TypeAnnotationNode

class TypeAnnotations private constructor(
    val nodes: List<TypeAnnotationNode>,
    val path: TypePath?,
    val classNode: ClassNode,
    val outerCache: TypeAnnotations?,
) {
    constructor(
        nodes: List<TypeAnnotationNode>,
        path: TypePath?,
        classNode: ClassNode,
    ) : this(nodes, path, classNode, null)

    private fun nestedPath(join: String): TypePath =
        TypePath.fromString(path?.toString().orEmpty() + join)

    fun inArray(): TypeAnnotations = copy(nestedPath("["), this)

    fun outArray(): TypeAnnotations {
        check(path != null) { "type path is root" }
        check(path.getStep(path.length - 1) == TypePath.ARRAY_ELEMENT) { "type path is not of array element" }
        if (outerCache != null) return outerCache
        return copy(TypePath.fromString(path.toString().dropLast(1)))
    }

    fun nested(): TypeAnnotations = copy(nestedPath("."), this)

    fun outer(): TypeAnnotations {
        check(path != null) { "type path is root" }
        check(path.getStep(path.length - 1) == TypePath.INNER_TYPE) { "type path is not of inner type" }
        if (outerCache != null) return outerCache
        return copy(TypePath.fromString(path.toString().dropLast(1)))
    }

    fun typeParam(i: Int): TypeAnnotations = copy(nestedPath("$i;"), this)

    fun wildcardBound(): TypeAnnotations = copy(nestedPath("*"), this)

    fun isRoot(): Boolean = path == null

    fun annotate(type: TypeName): TypeName {
        if (current.isEmpty()) return type
        return type.annotated(current.toSpecs(classNode))
    }

    fun annotate(type: ClassName): ClassName {
        if (current.isEmpty()) return type
        return type.annotated(current.toSpecs(classNode))
    }

    fun annotate(type: TypeVariableName): TypeVariableName {
        if (current.isEmpty()) return type
        return type.annotated(current.toSpecs(classNode))
    }

    fun annotate(type: ParameterizedTypeName): ParameterizedTypeName {
        if (current.isEmpty()) return type
        return type.annotated(current.toSpecs(classNode))
    }

    private fun copy(path: TypePath?, outerCache: TypeAnnotations? = null) =
        TypeAnnotations(nodes, path, classNode, outerCache)

    val current: List<TypeAnnotationNode> by lazy { nodes.filter { it.typePath?.toString() == path?.toString() } }

    companion object {
        val EMPTY = TypeAnnotations(emptyList(), null, ClassNode())
        fun root(nodes: List<TypeAnnotationNode>, classNode: ClassNode) = TypeAnnotations(nodes, null, classNode)
    }
}
