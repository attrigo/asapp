# Gradle full build — design spec

**Date**: 2026-07-27
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate the full build to Gradle" (line 23), with its three attached notes (lines 24–26).
**Scope**: Aggregate the ten migrated build stages behind one lifecycle task, `fullBuild` — the `mvn clean install -Pfull` / `mvn clean verify -Pfull` equivalent consumed later by the running-locally, CI, release, and build-documentation subtasks. Registered per module in `asapp.java-conventions` and extended by each archetype plugin with exactly the artifacts Maven's `-Pfull` activated there, so `./gradlew fullBuild` fans out across all 7 modules through Gradle's task selector. No root aggregator, no flags, no new plugin, no `pom.xml` or source edit.

## 1. Context

Eleven prior subtasks — the project skeleton plus ten build stages (dependency management, compilation, unit testing, integration testing, coverage reporting, mutation testing, formatting checks, API documentation, javadoc/sources jars, packaging) — moved the build onto Gradle 9.x / JDK 25. Each produced invokable tasks and deliberately deferred the umbrella here; the later specs name "the `full`/`fullBuild` umbrella" in their non-goals. This subtask adds the umbrella and nothing else.

**What Maven's "full build" actually is.** The `full` profile flips exactly four skip flags to `false`: `jacoco.skip`, `maven.javadoc.skip`, `maven.source.skip`, `spotless.check.skip`. Everything else in `mvn install -Pfull` is the ordinary lifecycle, which runs on every `mvn verify` regardless of profile:

| Maven mechanism | Gated by `-Pfull`? | Gradle equivalent | Reaches `fullBuild` via |
|---|---|---|---|
| compile · surefire · failsafe · repackage · build-info · git.properties | no — every build | `build` (`assemble` + `check`) | `dependsOn("build")` |
| `spotless:check` | **yes** (`skip=true` by default) | `spotlessCheck` | transitively, through `check` |
| `jacoco` reports (ut / it / aggregate) | **yes** | `jacocoTestReport` · `jacocoIntegrationTestReport` · `jacocoMergedReport` | explicit edge |
| `maven-javadoc-plugin` · `maven-source-plugin` | **yes** | `javadocJar` · `sourcesJar` | explicit edge |
| `asciidoctor:process-asciidoc` | **no** — bound to `post-integration-test`, always on | `asciidoctor` (opt-in since the API-docs subtask) | explicit edge |
| `pitest` | no phase binding — manual only | `pitest` | **not** in the umbrella |
| `cyclonedx` SBOM | no — every build | *missing under Gradle* | **not** in the umbrella (§8) |

**Which modules each flag reaches.** Maven's `<pluginManagement>` configures without activating; a child must re-declare a plugin in its own `<build><plugins>`. So `-Pfull`'s reach is per-module:

| Module group | Coverage | Javadoc + sources | API guide | Formatting |
|---|---|---|---|---|
| `libs/asapp-commons-url`, `libs/asapp-http-clients` | unit only | ✅ | ❌ | ✅ |
| `services/asapp-config-service`, `services/asapp-discovery-service` | **none** — neither declares `jacoco` | ❌ | ❌ | ✅ |
| `services/asapp-{authentication,tasks,users}-service` | unit + integration + merged | ✅ | ✅ | ✅ |

So `-Pfull` for the two infra services means nothing beyond the formatting check — their full build *is* their ordinary build.

**Current Gradle state** (Gradle 9.6.1, JDK 25). `check` already runs `test` (`*Tests`) → `integrationTest` (`*IT`, `*E2EIT`) → `spotlessCheck`; `build` adds `assemble`, which the Spring Boot plugin wires to `bootJar` for the 5 services (§11 References). Opt-in tasks exist and are individually invokable: `jacocoTestReport` (all 7), `jacocoIntegrationTestReport` + `jacocoMergedReport` (5 services), `asciidoctor` (3 domain services), `javadocJar` + `sourcesJar` (2 libs + 3 domain services), `pitest` (3 domain services). No task aggregates them; the root `build.gradle.kts` holds only a placeholder comment. `gradle.properties` has `org.gradle.caching=true`, `org.gradle.parallel=false` (WSL), and the configuration cache deferred but intended.

