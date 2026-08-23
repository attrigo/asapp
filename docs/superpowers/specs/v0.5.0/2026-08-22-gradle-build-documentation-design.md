# Build documentation migration to Gradle — design spec

**Date**: 2026-08-22
**Status**: Approved
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate build documentation to Gradle" (line 53)
**Scope**: Rewrite every Maven command, artifact path and build claim across the nine narrative README files — root, five services, two libraries, `tools/jmeter` — plus the `TODO.md` bookkeeping. No build script, no convention plugin, no `pom.xml`, no workflow, no application source, no `.claude/` surface.

## 1. Context

Twenty-three subtasks moved the build onto Gradle 9.6.1. Every one of them deferred its README debt to this subtask, the way `2026-08-01-gradle-database-migration-commands-design.md` §3 records explicitly: *"Documentation. The Database Management sections of the three service READMEs still show `mvn liquibase:*`. Developer decision — handed to 'Migrate build documentation to Gradle' as a note, the way every prior subtask handed over its doc debt."*

The debt is now the whole developer-facing surface. `CLAUDE.md` and `.claude/rules/` were already synced (`e7c84199`); the narrative READMEs were not.

**What is stale.** 143 lines across nine files carry `mvn`, `Maven` or `target/`:

| File | Lines | Maven hits |
|---|---|---|
| `README.md` | 642 | 22 |
| `services/asapp-authentication-service/README.md` | 426 | 23 |
| `services/asapp-users-service/README.md` | 483 | 23 |
| `services/asapp-tasks-service/README.md` | 408 | 23 |
| `services/asapp-config-service/README.md` | 308 | 15 |
| `services/asapp-discovery-service/README.md` | 283 | 15 |
| `libs/asapp-http-clients/README.md` | 164 | 12 |
| `libs/asapp-commons-url/README.md` | 129 | 7 |
| `tools/jmeter/README.md` | 131 | 3 |

**Three kinds of staleness, not one.** A pure find-and-replace handles only the first:

1. **Commands and paths that have a direct Gradle equivalent** — the bulk of it.
2. **Instructions that are actively harmful under Gradle.** The config-service README opens its Quick Start with *"Run the service from the module directory"* followed by `cd services/asapp-config-service` — that working directory is exactly what leaves the config server serving nothing, and the three domain-service READMEs repeat the `cd` when telling a reader to start config and discovery. *"Automatically installed on `mvn install`"* describes a hook installation that no longer happens. *"Generate Spring REST API docs (no tests needed)"* describes a task that now depends on the whole integration tier.
3. **Claims that were already false under Maven.** The `config-service` and `discovery-service` Documentation tables list a mutation report, a coverage report and Javadoc. `pitest-maven` carried `<skip>true</skip>` for both; `maven-javadoc-plugin` sits in `<pluginManagement>` and neither pom declares it; neither pom declares jacoco. All three rows have been fiction since they were written, and the migration is where they surface.

## 2. Goals

1. No `mvn`, `Maven` or `target/` reference survives in any of the nine files, except where naming Maven is the point (nothing currently qualifies).
2. Every documented command runs from the repository root and works as written.
3. Every documented artifact path is one the Gradle build actually produces.
4. The three harmful instructions in §1.2 are deleted, not translated.
5. `config-service` and `discovery-service` document only artifacts that exist.

## 3. Non-goals

- **No restructuring.** Each README keeps its current section order and its full Development block, duplication included. **Developer decision**, taken with de-duplication and a central `docs/BUILD.md` both on the table: a reader landing on a module README from GitHub gets a self-contained page, and the sweep stays mechanical.
- **No `pom.xml` edit.** Maven stays working until "Verify full parity, then remove Maven entirely" (line 65).
- **No `.claude/` edit.** `CLAUDE.md`, the rules, the agents and the skills were synced in `e7c84199` and are not re-opened here.
- **No build change.** Where the docs meet a build gap — `fullBuild` producing no reports for the two infra services, `asciidoctor` dragging the integration tier — the documentation states what is true. Changing the build is a separate decision.
- **No fix for `.claude/rules/maven.md`** or the code-reviewer's routing entry — line 76 owns those.

## 4. Key decisions

