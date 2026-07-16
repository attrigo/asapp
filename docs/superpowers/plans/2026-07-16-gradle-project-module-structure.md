# Gradle Project & Module Structure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a Gradle multi-project build skeleton — wrapper, root settings/build config, a `build-logic` composite build for future shared config, and empty per-module build scripts — that coexists with the untouched Maven build, mirroring today's 7-module layout exactly.

**Architecture:** Gradle project paths mirror the existing `libs/`/`services/` folder layout 1:1 via `include("libs:asapp-commons-url")`-style path includes (no files moved, no custom `projectDir` remapping). Shared build configuration for later subtasks (dependency management, compilation, testing, etc.) will live in precompiled convention plugins inside a `build-logic` composite build, wired in via `pluginManagement { includeBuild("build-logic") }`. This task only creates the skeleton — no plugins, dependencies, or compiler config are applied to any module yet.

**Tech Stack:** Gradle 9.6.1 (Kotlin DSL), Java 25. No new production dependencies.

## Global Constraints

- Gradle version pinned to **9.6.1** — the first stable Gradle line with full Java 25 support (9.1.0+; 8.x fails outright on Java 25).
- All Gradle build scripts use **Kotlin DSL** (`.gradle.kts`).
- Shared build configuration lives in the **`build-logic` composite build** (precompiled convention plugins, `asapp.*-conventions` naming) — never in root `subprojects {}`/`allprojects {}` conditional blocks.
- Root Gradle project name is **`asapp`** (not `asapp-parent` — no Maven-parent-POM concept applies).
- Gradle project paths mirror the existing folder layout exactly: `:libs:asapp-commons-url`, `:libs:asapp-http-clients`, `:services:asapp-authentication-service`, `:services:asapp-config-service`, `:services:asapp-discovery-service`, `:services:asapp-tasks-service`, `:services:asapp-users-service`.
- **No `pom.xml` changes of any kind.** Maven must keep building successfully (`mvn clean install`) throughout and after this plan.
- **No dependency declarations, plugin application (`java`, Spring Boot, etc.), or compiler configuration in this plan.** Those are separate, later `TODO.md` subtasks ("Migrate dependency management to Gradle", "Migrate compilation to Gradle", ...).
- Source spec: `docs/superpowers/specs/v0.5.0/2026-07-16-gradle-project-module-structure-design.md`.

---

### Task 1: Gitignore Gradle's local cache directory

Every later task in this plan runs a Gradle command at the repo root, and Gradle creates a project-local `.gradle/` cache/state directory (configuration cache, file-system-watch state, etc.) the moment it does. Ignoring it now — before any other task runs a Gradle command at the repo root — keeps `git status` clean throughout the rest of the plan. (`build/`, Gradle's default output directory, is already ignored today via the existing `### NetBeans ###` block — no change needed there.)

**Files:**
- Modify: `.gitignore`

**Interfaces:**
- Produces: a repo-root `.gitignore` that keeps `.gradle/` untracked for every subsequent task.

- [ ] **Step 1: Add the Gradle ignore entry**

Append a new section at the end of `.gitignore`:

```gitignore

### Gradle ###
.gradle/
```

- [ ] **Step 2: Verify the pattern actually ignores a Gradle cache directory**

```bash
mkdir -p .gradle/test-probe && touch .gradle/test-probe/file.txt
git status --short
```

Expected: no output (nothing untracked shown) — `.gitignore` is not yet staged, but `git status --short` already applies working-tree `.gitignore` rules, so `.gradle/` must not appear.

- [ ] **Step 3: Clean up the probe directory**

```bash
rm -rf .gradle
```

- [ ] **Step 4: Commit**

```bash
git add .gitignore
git commit -m "$(cat <<'EOF'
chore(gradle): ignore Gradle's local cache directory

EOF
)"
```

---

### Task 2: Scaffold the build-logic composite build

`build-logic` is a separate Gradle build, included by the root build via `pluginManagement`, that will hold every shared convention plugin the remaining 16 Gradle-migration subtasks add (compilation, testing, coverage, mutation testing, formatting, packaging, ...). This task only proves the composite-build wiring works — it deliberately contains zero convention plugins yet.

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`

**Interfaces:**
- Produces: a `build-logic` directory, buildable on its own, that Task 3's root `settings.gradle.kts` will reference via `pluginManagement { includeBuild("build-logic") }`.

- [ ] **Step 1: Create `build-logic/settings.gradle.kts`**

```kotlin
rootProject.name = "build-logic"
```

- [ ] **Step 2: Create `build-logic/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
```

- [ ] **Step 3: Verify build-logic configures and builds cleanly on its own**

The wrapper doesn't exist yet (Task 4), so use the machine's Gradle 9.6.1 installation directly:

```bash
gradle -p build-logic help --console=plain
```

Expected: ends with `BUILD SUCCESSFUL`, having resolved the `kotlin-dsl` plugin from `gradlePluginPortal()`/`mavenCentral()` with zero convention-plugin source files present.

- [ ] **Step 4: Commit**

```bash
git add build-logic/
git commit -m "$(cat <<'EOF'
build(gradle): scaffold the build-logic composite build

EOF
)"
```

---

### Task 3: Add root settings, build config, and module structure

**Files:**
- Create: `gradle.properties`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`

