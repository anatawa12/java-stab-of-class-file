package com.anatawa12.javaStabGen

import com.anatawa12.javaStabGen.tests.InterfaceImplementTest
import com.squareup.javapoet.JavaFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InterfaceImplementTest {
    @Test
    fun test() {
        val className = InterfaceImplementTest::class.qualifiedName!!.replace('.', '/') + ".class"
        val bytes = InterfaceImplementTest::class.java.classLoader.getResourceAsStream(className)!!.readBytes()
        val typeSpec = ClassStabGenerator.generate(bytes)!!
        val sourceFile =
            buildString { JavaFile.builder(typeSpec.first.packageName(), typeSpec.second).build().writeTo(this) }
        assertTrue(sourceFile.contains("implements Cloneable")) {
            "invalid generated code: " +
                    "No 'implements Cloneable'\n$sourceFile"
        }
    }
}
