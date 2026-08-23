# Gradle CI aggregate task — design spec

**Date**: 2026-08-09
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Evaluate a single task that runs every CI check" (line 32), with its attached note.
**Scope**: Register a `ciBuild` lifecycle task covering everything `.github/workflows/ci.yml` gates on today — `build` in all 7 modules, `asciidoctor` in the 3 domain services, and the `build-logic` included build's `check` — then collapse the workflow's three Gradle steps into one. Three convention plugins, one workflow file, plus `.claude/rules/gradle.md` and `TODO.md`. No new module, no new test, no `pom.xml` edit, no README edit; `release.yml` untouched.

## 1. Context

`ci.yml` invokes Gradle three times:

```yaml
- run: ./gradlew build --console=plain --stacktrace
- run: ./gradlew :build-logic:check --console=plain --stacktrace
- run: ./gradlew asciidoctor --console=plain --stacktrace
```

Three commands because no single task covers all three. `build` is a task selector across the 7 java modules; `asciidoctor` is a selector across the 3 domain services, deliberately off the `check` path (`gradle.md` `## API documentation`); `:build-logic:check` addresses an **included build**, which `## Build logic tests` requires be invoked explicitly and by qualified path.

**What the TODO note actually objects to.** The merge gate's contents are described in a YAML file, not in the build. Two consequences: the gate is not reproducible locally with one command — a developer would have to read `ci.yml` and retype three invocations, which in practice nobody does — and a check added to the build does not reach CI without a matching workflow edit. Moving the list into a task fixes both directions at once.

**Feasibility, probed rather than assumed.** Two throwaway `--init-script` probes were run against this working tree; neither wrote to the repository.

*Probe 1 — can a main-build task depend on a task of a `pluginManagement`-included build?* `build-logic` is included from `pluginManagement { includeBuild("build-logic") }`, not from a plain `includeBuild`, so it was not obvious that `gradle.includedBuild(...)` would even resolve it. It does. A root task declaring `dependsOn(gradle.includedBuild("build-logic").task(":check"))` scheduled the full graph:

```
:ciBuildProbe SKIPPED
:build-logic:test SKIPPED
:build-logic:validatePlugins SKIPPED
:build-logic:check SKIPPED
```

*Probe 2 — does an unqualified task-name selector reach a root-project task as well as every module's?* Registering the same name on the root and on every project with the `java` plugin, then running the bare name, scheduled `:ciBuildProbe` **and** all 7 module tasks, with `spotlessCheck` / `integrationTest` riding along. So the per-project pattern `fullBuild` already uses extends to the root, and one command reaches all three of today's step bodies.

*What probe 2 also caught.* Registering on `subprojects` blindly fails: `Could not determine the dependencies of task ':libs:ciBuildProbe'`. The `libs` and `services` container projects are subprojects too and have no `build` task. Registering in `asapp.java-conventions` avoids it — the same reason `fullBuild` is placed there.

**Why one task is not simply one command line.** `./gradlew build asciidoctor :build-logic:check` in a single step would also collapse the three invocations and save two configuration phases, but it leaves the checklist in the workflow, which is the note's whole complaint. Recorded in §4 as the rejected middle option.

**What the current three steps buy, and what replaces it.** Steps 2 and 3 carry `if: "!cancelled() && steps.setup-gradle.outcome == 'success'"` so a failed application build still produces the other two verdicts. A single task loses that by default — Gradle stops at the first failure. `--continue` restores it and improves on it: today a failing tasks-service test aborts the other four services' tiers as well, and under `--continue` they still run.

## 2. Goals

- **One command is the merge gate.** `./gradlew ciBuild` runs exactly what CI runs, from CI and from a developer's machine alike.
- **The build owns the checklist.** Adding a check to the build reaches CI with no workflow edit.
- **No change to what CI verifies.** A green run means the same thing before and after; this is a refactor, not a widening of the gate.
- **No regression in failure reporting.** A red run still reports every independent verdict, not just the first.
- **Simplify `ci.yml`.** The `id: setup-gradle` handle and both `if:` conditions exist only because there are three steps; they go with them.
- **Stay configuration-cache and Isolated-Projects safe**, like `fullBuild`: no cross-project task references, no `subprojects {}` / `allprojects {}`.

## 3. Non-goals

