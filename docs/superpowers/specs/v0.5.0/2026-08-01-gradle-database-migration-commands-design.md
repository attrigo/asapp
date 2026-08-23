# Gradle database migration commands — design spec

**Date**: 2026-08-01
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate database migration commands to Gradle" (line 26).
**Scope**: Apply the official `org.liquibase.gradle` plugin in `asapp.domain-service-conventions` (the 3 domain services) so `./gradlew :services:<svc>:liquibaseUpdate` / `liquibaseUpdateSql` / `liquibaseRollbackCount` replace `mvn liquibase:update` / `updateSQL` / `rollback`. One catalog block, two `build-logic` classpath entries, one convention-plugin block, one URL override per service script, one `gradle.properties` key, one rules section, four `TODO.md` edits. No `pom.xml` edit, no README edit, no application-source edit, no changelog-file edit.

## 1. Context

Fourteen prior subtasks moved the build onto Gradle 9.6.1 / JDK 25. This one migrates the **developer** database workflow — the only Liquibase surface Maven exposes as a command.

**What Maven's Liquibase wiring actually is.** A `pluginManagement` block in `services/pom.xml:384-394`, re-declared without configuration in the three domain-service poms (`asapp-authentication-service/pom.xml:245`, `asapp-tasks-service/pom.xml:235`, `asapp-users-service/pom.xml:280`). It is **bound to no phase**, so nothing in `mvn install` / `verify` invokes it — it exists purely for `mvn liquibase:<goal>` on the command line. Configuration comes from `<propertyFile>` plus three per-service `mvn-liquibase.properties` files.

