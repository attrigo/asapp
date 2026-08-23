# Gradle git hook installation — design spec

**Date**: 2026-08-01
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate git hook installation to Gradle" (line 27).
**Scope**: Register an `installGitHooks` task on the root project that copies the two tracked hooks into the repository's hooks directory, and swap the pre-commit hook's dead `mvn spotless:check` for a real `./gradlew spotlessCheck`. Two new `build-logic` files (the task class and the root archetype's convention plugin), a one-line root build script, one hook script, one rules section — including a reworded rule — and three `TODO.md` edits. No third-party plugin, no catalog entry, no `build-logic/build.gradle.kts` change, no `pom.xml` edit, no README edit.

## 1. Context

Fifteen prior subtasks moved the build onto Gradle 9.6.1 / JDK 25. Git hooks are the last developer-workflow capability still owned exclusively by Maven.

**What the hooks are.** Two bash scripts tracked at `git/hooks/`:

| Hook | What it does |
|---|---|
| `pre-commit` | Reads the *staged blob* of every text file (`git show ":$file"`) and fails on any CR byte, then runs `mvn spotless:check` |
| `commit-msg` | Regex-matches the message against Conventional Commits, skipping `fixup!` / `squash!` / `amend!` |

**How they get installed today.** `git-build-hook-maven-plugin` 3.5.0, declared in the root `pluginManagement` and re-declared in all 7 module `<build><plugins>` blocks. Three facts about it were verified during design rather than assumed:

- Its `install` goal binds to **`PROCESS_SOURCES`**, not `initialize` — confirmed by `mvn -o initialize -X`, whose 1554-line debug log mentions the plugin **zero** times while showing `jacoco:prepare-agent` running at `initialize`. So it fires on every `mvn compile` / `verify` / `install`, but not on `mvn validate` or `mvn initialize`.
- It copies into **`<git-dir>/hooks`** and **does not read `core.hooksPath`** — `InstallMojo` locates the git directory with JGit's `FileRepositoryBuilder` and appends `hooks`, consulting no git config.
- It sets the executable bit on the copies, which is why the missing exec bit on the tracked sources (below) has never been felt.

**The state this repo is actually in.** `.git/config` carries `core.hooksPath = git/hooks`, set by hand — the POM declares only the `install` goal, never `configure`, so Maven did not write it. Because `core.hooksPath` makes git ignore `$GIT_DIR/hooks` outright, the plugin's copies in `C:/dev/repos/ttrigo/asapp/.git/hooks` are **inert on this machine**, and the tracked scripts are what fire. On a fresh clone the reverse holds: no `core.hooksPath`, so the copies are the live mechanism. Both paths lead to identical content today, which is why the divergence has gone unnoticed.

**The formatting gate does nothing.** `pom.xml:70` sets `spotless.check.skip=true` by default (the `ci` profile flips it), so the hook's `mvn spotless:check` is a skipped goal. Measured: **5.5s** to check nothing. This is what line 28 of `TODO.md` calls out.

**The tracked scripts are mode `100644`.** `git ls-files -s git/hooks` shows no exec bit on either file. Irrelevant under the copy approach, since the copy sets its own permissions — but it is why a `core.hooksPath` design would have needed `git update-index --chmod=+x` as well.

**Measured cost of the swap** (native Windows, warm daemon, `org.gradle.console=verbose` in force):

| Command | Time | Does it enforce anything? |
|---|---|---|
| `mvn spotless:check` (today's hook) | 5.5s | no — skipped |
| `./gradlew spotlessCheck`, fully up to date | **3s** | yes |
| `./gradlew spotlessCheck`, work to do | 6.8s | yes |

**Current Gradle state.** The root `build.gradle.kts` holds one line — `// populated in a later Gradle migration subtask` — and applies no plugins. `build-logic` holds four precompiled script plugins and no `.kt` source file. Spotless is applied in `asapp.java-conventions` across all 7 modules and `spotlessCheck` rides `check`, so the build already gates formatting; the hook gates it earlier.

## 2. Goals

- **One command**: `./gradlew installGitHooks` puts both hooks where git reads them, on any clone and in any linked worktree.
- **`.git/hooks` is the live directory** — the developer's chosen mechanism. Any `core.hooksPath` that would shadow it is cleared by the task, so the install is self-healing rather than dependent on remembering.
- **Hooks fire from anywhere** — any subdirectory, any worktree, IntelliJ's commit dialog included.
- **Task definition separate from task configuration** — developer requirement. No task class body in a build script; the class is a `.kt` file, its registration a convention plugin, its application one line.
- **The formatting gate actually gates**, at no worse than today's cost.
- **Nothing leaks into ordinary builds**: no `git` subprocess, no file I/O, no configuration-time work on `./gradlew help`; no root lifecycle task names.
- **Gradle never treats `.git` as its own** — no output directory registered inside the repository metadata.
- Zero `pom.xml` edits; `mvn` keeps installing hooks exactly as it does today, until the removal subtask.

## 3. Non-goals

- **README migration.** The root README documents `mvn git-build-hook:install` (line 394) and "Automatically installed on `mvn install`" (line 490). Both are handed to "Migrate build documentation to Gradle" (line 61) as a note, the same way every prior subtask in this migration handed over its doc debt — and for the same reason: the Maven command still works, so this is a stale command rather than a defect.
- **Removing the Maven plugin.** Deferred to "Verify full parity, then remove Maven entirely" (line 78), matching every prior subtask. The two tools write byte-identical files to the same directory, so coexistence is inert.
- **Fixing the `100644` mode on the tracked scripts.** Only matters if git reads them directly, which the copy approach does not do.
- **New hooks.** No `pre-push`, no `prepare-commit-msg`. Spotless 8.8.0's own `spotlessInstallGitPrePushHook` is not adopted (§4.1).
- **Staged-only formatting.** Spotless removed per-file selection in favour of `ratchetFrom`; §4 explains why neither is wired.
- **CI.** Hooks never run in CI; the `-Pci` formatting gate is line 38's subject.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Installation mechanism | **Copy into `<git-common-dir>/hooks`** | Developer decision, taken over configuring `core.hooksPath`. Keeps the standard location, touches no user git config beyond removing a shadow, and needs no exec bit in the index. Accepted cost: a second copy that goes stale when a hook is edited, so `installGitHooks` must be re-run after editing one. |
| Clearing `core.hooksPath` | **The task runs `git config --unset core.hooksPath`** | Not defensive programming — this repo has it set right now, and while it is set the copy is invisible. `--unset` exits **5** when the key is absent (verified), so the task must ignore a non-zero exit rather than treat it as failure. After unsetting, the task re-reads `git config --get core.hooksPath` and warns if a `--global` or `--system` value still shadows, since `--unset` reaches only the local scope. |
| Destination resolution | **`git rev-parse --path-format=absolute --git-common-dir`** | `.git` is a *file* in a linked worktree — this one resolves to `C:/dev/repos/ttrigo/asapp/.git/worktrees/asapp-replace-maven-with-gradle`, whose `hooks` subdirectory git never reads. `--git-common-dir` returns the shared directory that actually holds the hooks; `--path-format=absolute` (Git 2.31+, local is 2.54) removes the relative-vs-absolute inconsistency between a plain clone (`.git`) and a worktree. Hardcoding `.git/hooks` would silently install into a directory nothing reads. |
| Rejected: `Copy` task type | **A custom `DefaultTask`, not `Copy`** | `Copy` declares its destination as an `@OutputDirectory`, which enrols `.git/hooks` in Gradle's stale-output cleanup: Gradle "keeps a registry of all the outputs generated by Gradle which will be reset on each version change… If Gradle encounters an existing output file, then it will remove it if it is not part of the registered outputs" (gradle/gradle#821, #4353). That registry resets on every Gradle upgrade, and the directory holds 13 `*.sample` files Gradle did not create. Gradle deleting files inside `.git` is not a risk worth an up-to-date check on a task that runs once per clone. |
| Task tracking | **`@UntrackedTask(because = …)`** | The honest declaration of the above: the task's effect lives outside anything Gradle should fingerprint. It always runs, costs ~50ms, and the reason is recorded in the code rather than only here. |
| Rejected: `Sync` | **Never `Sync`** | Would delete the `*.sample` files and any hook installed by another tool. `Copy` semantics only. |
| Executable bit | **`filePermissions { unix("755") }`** | `setFileMode(0755)` is removed in Gradle 9; `filePermissions { unix("755") }` is the replacement, and the value is a **string**. Without it a fresh install on Linux/macOS/WSL produces non-executable hooks, which git skips **silently** — the worst possible failure mode for a commit gate. Verified: the installed files land as `-rwxr-xr-x`. |
| Task class location | **`build-logic/src/main/kotlin/com/attrigo/asapp/gradle/InstallGitHooks.kt`** | Developer requirement: task *definition* must not sit in a build script mixed with its *configuration*. `buildSrc` is barred by the DSL rules, so `build-logic` is the only home. Verified that a precompiled script plugin imports a class from its own `src/main/kotlin` without extra wiring. This is `build-logic`'s **first** `.kt` file — the four existing files are all `.gradle.kts` — so it takes a package, `com.attrigo.asapp.gradle`. |
| Registration location | **`build-logic/src/main/kotlin/asapp.root-conventions.gradle.kts`** | The root project cannot reference a `build-logic` *class* without applying a *plugin* from it: `pluginManagement { includeBuild("build-logic") }` substitutes plugin markers, not ordinary dependencies. A convention plugin is therefore not decoration but the mechanism, and it lands the registration one layer away from the class body, which is the separation asked for. |
| Named for the archetype, not the concern | **`asapp.root-conventions`, never `asapp.git-hooks-conventions`** | `build-logic` keeps one file per **module archetype** and rejects concern plugins outright. The root project is a genuine fifth archetype — the only project in the build with no Java, no modules, and no parent convention — so a file for it extends the existing axis rather than adding a second one. A `git-hooks` plugin would be exactly the rejected shape, and would need a sibling for every future root-level concern. |
| The plugin-less rule | **Reworded, not waived** | The rule reads "the root project stays plugin-less", but its body argues one thing only: `base` "would register root `clean`/`assemble`/`check`/`build`" whose cost is that "`:build` and `:check` become root-level no-ops that succeed while doing nothing". It already carves out "an aggregation plugin at the root would **not** violate that prohibition". Gradle's own *Best Practices for Structuring Builds* is narrower still — "be careful not to apply plugins unnecessarily in the root project — many plugins only affect source code" — and names the root as "the place to configure some settings and conventions that apply globally to the entire build". A repository has one hooks directory, so this is build-global by definition. Verified: with the plugin applied, root `tasks --all` lists `installGitHooks` under **Build Setup** and no `build` / `check` / `assemble` / `clean`. The heading over-generalised the body; the reworded rule targets plugins that register root lifecycle task names. |
| Not a root aggregator | **Permitted, and not a violation** | The separate prohibition (`gradle.md`, *Full build*) is on root tasks that **name subprojects' tasks**. `installGitHooks` names nothing and does its own work; there is no subproject counterpart for it to shadow, and no fan-out to inherit. |
| Git invocation site | **Inside the task action, via injected `ExecOperations`** | Resolving the directory at configuration time would fork `git` on **every** invocation, `./gradlew help` included. `workingDir` is set explicitly to the `git/hooks` source directory — it lies inside the working tree, which is all `git rev-parse` needs, and the Gradle daemon's own working directory is not something to inherit. |
| Trigger | **Explicit only — no build-time wiring** | Developer decision. Maven re-copied on every build; the Gradle analog would be doing the copy in `settings.gradle.kts`, which is configuration-time work on every invocation and, once the configuration cache lands, would silently degrade to running only on a cache miss. Hooks change roughly never, so once per clone is the whole story. |
| pre-commit formatting command | **`./gradlew spotlessCheck --console=plain --quiet`** | `--console=plain` because `gradle.properties` **forces** `org.gradle.console=verbose`, which emits raw ANSI escapes into non-terminal contexts such as IntelliJ's commit dialog. `--quiet` because the Liquibase plugin's `Project.container(Class, Closure)` deprecation warning fires on every invocation and would print on every commit. Verified against a deliberate violation: `-q --console=plain` still prints the full per-file diff and `Run 'gradlew.bat spotlessApply' to fix all violations`, and exits 1. |
| Wrapper guard in the hook | **`if [ -x ./gradlew ]` … else skip with a warning** | Structural, not scaffolding. An installed hook lives in `.git/hooks`, which is **shared by every worktree and does not change when you switch branches** — verified: neither `main` nor `ai/establish-conventions-rules-agents` contains `gradlew`, and the second is checked out in the sibling worktree right now. Without the guard, installing this hook breaks `git commit` in that worktree outright (`set -e` aborts). The guard remains correct after Maven removal, for any checkout of history predating the migration. `-x` works under Git-Bash on Windows (`gradlew` is `100755` in the index, verified). |
| Rejected: staged-only check | **Whole-project `spotlessCheck`** | Spotless removed `-PspotlessFiles` in favour of `ratchetFrom`, and `ratchetFrom` compares against a git ref rather than the index — it is a gradual-adoption tool for unformatted codebases, not a staged-files filter. This codebase is fully formatted, so it would buy nothing. Consequence to accept: the hook's two halves disagree about what "the commit" is — the CRLF check reads the staged blob, `spotlessCheck` reads the working tree. Deliberate, and the ordinary behaviour of every build-tool formatter hook. |
| Rejected: `spotlessApply` in the hook | **`spotlessCheck`, fail and tell the developer** | Applying would reformat files the developer did not stage and leave the commit contents disagreeing with the working tree unless the hook also re-staged — a hook silently editing a commit is worse than a hook refusing one. |
| `commit-msg` | **Untouched** | Pure bash, no build-tool reference, already correct. |

### 4.1 Third-party plugin survey

There is **no Gradle equivalent of `git-build-hook-maven-plugin`**. Ten plugins match "git hooks" on the Gradle Plugin Portal; seven are abandoned (`com.samcgardner.hookup` 2018, `com.stefletcher…` a 2016 SNAPSHOT, `tekgenesis.git_hooks` 2020, plus four single-author 0.0.x/1.0.0 releases untouched since 2023–25). The rest were read at source level:

| Plugin | Latest | Why it fails here |
|---|---|---|
| `org.danilopianini.gradle-pre-commit-git-hooks` | 2.1.21 · Jul 2026 | **Refuses to run in a worktree** — `GitHooksExtension.createHooks` detects `gitdir:` in `.git`, logs "Hooks generation in detached worktrees is not supported" (issue #396) and returns without writing. Also generates hooks from DSL strings/URLs rather than installing the tracked scripts, and writes at configuration time from `settings.gradle.kts`. |
| `eu.bambooapps.gradle.plugin.githook` | 1.1.1 · Dec 2024 | Closest shape — same task name — but `CopyGitHooks` filters `include("**/*.sh")`, and both hooks are extensionless, so it copies **nothing and reports success**. Destination is a `DirectoryProperty` defaulting to `.dir(".git")`, so worktrees break. `@CacheableTask` **plus** `@OutputDirectory` on `.git/hooks` means a build-cache hit clears the directory before restoring. Ignores `core.hooksPath`. |
| `com.github.jakemarsden.git-hooks` | 0.0.2 · **May 2020** | `GitHookWriter.generateScript` emits exactly `"#!/bin/bash\n" + gradleCommand + ' ' + gradleTask`, so a hook can only be one Gradle task invocation — the 60-line and 34-line scripts cannot be expressed. **`commit-msg` is impossible**: git passes the message file as `$1` and the generated script forwards no arguments. Last commit 2020-09-19; `project.afterEvaluate`, predating the configuration cache. |
| `com.fizzpod.lefthook` | Jul 2026 | Wraps Lefthook, a separate Go binary with its own YAML config, replacing the scripts entirely. |

All of them assume `.git` is a directory. The two capabilities that decide this task — resolving the shared hooks directory and clearing `core.hooksPath` — are present in none.

## 5. Changes by file

**`build-logic/src/main/kotlin/com/attrigo/asapp/gradle/InstallGitHooks.kt`** (new) — the task definition.

```kotlin
package com.attrigo.asapp.gradle

// imports: java.io.ByteArrayOutputStream, java.io.File, javax.inject.Inject,
// org.gradle.api.DefaultTask, org.gradle.api.file.{DirectoryProperty, FileSystemOperations},
// org.gradle.api.tasks.{InputDirectory, TaskAction, UntrackedTask}, org.gradle.process.ExecOperations

@UntrackedTask(because = "Writes into the Git repository's hooks directory, which Gradle must neither fingerprint nor clean")
abstract class InstallGitHooks : DefaultTask() {

    @get:InputDirectory
    abstract val source: DirectoryProperty

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val fsOps: FileSystemOperations

    @TaskAction
    fun install() {
        val sourceDir = source.get().asFile

        // ".git" is a file in a linked worktree, so the hooks live in the shared common directory,
        // not in this worktree's own git dir. Resolved from inside the working tree.
        val hooksDir = File(git(sourceDir, "rev-parse", "--path-format=absolute", "--git-common-dir"), "hooks")

        // 755 because git silently skips a hook that is not executable.
        fsOps.copy {
            from(sourceDir)
            into(hooksDir)
            filePermissions { unix("755") }
        }
        logger.lifecycle("Installed the Git hooks into $hooksDir")

        // git reads that directory only while core.hooksPath is unset; "--unset" exits 5 when absent.
        val result = execOps.exec {
            workingDir = sourceDir
            commandLine("git", "config", "--unset", "core.hooksPath")
            isIgnoreExitValue = true
        }
        if (result.exitValue == 0) {
            logger.lifecycle("Cleared core.hooksPath, which was shadowing that directory")
        }
        // "--unset" reaches only the local scope; a global or system value would still shadow.
        val remaining = git(sourceDir, "config", "--get", "core.hooksPath", ignoreExitValue = true)
        if (remaining.isNotEmpty()) {
            logger.warn("WARNING: core.hooksPath is still set to '$remaining' outside this repository - the installed hooks will not run")
        }
    }

    private fun git(dir: File, vararg args: String, ignoreExitValue: Boolean = false): String { /* captures stdout */ }
}
```

**`build-logic/src/main/kotlin/asapp.root-conventions.gradle.kts`** (new) — the registration, and the root archetype's home for any future build-global task.

```kotlin
import com.attrigo.asapp.gradle.InstallGitHooks

// Installs the project's Git hooks. Run once per clone, and again after editing a hook:
//   ./gradlew installGitHooks
// The hooks are copied rather than reached via core.hooksPath, so .git/hooks stays the live directory.
tasks.register<InstallGitHooks>("installGitHooks") {
    group = "build setup"
    description = "Installs the project's Git hooks into the repository's hooks directory."
    source = layout.projectDirectory.dir("git/hooks")
}
```

**`build.gradle.kts`** (root) — replaces the placeholder comment in full.

```kotlin
plugins {
    id("asapp.root-conventions")
}
```

**No `build-logic/build.gradle.kts` change.** The task uses core Gradle APIs only — `kotlin-dsl` already supplies the Gradle API and `javax.inject`, so there is no catalog version, no library entry, and no `implementation(...)` line, unlike spotless / pitest / asciidoctor / liquibase / git-properties / Boot.

**`git/hooks/pre-commit`** — the LF section is untouched; only the trailing formatting block changes.

```diff
  echo "Checking code style:"

- mvn spotless:check
+ # The installed hook lives in .git/hooks, which every worktree shares and which does not change
+ # when you switch branches - so a tree without the wrapper must not block the commit.
+ if [ -x ./gradlew ]; then
+     ./gradlew spotlessCheck --console=plain --quiet
+ else
+     echo -e "${YELLOW}[SKIP] No Gradle wrapper in this working tree - skipping the formatting check.${NC}"
+ fi
```

`set -e` at the top of the script already turns a non-zero `gradlew` exit into an aborted commit; no explicit exit handling is needed.

**`.claude/rules/gradle.md`** — two edits.

1. **Reword the plugin-less bullet** in *Coverage*. It currently opens "**The root project stays plugin-less, deliberately** — never apply `base` to the root `build.gradle.kts`, and never delete it". Rewrite the prohibition to what the bullet's own body argues and what this subtask verified: **never apply a plugin that registers root lifecycle task names** — `base` is the example, because root `build` / `check` / `assemble` / `clean` become no-ops that succeed while doing nothing, the same shape the *Full build* section rejects for a custom name. Keep the `clean`-gain and stray-reports reasoning, keep the aggregation-plugin carve-out, and add that `asapp.root-conventions` is the sanctioned counter-example: it registers `installGitHooks` and no lifecycle name (verified via root `tasks --all`), and Gradle's *Best Practices for Structuring Builds* both warns only against "plugins that only affect source code" at the root and names the root as "the place to configure some settings and conventions that apply globally to the entire build".

2. **Add a *Git hooks* section** between *Database migrations* and *Ordering*, documenting: `./gradlew installGitHooks` replaces `mvn git-build-hook:install`; the task **definition** lives in `build-logic/src/main/kotlin/com/attrigo/asapp/gradle/InstallGitHooks.kt` and its **registration** in `asapp.root-conventions`, never both in a build script — `build-logic`'s first `.kt` file, so class sources take the `com.attrigo.asapp.gradle` package while precompiled script plugins stay unpackaged at the source-set root, and a precompiled script plugin imports a class from its own `src/main/kotlin` with no extra wiring; the plugin is named for the **root archetype**, never `asapp.git-hooks-conventions`, because `build-logic` keeps one file per archetype and rejects concern plugins — the root is a genuine fifth archetype and future build-global tasks belong in the same file; the root cannot reference a `build-logic` class without applying a plugin from it, since `pluginManagement { includeBuild(...) }` substitutes plugin markers and not ordinary dependencies; the destination is resolved with `git rev-parse --path-format=absolute --git-common-dir` and **never** hardcoded to `.git/hooks`, because `.git` is a file in a linked worktree and that worktree's own git dir holds no hooks git will read; **never** use a `Copy` or `Sync` task — `Copy` registers the destination as an `@OutputDirectory`, enrolling `.git/hooks` in Gradle's stale-output cleanup, whose registry resets on every Gradle version change and which deletes files it did not create (the directory holds 13 `*.sample` files), and `Sync` would delete them outright; the task is `@UntrackedTask` and declares no outputs, so it always runs at ~50ms, and it is configuration-cache compatible (verified); `filePermissions { unix("755") }` is mandatory, not cosmetic — `setFileMode` is removed in Gradle 9, the replacement takes a **string**, and git skips a non-executable hook **silently**; the task clears `core.hooksPath` because a set value makes git ignore `.git/hooks` entirely (this repo had it set to `git/hooks`), tolerating `--unset`'s exit code **5** when the key is absent and warning when a global or system value survives; `git` runs **inside the task action** via injected `ExecOperations` with `workingDir` set explicitly, never at configuration time, which would fork `git` on every invocation including `./gradlew help`; the task is explicit-only and wired into **no** lifecycle task — re-run it after editing a hook, since the copy does not track its source; the pre-commit hook runs `./gradlew spotlessCheck --console=plain --quiet` (`--console=plain` because `gradle.properties` forces `org.gradle.console=verbose`, which emits raw ANSI escapes into IntelliJ's commit dialog; `--quiet` because the Liquibase plugin's deprecation warning would otherwise print on every commit — verified that violations, the per-file diff and the `spotlessApply` hint all still print) guarded by `if [ -x ./gradlew ]`, because an installed hook is shared across worktrees and does not change on branch switch, so a tree predating the migration must not have its commits blocked; `spotlessCheck` reads the **working tree** while the LF check reads the **staged blob**, a deliberate asymmetry since Spotless dropped per-file selection in favour of `ratchetFrom`, which compares against a git ref rather than the index; and **no third-party plugin is adopted** — there is no Gradle equivalent of `git-build-hook`, and all four live candidates assume `.git` is a directory (§4.1 of the design spec records which fails how).

**`TODO.md`** — three edits:

1. Check off "Migrate git hook installation to Gradle" (line 27).
2. Add under "Migrate build documentation to Gradle" (line 61):

```markdown
        - **Note:** the root README's `mvn git-build-hook:install` becomes `./gradlew installGitHooks`, and the Git Hooks section's "Automatically installed on `mvn install`" must not survive the rewrite — the Gradle task is deliberately explicit, run once per clone and again after editing a hook
```

3. Add under "Verify full parity, then remove Maven entirely" (line 78):

```markdown
        - **Note:** retire Maven's git-hook wiring here — the `git-build-hook-maven-plugin.version` property and the `pluginManagement` entry in the root `pom.xml`, plus the seven per-module `<build><plugins>` declarations (2 libs + 5 services); until then both tools write byte-identical files to the same directory, so coexistence is inert
```

## 6. Placement / altitude rationale

- **Definition → `build-logic/.../InstallGitHooks.kt`.** A task class is code; it belongs in a source file with a package, IDE support, and a compiler — not inlined in a script whose job is configuration.
- **Registration → `asapp.root-conventions`.** One layer away from the class body, and the only mechanism by which the root can see that class at all.
- **Application → root `build.gradle.kts`, one line.** A repository has one hooks directory; seven tasks writing the same two files would race under `org.gradle.parallel`.
- **Nothing in `asapp.java-conventions` or any module archetype plugin.** Hook installation is not a property of a module.
- **Nothing in `settings.gradle.kts`.** That is where the every-build variant would have lived; it was rejected in §4.

## 7. Verification / Definition of Done

Items 1–8 were proven during design against a scratch composite build mirroring this structure; they are re-run here against the real repository.

1. **The task installs.** `./gradlew installGitHooks` reports the destination as `C:/dev/repos/ttrigo/asapp/.git/hooks` — the *common* directory, not this worktree's git dir — and both files appear there matching `git/hooks/`.
2. **Permissions.** Both installed hooks are `-rwxr-xr-x`.
3. **The shadow is gone.** `git config --get core.hooksPath` returns nothing and exits 1; the run logged that it cleared the value.
4. **Re-running is clean.** A second `./gradlew installGitHooks` succeeds, logs no "cleared" line (the key is already absent, `--unset` exits 5), and emits no warning.
5. **The class is visible to the plugin.** `build-logic` compiles and `asapp.root-conventions` resolves `InstallGitHooks` with no `build-logic/build.gradle.kts` change.
6. **No root lifecycle tasks.** `./gradlew tasks --all` at the root lists `installGitHooks` under **Build Setup** and no `build`, `check`, `assemble` or `clean` root entry.
7. **Configuration cache.** `./gradlew installGitHooks --configuration-cache` reports `Configuration cache entry stored`.
8. **No leak into ordinary builds.** `./gradlew help --dry-run` mentions `installGitHooks` zero times, and `./gradlew help` logs none of the task's `lifecycle` lines — every `git` call sits inside `@TaskAction`.
9. **The hooks fire from anywhere.** `git commit` from `services/asapp-users-service/` runs both hooks; git changes the working directory to the worktree root before invoking them, so `./gradlew` resolves.
10. **The formatting gate bites.** Introduce a formatting violation, stage it, and `git commit`: the hook fails, prints the per-file diff and the `spotlessApply` hint, and no commit is created. `./gradlew spotlessApply` then lets the same commit through.
11. **The CRLF gate still bites.** Stage a file with CRLF endings and confirm the existing check fails before Gradle is invoked.
12. **`commit-msg` still bites.** A non-conventional message is rejected; a `fixup!` message is accepted.
13. **Cost.** A commit with nothing to reformat completes in ~3s of Gradle time; output carries no ANSI escapes and no deprecation warning.
14. **Gradle touched nothing else in `.git/hooks`.** All 13 `*.sample` files survive.
15. **The sibling worktree still commits.** In `C:/dev/repos/ttrigo/asapp` (on `ai/establish-conventions-rules-agents`, which has no `gradlew`), `git commit` runs the new hook, prints the skip warning, and completes.
16. **Maven untouched**: no `pom.xml` edited, so `mvn git-build-hook:install` behaves as before by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

README migration (line 61) · removing the Maven plugin (line 78) · the `100644` mode on the tracked scripts · new hook types · Spotless's `spotlessInstallGitPrePushHook` · `ratchetFrom` · staged-only formatting · a third-party hook plugin · CI changes (line 38) · `commit-msg` content · any `pom.xml` or application-source edit.

## 9. Contingencies

- **`filePermissions` is a no-op on Windows.** Expected and harmless: NTFS has no exec bit, and Git-Bash runs hooks by shebang regardless. The setting exists for Linux/macOS/WSL clones. It did apply in the scratch verification, which ran on this machine.
- **A `--global` `core.hooksPath` is in play.** The task warns rather than unsetting it — a global setting is a deliberate user-level choice and not a repository's to remove. The warning names the offending value.
- **`--path-format` unavailable.** Git 2.31+; local is 2.54. If an older git ever appears, fall back to bare `--git-common-dir` and resolve the result against the working-tree root.
- **The hook is too slow in practice.** The measured floor is ~3s. If it becomes annoying, the escape hatch is `git commit --no-verify` for the one-off, and moving the formatting gate to `pre-push` for the general case — but that is a policy change, not a fix, and belongs in its own entry.
- **A stale hook after editing `git/hooks/*`.** Inherent to the copy approach: re-run `installGitHooks`. If this bites repeatedly, the answer is the `core.hooksPath` mechanism rejected in §4, not a build-time re-copy.
- **`asapp.root-conventions` stays a one-task plugin.** Accepted: it is the root archetype's file, not a concern plugin, so a single member is the expected starting state rather than a smell. Future build-global tasks land beside it — that is the point of naming it for the archetype.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-16-git-hooks`. A single commit:

1. `build(gradle): migrate git hook installation to Gradle` — the `InstallGitHooks` task class, `asapp.root-conventions`, the root `build.gradle.kts`, the `git/hooks/pre-commit` change, the two `.claude/rules/gradle.md` edits, and the three `TODO.md` edits.

Following this migration's established pattern, implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-08-01-gradle-git-hooks.md`) were written before implementation. The core change shipped substantially as designed — the `InstallGitHooks` task class in `build-logic`, its `asapp.root-conventions` registration, the one-line root `id("asapp.root-conventions")` application, the `--git-common-dir` destination resolution, and the `core.hooksPath`-clearing mechanism all landed as sketched, and every load-bearing §4 mechanic (the `@UntrackedTask` annotation, the rejection of `Copy`/`Sync`, `filePermissions { unix("755") }`, injected `ExecOperations`/`FileSystemOperations`, the `com.attrigo.asapp.gradle` package) survives verbatim.

The canonical implementation is the current state of `InstallGitHooks.kt`, `asapp.root-conventions.gradle.kts`, the root `build.gradle.kts`, `git/hooks/pre-commit`, and the `## Git hooks` section of `.claude/rules/gradle.md` on this branch — not this document. The deltas below are where the two diverge; each names the section it supersedes or extends.

`Notable deltas:`

- **The pre-commit wrapper guard inverted from a skip-with-warning to a hard gate — supersedes §4's "Wrapper guard in the hook" row, §5's `pre-commit` diff block, and falsifies §7 item 15 as written.** A tree that cannot be style-checked must not commit unchecked: the skip guard let the formatting gate silently disappear, the same failure mode the design elsewhere calls "the worst possible" for a missing executable bit. The final guard in `git/hooks/pre-commit` is `if [ ! -x ./gradlew ]; then echo …"ERROR: No Gradle wrapper (./gradlew) available - cannot check formatting."; exit 1; fi` — the message names both causes, since the test fires whether the wrapper is absent or present without its mode bit. No fallback to a globally installed `gradle` was added: `gradlew` is committed at mode `100755`, so it is missing only in a tree predating the migration, which has no Gradle build for a global `gradle` to run against, and a global install would drift from the wrapper's pinned 9.6.1. The `## Git hooks` pre-commit bullet in `.claude/rules/gradle.md` records the consequence as accepted — a checkout predating the migration is blocked too. §7 item 15's verification is now that the sibling worktree exits 1 with this explicit error, not a bash diagnostic.
- **The pre-commit LF check was not left untouched — supersedes §5's "the LF section is untouched" claim and §1's hook table.** This migration introduced `build-logic`'s Kotlin sources and `.gradle.kts` convention scripts, and neither extension was in the CRLF check's list, so a CRLF Kotlin file could have been committed silently; all 17 tracked Kotlin files were already LF, so nothing starts failing as a result, but the gap existed regardless. `git/hooks/pre-commit` now sets `FILE_EXTENSIONS='java|kt|kts|xml|…'`, and the `dos2unix` help text — which carried its own hardcoded copy of the extension list, the drift its `# keep in sync!` comment warned against — now interpolates `$FILE_EXTENSIONS` instead. The top-of-script comment describing `set -e` was also reworded, from "any maven command" to "any command."
- **The copy is deliberately unbounded, and the hook set is derived at run time rather than declared — supersedes the spec's Scope line "copies the two tracked hooks" and extends §5's `InstallGitHooks.kt` sketch.** An intermediate hardening step bounded the copy to a `HOOK_NAMES = listOf("commit-msg", "pre-commit")` companion constant, then reverted it: baking the hook set into the class while the source directory stayed an input property meant a newly added hook would be silently dropped even as the task reported success — a silent-gate failure mode. The accepted inverse cost is a documented convention rather than a code constraint. `copyHooks` in `InstallGitHooks.kt` now derives `val hookNames = trackedHooksDir.listFiles { file -> file.isFile }.orEmpty().map { it.name }.sorted()`, and the copy spec carries no `include(...)`. The first `## Git hooks` bullet in `.claude/rules/gradle.md` warns to keep `git/hooks/` clean, since an untracked scratch file named like a real hook would start running for every worktree sharing the hooks directory.
- **A post-copy `canExecute()` check warns about hooks git would silently skip — extends §4's "Executable bit" row and §5's sketch.** Git skips a non-executable hook silently, and on Linux the copy spec's `unix("755")` is the only thing making the copies executable at all, since the tracked sources are mode `100644` — §1's own observation, previously treated as inert rather than as a real dependency. `InstallGitHooks.kt`'s `warnAboutNonExecutableHooks(gitHooksDir, hookNames)` now emits `WARNING: the installed {} is not executable - git will skip it silently` for any hook that fails the check; the `filePermissions` bullet in `.claude/rules/gradle.md` records that the guard is Linux/macOS/WSL-only, since NTFS reports `canExecute()` true regardless, so a silent run on Windows is not evidence the installed hooks are actually executable.
- **The `core.hooksPath` unset distinguishes a failed local unset from an external value that survives it — supersedes §4's "Clearing `core.hooksPath`" row and §5's sketch, both of which collapsed every non-zero exit into "the key was absent."** An unset that genuinely failed — a read-only or lock-contended config file — left the local value in place, and the design's two-way logic would then report it as a value set outside the repository, sending the developer to global config to chase a local problem. Verified against all three paths: exit 5 is silent, exit 0 reports the value cleared, and a locked config (exit 255) now names the local failure directly. `InstallGitHooks.kt`'s `clearHooksPath(workingDirectory)` computes `val localUnsetFailed = unsetResult.exitValue != 0 && unsetResult.exitValue != 5`, emits a distinct `WARNING: clearing this repository's core.hooksPath failed with exit value {} - its config file may be read-only or locked by another git process`, and branches three ways on `remainingHooksPath`. The `core.hooksPath` bullet in `.claude/rules/gradle.md` still describes only the two-outcome version and has not been updated for the third — a known documentation gap, recorded here rather than silently carried.
- **The task action is decomposed into four named private steps, with the exit-code rationale moved into KDoc — extends §5's single flat `install()` sketch.** The flat block computed the unset outcome well above the branch that consumed it, and the exit-code reasoning sat in an inline comment the log message half-repeated. `install()` in `InstallGitHooks.kt` now reads `resolveGitHooksDir` → `copyHooks` → `warnAboutNonExecutableHooks` → `clearHooksPath`, and the exit-code reasoning for `clearHooksPath` lives entirely in that function's KDoc rather than split between a comment and a log line.
- **Identifiers renamed throughout, including the task's public input property, folding in the `git` helper this section previously misdescribed — supersedes §5's sketch names.** `sourceDir`/`hooksDir` gave no way to tell which was which at a glance, the one distinction the whole task turns on; no behaviour changed. In `InstallGitHooks.kt`: the public input `source` → `hooksSource`; `execOps`/`fsOps` → `execOperations`/`fileSystemOperations`; `sourceDir` → `trackedHooksDir`; `hooksDir` → `gitHooksDir`; `git` → `runGit`; `output` → `stdoutBuffer`; `text` → `gitOutput`; `dir` → `workingDirectory`; `args` → `arguments`; `unset` → `unsetResult`; `remaining` → `remainingHooksPath`; `hook` → `hookName`. The public rename propagates to the one caller, `asapp.root-conventions.gradle.kts`. The helper's final signature is `private fun runGit(workingDirectory: File, vararg arguments: String, ignoreExitValue: Boolean = false): String`, writing into a `ByteArrayOutputStream` set as `standardOutput`, logging `Running git {} in {}` at debug before executing and `Git answered '{}'` after, and returning the trimmed `gitOutput`.
- **The hooks source is anchored with `layout.settingsDirectory`, not `layout.projectDirectory` — supersedes §5's `asapp.root-conventions.gradle.kts` sketch (`source = layout.projectDirectory.dir("git/hooks")`).** `projectDirectory` resolved correctly only because the plugin happens to be applied at the root; applied to a subproject it would point the task's `@InputDirectory` at `<module>/git/hooks` and fail validation with a message that says nothing about the real mistake. `.claude/rules/gradle.md` already settles this exact axis under `## Running locally` for `bootRun`'s `workingDir`. The final registration reads `hooksSource = layout.settingsDirectory.dir("git/hooks")`, and the three-line explanatory comment above it collapsed to one line.
- **Info/debug instrumentation and a class-level KDoc contract were added — extends §5's sketch, which carried inline comments and a single `lifecycle` line.** The task now explains itself at runtime, each message naming the reason for the step it announces. The `--unset` invocation bypasses `runGit` (which logs before executing), so it was the one of three git calls with no pre-call trace, leaving the class docstring's `--debug` promise unmet until this was closed. `InstallGitHooks.kt` now carries a class KDoc — "Add `--info` to see what it resolved, `--debug` to see every git call." — six `logger.info` step lines, a `logger.debug("Running git config --unset core.hooksPath in {}", workingDirectory)` call, and a lifecycle line naming both ends (`logger.lifecycle("Installed the Git hooks from {} into {}", trackedHooksDir, gitHooksDir)`); every message moved from Kotlin string interpolation to SLF4J `{}` placeholders.
- **Four `.claude/rules/gradle.md` edits landed, not §5's two.** The file sanctioned `asapp.root-conventions` only inside its own `## Git hooks` section, while the normative home for the pattern still read "one file per **module** archetype" and templated IDs as `asapp.<concern>-conventions` — the git-hooks section relied on a layout the rule's own axis statement forbade. The frontmatter `paths:` list gained `"**/build-logic/src/main/kotlin/**/*.kt"`, the new file type this task introduced; `## Shared Build Configuration` now reads `asapp.<archetype>-conventions` and "one file per **project** archetype … plus `asapp.root-conventions` for the root — a genuine fifth archetype"; and the `## Git hooks` root-archetype bullet was shortened to defer to that section instead of re-arguing it.
- **The `## Git hooks` section no longer cites the design spec, by policy — supersedes §5's rules-instruction clause "(§4.1 of the design spec records which fails how)".** That was the only citation of an archivable `docs/superpowers/specs/` path anywhere in `.claude/rules/`, and specs are archived at release, which would have left a dangling reference. The four rejected plugins are now inlined with per-candidate reasons directly in the `**No third-party plugin is adopted**` bullet — `eu.bambooapps.gradle.plugin.githook`, `org.danilopianini.gradle-pre-commit-git-hooks`, `com.github.jakemarsden.git-hooks`, `com.fizzpod.lefthook` — so the rules file is self-contained and this document is no longer load-bearing for it.
- **`spotlessInstallGitPrePushHook` is ruled out normatively, not just listed among §3's non-goals.** Spotless registers that task as a side effect of applying `id("com.diffplug.spotless")`, so it exists in this build and would write an unmanaged `pre-push` hook into the same shared hooks directory — tracked by nothing, reviewed by nothing, removed by nothing. The second `## Git hooks` bullet in `.claude/rules/gradle.md` now states that `git/hooks/` plus `installGitHooks` stay the only owner of hook policy.
- **The swap's cost is comparable, not a win, but the per-commit floor the design estimated is now directly measured — supersedes only §1's "Measured cost of the swap" table, and does not touch §7 item 13, which it confirms rather than contradicts.** The whole-hook wall clock came in at 4.698s old (Maven) versus 4.604s new (Gradle) — about 2% apart, not the halving the table projected. That comparison undersells the actual change, though: Maven's `mvn spotless:check` reported `Spotless check skipped` on every module and `BUILD SUCCESS` unconditionally, doing zero real work regardless of whether the staged file was misformatted, while Gradle's gate genuinely evaluates the files and fails the commit — `exit=1` plus a diff — when a violation exists. The case for the swap is correctness, not speed. Separately, `.claude/rules/gradle.md`'s `## Git hooks` section now records three warm-daemon, nothing-to-reformat `./gradlew spotlessCheck --console=plain --quiet --offline` runs at 3.03s / 3.22s / 3.11s, confirming §7 item 13's "~3s of Gradle time" prediction directly rather than leaving it an estimate.
- **`:installGitHooks`'s own duration is measured at 360-410ms on a warm daemon, with a cold-clone caveat now recorded — corrects §4's "Task tracking" row and the same "~50ms" figure repeated in §5's rules-section instruction, and completes what the surviving cost note originally left unqualified.** Three `./gradlew installGitHooks --profile` runs against this repository put the task's own execution duration at 375ms, 408ms, and 361ms — the ~50ms in the design was an unmeasured guess. `.claude/rules/gradle.md`'s `## Git hooks` section now marks that range "warm daemon" explicitly and adds that a fresh clone additionally resolves the entire `build-logic` plugin classpath before the task's files are copied, a resolution `--offline` cannot serve — a cost the original figure did not account for.
- **`TODO.md` carries six edits, not §5's three — supersedes §5's "`TODO.md` — three edits" and qualifies §3's "coexistence is inert" claim.** The Maven-removal note's "coexistence is inert" holds only conditionally: it is true while every checkout sharing the hooks directory carries the same `git/hooks/` content, but a Maven build from a pre-migration checkout re-installs the old hook repo-wide, so the note now adds "re-run `./gradlew installGitHooks` after one" — and the sibling worktree is currently such a checkout, so the condition is live rather than hypothetical. A new in-version subtask, "Add automated tests for the build's custom tasks," was added: exercising the custom task types needs a real git repository, so the tests are functional rather than unit — `build-logic` became source-bearing in this task, and the task class changed several times during review with only manual verification behind it. Three Backlog items were added: a `.kt`/`.kts` formatter, since Spotless is applied only in `asapp.java-conventions` and declares only a `java` format, leaving the convention plugins and this task's own class as the one part of the build the commit gate does not check; stale-hook detection — warn when an installed hook no longer matches its tracked source, a warning only, never a failure, giving §9's stale-hook contingency a tracked detection path; and a staged-content check — check formatting against the staged content rather than the working tree, turning §4's "Rejected: staged-only check" row and §3's "Staged-only formatting" non-goal from closed rejections into a tracked follow-up, noting that the obvious `git stash --keep-index` wrapper is unsafe as written.

For future `build-logic` task edits, treat `InstallGitHooks.kt`, `asapp.root-conventions.gradle.kts`, and the `## Git hooks` section of `.claude/rules/gradle.md` as the template; this spec is preserved as a record of the original design intent.

## 12. References

- Git — [githooks](https://git-scm.com/docs/githooks): "Before Git invokes a hook, it changes its working directory to either `$GIT_DIR` in a bare repository or the root of the working tree in a non-bare repository"; "By default the hooks directory is `$GIT_DIR/hooks`, but that can be changed via the `core.hooksPath` configuration variable". The first sentence is why `./gradlew` resolves from a hook invoked in a subdirectory.
- Git — [git-rev-parse](https://git-scm.com/docs/git-rev-parse): `--git-common-dir` returns the shared git directory of a linked worktree; `--path-format=absolute` (Git 2.31+) forces absolute output, removing the plain-clone (`.git`) vs. worktree divergence.
- Git — [git-worktree](https://git-scm.com/docs/git-worktree): a linked worktree's `.git` is a file pointing at `$GIT_DIR/worktrees/<id>`; hooks are not per-worktree, so the common directory is the only correct destination.
- rudikershaw — [`git-build-hook`](https://github.com/rudikershaw/git-build-hook) and its `InstallMojo`: `@Mojo(name = "install", defaultPhase = LifecyclePhase.PROCESS_SOURCES)`; the hooks directory is built as `<git-directory>/hooks` from JGit's `FileRepositoryBuilder`, with no `core.hooksPath` lookup. The README presents `configure` (which sets `core.hooksPath`) and `install` (which copies) as alternative approaches.
- Gradle — [Best Practices for Structuring Builds](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html): "Be careful not to apply plugins unnecessarily in the root project — many plugins only affect source code and should only be applied to the projects that contain source code"; the root project "is the place to configure some settings and conventions that apply globally to the entire build, that are not configured via Settings". The basis for rewording the plugin-less rule.
- Gradle — [`Copy` DSL reference](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html): `filePermissions { unix(…) }` / `dirPermissions { unix(…) }`; "If the property has no value set, that means that existing permissions are preserved". `setFileMode(int)` was deprecated for removal in Gradle 9.
- Gradle — [`UntrackedTask`](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/UntrackedTask.html) (since 7.3): marks a task whose state Gradle does not track, so it always runs and its destinations are excluded from output handling.
- Gradle — stale output cleanup, [gradle/gradle#821](https://github.com/gradle/gradle/issues/821) and [#4353](https://github.com/gradle/gradle/issues/4353): Gradle keeps a registry of generated outputs which resets on each version change, and removes existing files in registered output directories that are not part of it. The basis for refusing an `@OutputDirectory` inside `.git`.
- Gradle — [`ExecOperations`](https://docs.gradle.org/current/javadoc/org/gradle/process/ExecOperations.html): the injectable replacement for `project.exec`, usable from a task action without holding a `Project` reference.
- Diffplug — [Spotless Gradle plugin](https://github.com/diffplug/spotless/tree/main/plugin-gradle): `spotlessInstallGitPrePushHook` installs a *pre-push* hook (pre-commit tracked as issue #623); `ratchetFrom` limits formatting to files changed since a given git ref and is documented for gradual adoption on unformatted codebases.
- Plugin survey sources (§4.1): [`gradle-pre-commit-git-hooks`](https://github.com/DanySK/gradle-pre-commit-git-hooks) `GitHooksExtension.kt` and [issue #396](https://github.com/DanySK/gradle-pre-commit-git-hooks/issues/396) · [`GitHookPlugin`](https://github.com/BambooAppsDevTeam/GitHookPlugin) `CopyGitHooks.kt` / `GitHookPlugin.kt` · [`git-hooks-gradle-plugin`](https://github.com/jakemarsden/git-hooks-gradle-plugin) `GitHookWriter.java` · [Gradle Plugin Portal, "git hooks"](https://plugins.gradle.org/search?term=git+hooks).
