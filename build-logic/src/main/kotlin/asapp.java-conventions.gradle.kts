import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

// Pins the JDK that compiles (and later tests) the project — reproducible regardless of the JVM launching Gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Compiler settings at parity with Maven: Java 25 API level, UTF-8 sources, and -parameters
// (-parameters is required by Spring for name-based binding: @ConfigurationProperties constructor binding,
//  constructor DI, and unnamed @PathVariable/@RequestParam). No typed property exists, so use compilerArgs.
tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Imports the Spring Boot BOM (via the io.spring.dependency-management plugin), overriding its jackson-bom version to pick up a CVE fix
// Uses this plugin over Gradle-native platform() for the bomProperty override
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get().requiredVersion}") {
            bomProperty("jackson-bom.version", libs.findVersion("jackson-bom").get().requiredVersion)
        }
    }
}
