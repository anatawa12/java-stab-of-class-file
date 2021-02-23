# java stab of class file generator

This is for a workaround of [KT-24309].

## How to use

### As a Gradle Library

1. add ``com.anatawa12.java-stab-gen:gradle-library:<version>`` to buildscript depencencies.
   ```groovy
   buildscript {
       repositories {
           mavenCentral()
       }
       dependencies {
           classpath("com.anatawa12.java-stab-gen:gradle-library:<version>")
       }
   }
   ```
2. create task with type `com.anatawa12.javaStabGen.gradle.ClassesGenerator`
   <details>
      <summary>kotlin</summary>

      ```kotlin
      import com.anatawa12.javaStabGen.gradle.GenerateJavaStab

      val generateJavaStab by tasks.creating(GenerateJavaStab::class) {
          generatedDir = file("$buildDir/generated/stab")
          classpath = classpath_you_want_to_generate_stab_for
      }
      ```

   </details>
   <details open>
      <summary>groovy</summary>

      ```groovy
      import com.anatawa12.javaStabGen.gradle.GenerateJavaStab
      task generateJavaStab(type: GenerateJavaStab) {
          generatedDir = file("$buildDir/generated/stab")
          classpath = classpath_you_want_to_generate_stab_for
      }
      ```

   </details>

[KT-24309]: https://youtrack.jetbrains.com/issue/KT-24309
