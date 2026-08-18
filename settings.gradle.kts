plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
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
