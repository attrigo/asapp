# Reuse the Spring Boot BOM for the build's own dependency versions — design spec

**Date**: 2026-08-05
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Reuse the Spring Boot BOM for the build's own dependency versions" (line 29).
**Scope**: Import `spring-boot-dependencies` as a Gradle platform on `build-logic`'s own classpath so its three hand-copied version pins go versionless. Three files changed (`gradle/libs.versions.toml`, `build-logic/build.gradle.kts`, `.claude/rules/gradle.md`), plus `TODO.md` edits. No new file, no new test, no change to the application's dependency resolution, no change to any command, no README edit, no `pom.xml` edit.

## 1. Context

`build-logic` is a `pluginManagement` included build with its own dependency classpath, entirely separate from the application's. The application's versions come from the Spring Boot BOM, imported through `io.spring.dependency-management` in `asapp.library-conventions` and auto-imported by the Boot plugin in `asapp.service-conventions`. `build-logic` imports no BOM at all, so three of its versions are hand-copied numbers in the root catalog, each carrying the same trailing comment:

```toml
assertj = "3.27.7" # Keep in sync with the Boot BOM
junit-bom = "6.0.3" # Keep in sync with the Boot BOM
liquibase = "5.0.2" # Keep in sync with the Boot BOM
```

**Why this entry exists.** The database-migration subtask built a `verifyLiquibaseVersionAlignment` check that compared the catalog pin against the resolved `liquibaseRuntime` version, verified it, then rolled it back — `2026-08-01-gradle-database-migration-commands-design.md:292` records the reasoning: "guarding an invariant is the wrong shape when the invariant can be deleted instead. `build-logic` imports no Boot BOM only because it was never wired to one." The build-logic-tests subtask then added two more copies of the same duplication (`junit-bom`, `assertj`). Today the comments plus two `gradle.md` bullets are the only thing holding the three numbers equal to the BOM.

**Honest grading of what this is worth**, recorded so a future reader does not over-read the entry:

| Pin | Value of removing the duplication |
|---|---|
| `liquibase` | **Mild correctness.** The catalog pin governs the *apply-time* copy only, which decides which of the 41 Liquibase tasks get registered; the runtime copy is already versionless and always equals the application's. A stale pin skews command discovery, it does not corrupt a migration. |
| `junit-bom`, `assertj` | **Housekeeping only.** Nothing requires `build-logic`'s test framework to match the application's — that alignment was self-imposed by the build-logic-tests subtask ("so `build-logic` and the application never drift apart"), and the maintenance obligation exists only because that choice was made. |

So this is tidiness with a small correctness component, not a fix. It is worth doing because the end state is *less* configuration than the alternatives, on a stock Gradle mechanism.

**Measured before designing.** A throwaway project under the scratchpad, built with this repository's own wrapper (Gradle 9.6.1), applying `kotlin-dsl` and declaring `build-logic`'s eight dependencies plus the platform, resolved `compileClasspath` / `runtimeClasspath` / `testRuntimeClasspath`:

| Question | Measured answer |
|---|---|
| Do the versionless coordinates resolve to today's pins? | Yes, all four: `liquibase-core` → **5.0.2**, `assertj-core` → **3.27.7**, `junit-jupiter` → **6.0.3**, `junit-platform-launcher` → **6.0.3** |
| Is anything downgraded? | **No.** Every substitution in all three trees is an upgrade. `platform()` imports BOM entries as *require* constraints, so conflict resolution is highest-wins |
| Does the BOM's `kotlin.version` (2.2.21) displace Gradle's embedded Kotlin? | **No** — `compileClasspath` reports `kotlin-stdlib:2.2.21 -> 2.3.21` and `kotlin-reflect:2.2.21 -> 2.3.21` |
| Does the BOM's `commons-lang3` (3.19.0) displace Liquibase's? | **No** — `3.19.0 -> 3.20.0` |
| Net change to the consumer-visible plugin classpath (`runtimeClasspath`) | Two things: `commons-logging` 1.3.5 → **1.3.6**, and the four Kotlin stdlib facade artifacts 1.9.10 → **2.2.21**. No module added, none removed (66 → 67 nodes, the extra being the BOM itself) |
| Jackson | **3.1.0 before and after**, unchanged |

