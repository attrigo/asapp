# Maven removal — design spec

**Date**: 2026-08-22
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Verify full parity, then remove Maven entirely" (line 54)
**Scope**: Establish that the Gradle build reproduces everything the Maven build did, then delete Maven from the repository — 10 `pom.xml` files, 15 Maven wrapper files, 3 `mvn-liquibase.properties`, 5 module `.gitignore` files, and the root `.gitignore`'s four Maven lines. Retire the three migration-time scaffolds, revert the REST Docs snippets directory to `build/`, retire `.claude/rules/maven.md` with its routing entry, and update the developer's Claude memory. No application source, no test source, no dependency change, no new Gradle capability.

## 1. Context

Twenty-six subtasks moved the build onto Gradle 9.6.1. This is the twenty-seventh and last: the poms have been non-authoritative for the whole epic, kept alive only so each subtask could be verified against a working reference.

**Maven is still wired into eleven places.** Ten `pom.xml` files (root, two aggregators, seven modules) plus fifteen wrapper files in five of the seven modules — `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`. Thirty-three tracked files in total, once the three `mvn-liquibase.properties` and the five per-module `.gitignore` files are counted.

**Three scaffolds exist only because the epic lives off `main`.** `org.gradle.console=verbose` in `gradle.properties`, the `build/replace-maven-with-gradle*` pattern in `ci.yml`'s push trigger, and `cache-read-only: false` on `setup-gradle`. Each carries a comment naming this subtask as its retirement point.

**One coupling is not obvious and is the reason this cannot be split.** Spring REST Docs' `ManualRestDocumentation` chooses its output directory with `new File("pom.xml").exists() ? "target" : "build"`, evaluated against the `Test` task's working directory — which is the module directory. So the snippets a service emits move the instant that service's `pom.xml` is deleted. `asapp.domain-service-conventions` currently pins `snippetsDir` to `target/generated-snippets` to match, and passes an explicit `snippets` attribute to Asciidoctor because `spring-restdocs-asciidoctor` detects the build tool differently — via an unset `maven.home`, so it has always defaulted to `build/`. Removing the poms converges both back on `build/generated-snippets`; leaving the convention plugin pinned to `target/` would leave `asciidoctor` reading an empty directory and shipping a guide of placeholder sentences, with no failure. `2026-07-22-gradle-api-documentation-design.md` §11 records both halves.

**The git-hook hazard has now fired twice.** `2026-08-22-gradle-build-documentation-design.md` §11 recorded the first instance and fixed it with `./gradlew installGitHooks`. The installed `pre-commit` is once again the Maven-era one — 2,229 bytes, `mvn spotless:check`, no `kt|kts` extensions, no `./gradlew` guard — rewritten into the shared hooks directory at 23:10 on 2026-08-22, after that fix. The hooks directory is shared by every worktree on this machine, so any `mvn` run from a pre-migration checkout re-installs it. This is a treadmill until the poms are gone, which is what makes it this subtask's business rather than a recurring note.

**Dependabot turned out not to be a stake at all.** `TODO.md:64` assumed removing the poms would silently drop Dependabot's view. Checked against the live repository: `asapp` is public, the token holds admin, and `GET /repos/attrigo/asapp/vulnerability-alerts` returns **404** — alerts are off. The poms have been feeding a dependency graph nothing reads. See §4.

## 2. Goals

- Every Maven capability is accounted for against a Gradle counterpart **before** anything is deleted, with each row citing the subtask spec that migrated it (§5).
- Maven leaves the repository completely: no pom, no wrapper, no Maven-only property file, no Maven line in a `.gitignore`.
- `password=secret` stops shipping inside every service jar and image.
- The three migration scaffolds are retired, and the snippets directory returns to `build/`.
- The `.claude` surface stops routing a rule file for a file type that no longer exists, and starts routing the Gradle rules it never gained.

## 3. Non-goals

