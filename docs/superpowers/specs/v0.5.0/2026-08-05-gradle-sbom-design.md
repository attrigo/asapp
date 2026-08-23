# Restore the software bill of materials in the packaged services — design spec

**Date**: 2026-08-05
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Restore the software bill of materials in the packaged services" (line 30).
**Scope**: Apply the CycloneDX Gradle plugin in `asapp.service-conventions` so each of the five services embeds `META-INF/sbom/application.cdx.json` again and the actuator `/sbom` endpoint reports the `application` id. Three build files changed (`gradle/libs.versions.toml`, `build-logic/build.gradle.kts`, `asapp.service-conventions.gradle.kts`), five test files tightened, plus `.claude/rules/gradle.md` and `TODO.md`. No new task type, no new test class, no README edit, no `pom.xml` edit, no change to actuator exposure.

## 1. Context

Maven produced an SBOM on every build. `spring-boot-starter-parent` declares `cyclonedx-maven-plugin` in `pluginManagement` with the goal `makeAggregateBom`, `projectType=application`, `outputFormat=json`, `outputName=application.cdx` and `outputDirectory=${project.build.outputDirectory}/META-INF/sbom`. `services/pom.xml:297-306` re-binds that same execution (`<id>default</id>`, which is the id an execution without one receives) from `generate-resources` to `prepare-package`, and all five service poms declare the plugin in `<build><plugins>`. The file therefore landed in `target/classes/META-INF/sbom/application.cdx.json` before Failsafe ran, and `spring-boot-maven-plugin`'s `repackage` carried it into the jar.

The Gradle build produces none. `SbomEndpoint` auto-detects three locations and finds nothing, so `/sbom` answers `{"ids":[]}` — an endpoint that is exposed under the locked-down base profile (`central-config/application.properties:24` lists `health,info,prometheus,sbom`) and returns an empty list. The five `ActuatorEndpointsIT` classes assert only that `ids` is an array, so nothing is red today.

**What Spring Boot 4.0.5 does for us.** `CyclonedxPluginAction` is a `PluginApplicationAction` keyed on `org.cyclonedx.gradle.CyclonedxPlugin`. When that plugin is applied to a project, Boot:

- looks up `tasks.named("cyclonedxBom", CyclonedxAggregateTask.class)` — **in the same project**, which is why the plugin has to be applied per Boot application and not at the root;
- configures that task: `projectType` → `APPLICATION`, `jsonOutput` → `build/reports/cyclonedx/application.cdx.json`, `xmlOutput.unsetConvention()`, `includeLicenseText` → `false`;
- makes `processResources` depend on it and copy the JSON into `META-INF/sbom`;
- adds `Sbom-Format: CycloneDX` and `Sbom-Location: META-INF/sbom/application.cdx.json` to the `bootJar` manifest.

Boot's own `CyclonedxPluginActionIntegrationTests` asserts exactly that manifest pair and that the entry exists in the archive, so this is contract, not incidental behaviour.

**What the plugin registers.** Version 3.x registers **two** tasks. `CyclonedxPlugin.apply` walks the project and its subprojects, giving each a `cyclonedxDirectBom` task (`CyclonedxDirectTask`) plus an outgoing `cyclonedxDirectBom` configuration, then registers one `cyclonedxBom` aggregate task (`CyclonedxAggregateTask`) on the project it was applied to, wired to consume the direct BOMs. At a leaf project the aggregate wraps a single direct BOM. The consequence that shapes this design: the scope knobs (`includeConfigs`, `skipConfigs`, `includeMetadataResolution`, `includeBuildEnvironment`) are declared on `CyclonedxDirectTask`, while the metadata knobs on `BaseCyclonedxTask` (`projectType`, `schemaVersion`, `includeBomSerialNumber`, `includeBuildSystem`, `includeLicenseText`, `jsonOutput`, `xmlOutput`) exist on both — and only the aggregate's metadata reaches the shipped file, because `CyclonedxAggregateTask.mergeAll` builds its own root Bom and merges only the `components` and `dependencies` of its inputs.

