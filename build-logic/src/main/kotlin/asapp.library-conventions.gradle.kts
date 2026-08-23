plugins {
    // ## Gradle
    `java-library`
    // ## ASAPP
    id("asapp.java-conventions")
    id("asapp.javadoc-sources-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get().requiredVersion}") {
            // Overrides the managed Jackson version in the imported Spring Boot BOM.
            // This covers libraries only; services override the same version in asapp.service-conventions.
            bomProperty("jackson-bom.version", libs.findVersion("jackson-bom").get().requiredVersion)
        }
    }
}

// # Tasks

// ## Aggregates

// Adds the jacoco coverage report to the full build
tasks.named("fullBuild") {
    dependsOn("jacocoTestReport")
}
