# Task Review — Migrate API documentation generation to Gradle

`main...HEAD` · 5 files

> Apply-now findings only — deferred findings were routed to `TODO.md`. Code review only; nothing was committed. Checkboxes are ticked as findings are resolved (by the developer or `asapp-resolve-review-issues`).

0 must-fix · 3 should-fix · 2 nice-to-have

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | Restore the `// Org` origin comment | Issue | S | Low |
| S2 | Move AsciiDoctor catalog entries to the `Org` origin group | Issue | S | Low |
| S3 | Delete the snippets dir on `clean` during Maven coexistence | Issue | S | Med |

- [x] **S1 — Restore the `// Org` origin comment**
    - **Location:** `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:62`
    - **Description:** This commit changed the origin comment `// Org` to `// Org/` above the `testcontainers-shared` dependency — an edit unrelated to the API-docs feature.
    - **Why it matters:** The ordering rule fixes the origin vocabulary to `ASAPP / Spring Boot / Spring Cloud / Spring / Org / Other`; the stray `/` breaks that token and reads as an accidental keystroke, eroding trust in the diff.
    - **Evidence:** diff hunk `-    // Org` / `+    // Org/` (`git show 5754b8e5`).
    - **Recommended action:** Restore the comment to `// Org`.
    - **Applied:** Reverted the stray `// Org/` back to `// Org` (line 62).

- [x] **S2 — Move AsciiDoctor catalog entries to the `Org` origin group**
    - **Location:**
        - `gradle/libs.versions.toml:8-9` (`[versions]`)
        - `gradle/libs.versions.toml:58` (`[libraries]`)
        - `build-logic/build.gradle.kts:17`
    - **Description:** The three new `org.asciidoctor:*` entries were filed under the `Other` origin group, but the catalog buckets `Org` vs `Other` strictly by whether the module's groupId starts with `org.`.
    - **Why it matters:** The catalog is the project's reference pattern; mis-bucketing misleads contributors and breaks the mirrored-from-Maven grouping — `org.jacoco`, `org.mapstruct`, `org.pitest`, `org.testcontainers`, `org.bouncycastle` all already sit under `Org`.
    - **Recommended action:** Move `asciidoctor-gradle` + `asciidoctorj` (versions), `asciidoctor-gradle-plugin` (library), and the `implementation(libs.asciidoctor.gradle.plugin)` line into an `Org` group.
    - **Resolver notes:** Keep each origin alphabetical — under `Org` the version/library order becomes `asciidoctor-gradle` < `asciidoctorj` < `jacoco`; in `build-logic/build.gradle.kts` add a `// Org` group above `// Other`. Leave the `spring-restdocs-asciidoctor` entry under `## Spring` — that one is correctly placed.
    - **Applied:** Moved `asciidoctor-gradle`/`asciidoctorj` (versions) and `asciidoctor-gradle-plugin` (library) into `Org` (before `jacoco`), and added a `// Org` group in `build-logic/build.gradle.kts`; no value changes.

- [x] **S3 — Delete the snippets dir on `clean` during Maven coexistence**
    - **Location:** `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:79` (`snippetsDir`), interacting with the `clean` task from `asapp.java-conventions`.
    - **Description:** `snippetsDir` resolves to `target/generated-snippets`, outside Gradle's `build/`, so `./gradlew clean` never removes it — unlike every other generated artifact (coverage/pitest reports, compiled classes).
    - **Why it matters:** `clean asciidoctor` silently reuses stale snippets, so renamed/removed endpoints leak into `api-guide.html`. A live run confirmed `clean` deleted `build/docs/asciidoc/api-guide.html` while leaving `target/generated-snippets/**` fully intact.
    - **Recommended action:** Add `tasks.named<Delete>("clean") { delete(snippetsDir) }` so `clean` covers the snippets dir during the Maven-coexistence window.
    - **Resolver notes:** Interim mitigation only — the full fix (revert `snippetsDir` to `layout.buildDirectory.dir("generated-snippets")`) is tracked under the Maven-removal subtask in `TODO.md`, at which point this hook becomes redundant. It does not address the separate hazard of a `mvn` run mutating this Gradle-tracked output out-of-band (also noted in `TODO.md`).
    - **Applied:** Added a `Delete`-task `clean` hook that removes `target/generated-snippets`; committed alongside `TODO.md` notes tracking the hook's removal at Maven removal. Verified `build-logic` compiles (`./gradlew :services:asapp-tasks-service:help`).

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | Decouple API-doc generation from the full integration tier | Improvement | M | Low |
| N2 | Order the `asciidoctorExt` dependency block by the scope/origin convention | Improvement | S | Low |

- [x] **N1 — Decouple API-doc generation from the full integration tier**
    - **Location:** `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:93` (`asciidoctor.dependsOn(integrationTest)`)
    - **Description:** `asciidoctor` depends on the entire `integrationTest` task, which runs every `*IT` (all Testcontainers, 1g heap), even though only the `*ApiDocumentationIT` classes emit REST Docs snippets.
    - **Why it matters:** `./gradlew asciidoctor` pays the full integration-suite cost with no cheap docs-only path. Impact is bounded — docs are opt-in and run rarely — and this matches the established pattern (`jacocoIntegrationTestReport` also depends on the full tier; Maven bound docs to `post-integration-test`).
    - **Recommended action:** Register a dedicated `Test` task that includes only `**/*ApiDocumentationIT.class`, emits the snippets, and point `asciidoctor.dependsOn(...)` at it instead of the full `integrationTest`.
    - **Resolver notes:** This is a test-task-architecture change, not a one-liner — the new task reuses the `test` source set (like `integrationTest`) and must carry the snippets `outputs.dir`. Confirm both `./gradlew asciidoctor` and `./gradlew check` still behave (the full `integrationTest` stays on `check`).
    - **Applied:** Deferred to `TODO.md` backlog (`#### build`) rather than implemented — out of the migration's parity scope. Investigation confirmed the `*ApiDocumentationIT` tests are Docker-free `@WebMvcTest` slices, so a gated design can give a fast standalone docs path while the full build stays Maven-faithful (all ITs → then docs); captured for post-migration.

- [x] **N2 — Order the `asciidoctorExt` dependency block by the scope/origin convention**
    - **Location:** `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:69-72` (the `asciidoctorExt` configuration + its `dependencies {}` block)
    - **Description:** The `asciidoctorExt` configuration and its dedicated `dependencies {}` block sit after the main dependency block, outside the `CVE / Compile / Runtime / Test` scope + origin structure the ordering rule prescribes for dependency blocks.
    - **Why it matters:** In a file the ordering rule otherwise governs, this lands as an unstructured exception; a contributor following the convention can't tell whether it's deliberate.
    - **Recommended action:** Either fold the `asciidoctorExt(...)` dependency into the ordered structure under a `Build` origin comment, or keep the cohesive "API-docs feature block" and mark it an intentional exception.
    - **Resolver notes:** The `code-reviewer` judged the current bottom-of-file feature-block grouping (config + dependency + task together) a reasonable readability trade-off. Also overlaps with the in-version "Clean Gradle files" subtask (0.5.0), which tracks build-script block ordering — reconcile there if that lands first.
    - **Applied:** Kept as-is — the block stands as an intentional, cohesive API-docs feature grouping; original comment retained, no code change.
