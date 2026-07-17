plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    // Other
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    // Org
    runtimeOnly("org.postgresql:postgresql")
}
