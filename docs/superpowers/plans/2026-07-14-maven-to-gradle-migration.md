# Maven → Gradle Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Maven build with a Kotlin-DSL Gradle build at functional parity, so every later build is cached, parallel, and incremental.

**Architecture:** An included build (`build-logic`) holds four chained convention plugins (`java` → `library`/`service` → `domain-service`) that carry all shared configuration; the 7 leaf `build.gradle.kts` files shrink to "pick a tier + list your own deps". A `gradle/libs.versions.toml` catalog is the single source of truth for versions; BOMs are imported as native `platform()`s. Unit vs integration tests are split by name-filtered `Test` tasks over the existing `src/test/java`, with zero file movement.

**Tech Stack:** Gradle 9.3.0 (Kotlin DSL), Java 25 toolchain, Spring Boot 4.0.5 Gradle plugin, Spring Cloud 2025.1.1, Spotless 8.8.0, gradle-pitest 1.19.0 (PIT core 1.23.0), CycloneDX 3.2.4, git-properties 4.0.1, Asciidoctor JVM 4.0.5, JaCoCo 0.8.13.

## Global Constraints

Every task's requirements implicitly include this section. Values are copied verbatim from the source POMs and the design spec.

- **Group / version:** `group=com.attrigo.asapp`, `version=0.5.0-SNAPSHOT` — single source in `gradle.properties`. Gradle auto-assigns the `group` and `version` gradle.properties keys to `project.group`/`project.version` for **every** project, so convention plugins do not restamp them.
- **Java:** toolchain language version **25**; `UTF-8` everywhere; `-parameters` on every `JavaCompile` (the Spring Boot Maven parent enabled this; the Gradle plugin does **not**, so it must be added explicitly).
- **No dependency upgrades.** Every version migrates exactly as-is (Spring Boot stays 4.0.5). Upgrades are 0.6.0 work, out of scope.
- **DSL:** Kotlin DSL (`*.gradle.kts`) only.
- **Image names:** `ghcr.io/attrigo/<service-name>:<version>` with Paketo `java` + `health-checker` buildpacks, `BP_HEALTH_CHECKER_ENABLED=true`, `BP_JVM_VERSION=25`.
- **PIT:** `mutationThreshold` = **100**; domain + application-in-service packages; auth/tasks/users only.
- **Catalog ordering discipline:** within every table (`[versions]`, `[libraries]`, `[bundles]`, `[plugins]`) keep the POM-style comment groups and sort alphabetically **within** each group.
- **File renames:** always `git mv <old> <new>` — never delete and recreate.
- **Coexistence:** Maven and Gradle coexist on the branch during authoring (to diff outputs). Maven is deleted only in the final task, before merge. Commits follow Conventional Commits; the branch is `build/replace-maven-with-gradle`.

**Confirmed decisions from planning (not in the spec):**
- The **Liquibase Maven plugin is dropped, not reproduced.** It had no lifecycle-bound execution (manual CLI goals only); runtime migrations run via the `spring-boot-starter-liquibase` dependency, which migrates unchanged. No Gradle Liquibase plugin is added.
- **Jackson CVE override is scoped to `service-conventions`**, not the root/`java-conventions`. Jackson is only on the classpath of the deployed services; scoping it there keeps `asapp-http-clients`' deliberately-pinned test `jackson-databind:3.0.4` intact (a root-level Jackson platform would raise it to 3.1.1). This preserves both versions exactly as Maven resolved them (§12: migrate as-is).

**A note on "tests" in this plan.** This is a build-system migration; there is no product code to TDD. The verification analog is: **run a Gradle command, assert its observed output** (task graph, test counts, artifacts, reports). Parity targets are the spec §10 figures, already captured as the baseline: **149 test classes** (99 `*Tests`, 46 `*IT`, 4 `*E2EIT`), **5 runnable boot jars**, **5 images**, per-module JaCoCo, PIT green on 3 services, REST-docs HTML on 3 services, zero Spotless churn.

---

## File Structure

**Created:**
- `settings.gradle.kts` — root settings: repos, catalog (auto), `includeBuild("build-logic")`, 7 `include(...)`.
- `build.gradle.kts` — root: `base` plugin, `installGitHooks`, `fullBuild` aggregate.
- `gradle.properties` — group, version, speed flags.
- `gradle/libs.versions.toml` — the version catalog.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` — pinned wrapper.
- `build-logic/settings.gradle.kts` — re-registers the catalog.
- `build-logic/build.gradle.kts` — `kotlin-dsl`; plugin-marker deps; catalog-accessor wiring.
- `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`
- `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`
- `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`
- `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`
- 7 leaf `build.gradle.kts` (2 libs + 5 services).
- `.claude/rules/gradle.md` (via `git mv` from `maven.md`, then rewritten).

**Modified:** `.gitignore`, `.gitattributes`, `git/hooks/pre-commit`, `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `.claude/skills/asapp-release/SKILL.md`, `README.md`, `CLAUDE.md`.

**Deleted (final task only):** all 10 `pom.xml` files; `.claude/rules/maven.md` (superseded by the `git mv`); the obsolete memory file `project_spotless_submodule_formatter_path.md` + its `MEMORY.md` pointer.

**Untouched (must survive):** `asapp_formatter.xml`, `header-license`, `git/hooks/commit-msg`, `.github/cliff.toml`, `docker-compose.yaml`, `tools/jmeter/**`, all `src/**`.

---

## Task 1: Wrapper, settings, and project enumeration

**Files:**
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`

**Interfaces:**
- Produces: the project paths `:libs:asapp-commons-url`, `:libs:asapp-http-clients`, `:services:asapp-authentication-service`, `:services:asapp-config-service`, `:services:asapp-discovery-service`, `:services:asapp-tasks-service`, `:services:asapp-users-service`; the `libs` catalog namespace (auto-registered from `gradle/libs.versions.toml`, created in Task 2); root `group`/`version`.

- [ ] **Step 1: (Optional) harvest a cross-check with `gradle init`**

In a throwaway copy of the repo (not this working tree), run `gradle init --type pom` and keep the generated dependency list only as a diff target against Tasks 3–5. It reproduces none of the custom plugin wiring — do not copy its output in. Skip if no Gradle is installed; the leaf dep lists in this plan were transcribed directly from the POMs.

- [ ] **Step 2: Generate the pinned wrapper**

With any Gradle 9.x available (`scoop install gradle` / `choco install gradle` / SDKMAN), run from the repo root:

```bash
gradle wrapper --gradle-version 9.3.0
```

Then confirm `gradle/wrapper/gradle-wrapper.properties` pins the distribution exactly:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Gradle 9.1.0+ supports running the daemon on JDK 25; 9.3.0 is the current stable. If a newer 9.x is out at execution time, pin that instead and record it.

- [ ] **Step 3: Write `gradle.properties`**

```properties
group=com.attrigo.asapp
version=0.5.0-SNAPSHOT

