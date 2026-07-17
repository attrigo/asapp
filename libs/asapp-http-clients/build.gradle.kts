plugins {
    id("asapp.library-conventions")
}

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
    testImplementation(libs.jackson.databind)
}
