# Migrate the CI workflow to Gradle — design spec

**Date**: 2026-08-07
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate the CI workflow to Gradle" (line 31).
**Scope**: Point `.github/workflows/ci.yml` at `./gradlew`, hand caching to `gradle/actions/setup-gradle`, and add the `:build-logic:check` step that no other invocation reaches. Two files changed (`.github/workflows/ci.yml`, `.claude/rules/gradle.md`), plus `TODO.md` edits. No new file, no new test, no build-script change, no `pom.xml` edit, no README edit, and `release.yml` untouched.

## 1. Context

`ci.yml` runs one job with four steps: checkout, `actions/setup-java@v5` with `cache: maven`, and `mvn verify -B --no-transfer-progress -Pci`, at `timeout-minutes: 20`. Everything above `jobs:` — the `push`/`pull_request` triggers on `main`, the `concurrency` group with `cancel-in-progress: true`, and `permissions: contents: read` — is correct as-is and this change does not touch it.

**The `-Pci` profile is one property.** `pom.xml:203-207` is the whole of it:

```xml
<profile>
    <id>ci</id>
    <properties>
        <spotless.check.skip>false</spotless.check.skip>
    </properties>
</profile>
```

Spotless is skipped by default under Maven and the profile un-skips it. Gradle needs no analog: `spotlessCheck` is wired into `check` by default, a deliberate divergence already decided and recorded in `gradle.md` `## Formatting` precisely to honour the `ci`-profile intent. So `mvn verify -Pci` → `./gradlew build`, with the profile evaporating rather than being translated.

**`mvn verify` maps to `build`, not `check`.** Maven's `verify` runs through `package` (Boot repackage) and Failsafe's `integration-test`; Gradle's `build` is `assemble` + `check`, where `check` pulls `test`, `integrationTest` and `spotlessCheck`. Same coverage, same artifacts.

**The build-logic gap, measured rather than assumed.** `./gradlew build --dry-run` reaches the included build and stops at `:build-logic:jar` — those tasks run on *every* invocation because plugin resolution needs the jar. `:build-logic:check --dry-run` adds exactly two tasks beyond that graph:

```
:build-logic:test              ← the 11-scenario InstallGitHooksFunctionalTest suite
:build-logic:validatePlugins   ← task/property annotation contract
```

So the second step's unique coverage is those two tasks and nothing else. It is worth having for `test`: `./gradlew build` compiles `InstallGitHooks` but never *executes* it, and CI never runs `installGitHooks`, so without this step nothing anywhere verifies linked-worktree common-directory resolution, `core.hooksPath` clearing, or the 755 permission bit — where a regression makes git **silently** skip the hook, the failure mode `## Git hooks` singles out as the worst a commit gate can have.

**The TODO note for this entry overstates the case, and the correction belongs here.** It calls `:build-logic:check` "the only automated signal that a Boot upgrade has broken the build's own tooling." Most of that blast radius already lands on step 1: if the BOM raised `kotlin-stdlib` above the `strictly` pin, plugin resolution hard-fails on *every* invocation; if it moved a Spotless / Asciidoctor / JGit transitive into an incompatibility, `spotlessCheck` / `asciidoctor` / `generateGitProperties` fail inside `./gradlew build`. The genuinely uncovered surface is narrower — the `InstallGitHooks` execution path plus `validatePlugins`. The step earns its place; not for the stated reason.

**Console output, measured.** `gradle.properties` commits `org.gradle.console=verbose` as migration-time diagnostics. `verbose` forces the rich renderer regardless of terminal attachment, so with output piped — exactly how GitHub Actions captures it — Gradle still emits its progress-area redraw:

```
^[[2A^[[1m│^[[0;32;1m···^[[0;39;1m│ 0% CONFIGURING [214ms]^[[m^[[40D^[[1B
```

Cursor-up (`^[[2A`), clear-line (`^[[0K`), cursor-left-40 (`^[[40D`). GitHub renders ANSI colour but does not emulate a cursor, so each redraw appends instead of overwriting. `--console=plain` on the same command loses no information — every task name **and** every outcome survives:

```
> Task :build-logic:compileKotlin UP-TO-DATE
> Task :build-logic:compileJava NO-SOURCE
> Task :libs:asapp-commons-url:compileJava UP-TO-DATE
```

That outcome column (`UP-TO-DATE` / `FROM-CACHE` / executed) is the cache-effectiveness signal this version exists to produce.

**Two behavioural deltas in CI that this change materialises, both already decided elsewhere.** Neither is new policy; both first take effect here and a reader comparing CI logs before and after needs them named:

