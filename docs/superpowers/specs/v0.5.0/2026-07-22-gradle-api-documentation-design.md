# Gradle API documentation generation — design spec

**Date**: 2026-07-22
**Status**: Planned
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate API documentation generation to Gradle" (line 20). No attached TODO note.
**Scope**: Reproduce Maven's Spring REST Docs → AsciiDoc → HTML pipeline under Gradle at parity, for the three domain services that have `src/docs/asciidoc` (authentication, tasks, users). Put the `org.asciidoctor.jvm.convert` plugin on the `build-logic` classpath and apply + configure it in `asapp.domain-service-conventions`, wiring the `asciidoctor` task to the snippets that the `*ApiDocumentationIT` tests emit from the `integrationTest` task. Generation is **opt-in** (off `check`/`build`), run via `./gradlew asciidoctor` — the flag-free analog of the reports migrated before it (developer decision, §4). No test-source change, no `pom.xml` edit, no README change.

## 1. Context

Eight prior subtasks put dependency management, compilation, unit testing, integration testing, coverage reporting, mutation testing, and formatting checks on Gradle 9.x / JDK 25. This subtask migrates API documentation generation, the ninth build stage.

**What Maven does today.** The pipeline has two halves:

1. **Snippet generation (tests).** `*ApiDocumentationIT` tests — `@WebMvcTest` slices that extend `RestDocsWebMvcTestContext` (`@ExtendWith(RestDocumentationExtension.class)`, no-arg) — call Spring REST Docs' `document("name", …)` to emit AsciiDoc snippets. Under Maven these land in `target/generated-snippets`. The tests are named `*IT`, so they run in the integration tier (Failsafe under Maven; the `integrationTest` task under Gradle). `spring-restdocs-mockmvc` `4.0.0` is a test dependency.
2. **AsciiDoc → HTML conversion (build plugin).** `asciidoctor-maven-plugin` `3.2.0`, bound to the `post-integration-test` phase (goal `process-asciidoc`), converts `src/docs/asciidoc/api-guide.adoc` → `target/generated-docs/api-guide.html`. Config: `sourceDirectory=src/docs/asciidoc`, `backend=html`, `doctype=book`; a plugin-level dependency on `spring-restdocs-asciidoctor` `4.0.0` supplies the `operation::` block macro and the `snippets` attribute. It runs on **every** `mvn verify`/`install` (not gated behind `-Pfull`).

Only the three **domain** services carry `src/docs/asciidoc/api-guide.adoc`: authentication, tasks, users. Config and discovery (infrastructure services) have none.

**Current Gradle state.** The `integrationTest` task (registered in `asapp.service-conventions`, `include("**/*IT.class")`) already runs the `*ApiDocumentationIT` tests. No Asciidoctor plugin is applied anywhere yet. `asapp.domain-service-conventions` already applies to exactly {authentication, tasks, users} and already declares `spring-restdocs-mockmvc` and the `info.solidsoft.pitest` plugin — it is the correct altitude for a concern scoped to the three API-documented services.

**How third-party plugins reach the convention build.** A precompiled convention plugin cannot version a plugin in its own `plugins {}` block; the plugin must be on the `build-logic` classpath. The project already does this three times — `io.spring.dependency-management`, `info.solidsoft.pitest`, `com.diffplug.spotless`: a catalog `[libraries]` entry, `implementation(libs.<accessor>)` in `build-logic/build.gradle.kts`, then a versionless `id(...)` in the convention plugin. Asciidoctor follows the identical mechanism.

**Snippets output directory — no test change needed.** Spring REST Docs' `RestDocumentationExtension` (no-arg) auto-detects the build tool and defaults its output directory to `target/generated-snippets` under Maven and **`build/generated-snippets` under Gradle** (reference guide, §10). The `*ApiDocumentationIT` tests therefore emit to the correct Gradle location with **zero source changes** when run under `./gradlew integrationTest`.

## 2. Goals

