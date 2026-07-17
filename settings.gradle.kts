pluginManagement {
    includeBuild("build-logic")
}

// Centralizes the repository used to resolve dependencies for every project in the build
dependencyResolutionManagement {
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
