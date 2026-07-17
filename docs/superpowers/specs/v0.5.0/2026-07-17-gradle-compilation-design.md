# Gradle compilation — design spec

**Date**: 2026-07-17
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate compilation to Gradle"
**Scope**: Make `compileJava`/`compileTestJava` actually succeed for all 7 modules at parity with Maven — Java version, source encoding, the `-parameters` flag, and the MapStruct annotation processor. No test execution, no other `<build><plugins>` migration, no `pom.xml` edits.

## 1. Context

The previous subtask ("Migrate dependency management to Gradle", `docs/superpowers/specs/v0.5.0/2026-07-16-gradle-dependency-management-design.md`) wired every dependency coordinate into Gradle at parity with the POMs, but deliberately applied the `java`/`java-library` plugins **bare** — no toolchain, no `release`, no compiler args — so `./gradlew :module:dependencies` resolves cleanly while `compileJava` is not yet expected to work. That spec explicitly deferred to *this* one (its §3): *"Java toolchain, `release` version, compiler args, MapStruct annotation-processor compiler flags — next subtask."*

What Maven does for compilation today:
- **Java version**: `<release>25</release>` on `maven-compiler-plugin` (property `java.version=25`), configured in `libs/pom.xml` and `services/pom.xml`. No `maven-toolchains-plugin` — Maven just uses whichever JDK runs it.
- **Encoding**: `project.build.sourceEncoding=UTF-8` (inherited from `spring-boot-starter-parent`).
- **`-parameters`**: enabled because `spring-boot-starter-parent` sets `maven.compiler.parameters=true`.
- **MapStruct**: `services/pom.xml` adds `org.mapstruct:mapstruct-processor` via `maven-compiler-plugin` `<annotationProcessorPaths>`. Present on all 5 services in Maven, but only the 3 domain services (`authentication`, `tasks`, `users`) actually have `@Mapper` classes; `config`/`discovery` and both libs have none.

Current Gradle state: `java`/`java-library` applied bare; `implementation(libs...mapstruct)` present in `asapp.domain-service-conventions` but with **no `annotationProcessor`** wired, so mapper implementations would not generate. Build environment: JDK 25.0.1, Gradle 9.6.1 (which supports both running on and targeting JDK 25 since 9.1.0).

## 2. Goals

- `./gradlew compileJava compileTestJava` succeeds for all 7 modules.
- Compiled bytecode targets the Java 25 API, matching Maven's `<release>25</release>`.
- MapStruct `*MapperImpl` classes generate for the 3 domain services.
- Every compilation setting Maven applies today has a Gradle equivalent (Java level, UTF-8, `-parameters`).
- Settings live at the correct convention-plugin altitude, consistent with the taxonomy the dependency subtask established.

## 3. Non-goals

- **Test execution** — running `test`/unit/integration tests is the next subtask. This subtask only compiles main + test sources.
- Any other Maven `<build><plugins>` entry (jacoco, spotless, pitest, javadoc, source, spring-boot, git-build-hook) — each maps to its own later TODO subtask.
- Any `pom.xml` edit. Maven keeps building unchanged until the final "verify full parity, then remove Maven entirely" subtask.
- Compiler strictness Maven does not have today (`-Werror`, `-Xlint`, deprecation/warning failures).

## 4. Key decisions

Each decision was grounded in official documentation (Gradle user guide, Spring reference docs, MapStruct reference guide) — see §8 References.

