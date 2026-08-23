# Gradle parallel builds — design spec

**Date**: 2026-08-14
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Re-enable parallel builds" (line 35), with its four attached notes.
**Scope**: Flip `org.gradle.parallel` in `gradle.properties` and measure the result. One properties file, this spec, `TODO.md`. No rule file (§10), no convention plugin, no workflow, no application source, no `pom.xml`.

## 1. Context

`gradle.properties` has carried this since the first Gradle commit:

```properties
# disabled: only reliable on native Windows — causes an IOException running Gradle under WSL, re-enable once resolved
org.gradle.parallel=false
```

`git log -S 'org.gradle.parallel' -- gradle.properties` returns exactly one commit: `326d7c4e`, *build(gradle): add Gradle project and module structure skeleton*, 2026-07-16. The value has never been touched since. The 0.5.0 goal line reads *"move the build onto Gradle so every later build is cached, parallel, and incremental"* — caching and incrementality shipped; parallelism is the third that was switched off on day one and never revisited.

**The failure behind the comment was never captured.** No message, no stack trace, no reproduction exists in this repository. The only written trace is second-hand, in the skeleton subtask's own spec (`2026-07-16-gradle-project-module-structure-design.md:166`), which records the setting as scope pulled forward and parenthesizes the reason as *"parallel disabled due to a WSL `IOException`"*. That is the whole evidentiary record. A measure of how little the line was exercised afterwards: the dependency-management subtask found the `#` comment sitting **inline** on the value and moved it onto its own line, "fixing a value-corruption bug inherited from the earlier skeleton subtask" (`2026-07-16-gradle-dependency-management-design.md:99`) — the setting was malformed for a stretch and nobody noticed, because nothing depended on it being read correctly.

**Both candidate root causes are gone.**

*Candidate 1 — the repository was on the Windows drive.* `TODO.md` still points its Problems-report link at `file:///mnt/c/dev/repos/ttrigo/asapp-replace-maven-with-gradle/build/reports/problems/problems-report.html`. The checkout lived under `/mnt/c`, reached through WSL's drvfs translation layer, which is exactly where concurrent file operations from a parallel build would be expected to produce `IOException`s. Today both worktrees — `/home/ttrigo/repos/ttrigo/asapp` and `/home/ttrigo/repos/ttrigo/asapp-replace-maven-with-gradle` — sit on ext4 (`/dev/sdd`), and `/mnt/c/dev/repos/ttrigo/` holds only `scripts` and `tools`. The environment the comment protects no longer exists.

*Candidate 2 — the symptom was misattributed.* `.claude/rules/gradle.md:43` records a root cause found on 2026-08-05, three weeks after the setting was added: two Gradle builds against one working tree race Gradle's unlocked test-results writer and fail any `Test` task with `EOFException` or `NoSuchFileException`. Both are `IOException` subclasses. The full analysis (`2026-08-05-gradle-build-bom-reuse-design.md:189`) reproduced it on two unrelated modules, confirmed four sequential runs green against two concurrent runs red, and established it has nothing to do with `org.gradle.parallel` — which parallelizes *projects within one build*, not builds. If the original observation was this race, the setting never addressed it.

**The setting also has the wrong blast radius.** It is committed, so it disables parallelism on the GitHub runner and in every clone in order to protect one machine. Gradle's precedence for these properties is documented — command-line `-D`, then `GRADLE_USER_HOME/gradle.properties`, then the project root's, then `GRADLE_HOME`'s, first match wins — so a machine-specific opt-out placed in `~/.gradle/gradle.properties` **overrides** a committed `org.gradle.parallel=true` without touching the repository. That is where it belonged all along. No such file exists on this machine today.

### The build is already parallel-safe, by construction

Audited before deciding, not assumed:

