import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Mutation testing (PIT) runs only on the domain services — Maven applied pitest-maven everywhere but <skip>
// on libs + infra services; here the plugin is simply absent from those. Per-service targetClasses/targetTests
// live in each service's build script; this block holds the shared config.
pitest {
    // PIT core + the JUnit 5 bridge on PIT's own 'pitest' configuration (off the test compile classpath)
    pitestVersion = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    // Fail below 100% mutation coverage (Maven mutationThreshold=100)
    mutationThreshold = 100
    // Fork PIT on the Java 25 toolchain, not the Gradle daemon JVM (szpak/gradle-pitest-plugin#301)
    jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.get().executablePath
    // Stable report path (build/reports/pitest), not a timestamped subfolder
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
