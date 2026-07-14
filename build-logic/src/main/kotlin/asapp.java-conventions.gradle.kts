import com.diffplug.spotless.LineEnding
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    jacoco
    id("com.diffplug.spotless")
}

// The codeSource.location dependency in build-logic/build.gradle.kts puts the generated
// LibrariesForLibs class on this script's compile classpath, but precompiled script plugins
// still need this explicit bridge to bind the `libs` name — see gradle/gradle#15383.
val libs = the<LibrariesForLibs>()

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Spring Boot BOM as a native platform — the Boot plugin does NOT auto-apply it.
    val bootBom = platform(libs.spring.boot.bom)
    implementation(bootBom)
    annotationProcessor(bootBom)

    // JUnit Platform launcher required by Gradle's useJUnitPlatform(); version managed by the Boot BOM.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// A Gradle platform() only *recommends* versions, so a transitive dep that requests a newer Spring
// Boot wins via highest-version conflict resolution. bootui-spring-boot-starter:1.4.0 pulls
// spring-boot-starter-actuator:4.1.0, which drags micrometer to 1.17.0 against the 4.0.5-era
// prometheus client (1.4.3) → NoSuchMethodError at ApplicationContext load. Maven's
// <dependencyManagement> pins authoritatively; reproduce that by forcing the whole Spring Boot
// module group (all ship in lockstep) back to the catalog version.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.springframework.boot") {
            useVersion(libs.versions.springBoot.get())
            because("Pin Spring Boot to the BOM version; see java-conventions (bootui pulls 4.1.0).")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

// Unit tests: *Tests (Maven surefire equivalent).
tasks.test {
    useJUnitPlatform()
    filter {
        includeTestsMatching("*Tests")
        isFailOnNoMatchingTests = false   // Maven surefire equivalent: no unit-test classes is not a failure
    }
}

// Integration tests: *IT + *E2EIT (Maven failsafe equivalent) over the SAME src/test/java.
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests (*IT, *E2EIT)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("*IT")   // matches *IT and *E2EIT
        isFailOnNoMatchingTests = false   // Maven failsafe equivalent: no IT classes is not a failure
    }
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTest)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Coverage combines unit + integration (matches Maven's merged UT+IT report).
tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTest)
    executionData(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
}

spotless {
    lineEndings = LineEnding.UNIX
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        eclipse("4.35").configFile(rootProject.file("asapp_formatter.xml"))
        importOrder("java|javax", "org", "com", "", "com.attrigo")
        removeUnusedImports()
        licenseHeaderFile(rootProject.file("header-license"), "package ")
    }
}
