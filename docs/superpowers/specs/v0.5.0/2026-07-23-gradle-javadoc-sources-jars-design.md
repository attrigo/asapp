# Gradle javadoc & sources jar generation — design spec

**Date**: 2026-07-23
**Status**: Approved — pending implementation
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate javadoc and sources jar generation to Gradle" (line 21). No attached TODO note.
**Scope**: Reproduce Maven's `maven-javadoc-plugin` (`javadoc-no-fork`, `doclint=all,-missing`) and `maven-source-plugin` (`jar-no-fork`) under Gradle at parity, for the 5 modules that activate them today — the 2 libs (`asapp-commons-url`, `asapp-http-clients`) and the 3 domain services (authentication, tasks, users). Register plain `javadocJar` / `sourcesJar` `Jar` tasks in `asapp.library-conventions` and `asapp.domain-service-conventions`, and configure the `javadoc` task's doclint. Generation is **opt-in** (off `check`/`build`), run via `./gradlew javadocJar sourcesJar` — the flag-free analog of the reports migrated before it. No source change, no `pom.xml` edit.

## 1. Context

Nine prior subtasks put dependency management, compilation, unit testing, integration testing, coverage reporting, mutation testing, formatting checks, and API documentation on Gradle 9.x / JDK 25. This subtask migrates the supplementary-jar generation (javadoc + sources), the tenth build stage.

**What Maven does today.** The parent `pom.xml` declares both plugins in `<pluginManagement>` only — configuration, not activation:

1. **`maven-javadoc-plugin`** — execution `attach-javadocs`, goal `javadoc-no-fork`, bound to the `verify` phase, `<doclint>all,-missing</doclint>` (report every javadoc problem except missing-comment warnings). `javadoc-no-fork` reuses the already-compiled build rather than forking a second compile.
2. **`maven-source-plugin`** — execution `attach-sources`, goal `jar-no-fork`, bound to the `verify` phase; packages the main sources into a `-sources.jar`.

Both are **skipped by default** (`maven.javadoc.skip=true`, `maven.source.skip=true` in the parent `<properties>`) and **enabled only under `-Pfull`** (the `full` profile flips both skips to `false`). They are never `mvn deploy`-ed — the jars are generated as a build-completeness / doc-lint exercise, not published anywhere.

**Which modules activate them.** `<pluginManagement>` alone runs nothing; a child must re-declare the plugin in its own `<build><plugins>`. Exactly 5 do:

| Module | Convention plugin | javadoc + sources? |
|---|---|---|
| `libs/asapp-commons-url` | `asapp.library-conventions` | ✅ |
| `libs/asapp-http-clients` | `asapp.library-conventions` | ✅ |
| `services/asapp-authentication-service` | `asapp.domain-service-conventions` | ✅ |
| `services/asapp-tasks-service` | `asapp.domain-service-conventions` | ✅ |
| `services/asapp-users-service` | `asapp.domain-service-conventions` | ✅ |
| `services/asapp-config-service` | `asapp.service-conventions` | ❌ |
| `services/asapp-discovery-service` | `asapp.service-conventions` | ❌ |

The 2 infra services (config, discovery — thin Spring Cloud Config Server / Eureka wrappers) get nothing. The target set is therefore exactly `library-conventions` (2 libs) ∪ `domain-service-conventions` (3 domain services).

**Current Gradle state.** The `java` plugin (via `asapp.java-conventions`) already registers a `javadoc` task and the `main` source set in every module; no `javadocJar`/`sourcesJar` task exists yet. The Java 25 toolchain configured in `asapp.java-conventions` already routes `javadoc` (and `test`) through the same JDK as compilation — a documented Gradle guarantee (§9 References). The Spring Boot plugin is **not** applied yet (that is the next subtask, line 22), so `assemble` currently produces a plain library jar and there is no bootJar interaction to consider.

## 2. Goals

