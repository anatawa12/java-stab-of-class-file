package com.anatawa12.javaStabGen

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

fun List<AnnotationNode>.toSpecs(classNode: ClassNode): List<AnnotationSpec> = mapNotNull { it.toSpec(classNode) }

fun AnnotationNode.toSpec(classNode: ClassNode): AnnotationSpec? {
    var spec: AnnotationSpec? = null
    accept(AnnotationSpecGenerator(desc, classNode) { spec = it })
    return spec
}

private class AnnotationSpecGenerator(descriptor: String, val classNode: ClassNode, val receive: (AnnotationSpec?) -> Unit) : AnnotationVisitor(Opcodes.ASM9) {
    lateinit var builder: AnnotationSpec.Builder

    var isInvalid: Boolean = false

    private fun invalidate() {
        isInvalid = true
    }

    init {
        val typeName = typeNameFromDescriptor(descriptor, classNode) as? ClassName
        if (typeName == null) invalidate()
        else builder = AnnotationSpec.builder(typeName)
    }

    override fun visit(name: String, value: Any) {
        if (isInvalid) return
        builder.addMember(name, value.asCode(classNode) ?: return invalidate())
    }

    override fun visitEnum(name: String, descriptor: String, value: String) {
        if (isInvalid) return
        val typeName = typeNameFromDescriptor(descriptor, classNode)
        if (typeName == null) invalidate()
        builder.addMember(name, CodeBlock.of("\$T.\$N", typeName, value))
    }

    override fun visitAnnotation(name: String, descriptor: String): AnnotationVisitor? {
        if (isInvalid) return null
        return AnnotationSpecGenerator(descriptor, classNode) { annotationSpec ->
            builder.addMember(name, "\$L", annotationSpec ?: return@AnnotationSpecGenerator invalidate())
        }
    }

    override fun visitArray(name: String): AnnotationVisitor? {
        if (isInvalid) return null
        return AnnotationArrayGenerator(classNode) { codeBlock ->
            builder.addMember(name, "\$L", codeBlock ?: return@AnnotationArrayGenerator invalidate())
        }
    }

    override fun visitEnd() {
        receive(builder.build())
    }
}

private class AnnotationArrayGenerator(val classNode: ClassNode, val receive: (CodeBlock?) -> Unit) : AnnotationVisitor(Opcodes.ASM9) {
    val builder: CodeBlock.Builder = CodeBlock.builder()

    var isInvalid: Boolean = false

    private fun invalidate() {
        isInvalid = true
    }

    init {
        builder.add("{")
    }

    override fun visit(name: String?, value: Any) {
        if (isInvalid) return
        builder.add("\$L, ", value.asCode(classNode) ?: return invalidate())
    }

    override fun visitEnum(name: String?, descriptor: String, value: String) {
        if (isInvalid) return
        val typeName = typeNameFromDescriptor(descriptor, classNode)
        if (typeName == null) invalidate()
        builder.add("\$T.\$N, ", typeName, value)
    }

    override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor? {
        if (isInvalid) return null
        return AnnotationSpecGenerator(descriptor, classNode) { annotationSpec ->
            builder.add("\$L, ", annotationSpec ?: return@AnnotationSpecGenerator invalidate())
        }
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        if (isInvalid) return null
        return AnnotationArrayGenerator(classNode) { codeBlock ->
            builder.add("\$L, ", codeBlock ?: return@AnnotationArrayGenerator invalidate())
        }
    }

    override fun visitEnd() {
        builder.add("}")
        receive(builder.build())
    }
}

private fun Any.asCode(classNode: ClassNode): CodeBlock? {
    return when (this) {
        is ByteArray -> asCode()
        is BooleanArray -> asCode()
        is ShortArray -> asCode()
        is CharArray -> asCode()
        is IntArray -> asCode()
        is LongArray -> asCode()
        is FloatArray -> asCode()
        is DoubleArray -> asCode()
        is Byte -> asCode()
        is Boolean -> asCode()
        is Short -> asCode()
        is Char -> asCode()
        is Int -> asCode()
        is Long -> asCode()
        is Float -> asCode()
        is Double -> asCode()
        is String -> asCode()
        is Type -> asCode(classNode)
        else -> error("invalid value type: ${this::class}")
    }
}

private fun ByteArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun BooleanArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun ShortArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun CharArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun IntArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun LongArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun FloatArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun DoubleArray.asCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

private fun Byte.asCode() = CodeBlock.of("\$L", this)
private fun Boolean.asCode() = CodeBlock.of("\$L", this)
private fun Short.asCode() = CodeBlock.of("\$L", this)
private fun Char.asCode() = CodeBlock.of("\$%L'", this.escape())
private fun Int.asCode() = CodeBlock.of("\$L", this)
private fun Long.asCode() = CodeBlock.of("\$L", this)
private fun Float.asCode() = CodeBlock.of("\$L", this)
private fun Double.asCode() = CodeBlock.of("\$L", this)
private fun String.asCode() = CodeBlock.of("\$S", this)
private fun Type.asCode(classNode: ClassNode): CodeBlock? {
    return CodeBlock.of("\$T", asTypeName(classNode) ?: return null)
}

private fun Char.escape(): String = when (this) {
    '\b' -> "\\b" /* \u0008: backspace (BS) */
    '\t' -> "\\t" /* \u0009: horizontal tab (HT) */
    '\n' -> "\\n" /* \u000a: linefeed (LF) */
    '\u000c' -> "\\f" /* \u000c: form feed (FF) */
    '\r' -> "\\r" /* \u000d: carriage return (CR) */
    '\"' -> "\"" /* \u0022: double quote (") */
    '\'' -> "\\'" /* \u0027: single quote (') */
    '\\' -> "\\\\" /* \u005c: backslash (\) */
    else -> if (Character.isISOControl(this)) String.format("\\u%04x", this.toInt()) else this.toString()
}