**Convention-plugin hierarchy** (placement-relevant): `java-conventions` (all 7) ← `library-conventions` (2 libs) and `service-conventions` (5 services, applied directly by config + discovery) ← `domain-service-conventions` (3 domain services).

## 2. Goals

- **One command**: `./gradlew fullBuild` replaces `mvn clean install -Pfull` / `mvn clean verify -Pfull` for the whole build.
- **Parity of reach**: umbrella membership mirrors Maven's per-module `-Pfull` activation exactly — including `fullBuild` ≡ `build` for config and discovery.
- **Per-module addressability**: `./gradlew :services:asapp-tasks-service:fullBuild` is the analog of `mvn install -Pfull -pl services/asapp-tasks-service`.
- **Flag-free**, like every migrated stage before it: no `-Pfull`, no `-Pcoverage`.
- **Configuration-cache and Isolated-Projects safe**: no cross-project task references, no `subprojects {}` / `allprojects {}`.
- **Correct altitude**: convention plugins only; the root build script stays untouched.
- **Cheap re-runs**: a `build` followed by a `fullBuild` (or the reverse) must re-run no tests — only the reports.
- Zero `pom.xml` edits, zero source edits; `mvn -Pfull` keeps working until the removal subtask.

## 3. Non-goals

- **Gating the JaCoCo agent** — resolved in §4 as *accept always-on*; no code changes, the note at line 25 is retired by that decision.
- **`clean` semantics** — `fullBuild` never cleans (§4); a from-scratch build stays a separate `./gradlew clean` invocation.
- **Consuming the umbrella.** Swapping the CI workflow (line 31), release workflow (line 33), README / build documentation (line 34), git hooks (line 29), local runs (line 27), and Docker images (line 28) over to it are their own subtasks. This spec only creates the task and leaves forward notes.
- **`pitest`, `bootBuildImage`, `bootRun`** — none was part of `-Pfull`.
- **Publishing** — no `maven-publish`; `install` maps to `build` (§4).
- **The CycloneDX SBOM gap** — a `build`-level parity gap, not an umbrella one; promoted to its own TODO subtask (§8).
- **Cross-module coverage aggregation** (`jacoco-report-aggregation`) — Maven's "aggregate" was a per-service ut+it merge, already mirrored.
- **De-duplicating the archetype blocks** — deferred to "Clean Gradle files" (line 35).

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Aggregation mechanism | **A per-project lifecycle task, registered once in `asapp.java-conventions` and extended by each archetype plugin** | Gradle resolves an unqualified command-line task name as a *task selector* that runs in every subproject defining it (§11 References), so `./gradlew fullBuild` from the root reaches all 7 modules with no root task and no cross-project wiring. Gradle's own guidance for lifecycle tasks is exactly this: `tasks.register()` a name, wire the targets with `dependsOn()`, and let Gradle work out the rest (§11). |
| Rejected: root aggregator | **No `fullBuild` in the root `build.gradle.kts`** | Naming subprojects' tasks (or mapping over `subprojects`) is cross-project configuration — forbidden by the project's own Gradle rules, incompatible with Isolated Projects, and it needs an edit every time a module is added. A no-op root task is worse: `./gradlew :fullBuild` would silently succeed having built nothing. |
| Rejected: no task at all | **Rejected** | The four downstream consumers would each spell out a seven-task command line that drifts silently, and the TODO note asks for one lifecycle task. |
| Coverage reach | **Wire the coverage reports at the 5-module Maven altitude — `library-conventions` and `domain-service-conventions` — not at `java-conventions` / `service-conventions`** | `-Pfull` produced coverage only for the 2 libs + 3 domain services; config and discovery never declared `jacoco`. Wiring `jacocoIntegrationTestReport` / `jacocoMergedReport` at the service altitude would hand the two infra services (2–3 main classes each) reports Maven never produced. Developer decision: keep the umbrella at Maven's reach; the tasks stay registered where they are and remain individually invokable. |
| `jacocoTestReport` off the base altitude | **Wire it in the two archetypes too, not in `java-conventions`** | Wiring it at the base looks identical today only because config and discovery have no `*Tests` — their `test` task is `NO-SOURCE`, so no exec data exists and `JacocoReport`'s `onlyIf("Any of the execution data files exists")` skips the report (§11 References). That parity would be *incidental* and would break silently the day someone adds a unit test to an infra service. The 5-module altitude makes it intentional — and it is the same activation set `javadocJar`/`sourcesJar` already use. |
| Rejected: relocating the report *tasks* | **Rejected** | Moving the `jacocoIntegrationTestReport` / `jacocoMergedReport` registration down to `domain-service-conventions` would re-open a decision that shipped in the coverage subtask and churn `gradle.md` plus that spec, for no gain over restricting the umbrella. |
| `spotlessCheck` | **Not listed — it arrives transitively through `check` → `build`** | Per the note at line 26 it stays a plain gate, never re-gated as optional; a second explicit edge would be deduped by Gradle anyway. The task *description* names the formatting check, so `./gradlew <module>:tasks` still advertises that the umbrella covers it. |
| JaCoCo agent | **Accept it auto-attached to every `Test` task — no gating, no code** | Gradle exposes the agent as an `@Nested @Optional` input that returns `null` when disabled (§11 References), so toggling it splits each test tier into **two** up-to-date / build-cache fingerprints: a local `build` followed by a `fullBuild` would re-run the whole Testcontainers-backed integration tier, which costs far more than instrumentation ever saves on a build-speed-focused version. **Rejected**: `gradle.taskGraph.whenReady` auto-detection (post-graph task mutation, configuration-cache incompatible — and the configuration cache is explicitly planned in `gradle.properties`); a `-Pcoverage` flag (reintroduces the profile flag this migration removed, and every report task would then need it). The overhead is measured during verification (§7) so the acceptance rests on a number. |
| `clean` | **Never baked in** | `mvn clean install -Pfull` bundles the clean; a Gradle lifecycle task that deletes outputs would defeat the incremental state and build cache that 0.5.0 exists to buy. From-scratch stays `./gradlew clean` as its own invocation. |
| Edge syntax | **String task names — `dependsOn("javadocJar")`, not `dependsOn(tasks.named("javadocJar"))`** | `tasks.named(String)` throws `UnknownTaskException` at call time when the task is not yet registered, which would couple each block to statement order inside its script (in `library-conventions` / `domain-service-conventions` the jar tasks are registered in the same file). String names resolve at graph time, so the block is placement-independent. A deliberate, local divergence from the repo's `tasks.named` habit. |
| Task identity | **`fullBuild`, group `build`, description naming every stage it covers** | The name the prior specs already use for this umbrella in their non-goals. Group `build` puts it beside `assemble`/`build` in `tasks` output; the description is the only place a reader learns that formatting rides along implicitly. |
| `pitest` excluded | **Out of the umbrella** | Maven bound PIT to no phase — `-Pfull` never ran it, and the README documents `mutationCoverage` as its own command. Adding it to the umbrella would multiply full-build time by the mutation run. Putting PIT on CI is already a separate backlog item. |
| `install` → `build` | **No local-repo publish** | Nothing in the repo resolves `com.attrigo.asapp` artifacts from `~/.m2` (no `tools/` pom, no external consumer) and Gradle wires the lib dependency as `project(":libs:asapp-commons-url")`, so `install`'s side effect has no consumer to preserve. |
| Duplication | **Accepted** | `jacocoTestReport` / `javadocJar` / `sourcesJar` now appear in both archetype plugins — a second small block on the {2 libs + 3 domain services} axis that the javadoc/sources registration already straddles. Extraction into `asapp.javadoc-sources-conventions` stays parked with the existing repayment trigger in `gradle.md` and the "Clean Gradle files" subtask. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all 7 modules) — register the umbrella. Placed after the coverage block; exact block ordering is deferred to the "Clean Gradle files" subtask (line 35).

