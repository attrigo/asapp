# Gradle coverage reporting — design spec

**Date**: 2026-07-18
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate coverage reporting to Gradle" (line 17; no TODO notes attached)
**Scope**: Reproduce Maven's JaCoCo coverage reporting under Gradle at parity — the same three-report set (unit / integration / merged) per module, off by default, report-only (no thresholds). Apply the `jacoco` plugin in the convention plugins, pin the tool version, and register the integration and merged report tasks. No `pom.xml` edit, no CI/README change, no coverage enforcement.

## 1. Context

The previous subtask ("Migrate integration testing to Gradle", `docs/superpowers/specs/v0.5.0/2026-07-18-gradle-integration-testing-design.md`) made `./gradlew check` run the unit tier (`test`, `*Tests`) then the integration tier (`integrationTest`, `*IT` + `*E2EIT`) at Failsafe parity. Both test tasks now exist and run; nothing measures their coverage yet. This spec adds that measurement.

**What Maven does today.** JaCoCo (`jacoco-maven-plugin` `0.8.13`) is configured in the two intermediate poms and referenced by each leaf, but is **off by default** — `jacoco.skip=true`, flipped to `false` only by the `-Pfull` profile. CI (`ci.yml`, `-Pci`) does **not** enable it; only the release workflow (`mvn install -Pfull`) does. It is **report-only** — no `check` goal, no thresholds. The report set differs by module group:

| Module group | Maven executions | Reports produced |
|---|---|---|
| **Libs** (`asapp-commons-url`, `asapp-http-clients`) | `prepare-agent` → `report` | `jacoco-ut` (unit only) |
| **Services** (×5) | `prepare-agent`, `report` (ut); `prepare-agent-integration`, `report` (it); `merge` + `report` (aggregate) | `jacoco-ut`, `jacoco-it`, `jacoco-aggregate` (merged ut+it) |

The exec data lands under `target/coverage-reports/` (`jacoco-ut.exec`, `jacoco-it.exec`, merged `aggregate.exec`); HTML reports land under `target/site/jacoco-ut|jacoco-it|jacoco-aggregate/`. The README surfaces exactly one of them — `target/site/jacoco-aggregate/index.html` — as *the* coverage artifact; the separate ut/it reports are never linked. `asapp-commons-url` has **no test sources**, so its unit report is empty in practice.

**Current Gradle state** (Gradle 9.6.1, JDK 25). `asapp.java-conventions` (all 7 modules) applies `java` + `io.spring.dependency-management`, runs `test` (unit) on the JUnit Platform, and reads the version catalog via `extensions.getByType(VersionCatalogsExtension::class.java).named("libs")`. `asapp.service-conventions` (5 services) registers the `integrationTest` `Test` task (reuses the `test` source set, `maxHeapSize = "1g"`, `include("**/*IT.class")`, `shouldRunAfter(test)`) and wires `check.dependsOn(integrationTest)`. No JaCoCo anywhere yet.

**Convention-plugin hierarchy** (placement-relevant): `java-conventions` → applied by `library-conventions` (libs) and `service-conventions` (config, discovery apply it directly) → `domain-service-conventions` (auth, tasks, users). So anything in `java-conventions` reaches all 7 modules; anything in `service-conventions` reaches the 5 services.

## 2. Goals

- The same three-report set Maven produces: **libs → a unit report; each service → unit, integration, and merged (unit+integration) reports** — the direct analogs of `jacoco-ut`, `jacoco-it`, `jacoco-aggregate`.
- **Off by default**: a normal `./gradlew build` / `check` generates **no** coverage report; coverage is produced only when a report task is named (the flag-free analog of `-Pfull`).
- **Report-only**, no thresholds or `check`-time enforcement — Maven has none.
- Coverage runs correctly on the **Java 25** toolchain.
- Settings live at the correct convention-plugin altitude (unit report at base, integration/merged at service level), consistent with where the test tiers are defined.
- Zero `pom.xml` edits; Maven's coverage keeps working unchanged until the final removal subtask.