| Question | Decision | Rationale |
|---|---|---|
| README shape | **Faithful swap; keep the duplication** | **Developer decision.** Under Maven each module README could honestly say "run `mvn` here"; under Gradle every command is root-invoked, which weakens that framing but does not outweigh a self-contained page per module. De-duplicating to the root README, and a central `docs/BUILD.md`, were both offered and declined. |
| `mvn clean install -DskipTests` | **`./gradlew assemble`** | **Developer decision.** `-x test` alone does not skip the tests — `integrationTest` is wired into `check` (`asapp.service-conventions:111`) — so the literal parity command is `./gradlew build -x test -x integrationTest`, which is verbose and silently stops skipping everything the day another task joins `check`. `assemble` is the idiomatic answer and cannot rot. It also skips `spotlessCheck`, which Maven's `-DskipTests` ran; the pre-commit hook covers that. |
| The three new run capabilities | **Only `bootRun` and the `--args` profile override are documented** | **Developer decision, overriding the `TODO.md` note.** The note asked for `classes --continuous`, `--debug-jvm` and a terminal-per-service flow as well. All three are dropped: the terminal flow is already implicit in the Quick Start steps that start config and discovery "in a separate terminal", and the other two are power-user flags that do not earn a line in nine files. §6 records the `TODO.md` correction. |
| Where the run commands go | **A new `### Run Locally` under `## Development`** | In the five service READMEs and the root README. Quick Start stays the short get-it-running path; Development holds the day-to-day loop. Consistent with the faithful-duplication choice. |
| `config` / `discovery` documentation sections | **Document the truth** | **Developer decision**, taken over both trimming the sections away and swapping the paths mechanically. The mutation row and the mutation command are deleted — no `pitest` task exists on those two modules. Coverage and Javadoc rows stay, each paired with the explicit task that produces it, because `fullBuild` reaches neither for `asapp.service-conventions` modules. |
| Javadoc on the infra services | **Documented — verified green** | `./gradlew :services:asapp-config-service:javadoc :services:asapp-discovery-service:javadoc` was run on the unmodified tree before this spec was written. `BUILD SUCCESSFUL`, two "use of default constructor" warnings per module, no doclint errors, `build/docs/javadoc/index.html` produced for both. `2026-07-23-gradle-javadoc-sources-jars-design.md` §4 flagged doc-linting never-linted infra source as a risk; measured, it is not one. The row is unconditional. |
| Library usage snippets | **`implementation(project(":libs:asapp-commons-url"))`** | The current XML snippet uses `${asapp.version}`, implying an external artifact. Services have never consumed these that way — `asapp.domain-service-conventions:24` and `services/asapp-users-service/build.gradle.kts:9` both use `project(...)`. The Gradle rewrite is the accurate one. |
| Requirements sections | **`**Gradle**: 9.6.1 (via the included wrapper — no install needed)`** | Replaces `**Maven**: 3.9.14+` in all nine files, and the two library Maven badges. The wrapper clause is the part that matters: unlike Maven, nothing needs installing. |
| Root README Continuous Delivery | **In scope** | **Developer decision.** Not named in the `TODO.md` notes, but the subsection still says *"Removes `-SNAPSHOT` suffix from all POM versions"* and *"Builds and verifies the project (`mvn clean install`)"*, where `asapp-release` reads `gradle.properties` and runs `./gradlew test` as a local pre-flight with `fullBuild` in CI. The `/release` invocation is corrected to `/asapp-release` in the same pass. |

## 5. The mapping

Every command runs from the repository root. No `cd`, ever — a module working directory is what leaves the config server serving nothing.