- No new Gradle capability, no task rewiring, no dependency change. Anything the matrix surfaces as a genuine gap **blocks deletion** and is raised, not fixed opportunistically here.
- No `mvn` invocation. Parity is established on paper (§4), not by a side-by-side build.
- No Dependabot enablement and no dependency submission (§4).
- No touch of the historical specs under `docs/superpowers/specs/`, which describe Maven in the past tense by design.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Parity evidence | **A written matrix over all 10 poms, each row citing the spec that migrated it** | **Developer decision.** Every capability was migrated under its own spec with its own verification, and CI has been green on Gradle for the epic. A side-by-side `mvn -Pfull clean install` against `./gradlew clean fullBuild` was costed and rejected: two full builds by hand for byte-level evidence about a build that has not been authoritative for weeks. The matrix is cheap, and it surfaces a gap as a row with no counterpart *before* deletion rather than as a missing artifact afterwards. |
| Cleanup reach | **Wrappers and the five module `.gitignore` files go too** | The `TODO.md` notes name neither. `./mvnw` would survive in five modules pointed at deleted poms. Every pattern in the five module `.gitignore` files is already covered recursively by the root one, and only 5 of 7 modules carry one, so the set is inconsistent as well as redundant. |
| Dependency submission | **Dropped; recorded as a backlog entry instead** | **Developer decision, reversing `TODO.md:64`.** Dependabot alerts are off (§1), so the graph feeds nothing. Adding submission would raise the CI job from `contents: read` to `contents: write` on a public repository for no current benefit. The trap is real and worth recording — enabling alerts later would *look* like it works and stay empty, because GitHub parses poms but not Gradle scripts — so it becomes a backlog entry under `ci`, not a workflow change. |
| Pom deletion and the snippets revert | **One commit, never two** | The coupling in §1. Any ordering leaves a window where the API guide builds green from an empty snippets directory. |
| `permissions` in `ci.yml` | **Left at workflow level, unchanged** | It was only moving to job level to scope a `contents: write` grant that is no longer being made. |
| `.claude/rules/maven.md` | **Deleted, and its routing line replaced rather than removed** | The code-reviewer routing table lists `maven.md` but never gained `gradle.md` or `gradle-task-types.md`. Removing the Maven line alone would leave every build script unrouted — a regression disguised as a cleanup. |
| Leftover `target/` directories | **Deleted from the working tree** | Five empty ones remain on disk. They stop being ignored the moment the root `.gitignore`'s `target/` line goes, so they would surface as untracked noise. |

## 5. The parity matrix

Three verdicts only. **Covered** — a Gradle counterpart exists and a spec records it. **Divergence** — deliberate, with the rationale already recorded. **Gap** — no counterpart; blocks deletion.

Spec references are shortened to their date (`07-22` = `2026-07-22-gradle-api-documentation-design.md`), all under `docs/superpowers/specs/v0.5.0/`.

### 5.1 Root `pom.xml`

| Maven | Gradle counterpart | Spec | Verdict |
|---|---|---|---|
| `spring-boot-starter-parent` 4.0.5 | `io.spring.dependency-management` + Boot plugin, version from `libs.versions.toml` | 07-16 deps | covered |
| `groupId` / `version` / `packaging` | `group`, `version` in `gradle.properties`; `rootProject.name` | 07-16 structure | covered |
| `<licenses>`, `<developers>`, `<scm>` | none | — | divergence §5.5 |
| `java.version` 25 | `java.toolchain.languageVersion` + `options.release`, `asapp.java-conventions` | 07-17 | covered |
| `project.build.sourceEncoding` | `options.encoding`, Javadoc `encoding`/`docEncoding`/`charSet`, `buildInfo` `encoding` | 07-17 | covered |
| `archunit.version`, `jackson-bom.version` | `libs.versions.toml`; Jackson overridden per archetype | 08-05 BOM | covered |
| 5 plugin version properties | `libs.versions.toml` + the `build-logic` classpath | 08-05 BOM | covered |
| `maven-javadoc-plugin`, `doclint all,-missing` | `javadoc` options + `javadocJar`, `asapp.javadoc-sources-conventions` | 07-23 | covered |
| `maven-source-plugin` | `sourcesJar` | 07-23 | covered |
| `pitest-maven`, `mutationThreshold` 100 | `pitest` extension, `asapp.domain-service-conventions` | 07-20 | covered |
| `spotless-maven-plugin` — eclipse 4.35, `asapp_formatter.xml`, import order, unused imports, license header, UNIX endings | `spotless` block, `asapp.java-conventions` | 07-21 | covered |
| `git-build-hook-maven-plugin`, installs `pre-commit` + `commit-msg` | `InstallGitHooks`, `asapp.root-conventions` | 08-01 hooks | covered |
| profile `full` — 4 skip flags off | `fullBuild` | 07-27 | covered |
| profile `ci` — spotless on | `ciBuild` | 08-09 aggregate | covered |
| `<modules>` | `include(...)` in `settings.gradle.kts` | 07-16 structure | covered |