## 3. Non-goals

- **A one-command "full build".** The single lifecycle task that bundles `build` + coverage + formatting + API docs + javadoc/sources (the true `mvn install -Pfull` equivalent) is deferred to its own TODO subtask ("Migrate the full build to Gradle"), added by this change. This spec produces the report **tasks**; it does not aggregate them behind one name.
- **Coverage thresholds / `jacocoTestCoverageVerification`.** Maven enforces nothing; neither do we.
- **Cross-module aggregation** (`jacoco-report-aggregation`). Maven's "aggregate" is a *per-service* merge of that service's ut+it — not a build-wide roll-up. This spec mirrors the per-module merge only.
- **XML/CSV report formats.** Deferred — see §4 (no machine consumer exists today).
- **CI / release workflow, README / build docs, `pom.xml`.** Each is its own later subtask. CI still runs `mvn`; the README still documents Maven paths.
- **Mutation (PIT), formatting (Spotless), packaging, javadoc/sources jars, API docs** — separate subtasks.

## 4. Key decisions

Each is grounded in the official Gradle and JaCoCo docs and current Gradle 9.6 / JDK 25 behavior — see §9 References.

| Decision | Choice | Rationale |
|---|---|---|
| Plugin + altitude | **Apply the core `jacoco` plugin in `asapp.java-conventions`** (all 7 modules) | Coverage measurement is identical for every module, and the plugin auto-creates the unit report task (`jacocoTestReport`) wherever it is applied — so applying it at the base gives libs their unit report for free and gives services the same base the integration/merged tasks build on. Same altitude as the compiler and unit-test config. |
| Tool version | **Pin `toolVersion = "0.8.14"`** via a `[versions]` catalog entry | JaCoCo `0.8.13` (Maven's pin) has only *experimental* Java 25 support; **`0.8.14` makes it official** (ASM 9.10.1). The toolchain is Java 25, so this matters. `0.8.14` is also already Gradle 9.6's bundled default, so this is a de-facto no-op against the default — but pinning explicitly honors the project's pin-everything philosophy and survives future wrapper upgrades. The bump from `0.8.13` carries only Java-version and Kotlin-filter changes — no behavioral risk for this Java project. |
| Report structure | **Faithful three-report mirror** — `jacocoTestReport` (unit), `jacocoIntegrationTestReport` (integration), `jacocoAggregateReport` (merged) | Chosen over a single collapsed report to preserve exact parity with Maven's `jacoco-ut` / `jacoco-it` / `jacoco-aggregate` set. The unit task is the plugin's auto-created one (base, all 7); the other two are registered in `service-conventions` (5 services). |
| Merged-report execution data | **`executionData(tasks.withType<Test>())`** on the aggregate; **`executionData(integrationTest.get())`** on the integration report; unit report keeps the plugin default (`test` only) | `JacocoReport.executionData(TaskCollection)` includes only tasks carrying a `JacocoTaskExtension` and silently ignores the rest, so `withType<Test>()` lazily yields exactly `test` + `integrationTest` per service — the merge is free, no `.exec` file-path juggling like Maven's explicit `merge` goal. |
| Report-to-test wiring | **Each report `dependsOn` its underlying test task(s)** | The `jacoco` plugin deliberately does **not** make `jacocoTestReport` depend on `test`. Without the `dependsOn`, naming a report task would report stale/empty data. Wiring it means one command (`./gradlew jacocoAggregateReport`) runs the tests then the report. |
| Activation | **Reports off the `check`/`build` path (opt in by naming the task); JaCoCo agent left auto-attached — no flag** | Mirrors Maven's "off by default" for the *report* without a `-Pcoverage` flag: `./gradlew build` produces no report; you name the report task to get one. The agent is auto-attached to every `Test` task by the plugin; its cost only materializes when a test task actually executes, and Gradle skips up-to-date test tasks — so the daily loop rarely pays it. **Rejected**: gating the agent behind `-Pcoverage` — the flag then leaks onto the report command (a report task needing a "give me coverage" flag is friction), and the clean report ergonomics outweigh shaving a few percent off `./gradlew test`. |
| Enforcement | **None** — no `jacocoTestCoverageVerification`, no `check` wiring | Report-only parity; Maven enforces nothing. |
| Report formats | **HTML only** (Gradle default); XML/CSV off | Maven's `report` goal emitted HTML+XML+CSV, but no machine consumer (SonarQube, Codecov) exists in the repo and the README only ever surfaced the HTML. YAGNI: enable XML when a consumer appears (already a backlog concern). This is the one deliberate format divergence from Maven, made explicit via a `reports {}` block. |
| Source set for custom reports | **`sourceSets(project.the<SourceSetContainer>()["main"])`** | The accessor-independent lookup, consistent with how `integrationTest` resolves the test source set in the same file; the `sourceSets`/`main` type-safe accessor may not resolve inside the precompiled `service-conventions` script plugin. |

## 5. Changes by file

**`gradle/libs.versions.toml`** — add the tool version under `# Build` (origin `## Org`, after the existing `## Spring` entry; versions-only, no `[libraries]` entry since it is consumed via `toolVersion`, exactly like the `spring-boot` / `jackson-bom` BOM versions):

```toml
# Build
## Spring
spring-dependency-management = "1.1.7"
## Org
jacoco = "0.8.14"
```

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all 7 modules) — apply the plugin, pin the tool, and wire the unit report to its test task.