| Hazard | Why it does not apply |
|---|---|
| Two projects writing one output directory | Every generated path is per-project: each module's own `build/`, and `snippetsDir = layout.projectDirectory.dir("target/generated-snippets")` (`asapp.domain-service-conventions.gradle.kts:104`) |
| Two tasks writing the repository's hooks directory | `installGitHooks` is registered on the **root project alone** (`asapp.root-conventions.gradle.kts`) — `2026-08-01-gradle-git-hooks-design.md:225` chose that placement because "seven tasks writing the same two files would race under `org.gradle.parallel`" |
| Ordering edges silently dropped | Both use `mustRunAfter`, never `shouldRunAfter`: tests after `spotlessCheck` (`asapp.java-conventions.gradle.kts:55`) and `integrationTest` after `test` (`asapp.service-conventions.gradle.kts:153`). `gradle.md:31` states the reason is precisely "the day `org.gradle.parallel` is enabled" |
| Container port collisions | Every Testcontainers container takes a dynamic host port; nothing in the three `TestContainerConfiguration` classes pins one |

**One `TODO.md` note was wrong and is corrected here:** it warns about "five concurrent Testcontainers tiers". Only **three** services run containers — `asapp-authentication-service`, `asapp-tasks-service` and `asapp-users-service`. `asapp-config-service` and `asapp-discovery-service` have integration tests but no containers. Peak load is 7 containers (3× `postgres:17.7`, 3× `redis:8.4.0-alpine`, 1× `mockserver/mockserver:5.15.0`) plus one Ryuk per test JVM, against 24 CPUs and 16 GB on this machine.

## 2. Goals

- Parallel project execution on by default, for every clone and for CI.
- The machine-specific escape hatch documented where it actually belongs, so the next workaround for one environment does not get committed.
- A measured before/after wall-clock number, so the epic's "parallel" claim rests on evidence rather than on a flag being set.

## 3. Non-goals

- Reproducing the original `IOException`. Its environment is gone (§1); characterization happens only if it recurs (§8).
- Capping worker count pre-emptively (§4).
- Test-level parallelism (`maxParallelForks`), which is a different axis and a separate backlog item.
- Making the configuration cache work — still blocked by `asciidoctor` (`gradle.properties:5`).

## 4. Key decisions

| Decision | Choice | Why |
|---|---|---|
| Where the enabled value lives | **`org.gradle.parallel=true` in the committed `gradle.properties`** | Parallelism is a property of the build, not of one developer's machine. The reverse arrangement — off in the repo, on per machine — is what this task exists to undo. |
| Where an opt-out would live | **`GRADLE_USER_HOME/gradle.properties`**, documented, not created | Gradle's precedence table puts it above the project file, so it works with no repository change and affects no one else. Not pre-staged: creating it now would preserve today's behaviour on the only machine that can measure the change. |
| Characterizing the old failure | **Flip on and run; investigate only if an `IOException` actually appears** | Both candidate causes are gone or unrelated (§1). Reproducing on `/mnt/c` would confirm a hypothesis about an environment nobody builds in any more, at the cost of a full Testcontainers run on the slowest available filesystem. `--stacktrace` is already on the CI command and on both measurement runs, so a recurrence arrives characterized. |
| Worker count | **`org.gradle.workers.max` left unset** | The default is the CPU count — 24 here, 4 on `ubuntu-latest`. A committed cap costs speed on the 24-core box and is a no-op on the runner. `--max-workers=N` caps a single run when needed; a committed value follows only if §6 measures saturation. Recorded as a deliberately-unset key in the properties file, matching how `org.gradle.jvmargs` is handled there. |
| Task ordering | **Unchanged** | Both edges are already `mustRunAfter` (§1). Nothing to migrate. |
| Where the placement rule is recorded | **This spec alone — §1, §4 and §8** | A `.claude/rules/gradle.md` bullet was written and then dropped on review (§10). It fails that file's own keep-test, whose decisive third leg is "getting it wrong fails quietly": a committed `org.gradle.parallel=false` is a visible line in a 13-line file, carrying a comment that explains itself. |

## 5. Changes by file

**`gradle.properties`** — the two lines at `:9-10` become one:

```properties
org.gradle.parallel=true
```

Alphabetical `org.gradle.*` ordering is preserved. `org.gradle.console=verbose` is untouched — its removal is a later subtask. The placement rule and the deliberately-unset `org.gradle.workers.max` are carried by §4 and §8 of this spec rather than by comments here (§10).