org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

- [ ] **Step 4: Write `settings.gradle.kts`**

`build-logic` is not created until Task 3, so the `includeBuild` line is added there — leave it out now.

```kotlin
rootProject.name = "asapp"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build-logic is wired in Task 3 via: includeBuild("build-logic")

include(":libs:asapp-commons-url")
include(":libs:asapp-http-clients")
include(":services:asapp-authentication-service")
include(":services:asapp-config-service")
include(":services:asapp-discovery-service")
include(":services:asapp-tasks-service")
include(":services:asapp-users-service")
```

- [ ] **Step 5: Write a minimal root `build.gradle.kts`**

`installGitHooks` and `fullBuild` are added in Task 6; keep root minimal for now.

```kotlin
plugins {
    base
}
```

- [ ] **Step 6: Verify the project tree resolves**

Run: `./gradlew projects`
Expected: BUILD SUCCESSFUL; the tree lists `Root project 'asapp'` with children `libs` → `asapp-commons-url`, `asapp-http-clients` and `services` → the five services. (The `libs`/`services` nodes are bare path segments with no build script — expected.)

- [ ] **Step 7: Commit**

```bash
git add gradlew gradlew.bat gradle/wrapper settings.gradle.kts build.gradle.kts gradle.properties
git commit -m "build(gradle): add wrapper, settings, and project enumeration"
```

---

## Task 2: Version catalog

**Files:**
- Create: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: every catalog accessor consumed by Tasks 3–6. Key aliases → accessors: `spring-boot-bom`→`libs.spring.boot.bom`, `spring-cloud-bom`→`libs.spring.cloud.bom`, `jackson-bom`→`libs.jackson.bom`, `mapstruct`/`mapstruct-processor`, `springdoc-openapi`→`libs.springdoc.openapi`, `nimbus-jose-jwt`→`libs.nimbus.jose.jwt`, `resilience4j`, `bootui`, `bcprov`, `bcpkix`, `guava`, `commons-io`→`libs.commons.io`, `commons-beanutils`→`libs.commons.beanutils`, `rhino`, `archunit-junit5`→`libs.archunit.junit5`, `json-unit-assertj`→`libs.json.unit.assertj`, `mockserver-client`/`mockserver-netty`, `spring-restdocs-mockmvc`/`spring-restdocs-asciidoctor`, `testcontainers-*`, `jackson-databind`; bundle `libs.bundles.testcontainers`; versions `libs.versions.pitest`, `libs.versions.pitestJunit5`, `libs.versions.jacoco`, `libs.versions.springBoot`, `libs.versions.spotless`, `libs.versions.cyclonedx`, `libs.versions.gitProperties`, `libs.versions.asciidoctor`, `libs.versions.pitestPlugin`.

- [ ] **Step 1: Write `gradle/libs.versions.toml`**

```toml
[versions]
# BOM
springBoot   = "4.0.5"
springCloud  = "2025.1.1"
# Compile · Org
mapstruct        = "1.6.3"
resilience4j     = "2.4.0"
springdocOpenapi = "3.0.2"
# Compile · Other
bootui        = "1.4.0"
nimbusJoseJwt = "9.37.4"
# Test · Spring
springRestdocs = "4.0.0"
# Test · Org
archunit       = "1.4.1"
mockserver     = "5.15.0"
testcontainers = "2.0.4"
# Test · Other
jacksonDatabind     = "3.0.4"
jsonUnit            = "5.1.1"
testcontainersRedis = "2.2.4"
# CVE overrides (bump above what the BOMs pin)
bouncycastle     = "1.83"
commonsBeanutils = "1.11.0"
commonsIo        = "2.21.0"
guava            = "33.4.8-jre"
jackson          = "3.1.1"
rhino            = "1.9.1"
# Build tooling
jacoco       = "0.8.13"
pitest       = "1.23.0"
pitestJunit5 = "1.2.3"
# Plugins
asciidoctor   = "4.0.5"
cyclonedx     = "3.2.4"
gitProperties = "4.0.1"
pitestPlugin  = "1.19.0"
spotless      = "8.8.0"

[libraries]
# BOM
spring-boot-bom  = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "springBoot" }
spring-cloud-bom = { module = "org.springframework.cloud:spring-cloud-dependencies", version.ref = "springCloud" }
jackson-bom      = { module = "tools.jackson:jackson-bom", version.ref = "jackson" }
# Compile · Org
mapstruct           = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }
springdoc-openapi   = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdocOpenapi" }
# Compile · Other
bootui         = { module = "com.julien-dubois.bootui:bootui-spring-boot-starter", version.ref = "bootui" }
nimbus-jose-jwt = { module = "com.nimbusds:nimbus-jose-jwt", version.ref = "nimbusJoseJwt" }
resilience4j   = { module = "io.github.resilience4j:resilience4j-spring-boot4", version.ref = "resilience4j" }
# CVE · direct
bcpkix = { module = "org.bouncycastle:bcpkix-jdk18on", version.ref = "bouncycastle" }
bcprov = { module = "org.bouncycastle:bcprov-jdk18on", version.ref = "bouncycastle" }
# CVE · transitive
commons-beanutils = { module = "commons-beanutils:commons-beanutils", version.ref = "commonsBeanutils" }
commons-io        = { module = "commons-io:commons-io", version.ref = "commonsIo" }
guava             = { module = "com.google.guava:guava", version.ref = "guava" }
rhino             = { module = "org.mozilla:rhino", version.ref = "rhino" }
# Test · Spring
spring-restdocs-asciidoctor = { module = "org.springframework.restdocs:spring-restdocs-asciidoctor", version.ref = "springRestdocs" }
spring-restdocs-mockmvc     = { module = "org.springframework.restdocs:spring-restdocs-mockmvc", version.ref = "springRestdocs" }
# Test · Org
archunit-junit5           = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }
mockserver-client         = { module = "org.mock-server:mockserver-client-java", version.ref = "mockserver" }
mockserver-netty          = { module = "org.mock-server:mockserver-netty", version.ref = "mockserver" }
testcontainers-junit      = { module = "org.testcontainers:testcontainers-junit-jupiter", version.ref = "testcontainers" }
testcontainers-mockserver = { module = "org.testcontainers:testcontainers-mockserver", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:testcontainers-postgresql", version.ref = "testcontainers" }
# Test · Other
jackson-databind  = { module = "tools.jackson.core:jackson-databind", version.ref = "jacksonDatabind" }
json-unit-assertj = { module = "net.javacrumbs.json-unit:json-unit-assertj", version.ref = "jsonUnit" }
testcontainers-redis = { module = "com.redis:testcontainers-redis", version.ref = "testcontainersRedis" }

[bundles]
# The common Testcontainers trio for auth/tasks (users adds mockserver separately)
testcontainers = ["testcontainers-junit", "testcontainers-postgresql", "testcontainers-redis"]

[plugins]
asciidoctor   = { id = "org.asciidoctor.jvm.convert", version.ref = "asciidoctor" }
cyclonedx     = { id = "org.cyclonedx.bom", version.ref = "cyclonedx" }
git-properties = { id = "com.gorylenko.gradle-git-properties", version.ref = "gitProperties" }
pitest        = { id = "info.solidsoft.pitest", version.ref = "pitestPlugin" }
spring-boot   = { id = "org.springframework.boot", version.ref = "springBoot" }
spotless      = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

- [ ] **Step 2: Verify the catalog parses**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL. (A malformed TOML fails every invocation with a catalog-parse error, so a clean `help` proves it.)

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build(gradle): add version catalog"
```

