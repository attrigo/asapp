# Gradle Integration Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Gradle run the integration tiers (`*IT` + `*E2EIT`) on the JUnit Platform at parity with Maven Failsafe, wired into `check`, without moving any test file.

**Architecture:** A single `integrationTest` `Test` task reuses the existing `src/test/java` source set and selects both suffixes by class-name pattern (`**/*IT.class`, which also matches `*E2EIT`). `useJUnitPlatform()` is lifted from the unit `test` task into a shared `tasks.withType<Test>().configureEach` rule in the base convention plugin so every tier inherits it. The new task lives in the service convention plugin (services only), is ordered after the unit tier, and is added to `check`.

**Tech Stack:** Gradle 9.6.x (Kotlin DSL), `build-logic` composite build with precompiled convention plugins, JDK 25, Spring Boot 4, JUnit 5 (JUnit Platform), Testcontainers, MockServer.

**Design spec:** `docs/superpowers/specs/v0.5.0/2026-07-18-gradle-integration-testing-design.md`

## Global Constraints

- **Kotlin DSL only**; all shared config goes in `build-logic` convention plugins — never in leaf build scripts or `subprojects {}`/`allprojects {}`.
- **No separate source set, no `jvm-test-suite` plugin** — one `src/test/java`, select by class-name pattern (evaluated-and-rejected alternatives are recorded in the spec §4).
- **One combined `integrationTest` task** runs both `*IT` and `*E2EIT` (Failsafe parity) — not split into integration/e2e tasks.
- **`useJUnitPlatform()` declared once** via `tasks.withType<Test>().configureEach` in `asapp.java-conventions`.
- **`integrationTest` lives in `asapp.service-conventions`** (covers all 5 services; libs get no such task).
- **Selection:** `include("**/*IT.class")` — expected exactly **50 classes** total: authentication 17, config 4, discovery 4, tasks 11, users 14; libs 0.
- **Wiring:** `integrationTest.shouldRunAfter(tasks.named("test"))` and `check.dependsOn(integrationTest)`.
- **Zero new dependencies, zero moved files, no `pom.xml` edit.** Maven stays green by construction.
- **Docker execution is DELEGATED to the developer.** The implementing agent MUST NOT run `./gradlew integrationTest` or `./gradlew check` (they start Testcontainers, which needs Docker). Agent verification is limited to non-Docker commands: `--dry-run`, `tasks`, and the unit `test` task. The full IT run is handed to the developer in Step 8 and the plan waits for their confirmation.
- **Final ordering of build-script blocks is provisional** — the dedicated "Keep Claude Code files in sync" subtask owns the block-ordering rule; place new blocks sensibly here and do not restructure existing ones.
- **Single commit**, only after the developer confirms the Docker-backed run is green.

---

### Task 1: Run the integration tiers under Gradle, wired into `check`

**Files:**
- Modify: `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts` (lift `useJUnitPlatform()` to `withType<Test>`)
- Modify: `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts` (register `integrationTest`, wire into `check`)
- Modify: `.claude/rules/gradle.md` (Testing section)
- Modify: `TODO.md:16` (check off the subtask)

**Interfaces:**
- Consumes (already present, do not re-add): the `test` source set with its runtime classpath carrying the JUnit engine (Spring Boot `*-test` starters), the platform launcher (`testRuntimeOnly("org.junit.platform:junit-platform-launcher")` in `asapp.java-conventions`), Testcontainers, and MockServer.
- Produces: a `integrationTest` `Test` task on every project that applies `asapp.service-conventions` (the 5 services), reachable via `check`; the shared `tasks.withType<Test>().configureEach { useJUnitPlatform() }` rule that also configures it.

---

- [ ] **Step 1: Lift `useJUnitPlatform()` into a shared `withType<Test>` rule**

In `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`, replace this exact block:

```kotlin
// Runs the unit tier on the JUnit Platform; include only *Tests
tasks.named<Test>("test") {
    useJUnitPlatform()
    include("**/*Tests.class")
}
```

with:

```kotlin
// Every test tier runs on the JUnit Platform (JUnit 5 isn't Gradle's default)
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// The unit tier includes only *Tests (Surefire-parity)
tasks.named<Test>("test") {
    include("**/*Tests.class")
}
```

Leave the rest of the file (`java { toolchain }`, `tasks.withType<JavaCompile>`, `dependencyManagement`, the `testRuntimeOnly` launcher in `dependencies {}`) untouched.

- [ ] **Step 2: Register the `integrationTest` task and wire it into `check`**

In `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`, append this block at the **end** of the file (after the closing `}` of the existing `dependencies { }` block):