**`.claude/rules/gradle.md`** — unchanged. A `## Developer workflow` bullet was written for it and dropped on review (§10).

**`TODO.md`** — line 35 ticked, its four notes dropped, matching how lines 12–34 were closed.

**This spec.**

## 6. Verification / Definition of Done

The measurement runs are developer-run, as every Docker-touching check on this migration has been. Warm the daemon with one throwaway run first so neither timing pays daemon startup.

1. **Serial baseline** — `./gradlew clean`, then
   `./gradlew ciBuild --no-parallel --no-build-cache --profile --console=plain --stacktrace`
2. **Parallel** — `./gradlew clean`, then
   `./gradlew ciBuild --parallel --no-build-cache --profile --console=plain --stacktrace`

   CLI flags outrank `gradle.properties`, so each run measures what it names regardless of what is committed. `--no-build-cache` keeps both doing real work after `clean`.
3. **Parallelism engaged, not merely configured** — in `build/reports/profile/profile-*.html`, the summed task durations must exceed total build time. Overlap is the proof; a green run alone is not. Interleaved `> Task :services:…` lines from different modules in the console is the same signal.
4. **Docker under load** — `docker stats` / `docker ps` during run 2: peak concurrent containers, and any container start timeout or OOM. This is the one genuine saturation risk, and the only thing that would justify a committed worker cap.
5. **Green everywhere** — run 2 passes every tier; then `./gradlew :build-logic:check` (never runs under `build`, per `gradle.md`) and `./gradlew spotlessCheck`.
6. **CI** — `ci.yml`'s `push:` trigger already matches `build/replace-maven-with-gradle*`, so pushing the branch exercises parallelism on a 4-vCPU runner at no extra cost. The run must be green and stay inside `timeout-minutes: 30`; compare its duration against the previous green run on this branch.

**Done when** runs 2, 5 and 6 are green and §10 carries the real before/after numbers.

## 7. Out of scope / YAGNI

`maxParallelForks` and any test-JVM tuning · `org.gradle.workers.max` unless §6 measures saturation · configuration-cache work · removing `org.gradle.console=verbose` · `--max-workers` in `ci.yml` unless §6.6 regresses · the stale `/mnt/c` Problems-report URL in `TODO.md`, which belongs to the "Clean Gradle files" subtask · `--scan` · README edits · any convention plugin, workflow, application source or `pom.xml` edit.

## 8. Contingencies

