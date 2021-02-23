# java stab of class file generator

This is for a workaround of [KT-24309].

## How to use

### As a Gradle Library

1. add ``com.anatawa12.java-stab-gen:gradle-library:1.0.0`` to buildscript depencencies.
   <details>
      <summary>kotlin</summary>
       ```groovy
       buildscript {
           repositories {
               mavenCentral()
           }
           dependencies {
               classpath("com.anatawa12.java-stab-gen:gradle-library:1.0.0")
           }
       }
       ```
   </details>
   <details>
      <summary>groovy</summary>
       ```groovy
       buildscript {
           repositories {
               mavenCentral()
           }
           dependencies {
               classpath "com.anatawa12.java-stab-gen:gradle-library:1.0.0"
           }
       }
       ```
   </details>
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
   <details>
      <summary>groovy</summary>

      ```groovy
      import com.anatawa12.javaStabGen.gradle.GenerateJavaStab
      task generateJavaStab(type: GenerateJavaStab) {
          generatedDir = file("$buildDir/generated/stab")
          classpath = classpath_you_want_to_generate_stab_for
      }
      ```

   </details>

## Examples

### Workaround of [KT-24309]

<details>
<summary>groovy</summary>

```groovy
import com.anatawa12.javaStabGen.gradle.GenerateJavaStab

def theLibraryDependsThisProject = TODO

// generate stab for $theLibraryDependsThisProject
// into "$buildDir/generated/stab"
task generateJavaStab(type: GenerateJavaStab) {
   generatedDir = file("$buildDir/generated/stab")
   classpath = files(theLibraryDependsThisProject)
}
compileKotlin {
   dependsOn(generateJavaStab)
   // add and include generateJavaStab.generatedDir as java source code
   source(generateJavaStab.generatedDir)
   include("**/*.java")
}
```

</details>

[KT-24309]: https://youtrack.jetbrains.com/issue/KT-24309
