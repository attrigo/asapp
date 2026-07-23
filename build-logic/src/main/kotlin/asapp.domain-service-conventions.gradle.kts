import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
    id("org.asciidoctor.jvm.convert")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Shared PIT config for the domain services; per-service targetClasses/targetTests live in each service's build script
pitest {
    // Pin the PIT engine and JUnit 5 support versions
    pitestVersion = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    // Skip the plugin's launcher auto-add, its JUnit 5-era launcher breaks the JUnit 6 coverage minion
    addJUnitPlatformLauncher = false
    // Fail below 100% mutation coverage
    mutationThreshold = 100
    // Run PIT on the Java 25 toolchain, not the Gradle daemon's JVM (which may be older and fail on Java 25 code)
    jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.map { it.executablePath }
    // Keep reports at build/reports/pitest instead of a new timestamped folder each run
    timestampedReports = false
}

dependencies {
    // Compile
    // ASAPP
    implementation(project(":libs:asapp-commons-url"))
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // Org
    implementation(libs.findLibrary("mapstruct").get())
    annotationProcessor(libs.findLibrary("mapstruct-processor").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
    // Other
    implementation(libs.findLibrary("nimbus-jose-jwt").get())

    // Runtime
    // Org
    runtimeOnly("org.postgresql:postgresql")
    // Other
    runtimeOnly(libs.findLibrary("bootui-spring-boot-starter").get())

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Spring
    testImplementation(libs.findLibrary("spring-restdocs-mockmvc").get())
    // Org
    testImplementation(libs.findBundle("testcontainers-shared").get())
    // Other
    testImplementation(libs.findLibrary("archunit-junit5").get())
}

// Add spring-restdocs-asciidoctor (provides the guide's "operation::" blocks) under "asciidoctorExt" so the builder can load it.
val asciidoctorExt = configurations.create("asciidoctorExt")
dependencies {
    asciidoctorExt(libs.findLibrary("spring-restdocs-asciidoctor").get())
}

// Use an Asciidoctor engine compatible with spring-restdocs-asciidoctor (newer version); the one bundled with the Asciidoctor plugin is too old and has a security issue.
asciidoctorj {
    setVersion(libs.findVersion("asciidoctorj").get().requiredVersion)
}

val snippetsDir = layout.projectDirectory.dir("target/generated-snippets")

// Clean the Asciidoctor snippets which sit under target/
tasks.named<Delete>("clean") {
    delete(snippetsDir)
}

// The integration tests (*ApiDocumentationIT) create the examples.
tasks.named<Test>("integrationTest") {
    outputs.dir(snippetsDir)
}

// Build the HTML guide.
tasks.named<AsciidoctorTask>("asciidoctor") {
    inputs.dir(snippetsDir)
    // Load asciidoctorExt into the builder; this is what makes "operation::" work.
    configurations("asciidoctorExt")
    // Point the builder at the examples; only needed while Maven is present.
    attributes(mapOf("snippets" to snippetsDir.asFile.invariantSeparatorsPath))
    dependsOn(tasks.named<Test>("integrationTest"))
}

// Report all javadoc problems except missing comments.
tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

// Package the javadoc output; classifier `javadoc`.
tasks.register<Jar>("javadocJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main Javadoc."
    archiveClassifier = "javadoc"
    // from: what to package — the javadoc task's output.
    from(tasks.named("javadoc"))
}

// Package the main sources; classifier `sources`.
tasks.register<Jar>("sourcesJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main sources."
    archiveClassifier = "sources"
    // from: what to package — the main source set's files (allSource = Java sources + resources).
    from(project.the<SourceSetContainer>()["main"].allSource)
}
