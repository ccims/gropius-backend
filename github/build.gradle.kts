val graphglueVersion: String by project
val apolloVersion: String by project
val kosonVersion: String by project

plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
    id("com.apollographql.apollo3")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(path = ":sync"))
    implementation("com.apollographql.apollo3", "apollo-runtime", apolloVersion)
    implementation("com.apollographql.apollo3", "apollo-adapters", apolloVersion)
    implementation("com.lectra", "koson", kosonVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-debug", "1.4.0")
}


apollo {
    service("github") {
        packageName.set("gropius.sync.github.generated")
        introspection {
            endpointUrl.set("https://api.github.com/graphql")
            schemaFile.set(file("src/main/graphql/gropius/sync/github/schema.graphqls"))
            mapScalar(
                "DateTime",
                "java.time.OffsetDateTime",
                "com.apollographql.apollo3.adapter.JavaOffsetDateTimeAdapter"
            )
        }
        generateOptionalOperationVariables.set(false)
        codegenModels.set("responseBased")
    }
}
