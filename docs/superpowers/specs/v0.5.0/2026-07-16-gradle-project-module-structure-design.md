# Gradle project & module structure — design spec

**Date**: 2026-07-16
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Set up the Gradle project and module structure"
**Scope**: New Gradle build files (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, wrapper, `build-logic/`, per-module stubs) added alongside the existing Maven build. No `pom.xml` changes, no dependency/plugin/compiler configuration.

## 1. Context

ASAPP builds today with Maven: a root `asapp-parent` POM (Spring Boot 4.0.5 parent, Java 25, shared `pluginManagement`), two aggregator POMs (`libs`, `services`, each with their own shared `dependencyManagement`/plugin config), and 7 leaf modules — 2 libraries (`asapp-commons-url`, `asapp-http-clients`) and 5 services (`asapp-authentication-service`, `asapp-config-service`, `asapp-discovery-service`, `asapp-tasks-service`, `asapp-users-service`).

`TODO.md` v0.5.0 schedules a 17-subtask migration off Maven onto Gradle, ending with "verify full parity, then remove Maven entirely." This spec covers only the **first** subtask: standing up the Gradle project/module skeleton that every later subtask (dependency management, compilation, testing, coverage, mutation testing, formatting, docs, packaging, running locally, Docker, git hooks, CI, release, build docs) will build on top of. Maven keeps building the project, unmodified, until the final subtask.

## 2. Goals

- A working Gradle multi-project build that recognizes all 7 current modules at the same physical paths, with no files moved.
- A place for shared build configuration (Maven's `pluginManagement`/`dependencyManagement` equivalent) that scales cleanly across the next 16 subtasks without becoming a tangled root script.
- Zero impact on the existing Maven build — both build systems coexist and both succeed throughout the migration.
- A structural foundation that needs no further changes once this task lands — every subsequent subtask is purely additive (a new convention plugin, applied where needed).

## 3. Non-goals

- No dependency declarations or version catalog (next subtask: "Migrate dependency management to Gradle").
- No compiler, source-set, or Spring Boot plugin configuration (next-next subtask: "Migrate compilation to Gradle").
- No testing, coverage, mutation testing, formatting, packaging, Docker, git hook, CI, or release changes — each is its own later subtask.
- No `pom.xml` edits of any kind.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build script DSL | **Kotlin DSL** (`.gradle.kts`) throughout | Type-safe, best IntelliJ support (already the project's IDE), and the natural fit for writing the precompiled convention plugins in `build-logic`. |
| Shared build config | **`build-logic` composite build** with precompiled convention plugins (`asapp.*-conventions`) | Mirrors Maven's `pluginManagement` cleanly; each later subtask adds/extends one isolated plugin file instead of piling conditionals into root `subprojects {}` blocks. Avoids `buildSrc`'s whole-build cache invalidation on any change — directly serves the migration's stated goal of a cached, incremental build. |
| Root project name | `asapp` (not `asapp-parent`) | Gradle's root project isn't a deployable artifact the way a Maven parent POM conceptually is; the `-parent` suffix doesn't carry meaning here. |
| Module path structure | Path mirrors folder layout exactly: `:libs:asapp-commons-url`, `:services:asapp-tasks-service`, etc. | Zero file moves, familiar navigation, no custom `projectDir` remapping needed. Unlike Maven, `libs`/`services` don't need to exist as real Gradle projects — they're pure path/folder grouping. |
| Gradle version | **9.6.1** (current stable) | First stable Gradle line with full Java 25 support (9.1.0+); Gradle 8.x fails outright on Java 25. |

## 5. Repository layout

```
build-logic/                                   # composite build for shared convention plugins
├── settings.gradle.kts                        # rootProject.name = "build-logic"
├── build.gradle.kts                           # `kotlin-dsl` plugin + repositories
└── src/main/kotlin/                           # empty — first asapp.*-conventions plugin lands next subtask

settings.gradle.kts                            # root: includeBuild("build-logic") + module includes
build.gradle.kts                               # root: allprojects { group/version from gradle.properties }
gradle.properties                              # group=com.attrigo.asapp / version=0.5.0-SNAPSHOT
gradlew, gradlew.bat, gradle/wrapper/*          # Gradle wrapper, pinned to 9.6.1

libs/asapp-commons-url/build.gradle.kts        # empty stub
libs/asapp-http-clients/build.gradle.kts       # empty stub
services/asapp-authentication-service/build.gradle.kts   # empty stub
services/asapp-config-service/build.gradle.kts           # empty stub
services/asapp-discovery-service/build.gradle.kts        # empty stub
services/asapp-tasks-service/build.gradle.kts             # empty stub
services/asapp-users-service/build.gradle.kts             # empty stub
```

No `libs/build.gradle.kts` or `services/build.gradle.kts` — those folders are organizational only, not Gradle projects.

## 6. Root build files

**`settings.gradle.kts`**:

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

**`build.gradle.kts`**:

```kotlin
allprojects {
    group = property("group") as String
    version = property("version") as String
}
```

**`gradle.properties`**:

```properties
group=com.attrigo.asapp
version=0.5.0-SNAPSHOT
```

Gradle exposes `$rootDir/gradle.properties` entries as project properties on every project in the build, so `property("group")`/`property("version")` resolve identically whether read from the root or a leaf module. This single-sources the version the same way the Maven parent POM does today via inheritance.

## 7. `build-logic` scaffold

**`build-logic/settings.gradle.kts`**:

```kotlin
rootProject.name = "build-logic"
```

**`build-logic/build.gradle.kts`**:

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}
```

No convention plugin files exist yet — there is nothing to configure until the next subtask needs one. This scaffold only proves the composite-build wiring (`includeBuild("build-logic")` from the root `settings.gradle.kts`) works end to end.

## 8. Leaf module stubs

Each of the 7 leaf modules gets an empty `build.gradle.kts`. An empty file is sufficient for Gradle to recognize the directory as a project; it carries no plugins or configuration until its concern-specific subtask (compilation, dependencies, etc.) fills it in.

## 9. Gradle wrapper

Generated pinned to **Gradle 9.6.1**, `bin` distribution (smaller, faster CI — no bundled sources/docs needed), with `distributionSha256Sum` set in `gradle-wrapper.properties` so a compromised download mirror can't silently swap the Gradle binary.

## 10. `.gitignore`

Add a `.gradle/` entry (Gradle's per-project cache/daemon state directory). `build/` — Gradle's default output directory — is already ignored today. No `.gitattributes` changes needed: `*.jar binary` and the `*.bat` → CRLF rule already cover `gradle-wrapper.jar` and `gradlew.bat` correctly; `gradlew` (extensionless shell script) falls under the existing default LF rule.

## 11. Validation

- `./gradlew projects` lists the root project plus all 7 modules at their expected paths.
- `./gradlew help` succeeds.
- `mvn clean install` still succeeds unchanged — proving Maven and Gradle coexist cleanly.
- `./gradlew build`/`./gradlew check` are **not** run as acceptance criteria — with zero plugins applied anywhere, there is nothing yet to build or check.

## 12. Out of scope / YAGNI

Dependency declarations · version catalog · compiler/source-set config · Spring Boot plugin · testing/coverage/mutation-testing setup · formatting checks · Javadoc/sources jars · packaging · Docker image building · running locally · git hook installation · CI/release workflow changes · any `pom.xml` edit.

## 13. Git workflow

Lands directly on the current branch, `build/replace-maven-with-gradle` (already checked out). Suggested commit slicing:

1. `build(gradle): add wrapper and build-logic composite build scaffold`
2. `build(gradle): add root settings and module structure`
3. `build(gradle): add empty build scripts for all modules`
4. `chore(gradle): gitignore Gradle build/cache output`

## 14. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-07-16-gradle-project-module-structure.md`) were written before implementation. The core change shipped substantially as designed — the root `settings.gradle.kts` module layout, the `build-logic` composite build, the Gradle wrapper, and the per-module leaf `build.gradle.kts` stubs all landed as designed.

