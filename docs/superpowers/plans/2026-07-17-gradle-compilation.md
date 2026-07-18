# Migrate Compilation to Gradle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `compileJava`/`compileTestJava` succeed for all 7 Gradle modules at parity with Maven — Java 25 API level, UTF-8 encoding, the `-parameters` flag, and the MapStruct annotation processor.

**Architecture:** Two changes to the existing `build-logic` convention plugins. Base compiler config (toolchain + `release`/`encoding`/`-parameters`) goes in `asapp.java-conventions` (all 7 modules); the MapStruct `annotationProcessor` goes in `asapp.domain-service-conventions` (the 3 domain services that have `@Mapper` classes). A daemon-wide UTF-8 safety net goes in `gradle.properties`. Maven is untouched.

**Tech Stack:** Gradle 9.6.1 (Kotlin DSL, `build-logic` composite build, version catalog), Java 25, Spring Boot 4.0.5, MapStruct 1.6.3.

**Source spec:** `docs/superpowers/specs/v0.5.0/2026-07-17-gradle-compilation-design.md`

## Global Constraints

- **Java version:** `25` (toolchain `languageVersion` + `options.release`) — exact value everywhere.
- **Kotlin DSL only** — never Groovy DSL (`.gradle`). Per `.claude/rules/gradle.md`.
- **No `pom.xml` edits** — Maven must keep building unchanged; both builds coexist until the final "remove Maven" subtask.
- **No `mvn` commands** — do not run Maven for verification (standing constraint). Verify with `./gradlew` only. Note: `git commit` fires the repo's pre-commit hook, which runs Maven Spotless — that is the hook firing automatically, not a `mvn` command you invoke; it only checks `src/**/*.java` files (none change here) so it passes.
- **No added compiler strictness** — no `-Werror`/`-Xlint`/deprecation-fail; Maven has none.
- **Follow `.claude/rules/gradle.md` ordering** — catalog entries and dependency blocks grouped by scope/origin, alphabetical within each origin group; `gradle.properties` keys grouped (identity first, then `org.gradle.*`) and sorted alphabetically within each group.
- **Convention-plugin altitude** — shared-and-identical config lives in the convention plugin; nothing shared moves to a leaf. Never use `subprojects {}`/`allprojects {}` or `buildSrc`.
- **Commands are POSIX/bash** (run via the Bash tool / Git Bash). `./gradlew` (not `.\gradlew`).

---

### Task 1: Configure Java compilation (all 7 modules)

Adds the Java toolchain and compiler settings to the base convention plugin, plus the daemon-wide UTF-8 safety net. After this task every module compiles with the Java 25 API level, UTF-8 sources, and `-parameters`.

**Files:**
- Modify: `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`
- Modify: `gradle.properties`
- Modify: `.claude/rules/gradle.md` (document the compilation convention)

**Interfaces:**
- Consumes: the existing `asapp.java-conventions` plugin (bare `java` + `io.spring.dependency-management`, from the dependency-management subtask) applied by all 7 modules directly or transitively.
- Produces: all 7 modules compile main + test sources targeting Java 25 with `-parameters` and UTF-8. Task 2 relies on this compiling baseline.

- [ ] **Step 1: Observe the baseline — `-parameters` is NOT yet in effect**

