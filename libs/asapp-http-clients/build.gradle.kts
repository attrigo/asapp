plugins {
    id("asapp.library-conventions")
}

dependencies {
    // Compile
    // ASAPP
    implementation(project(":libs:asapp-commons-url"))
    // Spring
    compileOnly("org.springframework:spring-web")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring
    testImplementation("org.springframework:spring-web")
    // Other
    testImplementation(libs.jackson.databind)
}