```kotlin
// The full-build umbrella (the mvn "-Pfull" equivalent); each module archetype adds the extra artifacts Maven activated for it.
tasks.register("fullBuild") {
    group = "build"
    description = "Runs the full build: assemble, every test tier and the formatting check, plus the coverage reports, " +
        "API documentation, and javadoc and sources jars each module produces."
    dependsOn("build")
}
```

**`build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`** (2 libs) — add the libs' `-Pfull` artifacts.

```kotlin
// Add the libs' -Pfull artifacts to the umbrella: unit coverage plus the javadoc and sources jars.
tasks.named("fullBuild") {
    dependsOn("jacocoTestReport", "javadocJar", "sourcesJar")
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** (3 domain services) — add the domain services' `-Pfull` artifacts.

```kotlin
// Add the domain services' -Pfull artifacts to the umbrella: the three coverage reports, the API guide, and the javadoc and sources jars.
tasks.named("fullBuild") {
    dependsOn(
        "jacocoTestReport",
        "jacocoIntegrationTestReport",
        "jacocoMergedReport",
        "asciidoctor",
        "javadocJar",
        "sourcesJar",
    )
}
```

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** — **unchanged**. Nothing is wired at the 5-service altitude, which is what makes `fullBuild` ≡ `build` for config and discovery.

**No other build file changes.** Core Gradle only — no `gradle/libs.versions.toml` entry, no `build-logic/build.gradle.kts` classpath addition, no root `build.gradle.kts` edit (its stale placeholder comment is left to "Clean Gradle files").

**`.claude/rules/gradle.md`** — add a **Full build** section documenting: `fullBuild` (group `build`) registered in `asapp.java-conventions` with `dependsOn("build")`, extended by `asapp.library-conventions` (`jacocoTestReport`, `javadocJar`, `sourcesJar`) and `asapp.domain-service-conventions` (those three plus `jacocoIntegrationTestReport`, `jacocoMergedReport`, `asciidoctor`), with `asapp.service-conventions` adding nothing so `fullBuild` ≡ `build` for config/discovery — Maven's `-Pfull` reach exactly; `./gradlew fullBuild` fans out via Gradle's task selector, so there is **no** root aggregator and no cross-project reference (configuration-cache / Isolated-Projects safe), at the cost of the root `tasks` report not listing it (`tasks --all`, or a module's own `tasks`, does); wire umbrella edges with **string task names** (`tasks.named(String)` throws at call time when the task is not yet registered, coupling the block to statement order); never wire coverage at the `java-conventions` / `service-conventions` altitude even though the tasks are registered there (parity would be incidental, resting on config/discovery having no `*Tests`); never list `spotlessCheck` (it rides `check`; the description names it); never bake `clean` in, and never translate `mvn clean install -Pfull` as `./gradlew clean fullBuild` (discards the incremental state and cache the version buys); keep `pitest` / `bootBuildImage` / `bootRun` out (never part of `-Pfull`); the JaCoCo agent stays auto-attached to every `Test` task — gating it splits each tier into two cache fingerprints (`@Nested @Optional`, `null` when disabled), `taskGraph.whenReady` detection is configuration-cache incompatible, and a `-Pcoverage` flag reintroduces the profile flag the migration removed; `mvn install` maps to `build`, not a local-repo publish (nothing resolves `com.attrigo.asapp` from `~/.m2`).

**`TODO.md`** — four edits (the umbrella itself spawns no new subtask):

1. Check off "Migrate the full build to Gradle" (line 23) and retire its three notes (lines 24–26): the aggregate is the task itself, the JaCoCo-agent question is decided (accept always-on), and the `spotlessCheck` note is honored by leaving it transitive.
2. Add under "Migrate the release workflow to Gradle" (line 33): `- **Note:** run ./gradlew fullBuild without clean — translating mvn clean install -Pfull literally discards the incremental state and build cache this version buys`.
3. Add under "Migrate build documentation to Gradle" (line 34): a note carrying the `target/` → `build/` artifact map from §7 (coverage, API guide, javadoc/sources jars, mutation reports).
4. Add a new subtask ahead of "Verify full parity, then remove Maven entirely" (line 49): `- [ ] (build) Restore the software bill of materials in the packaged services`, with notes covering the gap (Maven's CycloneDX ran at `prepare-package` on every build; Gradle produces none, so the actuator `/sbom` endpoint serves an empty list), the available TDD path (`ActuatorEndpointsIT` asserts only that `ids` is an array — tightening it to require `application` goes red today and green once `org.cyclonedx.bom` is applied; the assertion also holds under Maven, whose SBOM lands in `target/classes` before Failsafe runs), the caching hazard (Spring Boot's plugin reaction copies the SBOM through `processResources`, so it rides the **test runtime classpath** and its per-build timestamp will break `integrationTest` up-to-dateness — the properties normalization used for `build-info.properties` / `git.properties` cannot ignore a JSON key, so use `runtimeClasspath { ignore("META-INF/sbom/application.cdx.json") }`), and the cost (`cyclonedxBom` becomes a `processResources` dependency, resolving the full dependency graph on every build — measure it; Boot 4.0.5 targets the CycloneDX Gradle plugin 3.x aggregate task).

## 6. Placement / altitude rationale

- **Registration → `asapp.java-conventions` (all 7).** Every module has a `build` task and must participate in the root fan-out, including the two infra services whose umbrella is exactly their build. Same altitude as the compiler, unit-test, and formatting config.
- **Coverage + javadoc/sources edges → `asapp.library-conventions` + `asapp.domain-service-conventions` (5).** The two archetypes whose modules Maven activated those plugins in — the same reasoning, and the same activation set, as the shipped javadoc/sources registration.
- **API-guide edge → `asapp.domain-service-conventions` (3).** Where the `asciidoctor` task already lives.
- **Nothing → `asapp.service-conventions` (5).** Deliberate: an edge here is what would over-reach onto config and discovery.
- **Nothing at the root.** The task selector removes the need, and a root task would be either cross-project (forbidden) or a silent no-op.

## 7. Verification / Definition of Done

- **Graph shape (first step, cheap):** `./gradlew fullBuild --dry-run` from the root schedules `fullBuild` in all 7 modules; `spotlessCheck`, `test`, and `integrationTest` appear; `pitest`, `bootBuildImage`, and `bootRun` do not. `./gradlew :services:asapp-config-service:fullBuild --dry-run` is identical to `:services:asapp-config-service:build --dry-run`, and `:libs:asapp-commons-url:fullBuild --dry-run` adds `jacocoTestReport`, `javadocJar`, `sourcesJar` over `build`.
- **Artifacts** (Docker running, since the umbrella executes the integration tier) — one `./gradlew fullBuild` produces:

  | Artifact | Path | Modules |
  |---|---|---|
  | Runnable / library jar | `build/libs/<name>.jar` | 7 (`bootJar` for the 5 services) |
  | Test + integration reports | `build/reports/tests/{test,integrationTest}/index.html` | 7 / 5 |
  | Unit coverage | `build/reports/jacoco/test/html/index.html` | 5 (skipped for `asapp-commons-url`, no tests) |
  | Integration coverage | `build/reports/jacoco/jacocoIntegrationTestReport/html/index.html` | 3 |
  | Merged coverage | `build/reports/jacoco/jacocoMergedReport/html/index.html` | 3 |
  | REST API guide | `build/docs/asciidoc/api-guide.html` | 3 |
  | Javadoc / sources jars | `build/libs/<name>-<version>-{javadoc,sources}.jar` | 5 |

- **Reach:** no coverage report, javadoc jar, sources jar, or API guide is produced for `asapp-config-service` or `asapp-discovery-service`.
- **Idempotence:** an immediate second `./gradlew fullBuild` reports everything `UP-TO-DATE`.
- **The always-on-agent claim:** a plain `./gradlew build` straight after `fullBuild` must leave `test` and `integrationTest` `UP-TO-DATE` — the single-fingerprint benefit that justifies not gating the agent.
- **Agent overhead measured:** time `./gradlew :services:asapp-users-service:test --rerun-tasks` with the agent on, then again with a temporary `isEnabled = false`, and record the delta in the post-implementation notes. If it is materially worse than a modest instrumentation overhead, re-open the gating decision rather than silently accepting it.
- **Maven untouched:** no `pom.xml` and no source edited, so `mvn -Pfull` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

`pitest` · `bootBuildImage` / Docker images (line 28) · `bootRun` / local runs (line 27) · git hooks (line 29) · CI (line 31), release (line 33), and build-documentation (line 34) consumption · `clean` bundling · `maven-publish` / deploy · cross-module coverage aggregation · XML/CSV coverage formats · the root `build.gradle.kts` placeholder comment (line 35's cleanup) · de-duplicating the archetype blocks (line 35) · the CycloneDX SBOM gap (promoted to its own subtask, §5) · any `pom.xml` or application-source edit.

## 9. Contingencies

- **`tasks.named("fullBuild")` unresolvable in an archetype plugin.** Should the precompiled-script classpath fail to see the task registered by the plugin applied in the same `plugins {}` block, fall back to `tasks.matching { it.name == "fullBuild" }.configureEach { … }`, or register the umbrella independently in each archetype. Not expected: the `plugins {}` block applies `asapp.java-conventions` before the script body runs, which is how the existing `tasks.named("check")` / `tasks.named<Test>("integrationTest")` calls already work across these plugins.
- **A string edge fails at graph time.** If `dependsOn("jacocoIntegrationTestReport")` errors as unknown, the cause is an archetype mismatch (the edge landed in a plugin whose modules lack the task) — fix the placement, do not soften the edge with a task-existence check.
- **`fullBuild` invisible in tooling.** If the root `tasks` report's omission proves annoying in practice, prefer documenting `./gradlew tasks --all` over adding a root task; a root `fullBuild` that aggregates nothing is a silent-success trap.
- **Report tasks never up-to-date.** Should the idempotence check fail on a `JacocoReport` or `asciidoctor` task, treat it as a pre-existing input/output-declaration issue in that stage's own wiring, not as an umbrella defect, and record it rather than papering over it here.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle`. A single commit:

