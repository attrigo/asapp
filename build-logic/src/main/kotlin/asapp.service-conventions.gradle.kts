import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.java-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("spring-cloud").get().requiredVersion}")
    }
}

dependencies {
    constraints {
        implementation(libs.findLibrary("guava").get())
        implementation(libs.findLibrary("commons-beanutils").get())
        implementation(libs.findLibrary("commons-io").get())
        implementation(libs.findLibrary("bcpkix-jdk18on").get())
        implementation(libs.findLibrary("bcprov-jdk18on").get())
        implementation(libs.findLibrary("rhino").get())
    }

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.findLibrary("json-unit-assertj").get())
}
