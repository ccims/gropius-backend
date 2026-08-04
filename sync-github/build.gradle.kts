plugins {
    id("gropius.application-conventions")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apollo)
}

dependencies {
    implementation(project(":sync"))
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.adapters.core)
    implementation(libs.koson)
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
                "com.apollographql.adapter.core.JavaOffsetDateTimeAdapter"
            )
        }
        generateOptionalOperationVariables.set(false)
        codegenModels.set("responseBased")
    }
}
