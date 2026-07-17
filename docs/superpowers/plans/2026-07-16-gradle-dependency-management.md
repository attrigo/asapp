# Gradle Dependency Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every Gradle module the same dependency graph Maven resolves today — a version catalog, BOM imports via `io.spring.dependency-management`, and convention-plugin/leaf `dependencies {}` blocks — verified purely through dependency resolution, with no Java toolchain/compiler config yet.

**Architecture:** A single root `gradle/libs.versions.toml` feeds every version. Four layered convention plugins in `build-logic` (`asapp.java-conventions` → `asapp.library-conventions` / `asapp.service-conventions` → `asapp.domain-service-conventions`) each apply their prerequisite plugin internally, so every leaf `build.gradle.kts` declares exactly one convention-plugin `id(...)` plus whatever dependencies still vary at that leaf.

**Tech Stack:** Gradle 9.6.1, Kotlin DSL, `io.spring.dependency-management` 1.1.7, Spring Boot 4.0.5 BOM, Spring Cloud 2025.1.1 BOM.

## Global Constraints

- Kotlin DSL (`.gradle.kts`) only — never Groovy DSL.
- Shared build config lives only in `build-logic` precompiled convention plugins, named `asapp.<concern>-conventions`.
- No `pom.xml` edits; Maven (`mvn clean install`) must keep succeeding unchanged throughout.
- No Java toolchain, `release` version, compiler args, or annotation-processor wiring (including `mapstruct-processor`) — that's the next subtask.
- No Maven `<build><plugins>` migration (jacoco, spotless, pitest, asciidoctor, cyclonedx, git-build-hook, git-commit-id, liquibase, spring-boot-maven-plugin) — each is its own later subtask.
- `compileJava`/`compileTestJava` succeeding is **not** required by this plan — only `./gradlew :module:dependencies` resolving cleanly.
- Every version below is copied verbatim from the current Maven POMs: Spring Boot `4.0.5`, Spring Cloud `2025.1.1`, `io.spring.dependency-management` plugin `1.1.7` (latest, verified against plugins.gradle.org), `mapstruct` `1.6.3`, `springdoc-openapi-starter` `3.0.2`, `bootui` `1.4.0`, `nimbus-jose-jwt` `9.37.4`, `resilience4j` `2.4.0`, `spring-restdocs` `4.0.0`, `mockserver` `5.15.0`, `testcontainers` `2.0.4`, `testcontainers-redis` `2.2.4`, `json-unit-assertj` `5.1.1`, `archunit` `1.4.1`, `pitest-junit5-plugin` `1.2.3`, `jackson-databind` `3.0.4`, `jackson-bom` (CVE override) `3.1.1`, `bcpkix-jdk18on`/`bcprov-jdk18on` `1.83`, `rhino` `1.9.1`, `guava` `33.4.8-jre`, `commons-beanutils` `1.11.0`, `commons-io` `2.21.0`.

---

### Task 1: Version catalog

**Files:**
- Create: `gradle/libs.versions.toml`

**Interfaces:**
- Produces: catalog alias strings (e.g. `"mapstruct"`, `"guava"`) consumed via `libs.findLibrary("<alias>").get()` / `libs.findVersion("<alias>").get().requiredVersion` from every later task — both in `build-logic` convention plugins and in the main build's leaf `build.gradle.kts` files. The main build gets this catalog automatically (Gradle auto-detects `gradle/libs.versions.toml`); `build-logic` needs explicit wiring, done in Task 2.

- [ ] **Step 1: Create the version catalog**

