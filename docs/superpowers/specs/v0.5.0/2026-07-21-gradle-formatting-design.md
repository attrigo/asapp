# Gradle formatting checks — design spec

**Date**: 2026-07-21
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate formatting checks to Gradle" (line 19). No attached TODO note.
**Scope**: Reproduce Maven's Spotless formatting check/apply under Gradle at parity — the Eclipse JDT formatter (`4.35` + `asapp_formatter.xml`), the same import order, remove-unused-imports, license header, and UNIX line endings, applied to every Java module. Put the `com.diffplug.spotless` plugin on the `build-logic` classpath and apply + configure it in `asapp.java-conventions`. `spotlessCheck` rides on the `check` task (developer decision — see §4); `spotlessApply` replaces `mvn spotless:apply`. One intentional divergence from Maven: `removeUnusedImports` switches to the `cleanthat-javaparser-unnecessaryimport` engine (§4). No `pom.xml` edit, no CI/git-hook/README change.

## 1. Context

Seven prior subtasks put dependency management, compilation, unit testing, integration testing, coverage reporting, and mutation testing on Gradle 9.6.1 / JDK 25. This subtask migrates the formatting check (Spotless), the eighth build stage.

**What Maven does today.** Spotless is configured once in the root `pom.xml` `pluginManagement` and inherited by every module. It is **skipped by default** (`spotless.check.skip=true`) and enabled only under the `ci` and `full` profiles; the `check` goal is bound to the `verify` phase. Developers apply formatting via `mvn spotless:apply` and the CI runs `mvn verify -Pci`.

`spotless-maven-plugin` `3.4.0`, `<java>` config:
- `<lineEndings>UNIX</lineEndings>` (top-level `<configuration>`)
- includes `src/main/java/**/*.java`, `src/test/java/**/*.java`
- `<eclipse><version>4.35</version><file>asapp_formatter.xml</file></eclipse>`
- `<importOrder><order>java|javax,org,com,,com.attrigo</order></importOrder>` — five groups; the empty group (`,,`) is the catch-all, and `com.attrigo` sorts **after** it
- `<removeUnusedImports/>` (default engine — google-java-format)
- `<licenseHeader><file>header-license</file><delimiter>package </delimiter></licenseHeader>`

`asapp_formatter.xml` (Eclipse `CodeFormatterProfile`, profile version 23) and `header-license` (Apache-2.0 header) both live at the repo root.

**Current Gradle state** (Gradle 9.6.1, JDK 25). No Spotless plugin is applied anywhere yet. `asapp.java-conventions` already owns the all-module Java concerns (toolchain, compiler args, JUnit platform, unit-test include, `jacoco`) — the correct altitude for a formatting concern that reaches every module, exactly as Maven's root-pom `pluginManagement` did.

**How third-party plugins reach the convention build.** A precompiled convention plugin cannot version a plugin in its own `plugins {}` block; the plugin must be on the `build-logic` classpath. The project already does this twice — `io.spring.dependency-management` and `info.solidsoft.pitest`: a catalog `[libraries]` entry, `implementation(libs.<accessor>)` in `build-logic/build.gradle.kts`, then a versionless `id(...)` in the convention plugin. Spotless follows the identical mechanism.

## 2. Goals

