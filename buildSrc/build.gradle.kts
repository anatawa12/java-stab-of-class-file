plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
}

tasks.compileKotlin {
    source("./info.kt")
}
