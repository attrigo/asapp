rootProject.name = "build-logic"

// Exposes the root version catalog (gradle/libs.versions.toml) to the convention plugins in this build
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
