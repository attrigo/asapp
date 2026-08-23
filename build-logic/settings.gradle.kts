rootProject.name = "build-logic"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Centralizes the repositories used to resolve the plugin jars this build depends on
    // Central leads so the portal serves only the plugin jars it alone publishes
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // Exposes the root version catalog (gradle/libs.versions.toml) to the convention plugins in this build
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
