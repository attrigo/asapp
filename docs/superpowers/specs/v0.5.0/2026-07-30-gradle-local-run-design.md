# Gradle local run — design spec

**Date**: 2026-07-30
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate running the app locally to Gradle" (line 24).
**Scope**: Configure the `bootRun` task the Spring Boot plugin already registers on the 5 services so `./gradlew :services:<svc>:bootRun` replaces `cd services/<svc> && mvn spring-boot:run` — the `dev` profile default Maven wired into each POM (`native,dev` for config-service), plus the working directory that makes the local stack actually resolve `central-config/`. Two build blocks, one rules section, two `TODO.md` edits. No new plugin, no catalog entry, no `pom.xml` edit, no application-source or `application.properties` edit.

## 1. Context

Twelve prior subtasks moved the build onto Gradle 9.6.1 / JDK 25. The packaging subtask applied `org.springframework.boot` in `asapp.service-conventions`, which registered `bootRun` (and `bootTestRun`) on all 5 services as a side effect. Those tasks exist and start an application today, but carry no project configuration at all — no profile, no working directory. This subtask configures `bootRun` and nothing else.

**What Maven's local run actually is.** Each service POM configures `spring-boot-maven-plugin` with a `<profiles>` list; everything else is the goal's defaults:

| Maven mechanism | Value | Gradle equivalent | Needs configuring? |
|---|---|---|---|
| `<profiles>` | `dev` (auth, discovery, tasks, users) · `native` + `dev` (config) | no counterpart — the plugin has no profiles DSL | **yes** (§4) |
| `workingDirectory` | unset → `basedir`, i.e. the module directory | `JavaExec.workingDir`, default `project.projectDir` — the same module directory | **yes**, and it is a bug fix (§4) |
| `optimizedLaunch` | `true` | `BootRun.optimizedLaunch`, default `true` | no — parity by default |
| `addResources` | `false` | opt-in `sourceResources(…)` | no — must **not** be called |
| devtools | `optional` dependency, excluded from the repackaged jar | `developmentOnly` → on `runtimeClasspath` (bootRun's classpath), off `bootJar` via `productionRuntimeClasspath` | no — wired in the packaging subtask |
| `fork` | `true` | `JavaExec` always forks | no |

**The `central-config` defect this subtask inherits.** config-service resolves the config server's filesystem backend from the process working directory:

```properties
# services/asapp-config-service/src/main/resources/application.properties:14
spring.cloud.config.server.native.searchLocations=file:///${user.dir}/central-config
```

`central-config/` exists only at the repo root, but Maven runs the application with the *module* directory as its working directory. Measured on this branch (native Windows, `curl -u user:secret http://localhost:8888/asapp-config-service/asapp-users-service/dev`):

| How config-service was launched | `propertySources` returned |
|---|---|
| `cd services/asapp-config-service && mvn spring-boot:run` — the flow every README documents | **`[]`** |
| `mvn spring-boot:run -pl services/asapp-config-service` from the repo root | **`[]`** |
| the same, plus `-Dspring-boot.run.workingDirectory=<repo root>` | `application-dev.properties`, `asapp-users-service.properties`, `application.properties` |

The blast radius is the whole local stack, not just config-service: `asapp.security.jwt-secret` is defined **only** in `central-config/application.properties`, and authentication, tasks and users import the config server non-optionally (`spring.config.import=configserver:…`, no `optional:` prefix), so an empty config server means those three cannot start. The shared `dev` overlay — Swagger, BootUI, `com.attrigo.asapp=DEBUG` — lives only in `central-config/application-dev.properties` and is likewise unreachable. Gradle's `workingDir` default is the same module directory, so a literal migration reproduces the defect silently.

**Current Gradle state.** `asapp.service-conventions` (5 services) applies the Boot plugin, disables the plain `jar`, configures `springBoot.buildInfo`, declares the runtime-classpath normalization, and registers `integrationTest` plus the two extra JaCoCo reports. `services/asapp-config-service/build.gradle.kts` holds only a `plugins {}` block and one dependency. `gradle.properties` has `org.gradle.caching=true`, `org.gradle.parallel=false` (WSL), configuration cache deferred but intended.

**Convention-plugin hierarchy**: `java-conventions` (all 7) ← `library-conventions` (2 libs) and `service-conventions` (5 services, applied directly by config + discovery) ← `domain-service-conventions` (3 domain services).

## 2. Goals

- **One command per service**: `./gradlew :services:<svc>:bootRun`, invoked from the repo root, replaces `cd services/<svc> && mvn spring-boot:run`.
- **Profile parity**: `dev` active by default on all five, `native,dev` on config-service — the same lists the POMs wire today.
- **A documented override** for any other posture, replacing the root README's `-Dspring-boot.run.profiles=…`.
- **A local stack that actually runs**: config-service serves `central-config/`, so the three config-server clients start and receive the `dev` overlay.
- **Configuration-cache and Isolated-Projects safe**: no cross-project references, no `subprojects {}` / `allprojects {}`, no `rootProject` access.
- **Correct altitude**: convention plugin for the 5-module policy, the service's own script for its per-service data.
- Zero `pom.xml` edits, zero application-source edits; `mvn spring-boot:run` keeps working (as well as it ever did) until the removal subtask.

## 3. Non-goals

- **README migration.** The five service READMEs and the root README still document `mvn spring-boot:run`, and one of them documents it *wrongly* — §5 hands both to "Migrate build documentation to Gradle" (line 45) as a note, the same way every prior subtask in this migration handed over its doc debt.
- **Docker image building** (line 25), **database migration commands** (line 26) — the next two subtasks.
- **Running the whole stack with one command.** Maven had no such thing; a task that starts five blocking JVMs is not something Gradle models well, and the terminal-per-service flow is verified to work (§7).
- **`bootTestRun`.** Registered by the plugin, no Maven counterpart in use, left untouched.
- **Remote-debug configuration.** `--debug-jvm` is a built-in `JavaExec` option; it is documented, not configured.
- **devtools tuning** (`restart.additional-paths`, LiveReload, `.spring-boot-devtools.properties`) — Maven configured none of it.
- **Fixing `${user.dir}` in `application.properties`** — the working directory is set build-side (§4); the property file is not touched.
- **The `docker` overlay** (`file:///central-config`) — correct as-is, out of scope.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Profile default lives in the build | **`systemProperty("spring.profiles.active", …)` on `bootRun`** | Spring's how-to *Set the Active Spring Profiles* names exactly two levers — "you would normally set a System property (`spring.profiles.active`) or an OS environment variable (`SPRING_PROFILES_ACTIVE`)" — and never mentions build-plugin configuration. The Gradle plugin has no `<profiles>` counterpart and never will: [spring-boot#832](https://github.com/spring-projects/spring-boot/issues/832) asked for it, including a literal `bootRun { profile: development }` DSL, and was closed as a duplicate with no DSL added. Baking the default preserves the one guarantee the POM gave: config-service can never boot without `native`. |
| Override mechanism | **`--args='--spring.profiles.active=…'`, documented, not configured** | The Boot Gradle plugin's *Passing Arguments to your Application* section documents this exact form with this exact example. It outranks the default by Spring's own precedence table — command-line arguments sit at **#11**, Java system properties at **#6** — and it is shell-neutral, which matters where PowerShell, git-bash and WSL are all in use. |
| Rejected: program-argument default | **Not `args("--spring.profiles.active=dev")`** | Mechanically what Maven's `<profiles>` does, but Gradle's `--args` *replaces* the argument list wholesale, so an unrelated override (`--args='--server.port=9999'`) would silently drop the profile. The docs use `--args` for ad-hoc profiles, never for a baked-in default. |
| Rejected: `-P` bridge | **No `providers.gradleProperty("spring.profiles.active")` wiring** | The plugin docs' `systemProperty(name, findProperty(…) ?: default)` pattern is real, and Gradle's configuration-cache guidance would spell it `providers.gradleProperty(…)` rather than `findProperty`. But `--args` already covers every override, and two documented ways to set one value is the kind of surface this migration has otherwise avoided. **Revisit trigger**: a second run-time value ever needing a build-supplied, CLI-overridable default. |
| Rejected: `SPRING_PROFILES_ACTIVE` default | **Not `environment(…)`** | Spring's other named lever, and the one the repo already documents for jars and the Docker stack. Rejected on ergonomics only: the override becomes shell-specific (`$env:SPRING_PROFILES_ACTIVE='…';` in PowerShell vs. an inline assignment in bash), and an explicitly-set `environment(…)` entry overrides the inherited one anyway, so it would need a `providers.environmentVariable(…)` bridge to behave as developers expect. |
| config-service restates `dev` | **`"native,dev"`, not an append** | `spring.profiles.active` **replaces** rather than adds — the same trap the config-service README already calls out ("never `dev` or `prod` alone"). `spring.profiles.include` would add rather than replace, but it belongs in application config, not in a build script reaching around it. The list is short, single-site, and carries a comment. |
| Profile altitude | **`dev` in `asapp.service-conventions` (5); `native,dev` in `services/asapp-config-service/build.gradle.kts`** | "Every service runs with dev tooling locally" is archetype policy — all five READMEs state it. The `native` backend profile is per-service data, and lands where the per-service pitest globs land. |
| Working directory | **`workingDir = layout.settingsDirectory.asFile` on `bootRun`** | The measured fix (§1). It restores what `${user.dir}/central-config` was always meant to mean without touching application config, and it is the direct analog of the `-Dspring-boot.run.workingDirectory=<repo root>` invocation proven to work. |
| Working-directory altitude | **All 5, in `asapp.service-conventions`** | Developer decision. "The local run's working directory is the repo root" is a run-policy statement about the archetype, not data about config-service; config-service merely happens to be the module that currently depends on it. It also matches where `./gradlew` is invoked from, so the app's cwd and the developer's cwd agree — an invariant no future service can accidentally break. |
| `layout.settingsDirectory`, not `rootDir` | **`layout.settingsDirectory.asFile`** | `ProjectLayout.getSettingsDirectory()` (Gradle 8.13+) is "the directory containing the settings file … shared by all projects in the build" — the Isolated-Projects-safe spelling of `rootDir`, which is defined as `rootProject.projectDir` and therefore reaches across projects. It resolves to the repo root because `settings.gradle.kts` sits there. The repo's existing `rootProject.file(…)` uses (Spotless config) are the older idiom and are not touched here. |
| `optimizedLaunch` | **Left at the default** | Gradle's default is `true`; the Maven goal's default is also `true`. Parity by inaction — writing it down would only imply it was a choice. |
| `sourceResources` | **Never called** | Maven's `addResources` defaults to `false`, so live resource editing was never on. Calling `sourceResources(sourceSets["main"])` would be a new behavior, not a migration. devtools already restarts on classpath change, which is what the project actually relies on. |
| devtools | **Nothing to do** | The Boot plugin makes `runtimeClasspath` extend `developmentOnly` and derives `productionRuntimeClasspath` as "`runtimeClasspath` minus any dependencies that only appear in `developmentOnly`", which `bootJar` packages. So devtools is on `bootRun`'s classpath and out of the jar — Maven's `excludeDevtools` behavior, already shipped in the packaging subtask. |
| Recompile loop | **Documented, not configured** | Maven offered nothing here; Gradle does: `./gradlew :services:<svc>:classes --continuous` in a second terminal recompiles on save, which devtools observes and restarts on. One rules line, no build code. |
| `bootTestRun` | **Untouched** | Configuring `bootRun` by name leaves it alone by construction. It has no Maven counterpart in use, and giving it a `dev` profile would be inventing policy. |
| `bootRun` and the umbrella | **Stays out of `check` / `build` / `fullBuild`** | Already the standing rule from the full-build subtask; a blocking task in a lifecycle graph is a hang, not a build. |

## 5. Changes by file

**`build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`** (5 services) — add the import alongside the existing ones and the block after the `normalization { }` block. Exact block ordering is deferred to "Clean Gradle files" (line 47).

```kotlin
import org.springframework.boot.gradle.tasks.run.BootRun
```

```kotlin
// Local run (the "mvn spring-boot:run" equivalent); override the profiles with --args.
tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
    // The repo root, so config-service's "file:///${user.dir}/central-config" resolves.
    workingDir = layout.settingsDirectory.asFile
}
```

`tasks.named<BootRun>("bootRun")` is safe at this point for the same reason the file's existing `tasks.named<Jar>("jar")` is: the Boot plugin is applied in this script's own `plugins {}` block, so the task is registered before the body runs.

**`services/asapp-config-service/build.gradle.kts`** — add the import and the override.

```kotlin
import org.springframework.boot.gradle.tasks.run.BootRun
```

```kotlin
// The config server's filesystem backend needs "native"; the list replaces rather than adds, so "dev" is re-listed.
tasks.named<BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "native,dev")
}
```

**No other build file changes.** Core Gradle plus the already-applied Boot plugin — no `gradle/libs.versions.toml` entry, no `build-logic/build.gradle.kts` classpath addition, no root `build.gradle.kts` edit.

**`.claude/rules/gradle.md`** — add a **Running locally** section documenting: `./gradlew :services:<svc>:bootRun` replaces `cd services/<svc> && mvn spring-boot:run`, invoked from the repo root; the profile default is a `systemProperty` on `bootRun` in `asapp.service-conventions` (`dev`, 5 services) overridden to `native,dev` in `services/asapp-config-service/build.gradle.kts`, because `spring.profiles.active` replaces rather than adds and the config server's filesystem backend needs `native`; the Boot Gradle plugin has **no** `<profiles>` counterpart and none is coming (spring-boot#832 closed with no DSL), so the system property is the documented lever — never `args("--spring.profiles.active=…")`, because Gradle's `--args` replaces the whole argument list and would silently drop the default; overrides use `--args='--spring.profiles.active=…'` (command-line arguments #11 beat system properties #6 in Spring's precedence order) and no `-P` bridge is wired, with the revisit trigger being a second build-supplied, CLI-overridable run-time value; `workingDir = layout.settingsDirectory.asFile` on all 5 services because Gradle defaults `JavaExec.workingDir` to `project.projectDir` and config-service resolves `file:///${user.dir}/central-config`, which exists only at the repo root — measured: module-directory cwd returns **zero** property sources from `/asapp-<svc>/dev` while repo-root cwd returns all three, and since `asapp.security.jwt-secret` lives only in `central-config/application.properties` and the three domain services import the config server non-optionally, the module-directory default takes the whole local stack down; `layout.settingsDirectory` rather than `rootDir` (Gradle 8.13+, Isolated-Projects safe, `rootDir` is defined as `rootProject.projectDir`); `optimizedLaunch` left at its `true` default because Maven's is `true` too; **never** call `sourceResources(…)` (Maven's `addResources` defaults to `false`); devtools needs no wiring — the Boot plugin makes `runtimeClasspath` extend `developmentOnly` and packages `productionRuntimeClasspath`, so it is on `bootRun`'s classpath and out of the jar; the auto-recompile loop is `./gradlew :services:<svc>:classes --continuous` in a second terminal (Maven had no equivalent); `--debug-jvm` replaces `-Dspring-boot.run.jvmArguments=-agentlib:jdwp=…`; `bootTestRun` is deliberately left unconfigured; `bootRun` stays out of `check` / `build` / `fullBuild`; and concurrent `bootRun` invocations in this repo are verified safe — config-service and discovery-service ran together on Gradle 9.6.1 with separate daemons, zero cache-lock timeouts, both healthy.

**`TODO.md`** — two edits:

1. Check off "Migrate running the app locally to Gradle" (line 24).
2. Add under "Migrate build documentation to Gradle" (line 45) a note carrying this subtask's documentation debt:

```markdown
        - **Note:** the local-run commands become `./gradlew :services:<svc>:bootRun` from the repo root, with no `cd` — so the "Run the service from the module directory" instruction must not survive the rewrite, since a module-directory working directory is exactly what leaves the config server serving nothing; the root README's `-Dspring-boot.run.profiles=…` override line becomes `--args='--spring.profiles.active=…'`, and config-service's `native,dev` list stays called out
```

A note rather than a subtask of its own (developer decision, after weighing the alternative). The wrong instruction is not separable work: the Gradle run command has no `cd` at all, so it disappears as a side effect of the ordinary command translation, and a separate entry would own nothing line 45 does not already have to edit — in the same README sections, beside the `mvn clean install` lines it must rewrite anyway. It also keeps the precedent every prior subtask in this migration set for its own doc debt, and keeps the subtask list from growing an eighteenth item for a few lines of prose. The note is worded so the defect reads as a defect, not as a stale command.

## 6. Placement / altitude rationale

- **Profile default + working directory → `asapp.service-conventions` (5).** Both are statements about how *a Spring Boot service in this repo* runs locally, the same altitude that already owns the Boot plugin, devtools, the disabled plain jar, and build-info.
- **`native,dev` → `services/asapp-config-service/build.gradle.kts` (1).** Per-service data, alongside where the per-service pitest globs live. The convention plugin should not know that one of its five members has a filesystem backend.
- **Nothing → `asapp.domain-service-conventions`.** The three domain services run exactly like the two infra ones.
- **Nothing at the root.** No aggregation, no cross-project reference.

## 7. Verification / Definition of Done — run by the developer

Implementation does **not** execute these; the local stack needs Docker, several terminals, and a human watching startup logs. The developer runs them and records the outcome in §11.

1. **Config server serves `central-config` — the regression test for the defect.** In one terminal `./gradlew :services:asapp-config-service:bootRun`; then
   `curl -u user:secret http://localhost:8888/asapp-config-service/asapp-users-service/dev`
   must return **three** `propertySources` (`application-dev.properties`, `asapp-users-service.properties`, `application.properties`), not `[]`.
2. **Profiles are what the POMs wired.** config-service's startup log reads `The following 2 profiles are active: "native", "dev"`; every other service reads `The following 1 profile is active: "dev"`.
3. **The stack comes up.** config → discovery → `docker-compose up -d asapp-users-postgres-db` → `./gradlew :services:asapp-users-service:bootRun`, each in its own terminal. users-service reaching `UP` on `http://localhost:8092/asapp-users-service/actuator/health` proves the JWT secret and the `dev` overlay arrived over the config server — the check that fails today.
4. **The `dev` overlay is live.** `http://localhost:8082/asapp-users-service/swagger-ui.html` responds; Swagger is disabled in the base config and enabled only by `central-config/application-dev.properties`.
5. **The override works.** `./gradlew :services:asapp-users-service:bootRun --args='--spring.profiles.active=prod'` logs `"prod"`, and Swagger stops responding.
6. **devtools still restarts.** With users-service running, `./gradlew :services:asapp-users-service:classes --continuous` in a second terminal; touching a main source file triggers a devtools restart in the first.
7. **No collateral damage.** `./gradlew build` behaves as before — `bootRun` appears in no lifecycle graph (`./gradlew :services:asapp-users-service:build --dry-run` lists no `bootRun`).
8. **Maven untouched**: no `pom.xml` and no application source edited, so `mvn spring-boot:run` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

## 8. Out of scope / YAGNI

README and root-README migration (line 45) · Docker image building (line 25) · database migration commands (line 26) · a one-command whole-stack task · `bootTestRun` · `sourceResources` / live resource reloading · devtools tuning · remote-debug configuration · a `-P` profile flag · `SPRING_PROFILES_ACTIVE` plumbing · editing `application.properties` or the `docker` overlay · any `pom.xml` or application-source edit.

## 9. Contingencies

- **`layout.settingsDirectory` unavailable in a precompiled script plugin.** Fall back to `rootDir` (`rootProject.projectDir`), the idiom the repo already uses for Spotless config, and note the Isolated-Projects debt in `gradle.md`. Not expected on Gradle 9.6.1.
- **A service turns out to need the module directory as its cwd.** Override `workingDir` in that service's own build script, exactly as config-service overrides the profile list. Do not weaken the shared policy.
- **Profiles lost across a devtools restart.** They cannot be — a restart reuses the JVM, and the system property outlives it. If it somehow happens, switch that service to `args("--spring.profiles.active=…")` and accept the `--args` fragility for it alone rather than moving everything.
- **Windows Ctrl+C leaves the application JVM alive.** A known rough edge of `gradlew.bat` under a console that intercepts Ctrl+C, not something a build script can fix. If observed, document the fallback (stop the process listening on the service port; `./gradlew --stop` kills daemons, not the forked app) in the rules section rather than working around it in the build.
- **Concurrent `bootRun`s contend on Gradle's caches.** Measured during design and recorded in the rules section (§5) as not happening here, but each concurrent build spawns its own daemon; if memory pressure shows up with three or more services, cap it via the user-level Gradle home — never by committing a daemon setting that penalizes every build.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-13-run-app`. A single commit:

1. `build(gradle): migrate running the app locally to Gradle` — the `bootRun` block in `asapp.service-conventions`, the config-service override, the `.claude/rules/gradle.md` "Running locally" section, and the two `TODO.md` edits.

Following this migration's established pattern, implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec was written before implementation. The change shipped **exactly as designed**, with no deviations: `asapp.service-conventions` carries `systemProperty("spring.profiles.active", "dev")` plus `workingDir = layout.settingsDirectory.asFile` in one `tasks.named<BootRun>("bootRun")` block, and `services/asapp-config-service/build.gradle.kts` overrides the list to `"native,dev"`. `layout.settingsDirectory` resolved without incident on Gradle 9.6.1, so the §9 `rootDir` fallback was not needed. No catalog, `build-logic` classpath, `pom.xml`, or application-source edit.

**Verified during implementation** — the parts that need neither Docker nor multiple terminals, both against `:services:asapp-config-service:bootRun`:

- **§7.1, the regression test:** `/asapp-config-service/asapp-users-service/dev` returns three property sources — `application-dev.properties`, `asapp-users-service.properties`, `application.properties` — where the same query returned `[]` under every documented Maven invocation. The defect is closed.
- **§7.2, profiles:** the startup log reads `The following 2 profiles are active: "native", "dev"`, so the convention-plugin default and the per-service override compose as intended.
- **§7.7, no collateral damage:** `./gradlew :services:asapp-users-service:build --dry-run` mentions `bootRun` **zero** times — it is in no lifecycle graph.

**Verified by the developer** (§7.3–7.6, §7.8): the full three-service stack against PostgreSQL and Redis, the `dev` overlay reaching users-service over the config server, the Swagger check, the `--args` override, and the devtools `--continuous` recompile loop all pass. §7.3 — users-service reaching `UP`, which could not pass before this change — is confirmed, so the `central-config` defect is closed end to end and not merely at the config-server boundary.

The §9 Windows Ctrl+C rough edge did not materialize during that run and needs no rules entry.

The canonical source of truth for exact behavior is the current state of `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts`, `services/asapp-config-service/build.gradle.kts`, and the "Running locally" section of `.claude/rules/gradle.md` on this branch, not this document.

## 12. References

- Spring Boot — [Gradle plugin, Running your Application](https://docs.spring.io/spring-boot/4.0/gradle-plugin/running.html): `bootRun` is a `JavaExec` subclass using the main source set's runtime classpath; arguments are passed with `--args='<arguments>'` (Gradle 4.9+), the documented example being `./gradlew bootRun --args='--spring.profiles.active=dev'`; build-supplied system properties use `systemProperty(name, findProperty(…) ?: "default")` with `-P`; `optimizedLaunch` defaults to optimizing startup; `sourceResources(sourceSets["main"])` is opt-in; `bootTestRun` is registered alongside.
- Spring Boot — [How-to, Set the Active Spring Profiles](https://docs.spring.io/spring-boot/4.0/how-to/properties-and-configuration.html): "you would normally set a System property (`spring.profiles.active`) or an OS environment variable (`SPRING_PROFILES_ACTIVE`)". No build-tool mechanism is named.
- Spring Boot — [Reference, Externalized Configuration](https://docs.spring.io/spring-boot/4.0/reference/features/external-config.html): the precedence list — OS environment variables (#5), Java system properties (#6), command-line arguments (#11). The basis for `--args` overriding a `systemProperty` default.
- Spring Boot — [Reference, Profiles](https://docs.spring.io/spring-boot/4.0/reference/features/profiles.html): `spring.profiles.active` replaces rather than adds; `spring.profiles.include` is the additive form.
- Spring Boot — [Gradle plugin, Reacting to Other Plugins](https://docs.spring.io/spring-boot/4.0/gradle-plugin/reacting.html): `developmentOnly` is "for dependencies that are only required at development time … and should not be packaged in executable jars and wars"; `productionRuntimeClasspath` "is equivalent to `runtimeClasspath` minus any dependencies that only appear in the `developmentOnly` or `testDevelopmentOnly` configurations".
- Spring Boot — [Maven plugin, `spring-boot:run`](https://docs.spring.io/spring-boot/4.0/maven-plugin/run.html): `workingDirectory` — "Current working directory to use for the application. If not specified, basedir will be used"; `addResources` defaults to `false`; `optimizedLaunch` defaults to `true`; `profiles` is "a convenience shortcut of specifying the 'spring.profiles.active' argument".
- Spring Boot — [issue #832, "It should be easier to set which profile is active when running `gradle` tasks"](https://github.com/spring-projects/spring-boot/issues/832): closed as a duplicate with no DSL added; a follow-up request for `bootRun { profile: development }` was explicitly separated by the maintainers from command-line activation, and the answers that stuck in the thread are `SPRING_PROFILES_ACTIVE=dev gradle bootRun` and `./gradlew bootRun --args='--spring.profiles.active=local'`.
- Gradle — [`JavaExec` DSL reference](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.JavaExec.html): `workingDir` defaults to `project.projectDir`; `--debug-jvm` is a built-in command-line option.
- Gradle — [`ProjectLayout.getSettingsDirectory()`](https://docs.gradle.org/current/javadoc/org/gradle/api/file/ProjectLayout.html): since Gradle 8.13, "the directory containing the settings file. It is shared by all projects in the build."
- Gradle — [Configuration cache requirements](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html): project properties should be read through `providers.gradleProperty(…)` rather than `project.findProperty(…)`. Cited for the rejected `-P` bridge — if it is ever wired, this is the spelling.
