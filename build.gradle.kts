description = "A Cross-Component Issue Management System for Component-based Architectures"

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dependencies {
        subprojects.forEach { dokka(it) }
    }
}