```kotlin
plugins {
    java
    jacoco                                   // NEW
    id("io.spring.dependency-management")
}
```
After the existing `val libs = …` line (needs the catalog):
```kotlin
// Pin the coverage tool: 0.8.14 gives official Java 25 support (0.8.13 is experimental); also Gradle 9.6's default
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}
```
Alongside the other task configuration:
```kotlin
// Unit-tier coverage report (Maven jacoco-ut analog). The plugin does not wire it to run the tests — do so here.
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}
```

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** (5 services) — register the integration and merged reports, after the `integrationTest` / `check` block:

```kotlin
// Integration-tier coverage report (Maven jacoco-it analog)
tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    description = "Generates a code coverage report for the integration tier (*IT, *E2EIT)."
    group = "verification"
    dependsOn(integrationTest)
    executionData(integrationTest.get())
    sourceSets(project.the<SourceSetContainer>()["main"])
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}

// Merged unit + integration coverage report (Maven jacoco-aggregate analog)
tasks.register<JacocoReport>("jacocoAggregateReport") {
    description = "Generates a merged unit and integration code coverage report."
    group = "verification"
    dependsOn(tasks.withType<Test>())
    executionData(tasks.withType<Test>())          // test + integrationTest (only instrumented tasks are included)
    sourceSets(project.the<SourceSetContainer>()["main"])
    reports {
        html.required = true
        xml.required = false
        csv.required = false
    }
}
```

Contingencies (resolve at implementation, mirroring how prior subtasks hedged their imports):
- If `JacocoReport` does not resolve via the Kotlin DSL default imports in the precompiled script plugins, add `import org.gradle.testing.jacoco.tasks.JacocoReport`. (`SourceSetContainer` and `Test` already resolve in `service-conventions`.)
- `asapp-commons-url` has **no test sources**, so its `jacocoTestReport` may find no execution data. Maven's `report` goal skips a missing data file gracefully. Confirm Gradle degrades the same way (empty/skipped, not failed); if it errors on missing exec, guard that one report — but do **not** special-case it in the shared plugin unless it actually breaks (parity with Maven, which applies the same plugin to it without incident).

