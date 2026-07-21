plugins {
    // Required so .gradle.kts files under src/main/kotlin compile as applicable plugins — not automatic otherwise
    `kotlin-dsl`
}

// Centralizes the repositories used to resolve plugins for the convention build scripts
repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.gradle.pitest.plugin)
    implementation(libs.spotless.plugin)
}
