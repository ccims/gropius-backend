// Provisions the JDK the toolchain asks for when it is not already installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        // graph-glue snapshots, until 7.2.5 is released. A locally built graph-glue
        // (`./gradlew publishToMavenLocal` in the graph-glue repo) takes precedence.
        mavenLocal {
            content { includeGroup("io.github.graphglue") }
        }
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("io.github.graphglue") }
        }
    }
}

rootProject.name = "gropius-backend"

include(":core")
include(":api-common")
include(":api-public")
include(":api-internal")
include(":sync-github")
include(":sync-jira")
include(":sync")
