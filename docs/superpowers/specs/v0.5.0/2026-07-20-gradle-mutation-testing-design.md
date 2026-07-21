# Gradle mutation testing — design spec

**Date**: 2026-07-20
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate mutation testing to Gradle" (line 18). Attached TODO note: *"move the PIT plugin dependency off the test compile classpath onto the mutation tool's own configuration."*
**Scope**: Reproduce Maven's PIT mutation testing under Gradle at parity — mutation analysis on the 3 domain services only, each targeting its `domain` + `application.*.in.service` packages at a 100% threshold, run on demand (off the `check`/`build` path). Apply the `info.solidsoft.pitest` plugin in the domain-service convention plugin, pin PIT + the JUnit 5 bridge from the catalog, move the JUnit 5 bridge off the test compile classpath onto PIT's own `pitest` configuration, and fork PIT on the Java 25 toolchain. No `pom.xml` edit, no CI/README change.

## 1. Context

The five prior subtasks put dependency management, compilation, unit testing, integration testing, and coverage reporting on Gradle 9.6.1 / JDK 25. This subtask migrates mutation testing (PIT), the sixth of the build stages.

**What Maven does today.** PIT is configured but **off the default build** — it is bound to no phase and run manually via `mvn org.pitest:pitest-maven:mutationCoverage`.

- Root `pom.xml`: `pitest-maven` `1.23.0` in `pluginManagement` with `mutationThreshold=100`; `pitest-junit5-plugin` `1.2.3` declared as a `test`-scoped dependency in `dependencyManagement`.
- Each **domain service** (`asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`): declares `pitest-junit5-plugin` as a `test` dependency and configures `pitest-maven` with per-service `targetClasses` + `targetTests`:
  - authentication → `com.attrigo.asapp.authentication.domain.*`, `com.attrigo.asapp.authentication.application.*.in.service.*`
  - tasks → `com.attrigo.asapp.tasks.domain.*`, `com.attrigo.asapp.tasks.application.*.in.service.*`
  - users → `com.attrigo.asapp.users.domain.*`, `com.attrigo.asapp.users.application.*.in.service.*`
- **Libs** (`asapp-commons-url`, `asapp-http-clients`) and the **infra services** (`asapp-config-service`, `asapp-discovery-service`): `pitest-maven` with `<skip>true</skip>` — no mutation testing.

So mutation testing runs on exactly 3 modules, each mutating only its domain layer and application-service layer, and the build fails below 100% mutation kill.

**Current Gradle state** (Gradle 9.6.1, JDK 25). The migration parked the JUnit 5 bridge as `testImplementation(libs.findLibrary("pitest-junit5-plugin").get())` in `asapp.domain-service-conventions.gradle.kts:45` — placeholder wiring that keeps the class present for a future PIT run but puts it on the **test compile classpath**, which is what the TODO note flags. No PIT plugin is applied anywhere yet.

**Convention-plugin hierarchy** (placement-relevant): `asapp.java-conventions` (all 7 modules) → `asapp.library-conventions` (2 libs) and `asapp.service-conventions` (5 services; config + discovery apply it directly) → `asapp.domain-service-conventions` (auth, tasks, users). So anything in `domain-service-conventions` reaches exactly the 3 modules that run mutation testing — the correct single altitude for PIT.

**How third-party plugins reach the convention build.** A precompiled convention plugin cannot version a plugin in its own `plugins {}` block; the plugin must be on the `build-logic` classpath. The project already does this for `io.spring.dependency-management` — a catalog `[libraries]` entry plus `implementation(libs.spring.dependency.management.plugin)` in `build-logic/build.gradle.kts`, then a versionless `id("io.spring.dependency-management")` in the convention plugin. PIT follows the identical mechanism.

## 2. Goals