1. `build(gradle): aggregate the full build into a Gradle lifecycle task` — the `fullBuild` registration in `asapp.java-conventions`, the two archetype edges, the `.claude/rules/gradle.md` "Full build" section, and the five `TODO.md` edits.

Following this migration's established pattern (coverage, mutation, formatting, API-doc, javadoc/sources, and packaging subtasks), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec was written before implementation. The change shipped as designed — `fullBuild` (group `build`) is registered in `asapp.java-conventions` with `dependsOn("build")`, `asapp.library-conventions` adds `jacocoTestReport` + `javadocJar` + `sourcesJar`, `asapp.domain-service-conventions` adds those three plus `jacocoIntegrationTestReport` + `jacocoMergedReport` + `asciidoctor`, and `asapp.service-conventions` is untouched. Verified: `./gradlew fullBuild --dry-run` schedules the umbrella in all 7 modules with the exact per-archetype reach (libs get the jars + unit report; config/discovery get nothing beyond their build path, so their `fullBuild` graph equals their `build` graph; domain services get all six extras), and no `pitest` / `bootBuildImage` / `bootRun` anywhere. Both libs were built end-to-end: `javadoc`/`sources` jars in `build/libs`, `asapp-http-clients` unit coverage at `build/reports/jacoco/test/html/index.html`, `asapp-commons-url`'s `jacocoTestReport` **SKIPPED** for want of exec data (exactly the §4 prediction), and a second `fullBuild` reporting everything `UP-TO-DATE`.

