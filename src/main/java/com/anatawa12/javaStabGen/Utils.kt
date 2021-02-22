package com.anatawa12.javaStabGen

import org.objectweb.asm.TypeReference
import org.objectweb.asm.tree.TypeAnnotationNode

@Suppress("UNCHECKED_CAST")
fun <T : Any> List<T?>.nullIfSomeAreNull(): List<T>? = if (any { it == null }) null else this as List<T>

fun List<TypeAnnotationNode>.filterWith(typeReference: TypeReference) = filterWith(typeReference.value)
fun List<TypeAnnotationNode>.filterWith(typeRef: Int) = filter { it.typeRef == typeRef }