Compile a representative non-MapStruct library and confirm no method-parameter metadata is emitted yet (this is the "fails first" evidence — the flag isn't wired):

```bash
./gradlew :libs:asapp-http-clients:compileJava
find libs/asapp-http-clients/build/classes/java/main -name 'TasksHttpClient.class' -exec javap -v {} \; | grep -c "MethodParameters"
```
Expected: the compile SUCCEEDS (bare `java` plugin already compiles on the JDK 25 launcher), and the count prints **`0`** — no `MethodParameters` attribute on the interface's method, because `-parameters` is not configured.

Note: we grep the `TasksHttpClient` **interface** class specifically, not the whole module. The module's other class, the `TasksByUserIdResponse` **record**, always carries `MethodParameters` on its canonical constructor regardless of `-parameters` (javac emits it unconditionally for records, since component names are fixed public API), so a whole-module count starts at 2, not 0. Scoping to the interface isolates the flag's effect and gives a clean 0→N transition (verified on JDK 25.0.1: interface method 0 without / 2 with `-parameters`; record 2 without / 3 with).

- [ ] **Step 2: Add the toolchain and compiler config to `asapp.java-conventions`**

Edit `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts` to the following. Insert the two new blocks between `plugins {}` and the `val libs` line; leave the rest unchanged. (No new `import` is needed: `JavaCompile` and `JavaLanguageVersion` are in Gradle's Kotlin-DSL default imports — only non-default types like `VersionCatalogsExtension` get an explicit import in these files.)

```kotlin
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

// Pins the JDK that compiles (and later tests) the project — reproducible regardless of the JVM launching Gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Compiler settings at parity with Maven: Java 25 API level, UTF-8 sources, and -parameters
// (-parameters is required by Spring for name-based binding: @ConfigurationProperties constructor binding,
//  constructor DI, and unnamed @PathVariable/@RequestParam). No typed property exists, so use compilerArgs.
tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Imports the Spring Boot BOM (via the io.spring.dependency-management plugin), overriding its jackson-bom version to pick up a CVE fix
// Uses this plugin over Gradle-native platform() for the bomProperty override
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get().requiredVersion}") {
            bomProperty("jackson-bom.version", libs.findVersion("jackson-bom").get().requiredVersion)
        }
    }
}
```

- [ ] **Step 3: Add the daemon-wide UTF-8 safety net to `gradle.properties`**

Edit `gradle.properties` to insert `org.gradle.jvmargs=-Dfile.encoding=UTF-8` in alphabetical position among the `org.gradle.*` keys (after `org.gradle.console`, before the `parallel` block). Full resulting file:

```properties
group=com.attrigo.asapp
version=0.5.0-SNAPSHOT

org.gradle.caching=true
# org.gradle.configuration-cache: deferred until pitest, the Liquibase plugin, and Docker image building (bootBuildImage) are wired up and each is verified configuration-cache compatible
org.gradle.console=verbose
org.gradle.jvmargs=-Dfile.encoding=UTF-8
# disabled: only reliable on native Windows — causes an IOException running Gradle under WSL, re-enable once resolved
org.gradle.parallel=false
```

- [ ] **Step 4: Verify the JDK 25 toolchain resolves**

```bash
./gradlew -q javaToolchains
```
Expected: the output lists a **Java 25** toolchain (the installed JDK 25.0.1) as detected. If instead the later compile fails with "No matching toolchains found for requested specification: {languageVersion=25}", it means Gradle can't locate a JDK 25 — resolve by ensuring JDK 25 is installed and discoverable (it is the launcher here), not by removing the toolchain block.

- [ ] **Step 5: Verify `-parameters` now takes effect and bytecode targets Java 25**

Recompile the same library (the changed convention plugin forces a rebuild) and re-inspect:

```bash
./gradlew :libs:asapp-http-clients:compileJava
find libs/asapp-http-clients/build/classes/java/main -name 'TasksHttpClient.class' -exec javap -v {} \; | grep -c "MethodParameters"
find libs/asapp-http-clients/build/classes/java/main -name '*.class' | head -1 | xargs javap -v | grep "major version"
```
Expected: compile SUCCEEDS; the `MethodParameters` count on the interface class is now **greater than 0** (it was `0` at Step 1 — same class, same target — so the flag is in effect; the value is 2); `major version: 69` (Java 25).

- [ ] **Step 6: Verify all 7 modules compile (main + test)**

```bash
./gradlew compileJava compileTestJava
```
Expected: `BUILD SUCCESSFUL`, with `compileJava`/`compileTestJava` executed (or `UP-TO-DATE`) for all 7 modules — the two libs, all five services. No compilation errors.

- [ ] **Step 7: Document the compilation convention in `.claude/rules/gradle.md`**

Insert this new section immediately **after** the `## Versioning` section and **before** the `## Ordering` section:

```markdown
## Compilation

- Java version, encoding, and compiler args live only in `asapp.java-conventions` (all modules) — never per-leaf
- Pin the Java version with a toolchain (`java { toolchain { languageVersion } }`) **plus** `options.release` on `JavaCompile`: the toolchain fixes which JDK compiles/tests, `release` enforces the bytecode/API level
- Configure compiler tasks lazily via `tasks.withType<JavaCompile>().configureEach { }` — set `options.encoding = "UTF-8"` and add `-parameters` (required by Spring for name-based binding) to `options.compilerArgs`
- Pin `org.gradle.jvmargs=-Dfile.encoding=UTF-8` in `gradle.properties` as a daemon-wide UTF-8 safety net, complementary to the per-task `options.encoding`
```

- [ ] **Step 8: Commit**

```bash
git add build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts gradle.properties .claude/rules/gradle.md
git commit -m "build(gradle): configure Java compilation

Add the Java 25 toolchain and compiler settings (release=25, UTF-8
encoding, -parameters) to asapp.java-conventions, plus a daemon-wide
UTF-8 safety net in gradle.properties, so all 7 modules compile at
parity with Maven. -parameters is required by Spring for name-based
binding; the later Spring Boot plugin adds it behind a contains() guard,
so no conflict.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```
Expected: the pre-commit hook runs (Maven Spotless, Java files only → passes), commit-msg check passes, commit is created.

---

### Task 2: Wire the MapStruct annotation processor (3 domain services)

Adds `mapstruct-processor` to the version catalog and to `annotationProcessor` in `asapp.domain-service-conventions`, so the 3 domain services generate their `*MapperImpl` classes during compilation. Compilation already succeeds without it (mappers are plain annotated interfaces); the observable effect is code generation.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`
- Modify: `.claude/rules/gradle.md` (document the `annotationProcessor` pairing)
- Modify: `TODO.md` (check the completed subtask)
- Modify: `docs/superpowers/specs/v0.5.0/2026-07-17-gradle-compilation-design.md` (mark implemented)

**Interfaces:**
- Consumes: the compiling baseline from Task 1; the existing `mapstruct` catalog entry (`mapstruct = "1.6.3"`, `mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }`) and the `implementation(libs.findLibrary("mapstruct").get())` line in `asapp.domain-service-conventions`.
- Produces: `*MapperImpl.java`/`.class` generated for `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`. Terminal task — completes the subtask.

- [ ] **Step 1: Observe the baseline — no mapper implementations are generated**

Compile a domain service and confirm MapStruct generates nothing yet (the "fails first" evidence — the processor isn't wired):

```bash
./gradlew :services:asapp-tasks-service:compileJava
find services/asapp-tasks-service/build/generated/sources/annotationProcessor -name '*MapperImpl.java' 2>/dev/null | wc -l
```
Expected: compile SUCCEEDS (mapper interfaces are valid Java on their own), but the count prints **`0`** (the generated-sources directory is empty or absent) — no `*MapperImpl.java` because there is no annotation processor on the classpath.

- [ ] **Step 2: Add the `mapstruct-processor` catalog entry**

Edit `gradle/libs.versions.toml`. Under `[libraries]` → `# Compile` → `## Org`, add the processor line immediately after the existing `mapstruct` entry (shares the same `version.ref`; no new `[versions]` entry):

```toml
# Compile
## Org
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }
springdoc-openapi-starter-webmvc-ui = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdoc-openapi-starter" }
```

- [ ] **Step 3: Add the `annotationProcessor` line to `asapp.domain-service-conventions`**

Edit `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`. In the `dependencies {}` block, under `// Compile` → `// Org`, add the `annotationProcessor` line immediately after the `mapstruct` implementation:

```kotlin
    // Org
    implementation(libs.findLibrary("mapstruct").get())
    annotationProcessor(libs.findLibrary("mapstruct-processor").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
```

- [ ] **Step 4: Verify the 3 domain services now generate mapper implementations**

```bash
./gradlew :services:asapp-authentication-service:compileJava :services:asapp-tasks-service:compileJava :services:asapp-users-service:compileJava
for svc in asapp-authentication-service asapp-tasks-service asapp-users-service; do
  echo -n "$svc: "; find "services/$svc/build/generated/sources/annotationProcessor" -name '*MapperImpl.java' | wc -l
done
```
Expected: all three compile SUCCESSFULLY, and each prints a count **greater than 0** (e.g. `TaskMapperImpl.java`, `UserMapperImpl.java` are generated). This is the behavioral gate: the processor is wired.

- [ ] **Step 5: Verify the full compile still passes for all 7 modules**

```bash
./gradlew clean compileJava compileTestJava
```
Expected: `BUILD SUCCESSFUL` for all 7 modules from a clean state — confirms nothing regressed and the two infra services / two libs are unaffected by the processor (which is scoped to the 3 domain services).

- [ ] **Step 6: Document the `annotationProcessor` pairing in `.claude/rules/gradle.md`**

In the `## Ordering` section, under the **Dependency blocks** bullet, add this nested sub-bullet:

```markdown
  - A library's `annotationProcessor(...)` pairs immediately after its `implementation(...)` within the same scope/origin group (e.g. `mapstruct-processor` right after `mapstruct`)
```

- [ ] **Step 7: Mark the subtask complete in `TODO.md`**

Change line 14 from:
```markdown
    - [ ] Migrate compilation to Gradle
```
to:
```markdown
    - [X] Migrate compilation to Gradle
```

- [ ] **Step 8: Mark the design spec implemented**

Edit `docs/superpowers/specs/v0.5.0/2026-07-17-gradle-compilation-design.md`:
- Change the header line `**Status**: Approved (implementation pending)` to `**Status**: Implemented`.
- Append a `## 10. Post-implementation notes` section recording what actually shipped: state that it shipped as designed, or list any concrete deviations discovered during implementation (following the delta-note style of the dependency-management spec's §10).

- [ ] **Step 9: Commit**

```bash
git add gradle/libs.versions.toml build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts .claude/rules/gradle.md TODO.md docs/superpowers/specs/v0.5.0/2026-07-17-gradle-compilation-design.md
git commit -m "build(gradle): wire the MapStruct annotation processor

Add mapstruct-processor to the version catalog and to annotationProcessor
in asapp.domain-service-conventions, so the three domain services generate
their *MapperImpl classes during compilation. Scoped to the domain services
(the only modules with @Mapper classes), matching the dependency altitude.

Completes the 0.5.0 'Migrate compilation to Gradle' subtask.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```
Expected: pre-commit hook passes (no Java files changed), commit created.

---

## Definition of Done (from spec §7)

- `./gradlew compileJava compileTestJava` succeeds for all 7 modules. ✔ Task 1 Step 6 / Task 2 Step 5
- `*MapperImpl` classes generate for the 3 domain services. ✔ Task 2 Step 4
- Emitted bytecode targets Java 25 (major version 69). ✔ Task 1 Step 5
- `-parameters` in effect. ✔ Task 1 Step 5
- UTF-8 encoding configured (per-task + daemon-wide). ✔ Task 1 Steps 2–3
- Maven untouched (no `pom.xml` edits; not re-verified via `mvn`). ✔ by construction
