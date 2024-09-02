val graphglueVersion: String by project
val kosonVersion: String by project
val kotlinxSerializationVersion: String by project
val ktorVersion: String by project

plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(path = ":sync"))
    implementation("com.lectra", "koson", kosonVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinxSerializationVersion)
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.ktor", "ktor-client-logging", ktorVersion)
}
