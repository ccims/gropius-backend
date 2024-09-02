pluginManagement {
    val springBootVersion: String by settings
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val apolloVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.apollographql.apollo3") version apolloVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

include(":core")
include(":api-common")
include(":api-public")
include(":api-internal")
include(":sync-github")
include(":sync-jira")
include(":sync")