import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.library-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework:spring-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.findLibrary("jackson-databind").get())
}