The Kotlin facade move is worth naming: `build-logic` currently compiles against the embedded 2.3.21 while its *runtime* classpath resolves 1.9.10 transitively. The BOM raises the runtime side to 2.2.21, which narrows a pre-existing compile/runtime gap rather than opening one.

**One coupling this accepts.** After the import, a Spring Boot upgrade also moves `build-logic`'s test framework and its plugin-classpath transitives. Boot 4.1 (v0.6.0) will shift the build's own JUnit in the same change that shifts the application's. That is the intended direction — one number describes what this repository builds against — but it is a new coupling, not a free lunch.

## 2. Goals

- **One source of truth.** A Boot upgrade changes `spring-boot` in the catalog and nothing else; no coordinate on `build-logic`'s classpath needs a matching hand edit.
- **Byte-identical resolution.** Every declared coordinate resolves to exactly the version it resolves to today. This change is version-neutral by construction, not by luck.
- **No downgrade anywhere**, in particular not to Gradle's embedded Kotlin, which the Kotlin DSL compiles the five convention plugins against.
- **Net less configuration**: three versions and three library entries out, one library entry and one dependency line in.
- **The application's dependency resolution is untouched.** `build-logic`'s classpath is the *buildscript* classpath; the application resolves through `io.spring.dependency-management` inside the convention plugins.

## 3. Non-goals

