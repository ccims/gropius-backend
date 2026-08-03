description = "A Cross-Component Issue Management System for Component-based Architectures"

// The version comes from the version catalog via `buildSrc`, which already puts Dokka
// on the build classpath, so it is applied here without a version.
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dependencies {
        subprojects.forEach { dokka(it) }
    }
}