```toml
[versions]
spring-boot = "4.0.5"
spring-cloud = "2025.1.1"

mapstruct = "1.6.3"
springdoc-openapi-starter = "3.0.2"
bootui = "1.4.0"
nimbus-jose-jwt = "9.37.4"
resilience4j = "2.4.0"

spring-restdocs = "4.0.0"
mockserver = "5.15.0"
testcontainers = "2.0.4"
testcontainers-redis = "2.2.4"
json-unit-assertj = "5.1.1"
archunit = "1.4.1"
pitest-junit5-plugin = "1.2.3"
jackson-databind = "3.0.4"

jackson-bom = "3.1.1"
bcpkix-jdk18on = "1.83"
bcprov-jdk18on = "1.83"
rhino = "1.9.1"
guava = "33.4.8-jre"
commons-beanutils = "1.11.0"
commons-io = "2.21.0"

[libraries]
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
springdoc-openapi-starter-webmvc-ui = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdoc-openapi-starter" }
bootui-spring-boot-starter = { module = "com.julien-dubois.bootui:bootui-spring-boot-starter", version.ref = "bootui" }
nimbus-jose-jwt = { module = "com.nimbusds:nimbus-jose-jwt", version.ref = "nimbus-jose-jwt" }
resilience4j-spring-boot4 = { module = "io.github.resilience4j:resilience4j-spring-boot4", version.ref = "resilience4j" }
spring-restdocs-mockmvc = { module = "org.springframework.restdocs:spring-restdocs-mockmvc", version.ref = "spring-restdocs" }
mockserver-client-java = { module = "org.mock-server:mockserver-client-java", version.ref = "mockserver" }
mockserver-netty = { module = "org.mock-server:mockserver-netty", version.ref = "mockserver" }
testcontainers-junit-jupiter = { module = "org.testcontainers:testcontainers-junit-jupiter", version.ref = "testcontainers" }
testcontainers-mockserver = { module = "org.testcontainers:testcontainers-mockserver", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:testcontainers-postgresql", version.ref = "testcontainers" }
testcontainers-redis = { module = "com.redis:testcontainers-redis", version.ref = "testcontainers-redis" }
json-unit-assertj = { module = "net.javacrumbs.json-unit:json-unit-assertj", version.ref = "json-unit-assertj" }
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }
pitest-junit5-plugin = { module = "org.pitest:pitest-junit5-plugin", version.ref = "pitest-junit5-plugin" }
jackson-databind = { module = "tools.jackson.core:jackson-databind", version.ref = "jackson-databind" }
bcpkix-jdk18on = { module = "org.bouncycastle:bcpkix-jdk18on", version.ref = "bcpkix-jdk18on" }
bcprov-jdk18on = { module = "org.bouncycastle:bcprov-jdk18on", version.ref = "bcprov-jdk18on" }
rhino = { module = "org.mozilla:rhino", version.ref = "rhino" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
commons-beanutils = { module = "commons-beanutils:commons-beanutils", version.ref = "commons-beanutils" }
commons-io = { module = "commons-io:commons-io", version.ref = "commons-io" }
```

