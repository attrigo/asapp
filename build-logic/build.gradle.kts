plugins {
    // Required so .gradle.kts files under src/main/kotlin compile as applicable plugins — not automatic otherwise
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