| Maven mechanism | Value | Gradle equivalent | Needs configuring? |
|---|---|---|---|
| `<changeLogFile>` | `src/main/resources/liquibase/db/changelog/db.changelog-master.xml` | `changelogFile` activity argument | **yes, but reshaped** (§4) |
| — (implicit: Maven's basedir search path) | project basedir | `searchPath` activity argument | **yes — new, and load-bearing** (§4) |
| `url` (property file) | `jdbc:postgresql://localhost:{5432,5433,5434}/{authentication,tasks,users}db` | `url` activity argument | **yes, per service** (§4) |
| `username` / `password` (property file) | `user` / `secret` | `username` / `password` activity arguments | **yes, shared** (§4) |
| `<migrationSqlOutputFile>` | `src/main/resources/liquibase/output/migrationSqlOutput.sql` | `outputFile` activity argument | **no — deliberately dropped** (§4) |
| `<outputFileEncoding>` | `UTF-8` | `outputFileEncoding` | **no** — already the Liquibase default |
| `promptForNonLocalDatabase` (property file) | `true` | — | **no — no analog, and a no-op today** (§4) |
| `mvn liquibase:rollback -Dliquibase.rollbackCount=1` | — | `liquibaseRollbackCount -PliquibaseCount=1` | **yes — the goal splits into three CLI commands** (§4) |
| the other ~38 Liquibase goals | available, undocumented | 41 generated tasks | no — free with the plugin |

**What is *not* in scope, by construction.** Each service also runs Liquibase at boot through `spring.liquibase.change-log=liquibase/db/changelog/db.changelog-master.xml` (`application.properties:23`, plus a `docker` context in `application-docker.properties`). That path is Spring's, not the build tool's, and is untouched here — but §4 shows it is not *unrelated*, because both paths write to the same `DATABASECHANGELOG` table.

**Why rollback is the reason this subtask exists.** `.claude/rules/liquibase.md` requires every changeset to carry a `<rollback>` block. `liquibase:rollback` is the only tool that ever exercises them. Losing it would mean the rule is enforced by review alone.

**Six findings from a throwaway Gradle 9.6.1 build**, run before this design was written. The plugin's own documentation covers none of them.

1. **The plugin works with Liquibase 5.0.2.** Spring Boot 4.0.5 manages `liquibase.version` = **5.0.2**; the plugin documents support only through 4.31.1. Applied against 5.0.2 it discovers and registers **41** command tasks correctly.
2. **It is configuration-cache compatible.** `./gradlew status --configuration-cache` → `Configuration cache entry stored.` This matters because the configuration cache is deferred but intended (`gradle.properties:5`), and it confirms the plugin will not become the second blocker after asciidoctor.
3. **It introduces a Gradle-10 blocker.** `The Project.container(Class, Closure) method has been deprecated. This is scheduled to be removed in Gradle 10.` — emitted from the plugin's `LiquibasePlugin.doApplyExtension`, on *every* invocation including `./gradlew help`. Upstream [issue #181](https://github.com/liquibase/liquibase-gradle-plugin/issues/181), open since 2025-12-25, no fix, no branch.
4. **It needs Liquibase on the apply-time buildscript classpath.** Since plugin 3.0.0 the task list is derived from Liquibase's `CommandFactory` inside `apply()`, so without it the plugin dies with `NoClassDefFoundError: liquibase/Scope`. Upstream [issue #182](https://github.com/liquibase/liquibase-gradle-plugin/issues/182), closed with exactly this resolution.
5. **A Maven-parity relative changelog path does not work at all.** The default search path is the **invocation** directory, not the module:
   ```
   The file src/main/resources/liquibase/db/changelog/db.changelog-master.xml was not found
   in the configured search path:  - C:\dev\repos\ttrigo\asapp-replace-maven-with-gradle
   ```
   The plugin's usage doc gestures at this with a `jvmArgs "-Duser.dir=$project.projectDir"` workaround for "a subproject structure"; `searchPath` is the Liquibase-native fix for the same problem (§4).
6. **`picocli` is required and arrives from nowhere.** `liquibase-core` declares it `<optional>true</optional>`, so it is not transitive, and the Spring Boot BOM does not manage it. Without it: `NoClassDefFoundError: picocli/CommandLine$IFactory`. Contrast the plugin doc's other suggested extras — logback and `jaxb-api` — which are **not** needed (jaxb-api is a non-optional transitive of `liquibase-core` 5.0.2, and the CLI ran clean without logback).

**The finding that changes behavior: recorded changeset paths.** Liquibase records each changeset's identity as its changelog path *relative to the search path*, and that string is the `FILENAME` column of `DATABASECHANGELOG`. Both forms were run against an in-memory database and the recorded path read off the resulting log line:

| Search path | Changelog argument | Recorded as | Same as the running app? |
|---|---|---|---|
| module basedir (**what Maven does**) | `src/main/resources/liquibase/db/changelog/db.changelog-master.xml` | `src/main/resources/liquibase/db/changelog/v0.2.0/changesets/…` | **no** |
| `src/main/resources` (**this design**) | `liquibase/db/changelog/db.changelog-master.xml` | `liquibase/db/changelog/v0.2.0/changesets/…` | **yes** |

So today the Maven CLI and the running service disagree about changeset identity: a changeset applied by the booting app is invisible to `mvn liquibase:rollback`, and vice versa. Anchoring `searchPath` at `src/main/resources` is required anyway (finding 5) and closes that gap as a side effect.

**Current Gradle state.** `asapp.domain-service-conventions` (3 domain services) applies `asapp.service-conventions`, pitest, and asciidoctor; carries the shared pitest block, 21 dependency coordinates, the asciidoctorExt configuration, the snippets wiring, javadoc/sources jars, and the `fullBuild` widening. `build-logic/settings.gradle.kts` already declares `gradlePluginPortal()` — load-bearing here, because plugin 3.1.0 is published to the Plugin Portal only (Maven Central stops at 3.0.2).

**Convention-plugin hierarchy**: `java-conventions` (all 7) ← `library-conventions` (2 libs) and `service-conventions` (5 services, applied directly by config + discovery) ← `domain-service-conventions` (3 domain services).

## 2. Goals

- **Command parity for the three documented operations**: `liquibaseUpdate`, `liquibaseUpdateSql`, `liquibaseRollbackCount` replace `mvn liquibase:update` / `updateSQL` / `rollback -Dliquibase.rollbackCount=N`.
- **Rollback keeps working**, so the `<rollback>` blocks every changeset is required to carry remain executable.
- **The CLI runs the same Liquibase as the application** — one BOM-managed version, so checksums and changelog parsing cannot diverge between a developer's `liquibaseUpdate` and the service's boot-time migration.
- **The CLI and the running service agree on changeset identity**, closing the `DATABASECHANGELOG` mismatch described in §1.
- **Correct altitude**: shared configuration in the convention plugin that owns the 3-domain-service archetype, only the per-service URL in the service scripts.
- Zero `pom.xml` edits — `mvn liquibase:*` keeps working until the removal subtask.

## 3. Non-goals

- **Retiring Maven's Liquibase wiring.** The `pluginManagement` block, the three per-service plugin declarations, and the three `mvn-liquibase.properties` files all stay. **Developer decision**, matching the coexistence pattern every prior subtask followed: Maven is left intact and Gradle added alongside, so a working fallback exists while parity is confirmed. Handed to "Verify full parity, then remove Maven entirely" (line 74) with a note (§5).
- **Documentation.** The Database Management sections of the three service READMEs still show `mvn liquibase:*`. **Developer decision** — handed to "Migrate build documentation to Gradle" (line 47) as a note, the way every prior subtask handed over its doc debt.
- **The boot-time migration.** `spring.liquibase.*` in `application.properties` / `application-docker.properties` is Spring's mechanism, not the build's. Untouched.
- **The changelogs themselves.** No changeset, no `db.changelog-master.xml`, no `logicalFilePath` edit. Making them database-agnostic is a separate Backlog item.
- **The `docker` context.** Seed-data changesets carry `context="docker"` and are skipped by a context-less CLI run — exactly as under Maven, which set no contexts either.
- **Test-tier Liquibase.** The integration tier runs migrations through Testcontainers and Spring; nothing here touches it.
- **Git hook installation** (line 31), **the CI workflow** (line 33) — the next two subtasks.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Plugin vs hand-rolled tasks | **The official `org.liquibase.gradle` plugin, 3.1.0** | **Developer decision**, taken with the Gradle-10 cost below on the table. A hand-rolled alternative (one `liquibaseRuntime` configuration plus 3–5 `JavaExec` tasks, ~35 lines) was costed: it is Gradle-10-safe today, adds nothing to the shared buildscript classpath, and needs only one Liquibase version — but it is unmaintained by anyone but this repo, and covers only the commands written by hand. The plugin is Liquibase-maintained, and a version bump brings upstream fixes. |
| Accepted cost: a Gradle-10 deprecation | **Every build now warns `incompatible with Gradle 10`** | Finding 3. Not fixable here — it is one `Project.container(Class, Closure)` call inside the plugin. Consequence recorded against "Review Deprecated Gradle features" (line 62) in §5, because that subtask can no longer reach zero locally. Today it is a warning line; it becomes a hard break only when Gradle 10 ships. Upstream is responsive (#182 was answered in two days) and the fix is a one-line API swap. |
| Accepted cost: Liquibase on the shared buildscript classpath | **`implementation(libs.liquibase.core)` in `build-logic`** | Finding 4 — required, not optional. `build-logic` has one classpath, so `liquibase-core` and its seven transitives land on the buildscript classpath of all 7 modules, not just the 3 that use it. No narrower placement exists short of abandoning convention plugins for per-module `buildscript {}` blocks, which the `## Shared Build Configuration` rule forbids. |
| Two Liquibase versions | **Catalog-pinned `5.0.2` for apply time; BOM-managed (versionless) for `liquibaseRuntime`** | They serve different purposes and only one can be BOM-managed. `build-logic` imports no Boot BOM, so its copy must be pinned literally. The **runtime** copy must stay versionless so the CLI always runs precisely the Liquibase the application runs — the correctness half of the decision. Drift between them only skews command discovery; drift between the runtime copy and the app would skew checksums. The catalog pin carries a comment naming its obligation, and re-checking it belongs to the Boot upgrade in 0.6.0 (§5). |
| `searchPath` | **`layout.projectDirectory.dir("src/main/resources").asFile.path`** | Finding 5 — without it nothing resolves at all. Anchored at `src/main/resources` rather than `build/resources/main` so the tasks read the sources the developer edits, need no `processResources` dependency, and can never operate on a stale copy. |
| `changelogFile` | **`liquibase/db/changelog/db.changelog-master.xml`** — classpath-shaped, not filesystem-shaped | Follows from `searchPath`. The payoff is the §1 table: recorded changeset identity becomes byte-identical to what the booting service writes, so the two paths finally share one `DATABASECHANGELOG`. |
| Consequence of that | **A dev database migrated by `mvn liquibase:update` reads as un-migrated** | Its rows carry the `src/main/resources/…` prefix. Recreate the database (or `liquibaseDropAll`) once. A database migrated by *booting the service* — the normal path — needs nothing, since it already carries the new form. |
| Config split | **Shared arguments in the convention plugin; the URL in each service script** | **Developer decision.** Closest to documented practice: the plugin's own examples show *one* activity whose varying values are supplied per build, and state explicitly that "the database credentials are driven by build properties … so that you don't need a separate activity for each database". It also matches how this repo already splits pitest — six shared settings in the convention plugin, `targetClasses`/`targetTests` per service. |
| Cost of that split | **One `@Suppress("UNCHECKED_CAST")` per service script** | `Activity.arguments` is an untyped Groovy `def` populated via `methodMissing`, so Kotlin sees `Any?` and mutating one key needs a cast. Three lines of wart, against duplicating `password = "secret"` into three files. |
| `logLevel` | **Not set — the tasks run at Liquibase's default of `OFF`** | Assigning the whole `arguments` map wipes the `[logLevel: 'info']` that `Activity` seeds, so this is a real choice rather than inaction. Both were tried; the quiet one won. Nothing useful is lost — the update summary, the success line, and errors are CLI output, not logging — and `-PliquibaseLogLevel=info` restores it per run. |
| Task naming | **`liquibaseTaskPrefix=liquibase` in root `gradle.properties`** | **Developer decision.** The plugin otherwise registers 41 generic top-level names (`update`, `status`, `tag`, `diff`, `validate`, `snapshot`, `history`, `dropAll`, `executeSql`, `lpm`, …) in 3 of 7 modules, where a bare `./gradlew update` would fan out to all three databases through the task selector. The property is **undocumented** — it exists only in `LiquibasePlugin.applyTasks` — but is read through `project.hasProperty`, works from `gradle.properties` (verified), and would fail loudly (unknown task) rather than silently if ever removed. |
| `rollback` → three tasks | **`liquibaseRollbackCount -PliquibaseCount=1`** | Maven's single `rollback` goal took `rollbackCount` / `rollbackTag` / `rollbackDate`; the Liquibase CLI splits these into `rollback-count`, `rollback` (tag), and `rollback-to-date`, so the plugin generates three tasks. `-Pliquibase<Arg>` is the plugin's documented override mechanism; verified end to end (`Liquibase command 'rollback-count' was executed successfully`). |
| No default `count` | **`count` is not set in the activity** | Parity with Maven, which also required the value explicitly. Setting it would also silently give `liquibaseUpdateCount` and `liquibaseFutureRollbackCountSql` the same default, since activity arguments apply to every command that accepts them. |
| `migrationSqlOutputFile` | **Dropped — `liquibaseUpdateSql` prints to stdout** | **Deliberate divergence.** `outputFile` lives on the *activity*, not on a command, so a fixed path would reach every command that accepts one (`snapshot`, `diff`, `generateChangelog`, the `*Sql` family), several of which do not emit SQL. Reading the SQL is the point of a dry run anyway, and `-PliquibaseOutputFile=…` covers the ad-hoc case. Nothing sensible is lost: Maven's own default was `target/liquibase/migrate.sql`, and the pom's override pointed into `src/main/resources`, where `processResources` would have copied any generated file into the jar. |
| `outputFileEncoding` | **Dropped** | UTF-8 is Liquibase's default, and the Maven plugin declares no default of its own — so the pom was writing a default down, not configuring anything. Moot regardless once `outputFile` is gone. |
| `promptForNonLocalDatabase` | **Dropped** | A `liquibase-maven-plugin` parameter with no CLI or Gradle analog, and a no-op today: all three URLs are `localhost`, so the prompt it guards never fires. |
| `liquibaseRuntime` contents | **`liquibase-core` + `postgresql` versionless, `picocli` from the catalog** | The first two are BOM-managed; `picocli` is neither transitive nor BOM-managed (finding 6). Nothing else: logback and `jaxb-api` from the plugin docs are unnecessary here. |
| Where the plugin is applied | **`asapp.domain-service-conventions` (3), never `asapp.service-conventions` (5)** | config-service and discovery-service have no database, exactly as under Maven, where only the three domain-service poms declared the plugin. The flag-free analog of a per-module opt-in — the same reach as pitest and asciidoctor. |
| Umbrella membership | **Off `check` / `build` / `fullBuild` entirely** | Maven bound the plugin to no phase. These tasks mutate a developer's database; nothing may pull them in transitively. Nothing needs configuring — the plugin registers them standalone. |
| Docker/DB-dependent verification | **Delegated to the developer in full (§7)** | Developer instruction: implementation runs **no** command needing Docker or a live database. Everything observable without one stays with implementation. |

## 5. Changes by file

**`gradle/libs.versions.toml`** — three versions and three libraries, placed per the `## Ordering` rule (scope `# Build`, origin `## Org` for `org.liquibase`, `## Other` for `info.picocli`, alphabetical within each origin).

```toml
[versions]
# Build
## Org
liquibase = "5.0.2"          # apply-time only — keep equal to the Spring Boot BOM's liquibase.version
liquibase-gradle = "3.1.0"
## Other
picocli = "4.7.7"

[libraries]
# Build
## Org
liquibase-core = { module = "org.liquibase:liquibase-core", version.ref = "liquibase" }
liquibase-gradle-plugin = { module = "org.liquibase:liquibase-gradle-plugin", version.ref = "liquibase-gradle" }
## Other
picocli = { module = "info.picocli:picocli", version.ref = "picocli" }
```

`picocli` is a `# Build` entry rather than `# Runtime` deliberately: it is a dependency of the build's Liquibase CLI, never of any service's runtime classpath.

**`build-logic/build.gradle.kts`** — two entries under `// Org`:

```kotlin
implementation(libs.liquibase.core)            // apply-time requirement — plugin issue #182
implementation(libs.liquibase.gradle.plugin)
```

Both resolve from `gradlePluginPortal()`, already declared in `build-logic/settings.gradle.kts`. This is not incidental for the plugin: 3.1.0 was never published to Maven Central, which stops at 3.0.2.

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`** (3 domain services) — the plugin id, the runtime dependencies, and the activity. Exact statement ordering within the file is deferred to "Clean Gradle files" (line 53).

```kotlin
plugins {
    id("org.liquibase.gradle")
}
```

```kotlin
dependencies {
    // Liquibase CLI — the classpath the liquibase* tasks fork with, not the application's
    liquibaseRuntime("org.liquibase:liquibase-core")   // BOM-managed: always the version the app runs
    liquibaseRuntime("org.postgresql:postgresql")
    liquibaseRuntime(libs.findLibrary("picocli").get())  // optional in Liquibase's pom, absent from the BOM
}

// Developer database commands; the per-service url is added in each service's build script.
liquibase {
    activities.register("main") {
        this.arguments = mutableMapOf(
            // Anchor the search path at the module's resources: the default is the directory Gradle
            // was invoked from, so a module-relative changelog path is never found. Anchoring here
            // also makes recorded changeset paths identical to the ones the running service writes.
            "searchPath" to layout.projectDirectory.dir("src/main/resources").asFile.path,
            "changelogFile" to "liquibase/db/changelog/db.changelog-master.xml",
            "username" to "user",
            "password" to "secret",
        )
    }
    runList = "main"
}
```

**`services/asapp-{authentication,tasks,users}-service/build.gradle.kts`** — one block each, differing only in the URL (`5432/authenticationdb`, `5433/tasksdb`, `5434/usersdb`):

```kotlin
// The one per-service Liquibase value; everything else is in asapp.domain-service-conventions.
liquibase {
    activities.named("main") {
        @Suppress("UNCHECKED_CAST")
        (arguments as MutableMap<String, String>)["url"] = "jdbc:postgresql://localhost:5433/tasksdb"
    }
}
```

**`gradle.properties`** — one key, after the `org.gradle.*` group:

```properties
# Namespaces the plugin's 41 generated task names: update -> liquibaseUpdate
liquibaseTaskPrefix=liquibase
```

**`.claude/rules/gradle.md`** — add a **Database migrations** section after "Docker images" and before "Ordering" (the file tracks subtask order), plus one line in `## Ordering` recording that `gradle.properties` now has a third key group (identity, then `org.gradle.*`, then plugin properties). The section must carry:

- `./gradlew :services:<svc>:liquibaseUpdate` / `liquibaseUpdateSql` / `liquibaseRollbackCount -PliquibaseCount=N` replace `mvn liquibase:update` / `updateSQL` / `rollback -Dliquibase.rollbackCount=N`. Invoked from the repo root with no `cd`.
- The plugin (`org.liquibase.gradle`, catalog `liquibase-gradle`) goes on the `build-logic` classpath **together with `liquibase-core`**: since 3.0.0 the plugin derives its task list from Liquibase's `CommandFactory` inside `apply()`, so without the second entry it fails with `NoClassDefFoundError: liquibase/Scope` (issue #182). Resolved from `gradlePluginPortal()` — 3.1.0 is not on Maven Central. Applied versionless with `id("org.liquibase.gradle")` **only** in `asapp.domain-service-conventions` (the 3 database-backed services); config/discovery and the two libs get no liquibase tasks, the flag-free analog of Maven's per-service plugin declaration.
- **Two Liquibase versions, on purpose.** The catalog pins `liquibase = 5.0.2` for the apply-time copy only, because `build-logic` imports no Boot BOM; `liquibaseRuntime("org.liquibase:liquibase-core")` stays **versionless** so the CLI always runs the exact version the application runs, which is what keeps checksums and changelog parsing consistent between a developer's `liquibaseUpdate` and the service's boot-time migration. Keep the pin equal to Boot's managed `liquibase.version` on every Boot upgrade — drift there only skews command discovery, but drift on the runtime side would be a correctness bug.
- `liquibaseRuntime` needs exactly three coordinates: `liquibase-core` and `postgresql` (both BOM-managed, versionless) and `picocli` from the catalog. `picocli` is **not** optional in practice — the CLI entry point is built on it and it is declared `<optional>true</optional>` by `liquibase-core` and unmanaged by the Boot BOM, so omitting it fails with `NoClassDefFoundError: picocli/CommandLine$IFactory`. The plugin docs' other suggested extras are **not** needed here: `jaxb-api` is a non-optional transitive of `liquibase-core` 5.0.2, and logback is unnecessary.
- **`searchPath` is mandatory, not cosmetic.** The plugin forks Liquibase with a search path defaulting to the directory Gradle was invoked from, so a Maven-shaped `src/main/resources/...` changelog path fails outright with `not found in the configured search path`. Set `searchPath` to `layout.projectDirectory.dir("src/main/resources").asFile.path` and `changelogFile` to the classpath-shaped `liquibase/db/changelog/db.changelog-master.xml`. Anchor at `src/main/resources`, never `build/resources/main` — no `processResources` dependency, and never a stale copy. This is the Liquibase-native fix for the problem the plugin docs work around with `jvmArgs "-Duser.dir=$project.projectDir"`.
- **That choice also fixes a real mismatch.** Liquibase records each changeset's identity as its changelog path relative to the search path, and that string is the `DATABASECHANGELOG.FILENAME` column. Maven's basedir search path recorded `src/main/resources/liquibase/db/changelog/…`, while the booting service records `liquibase/db/changelog/…` from `spring.liquibase.change-log` — so the Maven CLI and the running app never saw each other's work. The `src/main/resources` anchor makes the two byte-identical. One-time consequence: a database previously migrated by `mvn liquibase:update` reads as un-migrated and must be recreated; one migrated by booting the service needs nothing.
- **Config split:** the shared arguments (`searchPath`, `changelogFile`, `username`, `password`) live in the activity registered in `asapp.domain-service-conventions`; only `url` is per-service, added in each service's own build script. Same routing as pitest's `targetClasses`/`targetTests`, and it follows the plugin's own guidance that one activity plus a varying value beats one activity per database. Mutating the single key needs `@Suppress("UNCHECKED_CAST")` because `Activity.arguments` is an untyped Groovy `def` filled via `methodMissing` — accepted, and the reason the whole block is not simply duplicated three times.
- **No `logLevel` in the activity, deliberately** — assigning the whole `arguments` map wipes the `[logLevel: 'info']` that `Activity` seeds, leaving Liquibase's own default of `OFF`. Both were tried and the quiet one kept; the update summary, success line and errors still print, being CLI output rather than logging. `-PliquibaseLogLevel=info` restores it per run.
- **`liquibaseTaskPrefix=liquibase` in root `gradle.properties`** namespaces all 41 generated tasks (`update` → `liquibaseUpdate`). Without it, three modules each register 41 generic top-level names and a bare `./gradlew update` fans out to all three databases through the task selector. The property is undocumented — it exists only in `LiquibasePlugin.applyTasks`, read via `project.hasProperty` — but works from `gradle.properties` and would fail loudly, not silently, if upstream ever dropped it.
- **Maven's single `rollback` goal maps to three tasks**, because the Liquibase CLI splits it: `liquibaseRollbackCount` (`-PliquibaseCount=N`), `liquibaseRollback` (tag), `liquibaseRollbackToDate`. `-Pliquibase<Arg>` capitalized is the plugin's documented override for any activity argument. Do **not** give `count` a default in the activity — activity arguments reach every command that accepts them, so it would also silently default `liquibaseUpdateCount`.
- **Three Maven settings are deliberately dropped.** `migrationSqlOutputFile`: the CLI's `outputFile` is an *activity* argument, so one fixed path would reach every command that accepts `--output-file` — `liquibaseUpdateSql` prints to stdout instead, and `-PliquibaseOutputFile=…` covers the ad-hoc case. `outputFileEncoding`: UTF-8 is already Liquibase's default and the Maven plugin declares none of its own, so the pom was writing a default down. `promptForNonLocalDatabase`: `liquibase-maven-plugin`-only, no CLI analog, and a no-op since all three URLs are `localhost`.
- **Keep every liquibase task off the `check` / `build` / `fullBuild` path** — Maven bound the plugin to no phase, and these tasks mutate a developer's database. Nothing to configure; the plugin registers them standalone.
- **Known cost, not a defect:** applying the plugin emits `The Project.container(Class, Closure) method has been deprecated … removed in Gradle 10` on every invocation, including `./gradlew help` ([issue #181](https://github.com/liquibase/liquibase-gradle-plugin/issues/181), open). It is inside the plugin and cannot be fixed here. The plugin **is** configuration-cache compatible (verified on Gradle 9.6.1 with Liquibase 5.0.2), so it does not hold back the deferred configuration cache.

**`TODO.md`** — four edits:

1. Check off "Migrate database migration commands to Gradle" (line 26).
2. Add under "Verify full parity, then remove Maven entirely" (line 74):

```markdown
    - **Note:** retire Maven's Liquibase wiring here — the `pluginManagement` block in `services/pom.xml`, the three per-service plugin declarations, and the three `mvn-liquibase.properties` files, which Gradle no longer reads
    - **Note:** those property files sit in `src/main/resources`, so `password=secret` currently ships inside every service jar and image — deleting them closes that too
```

3. Add under "Migrate build documentation to Gradle" (line 47):

```markdown
        - **Note:** the Database Management section of all three domain-service READMEs becomes `./gradlew :services:<svc>:liquibaseUpdate` / `liquibaseUpdateSql` / `liquibaseRollbackCount -PliquibaseCount=1`, run from the repo root with no `cd`; the `mvn liquibase:updateSQL` output file has no successor — the SQL now prints to stdout
```

4. Add under "Clean Gradle files" → "Review Deprecated Gradle features …" (line 62):

```markdown
            - **Note:** one deprecation is upstream and cannot be cleared locally — the Liquibase plugin's `Project.container(Class, Closure)` call (liquibase-gradle-plugin#181); it fires on every invocation, so zero-deprecation is unreachable until that ships
```

## 6. Placement / altitude rationale

- **The plugin, the `liquibaseRuntime` dependencies, and the shared activity → `asapp.domain-service-conventions` (3).** "How a database-backed service in this repo is migrated from the command line" is archetype policy for exactly the three modules that have a database — the same reach as pitest and asciidoctor, and the same reach Maven used by declaring the plugin in three service poms.
- **Nothing in `asapp.service-conventions` (5).** config-service and discovery-service have no database; giving them 41 tasks pointed at nothing would be new surface, not migration.
- **Nothing in the libs, by construction.** No `asapp.domain-service-conventions` → no plugin → no tasks.
- **Only `url` per service.** It is the sole per-service datum, exactly as `targetClasses`/`targetTests` are for pitest.
- **Nothing at the root** beyond the `liquibaseTaskPrefix` property, which is a build-wide naming policy rather than a task.

## 7. Verification / Definition of Done

**Implementation runs no command that needs Docker or a live database** (developer instruction). It verifies only what is observable without one:

1. **The build configures.** `./gradlew tasks` from the root succeeds — `build-logic` recompiles with the two new classpath entries and all 7 modules configure.
2. **Reach is exactly three modules.** `./gradlew tasks --all` lists `liquibaseUpdate` for the three domain services and **not** for config-service, discovery-service, or either lib.
3. **The prefix took effect.** `./gradlew :services:asapp-users-service:tasks --group=liquibase` lists prefixed names only (`liquibaseUpdate`, `liquibaseUpdateSql`, `liquibaseRollbackCount`, …) — no bare `update` anywhere in `tasks --all`.
4. **The CLI classpath resolves to the intended versions.** `./gradlew :services:asapp-users-service:dependencies --configuration liquibaseRuntime` shows `liquibase-core:5.0.2` and a BOM-managed `postgresql`, confirming `io.spring.dependency-management` reaches this configuration, plus `picocli:4.7.7`.
5. **No collateral damage.** `./gradlew :services:asapp-users-service:build --dry-run` and `:fullBuild --dry-run` mention no liquibase task.
6. **The deprecation is the only new one.** `./gradlew help --warning-mode all` reports `Project.container(Class, Closure)` and nothing else new.
7. **Maven untouched**: no `pom.xml` and no `mvn-liquibase.properties` edited, so `mvn liquibase:*` is unaffected by construction; per the standing migration constraint this is **not** re-verified by running `mvn`.

**The developer runs the database-dependent checks** and records the outcome in §11:

8. **Update works.** `docker-compose up -d asapp-tasks-postgres-db`, then `./gradlew :services:asapp-tasks-service:liquibaseUpdate` against a fresh database completes and creates the schema.
9. **Dry-run works.** `./gradlew :services:asapp-tasks-service:liquibaseUpdateSql` prints the migration SQL to the console and writes no file into the repository.
10. **Rollback works.** `./gradlew :services:asapp-tasks-service:liquibaseRollbackCount -PliquibaseCount=1` reverts the last changeset — the check that proves the `<rollback>` blocks are still exercised.
11. **Path parity, the important one.** After a `liquibaseUpdate`, boot the same service against that database (`./gradlew :services:asapp-tasks-service:bootRun`): it must start reporting **no pending changesets**. Inspecting `DATABASECHANGELOG.FILENAME` should show `liquibase/db/changelog/…` with no `src/main/resources/` prefix.
12. **The other direction.** Against a database created by *booting* the service, `liquibaseRollbackCount -PliquibaseCount=1` finds and reverts a changeset — impossible under Maven, and the concrete payoff of the search-path choice.

## 8. Out of scope / YAGNI

Retiring Maven's Liquibase wiring (line 74) · README rewrites (line 47) · the boot-time `spring.liquibase.*` configuration · changelog or changeset edits · `logicalFilePath` · database-agnostic changelogs (Backlog) · the `docker` seed-data context · `contexts` / `labels` filtering · multiple activities or a property-driven `runList` · Liquibase Hibernate diff / `generateChangelog` workflows · a root aggregator task · CI or release-workflow wiring (lines 33, 37) · git hook installation (line 31) · any `pom.xml` or application-source edit.

## 9. Contingencies

- **`io.spring.dependency-management` does not reach `liquibaseRuntime`** (§7.4 shows an unresolved version). Pin both from the catalog and record the fallback in `gradle.md` — but prefer BOM management, since a pinned runtime Liquibase can drift from the application's.
- **The type-safe `liquibaseRuntime(…)` accessor is unavailable** in the precompiled convention plugin. Use the string form `"liquibaseRuntime"(…)`, which always works; nothing else changes.
- **The `@Suppress("UNCHECKED_CAST")` mutation fails** because `arguments` is not the map instance expected. Fall back to assigning the complete map in each service script (the rejected "whole block per service" option) and update the `gradle.md` config-split bullet in the same change.
- **`liquibaseTaskPrefix` stops working** after a plugin upgrade. Tasks vanish under their prefixed names and every documented command fails loudly. Drop the property, accept bare names, and update the rules bullet.
- **Plugin 3.1.0 cannot be resolved.** It is Plugin-Portal-only; confirm `gradlePluginPortal()` is still first in `build-logic/settings.gradle.kts` before suspecting anything else.
- **A future Boot upgrade moves Liquibase past what plugin 3.1.0 tolerates.** Task discovery would break at apply time, loudly, on every build. Pin the apply-time catalog version back to a tolerated release — command discovery is all it drives — and leave `liquibaseRuntime` BOM-managed.
- **Gradle 10 ships before upstream fixes #181.** The build breaks on the wrapper upgrade, not silently. Options at that point: stay on 9.x until the fix, or fall back to the hand-rolled `JavaExec` design costed in §4.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-15-liquibase`. A single commit:

1. `build(gradle): migrate database migration commands to Gradle` — the catalog entries, the two `build-logic` classpath entries, the `asapp.domain-service-conventions` block, the three service-script URL lines, the `gradle.properties` key, the `.claude/rules/gradle.md` "Database migrations" section, the four `TODO.md` edits, and this spec.

The spec rides the implementation commit rather than landing in one of its own, matching all fourteen prior subtasks in this migration.

Following this migration's established pattern, implementation proceeds via the compressed flow — no separate writing-plans document — unless the developer requests a full plan.

## 11. Post-implementation notes

This spec was written before implementation. The core change shipped substantially as designed — the `org.liquibase.gradle` plugin applied in `asapp.domain-service-conventions` and nowhere else, one shared activity plus a per-service `url`, the `src/main/resources` search-path anchor that makes `DATABASECHANGELOG.FILENAME` byte-identical to what the booting service writes, the `liquibaseTaskPrefix` namespace, and all 41 tasks off the `check` / `build` / `fullBuild` path. **No §9 contingency fired**: `io.spring.dependency-management` does reach `liquibaseRuntime`, and the type-safe `liquibaseRuntime(…)` accessor resolves inside the precompiled convention plugin, so no string-invoke fallback was needed.

Sections 1–10 are the design record. The canonical implementation is the current state of `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`, the three domain-service build scripts, `gradle/libs.versions.toml`, `gradle.properties`, and the `## Database migrations` section of `.claude/rules/gradle.md` on this branch — not this document. The deltas below are where the two diverge; each names the section it supersedes.

`Notable deltas:`

- **The activity is configured additively, never by whole-map assignment (reverses §4 "Cost of that split" and the `@Suppress("UNCHECKED_CAST")` snippets in §5).** Both sides of the seam now use `withGroovyBuilder { "key"(value) }` — `activities.register("main")` in the convention plugin and `activities.named("main")` in each service script — dispatched through `Activity`'s `methodMissing` straight into the untyped `arguments` map. The cast the design costed at "three lines of wart" exists nowhere in the repository. Whole-map assignment was the actual defect: it silently wiped a seeded key, the same shape `.claude/rules/gradle.md` already forbids for `bootBuildImage`'s `environment`, and it split one convention across two idioms.
- **`logLevel` is set explicitly to `"off"` (reverses §4 "`logLevel`", which records it as not set).** The additive form leaves the plugin's seeded `[logLevel: 'info']` in place, so omitting the key would print every task at INFO. Quiet output is unchanged from what shipped mid-branch, but it is now a declared value rather than a side effect of the wipe. `-PliquibaseLogLevel=info` still restores it per run; `trace` is not a valid level and degrades silently to `INFO`, which `gradle.md` now records.
- **A new `// Liquibase` dependency scope group (extends §5's ordering).** The `liquibaseRuntime` block carries its own outer scope comment rather than trailing `// Test` unlabelled, and `Liquibase` joins the Scope-groups list in `gradle.md`'s `## Ordering` section — with the list's "the rest mirror Maven scopes" clause amended, since this one names the plugin's own classpath and explicitly not the application's `Runtime`.
- **`promptForNonLocalDatabase` is dropped on a different premise (corrects §4 "`promptForNonLocalDatabase`").** The design called it a `liquibase-maven-plugin` parameter "with no CLI or Gradle analog". It has one: `LiquibaseCommandLineConfiguration` declares it as a global argument on the modern `LiquibaseCommandLine` path this plugin forks, reachable as an ordinary activity argument. The drop still holds — all three URLs are `localhost`, so the prompt never fires — but that, not the absent analog, is the reason a future non-local URL would be argued against.
- **`liquibaseTaskPrefix` fixes name collisions only (narrows §4 "Task naming" and §6).** The design's rationale read as though the prefix also solved task-selector fan-out. It does not: an unqualified `./gradlew liquibaseUpdate` — or `liquibaseDropAll`, `liquibaseClearChecksums`, `liquibaseChangelogSync` — still reaches all three developer databases, verified by `--dry-run` before and after the property. This is Maven parity (a root `mvn liquibase:dropAll` fanned out identically), so the correction is wording plus the standing rule to always invoke path-qualified; no build-failing guard was added.
- **The missing-`count` failure is observed, not inferred (§4 "No default `count`", §7.10).** `liquibaseRollbackCount` with no `-PliquibaseCount` fails at CLI parse time with `Error parsing command line: Invalid argument '--count': missing required argument`, before any connection is attempted — confirmed with the Docker daemon down, so no database was needed after all. The text is recorded in `gradle.md` as support for the no-default rule.
- **The convention-plugin configuration cost is measured (§6).** Applying the plugin through `asapp.domain-service-conventions` configures it on every invocation, `./gradlew help` included, at roughly +15-20 ms per domain service — about +50-60 ms across the three, by `--profile` against a pre-Liquibase worktree. Recorded beside the deprecation bullet in `gradle.md` so any future move off the convention plugin is argued from a number.
- **The apply-time/runtime version invariant is scheduled, not guarded (§4 "Two Liquibase versions").** A `verifyLiquibaseVersionAlignment` check comparing the catalog pin against the resolved `liquibaseRuntime` version was built and fully verified, then rolled back: guarding an invariant is the wrong shape when the invariant can be deleted instead. `build-logic` imports no Boot BOM only because it was never wired to one, and importing one would let `liquibase-core` go versionless there and collapse the two numbers into one. Routed to `TODO.md` as "Reuse the Spring Boot BOM for the build's own dependency versions", scoped beyond Liquibase. Until it lands, the catalog's trailing `# Keep in sync with the Boot BOM` comment plus the `gradle.md` bullet are the only thing holding the two equal.
- **Maven's Liquibase wiring stays for now (§8 holds, with a live consequence).** Removing the four `pom.xml` declarations was considered for this task and left where §8 put it, under "Verify full parity, then remove Maven entirely". The cost of coexistence is real and worth stating: `mvn liquibase:update` still anchors at basedir and records `src/main/resources/liquibase/…`, so the two commands write divergent changeset identities against the same database until that removal lands.
- **The trailing-comment form in the version catalog is now a stated convention (extends §5).** The `liquibase` and `picocli` entries introduced trailing `#` annotations, a third purpose for a marker the catalog previously used only for scope and origin headers. `gradle.md`'s `## Ordering` section now names the rule: a `#` on its own line is always a group header, a trailing `#` is a short per-entry pointer used only where the constraint is invisible from the coordinate, and it is never promoted to its own line.

**Verified during implementation** (§7.1–7.7, none of which needs Docker or a database):

- **§7.1, the build configures:** `build-logic` recompiles with the two new classpath entries and all 7 modules configure.
- **§7.2, reach is exactly three modules:** `./gradlew tasks --all` lists `liquibaseUpdate` for authentication, tasks, and users — and for no other module.
- **§7.3, the prefix took effect:** `:services:asapp-users-service:tasks --group=liquibase` lists prefixed names only (`liquibaseUpdate`, `liquibaseUpdateSql`, `liquibaseRollbackCount`, `liquibaseValidate`, …). `tasks --all` contains **no** bare `update` task anywhere.
- **§7.4, the CLI classpath resolves as intended:** `dependencies --configuration liquibaseRuntime` shows `org.liquibase:liquibase-core -> 5.0.2` and `org.postgresql:postgresql -> 42.7.10` — both arrow-resolved, so the Boot BOM governs this configuration — plus `info.picocli:picocli:4.7.7` from the catalog.
- **§7.5, no collateral damage:** `liquibase` appears **zero** times in `:services:asapp-users-service:build --dry-run` and in `:fullBuild --dry-run`.
- **§7.6, the deprecation:** `help --warning-mode all` reports exactly two, both traced to `asapp.domain-service-conventions.gradle.kts`: the **pre-existing** `StartParameter.isConfigurationCacheRequested` from asciidoctor's transitive Grolifant (line 10, already documented under `## API documentation`) and the **new** `Project.container(Class, Closure)` from the Liquibase plugin (line 11). No third warning — the accepted cost is the only thing this subtask added.
- **Configuration cache, re-confirmed in the real build:** `:services:asapp-tasks-service:liquibaseUpdateSql --configuration-cache --dry-run` → `Configuration cache entry stored.` The scratch-build result of §1 finding 2 holds in this repo.
- **§7.7, Maven untouched**: no `pom.xml` and no `mvn-liquibase.properties` edited, so `mvn liquibase:*` is unaffected by construction; not re-verified by running `mvn`, per the standing migration constraint.

**Delegated to the developer** (§7.8–7.12, all of which need a running database, per the developer instruction in §4): update, dry-run, rollback, and the two path-parity checks — `liquibaseUpdate` followed by a `bootRun` that reports no pending changesets, and `liquibaseRollbackCount` against a database the *app* created. §7.11 and §7.12 are the pair that proves the `DATABASECHANGELOG` gap is actually closed rather than merely configured.

For future Liquibase or developer-command edits, treat `asapp.domain-service-conventions.gradle.kts`, the three service build scripts, and `gradle.md`'s `## Database migrations` section as the template — in particular the additive `withGroovyBuilder` idiom on both sides of the convention/service seam. This spec is preserved as a record of the original design intent.

## 12. References

- Liquibase Gradle Plugin — [usage](https://github.com/liquibase/liquibase-gradle-plugin/blob/main/doc/usage.md): the `liquibase { activities { … } }` / `runList` model; activity methods are pass-throughs to Liquibase CLI parameters, camelCase translated to kebab-case; `-Pliquibase<Arg>` capitalized overrides any activity argument; `liquibaseRuntime` must carry Liquibase, picocli and a JDBC driver; "the plugin will need to be able to find Liquibase on the classpath when it is applied"; and the subproject `jvmArgs "-Duser.dir=$project.projectDir"` workaround. Every Kotlin DSL example in the file reads "Coming Soon".
- Liquibase Gradle Plugin — [examples](https://github.com/liquibase/liquibase-gradle-plugin/blob/main/doc/examples.md): "We only need one activity block for each type of activity … the database credentials are driven by build properties … so that you don't need a separate activity for each database" — the basis of the config split in §4. Both examples set `logLevel "info"` explicitly.
- Liquibase Gradle Plugin — [changelog](https://github.com/liquibase/liquibase-gradle-plugin/blob/main/doc/changelog.md): 3.1.0 "Made the plugin compatible with Gradle's Configuration Cache" and with Gradle 9; 3.0.0 dropped Liquibase below 4.24 and introduced the apply-time Liquibase requirement.
- Liquibase Gradle Plugin — [issue #182, "incompatible with Gradle 9.3.0"](https://github.com/liquibase/liquibase-gradle-plugin/issues/182) (closed): the `NoClassDefFoundError: liquibase/Scope` report and the maintainer's resolution — "Since we now need to know about Liquibase classes at apply time, Liquibase also needs to be part of the buildscript dependencies".
- Liquibase Gradle Plugin — [issue #181, "Deprecation Warning in Gradle 9: Project.container(Class, Closure)"](https://github.com/liquibase/liquibase-gradle-plugin/issues/181) (open): the Gradle-10 blocker accepted in §4.
- Liquibase Gradle Plugin 3.1.0 source — `LiquibasePlugin.applyTasks` builds the task list from `Scope.getCurrentScope().getSingleton(CommandFactory).getCommands(false)` at apply time and applies `liquibaseTaskPrefix` via `project.hasProperty`; `Activity` defaults `arguments = [logLevel: 'info']` and routes every other setter through `methodMissing`; `LiquibaseTask.runLiquibase` forks `execOperations.javaexec` with `liquibase.integration.commandline.LiquibaseCommandLine`.
- Gradle Plugin Portal — [org.liquibase.gradle](https://plugins.gradle.org/plugin/org.liquibase.gradle): 3.1.0, published 2025-12-06. Maven Central's `maven-metadata.xml` for the same coordinates stops at 3.0.2, which is why `gradlePluginPortal()` is load-bearing.
- Liquibase — [rollback-count command](https://docs.liquibase.com/commands/rollback/rollback-count.html): `liquibase rollback-count --count=2 --changelog-file=… --url=…`, the CLI shape behind `liquibaseRollbackCount -PliquibaseCount=N`.
- Liquibase — [searchPath](https://docs.liquibase.com/parameters/search-path.html): the list of locations Liquibase resolves changelog paths against, and the origin of the recorded changeset path.
- `spring-boot-dependencies:4.0.5` — `<liquibase.version>5.0.2</liquibase.version>`; no `picocli` management, which is why picocli needs a catalog entry.
- `liquibase-core:5.0.2` POM — `info.picocli:picocli:4.7.7` is `<optional>true</optional>`; `javax.xml.bind:jaxb-api`, `commons-lang3`, `commons-io`, `snakeyaml`, `commons-text`, `commons-collections4` and `opencsv` are non-optional transitives, so none of the plugin docs' extra suggestions apply.
- Gradle — [upgrading to version 9: Project container methods](https://docs.gradle.org/9.6.1/userguide/upgrading_version_9.html#project_container_methods): `Project.container(Class, Closure)` is deprecated for removal in Gradle 10, replaced by `objects.domainObjectContainer(Class, NamedDomainObjectFactory)`.