**Two defaults that are not parity.** Read from the 3.3.0 sources:

| Default | Effect | Maven's behaviour |
|---|---|---|
| `includeConfigs = []` | `SbomGraphProvider.shouldIncludeConfiguration` treats an empty list as "include everything", so **every resolvable configuration** is traversed — the test, mutation-testing, documentation and Liquibase tooling classpaths included | `includeTestScope` defaults to `false`; compile, provided, runtime and system scopes only |
| `includeBuildSystem = true` | `SbomBuilder.addBuildSystemMetaData` adds a `build-system` external reference from `EnvironmentUtils.getBuildURI()`, which composes `${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}` when those are set — so nothing locally, a per-run URL in CI | The Maven plugin derives external references from the POM model only, and no pom here declares `<scm>`, `<url>`, `<ciManagement>`, `<issueManagement>` or `<distributionManagement>` — verified, zero matches |

Both are corrected here. The name matching is `Configuration.getName()::matches`, i.e. `String.matches`, a full-string regex match — so `"runtimeClasspath"` selects `runtimeClasspath` and not `testRuntimeClasspath`.

**One divergence that cannot be corrected.** `SbomBuilder.buildFinilizedRootComponent` calls `ExternalReferencesUtil.complementByEnvironment` unconditionally, which adds a VCS external reference read from the git remote (`GitUtils.getGitUrlFromEnvironmentVariable()`, falling back to `getGitUrlFromGitRepo()`). There is no flag; the only suppression is supplying a VCS reference of our own, which is worse than the thing it suppresses. So `https://github.com/attrigo/asapp` appears in the SBOM where Maven emitted no external references at all. Harmless — the repository and the ghcr images are both public — but it is new content, and because `addExternalReference` accepts only `https://` and `ssh://` URLs, an scp-style remote (`git@github.com:…`) is silently dropped, so the field can differ between two clones of the same commit.

