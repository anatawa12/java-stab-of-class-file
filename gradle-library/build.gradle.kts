plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

group = project(":").group
version = project(":").version

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(project(":"))
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