Given the deltas below, the canonical implementation is the current state of `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `build-logic/`, the leaf module `build.gradle.kts` files, `.gitignore`, and `.claude/rules/gradle.md` on this branch, not this document.

Notable deltas:

- **Root `group`/`version` block removed (reverses §6/§9)**: Spec §6 and §9 asserted `allprojects { group = …; version = … }` in `build.gradle.kts` was required because Gradle does not auto-apply `group`/`version` from `gradle.properties`. That claim was wrong — Gradle does auto-propagate them — so the block was deleted; `build.gradle.kts` is now a single scaffolding comment, and `.claude/rules/gradle.md`'s Versioning section was rewritten to match. The spec's stated rationale (§6/§9) is superseded by this correction.
- **`build-logic` convention-plugin stubs added ahead of schedule (extends beyond §5/§7/§12's "empty for now")**: Three comment-only stub files were added — `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`, `asapp.library-conventions.gradle.kts`, `asapp.service-conventions.gradle.kts` — establishing a layered `java` → `library`/`service` taxonomy that §12 (Out of scope / YAGNI) deferred to a later subtask. `.claude/rules/gradle.md`'s worked example was updated to name `asapp.service-conventions` (and `asapp.library-conventions`) instead of the originally sketched `asapp.spring-boot-conventions`.
- **`gradle.properties` gained execution toggles beyond §6's group/version scope**: `org.gradle.caching=true` and `org.gradle.parallel=false` (parallel disabled due to a WSL `IOException`) were added, plus a deferred configuration-cache comment — scope pulled forward from later build-tuning subtasks rather than kept to the pure structural skeleton §3/§12 describe.
- **Leaf and root build scripts carry a scaffolding comment instead of being strictly empty (cosmetic vs. §5/§8)**: all 7 leaf `build.gradle.kts` files plus the root script contain `// populated in a later Gradle migration subtask` to signal intentional scaffolding rather than an oversight.
- **New governance/doc artifacts not scoped by §13**: `.claude/rules/gradle.md` (a new Gradle conventions rule doc) was added, and `TODO.md` gained a "Keep Claude Code files in sync with the migration" subtask — both additive, not called for by the original plan.
- **`.gitignore` build-output ignores relocated (cosmetic vs. §10's "no change needed")**: the `build/` and `!**/src/{main,test}/**/build/` ignore lines moved from the NetBeans block into a new `### Gradle ###` section.

For future Gradle build-file edits, treat `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `build-logic/`, and `.claude/rules/gradle.md` as the template; this spec is preserved as a record of the original design intent.