| Maven | Gradle |
|---|---|
| `mvn clean install` | `./gradlew build` |
| `mvn clean install -DskipTests` | `./gradlew assemble` |
| `mvn clean verify` | `./gradlew check` |
| `mvn org.pitest:pitest-maven:mutationCoverage` | `./gradlew pitest` (domain services only) |
| `mvn git-build-hook:install` | `./gradlew installGitHooks` |
| `mvn spotless:apply` | `./gradlew spotlessApply` |
| `mvn clean verify -Pfull` | `./gradlew fullBuild` |
| `mvn spring-boot:run` | `./gradlew :services:<svc>:bootRun` |
| `-Dspring-boot.run.profiles=…` | `--args='--spring.profiles.active=…'` |
| `mvn spring-boot:build-image` | `./gradlew bootBuildImage` · `./gradlew :services:<svc>:bootBuildImage` |
| `mvn liquibase:update` | `./gradlew :services:<svc>:liquibaseUpdate` |
| `mvn liquibase:updateSQL` | `./gradlew :services:<svc>:liquibaseUpdateSql` |
| `mvn liquibase:rollback -Dliquibase.rollbackCount=1` | `./gradlew :services:<svc>:liquibaseRollbackCount -PliquibaseCount=1` |
| `mvn asciidoctor:process-asciidoc@generate-docs` | `./gradlew :services:<svc>:asciidoctor` |

| Artifact | `target/` | `build/` |
|---|---|---|
| REST API docs | `target/generated-docs/api-guide.html` | `build/docs/asciidoc/api-guide.html` |
| Unit coverage | `target/site/jacoco-aggregate/index.html` | `build/reports/jacoco/test/html/index.html` |
| Integration coverage | — | `build/reports/jacoco/jacocoIntegrationTestReport/html/index.html` |
| Merged coverage | — | `build/reports/jacoco/jacocoMergedReport/html/index.html` |
| Mutation report | `target/pit-reports/<timestamp>/index.html` | `build/reports/pitest/index.html` |
| Javadoc | `target/site/apidocs/index.html` | `build/docs/javadoc/index.html` |
| Javadoc / sources jars | `target/*.jar` | `build/libs/<name>-<version>-{javadoc,sources}.jar` |

Three notes ride along with the mapping:

- **`liquibaseUpdateSql` prints to stdout.** Maven's `<migrationSqlOutputFile>` has no successor by design (`2026-08-01` §4); `-PliquibaseOutputFile=<path>` is the per-run way to capture it.
- **`./gradlew :build` builds nothing and reports success.** The root project has no `build` task, so Gradle reads the name as an abbreviation for `:buildEnvironment`. A blockquote under the root README's Build section says so.
- **`spotlessInstallGitPrePushHook` must never be run.** The Spotless plugin registers it with an inviting description, and it writes its own `pre-push` into the hooks directory `installGitHooks` manages from the tracked `git/hooks/`. A warn-off goes in the root README's Git Hooks section.
- **`bootRun` already runs from the repository root.** `asapp.service-conventions:192` sets `workingDir = layout.settingsDirectory.asFile`, so the config server finds `central-config/` no matter where the command was typed. This is what makes deleting every `cd` safe rather than merely tidier. The default profile list is a system property, so `--args` is free for the developer — and because a command-line argument outranks a system property, an explicit list *replaces* the default rather than adding to it. The root README already says exactly that; the sentence survives the rewrite unchanged.

**What each archetype's `fullBuild` actually produces** — the reason the Documentation tables differ by module:

| Archetype | Modules | `fullBuild` adds |
|---|---|---|
| `asapp.library-conventions` | 2 libs | `jacocoTestReport`, `javadocJar`, `sourcesJar` |
| `asapp.service-conventions` | config, discovery | nothing beyond `build` |
| `asapp.domain-service-conventions` | auth, tasks, users | the three coverage reports, `asciidoctor`, `javadocJar`, `sourcesJar` |

## 6. Changes by file

### `README.md`