- **Parity of pipeline**: the same two halves — REST Docs snippets from the integration tier, then AsciiDoc→HTML conversion of `src/docs/asciidoc/api-guide.adoc` — producing the same `api-guide.html` content.
- **Parity of reach**: the three domain services that have `api-guide.adoc` (authentication, tasks, users), the analog of the per-service Maven plugin activation.
- **Command**: `./gradlew asciidoctor` (or `:services:<svc>:asciidoctor`) replaces `mvn asciidoctor:process-asciidoc@generate-docs`.
- **Wiring**: `asciidoctor.dependsOn(integrationTest)` — Option A, faithful and simple; snippets come from the `*ApiDocumentationIT` tests in that tier (developer decision, §4).
- **Off the check path**: generation is opt-in, not attached to `check`/`build` — consistent with the coverage and mutation reports (developer decision, §4).
- **Minimal baseline**: the convention-plugin config is the minimal Spring-reference equivalent, adapted only where the repo forces it (catalog-pinned version; `integrationTest` in place of `test`; `tasks.named` because the code lives in a precompiled convention plugin). Risk mitigations (AsciidoctorJ version pin, JDK-25 fork JVM) are held as contingencies (§9), added only if the spike proves them necessary.
- Settings at the correct altitude: all config in `asapp.domain-service-conventions`; nothing per-leaf.
- Zero test-source edits, zero `pom.xml` edits; Maven's Asciidoctor keeps working until the final removal subtask.

## 3. Non-goals

- **Javadoc / sources jar generation** — the next subtask (line 21).
- **Packaging** (Spring Boot plugin, build-info, git.properties, the temporary `ActuatorEndpointsIT` `/info` filter) — line 22–27.
- **Full-build aggregation.** Folding `asciidoctor` into a one-command `fullBuild` umbrella alongside coverage / javadoc / sources is the "Migrate the full build" subtask (line 28–29); this subtask only makes the task exist and be invokable.
- **README / human-doc sync.** The "REST API docs → `target/generated-docs/api-guide.html`" rows and the `mvn asciidoctor:…` commands in the root and per-service READMEs are **not** touched here — matching every prior migration commit (the root README still shows Maven paths, e.g. `target/site/jacoco-aggregate/…`). This belongs to "Migrate build documentation" (line 39) / the Claude-files sync subtask (line 48).
- **CI / git-hook swaps**, **CLAUDE.md command swap**, and the **final Maven removal** — each its own later subtask.
- **Changing the docs themselves** (`api-guide.adoc`, the snippets, the `*ApiDocumentationIT` tests) — carried over unchanged.

## 4. Key decisions

Each is grounded in the current Asciidoctor Gradle plugin / Spring REST Docs docs and source — see §10 References.

