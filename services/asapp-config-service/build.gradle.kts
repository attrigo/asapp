import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("asapp.service-conventions")
}

dependencies {
    // Compile
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-config-server")

    // Runtime
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.json.unit.assertj)
}

tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "native,dev")
}