- **Parity of formatter**: identical Eclipse JDT formatter (`4.35`) driven by the unchanged `asapp_formatter.xml`, producing byte-identical output.
- **Parity of steps**: same import order (including the empty catch-all group and trailing `com.attrigo`), remove-unused-imports, license header (`header-license`, `package ` delimiter), and UNIX line endings.
- **Parity of reach**: every Java module (2 libs + 5 services), the analog of Maven's inherited root-pom config.
- **Commands**: `./gradlew spotlessApply` replaces `mvn spotless:apply`; `./gradlew spotlessCheck` replaces `mvn spotless:check`.
- **Check-path**: `spotlessCheck` runs as part of `./gradlew check` / `build` (developer decision, §4).
- **Java 25 robustness**: the check runs on the Gradle daemon JVM without daemon-wide `--add-exports` (which the project's conventions forbid).
- Settings at the correct altitude: all Spotless config in `asapp.java-conventions`; nothing per-leaf.
- Zero `pom.xml` edits; Maven's Spotless keeps working until the final removal subtask.

## 3. Non-goals

- **CI workflow swap.** Pointing CI at `./gradlew` is the "Migrate the CI workflow to Gradle" subtask (line 34). CI still runs `mvn verify -Pci`.
- **Git-hook swap.** The pre-commit hook still calls `mvn spotless:check`; migrating it is the "Migrate git hook installation to Gradle" subtask (line 33).
- **Full-build aggregation.** Folding `spotlessCheck` into a one-command `fullBuild` umbrella is the "Migrate the full build to Gradle" subtask (line 28–29).
- **CLAUDE.md `Build` / `Format` command swap** and the **final Maven removal** (line 47) — each its own later subtask.
- **Non-Java formatters** (`.xml` / `.yaml` / `.md`) — explicit Backlog items under `build`; not introduced here.
- **Changing the formatter profile, import order, header, or line endings** — carried over unchanged.

## 4. Key decisions

Each is grounded in the current Spotless Gradle plugin docs — see §9 References.

| Decision | Choice | Rationale |
|---|---|---|
| Plugin + altitude | **Apply `com.diffplug.spotless` in `asapp.java-conventions`** (all 7 modules) | Formatting is an all-module Java concern with the same reach as Maven's inherited root-pom config; `java-conventions` is where the analogous all-module concerns already live (`jacoco`, compiler args). No dedicated `asapp.format-conventions` plugin — the codebase keeps single-tool concerns in `java-conventions` (precedent: `jacoco`). |
| Plugin version | **`com.diffplug.spotless` `8.8.0`** (latest, 2026-06-29) | Requires JRE 17+ / Gradle 7.3+; ships a Gradle 9 fix (`predeclareDepsFromBuildscript()`) and declares configuration-cache support. Catalog-pinned per the project's pin-everything philosophy. |
| Formatter | **`eclipse("4.35").configFile(rootProject.file("asapp_formatter.xml"))`** | Exact Maven parity — same JDT engine version and same profile file, so output is byte-identical. |
| Config-file resolution | **Anchor both files with `rootProject.file(...)`** | The convention plugin applies per-subproject, so a bare `"asapp_formatter.xml"` would resolve against each module's dir and fail. `rootProject.file(...)` points at the repo-root files and also *fixes* the Maven quirk where running Spotless from a submodule broke the formatter path. |
| Import order | **`importOrder("java\|javax", "org", "com", "", "com.attrigo")`** | Direct 1:1 map of `java\|javax,org,com,,com.attrigo`; `""` is the empty catch-all, `com.attrigo` (most-specific prefix) sorts last — identical grouping. |
| Remove-unused-imports engine (**the one divergence from Maven**) | **`removeUnusedImports("cleanthat-javaparser-unnecessaryimport")`** | Maven used the default google-java-format engine, which uses javac internals and needs daemon-wide `--add-exports jdk.compiler/…` on JDK 16+. Spotless Gradle runs the step in the daemon JVM, and the project's conventions explicitly forbid daemon-wide JVM args (`org.gradle.jvmargs` clobbers Gradle's defaults). The cleanthat engine processes any language level on a JDK8+ runtime with no exports and is functionally equivalent for import removal (the Eclipse step does the actual formatting). Robust on the Java 25 daemon with zero JVM-arg fiddling. |
| License header | **`licenseHeaderFile(rootProject.file("header-license"))`** | The Java format's built-in delimiter is already `package `, matching Maven's explicit `<delimiter>package </delimiter>` — so no delimiter argument is needed. |
| Line endings | **`spotless { lineEndings = LineEnding.UNIX }`** | Exact map of Maven's top-level `<lineEndings>UNIX</lineEndings>`. Set once at the extension level. |
| Target files | **Inferred from the Java source sets** (no explicit `target()`) | The `java{}` block auto-targets `src/main/java` + `src/test/java`, equivalent to Maven's explicit includes for the project's standard layout. Build/generated sources (e.g. MapStruct output) are outside the source sets and untargeted by both tools. |
| Step order | **eclipse → importOrder → removeUnusedImports → licenseHeaderFile** | Mirrors the Maven step order so `spotlessApply` converges to identical output. |
| Check-path | **Keep `spotlessCheck` wired into `check`** (Gradle default) | Developer decision. Faithful to the `ci`-profile intent — Maven gated formatting on **every** CI build (`-Pci`), not only `-Pfull` — so it belongs with the everyday gate (`check`), unlike coverage/pitest which were `full`-only and kept off `check`. Cheap, cacheable, idiomatic, zero extra config. Slightly stricter than Maven's local default-skip, but the pre-commit hook already enforces format on every commit. |