---

## Task 3: build-logic, `java`/`library` conventions, both libs green

**Files:**
- Create: `build-logic/settings.gradle.kts`, `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`
- Create: `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`
- Create: `libs/asapp-commons-url/build.gradle.kts`, `libs/asapp-http-clients/build.gradle.kts`
- Modify: `settings.gradle.kts` (add `includeBuild("build-logic")`)

**Interfaces:**
- Consumes: catalog accessors from Task 2; project paths from Task 1.
- Produces: convention plugin IDs `asapp.java-conventions`, `asapp.library-conventions`; per-module tasks `test`, `integrationTest`, `jacocoTestReport`, `spotlessCheck`/`spotlessApply`, `javadocJar`, `sourcesJar`; the `libs`-catalog accessor available **inside** precompiled convention scripts (via the codeSource wiring).

- [ ] **Step 1: Write `build-logic/settings.gradle.kts`**

Re-registers the catalog so `build-logic` (and its convention plugins) can read `libs`.

```kotlin
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

- [ ] **Step 2: Write `build-logic/build.gradle.kts`**

Plugin-marker deps put each third-party plugin on the convention scripts' classpath so they can be applied by `id(...)` without a version. The final `files(...)` line is the well-known workaround that exposes the `libs` catalog accessor inside precompiled `.gradle.kts` convention plugins.

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:${libs.versions.springBoot.get()}")
    implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:${libs.versions.spotless.get()}")
    implementation("info.solidsoft.pitest:info.solidsoft.pitest.gradle.plugin:${libs.versions.pitestPlugin.get()}")
    implementation("org.cyclonedx.bom:org.cyclonedx.bom.gradle.plugin:${libs.versions.cyclonedx.get()}")
    implementation("com.gorylenko.gradle-git-properties:com.gorylenko.gradle-git-properties.gradle.plugin:${libs.versions.gitProperties.get()}")
    implementation("org.asciidoctor.jvm.convert:org.asciidoctor.jvm.convert.gradle.plugin:${libs.versions.asciidoctor.get()}")

    // Expose the version catalog to the precompiled convention plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
```

- [ ] **Step 3: Write `asapp.java-conventions.gradle.kts`**

```kotlin
import com.diffplug.spotless.LineEnding

plugins {
    java
    jacoco
    id("com.diffplug.spotless")
}

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
    filter { includeTestsMatching("*Tests") }
}

// Integration tests: *IT + *E2EIT (Maven failsafe equivalent) over the SAME src/test/java.
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests (*IT, *E2EIT)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("*IT") }   // matches *IT and *E2EIT
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
```

The Spotless plugin auto-wires `spotlessCheck` into `check` — no manual wiring needed. `rootProject.file(...)` resolves `asapp_formatter.xml`/`header-license` from the build root regardless of which module runs, so Spotless works from any module.

- [ ] **Step 4: Write `asapp.library-conventions.gradle.kts`**

```kotlin
plugins {
    id("asapp.java-conventions")
}

// Javadoc & sources jars — off the fast `build` path; invoked explicitly or via fullBuild.
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allJava)
}
```

- [ ] **Step 5: Wire the included build in the root `settings.gradle.kts`**

Replace the placeholder comment line with the actual include (must appear before the `include(...)` calls):

```kotlin
// build-logic is wired in Task 3 via: includeBuild("build-logic")
```

becomes

```kotlin
includeBuild("build-logic")
```

- [ ] **Step 6: Write `libs/asapp-commons-url/build.gradle.kts`**

The POM declares no dependencies.

```kotlin
plugins {
    id("asapp.library-conventions")
}
```

- [ ] **Step 7: Write `libs/asapp-http-clients/build.gradle.kts`**