1. **Requirements** (`:48`) — Maven line → Gradle line.
2. **Quick Start → Installation** (`:67`) — `./gradlew build`.
3. **Quick Start → Running the Application** (`:74`) — `./gradlew bootBuildImage`.
4. **Configuration & Profiles → Activating a profile** (`:160`) — `./gradlew :services:<svc>:bootRun`; the override becomes `--args='--spring.profiles.active=…'`; *"it's wired into each service's Maven plugin"* → wired into each service's `bootRun`. The config-service `native,dev` callout below it stays as written.
5. **Technology Stack → Code Quality** (`:361`) — *"Spotless Maven Plugin"* → *"Spotless Gradle Plugin"*.
6. **Development → Build** (`:370-378`) — `./gradlew build` and `./gradlew assemble`, plus the `:build` blockquote.
7. **Development → Test** (`:380-388`) — `./gradlew check`, `./gradlew pitest`.
8. **Development → `### Run Locally`** — **new**, between Test and Code Quality. `bootRun` and the `--args` override.
9. **Development → Code Quality** (`:390-398`) — `./gradlew installGitHooks`, `./gradlew spotlessApply`.
10. **Development → Generate Documentation** (`:400-405`) — `./gradlew fullBuild`.
11. **Reference → Documentation** (`:445-455`) — `build/` paths; the single coverage row becomes three.
12. **Contributing → Code Standards** (`:484`) — `./gradlew spotlessApply`.
13. **Contributing → Git Hooks** (`:488-495`) — *"Automatically installed on `mvn install`"* is **deleted**. Replaced with `./gradlew installGitHooks`, run once per clone and again after editing a hook, plus the `spotlessInstallGitPrePushHook` warn-off.
14. **CI/CD → Continuous Integration** (`:499-521`) — pipeline steps become checkout → set up JDK 25 (Temurin) → set up Gradle (wrapper validation + build cache) → `./gradlew ciBuild`. The *"Reports Generated"* list is **deleted**: Surefire and Failsafe do not exist under Gradle, and `-Pci` never produced those reports.
15. **CI/CD → Continuous Delivery** (`:523-545`) — `/release` → `/asapp-release`; *"all POM versions"* → the version in `gradle.properties`; *"Builds and verifies the project (`mvn clean install`)"* → verifies with `./gradlew test` as a local pre-flight, with the full `fullBuild` running in CI after the tag lands.

### The three domain-service READMEs

`asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service` — identical treatment, service name and port substituted:

1. **Requirements** — Gradle line.
2. **Quick Start → Run Locally (Development Mode)** — the `cd services/asapp-config-service && mvn spring-boot:run` and `cd services/asapp-discovery-service && mvn spring-boot:run` steps become root-invoked `./gradlew :services:<svc>:bootRun`, and the `cd` is **deleted**; step 4 becomes this service's `bootRun`.
3. **Quick Start → Run with Docker** — `./gradlew :services:<svc>:bootBuildImage`.
4. **Configuration & Profiles** — *"`mvn spring-boot:run` activates `<list>` (wired in the POM)"* → `bootRun`, wired in the build script. Each service's profile list is carried over verbatim; config-service keeps `native,dev`, which its own `bootRun` override sets (`services/asapp-config-service/build.gradle.kts:19-21`).
5. **Development → Build / Test / Code Quality** — per §5.
6. **Development → `### Run Locally`** — **new**, after Test.
7. **Development → Database Management** — `docker-compose up -d <svc>-postgres-db` stays; the three Liquibase commands become their Gradle tasks. `liquibaseUpdateSql` is described as printing to stdout, with `-PliquibaseOutputFile=<path>` documented alongside.
8. **Development → Generate Documentation** — `./gradlew fullBuild`, and `./gradlew :services:<svc>:asciidoctor` for the API guide alone. The *"(no tests needed)"* claim is **deleted**: `asciidoctor` `dependsOn` `integrationTest` (`asapp.domain-service-conventions:127`), so it runs the whole integration tier. Docker is therefore required.
9. **Reference → Documentation** — `build/` paths; coverage becomes three rows.
10. **Contributing** — `./gradlew check`, `./gradlew spotlessApply`.

### The two infra-service READMEs

`asapp-config-service`, `asapp-discovery-service` — items 1, 3, 4, 5 and 6 as above, with no Database Management section, and item 2 differing:

2. **Quick Start → Run Locally (Development Mode)** — these two do not start other services, but they carry the worst form of the working-directory instruction: a literal `# 1. Run the service from the module directory` comment followed by `cd services/<svc>` on its own line and then `mvn spring-boot:run`. The comment and the `cd` line are both **deleted**; the step becomes `./gradlew :services:<svc>:bootRun`, and the remaining numbered steps keep their numbering.

Then:

