import org.cyclonedx.gradle.CyclonedxAggregateTask
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.parsers.BomParserFactory
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    // ## ASAPP
    id("asapp.java-conventions")
    // ## Spring
    id("org.springframework.boot")
    // ## Other
    id("com.gorylenko.gradle-git-properties")
    id("org.cyclonedx.bom")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Overrides the managed Jackson version via a Gradle "extra" property.
// It goes through "extra" because the Boot plugin auto-imports the BOM, which ignores bomProperty.
// This covers services only; libraries override the same version in asapp.library-conventions.
extra["jackson-bom.version"] = libs.findVersion("jackson-bom").get().requiredVersion

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("spring-cloud").get().requiredVersion}")
    }
}

dependencies {
    // # CVE
    constraints {
        // ## Other
        implementation(libs.findLibrary("bcpkix-jdk18on").get())
        implementation(libs.findLibrary("bcprov-jdk18on").get())
        implementation(libs.findLibrary("commons-beanutils").get())
        implementation(libs.findLibrary("commons-io").get())
        implementation(libs.findLibrary("guava").get())
        implementation(libs.findLibrary("rhino").get())
    }

    // # Compile
    // ## Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // # Runtime
    // ## Spring Boot
    // Keeps devtools available for local runs but out of the runnable jar
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // ## Other
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // # Test
    // ## Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // ## Other
    testImplementation(libs.findLibrary("json-unit-assertj").get())
}

// # Extensions

// Ignores volatile build, git and SBOM metadata so every runtime-classpath consumer stays cacheable
normalization {
    runtimeClasspath {
        properties("META-INF/build-info.properties") {
            ignoreProperty("build.time")
        }
        ignore("git.properties")
        ignore("META-INF/sbom/application.cdx.json")
    }
}

// Generates build-info.properties, surfaced by the actuator /info endpoint
springBoot {
    buildInfo {
        properties {
            additional.put("encoding", "UTF-8")
            additional.put("java", java.toolchain.languageVersion.map { it.toString() })
        }
    }
}

// Restricts git.properties so the build host and developer identity never reach published images
gitProperties {
    keys = listOf("git.branch", "git.commit.id", "git.commit.time", "git.build.version")
}

// # Tasks

// ## Test

// Runs the integration tier, reusing the test source set
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs the integration and end-to-end tiers (*IT, *E2EIT)."

    // Full @SpringBootTest contexts share one worker JVM and exhaust the 512m default heap
    maxHeapSize = "1g"
    // Reuses the compiled test classes and their runtime classpath; the *IT subset is selected below
    val testSourceSet = project.the<SourceSetContainer>()["test"]
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include("**/*IT.class")
    mustRunAfter(tasks.named("test"))
}

// Adds the integration tier to check
tasks.named("check") {
    dependsOn(tasks.named<Test>("integrationTest"))
}

// ## Coverage

// Reports coverage for the integration tier
tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    group = "verification"
    description = "Generates a code coverage report for the integration tier (*IT, *E2EIT)."

    val integrationTest = tasks.named<Test>("integrationTest")
    dependsOn(integrationTest)
    executionData(integrationTest.map { it.the<JacocoTaskExtension>().destinationFile })
    sourceSets(project.the<SourceSetContainer>()["main"])
}

// Reports merged unit and integration coverage
tasks.register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    description = "Generates a merged unit and integration code coverage report."

    val test = tasks.named<Test>("test")
    val integrationTest = tasks.named<Test>("integrationTest")
    dependsOn(test, integrationTest)
    executionData(
        test.map { it.the<JacocoTaskExtension>().destinationFile },
        integrationTest.map { it.the<JacocoTaskExtension>().destinationFile },
    )
    sourceSets(project.the<SourceSetContainer>()["main"])
}

// ## Packaging

// Disables the plain library jar so each service ships a single runnable jar
tasks.named<Jar>("jar") {
    enabled = false
}

// Sets what the SBOM at META-INF/sbom scans, the file the actuator /sbom endpoint serves.
// Boot's plugin owns the rest — the project type, the resource copy and the jar manifest.
tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
    // The configuration that actually ships in the bootJar
    includeConfigs = listOf("productionRuntimeClasspath")
    // Only the JSON feeds the aggregate; the XML would be written and reparsed for nothing
    xmlOutput.unsetConvention()
}

// Writes the SBOM that ships; leaving out the build system keeps it the same on every machine
tasks.named<CyclonedxAggregateTask>("cyclonedxBom") {
    includeBuildSystem = false
    val bomOutput = jsonOutput
    // Fails the build if the SBOM came out empty
    doLast {
        val bomFile = bomOutput.get().asFile
        val components = BomParserFactory.createParser(bomFile).parse(bomFile).components
        if (components.isNullOrEmpty()) {
            throw GradleException("No components found in $bomFile — the includeConfigs scope filter on cyclonedxDirectBom likely matched no configuration.")
        }
    }
}

// Builds the service's Docker image; needs a running Docker daemon
// Leaves createdDate unset, so identical builds produce identical images
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = "ghcr.io/attrigo/${project.name}:${project.version}"
    // Replaces the builder's default list, so the java buildpack is re-selected explicitly;
    // the health checker contributes the /workspace/health-check the compose health checks run
    buildpacks = listOf("urn:cnb:builder:paketo-buildpacks/java", "docker.io/paketobuildpacks/health-checker")
    // environment is a MapProperty: put each entry, never assign, which would drop the others
    environment.put("BP_HEALTH_CHECKER_ENABLED", "true")
    environment.put("BP_JVM_VERSION", java.toolchain.languageVersion.map { it.toString() })
}

// ## Developer commands

// Sets the local-run defaults for every service; a developer can override them with --args
tasks.named<BootRun>("bootRun") {
    // Uses a system property so a developer's --args adds to this rather than replacing it
    systemProperty("spring.profiles.active", "dev")
    // Runs from the repo root, where config-service looks for its config files
    workingDir = layout.settingsDirectory.asFile
}
