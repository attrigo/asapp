import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("asapp.java-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Imports the Spring Cloud BOM, additive on top of the Spring Boot BOM already imported by asapp.java-conventions
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("spring-cloud").get().requiredVersion}")
    }
}

dependencies {
    // CVE
    constraints {
        // Org
        implementation(libs.findLibrary("bcpkix-jdk18on").get())
        implementation(libs.findLibrary("bcprov-jdk18on").get())
        implementation(libs.findLibrary("rhino").get())
        // Other
        implementation(libs.findLibrary("guava").get())
        implementation(libs.findLibrary("commons-beanutils").get())
        implementation(libs.findLibrary("commons-io").get())
    }

    // Compile
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Runtime
    // Spring Boot
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    // Other
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // Other
    testImplementation(libs.findLibrary("json-unit-assertj").get())
}

// The integration tier reuses the test source set
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the integration and end-to-end tiers (*IT, *E2EIT)."
    group = "verification"
    // Full @SpringBootTest contexts share one worker JVM and exhaust the 512m default heap; give it headroom
    maxHeapSize = "1g"
    // Reuse the compiled test classes and their runtime classpath (the *IT subset is selected below)
    val testSourceSet = project.the<SourceSetContainer>()["test"]
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include("**/*IT.class")
    shouldRunAfter(tasks.named("test"))
}

// Check runs the integration tier too
tasks.named("check") {
    dependsOn(integrationTest)
}

// Integration-tier coverage report (jacoco-it analog)
tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    description = "Generates a code coverage report for the integration tier (*IT, *E2EIT)."
    group = "verification"
    dependsOn(integrationTest)
    executionData(integrationTest.get())
    sourceSets(project.the<SourceSetContainer>()["main"])
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}

// Merged unit + integration coverage report (jacoco-aggregate analog)
tasks.register<JacocoReport>("jacocoAggregateReport") {
    description = "Generates a merged unit and integration code coverage report."
    group = "verification"
    val test = tasks.named<Test>("test")
    dependsOn(test, integrationTest)
    executionData(test.get(), integrationTest.get())
    sourceSets(project.the<SourceSetContainer>()["main"])
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}
