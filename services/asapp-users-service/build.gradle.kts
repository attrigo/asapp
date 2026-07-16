import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.domain-service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(project(":libs:asapp-http-clients"))
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(libs.findLibrary("resilience4j-spring-boot4").get())
    implementation("io.micrometer:micrometer-registry-prometheus")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation(libs.findLibrary("mockserver-client-java").get())
    testImplementation(libs.findLibrary("mockserver-netty").get())
    testImplementation(libs.findLibrary("testcontainers-mockserver").get())
}
