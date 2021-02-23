package com.anatawa12.javaStabGen

import com.anatawa12.javaStabGen.tests.TypeParameterTest
import com.squareup.javapoet.JavaFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeParameterTest {
    @Test
    fun test() {
        val className = TypeParameterTest::class.qualifiedName!!.replace('.', '/') + ".class"
        val bytes = TypeParameterTest::class.java.classLoader.getResourceAsStream(className)!!.readBytes()
        val typeSpec = ClassStabGenerator.generate(bytes)!!
        val sourceFile =
            buildString { JavaFile.builder(typeSpec.first.packageName(), typeSpec.second).build().writeTo(this) }
        assertTrue(sourceFile.contains("O extends OutputStream")) {
            "invalid generated code: " +
                    "No 'O extends OutputStream'\n$sourceFile"
        }
        assertTrue(sourceFile.contains("I extends InputStream")) {
            "invalid generated code: " +
                    "No 'I extends InputStream'\n$sourceFile"
        }
    }
}
