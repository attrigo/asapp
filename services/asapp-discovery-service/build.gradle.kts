plugins {
    id("asapp.service-conventions")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
