description = "A Cross-Component Issue Management System for Component-based Architectures"

plugins {
    kotlin("jvm")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
}