Maven `<optional>true</optional>` on `spring-web` maps to `implementation` (non-api: not leaked to consumers' compile classpath; harmless runtime transitivity since every consumer already bundles spring-web). The test `jackson-databind` keeps its module-local `3.0.4` pin — no Jackson platform reaches libs, so it resolves cleanly.

```kotlin
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
```

- [ ] **Step 8: Verify both libs build, format-clean, and produce jars**

Run: `./gradlew :libs:asapp-commons-url:build :libs:asapp-http-clients:build`
Expected: BUILD SUCCESSFUL; `asapp-http-clients` unit tests (`*Tests`) run and pass; `spotlessCheck` passes with **no** reformatting.

Run: `./gradlew :libs:asapp-http-clients:javadocJar :libs:asapp-http-clients:sourcesJar`
Expected: BUILD SUCCESSFUL; `libs/asapp-http-clients/build/libs/` contains a `-javadoc.jar` and a `-sources.jar`.

- [ ] **Step 9: Commit**

```bash
git add build-logic settings.gradle.kts libs/asapp-commons-url/build.gradle.kts libs/asapp-http-clients/build.gradle.kts
git commit -m "build(gradle): add build-logic with java and library conventions"
```

---

## Task 4: `service-conventions`; config + discovery green

**Files:**
- Create: `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`
- Create: `services/asapp-config-service/build.gradle.kts`, `services/asapp-discovery-service/build.gradle.kts`

**Interfaces:**
- Consumes: `asapp.java-conventions`; catalog BOMs + CVE libs; `libs.versions.*`.
- Produces: convention plugin ID `asapp.service-conventions`; per-service `bootJar`, `bootRun` (dev profile), `bootBuildImage` (ghcr name), `cyclonedxBom`, git-properties generation, `buildInfo`; the plain `jar` disabled so `build/libs/*.jar` is exactly the one boot jar.

- [ ] **Step 1: Write `asapp.service-conventions.gradle.kts`**

```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("asapp.java-conventions")
    id("org.springframework.boot")
    id("org.cyclonedx.bom")
    id("com.gorylenko.gradle-git-properties")
}

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
```

Applying `org.cyclonedx.bom` + the Spring Boot plugin auto-embeds the SBOM into the boot jar (Spring Boot ≥3.3 integration wires `bootJar` to depend on `cyclonedxBom`). No extra config needed. `config`/`discovery` get no PIT (only `domain-service-conventions` applies it) — equivalent to Maven's `<skip>true</skip>`.

- [ ] **Step 2: Write `services/asapp-config-service/build.gradle.kts`**

Config Server serves config from the classpath under the `native` profile, so its `bootRun` overrides the default `dev` to `native,dev` (matches the Maven `spring-boot-maven-plugin` `<profiles>` block).

```kotlin
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("asapp.service-conventions")
}

dependencies {
    // Compile
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-config-server")

    // Runtime
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.json.unit.assertj)
}

tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "native,dev")
}
```

- [ ] **Step 3: Write `services/asapp-discovery-service/build.gradle.kts`**

```kotlin
plugins {
    id("asapp.service-conventions")
}

dependencies {
    // Compile
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")

    // Runtime
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.json.unit.assertj)
}
```

- [ ] **Step 4: Verify both infra services build**

Run: `./gradlew :services:asapp-config-service:build :services:asapp-discovery-service:build`
Expected: BUILD SUCCESSFUL; unit + integration tests run and pass; `spotlessCheck` clean. (These two services have no Testcontainers dependency, so their ITs need no Docker daemon.)

Run: `./gradlew :services:asapp-config-service:bootJar`
Expected: exactly one jar in `services/asapp-config-service/build/libs/` (no `-plain.jar`).

- [ ] **Step 5: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts services/asapp-config-service/build.gradle.kts services/asapp-discovery-service/build.gradle.kts
git commit -m "build(gradle): add service conventions and infra services"
```

---

## Task 5: `domain-service-conventions`; auth/tasks/users green

**Files:**
- Create: `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`
- Create: `services/asapp-authentication-service/build.gradle.kts`, `services/asapp-tasks-service/build.gradle.kts`, `services/asapp-users-service/build.gradle.kts`

**Interfaces:**
- Consumes: `asapp.service-conventions`; `libs.mapstruct`/`libs.mapstruct.processor`; `libs.spring.restdocs.asciidoctor`; PIT versions; the `libs.bundles.testcontainers` bundle.
- Produces: convention plugin ID `asapp.domain-service-conventions`; per-service `pitest`, `asciidoctor` (REST docs HTML), `javadocJar`, `sourcesJar`; MapStruct processing wired for all three.

- [ ] **Step 1: Write `asapp.domain-service-conventions.gradle.kts`**

MapStruct (api + processor) is a tier-wide trait, hoisted here so the three leaves don't repeat it. PIT uses a wildcard each module resolves against its own classpath (improving on Maven's hardcoded per-service package). `pitest-junit5-plugin` is supplied via `junit5PluginVersion` — leaves no longer declare it as a test dep.

```kotlin
import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
    id("org.asciidoctor.jvm.convert")
}

dependencies {
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // REST Docs asciidoctor extension (operation:: macros).
    "asciidoctor"(libs.spring.restdocs.asciidoctor)
}

// Javadoc & sources jars — off the fast `build` path; invoked explicitly or via fullBuild.
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allJava)
}

pitest {
    pitestVersion.set(libs.versions.pitest.get())
    junit5PluginVersion.set(libs.versions.pitestJunit5.get())
    mutationThreshold.set(100)
    targetClasses.set(
        setOf(
            "com.attrigo.asapp.*.domain.*",
            "com.attrigo.asapp.*.application.*.in.service.*",
        ),
    )
    targetTests.set(
        setOf(
            "com.attrigo.asapp.*.domain.*",
            "com.attrigo.asapp.*.application.*.in.service.*",
        ),
    )
}