- **Parity of coverage set**: mutation testing runs on the 3 domain services and nowhere else — the analog of Maven's per-service config + `<skip>` on libs/infra services.
- **Parity of scope**: each service mutates only `com.attrigo.asapp.<svc>.domain.*` and `com.attrigo.asapp.<svc>.application.*.in.service.*`, tested by the same package sets.
- **Parity of threshold**: build fails below 100% mutation kill (`mutationThreshold = 100`).
- **Off the default build**: `./gradlew build` / `check` never runs PIT; it runs on demand via `./gradlew pitest` (the analog of Maven's manual goal).
- **Resolve the TODO note**: the JUnit 5 bridge moves off `testImplementation` onto PIT's own `pitest` configuration.
- **Java 25 correctness**: PIT parses and mutates Java 25 bytecode, and the mutation minions run on the Java 25 toolchain — not on whatever JDK the Gradle daemon happens to run.
- Settings at the correct altitude: shared PIT config in `asapp.domain-service-conventions`; per-service target packages in each service's build script.
- Zero `pom.xml` edits; Maven's PIT keeps working until the final removal subtask.

## 3. Non-goals

- **PIT in CI.** Adding PIT to the CI pipeline is an explicit backlog item (`TODO.md` → Backlog → ci → "Add Pitest to the CI pipeline"); it stays out of CI here.
- **Full-build aggregation.** Folding `pitest` into the one-command `fullBuild` umbrella is the "Migrate the full build to Gradle" subtask.
- **README / build-doc updates**, **CI/release workflow**, and the **final Maven removal** — each its own later subtask. CI still runs `mvn`; the README still documents `mvn … pitest`.
- **Coverage-guided / history-based incremental PIT, mutator tuning, `pitestReportAggregate`** — none exist in the Maven setup; not introduced here.
- **Changing what is mutated** — the domain + application-service scope and the 100% threshold are carried over unchanged.

## 4. Key decisions

Each is grounded in the current gradle-pitest-plugin, PIT, and Gradle docs — see §9 References.

| Decision | Choice | Rationale |
|---|---|---|
| Plugin + altitude | **Apply `info.solidsoft.pitest` only in `asapp.domain-service-conventions`** (3 domain services) | That plugin reaches exactly the modules Maven ran PIT on. Applying it nowhere else makes libs + infra services have no `pitest` task at all — the clean, flag-free analog of Maven's `<skip>true</skip>`, with nothing to skip. |
| Plugin version | **gradle-pitest-plugin `1.19.0`** (latest) | Newest release (2026-03-29); the only line claiming Gradle 9 support and Java 17+/Gradle 8.4+ floors. |
| PIT core version | **`pitestVersion = 1.25.8`** (override the plugin's bundled `1.22.1`) | PIT gained **official Java 25 bytecode support (ASM 9.10.1) in `1.25.6`** and **Java 25 mutator fixes in `1.25.8`**. Maven's pinned `1.23.0` predates both. This deliberately **diverges from Maven** — exactly the reasoning already accepted for the JaCoCo `0.8.13→0.8.14` Java-25 bump in the coverage subtask. Pinning it from the catalog also honors the project's pin-everything philosophy (the plugin's default would otherwise drift with plugin upgrades). |
| JUnit 5 bridge wiring (**the TODO note**) | **`junit5PluginVersion = 1.2.3`** in the `pitest {}` block; **remove** `testImplementation(pitest-junit5-plugin)` | `junit5PluginVersion` is the plugin's recommended idiom (since 1.4.7): it adds `org.pitest:pitest-junit5-plugin` to the plugin's own **`pitest` configuration** (PIT's runtime classpath) and sets `testPlugin=junit5`. That is precisely "the mutation tool's own configuration" the note asks for — the bridge leaves the test compile classpath. Version stays catalog-pinned at Maven's `1.2.3`. |
| PIT fork JVM | **`jvmPath` pinned to the Java 25 toolchain launcher** | gradle-pitest-plugin forks PIT on **the Gradle daemon's JVM, not the toolchain** — issue [#301](https://github.com/szpak/gradle-pitest-plugin/issues/301) is still open. On a Java 25 toolchain, a daemon on an older JDK yields `unsupported class file version` and a 0-coverage report. `jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.get().executablePath` forces PIT to fork JDK 25, reusing the single toolchain source of truth. No Maven analog (Maven ran PIT in-process). |
| Threshold | **`mutationThreshold = 100`** in the convention plugin | Maven's root `mutationThreshold=100` applied to the non-skipped (domain) modules; the convention plugin is that same altitude. |
| Target packages | **Per-service `targetClasses` + `targetTests` in each service's build script** | The packages are genuinely per-service data (not shared policy), mirroring how each service's build script already carries its own module-specific dependencies. Explicit per-service prefixes are exact Maven parity and avoid the glob-breadth of a shared `com.attrigo.asapp.*.domain.*` wildcard. Shared config (versions, threshold, `jvmPath`) stays in the convention plugin. |
| Activation | **`pitest` task off the `check`/`build` path; run `./gradlew pitest`** | Maven bound PIT to no phase (manual goal). Same here — a normal build never pays the mutation cost. Mirrors the coverage subtask's "off by default" stance. |
| Report path | **`timestampedReports = false`** (stable `build/reports/pitest`) | Small parity-neutral improvement: a stable, overwrite-in-place report dir instead of a timestamped subfolder, consistent with where the coverage reports land. |

## 5. Changes by file

**`gradle/libs.versions.toml`**

`[versions]` — add the plugin version under `# Build / ## Org` (alphabetical, before `jacoco`) and the PIT core version under `# Test / ## Org` (alphabetical, before `pitest-junit5-plugin`); keep the existing `pitest-junit5-plugin`:
```toml
# Build
## Spring
spring-dependency-management = "1.1.7"
## Org
gradle-pitest-plugin = "1.19.0"        # NEW
jacoco = "0.8.14"
```
```toml
# Test
…
## Org
pitest = "1.25.8"                      # NEW
pitest-junit5-plugin = "1.2.3"         # kept — now consumed as a version string
testcontainers = "2.0.4"
```

`[libraries]` — add the plugin marker under a new `# Build / ## Org` subsection; **remove** the `pitest-junit5-plugin` library entry (it is now consumed via `junit5PluginVersion` as a version string, so no dependency accessor is needed — the same versions-only treatment as `spring-boot`/`jackson-bom`):
```toml
# Build
## Spring
spring-dependency-management-plugin = { module = "io.spring.gradle:dependency-management-plugin", version.ref = "spring-dependency-management" }
## Org
gradle-pitest-plugin = { module = "info.solidsoft.gradle.pitest:gradle-pitest-plugin", version.ref = "gradle-pitest-plugin" }   # NEW
```
```toml
# Test
…
## Org
# (removed) pitest-junit5-plugin = { module = "org.pitest:pitest-junit5-plugin", version.ref = "pitest-junit5-plugin" }
testcontainers-junit-jupiter = …
```

**`build-logic/build.gradle.kts`** — put the plugin on the convention-build classpath (mirrors the spring-dependency-management wiring):
```kotlin
dependencies {
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.gradle.pitest.plugin)   // NEW
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** — apply the plugin, add shared PIT config, and remove the test-classpath bridge:
```kotlin
plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")            // NEW
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

// Mutation testing (PIT) runs only on the domain services — Maven applied pitest-maven everywhere but <skip>
// on libs + infra services; here the plugin is simply absent from those. Per-service targetClasses/targetTests
// live in each service's build script; this block holds the shared config.
pitest {
    // PIT core + the JUnit 5 bridge on PIT's own 'pitest' configuration (off the test compile classpath)
    pitestVersion       = libs.findVersion("pitest").get().requiredVersion
    junit5PluginVersion = libs.findVersion("pitest-junit5-plugin").get().requiredVersion
    // Fail below 100% mutation coverage (Maven mutationThreshold=100)
    mutationThreshold = 100
    // Fork PIT on the Java 25 toolchain, not the Gradle daemon JVM (szpak/gradle-pitest-plugin#301)
    jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.get().executablePath
    // Stable report path (build/reports/pitest), not a timestamped subfolder
    timestampedReports = false
}

dependencies {
    // …
    // Org
    // (removed) testImplementation(libs.findLibrary("pitest-junit5-plugin").get())
    testImplementation(libs.findBundle("testcontainers-shared").get())
    // Other
    testImplementation(libs.findLibrary("archunit-junit5").get())
}
```

**`services/asapp-authentication-service/build.gradle.kts`**, **`…/asapp-tasks-service/build.gradle.kts`**, **`…/asapp-users-service/build.gradle.kts`** — per-service target packages (users shown; auth/tasks identical with their own `<svc>` prefix), added as a `pitest {}` block:
```kotlin
pitest {
    targetClasses = setOf(
        "com.attrigo.asapp.users.domain.*",
        "com.attrigo.asapp.users.application.*.in.service.*",
    )
    targetTests = setOf(
        "com.attrigo.asapp.users.domain.*",
        "com.attrigo.asapp.users.application.*.in.service.*",
    )
}
```

**`.claude/rules/gradle.md`** — add a **Mutation testing** section: the `info.solidsoft.pitest` plugin is on the `build-logic` classpath (catalog `gradle-pitest-plugin` library + `build-logic/build.gradle.kts` dependency) and applied **only** in `asapp.domain-service-conventions`; `pitestVersion` (`pitest`) and `junit5PluginVersion` (`pitest-junit5-plugin`) come from the catalog; the JUnit 5 bridge lives on the plugin's `pitest` configuration, **never** `testImplementation`; `mutationThreshold = 100`; `jvmPath` is pinned to the toolchain launcher (per #301, since PIT otherwise forks the daemon JVM); `timestampedReports = false`; per-service `targetClasses`/`targetTests` go in each service build script; the `pitest` task is off the `check`/`build` path (run `./gradlew pitest`). (Deeper rule-file restructuring remains the "Keep Claude Code files in sync" subtask.)

**`TODO.md`** — check off "Migrate mutation testing to Gradle" (line 18). No new subtask is spawned.

## 6. Placement / altitude rationale

- **Plugin + shared PIT config → `asapp.domain-service-conventions` (3 domain services).** This is the exact set of modules Maven ran PIT on; the convention plugin is the single correct altitude for the plugin application, the pinned versions, the threshold, and the `jvmPath` toolchain wiring, all identical across the three.
- **`targetClasses` / `targetTests` → each service's build script.** These differ per service and are module-specific data, co-located with the module's other build-script specifics; the shared block above owns everything common.
- **Plugin marker → `build-logic` classpath.** Required for a versionless `id("info.solidsoft.pitest")` in a precompiled convention plugin; identical to the spring-dependency-management wiring already in the repo.

## 7. Verification / Definition of Done

- **Gradle 9.6.1 compatibility (top risk):** gradle-pitest-plugin `1.19.0` is smoke-tested only to Gradle **9.0**, but our wrapper is **9.6.1**. First implementation step: confirm `build-logic` compiles and the `pitest` task configures on 9.6.1. If it breaks, re-evaluate (a newer plugin RC, or report) before proceeding.
- **Coverage set matches Maven:** `pitest` registers on the 3 domain services (`./gradlew :services:asapp-users-service:tasks --group verification` shows it) and is **absent** on libs, config, and discovery.
- **TODO note resolved:** `pitest-junit5-plugin` no longer appears on any module's `testCompileClasspath` / `testImplementation` (`./gradlew :services:asapp-users-service:dependencies --configuration testCompileClasspath`); it appears on the `pitest` configuration instead.
- **Java 25 fork:** `./gradlew :services:asapp-users-service:pitest` runs mutation analysis on the Java 25 toolchain (no "unsupported class file version"), mutating only the two target package trees, and passes at 100%.
- **Off by default:** `./gradlew build` / `check` schedules no `pitest` task (`--dry-run`).
- **Cost:** a real `pitest` run forks a JVM per mutation and is **expensive** — like the coverage subtask, verify task wiring cheaply (`tasks`, `dependencies`, `--dry-run`) and leave the full mutation run to the developer unless explicitly requested.
- **Maven untouched:** no `pom.xml` edited, so `mvn … pitest` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

PIT in CI (backlog item) · the `fullBuild` / `-Pfull` umbrella · README / build-doc updates · CI / release workflow · the final Maven removal · any `pom.xml` edit · `pitestReportAggregate` / cross-module roll-up · history-based incremental PIT · mutator/engine tuning beyond Maven's config.

## 9. Contingencies

Resolve at implementation, mirroring how prior subtasks hedged their DSL/imports:
- **Leaf `pitest {}` accessor.** The `pitest {}` type-safe accessor should be generated in the service build scripts because the `implementation`/`testImplementation` accessors from convention-applied plugins already resolve there. If it does not, fall back to `configure<info.solidsoft.gradle.pitest.PitestPluginExtension> { … }`.
- **`junit-platform-launcher` double-declaration.** gradle-pitest-plugin auto-adds `junit-platform-launcher` to `testRuntimeOnly` (`addJUnitPlatformLauncher = true`), which `asapp.java-conventions` already declares (BOM-managed, versionless). If this double-add causes a version skew or warning, set `addJUnitPlatformLauncher = false` (the project already provides the launcher).
- **`jvmPath` assignment form.** The documented `…launcherFor { }.get().executablePath` resolves the launcher eagerly; if a lazier form is preferred, use `jvmPath.set(javaToolchains.launcherFor { … }.map { it.executablePath })`. (Configuration cache is disabled project-wide, so eager resolution is acceptable.)
- **`targetClasses`/`targetTests` property type.** They are `SetProperty<String>`; if Kotlin `=` assignment does not apply, use `.set(setOf(…))`.

## 10. References

- gradle-pitest-plugin — [Documentation](https://gradle-pitest-plugin.solidsoft.info/) & [CHANGELOG](https://github.com/szpak/gradle-pitest-plugin/blob/master/CHANGELOG.md) (`1.19.0` latest, default PIT `1.22.1`, Gradle 9 support tested to 9.0, min Gradle 8.4 / Java 17; `junit5PluginVersion` adds `pitest-junit5-plugin` to the `pitest` config + sets `testPlugin=junit5`; `targetClasses` default `${project.group}.*`; `pitest` task; `timestampedReports`; auto-adds `junit-platform-launcher`)
- gradle-pitest-plugin — [Issue #301: run PIT using the toolchain JDK](https://github.com/szpak/gradle-pitest-plugin/issues/301) (open; PIT forks the Gradle daemon JVM by default; workaround `jvmPath = javaToolchains.launcherFor { … }.get().executablePath`)
- PIT — [Releases](https://github.com/hcoles/pitest/releases) (`1.25.6` ASM 9.10.1 / official Java 25; `1.25.8` Java 25 mutator fixes; latest `1.25.8`, 2026-07-20)
- Gradle — [Toolchains for JVM projects](https://docs.gradle.org/current/userguide/toolchains.html) (`javaToolchains.launcherFor`, `JavaLauncher.executablePath`)

## 11. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-7-mutation-test`. A single commit is appropriate given the size:

1. `build(gradle): migrate mutation testing to Gradle` — the catalog `gradle-pitest-plugin` + `pitest` versions and the `[libraries]` swap, the `build-logic` dependency, the plugin application + shared `pitest {}` config in `asapp.domain-service-conventions` (with the `testImplementation` bridge removed), and the per-service `pitest {}` blocks in the 3 domain service scripts.

The `.claude/rules/gradle.md` Mutation testing section and the `TODO.md` checkbox ride with that commit. Following this migration's established pattern (per the coverage subtask), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 12. Post-implementation notes

This spec was written before implementation. The core change shipped substantially as designed — the `info.solidsoft.pitest` plugin is applied only in `asapp.domain-service-conventions.gradle.kts` (the 3 domain services), the JUnit 5 bridge moved off `testImplementation` onto the plugin's own `pitest` configuration, per-service `targetClasses`/`targetTests` remained per-service, `mutationThreshold = 100` with `timestampedReports = false`, and the `pitest` task stayed off the `check`/`build` path; every §4 key decision reached the branch intact.

The canonical implementation is the current state of `asapp.domain-service-conventions.gradle.kts`, the three `services/asapp-{authentication,tasks,users}-service/build.gradle.kts` scripts, `gradle/libs.versions.toml`, and the Mutation testing section of `.claude/rules/gradle.md` on this branch — not this document.

Notable deltas:

- **`addJUnitPlatformLauncher = false` escalated from a §9 contingency to a mandatory fix.** The spec parked it only as a §9 "if it causes skew" hedge. In practice the plugin's launcher auto-add injects an unaligned `junit-platform-launcher` `1.12.2` that can't map JUnit 6's `6.0.x` platform versioning and crashes the coverage minion (`OutputDirectoryCreator not available`). The line now sits in the shared `pitest {}` block of `asapp.domain-service-conventions.gradle.kts`, disabling the auto-add so the BOM-aligned `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` from `asapp.java-conventions` is the minion's only launcher. This is a hard runtime prerequisite on the JUnit 6 / Java 25 stack and is now a first-class bullet in `.claude/rules/gradle.md`.
- **`jvmPath` uses the lazy `.map` form, not the eagerly-resolved `.get()` of §4/§5 (review S1).** §4's decision table and §5's code showed `…launcherFor { … }.get().executablePath`, which resolves the JDK-25 toolchain at configuration time on every invocation against the 3 services. Shipped code in `asapp.domain-service-conventions.gradle.kts` uses `…launcherFor { … }.map { it.executablePath }` — same JVM/value at `pitest` time, only the resolution timing changed. `.claude/rules/gradle.md` was synced to the lazy form.
- **Per-service targets bound to one local `val pitestPackages`, not the duplicated `setOf` literals of §5 (review N1).** §5 showed `targetClasses` and `targetTests` as two verbatim-duplicated literals per service. Each of `services/asapp-{authentication,tasks,users}-service/build.gradle.kts` now declares one `val pitestPackages` in its `pitest {}` block and assigns both properties to it (intra-file dedup only). Cross-service centralization was declined — targets stay per-service data per §4 "Target packages" and the `.claude/rules/gradle.md` per-service blessing.
- **Catalog alias is `gradle-pitest` under `## Other`, not `gradle-pitest-plugin` under `## Org` (revises §5 catalog placement).** In `gradle/libs.versions.toml` the `[versions]` alias is `gradle-pitest = "1.19.0"` (the `-plugin` suffix dropped) under `# Build / ## Other` after `jacoco`; the `[libraries]` entry keeps the name `gradle-pitest-plugin` but points at `version.ref = "gradle-pitest"`. The `build-logic/build.gradle.kts` accessor `libs.gradle.pitest.plugin` is unchanged, and the `pitest-junit5-plugin` library entry was removed as designed.
- **`mockserver` relocated to `# Test / ## Org` — opportunistic cleanup outside this design's scope.** The branch also moved the `mockserver` version and its `mockserver-client-java` / `mockserver-netty` library entries in `gradle/libs.versions.toml` from `# Test / ## Other` into `# Test / ## Org`. Not a mutation-testing requirement (§5 lists no mockserver edit); recorded so it isn't mistaken for one.
- **`pitest {}` comments condensed from §5's verbose form.** The shipped block in `asapp.domain-service-conventions.gradle.kts` uses a one-line header comment and a threshold comment that drops the Maven reference ("Fail below 100% mutation coverage"). Behavior-neutral.
- **`TODO.md` changes exceed §5's "tick line 18, spawn nothing."** Line 18 is checked and the resolved PIT-classpath note removed as intended, but the branch also added a "Clean Gradle files" housekeeping bucket and (in the working tree) applied review S2 (removed a trailing comma) plus a new "review IntelliJ warnings" note — general migration housekeeping riding on this branch, landing with the close.

For future Gradle mutation-testing / PIT edits, treat `asapp.domain-service-conventions.gradle.kts`, the three service build scripts, and the `.claude/rules/gradle.md` Mutation testing section as the template; this spec is preserved as a record of the original design intent.
