val graphglueVersion: String by project
val graphqlJavaVersion: String by project
val ktorVersion: String by project
val kotlinxSerializationVersion: String by project
val springBootVersion: String by project

plugins {
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(path = ":core"))
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinxSerializationVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    api("org.springframework.boot", "spring-boot-starter-data-mongodb-reactive", springBootVersion)
}