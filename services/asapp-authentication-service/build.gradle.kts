plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    // Org
    // Required by Spring Security PasswordEncoderFactories
    implementation(libs.bcprov.jdk18on)
    // Must be compile-scope to be used by JdbcConversionsConfiguration
    implementation("org.postgresql:postgresql")
    // Other
    implementation("io.micrometer:micrometer-registry-prometheus")
}
