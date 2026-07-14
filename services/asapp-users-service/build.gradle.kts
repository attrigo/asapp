plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    implementation(project(":libs:asapp-commons-url"))
    implementation(project(":libs:asapp-http-clients"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation(libs.springdoc.openapi)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.resilience4j)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.bootui)

    // Test
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.mockserver)
    testImplementation(libs.mockserver.client)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.json.unit.assertj)
}
