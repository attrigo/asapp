import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.domain-service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Required by Spring Security PasswordEncoderFactories
    implementation(libs.findLibrary("bcprov-jdk18on").get())
    // Must be compile-scope to be used by JdbcConversionsConfiguration
    implementation("org.postgresql:postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
