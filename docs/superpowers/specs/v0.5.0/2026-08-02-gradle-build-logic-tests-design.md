# Automated tests for the build's custom tasks — design spec

**Date**: 2026-08-02
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Add automated tests for the build's custom tasks" (line 28).
**Scope**: Give `build-logic` a test source set and cover its one custom task type, `InstallGitHooks`, with nine TestKit functional tests run against throwaway git repositories. Three files changed (`build-logic/build.gradle.kts`, `gradle/libs.versions.toml`, `.claude/rules/gradle.md`), one added (the test class), plus `TODO.md` edits. No change to any main-build command, no new root or lifecycle task, no `pom.xml` edit, no README edit.

## 1. Context

Sixteen prior subtasks moved the build onto Gradle 9.6.1 / JDK 25. The last of them made `build-logic` source-bearing for the first time: `InstallGitHooks.kt` is the only `.kt` file in the build and the only custom task **type** in the repository. Everything else registered by the convention plugins is a stock type (`Test`, `Jar`, `JacocoReport`) or a bodiless lifecycle task (`fullBuild`).

**Why this entry exists.** `InstallGitHooks` changed five times during its own review — the copy was bounded to a hardcoded hook list and then unbounded again, a `canExecute()` warning was added, the `core.hooksPath` unset grew a third branch, the action was split into four private steps, and the input property was renamed. Every one of those changes was verified by hand, once, on one machine. Nothing re-verifies them.

**What the task does, and what is load-bearing about it.** `.claude/rules/gradle.md`'s `## Git hooks` section records six mechanics as deliberate and non-obvious, each with a silent failure mode:

| Mechanic | Silent failure if it regresses |
|---|---|
| `git rev-parse --path-format=absolute --git-common-dir` | hooks installed into a directory git never reads (linked worktrees) |
| the unbounded copy | a newly added hook dropped while the task reports success |
| never `Copy` / `Sync` | Gradle's stale-output cleanup deleting files inside `.git` |
| `@UntrackedTask` | the task reporting `UP-TO-DATE` while the installed copy is stale |
| tolerating `git config --unset`'s exit **5** | a clean repository failing the task |
| clearing `core.hooksPath`, warning when one survives | every installed hook inert, with no explanation |

Five of the six fail *quietly* — the build stays green and the commit gate stops gating. That is the argument for automating them.

**Why the tests are functional rather than unit.** Gradle's own guidance is explicit that `ProjectBuilder` "does not execute tasks — it is only suitable for verifying configuration logic". Every behaviour in the table above is execution behaviour, and all but one of them depends on the state of a **real** git repository — a linked worktree resolving to a shared common directory, a `core.hooksPath` set at one scope and not another, an exit code from `git config --unset`. There is nothing left to unit-test once those are removed.

**Current `build-logic` state.** One `build.gradle.kts` (a `plugins { kotlin-dsl }` block and eight `implementation` lines), five precompiled script plugins, one task class, and **no `src/test`**. The `kotlin-dsl` plugin applies `java-gradle-plugin`, so three things already exist and cost nothing to adopt (verified via `:build-logic:tasks --all`):

- `pluginUnderTestMetadata` — the classpath-injection metadata `withPluginClasspath()` consumes
- `validatePlugins` — annotation validation for custom task types, wired into `check` and **passing today**
- `gradleTestKit()` already on `testImplementation`

`:build-logic:test` is reachable from the repo root despite `build-logic` being a `pluginManagement` included build (verified).

## 2. Goals

- **The six mechanics above are pinned by an executing test**, each of which fails when the mechanic is removed.
- **The tests never touch this repository.** Every scenario runs against a repository it created itself under a temp directory, with git's global and system configuration neutralised.
- **The registration is covered too**, not just the class — the fixtures apply `asapp.root-conventions`, so `hooksSource = layout.settingsDirectory.dir("git/hooks")` is exercised on every scenario.
- **No main-build command changes behaviour.** `./gradlew help`, `build`, `fullBuild`, `bootRun` and IntelliJ sync are byte-for-byte unaffected.
- **The suite is cheap to run and easy to reach**: one command, one daemon, a compiled fixture script reused across scenarios.

## 3. Non-goals

