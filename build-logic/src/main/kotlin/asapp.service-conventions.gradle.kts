import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.java-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Imports the Spring Cloud BOM, additive on top of the Spring Boot BOM already imported by asapp.java-conventions
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("spring-cloud").get().requiredVersion}")
    }
}

dependencies {
    // CVE
    constraints {
        // Org
        implementation(libs.findLibrary("bcpkix-jdk18on").get())
        implementation(libs.findLibrary("bcprov-jdk18on").get())
        implementation(libs.findLibrary("rhino").get())
        // Other
        implementation(libs.findLibrary("guava").get())
        implementation(libs.findLibrary("commons-beanutils").get())
        implementation(libs.findLibrary("commons-io").get())
    }

    // Compile
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Runtime
    // Spring Boot
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    // Other
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // Other
    testImplementation(libs.findLibrary("json-unit-assertj").get())
}
