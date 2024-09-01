val graphglueVersion: String by project
val graphqlJavaVersion: String by project
val jsonSchemaValidatorVersion: String by project
val springBootVersion: String by project

plugins {
    kotlin("plugin.spring")
}

dependencies {
    api("io.github.graphglue", "graphglue-core", graphglueVersion)
    api("com.graphql-java","graphql-java-extended-scalars", graphqlJavaVersion)
    implementation("org.springframework.boot", "spring-boot-starter-actuator", springBootVersion)
}