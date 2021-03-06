package com.anatawa12.javaStabGen

import com.squareup.javapoet.TypeName
import org.objectweb.asm.TypeReference
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.TypeAnnotationNode

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> List<T?>.nullIfSomeAreNull(): List<T>? = if (any { it == null }) null else this as List<T>

internal fun List<TypeAnnotationNode>.filterWith(typeReference: TypeReference, classNode: ClassNode) =
    filterWith(typeReference.value, classNode)

internal fun List<TypeAnnotationNode>.filterWith(typeRef: Int, classNode: ClassNode) =
    TypeAnnotations.root(filter { it.typeRef == typeRef }, classNode)

internal fun <E> List<E>.mapIndexedArrayOrNull(function: (index: Int, E) -> TypeName?): Array<TypeName>? {
    return Array(size) { i -> function(i, this[i]) ?: return null }
}