| Delta | Origin |
|---|---|
| **CI stops rendering `api-guide.html`.** Maven bound `asciidoctor` to `post-integration-test` with no skip property and no profile guard, so it ran on every CI build. Gradle keeps it off the `check` path and on the release path only. | `gradle.md` `## API documentation` |
| **The JaCoCo agent now attaches on CI runs.** `-Pci` left `jacoco.skip=true`, so Maven CI ran the tests un-instrumented; Gradle attaches the agent to every `Test` task unconditionally (gating it would split every tier's cache entry). Measured at **+22%** on the users-service unit tier. No report is produced either way — the report tasks stay off `check`. | `gradle.md` `## Full build` |

The SBOM is parity, not a delta: Maven's `prepare-package` binding meant `mvn verify` produced it, and Gradle's `processResources` edge does the same on `build`.

## 2. Goals

- **CI verifies the Gradle build.** The merge gate stops being Maven, which is the point of the entry.
- **No formatting-gate regression.** `spotlessCheck` still fails the build on every CI run, via `check` rather than a profile.
- **The build cache survives between CI runs.** `org.gradle.caching=true` is already set; CI needs a cache that persists `~/.gradle/caches` for it to mean anything on the default branch.
- **Close the wrapper-integrity gap.** The committed `gradle-wrapper.jar` is the code that decides whether to honour `distributionSha256Sum` at all, and nothing validates it today.
- **Give `:build-logic:check` its first automated trigger.** It has none; a developer's local build is currently the only signal.
- **Deterministic, readable CI logs** that do not depend on a migration-time `gradle.properties` setting.

## 3. Non-goals

- **`release.yml`.** Its own entry (`TODO.md` line 37), with its own notes on `fullBuild` without `clean`, the absent `-DskipTests` analog, and the `docker push` loop. Untouched here, so the tag path keeps running Maven until that lands — an accepted interim split.
- **The README's CI/CD section.** Developer decision to defer to *Migrate build documentation to Gradle* (line 60); §5 adds the note that carries it there. The subsection is already wrong today, claiming `mvn verify -Pfull` and JaCoCo/Surefire/Failsafe reports that `-Pci` never produced.
- **Uploading test reports on failure.** Developer decision: strict parity, since today's workflow uploads nothing. Gradle prints failing test names and assertion errors to the console and `setup-gradle`'s job summary is on by default.
- **Pitest in CI** and **path-filtered triggers** — both already Backlog items under `#### ci`.
- **Restoring parallel builds** (line 69) or **removing `pom.xml`** (line 74). This change is neutral to both.
- **GitHub dependency-graph submission.** Flagged in §7 as a consequence of the *later* Maven-removal entry, not work for this one.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Command | **`./gradlew build`** | Faithful to `mvn verify`: `assemble` + `check`, where `check` carries `test`, `integrationTest` and `spotlessCheck`. |
| `-Pci` analog | **None — the profile evaporates** | Its single property un-skipped Spotless; `spotlessCheck` is already on Gradle's `check` path by an earlier, deliberate decision. Adding any CI-only flag would reintroduce the profile this migration removed. |
| Rejected: `fullBuild` in CI | **Never** | It adds coverage reports, javadoc/sources jars and `asciidoctor` — release artifacts, not merge gates. `fullBuild` belongs to `release.yml`. |
| Caching | **`gradle/actions/setup-gradle@v6`** | Its `cache-read-only` default is `ref_name != default_branch`, so `main` writes and PRs read — the correct scoping with nothing to configure. `setup-java`'s `cache: gradle` caches only dependency files, not the Gradle build cache that `org.gradle.caching=true` fills. |
| Cache provider | **`'basic'`**, not the `'enhanced'` default | `'enhanced'` is Gradle's full-featured commercial caching service (`gradle-actions-caching`) — closed-source, distributed under Gradle's Terms of Use, which using v6 means accepting; Gradle's own docs call it free for public repositories today but reserve introducing functionality restrictions in future updates. `'basic'` is MIT and built on `@actions/cache`, so it cannot be moved behind a subscription; the features it drops (`cache-cleanup`, deduplication, `gradle-home-cache-includes`/`-excludes`, `cache-write-only`, `cache-overwrite-existing`, `gradle-home-cache-strict-match`) are all unused by this workflow. `cache-read-only` and `validate-wrappers` are both provider-independent, so neither of the two temporary scaffolds is affected. Same class of decision as §7's rejection of `build-scan-publish` — a commercial, Terms-of-Use-gated component declined in favour of the OSS-safe alternative. |
| `cache: maven` on `setup-java` | **Removed** | Redundant once Maven is not invoked, and the two caching mechanisms conflict. `setup-java` stays for the JDK 25 toolchain, which `## Compilation` records as having no fallback. |
| Step order: JDK then Gradle | **`setup-java` before `setup-gradle`** | `setup-gradle` uses the JDK already on `PATH`; reversing them would set Gradle up against the runner's default JVM. |
| Wrapper validation | **The `validate-wrappers: true` default** | Validates the committed jar against Gradle's published checksums. No standalone `gradle/actions/wrapper-validation` step alongside it, and **never** `validate-wrappers: false`. |
| `:build-logic:check` | **A second, separately-invoked step** | `./gradlew build` never reaches an included build — measured, §1. No cross-build edge is wired to change that, per `## Build logic tests`. |
| One job, not two | **One job, two sequential steps** | `## Build logic tests` forbids two concurrent Gradle builds against one working tree (a real results-writer race, not a flake). Two jobs would need two checkouts and pay a second cache restore for a ~60 s suite. |
| Step order: app then build-logic | **`build` first** | The application build is the primary signal and the likelier failure; the tooling suite is the rarer one. Fail-fast on the cheaper signal would tax every green run to speed up the rare case. |
| `if: '!cancelled()'` on the build-logic step | **Yes** | Overrides the implicit `if: success()` so one red run reports both verdicts — the Boot-upgrade case where both break, which otherwise costs a push-wait-discover cycle. Not `always()`: with `cancel-in-progress: true` set, `always()` keeps the step running *through* a cancellation, spending ~60 s on a discarded run. The `!` must be quoted — it is a YAML tag indicator. `success() \|\| failure()` is **not** a safe alternative — it has the identical defect: both override the implicit `success()` for every preceding step, not just this one, so either form still lets a failed `Set up Gradle` (its headline failure mode is `validate-wrappers` rejecting the wrapper jar) run `./gradlew` here. §10 records the fix: also gate on `steps.setup-gradle.outcome == 'success'`. |
| Console | **`--console=plain` on both invocations** | Measured, §1: `verbose` emits cursor-control escapes into a piped stream; plain retains every task name and outcome. Also correct after `org.gradle.console=verbose` is removed at Maven retirement — it is what auto-detection would pick in CI anyway. |
| Rejected: `-q` / `--quiet` | **Never in CI** | Measured: a successful run under `--console=plain --quiet` emits **zero bytes**. Task headers, `BUILD SUCCESSFUL` and the actionable-task summary all sit at `LIFECYCLE`, so `-q` deletes the cache-effectiveness signal outright. It is also quieter than Maven CI was — `-B --no-transfer-progress` suppressed download progress, not the build log. The pre-commit hook pairs `--console=plain --quiet` because it fires interactively on every commit; different context. |
| Rejected: `--warning-mode=none` | **No** | It would silence the Liquibase plugin's unfixable deprecation banner, but that banner is the live signal for *Review Deprecated Gradle features* (line 56). Hiding it in CI blinds the task tracking it. |
| Rejected: `--no-daemon` | **Never** | `setup-gradle` manages daemon lifecycle and stops it to save the cache; `--no-daemon` fights that and Gradle no longer recommends it in CI. |
| `-B` / `--no-transfer-progress` analogs | **None** | Non-interactive is Gradle's default; `--console=plain` covers the progress noise. |
| Action pinning | **Major tags (`@v6`)** | Matches every action already in the repository (`checkout@v6`, `setup-java@v5`, `login-action@v4`, `git-cliff-action@v4`). |
| `timeout-minutes` | **20 → 30** | The first run has no cache and pays the wrapper distribution download; the build-logic suite adds ~60 s on every run; and the JaCoCo agent now instruments the tiers (+22% measured on one unit tier). `release.yml`'s equivalent job already sits at 30, so this aligns rather than invents. |
| `permissions` | **`contents: read`, unchanged** | Job summaries write to the step-summary file and caching uses the Actions cache service; neither needs more. `contents: write` would only be needed for dependency-graph submission, which §7 declines. |
| Commit type | **`ci(gradle)`** | The change is entirely a workflow file. Departs from the run of `build(gradle)` commits on this task's siblings, which touched build scripts; `ci` is a listed tooling scope in `.claude/rules/todo.md`. |

## 5. Changes by file

**`.github/workflows/ci.yml`** — the `jobs:` block becomes:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - name: Checkout the project
        uses: actions/checkout@v6

      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Build and test the project
        run: ./gradlew build --console=plain

      - name: Build and test the build logic
        if: '!cancelled()'
        run: ./gradlew :build-logic:check --console=plain
```

Five edits: `cache: maven` dropped from `setup-java`; a new `Set up Gradle` step; the `mvn` line replaced; a new build-logic step; `timeout-minutes` 20 → 30. Nothing above `jobs:` changes. The two-line header comment stays accurate as written.

**`.claude/rules/gradle.md`** — three claims this change falsifies:

1. `## Shared Build Configuration`, the closing clause of the `build-logic`-BOM bullet: "`:build-logic:check` is what would catch such a break, and CI does not run it yet, so until the Gradle cutover a developer's local build is the first signal." CI now runs it. Rewrite to name the CI step as the trigger — and, per §1, scope the claim honestly: step 1 already catches most Boot-BOM breakage, so this step's unique reach is the `InstallGitHooks` execution path and `validatePlugins`.
2. `## Compilation`, the toolchain-prerequisite bullet: "true today (both workflows still invoke Maven) and after a future cutover to `./gradlew`". Half-false now — `ci.yml` invokes Gradle, `release.yml` still Maven. The surrounding claim that *CI needs nothing extra* stays true and becomes load-bearing rather than hypothetical.
3. `## Build logic tests`, the opening bullet: "The CI step is the other trigger, and it is the migration's, not this section's." The step exists; state what it is and that it is explicit and qualified, matching the section's own rule.

**`TODO.md`** — tick line 31 and remove its five notes, which this spec absorbs (including the overstated one §1 corrects). Add one note under *Migrate build documentation to Gradle* (line 60), carrying the deferred README work:

> - **Note:** the README's Continuous Integration subsection still lists Maven pipeline steps and claims JaCoCo/Surefire/Failsafe reports `-Pci` never produced — it becomes checkout, JDK setup, Gradle setup (wrapper validation + build cache), `./gradlew build`, `./gradlew :build-logic:check`

## 6. Verification / Definition of Done

**Locally verifiable, and the honest limit.** The workflow triggers only on `push` to `main` and `pull_request` targeting `main`, and this project's flow squash-merges into local `main` and pushes separately — so the workflow itself cannot run from this branch. Verification splits:

Before landing:

- `./gradlew build --console=plain` from a clean tree — green across all seven modules, including the five Testcontainers integration tiers (needs Docker). This is the exact command CI will run.
- `./gradlew :build-logic:check --console=plain` — green: `validatePlugins` plus the eleven `InstallGitHooksFunctionalTest` scenarios.
- Both commands piped through `cat -v` emit no `^[` escape sequences at all, confirming `--console=plain` overrides the committed `org.gradle.console=verbose`.
- `ci.yml` parses as YAML, and `if: '!cancelled()'` is quoted — an unquoted `!` is a YAML tag indicator and a hard parse error.
- No `mvn` invocation and no `-Pci` reference survives in `.github/workflows/ci.yml`.
- `.claude/rules/gradle.md` contains no surviving claim that CI does not run `:build-logic:check`.

On the first `main` push after merge — the only place these can be observed:

- The workflow runs to green, with both Gradle steps present and the wrapper-validation check passing inside `Set up Gradle`.
- Logs are plain: `> Task :… UP-TO-DATE` lines, no escape residue.
- The job summary appears (the `add-job-summary: always` default).
- The second `main` push shows `FROM-CACHE` / `UP-TO-DATE` outcomes, proving the cache persisted and was written on the default branch.

Optional, at the developer's discretion: pushing this branch and opening a PR to `main` would fire the `pull_request` trigger and move every observation above ahead of the merge. Worth it if the first post-merge run failing on `main` is unwelcome.

## 7. Out of scope / YAGNI

- **`cache-encryption-key`.** Only needed to cache configuration-cache entries; the configuration cache is deferred (asciidoctor blocks it).
- **`build-scan-publish`.** Requires accepting Develocity terms and publishes build data publicly.
- **Tuning `add-job-summary` / `add-job-summary-as-pr-comment`.** Defaults (`always` / `never`) are right.
- **A build matrix.** One JDK, one OS — the toolchain pins 25 and there is nothing to vary.
- **`--stacktrace`, `--scan`, `--continue`.** Add them to a failing run when a failing run exists.
- **GitHub dependency-graph submission** (`dependency-graph: generate-and-submit`, plus `contents: write`). Nothing breaks today because GitHub parses the still-present `pom.xml` files. **This is a real consequence of a later entry, not this one:** GitHub does *not* parse Gradle build scripts, so *Verify full parity, then remove Maven entirely* (line 74) silently drops Dependabot's view of these dependencies. It belongs as a note there.

## 8. Contingencies

- **`No matching toolchains found`.** `setup-java` ran after `setup-gradle`, or its `java-version` drifted. Toolchain auto-detection includes the JVM running Gradle, so ordering is the whole fix — never add a toolchain resolver, which `## Compilation` rejects for `--offline` and drift reasons.
- **Wrapper validation fails.** The committed `gradle-wrapper.jar` does not match a published checksum. Investigate the jar; **never** set `validate-wrappers: false` and never paper over it with a standalone validation step.
- **The run exceeds 30 minutes.** Read the task outcomes before raising the number — a cold cache and a genuinely stuck Testcontainers pull look different in the log. The timeout exists to catch a hang, not to accommodate an unbounded build.
- **An integration tier fails only in CI.** Docker is available on `ubuntu-latest` and these tiers already passed under Failsafe, so suspect a host-port or resource assumption rather than the migration. Every container takes a dynamic host port (verified for line 69), so a fixed-port collision would be new.
- **`:build-logic:test` fails with `EOFException` or `NoSuchFileException`.** That is the concurrency race in `## Build logic tests`, not a regression — it needs two Gradle builds in one working tree, which sequential steps in one job cannot produce. If it ever appears in CI, look for a second Gradle invocation, not a test bug.
- **Cache never restores.** Check whether the run was on a non-default branch, where `cache-read-only` is `true` by design. The build-logic suite is expected to execute every run regardless: `pluginUnderTestMetadata` bakes absolute classpath paths into a file on the test runtime classpath, so it is never `FROM-CACHE` across machines.

## 9. Git workflow

Two commits on `build/replace-maven-with-gradle-20-ci-workflow`: this spec on its own (`docs(gradle)`), then the implementation (`ci(gradle)`) — the same split the previous two entries used, committing the spec at approval time rather than riding the implementation commit.

## 10. Post-implementation notes

The canonical implementation is `.github/workflows/ci.yml`, `.claude/rules/gradle.md` and the `TODO.md` entries, not this document.

- **Every §6 "before landing" check passed, all run locally.** `./gradlew build --console=plain` → `BUILD SUCCESSFUL in 4m 49s`, `87 actionable tasks: 39 executed, 4 from cache, 44 up-to-date`, across all seven modules (2 libs + 5 services); every service ran both `test` and `integrationTest`, `spotlessCheck` rode `check` in all seven modules with no separate invocation, and four `generateGitProperties` tasks came back `FROM-CACHE`, so the local build cache was live. Run by the developer, not an agent — it needs Docker for the Testcontainers tiers.
- `./gradlew :build-logic:check --console=plain` → `BUILD SUCCESSFUL in 51.7s`, with `validatePlugins` and `test` both executing.
- `./gradlew spotlessCheck --console=plain` → `BUILD SUCCESSFUL in 10s` (`23 actionable tasks: 14 executed, 9 up-to-date`).
- **`--console=plain` measured to override the committed `org.gradle.console=verbose`, as §1 predicted.** Piping each command through `cat -v | grep -c '\^\['` returned `0` for `:build-logic:check` and `0` for the second `./gradlew build` — no cursor-control escapes reached the piped stream either time.
- **actionlint green.** `docker run --rm -v "$PWD:/repo:ro" --workdir /repo rhysd/actionlint:latest -color`, run by the developer → no output, `EXIT=0`. Confirms `ci.yml` parses as YAML and that `if: '!cancelled()'` kept its quotes.
- **The actionlint baseline (spec-plan Step 2) was deliberately not run.** Its only purpose was attributing post-edit findings to a cause; the post-edit run was clean, so there was nothing to attribute.
- The Maven-residue grep (`grep -nE 'mvn|-Pci|cache: maven' .github/workflows/ci.yml`) returned no output, `EXIT=1`. The stale-claim grep over `.claude/rules/gradle.md` returned no output, `EXIT=1`. The branch-pattern grep returned exactly one line, `build/replace-maven-with-gradle*`, with no `-` before the `*`.
- **Diff shapes, and the four protected regions untouched.** `ci.yml` 21 insertions / 5 deletions; `gradle.md` 3/3; `TODO.md` 4/6. The four protected regions of `ci.yml` — `name`, the `pull_request` trigger, the `concurrency` group, `permissions` — carry zero `+`/`-` lines, verified independently by the reviewer.
- **One incidental observation, not a regression.** The application build's summary still carries `Deprecated Gradle features were used in this build, making it incompatible with Gradle 10`. It predates this change and `--warning-mode=none` is forbidden (§4's rejection), so it will appear in CI logs too.
- **The workflow shipped diverges from §5's block, deliberately.** `origin/main` carries no `gradlew`, no `settings.gradle.kts` and no `build-logic`, and this branch is 21 commits ahead of it, so §6's premise — that a `main` push follows shortly and is where the workflow is first observed — does not hold. `main` sees this workflow only when the whole epic merges, which still needs the release workflow, the documentation migration and the Maven removal. Rather than ship an unexecuted merge gate, the `push:` trigger temporarily also matches `build/replace-maven-with-gradle*` and `setup-gradle` temporarily takes `cache-read-only: false`. Three places hold the revert: the two in-file `TEMPORARY (Maven→Gradle epic)` comments (on the `push:` trigger and on the `setup-gradle` step), the header comment's third line, and the `TODO.md` warning under *Verify full parity, then remove Maven entirely*.
- **Two further deviations from §5 survived review, both developer-approved.** The step-order sentence added to `## Compilation`'s toolchain-prerequisite bullet — stating that `actions/setup-java` must precede `gradle/actions/setup-gradle` — is not one of §5's three listed claims, but spec §8's first contingency treats step order as the whole fix for `No matching toolchains found`, and nothing else in the repository recorded it. The Dependabot note added under *Verify full parity, then remove Maven entirely* in `TODO.md` — recording that removing the `pom.xml` files drops GitHub's dependency-graph view of these coordinates, and how `dependency-graph: generate-and-submit` restores it — traces to §7's own "belongs as a note there" rather than to §5. No `gradle.md` bullet beyond the three §5 listed was reached; both additions above sit inside those same three bullets or in `TODO.md`, not in a fourth place.
- **Nothing had run on a GitHub runner as of this commit — three runs have executed on this branch since.** Every result above was local, and that held only until Task 3 pushed the branch. Runs `31217470678`, `31222111399` and `31251727769` ran afterward; see below for what they showed, recorded in a follow-up `docs(gradle)` commit rather than by rewriting this section's earlier bullets.
- **A whole-branch review after this commit was first written surfaced two further findings, both developer-approved and folded in by amending this same commit rather than stacking a new one.** `if: '!cancelled()'` overrides the implicit `success()` for every step above it, not only the build step, so a `Set up Gradle` failure — its headline failure mode is `validate-wrappers` rejecting the committed `gradle-wrapper.jar` — still let the build-logic step run `./gradlew`, re-executing the very jar validation had just rejected and reopening the wrapper-integrity gap §2 lists as a goal of this change. Fixed by adding `id: setup-gradle` to the setup step and extending the build-logic step's condition to `"!cancelled() && steps.setup-gradle.outcome == 'success'"`. Separately, the developer decided to pin `cache-provider: 'basic'` rather than accept `setup-gradle@v6`'s `'enhanced'` default — closed-source, Terms-of-Use-gated, and free for public repositories only today — recorded in §4's new Cache provider row, on the same reasoning §7 already used to reject `build-scan-publish`.
- **`cache-provider: 'basic'` is a permanent decision, not a third temporary scaffold.** It carries no `TEMPORARY (Maven→Gradle epic)` marker and is not among the items `TODO.md` lists for reverting when the epic merges — only the `push:` branch pattern and `cache-read-only: false` are.
- **Both fixes are now observed running, not merely confirmed by static checks.** `steps.setup-gradle.outcome == 'success'` gates correctly: run 3 attempt 2 shows `Set up Gradle` → success, `Build and test the project` → cancelled, `Build and test the build logic` → skipped, so the skip is attributable to the manual cancellation and not to the outcome gate — the behaviour this fix was written to produce. `cache-provider: 'basic'` is confirmed by runs 2 and 3: run 2 saved one monolithic `setup-java-Linux-x64-gradle-fba614bb…` entry (590.56 MiB, `Entries: 0 restored, 1 saved`), and run 3 attempt 1 restored it (`Entries: 1 restored, 0 saved`, ~8s to pull 619251643 bytes at 104.6 MB/s). Neither confirmation depends on actionlint or on reading `setup-gradle`'s documentation anymore, superseding the static-only confirmation this bullet originally recorded.
- **Three runs exist on this branch, run 1 predating the whole-branch review's fix wave.** All three use `gradle/actions/setup-gradle@v6` and `actions/setup-java@v5` with JDK 25 Temurin. Run 1 — `31217470678`, commit `c5f63992`, **`enhanced`** cache provider — ran because the branch was pushed at that commit before the review's fixes were amended in. Conclusion: success, job 2026-08-07T20:49:00Z → 20:57:34Z = **8m 34s**. `enhanced` wrote ten fine-grained cache entries — `gradle-dependencies-v2` 295.70 MiB, `gradle-wrapper-zips-v2` 133.47 MiB, `gradle-transforms-v2` 99.07 MiB, `gradle-generated-gradle-jars-v2` 40.20 MiB + 26.07 KiB, `gradle-home-v2|Linux-X64|build[…]-c5f6399…` 12.57 MiB, `gradle-build-cache-v2` 8.57 MiB, `gradle-kotlin-dsl-v2` 153.59 KiB, `gradle-groovy-dsl-v2` 89.57 KiB, `gradle-instrumented-jars-v2` 58.78 KiB — all ten deleted manually afterward, since the shipped workflow pins `cache-provider: 'basic'`, not `enhanced`.
- **Run 2 — `31222111399`, commit `971bb4ce`, `basic`, cold.** Conclusion: success, job 21:58:38Z → 22:07:03Z = **8m 25s**. `Basic caching did not find an entry to restore. Will start with empty state.` — the only caches present were `enhanced`-format and unreadable by `basic`. `./gradlew build --console=plain` → `BUILD SUCCESSFUL in 7m 27s`, `87 actionable tasks: 83 executed, 4 from cache`; `./gradlew :build-logic:check --console=plain` → `BUILD SUCCESSFUL in 39s`, `13 actionable tasks: 4 executed, 9 up-to-date`. All five `integrationTest` tasks executed, with no cache marker on any of them. `All Gradle Wrapper jars are valid`. Job summary: `Entries: 0 restored, 1 saved` — one monolithic entry, `setup-java-Linux-x64-gradle-fba614bb…`, **590.56 MiB**.
- **Run 3 attempt 1 — `31251727769`, commit `4d900cd5`, `basic`, warm. Conclusion: success.** Job 2026-08-08T09:57:52Z → 09:58:32Z = **40s**. `Cache hit for: setup-java-Linux-x64-gradle-fba614bb…`; `Received 619251643 of 619251643 (100.0%), 104.6 MBs/sec`; `Cache restored successfully`, restore took ~8s. `./gradlew build --console=plain` → `BUILD SUCCESSFUL in 21s`, `87 actionable tasks: 31 executed, 49 from cache, 7 up-to-date`; `./gradlew :build-logic:check --console=plain` → `BUILD SUCCESSFUL in 1s`, `13 actionable tasks: 1 executed, 3 from cache, 9 up-to-date`. `All Gradle Wrapper jars are valid`, with checksum `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7` recorded against `gradle/wrapper/gradle-wrapper.jar`. Job summary present (`Generating Job Summary`): `Entries: 1 restored, 0 saved`. Cache-effectiveness delta against run 2: whole job **8m 25s → 40s**, `build` **7m 27s → 21s**, `:build-logic:check` **39s → 1s** — the observation `cache-read-only: false` exists to produce. Every test task came back `FROM-CACHE` and none executed: `:libs:asapp-http-clients:test`, and for all five services both `test` and `integrationTest` where sources exist (`asapp-commons-url:test`, `asapp-config-service:test` and `asapp-discovery-service:test` are `NO-SOURCE`). **A warm green run replays cached test outcomes rather than re-running the tests** — see the falsified-claim note below for what that means for this workflow's guarantee.
- **Run 3 attempt 2 — same run `31251727769`, cancelled.** Job 10:01:05Z → 10:01:46Z, cancelled manually with `gh run cancel` mid-build. Step outcomes from the API: `Set up Gradle` → success, `Build and test the project` → cancelled, `Build and test the build logic` → skipped. This is the proof `!cancelled()` behaves as intended: because the setup step succeeded, the skip is attributable to the cancellation, not to the `steps.setup-gradle.outcome` gate, so the build-logic suite's ~40s (cold) / ~1s (warm) is never spent on a discarded run.
- **Logs carry exactly one escape sequence per Gradle step, not zero — and it is not Gradle's.** `cat -v | grep -c '\^\['` returns **1** for each Gradle step; that one hit is GitHub Actions' own cyan echo of the command it is about to run — `^[[36;1m./gradlew build --console=plain^[[0m` — not Gradle output. Gradle's own output contributes zero escape sequences.
- The `basic` provider advertises itself, in both the log and the job summary, on runs 2 and 3: `Basic Caching: This build uses the basic open-source caching provider. For faster builds and advanced features, consider switching to the Enhanced Caching provider.` Expected, not a defect.
- **`enhanced` cold (run 1, 8m 34s) and `basic` cold (run 2, 8m 25s) are not a controlled comparison.** The two runs are on different commits and different runners, so the nine-second difference is not evidence that `basic` costs nothing measurable in general — only that nothing large enough to matter surfaced in this one pair.
- The pre-existing `Deprecated Gradle features were used in this build, making it incompatible with Gradle 10` banner appears in every run, as expected; `--warning-mode=none` remains forbidden.
- `timeout-minutes: 30` was never approached across any of the three runs — the worst observed job was run 1's **8m 34s**.
- **§8's `Cache never restores` contingency claims the build-logic suite executes on every run because `pluginUnderTestMetadata` bakes absolute classpath paths that never match across machines — the evidence falsifies that on a GitHub runner, without any change to §8 itself.** Every job's workspace is `/home/runner/work/asapp/asapp` (verified in the logs), a fixed path, so the baked paths *do* match across runs there: in run 3 attempt 1, `:build-logic:test` and `:build-logic:validatePlugins` were both `FROM-CACHE`, and the step finished in 1s. `pluginUnderTestMetadata` itself still executed. §8's claim holds for a developer's machine, where the working-tree path varies; it does not hold for a GitHub runner, where it is constant.
- **A warm green run does not re-verify the tests.** In run 3 attempt 1 every test task was `FROM-CACHE`, replayed from run 2 where they genuinely executed. This is correct Gradle build-cache behaviour for a comment-only change (`4d900cd5` touched no source), but it means a green CI result on a documentation-only push carries no new evidence that the test suites still pass — only that nothing changed since the run that last executed them.
- **§9 budgeted two commits on this branch; there are four.** `d1b4ae6d` (this spec), `971bb4ce` (the implementation, itself an amended rewrite of the originally pushed `c5f63992`), `4d900cd5` (`ci(gradle): tighten the comments on the new workflow steps` — comment wording only, no behaviour change), and this `docs(gradle)` commit recording the runs.
- **Reaching `origin` took a push, an amend, and a force-push — not the single push §9 implies.** `c5f63992` was pushed at 2026-08-07 22:48 local and ran on a runner (run 1) before the whole-branch review's two findings were fixed. Fixing them amended that commit into `971bb4ce` instead of stacking a new one, so getting the fix to `origin` required a force-push, rewriting the branch's already-published history. Harmless on a solo branch, but the sequence was push → rewrite → force-push.
- **`4d900cd5` changed comment wording only and still fired a full CI run (run 3), because the temporary branch-pattern trigger matches every push to this branch regardless of content.** No `.yml` behaviour changed and no source changed; the push nonetheless produced both the warm-cache observation (attempt 1) and the cancellation observation (attempt 2) that make up run 3.

Notable deltas from the whole-branch review that followed, most consequential first:

- **CI runs `asciidoctor` as a third gating step (revises §1).** §1's "CI stops rendering `api-guide.html`" row is now false; `ci.yml` holds the truth.
- **`--stacktrace` on all three Gradle invocations (revises §7).** §7 deferred it until a failing run existed; the review applied it now — `ci.yml` holds it.
- **§5's workflow block matches nothing shipped (revises §5).** Six accumulated edits — `id`, `validate-wrappers`, `cache-provider`, `--stacktrace`, the asciidoctor step, both scaffolds.
- **`TODO.md` gained a subtask and three backlog entries (revises §5).** §5 prescribed one tick, five deletions and one note; review deferrals were booked instead of fixed.
- **`validate-wrappers: true` stated explicitly, not left defaulted (revises §4).** An action default can move silently; the `Set up Gradle` step now states it.
- **Ten commits on this branch, not two (revises §9).** Review fixes, comment tightening and backlog bookings landed separately — superseding this section's earlier "there are four".

## 11. References

- `gradle/actions` docs, *setup-gradle* — caching, `cache-read-only` default, wrapper validation, job summaries; input defaults read from `setup-gradle/action.yml`
- GitHub Actions docs, *Evaluate expressions* — `success()` / `failure()` / `cancelled()` / `always()` status check functions and the implicit step `if`
- GitHub Actions docs, *Control the concurrency of workflows and jobs* — concurrency groups and `cancel-in-progress`
- Gradle User Manual, *Command-Line Interface* — `--console` modes and the log-level flags
- Gradle User Manual, *Composite builds* — why `build` does not reach an included build's `check`
- `docs/superpowers/specs/v0.5.0/2026-07-21-gradle-formatting-design.md` — the `spotlessCheck`-on-`check` decision that makes `-Pci` unnecessary, and its own note that the CI swap is this entry
- `docs/superpowers/specs/v0.5.0/2026-08-02-gradle-build-logic-tests-design.md` — the suite this change first automates
- `docs/superpowers/specs/v0.5.0/2026-08-05-gradle-build-bom-reuse-design.md` §10 — the post-implementation note that routed this requirement into `TODO.md`
- `.claude/rules/gradle.md` — `## Shared Build Configuration`, `## Compilation`, `## Formatting`, `## API documentation`, `## Full build`, `## Build logic tests`
