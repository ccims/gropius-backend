val graphglueVersion: String by project
val springBootVersion: String by project

plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
    id("com.apollographql.apollo3").version("3.3.0")
}

dependencies {
    implementation(project(path = ":sync"))
    implementation("com.apollographql.apollo3:apollo-runtime:3.3.0")
    implementation("com.apollographql.apollo3:apollo-adapters:3.3.0")
    implementation("org.springframework.boot", "spring-boot-starter-data-mongodb-reactive", springBootVersion)
}


apollo {
    packageName.set("gropius.sync.github.generated")
    introspection {
        endpointUrl.set("https://api.github.com/graphql")
        schemaFile.set(file("src/main/graphql/gropius/sync/github/schema.graphqls"))
        mapScalar("DateTime", "java.time.OffsetDateTime", "com.apollographql.apollo3.adapter.JavaOffsetDateTimeAdapter")
    }
    generateOptionalOperationVariables.set(false)
    codegenModels.set("responseBased")
}