The canonical source of truth for exact behavior is the current state of `build-logic/src/main/kotlin/asapp.{java,library,domain-service}-conventions.gradle.kts` and the "Full build" section of `.claude/rules/gradle.md` on this branch, not this document.

Notable deltas:

- **The root `tasks` report *does* list `fullBuild` — corrects §5 and moots §9.** The design predicted a discoverability trade-off ("the root `tasks` report does not list it"). It is wrong: the root report is titled *"Tasks runnable from root project 'asapp'"* and includes selector-reachable subproject tasks, so `fullBuild` appears under **Build tasks** at the root with no root task registered and no `--all` needed. The §9 "invisible in tooling" contingency therefore never applies, and `gradle.md` documents the corrected behavior — the per-project design has no discoverability cost at all.
- **JaCoCo agent overhead measured: ~+22%.** Isolated `test` task duration for `:services:asapp-users-service` (via `--profile`, warm daemon, `--rerun`): **13.2 / 13.3s** agent-off vs **15.9 / 16.4s** agent-on. Total wall-clock proved too noisy to conclude from (16–25s off, 23–34s on, overlapping ranges), which is why the per-task figure is the one recorded. ~22% is a textbook JaCoCo instrumentation cost, not "materially worse than a modest overhead", so §4's accept-always-on decision stands on §7's own criterion rather than on assumption. The number is recorded in `gradle.md`. The integration tier's relative overhead will be smaller still (container startup and Spring context loading dominate), though that was not measured.
- **Integration-tier verification deferred to the developer.** Docker was unavailable in the implementation environment, so the Docker-backed portion of §7 was not executed — the same split the coverage subtask used for its integration/merged reports. Outstanding: one full `./gradlew fullBuild`, the artifact map for the 3 domain services (integration + merged coverage HTML, `build/docs/asciidoc/api-guide.html`), whole-umbrella idempotence, and the `build`-after-`fullBuild` check that `integrationTest` stays `UP-TO-DATE` (the single-fingerprint claim behind the always-on agent).