## 5. Changes by file

**`gradle/libs.versions.toml`**

`[versions]` — add under `# Build / ## Other` (alphabetical, after `gradle-pitest`):
```toml
# Build
…
## Other
gradle-pitest = "1.19.0"
spotless = "8.8.0"          # NEW
```

`[libraries]` — add the plugin marker under `# Build / ## Other` (alphabetical, after `gradle-pitest-plugin`):
```toml
# Build
## Spring
spring-dependency-management-plugin = { module = "io.spring.gradle:dependency-management-plugin", version.ref = "spring-dependency-management" }
## Other
gradle-pitest-plugin = { module = "info.solidsoft.gradle.pitest:gradle-pitest-plugin", version.ref = "gradle-pitest" }
spotless-plugin = { module = "com.diffplug.spotless:spotless-plugin-gradle", version.ref = "spotless" }   # NEW
```

**`build-logic/build.gradle.kts`** — put the plugin on the convention-build classpath (mirrors the existing wiring):
```kotlin
dependencies {
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.gradle.pitest.plugin)
    implementation(libs.spotless.plugin)     // NEW
}
```

**`build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`** — apply the plugin and add the Spotless config. Add the import and the `id(...)`:
```kotlin
import com.diffplug.spotless.LineEnding      // NEW

plugins {
    java
    jacoco
    id("com.diffplug.spotless")              // NEW
    id("io.spring.dependency-management")
}
```
And the extension block (placement is a block-ordering detail deferred to the "Clean Gradle files" subtask; grouped with the source-quality config):
```kotlin
// Formatting (Spotless) — all modules, mirroring Maven's inherited root-pom config.
// Eclipse JDT 4.35 + asapp_formatter.xml drive the formatting; the config files are anchored to
// rootProject so they resolve from every subproject. removeUnusedImports uses the cleanthat engine
// (not the default google-java-format) so the step needs no daemon --add-exports on Java 25.
spotless {
    lineEndings = LineEnding.UNIX
    java {
        eclipse("4.35").configFile(rootProject.file("asapp_formatter.xml"))
        importOrder("java|javax", "org", "com", "", "com.attrigo")
        removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
        licenseHeaderFile(rootProject.file("header-license")) // default Java delimiter "package " == Maven
    }
}
```

**`.claude/rules/gradle.md`** — add a **Formatting** section: the `com.diffplug.spotless` plugin is on the `build-logic` classpath (catalog `spotless` version + `spotless-plugin` library + `build-logic/build.gradle.kts` dependency) and applied in `asapp.java-conventions` (all modules); version pinned from the catalog; the Java steps preserve Maven exactly — `eclipse("4.35")` + `asapp_formatter.xml`, `importOrder` with the empty catch-all group and trailing `com.attrigo`, `licenseHeaderFile(header-license)` (built-in `package ` delimiter), and `lineEndings = LineEnding.UNIX`; both config files anchored with `rootProject.file(...)`; `removeUnusedImports` uses `cleanthat-javaparser-unnecessaryimport` (not google-java-format) to avoid daemon-wide `--add-exports` on Java 25; `spotlessCheck` rides on `check`; `spotlessApply` formats. (Deeper rule-file restructuring remains the "Keep Claude Code files in sync" subtask.)

**`TODO.md`** — check off "Migrate formatting checks to Gradle" (line 19). No new subtask is spawned.

## 6. Placement / altitude rationale

- **Plugin + all Spotless config → `asapp.java-conventions` (7 modules).** Formatting reaches every Java module exactly like Maven's inherited root-pom config; this is the same altitude as the other all-module Java concerns (`jacoco`, compiler args, JUnit platform). No per-leaf config and no dedicated formatting convention plugin.
- **Plugin marker → `build-logic` classpath.** Required for a versionless `id("com.diffplug.spotless")` in a precompiled convention plugin; identical to the spring-dependency-management and pitest wiring already in the repo.
- **Config files stay at repo root, referenced via `rootProject.file(...)`.** `asapp_formatter.xml` and `header-license` are unchanged; only the reference mechanism moves from Maven's basedir resolution to `rootProject`.

