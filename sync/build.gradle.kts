plugins {
    id("gropius.kotlin-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core"))
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.contentNegotiation)
    api(libs.ktor.serialization.kotlinxJson)
    api(libs.kotlinx.serialization.json)
    api(libs.springBoot.starter.data.mongodb.reactive)
    implementation(libs.springBoot.starter.webflux)
}
