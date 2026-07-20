import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Shared PIT config for the domain services; per-service targetClasses/targetTests live in each service's build script
pitest {
    // Pin the PIT engine and JUnit 5 support versions
    pitestVersion = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    // Fail below 100% mutation coverage
    mutationThreshold = 100
    // Run PIT on the Java 25 toolchain, not the Gradle daemon's JVM (which may be older and fail on Java 25 code)
    jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.get().executablePath
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
