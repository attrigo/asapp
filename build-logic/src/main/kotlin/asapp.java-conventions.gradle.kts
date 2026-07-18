import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

val javaVersion = 25

// Selects which JDK compiles, tests, and runs the app
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

// Compiler options applied to every Java compilation
// Release: target exactly the pinned Java version, rejecting newer or JDK-internal APIs
// Encoding: pin source encoding so it never depends on the OS or Gradle daemon default
// CompilerArgs: -parameters keeps method parameter names in bytecode for Spring name-based binding
//   (@ConfigurationProperties constructor binding, constructor DI, unnamed @PathVariable/@RequestParam)
tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

// Every test tier runs on the JUnit Platform
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// The unit tier includes only *Tests
tasks.named<Test>("test") {
    include("**/*Tests.class")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Imports the Spring Boot BOM (via the io.spring.dependency-management plugin), overriding its jackson-bom version to pick up a CVE fix
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get().requiredVersion}") {
            bomProperty("jackson-bom.version", libs.findVersion("jackson-bom").get().requiredVersion)
        }
    }
}

dependencies {
    // Test
    // Org
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