### 5.2 `services/pom.xml`

| Maven | Gradle counterpart | Spec | Verdict |
|---|---|---|---|
| `spring-cloud-dependencies` 2025.1.1 | `dependencyManagement { imports { mavenBom(...) } }`, `asapp.service-conventions` | 07-16 deps | covered |
| ~20 dependency version properties | `libs.versions.toml`, plus a CVE `constraints` block | 07-16 deps | covered |
| `maven-compiler-plugin` `annotationProcessorPaths` mapstruct | `annotationProcessor(libs…mapstruct-processor)` | 07-17 | covered |
| `maven-failsafe-plugin` `integration-test` + `verify` | `integrationTest` task, wired into `check` | 07-18 IT | covered |
| `maven-surefire-plugin` | `test`, narrowed to `**/*Tests.class` | 07-18 unit | covered |
| `spring-boot-maven-plugin` image — buildpacks, name, `BP_*` env | `bootBuildImage` | 07-31 | covered |
| image `createdDate=${maven.build.timestamp}` | left unset | 07-31 | divergence §5.5 |
| `build-info` with `encoding` + `java` | `springBoot.buildInfo` | 07-24 | covered |
| `asciidoctor-maven-plugin` at `post-integration-test` | `asciidoctor` task, opt-in | 07-22 | divergence §5.5 |
| `cyclonedx-maven-plugin` at `prepare-package` | `cyclonedxDirectBom` + `cyclonedxBom`, with an empty-SBOM guard | 08-05 SBOM | covered |
| `jacoco-maven-plugin`, 5 executions | `jacocoTestReport`, `jacocoIntegrationTestReport`, `jacocoMergedReport` | 07-18 coverage | covered |
| `liquibase-maven-plugin` `propertyFile` + `changeLogFile` | `liquibase { activities }`, URL per service | 08-01 db | covered |
| `migrationSqlOutputFile`, `outputFileEncoding` | dropped; `liquibaseUpdateSql` prints to stdout | 08-01 db | divergence §5.5 |
| `git-commit-id-maven-plugin` at `prepare-package` | `com.gorylenko.gradle-git-properties`, keys narrowed to four | 07-24 | divergence §5.5 |

### 5.3 `libs/pom.xml`

| Maven | Gradle counterpart | Spec | Verdict |
|---|---|---|---|
| `spring-boot.build-image.skip=true` | libraries never apply the Boot plugin, so no `bootBuildImage` exists | 07-16 structure | covered |
| `pitest-maven` `<skip>true</skip>` | `pitest` applied only in `asapp.domain-service-conventions` | 07-20 | covered |
| `jacoco` unit executions | `jacocoTestReport` via `asapp.java-conventions` | 07-18 coverage | covered |
| `maven-compiler-plugin` `release` | `options.release` | 07-17 | covered |

### 5.4 The seven module poms

