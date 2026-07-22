# Task Review — Migrate formatting checks to Gradle

`504b7d6b..HEAD` (formatting subtask commits) · 5 files

> All findings below are **apply-now** — to be resolved before closing this task (per your selection; none deferred to `TODO.md`). This is a code review only — nothing has been committed.

Both reviewers empirically ran `spotlessCheck` / `--dry-run` against real modules and confirmed the config passes clean and mirrors Maven's Spotless setup 1:1 (Eclipse 4.35 + `asapp_formatter.xml`, importOrder groups incl. empty catch-all and trailing `com.attrigo`, licenseHeader, UNIX line endings). Plugin wiring, catalog placement, and convention-plugin altitude all match the established jacoco/pitest patterns. No must-fix issues.

0 must-fix · 1 should-fix · 2 nice-to-have

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | Inaccurate "built-in `package ` delimiter" claim in doc + comment | issue | S | Low |

- [x] **S1 — Inaccurate "built-in `package ` delimiter" claim in doc + comment**
    - **Location:**
        - `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:42` — comment `// default Java delimiter "package " == Maven`
        - `.claude/rules/gradle.md` (Formatting section) — "the Java format's built-in `package ` delimiter matches Maven's explicit one, so no delimiter argument is needed"
    - **Description:** Spotless's built-in Java license delimiter is `(package|import|public|class|module) `, not `package `; the equivalence with Maven's explicit `<delimiter>package </delimiter>` holds only because every source file in this repo leads with a `package` declaration.
    - **Why it matters:** A factually wrong claim in the conventions doc can mislead a future formatting/migration decision — the "== Maven" equivalence is coincidental-for-this-repo, not universal (a default-package file or `module-info.java` would diverge).
    - **Recommended action:** Reword to state the built-in default is broader but resolves identically here because all sources begin with `package` — or pass an explicit `"package "` delimiter to make the parity literal and self-documenting.
    - **Resolver notes:** Two spots to keep in sync — the `.kts` comment and the `gradle.md` prose. Whichever wording is chosen, apply it to both.
    - **Applied:** Pinned the delimiter explicitly — `licenseHeaderFile(rootProject.file("header-license"), "package ")` — so parity with Maven's `<delimiter>package </delimiter>` is literal, not coincidental; updated the `gradle.md` bullet to match and to note Spotless's actual broader default `(package|import|public|class|module) `. Verified `./gradlew spotlessCheck` passes clean on all 7 modules (identical output).

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | No documented escape hatch for the always-on `spotlessCheck` gate | improvement | S | Low |
| N2 | `cleanthat` unused-import engine substitution never exercised | improvement | S | Low |

- [x] **N1 — No documented escape hatch for the always-on `spotlessCheck` gate**
    - **Location:** `.claude/rules/gradle.md` (Formatting section) / inherited `check.dependsOn(spotlessCheck)` in `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts`
    - **Description:** Wiring `spotlessCheck` onto `check` unconditionally means a plain `./gradlew build` now fails on unformatted code, with no Maven-style opt-out (Maven's `spotless.check.skip=true` made this a local non-issue); no doc mentions the `-x spotlessCheck` escape hatch. Raised independently by both reviewers.
    - **Why it matters:** A developer mid-edit wanting a fast compile/test loop hits a new failure mode with no documented workaround.
    - **Recommended action:** Add one line to the `gradle.md` Formatting section noting `./gradlew build -x spotlessCheck` as the quick-iteration opt-out.
    - **Resolver notes:** The always-on decision itself is intentional and well-reasoned (faithful to Maven's `-Pci` / verify-phase intent) — this is purely about documenting the local-dev consequence, not disputing the decision. Related forward-looking notes already sit under the git-hook and CI subtasks in `TODO.md`.
    - **Applied:** Extended the "Keep `spotlessCheck` wired into `check`" bullet in `gradle.md` with the `./gradlew build -x spotlessCheck` local opt-out (the Gradle analog of Maven's `spotless.check.skip=true`).

- [x] **N2 — `cleanthat` unused-import engine substitution never exercised**
    - **Location:** `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:41` — `removeUnusedImports("cleanthat-javaparser-unnecessaryimport")`
    - **Description:** The switch from Maven's default `google-java-format` engine to `cleanthat` is a real behavioral divergence, but the current tree has zero unused imports, so `spotlessCheck` passing today never actually exercises the import-removal step.
    - **Why it matters:** If cleanthat's removal semantics differ from google-java-format on an edge case (static imports, imports used only in Javadoc `{@link}`), the first file that introduces an unused import silently gets a different canonical form than Maven would have produced — not a build break, but an unverified assumption becoming load-bearing.
    - **Recommended action:** Do a one-off manual round-trip — add a deliberately unused import to a scratch file, run `spotlessApply`, confirm removal, then discard.
    - **Resolver notes:** Documentation/verification gap, not a code defect; the engine choice is well-justified (avoids the daemon-wide `--add-exports jdk.compiler/…` the project's conventions forbid). Since this is a manual verification rather than a code change, "resolving" it means performing the round-trip and confirming the outcome.
    - **Applied:** Ran the round-trip on `libs/asapp-commons-url/.../TaskApiUrl.java` — added an unused `import java.util.List;`, confirmed `spotlessCheck` failed flagging its removal, ran `spotlessApply`, and confirmed cleanthat stripped it (file restored byte-for-byte to its committed state, `git diff` empty). Engine verified active and correct; scratch edit discarded. No code change committed.