- **The `IOException` returns.** Capture the stack trace before touching the setting — that is the artifact this task exists because nobody produced. First rule out a second Gradle build against this tree (IDE sync, a second agent), per `gradle.md:43`; that failure is real, common, and not parallelism. Only after it is characterized does the opt-out go into `~/.gradle/gradle.properties` — never back into the committed file.
- **Docker saturates on run 2.** Cap with `--max-workers` to find a working value, then commit `org.gradle.workers.max` only if the working value is well below the CPU count. A cap chosen to make one run pass, without a measurement behind it, is the same mistake as the line being removed here.
- **CI is slower or flakier on 4 vCPUs.** Three Testcontainers tiers on four cores is the plausible regression. The fix is `--max-workers` on the workflow's Gradle step, not disabling parallelism repo-wide — the runner is one environment, and §1 is about not letting one environment set the default.
- **Cross-module fail-fast weakens.** `mustRunAfter(spotlessCheck)` is intra-project, so module A's tests can now start before module B's formatting check has failed. Inherent to parallel execution, and moot in CI, which already passes `--continue`. Do **not** add cross-project ordering edges to restore it — that serializes the build and gives back everything this change buys.

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-24-parallel-builds`. **One** commit — `build(gradle)` — carrying this spec, `gradle.properties` and the `TODO.md` tick together. That is the epic's actual per-subtask shape: `a6104e15`, `b6d150db` and their siblings each ship their design spec *inside* the implementation commit. A two-commit split was planned here first, copied from the ci-aggregate spec's §9, which prescribes it and then did not follow it either.

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document.

## 10. Post-implementation notes

The canonical implementation is the single line in `gradle.properties` — not this document. Both measurement runs were developer-run on this machine (24 CPUs, 16 GB, ext4, Docker in WSL2); their console logs were captured to `.superpowers/sdd/` and their profile reports to `build/reports/profile/`. Both locations are untracked — `.superpowers/sdd/.gitignore` is `*`, and `build/` is build output — so every figure below is quoted inline rather than left as a pointer.

- **The build is 2.36× faster: `5m59.08s` → `2m31.99s`, a saving of `3m27s` on a cold, cache-disabled `ciBuild`.** From the two profile reports, `profile-2026-08-14-15-41-29.html` (serial) and `profile-2026-08-14-15-49-44.html` (parallel). Discounting the two pieces of work the parallel run got for free — `:build-logic:test` was `UP-TO-DATE` there but executed for `22.024s` in the serial run, and artifact transforms cost `0.926s` serial against `0s` — the honest floor is **2.21×**. Both runs executed all nine test tasks across the seven modules, all three Testcontainers tiers included, with zero failures.
- **§6.3's proof-of-overlap criterion was too weak as written, and the correction matters for anyone re-running this.** "Summed task durations exceed total build time" is *also* true of the serial run — `6m16.53s` of task execution against `5m59.08s` total, a ratio of 1.05×. The inequality alone proves nothing. The discriminating figure is the **ratio**: 1.05× serial versus **3.22×** parallel (`8m9.48s` of task execution inside `2m31.99s` of wall clock).
- **The cleaner proof is module interleaving, which §6 should have named instead.** Reducing each log's `> Task` lines to their owning module and collapsing runs gives **18** module switches serially against **65** in parallel. The serial log runs the four remaining services as four solid blocks — `asapp-config-service(22) asapp-discovery-service(22) asapp-tasks-service(23) asapp-users-service(23)` — while the parallel log shatters them, with `asapp-authentication-service`, `asapp-tasks-service` and `asapp-users-service` alternating continuously through the integration tier. That is the three Testcontainers tiers running at once, visible directly.
- **Every individual task got slower, and the wall clock still halved.** Aggregate task time rose 30% (`6m16.53s` → `8m9.48s`) under contention: each of the three heavy integration tiers cost ~13s more (`users` `1m14.60s` → `1m28.13s`, `authentication` `1m1.68s` → `1m14.23s`, `tasks` `47.743s` → `1m0.45s`) and each unit tier ~10s more, roughly +20% and +65% respectively. Contention is real and it is worth paying for.
- **The build is now bounded by its slowest single module, which retires the worker-cap question in the opposite direction from §8's contingency.** `:services:asapp-users-service` totals `2m21.37s` of the `2m31.99s` build — 93% — and its `integrationTest` alone is `1m28.13s`, 58% of total wall clock. There is ~11s of slack above the critical path. Capping workers could only lengthen it; the next build-speed win is not more parallelism but a faster users-service integration tier, which is what the backlog's "Review Spring test-context usage to cut integration-test time" and "Decouple API-doc generation from the full integration tier" already describe.
- **No Docker saturation, and no `IOException`.** Three concurrent Testcontainers tiers — 7 containers at peak plus Ryuk — ran clean on the 24-CPU box. §6.4's `docker stats` observation was not separately captured; the evidence is the green tiers and the absence of any container-start timeout in either log. §8's first contingency never fired, which is the outcome §1 predicted.
- **§6.5's two extra checks are green but replayed rather than re-executed.** `:build-logic:check` → `BUILD SUCCESSFUL in 3s, 13 actionable tasks: 13 up-to-date`; `spotlessCheck` → `BUILD SUCCESSFUL in 1s, 23 actionable tasks: 23 up-to-date`. Nothing in `build-logic` or in any Java source changed, so up-to-date is the correct outcome — but neither run is independent evidence that those checks pass, only that this change did not invalidate them.
- **The serial log is truncated and never captured its `BUILD SUCCESSFUL` line.** `.superpowers/sdd/serial-build.log` ends at the Problems-report line after `:services:asapp-users-service:ciBuild`. It contains zero `FAILED` / `FAILURE` lines, its final task completed, and Gradle writes the profile report only at build end — so the run completed — but the summary line itself is not in evidence and is not claimed here. The parallel run's is: `BUILD SUCCESSFUL in 2m 31s`, `94 actionable tasks: 81 executed, 13 up-to-date`.
- **Both comments shipped in §5's properties block were removed on review; the line stands bare.** Developer decision. Together with the rule-file note below it means this spec is the whole record: §1 and §4 for where a machine-specific opt-out belongs, §4 for `org.gradle.workers.max` being unset deliberately — which the critical-path measurement above now justifies with evidence rather than with a default.
- **`fullBuild` is green under parallelism too — `37s`, developer-run.** The heavier umbrella (coverage reports, `asciidoctor`, javadoc and sources jars on top of `ciBuild`'s scope) holds up. At 37s against the cold `ciBuild`'s `2m31.99s` it is plainly a warm run replaying up-to-date outcomes rather than re-executing the tiers, so it evidences the task graph rather than the tests — but the graph is what parallelism could have broken.
- **The `.claude/rules/gradle.md` bullet was written, rewritten, then dropped entirely. Nothing shipped to any rule file, and the header's "one rule bullet" scope line was wrong until this pass corrected it.** The first wording explained the mechanism without saying what to do; the rewrite led with the prohibition and read correctly. The rewrite is not why it went. Tested against the keep-test `2026-08-11-gradle-rules-trap-cut-design.md` §3 established — *(1) Claude would plausibly do the wrong thing, (2) reading the code does not reveal it, (3) getting it wrong fails **quietly*** — it passes 1 and 2 and fails 3, the leg that spec says does most of the cutting. A committed `org.gradle.parallel=false` is not a quiet failure: it is a visible line in a 13-line file carrying a comment that explains itself, and its symptom is a slower build everywhere rather than a wrong result that looks right. The second objection is the worse one — every one of the 31 surviving bullets is a constraint of *this* build, while this one is generic Gradle-plus-version-control hygiene that would read identically in any repository, squarely the "technology and design decisions taken during the migration" that §1 of that spec assigns to the specs rather than to `gradle.md`. Adding bullet 32 from a single incident, three days after the file was cut 168 → 31, is the drift that pass existed to reverse. Revisit only if a *second* machine-specific setting gets committed: two is a pattern, one is an anecdote.
- **Nothing has run on a GitHub runner as of this commit.** Every result above is local, on 24 CPUs. §6.6 — the 4-vCPU `ubuntu-latest` run, where three integration tiers on four cores is the plausible regression — is the one outstanding verification, and the temporary branch trigger means pushing this branch produces it.

## 11. References

- Gradle — [Configuring the Build Environment](https://docs.gradle.org/current/userguide/build_environment.html): the property precedence order this design depends on — command line `-D`, `GRADLE_USER_HOME/gradle.properties`, project root `gradle.properties`, `GRADLE_HOME/gradle.properties`, "the first one found in any of these locations wins".
- Gradle — [Command-Line Interface](https://docs.gradle.org/current/userguide/command_line_interface.html): `--parallel` / `--no-parallel`, `--max-workers`, `--profile`, `--no-build-cache`.
- `docs/superpowers/specs/v0.5.0/2026-07-16-gradle-project-module-structure-design.md` — where the setting was introduced, and the only written trace of the failure it cites.
- `docs/superpowers/specs/v0.5.0/2026-08-05-gradle-build-bom-reuse-design.md` — the concurrent-builds race whose signatures are `IOException` subclasses, and its full reproduction.
- `docs/superpowers/specs/v0.5.0/2026-08-01-gradle-git-hooks-design.md` — the root-only `installGitHooks` placement, chosen for this day.
- `docs/superpowers/specs/v0.5.0/2026-08-09-gradle-ci-aggregate-task-design.md` — `ciBuild`, the task both measurement runs invoke.
