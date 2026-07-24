# Task Review — Migrate javadoc and sources jar generation to Gradle

`main...HEAD` (task commit `c1ce5ca8`) · 3 files in scope

> Every finding below is recorded here for `asapp-resolve-review-issues` to act on (nothing was deferred to `TODO.md`). This is a code review only — no code was changed, committed, or merged. The design spec (`docs/superpowers/**`) and `TODO.md` were excluded from scope.

Reviewers: `code-reviewer`, `architect-reviewer`, `devops-engineer`. Two of them ran the actual Gradle build and confirmed full Maven parity empirically — doclint flag (`-Xdoclint:all,-missing`), jar output paths/classifiers/contents, opt-in gating (jars absent from the `build`/`check`/`assemble` graphs), 5-module coverage, the implicit `javadoc` task dependency, the JDK 25 toolchain, and up-to-date caching. The deliberate block-duplication across the two archetype plugins was judged sound.

**0 must-fix · 0 should-fix · 4 nice-to-have**

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | Duplicated "by-design" blocks aren't byte-identical (comment punctuation) | issue | S | Low |
| N2 | No repayment trigger recorded for the elective duplication | improvement | S | Med |
| N3 | New `Jar` tasks don't set reproducible-archive options | improvement | S | Low |
| N4 | Plain `Jar` tasks won't auto-attach to a future `maven-publish` | improvement | S | Low |

- [x] **N1 — Duplicated "by-design" blocks aren't byte-identical (comment punctuation)**
    - **Location:**
        - `build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts:20,29`
        - `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:114,123`
    - **Description:** The two convention plugins' `javadocJar`/`sourcesJar` blocks are meant to be identical twins, but the inline `// from:` comments use a semicolon in the library copy and an em dash in the domain-service copy.
    - **Why it matters:** `gradle.md` justifies the duplication by asserting the blocks are "repeated verbatim by design"; a punctuation divergence on the very commit that introduced them makes that claim already false and defeats future "are these still in sync?" diffing.
    - **Evidence:** `// from: what to package; the javadoc task's output.` (library) vs `// from: what to package — the javadoc task's output.` (domain-service); the same divergence repeats on the `sourcesJar` comment.
    - **Recommended action:** Pick one connective (the em dash matches the other new comments in the file) and make both files byte-identical.
    - **Resolver notes:** Cheap copy-paste fix, no functional impact. Flagged independently by `code-reviewer` and `architect-reviewer`.
    - **Applied:** Standardized both `// from:` comments in `asapp.domain-service-conventions.gradle.kts` on the semicolon; a `diff` confirms the javadoc/doclint/jar blocks in the two convention plugins are now byte-identical.

- [x] **N2 — No repayment trigger recorded for the elective duplication**
    - **Location:** the duplicated javadoc/sources block in both convention plugins; documentation home `.claude/rules/gradle.md` ("## Javadoc & sources jars")
    - **Description:** The duplication is defensible today, but it is *elective* — a shared `asapp.javadoc-sources-conventions` plugin applied by both archetypes is a real alternative — and the doc's cited precedent (the duplicated version-catalog accessor) is weaker than stated: that accessor is *forced* by precompiled-script scoping, whereas this block is not.
    - **Why it matters:** The plugin taxonomy now carries its first "duplicate-a-concern-across-two-archetypes" precedent with no recorded point at which the debt should be repaid, inviting the same copy-paste for future publishing/BOM concerns that share the identical {2 libs + 3 domain services} activation set.
    - **Recommended action:** Note in `gradle.md` a concrete repayment trigger — extract to a shared `asapp.<concern>-conventions` plugin the moment a third module joins the {libs + domain-services} activation set or the block grows beyond bare task registration (e.g. javadoc links / `-quiet` / offline options).
    - **Resolver notes:** Taxonomy-evolution note, not a defect; keep the duplication for now. From `architect-reviewer`.
    - **Applied:** Reframed the "by design" bullet in `gradle.md`'s "## Javadoc & sources jars" section as *accepted elective duplication* (correcting the version-catalog-accessor precedent, whose duplication is *forced* by script scoping, not elective), and added a repayment-trigger bullet — extract a shared `asapp.javadoc-sources-conventions` plugin once a third module joins the {2 libs + 3 domain services} set or the block grows beyond bare task registration.

- [x] **N3 — New `Jar` tasks don't set reproducible-archive options**
    - **Location:** `javadocJar` / `sourcesJar` registrations in both `asapp.library-conventions.gradle.kts` and `asapp.domain-service-conventions.gradle.kts`
    - **Description:** The tasks use Gradle defaults (`preserveFileTimestamps = true`, `reproducibleFileOrder = false`), so two builds of the same commit on different machines/times can produce byte-different jars.
    - **Why it matters:** Undercuts bit-for-bit reproducibility — the point of pinning everything else in this migration — and becomes relevant the moment a release/publish stage signs or compares these artifacts.
    - **Recommended action:** When reproducibility becomes a goal, add it once repo-wide via `tasks.withType<AbstractArchiveTask>().configureEach { isPreserveFileTimestamps = false; isReproducibleFileOrder = true }` in `asapp.java-conventions` — not per-task here.
    - **Resolver notes:** Not a regression — no existing `Jar` task (including the plain `jar`) sets these, and the Maven baseline lacks `project.build.outputTimestamp` too; fixing it only for these two tasks would be inconsistent and would diverge from this stage's strict-parity target. Best deferred in practice to the full-build/release-automation subtask. Flagged independently by `devops-engineer` and `code-reviewer`.
    - **Applied:** Deferred with no code change — logged as a new `Backlog → Technical → build` bullet ("Make all build jars byte-reproducible …"), to be applied repo-wide in `asapp.java-conventions` when artifacts are published or signed.

- [x] **N4 — Plain `Jar` tasks won't auto-attach to a future `maven-publish`**
    - **Location:** `javadocJar` / `sourcesJar` registrations in both plugins; forward hand-off to the "Migrate packaging" subtask (`asapp.service-conventions.gradle.kts` packaging marker)
    - **Description:** Using plain `Jar` tasks (correct, to keep them off `assemble`) means they are not registered as `java` documentation variants, so a future `maven-publish` migration must attach them manually via `artifact(tasks["javadocJar"])` / `artifact(tasks["sourcesJar"])`.
    - **Why it matters:** Under Maven these jars rode `install`/`deploy` automatically; if the publishing subtask forgets the explicit wiring, the published POM silently loses its `-javadoc`/`-sources` classifiers — a Maven-parity regression discovered only at publish time.
    - **Recommended action:** Add a one-line forward note to the "Migrate packaging" subtask so the publishing work attaches these two jars to the publication explicitly.
    - **Resolver notes:** Boundary hand-off note; out of scope to wire now (publishing not yet migrated). From `architect-reviewer`.
    - **Applied:** Recorded a hand-off `- **Note:**` under the "Migrate packaging to Gradle" subtask in `TODO.md` so the publishing work attaches the javadoc/sources jars explicitly (else the published artifact silently drops its `-javadoc`/`-sources` classifiers).
