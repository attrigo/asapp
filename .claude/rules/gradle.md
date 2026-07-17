---
paths:
  - "**/*.gradle.kts"
  - "**/gradle.properties"
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

- Java version, encoding, and compiler args live only in `asapp.java-conventions` (all modules) — never per-leaf
- Pin the Java version with a toolchain (`java { toolchain { languageVersion } }`) **plus** `options.release` on `JavaCompile`: the toolchain fixes which JDK compiles/tests, `release` enforces the bytecode/API level
- Configure compiler tasks lazily via `tasks.withType<JavaCompile>().configureEach { }` — set `options.encoding = "UTF-8"` and add `-parameters` (required by Spring for name-based binding) to `options.compilerArgs`
- Set source encoding explicitly per-task with `options.encoding = "UTF-8"` (the reproducible, Maven-parity knob); do **not** pin `org.gradle.jvmargs=-Dfile.encoding=UTF-8` daemon-wide — it clobbers Gradle's default daemon JVM args (metaspace cap, OOM heap-dump) and is redundant on JDK 18+ where `file.encoding` already defaults to UTF-8 (JEP 400)

## Ordering

Group and sort entries to mirror the [Maven POM convention](maven.md): an outer **scope** comment, then an inner **origin** comment, alphabetical within each origin group. Reordering never changes a value, coordinate, or key.

- **Scope** groups: `Build`, `BOM`, `Compile`, `Runtime`, `Test`, `CVE` — `Build` holds build-logic/Gradle-plugin catalog entries; the rest mirror Maven POM scopes
- **Origin** groups, in order: `ASAPP`, `Spring Boot`, `Spring Cloud`, `Spring`, `Org`, `Other`
- **Version catalog** (`gradle/libs.versions.toml`): mark scope with `#` and origin with `##` in `[versions]` and `[libraries]`; keep the `jackson-bom` override under its own `# Overrides SB4 BOM` note
- **Dependency blocks** (`*.gradle.kts`): mark scope and origin with `//` (scope line, then origin line); the `CVE` `constraints {}` block leads `dependencies {}`
  - A library's `annotationProcessor(...)` pairs immediately after its `implementation(...)` within the same scope/origin group (e.g. `mapstruct-processor` right after `mapstruct`)
- **`gradle.properties`**: identity keys (`group`, `version`) first, then `org.gradle.*` settings, sorted alphabetically by key within each group