- **Widening the gate to the `fullBuild`-only tasks.** The javadoc/sources jars and the three JaCoCo report tasks stay unguarded by CI. That gap is `TODO.md` line 39's, and it belongs to the release-workflow entry — pulling it in here would make CI slower and change what a green run means, which §2 rules out.
- **`release.yml`.** Its own entry. It keeps running Maven until that lands, and `fullBuild` remains its target, not `ciBuild`.
- **The two `TEMPORARY (Maven→Gradle epic)` scaffolds in `ci.yml`** — the `build/replace-maven-with-gradle*` push pattern and `cache-read-only: false`. Both are reverted by the Maven-removal entry (line 83's warning), untouched here.
- **The README's Continuous Integration subsection.** Deferred to *Migrate build documentation to Gradle*; §5 updates the note that carries it there.
- **Uploading test reports, path-filtered triggers, Pitest in CI, action pinning.** All already booked elsewhere.
- **Wiring `asciidoctor` or `:build-logic:check` onto `check` or `build`.** Both remain forbidden — see §4.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Aggregation mechanism | **A per-project lifecycle task, plus one root registration** | The same task-selector pattern `fullBuild` uses, extended by a root registration that carries the one edge no module can own. Probe 2 confirms the selector reaches both. |
| Task name | **`ciBuild`** | Developer decision. Mirrors `fullBuild`, and names its consumer plainly. Rejected: `checkAll` (reads as an extension of `check`, which it deliberately is not) and `mergeCheck`. |
| Task group | **`build`** | Beside `assemble`, `build` and `fullBuild` in the `tasks` report. |
| `build` edge | **`asapp.java-conventions` (7 modules)** | Where `fullBuild` is registered, and the only altitude that excludes the `libs` / `services` container projects — the failure probe 2 produced. |
| `asciidoctor` edge | **`asapp.domain-service-conventions` (3 modules)** | Where the task exists. Matches `fullBuild`'s placement for the same edge. |
| `:build-logic:check` edge | **`asapp.root-conventions`, declared exactly once** | It is a whole-build concern, not a per-module one. The root is the only project that can hold it without repeating it. |
| Rejected: the cross-build edge in all 7 modules | **No** | Already rejected in `gradle.md` `## Build logic tests` for `check`; the objection — declaring one cross-build edge seven times, and making every module wait on the build's own tooling — applies unchanged to `ciBuild`. |
| Rejected: `check.dependsOn(includedBuild.task(":check"))` | **Still no** | Unchanged from `## Build logic tests`. It would put a build-logic wait into `:services:<svc>:build`, and thus into `bootRun` and every local build. The `ciBuild` edge is the narrow exception: opt-in, on a task nothing else depends on. |
| Rejected: `asciidoctor` onto `check` | **Still no** | `## API documentation` forbids it; the API guide is a release artifact. `ciBuild` reaches it by an explicit edge, exactly as `fullBuild` does. |
| Rejected: `ciBuild` dependsOn `fullBuild` | **No** | It would add the javadoc/sources jars and three JaCoCo reports to every merge gate — the widening §3 rules out, and the reason the CI spec rejected `fullBuild` in CI. |
| Rejected: one step, three task names | **No** | `./gradlew build asciidoctor :build-logic:check` collapses the invocations but leaves the checklist in the workflow. It solves the cheaper half of the problem and none of the expensive half. |
| Root task is not a no-op | **Legitimate, unlike a root `fullBuild`** | `## Full build` bans a root `fullBuild` because it would aggregate nothing and make `./gradlew :fullBuild` a silent success. The root `ciBuild` carries a real edge and does real work, so the ban's reasoning does not reach it. |
| Description, set once | **In `asapp.java-conventions` only; the root registration sets `group` and no description** | Gradle surfaces one description per task name in the root report (`## Full build`, measured). A second string is how that report started lying once already; an identical duplicate is the same hazard one edit away. §8 carries the fallback if the root report degrades. |
| Description is hedged, not a superset | **"… for the modules that produce it … at the root"** | The same rule `## Full build` records: the one string is read from every `<module>:tasks` view too, and no module's `ciBuild` carries both the API guide and the build-logic edge. An unqualified list would advertise checks each module never runs. |
| Edge syntax | **String task names** — `dependsOn("build")`, `dependsOn("asciidoctor")` | `## Full build`'s rule: `tasks.named(String)` throws at call time when the task is not yet registered, coupling a block to statement order within its own script. |
| CI invocation | **`./gradlew ciBuild --console=plain --stacktrace --continue`** | The first two flags carry over unchanged. `--continue` replaces the `!cancelled()` machinery and covers more: independent module tiers now also survive a sibling's failure. |
| `if:` conditions and `id: setup-gradle` | **Removed** | They existed to run steps 2 and 3 after a step-1 failure, and to stop a rejected wrapper jar from reaching `./gradlew`. With one step, the implicit `if: success()` on the step after `Set up Gradle` closes the same hole with nothing to write. |
| `timeout-minutes: 30` | **Unchanged** | The work is identical; only the number of invocations changes. |
| Commit type | **`build(gradle)`** | Three convention plugins are the substance; the workflow edit follows from them. The CI-only sibling used `ci(gradle)` because it touched nothing else. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** (all 7 modules) — register the task, immediately after the `fullBuild` block:

```kotlin
// The CI umbrella; each module archetype extends it with the extra checks the pipeline gates on.
tasks.register("ciBuild") {
    group = "build"
    description = "Runs the checks the CI pipeline gates on: assemble, every test tier and the formatting check, plus the API documentation for the modules that produce it and the build logic's own checks at the root."
    dependsOn("build")
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** (3 domain services) — add the API guide, after the `fullBuild` block:

```kotlin
// Add the domain services' extra CI check: the API guide.
tasks.named("ciBuild") {
    dependsOn("asciidoctor")
}
```

**`build-logic/src/main/kotlin/asapp.root-conventions.gradle.kts`** (root only) — add the build's own suite. A `register`, not a `named`: the root applies no `java-conventions`, so it has no `ciBuild` yet.

```kotlin
// The root's share of the CI umbrella: the build logic's own checks, which live in an included build no module task reaches.
tasks.register("ciBuild") {
    group = "build"
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
```

**`build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`** and **`asapp.service-conventions.gradle.kts`** — **unchanged**. The libs and the two infra services contribute nothing beyond their `build`.

**`.github/workflows/ci.yml`** — three steps become one, and `Set up Gradle` loses its `id`:

```yaml
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          validate-wrappers: true
          cache-provider: 'basic'
          cache-read-only: false

      - name: Build and verify the project
        run: ./gradlew ciBuild --console=plain --stacktrace --continue
```

Only the `id:` line is removed from `Set up Gradle`; its three explanatory comments (wrapper validation, cache provider, and the `TEMPORARY` scaffold) are elided from the block above but survive verbatim. Everything above `jobs:` is untouched, as is the three-line header comment. The `Build the project documentation` step's comment — explaining why `asciidoctor` is invoked separately — disappears with the step; its content moves into the `## CI build` rule section.

**`.claude/rules/gradle.md`** — one new section and three corrections:

1. **New `## CI build` section**, placed after `## Full build`: `ciBuild` (group `build`) registered in `asapp.java-conventions` with `dependsOn("build")`, extended by `asapp.domain-service-conventions` with `asciidoctor` and by `asapp.root-conventions` with `gradle.includedBuild("build-logic").task(":check")`; string edges, per `## Full build`; description set exactly once, in `asapp.java-conventions`, with the root registration deliberately setting none; the root registration is legitimate here because it carries a real edge, unlike the banned root `fullBuild`; `ciBuild` is the **only** cross-build edge in the repository and the ban on `check` / `build` / `fullBuild` reaching an included build is unchanged; `asciidoctor` reaches `ciBuild` by explicit edge and **never** by a `check` edge; `ciBuild` must never depend on `fullBuild` or gain the release-only artifacts; CI runs it as `./gradlew ciBuild --console=plain --stacktrace --continue`, with `--continue` the reason the workflow needs no `!cancelled()` gating.
2. **`## Build logic tests`, first bullet.** It states that `./gradlew build` / `check` / `fullBuild` never reach an included build and that "**no** cross-build edge is wired to change that", then describes CI's separate `Build and test the build logic` step, its `if:` condition, and why it is a second step in the same job rather than a second job. The first two halves are now false. Rewrite: the edge exists, on `ciBuild`, declared once at the root; the ban on `check` / `build` / `fullBuild` edges stands with its reasoning intact; CI's trigger is the single `ciBuild` step, and the `if:` clause is gone. **Keep the one-job constraint** — two concurrent Gradle builds against one working tree still race, and a single step satisfies it more directly than two sequential ones did.
3. **`## Build logic tests`, third bullet, closing sentence.** "An outer `check.dependsOn(gradle.includedBuild("build-logic").task(":check"))` is rejected too" stays rejected — add the clause distinguishing it from the `ciBuild` edge (opt-in task nothing depends on, declared once, versus a seven-module edge on the path of every local build).
4. **`## API documentation`, last bullet.** "Keep the `asciidoctor` task off the `check`/`build` path — opt in via `./gradlew asciidoctor` … or get it from `fullBuild`" gains `ciBuild`. The same bullet's claim that the guide "belongs on the release path … and not on every develop-branch build" is already false — CI has rendered it since the workflow migration — so correct it rather than carry it forward: it stays off `check`, and CI reaches it by an explicit edge to catch conversion breaks at merge time.

**`TODO.md`** — three edits:

1. Tick line 32 and remove its note; this spec absorbs it.
2. Line 68's note under *Migrate build documentation to Gradle* prescribes the README's future CI list as "… `./gradlew build`, `./gradlew :build-logic:check`" — already stale (it omits `asciidoctor`) and about to be more so. It becomes a single `./gradlew ciBuild`.
3. Line 39's note under *Migrate the release workflow to Gradle* lists `asciidoctor` among the `fullBuild`-only tasks with no automated coverage. CI has covered it since the workflow migration, so drop it from that list; the javadoc/sources jars and the three JaCoCo reports stay, and the note's point survives intact.

## 6. Verification / Definition of Done

Graph shape first, cheap and Docker-free:

- `./gradlew ciBuild --dry-run` from the root schedules `:ciBuild` plus `ciBuild` in all 7 modules, the `:build-logic:check` graph (`test`, `validatePlugins`, `check`), and `asciidoctor` in exactly the 3 domain services and nowhere else. `test`, `integrationTest` and `spotlessCheck` all appear.
- `./gradlew :ciBuild --dry-run` schedules the build-logic graph and nothing else.
- `./gradlew :services:asapp-config-service:ciBuild --dry-run` equals that module's `build --dry-run` — no `asciidoctor`, no build-logic tasks.
- `./gradlew tasks` from the root lists `ciBuild` under **Build tasks** with the `asapp.java-conventions` description. If the description is missing or empty, §8's fallback applies.

Then, with Docker (developer-run, as with every integration-tier check on this migration):

- One `./gradlew ciBuild --console=plain --stacktrace --continue` green from a clean tree — the exact command CI will run.
- An immediate second run reports everything `UP-TO-DATE`, and a plain `./gradlew build` straight after leaves `test` and `integrationTest` `UP-TO-DATE` — the aggregate introduces no new fingerprint.

Workflow and residue:

- `actionlint` clean on `ci.yml`.
- `ci.yml` contains exactly one `./gradlew` invocation, no `if:` condition, and no `id: setup-gradle`; both `TEMPORARY (Maven→Gradle epic)` comments and everything above `jobs:` are unchanged (zero `+`/`-` lines in that region).
- `.claude/rules/gradle.md` retains no claim that no cross-build edge exists, and none that CI runs `:build-logic:check` or `asciidoctor` as its own step.

Observable only after the branch is pushed, since the temporary trigger fires on it:

- The run is green with a single Gradle step, and its log shows the build-logic tasks and `asciidoctor` inside that one step.
- A deliberately failing check confirms `--continue` still reports the independent verdicts. Optional — worth doing once, on a throwaway commit, since it is the behaviour replacing the `!cancelled()` wiring.

## 7. Out of scope / YAGNI

`fullBuild`-only artifacts in CI · `release.yml` · the two temporary `ci.yml` scaffolds · README edits · a second job or a build matrix · `--scan` / `--profile` in CI · uploading reports on failure · configuration-cache work (still blocked by `asciidoctor`) · extracting a shared description constant before §8 shows it is needed · any `pom.xml` or application-source edit.

## 8. Contingencies

- **The root `tasks` report shows `ciBuild` with no description, or with the wrong one.** Gradle surfaces one description per task name and the choice among registrations is unspecified. Fallback: hold the string in a `const val` in `build-logic/src/main/kotlin/com/attrigo/asapp/gradle/`, alongside `InstallGitHooks`, and reference it from both registrations — identical by construction rather than by discipline. Do **not** paste the literal into `asapp.root-conventions`.
- **`gradle.includedBuild("build-logic")` fails to resolve in the precompiled root plugin.** Probe 1 resolved it from an init script, which is a different context. If the plugin context differs, the fix is placement, not softening the edge — try the root `build.gradle.kts` before abandoning the shape, and record why the convention plugin could not hold it.
- **`:build-logic:check` runs more than once.** It should not; Gradle dedupes a task reached by several paths. If it does, the cause is a second edge added somewhere beyond the root — remove it rather than guarding it.
- **`--continue` produces confusing cascades.** Expected on a red run: unrelated failures now surface together. Read the task names before concluding a new breakage exists. Never drop `--continue` to quiet a log — that reintroduces the fail-fast behaviour the `!cancelled()` wiring existed to prevent.
- **A developer finds `./gradlew ciBuild` too slow locally.** It is CI's gate, not a pre-commit hook, and it always was — `./gradlew build` remains the everyday command. Do not add a flag to trim it; that reintroduces the profile switching this migration removed.

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-21-gradle-task-ci-workflow`. Two commits, matching this task's siblings: this spec on its own (`docs(gradle)`), then the implementation (`build(gradle)`) carrying the three convention plugins, `ci.yml`, the `gradle.md` section and corrections, and the three `TODO.md` edits.

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document unless the developer asks for one.

## 10. Post-implementation notes

The canonical implementation is the three convention plugins, `.github/workflows/ci.yml` and the `## CI build` section of `.claude/rules/gradle.md` — not this document.

- **§4's "the root registration sets `group` and no description" row is wrong on both halves, and §8's first contingency fired.** With the root registration left description-less, `./gradlew tasks` printed a bare `ciBuild` line: Gradle surfaces one description per task name and picks the **root project's**, dropping the string `asapp.java-conventions` sets. So the description is set in **both** registrations, verbatim — the root's drives the root report, `java-conventions`' drives every `<module>:tasks` view, and neither substitutes for the other. `group`, by contrast, is set **only** in `asapp.java-conventions`: measured, with the root's deleted, `ciBuild` still lists under **Build tasks** in both `./gradlew tasks` and `./gradlew :tasks`, so a second declaration buys nothing.
- **The description shipped short — `"Runs every check the CI pipeline gates on."` — not §5's enumerated string.** Developer decision, and it dissolves two problems at once: §4's "hedged, not a superset" row exists only because an enumerated list would advertise checks a given project never runs, which a string naming no artifacts cannot do; and a one-line string is cheap to keep identical across the two registrations that must both carry it.
- **§8's fallback — a shared `CI_BUILD_DESCRIPTION` constant — was implemented, then removed on review.** It worked, but the justification given for it here did not survive scrutiny: this section originally cited `## Full build`'s drifted `fullBuild` descriptions as precedent, and that incident was three *deliberately different* per-archetype strings each overriding the last, not a duplicated literal that drifted apart. Two literals in two adjacent files, with a comment on each and the constraint recorded in `## CI build`, is the proportionate shape; the constant cost a source file, a KDoc and an import in two convention plugins for one string. Developer decision.
- **Graph shape verified exactly as §6 prescribes.** `./gradlew ciBuild --dry-run` scheduled `:ciBuild`, the `:build-logic:{test,validatePlugins,check}` graph, `ciBuild` in all 7 modules, and `asciidoctor` in exactly the 3 domain services. `./gradlew :ciBuild --dry-run` scheduled the build-logic graph and nothing else. `:services:asapp-config-service:ciBuild --dry-run` differs from that module's `build --dry-run` by exactly one line — the lifecycle task itself.
- **`:build-logic:check` green, and the reason is worth recording.** `BUILD SUCCESSFUL in 22s`, `validatePlugins` and `test` both executing. The suite could plausibly have gone red: its TestKit fixtures apply `asapp.root-conventions` in builds that include no `build-logic`, where `gradle.includedBuild("build-logic")` throws. It does not, because `tasks.register`'s configuration block is lazy and every fixture invokes `installGitHooks` by name — verified by reading the fixtures' three `GradleRunner` invocations, none of which runs `tasks`. Recorded as a constraint in `## CI build`, since a future fixture that realizes the whole task container would break it.
- **`actionlint` clean** — `docker run --rm -v "$PWD:/repo:ro" --workdir /repo rhysd/actionlint:latest -color`, no output, `EXIT=0`.
- **Workflow residue checks pass.** `ci.yml` holds exactly one `./gradlew` invocation; a grep for `if:`, `id:`, `mvn` and `-Pci` returns nothing. Everything above `jobs:` is untouched and both `TEMPORARY (Maven→Gradle epic)` comments survive verbatim.
- **The full `./gradlew ciBuild` is green — `BUILD SUCCESSFUL in 1m 38s`, `94 actionable tasks: 65 executed, 4 from cache, 25 up-to-date`**, run by the developer. It confirms the cross-build edge in a real execution rather than only under `--dry-run`: `:build-logic:test`, `:build-logic:validatePlugins` and `:build-logic:check` all ran inside the same invocation as the application build, interleaved with it. All 8 `ciBuild` tasks executed (`:ciBuild`, both libs, all 5 services); `asciidoctor` ran in exactly the 3 domain services and in neither infra service; `spotlessCheck` ran in all 7.
- **That run did not execute a single test, and the reason is the aggregate's, not a defect.** Every test task reported `UP-TO-DATE` — `:libs:asapp-http-clients:test`, and both `test` and `integrationTest` on all five services — so no Testcontainers tier started. Editing the convention plugins changed the plugin classpath, which re-executed every `compileJava`, but the class files came out identical, so the test tasks' inputs never moved. The same "a warm green run replays cached outcomes rather than re-running the tests" caveat the CI-workflow spec records applies here unchanged, and it is why the graph is what this run verifies.
- **Idempotence holds, with a pre-existing exception §6 should have anticipated.** A second `./gradlew ciBuild` → `BUILD SUCCESSFUL in 18s`, `94 actionable tasks: 10 executed, 84 up-to-date`. Every test tier, every `asciidoctor` and `:build-logic:test` came back `UP-TO-DATE`. The ten that re-execute are `bootBuildInfo` and `bootJar` on the five services — `## Packaging` already records why (`bootBuildInfo` restamps `build.time`, one of its own inputs, so it can never be up-to-date, and `bootJar` consumes `build/resources/main` with `PathSensitivity.RELATIVE` rather than `@Classpath`, so it repacks). §6's "everything `UP-TO-DATE`" was too strong; the umbrella introduces no new fingerprint, which is the claim that matters.
- **`build` after `ciBuild` leaves every test tier `UP-TO-DATE`** — `BUILD SUCCESSFUL in 10s`, `87 actionable tasks: 10 executed, 77 up-to-date`, the same ten packaging tasks. Adding `ciBuild` costs a developer's ordinary `./gradlew build` nothing.
- **Nothing has run on a GitHub runner as of this commit.** Every result above is local. The temporary branch trigger means pushing the branch will produce a run.

## 11. References

- Gradle — [Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html): `gradle.includedBuild(name).task(path)` as the documented way to depend on a task in an included build, and the rule that a build's lifecycle tasks do not reach one on their own.
- Gradle — [Command-Line Interface](https://docs.gradle.org/current/userguide/command_line_interface.html): an unqualified task name is a task selector; `--continue` "executes all tasks that the failed task did not depend on".
- Gradle — [Organizing Tasks](https://docs.gradle.org/current/userguide/organizing_tasks.html): lifecycle tasks bundle actionable tasks and do no work themselves.
- GitHub Actions docs, *Evaluate expressions* — the implicit step `if: success()` that makes the removed conditions unnecessary.
- `docs/superpowers/specs/v0.5.0/2026-07-27-gradle-full-build-design.md` — the umbrella pattern this reuses, and the root-aggregator ban §4 distinguishes itself from.
- `docs/superpowers/specs/v0.5.0/2026-08-07-gradle-ci-workflow-design.md` — the three-step workflow this collapses, including the `!cancelled()` and `setup-gradle.outcome` reasoning it retires.
- `docs/superpowers/specs/v0.5.0/2026-08-02-gradle-build-logic-tests-design.md` — the suite the root edge reaches.
- `.claude/rules/gradle.md` — `## Full build`, `## API documentation`, `## Build logic tests`.