- **Testing the convention plugins' wiring** (`fullBuild` edges, `integrationTest` registration, the `spotlessCheck` ordering). The TODO note scopes this to "the custom task **types**", and the real build already proves most of that wiring on every run. Developer decision.
- **Testing the hook scripts themselves.** `git/hooks/pre-commit` and `commit-msg` are bash; covering them means a second, non-Gradle harness. Their `.kt`/`.kts` extension gap and the staged-content question are already tracked in the Backlog.
- **Running the suite from the main build.** §4 records why, and the CI step is handed to line 39.
- **Formatting the new sources.** Spotless is applied only in `asapp.java-conventions` and declares only a `java` format, so `build-logic`'s Kotlin is unchecked — an existing Backlog item, not this task's to close.
- **Coverage or mutation testing of `build-logic`.** No JaCoCo, no PIT; the build logic is not application code.
- **A consumer smoke build.** Gradle documents an optional second project that applies the plugin through `includeBuild()` — this repository *is* that consumer, on every build.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| What is under test | **`InstallGitHooks` only** | It is the build's only custom task type. Developer decision, matching the TODO note's wording. |
| Test kind | **TestKit functional tests** (`GradleRunner`) | Gradle: `ProjectBuilder` "does not execute tasks"; every behaviour worth pinning here is execution against a real git repository. |
| Source set | **the default `test`**, class `InstallGitHooksFunctionalTest` | A deliberate, recorded divergence from Gradle's prescribed layout, which puts functional tests in a separate `src/functionalTest` source set registered via `gradlePlugin.testSourceSets` and wired into `check`. Here there is exactly **one** tier and every test in it is functional, so the split would leave `src/test` permanently empty and cost a source set, a suite and a `check` edge for a single class. The `*FunctionalTest` suffix carries the distinction the directory would have. **Revisit trigger:** the first `ProjectBuilder` unit test — at that point the two tiers are real and the split earns itself. The repo's `*Tests` / `*IT` suffixes do not apply: `.claude/rules/testing-*.md` are path-scoped to `**/*.java`, and `*IT` means a Testcontainers-backed Spring test in the five service modules. |
| Test framework | **JUnit 6.0.3 + AssertJ 3.27.7** | Gradle's docs recommend Spock "for its expressiveness", but there is no Groovy anywhere in this repository and the DSL rule bans it from build scripts; adopting it means a second language and compiler for one test class. JUnit + AssertJ is what every other test in the repo uses. Both versions are pinned to what Boot 4.0.5 already resolves for the application (verified: `org.junit:junit-bom:6.0.3`, `org.junit.platform:junit-platform-launcher:6.0.3`, `org.assertj:assertj-core:3.27.7`), so `build-logic` and the application never drift apart. |
| Rejected: `jar.dependsOn(test)` | **Impossible — a dependency cycle** | The intuitive wiring ("no plugin jar until its tests pass") cannot be expressed: `compileTestKotlin` already depends on `jar`. Verified — Gradle reports `:build-logic:compileTestJava ← :build-logic:compileTestKotlin ← :build-logic:jar ← :build-logic:test ← :build-logic:compileTestJava`. Recorded because it is the first thing anyone revisiting this will try. |
| Rejected: `jar.finalizedBy(test)` | **Works, but reinstates a removed Gradle behaviour** | Verified against this repository: the finalizer fires on a plain `./gradlew help` even with `jar` **UP-TO-DATE**, and a failing finalizer fails the outer build with exit 1. It was rejected on reach, not mechanics — `:build-logic:jar` is on the *plugin-resolution* path (verified: it runs on `./gradlew help`), so this attaches the suite to **every** command, IntelliJ sync included, lets a red build-logic test block `bootRun`, and makes the first command after any `build-logic` edit wait on the whole suite. It is also precisely what `buildSrc` did until **Gradle 8.0**, which removed it deliberately: "when Gradle builds the output of `buildSrc` it only runs the tasks that produce that output and no longer runs the `build` task. You can run the tests for `buildSrc` in the same way as other projects by explicitly calling them from the command line if needed." |
| Rejected: an outer `check` edge | **No `gradle.includedBuild(…).task(…)` anywhere** | `check.dependsOn(gradle.includedBuild("build-logic").task(":check"))` in `asapp.java-conventions` would run the suite on `build` / `check` / `fullBuild` only, sparing `help` and `bootRun`. Rejected as the middle option with no documentation behind it: it declares the same cross-build edge in all seven modules, and it puts a Testcontainers-scale wait into `:services:<svc>:build` for a change in a directory that module does not own. |
| Trigger | **Explicit `./gradlew :build-logic:check`, plus a CI step** | Developer decision, and the one Gradle's docs point at: tasks in an included build are executed explicitly, by qualified path, from the command line or the IDE. `check` rather than `test` because it adds `validatePlugins` for free. |
| Plugin injection | **`withPluginClasspath()` + `plugins { id("asapp.root-conventions") }`** | Automatic injection is what `java-gradle-plugin` exists for, and applying the real convention plugin covers the **registration** as well as the class — the alternative, `tasks.register<InstallGitHooks>(…)` in the fixture, would leave `hooksSource = layout.settingsDirectory.dir("git/hooks")` untested, the exact line a previous review had to correct. |
| Git isolation | **`GIT_CONFIG_GLOBAL` + `GIT_CONFIG_NOSYSTEM=1`** | Without it the developer's own `core.hooksPath`, if ever set globally, silently fails scenario 4 and passes scenario 7 for the wrong reason. Both variables are ordinary git environment variables (`GIT_CONFIG_GLOBAL` since Git 2.32; local is 2.54, CI's ubuntu image is newer), and they also guarantee the suite cannot write to the developer's real config. |
| One environment per class | **A class-level `gitconfig`, rewritten before each test** | `withEnvironment` forks a process per distinct environment, so a per-test config file would mean a fresh TestKit daemon per scenario. A single file, truncated and re-seeded with `user.name` / `user.email` in `@BeforeEach`, keeps the environment constant (one daemon for the suite) while leaving each scenario a clean global config. |
| `withEnvironment` consequence | **`withDebug(true)` is unavailable** | Gradle's Javadoc: "When environment is specified, running with `isDebug()` is not allowed" — debug mode runs in-process and TestKit must fork to pass environment variables. Debug the task through a real `./gradlew installGitHooks --debug` run instead. |
| Fixture scripts | **Byte-identical across scenarios** | The Kotlin DSL compiles build scripts; identical content means one compilation reused from the TestKit cache for the other eight scenarios. Cheap to preserve, and expensive to lose by accident. |
| Assertion of behaviour, not of implementation | **Assert on the filesystem, on git's own answers, and on the task's output** | Scenario 3 asserts `git config --get core.hooksPath` is empty, not that a particular command ran; scenario 2 asserts where the files landed, not which `rev-parse` flags were passed. |