**Two fields change on every execution, and neither can be pinned.** `SbomBuilder.buildBom` writes `bom.serialNumber = "urn:uuid:" + UUID.randomUUID()` when `includeBomSerialNumber` is true (its default, and the Maven plugin's), and cyclonedx-core-java's `Metadata` initialises `private Date timestamp = new Date()` at construction, with no `setTimestamp` call anywhere in the plugin and no `outputTimestamp` analog to the Maven plugin's. A deterministic SBOM is therefore unreachable, which is what makes §4's normalization mandatory rather than a nicety.

## 2. Goals

- **The actuator endpoint tells the truth again.** `/sbom` reports the `application` id on all five services, under Gradle and under Maven, driven by a test that is red before the change.
- **The SBOM describes what ships**, not what built it: the runtime dependency graph, with no test or build-tool components.
- **The integration tier stays build-cache-reusable** across a `clean` and across machines, despite a file whose serial number and timestamp are fresh on every execution.
- **Maven parity in content and in when it runs.** On the `build` path, as `prepare-package` was; same project type; same output name; test scope excluded; no CI provenance.
- **The five services get it and the two libs do not** — the flag-free analog of the libs never declaring the Maven plugin.
- **No hand-maintained duplication.** One catalog pin, one `build-logic` classpath entry, one plugin id, configuration only where the plugin's defaults are wrong.

## 3. Non-goals

- **An XML SBOM.** Boot unsets the aggregate's `xmlOutput` and this design unsets the direct task's too. One format, one file.
- **A repo-wide or library SBOM.** Coverage stays per-service, matching Maven. The two libs and the intermediate `libs`/`services` projects get no CycloneDX task at all.
- **Publishing the SBOM as a separate build artifact**, or attaching it to anything. It rides inside the jar, as it did under Maven.
- **Image-level SBOMs.** Paketo's own buildpack SBOM is a separate mechanism, untouched, and `bootBuildImage` needs no change — the SBOM arrives inside the `bootJar` it consumes.
- **A deterministic SBOM.** Unreachable (§1); normalization handles the consequence instead.
- **CI provenance in the SBOM.** Deliberately switched off, and forwarded to the Backlog rather than silently dropped (§5).
- **Actuator exposure changes.** `sbom` is already in every base exposure list; the `DevToolingLockdownIT` and `SecurityConfigurationIT` assertions cover links, not content, and are untouched.
- **A `fullBuild` edge.** The SBOM arrives via `processResources` → `classes` → `build`; adding an umbrella edge would be a duplicate Gradle would dedupe anyway.
- **New build-logic tests.** No custom task type is added. Configuration-level tests for convention wiring are an existing Backlog item.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Altitude | **`asapp.service-conventions`** (the 5 services) | Exactly Maven's altitude: one `pluginManagement` entry in `services/pom.xml` plus five per-service declarations. Boot's action needs `cyclonedxBom` in the same project as `bootJar`, so per-service is also the only placement that works. |
| Rejected: apply at the root | **Never** | Boot looks the aggregate task up in its own project, so no service jar would get an SBOM; and a root aggregate describes the repository, not a running service. |
| Rejected: generate the file outside Gradle (custom task, `syft`) and point `management.endpoint.sbom.application.location` at it | **Never** | Reimplements a maintained plugin, forfeits the manifest attributes and Boot's `processResources` wiring, and puts a build output outside up-to-date tracking. Its one selling point — determinism — it does not deliver either, since we would then own schema generation. |
| Version | **`3.3.0`**, catalog-pinned under `# Build` → `## Org` | Boot 4.0.5 compiles against 3.0.1, whose `CyclonedxDirectTask` declares no dependency input — only scalar `@Input`s and an `@OutputFile` — so it reports `UP-TO-DATE` after its first run and **ships a stale SBOM** after a dependency bump. 3.3.0 added `@InputFiles @PathSensitive(RELATIVE) getResolvedDependencies()` for exactly this. Every API Boot's action touches is unchanged in 3.3.0. |
| Why catalog-pinned rather than versionless | **The Boot BOM does not manage it** | `spring-boot-dependencies:4.0.5` manages `cyclonedx-maven-plugin` (2.9.1) only — verified against the published POM. Per `## Shared Build Configuration`, what the BOM does not manage stays catalog-pinned. |
| Repository | **`gradlePluginPortal()`**, already declared in `build-logic/settings.gradle.kts` | `org.cyclonedx:cyclonedx-gradle-plugin` stops at **1.4.0** on Maven Central (2021); 3.x publishes to the Plugin Portal only. Boot's own build adds `gradlePluginPortal { content { includeGroup("org.cyclonedx") } }` for the same reason. Same shape as `liquibase-gradle` 3.1.0. |
| SBOM scope | **`includeConfigs = listOf("runtimeClasspath")`** on `cyclonedxDirectBom` | Closest to Maven's compile+runtime set. The Boot plugin makes `runtimeClasspath` extend `developmentOnly`, so devtools is listed — as it was under Maven. No service declares a `compileOnly` dependency, so nothing that ships is missed. Developer decision, taken over the jar-exact `productionRuntimeClasspath` and over the plugin default. |
| Rejected: `skipConfigs` | **No** | An allowlist states the intent; a denylist has to be extended every time the build gains a resolvable configuration. |
| CI provenance | **`includeBuildSystem = false`** on `cyclonedxBom` | Maven emitted no build-system reference, and switching it off keeps the file identical on a laptop and in CI, removing one more per-run-volatile field on the same reasoning as the unset `createdDate`. Developer decision; forwarded to the Backlog for a later revisit. |
| Direct task XML | **`xmlOutput.unsetConvention()`** | The direct task's outgoing artifacts are `getOutputFiles()` = JSON + XML, and `mergeAll` parses **every** input file. Left at the default, each build writes an XML twin and the aggregate parses it for nothing. Mirrors what Boot does to the aggregate. |
| Licence resolution | **`includeMetadataResolution` left at its default `true`** | It is what resolves each component's licence, which Maven's SBOM also carried. It is also the expensive part; §8 makes disabling it the measured fallback, not the default. |
| `includeBomSerialNumber` | **Left at its default `true`** | The Maven plugin's default too. Its volatility is absorbed by normalization, and removing the field would be a divergence bought for nothing. |
| `schemaVersion` | **Left at its default** (`VERSION_16`) | The Maven plugin 2.9.1 also defaults to CycloneDX 1.6. Writing it down would imply a choice was made. |
| Build-cache safety | **`ignore("META-INF/sbom/application.cdx.json")`** in the existing `normalization { runtimeClasspath { } }` block | Whole-file, not key-level: there is no JSON normalizer, and the `properties(...)` form cannot ignore a JSON key. Safe on the same argument `git.properties` already rests on — no test reads the content; the tightened assertion turns on the file *existing*, and normalization touches only the fingerprint. |
| Where it runs | **On the `build` path, via Boot's `processResources` edge** | Maven's `prepare-package` binding ran on every build, and the endpoint has to work in the `integrationTest` tier and under `bootRun`. Nothing to wire and nothing to gate. |
| Rejected: release-path only, like `asciidoctor` | **No** | The asciidoctor divergence works because `api-guide.html` is consumed by humans after a release. An SBOM absent from a dev build makes `/sbom` lie locally and makes the new assertion pass only under `fullBuild`. It is also not cleanly reachable: disabling the task breaks the `processResources` input Boot wires. |
| Documentation shape | **A new `## SBOM` section in `.claude/rules/gradle.md`, after `## Packaging`** | The file carries one section per migration subtask; the SBOM is its own concern with its own defaults to warn about, and folding six bullets into `## Packaging` would bury them. |

## 5. Changes by file

**`gradle/libs.versions.toml`** — one version, one library.

`[versions]` → `# Build` → `## Org`, alphabetically between `asciidoctorj` and `jacoco`:

```toml
cyclonedx-gradle = "3.3.0"
```

`[libraries]` → `# Build` → `## Org`, alphabetically between `asciidoctor-gradle-plugin` and `liquibase-gradle-plugin`:

```toml
cyclonedx-gradle-plugin = { module = "org.cyclonedx:cyclonedx-gradle-plugin", version.ref = "cyclonedx-gradle" }
```

**`build-logic/build.gradle.kts`** — one line in `// Build` → `// Org`, second in that group:

```kotlin
implementation(libs.asciidoctor.gradle.plugin)
implementation(libs.cyclonedx.gradle.plugin)
implementation("org.liquibase:liquibase-core")
implementation(libs.liquibase.gradle.plugin)
```

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** — four edits.

Two imports, ahead of the existing `org.gradle.*` ones:

```kotlin
import org.cyclonedx.gradle.CyclonedxAggregateTask
import org.cyclonedx.gradle.CyclonedxDirectTask
```

The plugin id, added to the existing `plugins { }` block:

```kotlin
plugins {
    id("asapp.java-conventions")
    id("org.springframework.boot")
    id("com.gorylenko.gradle-git-properties")
    id("org.cyclonedx.bom")
}
```

The two task blocks, placed after `gitProperties { }` and before `normalization { }` — with `build-info.properties` and `git.properties`, this is the third piece of packaged metadata, and the normalization block that follows then reads as the consequence of all three:

```kotlin
// The SBOM embedded at META-INF/sbom (surfaced by the actuator /sbom endpoint).
// Boot's plugin reaction owns the rest: JSON only, the "application" project type,
// the processResources copy, and the bootJar manifest attributes.
tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
    // Maven listed compile and runtime scopes only; the plugin default is every resolvable
    // configuration, which would advertise the test and build tooling as shipped components.
    includeConfigs = listOf("runtimeClasspath")
    // Only the JSON feeds the aggregate — the XML would be written and re-parsed for nothing.
    xmlOutput.unsetConvention()
}

tasks.named<CyclonedxAggregateTask>("cyclonedxBom") {
    // Maven recorded no CI run, and leaving it out keeps the file identical on every machine.
    includeBuildSystem = false
}
```

And one line appended to the existing normalization block:

```kotlin
normalization {
    runtimeClasspath {
        properties("META-INF/build-info.properties") {
            ignoreProperty("build.time")
        }
        ignore("git.properties")
        ignore("META-INF/sbom/application.cdx.json")
    }
}
```

**Five `ActuatorEndpointsIT` classes** — `services/asapp-{authentication,config,discovery,tasks,users}-service`, the `ReturnsStatusOkAndBodyContainsSBOMIds_OnSBOMEndpoint` test in each. The `Then` block's

```java
.node("ids")
.isArray();
```

becomes

```java
.node("ids")
.isArray()
.contains("application");
```

No Javadoc change — each Coverage list already reads "SBOM endpoint returns application SBOM identifiers", which is what the assertion now actually checks.

**`.claude/rules/gradle.md`** — a new `## SBOM` section between `## Packaging` and `## Full build`, carrying: the plugin/catalog/`build-logic` wiring and the Plugin-Portal-only fact; the per-service altitude and its Maven derivation; the two-task split and why the scope knobs go on `cyclonedxDirectBom` while the metadata knobs go on `cyclonedxBom`; the `3.3.0`-over-Boot's-3.0.1 reason with the stale-SBOM failure mode named; `includeConfigs` and `includeBuildSystem` as corrections of non-parity defaults, with the Backlog pointer for the latter; the unavoidable VCS external reference; and a cross-reference to `## Packaging`'s normalization bullet, which gains the SBOM as its third entry with the per-execution serial and timestamp as the reason.

**`TODO.md`** — tick line 30's subtask and remove its four notes, which this spec absorbs. Add one Backlog entry under `### Technical` → `#### build`, after the byte-reproducible-jars item (the nearest neighbour on artifact determinism):

```markdown
* Record the CI run that produced each service's bill of materials
    * Switched off during the Gradle migration for Maven parity and a machine-independent file; re-enabling is one line
```

## 6. Verification / Definition of Done

Run on `:services:asapp-users-service` unless stated; the convention plugin is shared, so one service proves the wiring and the five ITs prove the rest.

**The file and the archive**

- `./gradlew :services:asapp-users-service:build` leaves `build/resources/main/META-INF/sbom/application.cdx.json` in place, and no `build/reports/cyclonedx-direct/bom.xml`.
- The `bootJar` manifest carries `Sbom-Format: CycloneDX` and `Sbom-Location: META-INF/sbom/application.cdx.json`, and the archive contains that entry at the jar root (Boot hoists app `META-INF/*` to the root, as it already does for `build-info.properties`).

**Content**

- `metadata.component.type` is `application`; `metadata.component` carries a `vcs` external reference and **no** `build-system` one.
- `components` includes the runtime graph (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jdbc`, `postgresql`, `mapstruct`, `nimbus-jose-jwt`, `resilience4j-spring-boot4`, `micrometer-registry-prometheus`, `spring-boot-devtools`).
- `components` includes **none** of `testcontainers`, `junit-jupiter`, `assertj-core`, `mockserver-netty`, `pitest`, `asciidoctorj`, `archunit`, `jacoco`, `liquibase-core`.
- The service itself appears once, as `metadata.component`, not additionally in `components` — confirming `mergeAll`'s bomRef de-duplication holds at a leaf project.
- Capture the plugin's INFO line `For project asapp-users-service following configurations are in scope…` and confirm it names `runtimeClasspath` alone.

**The endpoint, both build tools**

- The five tightened `ActuatorEndpointsIT` tests fail before the build change (`{"ids":[]}`) and pass after.
- Under Maven, `application.cdx.json` still lands in `target/classes/META-INF/sbom/` at `prepare-package`, ahead of Failsafe — provable without Docker by building one service to `prepare-package -DskipTests` and looking for the file. A full `mvn verify` on one service is the belt-and-braces version and runs only on request. **This is a gate, not a nicety:** `ci.yml` still invokes Maven, so a Maven-red assertion breaks CI on this branch.

**Incrementality and cache**

- Two consecutive `:build` runs: `cyclonedxDirectBom` and `cyclonedxBom` report `UP-TO-DATE` on the second.
- Touch a Java source: the SBOM tasks stay `UP-TO-DATE` while the compile and test tasks re-run.
- Bump a catalog version the service resolves: the SBOM tasks **re-execute** — the defect 3.0.1 would have had.
- `clean` then `build` with the build cache warm: `test` and `integrationTest` come back `FROM-CACHE` despite a freshly minted serial and timestamp. This is the normalization payoff and the one measurement that justifies the line.

**Cost and hygiene**

- `--profile` durations for `cyclonedxDirectBom` and `cyclonedxBom`, cold (post-`clean`) and warm, recorded in §10.
- `./gradlew help --profile` before and after, for the apply-time overhead — the number the Liquibase plugin's +15–20 ms per service was recorded against.
- `./gradlew help --warning-mode all` reports no new deprecation beyond the two known upstream ones (Liquibase's `Project.container(Class, Closure)`, grolifant's `StartParameter.isConfigurationCacheRequested`).
- `./gradlew spotlessCheck` clean across all seven modules; `./gradlew :build-logic:check` green (the plugin classpath gained an entry).

## 7. Out of scope / YAGNI

- The CI and release workflow migrations, and the parallel-builds and Maven-removal subtasks.
- README changes: no README documents an SBOM command, and every `/sbom` exposure list is unchanged.
- A guard asserting `includeConfigs` stays narrow. The rules bullet plus the "components include none of …" verification above is the record; a configuration-level test is the existing Backlog item's job.
- Aligning the build classpath's own Jackson, or anything else `build-logic` resolves. This adds one plugin artifact and changes no application resolution.
- Making the jar byte-reproducible. Already a Backlog item, and the SBOM's unpinnable timestamp is now one more input to it — worth noting there, not solving here.

## 8. Contingencies

- **Boot's reaction does not fire, or `named("cyclonedxBom", CyclonedxAggregateTask.class)` throws.** A 3.3.0-versus-3.0.1 API drift. Step down 3.2.4 → 3.0.1; at 3.0.1, document the stale-SBOM defect as a known cost and revisit when Boot's own pin moves. Do not work around it by configuring the direct task in the aggregate's place — the copy Boot wires reads the aggregate's output.
- **`checkForMissingInputSboms` fails the build** with "input SBOMs … do not exist". The direct task was skipped; find out why rather than disabling it, since a disabled direct task means an SBOM with no components.
- **The cold cost is unacceptable.** Set `includeMetadataResolution = false`, which drops the per-component POM lookup that resolves licences. That is a real loss against Maven, so it is a decision argued from the §6 numbers, recorded in the rules if taken — not a pre-emptive default.
- **`runtimeClasspath` yields an empty or implausibly short component list.** Read the in-scope-configurations INFO line before changing anything; the likely cause is a name that does not full-match, not a broken graph.
- **Cross-machine cache reuse still misses on the integration tier.** Confirm the ignore is inside `runtimeClasspath { }` and the path is classpath-relative (`META-INF/sbom/application.cdx.json`, no leading slash) before suspecting anything else.
- **A configuration-cache problem surfaces.** Irrelevant today — the configuration cache is deferred behind asciidoctor — but record it in the rules so the deferred subtask inherits a known second blocker rather than discovering one.

## 9. Git workflow

Two commits on `build/replace-maven-with-gradle-19-restore-bom`: this spec on its own (`docs(gradle)`), then the implementation (`build(gradle)`), following the convention the BOM-reuse subtask established.

## 10. Post-implementation notes

This spec was written before implementation. What it designed did ship: the CycloneDX plugin applies in `asapp.service-conventions`, the SBOM embeds at `META-INF/sbom/application.cdx.json`, and `/sbom` reports the `application` id on all five services. But the design was revised in two load-bearing ways during review — the scope decision §4 argued for was reversed, and the red-before-green endpoint guarantee §2 promised was not delivered — so the deltas below are not cosmetic.

The canonical implementation is the current state of the real artifacts on this branch — `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/build.gradle.kts` — not this document. `.claude/rules/gradle.md`'s `## SBOM` section is the durable, re-measured record of the measurements and defaults below; treat it, not this spec, as the source of truth going forward.

**Notable deltas:**

- **Scope narrowed to `productionRuntimeClasspath`** (reverses §4's `SBOM scope` row and §5's block). `runtimeClasspath` extends `developmentOnly`, so the row's chosen `includeConfigs = listOf("runtimeClasspath")` would have advertised `spring-boot-devtools` as a component of a jar the Boot plugin deliberately excludes it from — a false positive for anyone scanning the publicly pullable ghcr image. `includeConfigs = listOf("productionRuntimeClasspath")` is what shipped instead. Component count moved with it: **225** Gradle components against Maven's **224**, down from the **226** this section's first draft reported, devtools now confirmed **absent**, and the plugin's in-scope INFO line now names `productionRuntimeClasspath`, not `runtimeClasspath`.
- **An in-build empty-BOM guard was added** (reverses §7's "out of scope" YAGNI item, which routed this to "the existing Backlog item's job"). A `doLast` on `cyclonedxBom` parses its own `jsonOutput` with `org.cyclonedx.parsers.BomParserFactory` and throws `GradleException` when `components` is null-or-empty — null and not merely empty because CycloneDX annotates the field `@JsonInclude(NON_EMPTY)`, so an absent array deserializes to `null` rather than to an empty list, and a check on emptiness alone would miss the case it exists for — pulling in `CyclonedxAggregateTask`, `CyclonedxDirectTask`, `BomParserFactory` and `GradleException`, four imports against §5's "two." Its stated hole: it covers the silent-empty case, not the too-wide one, and Gradle skips `doLast` on `UP-TO-DATE`/`FROM-CACHE`, so it is a first-execution and regression gate, not an always-on one.
- **The five tightened `ActuatorEndpointsIT` assertions were reverted** (reverses §5's test change, §2's first Goal, and §6's DoD bullet for it). The `ids` assertion passes whenever a readable file exists at the classpath location, so it stays green for a zero-component SBOM — a guard in appearance only, and all five classes still read the bare `.node("ids").isArray()`, not `.contains("application")`. §2's "driven by a test that is red before the change" promise is **not delivered**. The real content assertion is now a separate `(tests)` task under `## 0.5.0` in `TODO.md` ("Assert the packaged bill of materials lists real components") — deliberately outside this Gradle-migration task, and deliberately not a Backlog item.
- **The `3.3.0` version rationale was misattributed** (corrects §4's `Version` row and §8's rollback step). The cache-invalidation fix — `@InputFiles @PathSensitive(RELATIVE) getResolvedDependencies()` — landed at **3.2.0**, not 3.3.0; 3.3.0's own change is unrelated, lazy `addAllLater` task registration in place of `afterEvaluate`. The catalog pin itself is unchanged (`cyclonedx-gradle = "3.3.0"`); what changed is the rollback chain, now ordered `3.3.0` → `3.2.4` → `3.0.1`, with the stale-SBOM cost attached to the `3.0.1` step alone rather than to any step-down.
- **The lifecycle-placement parity claim was inverted** (corrects §4's `Where it runs` row and its `Rejected: release-path only` row). Boot's parent POM binds `cyclonedx-maven-plugin` to `generate-resources`, ahead of `compile` and `test`; `services/pom.xml` deliberately overrode that to `prepare-package` to keep it off `mvn compile` and `mvn test`. The Gradle wiring, arriving through `processResources`, knowingly **reintroduces** that cost on four entry points — `test`, `classes`, `integrationTest`, `bootRun` — rather than matching Maven's later placement. An `onlyIf` gate on `gradle.taskGraph` was weighed and declined on its own tradeoffs, not, as §4 claimed, unreachable.
- **The cost figures were superseded** (corrects §6's measurement request and this section's own first draft). A warm `UP-TO-DATE` invocation still pays the plugin's full dependency-resolution walk to fingerprint `getResolvedDependencies()` — measured at **≈1.2 s** — so the original "~0.01 s warm" figure was task-action duration only, not the cost of the invocation itself. See `.claude/rules/gradle.md` `## SBOM`'s "Measured cost" bullet for the current numbers rather than re-transcribing them here.
- **The configuration cache turned out compatible** (corrects §8's contingency, which called it "irrelevant today" and asked that it be recorded as a known second blocker). Measured: an entry stores, then reuses, with `totalProblemCount: 0`. The deferred configuration-cache subtask inherits no second blocker from this plugin. Isolated Projects is the real caution here — and it fails on `io.spring.dependency-management`, with zero matches for `CyclonedxPlugin` in any diagnostic trace, so a future investigator must not chase CycloneDX when that work starts.
- **`processResources` is not the `build-info.properties` trade-off** (corrects this section's own first draft). What was one mechanism is three distinct ones, per the `## Packaging` normalization-outcomes bullet: the SBOM is a genuine `Copy` *source*, and its content is volatile, so `processResources` executes and cannot be cache-restored whenever the SBOM regenerates. The `test`/`integrationTest` half of the original claim — that the expensive tier stays cacheable while the cheap repack does not — still holds.
- **Field-level parity is count-level only** (extends §6's `Content` DoD, which asked only for type, presence/absence and de-duplication). `scope: "required"` sits on 224/224 Maven components and 0/225 Gradle ones; `purl` gains a `?project_path=…` qualifier where Maven emitted `?type=jar`; `metadata.component` loses `description` and `licenses`; `metadata.lifecycles` and `metadata.properties` disappear entirely. No knob exists for any of it — in particular `licenseChoice`, the one that reads like the fix for the missing `licenses`, writes a different field and does not restore it. Only the `purl` change has a plausible consumer consequence, and it is deliberately given no `TODO.md` entry — worth acting on only if that identity change ever matters to a real consumer.

Two corrections from this section's original draft still hold and stay recorded here, since they correct the spec's own §1 analysis rather than the shipped tree: Maven emitted **two** external references, not zero — both malformed, concatenating the module path onto `spring-boot-starter-parent`'s inherited `<url>`/`<scm>` — so Gradle's single well-formed `vcs` reference is a net improvement over Maven, not a divergence from it; and `liquibase-core` and `archunit` genuinely ship under both tools, so their presence in the shipped SBOM is parity, not scope leakage.

For future build-convention edits, treat `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts` and `.claude/rules/gradle.md` `## SBOM` as the template; this spec is preserved as a record of the original design intent.

## 11. References

- Spring Boot Gradle Plugin reference, *Reacting to Other Plugins* → CycloneDX — the configured properties, the `META-INF/sbom` location, and the two manifest attributes
- `spring-projects/spring-boot@v4.0.5` → `build-plugin/spring-boot-gradle-plugin/src/main/java/org/springframework/boot/gradle/plugin/CyclonedxPluginAction.java` and its `CyclonedxPluginActionIntegrationTests` — the aggregate-task lookup, the `processResources` copy, and the asserted manifest contract
- `spring-projects/spring-boot@v4.0.5` → `platform/spring-boot-internal-dependencies/build.gradle` — the 3.0.1 pin Boot builds against
- `spring-projects/spring-boot@v4.0.5` → `module/spring-boot-actuator/.../sbom/SbomEndpoint.java` — the three auto-detected locations and the `application` id
- `CycloneDX/cyclonedx-gradle-plugin@3.3.0` → `CyclonedxPlugin`, `CyclonedxDirectTask`, `CyclonedxAggregateTask`, `BaseCyclonedxTask`, `SbomBuilder`, `SbomGraphProvider`, `utils/ExternalReferencesUtil`, `utils/EnvironmentUtils` — task registration, the input declarations 3.0.1 lacks, the configuration filter, and the volatile metadata
- `spring-boot-starter-parent:4.0.5` and `services/pom.xml:297-306` — the Maven execution this restores
- Gradle User Manual, *Incremental Build* → runtime-classpath normalization — the `ignore(...)` mechanism and its `@Classpath` precondition
- `.claude/rules/gradle.md` — `## Packaging` (the normalization block and the metadata-file behaviour), `## Shared Build Configuration` (catalog-pinning rule), `## Repositories & settings` (the Plugin Portal), `## Ordering`