| Decision | Choice | Rationale |
|---|---|---|
| Java version mechanism | **Java toolchain (`languageVersion = 25`) + `options.release = 25`** | Gradle officially recommends toolchains as the default ("For most users: Use Java toolchains") — a toolchain pins *which JDK* compiles and tests (reproducible, self-documenting) but does not by itself constrain the bytecode/API level. Adding `options.release = 25` supplies that enforcement (the direct equivalent of Maven's `<release>`). Gradle's guide endorses exactly this combination for "a specific JDK **and** a compatible bytecode level." A strictly stronger guarantee than Maven has today (which pins only the API level, not the JDK). |
| Toolchain auto-provisioning (foojay resolver) | **Not included** | The toolchain resolves the locally-installed JDK 25 (the dev launcher today; CI's `setup-java` provides it). Auto-download machinery (a settings plugin + network dependency) adds no value while JDK 25 is always present, and fits more naturally with the later CI subtask if ever wanted. |
| `-parameters` flag | **`options.compilerArgs.add("-parameters")` in `asapp.java-conventions`** | Officially required by Spring, not stylistic: Spring Framework 6.1 removed the bytecode-parsing fallback, so without it `@ConfigurationProperties` constructor binding, name-based dependency injection, and unnamed `@PathVariable`/`@RequestParam`/`@RequestHeader` binding fail at runtime. Gradle's `CompileOptions` has no typed `parameters` property, so `compilerArgs` is canonical — it is exactly what the Spring Boot Gradle plugin does internally. |
| Interaction with the future Spring Boot plugin | **Add manually now; leave a note for the packaging subtask** | The `org.springframework.boot` plugin (deferred to "Migrate packaging") auto-adds `-parameters`, but behind a `compilerArgs.contains("-parameters")` guard — so the manual entry causes no duplicate/conflict when the plugin lands. It can be removed at that point for a single source of truth. `io.spring.dependency-management` (already applied) does no compiler configuration, so nothing wires `-parameters` today without this. |
| Source encoding | **`options.encoding = "UTF-8"` (per-compile-task) + `org.gradle.jvmargs=-Dfile.encoding=UTF-8` (daemon-wide)** | `JavaCompile.options.encoding` defaults to the platform charset, which Gradle's best-practices guide flags as a build-cache/reproducibility hazard under an explicit "Use UTF-8 File Encoding" rule. The per-task setting is the direct Maven-parity fix; the `gradle.properties` daemon-wide setting is Gradle's recommended complementary safety net (also covers Kotlin-DSL script compilation and any tool that trusts the platform default). |
| MapStruct processor | **`annotationProcessor(libs.findLibrary("mapstruct-processor").get())` in `asapp.domain-service-conventions`, paired with the existing `implementation(mapstruct)`** | MapStruct's official Gradle setup is `implementation(mapstruct)` + `annotationProcessor(mapstruct-processor)`. It lives in `asapp.domain-service-conventions` because that is exactly where `mapstruct` already lives and the only 3 modules with `@Mapper` classes apply it. Incremental annotation processing works automatically (MapStruct is a registered isolating processor since 1.4). |
| No `testAnnotationProcessor` | **Omitted** | All `@Mapper` types are in main source only; a test-scoped processor would be an idle, unused dependency. |
| No MapStruct compiler args | **Omitted** | `componentModel = "spring"` is set on every `@Mapper` annotation individually, so the global `-Amapstruct.defaultComponentModel=spring` fallback is never consulted; no other `-Amapstruct.*` option applies. |
| MapStruct catalog entry | **`mapstruct-processor` library sharing `version.ref = "mapstruct"`** | Mirrors the catalog's existing sibling-entry pattern (e.g. the `testcontainers-*` family). Consumed in the convention plugin via `VersionCatalogsExtension`/`findLibrary`, matching the established `build-logic` access style (precompiled script plugins do not get the generated `libs.*` accessor). |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all 7 modules) — add:
```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** (3 domain services) — add one line in the `// Compile` / `// Org` group, immediately after the existing `mapstruct` implementation:
```kotlin
implementation(libs.findLibrary("mapstruct").get())
annotationProcessor(libs.findLibrary("mapstruct-processor").get())
```

**`gradle/libs.versions.toml`** — add under `[libraries]` → `# Compile` / `## Org`, alphabetically right after `mapstruct`:
```toml
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }
```

**`gradle.properties`** — add the daemon-wide encoding safety net (keys stay grouped/sorted per `gradle.md`):
```properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

**`.claude/rules/gradle.md`** — document the new conventions introduced here (consistent with how the two prior subtasks updated it inline): a "Compilation" note that the Java toolchain + `release`/`encoding`/`-parameters` compiler config lives in `asapp.java-conventions`, and that a library's `annotationProcessor(...)` pairs immediately after its `implementation(...)` within the same scope/origin group.

## 6. Placement / altitude rationale

- **Base compiler config → `asapp.java-conventions` (all 7).** Java level, encoding, and `-parameters` are identical for every module including the two libs, so they belong in the base plugin, not repeated per leaf.
- **MapStruct processor → `asapp.domain-service-conventions` (the 3 domain services).** `config`/`discovery` (pure platform servers) and both libs have zero `@Mapper` classes. Placing the processor beside the `mapstruct` implementation it already declares keeps the processor exactly at the altitude of the modules that use it — narrower and more faithful than Maven's `services/pom.xml`, which attached the processor path to all 5 services (inert on `config`/`discovery`).
- **Daemon-wide encoding → `gradle.properties`.** A build-wide safety net is by nature a root-level setting, complementary to (not a replacement for) the per-task `options.encoding`.

## 7. Verification / Definition of Done

- `./gradlew compileJava compileTestJava` succeeds for all 7 modules (equivalently `./gradlew classes testClasses`).
- The 3 domain services generate MapStruct implementations — e.g. `*MapperImpl.java` files appear under `services/asapp-tasks-service/build/generated/sources/annotationProcessor/java/main/` (or the corresponding `*MapperImpl.class` under `build/classes`).
- Emitted bytecode targets Java 25 (major version 69) — spot-check one compiled `.class`.
- Maven is **untouched** — no `pom.xml` is edited, so the Maven build is unaffected by construction. Per the standing constraint, this is **not** re-verified by running any `mvn` command.

## 8. Out of scope / YAGNI

Test execution · every other Maven `<build><plugins>` entry (jacoco, spotless, pitest, javadoc, source, spring-boot, git-build-hook) · Javadoc/Test task encoding (those tasks have their own encoding knobs and belong to the docs/testing subtasks) · foojay toolchain auto-provisioning · compiler strictness (`-Werror`/lint) · MapStruct compiler args · `testAnnotationProcessor` · any `pom.xml` edit.

### References
- Gradle — [Toolchains for JVM projects](https://docs.gradle.org/current/userguide/toolchains.html), [Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html), [Best Practices: Use UTF-8 File Encoding](https://docs.gradle.org/current/userguide/best_practices_performance.html#use_utf8_encoding), [CompileOptions](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.CompileOptions.html)
- Spring — [Boot reference: constructor binding requires `-parameters`](https://docs.spring.io/spring-boot/reference/features/external-config.html), [Boot Gradle plugin: reacting to the java plugin](https://docs.spring.io/spring-boot/gradle-plugin/reacting.html), [Framework 6.1 release notes: parameter name retention](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.1-Release-Notes)
- MapStruct — [Installation (Gradle)](https://mapstruct.org/documentation/installation/), [Reference guide §2.4 configuration options](https://mapstruct.org/documentation/stable/reference/html/#configuration-options)

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle`. Suggested commit slicing:

1. `build(gradle): configure Java compilation` — the `asapp.java-conventions` compiler block + the `gradle.properties` encoding safety net.
2. `build(gradle): wire the MapStruct annotation processor` — the catalog `mapstruct-processor` entry + the `annotationProcessor` line in `asapp.domain-service-conventions`.

(A single `build(gradle): migrate compilation to Gradle` commit is equally acceptable given the size — decided at commit time.) The `.claude/rules/gradle.md` update and the `TODO.md` checkbox ride with whichever commit lands the change they describe.

## 10. Post-implementation notes

This spec was written before implementation and split into the two commits §9 suggested: `build(gradle): configure Java compilation` (`f8d5ce4a`) for the toolchain/`release`/encoding/`-parameters` base config, and `build(gradle): wire the MapStruct annotation processor` for the catalog entry and `annotationProcessor` line. Both landed exactly as designed — no deviations found.

The canonical implementation is the current state of the Gradle build files on this branch — `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`, `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, and `.claude/rules/gradle.md` — not this document.

**Confirmed at parity with §4/§5/§6, file-for-file:**

- The Java toolchain (`languageVersion = JavaLanguageVersion.of(25)`) and `options.release = 25` land together in `asapp.java-conventions`, exactly per §4's "Java version mechanism" row.
- `-parameters` is added via `options.compilerArgs.add("-parameters")` (no typed `CompileOptions` property exists), applied through `tasks.withType<JavaCompile>().configureEach {}` so it reaches both `compileJava` and `compileTestJava` for all 7 modules.
- `options.encoding = "UTF-8"` is set per-task, and `org.gradle.jvmargs=-Dfile.encoding=UTF-8` is present in `gradle.properties` as the daemon-wide safety net — both per §4/§5.
- `mapstruct-processor` was added to `gradle/libs.versions.toml` sharing `version.ref = "mapstruct"` (no new `[versions]` entry), positioned alphabetically immediately after `mapstruct` under `# Compile` / `## Org` — no floating version, per §5.
- `annotationProcessor(libs.findLibrary("mapstruct-processor").get())` was added to `asapp.domain-service-conventions`, immediately after `implementation(libs.findLibrary("mapstruct").get())` in the same `// Org` group — scoped only to the 3 domain services, never the base `asapp.java-conventions` plugin, matching the altitude rationale in §6.
- No `testAnnotationProcessor`, no MapStruct `-Amapstruct.*` compiler args, no foojay toolchain auto-provisioning — all omitted exactly per §4/§8.
- Behavioral verification: `*MapperImpl.java` now generates for all 3 domain services (12 files for `asapp-authentication-service`, 7 for `asapp-tasks-service`, 6 for `asapp-users-service`), where a pre-wiring baseline compile confirmed zero generated files; `./gradlew clean compileJava compileTestJava` succeeds for all 7 modules from a clean state; `pom.xml` was never touched and no `mvn` command was run to verify.

No deltas to record — this is the rare case where the shipped build files match the design as written, decision-for-decision.

For future Gradle compilation edits, treat these build files as the template; this spec is preserved as a record of the original design intent.
