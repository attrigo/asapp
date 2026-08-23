# Gradle Docker image building — design spec

**Date**: 2026-07-31
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate Docker image building to Gradle" (line 25).
**Scope**: Configure the `bootBuildImage` task the Spring Boot plugin already registers on the 5 services so `./gradlew bootBuildImage` replaces `mvn spring-boot:build-image` — the image name, the two buildpacks, and the buildpack environment Maven declares in `services/pom.xml`. One build block, one rules section, four `TODO.md` edits. No new plugin, no catalog entry, no `pom.xml` edit, no `docker-compose.yaml` edit, no application-source edit.

## 1. Context

Thirteen prior subtasks moved the build onto Gradle 9.6.1 / JDK 25. The packaging subtask applied `org.springframework.boot` in `asapp.service-conventions`, which registered `bootBuildImage` on all 5 services as a side effect and deliberately left it unconfigured (`2026-07-24-gradle-packaging-design.md` §3: "Applying the plugin *creates* the `bootBuildImage` task but this subtask leaves it unconfigured"). The task exists today and would build an image named `docker.io/library/<module>:0.5.0-SNAPSHOT` — the plugin default, which no `docker-compose.yaml` entry references. This subtask configures it and nothing else.

**What Maven's image build actually is.** One `<image>` block in `services/pom.xml:240-252`, inherited by all 5 services; the two libs opt out with `spring-boot.build-image.skip=true` (`libs/pom.xml:22`). Everything else is the goal's defaults.

| Maven mechanism | Value | Gradle equivalent | Needs configuring? |
|---|---|---|---|
| `<name>` | `ghcr.io/attrigo/${project.artifactId}:${project.version}` | `imageName`, default `docker.io/library/${project.name}:${project.version}` | **yes** (§4) |
| `<buildpacks>` | `urn:cnb:builder:paketo-buildpacks/java`, `docker.io/paketobuildpacks/health-checker` | `buildpacks`, default = the builder's own list | **yes** (§4) |
| `<env>` → `BP_HEALTH_CHECKER_ENABLED` | `true` | `environment` | **yes** (§4) |
| `<env>` → `BP_JVM_VERSION` | `${java.version}` → `25` | `environment` | **yes** (§4) |
| `<createdDate>` | `${maven.build.timestamp}` | `createdDate`, accepts an ISO 8601 instant or `now` | **no — deliberately dropped** (§4) |
| builder | unset → `paketobuildpacks/builder-noble-java-tiny:latest` | same default | no — parity by inaction |
| `runImage` / `trustBuilder` / `pullPolicy` / `imagePlatform` | unset | same defaults | no |
| `applicationDirectory` / `securityOptions` / `bindings` / `network` | unset | same defaults | no |
| `tags` / `publish` / registry credentials | unset — the release workflow pushes with `docker push` | out of scope, → line 37 (§3) | no |
| libs excluded | `spring-boot.build-image.skip=true` | no Boot plugin in the libs → no task at all | no — flag-free analog |

**What the images are consumed by.** `docker-compose.yaml` references `ghcr.io/attrigo/<service>:0.5.0-SNAPSHOT` for all five services, so the image name is not cosmetic — a wrong name leaves the compose stack pulling from a registry instead of using the local build. config-service and discovery-service additionally declare `healthcheck: ["CMD", "/workspace/health-check"]`, a binary contributed **only** by the health-checker buildpack, and three services `depends_on … condition: service_healthy` those two. So the `buildpacks` list is load-bearing for the whole stack, not just for those two containers.

**Two findings that changed the translation.** Both verified against Spring Boot 4.0.5 rather than assumed from older docs:

1. **Boot no longer derives `BP_JVM_VERSION`.** `BootBuildImage` in v4.0.5 declares no `targetJavaVersion` property, and `customizeEnvironment` applies only what the build configures — the old `targetCompatibility`-based derivation is gone. It was removed because `bootJar` now writes `Build-Jdk-Spec` into the manifest ([spring-boot#32829](https://github.com/spring-projects/spring-boot/issues/32829)), computed from `getTargetJavaVersion().get().getMajorVersion()`, and Paketo's `libjvm` reads that attribute. Verified empirically on this branch: `services/asapp-users-service/build/libs/asapp-users-service-0.5.0-SNAPSHOT.jar` carries `Build-Jdk-Spec: 25`. So the JVM version would reach the image either way — but through an implicit two-hop chain rather than a declaration.
2. **`bootBuildImage` is never up-to-date.** The task is annotated `@DisableCachingByDefault` and declares no outputs, so Gradle re-executes it on every invocation. There is no build-cache or normalization concern of the kind the `build-info.properties` / `git.properties` timestamps created for `integrationTest`, and nothing to configure.

**Current Gradle state.** `asapp.service-conventions` (5 services) applies the Boot plugin, disables the plain `jar`, configures `springBoot.buildInfo`, declares the runtime-classpath normalization, configures `bootRun`, and registers `integrationTest` plus the two extra JaCoCo reports. `gradle.properties` has `org.gradle.caching=true`, `org.gradle.parallel=false` (WSL), configuration cache deferred but intended.

**Convention-plugin hierarchy**: `java-conventions` (all 7) ← `library-conventions` (2 libs) and `service-conventions` (5 services, applied directly by config + discovery) ← `domain-service-conventions` (3 domain services).

## 2. Goals

- **One command**: `./gradlew bootBuildImage` from the repo root builds all five images; `./gradlew :services:<svc>:bootBuildImage` builds one. Replaces `mvn spring-boot:build-image`.
- **Name parity**: `ghcr.io/attrigo/<service>:<version>`, so `docker-compose.yaml` keeps working unedited.
- **Buildpack parity**: the same two buildpacks in the same order, so `/workspace/health-check` exists and the compose healthchecks and `depends_on` gates behave as they do today.
- **JVM parity, declared not inferred**: the image runs a Java 25 runtime because the build says so, single-sourced from the toolchain.
- **Correct altitude**: one block in the convention plugin that owns the 5-service archetype; nothing per-service, nothing at the root.
- Zero `pom.xml` edits, zero `docker-compose.yaml` edits, zero application-source edits; `mvn spring-boot:build-image` keeps working until the removal subtask.

## 3. Non-goals

- **Image publishing.** `publish`, `tags`, and `docker { publishRegistry { … } }` stay unconfigured. `release.yml:79-86` builds with Maven and then loops `docker push`; replacing that command — and deciding whether the push moves into the build — belongs to "Migrate the release workflow to Gradle" (line 37), which gets a note (§5). Developer decision.
- **Documentation.** `mvn spring-boot:build-image` appears in the root README, all five service READMEs, `tools/jmeter/README.md`, and `CLAUDE.md`. Handed to "Migrate build documentation to Gradle" (line 45) as a note, the way every prior subtask in this migration handed over its doc debt.
- **Reproducible images.** §4 adopts the fixed created date, which is a *precondition* for reproducibility, not reproducibility itself — the packaged jar is still not byte-reproducible. Making it so is an existing Backlog item, not this subtask.
- **Native images.** No GraalVM, no `bootBuildImage` native configuration; nothing in the Maven build did this either.
- **`docker-compose.yaml`.** Image references already match what this design produces.
- **Database migration commands** (line 26), **git hook installation** (line 31) — the next two subtasks.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Image name | **`imageName = "ghcr.io/attrigo/${project.name}:${project.version}"`** | Literal translation of Maven's `ghcr.io/attrigo/${project.artifactId}:${project.version}`. The Gradle default (`docker.io/library/<name>:<version>`) matches nothing in `docker-compose.yaml`, so this is not optional. |
| Name altitude | **The convention plugin, all 5 — not per-service** | Maven templated the name from `${project.artifactId}` in a single inherited block; five hand-written names would be new divergence, not migration, and would add five sites for one policy. |
| `project.name` is safe here | **Deliberate, and not the case `gradle.md`'s "don't derive from `project.name`" bullet forbids** | That rule guards the pitest globs, where a module name is used to *guess a package prefix* and a partial typo can silently shrink the mutant population while a ratio threshold still passes. Here `project.name` **is** the artifact id — the same identity Maven interpolated — and any wrong value fails loudly at `docker push` or at `docker-compose up`. |
| Rejected: root `buildImages` aggregator | **No root task** | Naming subprojects' tasks from the root is cross-project configuration, and `./gradlew bootBuildImage` already fans out to all five through Gradle's task selector — the same mechanism `fullBuild` relies on. The standing "never add a root aggregator" rule applies unchanged. |
| Buildpacks | **`buildpacks = listOf("urn:cnb:builder:paketo-buildpacks/java", "docker.io/paketobuildpacks/health-checker")`** | Verbatim parity, order preserved (buildpack order is execution order). `buildpacks` *replaces* the builder's default list, so both entries are required: the first re-selects the builder's own java buildpack, the second adds the health checker as an OCI image. Neither reference is version-pinned — same as Maven, so both resolve to `:latest` on every build. |
| `BP_HEALTH_CHECKER_ENABLED` | **Literal `"true"`** | Parity. The health-checker buildpack contributes `/workspace/health-check` only when enabled, and three compose services gate on the resulting healthchecks. |
| `BP_JVM_VERSION` | **Explicit, wired lazily from the toolchain: `environment.put("BP_JVM_VERSION", java.toolchain.languageVersion.map { it.toString() })`** | Parity in effect (`25`), single-sourced from the one place the Java version is pinned, and the same lazy-provider pattern as the file's existing `springBoot.buildInfo` `additional.put("java", …)`. An explicitly set `BP_JVM_VERSION` also outranks the manifest-derived value ([libjvm#350](https://github.com/paketo-buildpacks/libjvm/issues/350), closed), so the declaration is authoritative rather than advisory. |
| Rejected: drop `BP_JVM_VERSION` | **Not relying on `Build-Jdk-Spec` alone** | It would work today — the manifest carries `25` and `libjvm` reads it — but it makes the runtime JVM an implicit consequence of a three-link chain (toolchain → `targetCompatibility` → manifest → buildpack), silently breakable by anything that sets `targetCompatibility` independently of the toolchain, and it drops a value Maven declared. |
| Rejected: hardcoded `"25"` | **Not a literal** | Byte-identical to Maven, but it duplicates the Java version already pinned in `asapp.java-conventions`, giving a future bump two sites to touch and one to forget. |
| `environment` set with two `put` calls | **`put`, never `=`** | `environment` is a `MapProperty`; assignment replaces the whole map, so a later `=` would silently drop an earlier entry. `put` is additive and matches the `additional.put(…)` idiom already in this file. |
| `createdDate` | **Not configured — the CNB fixed date (`1980-01-01T00:00:01Z`) applies** | **Deliberate divergence from Maven, developer decision.** The image `created` field feeds the config JSON, whose digest feeds the manifest, whose digest *is* the image identity — so a wall-clock value means identical source produces a different image every build. CNB pins it build-wide for exactly that reason. Nothing is lost informationally: `build-info.properties` still carries the real `build.time` and the actuator `/info` endpoint still reports it (the normalization block excludes that key from the *fingerprint*, not from the file). |
| Honest limit of that choice | **The images are not reproducible yet** | The jar inside still varies per build — `build.time`, `git.properties` host/user, and Gradle's `preserveFileTimestamps = true` default — and the builder is `:latest` under `pullPolicy = ALWAYS`. The fixed date is the precondition; the Backlog's "Make all build jars byte-reproducible" item is the remaining variable, and gets a sub-bullet recording that (§5). |
| Rejected: `createdDate = "now"` | **Not the Maven timestamp** | Considered and rejected by the developer after weighing it. It is exact parity and keeps `docker images` readable — the fixed date is why buildpack images report as ~46 years old ([docker/cli#2995](https://github.com/docker/cli/issues/2995)) — but it trades away digest stability permanently for a display detail, and the build time remains available from the actuator. |
| builder / runImage / trustBuilder / pullPolicy / imagePlatform / securityOptions / applicationDirectory / bindings / network / cleanCache / verboseLogging / the three cache specs | **All left at their defaults** | Maven configured none of them and the defaults are identical on both sides — including the builder, `paketobuildpacks/builder-noble-java-tiny:latest` in Boot 4.0.5, which both plugins resolve the same way. Writing any of them down would imply a choice was made. |
| No test run before the image | **Accepted gain, not a gap** | `mvn spring-boot:build-image` forks the `package` lifecycle (which is why Boot ships a separate `build-image-no-fork` goal, and why `release.yml` passes `-DskipTests`). `bootBuildImage` depends only on `bootJar`, so no tier runs and no skip flag is needed — noted for line 37. |
| Caching / up-to-dateness | **Nothing to configure** | `@DisableCachingByDefault`, no declared outputs → always executes, matching Maven. No normalization, no `outputs.upToDateWhen`, no cache key concerns. |
| Umbrella membership | **Stays out of `check` / `build` / `fullBuild`** | The standing rule from the full-build subtask: "images and local runs were always separate commands". Nothing here changes it. |
| Docker-dependent verification | **Delegated to the developer in full (§7)** | Developer instruction: implementation runs **no** command that requires a Docker daemon. Everything observable without Docker stays with implementation; everything else is recorded in §11 by the developer. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** (5 services) — add the import alongside the existing ones, and the block. Exact statement ordering within the file is deferred to "Clean Gradle files" (line 50).

```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
```

```kotlin
// Docker image (the "mvn spring-boot:build-image" equivalent); needs a running Docker daemon.
// No createdDate: the CNB fixed date keeps the image digest stable across identical builds.
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = "ghcr.io/attrigo/${project.name}:${project.version}"
    // Replaces the builder's default list, so the java buildpack is re-selected explicitly;
    // the health checker contributes the /workspace/health-check the compose healthchecks run.
    buildpacks = listOf("urn:cnb:builder:paketo-buildpacks/java", "docker.io/paketobuildpacks/health-checker")
    environment.put("BP_HEALTH_CHECKER_ENABLED", "true")
    environment.put("BP_JVM_VERSION", java.toolchain.languageVersion.map { it.toString() })
}
```

`tasks.named<BootBuildImage>("bootBuildImage")` is safe at this point for the same reason the file's existing `tasks.named<Jar>("jar")` and `tasks.named<BootRun>("bootRun")` are: the Boot plugin is applied in this script's own `plugins {}` block, so the task is registered before the body runs.

**No other build file changes.** Core Gradle plus the already-applied Boot plugin — no `gradle/libs.versions.toml` entry, no `build-logic/build.gradle.kts` classpath addition, no root or per-service build script edit.

**`.claude/rules/gradle.md`** — add a **Docker images** section, placed after "Running locally" and before "Ordering" (the file tracks subtask order). It must carry:

- `./gradlew bootBuildImage` (root, all 5 via the task selector) and `./gradlew :services:<svc>:bootBuildImage` (one) replace `mvn spring-boot:build-image`; the task is registered by the Boot plugin applied in `asapp.service-conventions`, so the two libs have no `bootBuildImage` at all — the flag-free analog of Maven's `spring-boot.build-image.skip=true` in `libs/pom.xml`.
- The whole configuration is one `tasks.named<BootBuildImage>("bootBuildImage")` block in `asapp.service-conventions` (5 services) — never per-service and never a root aggregator, since an unqualified task name already fans out and a root task would be cross-project configuration.
- `imageName = "ghcr.io/attrigo/${project.name}:${project.version}"`, because the plugin default `docker.io/library/<name>:<version>` matches nothing in `docker-compose.yaml`. Using `project.name` here is **not** the pattern the pitest bullet forbids: it *is* the artifact id Maven interpolated, and a wrong value fails loudly rather than silently narrowing coverage.
- `buildpacks` lists `urn:cnb:builder:paketo-buildpacks/java` **then** `docker.io/paketobuildpacks/health-checker`; the property replaces the builder's default list, so the java buildpack must be re-listed, and order is execution order. The health checker plus `BP_HEALTH_CHECKER_ENABLED=true` is what contributes `/workspace/health-check`, which config-service's and discovery-service's compose healthchecks invoke and three other services gate on via `depends_on … service_healthy`.
- `BP_JVM_VERSION` is set explicitly and lazily from the toolchain (`java.toolchain.languageVersion.map { it.toString() }`), never hardcoded and never dropped: Boot 4.0.5 removed the old `targetCompatibility`-based derivation (`BootBuildImage` has no `targetJavaVersion` property; `customizeEnvironment` applies only what the build sets) because `bootJar` now writes `Build-Jdk-Spec` into the manifest (spring-boot#32829, verified on this branch as `Build-Jdk-Spec: 25`) and Paketo's `libjvm` reads it — so relying on that alone would work but would make the runtime JVM an implicit consequence of toolchain → `targetCompatibility` → manifest → buildpack. An explicit `BP_JVM_VERSION` outranks the manifest-derived value (libjvm#350).
- Use `environment.put(…)`, never `environment = mapOf(…)` — it is a `MapProperty` and assignment replaces the whole map.
- `createdDate` is **deliberately not configured**, a divergence from Maven's `<createdDate>${maven.build.timestamp}</createdDate>`: the `created` field feeds the config JSON → manifest → image digest, so a wall-clock value gives identical source a different image identity every build, while CNB's fixed `1980-01-01T00:00:01Z` keeps it stable. The cost is that `docker images` reports the images as ~46 years old; the real build time stays in `build-info.properties` and on the actuator `/info` endpoint. **Not yet reproducibility** — the packaged jar still varies (`build.time`, git host/user, `preserveFileTimestamps = true`) and the builder is `:latest` under `pullPolicy = ALWAYS`; the Backlog's byte-reproducible-jars item is the remaining variable, and is the revisit trigger. If the fixed date is ever reverted, `createdDate = "now"` is one line — update this bullet in the same change.
- Everything else is left at its default (builder, `runImage`, `trustBuilder`, `pullPolicy`, `imagePlatform`, `securityOptions`, `applicationDirectory`, `bindings`, `network`, `tags`, `publish`, `docker { }` registries, `cleanCache`, `verboseLogging`, the three cache specs) — Maven set none of them and the defaults match, including the Boot 4.0.5 builder `paketobuildpacks/builder-noble-java-tiny:latest`.
- The task is `@DisableCachingByDefault` with no declared outputs, so it **always executes** and is never `UP-TO-DATE` — matching Maven, and nothing to configure.
- Unlike `mvn spring-boot:build-image`, which forked the `package` lifecycle, `bootBuildImage` depends only on `bootJar`, so no test tier runs and `-DskipTests` has no analog.
- Keep it off the `check` / `build` / `fullBuild` path — images were always a separate command.

**`TODO.md`** — four edits:

1. Check off "Migrate Docker image building to Gradle" (line 25).
2. Add under "Migrate the release workflow to Gradle" (line 37):

```markdown
        - **Note:** `-DskipTests` has no analog — `bootBuildImage` depends only on `bootJar`, while `mvn spring-boot:build-image` forked the whole `package` lifecycle
        - **Note:** the `docker push` loop could retire — `./gradlew bootBuildImage --publishImage` with a `docker { publishRegistry { … } }` block pushes from the build; weigh that against keeping the push visible in the workflow
```

3. Add under "Migrate build documentation to Gradle" (line 45):

```markdown
        - **Note:** the Docker image command becomes `./gradlew bootBuildImage` (all five) or `./gradlew :services:<svc>:bootBuildImage` — it appears in the root README, all five service READMEs, `tools/jmeter/README.md`, and `CLAUDE.md`
```

4. Add under the Backlog's `#### build` → "Make all build jars byte-reproducible …" item:

```markdown
    * The images already build with the CNB fixed created date, so the jar is the last remaining variable
```

## 6. Placement / altitude rationale

- **The whole block → `asapp.service-conventions` (5).** "How a Spring Boot service in this repo becomes a container image" is archetype policy, at the same altitude that already owns the Boot plugin, the disabled plain jar, build-info, git-properties, the runtime-classpath normalization, and `bootRun`. It is also the exact altitude Maven used: one inherited `<image>` block in `services/pom.xml`.
- **Nothing in `asapp.domain-service-conventions`.** The three domain services are imaged exactly like the two infra ones — no per-archetype variation, unlike javadoc/sources, pitest, and asciidoctor.
- **Nothing per-service.** There is no per-service image data: the name is templated, and the buildpacks and environment are identical for all five.
- **Nothing in the libs, by construction.** No Boot plugin → no `bootBuildImage`.
- **Nothing at the root.** No aggregation, no cross-project reference.

## 7. Verification / Definition of Done

**Implementation runs no command that requires a Docker daemon** (developer instruction). It verifies only what is observable without one:

1. **The build configures.** `./gradlew tasks` (or `projects`) from the root succeeds — `build-logic` recompiles and all 7 modules configure with the new block.
2. **The task configures on a service.** `./gradlew :services:asapp-config-service:bootBuildImage --dry-run` completes without executing anything, proving the block compiles and the task resolves. (Dry-run does not finalize task inputs, so it validates configuration, not the resolved image name.)
3. **No collateral damage.** `./gradlew :services:asapp-users-service:build --dry-run` and `:fullBuild --dry-run` never mention `bootBuildImage`.
4. **Maven untouched**: no `pom.xml` edited, so `mvn spring-boot:build-image` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

**The developer runs the Docker-dependent checks** and records the outcome in §11:

5. **An image builds under the right name.** `./gradlew :services:asapp-config-service:bootBuildImage`, then `docker images ghcr.io/attrigo/asapp-config-service` lists tag `0.5.0-SNAPSHOT`.
6. **The created date is the divergence, confirmed.** `docker inspect -f '{{.Created}}' ghcr.io/attrigo/asapp-config-service:0.5.0-SNAPSHOT` → `1980-01-01T00:00:01Z`.
7. **The health checker landed.** `docker-compose up -d asapp-config-service` reports the container **healthy** — the compose healthcheck runs `/workspace/health-check`, which exists only if both the health-checker buildpack and its env var took effect. This is the check that would fail if `buildpacks` had dropped either entry.
8. **The JVM is 25.** The container's startup log reads `using Java 25.…` — confirms `BP_JVM_VERSION` reached the buildpack.
9. **The whole stack still comes up.** `./gradlew bootBuildImage` (all five — the expensive run, at the developer's discretion), then `docker-compose up -d`: every service reaches healthy/up, with no `docker-compose.yaml` edit.

## 8. Out of scope / YAGNI

Image publishing, `tags`, and registry credentials (line 37) · README / `CLAUDE.md` / jmeter-doc migration (line 45) · byte-reproducible jars (Backlog) · native images · `docker-compose.yaml` edits · a root `buildImages` aggregator · pinning the builder or buildpack versions · `imagePlatform` / multi-arch images · database migration commands (line 26) · git hook installation (line 31) · any `pom.xml` or application-source edit.

## 9. Contingencies

- **`java` does not resolve inside the task block.** The file already reaches the extension from a nested lambda (`springBoot { buildInfo { … java.toolchain … } }`), so this is not expected; if Kotlin complains, hoist `val javaLanguageVersion = java.toolchain.languageVersion.map { it.toString() }` above the block and reference it — same laziness, same single source.
- **`buildpacks = listOf(…)` rejected.** Lazy-property assignment has been supported since Gradle 8.2 and this build is on 9.6.1; if it fails, use `buildpacks.set(listOf(…))` and note nothing — it is the same property.
- **`urn:cnb:builder:paketo-buildpacks/java` stops resolving** because a future default builder no longer ships that buildpack. Pin `builder` explicitly to a builder that does, and record the pin as new policy in `gradle.md` — do not silently drop the buildpacks list, which would also drop the health checker.
- **The fixed created date proves annoying in practice.** `createdDate = "now"` is one line; the `gradle.md` bullet must be updated in the same change so the rules file does not lie.
- **A service ever needs a different registry, tag, or buildpack set.** Override `imageName` (or the offending property) in that service's own build script, exactly as config-service overrides the `bootRun` profile list. Do not weaken the shared policy.
- **No Docker daemon / WSL socket trouble.** The task fails with a connection error, exactly as `mvn spring-boot:build-image` does. Not a build-script concern, and not something this subtask papers over.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-14-docker-image`. A single commit:

1. `build(gradle): migrate Docker image building to Gradle` — the `bootBuildImage` block in `asapp.service-conventions`, the `.claude/rules/gradle.md` "Docker images" section, the four `TODO.md` edits, and this spec.

The spec rides the implementation commit rather than landing in one of its own, matching all thirteen prior subtasks in this migration.

Following this migration's established pattern, implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec was written before implementation. The change shipped **exactly as designed**, with no deviations: one `tasks.named<BootBuildImage>("bootBuildImage")` block in `asapp.service-conventions` carrying `imageName`, the two-entry `buildpacks` list, and the two `environment.put(…)` calls, with `createdDate` left unset. No catalog, `build-logic` classpath, per-service script, `docker-compose.yaml`, `pom.xml`, or application-source edit. Neither §9 contingency fired — the `java` extension accessor resolved inside the task-configuration block (as the file's existing `springBoot { buildInfo { … } }` block predicted), and lazy-property assignment on `imageName` / `buildpacks` worked on Gradle 9.6.1.

**Verified during implementation** (§7.1–7.4, none of which needs Docker):

- **§7.1–7.2, the build configures:** `build-logic` recompiles, all 7 modules configure, and `:services:asapp-config-service:bootBuildImage --dry-run` schedules the task without executing it.
- **§7.3, no collateral damage:** `bootBuildImage` appears **zero** times in `:services:asapp-users-service:build --dry-run` and in `:fullBuild --dry-run` — it is in no lifecycle graph.
- **Altitude, measured not assumed:** `./gradlew tasks --all` lists exactly **five** `bootBuildImage` tasks, one per service, none in the two libs — the Boot-plugin-reach claim of §6, confirmed.
- **The resolved inputs, on all five services.** Dry-run proves the block compiles but does not finalize task inputs, so a throwaway init script printed each task's `@Input` properties, forcing the lazy providers to resolve: `imageName = ghcr.io/attrigo/<service>:0.5.0-SNAPSHOT` (byte-identical to all five `docker-compose.yaml` references), `environment = {BP_HEALTH_CHECKER_ENABLED=true, BP_JVM_VERSION=25}` (so the toolchain provider resolves to `25`), `buildpacks = [urn:cnb:builder:paketo-buildpacks/java, docker.io/paketobuildpacks/health-checker]` in that order, and `createdDate = null` / `builder = null` / `pullPolicy = null` / `publish = false` / `tags = []` — the divergence and the parity-by-inaction defaults, both as specified.
- **§7.4, Maven untouched**: no `pom.xml` edited, so `mvn spring-boot:build-image` is unaffected by construction; not re-verified by running `mvn`, per the standing migration constraint.

**Verified by the developer** (§7.5–7.9, all Docker-dependent, delegated in full per the developer instruction in §4): all checks pass — the images build under the expected `ghcr.io/attrigo/<service>:<version>` names, the fixed CNB created date is in place, the health-checker-backed compose healthcheck reports healthy, the container runs Java 25, and the full five-service stack comes up healthy with no `docker-compose.yaml` edit. §7.7 is the one that would have caught a dropped buildpack or a missing env var, since `/workspace/health-check` exists only if both took effect — so the buildpack list and its environment are confirmed end to end, not merely configured.

The canonical source of truth for exact behavior is the current state of `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts` and the "Docker images" section of `.claude/rules/gradle.md` on this branch, not this document.

## 12. References

- Spring Boot — [Gradle plugin, Packaging OCI Images](https://docs.spring.io/spring-boot/4.0.5/gradle-plugin/packaging-oci-image.html): `imageName` defaults to `docker.io/library/${project.name}:${project.version}`; `builder` defaults to `paketobuildpacks/builder-noble-java-tiny:latest`; `pullPolicy` defaults to `ALWAYS`; `applicationDirectory` to `/workspace`; `securityOptions` to `["label=disable"]` on Linux/macOS; `buildpacks` overrides the builder's defaults and accepts `[urn:cnb:builder:]<id>` and `[docker://]<host>/<repo>[:tag]` forms; `createdDate` "defaults to a fixed date that enables build reproducibility" and accepts an ISO 8601 instant or `now`; `publish` / `--publishImage` and `docker { publishRegistry { … } }` are the publishing levers.
- Spring Boot — [Maven plugin, `spring-boot:build-image`](https://docs.spring.io/spring-boot/4.0/maven-plugin/build-image.html): "Package an application into an OCI image using a buildpack, **forking the lifecycle to make sure that `package` ran**. This goal is suitable for command-line invocation" — which is why the separate `build-image-no-fork` goal exists for `execution` bindings, and why `release.yml` needs `-DskipTests`. `image.name`, `image.buildpacks`, `image.env`, and `image.createdDate` are the properties this design translates.
- Spring Boot 4.0.5 source — `BootBuildImage.java`: annotated `@DisableCachingByDefault`, declares no outputs, and has **no** `targetJavaVersion` property; `customizeEnvironment` applies only the configured `environment` map, so `BP_JVM_VERSION` is never derived by the plugin. `BootJar.copy()` passes `getTargetJavaVersion().get().getMajorVersion()` into `BootArchiveSupport.configureManifest`, which does `attributes.putIfAbsent("Build-Jdk-Spec", jdkVersion)`.
- Spring Boot — [issue #32829, "Manifests of jars built with Gradle do not have a Build-Jdk-Spec entry"](https://github.com/spring-projects/spring-boot/issues/32829): Maven adds `Build-Jdk-Spec`, Paketo's Java buildpacks read it to choose the JVM major version, and the stated benefit of adding it on the Gradle side was that the build plugins "won't need to set the Paketo-specific `BP_JVM_VERSION` environment variable".
- Paketo — [libjvm issue #350, "BP_JVM_VERSION should have priority over derivation from MANIFEST.MF"](https://github.com/paketo-buildpacks/libjvm/issues/350) (closed): an explicitly set `BP_JVM_VERSION` takes precedence over the manifest-derived value.
- Paketo — [Java Buildpack Reference](https://paketo.io/docs/reference/java-reference/): `BP_JVM_VERSION` configures the JDK/JRE major version installed in the image.
- Cloud Native Buildpacks — [What is build reproducibility?](https://buildpacks.io/docs/for-app-developers/concepts/reproducibility/): timestamps are set to `1980-01-01T00:00:01Z` so images are bit-for-bit reproducible and rebasable; platforms invoking the lifecycle directly can override it with `SOURCE_DATE_EPOCH`.
- Docker — [cli issue #2995, "Improve UX for images created '40 years ago'"](https://github.com/docker/cli/issues/2995): the user-visible consequence of the fixed created date, and the reason a build timestamp is tempting.
- Gradle — [Properties file normalization](https://docs.gradle.org/current/userguide/incremental_build.html#sec:property_file_normalization): cited for why the existing `build-info.properties` / `git.properties` normalization affects only fingerprints, leaving the real `build.time` in the file and on the actuator `/info` endpoint.
