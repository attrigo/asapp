import com.diffplug.spotless.LineEnding

plugins {
    // ## Gradle
    jacoco
    java
    // ## Spring
    id("io.spring.dependency-management")
    // ## Other
    id("com.diffplug.spotless")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val javaVersion = 25

dependencies {
    // # Test
    // ## Other
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// # Extensions

// Pins the JaCoCo tool so a Gradle wrapper bump cannot silently change it
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

// Selects which JDK compiles, tests, and runs the app
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

// Applies the same Java formatting rules to every module
spotless {
    lineEndings = LineEnding.UNIX
    java {
        eclipse("4.35").configFile(layout.settingsDirectory.file("asapp_formatter.xml"))
        importOrder("java|javax", "org", "com", "", "com.attrigo")
        removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
        licenseHeaderFile(layout.settingsDirectory.file("header-license"), "package ")
    }
}

// # Tasks

// ## Compile

// Sets the compiler options for every Java compilation
tasks.withType<JavaCompile>().configureEach {
    // Targets exactly the pinned Java version, rejecting newer and JDK-internal APIs
    options.release = javaVersion
    // Pinned so the source encoding never follows the OS or Gradle daemon default
    options.encoding = "UTF-8"
    // Keeps method parameter names in bytecode for Spring's name-based binding
    // (@ConfigurationProperties constructor binding, constructor DI, unnamed @PathVariable/@RequestParam)
    options.compilerArgs.add("-parameters")
}

// ## Test

// Sets the options every test tier shares
tasks.withType<Test>().configureEach {
    // Every tier runs on the JUnit Platform
    useJUnitPlatform()
    // Fails on formatting before paying for any tier
    mustRunAfter(tasks.named("spotlessCheck"))
}

// Narrows the unit tier to *Tests
tasks.named<Test>("test") {
    include("**/*Tests.class")
}

// ## Coverage

// Applies one report-format (HTML) policy to every JacocoReport task (unit, integration, merged)
tasks.withType<JacocoReport>().configureEach {
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}

// Runs the unit tier first, which the JaCoCo plugin does not wire itself
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}

// ## Aggregates

// Declares the full-build umbrella; each module archetype extends it with the extra artifacts it produces
tasks.register("fullBuild") {
    group = "build"
    description = "Runs the full build: assemble, every test tier, the formatting check, plus the coverage reports, the API documentation, and the javadoc and sources jars for the modules that produce them."

    dependsOn("build")
}

// Declares the CI umbrella; asapp.domain-service-conventions extends it with the extra check the pipeline gates on
tasks.register("ciBuild") {
    group = "build"
    description = "Runs every check the CI pipeline gates on."

    dependsOn("build")
}
