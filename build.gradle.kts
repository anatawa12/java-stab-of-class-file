plugins {
    kotlin("jvm") version "1.4.21"
}

group = "com.anatawa12.java-stab-gen"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.ow2.asm:asm-tree:9.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

val test by tasks.getting(Test::class) {
    // JUnit platform を使う設定
    useJUnitPlatform()
}
