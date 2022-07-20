val graphglueVersion: String by project
val graphqlJavaVersion: String by project

plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(path = ":core"))
}