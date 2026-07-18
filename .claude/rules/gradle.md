---
paths:
  - "**/*.gradle.kts"
  - "**/gradle.properties"
  - "**/libs.versions.toml"
---

# Gradle Build Conventions

## DSL

- Use Kotlin DSL (`.gradle.kts`) for every Gradle build script
- Never use Groovy DSL (`.gradle`)

## Shared Build Configuration

- Place cross-cutting build configuration only in precompiled convention plugins inside the `build-logic` composite build
- Reference `build-logic` from the root `settings.gradle.kts` via `pluginManagement { includeBuild("build-logic") }`
- Never place shared config in root `subprojects {}` / `allprojects {}` conditional blocks
- Never use `buildSrc`
- Namespace convention plugin IDs `asapp.<concern>-conventions`, e.g. `asapp.java-conventions`, `asapp.service-conventions`

## Module Structure

- Mirror module project paths exactly on the physical folder layout — no `projectDir` remapping, no flattening
- Declare each leaf module with its own colon-path `include(...)` call in the root `settings.gradle.kts`
- Group `include(...)` calls by top-level folder (`libs`, `services`); sort alphabetically by module name within each group:
  ```kotlin
  include("libs:asapp-commons-url")
  include("libs:asapp-http-clients")
  include("services:asapp-authentication-service")
  include("services:asapp-config-service")
  include("services:asapp-discovery-service")
  include("services:asapp-tasks-service")
  include("services:asapp-users-service")
  ```
- Never give `libs` or `services` their own `build.gradle.kts` or explicit `include(...)` — Gradle creates them automatically as empty intermediate projects

## Root Project Identity

- Set `rootProject.name = "asapp"` in the root `settings.gradle.kts`
- Never use `asapp-parent` — Gradle's root project isn't a deployable artifact

## Versioning

- Single-source `group` and `version` in root `gradle.properties` (`group=com.attrigo.asapp`, `version=...`) — Gradle propagates both to every project in the build automatically, no `allprojects`/`subprojects` block needed
- Never set `group` or `version` in a module's own build script

## Compilation

- Set Java version, encoding, and compiler args only in `asapp.java-conventions` — never per-leaf
- Pin the Java version with a toolchain **plus** `options.release` on `JavaCompile` (toolchain = which JDK compiles/tests; `release` = bytecode/API level)
- In `tasks.withType<JavaCompile>().configureEach { }`, set `options.encoding = "UTF-8"` and add `-parameters` (Spring name-based binding) to `options.compilerArgs`
- Set encoding per-task, never daemon-wide via `org.gradle.jvmargs` (that clobbers Gradle's default JVM args)

## Testing

- Configure test execution only in convention plugins — never per-leaf
- Enable the JUnit Platform once via `tasks.withType<Test>().configureEach { useJUnitPlatform() }` in `asapp.java-conventions` (JUnit 5 isn't Gradle's default) — it covers every tier, including tasks registered in other plugins
- Unit tier: `tasks.named<Test>("test")` in `asapp.java-conventions` sets `include("**/*Tests.class")`
- Integration tier: register one `integrationTest` `Test` task in `asapp.service-conventions` (services only), reusing the `test` source set (`testClassesDirs`/`classpath`) with `include("**/*IT.class")` (also matches `*E2EIT`); order it `shouldRunAfter(tasks.named("test"))` and wire `check.dependsOn(integrationTest)`
- Declare `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — Gradle 9 no longer auto-provides it and the Spring Boot starters don't bundle it (versionless, BOM-managed)
- The base plugin adds only the platform launcher, not a JUnit engine — each module that runs tests supplies its own via a `spring-boot-starter-*-test` starter

## Coverage

- Apply the core `jacoco` plugin in `asapp.java-conventions` (all modules) and pin the tool from the catalog: `jacoco { toolVersion = libs.findVersion("jacoco").get().requiredVersion }` — `0.8.14` for official Java 25 support (also Gradle's default)
- The unit-tier report is the plugin's own `jacocoTestReport` task (all modules); add `dependsOn(tasks.named("test"))` since the plugin does not wire it to run the tests
- Register the integration-tier (`jacocoIntegrationTestReport`) and merged (`jacocoAggregateReport`) reports in `asapp.service-conventions` (services only); reference the `Test` tasks explicitly in `executionData()`/`dependsOn()` (integration → `integrationTest`; merged → `test` + `integrationTest`) — passing a live `tasks.withType<Test>()` collection to `executionData()` fails at task realization (`DefaultTaskCollection#all` context error)
- Keep reports off the `check`/`build` path — opt in by naming the report task (the flag-free analog of Maven's `-Pfull`); the JaCoCo agent is auto-attached to every `Test` task, so there is no activation flag
- HTML only; no coverage thresholds or enforcement (report-only, at Maven parity)

## Ordering

Outer **scope** comment, inner **origin** comment, alphabetical within each origin. Reordering never changes a value, coordinate, or key.

- **Scope** groups: `Build`, `BOM`, `Compile`, `Runtime`, `Test`, `CVE` — `Build` holds build-logic/plugin catalog entries; the rest mirror Maven scopes
- **Origin** order: `ASAPP`, `Spring Boot`, `Spring Cloud`, `Spring`, `Org`, `Other`
- **Version catalog** (`libs.versions.toml`): scope with `#`, origin with `##`; keep the `jackson-bom` override under `# Overrides SB4 BOM`
- **Dependency blocks** (`*.gradle.kts`): scope then origin with `//`; the `CVE` `constraints {}` leads `dependencies {}`; put each `annotationProcessor(...)` right after its `implementation(...)` (e.g. `mapstruct-processor` after `mapstruct`)
- **`gradle.properties`**: identity keys (`group`, `version`) first, then `org.gradle.*`, alphabetical within each group