| Maven | Gradle counterpart | Spec | Verdict |
|---|---|---|---|
| `git-build-hook` declared in all 7 modules | one `installGitHooks` on the root project | 08-01 hooks | covered |
| `spring-boot-maven-plugin` `<profiles>dev` ×5 | `bootRun` `spring.profiles.active=dev` | 07-30 run | covered |
| config-service `<profiles>native,dev` | `bootRun` overridden to `native,dev` in its own script | 07-30 run | covered |
| pitest `targetClasses`/`targetTests` ×3 domain | per-service `pitest` block | 07-20 | covered |
| pitest `<skip>true</skip>` ×2 infra | pitest not applied to infra services | 07-20 | covered |
| no `jacoco` on config / discovery | `asapp.java-conventions` applies JaCoCo to every module | 07-18 coverage | divergence §5.5 |
| no javadoc / source on config / discovery | `asapp.javadoc-sources-conventions` not applied there | 07-23 | covered |
| `asciidoctor` + `liquibase` on the 3 domain services | `asapp.domain-service-conventions` | 07-22, 08-01 | covered |
| per-module `<dependencies>` | per-module `build.gradle.kts` + convention plugins | 07-16 deps | covered |

### 5.5 Accepted divergences

All five are pre-recorded decisions, not discoveries. None blocks deletion.

1. **`<licenses>`, `<developers>`, `<scm>` have no Gradle counterpart.** Only a published POM consumes them and this project publishes none. Confirm before ticking the row that the CycloneDX SBOM does not source metadata from them — it is generated from resolved configurations, and `includeBuildSystem = false` already excludes build-tool metadata, so the expected answer is that nothing is lost.
2. **`createdDate` unset.** Maven pinned `${maven.build.timestamp}`; leaving it unset makes identical builds produce identical images. Reproducibility for the jar side is a backlog item.
3. **The API guide is opt-in.** Maven ran Asciidoctor on every `verify`; Gradle keeps it off `check`/`build` and on `fullBuild`/`ciBuild` — the flag-free analog of `-Pfull`.
4. **`migrationSqlOutputFile` dropped.** `outputFile` sits on the activity, so a fixed path would leak into `snapshot`, `diff` and `generateChangelog`. Maven's override also pointed into `src/main/resources`, where `processResources` would have copied generated SQL into the jar.
5. **Two supersets, both improvements.** `git.properties` is narrowed to four keys so the build host and developer identity never reach a published image; config and discovery gain coverage reports Maven never produced for them.

## 6. Changes by file

### C1 — `build(gradle)`: remove Maven

**Deleted (33 tracked files), with `git rm`:**

- 10 poms: `pom.xml`, `libs/pom.xml`, `services/pom.xml`, and one per module.
- 15 wrapper files: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` in `libs/asapp-commons-url`, `libs/asapp-http-clients`, `services/asapp-authentication-service`, `services/asapp-tasks-service`, `services/asapp-users-service`.
- 3 × `src/main/resources/liquibase/config/mvn-liquibase.properties` (authentication, tasks, users) — the files carrying `password=secret` into every jar and image.
- 5 module `.gitignore` files.

**Edited:**

- `.gitignore` — drop `target/`, `!.mvn/wrapper/maven-wrapper.jar`, `!**/src/main/**/target/`, `!**/src/test/**/target/`. Keep everything else.
- `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts` — three edits: line 19 `snippetsDir` back to `layout.buildDirectory.dir("generated-snippets")`; lines 106–109 the `clean` hook deleted with its comment; lines 125–126 the `snippets` attribute deleted with its comment.

**Untracked cleanup:** remove the five empty `target/` directories.

### C2 — `build(gradle)`: retire the console scaffold

`gradle.properties` lines 8–9: the comment and `org.gradle.console=verbose`.

### C3 — `ci(gradle)`: retire the workflow scaffolds

`.github/workflows/ci.yml`:

- Line 3 — the third header-comment line.
- Lines 9–13 — the `TEMPORARY` block and `build/replace-maven-with-gradle*`, leaving `branches: [ "main" ]`.
- Lines 48–51 — the `TEMPORARY` comment and `cache-read-only: false`, restoring the action default of write on the default branch and read-only elsewhere.

`permissions: contents: read` is untouched.

### C4 — `docs(gradle)`: retire the Maven rule and the bookkeeping

- **Delete** `.claude/rules/maven.md`. Nothing is lost: `gradle.md` §"Block order" already carries the equivalent ordering discipline for the build scripts that remain.
- `.claude/agents/code-reviewer.md` line 37 — replace the `maven.md` row with two rows, `gradle.md` first and `gradle-task-types.md` second, moved up between `domain-design.md` and `liquibase.md` to match the order `ls` gives the rules directory:
  - `` `gradle.md` ``: paths `**/build.gradle.kts`, `**/settings.gradle.kts`, `**/src/main/kotlin/*.gradle.kts`, `**/gradle.properties`, `**/gradle-wrapper.properties`, `**/libs.versions.toml`
  - `` `gradle-task-types.md` ``: paths `**/build-logic/src/**/*.kt`
- `TODO.md` — tick line 54, drop the twelve consumed bullets, and add under `#### ci` in the backlog:

```
* Submit the dependency graph from the pipeline if vulnerability alerts are enabled
    * GitHub reads dependencies from Maven poms but not Gradle scripts, so the graph would stay empty
    * Needs contents write on the build job, which has read today
```

### Outside the repository — the developer's Claude memory

`feedback_mvn_permissions.md`: cut the Maven half of each inexpensive/expensive pair, and rename both the file and its `name:` slug off `mvn`. The `MEMORY.md` pointer line moves with it. **Confirm with the developer before editing**, per the standing rule; no commit, as the memory lives outside this checkout.

## 7. Verification / Definition of Done

**Claude runs:**

1. **No Maven residue** — `git ls-files | grep -iE 'pom\.xml|mvnw|\.mvn/|mvn-liquibase'` returns nothing. Then `grep -rIn -iE 'mvn|maven' --exclude-dir={.git,build,docs,.gradle,.idea} .` returns only four kinds of hit, all expected: the four Gradle API calls (`mavenCentral` ×2, `mavenBom` ×2); `TODO.md`'s own Maven references — the epic title, the SBOM task's parity notes, and the two backlog entries; and the `.git` pointer file, whose gitdir path contains this worktree's directory name.
2. **The build still configures** — `./gradlew help` and `./gradlew ciBuild --dry-run` resolve.
3. **The build logic's own suite** — `./gradlew :build-logic:check`. The `InstallGitHooks` fixtures are self-contained, but this suite is its only automated guard.
4. **Hooks** — `./gradlew installGitHooks`, then confirm the installed `pre-commit` is byte-identical to `git/hooks/pre-commit` (2,425 bytes) and executable.
5. **Formatting** — `./gradlew spotlessCheck`.
6. **Packaging still works** — `./gradlew :services:asapp-tasks-service:bootJar`, then confirm no `mvn-liquibase.properties` inside the jar.

**The developer runs**, after C1–C4 land:

7. **The full build** — `./gradlew clean fullBuild`. This is the row that matters: confirm `build/generated-snippets` is populated for all three domain services, that `target/` reappears nowhere, and that each `build/docs/asciidoc/api-guide.html` contains expanded examples rather than placeholder sentences.
8. **Cache resolution** — an immediate second `./gradlew fullBuild` with no edits in between: every task `UP-TO-DATE` or `FROM-CACHE`, no spurious integration-tier rerun.
9. **Images** — `./gradlew bootBuildImage`.
10. **Mutation testing** — `./gradlew pitest`.

**Done when** 1–6 pass, 7–10 are developer-confirmed, the memory edit is confirmed and applied, and `TODO.md:54` is ticked.

## 8. Out of scope / YAGNI

Dependabot enablement and dependency submission (§4, backlog) · `.github/dependabot.yml` · the historical specs under `docs/superpowers/specs/`, which describe Maven in the past tense · `TODO.md:67` (the SBOM component assertion) and `TODO.md:72` (ArchUnit), both separate tasks in this version · jar reproducibility (backlog) · decoupling the API docs from the integration tier (backlog) · warning when an installed hook drifts from its source (backlog) · any Gradle capability Maven never had.

## 9. Contingencies

