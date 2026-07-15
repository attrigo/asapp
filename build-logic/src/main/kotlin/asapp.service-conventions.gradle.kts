import org.gradle.accessors.dm.LibrariesForLibs
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("asapp.java-conventions")
    id("org.springframework.boot")
    id("org.cyclonedx.bom")
    id("com.gorylenko.gradle-git-properties")
}

// Same explicit bridge as asapp.java-conventions — see gradle/gradle#15383.
val libs = the<LibrariesForLibs>()

dependencies {
    // Spring Cloud BOM as a native platform.
    val cloudBom = platform(libs.spring.cloud.bom)
    implementation(cloudBom)
    annotationProcessor(cloudBom)

    // CVE override: raise Jackson above the Spring Boot BOM pin (mirrors the Maven
    // <jackson-bom.version> override), for the deployed services only.
    implementation(platform(libs.jackson.bom))

    // Purely-transitive CVE bumps — forced even when only pulled in transitively.
    constraints {
        implementation(libs.guava)
        implementation(libs.commons.io)
        implementation(libs.commons.beanutils)
        implementation(libs.rhino)
        implementation(libs.bcprov)
        implementation(libs.bcpkix)
    }

    // Every service declares devtools (Maven: runtime + optional).
    // `developmentOnly` does not extend `implementation`, so the Spring Boot BOM platform
    // must be attached here too, or the version-less coordinate below fails to resolve.
    developmentOnly(platform(libs.spring.boot.bom))
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

// Disable the plain jar so build/libs/*.jar is only the runnable boot jar.
tasks.named<Jar>("jar") {
    enabled = false
}

springBoot {
    buildInfo {
        properties {
            additional.set(
                mapOf(
                    "encoding" to "UTF-8",
                    "java" to "25",
                ),
            )
        }
    }
}

// bootBuildInfo writes a fresh build.time into META-INF/build-info.properties on every build.
// That file rides the test runtime classpath, so its changing timestamp made integrationTest
// never up-to-date and re-ran the whole suite each build. Ignore just that property in runtime
// classpath normalization: the real timestamp stays in the file (actuator /info still reports it)
// while test up-to-date and cache checks treat it as unchanged.
normalization {
    runtimeClasspath {
        properties("META-INF/build-info.properties") {
            ignoreProperty("build.time")
        }
    }
}

tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    buildpacks.set(
        listOf(
            "urn:cnb:builder:paketo-buildpacks/java",
            "docker.io/paketobuildpacks/health-checker",
        ),
    )
    imageName.set("ghcr.io/attrigo/${project.name}:${project.version}")
    environment.set(
        mapOf(
            "BP_HEALTH_CHECKER_ENABLED" to "true",
            "BP_JVM_VERSION" to "25",
        ),
    )
    createdDate.set("now")
    docker {
        publishRegistry {
            url.set("ghcr.io")
            username.set(providers.environmentVariable("GHCR_USERNAME").orElse(""))
            password.set(providers.environmentVariable("GHCR_TOKEN").orElse(""))
        }
    }
}
