# Gradle unit testing — design spec

**Date**: 2026-07-18
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate unit testing to Gradle"
**Scope**: Make `./gradlew test` run the unit tier (`*Tests`) on the JUnit Platform, at parity with `mvn test` (Maven Surefire) — framework enablement, unit-tier test selection, and the one test-runtime dependency Gradle 9 now requires. No integration/e2e execution, no other `<build><plugins>` migration, no `pom.xml` edits, no files moved.

## 1. Context

The previous subtask ("Migrate compilation to Gradle", `docs/superpowers/specs/v0.5.0/2026-07-17-gradle-compilation-design.md`) made `compileJava`/`compileTestJava` succeed for all 7 modules — so every test source already compiles — but deliberately stopped short of running anything: its §3 non-goals list "Test execution — running `test`/unit/integration tests is the next subtask." This spec is that next subtask, and covers **only the unit tier**.

How the project organizes tests today (all three tiers share one `src/test/java` per module, separated purely by class-name suffix, exactly as Maven Surefire/Failsafe distinguish them):

| Tier | Suffix | Maven plugin / phase | Count | Modules |
|---|---|---|---|---|
| Unit | `*Tests` | Surefire (`test`) | 99 | http-clients 1, authentication 44, tasks 26, users 28 |
| Integration | `*IT` | Failsafe (`integration-test`) | 46 | authentication 15, config 4, discovery 4, tasks 10, users 13 |
| E2E | `*E2EIT` | Failsafe (`integration-test`) | 4 | authentication 2, tasks 1, users 1 |

`config` and `discovery` have **zero** unit tests — they are pure platform servers (Config Server / Eureka) covered only by `*IT`. `asapp-commons-url` has no test sources at all. The unit tier is pure Mockito (`@ExtendWith`, no `@SpringBootTest`, no Testcontainers), so it needs **no Docker** and runs fast.

