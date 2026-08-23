# Gradle packaging (Spring Boot plugin) — design spec

**Date**: 2026-07-24
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate packaging to Gradle" (line 22), with attached Warning/Notes (lines 23–28).
**Scope**: Apply the Spring Boot Gradle plugin (`org.springframework.boot` 4.0.5) to the 5 services so each produces an executable `bootJar` at Maven parity, and reproduce everything the Maven `spring-boot-maven-plugin` (repackage + `build-info`) and `git-commit-id-maven-plugin` contributed: devtools kept out of the production jar, the jackson CVE override preserved under the plugin's auto-imported BOM, `build-info.properties` + `git.properties` generated for the actuator `/info` endpoint. Then delete the temporary `ActuatorEndpointsIT` `/info` filter (all 5 services). No `pom.xml` or application-source edits.

## 1. Context

Ten prior subtasks put dependency management, compilation, unit + integration testing, coverage, mutation testing, formatting, API docs, and javadoc/sources jars on Gradle 9.x / JDK 25. This subtask migrates **packaging** — turning each service into a runnable executable jar and wiring the build/git metadata the `/info` endpoint exposes. It is the first subtask to apply the `org.springframework.boot` Gradle plugin.

### What Maven does today

Two *distinct* Maven mechanisms contribute what people loosely call "Spring Boot packaging". Keeping them separate is the key to the Gradle mapping:

1. **`spring-boot-starter-parent` (the parent POM)** — inherited by **all 7 modules** through `asapp-parent → spring-boot-starter-parent`. It configures `maven-compiler-plugin` with `<parameters>true</parameters>` (verified in the 4.0.5 POM, §11) and supplies dependency management (the `spring-boot-dependencies` BOM) + plugin version defaults. This is why the **libs** get `-parameters` — from the *parent*, not from any build plugin.
2. **`spring-boot-maven-plugin` (the build plugin)** — declared only in the **5 services** (`services/*/pom.xml`). Its default `repackage` goal (bound by the parent) rewrites the module jar into an executable fat jar; a `build-info` execution in `services/pom.xml` `<pluginManagement>` writes `build-info.properties` with additional `encoding` + `java` properties. It also carries the image config (buildpacks/name/env) used by `spring-boot:build-image` — **out of scope here** (Docker subtask, line 34).

Separately, **`git-commit-id-maven-plugin`** (declared per-service, default config, bound to `prepare-package`) writes `git.properties`. And **`spring-boot-devtools`** is a `runtime`/`optional=true` dependency in each service — the `repackage` goal excludes it from the fat jar by default.

The actuator `/info` endpoint therefore surfaces `build` (from `build-info.properties`), `git` (from `git.properties`), and `java`/`os`/`process` (from env info contributors, no files needed). `ActuatorEndpointsIT.ReturnsStatusOkAndBodyContainsGitBuildJavaOsProcessDetails_OnInfoEndpoint` asserts all five keys — it exists in all 5 services.

### Current Gradle state

- `io.spring.dependency-management` is applied in `asapp.java-conventions` (all 7), which **manually imports** `spring-boot-dependencies` with a `bomProperty("jackson-bom.version", …)` override for a jackson CVE fix.
- `-parameters` + `UTF-8` + `release=25` are set on every `JavaCompile` in `asapp.java-conventions` (all 7) — the hand-written analog of Maven's parent-POM compiler config.
- devtools is declared `runtimeOnly("org.springframework.boot:spring-boot-devtools")` in `asapp.service-conventions` (all 5 services).
- The `org.springframework.boot` Gradle plugin is **not applied anywhere**; there is no `bootJar`, no `build-info.properties`, no `git.properties`. `assemble` currently produces plain library jars.
- A **temporary filter** in `asapp.service-conventions`' `integrationTest` task excludes the `/info` test in all 5 services (it can't pass without build-info + git.properties), keeping the integration tier and its coverage reports green until this subtask.

### The Maven → Gradle mapping (the crux)

Gradle has no "parent POM". Maven's two mechanisms split across **two Gradle plugins with different reach**:

