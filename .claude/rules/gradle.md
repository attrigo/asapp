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

- Configure unit-test execution only in `asapp.java-conventions` — never per-leaf
- On `tasks.named<Test>("test")` (not `withType` — one test task today, later tiers get their own), call `useJUnitPlatform()` (JUnit 5 isn't Gradle's default) and `include("**/*Tests.class")` (Surefire-parity — runs only `*Tests`, never `*IT`/`*E2EIT`)
- Declare `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — Gradle 9 no longer auto-provides it and the Spring Boot starters don't bundle it (versionless, BOM-managed)

## Ordering

Outer **scope** comment, inner **origin** comment, alphabetical within each origin. Reordering never changes a value, coordinate, or key.

- **Scope** groups: `Build`, `BOM`, `Compile`, `Runtime`, `Test`, `CVE` — `Build` holds build-logic/plugin catalog entries; the rest mirror Maven scopes
- **Origin** order: `ASAPP`, `Spring Boot`, `Spring Cloud`, `Spring`, `Org`, `Other`
- **Version catalog** (`libs.versions.toml`): scope with `#`, origin with `##`; keep the `jackson-bom` override under `# Overrides SB4 BOM`
- **Dependency blocks** (`*.gradle.kts`): scope then origin with `//`; the `CVE` `constraints {}` leads `dependencies {}`; put each `annotationProcessor(...)` right after its `implementation(...)` (e.g. `mapstruct-processor` after `mapstruct`)
- **`gradle.properties`**: identity keys (`group`, `version`) first, then `org.gradle.*`, alphabetical within each group