What Maven does for unit tests today:
- `maven-surefire-plugin` is declared bare in `services/pom.xml` (no custom `<includes>`/`<excludes>`), so it uses Spring Boot parent's defaults. Surefire's default include patterns (`Test*`, `*Test`, `*Tests`, `*TestCase`) match only the `*Tests` classes here; `*IT`/`*E2EIT` end in `IT`, which Surefire does not include (that is Failsafe's `*IT` pattern). So `mvn test` runs exactly the 99 `*Tests`.
- JUnit 5 (Jupiter) is the engine, via `spring-boot-starter-test` / the Spring Boot 4 modular `*-test` starters.

Current Gradle state: the `java` plugin is applied bare (compilation configured, no test config). Gradle still defaults its `Test` task to **JUnit 4**, and — because all three tiers live in one source set — an unconfigured `test` task would try to run *all 149* classes, including the Docker-dependent `*IT`/`*E2EIT`. Both must be addressed: enable the JUnit Platform, and restrict `test` to the unit tier.

Build environment: JDK 25, Gradle 9.6.1.

## 2. Goals

- `./gradlew test` succeeds and runs exactly the 99 `*Tests` unit tests on the JUnit Platform.
- The set matches Maven Surefire's set one-for-one (per module: http-clients 1, authentication 44, tasks 26, users 28; commons-url/config/discovery 0).
- `*IT`/`*E2EIT` are never executed by `test` (no Docker required to run `./gradlew test`).
- Settings live at the correct convention-plugin altitude, consistent with the taxonomy the compilation subtask established.

## 3. Non-goals

- **Integration & e2e execution and structure** — running `*IT`/`*E2EIT`, and any decision about separate source sets / the `jvm-test-suite` plugin, is the next subtask ("Migrate integration testing to Gradle"). This spec does not move a single test file.
- Any other Maven `<build><plugins>` entry — JaCoCo (coverage), PIT (mutation), Spotless (formatting), asciidoctor (docs), spring-boot (packaging), git hooks — each maps to its own later TODO subtask.
- The `pitest-junit5-plugin` currently sitting on the test compile classpath (`testImplementation` in `asapp.domain-service-conventions`) is left exactly as-is; relocating it onto PIT's own configuration is explicitly the mutation-testing subtask's job (see the TODO note on that line).
- Any `pom.xml` edit. Maven keeps building unchanged until the final "verify full parity, then remove Maven entirely" subtask.
- Developer-facing console `testLogging`, parallel test forking, and any test-execution tuning Maven does not have today.

## 4. Key decisions

Each decision was grounded in official documentation (Gradle user guide, Spring Boot reference/plugin docs) and the current Gradle 9 / Spring Boot 4 behavior — see §8 References.

| Decision | Choice | Rationale |
|---|---|---|
| Enable the test framework | **`useJUnitPlatform()`** | Gradle still defaults the `Test` task to JUnit 4; JUnit 5 must be enabled explicitly. The Spring Boot Gradle plugin does not do this either — and it is not applied yet (deferred to the packaging subtask) — so nothing enables it implicitly. |
| Which task carries the config | **`tasks.named<Test>("test") { … }`** (not `tasks.withType<Test>().configureEach`) | Exactly one `Test` task exists today (`test`). `named` is the lazy configuration-avoidance accessor Spring Initializr itself generates, and scopes the config to the single task this subtask actually creates. Using `withType(...).configureEach` would configure a lone existing task purely in anticipation of a future `integrationTest` task — preempting the integration subtask, which should declare `useJUnitPlatform()` on its own task where that task is defined. (This is unlike the compiler config, where `withType<JavaCompile>().configureEach` is justified because two compile tasks — `compileJava` + `compileTestJava` — genuinely exist now.) |
| JUnit Platform launcher | **`testRuntimeOnly("org.junit.platform:junit-platform-launcher")`** in `asapp.java-conventions` | Gradle 9 removed the deprecated machinery that guessed and injected a launcher version, so the launcher must now be on the test runtime classpath explicitly or the run fails with "Failed to load JUnit Platform." Spring Boot declined to add it transitively to its test starters (spring-boot#46037, closed *not planned*), so the project must declare it. This is the **only** new dependency — JUnit Jupiter already arrives transitively through the Spring Boot test starters wired in the dependency subtask. |
| Launcher version | **None (versionless coordinate string)** | The Spring Boot BOM imports `junit-bom`, which manages every `org.junit.platform:*` coordinate. Per the dependency subtask's established rule, a BOM-managed artifact gets no `[versions]`/`[libraries]` catalog entry — it is written as a plain coordinate string in the `dependencies {}` block, like the `spring-boot-starter-*` strings. |
| Restrict `test` to the unit tier | **`include("**/*Tests.class")`** | The direct analog of Maven Surefire's `<includes>`: a file-pattern filter over the compiled test classes. It whitelists the one unit-test suffix the project uses and thereby excludes `*IT`/`*E2EIT` without naming them — no Docker-dependent class is ever loaded by `test`. |
| Whitelist vs. blacklist | **Whitelist `*Tests`** (not `exclude("**/*IT.class")`) | Matches Surefire's include-based semantics and reinforces the project's strict `*Tests` unit-naming convention (a stray non-`*Tests` class silently won't run under `test`, which is the desired signal). The project uses exactly one unit pattern, so whitelisting leaves no parity gap. |
| File-pattern `include` vs. `filter { includeTestsMatching }` | **File-pattern `include`** | Gradle's user guide notes filtering "supersedes" include/exclude and is the modern API, but include/exclude is fully supported (not deprecated) and is the exact, self-documenting parity for what Surefire does — operating on class-file paths rather than fully-qualified test names. Parity is this migration's north star, so the closer analog wins. |
| Console output | **Gradle defaults, no `testLogging`** | Gradle's default `Test` reporting — an HTML report and JUnit XML under `build/reports/tests` and `build/test-results`, plus failure output on the console — is functionally equivalent to Surefire's. Adding `testLogging` events is a developer-experience nicety with no parity requirement; omitted per YAGNI, consistent with prior subtasks. |
| Placement | **`asapp.java-conventions`** (all 7 modules) | Unit tests run identically for every module that has them; the convention plugin is the single correct altitude, exactly as compiler config lives there. Modules with zero `*Tests` (commons-url, config, discovery) simply run an empty `test` task — harmless and correct. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all 7 modules) — two additions.

Test-task configuration, placed immediately after the existing `tasks.withType<JavaCompile>` block (keeping task configuration grouped):
```kotlin
// Runs the unit tier on the JUnit Platform; include only *Tests
tasks.named<Test>("test") {
    useJUnitPlatform()
    include("**/*Tests.class")
}
```

A new `dependencies {}` block at the end of the file (after `dependencyManagement`):
```kotlin
dependencies {
    // Test
    // Org
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

(If the `Test` type does not resolve via Gradle's Kotlin DSL default imports in the precompiled script plugin, add `import org.gradle.api.tasks.testing.Test` at the top — `JavaCompile` already resolves unimported in this file, so `Test` is expected to as well.)

**`.claude/rules/gradle.md`** — add a short "Testing" section (consistent with how the prior subtasks documented their conventions inline): unit tests run on the JUnit Platform via `useJUnitPlatform()` with `include("**/*Tests.class")` on the `test` task in `asapp.java-conventions`; the JUnit Platform launcher is declared as `testRuntimeOnly` because Gradle 9 no longer provides it automatically.

**`TODO.md`** — check off "Migrate unit testing to Gradle" (line 15).

## 6. Placement / altitude rationale

- **Unit-test config → `asapp.java-conventions` (all 7).** Framework enablement and the unit-tier `include` are identical for every module — including the two libs — so they belong in the base plugin, not repeated per leaf. This mirrors where the compiler config already lives.
- **Launcher dependency → `asapp.java-conventions` (all 7).** Every module that runs tests needs it on the test runtime classpath; declaring it once in the base plugin covers all of them. Modules without tests carry an idle test-runtime dependency, which is inert.
- **`test`-task scope, not `withType`.** Deliberately configured on the single `test` task only, so the integration subtask owns the configuration of whatever task(s) it introduces for `*IT`/`*E2EIT` — this subtask does not reach forward into that structure.

## 7. Verification / Definition of Done

- `./gradlew test` succeeds for all 7 modules from a clean state, with **no Docker** running.
- It runs exactly the 99 `*Tests` — spot-checkable per module against the counts in §1 (e.g. `./gradlew :services:asapp-tasks-service:test` reports 26; `:services:asapp-config-service:test` reports 0). Cross-check the total against `mvn test`'s Surefire tally.
- No `*IT`/`*E2EIT` class is executed by `test` (confirmable from the test reports under `build/reports/tests/test` — only `*Tests` classes appear).
- Maven is **untouched** — no `pom.xml` is edited, so `mvn test` is unaffected by construction. Per the standing migration constraint, this is **not** re-verified by running any `mvn` command.

## 8. Out of scope / YAGNI

Integration/e2e execution and structure (separate source sets, `jvm-test-suite`) · JaCoCo coverage · PIT mutation testing (and relocating `pitest-junit5-plugin` off the test classpath) · Spotless · asciidoctor docs · the `spring-boot` Gradle plugin / packaging · parallel test forking (`maxParallelForks`) · `testLogging` console events · JVM args or system properties for tests · moving any test file · any `pom.xml` edit.

### References
- Gradle — [Testing in Java & JVM projects](https://docs.gradle.org/current/userguide/java_testing.html) (JUnit Platform must be enabled explicitly; launcher dependency required; filtering vs include/exclude), [gradle#34512](https://github.com/gradle/gradle/issues/34512) (Gradle 9 requires the launcher on the test runtime classpath)
- Spring — [Boot Gradle plugin: reacting to the java plugin](https://docs.spring.io/spring-boot/gradle-plugin/reacting.html) (the plugin does not configure `useJUnitPlatform()`), [spring-boot#46037](https://github.com/spring-projects/spring-boot/issues/46037) (starters will not bundle `junit-platform-launcher`; declared *not planned*)

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-4-ut`. A single commit is appropriate given the size:

1. `build(gradle): migrate unit testing to Gradle` — the `asapp.java-conventions` test-task block and the `testRuntimeOnly` launcher dependency.

The `.claude/rules/gradle.md` "Testing" note and the `TODO.md` checkbox ride with that commit.

## 10. Post-implementation notes

This spec was written before implementation; the task used the compressed flow, so there is no separate plan file. The core change shipped substantially as designed: the `test`-task block (`useJUnitPlatform()` + `include("**/*Tests.class")`) and the `testRuntimeOnly` JUnit Platform launcher dependency landed in `asapp.java-conventions` exactly as specified in §5.

The canonical implementation is the current state of `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts` and `.claude/rules/gradle.md` on this branch, not this document.

Notable deltas:
- **Engine/launcher contract documented beyond §5** — The `## Testing` section of `.claude/rules/gradle.md` gained a fourth bullet (a post-review improvement, finding N1) that §5 did not specify: the base `asapp.java-conventions` plugin supplies only the JUnit Platform *launcher*, not a JUnit *engine*, so every module that runs tests must bring its own engine via a `spring-boot-starter-*-test` starter. This closes a documentation gap — a pure-library leaf (e.g. `libs/asapp-commons-url`) with no test starter would otherwise get a confusing `compileTestJava` failure if a `*Tests` class were added, because the platform launcher lives in a different file/layer than the engine.
- **The `import org.gradle.api.tasks.testing.Test` contingency was not needed** — §5 hedged that an explicit `import org.gradle.api.tasks.testing.Test` might be required if the `Test` type did not resolve via the Kotlin DSL default imports in the precompiled script plugin. It resolved unimported (like `JavaCompile` already did), so no import line was added to `asapp.java-conventions.gradle.kts`.

For future Gradle test-configuration edits, treat `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts` and the `## Testing` section of `.claude/rules/gradle.md` as the template; this spec is preserved as a record of the original design intent.
