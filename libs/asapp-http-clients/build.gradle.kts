plugins {
    id("asapp.library-conventions")
}

dependencies {
    // Compile
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework:spring-web")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.jackson.databind)
}
