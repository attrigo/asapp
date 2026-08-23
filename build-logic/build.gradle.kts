plugins {
    // ## Gradle
    // Compiles the .gradle.kts files under src/main/kotlin as applicable plugins — not automatic otherwise
    `kotlin-dsl`
}

dependencies {
    // # BOM
    // ## Spring Boot
    // Governs the versionless dependencies below
    implementation(platform(libs.spring.boot.dependencies))

    // # Build
    // Pins Kotlin to the version Gradle itself uses
    constraints {
        // ## Other
        implementation("org.jetbrains.kotlin:kotlin-reflect") {
            version { strictly(embeddedKotlinVersion) }
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib") {
            version { strictly(embeddedKotlinVersion) }
        }
    }
    // ## Other
    // Not redundant — the plugin below needs it
    implementation("org.liquibase:liquibase-core")

    // # Plugin
    // ## Spring Boot
    implementation(libs.spring.boot.gradle.plugin)
    // ## Spring
    implementation(libs.spring.dependency.management.plugin)
    // ## Other
    implementation(libs.asciidoctor.gradle.plugin)
    implementation(libs.cyclonedx.gradle.plugin)
    implementation(libs.gradle.git.properties.plugin)
    implementation(libs.gradle.pitest.plugin)
    implementation(libs.liquibase.gradle.plugin)
    implementation(libs.spotless.plugin)

    // # Test
    // ## Other
    // Leaves out TestKit: the kotlin-dsl plugin already provides it
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Runs the build's own tests (build-logic/src/test/kotlin) on the JUnit Platform
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
