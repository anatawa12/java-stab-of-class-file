package com.anatawa12.javaStabGen

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

fun List<AnnotationNode>.toSpecs(classNode: ClassNode): List<AnnotationSpec> = mapNotNull { it.toSpec(classNode) }

fun AnnotationNode.toSpec(classNode: ClassNode): AnnotationSpec? {
    val typeName = typeNameFromDescriptor(desc, classNode) as? ClassName ?: return null
    val builder = AnnotationSpec.builder(typeName)

    for (list in values.asSequence().chunked(2)) {
        val name = list[0] as String
        val value = list[1]
        builder.addMember(name, value.asAnnotationCode(classNode))
    }

    return builder.build()
}

fun Any.asAnnotationCode(classNode: ClassNode): CodeBlock? {
    return when (this) {
        is ByteArray -> asArrayBodyCode()
        is BooleanArray -> asArrayBodyCode()
        is ShortArray -> asArrayBodyCode()
        is CharArray -> asArrayBodyCode()
        is IntArray -> asArrayBodyCode()
        is LongArray -> asArrayBodyCode()
        is FloatArray -> asArrayBodyCode()
        is DoubleArray -> asArrayBodyCode()
        is Byte -> asCode()
        is Boolean -> asCode()
        is Short -> asCode()
        is Char -> asCode()
        is Int -> asCode()
        is Long -> asCode()
        is Float -> asCode()
        is Double -> asCode()
        is String -> asCode()
        is Type -> asAnnotationCode(classNode)
        is AnnotationNode -> asArrayBodyCode(classNode)
        is List<*> -> asArrayBodyCode(classNode)
        is Array<*> -> {
            val desc = this[0] as String
            val name = this[1] as String
            val typeName = typeNameFromDescriptor(desc, classNode) ?: return null
            CodeBlock.of("\$T.\$N", typeName, name)
        }
        else -> error("invalid value type: ${this::class}")
    }
}

fun AnnotationNode.asArrayBodyCode(classNode: ClassNode): CodeBlock? = CodeBlock.of("\$L", toSpec(classNode))

fun List<*>.asArrayBodyCode(classNode: ClassNode) = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it!!.asAnnotationCode(classNode)) } }.add("}").build()

fun ByteArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun BooleanArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun ShortArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun CharArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun IntArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun LongArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun FloatArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun DoubleArray.asArrayBodyCode() = CodeBlock.builder().add("{")
    .apply { forEach { add("\$L, ", it.asCode()) } }.add("}").build()

fun Byte.asCode() = CodeBlock.of("\$L", this)
fun Boolean.asCode() = CodeBlock.of("\$L", this)
fun Short.asCode() = CodeBlock.of("\$L", this)
fun Char.asCode() = CodeBlock.of("\$%L'", this.escape())
fun Int.asCode() = CodeBlock.of("\$L", this)
fun Long.asCode() = CodeBlock.of("\$L", this)
fun Float.asCode() = CodeBlock.of("\$L", this)
fun Double.asCode() = CodeBlock.of("\$L", this)
fun String.asCode() = CodeBlock.of("\$S", this)
fun Type.asAnnotationCode(classNode: ClassNode): CodeBlock? =
    asTypeName(classNode)?.let { CodeBlock.of("\$T.class", it) }

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
