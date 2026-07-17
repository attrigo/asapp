plugins {
    id("asapp.service-conventions")
}

dependencies {
    // Compile
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")

    // Runtime
    // Other
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