| Maven source | Contributes | Gradle equivalent | Applied to |
|---|---|---|---|
| `spring-boot-starter-parent` (parent POM) | BOM / dependency management | `io.spring.dependency-management` | all 7 (`java-conventions`) |
| `spring-boot-starter-parent` (parent POM) | `-parameters`, `UTF-8` compiler defaults | **hand-written in `java-conventions`** | all 7 |
| `spring-boot-maven-plugin` (build plugin) | repackage, `build-info`, image | `org.springframework.boot` (Gradle plugin) | **services only** |

Because the `org.springframework.boot` Gradle plugin bundles both the repackage behavior *and* a `-parameters` compiler default, and because it is idiomatic to apply it only to application modules (applying it to a library creates a `bootJar` and expects a main class), it **cannot** be the `-parameters` source for the libs. `asapp-http-clients` proves the libs need it: `TasksHttpClient.getTasksByUserId(@PathVariable UUID id)` has an **unnamed** path variable, so its compiled bytecode must carry parameter names, or HTTP-interface path binding breaks in the consuming service. Therefore `java-conventions` remains the `-parameters` source for all 7 (see §4).

## 2. Goals

- **Executable jars at parity.** Each of the 5 services produces a runnable `build/libs/<name>-<version>.jar` via `bootJar`, on `assemble`/`build` (Maven's `repackage`). The plain library `jar` is disabled so each service emits **one** artifact, matching Maven's single repackaged jar.
- **devtools out of production.** devtools moves to the `developmentOnly` configuration — available for local dev runs, excluded from the fat jar (Maven's `excludeDevtools` default).
- **jackson CVE override preserved.** jackson resolves to `3.1.1` in the services despite the Spring Boot plugin auto-importing the BOM (see §4 — `bomProperty` is silently ignored under the plugin).
- **`/info` metadata.** `build-info.properties` (via `springBoot { buildInfo() }`, with `encoding` + `java` additional properties) and `git.properties` (via `gradle-git-properties`) are generated, on the classpath for tests, and packaged in the jar.
- **`/info` test green.** The temporary `ActuatorEndpointsIT` `/info` filter is removed from all 5 services; `./gradlew check` and `build` pass.
- **Correct altitude.** Spring Boot plugin + devtools + jackson-override + build-info + git-properties + jar-disable in `asapp.service-conventions` (all 5 services); the moved BOM import in `asapp.library-conventions` (2 libs); `-parameters` untouched in `asapp.java-conventions`. Nothing per-leaf.
- **Zero source / `pom.xml` edits.** Maven keeps working until the final removal subtask.

## 3. Non-goals

- **Docker image building** (`bootBuildImage` — buildpacks, image name, env, `createdDate`) — line 34. Applying the plugin *creates* the `bootBuildImage` task but this subtask leaves it unconfigured.
- **Running the app locally** (`bootRun`, the Maven `dev` profile config) — line 33. The plugin creates `bootRun`/`bootTestRun`; configuring them is the next subtask.
- **Publishing** — no `maven-publish`, no deploy. The TODO note "attach the javadoc/sources jars to the published artifact" is a **forward reference** kept under this subtask in `TODO.md`; it fires only when publishing lands, which is not in 0.5.0. Parity with Maven, which never `deploy`-ed.
- **Full-build umbrella** (`fullBuild` aggregating coverage/format/docs/jars) — lines 29–32.
- **CycloneDX SBOM.** Maven ran `cyclonedx-maven-plugin` (`prepare-package`); the actuator `/sbom` endpoint reads it. The `/sbom` test is **not** filtered today and currently passes on Gradle, so SBOM generation is **not** a packaging blocker and is out of scope here (raise separately if the `/sbom` test proves to depend on it — see §9).
- **CLAUDE.md build-command swap, README/human-doc sync, CI/git-hook swaps, final Maven removal** — each its own later subtask.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Where to apply the Spring Boot plugin | **`asapp.service-conventions` (all 5 services), versionless** | All 5 services are `@SpringBootApplication` executables needing a fat jar. Libs are not boot apps — applying the plugin to them would create a `bootJar` expecting a main class. Put the plugin on the `build-logic` classpath (catalog library + `implementation`), applied as `id("org.springframework.boot")` — the established pattern for asciidoctor/pitest/spotless. |
| Plain `jar` task | **Disable in `service-conventions`** (`tasks.named<Jar>("jar") { enabled = false }`) | With the plugin applied, Gradle builds both `bootJar` (`<name>.jar`) and a plain `<name>-plain.jar`. Maven's `repackage` yielded one executable jar. No module consumes a service as a library, so the plain jar is dead weight — disabling it restores Maven's single-artifact output. (Do **not** disable when building native images — N/A here.) |
| devtools placement | **`developmentOnly(...)`** (was `runtimeOnly`) | The plugin excludes `developmentOnly` deps from the executable jar (Maven's `excludeDevtools` default), while keeping devtools available for `bootRun`. This resolves the TODO **Warning** (devtools must not ship in the production jar). |
| jackson CVE override under the plugin | **Move the manual BOM import out of `java-conventions` → `library-conventions`; services use the plugin's auto-import + `extra["jackson-bom.version"]`** | The plugin auto-imports `spring-boot-dependencies` whenever `io.spring.dependency-management` is present (§11). Once it does, `bomProperty` overrides are **silently ignored** (dependency-management-plugin **#219**), so the jackson `3.1.1` fix would be lost — and importing the same BOM twice is itself fragile (**#259**). Documented fix: override the BOM version *property* via a Gradle extra property (`extra["jackson-bom.version"] = …`), which the auto-imported BOM honors. Libs have **no** Boot plugin, so their manual import + `bomProperty` still works — keep it, relocated to `library-conventions`. Result: exactly one BOM import per module type; CVE fix preserved everywhere. This resolves the TODO **Note** on BOM duplication/conflict. |
| `-parameters` | **No change — stays in `java-conventions` (all 7)** | It is genuinely an all-7 concern (services: Spring name-binding; libs: `asapp-http-clients` unnamed `@PathVariable`). `java-conventions` is the Gradle analog of Maven's parent-POM `<parameters>true</parameters>` (§1, §11), which fed all 7. The plugin *also* adds `-parameters` to the 5 services — a harmless idempotent duplicate. The TODO **Note** ("drop it, the plugin now auto-adds it") assumed the plugin covers every module; it covers only services, so the note is **rewritten**, not acted on. Any duplicate-arg lint is deferred to the "Review IntelliJ warnings" cleanup (line 47). |
| build-info | **`springBoot { buildInfo() }` in `service-conventions`, with `additional` = `encoding`, `java`** | Configures the `bootBuildInfo` task → `build/resources/main/META-INF/build-info.properties`, and makes `classes` depend on it. `additional` replicates Maven's `<additionalProperties>` (`encoding=UTF-8`, `java=25`). Reproduces the `build-info` execution in `services/pom.xml`. |
| git.properties | **`com.gorylenko.gradle-git-properties` v4.0.1 in `service-conventions`** | No Gradle built-in exists (TODO **Note**); this is the de-facto standard, supports Gradle 9 + the configuration cache, and writes `build/resources/main/git.properties`, auto-consumed by the actuator `/info` `GitInfoContributor`. Reproduces `git-commit-id-maven-plugin`. On the `build-logic` classpath like the other third-party plugins; resolved from `gradlePluginPortal()` (not published to Maven Central). |
| Temporary `/info` filter | **Delete from `service-conventions`** | With build-info + git.properties now present, the `/info` test passes. Removing the `filter { excludeTestsMatching(…) }` block restores the full integration tier + coverage across all 5 services. Resolves the TODO subtask on line 28. |
| Applied to all 5 services (incl. config/discovery) | **Yes** | All 5 are Spring Boot apps with an `/info` endpoint and an `ActuatorEndpointsIT`. `service-conventions` is their common plugin, so bootJar/build-info/git.properties/devtools all land there uniformly. |

## 5. Changes by file

### `gradle/libs.versions.toml`
- **`[versions]` → `# Build` → `## Other`**: add `gradle-git-properties = "4.0.1"` (alphabetical before `spotless`). No new version for Spring Boot — reuse the existing `# BOM ## Spring Boot` `spring-boot = "4.0.5"`.
- **`[libraries]` → `# Build`**: add a `## Spring Boot` origin group (ordered before `## Spring`) with
  `spring-boot-gradle-plugin = { module = "org.springframework.boot:spring-boot-gradle-plugin", version.ref = "spring-boot" }`,
  and under `## Other` add
  `gradle-git-properties-plugin = { module = "com.gorylenko.gradle-git-properties:gradle-git-properties", version.ref = "gradle-git-properties" }` (alphabetical: `gradle-git-properties-plugin`, `gradle-pitest-plugin`, `spotless-plugin`).

### `build-logic/build.gradle.kts`
- Under `dependencies { // Build }`, add `// Spring Boot` → `implementation(libs.spring.boot.gradle.plugin)` and, under `// Other`, `implementation(libs.gradle.git.properties.plugin)`. (Final block ordering per gradle.md is a cleanup-subtask concern; place sensibly.)

### `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`
- **Remove** the `dependencyManagement { imports { mavenBom("…spring-boot-dependencies…") { bomProperty("jackson-bom.version", …) } } }` block (moves to `library-conventions`).
- **Keep** the `io.spring.dependency-management` plugin, the `-parameters`/encoding/release `JavaCompile` config, jacoco, spotless, and the `val libs` accessor (still used for `jacoco` toolVersion).

### `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`
- Add the `val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")` accessor and the relocated
  `dependencyManagement { imports { mavenBom("org.springframework.boot:spring-boot-dependencies:${…spring-boot…}") { bomProperty("jackson-bom.version", …jackson-bom…) } } }`.
  Libs have no Boot plugin, so `bomProperty` works here. (Javadoc/sources block from the prior subtask stays.)

### `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`
- **plugins {}**: add `id("org.springframework.boot")`.
- **jackson override**: `extra["jackson-bom.version"] = libs.findVersion("jackson-bom").get().requiredVersion` (drives the plugin's auto-imported BOM).
- **Disable plain jar**: `tasks.named<Jar>("jar") { enabled = false }`.
- **devtools**: change `runtimeOnly("org.springframework.boot:spring-boot-devtools")` → `developmentOnly("org.springframework.boot:spring-boot-devtools")`.
- **build-info**:
  ```kotlin
  springBoot {
      buildInfo {
          properties {
              additional.set(mapOf(
                  "encoding" to "UTF-8",
                  "java" to java.toolchain.languageVersion.get().toString(),
              ))
          }
      }
  }
  ```
- **git.properties**: add `id("com.gorylenko.gradle-git-properties")` to plugins {} (default config is sufficient).
- **Remove** the `filter { excludeTestsMatching("*ActuatorEndpointsIT.…OnInfoEndpoint") }` block (and its `TEMPORARY` comment) from the `integrationTest` task registration. The Spring Cloud BOM import stays.

### `.claude/rules/gradle.md`
- Add a **Packaging** section: apply `org.springframework.boot` (build-logic classpath, versionless) in `asapp.service-conventions` (5 services, not libs); disable the plain `jar`; devtools on `developmentOnly` (out of the fat jar); the two-plugin Maven→Gradle split and **why `-parameters` stays in `java-conventions`** (parent-POM analog, all-7 concern; plugin's add to services is redundant-harmless); the jackson override **must** use `extra["jackson-bom.version"]` (not `bomProperty`, #219) with the manual BOM import relocated to `library-conventions`; `springBoot { buildInfo() }` (+`encoding`/`java`); `gradle-git-properties` for git.properties; both metadata files land in `build/resources/main` and are packaged.

### `TODO.md`
- Check off "Migrate packaging to Gradle" (line 22) and its `/info`-filter subtask (line 28).
- **Rewrite** the resolved Notes: the `-parameters` note (explain it's kept, not dropped, and why); the BOM note (resolved via `extra[...]` + relocation); the devtools warning (resolved via `developmentOnly`).
- **Keep** the "attach javadoc/sources jars to the published artifact" note as a forward reference (publishing not in 0.5.0).

## 6. Placement / altitude rationale

- **Spring Boot plugin, jar-disable, devtools, jackson-`extra`, buildInfo, git-properties → `service-conventions`.** All are service-only (the 5 boot apps). This is the plugin whose modules Maven declared `spring-boot-maven-plugin` + `git-commit-id-maven-plugin` in.
- **BOM import → `library-conventions`.** It must leave the services' path (to avoid the double-import + `bomProperty`-ignored trap) but the libs still need it and can still use `bomProperty` (no Boot plugin). `library-conventions` is exactly the libs-only plugin.
- **`-parameters` → stays in `java-conventions`.** All-7 concern; the parent-POM analog. Same altitude logic the javadoc/sources spec used to *reject* `java-conventions` for a 5-module concern — inverted here because `-parameters` genuinely is all-7.
- **New third-party plugins on the `build-logic` classpath.** Consistent with asciidoctor/pitest/spotless: catalog version + library, `implementation(...)` in `build-logic/build.gradle.kts`, applied versionless.

## 7. Verification / Definition of Done

Spike order — highest-risk first:

1. **build-logic compiles + services configure.** After the plugin + accessor changes, `./gradlew :services:asapp-authentication-service:tasks` resolves and shows `bootJar`, `bootBuildInfo`, `generateGitProperties`.
2. **jackson override survives the auto-import (top risk).** `./gradlew :services:asapp-authentication-service:dependencyInsight --dependency jackson-databind` (or `--configuration runtimeClasspath`) shows the jackson version consistent with `jackson-bom 3.1.1`. If not, apply the §9 contingency before proceeding.
3. **Executable jar.** `./gradlew :services:asapp-authentication-service:bootJar` produces `build/libs/asapp-authentication-service-<version>.jar` and **no** `-plain.jar`. Spot-check it is a Spring Boot executable (contains `BOOT-INF/`, `org/springframework/boot/loader`).
4. **devtools excluded.** The bootJar does **not** contain `spring-boot-devtools` under `BOOT-INF/lib/`.
5. **Metadata present + packaged.** `build/resources/main/META-INF/build-info.properties` (keys incl. `build.encoding`, `build.java`) and `build/resources/main/git.properties` exist after `classes`; both are inside the bootJar.
6. **`/info` test green.** `./gradlew :services:asapp-authentication-service:integrationTest` runs `ActuatorEndpointsIT` including the previously-filtered `…OnInfoEndpoint` test, and it passes (response has `git`, `build`, `java`, `os`, `process`).
7. **Full check green, all 5 services.** `./gradlew check` and `./gradlew build` pass across every module; coverage reports for the integration tier regenerate without the filtered method. Confirm config + discovery services (thin wrappers) also pass their `/info` tests.
8. **Libs unaffected.** `:libs:asapp-http-clients:build` still produces a plain jar compiled with `-parameters` (verify a `@PathVariable`-bearing interface still binds — the http-client E2E/IT path in a consuming service is the real check), and jackson still resolves to `3.1.1` in libs.
9. **Reach.** `./gradlew :libs:asapp-commons-url:tasks` shows **no** `bootJar` (plugin not applied to libs).
10. **Maven untouched.** No `pom.xml` / source edited; per the standing migration constraint, Maven is **not** re-run to re-verify.

## 8. Out of scope / YAGNI

`bootBuildImage`/Docker config (line 34) · `bootRun`/local-run config (line 33) · `maven-publish`/deploy + attaching javadoc-sources (waits for publishing) · CycloneDX SBOM (not a packaging blocker; §3) · the `fullBuild` umbrella (lines 29–32) · CLAUDE.md command swap · README/human-doc sync · CI/git-hook swaps · final Maven removal · any `pom.xml` or application-source edit.

## 9. Contingencies

Resolve at implementation, mirroring prior subtasks. The spike (§7) selects which apply.

- **`extra["jackson-bom.version"]` doesn't override in a precompiled convention plugin.** If §7.2 shows jackson not at `3.1.1`: (a) set it as a real Gradle project property instead (`the<ExtraPropertiesExtension>()["jackson-bom.version"] = …`), or (b) set it in each service's `build.gradle.kts` (5 lines, still catalog-driven), or (c) as a last resort add an explicit dependency constraint pinning the jackson-bom / jackson artifacts. Do **not** revert to `bomProperty` on a manual import — #219 makes it a silent no-op under the plugin.
- **`developmentOnly` / `springBoot` / `jar` accessors don't resolve in the precompiled plugin.** They are generated from the plugins applied in `service-conventions`' own `plugins {}` block, so they should exist; if not, fall back to `configurations.named("developmentOnly")` / `add("developmentOnly", …)`, `extensions.configure<SpringBootExtension> { … }`, and `tasks.named<Jar>("jar")` with an explicit import.
- **`java.toolchain.languageVersion.get()` unsafe at config time.** If `.get()` complains, hardcode `"25"` for the `java` build-info property (matches Maven's literal `${java.version}` = 25), or read it from the catalog if a java-version entry is later added.
- **devtools on the test classpath.** `developmentOnly` removes devtools from the test/`integrationTest` runtime classpath (Maven's `runtime` scope kept it on). devtools is inert in tests (Spring disables restart under test contexts), so this is expected. If any test regresses because devtools is absent, switch to `testAndDevelopmentOnly` (keeps it in tests + `bootRun`, still out of the fat jar).
- **build-info / git.properties not on the `integrationTest` classpath.** Both hook into `classes`/`processResources`, which `integrationTest` depends on transitively, so they should be generated first. If the `/info` test still 404s the files, add an explicit `integrationTest.dependsOn("bootBuildInfo", "generateGitProperties")`.
- **`/sbom` test regresses.** If removing the `/info` filter surfaces that `/sbom` also depended on a Maven-only artifact (CycloneDX), that is a *separate* discovery — do not expand scope; log it as its own TODO entry and, if needed, re-add a narrow filter for the `/sbom` method only (the `/info` method is the one this subtask must un-filter).

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-11-package`. Two commits keep the mechanical relocation separate from the feature:

1. `build(gradle): apply Spring Boot plugin and migrate packaging to Gradle` — the plugin application + jar-disable + devtools move + jackson `extra` override + BOM relocation + build-info + git.properties (catalog, build-logic classpath, `java`/`library`/`service` conventions), plus the `.claude/rules/gradle.md` Packaging section.
2. `test(gradle): re-enable the ActuatorEndpointsIT /info test on the integration tier` — remove the temporary filter (all 5 services) and check off `TODO.md`.

(A single squashed commit is acceptable if the developer prefers.) Per this migration's established pattern (coverage, mutation, formatting, API-doc, javadoc/sources subtasks), implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. References

- Spring Boot 4.0 Gradle plugin — [Packaging Executables](https://docs.spring.io/spring-boot/4.0/gradle-plugin/packaging.html): `bootJar` on `assemble`; the plain `jar` gets the `plain` classifier by default (disable via `tasks.named("jar") { enabled = false }`); "all dependencies declared in the `developmentOnly` configuration will be excluded from an executable jar or war".
- Spring Boot 4.0 Gradle plugin — [Reacting to Other Plugins](https://docs.spring.io/spring-boot/4.0/gradle-plugin/reacting.html): applying the `java` plugin makes it "configure any `JavaCompile` tasks to use the `-parameters` compiler argument" and set `UTF-8`; creates `developmentOnly`, `testAndDevelopmentOnly`, `productionRuntimeClasspath`; and "when the `io.spring.dependency-management` plugin is applied … the Spring Boot plugin will automatically import the `spring-boot-dependencies` bom".
- Spring Boot 4.0 Gradle plugin — [Managing Dependencies](https://docs.spring.io/spring-boot/4.0/gradle-plugin/managing-dependencies.html): override a managed version by setting the corresponding property, e.g. Kotlin `extra["slf4j.version"] = "1.7.20"` — the mechanism used here for `jackson-bom.version`.
- Spring Boot 4.0 Gradle plugin — [Integrating with Actuator](https://docs.spring.io/spring-boot/4.0/gradle-plugin/integrating-with-actuator.html): `springBoot { buildInfo { properties { additional.set(...) } } }` → `bootBuildInfo` → `META-INF/build-info.properties` in `build/resources/main`; `classes` depends on it.
- dependency-management-plugin — [issue #219](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/219): `bomProperty` overrides are ignored once `org.springframework.boot` is applied; `ext['…version']` is the working alternative.
- dependency-management-plugin — [issue #259](https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/259): last-wins ordering is not honoured when the same BOM is imported multiple times — the reason to avoid a manual + auto double-import.
- gradle-git-properties — [plugin portal](https://plugins.gradle.org/plugin/com.gorylenko.gradle-git-properties) / [repo](https://github.com/n0mer/gradle-git-properties): v4.0.1, Gradle 5.1–9.x + configuration-cache support; writes `build/resources/main/git.properties`; impl module `com.gorylenko.gradle-git-properties:gradle-git-properties` (Gradle Plugin Portal).
- `spring-boot-starter-parent:4.0.5` POM — `maven-compiler-plugin` `<configuration><parameters>true</parameters></configuration>` (evidence that Maven fed `-parameters` to all modules via the parent, not the build plugin).

## 12. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/v0.5.0/2026-07-24-gradle-packaging.md`) were written before implementation. The core change shipped substantially as designed — the `org.springframework.boot` and `com.gorylenko.gradle-git-properties` plugins applied to the 5 services in `asapp.service-conventions`, the plain library jar disabled, devtools moved to `developmentOnly`, the jackson CVE override expressed as `extra["jackson-bom.version"]` with the manual `spring-boot-dependencies` BOM import relocated to `asapp.library-conventions`, `build-info.properties` + `git.properties` generated for `/info`, and the temporary `ActuatorEndpointsIT` `/info` filter removed across all 5 services.

The canonical implementation is the current state of the real artifacts on this branch, not this document. Consult the convention plugins (`asapp.service-conventions.gradle.kts`, `asapp.library-conventions.gradle.kts`, `asapp.java-conventions.gradle.kts`), the version catalog (`gradle/libs.versions.toml`), `build-logic/build.gradle.kts`, and `.claude/rules/gradle.md`.

Notable deltas:

- **Runtime-classpath normalization for build-cache stability (adds to the §5 `service-conventions` changes; revises the §7.5/§7.6 assumption that default git-properties config needed no cache treatment).** `asapp.service-conventions.gradle.kts` gained a `normalization { runtimeClasspath { … } }` block that ignores `build.time` in `META-INF/build-info.properties` and `git.build.host` / `git.build.user.name` / `git.build.user.email` in `git.properties`. `bootBuildInfo` restamps `build.time` and `gradle-git-properties` writes machine-specific host/user keys on every build; because both files ride the test runtime classpath, their volatile content made the `integrationTest`/`test` tasks never UP-TO-DATE. The block adjusts only the `@Classpath` up-to-date fingerprint — real values still reach `/info`; it deliberately does not stabilize `processResources`/`bootJar`, which still repack.

- **`buildInfo` java version wired lazily rather than eagerly (revises the §5 build-info snippet).** §5 showed `additional.set(mapOf("encoding" to "UTF-8", "java" to java.toolchain.languageVersion.get().toString()))`; the shipped `asapp.service-conventions.gradle.kts` uses per-key lazy puts — `additional.put("encoding", "UTF-8")` and `additional.put("java", java.toolchain.languageVersion.map { it.toString() })` — removing the config-time `.get()` on the toolchain provider to match the lazy `.map { }` pattern used for the pitest `jvmPath` in `asapp.domain-service-conventions`.

- **Jackson CVE override cross-reference centralized in `.claude/rules/gradle.md` (refines the §4 jackson decision and its §5 gradle.md bullet).** The override split shipped exactly as designed — `extra["jackson-bom.version"]` in `asapp.service-conventions.gradle.kts` and `mavenBom(...) { bomProperty("jackson-bom.version", …) }` in `asapp.library-conventions.gradle.kts`, both single-sourced from the `libs.findVersion("jackson-bom")` catalog entry — but the canonical cross-reference between the two sites and the repayment trigger (collapse both archetypes onto one mechanism once the dependency-management-plugin bug is fixed or a second BOM-property override appears) live in the `gradle.md` Packaging "jackson CVE override" bullet rather than being duplicated as prose in the two `.kts` sites, which carry only terse local comments.

- **`gradle.md` issue citations normalized to `owner/repo#issue` (refines the §5 gradle.md bullet and §11 shorthand).** The Packaging section now cites `spring-gradle-plugins/dependency-management-plugin#219` on first mention, bare `#259`/`#219` for same-repo repeats, and `spring-projects/spring-boot#28562`, matching the file's existing `szpak/gradle-pitest-plugin#301` convention.

- **`gradle.md` corrects the packaged location of `build-info.properties` (revises the §5 gradle.md bullet and the §7.5 "both inside the bootJar" phrasing).** Verification found the runtime reality: in the bootJar, `git.properties` sits at `BOOT-INF/classes/git.properties` while `build-info.properties` is hoisted to the jar-root `META-INF/` by `bootJar`'s `moveMetaInfToRoot` (Maven-repackage parity, `spring-projects/spring-boot#28562`); both remain on the runtime classpath via `LaunchedClassLoader` parent-first delegation. Documentation-only correction — no functional change.

For future packaging or convention-plugin edits, treat these convention plugins, the version catalog, and `.claude/rules/gradle.md` as the template; this spec is preserved as a record of the original design intent.
