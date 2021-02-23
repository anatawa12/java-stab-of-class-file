plugins {
    kotlin("jvm") version "1.4.21"
    `java-library`
    `maven-publish`
    signing
}

group = "com.anatawa12.java-stab-gen"
version = "1.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    api("com.squareup:javapoet:1.13.0")
    implementation("org.ow2.asm:asm-tree:9.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

val test by tasks.getting(Test::class) {
    // JUnit platform を使う設定
    useJUnitPlatform()
}

@Suppress("UnstableApiUsage")
java {
    withJavadocJar()
    withSourcesJar()
}

val mavenPublication = publishing.publications.create("maven", MavenPublication::class) {
    from(components["java"])
    pom {
        name.set(project.base.archivesBaseName)
        configureAboutRepository()
    }
}

publishing.repositories {
    maven {
        name = "mavenCentral"
        url =
            if (version.toString().endsWith("SNAPSHOT")) uri("https://oss.sonatype.org/content/repositories/snapshots")
            else uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

        credentials {
            username = project.findProperty("com.anatawa12.sonatype.username")?.toString() ?: ""
            password = project.findProperty("com.anatawa12.sonatype.passeord")?.toString() ?: ""
        }
    }
}

signing {
    sign(mavenPublication)
}