7. **Development → Test** — the mutation command is **deleted**. No `pitest` task is registered on these modules.
8. **Development → Generate Documentation** — `./gradlew fullBuild` produces nothing extra here, so the section documents the two tasks that do produce something: `:jacocoTestReport` and `:javadoc`, each labelled as not part of `fullBuild` for this service.
9. **Reference → Documentation** — the **Mutation report row is deleted**. Test coverage → `build/reports/jacoco/test/html/index.html`; Javadoc → `build/docs/javadoc/index.html`.

### The two library READMEs

`asapp-commons-url`, `asapp-http-clients`:

1. **Badge** — the Maven badge becomes a Gradle 9.6.1 badge linking to `https://gradle.org/`.
2. **Requirements** — Gradle line.
3. **Usage step 1** — *"Add the dependency to `pom.xml`"* and its XML block → *"Add the dependency to the consuming module's `build.gradle.kts`"* and `implementation(project(":libs:<name>"))`.
4. **Development → Build / Test / Code Quality / Generate Documentation** — per §5. `fullBuild` is accurate for libraries: it produces the unit coverage report and both jars.
5. **Reference → Documentation** — `build/reports/jacoco/test/html/index.html` (http-clients only) and `build/docs/javadoc/index.html`.
6. **Contributing** — `./gradlew spotlessApply`, `./gradlew check`.

### `tools/jmeter/README.md`

1. **Intro** (`:3-4`) — *"outside the Maven build and CI: nothing here runs during `mvn verify`"* → the Gradle wording, `./gradlew check`.
2. **Prerequisites** (`:36`) — *"build images first with `mvn spring-boot:build-image`"* → `./gradlew bootBuildImage`.

### `TODO.md`

Tick line 53 and delete its eleven notes, matching how every completed subtask in this epic was closed (`e7c84199`, `5f223871`): the notes are working instructions, and the spec is the record once they are executed. §4 carries the one decision that diverged from them — `classes --continuous`, `--debug-jvm` and the terminal-per-service callout were dropped deliberately, not overlooked.

None of the eleven need handing forward to "Verify full parity, then remove Maven entirely": each was a doc-rewrite instruction, and all are executed here.

## 7. Verification / Definition of Done

1. **No Maven residue** — `grep -rIn -e 'mvn' -e 'Maven' -e 'target/' --include='README.md' . | grep -v '^./docs/'` returns nothing.
2. **Every documented Gradle task exists** — for each distinct task in the nine files, `./gradlew <task> --dry-run` resolves. Includes the negative cases: `:services:asapp-config-service:pitest` and `:services:asapp-discovery-service:pitest` must still fail with task-not-found, which is why their rows were deleted.
3. **The infra Javadoc rows are real** — `ls services/asapp-config-service/build/docs/javadoc/index.html` and the discovery equivalent, after the `:javadoc` runs. Already confirmed on the pre-change tree (§4).
4. **Liquibase task names resolve** — `./gradlew :services:asapp-tasks-service:liquibaseUpdate --dry-run`, and the same for `liquibaseUpdateSql` and `liquibaseRollbackCount`.
5. **The `:build` claim holds** — `./gradlew :build` still reports success while building nothing, so the blockquote is warning about a live trap and not a fixed one.
6. **Formatting** — `./gradlew spotlessCheck`. Markdown has no formatter configured (backlog), so this only proves nothing else broke.
7. **Links resolve** — every relative link touched still points at an existing file.
8. **Developer-run** — a spot check that one documented flow works end to end: `./gradlew :services:asapp-tasks-service:bootRun` from the repository root, against a started config and discovery service.

**Done when** 1–7 pass, 8 is developer-confirmed, and `TODO.md:53` is ticked with its note rewritten.

## 8. Out of scope / YAGNI

`pom.xml` of any module · every `.claude/` surface, `CLAUDE.md` included · `.claude/rules/maven.md` and the code-reviewer routing entry (line 76) · the developer's Claude memory (line 77) · workflow files · application source · the `docs/superpowers/` READMEs · a central `docs/BUILD.md` · de-duplicating the Development blocks · Markdown formatting rules (backlog) · wiring coverage or Javadoc into `fullBuild` for the infra services · decoupling `asciidoctor` from the integration tier (backlog) · any content unrelated to the build.

## 9. Contingencies