```kotlin
// The integration tier: reuse the test source set, run *IT and *E2EIT (Failsafe-parity)
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the integration and end-to-end tiers (*IT, *E2EIT)."
    group = "verification"
    val testSourceSet = the<SourceSetContainer>()["test"]
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include("**/*IT.class") // also matches *E2EIT.class
    shouldRunAfter(tasks.named("test"))
}

// mvn verify parity: check runs the integration tier too
tasks.named("check") {
    dependsOn(integrationTest)
}
```

Notes for the implementer:
- `Test` resolved without an explicit import in `asapp.java-conventions`, so it is expected to resolve here too. If compilation in Step 3 fails on an unresolved symbol, add `import org.gradle.api.tasks.testing.Test` and/or `import org.gradle.api.tasks.SourceSetContainer` at the top of the file.
- `the<SourceSetContainer>()["test"]` is deliberate (accessor-independent), because this plugin's `plugins {}` block applies only `asapp.java-conventions`, not `java` directly — the type-safe `sourceSets` accessor may not be generated. Do **not** switch it to `sourceSets["test"]` unless Step 3 proves that accessor resolves.

- [ ] **Step 3: Verify the build scripts compile and the wiring is correct (no Docker)**

Run:

```bash
./gradlew :services:asapp-config-service:check --dry-run
```

Expected: `BUILD SUCCESSFUL`, and the printed task graph lists `test` **before** `integrationTest`, and `integrationTest` **before** `check`, all marked `SKIPPED` (dry-run executes nothing). The relevant lines look like:

```
:services:asapp-config-service:test SKIPPED
:services:asapp-config-service:integrationTest SKIPPED
:services:asapp-config-service:check SKIPPED
```

This single command proves: (a) both convention plugins compile, (b) `integrationTest` is registered, (c) `check` depends on it, (d) it is ordered after `test`. If `integrationTest` is absent or ordered wrong, fix Step 1/Step 2 before continuing.

- [ ] **Step 4: Verify libs get NO `integrationTest` task (no Docker)**

Run:

```bash
./gradlew :libs:asapp-http-clients:integrationTest --dry-run
```

Expected: **FAILURE** — `Task 'integrationTest' not found in project ':libs:asapp-http-clients'.` This confirms the task is scoped to services only (libs apply `asapp.library-conventions`, not `asapp.service-conventions`). A success here would mean the task leaked to libs — revisit Step 2's placement.

- [ ] **Step 5: Verify the unit tier still runs on the JUnit Platform (no Docker)**

The lift in Step 1 must not silently drop `useJUnitPlatform()` — if it did, Gradle would fall back to JUnit 4, discover **zero** Jupiter tests, and still report `BUILD SUCCESSFUL` (a false green). Confirm the unit tier still discovers its tests by counting result files:

```bash
./gradlew :services:asapp-users-service:test --rerun-tasks
ls services/asapp-users-service/build/test-results/test/*.xml | wc -l
```

Expected: `BUILD SUCCESSFUL`, then `28` (one JUnit XML per unit-test class in `asapp-users-service`). A `0` means the platform is no longer enabled — fix Step 1. (Unit tests here are pure Mockito, so this needs no Docker.)

- [ ] **Step 6: Update the Gradle rules**

In `.claude/rules/gradle.md`, replace the entire `## Testing` section — these four bullets:

```markdown
## Testing

- Configure unit-test execution only in `asapp.java-conventions` — never per-leaf
- On `tasks.named<Test>("test")` (not `withType` — one test task today, later tiers get their own), call `useJUnitPlatform()` (JUnit 5 isn't Gradle's default) and `include("**/*Tests.class")` (Surefire-parity — runs only `*Tests`, never `*IT`/`*E2EIT`)
- Declare `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — Gradle 9 no longer auto-provides it and the Spring Boot starters don't bundle it (versionless, BOM-managed)
- The base plugin adds only the platform launcher, not a JUnit engine — each module that runs tests supplies its own via a `spring-boot-starter-*-test` starter
```

with:

```markdown
## Testing

- Configure test execution only in convention plugins — never per-leaf
- Enable the JUnit Platform once for every tier via `tasks.withType<Test>().configureEach { useJUnitPlatform() }` in `asapp.java-conventions` (JUnit 5 isn't Gradle's default); the live rule also configures test tasks registered in other convention plugins
- Unit tier: on `tasks.named<Test>("test")` in `asapp.java-conventions`, set `include("**/*Tests.class")` (Surefire-parity — runs only `*Tests`)
- Integration tier: register a single `integrationTest` `Test` task in `asapp.service-conventions` (services only) that reuses the `test` source set (`testClassesDirs`/`classpath`) and sets `include("**/*IT.class")` — the `*IT` glob also matches `*E2EIT`, mirroring Maven Failsafe running both. Order it with `shouldRunAfter(tasks.named("test"))` and add it to the lifecycle with `check.dependsOn(integrationTest)` (`mvn verify` parity)
- Declare `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — Gradle 9 no longer auto-provides it and the Spring Boot starters don't bundle it (versionless, BOM-managed)
- The base plugin adds only the platform launcher, not a JUnit engine — each module that runs tests supplies its own via a `spring-boot-starter-*-test` starter
```