## 7. Verification / Definition of Done

- **Eclipse `4.35` support in Spotless `8.8.0` (top risk):** first implementation step — confirm `build-logic` compiles and `spotlessCheck` configures/resolves the `4.35` JDT formatter on Gradle 9.6.1 / JDK 25. If `4.35` is unsupported by the `8.8.0` eclipse step, re-evaluate (nearest supported version that produces identical output, or report) before proceeding.
- **Parity on the current tree:** `./gradlew spotlessCheck` **passes** on the current Maven-formatted source with no violations, and `./gradlew spotlessApply` produces **no diffs** (`git diff --exit-code`) — proves the Gradle config formats identically to Maven, including the cleanthat import-removal engine and the import-order grouping.
- **Reach:** `spotlessCheck` / `spotlessApply` register on all 7 modules (`./gradlew :libs:asapp-commons-url:tasks --group verification` and a service both show them).
- **Round-trip:** introduce a formatting violation (e.g. bad indentation, an unused import, a stripped header) → `./gradlew spotlessCheck` fails → `./gradlew spotlessApply` fixes it back to the Maven-produced form.
- **Check-path:** `./gradlew check --dry-run` schedules `spotlessCheck`.
- **Java 25 robustness:** `spotlessCheck` runs on the Gradle daemon with no `--add-exports` added anywhere (no `org.gradle.jvmargs` change).
- **Maven untouched:** no `pom.xml` edited, so `mvn spotless:apply` / `mvn verify -Pci` are unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

CI workflow swap (line 34) · git-hook swap (line 33) · the `fullBuild` / `-Pfull` umbrella (line 28–29) · CLAUDE.md command swap + final Maven removal (line 47) · non-Java formatters (`.xml`/`.yaml`/`.md`, Backlog) · any change to `asapp_formatter.xml`, the import order, the header, or line endings · any `pom.xml` edit · a dedicated `asapp.format-conventions` plugin.

## 9. Contingencies

Resolve at implementation, mirroring how prior subtasks hedged their DSL/imports:
- **`spotless {}` accessor.** The type-safe `spotless {}` accessor should generate in the convention plugin since the plugin is on the `build-logic` classpath and applied in the same `plugins {}` block. If it does not, fall back to `configure<com.diffplug.gradle.spotless.SpotlessExtension> { … }`.
- **`LineEnding` import.** If `import com.diffplug.spotless.LineEnding` does not resolve on the convention-plugin classpath, use the fully-qualified `lineEndings = com.diffplug.spotless.LineEnding.UNIX` inline.
- **cleanthat import-removal parity.** The current tree has no unused imports, so both engines are no-ops there; if a deliberately-added unused import is not removed identically, confirm the cleanthat step is active and re-run. The engine only removes unused imports — it does not reorder or reformat (that stays with importOrder + eclipse).
- **`4.35` formatter output drift.** If `8.8.0`'s eclipse step formats `4.35` even slightly differently from `spotless-maven-plugin 3.4.0`, treat any resulting diff as a blocker and reconcile before checking the box (parity is the whole point of this subtask).

## 10. References