- **A documented task turns out not to exist.** Fix the documentation, never the build — a build change here is a scope escape. Log it as a nested note under line 65 instead.
- **`:javadoc` regresses on an infra service later.** The row cites a task, not a `fullBuild` output, so it degrades to a failing command rather than a silent lie. Acceptable; the alternative was deleting a real capability.
- **The `asciidoctor` integration-tier dependency is removed by the backlog item before this lands.** Then the *"(no tests needed)"* claim becomes true again — but restore it only after confirming, not on the strength of the backlog entry existing.
- **Step 1's grep hits a false positive.** `Maven` may legitimately appear in a sentence about the migration. Nothing currently qualifies; if something does, note the exception in the commit body rather than contorting the prose.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-26-docs`. **One** commit — `docs(gradle)` — carrying this spec, the nine READMEs and the `TODO.md` edit together, matching the epic's per-subtask shape.

**Stage those eleven paths explicitly.** The working tree carries seventeen modified `.claude/` files from unrelated in-flight authoring work — several of which *revert* the Gradle sync in `e7c84199` — and `git commit -a` would sweep them in.

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document.

## 11. Post-implementation notes

- **`TODO.md:71`'s coexistence hazard fired during this task.** The commit reported both hooks *"ignored because it's not set as executable"*. The installed `pre-commit` was the Maven-era one — `mvn spotless:check`, no `kt|kts` extensions, no `./gradlew` guard — written into the shared hooks directory at 18:42, non-executable because `git-build-hook-maven-plugin` sets no mode. `InstallGitHooks` is not at fault: it copies with `filePermissions { unix("755") }` and warns about any hook git would skip. `./gradlew installGitHooks` restored the tracked 2,425-byte hook, executable. The note's prescription — *"re-run `./gradlew installGitHooks` after one"* — is correct as written, and this is the first observed instance.
- **Generate Documentation became a command-to-artifact table — revises §6's domain, infra and library items, and §4's "document the truth" row.** The section was a bare command block, and the Reference table a bare location list, with nothing linking the two: a reader wanting the merged coverage report had no command to reach it. **Developer decision** — the mapping lives with the commands, as a `Command` / `Generates` table under a `./gradlew <module>:<command>` preamble, with `fullBuild`'s reach stated in one sentence below it. Consequence: the `Produced by` column §6 gave the two infra services is gone, so all five services now share one two-column Reference table. `libs/asapp-commons-url` gained the section outright, having had none.
- **The two infra services have three coverage reports, not one — corrects §6's infra item 8 and its Reference table.** `asapp.service-conventions` registers `jacocoIntegrationTestReport` and `jacocoMergedReport` for all five services, not just the domain three; only the `fullBuild` wiring is domain-only. Verified by dry-run on both. The first pass documented `jacocoTestReport` alone and understated what the infra services can produce.
- **The section counts in §6 held**, with one addition: `tools/jmeter/README.md:3` needed a re-wrap. *"Gradle build"* is two characters longer than *"Maven build"*, pushing a hand-wrapped line from 159 to 161 columns; a word moved to the next line brings it to 157.

## 12. References

- `docs/superpowers/specs/v0.5.0/2026-08-01-gradle-database-migration-commands-design.md` — the Liquibase task names, the `liquibaseTaskPrefix` policy, and why `migrationSqlOutputFile` has no successor.
- `docs/superpowers/specs/v0.5.0/2026-07-20-gradle-mutation-testing-design.md` §4 — mutation testing on the three domain services only, the flag-free analog of Maven's `<skip>true</skip>`.
- `docs/superpowers/specs/v0.5.0/2026-07-23-gradle-javadoc-sources-jars-design.md` §4 — why config and discovery get no javadoc/sources jars, and the doc-lint risk this spec measured and cleared.
- `docs/superpowers/specs/v0.5.0/2026-07-30-gradle-local-run-design.md` — `bootRun`, the profile wiring, and the repository-root working directory.
- `docs/superpowers/specs/v0.5.0/2026-08-09-gradle-ci-aggregate-task-design.md` — `ciBuild`, the single task the CI section now documents.
- `.claude/rules/gradle.md` → **Developer workflow** — the `:build` abbreviation trap and the `spotlessInstallGitPrePushHook` warn-off, both restated for a human audience here.
