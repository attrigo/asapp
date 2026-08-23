pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "asapp"

// Centralizes the repositories used to resolve dependencies for every project in the build
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Fails the build if any project declares its own repositories, keeping them central-only
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("libs:asapp-commons-url")
include("libs:asapp-http-clients")
include("services:asapp-authentication-service")
include("services:asapp-config-service")
include("services:asapp-discovery-service")
include("services:asapp-tasks-service")
include("services:asapp-users-service")
