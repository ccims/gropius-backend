val graphglueVersion: String by project
val kosonVersion: String by project

plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(path = ":sync"))
    implementation("com.lectra", "koson", kosonVersion)
}
