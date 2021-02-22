package com.anatawa12.javaStabGen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import javax.lang.model.SourceVersion

fun Type.asTypeName(classNode: ClassNode) = typeNameFromDescriptor(descriptor, classNode)

fun typeNameFromDescriptor(descriptor: String, classNode: ClassNode): TypeName? =
    typeNameFromDescriptorInternal(descriptor, classNode, 0)

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
): TypeName? = when (descriptor[0]) {
    'V' -> TypeName.VOID
    'Z' -> TypeName.BOOLEAN
    'B' -> TypeName.BYTE
    'S' -> TypeName.SHORT
    'I' -> TypeName.INT
    'J' -> TypeName.LONG
    'C' -> TypeName.CHAR
    'F' -> TypeName.FLOAT
    'D' -> TypeName.DOUBLE
    '[' -> typeNameFromDescriptorInternal(descriptor.substring(1), classNode, dimension + 1)
    'L' -> classNameFromInternalName(descriptor.substring(0, descriptor.length - 1), classNode)
    else -> error("invalid or unsupported first char of descriptor")
}

fun classNameFromInternalName(internalName: String, classNode: ClassNode): ClassName? {
    val innerClass = classNode.innerClasses.find { it.name == internalName }
    if (innerClass == null) {
        val className = internalName.substringAfterLast('/')
        val packageName = internalName.substringBeforeLast('/', "").replace('/', '.')
        if (!className.isJavaIdentifierName()) return null
        if (packageName.isNotEmpty() && !packageName.isJavaQualifiedName()) return null
        return ClassName.get(packageName, className)
    } else {
        if (innerClass.outerName == null) return null
        if (innerClass.innerName == null) return null
        val type = classNameFromInternalName(innerClass.outerName, classNode) ?: return null
        return type.nestedClass(innerClass.innerName)
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
