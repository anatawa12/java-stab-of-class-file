import org.gradle.api.publish.maven.MavenPom;

fun MavenPom.configureAboutRepository() {
    description.set("a stab in java of class file generator. a tool for workaround of KT-24309.")
    url.set("https://github.com/anatawa12/java-stab-of-class-file")

    scm {
        url.set("https://github.com/anatawa12/java-stab-of-class-file")
        connection.set("scm:git:https://github.com/anatawa12/java-stab-of-class-file.git")
        developerConnection.set("scm:git:git@github.com/anatawa12/java-stab-of-class-file.git")
    }

    issueManagement {
        system.set("github")
        url.set("https://github.com/anatawa12/java-stab-of-class-file/issues")
    }

    licenses {
        license {
            // TODO
            distribution.set("repo")
        }
    }

    developers {
        developer {
            id.set("anatawa12")
            name.set("anatawa12")
            roles.set(setOf("developer"))
        }
    }
}
