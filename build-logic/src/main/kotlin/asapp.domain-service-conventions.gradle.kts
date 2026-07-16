import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(libs.findLibrary("mapstruct").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
    implementation(libs.findLibrary("nimbus-jose-jwt").get())
    runtimeOnly(libs.findLibrary("bootui-spring-boot-starter").get())

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.findLibrary("spring-restdocs-mockmvc").get())
    testImplementation(libs.findLibrary("pitest-junit5-plugin").get())
    testImplementation(libs.findLibrary("testcontainers-junit-jupiter").get())
    testImplementation(libs.findLibrary("testcontainers-postgresql").get())
    testImplementation(libs.findLibrary("testcontainers-redis").get())
    testImplementation(libs.findLibrary("archunit-junit5").get())
}
