import com.diffplug.spotless.LineEnding
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    id("com.diffplug.spotless")
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

// Formatting (Spotless) — all modules, mirroring Maven's inherited root-pom config.
// Eclipse JDT 4.35 + asapp_formatter.xml drive the formatting; the config files are anchored to
// rootProject so they resolve from every subproject. removeUnusedImports uses the cleanthat engine
// (not the default google-java-format) so the step needs no daemon --add-exports on Java 25.
spotless {
    lineEndings = LineEnding.UNIX
    java {
        eclipse("4.35").configFile(rootProject.file("asapp_formatter.xml"))
        importOrder("java|javax", "org", "com", "", "com.attrigo")
        removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
        licenseHeaderFile(rootProject.file("header-license"), "package ")
    }
}

// Every test tier runs on the JUnit Platform
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// The unit tier includes only *Tests
tasks.named<Test>("test") {
    include("**/*Tests.class")
}

// Unit-tier coverage report; the plugin does not wire it to run the tests
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}

// Single-source the report format policy for every JacocoReport task (unit, integration, merged): HTML only
tasks.withType<JacocoReport>().configureEach {
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Pin the JaCoCo tool so a Gradle wrapper bump can't silently change it; 0.8.14 gives official Java 25 support
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

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