- **Aligning the build's Jackson with the application's CVE override.** The build classpath resolves Jackson 3.1.0 while the application ships the overridden 3.1.1 — identical before and after this change, build-time only, never packaged. Out of scope, and not a regression this introduces.
- **Making `picocli` BOM-managed.** The Boot BOM genuinely does not manage it (which is why `liquibaseRuntime` declares it from the catalog); its `# Required by Liquibase's CLI, which doesn't pull it in` comment stands unchanged.
- **New tests.** `:build-logic:check`'s eleven functional scenarios run on the same AssertJ and JUnit versions before and after, so they are the regression net for this change, not a target for new assertions. §6 lists the verification instead.
- **Touching the application's BOM imports.** `asapp.library-conventions`' `mavenBom(...)` block and `asapp.service-conventions`' `jackson-bom.version` override are unrelated mechanisms on unrelated configurations.
- **Version alignment for the two Gradle plugin coordinates the BOM does not manage** (`liquibase-gradle`, and the six non-Spring plugins). They stay catalog-pinned; nothing manages plugin artifacts.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Mechanism | **`implementation(platform(libs.spring.boot.dependencies))`** | Stock Gradle. `platform()` imports the BOM's `dependencyManagement` entries as *require* constraints, so highest-wins conflict resolution applies and nothing can be forced backwards — measured, §1. `testImplementation` extends `implementation`, so one declaration governs the build scope and the test scope both. |
| Rejected: `enforcedPlatform()` | **Never** | Strict semantics would *force* the BOM's versions: measured, `kotlin-stdlib` 2.3.21 → 2.2.21 on the classpath the Kotlin DSL compiles against, and `commons-lang3` 3.20.0 → 3.19.0 under Liquibase. |
| Rejected: apply `io.spring.dependency-management` in `build-logic` | **Never** | Two independent reasons. Mechanically, `build-logic` declares that plugin as an `implementation` dependency *for its consumers*, so applying it to itself needs `plugins { id(…) version "…" }`, which needs a `pluginManagement { repositories { … } }` block in `build-logic/settings.gradle.kts` and a **literal** version there — settings-level `plugins {}` cannot read the catalog (the limitation the toolchain-resolver rule already records). Semantically, its Maven "managed version wins" behaviour has the same downgrade hazard as `enforcedPlatform()`. |
| Rejected: an automated alignment guard | **Already built and rolled back** | `verifyLiquibaseVersionAlignment` (see §1) keeps three hand-kept copies, adds a fourth moving part, and can only fail *after* a Boot bump instead of making the bump a no-op. |
| Rejected: delete the obligation instead of the duplication | **Considered and declined** | The cheaper alternative — keep the three pins as `build-logic`'s own choices, delete the three `# Keep in sync` comments and the two `gradle.md` mandates — is zero-risk and zero-coupling but leaves more configuration standing and accepts Liquibase apply-time drift. Developer decision to take the BOM import instead. |
| Catalog entry vs. inline coordinate | **A `spring-boot-dependencies` library entry** | `platform(libs.spring.boot.dependencies)` is type-safe and matches how `spring-boot-gradle-plugin` is declared beside it. The convention plugins use `mavenBom("…:${libs.findVersion(…)}")` string interpolation only because `mavenBom()` takes a string; `platform()` does not. Both forms read the same `spring-boot` version. |
| Coordinate form for the versionless three | **String literals**, e.g. `implementation("org.liquibase:liquibase-core")` | The established form in this repository for BOM-managed coordinates: `liquibaseRuntime("org.liquibase:liquibase-core")`, `liquibaseRuntime("org.postgresql:postgresql")`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`. A catalog entry exists to carry a version; these no longer have one. |
| Block ordering in `build-logic/build.gradle.kts` | **A leading `// BOM` → `// Spring Boot` group, above `// Build`** | The platform governs both existing scope groups (`liquibase-core` under `// Build`, AssertJ and JUnit under `// Test`), so `## Ordering`'s "a `platform(...)` BOM import leads its scope+origin group" cannot be applied literally. A leading group mirrors the CVE `constraints {}` block already leading `dependencies {}`, reuses the catalog's own `BOM` scope name, and signals that it governs every group below. Developer decision. |
| The dead `## Ordering` example | **Rewritten, not patched** | That bullet's example is `platform(libs.junit.bom)` before `assertj-core` in `build-logic/build.gradle.kts` — the exact line this change deletes. It becomes the leading-`// BOM`-group rule. |
| Verification depth | **Targeted, not `fullBuild`** | `build-logic`'s classpath is the buildscript classpath; the application's resolution comes from the convention plugins and is untouched. §6 lists what is actually at risk: plugin resolution, the Liquibase task registration, and Spotless (whose tree the plugin-runtime deltas sit in). |

## 5. Changes by file

**`gradle/libs.versions.toml`** — remove three versions and three libraries, add one library.

Removed from `[versions]` → `# Build` → `## Org` (the three carrying `# Keep in sync with the Boot BOM`):

```toml
assertj = "3.27.7"
junit-bom = "6.0.3"
liquibase = "5.0.2"
```

That group keeps `asciidoctor-gradle`, `asciidoctorj`, `jacoco`, `liquibase-gradle`. `picocli` is untouched under `## Other`.

Removed from `[libraries]`: `assertj-core` and `junit-bom` (`# Build` → `## Org`), and `liquibase-core` (`# Build` → `## Org`). Each was referenced from exactly one place, `build-logic/build.gradle.kts` — verified.

Added to `[libraries]` → `# Build` → `## Spring Boot`, alphabetically ahead of `spring-boot-gradle-plugin`:

```toml
spring-boot-dependencies = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
```

The `spring-boot = "4.0.5"` version stays where it is, under `# BOM` → `## Spring Boot`, now read by three places instead of two — the `spring-boot-gradle-plugin` library entry, `asapp.library-conventions`' `mavenBom(...)` import, and this new entry.

**`build-logic/build.gradle.kts`** — the `dependencies { }` block becomes:

```kotlin
dependencies {
    // BOM
    // Spring Boot
    implementation(platform(libs.spring.boot.dependencies))

    // Build
    // Spring Boot
    implementation(libs.spring.boot.gradle.plugin)
    // Spring
    implementation(libs.spring.dependency.management.plugin)
    // Org
    implementation(libs.asciidoctor.gradle.plugin)
    implementation("org.liquibase:liquibase-core")
    implementation(libs.liquibase.gradle.plugin)
    // Other
    implementation(libs.gradle.git.properties.plugin)
    implementation(libs.gradle.pitest.plugin)
    implementation(libs.spotless.plugin)

    // Test
    // Org
    // TestKit itself arrives with java-gradle-plugin, which kotlin-dsl applies
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

Three edits inside it: the new leading group; `libs.liquibase.core` → the versionless literal, keeping its alphabetical slot between `asciidoctor-gradle-plugin` and `liquibase-gradle-plugin`; and in `// Test`, `testImplementation(platform(libs.junit.bom))` deleted outright with `libs.assertj.core` → the versionless literal. Everything above `dependencies { }` is unchanged.

**`.claude/rules/gradle.md`** — three bullets rewritten, one invariant added:

1. `## Database migrations`, the "**Two Liquibase versions, deliberately**" bullet. Its premise ("`build-logic` imports no Boot BOM and the version must be literal there") is now false and there is one version, not two. Keep what stays true: the runtime copy is versionless so the CLI always runs the Liquibase the application runs. Drop the "keep the catalog pin equal to Boot's managed `liquibase.version` on every Boot upgrade" mandate.
2. `## Build logic tests`, the "JUnit and AssertJ are pinned in the **root catalog**…" bullet. Both are now BOM-managed and versionless; drop "**keep them in sync on every Boot upgrade**, the same hand-kept duplication as `liquibase`". The Spock rejection in the same bullet is untouched.
3. `## Ordering`, the **Dependency blocks** bullet. Replace the `platform(libs.junit.bom)` example with the rule that a BOM import governing more than one scope group leads `dependencies {}` as its own `// BOM` group, above `// Build` — the CVE `constraints {}` precedent in the same bullet. The single-group form stays documented; there is just no live example of it left in the repository.
4. New bullet, `## Shared Build Configuration` (where `build-logic`'s own shape is described): `build-logic` imports `spring-boot-dependencies` via `platform()`, **never** `enforcedPlatform()` and never by applying `io.spring.dependency-management` to itself — with the measured Kotlin-downgrade reason and the settings-level `plugins {}` catalog limitation, so the next reader does not retry either.

**`TODO.md`** — tick line 29's subtask and remove its four notes, which this spec absorbs. The count correction belongs here too: the Warning says "all six build plugins"; there are **seven** plugin artifacts on that classpath (`spring-boot-gradle-plugin`, `dependency-management-plugin`, `asciidoctor-gradle-jvm`, `liquibase-gradle-plugin`, `gradle-git-properties`, `gradle-pitest-plugin`, `spotless-plugin-gradle`) plus `liquibase-core`, and seven applied plugin ids across the convention plugins.

## 6. Verification / Definition of Done

- `./gradlew :build-logic:dependencies --configuration runtimeClasspath` — diffed against a pre-change baseline captured from the same command before editing anything (or from a stash of the pre-change tree). The only differences are `commons-logging` 1.3.6, the four Kotlin facades at 2.2.21, and the `spring-boot-dependencies` node. No module added or removed beyond that, and **no version lower** than the baseline anywhere.
- `./gradlew :build-logic:dependencies --configuration compileClasspath` — `kotlin-stdlib` and `kotlin-reflect` still resolve **2.3.21**; `liquibase-core` resolves **5.0.2**.
- `./gradlew :build-logic:dependencies --configuration testRuntimeClasspath` — `assertj-core` **3.27.7**, `junit-jupiter` **6.0.3**, `junit-platform-launcher` **6.0.3**.
- `./gradlew :build-logic:check` — all seven plugin artifacts resolve, `validatePlugins` passes, the eleven `InstallGitHooksFunctionalTest` scenarios pass.
- `./gradlew :services:asapp-users-service:tasks --all` — the Liquibase task list still registers (the `NoClassDefFoundError: liquibase/Scope` failure mode the second classpath entry exists to prevent).
- `./gradlew spotlessCheck` across all seven modules — clean. The plugin-runtime deltas sit in Spotless's dependency tree, so this is the one plugin whose behaviour could plausibly shift.
- `./gradlew :services:asapp-users-service:build` — compiles and passes the unit tier, proving the five convention plugins still configure and apply.
- `./gradlew help` — no new deprecation warning beyond the known Liquibase-plugin `Project.container(Class, Closure)` one.
- The catalog contains no `# Keep in sync with the Boot BOM` comment, and `gradle.md` contains no surviving sync mandate.

## 7. Out of scope / YAGNI

- Dependency locking or verification for `build-logic`'s classpath — both are already Backlog items for the repository as a whole.
- A guard asserting the platform is a `platform()` and not an `enforcedPlatform()`. The `gradle.md` bullet is the record; a test for a one-line declaration is the shape §4 already rejected once.
- Extending the BOM to `liquibaseRuntime` or any application configuration — those are already BOM-managed and versionless.
- Aligning the six non-Spring plugin versions with anything. Nothing manages Gradle plugin artifacts.

## 8. Contingencies

- **A plugin fails to resolve.** Read which coordinate and which constraint; if the BOM's constraint is genuinely the cause it will be an upgrade (require semantics cannot downgrade), so pin the affected coordinate explicitly in `build-logic/build.gradle.kts` rather than abandoning the platform.
- **Spotless output changes.** The measured `runtimeClasspath` diff touches none of Spotless's own artifacts, so this is not expected. If it happens, pin the affected coordinate; do not revert the formatter config.
- **`:build-logic:check` turns red.** AssertJ and JUnit resolve to the same versions as today, so a failure means a genuine behavioural change on the plugin classpath, not a test-framework difference. Debug it as such.
- **The Kotlin facade upgrade misbehaves.** Declare `implementation(embeddedKotlin("stdlib"))` to hold the runtime side at Gradle's embedded 2.3.21, which would also close the pre-existing compile/runtime gap outright. Not proposed up front: it adds a line to solve a problem that has not been observed.

## 9. Git workflow

Two commits on `build/replace-maven-with-gradle-18-sb-bom-deps`: this spec on its own (`docs(gradle)`), then the implementation (`build(gradle)`). A deliberate departure from the seventeen prior v0.5.0 specs, each of which rode its task's implementation commit — developer decision to commit the spec at approval time instead.

## 10. Post-implementation notes

- **Every §6 prediction held, measured on the real build rather than the probe.** `compileClasspath` gained the BOM node and nothing else — `kotlin-stdlib` and `kotlin-reflect` still resolve **2.3.21**, `liquibase-core` **5.0.2**. `runtimeClasspath` moved exactly `commons-logging` 1.3.5 → 1.3.6 and the four Kotlin stdlib facades 1.9.10 → 2.2.21. `testRuntimeClasspath` moved the same two things and resolved `assertj-core` **3.27.7**, `junit-jupiter` **6.0.3**, `junit-platform-launcher` **6.0.3** — identical to the pre-change versions, so the diff shows no AssertJ or JUnit line at all.
- **`./gradlew :build-logic:check` green in 54 s** (`validatePlugins` plus the eleven functional scenarios), `./gradlew spotlessCheck` green across all seven modules in 11 s, `./gradlew :services:asapp-users-service:test` green in 16 s. The integration tier was deliberately not run — the developer scoped this change's verification to commands that need no Docker, and nothing here reaches the application's own resolution.
- **`:services:asapp-users-service:tasks --all` lists 41 `liquibase*` tasks**, the documented count, so the apply-time `liquibase-core` entry still satisfies the plugin's `CommandFactory` task derivation with no version of its own.
- **No new deprecation.** `./gradlew help --warning-mode all` reports exactly the two known upstream ones: the Liquibase plugin's `Project.container(Class, Closure)` and grolifant's `StartParameter.isConfigurationCacheRequested`.
- **The `gradle.md` edit reached one bullet the spec did not list.** `## Database migrations`' plugin-classpath bullet described `liquibase-core` as "(catalog `liquibase` version + `liquibase-core` library)" — both gone — so it was corrected alongside the four planned edits. §5's list was three bullets plus one addition; the real count is four plus one.
- **`build-logic/build.gradle.kts` carries a one-line comment on the platform import** explaining that it governs every group below. Not in §5's sketch, added because the leading `// BOM` group is a new shape in this repository and the next reader has no precedent to infer it from.

**Deltas from the manual review pass, which ran after the notes above were written.** The five notes above record the state at the implementation commit; the review then changed one thing in the build and corrected six claims in the rules. Where the two disagree, the bullets below are the later word.

- **§8's Kotlin contingency became a preventive guard, because §1 and §4's central safety argument was one-directional.** The design rested on `platform()`'s *require* semantics meaning "nothing can be dragged backwards", and filed Kotlin as a contingency to act on only if the facade upgrade misbehaved — "adds a line to solve a problem that has not been observed". Highest-wins cuts forward too: the first Boot release whose `kotlin.version` exceeds Gradle's embedded compiler version would *raise* `kotlin-stdlib`/`kotlin-reflect` on the very classpath the Kotlin DSL compiles the five convention plugins against, and would hard-fail root plugin resolution outright, where `buildEnvironment` pins `kotlin-stdlib` `{strictly 2.3.21}` and a *require* above a *strict* is an error — an *application* Boot bump breaking the build's own tooling. `build-logic/build.gradle.kts` now leads its `// Build` group with a `constraints {}` block holding both at `strictly(embeddedKotlinVersion)`, so they track the wrapper rather than Boot; deliberately not catalog-pinned, since a hand-maintained number is exactly what this change exists to remove. Verified `{strictly 2.3.21} -> 2.3.21 (c)` on `:build-logic:compileClasspath`, out-ranking the BOM's `2.2.21` require. Boot 4.0.5's Kotlin sitting *below* the embedded version is the only reason nothing moves today. §8's `embeddedKotlin("stdlib")` was not the form taken — a constraint pins the transitive without adding a declared dependency.
- **The platform's reach does not stop at `build-logic`, which §1 and §4's "buildscript classpath" framing understated.** Declared on `implementation`, it is exported in `build-logic`'s `runtimeElements`, which the plugin-resolution `classpath` consumes; `settings.gradle.kts` includes `build-logic` once via `pluginManagement { includeBuild(...) }`, so Gradle resolves that classpath once build-wide — serving the root project and all seven modules, not just `build-logic`. Measured on the root `buildEnvironment`: 20 constraints contributed, moving `commons-logging` 1.3.5 → 1.3.6 and `kotlin-stdlib-jdk8` up to 2.2.21, so a Boot bump can move Spotless / PITest / Asciidoctor / JGit transitives on every module's plugin resolution. Narrowing the configuration is **not** an escape hatch — `liquibase-core` must stay on `implementation` for the Liquibase plugin's apply-time reflection, so the platform must sit there too; the export is inherent to the arrangement. Recorded in `.claude/rules/gradle.md` `## Shared Build Configuration` and in the `// BOM` group's own comment.
- **§3 and §4's claim that the BOM manages no plugin coordinate is false for one of the seven.** `io.spring.gradle:dependency-management-plugin` *is* BOM-managed, at 1.1.7 — verified the only one of the seven plugin coordinates constrained. It stays catalog-pinned anyway, deliberately, on the same reasoning as the Kotlin constraint: a plugin on the build's own classpath should not move because the application's Boot version moved. `gradle.md` now reads "six of the seven … because it must" and names the seventh as the exception, and attributes `liquibase-gradle`'s pin to that coordinate being unmanaged rather than to plugin coordinates being unmanaged in general. `gradle/libs.versions.toml` is unchanged.
- **§5's Liquibase rewrite understated why the two copies agree.** Both are versionless, but through *different* mechanisms — the apply-time copy through `platform()`'s require/highest-wins (a **floor**), the runtime copy through `io.spring.dependency-management`'s managed-version **override** (which also forces down) — so today's one number reached two ways is contingent, not guaranteed. `gradle.md` `## Database migrations` now says exactly that, credits changelog and checksum consistency to `liquibaseRuntime` alone (the apply-time copy only backs task discovery), and keeps the severity split as triage for a possible divergence rather than an instruction to hand-align. Both copies still resolve 5.0.2; documentation only.
- **The `gradle.md` edit count grew past the four-plus-one the note above already corrected.** Two further `## Ordering` bullets: removing the `liquibase` version key also killed that section's per-entry-annotation example, leaving `(picocli)` as the deliberate sole illustration; and the `**Scope** groups:` bullet needed its two grouping axes stated outright — `BOM` and `CVE` are *kinds*, the other five *consumption scopes*, and an entry can be one of each — because the catalog groups by scope while a dependency block groups by kind, which is why `spring-boot-dependencies` sits under `# Build` in the catalog yet leads `dependencies {}` as its own `// BOM` group. No entry moved in either file. A drafted annotation on the `spring-dependency-management` version key was dropped at the developer's request.
- **The only automated signal that a Boot upgrade broke the build's own tooling is not wired to CI.** `:build-logic:check` is what would catch it, `./gradlew build` never reaches an included build, and `ci.yml` still runs `mvn verify -Pci`, so there is no `./gradlew` invocation to hang the step on yet. Recorded in `gradle.md`'s revisit trigger and forwarded as a note under `TODO.md`'s *Migrate the CI workflow to Gradle* — the one place this task touched `TODO.md` beyond ticking its own line. Until it lands, a developer's local build is the first warning.
- **A test failure surfaced during review was a concurrency race, not a regression from this change.** `:build-logic:test` failing with `EOFException` (surfacing as a Kryo `Buffer underflow`) or `NoSuchFileException` root-caused to two Gradle builds executing the same `Test` task in one project directory: `build/test-results/test/binary/in-progress-results-generic.bin` is Gradle's own results-writer state, one path per task per project with no cross-build lock, so one process reads a half-written buffer while the other finds it deleted — two reported symptoms, one race. Reproduced on `:services:asapp-users-service:test` as well, so it is generic to any `Test` task and has nothing to do with TestKit, git fixtures, or temp repositories; four sequential forced re-runs were green and two concurrent runs failed with both signatures. No assertion ever failed and no code fix exists or is needed; recorded as a `## Build logic tests` bullet prescribing one Gradle build per working tree, with a `git worktree` per parallel agent.

Left undone on purpose: `gradle.md`'s `## Shared Build Configuration` bullet now carries five distinct topics and would read better as sub-bullets. That is a restructure for the *Clean Gradle files* subtask, not this one.

## 11. References

- Gradle User Manual, *Aligning dependency versions* / importing a Maven BOM — `platform()` vs `enforcedPlatform()` semantics
- Gradle User Manual, *Composite builds* — an included build's classpath and how its constraints reach consumers
- `docs/superpowers/specs/v0.5.0/2026-08-01-gradle-database-migration-commands-design.md` §4 and its post-implementation note at line 292 — the two-Liquibase-versions decision and the rolled-back guard that routed this entry to `TODO.md`
- `docs/superpowers/specs/v0.5.0/2026-08-02-gradle-build-logic-tests-design.md` §4 — the JUnit/AssertJ pins and the alignment intent this change replaces
- `.claude/rules/gradle.md` — `## Shared Build Configuration`, `## Database migrations`, `## Build logic tests`, `## Ordering`
