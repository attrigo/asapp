# Gradle integration testing — design spec

**Date**: 2026-07-18
**Status**: Approved
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate integration testing to Gradle" (incl. the note: lift `useJUnitPlatform()` to a shared `withType<Test>` config rather than repeating it per tier)
**Scope**: Make Gradle *run* the integration tiers (`*IT` + `*E2EIT`) at parity with Maven Failsafe — a single `integrationTest` task over the existing `src/test/java` source set, wired into `check`. Also lift `useJUnitPlatform()` off the `test` task into a shared `withType<Test>` block so both tiers inherit it. No test file is moved; no separate source set; no `pom.xml` edit.

## 1. Context

The previous subtask ("Migrate unit testing to Gradle", `docs/superpowers/specs/v0.5.0/2026-07-18-gradle-unit-testing-design.md`) made `./gradlew test` run exactly the 99 `*Tests` on the JUnit Platform, and **deliberately deferred** integration/e2e execution and any structural decision to this subtask — its §4 states that the integration subtask "should declare `useJUnitPlatform()` on its own task where that task is defined." The `*IT`/`*E2EIT` classes already **compile** (since the compilation subtask) but are **never run** by Gradle yet. This spec is that next step.

How the project organizes tests today — all three tiers share one `src/test/java` per module, separated purely by class-name suffix, exactly as Maven Surefire/Failsafe distinguish them:

| Tier | Suffix | Maven plugin / phase | Count | Modules |
|---|---|---|---|---|
| Unit | `*Tests` | Surefire (`test`) | 99 | http-clients 1, authentication 44, tasks 26, users 28 |
| Integration | `*IT` | Failsafe (`integration-test`, `verify`) | 46 | authentication 15, config 4, discovery 4, tasks 10, users 13 |
| E2E | `*E2EIT` | Failsafe (`integration-test`, `verify`) | 4 | authentication 2, tasks 1, users 1 |

**The two non-unit tiers total 50 classes**, distributed per service: authentication 17, config 4, discovery 4, tasks 11, users 14 — libs 0.

What Maven does for these tiers today:
- `maven-failsafe-plugin` is declared in `services/pom.xml` binding the `integration-test` + `verify` goals. Failsafe's default include patterns (`IT*`, `*IT`, `*ITCase`) match **both** `*IT` and `*E2EIT` (the latter ends in `IT`), so `mvn verify` runs all 50 in one phase. There is **no separate e2e phase** — the tier distinction is a source-code convention, not a Maven-lifecycle one.
- These tiers are Spring-context / Testcontainers / MockServer heavy — they require **Docker**.

The three tiers **share test utilities** in `src/test/java` — Object Mothers (`UserMother`, `JwtMother`, …), `TestContainerConfiguration`, `WebMvcTestContext`, assertion helpers, all under a `testutil` package. Verified references: `TestContainerConfiguration`/`WebMvcTestContext`/`*Mother` are used by 37 `*Tests`, 39 `*IT`, and 4 `*E2EIT` classes. This shared-fixture fact is decisive for the source-set decision in §4.

Current Gradle state (JDK 25, Gradle 9.6.x): `asapp.java-conventions` runs the unit `test` task with `useJUnitPlatform()` + `include("**/*Tests.class")`, and declares the `junit-platform-launcher` as `testRuntimeOnly`. No task runs `*IT`/`*E2EIT`; `check` depends only on `test`. Test infra (Testcontainers, MockServer, Spring Boot `*-test` starters) is **already** on the test classpath — it had to be, or the `*IT`/`*E2EIT` sources wouldn't compile — so **no new dependency** is needed to run them.

Convention-plugin hierarchy (relevant to placement): `java-conventions` → applied by both `library-conventions` (libs) and `service-conventions` (config, discovery apply this directly) → `domain-service-conventions` (authentication, tasks, users) applies `service-conventions`. So config/services inherit anything in `service-conventions`; libs do not.

## 2. Goals

