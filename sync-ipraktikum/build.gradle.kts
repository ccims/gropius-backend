plugins {
    id("gropius.application-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":sync"))
    implementation(libs.snakeyaml.engine)
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
