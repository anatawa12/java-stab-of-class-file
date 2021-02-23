package com.anatawa12.javaStabGen

 import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import javax.lang.model.SourceVersion

fun Type.asTypeName(typeAnnotations: TypeAnnotations) =
    typeNameFromDescriptor(descriptor, typeAnnotations)
fun Type.asTypeName(classNode: ClassNode) =
    typeNameFromDescriptor(descriptor, classNode)

fun typeNameFromDescriptor(descriptor: String, typeAnnotations: TypeAnnotations): TypeName? =
    typeNameFromDescriptorInternal(descriptor, typeAnnotations.classNode, 0, typeAnnotations)

fun typeNameFromDescriptor(descriptor: String, classNode: ClassNode): TypeName? =
    typeNameFromDescriptorInternal(descriptor, classNode, 0, TypeAnnotations.EMPTY)

fun primitiveTypeFromDescriptor(descriptor: Char): TypeName = when (descriptor) {
    'V' -> TypeName.VOID
    'Z' -> TypeName.BOOLEAN
    'B' -> TypeName.BYTE
    'S' -> TypeName.SHORT
    'I' -> TypeName.INT
    'J' -> TypeName.LONG
    'C' -> TypeName.CHAR
    'F' -> TypeName.FLOAT
    'D' -> TypeName.DOUBLE
    else -> error("invalid or unsupported first char of descriptor")
}

private tailrec fun typeNameFromDescriptorInternal(
    descriptor: String,
    classNode: ClassNode,
    dimension: Int,
    typeAnnotations: TypeAnnotations,
): TypeName? = when (descriptor[0]) {
    'V' -> TypeName.VOID.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'Z' -> TypeName.BOOLEAN.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'B' -> TypeName.BYTE.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'S' -> TypeName.SHORT.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'I' -> TypeName.INT.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'J' -> TypeName.LONG.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'C' -> TypeName.CHAR.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'F' -> TypeName.FLOAT.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    'D' -> TypeName.DOUBLE.processAnnotations(typeAnnotations).processDimensions(dimension, typeAnnotations)
    '[' -> typeNameFromDescriptorInternal(descriptor.substring(1), classNode, dimension + 1,
        typeAnnotations = typeAnnotations.inArray())
    'L' -> classNameFromInternalName(descriptor.substring(1, descriptor.length - 1), classNode, typeAnnotations)
        ?.processDimensions(dimension, typeAnnotations)
    else -> error("invalid or unsupported first char of descriptor")
}

private fun TypeName.processAnnotations(typeAnnotations: TypeAnnotations): TypeName = typeAnnotations.annotate(this)

private fun TypeName.processDimensions(dimension: Int, typeAnnotations: TypeAnnotations): TypeName {
    var self = this
    @Suppress("NAME_SHADOWING")
    var typeAnnotations = typeAnnotations
    repeat(dimension) {
        self = ArrayTypeName.of(self)
        typeAnnotations = typeAnnotations.outArray()
        self = typeAnnotations.annotate(self)
    }
    check(typeAnnotations.isRoot())
    return self
}
fun classNameFromInternalName(internalName: String, classNode: ClassNode): ClassName? {
    return classNameFromInternalName(internalName, classNode, TypeAnnotations.EMPTY)
}

fun classNameFromInternalName(internalName: String, typeAnnotations: TypeAnnotations): ClassName? {
    return classNameFromInternalName(internalName, typeAnnotations.classNode, typeAnnotations)
}

private fun classNameFromInternalName(
    internalName: String,
    classNode: ClassNode,
    typeAnnotations: TypeAnnotations,
): ClassName? {
    val innerClass = classNode.innerClasses.find { it.name == internalName }
    if (innerClass == null) {
        val className = internalName.substringAfterLast('/')
        val packageName = internalName.substringBeforeLast('/', "").replace('/', '.')
        if (!className.isJavaIdentifierName()) return null
        if (packageName.isNotEmpty() && !packageName.isJavaQualifiedName()) return null
        return ClassName.get(packageName, className).let(typeAnnotations::annotate)
    } else {
        if (innerClass.outerName == null) return null
        if (innerClass.innerName == null) return null
        val type = classNameFromInternalName(innerClass.outerName, classNode, typeAnnotations.nested()) ?: return null
        return type.nestedClass(innerClass.innerName).let(typeAnnotations::annotate)
    }
}

fun CharSequence.isJavaIdentifierName(): Boolean {
    if (SourceVersion.isKeyword(this)) return false
    if (isEmpty()) return false
    if (!this[0].isJavaIdentifierStart()) return false
    for (i in 1 until length) {
        if (!this[i].isJavaIdentifierPart()) return false
    }
    return true
}

fun String.isJavaQualifiedName(): Boolean = this.split('.').all { it.isJavaIdentifierName() }

fun String.asJavaIdentifierOrNull() = if (isJavaIdentifierName()) this else null

fun <S : SignatureVisitor> S.visit(signature: String) = also {
    SignatureReader(signature).accept(this)
}
