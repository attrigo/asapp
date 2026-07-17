pluginManagement {
    includeBuild("build-logic")
}

// Centralizes the repositories used to resolve dependencies for every project in the build
dependencyResolutionManagement {
    // Fail if any project declares its own repositories, keeping them central-only
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "asapp"

include("libs:asapp-commons-url")
include("libs:asapp-http-clients")
include("services:asapp-authentication-service")
include("services:asapp-config-service")
include("services:asapp-discovery-service")
include("services:asapp-tasks-service")
include("services:asapp-users-service")