// REST Docs snippets are produced during integrationTest; asciidoctor consumes them.
tasks.named<AsciidoctorTask>("asciidoctor") {
    dependsOn(tasks.named("integrationTest"))
    setSourceDir(file("src/docs/asciidoc"))
    baseDirFollowsSourceDir()
    options(mapOf("doctype" to "book"))
    attributes(
        mapOf(
            "snippets" to layout.buildDirectory.dir("generated-snippets").get().asFile,
        ),
    )
    outputOptions {
        backends("html5")
    }
}
```

- [ ] **Step 2: Write `services/asapp-authentication-service/build.gradle.kts`**

`bcprov` is a direct compile dep (Spring Security `PasswordEncoderFactories`); `postgresql` is compile-scope (used by `JdbcConversionsConfiguration`). MapStruct, devtools, and pitest-junit5 come from the conventions.

```kotlin
plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation(libs.bcprov)
    implementation("org.postgresql:postgresql")
    implementation(libs.springdoc.openapi)
    implementation(libs.nimbus.jose.jwt)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    runtimeOnly(libs.bootui)

    // Test
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.json.unit.assertj)
}
```

- [ ] **Step 3: Write `services/asapp-tasks-service/build.gradle.kts`**

`postgresql` is runtime here (not compile as in auth).

```kotlin
plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation(libs.springdoc.openapi)
    implementation(libs.nimbus.jose.jwt)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.bootui)

    // Test
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.json.unit.assertj)
}
```

- [ ] **Step 4: Write `services/asapp-users-service/build.gradle.kts`**

Users adds `asapp-http-clients`, aspectj, restclient, loadbalancer, resilience4j, and the MockServer trio.

```kotlin
plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    implementation(project(":libs:asapp-commons-url"))
    implementation(project(":libs:asapp-http-clients"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation(libs.springdoc.openapi)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.resilience4j)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Runtime
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.bootui)

    // Test
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.mockserver)
    testImplementation(libs.mockserver.client)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.json.unit.assertj)
}
```

- [ ] **Step 5: Verify the three domain services build (Docker required)**

Their integration tests use Testcontainers, so a Docker daemon must be running.

Run: `./gradlew :services:asapp-authentication-service:build :services:asapp-tasks-service:build :services:asapp-users-service:build`
Expected: BUILD SUCCESSFUL; unit + integration tests pass; `spotlessCheck` clean; MapStruct-generated mappers compile.

- [ ] **Step 6: Verify REST-docs HTML and mutation coverage on one service**

Run: `./gradlew :services:asapp-authentication-service:asciidoctor`
Expected: `services/asapp-authentication-service/build/docs/asciidoc/api-guide.html` exists (generated from the IT-produced snippets in `build/generated-snippets`). If snippets are not found, set `@AutoConfigureRestDocs(outputDir = "build/generated-snippets")` on the `*ApiDocumentationIT` tests — but Spring Boot's default output dir under Gradle is already `build/generated-snippets`, so no change is expected.

Run: `./gradlew :services:asapp-authentication-service:pitest`
Expected: BUILD SUCCESSFUL at threshold 100; report at `services/asapp-authentication-service/build/reports/pitest/index.html`.

- [ ] **Step 7: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts services/asapp-authentication-service/build.gradle.kts services/asapp-tasks-service/build.gradle.kts services/asapp-users-service/build.gradle.kts
git commit -m "build(gradle): add domain-service conventions and domain services"
```

---

## Task 6: Root aggregate tasks — whole-repo green

**Files:**
- Modify: `build.gradle.kts` (root)

**Interfaces:**
- Consumes: every module's `build`, `jacocoTestReport`, `javadocJar`, `sourcesJar`.
- Produces: root tasks `installGitHooks` (wired into `build`), `fullBuild`.

- [ ] **Step 1: Rewrite the root `build.gradle.kts`**

`installGitHooks` points git at the version-controlled hooks, idempotently (configuration-cache-safe: it reads the current value via a provider and only runs when it differs). `fullBuild` aggregates the `-Pfull` outcomes via explicit module paths (robust; the `:libs`/`:services` container projects have no build tasks, so they are excluded).

```kotlin
plugins {
    base
}

val leafModules = listOf(
    ":libs:asapp-commons-url",
    ":libs:asapp-http-clients",
    ":services:asapp-authentication-service",
    ":services:asapp-config-service",
    ":services:asapp-discovery-service",
    ":services:asapp-tasks-service",
    ":services:asapp-users-service",
)

// Modules that publish javadoc & sources jars (library + domain-service tiers).
val jarTierModules = listOf(
    ":libs:asapp-commons-url",
    ":libs:asapp-http-clients",
    ":services:asapp-authentication-service",
    ":services:asapp-tasks-service",
    ":services:asapp-users-service",
)

val installGitHooks by tasks.registering(Exec::class) {
    description = "Points git at the version-controlled hooks in git/hooks (idempotent)."
    group = "build setup"
    val currentHooksPath = providers.exec {
        commandLine("git", "config", "--get", "core.hooksPath")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }
    onlyIf { currentHooksPath.getOrElse("") != "git/hooks" }
    commandLine("git", "config", "core.hooksPath", "git/hooks")
}

tasks.named("build") {
    dependsOn(installGitHooks)
}

tasks.register("fullBuild") {
    group = "build"
    description = "Full verification (the Maven -Pfull equivalent): build + coverage + javadoc/sources jars."
    dependsOn(leafModules.map { "$it:build" })
    dependsOn(leafModules.map { "$it:jacocoTestReport" })
    dependsOn(jarTierModules.flatMap { listOf("$it:javadocJar", "$it:sourcesJar") })
}
```

- [ ] **Step 2: Verify the whole repo builds green (Docker required)**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL across all 7 modules; unit + integration tests pass; `spotlessCheck` clean.

- [ ] **Step 3: Verify git hooks were pointed at `git/hooks`**

Run: `git config --get core.hooksPath`
Expected: prints `git/hooks`.

- [ ] **Step 4: Verify the full build aggregate**

Run: `./gradlew fullBuild`
Expected: BUILD SUCCESSFUL; javadoc + sources jars present in the 5 jar-tier modules' `build/libs/`; per-module JaCoCo HTML at `build/reports/jacoco/test/html/index.html`.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts
git commit -m "build(gradle): add installGitHooks and fullBuild aggregate"
```

---

## Task 7: Git hook uses Gradle

**Files:**
- Modify: `git/hooks/pre-commit`

**Interfaces:**
- Consumes: the `spotlessCheck` task.

- [ ] **Step 1: Swap the Spotless invocation in `git/hooks/pre-commit`**

Change the single line

```bash
mvn spotless:check
```

to

```bash
./gradlew --quiet spotlessCheck
```

Everything else in the hook (the LF/CRLF check, colors, `set -e`) stays byte-for-byte. `git/hooks/commit-msg` is untouched.

- [ ] **Step 2: Verify the hook fails on a formatting slip and passes when clean**

Run (temporarily break formatting in a tracked file, then):
```bash
git add -A && git commit -m "test: hook check" --dry-run  # or attempt a real commit
```
Expected: the hook runs `./gradlew --quiet spotlessCheck` and **fails** on the unformatted file. Then `./gradlew spotlessApply`, re-stage, and the commit succeeds. Revert the temporary change.

- [ ] **Step 3: Commit**

```bash
git add git/hooks/pre-commit
git commit -m "build(gradle): point pre-commit hook at gradlew spotlessCheck"
```

---

## Task 8: CI workflow

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Replace the JDK-cache + build steps**

Drop `cache: maven` from `setup-java`, add `gradle/actions/setup-gradle@v4` (handles dependency + build caching across runs), and swap the build command. The final two steps become:

```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build and test the project
        run: ./gradlew build