**`.claude/rules/gradle.md`** — add a short "Coverage" section: the `jacoco` plugin is applied in `asapp.java-conventions` with `toolVersion` pinned from the catalog (`0.8.14`, for Java 25); the unit report (`jacocoTestReport`, all modules) is the plugin's own task with `dependsOn(test)` added; the integration (`jacocoIntegrationTestReport`) and merged (`jacocoAggregateReport`) reports are registered in `asapp.service-conventions` (5 services), the merged one reading `executionData(tasks.withType<Test>())`; reports are off the `check`/`build` path (opt in by naming the task), HTML-only, and there is no coverage enforcement. (Deeper rule-file restructuring remains the "Keep Claude Code files in sync" subtask.)

**`TODO.md`** — two edits: (1) at implementation time, check off "Migrate coverage reporting to Gradle" (line 17); (2) **now**, add a new subtask "Migrate the full build to Gradle" after "Migrate packaging to Gradle", capturing the deferred `-Pfull`-equivalent umbrella (see §3).

## 6. Placement / altitude rationale

- **`jacoco` plugin + `toolVersion` + `jacocoTestReport` wiring → `asapp.java-conventions` (all 7).** The plugin, the pinned tool, and the unit report are identical for every module; the base plugin is the single correct altitude, mirroring where the compiler and unit-test config already live. Libs get their unit report from the plugin's auto-created task with no service-level involvement.
- **`jacocoIntegrationTestReport` + `jacocoAggregateReport` → `asapp.service-conventions` (5 services).** They depend on `integrationTest`, which exists only in services and is registered in this same plugin. Co-locating keeps libs free of integration/merged report tasks they have no tests for.
- **Agent unchanged.** The plugin attaches the JaCoCo agent to every `Test` task (`test`, `integrationTest`) automatically; no per-tier or per-module wiring is needed.

## 7. Verification / Definition of Done

- `./gradlew jacocoAggregateReport` for a service (Docker running) runs `test` + `integrationTest` then emits a merged HTML report at `build/reports/jacoco/jacocoAggregateReport/html/index.html`; `jacocoIntegrationTestReport` emits the integration-only report; `jacocoTestReport` emits the unit-only report at `build/reports/jacoco/test/html/index.html`.
- **Report set matches Maven per module group**: libs expose only `jacocoTestReport`; each of the 5 services exposes all three — spot-checkable via `./gradlew :services:asapp-tasks-service:tasks --group verification`.
- **Off by default**: `./gradlew build` and `./gradlew check` produce **no** coverage report (no report task in the graph).
- The full build with coverage is the explicit `./gradlew clean build jacocoTestReport jacocoIntegrationTestReport jacocoAggregateReport` (Docker required) — every module's applicable reports generated in one invocation; this is the interim until the `fullBuild` umbrella subtask lands.
- Coverage runs cleanly on **Java 25** (the `0.8.14` reason for being) — no "unsupported class file major version".
- `asapp-commons-url` (no tests) does not break its `jacocoTestReport` (empty/skipped, matching Maven).
- Maven is **untouched** — no `pom.xml` edited, so `mvn … -Pfull` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.
- Generating reports is the expensive step (it drives the Docker-backed integration tier) — confirm before running it during implementation.

## 8. Out of scope / YAGNI

A one-command `fullBuild` / `-Pfull` umbrella (its own new subtask) · coverage thresholds / verification · cross-module (`jacoco-report-aggregation`) roll-up · XML/CSV report formats · renaming report output dirs to Maven's `jacoco-ut|it|aggregate` names · wiring any report into `check`/`build` · gating the agent behind a flag · CI / release / README / build-doc updates · any `pom.xml` edit.

## 9. References

