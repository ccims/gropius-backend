plugins {
    id("gropius.kotlin-conventions")
}

dependencies {
    api(libs.graphglue.core)
    api(libs.graphqlJava.extendedScalars)
    implementation(libs.springBoot.starter.actuator)
}