**Interfaces:**
- Consumes: `build-logic` from Task 2 (referenced via `pluginManagement { includeBuild("build-logic") }`).
- Produces: the `group`/`version` project properties (`com.attrigo.asapp` / `0.5.0-SNAPSHOT`) and the 7 module paths (`:libs:asapp-commons-url`, `:libs:asapp-http-clients`, `:services:asapp-authentication-service`, `:services:asapp-config-service`, `:services:asapp-discovery-service`, `:services:asapp-tasks-service`, `:services:asapp-users-service`) that every later Gradle-migration subtask targets.

- [ ] **Step 1: Create `gradle.properties`**

```properties
group=com.attrigo.asapp
version=0.5.0-SNAPSHOT
```

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "asapp"

include("libs:asapp-commons-url")
include("libs:asapp-http-clients")
include("services:asapp-authentication-service")
include("services:asapp-config-service")
include("services:asapp-discovery-service")
include("services:asapp-tasks-service")
include("services:asapp-users-service")
```

- [ ] **Step 3: Create `build.gradle.kts`**

```kotlin
allprojects {
    group = property("group") as String
    version = property("version") as String
}
```

Gradle exposes every key in `$rootDir/gradle.properties` as a project property on **every** project in the build (root and subprojects alike), so `property("group")`/`property("version")` resolve identically wherever they're read from. `group`/`version` are not auto-applied from `gradle.properties` — this explicit `allprojects` assignment is required.

- [ ] **Step 4: Verify the full project tree**

```bash
gradle projects --console=plain
```

Expected — note Gradle automatically creates `:libs` and `:services` as (empty, plugin-less) intermediate projects mapped to their existing directories, since neither is declared as its own `include(...)`:

```
Root project 'asapp'
+--- Project ':libs'
|    +--- Project ':libs:asapp-commons-url'
|    \--- Project ':libs:asapp-http-clients'
\--- Project ':services'
     +--- Project ':services:asapp-authentication-service'
     +--- Project ':services:asapp-config-service'
     +--- Project ':services:asapp-discovery-service'
     +--- Project ':services:asapp-tasks-service'
     \--- Project ':services:asapp-users-service'
```

- [ ] **Step 5: Verify group/version resolve on a leaf module**

```bash
gradle :services:asapp-tasks-service:properties --console=plain | grep -E "^(group|version):"
```

Expected:

```
group: com.attrigo.asapp
version: 0.5.0-SNAPSHOT
```

- [ ] **Step 6: Commit**

```bash
git add gradle.properties settings.gradle.kts build.gradle.kts
git commit -m "$(cat <<'EOF'
build(gradle): add root settings and module structure

EOF
)"
```

---

### Task 4: Generate and pin the Gradle wrapper

**Files:**
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`

**Interfaces:**
- Consumes: the root `settings.gradle.kts` from Task 3 — the repo root must already contain a Gradle settings/build file for the `wrapper` task to be available (an empty directory has no such task and fails with "does not contain a Gradle build").
- Produces: `./gradlew`, the wrapper every remaining task and every later Gradle-migration subtask invokes instead of the machine's globally-installed `gradle`.

The pinned checksum below is the real, published SHA-256 for `gradle-9.6.1-bin.zip`, fetched from `https://services.gradle.org/distributions/gradle-9.6.1-bin.zip.sha256`.

- [ ] **Step 1: Generate the wrapper**

```bash
gradle wrapper \
  --gradle-version 9.6.1 \
  --distribution-type bin \
  --gradle-distribution-sha256-sum 9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14 \
  --console=plain
```

Expected: `BUILD SUCCESSFUL`. Creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`.

- [ ] **Step 2: Verify the generated `gradle-wrapper.properties`**

```bash
cat gradle/wrapper/gradle-wrapper.properties
```

Expected (exact content Gradle 9.6.1 generates for these flags):

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionSha256Sum=9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
networkTimeout=10000
retries=0
retryBackOffMs=500
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Verify the wrapper bootstraps and matches the pinned version**

```bash
./gradlew --version
```

Expected: includes `Gradle 9.6.1`. This is the wrapper's first run, so it downloads `gradle-9.6.1-bin.zip` into its own cache and verifies it against `distributionSha256Sum` — a checksum mismatch would fail this step outright.

- [ ] **Step 4: Verify the wrapper reproduces Task 3's project tree**

```bash
./gradlew projects --console=plain
```

Expected: identical tree to Task 3 Step 4's output, now produced via `./gradlew` instead of the machine's global `gradle` — proving the wrapper is a drop-in replacement.

- [ ] **Step 5: Commit**

```bash
git add gradlew gradlew.bat gradle/
git commit -m "$(cat <<'EOF'
build(gradle): add Gradle wrapper pinned to 9.6.1