```

- [ ] **Step 2: Verify the YAML is well-formed**

Run: `./gradlew help` is unrelated; instead validate the file parses, e.g. with any YAML linter available, or visually confirm indentation. Expected: valid YAML, no `mvn` or `cache: maven` remaining.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: build with Gradle"
```

---

## Task 9: Release workflow

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: `build-and-test` job — build with `fullBuild`, upload Gradle jar path**

Drop `cache: maven`, add setup-gradle, swap the build and the artifact path:

```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build and test the project
        run: ./gradlew fullBuild

      - name: Upload service JARs
        uses: actions/upload-artifact@v6
        with:
          name: service-jars
          path: services/*/build/libs/*.jar
          retention-days: 1
```

- [ ] **Step 2: `publish-docker` job — build + push in one Gradle step**

`bootBuildImage --publishImage` builds and pushes; the plain-jar is disabled and `bootBuildImage` depends only on `bootJar` (not `test`), so no `-DskipTests` equivalent is needed. Credentials flow through the `publishRegistry` env vars configured in `service-conventions`. Remove the `docker/login-action` step, the `Extract version from tag` step, and the manual `docker push` loop; replace the build step with:

```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build and push Docker images
        env:
          GHCR_USERNAME: ${{ github.actor }}
          GHCR_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./gradlew bootBuildImage --publishImage
```

Remove the now-unused `outputs.version` from the `publish-docker` job header (nothing consumes it; `create-release` uses `github.ref_name`).

- [ ] **Step 3: `create-release` job — Gradle jar path in the glob**

The uploaded artifact strips the common `services/` root, so the download lands at `release-artifacts/<svc>/build/libs/<jar>`. Change the final glob:

```yaml
          gh release create "${{ github.ref_name }}" \
            --title "${{ github.ref_name }}" \
            --notes-file CHANGELOG_RELEASE.md \
            release-artifacts/*/build/libs/*.jar
```

- [ ] **Step 4: Verify no Maven references remain**

Confirm (visually or by search) `release.yml` contains no `mvn`, `target/`, `cache: maven`, or push-loop remnants.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: release and publish images with Gradle"
```

---

## Task 10: `asapp-release` skill — mechanism swaps

**Files:**
- Modify: `.claude/skills/asapp-release/SKILL.md`

Only the build-tool mechanism changes; every other step (Liquibase XML tagging, OpenAPI/docker-compose edits, git tag, push gate) is build-tool-agnostic and stays.

- [ ] **Step 1: Step 2 (Detect versions) — read `gradle.properties`**

Replace "Read the root `pom.xml` to extract the current version" with:

> Read `gradle.properties` to extract the current version from the `version=` line (e.g. `0.5.0-SNAPSHOT`).

- [ ] **Step 2: Step 5 (Remove SNAPSHOT) — edit `gradle.properties`**

Replace the `mvn versions:set -DremoveSnapshot=true ...` block and its "Confirm the root `pom.xml` now reads ..." line with:

> Edit `gradle.properties`: set the `version=` line to the release version with `-SNAPSHOT` stripped (e.g. `version=0.5.0`). Confirm the file now reads the release version.

The "Update OpenAPI version" and "Update docker-compose.yml" subsections stay unchanged.

- [ ] **Step 3: Step 7 (Build and verify) — `./gradlew test`**

Replace `mvn clean test` with `./gradlew test` (unit-only pre-flight; no Docker needed). Keep the surrounding "local pre-flight only" prose.

- [ ] **Step 4: Step 8 (Commit release and tag) — read version from `gradle.properties`**

Replace `RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)` with:

```bash
RELEASE_VERSION=$(grep '^version=' gradle.properties | cut -d= -f2)
```

- [ ] **Step 5: Step 9 (Bump to next SNAPSHOT) — edit `gradle.properties`**

Replace the `mvn versions:set -DnextSnapshot=true ...` block with:

> Edit `gradle.properties`: set the `version=` line to the next minor SNAPSHOT (e.g. `0.5.0` → `version=0.6.0-SNAPSHOT`). Confirm the file now reads the next SNAPSHOT version.

- [ ] **Step 6: Step 10 (Commit next dev version) — read version from `gradle.properties`**

Replace `NEXT_DEV_VERSION=$(mvn help:evaluate ...)` with:

```bash
NEXT_DEV_VERSION=$(grep '^version=' gradle.properties | cut -d= -f2)
```

- [ ] **Step 7: Guardrails — update the build command name**

In the Guardrails list, change "Never skip `mvn clean test`" to "Never skip `./gradlew test`".

- [ ] **Step 8: Commit**

```bash
git add .claude/skills/asapp-release/SKILL.md
git commit -m "docs(skill): update asapp-release for Gradle"
```

---

## Task 11: Docs & rules ripple

**Files:**
- Modify: `README.md`, `CLAUDE.md`
- Rename + rewrite: `.claude/rules/maven.md` → `.claude/rules/gradle.md`
- Audit: other `.claude/skills/**/SKILL.md` for `mvn` calls

- [ ] **Step 1: `README.md` — prerequisites**

In the Requirements list, replace the `- **Maven**: 3.9.14+` line with:

```markdown
- **Gradle**: not required — use the committed wrapper (`./gradlew`)
```

- [ ] **Step 2: `README.md` — Quick Start**

Installation block: `mvn clean install` → `./gradlew build`. Running block: `mvn spring-boot:build-image` → `./gradlew bootBuildImage`.

- [ ] **Step 3: `README.md` — Code Quality bullet**

Change `- **Formatting**: Spotless Maven Plugin` to `- **Formatting**: Spotless Gradle Plugin`.

- [ ] **Step 4: `README.md` — Development section**

Replace the Build / Test / Code Quality / Generate Documentation blocks with:

````markdown
### Build

```bash
# Build all modules (compile + unit + integration tests + spotlessCheck)
./gradlew build

# Build without tests
./gradlew build -x test -x integrationTest
```

### Test

```bash
# Run all tests
./gradlew test integrationTest