| Decision | Choice | Rationale |
|---|---|---|
| Plugin + altitude | **Apply `org.asciidoctor.jvm.convert` in `asapp.domain-service-conventions`** (the 3 domain services) | API docs exist only for {authentication, tasks, users}, exactly the set that applies this plugin and already declares `spring-restdocs-mockmvc` + `pitest`. Config/discovery (service-conventions) correctly get nothing — the flag-free analog of Maven's per-service activation. |
| Plugin version | **`org.asciidoctor.jvm.convert` `4.0.5`** (latest stable, 2025-08-25) | Runs on Gradle 9.x — issue #770 confirms 4.0.5 works on Gradle 9.5 with only a benign `StartParameter.isConfigurationCacheRequested` deprecation warning (from a transitive Grolifant dep; no functional blocker). `5.0.0-alpha.1` is a prerelease the plugin's own docs steer away from. Catalog-pinned per the project's pin-everything philosophy. |
| REST Docs extension | **`spring-restdocs-asciidoctor` on a custom `asciidoctorExt` configuration, activated per-task via `configurations("asciidoctorExt")`** | The documented Spring wiring. It supplies the `operation::` block macro and auto-points the `snippets` attribute at `build/generated-snippets`. Version reuses the existing catalog `spring-restdocs = "4.0.0"` (parity with Maven's `spring-restdocs-asciidoctor` 4.0.0). **Gotcha:** declaring the dependency is not enough — the task-level `configurations("asciidoctorExt")` call is what puts it on the AsciidoctorJ extension classpath; omit it and `operation::`/`{snippets}` silently fail. |
| Snippet source task | **`asciidoctor.dependsOn(integrationTest)` + `integrationTest.outputs.dir(snippetsDir)`** (Option A) | The snippet-producing `*ApiDocumentationIT` tests run in the integration tier, not the unit `test` task — so the Spring sample's `dependsOn(test)` maps to `dependsOn(integrationTest)`. Faithful and simple; docs always reflect a real integration run. Accepted cost: regenerating docs runs the full integration tier (including the Docker/Testcontainers `*IT`/`*E2EIT`), even though the doc snippets themselves come from Docker-free `@WebMvcTest` slices — this mirrors Maven, where docs rode on `verify` after the ITs. |
| Snippets directory | **`layout.buildDirectory.dir("generated-snippets")`**, declared as `integrationTest.outputs.dir(...)` and `asciidoctor.inputs.dir(...)` | Matches the REST Docs Gradle default (so the no-arg extension writes there with no test change) and establishes correct producer→consumer up-to-date tracking. |
| Check-path | **Opt-in — keep `asciidoctor` off `check`/`build`** | Consistent with how coverage and mutation reports were migrated (generated artifacts stay opt-in, named explicitly). The Spring reference sample likewise does not attach `asciidoctor` to `check`. Serves the 0.5.0 build-speed goal. Diverges from Maven's always-on-`verify` behavior — an accepted, deliberate divergence. |
| Output directory | **Accept the plugin default `build/docs/asciidoc/api-guide.html`** (no override) | Idiomatic Gradle. Verified from plugin source (`AsciidoctorTask.groovy` / `OutputOptions.groovy` at `release_4_0_5`): single-backend `html5` lands files directly in `build/docs/asciidoc` with no `/html5` subfolder. (The Spring reference table's `build/asciidoc/html5` is stale for this plugin version.) The Maven path `target/generated-docs/api-guide.html` is not mirrored; the README path row that documents it is updated later (§3, line 39). |
| Backend / doctype | **Leave both at plugin defaults — no explicit config** | Default backend is `html5`, which Asciidoctor treats as the target Maven's `<backend>html</backend>` aliases to. `doctype=book` is already declared in the `api-guide.adoc` header (`:doctype: book`), so the Maven plugin-level `<doctype>book</doctype>` is redundant under Gradle. Omitting both keeps the baseline minimal (see §2). |
| Task addressing | **`tasks.named<AsciidoctorTask>("asciidoctor")` / `tasks.named<Test>("integrationTest")`** | `named` is Gradle's recommended configuration-avoidance (lazy) API; the discouraged ones are the eager `getByName`/`create`. The Spring sample's `tasks.asciidoctor { }` / `tasks.test { }` are Kotlin DSL type-safe accessors (themselves backed by `named`) generated only in project build scripts — they are not reliably available in a precompiled convention plugin, where every existing task reference in this repo already uses `tasks.named`/`tasks.register` (`asapp.java-conventions`, `asapp.service-conventions`). |
| AsciidoctorJ version | **Baseline: not pinned. Held as a flagged contingency (§9).** | Plugin 4.0.5 defaults AsciidoctorJ to 2.5.7; the Spring REST Docs reference sample pins `asciidoctorj { version = "3.0.0" }` (and research indicates `spring-restdocs-asciidoctor` 4.0.0 needs AsciidoctorJ 3.0+), yet the Spring Initializr starter omits the pin. Two official Spring sources disagree, so per the "start as minimal as the Initializr snippet, let the spike decide" decision the pin is **not** in the baseline — but it is the **most-likely** contingency, and adding it is also a security win (2.5.7's bundled JRuby carries an unresolved SnakeYAML CVE; 3.0.x moves to an unaffected JRuby 9.4.x). |

## 5. Changes by file

**`gradle/libs.versions.toml`**

`[versions]` — add under `# Build / ## Other` (alphabetical, before `gradle-pitest`):
```toml
# Build
…
## Other
asciidoctor-gradle = "4.0.5"     # NEW
gradle-pitest = "1.19.0"
spotless = "8.8.0"
```

`[libraries]` — add the plugin marker under `# Build / ## Other` (alphabetical, before `gradle-pitest-plugin`) and the REST Docs extension under `# Test / ## Spring` (alphabetical, before `spring-restdocs-mockmvc`):
```toml
# Build
## Other
asciidoctor-gradle-plugin = { module = "org.asciidoctor:asciidoctor-gradle-jvm", version.ref = "asciidoctor-gradle" }   # NEW
gradle-pitest-plugin = { module = "info.solidsoft.gradle.pitest:gradle-pitest-plugin", version.ref = "gradle-pitest" }
spotless-plugin = { module = "com.diffplug.spotless:spotless-plugin-gradle", version.ref = "spotless" }
…
# Test
## Spring
spring-restdocs-asciidoctor = { module = "org.springframework.restdocs:spring-restdocs-asciidoctor", version.ref = "spring-restdocs" }   # NEW
spring-restdocs-mockmvc = { module = "org.springframework.restdocs:spring-restdocs-mockmvc", version.ref = "spring-restdocs" }
```

**`build-logic/build.gradle.kts`** — put the plugin on the convention-build classpath (mirrors the existing wiring; alphabetical, first under `// Other`):
```kotlin
dependencies {
    // Build
    // Spring
    implementation(libs.spring.dependency.management.plugin)
    // Other
    implementation(libs.asciidoctor.gradle.plugin)   // NEW
    implementation(libs.gradle.pitest.plugin)
    implementation(libs.spotless.plugin)
}
```

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** — apply the plugin, wire the extension, and configure the task (baseline, minimal):
```kotlin
import org.asciidoctor.gradle.jvm.AsciidoctorTask   // NEW

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
    id("org.asciidoctor.jvm.convert")               // NEW
}

// … existing libs accessor + pitest block …

// --- API documentation (Spring REST Docs → HTML), the 3 API-documented domain services ---
// spring-restdocs-asciidoctor supplies the operation:: block macro and points the `snippets`
// attribute at build/generated-snippets; the per-task configurations(...) call is what activates it.
val asciidoctorExt by configurations.creating
dependencies {
    asciidoctorExt(libs.findLibrary("spring-restdocs-asciidoctor").get())
}

val snippetsDir = layout.buildDirectory.dir("generated-snippets")

// The *ApiDocumentationIT tests (integration tier) emit the REST Docs snippets
tasks.named<Test>("integrationTest") {
    outputs.dir(snippetsDir)
}

// Convert src/docs/asciidoc/*.adoc → build/docs/asciidoc/*.html, after the integration tier.
// Kept off `check` — opt-in via ./gradlew asciidoctor, like the coverage/pitest reports.
tasks.named<AsciidoctorTask>("asciidoctor") {
    inputs.dir(snippetsDir)
    configurations("asciidoctorExt")
    dependsOn(tasks.named<Test>("integrationTest"))
}
```
(Exact block placement within the script is a block-ordering detail deferred to the "Clean Gradle files" subtask, line 41.)

**`.claude/rules/gradle.md`** — add an **API documentation** section: the `org.asciidoctor.jvm.convert` plugin (`4.0.5`) is on the `build-logic` classpath (catalog `asciidoctor-gradle` version + `asciidoctor-gradle-plugin` library + `build-logic/build.gradle.kts` dependency) and applied **only** in `asapp.domain-service-conventions` (the 3 API-documented domain services — libs and infra services get no `asciidoctor` task, the flag-free analog of Maven's per-service activation); `spring-restdocs-asciidoctor` (catalog version reused from `spring-restdocs`) rides a custom `asciidoctorExt` configuration and **must** be activated on the task via `configurations("asciidoctorExt")` (else the `operation::` macro / `snippets` attribute silently fail); snippets come from the `*ApiDocumentationIT` tests in `integrationTest` (`RestDocumentationExtension` writes to `build/generated-snippets` automatically under Gradle — no test change), so `asciidoctor.dependsOn(integrationTest)` and `integrationTest.outputs.dir(build/generated-snippets)`; output is the plugin default `build/docs/asciidoc/api-guide.html`; the `asciidoctor` task is kept **off** `check`/`build` (opt-in, run `./gradlew asciidoctor`), matching the coverage/pitest reports. (Deeper rule-file restructuring remains the "Keep Claude Code files in sync" subtask, line 48.)

**`TODO.md`** — check off "Migrate API documentation generation to Gradle" (line 20). No new subtask is spawned.

## 6. Placement / altitude rationale

- **Plugin + all Asciidoctor config → `asapp.domain-service-conventions` (3 services).** API docs exist only where `src/docs/asciidoc/api-guide.adoc` does — authentication, tasks, users — which is exactly the set that applies this convention plugin and already declares `spring-restdocs-mockmvc` and `pitest`. Same altitude, same reasoning as the mutation-testing placement.
- **Plugin marker → `build-logic` classpath.** Required for a versionless `id("org.asciidoctor.jvm.convert")` in a precompiled convention plugin; identical to the spring-dependency-management, pitest, and spotless wiring already in the repo.
- **Docs source stays put.** `src/docs/asciidoc/api-guide.adoc` (per service) is unchanged; the plugin's default `sourceDir` (`src/docs/asciidoc`) already matches the Maven `sourceDirectory`.

## 7. Verification / Definition of Done

- **Plugin-on-JDK-25 spike (top risk — first implementation step):** apply the baseline to **one** service and run `./gradlew :services:asapp-authentication-service:asciidoctor`. Confirm `build-logic` compiles, the `asciidoctor` task resolves and runs, and AsciidoctorJ (bundled JRuby) converts cleanly on the Java 25 setup. Two possible outcomes drive §9:
  - an AsciidoctorJ-version / extension-registration failure → apply the **AsciidoctorJ pin** contingency;
  - a JRuby-on-JDK-25 runtime failure → apply the **fork-JVM pin** contingency.
- **Content parity:** for all 3 services, `build/docs/asciidoc/api-guide.html` is generated and contains the endpoint sections with resolved snippets (the `operation::`/`{snippets}` macros expanded, not left literal). Spot-check against the Maven `target/generated-docs/api-guide.html`.
- **Reach:** the `asciidoctor` task registers on authentication/tasks/users and **not** on config/discovery/libs (`./gradlew :services:asapp-config-service:tasks` shows no `asciidoctor`).
- **Off the check path:** `./gradlew check --dry-run` (and `build --dry-run`) does **not** schedule `asciidoctor`.
- **Dependency wiring:** `./gradlew asciidoctor --dry-run` schedules `integrationTest` before `asciidoctor`.
- **Maven untouched:** no `pom.xml` and no test source edited, so `mvn asciidoctor:process-asciidoc@generate-docs` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

Javadoc/sources jars (line 21) · packaging + the temporary `ActuatorEndpointsIT` filter (line 22–27) · the `fullBuild` / `-Pfull` umbrella (line 28–29) · README / human-doc sync (line 39/48) · CI / git-hook swaps · CLAUDE.md command swap + final Maven removal · any change to `api-guide.adoc`, the snippets, or the `*ApiDocumentationIT` tests · any `pom.xml` edit · a dedicated Docker-free doc-snippet task (Option B, explicitly rejected in favor of Option A) · a dedicated `asapp.docs-conventions` plugin.

## 9. Contingencies

Resolve at implementation, mirroring how prior subtasks hedged their DSL/imports. The spike (§7) selects which apply.

- **AsciidoctorJ version pin (most likely).** If the spike fails to register `spring-restdocs-asciidoctor` against the plugin's default AsciidoctorJ 2.5.7, add `asciidoctorj = "3.0.0"` to the catalog (`# Build / ## Other`, alphabetical after `asciidoctor-gradle`) and `asciidoctorj { version = libs.findVersion("asciidoctorj").get().requiredVersion }` to the convention plugin — matching the Spring reference sample exactly. Document it in the gradle.md section.
- **JRuby-on-JDK-25 fork JVM.** AsciidoctorJ forks its own JVM for conversion (`executionMode = JAVA_EXEC`), by default the JVM that launched Gradle. There is no official JDK 25 statement for this plugin. If conversion fails on JDK 25, pin the `asciidoctor` task's fork to a JDK 21 launcher via the task's fork-options/toolchain block (Gradle can provision it), and document the divergence. Asciidoctor runs Ruby, not project bytecode, so it has no hard Java-25 requirement — hence no pin at baseline (unlike pitest, which must run on the Java 25 toolchain).
- **Custom-configuration dependency form.** If `asciidoctorExt(libs.findLibrary(...).get())` inside `dependencies {}` does not resolve in the convention plugin, fall back to `add("asciidoctorExt", libs.findLibrary("spring-restdocs-asciidoctor").get())`.
- **`AsciidoctorTask` import.** If `import org.asciidoctor.gradle.jvm.AsciidoctorTask` does not resolve on the convention-plugin classpath, address the task untyped (`tasks.named("asciidoctor") { … }`) and configure via the plugin's DSL, or fully-qualify the type.
- **Content drift vs Maven.** If the Gradle-produced `api-guide.html` differs from the Maven output beyond cosmetic asciidoctor-version differences (e.g. missing snippets), treat it as a blocker and reconcile before checking the box — the `configurations("asciidoctorExt")` activation and the `snippets` attribute pointing at `build/generated-snippets` are the usual causes.

## 10. References

- Spring REST Docs — [reference guide](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/) (`RestDocumentationExtension` auto-detects the build tool: Maven → `target/generated-snippets`, Gradle → `build/generated-snippets`; "Working with Asciidoctor" — `asciidoctorExt` configuration, `spring-restdocs-asciidoctor` sets the `snippets` attribute and adds the `operation` block macro, `asciidoctorj { version = "3.0.0" }`, `dependsOn test`/`inputs.dir snippetsDir`)
- Asciidoctor Gradle plugin — [plugin portal `org.asciidoctor.jvm.convert`](https://plugins.gradle.org/plugin/org.asciidoctor.jvm.convert) (`4.0.5` latest stable), [Gradle 9 compatibility (issue #770)](https://github.com/asciidoctor/asciidoctor-gradle-plugin/issues/770), and source at tag `release_4_0_5` (`AsciidoctorTask.groovy` / `OutputOptions.groovy` — default output `build/docs/asciidoc`, single-backend `html5` with no subfolder; `executionMode = JAVA_EXEC` fork; `module-versions.properties` default AsciidoctorJ `2.5.7`)
- Gradle — [Task configuration avoidance](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html) (`named()`/`register()` are the recommended lazy APIs; `getByName()`/`create()` are the discouraged eager ones) and [Sharing build logic in a convention plugin](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html) (versionless `id(...)` requires the plugin on the `build-logic` classpath)

## 11. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-9-api-doc`. A single commit is appropriate given the size:

1. `build(gradle): migrate API documentation generation to Gradle` — the catalog `asciidoctor-gradle` version + `asciidoctor-gradle-plugin` and `spring-restdocs-asciidoctor` libraries, the `build-logic` dependency, and the plugin application + `asciidoctorExt`/`asciidoctor` wiring in `asapp.domain-service-conventions` (plus any contingency from §9 that the spike triggers).

The `.claude/rules/gradle.md` API-documentation section and the `TODO.md` checkbox ride with that commit. Following this migration's established pattern (per the coverage, mutation, and formatting subtasks), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.