- **Parity of artifacts**: a `-javadoc.jar` and a `-sources.jar` per targeted module, javadoc built with `doclint=all,-missing`.
- **Parity of reach**: the same 5 modules Maven activates (2 libs + 3 domain services); config/discovery excluded — the flag-free analog of Maven's per-module plugin re-declaration.
- **Command**: `./gradlew javadocJar sourcesJar` (or `:services:asapp-tasks-service:javadocJar`) replaces the `-Pfull`-gated Maven executions.
- **Off the check path**: generation is opt-in, not attached to `check`/`build`/`assemble` — consistent with the coverage, mutation, and API-doc migrations, and faithful to Maven's default-skip / `-Pfull`-only behavior. Serves the 0.5.0 build-speed goal (local `./gradlew build` stays fast).
- **On the module-archetype axis**: the config lives in the two archetype plugins that need it, not in a new concern-oriented plugin — matching how the existing plugins are organized (developer decision, §4).
- Settings at the correct altitude: all config in the two convention plugins; nothing per-leaf.
- Zero source edits, zero `pom.xml` edits; Maven's `-Pfull` javadoc/sources keep working until the final removal subtask.

## 3. Non-goals

- **Packaging** (Spring Boot plugin, `bootJar`, build-info, git.properties, the temporary `ActuatorEndpointsIT` `/info` filter) — the next subtask (line 22–27).
- **Full-build aggregation.** Folding `javadocJar`/`sourcesJar` into a one-command `full`/`fullBuild` umbrella alongside coverage, the formatting check, and API docs is the "Migrate the full build" subtask (line 28–31); this subtask only makes the tasks exist and be invokable.
- **Publishing.** No `maven-publish`, no deploy, no artifact variants — parity with Maven, which generates but never deploys these jars.
- **README / human-doc sync.** The `mvn ... -Pfull` references in the root/per-service READMEs are not touched here — matching every prior migration commit. This belongs to "Migrate build documentation" (line 39) / the Claude-files sync subtask (line 48).
- **CI / git-hook swaps**, **CLAUDE.md command swap**, and the **final Maven removal** — each its own later subtask.
- **Documenting the MapStruct mapper implementations** — a separate backlog item ("Add Javadoc to mapper implementations", pending MapStruct 1.6.0); see §4 and §7.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Task registration | **Register plain `Jar` tasks `javadocJar` / `sourcesJar` manually — NOT `java.withJavadocJar()` / `withSourcesJar()`** | Per `JavaPluginExtension` (§9 References), the `withXxxJar()` helpers register the jars as documentation variants **and add them as a dependency of `assemble`** — forcing javadoc onto every `./gradlew build`. That contradicts Maven's default-skip / `-Pfull`-only behavior and the opt-in pattern used for coverage/pitest/asciidoctor. Manual `Jar` tasks give the same artifacts with the same classifiers, off the default path. Variant registration is irrelevant here — there is no publishing. |
| Placement | **Declare the block in `asapp.library-conventions` AND `asapp.domain-service-conventions`** | The target set (2 libs + 3 domain services) matches no single existing plugin — the nearest common ancestor, `asapp.java-conventions`, is all 7 and would pull in the infra services. The convention plugins are an axis of **module archetype** (`java`→`library`/`service`→`domain-service`), with concerns folded into whichever archetype needs them (e.g. `pitest`/`asciidoctor` live in `domain-service-conventions`). Declaring javadoc/sources in the two archetypes that need it stays on that axis; a new `asapp.<concern>-conventions` plugin would introduce a second, crossing axis (developer decision). The small repeated block is an accepted pattern here — cf. the `val libs = extensions.getByType(...)` accessor already duplicated across `java`/`service`/`domain-service` conventions, with de-duping parked in the backlog. |
| Not in `java-conventions` | **Rejected** | It applies to all 7 modules, so config + discovery would gain javadoc/sources jars Maven never produces — breaking the "verify full parity" gate (line 52). Worse, `doclint=all,-missing` would run on infra-service source that was never doc-linted, risking new build failures for modules nobody documents. Expanding doc coverage to infra services is a deliberate policy change to raise as its own TODO entry, not to smuggle into a migration (developer decision). |
| doclint fidelity | **`(options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)`** on the `javadoc` task | Emits `-Xdoclint:all,-missing`, the exact equivalent of Maven's `<doclint>all,-missing</doclint>`. The Kotlin-DSL cast to `StandardJavadocDocletOptions` is required (the `options` getter is typed `MinimalJavadocOptions`); `addBooleanOption(flag, true)` is the idiom for a value-less flag. |
| sources jar contents | **`from(project.the<SourceSetContainer>()["main"].allSource)`, classifier `sources`** | `allSource` = main Java + resources, matching Gradle's own `withSourcesJar()` and Maven's `-sources.jar`. `project.the<SourceSetContainer>()["main"]` is the repo's established idiom inside precompiled convention plugins (cf. `asapp.service-conventions` using `project.the<SourceSetContainer>()["test"]`) — the type-safe `sourceSets` accessor is not reliably generated there. |
| javadoc jar contents | **`from(tasks.named("javadoc"))`, classifier `javadoc`** | Packages the `javadoc` task's output and auto-creates the task dependency, so `javadocJar` builds javadoc on demand. Replaces `javadoc-no-fork`. |
| Toolchain | **No explicit config — rely on the `asapp.java-conventions` toolchain** | Gradle routes the `javadoc` task through the project's configured Java toolchain automatically (§9 References), so javadoc runs on JDK 25 like compilation and test. No `javadocTool` wiring needed. |
| Check-path | **Opt-in — keep both jars off `check`/`build`/`assemble`** | Consistent with coverage/pitest/asciidoctor (generated artifacts stay opt-in, named explicitly) and faithful to Maven's default-skip. This is the whole reason for manual `Jar` tasks over `withXxxJar()`. |
| Task addressing | **`tasks.named<Javadoc>("javadoc")` to configure; `tasks.register<Jar>("javadocJar"/"sourcesJar")` to create** | `named`/`register` are Gradle's recommended configuration-avoidance APIs and the repo's established convention inside precompiled plugins (every task reference in `asapp.java-conventions`/`asapp.service-conventions` uses them). |