- [ ] **Step 7: Check off the subtask in `TODO.md`**

In `TODO.md`, replace these two lines (line 16 and its now-consumed note on line 17):

```markdown
    - [ ] Migrate integration testing to Gradle
        - **Note:** lift `useJUnitPlatform()` to a shared `withType<Test>` config rather than repeating it per tier (the unit tier sets it on the `test` task today)
```

with:

```markdown
    - [X] Migrate integration testing to Gradle
```

(The note is removed because it was an execution hint for this task and is now fully satisfied.)

- [ ] **Step 8: Hand the Docker-backed verification to the developer (DELEGATED — do not run)**

Do **not** execute these — Testcontainers requires Docker. Post this to the developer and **wait for their confirmation** before Step 9:

> The non-Docker checks pass. Please run the integration tiers locally (Docker must be running) and confirm green:
>
> ```bash
> ./gradlew integrationTest
> ./gradlew check
> ```
>
> Expected — `integrationTest` runs **50** classes total, per service: authentication **17**, config **4**, discovery **4**, tasks **11**, users **14** (libs have none). `check` runs unit **then** integration for every module, all green. Per-service counts can be confirmed with, e.g.:
>
> ```bash
> ls services/asapp-authentication-service/build/test-results/integrationTest/*.xml | wc -l   # 17
> ```
>
> Reply with the result (or paste any failure) and I'll commit.

If the developer reports a failure, debug via `superpowers:systematic-debugging`, fix, re-run the non-Docker checks (Steps 3–5), and re-delegate Step 8. Only proceed once they confirm green.

- [ ] **Step 9: Commit (single commit, after the developer confirms green)**

```bash
git add build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts \
        build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts \
        .claude/rules/gradle.md \
        TODO.md
git commit -m "build(gradle): migrate integration testing to Gradle" -m "Make \`./gradlew check\` run the integration tiers (\`*IT\` + \`*E2EIT\`) on the JUnit Platform at parity with \`mvn verify\` (Failsafe). A single \`integrationTest\` task reuses the existing \`src/test/java\` source set and selects both suffixes by class-name pattern; \`useJUnitPlatform()\` is lifted to a shared \`withType<Test>\` rule so every tier inherits it." -m "- Lift \`useJUnitPlatform()\` to \`tasks.withType<Test>().configureEach\` in the base convention plugin; the \`test\` task keeps only its \`*Tests\` include
- Register a single \`integrationTest\` task in the service convention plugin, reusing the test source set and including \`**/*IT.class\` (also matches \`*E2EIT\`)
- Order it after the unit tier (\`shouldRunAfter\`) and wire it into \`check\` (\`mvn verify\` parity)
- Document the integration tier and the shared platform rule in the Gradle rules
- Check off the subtask in TODO.md" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

The pre-commit hook runs a Maven Spotless check and Conventional-Commit validation (no Docker) — expect it to pass. Confirm with `git log --oneline -1`.

---

## Self-Review

**1. Spec coverage** — every spec section maps to a step:
- §4 "one source set + name filter", "one combined task", "`include`", "`withType` lift", "placement in service-conventions", "reuse test classpath", "`check.dependsOn`", "`shouldRunAfter`" → Steps 1–2.
- §5 Changes by file: java-conventions → Step 1; service-conventions → Step 2; `gradle.md` → Step 6; `TODO.md` → Step 7.
- §7 Verification: non-Docker wiring/units → Steps 3–5; Docker-backed full run → Step 8 (delegated); Maven untouched → guaranteed by the file list in Step 9 (no `pom.xml`).
- §9 Git workflow: single commit → Step 9.

**2. Placeholder scan** — no TBD/TODO/"handle edge cases"/"similar to". Every edit shows exact before/after text; every command shows expected output.

**3. Type/name consistency** — `integrationTest` is named identically in Steps 2, 3, 4, 6, 8; `the<SourceSetContainer>()["test"]`, `testClassesDirs`, `classpath`, `include("**/*IT.class")`, `shouldRunAfter(tasks.named("test"))`, `check.dependsOn(integrationTest)` are consistent between Step 2 and the Step 6 rule text. Counts (50; auth 17 / config 4 / discovery 4 / tasks 11 / users 14) match between Global Constraints and Step 8.

No gaps found.
