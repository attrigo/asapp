plugins {
    id("asapp.service-conventions")
}

dependencies {
    // Compile
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-config-server")

    // Runtime
    // Other
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
