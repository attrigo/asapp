import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

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
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
    // Other
    implementation(libs.findLibrary("nimbus-jose-jwt").get())

    // Runtime
    // Other
    runtimeOnly(libs.findLibrary("bootui-spring-boot-starter").get())

    // Test
    // Spring
    testImplementation(libs.findLibrary("spring-restdocs-mockmvc").get())
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Org
    testImplementation(libs.findLibrary("pitest-junit5-plugin").get())
    testImplementation(libs.findLibrary("testcontainers-junit-jupiter").get())
    testImplementation(libs.findLibrary("testcontainers-postgresql").get())
    // Other
    testImplementation(libs.findLibrary("testcontainers-redis").get())
    testImplementation(libs.findLibrary("archunit-junit5").get())
}