## 12. References

- Gradle — [Command-Line Interface](https://docs.gradle.org/current/userguide/command_line_interface.html): an unqualified task name is a *task selector* — "You can also run a task for all subprojects using a task selector that consists of only the task name"; invoked from a subproject directory the selector is scoped to that project. The basis for having no root aggregator.
- Gradle — [Organizing Tasks](https://docs.gradle.org/current/userguide/organizing_tasks.html): lifecycle tasks "do not do work themselves" but "bundle actionable tasks and serve as targets for the build"; register with `tasks.register()`, wire with `dependsOn()`, and "you don't need to list all the tasks that Gradle will execute".
- Gradle — [Best practices for structuring builds](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html): convention plugins over `allprojects {}` / `subprojects {}`, which "apply to every project, including the empty ones" and "can degrade build performance".
- Gradle — [JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html): "all tasks of type `Test` are automatically enhanced to provide coverage information when the `java` plugin has been applied"; `jacocoTestReport` deliberately does not depend on `test`.
- Gradle — [`JacocoPluginExtension` source](https://github.com/gradle/gradle/blob/master/platforms/jvm/jacoco/src/main/java/org/gradle/testing/jacoco/plugins/JacocoPluginExtension.java): the agent rides `jvmArgumentProviders` as a `CommandLineArgumentProvider` whose `@Nested @Optional getJacoco()` returns `null` when disabled — so toggling `enabled` changes each `Test` task's input fingerprint. The evidence behind accepting the always-on agent.
- Gradle — [`JacocoReportBase` source](https://github.com/gradle/gradle/blob/master/platforms/jvm/jacoco/src/main/java/org/gradle/testing/jacoco/tasks/JacocoReportBase.java): `onlyIf("Any of the execution data files exists")`, so a module with no matching tests skips its report instead of failing.
- Gradle — [Task configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html): `register()` / `named()` as the lazy APIs; `named(String)` resolves against already-registered tasks, which is why umbrella edges use string names.
- Spring Boot — [Gradle plugin, Packaging Executable Archives](https://docs.spring.io/spring-boot/gradle-plugin/packaging.html): "The `assemble` task is automatically configured to depend upon the `bootJar` task so running `assemble` (or `build`) will also run the `bootJar` task."
- Spring Boot — [Gradle plugin, Reacting to Other Plugins](https://docs.spring.io/spring-boot/gradle-plugin/reacting.html) and [`CyclonedxPluginAction` source](https://github.com/spring-projects/spring-boot/blob/main/build-plugin/spring-boot-gradle-plugin/src/main/java/org/springframework/boot/gradle/plugin/CyclonedxPluginAction.java): applying `org.cyclonedx.bom` makes the plugin configure `cyclonedxBom` (JSON only, `application` type, no license text), copy the SBOM into `META-INF/sbom` **through `processResources`** — so it reaches the test runtime classpath — and add `Sbom-Format` / `Sbom-Location` manifest attributes to `bootJar`. Background for the SBOM subtask: [spring-boot#40890](https://github.com/spring-projects/spring-boot/issues/40890) (fixed in 3.3.1, the `processResources` wiring) and [spring-boot#42601](https://github.com/spring-projects/spring-boot/issues/42601) (multi-project SBOM generation).
