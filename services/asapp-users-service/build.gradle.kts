import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.domain-service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Compile
    // ASAPP
    implementation(project(":libs:asapp-http-clients"))
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    // Other
    implementation(libs.findLibrary("resilience4j-spring-boot4").get())
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    // Org
    runtimeOnly("org.postgresql:postgresql")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    // Org
    testImplementation(libs.findLibrary("testcontainers-mockserver").get())
    // Other
    testImplementation(libs.findLibrary("mockserver-client-java").get())
    testImplementation(libs.findLibrary("mockserver-netty").get())
}
