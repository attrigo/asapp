import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    // ## ASAPP
    id("asapp.javadoc-sources-conventions")
    id("asapp.service-conventions")
    // ## Other
    id("info.solidsoft.pitest")
    id("org.asciidoctor.jvm.convert")
    id("org.liquibase.gradle")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
// The extra classpath the Asciidoctor builder loads; its entry provides the guide's "operation::" blocks
@Suppress("UnstableApiUsage")
val asciidoctorExt = configurations.dependencyScope("asciidoctorExt")
@Suppress("UnstableApiUsage")
val asciidoctorExtClasspath = configurations.resolvable("asciidoctorExtClasspath") { extendsFrom(asciidoctorExt.get()) }
val snippetsDir = layout.buildDirectory.dir("generated-snippets")

dependencies {
    // # Compile
    // ## ASAPP
    implementation(project(":libs:asapp-commons-url"))
    // ## Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // ## Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // ## Other
    implementation(libs.findLibrary("mapstruct").get())
    annotationProcessor(libs.findLibrary("mapstruct-processor").get())
    implementation(libs.findLibrary("nimbus-jose-jwt").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())

    // # Runtime
    // ## Other
    // Boot UI ships ArchUnit transitively; keep the test-only library out of the runtime image
    runtimeOnly(libs.findLibrary("bootui-spring-boot-starter").get()) {
        exclude(group = "com.tngtech.archunit", module = "archunit")
    }
    runtimeOnly("org.postgresql:postgresql")

    // # Test
    // ## Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // ## Spring
    testImplementation(libs.findLibrary("spring-restdocs-mockmvc").get())
    // ## Other
    testImplementation(libs.findLibrary("archunit-junit5").get())
    testImplementation(libs.findBundle("testcontainers-shared").get())

    // # Tool
    // ## Spring
    @Suppress("UnstableApiUsage")
    asciidoctorExt(libs.findLibrary("spring-restdocs-asciidoctor").get())
    // ## Other
    liquibaseRuntime("org.liquibase:liquibase-core")
    liquibaseRuntime(libs.findLibrary("picocli").get())
    liquibaseRuntime("org.postgresql:postgresql")
}

// # Extensions

// Pins an Asciidoctor engine compatible with spring-restdocs-asciidoctor; the one the Asciidoctor plugin bundles is older and carries a security issue
asciidoctorj {
    setVersion(libs.findVersion("asciidoctorj").get().requiredVersion)
}

// Adds the database commands a developer runs by hand; each service adds its own url
liquibase {
    activities.register("main") {
        withGroovyBuilder {
            "searchPath"(layout.projectDirectory.dir("src/main/resources").asFile.path)
            "changelogFile"("liquibase/db/changelog/db.changelog-master.xml")
            "username"("user")
            "password"("secret")
            "logLevel"("off")
        }
    }
    runList = "main"
}

// Shares the mutation-testing config across the domain services; each one adds its own packages
pitest {
    pitestVersion = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    // Skips the plugin's launcher auto-add; its JUnit 5-era launcher breaks the JUnit 6 coverage minion
    addJUnitPlatformLauncher = false
    mutationThreshold = 100
    // Runs PIT on the Java 25 toolchain, not the Gradle daemon's JVM, which may be older and fail on Java 25 code
    jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.map { it.executablePath }
    // Keeps reports at build/reports/pitest instead of a new timestamped folder each run
    timestampedReports = false
}

// # Tasks

// ## Test

// Declares the generated examples as an output of the integration tier
tasks.named<Test>("integrationTest") {
    outputs.dir(snippetsDir)
}

// ## Documentation

// Builds the HTML API guide from the generated examples
tasks.named<AsciidoctorTask>("asciidoctor") {
    inputs.dir(snippetsDir)
    // Loads the asciidoctorExt classpath into the builder; this is what makes "operation::" work
    configurations(asciidoctorExtClasspath.name)
    dependsOn(tasks.named<Test>("integrationTest"))
}

// ## Aggregates

// Adds the domain services' extra full-build artifacts: the three coverage reports and the API guide
tasks.named("fullBuild") {
    dependsOn(
        "jacocoTestReport",
        "jacocoIntegrationTestReport",
        "jacocoMergedReport",
        "asciidoctor",
    )
}

// Adds the domain services' extra CI check: the API guide
tasks.named("ciBuild") {
    dependsOn("asciidoctor")
}