- Gradle — [The JaCoCo Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) (`jacocoTestReport` is auto-created and does **not** depend on `test`; not wired into `check`; agent auto-attached to `Test` tasks via `JacocoTaskExtension`; `toolVersion`; `reports {}` formats)
- Gradle — [`JacocoReport` DSL](https://docs.gradle.org/current/dsl/org.gradle.testing.jacoco.tasks.JacocoReport.html) (`executionData(TaskCollection)` includes only tasks with a `JacocoTaskExtension`, ignoring others; `sourceSets(...)`; `reports`)
- Gradle — [Upgrading within Gradle 9.x](https://docs.gradle.org/current/userguide/upgrading_version_9.html) (default JaCoCo tool version is `0.8.14`)
- JaCoCo — [Change History](https://www.jacoco.org/jacoco/trunk/doc/changes.html) & [Official support for Java 25 (#1933)](https://github.com/jacoco/jacoco/issues/1933) (`0.8.13` = experimental Java 25; `0.8.14` = official, ASM 9.10.1)

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle`. A single commit is appropriate given the size:

1. `build(gradle): migrate coverage reporting to Gradle` — the catalog `jacoco` version, the plugin + `toolVersion` + `jacocoTestReport` wiring in `asapp.java-conventions`, and the two report tasks in `asapp.service-conventions`.

The `.claude/rules/gradle.md` Coverage section and the `TODO.md` checkbox ride with that commit. The new "Migrate the full build to Gradle" TODO subtask is added with this design commit (housekeeping, ahead of implementation).

## 11. Post-implementation notes

Implemented on `build/replace-maven-with-gradle` via the compressed flow (no separate plan file). The design shipped substantially as written — the `jacoco` plugin + pinned `0.8.14` `toolVersion` + `jacocoTestReport` `dependsOn(test)` in `asapp.java-conventions`, and the `jacocoIntegrationTestReport` + `jacocoAggregateReport` tasks in `asapp.service-conventions`, all off the `check`/`build` path with the agent auto-attached (no flag). The canonical implementation is the current state of the convention plugins and `.claude/rules/gradle.md`, not this document.

Notable deltas:

- **The merged report references the two `Test` tasks explicitly — corrects §4/§5's `executionData(tasks.withType<Test>())`.** The designed `executionData(tasks.withType<Test>())` / `dependsOn(tasks.withType<Test>())` on `jacocoAggregateReport` failed at task realization with `DefaultTaskCollection#all(Action) on task set cannot be executed in the current context`: `JacocoReport.executionData(TaskCollection)` internally registers a `.all { }` callback on the collection, which Gradle forbids while the task set is being iterated (e.g. by the `tasks` report or during graph configuration). The shipped code references the tiers directly — `val test = tasks.named<Test>("test"); dependsOn(test, integrationTest); executionData(test.get(), integrationTest.get())`. The integration report was already explicit (`executionData(integrationTest.get())`) and needed no change. The `.claude/rules/gradle.md` Coverage section documents this gotcha.

- **`asapp-commons-url` (no tests) skips its report gracefully — resolves §5's contingency.** `./gradlew :libs:asapp-commons-url:jacocoTestReport` reports `SKIPPED` (no execution data), exactly as Maven's `report` goal skips a missing data file. No per-lib guard was needed.

- **`jacocoTestCoverageVerification` is auto-created but left idle.** The `jacoco` plugin also registers a `jacocoTestCoverageVerification` task on every module. It is intentionally **not** wired into `check` (report-only parity, no thresholds), so it never runs — noted to pre-empt confusion when it appears in the verification group.

- **`import org.gradle.testing.jacoco.tasks.JacocoReport` was added proactively to both convention plugins.** build-logic compiles with it; whether the type also resolves via the Kotlin DSL default imports was not separately tested (the explicit import is harmless and self-documenting).

Verified without Docker: build-logic compiles; the three report tasks register on services and only `jacocoTestReport` on libs; `./gradlew :services:asapp-tasks-service:jacocoTestReport` runs the unit tier and emits `build/reports/jacoco/test/html/index.html` from `build/jacoco/test.exec`; `jacocoAggregateReport --dry-run` schedules `test` + `integrationTest`; a plain `build` schedules no report. The Docker-backed end-to-end run of `jacocoIntegrationTestReport` / `jacocoAggregateReport` (which execute the integration tier) is left to the developer.
