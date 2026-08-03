plugins {
    id("gropius.application-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":sync"))
    implementation(libs.koson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.logging)
}