## 5. Changes by file

**`gradle/libs.versions.toml`** — two versions and two libraries, under `# Build` → `## Org` (build-logic classpath entries, `org.`-prefixed groups), alphabetical within the origin:

```toml
# [versions] → # Build → ## Org
assertj = "3.27.7"   # Keep in sync with the Boot BOM
junit-bom = "6.0.3"  # Keep in sync with the Boot BOM

# [libraries] → # Build → ## Org
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit-bom" }
```

**`build-logic/build.gradle.kts`** — a `dependencies` addition and one task configuration. `gradleTestKit()` is deliberately **not** declared: `java-gradle-plugin`, applied by `kotlin-dsl`, already puts it on `testImplementation`, and a comment says so rather than a redundant line.

```kotlin
// build-logic is outside asapp.java-conventions, so the JUnit Platform is enabled here
tasks.named<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    // …existing Build entries…

    // Test
    // Org
    // TestKit itself arrives with java-gradle-plugin, which kotlin-dsl applies
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

**`build-logic/src/test/kotlin/com/attrigo/asapp/gradle/InstallGitHooksFunctionalTest.kt`** (new) — the suite and its private fixture helpers. Shape:

```kotlin
class InstallGitHooksFunctionalTest {

    // One global config for the whole class keeps the environment constant, so TestKit reuses one daemon
    companion object {
        @JvmStatic @TempDir lateinit var globalGitConfigDir: File
        private val globalGitConfigFile get() = File(globalGitConfigDir, "gitconfig")
    }

    @TempDir lateinit var workspace: File

    /** Truncates the class's global config and re-seeds it with an identity, per scenario. */
    @BeforeEach
    fun seedGlobalGitConfig() { … }

    private fun gitEnvironment(): Map<String, String> =
        System.getenv() + mapOf(
            "GIT_CONFIG_GLOBAL" to globalGitConfigFile.absolutePath,
            "GIT_CONFIG_NOSYSTEM" to "1",
        )

    /** Runs `git init` in [directory], then writes the three fixture files and the named hooks. */
    private fun createFixtureRepository(directory: File, vararg hookNames: String) { … }

