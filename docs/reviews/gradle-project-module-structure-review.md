# Task Review — Set up the Gradle project and module structure

`main...HEAD` · 18 files

Apply-now findings only (the should-fix finding on the review rule's own coverage was deferred to `TODO.md`); code review only, nothing committed.

0 must-fix · 0 should-fix · 4 nice-to-have

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | No build-performance toggles set yet | Improvement | S | Med |
| N2 | Plan layered convention-plugin taxonomy | Improvement | M | Med |
| N3 | `.gitignore` Gradle section thin | Improvement | S | Low |
| N4 | Empty placeholder build scripts unmarked | Improvement | S | Low |

- [x] **N1 — No build-performance toggles set yet, despite being the migration's stated goal**
    - **Location:** `gradle.properties:1-2`
    - **Description:** `gradle.properties` only carries `group`/`version`; none of `org.gradle.parallel`, `org.gradle.caching`, `org.gradle.configuration-cache` are set, even though 0.5.0's goal is explicitly "cached, parallel, and incremental."
    - **Why it matters:** The configuration-cache decision constrains how every upcoming convention plugin must be authored (no `Project` access at execution time, no shared mutable state); deciding it late risks writing CC-incompatible convention plugins in the very next subtask and reworking them.
    - **Recommended action:** Decide now whether these toggles land in this foundational step or a named later step, and record that convention plugins must be configuration-cache-compatible.
    - **Resolver notes:** Distinct from the deliberately-deferred dependency/test/coverage config — this is setup-file content, not build logic.
    - **Applied:** Enabled `org.gradle.caching=true`; added `org.gradle.parallel=false` (explicitly disabled, not commented out) with a comment noting it only works reliably on native Windows and throws an IOException under WSL; added a comment deferring `org.gradle.configuration-cache` until pitest, the Liquibase plugin, and `bootBuildImage`/Docker image building are wired up and verified compatible.

- [x] **N2 — Plan a layered convention-plugin taxonomy so the Maven aggregator split survives the migration**
    - **Location:**
        - `libs/pom.xml`, `services/pom.xml` (baseline being ported)
        - `build-logic/` (currently no `src/main/kotlin`)
        - the 7 empty per-module `build.gradle.kts` files
    - **Description:** The Maven build encodes distinct build shapes — libs (jar + javadoc/sources, UT-only jacoco, pitest skipped) vs. services (bootJar + image, failsafe/IT, UT+IT+merge jacoco, asciidoctor, cyclonedx, liquibase, git-commit-id, spring-cloud BOM, mapstruct AP) — plus intra-service variance (config/discovery skip pitest and omit data/liquibase/mapstruct; auth/tasks/users scope pitest to the hexagonal locus) — that the current empty `:libs`/`:services` intermediates don't yet carry.
    - **Why it matters:** If the next migration subtask collapses these into one catch-all plugin, `build-logic` becomes the dumping ground the rule doc itself warns against, and the hexagonal pitest-scoping — a real architectural signal — gets lost.
    - **Recommended action:** Plan a layered set of convention plugins (e.g. `asapp.java-conventions` → `asapp.library-conventions` / `asapp.spring-boot-service-conventions`), applying pitest scoping only to the domain services.
    - **Resolver notes:** The current structure fully supports this; nothing to change in this diff itself — it's a heads-up for "Migrate dependency management to Gradle."
    - **Applied:** Scaffolded 3 empty precompiled convention-plugin placeholders in `build-logic/src/main/kotlin/` — `asapp.java-conventions`, `asapp.library-conventions`, `asapp.service-conventions` (renamed from the initially-suggested `spring-boot-conventions` for role-based symmetry with `library-conventions`) — each carrying only a one-line comment describing its future responsibility; none wired into any module yet. Updated `gradle.md`'s own naming example to match.

- [x] **N3 — `.gitignore`'s Gradle section is thin, `build/` sits under the wrong heading**
    - **Location:** `.gitignore:42-44` (new `### Gradle ###` block) vs. pre-existing `build/` at line 28 under `### NetBeans ###`
    - **Description:** The new Gradle section ignores only `.gradle/`; the actual Gradle build-output directory (`build/`) is covered only by a pre-existing line filed under the NetBeans heading.
    - **Why it matters:** Purely organizational today, but once Maven's `target/` line is removed in the final migration subtask, the Gradle-relevant ignores will be scattered and easy to mis-edit.
    - **Recommended action:** Consolidate `build/` under the `### Gradle ###` heading when Maven is retired.
    - **Applied:** Moved `build/` and its two `src/main`/`src/test` negation exceptions from `### NetBeans ###` into `### Gradle ###`, alongside `.gradle/`; `### NetBeans ###` now holds only its own 5 entries.

- [x] **N4 — Empty placeholder build scripts carry no self-documenting marker**
    - **Location:** `build.gradle.kts:1` (root) and all 7 leaf `build.gradle.kts` files
    - **Description:** All 8 build scripts are checked in fully empty with nothing indicating they're intentional scaffolding for later subtasks rather than an oversight.
    - **Why it matters:** Gradle doesn't require these files to exist for module recognition, so a future contributor could mistake a tracked empty file for dead scaffolding.
    - **Recommended action:** Optional one-line comment (e.g. `// populated in a later Gradle migration subtask`) — skip if the very next subtask fills these in anyway.
    - **Applied:** Added `// populated in a later Gradle migration subtask` as the sole line in all 8 build scripts (root + 7 modules), matching the marker style used for the N2 convention-plugin placeholders.
