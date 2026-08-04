plugins {
    id("gropius.application-conventions")
}

dependencies {
    implementation(project(":api-common"))
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
