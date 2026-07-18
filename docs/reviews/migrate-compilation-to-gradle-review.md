# Task Review — Migrate compilation to Gradle

`main...HEAD` · 4 files

> Apply-now findings only. Two deferred findings — run the mapper generation on test sources, and extract a shared version-catalog accessor across convention plugins — were routed to `TODO.md` (Backlog → build). This was a code review only; nothing has been committed.

1 should-fix · 2 nice-to-have

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | `gradle.md` frontmatter doesn't route to the version catalog it governs | issue | S | Med |

- [x] **S1 — `gradle.md` frontmatter doesn't route to the version catalog it governs**
    - **Location:** `.claude/rules/gradle.md:2-4` (frontmatter) vs `:61` (body) — the rule governs `gradle/libs.versions.toml`, which this task edits
    - **Description:** The `paths:` frontmatter lists only `**/*.gradle.kts` and `**/gradle.properties`, but the rule's own "Version catalog" bullet explicitly governs `libs.versions.toml`.
    - **Why it matters:** A future diff touching only `gradle/libs.versions.toml` (no `.gradle.kts` / `gradle.properties` alongside) won't be path-routed to `gradle.md`, silently defeating this rule's own applicability.
    - **Evidence:** frontmatter `paths: ["**/*.gradle.kts", "**/gradle.properties"]`; body line 61: `**Version catalog** (`libs.versions.toml`): scope with `#`, origin with `##` …`
    - **Recommended action:** Add a catalog glob (e.g. `**/libs.versions.toml`) to the frontmatter `paths`.
    - **Applied:** Added `**/libs.versions.toml` as a third `paths` entry (after `**/gradle.properties`) so a diff touching only the version catalog routes to `gradle.md`.

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | Java version `25` hardcoded twice, not single-sourced | improvement | S | Low |
| N2 | Toolchain + `release` pairing unexplained in-script | improvement | S | Med |

- [x] **N1 — Java version `25` hardcoded twice, not single-sourced**
    - **Location:** `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:10` (`JavaLanguageVersion.of(25)`) and `:18` (`options.release = 25`)
    - **Description:** The compile version `25` is hardcoded twice in the same file and, unlike every other version in the build, is absent from `gradle/libs.versions.toml`.
    - **Why it matters:** A future bump can update one literal but not the other — toolchain 26 / release 25 silently targets old bytecode; toolchain 25 / release 26 fails the build. The migration's whole theme is single-sourcing, so a bare literal here erodes that.
    - **Recommended action:** Bind once — a local `val javaVersion = 25` referenced by both, or a `[versions] java` catalog entry.
    - **Resolver notes:** Raised by both reviewers. The toolchain / `options.release` duality itself is correct and intended — this is only about the repeated literal.
    - **Applied:** Introduced a local `val javaVersion = 25` in `asapp.java-conventions.gradle.kts`, referenced by both the toolchain and `options.release`; both declarations kept.

- [x] **N2 — Toolchain + `release` pairing unexplained in-script**
    - **Location:** `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:8-12` (toolchain block, uncommented) vs `:14-21` (JavaCompile block, commented)
    - **Description:** The change adds a detailed comment for `-parameters` but none for why both `toolchain { languageVersion = 25 }` and `options.release = 25` are set, so to an unfamiliar reader the pair reads as redundant.
    - **Why it matters:** `.claude/rules/gradle.md` distinguishes them (toolchain = which JDK compiles/tests; `release` = bytecode/API level); without that rationale in the script, a future "simplification" could drop one and silently break the intended pinning.
    - **Recommended action:** Add a short comment on the `java { toolchain { … } }` block: it pins the compiling/testing JDK, distinct from `options.release` pinning the bytecode/API level.
    - **Applied:** Added a purpose comment to the toolchain block and per-option labels (Release / Encoding / CompilerArgs) above the compiler block; also trimmed the BOM comment to one line.
