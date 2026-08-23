# Gradle rule file — cut to the traps

**Date**: 2026-08-11
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md:35` → "Cut the Gradle rule file to what earns its place", under `TODO.md:34` "Keep Claude Code files in sync with the migration"
**Scope**: One bullet rewritten in `.claude/rules/rule-authoring.md` (done). `.claude/rules/gradle.md` cut from 168 rules to 31 and regrouped into 5 sections. No other rule file, no skill, no agent, no build script, no source.

## 1. Context

`.claude/rules/gradle.md` has had three cleanup passes and is still 245 lines / 168 bullets — 2.45× `rule-authoring.md:71` and 3.4× the next-largest rule (`testing-core.md`, 49 bullets).

| | lines | bullets | words | words/bullet |
|---|---|---|---|---|
| before `4b19db56` | 316 | 222 | 19,015 | 85 |
| `4b19db56` rewrite | 281 | 193 | 5,352 | 27 |
| `bdf1b493` prune | 245 | 168 | 4,593 | 27 |
| `5f48a9be` form fixes | 245 | 168 | 4,613 | 27 |

None of the three could fix the size, because each worked under a constraint that ruled the fix out:

- `4b19db56` compressed prose inside bullets. Its spec §3 forbade deleting any constraint; §7 forbade restructuring. It hit 27 words/bullet — the corpus median — and stopped.
- `bdf1b493` deleted 26 bullets under "earns its place", applied conservatively.
- `5f48a9be` fixed wording only.

Words per bullet bottomed out in pass one. The remaining driver is item count, and item count only falls by deleting.

**The mandate changed.** Prior passes preserved every constraint on principle. This pass applies the opposite: `gradle.md` holds rules and conventions, not the technology and design decisions taken during the migration. Those decisions are recorded in the 23 specs under `docs/superpowers/specs/v0.5.0/`, which is where they belong.

## 2. The standard was missing the test that does the work

`rule-authoring.md:47` already carried two thirds of the keep-test — "State the decisions and constraints the model wouldn't already follow or infer from the code."

The missing third is the **cost of violation**. A rule that a formatter, compiler or failing test catches does not need to exist; the failure prompts its own correction. What earns a rule is the quiet one.

`rule-authoring.md:48` carried a narrow version of this, limited to formatters ("Don't do a linter's job"). It has been widened:

> **Don't restate what fails loudly.** Omit what a formatter, compiler or failing test catches — Spotless included; a loud failure corrects itself. A rule earns its place on the quiet one: an alternative that looks right, is wrong, and says nothing.

"Build" was rejected as the operative word: this repo has a `## Build` section in `CLAUDE.md`, `gradle.md` is *about* the build, and the phrase would read as the Gradle build rather than as any self-announcing failure. "Fails loudly" pairs with "says nothing" inside the bullet, so the test explains itself.

Phrased as an omit-rule it is safe for the other 20 rule files. It removes only what something else catches, so consistency conventions — `testing-core.md`'s `@since` policy, `ports-adapters.md`'s adapter naming — are untouched. That distinction matters and is recorded in §7.

## 3. The keep-test

Keep a rule only when all three hold:

1. Claude would plausibly do the wrong thing
2. Reading the code does not reveal it
3. Getting it wrong **fails quietly**

Point 3 does most of the cutting. Code shows what *is*, never what *must not be* — so what survives is almost entirely a prohibition against a plausible alternative that fails without complaint.

## 4. Key decisions

### 4.1 One file. The split was evaluated and rejected

Splitting the per-capability content onto a narrower glob (`**/build-logic/src/main/kotlin/*.gradle.kts`) was designed in full and then rejected on evidence. It would have stopped ~105 bullets loading on the 11 build files that are not convention plugins.

The counter-evidence: over the last 6 months, **19 of 21 commits touching a build file touched a convention plugin**. Only 2 did not. Splitting would help 2 changes in 21 and mildly hurt the other 19, which would load both files (~265 lines) instead of one.

Recorded here so it is not re-proposed. It is worth revisiting only if steady-state editing shifts decisively toward module scripts — the 6-month window *is* the migration, which is inherently convention-plugin-heavy.

Consequence: the 12 constraints filed under the wrong heading are regrouped in place, not relocated to another file.

### 4.2 The 31 survivors