- A single `integrationTest` task runs exactly the 50 `*IT` + `*E2EIT` classes on the JUnit Platform, per service, at one-for-one parity with Maven Failsafe (auth 17, config 4, discovery 4, tasks 11, users 14; libs 0).
- `./gradlew check` (and therefore `build`) runs unit **then** integration — matching `mvn verify`. Unit failures fail fast, before Docker spins up.
- `useJUnitPlatform()` is declared **once** in a shared `withType<Test>` block, inherited by every tier (the task's note); the unit `test` task keeps only its tier `include`.
- Zero new dependencies, zero moved files, no `pom.xml` change.
- Settings live at the correct convention-plugin altitude: framework enablement stays base (`java-conventions`); the integration tier lives where integration tests exist (`service-conventions`).

## 3. Non-goals

- **Separate source sets / the `jvm-test-suite` plugin** — evaluated and rejected (see §4); this spec moves no test file and adds no source set.
- **Splitting integration vs e2e into two tasks** — one combined `integrationTest` is chosen (Failsafe parity); a later split remains possible if the tiers ever need to diverge (§4, §8).
- **Coverage (JaCoCo)** — including the Maven ut/it split-and-merge — is its own later subtask ("Migrate coverage reporting"). This spec runs the tests; it does not measure them.
- **Mutation (PIT), formatting (Spotless), API docs (asciidoctor/RestDocs generation), packaging (`spring-boot` plugin), git hooks, CI/release workflow** — each maps to its own later TODO subtask. The CI switch to `./gradlew` is what will invoke this tier on CI; until then CI still runs `mvn verify`.
- **Deep `.claude/rules/gradle.md` restructuring** — the "define block-ordering rule / clean rule file" work is the dedicated "Keep Claude Code files in sync" subtask. This spec only updates the Testing section to stay accurate.
- **Any `pom.xml` edit.** Maven keeps building unchanged until the final "verify full parity, then remove Maven entirely" subtask.
- Test-execution tuning Maven does not have today (parallel forking, `testLogging` events, JVM args).

## 4. Key decisions

Each decision is grounded in the official Gradle docs and the current Gradle 9 / Spring Boot 4 behavior — see References.

| Decision | Choice | Rationale |
|---|---|---|
| Test structure | **One source set (`src/test/java`), select by class-name pattern** — *not* a separate `src/integTest` source set, *not* `jvm-test-suite` | The Gradle docs lean toward a separate source set / the (incubating) `jvm-test-suite` plugin, but their two drivers don't apply here: (a) **divergent dependencies** — this project keeps one shared test-dependency surface, nothing to split; (b) **a `finalizedBy` cleanup task** (shut down a server) — Testcontainers self-manages lifecycle, none needed. Meanwhile the tiers **share `testutil` fixtures** (§1): with one source set that sharing is free; a split would force `integTest` to re-add `sourceSets.test.output` to its compile+runtime classpath (or duplicate fixtures) — friction for zero gain. The Maven-migration guide explicitly sanctions "keep them in the same directory as the unit tests." One source set + name filter is also the exact shape the unit tier already uses (`include("**/*Tests.class")`) and the faithful mirror of Surefire+Failsafe sharing `target/test-classes`. |
| Task granularity | **One combined `integrationTest` running `*IT` + `*E2EIT`** — not separate `integrationTest`/`e2eTest` tasks | Exact Maven-Failsafe parity: both suffixes ran together in one phase, there is no separate e2e lifecycle today. A single `include("**/*IT.class")` selects both (see next row). Splitting is deferred (YAGNI) — it can be introduced later if the tiers must diverge (distinct ordering, tags, or parallelism). |
| Tier selection | **`include("**/*IT.class")`** on the `integrationTest` task | A file-pattern filter over compiled test classes — the direct analog of Failsafe's includes. `*IT.class` matches every class ending in `IT.class`, which covers **both** `AuthenticationRestControllerIT` and `AuthenticationE2EIT` (the latter ends in `…E2EIT` → `…IT.class`). No `*Tests` class matches, so the unit and integration sets stay disjoint with no overlap and no exclude needed. |
| `include` vs `filter { includeTestsMatching }` | **File-pattern `include`** | Consistency: the unit tier uses `include("**/*Tests.class")`; using `filter` for the sibling tier would introduce a second mechanism for two sibling tasks, and switching *both* is out of scope (touches the committed unit tier). Robustness: a module with zero matching classes is a silent no-op under `include`, but **fails** under `filter.includeTestsMatching` ("No tests found") — so a future service scaffolded before it has any `*IT` would break `check` under `filter`. The docs note filtering "supersedes" include/exclude, but include/exclude is fully supported (not deprecated) and is the closer parity to Failsafe. Same call the unit spec made. |
| `useJUnitPlatform()` placement | **Lift to `tasks.withType<Test>().configureEach { }` in `asapp.java-conventions`**; the `test` task keeps only `include("**/*Tests.class")` | The task's own note. Both tiers need the JUnit Platform (Gradle still defaults `Test` to JUnit 4); declaring it once on the live `withType<Test>` rule is DRY and future-proof. `configureEach` is lazy and applies to **every** `Test` task in the project — including `integrationTest` registered later in `service-conventions`, because `java-conventions` is applied to that same project. This supersedes the unit spec's interim `named<Test>("test")` choice, exactly as that spec anticipated. |
| Task placement | **`integrationTest` registered in `asapp.service-conventions`** (all 5 services; libs excluded) | Integration tests exist only in services, and their infra deps already live in `service-conventions` / `domain-service-conventions`. Registering here covers config + discovery (direct) and auth/tasks/users (via `domain-service-conventions`), and keeps libs free of an idle empty task. Framework enablement stays in `java-conventions` (base) because it is identical for every tier and module. |
| Reusing the test classpath | **`testClassesDirs = test.output.classesDirs`, `classpath = test.runtimeClasspath`** | The task runs the already-compiled test classes and reuses the test runtime classpath, which already carries the JUnit engine (via Spring Boot `*-test` starters), the platform launcher (`testRuntimeOnly`), Testcontainers, and MockServer. Hence **no new dependency**. |
| Lifecycle wiring | **`check.dependsOn(integrationTest)`** | `mvn verify` parity — `./gradlew check`/`build` runs the integration tier too. Gradle's up-to-date checking skips it when nothing it depends on changed, so incremental builds stay fast (the 0.5.0 speed goal). |
| Ordering | **`integrationTest.shouldRunAfter(test)`** (not `mustRunAfter`) | Runs the fast unit tier first so a cheap failure aborts before Docker starts; `shouldRunAfter` (per the docs) preserves Gradle's freedom to parallelize when it can. Ordering only applies when both are scheduled (e.g. under `check`), which is the intended case. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all modules) — lift the platform enablement. Replace the current unit `test` block:

```kotlin
// before
tasks.named<Test>("test") {
    useJUnitPlatform()
    include("**/*Tests.class")
}
```
```kotlin
// after
// Every test tier runs on the JUnit Platform (JUnit 5 isn't Gradle's default)
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// The unit tier: include only *Tests (Surefire-parity)
tasks.named<Test>("test") {
    include("**/*Tests.class")
}
```

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** (5 services) — register the integration tier and wire it into `check`:

```kotlin
// The integration tier: reuse the test source set, run *IT and *E2EIT (Failsafe-parity)
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the integration and end-to-end tiers (*IT, *E2EIT)."
    group = "verification"
    val testSourceSet = the<SourceSetContainer>()["test"]
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include("**/*IT.class")               // also matches *E2EIT.class
    shouldRunAfter(tasks.named("test"))
}

// mvn verify parity: check runs the integration tier too
tasks.named("check") {
    dependsOn(integrationTest)
}
```

Contingencies (resolve at implementation, mirroring how the unit spec hedged its `import Test`):
- If `Test` / `SourceSetContainer` / `register`/`named` accessors don't resolve via the Kotlin DSL default imports in the precompiled script plugin, add the explicit imports (`org.gradle.api.tasks.testing.Test`, `org.gradle.api.tasks.SourceSetContainer`). `Test` resolved unimported in `java-conventions`, so it is expected to here too.
- If the `sourceSets["test"]` type-safe accessor is unavailable in `service-conventions` (its `plugins {}` block applies only `asapp.java-conventions`, not `java` directly), the `the<SourceSetContainer>()["test"]` generic form shown above is the deliberate, accessor-independent choice — consistent with how `java-conventions` uses `extensions.getByType(VersionCatalogsExtension::class.java)` rather than the `libs` accessor.

**`.claude/rules/gradle.md`** — update the Testing section to match: `useJUnitPlatform()` now lives on `tasks.withType<Test>().configureEach` in `asapp.java-conventions` (both tiers inherit it); the unit `test` task keeps `include("**/*Tests.class")`; the integration tier is a single `integrationTest` task in `asapp.service-conventions` reusing the `test` source set with `include("**/*IT.class")` (matches `*IT` and `*E2EIT`), `shouldRunAfter(test)`, and `check.dependsOn(integrationTest)`. Correct the now-stale note that says `useJUnitPlatform()` sits on `named<Test>("test")` "not `withType` — one test task today." (Deeper rule-file cleanup remains the "Keep Claude Code files in sync" subtask.)

**`TODO.md`** — check off "Migrate integration testing to Gradle" (line 16).

## 6. Placement / altitude rationale

- **`useJUnitPlatform()` → `asapp.java-conventions`, `withType<Test>`.** Platform enablement is identical for every tier and every module; the base plugin is the single correct altitude, and `withType` lets one declaration serve `test` and `integrationTest` alike.
- **`integrationTest` task + `check` wiring → `asapp.service-conventions`.** Only services have `*IT`/`*E2EIT`; co-locating the task with the service infra deps keeps libs free of an empty task. All 5 services are covered (config/discovery directly, the 3 domain services transitively).
- **Launcher / engine unchanged.** The launcher (`testRuntimeOnly`, base) and engine (Spring Boot `*-test` starters, service level) are already on `test.runtimeClasspath`, which `integrationTest` reuses — nothing to add.

## 7. Verification / Definition of Done

- `./gradlew integrationTest` (with **Docker running**) succeeds for all 5 services and runs exactly the 50 classes — spot-checkable per service against §1 counts (e.g. `:services:asapp-authentication-service:integrationTest` = 17; `:services:asapp-config-service:integrationTest` = 4; `:services:asapp-tasks-service:integrationTest` = 11). Total cross-checks against `mvn verify`'s Failsafe tally.
- No `*Tests` class is executed by `integrationTest`, and no `*IT`/`*E2EIT` by `test` — confirmable from the reports under `build/reports/tests/`.
- `./gradlew check` runs unit then integration (order visible in the build), and fails fast if a unit test fails. Libs' `check` still runs unit only (no `integrationTest` task).
- `./gradlew build` (which depends on `check`) runs both tiers. Consequence: `build`/`check` now require **Docker** (parity with `mvn verify`/`install`); `./gradlew test` and `./gradlew assemble` stay Docker-free for a fast inner loop (the analog of `mvn package`, which never ran Failsafe).
- Maven is **untouched** — no `pom.xml` edited, so `mvn verify` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.
- Running the full Docker-backed suite is the expensive step — confirm before executing it during implementation.

## 8. Out of scope / YAGNI

Separate source sets / `jvm-test-suite` · a split `e2eTest` task · JaCoCo coverage (and the ut/it split-and-merge) · PIT mutation · Spotless · asciidoctor/RestDocs generation · the `spring-boot` Gradle plugin / packaging · CI & release workflow migration · parallel test forking (`maxParallelForks`) · `testLogging` console events · JVM args / system properties for tests · moving any test file · any `pom.xml` edit.

### References
- Gradle — [Testing in Java & JVM projects](https://docs.gradle.org/current/userguide/java_testing.html) (enable the JUnit Platform explicitly; a custom `Test` task reuses a source set via `testClassesDirs`/`classpath`; `withType<Test>().configureEach`; `shouldRunAfter`; `check.dependsOn`; filtering vs include/exclude)
- Gradle — [Migrating from Maven, §Integration tests](https://docs.gradle.org/current/userguide/migrating_from_maven.html#migmvn:integration_tests) (Failsafe → Gradle source sets; explicitly allows keeping ITs "in the same directory as the unit tests")
- Gradle — [JVM Test Suite plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html) (each suite gets its own source set by default; not wired into `check` automatically; still incubating) — evaluated, not adopted

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-5-it`. A single commit is appropriate given the size:

1. `build(gradle): migrate integration testing to Gradle` — the `withType<Test>` lift in `asapp.java-conventions` and the `integrationTest` task + `check` wiring in `asapp.service-conventions`.

The `.claude/rules/gradle.md` Testing update and the `TODO.md` checkbox ride with that commit.
