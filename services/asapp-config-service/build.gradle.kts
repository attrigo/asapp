import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    // ## ASAPP
    id("asapp.service-conventions")
}

dependencies {
    // # Compile
    // ## Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-config-server")
}

// # Tasks

// ## Developer commands

// Adds "native" for the config server's filesystem backend; the list replaces rather than adds, so "dev" is re-listed
tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "native,dev")
}