EOF
)"
```

---

### Task 5: Add empty per-module build scripts and verify full Maven/Gradle parity

Every leaf module gets an empty `build.gradle.kts` — sufficient for Gradle to treat the directory as a concrete, file-backed project. No plugins or configuration go in yet; that starts with the next `TODO.md` subtask ("Migrate dependency management to Gradle").

**Files:**
- Create: `libs/asapp-commons-url/build.gradle.kts` (empty)
- Create: `libs/asapp-http-clients/build.gradle.kts` (empty)
- Create: `services/asapp-authentication-service/build.gradle.kts` (empty)
- Create: `services/asapp-config-service/build.gradle.kts` (empty)
- Create: `services/asapp-discovery-service/build.gradle.kts` (empty)
- Create: `services/asapp-tasks-service/build.gradle.kts` (empty)
- Create: `services/asapp-users-service/build.gradle.kts` (empty)

**Interfaces:**
- Consumes: the module paths declared in Task 3's `settings.gradle.kts` and the wrapper from Task 4.
- Produces: the final skeleton state that every remaining Gradle-migration subtask edits into (each existing file, currently empty, gains `plugins { id("asapp.<concern>-conventions") }` blocks over time).

- [ ] **Step 1: Create the 7 empty build scripts**

```bash
touch libs/asapp-commons-url/build.gradle.kts
touch libs/asapp-http-clients/build.gradle.kts
touch services/asapp-authentication-service/build.gradle.kts
touch services/asapp-config-service/build.gradle.kts
touch services/asapp-discovery-service/build.gradle.kts
touch services/asapp-tasks-service/build.gradle.kts
touch services/asapp-users-service/build.gradle.kts
```

- [ ] **Step 2: Verify the project tree is unaffected**

```bash
./gradlew projects --console=plain
```

Expected: the exact same tree as Task 3 Step 4 / Task 4 Step 4 — an empty build script changes nothing about project recognition.

- [ ] **Step 3: Verify `./gradlew help` succeeds**

```bash
./gradlew help --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Verify Maven still builds the whole reactor, untouched**

```bash
mvn clean install
```

Expected: `BUILD SUCCESS` for all 10 Maven reactor modules (`asapp-parent`, `asapp-libs`, `asapp-commons-url`, `asapp-http-clients`, `asapp-services`, and the 5 services) — proving Maven and Gradle coexist cleanly and nothing in this plan touched `pom.xml`.

- [ ] **Step 5: Verify no stray untracked files remain**

```bash
git status --short
```

Expected: only the 7 new `build.gradle.kts` files shown as untracked (`??`); no `.gradle/` or `build/` directories listed (ignored per Task 1 and the pre-existing `.gitignore` rule).

- [ ] **Step 6: Commit**

```bash
git add libs/asapp-commons-url/build.gradle.kts \
        libs/asapp-http-clients/build.gradle.kts \
        services/asapp-authentication-service/build.gradle.kts \
        services/asapp-config-service/build.gradle.kts \
        services/asapp-discovery-service/build.gradle.kts \
        services/asapp-tasks-service/build.gradle.kts \
        services/asapp-users-service/build.gradle.kts
git commit -m "$(cat <<'EOF'
build(gradle): add empty build scripts for all modules

EOF
)"
```

---

## Plan self-review

- **Spec coverage:** every section of the design spec maps to a task — §5/§6 repository layout and root build files → Task 3; §7 build-logic scaffold → Task 2; §9 wrapper → Task 4; §8 leaf stubs → Task 5; §10 `.gitignore` → Task 1 (moved earlier than the spec's suggested last-commit slot — see below); §11 validation → Task 5 Steps 2–5.
- **Deviation from the spec's suggested commit order (§13):** the spec suggested gitignoring `.gradle/` as the *last* commit. Planning surfaced a correctness issue: every task from Task 2 onward runs a Gradle command at (or under) the repo root, which creates a `.gradle/` cache directory immediately — leaving it untracked (though harmless) across four intermediate commits. Moving the gitignore change to Task 1 avoids that entirely. The spec's four other commits are preserved in substance, just reordered so `wrapper` generation (Task 4) comes after the root `settings.gradle.kts` it depends on (Task 3) — the spec's original grouping of wrapper generation together with the build-logic scaffold would have run `gradle wrapper` in a directory with no settings file yet, which fails outright (verified empirically during planning).
- **Placeholder scan:** no TBD/TODO markers; every step shows real, empirically-verified command output rather than a guess.
- **Type/name consistency:** the module path list (Task 3 Step 2) and the leaf `build.gradle.kts` file list (Task 5) both enumerate the same 7 modules in the same order; `group`/`version` values are identical everywhere they appear (Task 3 Steps 1, 3, 5).
