package com.anatawa12.javaStabGen

import com.anatawa12.javaStabGen.tests.TypeAnnotationTest
import com.squareup.javapoet.JavaFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeAnnotationTest {
    @Test
    fun test() {
        val className = TypeAnnotationTest::class.qualifiedName!!.replace('.', '/') + ".class"
        val bytes = TypeAnnotationTest::class.java.classLoader.getResourceAsStream(className)!!.readBytes()
        val typeSpec = ClassStabGenerator.generate(bytes)!!
        val sourceFile =
            buildString { JavaFile.builder(typeSpec.first.packageName(), typeSpec.second).build().writeTo(this) }
        var prevIndex = 0
        repeat(9) { i ->
            val index = sourceFile.indexOf("@Indexing($i)")
            assertTrue(prevIndex < index) { "invalid generated code: @Indexing annotation sorting\n$sourceFile" }
            assertTrue(sourceFile.indexOf("@Indexing($i)", index + 1)
                    == -1) { "invalid generated code: multiple annotation:\n$sourceFile" }
            prevIndex = index
        }
    }
}