# Run mutation testing (auth/tasks/users only)
./gradlew pitest
```

### Code Quality

```bash
# Install git hooks (also runs automatically on ./gradlew build)
./gradlew installGitHooks

# Apply formatting
./gradlew spotlessApply
```

### Generate Documentation

```bash
# Full verification with all reports (coverage, javadoc, sources, REST docs)
./gradlew fullBuild
```
````

- [ ] **Step 5: `README.md` — Documentation report paths**

Replace the "Generated per service under `target/` after `mvn clean verify -Pfull`" sentence and its table with:

```markdown
Generated per module under `build/` after `./gradlew fullBuild`:

| Artifact        | Location                                       |
|-----------------|------------------------------------------------|
| REST API docs   | `build/docs/asciidoc/api-guide.html`           |
| Test coverage   | `build/reports/jacoco/test/html/index.html`    |
| Mutation report | `build/reports/pitest/index.html`              |
| Javadoc         | `build/docs/javadoc/index.html`                |
```

- [ ] **Step 6: `README.md` — Contributing + Git Hooks + CI/CD prose**

- Contributing → Formatting: `mvn spotless:apply` → `./gradlew spotlessApply`.
- Git Hooks: "Automatically installed on `mvn install`" → "Automatically installed on `./gradlew build`".
- CI Pipeline Steps: replace "Maven dependency caching" + "Build and test (`mvn verify -Pfull`)" with "Gradle dependency + build caching (`gradle/actions/setup-gradle`)" + "Build and test (`./gradlew build`)".
- Continuous Delivery "Release Generation" numbered list: step 2 "Removes `-SNAPSHOT` suffix from all POM versions" → "Removes the `-SNAPSHOT` suffix in `gradle.properties`"; step 4 "Builds and verifies the project (`mvn clean install`)" → "Builds and verifies the project (`./gradlew test`)".

- [ ] **Step 7: `CLAUDE.md` — Build/Run/Testing blocks**

Replace the three fenced sections with:

```markdown
## Build
- Build: `./gradlew build`
- Full build (coverage + javadoc + sources + style check): `./gradlew fullBuild`
- Docker images: `./gradlew bootBuildImage`
- Format (Eclipse config: `asapp_formatter.xml`): `./gradlew spotlessApply`

## Run
- Service: `./gradlew :services:<name>:bootRun`
- Full stack: `docker-compose up -d` (services, PostgreSQL ×3, Redis, Prometheus, Grafana)

## Testing
- Test: `./gradlew build` (or `./gradlew test integrationTest`)
- Mutation testing: `./gradlew pitest`
```

- [ ] **Step 8: Rename and rewrite the build rule**

```bash
git mv .claude/rules/maven.md .claude/rules/gradle.md
```

Then replace the file contents with:

```markdown
# Gradle Build Conventions

## Version catalog — `gradle/libs.versions.toml`

Single source of truth for versions. Within every table (`[versions]`, `[libraries]`, `[bundles]`, `[plugins]`), keep the POM-style comment groups and sort alphabetically **within** each group.

## Convention plugins — `build-logic/src/main/kotlin/`

Four tiers, chained `java` → `library` / `service` → `domain-service`:

- `asapp.java-conventions` — all modules: Java 25 toolchain, UTF-8, `-parameters`, Spring Boot BOM `platform()`, Spotless, JaCoCo, `test` (`*Tests`) + `integrationTest` (`*IT`/`*E2EIT`) tasks.
- `asapp.library-conventions` — the two libs: + javadoc/sources jars.
- `asapp.service-conventions` — all five services: + Spring Boot app, Spring Cloud BOM, Jackson + transitive CVE overrides, SBOM, git properties, `bootBuildImage`, `bootRun` (dev profile), plain jar disabled.
- `asapp.domain-service-conventions` — auth/tasks/users: + MapStruct, PIT (threshold 100), REST-docs asciidoctor, javadoc/sources jars.

A leaf `build.gradle.kts` = pick a tier + list its own dependencies (version-less, resolved from the catalog or the imported BOMs).
```

- [ ] **Step 9: Audit other skills for `mvn` calls**

Run: search the skills tree for Maven invocations.
```bash
grep -rn "mvn " .claude/skills --include=SKILL.md
```
Expected: after Task 10, no functional `mvn` verification calls remain. If any skill (e.g. close-task) runs a build/test via `mvn`, update it to the `./gradlew` equivalent (`mvn clean verify`/`install` → `./gradlew build`; `mvn clean test` → `./gradlew test`). If none found, note it and move on.

- [ ] **Step 10: Verify docs contain no stale Maven build commands**

Run:
```bash
grep -rn "mvn \|target/\|-Pfull\|-Pci" README.md CLAUDE.md .claude/rules/gradle.md
```
Expected: no matches (JMeter's `./run-*.sh` and unrelated prose are fine).

- [ ] **Step 11: Commit**

```bash
git add README.md CLAUDE.md .claude/rules/gradle.md .claude/skills
git commit -m "docs: migrate build documentation to Gradle"
```

---

## Task 12: Parity verification, delete Maven, close out

**Files:**
- Modify: `.gitignore`, `.gitattributes`
- Delete: all 10 `pom.xml`; `project_spotless_submodule_formatter_path.md` memory file + its `MEMORY.md` pointer

**Interfaces:**
- Consumes: the entire Gradle build from Tasks 1–11.

- [ ] **Step 1: Prove test-count parity**

Run: `./gradlew clean test integrationTest`
Expected: BUILD SUCCESSFUL. Confirm the aggregate results across modules cover **149 test classes** — 99 `*Tests` under `test`, 46 `*IT` + 4 `*E2EIT` under `integrationTest` (per-module HTML reports under `build/reports/tests/test` and `build/reports/tests/integrationTest`). No class is silently skipped versus the spec §10 baseline.

- [ ] **Step 2: Prove artifact + report parity**

Run: `./gradlew clean fullBuild`
Expected: 5 runnable boot jars at `services/*/build/libs/*.jar` (one each, no `-plain.jar`); per-module JaCoCo on all 7; javadoc + sources jars on the 5 jar-tier modules; REST-docs HTML for the 3 domain services at `build/docs/asciidoc/api-guide.html`; `git.properties` and the SBOM embedded in each service boot jar (`unzip -l` a boot jar to confirm `BOOT-INF/classes/git.properties` and the CycloneDX SBOM entry).

- [ ] **Step 3: Prove image parity (Docker required)**

Run: `./gradlew bootBuildImage`
Expected: 5 images named exactly `ghcr.io/attrigo/<svc>:0.5.0-SNAPSHOT` (`docker images | grep attrigo`), built with the Paketo `java` + `health-checker` buildpacks.

- [ ] **Step 4: Prove mutation parity**

Run: `./gradlew pitest`
Expected: PIT green at threshold 100 on auth, tasks, users only; config/discovery/libs have no `pitest` task.

- [ ] **Step 5: Update `.gitignore`**

Remove the Maven-only lines and add Gradle's local cache dir. Delete:
```
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/
```
Add near the NetBeans/`build/` block:
```
.gradle/
```
Keep the existing `build/` and `!**/src/**/build/` lines.

- [ ] **Step 6: Update `.gitattributes`**

The existing rules already cover the wrapper (`gradlew` → LF via `* text=auto eol=lf`; `gradlew.bat` → CRLF via `*.bat`; `gradle-wrapper.jar` → binary via `*.jar`). Add explicit lines for clarity, after the "Windows batch" block:
```
# Gradle wrapper
gradlew text eol=lf
/gradle/wrapper/gradle-wrapper.jar binary
```

- [ ] **Step 7: Delete all Maven POMs**

```bash
git rm pom.xml libs/pom.xml services/pom.xml \
  libs/asapp-commons-url/pom.xml libs/asapp-http-clients/pom.xml \
  services/asapp-authentication-service/pom.xml \
  services/asapp-config-service/pom.xml \
  services/asapp-discovery-service/pom.xml \
  services/asapp-tasks-service/pom.xml \
  services/asapp-users-service/pom.xml