## 5. Changes by file

Add the **same block** to both plugins. No module applies both `library-conventions` and `domain-service-conventions` (libs → library → java; domain services → domain-service → service → java — disjoint branches), so there is no double-registration conflict; each plugin's block runs only for its own modules.

**`build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`** — currently just applies `asapp.java-conventions` + `` `java-library` ``. Add the imports and the block:
```kotlin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("asapp.java-conventions")
    `java-library`
}

// --- Javadoc & sources supplementary jars (Maven -Pfull parity) ---
// Opt-in, off check/build — run ./gradlew javadocJar sourcesJar, like the coverage/pitest/asciidoctor reports.
// NOT java.withJavadocJar()/withSourcesJar(): those attach the jars to `assemble`, forcing javadoc onto every build.

// Report all javadoc problems except missing comments — Maven's <doclint>all,-missing</doclint>.
tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

// Package the javadoc output; classifier `javadoc`. Replaces maven-javadoc-plugin:javadoc-no-fork.
tasks.register<Jar>("javadocJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main Javadoc."
    archiveClassifier = "javadoc"
    from(tasks.named("javadoc"))
}

// Package the main sources; classifier `sources`. Replaces maven-source-plugin:jar-no-fork.
tasks.register<Jar>("sourcesJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main sources."
    archiveClassifier = "sources"
    from(project.the<SourceSetContainer>()["main"].allSource)
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** — add the same imports and the same block (placed alongside the existing pitest/asciidoctor config). Exact block placement within the script is a block-ordering detail deferred to the "Clean Gradle files" subtask (line 41).

**`.claude/rules/gradle.md`** — add a **Javadoc & sources jars** section documenting: register plain `javadocJar`/`sourcesJar` `Jar` tasks (classifiers `javadoc`/`sources`) in `asapp.library-conventions` and `asapp.domain-service-conventions` (the 5 modules Maven activates — 2 libs + 3 domain services; config/discovery/`java-conventions` correctly excluded, the flag-free analog of Maven's per-module re-declaration); **not** `java.withJavadocJar()`/`withSourcesJar()` (they attach the jars to `assemble`, breaking the opt-in / `-Pfull` parity); configure `javadoc` with `-Xdoclint:all,-missing` via `(options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)` (Maven's `doclint=all,-missing`); the `javadoc` task uses the `java-conventions` toolchain (JDK 25) automatically; keep both tasks **off** `check`/`build` (opt-in, run `./gradlew javadocJar sourcesJar`), matching the coverage/pitest/asciidoctor reports; the block is repeated in both archetype plugins by design (module-archetype axis, no concern plugin — cf. the version-catalog accessor, an accepted duplication). Any future de-duplication rides with the "Clean Gradle files" subtask (line 40).

**`TODO.md`** — check off "Migrate javadoc and sources jar generation to Gradle" (line 21). No new subtask is spawned.

## 6. Placement / altitude rationale

- **Block → `asapp.library-conventions` + `asapp.domain-service-conventions` (5 modules).** These are the two archetype plugins whose modules Maven activates the plugins in. Same altitude reasoning as pitest/asciidoctor (concern folded into the archetype that needs it), extended to two archetypes because javadoc/sources spans both libs and domain services.
- **Not a new concern plugin.** Keeps the convention-plugin set on its single "module archetype" axis (developer decision, §4).
- **Not `java-conventions`.** Would break parity and doc-lint infra source (§4).
- **No `build-logic` classpath change.** Unlike asciidoctor/pitest/spotless, javadoc and jar packaging are core Gradle (`java`/`java-library` plugin) — no third-party plugin to put on the convention-build classpath, no catalog entry, no `build-logic/build.gradle.kts` edit.

## 7. Verification / Definition of Done

- **Doclint spike (top risk — first implementation step):** run `./gradlew :libs:asapp-commons-url:javadocJar` and one MapStruct service, e.g. `./gradlew :services:asapp-tasks-service:javadocJar`. Confirm `build-logic` compiles, the tasks resolve, and javadoc completes with `-Xdoclint:all,-missing` without failing the build. Maven's `-Pfull` ran the identical doclint level, so a green Maven full build predicts a green Gradle run — but doclint-on-Gradle is exercised here for the first time, so verify explicitly.
- **Artifacts:** for all 5 modules, `./gradlew javadocJar sourcesJar` produces `build/libs/<name>-<version>-javadoc.jar` and `build/libs/<name>-<version>-sources.jar`. Spot-check the sources jar contains `src/main/java` classes and the javadoc jar contains `index.html`.
- **Reach:** the tasks register on the 5 target modules and **not** on config/discovery (`./gradlew :services:asapp-config-service:tasks` shows no `javadocJar`/`sourcesJar`).
- **Off the check path:** `./gradlew check --dry-run` and `build --dry-run` schedule neither `javadocJar` nor `sourcesJar`.
- **MapStruct generated sources (expected divergence, not a blocker):** Gradle's `javadoc` sources from `main.allJava`, which excludes annotation-processor output, so the generated `*MapperImpl` classes are not javadoc'd. This matches the known backlog gap ("Add Javadoc to mapper implementations") and is not a regression — confirm the task simply succeeds without them, do not force generated sources in.
- **Maven untouched:** no `pom.xml` and no source edited, so `mvn ... -Pfull` javadoc/sources are unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

Packaging + the Spring Boot plugin (line 22–27) · the `full`/`fullBuild` umbrella (line 28–31) · publishing / `maven-publish` / deploy · README / human-doc sync (line 39/48) · CI / git-hook swaps · CLAUDE.md command swap + final Maven removal · javadoc for the MapStruct mapper impls (separate backlog item) · any `pom.xml` or source edit · a dedicated `asapp.<concern>-conventions` plugin (explicitly rejected, §4).

## 9. Contingencies

Resolve at implementation, mirroring how prior subtasks hedged their DSL/imports. The spike (§7) selects which apply.

- **Doclint failure in a targeted module.** If `-Xdoclint:all,-missing` fails a module that Maven's `-Pfull` passed, first confirm the Maven full build is actually green for that module (the skip flags mean it may rarely be exercised). If Maven passes and Gradle fails, the cause is almost always a source-root difference — reconcile before checking the box; do not weaken doclint below `all,-missing` (that would break parity).
- **`StandardJavadocDocletOptions` import.** If `import org.gradle.external.javadoc.StandardJavadocDocletOptions` does not resolve on the convention-plugin classpath, fully-qualify the cast inline, or use `addStringOption("Xdoclint:all,-missing", "-quiet")` if the boolean form misbehaves (both emit the same flag; `-quiet` additionally suppresses the warning-count summary).
- **`archiveClassifier` assignment.** If `archiveClassifier = "sources"` (Property assignment) does not compile, fall back to `archiveClassifier.set("sources")`.
- **Source-set accessor.** If `project.the<SourceSetContainer>()["main"]` needs adjustment, mirror the exact form already working in `asapp.service-conventions` for the `test` source set.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle`. A single commit is appropriate given the size:

1. `build(gradle): migrate javadoc and sources jar generation to Gradle` — the `javadocJar`/`sourcesJar` task registration + doclint config in `asapp.library-conventions` and `asapp.domain-service-conventions`, the `.claude/rules/gradle.md` section, and the `TODO.md` checkbox.

Following this migration's established pattern (per the coverage, mutation, formatting, and API-doc subtasks), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec was written before implementation. The change shipped as designed — `javadocJar`/`sourcesJar` `Jar` tasks plus the `-Xdoclint:all,-missing` `javadoc` config now live in `asapp.library-conventions` and `asapp.domain-service-conventions`, opt-in via `./gradlew javadocJar sourcesJar`, for the 5 modules (2 libs + 3 domain services). Verified: all 5 produce `build/libs/<name>-<version>-{javadoc,sources}.jar` (javadoc jar contains the HTML docs, sources jar the `.java` sources); config-service has no `javadocJar` task (`candidates are: 'javadoc'`); `:services:asapp-tasks-service:build --dry-run` schedules neither jar. No `pom.xml` or source edit.

The canonical source of truth for exact behavior is the current state of `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`, `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`, and `.claude/rules/gradle.md` on this branch, not this document.

Notable deltas:

- **`SourceSetContainer` not imported (vs §5's import list).** `org.gradle.api.tasks.SourceSetContainer` is already a Gradle Kotlin-DSL default import — `asapp.service-conventions` uses `project.the<SourceSetContainer>()` without importing it — so the block omits that import to match the repo convention. Only `Jar`, `Javadoc`, and `StandardJavadocDocletOptions` are imported explicitly. (Trimming any that prove redundant is left to the "Review IntelliJ warnings" cleanup subtask, line 46.)
- **Benign javadoc warning on authentication-service — not a doclint failure.** Its `javadoc` emits `warning: unknown enum constant When.MAYBE / class file for javax.annotation.meta.When not found` (a missing JSR-305 `javax.annotation.meta.When`, pulled transitively). It is a javac source-analysis warning, not a doclint error — the build stays green, and Maven's `-Pfull` javadoc emitted the same, so parity holds. No action taken.

## 12. References

- Gradle — [`JavaPluginExtension` (`withJavadocJar()` / `withSourcesJar()`)](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.JavaPluginExtension.html): each "adds a task … the produced artifact is registered as a documentation variant on the java component and added as a dependency on the `assemble` task" — the reason this spec registers plain `Jar` tasks instead.
- Gradle — [Building Java & JVM projects](https://docs.gradle.org/current/userguide/building_java_projects.html): toolchains apply consistently — "not only compilation benefits from it, but also other tasks such as `test` and `javadoc` will also consistently use the same toolchain".
- Gradle — [`StandardJavadocDocletOptions` / `addStringOption` / `addBooleanOption` (Kotlin DSL reference)](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.external.javadoc/-standard-javadoc-doclet-options/index.html): the `(options as StandardJavadocDocletOptions)` cast and `addBooleanOption("Xdoclint:all,-missing", true)` → `-Xdoclint:all,-missing`.
- Gradle — [Task configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html): `named()`/`register()` are the recommended lazy APIs.
