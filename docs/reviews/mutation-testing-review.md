# Task Review — Migrate mutation testing to Gradle

`da4f04a3..HEAD` · 8 files (stacked branch — `main` is still Maven, so the anchor is the coverage-migration commit, not `main`)

> Apply-now findings only — nothing was deferred to `TODO.md` this pass. Code review only; nothing committed. The migration is at full Maven parity (verified byte-for-byte against the old poms plus a live `./gradlew help --task pitest` run); these are the polish items to fix before closing.

0 must-fix · 2 should-fix · 1 nice-to-have

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | Eager `.get()` resolves the JDK-25 toolchain at configuration time | issue | S | Med |
| S2 | Trailing comma leaves an incomplete TODO note | issue | S | Low |

- [x] **S1 — Eager `.get()` resolves the JDK-25 toolchain at configuration time**
    - **Location:** `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:18`
    - **Description:** `jvmPath = javaToolchains.launcherFor { … }.get().executablePath` forces the Java-25 toolchain launcher to resolve during project *configuration*, on every Gradle invocation against the three domain services — not just when `pitest` runs.
    - **Why it matters:** It breaks Gradle's lazy-configuration model, adding toolchain-resolution cost and failure surface to unrelated commands (`tasks`, `help`, IDE sync, compile-only builds); `jvmPath` is `@Internal`, so deferring resolution costs nothing.
    - **Evidence:** The plugin declares `jvmPath` as a lazy `RegularFileProperty` consumed via `getJvmPath()?.getOrNull()`; only this call site forces eager evaluation.
    - **Recommended action:** Assign a lazy provider instead — `…launcherFor { … }.map { it.executablePath }` (the Kotlin DSL property setter accepts a `Provider` for a `RegularFileProperty`).
    - **Resolver notes:** No behavior change for `./gradlew pitest` — same JVM, same value; only *when* it is computed. One-liner, no test impact.
    - **Applied:** Changed the `jvmPath` assignment to a lazy `.map { it.executablePath }` in `asapp.domain-service-conventions.gradle.kts` and synced the matching prescription in `.claude/rules/gradle.md`; verified the domain service configures cleanly (`./gradlew :services:asapp-tasks-service:help`).

- [x] **S2 — Trailing comma leaves an incomplete TODO note**
    - **Location:** `TODO.md:40`
    - **Description:** The new note ends with a dangling `, ` instead of a finished phrase: `- **Note:** add blank lines to group the code of Gradle scripts, `.
    - **Why it matters:** Every other note in the file ends cleanly; this reads as a cut-off sentence that will confuse whoever picks up the "Clean Gradle files" bucket.
    - **Recommended action:** Drop the trailing comma and space — `add blank lines to group the code of Gradle scripts`.
    - **Resolver notes:** Only this line is affected; the note was introduced by this task.
    - **Applied:** Trailing comma already removed in the working-tree `TODO.md`; left uncommitted per the skill's no-commit-`TODO.md` guardrail (rides along with the branch's other `TODO.md` edits to close-task). No separate commit.

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | Mutation-target packages duplicated across services and within each file | improvement | S | Low |

- [x] **N1 — Mutation-target packages duplicated across services and within each file**
    - **Location:** `services/asapp-authentication-service/build.gradle.kts:13-23` · `services/asapp-tasks-service/build.gradle.kts:5-14` · `services/asapp-users-service/build.gradle.kts:28-37`
    - **Description:** The layer-target *shape* (`…domain.*` + `…application.*.in.service.*`) is repeated verbatim across all three service scripts (only the `<svc>` token differs), and within each file `targetTests` is an exact copy of `targetClasses`.
    - **Why it matters:** The "which layers we mutate" convention lives in six places with no single source of truth, so a layer rename becomes a six-edit change.
    - **Recommended action:** Bind both properties to one local `val pitestPackages` per script to remove the intra-file copy (full centralization isn't worth deriving `<svc>` from the project name for three services).
    - **Resolver notes:** Both the architecture and code reviewers judged this taste, not a defect — it mirrors the Maven poms' own duplication, and `.claude/rules/gradle.md` explicitly blesses per-service targets as "per-service data, not shared policy." Fine to leave as-is if the intra-file `val` isn't wanted.
    - **Applied:** Bound `targetClasses` and `targetTests` to one local `val pitestPackages` in each of the three domain-service scripts (intra-file dedup; targets kept per-service per the rule); verified all three configure cleanly (`./gradlew :services:asapp-{tasks,users,authentication}-service:help`).
