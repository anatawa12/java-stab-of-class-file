package com.anatawa12.javaStabGen

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec

class ClassesGenerator {
    private val classes = mutableMapOf<ClassName?, MutableMap<ClassName, TypeSpec>>()

    fun addFile(byteArray: ByteArray): Boolean {
        val (className, typeSpec) = ClassStabGenerator.generate(byteArray) ?: return false
        classes.getOrPut(className.enclosingClassName()) { mutableMapOf() }[className] = typeSpec
        return true
    }

    fun generate(): MutableList<JavaFile> {
        val classes = classes.mapValues { it.value.toMutableMap() }
        val files = mutableListOf<JavaFile>()
        for ((className, typeSpec) in classes[null].orEmpty()) {
            files += JavaFile.builder(className.packageName(), combineNestedClasses(classes, className, typeSpec))
                .build()
        }
        classes[null]?.clear()
        for ((className, typeSpecs) in classes) {
            if (typeSpecs.isNotEmpty())
                error("no class decl found for $className")
        }
        return files
    }

    private fun combineNestedClasses(
        classes: Map<ClassName?, MutableMap<ClassName, TypeSpec>>,
        className: ClassName,
        typeSpec: TypeSpec,
    ): TypeSpec {
        val innerTypes = classes[className]
        if (innerTypes == null || innerTypes.isEmpty()) return typeSpec
        return typeSpec.toBuilder().also {
            for ((innerName, innerType) in innerTypes) {
                it.addType(combineNestedClasses(classes, innerName, innerType))
            }
            innerTypes.clear()
        }.build()
    }
}
