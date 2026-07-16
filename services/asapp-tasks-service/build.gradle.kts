plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
}
