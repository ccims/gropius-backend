/**
 * Shared configuration for every Gropius backend module: Kotlin/Spring compilation against
 * the Spring Boot platform, and Dokka documentation that falls back to `@GraphQLDescription`.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)
}

dependencies {
    "api"(platform(libs.findLibrary("springBoot-dependencies").get()))
    "dokkaPlugin"(libs.findLibrary("dokka-graphqlDescriptionPlugin").get())
}

dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.entries.asIterable()
        )
    }
}
