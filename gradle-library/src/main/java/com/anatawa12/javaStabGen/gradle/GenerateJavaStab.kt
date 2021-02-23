package com.anatawa12.javaStabGen.gradle

import com.anatawa12.javaStabGen.ClassesGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

@CacheableTask
open class GenerateJavaStab : DefaultTask() {
    @OutputDirectory
    var generatedDir: File? = null

    @InputFiles
    var classpath: FileCollection = project.files()

    @TaskAction
    fun generate() {
        val generatedDir = generatedDir ?: error("no generatedDir is specified")
        generatedDir.deleteRecursively()

        val gen = ClassesGenerator()
        for (file in classpath) {
            if (file.isFile)
                generateForJar(file, gen)
            if (file.isDirectory)
                generateForDir(file, gen)
        }

        for (javaFile in gen.generate()) {
            javaFile.writeTo(generatedDir)
        }
    }

    private fun generateForJar(file: File, gen: ClassesGenerator) {
        ZipFile(file).use { zip ->
            for (entry in ZipFile(file).entries()) {
                if (entry.isDirectory) continue
                if (!entry.name.endsWith(".class")) continue
                gen.addFile(zip.getInputStream(entry).readBytes())
            }
        }
    }

    private fun generateForDir(dir: File, gen: ClassesGenerator) {
        for (file in dir.walkTopDown()) {
            if (!file.isFile) continue
            if (file.extension != "class") continue
            gen.addFile(file.readBytes())
        }
    }
}
