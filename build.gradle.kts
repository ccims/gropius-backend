import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "A Cross-Component Issue Management System for Component-based Architectures"

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

allprojects {
    val javaVersion: String by project

    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }
    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

subprojects {
    val dokkaGraphQLDescriptionPluginVersion: String by project

    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")

    dependencies {
        dokkaPlugin("io.github.graphglue", "dokka-graphql-description-plugin", dokkaGraphQLDescriptionPluginVersion)
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets {
            configureEach {
                includeNonPublic.set(true)
            }
        }
    }
}