- Spotless Gradle plugin — [Plugin portal](https://plugins.gradle.org/plugin/com.diffplug.spotless) (`8.8.0` latest, 2026-06-29; configuration-cache support) & [README](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md) (`eclipse('x').configFile(...)`; `importOrder(...)` with `''` empty group; `removeUnusedImports('cleanthat-javaparser-unnecessaryimport')`; `licenseHeaderFile` built-in `package ` delimiter for Java; target inferred from source sets; `spotlessCheck` added as a dependency of `check`; `spotlessApply`/`spotlessCheck` auto-created per format; JRE 17+ / Gradle 7.3+)
- Spotless — [google-java-format broken on JDK 16+ (removeUnusedImports)](https://github.com/diffplug/spotless/issues/834) (needs `--add-exports jdk.compiler/…`; cleanthat engine is the JDK8+-runtime workaround)
- Gradle — [Sharing build logic in a convention plugin](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html) (`build-logic` composite build; versionless `id(...)` requires the plugin on the classpath)

## 11. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-8-formatting`. A single commit is appropriate given the size:

1. `build(gradle): migrate formatting checks to Gradle` — the catalog `spotless` version + `spotless-plugin` library, the `build-logic` dependency, and the plugin application + `spotless {}` config in `asapp.java-conventions`.

The `.claude/rules/gradle.md` Formatting section and the `TODO.md` checkbox ride with that commit. Following this migration's established pattern (per the coverage and mutation subtasks), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 12. Post-implementation notes

This spec was written before implementation. The core change shipped substantially as designed — Spotless was wired onto the `build-logic` classpath (catalog `spotless = "8.8.0"` + `spotless-plugin`), applied via `id("com.diffplug.spotless")` in `asapp.java-conventions`, and configured with the full `spotless {}` block (eclipse `4.35` + `asapp_formatter.xml`, the five-group import order, the cleanthat remove-unused-imports engine, the license header, and UNIX line endings), with `spotlessCheck` riding on `check`; parity on the current tree was verified and the Eclipse-4.35-on-Spotless-8.8.0 top risk cleared.

Where the two diverge, the canonical implementation is the current state of the real artifacts on this branch — `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts` (the `spotless {}` block), `build-logic/build.gradle.kts`, `gradle/libs.versions.toml`, and the Formatting section of `.claude/rules/gradle.md` — not this document.

Notable deltas:
- **License-header delimiter pinned explicitly (reverses the §4 "License header" decision and the §5 code comment).** §4/§5 asserted Spotless's built-in Java delimiter is already `package `, so no delimiter argument was needed. That is inaccurate — Spotless's built-in Java default is the broader `(package|import|public|class|module) `; the equivalence with Maven's explicit `<delimiter>package </delimiter>` held only coincidentally because every source file in this repo begins with a `package` declaration. The delimiter is now passed explicitly — `licenseHeaderFile(rootProject.file("header-license"), "package ")` in `asapp.java-conventions.gradle.kts` — so Maven parity is literal, not coincidental; the Formatting section of `.claude/rules/gradle.md` was reworded to state this and to note the true broader default.
- **Local escape hatch documented (extends the §4 Check-path decision).** The always-on `spotlessCheck`-on-`check` wiring means a plain `./gradlew build` now fails on unformatted code, with no Maven-style default-skip. The Formatting section of `.claude/rules/gradle.md` now documents `./gradlew build -x spotlessCheck` as the fast-local-loop opt-out (the Gradle analog of Maven's `spotless.check.skip=true`). The always-on decision itself is unchanged.
- **Spotless block comment simplified (refines the §5 snippet).** The multi-line explanatory comment §5 proposed above the `spotless {}` block was collapsed to a single `// Formatting (Spotless) — all modules.` line in `asapp.java-conventions.gradle.kts`; the detailed rationale (cleanthat, rootProject anchoring, etc.) now lives in the Formatting section of `.claude/rules/gradle.md` rather than being duplicated in the build script.
- **build-logic dependencies grouped by scope and origin (refines the §5 snippet).** Beyond the plain three-line dependency snippet §5 showed, the `dependencies {}` block in `build-logic/build.gradle.kts` now carries `// Build` / `// Spring` / `// Other` grouping comments, matching the repo's Ordering convention.
- **cleanthat import-removal engine empirically verified (confirms the §4 intentional divergence).** §4's one deliberate departure from Maven — `removeUnusedImports("cleanthat-javaparser-unnecessaryimport")` instead of google-java-format — was exercised via a manual round-trip (a deliberately unused import added to a source file, confirmed flagged by `spotlessCheck`, then stripped by `spotlessApply`, file restored byte-for-byte). The engine is confirmed active and correct on the Java 25 daemon with no `--add-exports`. No artifact changed; this closed a verification gap, not a code gap.

For future formatting or Spotless edits, treat these artifacts — the `spotless {}` block in `asapp.java-conventions.gradle.kts` and the Formatting section of `.claude/rules/gradle.md` — as the template; this spec is preserved as a record of the original design intent.
