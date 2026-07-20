# Task Review — Migrate coverage reporting to Gradle

`d6cbf80b..HEAD` (coverage-reporting delta of `main...HEAD`) · 5 files

> Apply-now findings only — deferred findings (N2, N3) were routed to `TODO.md`, and S2 is already tracked at the "Migrate packaging" subtask. Code review only: nothing has been committed. `asapp-resolve-review-issues` fixes these; the developer ticks each box as it lands.

**1 should-fix · 1 nice-to-have**

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | Report-format policy duplicated & expressed inconsistently across report tasks | improvement | S | Med |

- [x] **S1 — Report-format policy duplicated & expressed inconsistently across report tasks**
    - **Location:**
        - `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts:82-87` and `:98-103` — byte-for-byte identical `reports { html/xml/csv }` + `group`/`sourceSets`
        - `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:41-43` — unit report sets no `reports {}` block, relying on Gradle's implicit default
    - **Description:** The "HTML only" report policy is duplicated across the two service-tier report tasks and expressed implicitly for the unit tier but explicitly for the integration/merged tiers.
    - **Why it matters:** One policy lives in three places; a future change (e.g. enabling XML for a CI consumer) must be edited in lockstep, and a Gradle-wrapper bump that shifts the implicit default would silently diverge the unit tier from the others.
    - **Recommended action:** Single-source the policy with `tasks.withType<JacocoReport>().configureEach { reports { html.required = true; xml.required = false; csv.required = false } }` in `asapp.java-conventions`, which normalizes the unit tier and also catches the later-registered integration/merged reports.
    - **Resolver notes:** `configureEach` in the base plugin also normalizes the libs `jacocoTestReport` — intended. A narrower alternative is a private helper fn in `service-conventions` deduping just the two service tasks; no shared build-logic Kotlin util file exists yet, so either approach stays in-file. No project rule covers Kotlin-DSL duplication (community standard: DRY / Extract Method).
    - **Applied:** Hoisted the HTML-only `reports {}` policy into a single `tasks.withType<JacocoReport>().configureEach {}` in `asapp.java-conventions` and removed the two duplicated blocks in `asapp.service-conventions`; updated the `gradle.md` Coverage note. The unit `jacocoTestReport` is now HTML-only across all modules (restores Maven parity). Verified via `./gradlew … --dry-run` (exit 0).

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | `jacocoAggregateReport` name overloads Gradle's cross-project "aggregation" term | improvement | S | Med |

- [x] **N1 — `jacocoAggregateReport` name overloads Gradle's cross-project "aggregation" term**
    - **Location:** `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts:91`; mirrored in `.claude/rules/gradle.md` Coverage section
    - **Description:** This task merges unit + integration execution data *within a single module*, but Gradle's own `jacoco-report-aggregation` plugin reserves "aggregate" for *cross-project* roll-up (its `testCodeCoverageReport`).
    - **Why it matters:** The TODO defers real cross-module coverage roll-up to the "Migrate the full build" subtask; when that lands, a per-module `jacocoAggregateReport` and a repo-wide aggregate would compete for the same word — a naming collision that misleads readers about scope.
    - **Recommended action:** Rename the task to `jacocoMergedReport` (matches the subtask's own "merged unit and integration…" wording), reserving "aggregate" for the future cross-project report, and update the mirror in `.claude/rules/gradle.md`.
    - **Resolver notes:** Cheapest to do now, before the name is referenced by the full-build subtask or documented further.
    - **Applied:** Renamed the task `jacocoAggregateReport` → `jacocoMergedReport` in `asapp.service-conventions`, updated the mention in `gradle.md`, and adjusted the report-format comment in `asapp.java-conventions`; pure rename, report output unchanged. Verified via `./gradlew … --dry-run` (exit 0).