- **The matrix turns up a genuine gap.** Deletion stops. Log it as a nested note under `TODO.md:54` and raise it — building the missing capability inside a removal commit is a scope escape.
- **Snippets do not appear under `build/` in step 7.** The likely cause is a stale `target/generated-snippets` that a service still reads, or an `asciidoctor` input left pinned. Do not re-add the explicit `snippets` attribute to force it; find which of the three edits did not land.
- **The pre-commit hook fires as Maven during this task.** Expected while any pre-migration checkout on this machine can still run `mvn`. Re-run `./gradlew installGitHooks`; after C1 the hazard is gone for this repository.
- **IntelliJ loses the project model.** It may still hold a Maven import. Re-import as Gradle; no repository change is warranted for it.
- **`:build-logic:check` fails after the poms go.** The fixtures are self-contained, so a failure means a real coupling nobody knew about. Investigate rather than adjust the test.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-27-mvn-removal`. **Four** commits, each leaving the build green, split by Conventional Commit scope:

| | Scope | Contents |
|---|---|---|
| C1 | `build(gradle)` | 33 deletions + the root `.gitignore` and the three `snippetsDir` edits |
| C2 | `build(gradle)` | the `gradle.properties` console scaffold |
| C3 | `ci(gradle)` | the two `ci.yml` scaffolds and the header line |
| C4 | `docs(gradle)` | the rule file, the routing table, `TODO.md`, and this spec |

C1 is indivisible: pom deletion and the snippets revert must land together (§1).

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document.

## 11. Post-implementation notes

The canonical implementation is Maven's absence from the tree, plus `asapp.domain-service-conventions.gradle.kts`, `ci.yml` and `gradle.properties` — not this document.

Notable deltas:

- **The git-hook hazard outlived the poms — revises §9, §7 row 4, and §1.** The shared hooks directory belongs to the main checkout, which still carries a `pom.xml` and no `./gradlew`; it silently rewrote the Maven-era, non-executable `pre-commit` back mid-task, so git skipped both hooks on this task's own commits. §9's "the hazard is gone for this repository" and §1's premise both no longer hold. Ruling: do not re-run `./gradlew installGitHooks` — its `[ ! -x ./gradlew ]` guard would block every commit in the sibling stream instead; the hooks' checks (CRLF, `spotlessCheck`, Conventional Commit header) were replicated by hand per commit.
- **Deleting the console scaffold breaks Row 8's evidence — revises §6 C2 and §7 rows 7–8.** With `org.gradle.console=verbose` gone, `auto` resolves to `rich`, which prints no task header for output-free tasks — measured, `spotlessCheck --console=plain` prints 31 `UP-TO-DATE` lines against `rich`'s zero. Row 8's "every task `UP-TO-DATE` or `FROM-CACHE`" is therefore unobtainable as written; commands needing fixed-shape output must pass `--console=plain`. That rule now lives in C2's commit body, not here.
- **The residue gate could not catch `pom.xml`.** §7.1's check greps `-iE 'mvn|maven'`; neither `pom.xml` nor `POM` matches either alternative, so `README.md`'s Project Structure tree kept a `pom.xml` entry through this task's own gate and through the preceding build-documentation task's gate (`2026-08-22-gradle-build-documentation-design.md`, which grepped `mvn`, `Maven`, `target/`). The pattern should widen to `-iE 'mvn|maven|pom\.xml'`: a removal task needs to grep for the artifact names being removed, not only the tool's name. The same check has two further gaps: (a) its `--exclude-dir={.git,build,docs,.gradle,.idea}` list omits the git-ignored `.superpowers` SDD workspace, dense with `mvn`/`maven` hits — safe to exclude, since the `git ls-files` half of the check stays authoritative over tracked residue; (b) its expected-hit enumeration omits `TODO.md`'s own ticked subtask title, `- [X] Verify full parity, then remove Maven entirely`, which matches `grep -i maven` — ruled expected and left verbatim, because §7.1's "any other hit is residue and must be cleared" would otherwise push an implementer to reword a completed task's own record.
- **`README.md`'s tree needed a fifth commit — revises §6 and §10.** The Project Structure tree sat outside §6's file inventory entirely and still named the deleted `pom.xml` as "Parent POM" with no Gradle build file listed. It now lists `build-logic/`, `build.gradle.kts` and `settings.gradle.kts`; `gradle/`, `gradlew`, `gradlew.bat` and `gradle.properties` stay out, mirroring the old tree's own omission of `.mvn/` and `mvnw`. §10's "Four commits" table is one short; C1–C4 map onto the first four exactly as specified.
- **The parity matrix arithmetic in the plan is wrong.** `docs/superpowers/plans/2026-08-22-gradle-maven-removal.md` states "45 rows total" and mandates reporting 40 covered, 5 divergence; §5's four tables actually hold 42 rows — 36 covered and 6 divergence, with 0 gaps. The 6 divergence rows trace correctly to §5.5's five numbered entries: entry 5, "Two supersets, both improvements," covers two rows by its own text — the `git.properties` key narrowing (§5.2) and the config/discovery JaCoCo superset (§5.4). The verdict is unchanged (zero gaps, deletion cleared); only the counts were wrong, and the plan is now a committed artifact carrying three incorrect numbers. §10's own line "implementation proceeds without a separate plan document" is contradicted by that plan's existence — it drove the whole run.
- **`promptForNonLocalDatabase` had no matrix row.** The deleted `mvn-liquibase.properties` files carried four keys; `url`, `username` and `password` all have Gradle counterparts in §5.2's `liquibase { activities }` row, but the Maven Liquibase plugin's `promptForNonLocalDatabase=true` guard — confirm before touching a non-local database — has none, and §5.2 lists no row for it. It should have been recorded as a sixth accepted divergence in §5.5 rather than going unlisted. Practical risk is nil: each domain service pins its Liquibase URL to `localhost` in its own build script.
- **The memory edit reached three more files than §6 named — revises §6's "Outside the repository" item.** §6 named only `feedback_mvn_permissions.md` and said the edit takes no commit. Four entries moved: that file was renamed to `feedback_build_permissions.md` (slug and `MEMORY.md` pointer included), `project_dev_workflow.md` swapped `mvn clean install` for `./gradlew clean fullBuild`, and `project_spotless_submodule_formatter_path.md` was deleted outright — a Maven-era hazard the epic's own formatter-anchoring subtask had already retired. All four were committed in the memory repository at the developer's request.
- **The `// ## Clean` heading went with the hook.** §6 C1 named lines 106–109; removing them left the phase heading at 104 with nothing under it, which `gradle.md` §"Block order" forbids ("label every group, the one holding a block's only entry included"). The edit is lines 104–110. The same deletion shifted the `snippets` attribute from §6 C1's stated lines 125–126 to 118–119 in the same file.
- **The `ci.yml` header needed two lines changed, not one.** §6 C3 named line 3; line 2 ends in `, and`, so deleting line 3 alone leaves a dangling clause. Line 2 now ends in a period.
- **Only two of the five `target/` directories were empty.** §4 called all five empty; `libs/asapp-commons-url`, `libs/asapp-http-clients` and `services/asapp-users-service` still held stale `classes`, `generated-sources`, `maven-status` and `test-classes` output from a 2026-08-06 Maven run. `rm -rf` covered both cases.

## 12. References

- `2026-07-22-gradle-api-documentation-design.md` §11 — the two independent build-tool detections, why `snippetsDir` was pinned to `target/`, and why the `clean` hook exists.
- `2026-08-01-gradle-database-migration-commands-design.md` §4 — the Liquibase task names and why `migrationSqlOutputFile` has no successor.
- `2026-08-01-gradle-git-hooks-design.md` — `InstallGitHooks`, and the coexistence window this subtask closes.
- `2026-08-05-gradle-sbom-design.md` — `includeConfigs`, `includeBuildSystem = false`, and the empty-SBOM guard.
- `2026-08-07-gradle-ci-workflow-design.md` — the two scaffolds C3 retires and why they were added.
- `2026-08-22-gradle-build-documentation-design.md` §11 — the first firing of the git-hook hazard.