Note: `spring-boot`/`spring-cloud` BOM coordinates and the `jackson-bom` override are consumed as `[versions]` entries only (built into coordinate strings by hand in later tasks, since `io.spring.dependency-management`'s `mavenBom(...)` takes a raw `"group:artifact:version"` string) — they don't need `[libraries]` entries.

- [ ] **Step 2: Verify Gradle accepts the catalog**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (nothing references the catalog yet, so this only proves the TOML is syntactically valid and doesn't break the build).

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build(gradle): add version catalog"
```

---

### Task 2: Wire `build-logic` to the version catalog and the dependency-management plugin

**Files:**
- Modify: `build-logic/settings.gradle.kts`
- Modify: `build-logic/build.gradle.kts`

**Interfaces:**
- Consumes: `gradle/libs.versions.toml` (Task 1).
- Produces: a `libs` version catalog accessible from any `build-logic` precompiled script plugin via `extensions.getByType(VersionCatalogsExtension::class.java).named("libs")`; the `io.spring.dependency-management` Gradle plugin (classpath-available, applicable via `plugins { id("io.spring.dependency-management") }` with no version needed) for Task 3 onward.

- [ ] **Step 1: Share the root catalog with `build-logic`**

Modify `build-logic/settings.gradle.kts` (currently just `rootProject.name = "build-logic"`):

```kotlin
rootProject.name = "build-logic"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

- [ ] **Step 2: Add the dependency-management plugin to `build-logic`'s own classpath**

Modify `build-logic/build.gradle.kts` (currently just the `kotlin-dsl` plugin + repositories):

```kotlin
plugins {
    // Required so .gradle.kts files under src/main/kotlin compile as applicable plugins — not automatic otherwise
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}
```

- [ ] **Step 3: Verify `build-logic` still configures cleanly**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (proves the catalog wiring and the new classpath dependency both resolve; `./gradlew help` at the root always configures the included `build-logic` build first, since `settings.gradle.kts` declares `pluginManagement { includeBuild("build-logic") }`).

- [ ] **Step 4: Commit**

```bash
git add build-logic/settings.gradle.kts build-logic/build.gradle.kts
git commit -m "build(gradle): wire build-logic to the version catalog and dependency-management plugin"
```

---

### Task 3: Populate `asapp.java-conventions`

**Files:**
- Modify: `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`

**Interfaces:**
- Consumes: `libs` version catalog and `io.spring.dependency-management` plugin classpath (Task 2).
- Produces: plugin ID `asapp.java-conventions` — applying it to a project gives it the bare `java` plugin (just enough for `implementation`/`testImplementation` configurations to exist — no toolchain/`release` config) plus the Spring Boot BOM (with the Jackson CVE-override version already applied). Every other convention plugin (Task 4, 5) applies this one internally.

- [ ] **Step 1: Write the convention plugin**

Replace the placeholder comment in `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`:

```kotlin
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get().requiredVersion}") {
            bomProperty("jackson-bom.version", libs.findVersion("jackson-bom").get().requiredVersion)
        }
    }
}
```

- [ ] **Step 2: Verify `build-logic` compiles this plugin cleanly**

Run: `./gradlew help`
Expected: `BUILD SUCCESSFUL` (Gradle compiles every precompiled script plugin in `build-logic` as part of configuring the root build, even before anything applies `asapp.java-conventions` — a Kotlin syntax error, bad catalog alias, or invalid `dependencyManagement` DSL call would fail this step immediately).

- [ ] **Step 3: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts
git commit -m "build(gradle): add java-conventions dependency management"
```

---

### Task 4: Populate `asapp.library-conventions` and wire both libraries

**Files:**
- Modify: `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`
- Modify: `libs/asapp-commons-url/build.gradle.kts`
- Modify: `libs/asapp-http-clients/build.gradle.kts`

**Interfaces:**
- Consumes: plugin ID `asapp.java-conventions` (Task 3); `libs` catalog aliases `"jackson-databind"`.
- Produces: plugin ID `asapp.library-conventions` (adds `java-library` on top of `asapp.java-conventions`, giving `api`/`implementation` configurations). Both lib modules fully resolve after this task — nothing later depends on them changing further in this plan.

- [ ] **Step 1: Write the convention plugin**

Replace the placeholder comment in `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`:

```kotlin
plugins {
    id("asapp.java-conventions")
    `java-library`
}
```

- [ ] **Step 2: Apply it to `asapp-commons-url`**

Replace the placeholder comment in `libs/asapp-commons-url/build.gradle.kts` — this library has zero dependencies today (Maven's `asapp-commons-url/pom.xml` declares none), so this is the entire file:

```kotlin
plugins {
    id("asapp.library-conventions")
}
```

- [ ] **Step 3: Apply it to `asapp-http-clients` and add its own dependencies**

Replace the placeholder comment in `libs/asapp-http-clients/build.gradle.kts`:

```kotlin
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
```

`spring-web` was Maven `<optional>true</optional>` — Gradle's `implementation` is already non-transitive to consumers by default, so no extra marker is needed. `asapp-commons-url` becomes `implementation` (not `api`): the one place `asapp-http-clients` uses a constant from it (`TaskApiUrl.TASKS_GET_BY_USER_ID_FULL_PATH`, inlined into an annotation value in `TasksHttpClient`) is a compile-time constant baked into the compiled class file — no `asapp-commons-url` type ever appears in `asapp-http-clients`' own public method signatures, so consumers never need it on their compile classpath.

- [ ] **Step 4: Verify both libraries resolve**

Run: `./gradlew :libs:asapp-commons-url:dependencies :libs:asapp-http-clients:dependencies`
Expected: `BUILD SUCCESSFUL`. In the `asapp-http-clients` output, the `testCompileClasspath` configuration shows `project :libs:asapp-commons-url`, `org.springframework:spring-web -> <a version resolved from the Spring Boot BOM>`, `org.springframework.boot:spring-boot-starter-test -> <BOM version>`, and `tools.jackson.core:jackson-databind:3.0.4`.

- [ ] **Step 5: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts libs/asapp-commons-url/build.gradle.kts libs/asapp-http-clients/build.gradle.kts
git commit -m "build(gradle): add library-conventions dependencies and wire both libraries"
```

---

### Task 5: Populate `asapp.service-conventions` and wire config-service + discovery-service

**Files:**
- Modify: `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`
- Modify: `services/asapp-config-service/build.gradle.kts`
- Modify: `services/asapp-discovery-service/build.gradle.kts`

**Interfaces:**
- Consumes: plugin ID `asapp.java-conventions` (Task 3); `libs` catalog aliases `"spring-cloud"`, `"guava"`, `"commons-beanutils"`, `"commons-io"`, `"bcpkix-jdk18on"`, `"bcprov-jdk18on"`, `"rhino"`, `"json-unit-assertj"`.
- Produces: plugin ID `asapp.service-conventions` — Spring Cloud BOM import, the shared CVE version constraints, and every dependency identical across all 5 services. Consumed internally by `asapp.domain-service-conventions` (Task 6) and directly by `config-service`/`discovery-service`, which need nothing else in this plan.

- [ ] **Step 1: Write the convention plugin**

Replace the placeholder comment in `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`:

```kotlin
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
```

None of `guava`/`commons-beanutils`/`commons-io`/`bcpkix-jdk18on`/`bcprov-jdk18on`/`rhino` are declared directly by any leaf module here — these are pure version pins for transitive dependencies Spring Cloud/Eureka's Netflix stack pulls in (CVE fixes), matching the Maven `services/pom.xml` dependencyManagement entries with no matching direct dependency. `spring-boot-devtools` stays `runtimeOnly` for now (not the `developmentOnly` configuration Spring Boot's Gradle plugin normally provides) — that plugin isn't applied until the packaging subtask.

- [ ] **Step 2: Wire `config-service`**

Replace the placeholder comment in `services/asapp-config-service/build.gradle.kts`:

```kotlin
plugins {
    id("asapp.service-conventions")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

`micrometer-registry-prometheus` is `runtimeOnly` here because Maven's `asapp-config-service/pom.xml` declares it with explicit `<scope>runtime</scope>` — unlike the three domain services below, where it has no scope tag (implicit compile).

- [ ] **Step 3: Wire `discovery-service`**

Replace the placeholder comment in `services/asapp-discovery-service/build.gradle.kts`:

```kotlin
plugins {
    id("asapp.service-conventions")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

- [ ] **Step 4: Verify both services resolve**

Run: `./gradlew :services:asapp-config-service:dependencies :services:asapp-discovery-service:dependencies`
Expected: `BUILD SUCCESSFUL`. Both modules' `testCompileClasspath` resolve `spring-boot-starter-actuator`/`-security` and their `-test` variants, `spring-boot-starter-webmvc-test`, `json-unit-assertj`, and each module's own starter (`spring-cloud-config-server` / `spring-cloud-starter-netflix-eureka-server`) at Spring Cloud BOM-managed versions, with no unresolved or conflicting versions.

- [ ] **Step 5: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts services/asapp-config-service/build.gradle.kts services/asapp-discovery-service/build.gradle.kts
git commit -m "build(gradle): add service-conventions dependencies and wire config/discovery services"
```

---

### Task 6: Create `asapp.domain-service-conventions` and wire tasks-service

**Files:**
- Create: `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`
- Modify: `services/asapp-tasks-service/build.gradle.kts`

**Interfaces:**
- Consumes: plugin ID `asapp.service-conventions` (Task 5); `libs` catalog aliases `"mapstruct"`, `"springdoc-openapi-starter-webmvc-ui"`, `"nimbus-jose-jwt"`, `"bootui-spring-boot-starter"`, `"spring-restdocs-mockmvc"`, `"pitest-junit5-plugin"`, `"testcontainers-junit-jupiter"`, `"testcontainers-postgresql"`, `"testcontainers-redis"`, `"archunit-junit5"`; project `:libs:asapp-commons-url` (Task 4).
- Produces: plugin ID `asapp.domain-service-conventions` — every dependency shared by `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`. Consumed directly by all three (this task wires `tasks-service`; Tasks 7–8 wire the other two).

- [ ] **Step 1: Create the convention plugin**

```kotlin
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
```

Note `spring-boot-starter-webmvc-test` is deliberately **not** repeated here — it's already in `asapp.service-conventions` (Task 5), common to all 5 services, not just these 3. Only the main (non-test) `spring-boot-starter-webmvc` is domain-specific.

Also deliberately excluded: `mapstruct-processor` as an `annotationProcessor` dependency. In Maven it lives inside the `maven-compiler-plugin`'s `<annotationProcessorPaths>` configuration (a compiler-plugin concern, not a `<dependency>`), so it's out of scope here — it lands in the next subtask (compilation) alongside the Java toolchain setup.

- [ ] **Step 2: Wire `tasks-service`**

Replace the placeholder comment in `services/asapp-tasks-service/build.gradle.kts` — this service needs nothing beyond `asapp.domain-service-conventions` except two dependencies whose configuration varies per leaf (verified against the full Maven `asapp-tasks-service/pom.xml` dependency list — nothing else is left over):

```kotlin
plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
}
```

- [ ] **Step 3: Verify tasks-service resolves**

Run: `./gradlew :services:asapp-tasks-service:dependencies`
Expected: `BUILD SUCCESSFUL`, with `testCompileClasspath` resolving every dependency listed above (from both `asapp.service-conventions` and `asapp.domain-service-conventions`) plus `micrometer-registry-prometheus` and `postgresql`, with no version conflicts.

- [ ] **Step 4: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts services/asapp-tasks-service/build.gradle.kts
git commit -m "build(gradle): add domain-service-conventions dependencies and wire tasks-service"
```

---

### Task 7: Wire authentication-service

**Files:**
- Modify: `services/asapp-authentication-service/build.gradle.kts`

**Interfaces:**
- Consumes: plugin ID `asapp.domain-service-conventions` (Task 6); `libs` catalog alias `"bcprov-jdk18on"`.

- [ ] **Step 1: Wire the module**

Replace the placeholder comment in `services/asapp-authentication-service/build.gradle.kts`. Everything here differs from `tasks-service`'s leftover set (verified against the full Maven `asapp-authentication-service/pom.xml` dependency list): `postgresql` is `implementation` (compile-scope, not `runtimeOnly`) because `JdbcConversionsConfiguration` needs it directly, and `bcprov-jdk18on` is a direct dependency (in addition to the shared CVE constraint already in `asapp.service-conventions`) because Spring Security's `PasswordEncoderFactories` needs it explicitly:

```kotlin
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("asapp.domain-service-conventions")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    // Required by Spring Security PasswordEncoderFactories
    implementation(libs.findLibrary("bcprov-jdk18on").get())
    // Must be compile-scope to be used by JdbcConversionsConfiguration
    implementation("org.postgresql:postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

- [ ] **Step 2: Verify authentication-service resolves**

Run: `./gradlew :services:asapp-authentication-service:dependencies`
Expected: `BUILD SUCCESSFUL`, with `compileClasspath` showing `org.bouncycastle:bcprov-jdk18on:1.83` and `org.postgresql:postgresql` present (not only on `runtimeClasspath`), and no version conflicts anywhere.

- [ ] **Step 3: Commit**

```bash
git add services/asapp-authentication-service/build.gradle.kts
git commit -m "build(gradle): wire authentication-service dependencies"
```

---

### Task 8: Wire users-service

**Files:**
- Modify: `services/asapp-users-service/build.gradle.kts`

**Interfaces:**
- Consumes: plugin ID `asapp.domain-service-conventions` (Task 6); `libs` catalog aliases `"resilience4j-spring-boot4"`, `"mockserver-client-java"`, `"mockserver-netty"`, `"testcontainers-mockserver"`; project `:libs:asapp-http-clients` (Task 4).

- [ ] **Step 1: Wire the module**

Replace the placeholder comment in `services/asapp-users-service/build.gradle.kts`. This is the last of the 7 modules and has the largest leftover set (verified against the full Maven `asapp-users-service/pom.xml` dependency list — every remaining dependency not already covered by `asapp.domain-service-conventions`/`asapp.service-conventions` is here):

```kotlin
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
```

`postgresql` is `runtimeOnly` here (like `tasks-service`, unlike `authentication-service`) — Maven's `asapp-users-service/pom.xml` declares it with explicit `<scope>runtime</scope>`.

- [ ] **Step 2: Verify users-service resolves**

Run: `./gradlew :services:asapp-users-service:dependencies`
Expected: `BUILD SUCCESSFUL`, with `testCompileClasspath` resolving `project :libs:asapp-http-clients` (which itself transitively resolves `project :libs:asapp-commons-url`), all Spring Boot/Spring Cloud starters at BOM-managed versions, `resilience4j-spring-boot4:2.4.0`, and the MockServer/Testcontainers test dependencies, with no version conflicts.

- [ ] **Step 3: Verify the whole build end to end**

Run: `./gradlew dependencies` (from the root — resolves every module in one pass)
Expected: `BUILD SUCCESSFUL` for all 7 modules.

Run: `mvn clean install`
Expected: `BUILD SUCCESS` — Maven still builds the project completely unchanged, proving both build systems coexist.

- [ ] **Step 4: Commit**

```bash
git add services/asapp-users-service/build.gradle.kts
git commit -m "build(gradle): wire users-service dependencies"
```