    private fun runInstallGitHooks(projectDirectory: File, vararg extraArguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath()
            .withEnvironment(gitEnvironment())
            .withArguments("installGitHooks", "--stacktrace", *extraArguments)
            .build()
}
```

The fixture project is three files: `settings.gradle.kts` (`rootProject.name = "fixture"`), `build.gradle.kts` (`plugins { id("asapp.root-conventions") }` and nothing else), and `git/hooks/` holding stub scripts named `pre-commit` and `commit-msg` — the task copies whatever it finds, so the content is a shebang and `exit 0`, written with LF endings.

The nine scenarios:

| # | Test | Arrangement → assertion |
|---|---|---|
| 1 | `installsEveryHook_PlainRepository` | plain `git init` → both hooks exist under `<repo>/.git/hooks` with the source's content; on POSIX also owner-executable (the assertion, not the test, is skipped on Windows) |
| 2 | `resolvesCommonHooksDir_LinkedWorktree` | commit the fixture, `git worktree add`, run **in the worktree** → the hooks land in the main repository's `.git/hooks`, and `.git/worktrees/<id>/hooks` does not receive them |
| 3 | `clearsHooksPath_SetLocally` | `git config core.hooksPath git/hooks` first → afterwards `git config --get core.hooksPath` is empty and the output reports the value was cleared |
| 4 | `succeeds_NoHooksPathSet` | nothing set → the build succeeds and the output carries no warning (`--unset` exit 5 tolerated) |
| 5 | `preservesExistingFiles_UnrelatedFilePresent` | a marker file placed in `.git/hooks` beforehand → it is still there afterwards |
| 6 | `installsAddedHook_NewFileInSource` | a third file added to `git/hooks/` → it installs, with no change to the task class |
| 7 | `warnsAboutSurvivingHooksPath_SetGlobally` | `core.hooksPath` written into the class's global config → the output warns and names the value, and the global config still holds it afterwards |
| 8 | `executes_SecondRun` | run twice → `SUCCESS` both times, never `UP-TO-DATE` |
| 9 | `reusesConfigurationCacheEntry_SecondRun` | run twice with `--configuration-cache` → "entry stored", then "entry reused" |

Names follow the repo's `<Behavior>_<Condition>` pattern; assertions use AssertJ. No license header — `build-logic`'s Kotlin sits outside Spotless, and `InstallGitHooks.kt` carries none either.

**`.claude/rules/gradle.md`** — two edits.

1. **Frontmatter**: add `"**/build-logic/src/test/kotlin/**/*.kt"` to `paths:`, beside the `src/main` entry the git-hooks subtask added.

2. **A `## Build logic tests` section** between `## Git hooks` and `## Ordering`, recording: `./gradlew :build-logic:check` is the command (`test` plus `validatePlugins`), and it is **explicit-only** — `./gradlew build` / `fullBuild` never reach an included build, and no cross-build edge is wired, because `jar.dependsOn(test)` is a cycle (`compileTestKotlin` depends on `jar`) and `jar.finalizedBy(test)` — which does work, verified — attaches the suite to every invocation including `./gradlew help` and IDE sync, reinstating the `buildSrc` behaviour Gradle removed in 8.0; custom task types are covered by **functional** tests only, since `ProjectBuilder` cannot execute tasks and every behaviour here needs a real git repository; tests live in the default `test` source set as `*FunctionalTest`, a recorded divergence from Gradle's `src/functionalTest` layout justified by there being exactly one tier, with the first `ProjectBuilder` unit test as the revisit trigger; JUnit and AssertJ versions are pinned in the catalog to what the Boot BOM already resolves for the application and must be kept in sync; `gradleTestKit()` is **never** declared, since `java-gradle-plugin` (applied by `kotlin-dsl`) already provides it, and the same plugin supplies `pluginUnderTestMetadata` and `validatePlugins` for free; fixtures apply the real convention plugin via `withPluginClasspath()` rather than registering the task type, so the registration is covered too; every fixture repository is created under a `@TempDir` with `GIT_CONFIG_GLOBAL` and `GIT_CONFIG_NOSYSTEM=1` so the developer's git config can neither influence nor be touched by the suite — **never** let a test run against the real repository; the environment map is constant across the class (one TestKit daemon) and built from `System.getenv()`, because `withEnvironment` replaces the environment wholesale and rules out `withDebug(true)`; fixture build scripts stay byte-identical so the Kotlin script compiles once; and the suite requires the `git` binary on `PATH`, which nothing else in the build does.

**`TODO.md`** — four edits:

1. Check off "Add automated tests for the build's custom tasks" (line 28).
2. Add under "Reuse the Spring Boot BOM for the build's own dependency versions" (line 30):

```markdown
        - **Note:** the JUnit BOM and AssertJ pins this subtask added to `build-logic` are two more hand-kept copies of a Boot-managed version — the same duplication as Liquibase, and part of the survey
```

3. Add under "Migrate the CI workflow to Gradle" (line 39):

```markdown
        - **Note:** `./gradlew build` never reaches an included build, so the build-logic functional tests need their own `./gradlew :build-logic:check` step — it also runs `validatePlugins`
```

4. Add under "Verify full parity, then remove Maven entirely" (line 80):

```markdown
        - **Note:** re-run `./gradlew :build-logic:check` after removing the `pom.xml` files — the fixtures are self-contained, but the suite is the only automated guard on `InstallGitHooks`
```

## 6. Placement / altitude rationale

- **Tests → `build-logic/src/test/kotlin`, next to the class they cover.** A task class is code; its tests belong in the same build, not in a module that consumes the plugin.
- **Package `com.attrigo.asapp.gradle`, mirroring the class under test.** Precompiled script plugins stay unpackaged at the source-set root; class sources take the package, as the git-hooks subtask established.
- **Versions → the root catalog, not `build-logic`'s own.** `build-logic/settings.gradle.kts` already re-points at `gradle/libs.versions.toml`; a second source of versions would be exactly the drift that file exists to prevent.
- **Nothing in the main build.** No root task, no `asapp.java-conventions` edge, no `fullBuild` member. The trigger decision in §4 is the whole reason.

## 7. Verification / Definition of Done

1. **The suite is green.** `./gradlew :build-logic:test` passes on Windows; the one POSIX-only assertion inside scenario 1 is reported as skipped, every scenario runs.
2. **`check` is green.** `./gradlew :build-logic:check` passes, running `test` and `validatePlugins`.
3. **The tests bite.** Three deliberate mutations, each reverted after: replacing `--git-common-dir` with a hardcoded `.git/hooks` fails scenario 2; restoring a hardcoded `HOOK_NAMES` list fails scenario 6; removing `@UntrackedTask` fails scenario 8.
4. **Nothing ran against the real repository.** After a full suite run, `git status` is clean, `git config --get core.hooksPath` answers exactly as it did before, and the mtimes in `<git-common-dir>/hooks` are unchanged.
5. **No main-build behaviour changed.** `./gradlew help --console=plain` lists the same `:build-logic:*` tasks as before — no `test`, no `pluginUnderTestMetadata`, no `validatePlugins` — and its wall clock is unchanged.
6. **Up-to-dateness works.** A second `./gradlew :build-logic:test` reports `UP-TO-DATE`; after `./gradlew :build-logic:clean` it is served `FROM-CACHE` — the root build's `org.gradle.caching=true` covers the whole build tree, which this item confirms rather than assumes.
7. **Only the intended invalidation.** Editing `InstallGitHooks.kt` re-runs the suite; editing an unrelated file in the main build does not.
8. **Cost is recorded.** The suite's wall clock, cold and warm, is measured and written into the rules section — no estimates.
9. **Maven untouched**: no `pom.xml` edited, so `mvn` behaves as before by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

Convention-plugin wiring tests · hook-script tests · a `functionalTest` source set · `ProjectBuilder` unit tests · Spock · Spotless for `build-logic` · JaCoCo or PIT for `build-logic` · a consumer smoke build · multi-Gradle-version testing via `withGradleVersion` · the CI step itself (line 39) · any change to `InstallGitHooks` behaviour.

## 9. Contingencies

- **`withPluginClasspath()` and the Kotlin DSL.** The known friction (kotlin-dsl-samples#492, open) is with type-safe **accessors** for an injected plugin's extensions; the fixture declares no extension and uses no accessor, only `plugins { id("asapp.root-conventions") }`. If it still fails to resolve, the fallback is `buildscript { dependencies { classpath(files(…)) } }` + `apply(plugin = "asapp.root-conventions")`, and the last resort is registering `InstallGitHooks` directly in the fixture — which costs the registration coverage §4 wanted.
- **`@TempDir` cleanup on Windows.** Git writes loose objects read-only, and a TestKit daemon may hold handles inside the fixture's `.gradle` directory. JUnit clears the read-only attribute before deleting; if cleanup still flakes, add `--no-watch-fs` to the runner arguments and, failing that, `@TempDir(cleanup = ON_SUCCESS)` so a failure leaves the evidence behind.
- **`git` must be on `PATH` to run the suite.** Nothing else in the build shells out to git — `gradle-git-properties` uses JGit — so this is a new prerequisite for `:build-logic:check` alone, not for `./gradlew build`. If it is ever missing, the failure is a clear `CreateProcess error=2` from the first fixture command, not a mystery.
- **Cross-machine build-cache reuse will not happen for `:build-logic:test`.** `pluginUnderTestMetadata` writes absolute classpath paths into a properties file that rides the test runtime classpath, so the task's fingerprint is machine-specific. Local up-to-dateness and the local cache work; CI will always execute the suite, which is the desired outcome anyway.
- **Scenario 9 depends on Gradle's console wording.** "Configuration cache entry stored" / "reused" are matched as substrings; a wording change in a future Gradle upgrade breaks the test loudly at upgrade time, which is the right moment to notice.
- **Nine nested builds is the floor, not a target.** If the warm-daemon wall clock lands somewhere that makes the suite annoying to run, the answer is to merge scenarios that share an arrangement (1, 5 and 6 all install into a plain repository), never to drop a mechanic from §1's table.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-17-gradle-builds-tests`. Two commits:

1. `docs(specs): add the build-logic tests design spec` — this document.
2. `build(gradle): add functional tests for the build's custom tasks` — the test class, the `build-logic/build.gradle.kts` and catalog changes, the two `.claude/rules/gradle.md` edits, and the four `TODO.md` edits.

Following this migration's established pattern, implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. References

- Gradle — [Testing Build Logic with TestKit](https://docs.gradle.org/current/userguide/test_kit.html): automatic plugin-classpath injection via `java-gradle-plugin` + `withPluginClasspath()`; TestKit builds run in isolation with their own Gradle user home and daemon.
- Gradle — [Testing Plugins](https://docs.gradle.org/current/userguide/testing_gradle_plugins.html): the unit / integration / functional pyramid, and "ProjectBuilder does **not** execute tasks — it is only suitable for verifying configuration logic".
- Gradle — [Testing Binary Plugins](https://docs.gradle.org/current/userguide/testing_binary_plugin_advanced.html): the prescribed `src/functionalTest` source set, `gradlePlugin.testSourceSets`, and wiring functional tests into `check`. The layout §4 diverges from, deliberately.
- Gradle — [`GradleRunner`](https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html): "When environment is specified, running with `isDebug()` is not allowed" — debug runs in-process and TestKit must fork to pass environment variables.
- Gradle — [Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html): tasks in an included build are executed explicitly by qualified path (`:build-logic:test`) from the command line or the IDE; the page prescribes no lifecycle integration with the consuming build.
- Gradle — [8.0 release notes](https://docs.gradle.org/8.0/release-notes.html): "when Gradle builds the output of `buildSrc` it only runs the tasks that produce that output and no longer runs the `build` task. You can run the tests for `buildSrc` in the same way as other projects by explicitly calling them from the command line if needed." The basis for rejecting `jar.finalizedBy(test)`.
- Gradle — [Best Practices for Structuring Builds](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html): "Favor `build-logic` Composite Builds for Build Logic", and fewer task invalidations than `buildSrc`.
- Gradle — [Java Gradle Plugin Development plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html): adds `gradleApi()` to `api`, `gradleTestKit()` to `testImplementation`, and registers `pluginUnderTestMetadata` and `validatePlugins`.
- Git — [git-config](https://git-scm.com/docs/git-config): `GIT_CONFIG_GLOBAL` (Git 2.32+) redirects the global configuration file; `GIT_CONFIG_NOSYSTEM` suppresses the system one.
- Git — [git-worktree](https://git-scm.com/docs/git-worktree): a linked worktree's `.git` is a file pointing at `$GIT_DIR/worktrees/<id>`; hooks are not per-worktree. The behaviour scenario 2 pins.

## 12. Post-implementation notes

This spec was written before implementation. The core change shipped substantially as designed — `build-logic` gained a `src/test/kotlin` suite of TestKit functional tests for `InstallGitHooks`, gated behind an explicit `./gradlew :build-logic:check` and never touching the main build's lifecycle.

The canonical implementation is the current state of `build-logic/src/test/kotlin/com/attrigo/asapp/gradle/InstallGitHooksFunctionalTest.kt`, `build-logic/build.gradle.kts`, `gradle/libs.versions.toml`, and `.claude/rules/gradle.md`'s `## Build logic tests` section — not this document.

**Notable deltas:**

- **Eleven scenarios, not nine — amends §5's "The nine scenarios" and §9's "nine nested builds is the floor".** All nine designed scenarios shipped verbatim; two more were written rather than deferred during review — `registersOnRootOnly_MultiProjectBuild` and `warnsAboutUnsetFailure_ConfigLocked`. Fourteen nested builds, not nine: three scenarios (`executes_SecondRun`, `reusesConfigurationCacheEntry_SecondRun`, `registersOnRootOnly_MultiProjectBuild`) each invoke a Gradle build twice. `.claude/rules/gradle.md`'s "Measured cost" bullet now reads "eleven scenarios / fourteen nested builds". `warnsAboutUnsetFailure_ConfigLocked` pre-creates a stale `.git/config.lock` rather than the obvious `File.setReadOnly()` on `.git/config`, because git writes config through `.git/config.lock` and renames over the target — Git-for-Windows' `mingw_rename` clears `FILE_ATTRIBUTE_READONLY` on the destination and retries, and POSIX `rename(2)` ignores the target's mode, so a read-only bit on `config` would land the unset in the already-covered success branch either way. A multi-valued `core.hooksPath` key was also rejected as the failure trigger — git returns exit 5 there, the same not-found path `succeeds_NoHooksPathSet` already covers. A pre-existing lock is taken `O_CREAT|O_EXCL` before parsing, with no timeout, so the failure is deterministic; the scenario deliberately does not assert the exit value, which surfaces as `255` on POSIX and `-1` on Windows.
- **§7 DoD item 3's third mutation is factually wrong — the one spec claim that was inverted.** Removing `@UntrackedTask` fails no scenario: `InstallGitHooks` declares no outputs, so Gradle never reports it `UP-TO-DATE` either way. The mutation that actually bites `executes_SecondRun` (plus three others) is re-registering `installGitHooks` as a stock `Copy`. This is now recorded as a rule in `.claude/rules/gradle.md`'s `## Build logic tests` section. It also weakens §2 Goal 1 and the `@UntrackedTask` row of §1's mechanics table — the *mechanic* (no declared outputs, never a `Copy`) is pinned, but not by the annotation those sections name. The other two mutations survive as stated.
- **§4's "byte-identical fixture scripts" decision now has one codified exception.** `MULTI_PROJECT_FIXTURE_SETTINGS_SCRIPT` (carrying `include("sub")`) is a deliberately non-identical second settings script, so the suite pays one extra Kotlin DSL script compilation. Recorded as the one exception in `.claude/rules/gradle.md`'s byte-identical-scripts bullet, whose reuse count moved from eight to nine.
- **§4/§5's git isolation shipped stronger than designed.** Beyond `GIT_CONFIG_GLOBAL` + `GIT_CONFIG_NOSYSTEM`, every inherited `GIT_*` variable is stripped, via a shared private `isGitEnvironmentVariable(key)` applied at two sites — `gitEnvironment()`'s `System.getenv()` copy and `runGit()`'s live `ProcessBuilder.environment()` map. §5's sketched `System.getenv() + mapOf(…)` is wrong as written; an ambient `GIT_DIR` or `GIT_CONFIG_COUNT` would otherwise reach the developer's real repository. A related finding is recorded in `.claude/rules/gradle.md`: `GIT_CONFIG_NOSYSTEM` also drops Git-for-Windows' system `core.autocrlf=true`, which is what keeps the fixture files LF on every platform.
- **§3/§8's convention-plugin-wiring non-goal was partially crossed.** `registersOnRootOnly_MultiProjectBuild` asserts registration *altitude* — root `:installGitHooks` succeeds while `:sub:installGitHooks` is not found — which is a wiring assertion, not a task-class behaviour assertion. The broader wiring surface stays untested and is now tracked in `TODO.md`'s Backlog under `### Technical` → `#### build` as "Cover the build conventions' task wiring with configuration-level tests".
- **§5's changed-file set is seven paths, and one file it excluded was touched.** Final set: `.claude/rules/gradle.md`, `TODO.md`, `build-logic/build.gradle.kts`, `build-logic/src/main/kotlin/com/attrigo/asapp/gradle/InstallGitHooks.kt`, the new `InstallGitHooksFunctionalTest.kt`, this spec, and `gradle/libs.versions.toml`. The unanticipated one is `InstallGitHooks.kt` — a KDoc-only correction to `copyHooks`: its "returns the names it copied" was false, and it now says it returns the top-level hook names, with nested files copied without checking they can run. The executable check stays files-only even though the copy is recursive, because per `githooks(5)` git resolves a hook by exact filename in exactly one directory — no recursion, no `run-parts`, no `pre-commit.d` convention — so nested files are inert by construction, and the files-only list is what keeps the warning's own wording ("git will skip it silently") true, since `canExecute()` on a directory means traversable, not runnable; narrowing the copy itself was rejected because it would add selection logic to a task whose selling point is having none. §8's out-of-scope line is worded as any change to `InstallGitHooks` *behaviour*, so §8 holds in substance. Also, §5 promises `.claude/rules/gradle.md` two edits; a third landed — the `## Ordering` section's "Dependency blocks" bullet gained a named exception, that a `platform(...)` BOM import leads its scope+origin group ahead of alphabetical order.
- **§5's four `TODO.md` edits are eight.** Beyond the designed four: the subtask's own explanatory `**Note:**` line was deleted when "Add automated tests for the build's custom tasks" was checked off; two Backlog entries were added under `### Technical` → `#### build` — the wiring-tests one above, and "Enforce formatting and license-header rules on the build's own scripts and sources" with four subtasks; and the pre-existing "Add code formatter for .kt and .kts files" subtask was removed from "Improve code formatting", folded into the new formatting entry. §3's "formatting the new sources" non-goal held — no Spotless was added to `build-logic`.
- **§5's code and class-shape sketches are stale enough to mislead.** `build-logic/build.gradle.kts` ships `tasks.withType<Test>().configureEach { useJUnitPlatform() }`, not the sketched `tasks.named<Test>("test")` — the type-based form is the repo convention and covers a future second `Test` task. The dependency block ships as `platform(libs.junit.bom)`, `libs.assertj.core`, `"org.junit.jupiter:junit-jupiter"`, `"org.junit.platform:junit-platform-launcher"`. `globalGitConfigDir` is named `globalGitConfigDirectory`. `createFixtureRepository(directory, vararg hookNames)` is now `createFixtureRepository(directory, hookNames: List<String> = HOOK_NAMES, settingsScript: String = FIXTURE_SETTINGS_SCRIPT)`. The private surface grew past the four sketched helpers to include `createMultiProjectFixtureRepository`, `commitEverything`, `gradleRunner`, a `BuildResult.installGitHooksOutcome` extension property, `gitConfigIsolation`, `isGitEnvironmentVariable`, `runGit`, `runGitOrFail`, a `GitResult` data class, and seven file-scope constants; `GLOBAL_GIT_CONFIG_SCRIPT` also seeds `[init] defaultBranch = main`.
- **The shipped `## Build logic tests` rules section is about double what §5 sketches, including two conventions the spec never decided.** Twenty bullets rather than roughly eleven. Two are new convention decisions with no spec antecedent, so the spec is not their source of record: that this tier stays flat with no `@Nested` grouping (using scenario- and helper-grouping comments instead), and which `testing-core.md` rules carry into the Kotlin tier versus deliberately diverge (`assertSoftly` at two-or-more properties instead of the Java tiers' three, `actual` naming with `firstResult`/`secondResult` as the documented exception, full-clause `describedAs`). Also new: that a second functional-test class must reuse this exact environment map or it forks a second TestKit daemon, with harness extraction deliberately deferred until such a class exists, and that `describedAs(…)` is used rather than `as(…)` because `as` is a Kotlin keyword.
- **Two §7 DoD wordings are inaccurate as written.** Item 8's cost figures did land in the rules file, but `.claude/rules/gradle.md` annotates them as measured at nine scenarios, before the multi-project and locked-config ones. Item 1's "reported as skipped" will never appear: the POSIX-only executable assertion uses `Assumptions.assumingThat(!OS.WINDOWS.isCurrentOs) { … }`, which skips the *assertion block* without aborting the scenario or marking anything skipped — `gradle.md` states this correctly.

For future build-logic-test edits, treat those four artifacts as the template; this spec is preserved as a record of the original design intent.
