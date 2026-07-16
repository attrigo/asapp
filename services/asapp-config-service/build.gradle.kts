plugins {
    id("asapp.service-conventions")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