```

Then confirm none remain: `find . -name pom.xml -not -path '*/target/*'` prints nothing. (The spec said "11" POMs; the repo has 10 — this deletes every `pom.xml`. Keep `asapp_formatter.xml` and `header-license`.)

- [ ] **Step 8: Final green build after deletion**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL with no `pom.xml` present — proves the Gradle build is fully self-contained.

- [ ] **Step 9: Remove the now-obsolete memory entry (pre-authorized during planning)**

Spotless now resolves `asapp_formatter.xml` via `rootProject.file(...)`, so the "run spotless per-submodule from repo root with -pl" note no longer applies.
```bash
rm "C:/Users/ttrigo/.claude/projects/C--dev-repos-ttrigo-asapp/memory/project_spotless_submodule_formatter_path.md"
```
Then remove its pointer line from `MEMORY.md`:
```
- [Spotless from submodule breaks formatter path](project_spotless_submodule_formatter_path.md) — run spotless:apply from repo root with -pl <module>, not from inside the submodule dir
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "build(gradle): remove Maven, finalize Gradle migration"
```

- [ ] **Step 11: Close the task**

Follow the normal per-task close flow (review → resolve → close-task). The `(build) Replace Maven with Gradle` item in `TODO.md` is complete.

---

## Self-Review

**Spec coverage:**
- §2 decisions — Kotlin DSL (all `.kts`), `gradle init` harvest (Task 1 Step 1), name-filtered Test tasks (T3 java-conventions), `platform()` BOMs + catalog (T2, T3, T4), 4 tiers (T3–T5), spotlessCheck→check (auto-wired, T3), `core.hooksPath` (T6), `gradle.properties` version (T1), `bootBuildImage --publishImage` (T9). ✅
- §3 layout + tiers — build-logic included build, 4 convention plugins, bare `libs`/`services` segments, chain — all in T3–T6. ✅
- §4 dependency management — catalog (T2), native platforms (T3/T4), CVE constraints (T4), internal modules via `project(...)` (T3/T5). ✅ (Jackson scoped to service-conventions with rationale.)
- §5 lifecycle — build/spotlessApply/jacocoTestReport/javadocJar+sourcesJar/pitest/fullBuild all mapped (T3–T6); JaCoCo UT+IT merge (T3); PIT wildcard (T5); unit/IT split (T3). ✅
- §6 git hooks — installGitHooks (T6), pre-commit swap (T7), commit-msg untouched. ✅
- §7 version/wrapper/CI/release/skill — T1, T8, T9, T10. ✅
- §8 docs ripple — README, CLAUDE.md, maven.md→gradle.md, skill audit (T11); memory flag → deletion pre-authorized (T12 Step 9). ✅
- §9 execution plan — followed in task order. ✅
- §10 verification — T12 Steps 1–4 assert all parity figures. ✅
- §11 tricky bits — catalog-accessor wiring (T3 Step 2), asciidoctor↔integrationTest (T5), mandatory BOM platform (T3/T4), Spotless 4.35 (T3, verified by clean run T3 Step 8), bootRun dev/native (T4). ✅
- §12 out of scope — ArchUnit guardrail, dep upgrades, JMeter, cross-module coverage — none introduced. ✅

**Placeholder scan:** No `TBD`/`handle edge cases`/"similar to Task N"; every convention plugin and leaf shows full content; every version is a concrete number.

**Type/name consistency:** Convention plugin IDs (`asapp.java-conventions` etc.), task names (`integrationTest`, `jacocoTestReport`, `fullBuild`, `installGitHooks`), and catalog accessors (`libs.spring.boot.bom`, `libs.bundles.testcontainers`, `libs.versions.pitest`) are used identically across tasks. Leaf module paths match `settings.gradle.kts` includes.

**Known risks flagged for the executor:**
- Gradle 9.3.0 / Spring Boot 4.0.5 / JDK 25 runtime compatibility — reconfirm at execution (T1 Step 2).
- Spotless 8.8.0 supporting the Eclipse `4.35` formatter profile — proven by the zero-churn run (T3 Step 8).
- A subproject path segment named `libs` coincides with the default catalog accessor name `libs`; they occupy different namespaces (`project(":libs:...")` string paths vs the `libs` catalog accessor) and do not clash here, but Gradle may emit an advisory warning. If it ever becomes a problem, rename the catalog via `versionCatalogs { create("deps") { ... } }`.
- REST Docs snippet output dir (T5 Step 6) — defaults to `build/generated-snippets` under Gradle; fallback documented if not.
