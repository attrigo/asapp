import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.library-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Compile
    // ASAPP
    implementation(project(":libs:asapp-commons-url"))
    // Spring
    implementation("org.springframework:spring-web")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Other
    testImplementation(libs.findLibrary("jackson-databind").get())
}
