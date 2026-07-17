plugins {
    id("asapp.domain-service-conventions")
}

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
    implementation(libs.resilience4j.spring.boot4)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    // Org
    runtimeOnly("org.postgresql:postgresql")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    // Org
    testImplementation(libs.testcontainers.mockserver)
    // Other
    testImplementation(libs.mockserver.client.java)
    testImplementation(libs.mockserver.netty)
}