49 of the 168 source bullets are consumed; 119 are cut outright. Merges are noted by their source lines.

**Placement** — where configuration lives

| # | Rule | From |
|---|---|---|
| 1 | Cross-cutting config only in convention plugins — never a module script, `subprojects {}`, `allprojects {}` or `buildSrc` | `:20`, `:22`, `:23`, `:58`, `:67` |
| 2 | Never `enforcedPlatform(...)` or `io.spring.dependency-management` in `build-logic` — both downgrade the embedded `kotlin-stdlib` | `:29` |
| 3 | `mavenCentral()` before `gradlePluginPortal()` in `build-logic/settings.gradle.kts` — the Portal proxies Central | `:35` |
| 4 | No `repositoriesMode` in `build-logic/settings.gradle.kts`, deliberately — it breaks IntelliJ import (gradle/gradle#15732) | `:36` |
| 5 | The root project registers no lifecycle task names and no aggregator tasks — a no-op root task succeeds while doing nothing | `:84`, `:167`, `:198` |

**Task graph** — what runs when

| # | Rule | From |
|---|---|---|
| 6 | `mustRunAfter`, never `shouldRunAfter`, for tests-after-formatting — Gradle drops the weaker edge under `org.gradle.parallel` | `:72`, `:73` |
| 7 | No repo-wide aggregate coverage report — it would cover the unit tier only while looking complete | `:83` |
| 8 | Toolchain-derived values wired lazily with `.map { }`, never `.get()` | `:103`, `:139`, `:202` |
| 9 | Never derive pitest `targetClasses` / `targetTests` from `project.name` — a narrowed mutant population still clears a 100% ratio | `:106` |
| 10 | Never bake `clean` into `fullBuild` — it discards the incremental state and build cache | `:171` |

**Dependencies and versions**

| # | Rule | From |
|---|---|---|
| 11 | `liquibase-core` stays on the `build-logic` classpath beside the plugin — task discovery needs it (liquibase-gradle-plugin#182) | `:211` |
| 12 | Keep `-parameters` in `asapp.java-conventions` — `asapp-http-clients` has an unnamed `@PathVariable` | `:134` |
| 13 | The jackson CVE override needs both mechanisms — `bomProperty` is silently ignored once Boot auto-imports the BOM | `:135` |
| 14 | Pin the SBOM plugin at `3.3.0`; step down `3.3.0` → `3.2.4` → `3.0.1`, never straight to `3.0.1` | `:150`, `:159` |
| 15 | SBOM `includeConfigs = listOf("productionRuntimeClasspath")` — an empty list means every resolvable configuration | `:151`, `:152` |
| 16 | Catalog and dependency-block order: origin order, alphabetical within it | `:235`, `:236`, `:237` |

**Build output**

| # | Rule | From |
|---|---|---|
| 17 | Never `java.withJavadocJar()` / `withSourcesJar()` — they add the jars to `assemble` | `:123` |
| 18 | Never `from(main.allSource)` on `sourcesJar`, and never fix it with `dependsOn("generateGitProperties")` | `:125`, `:126` |
| 19 | Any javadoc/sources policy change goes into both convention files, kept identical | `:122` |
| 20 | `git.properties` `keys` stay restricted — the defaults ship the build host and the developer's email | `:140` |
| 21 | The build-cache `normalization { runtimeClasspath { } }` block — without it `integrationTest` is never up-to-date | `:142` |
| 22 | SBOM scope knobs on `cyclonedxDirectBom`, metadata knobs on `cyclonedxBom`, never the other way | `:149` |
| 23 | `asciidoctorExt` activated via `configurations("asciidoctorExt")`, and `snippetsDir` matching the `snippets` attribute — both fail by rendering nothing | `:113`, `:115` |
| 24 | `environment.put(...)` on `bootBuildImage`, never `environment = mapOf(...)` — assignment replaces the whole map | `:203` |

**Developer workflow**

| # | Rule | From |
|---|---|---|
| 25 | Every wrapper upgrade passes `--gradle-distribution-sha256-sum` — `./gradlew wrapper` silently deletes `distributionSha256Sum` otherwise | `:54` |
| 26 | Set compiler encoding per-task, never daemon-wide via `org.gradle.jvmargs` — that clobbers Gradle's default JVM args | `:63` |
| 27 | `bootRun` profile via `systemProperty`, never `args(...)` — `--args` replaces the argument list wholesale | `:184`, `:185` |
| 28 | `bootRun` `workingDir = layout.settingsDirectory.asFile`, never `rootDir` — config-service resolves `searchLocations` against `${user.dir}` | `:188`, `:189` |
| 29 | Liquibase `searchPath` is mandatory, anchored at `src/main/resources` — it keeps `DATABASECHANGELOG.FILENAME` consistent with the app's boot-time migration | `:214`, `:215` |
| 30 | Liquibase `logLevel` set explicitly; never assign the whole `arguments` map — `Activity` seeds `[logLevel: 'info']` | `:217` |
| 31 | Never run `spotlessInstallGitPrePushHook` — it writes an unmanaged `pre-push` into the shared hooks directory | `:226` |

### 4.3 What is cut, by category

| Category | Approx. bullets | Why |
|---|---|---|
| Visible in the code | ~45 | Fails test 2 — the pattern is in the file being edited |
| Standard Gradle knowledge | ~12 | Fails test 1 — Kotlin DSL, `include(...)` sorting, `rootProject.name` |
| Fails loudly | ~20 | Fails test 3 — compile errors, `NoClassDefFoundError`, task-realization failures, `FAIL_ON_PROJECT_REPOS` |
| Decision records | ~30 | Not rules — "we deliberately left X unset", rejected alternatives, cost notes |
| Maven parity | ~6 | Argues against a build system being removed |
| Developer commands | ~6 | `CLAUDE.md` owns these (see §7) |

### 4.4 Sections

Five, matching the house pattern for small rule files (`configuration.md` 2 sections at 14 lines, `code-style.md` 4 at 26, `error-handling.md` 3 at 24): **Placement · Task graph · Dependencies and versions · Build output · Developer workflow**, plus `## Further reading`.

A `Contents:` line is dropped — `rule-authoring.md:22` calls for one only above ~100 lines.

### 4.5 Line budget

~62 lines: 6 frontmatter, 2 intro, 15 section overhead (5 × heading-plus-blank), ~36 rule lines, 4 further reading. Rules that wrap to a second line push this toward 67. **This is a consequence, not a target** (`rule-authoring.md:72`).

## 5. Changes by file

### `.claude/rules/rule-authoring.md` — done

One bullet at `:48` widened, per §2. Line count unchanged. The file is untracked, so this lands in the same commit.

### `.claude/rules/gradle.md` — rewritten

245 lines / 168 bullets → ~62 lines / 31 rules. Frontmatter unchanged — all four globs stay.

Review findings from `docs/reviews/2026-08-11-gradle-review.md` are resolved by the cut rather than individually: M1 (two names for config and discovery), M3/M4 (illegal bold spans), M5 (stale starter pattern), S3 (intro understates scope), S4, S5 (Contents line), N1, N2 all sit on bullets that are being deleted or rewritten. M2 and S1–S2 apply to surviving rules and are fixed in the rewrite. S6 (the `.gradle.kts` glob also matching `build-logic/build/kotlin-dsl/plugins-blocks/extracted/`) is untouched and stays open.

## 6. Verification / Definition of Done

Mechanical:

1. `grep -c '^- ' .claude/rules/gradle.md` = 31.
2. `wc -l` ≤ 67.
3. Five `##` sections plus `## Further reading`; all four `paths:` globs still quoted, forward slashes only.
4. No bold span is a whole sentence or a bare code span (`rule-authoring.md:54`).

Content — the guard against silent loss:

5. **Survivor check.** Each of the 31 rules in §4.2 appears exactly once, and its merge sources are fully entailed. For a merged rule this is an *entailment* check, not a presence check: the merged text must forbid the same thing, for the same set of files, as every source bullet. Presence-checking is insufficient — the prior spec's own 7→2 merge (§4.3) would have been false for 4 of its 7 sources.
6. **Orphan check — run before deleting anything.** For each of the 119 cut bullets, confirm its distinctive terms appear in `docs/superpowers/specs/v0.5.0/`. Anything absent exists only in this rule and would be lost outright. Write it into the relevant spec first, then cut.
   Known orphan: `:125` / `:126` (`from(main.allSource)`, and why `dependsOn("generateGitProperties")` is the wrong fix). The review record states their rationale exists in no design spec — the spec shipped the form they now forbid. Both are survivors here, so nothing is lost, but the same class may exist among the cuts.
7. Read the file end to end for constraints left dangling by a deleted neighbour — particularly cross-references that pointed at a cut bullet.

## 7. Out of scope / YAGNI

- **A second rule file.** Rejected on evidence, §4.1.
- **`gradle-build-logic.md`.** 43 lines / 27 bullets, and it would not pass §3 unchanged — `:39`, `:42` and `:43` restate `gradle.md`. A separate pass under the same test.
- **The other 19 rule files.** The widened `rule-authoring.md:48` now applies to them, but sweeping them is its own task. Note the asymmetry: `gradle.md` is nearly all traps, while `testing-core.md` and `ports-adapters.md` are nearly all consistency conventions, which the omit-rule leaves alone.
- **`CLAUDE.md`'s Gradle commands.** Now tracked as the sibling subtask `TODO.md:37`, "Update the remaining Claude Code files for Gradle", alongside `asapp-release` (9 Maven references), `devops-engineer.md` (1) and `asapp-draft-commit-msg` (1). `CLAUDE.md` still names the Maven forms at `:18`, `:19` and `:22`. Six command bullets are cut here on the basis that `CLAUDE.md` owns them, so **land that sibling first** or the project documents the Gradle commands nowhere.
- **`TODO.md:52`** — "reword the rule that mandates `rootProject.file(...)`". That rule (`:91`) is cut here as a loud failure, so only the code half of that finding remains. Update the entry rather than leaving it pointing at a deleted rule.
- **`TODO.md:45`** — "Add cleaning convention to gradle.md". Any addition must pass §3 like everything else; most block-ordering conventions will be visible in the code once the cleanup lands, and so fail test 2.

## 8. Contingencies

- **A cut bullet has no spec home (check 6).** Write it into the relevant v0.5.0 spec before deleting. Never delete to hit the line count.
- **A survivor turns out to fail loudly after all.** Cut it; 31 is not a floor.
- **The file lands materially over 67 lines.** Do not trim to fit. Re-check whether a survivor is really three rules in one bullet, and split or cut on the test, not on the number.

## 9. Git workflow

Branch `build/replace-maven-with-gradle-clean-gradlemd` (current). One commit covering both files:

```
docs(gradle): cut the Gradle rules to the traps
```

## 10. References

- `.claude/rules/rule-authoring.md` — the standard, `:48` widened by this spec
- `docs/superpowers/specs/v0.5.0/2026-08-11-gradle-rules-cleanup-design.md` — the compression pass this one reverses the premise of
- `docs/reviews/2026-08-11-gradle-review.md`, `docs/reviews/2026-08-11-gradle-clean-review.md` — the two review passes
- `docs/superpowers/specs/v0.5.0/` — 23 specs, the archive every cut rationale must be found in

## 11. Post-implementation notes

The canonical implementation is `.claude/rules/gradle.md`, `.claude/rules/gradle-task-types.md`, and the build scripts and `CLAUDE.md` sections that now carry the relocated rules.

Notable deltas:

- **Rules moved into the code they govern (revises §4.3).** Twelve survivors became comments at their config sites; the spec designed only survival or deletion.
- **Twenty rules survive, not thirty-one (revises §4.2).** Relocation and further pruning cut past the target; `gradle.md` is now 49 lines.
- **A second Gradle rule file ships (revises §4.1).** The rejected capability split held; the deferred file-kind split landed as `gradle-task-types.md`.
- **Build scripts and sources changed after all (revises Scope).** `integrationTest` now `mustRunAfter` `test`, so the tier edge survives enabling parallelism.
- **A different five sections (revises §4.4).** Structure, Derived values, Task graph, Declaration order, Developer workflow; Build output dissolved into the code comments.
- **The frontmatter narrowed to six globs (revises §5).** `**/*.gradle.kts` also matched Gradle's extracted convention-plugin copies, so three exact shapes replaced it.
- **A new invocation trap was added (revises §4.2).** `./gradlew :build` resolves to `:buildEnvironment` — it reports success and builds nothing.
- **`CLAUDE.md` converted in the cut commit (revises §7).** Merging the sibling edit removed the sequencing hazard the spec warned about.
- **`rule-authoring.md`'s widened test never landed (revises §5).** The file is still untracked, so the keep-test travels with nothing.
