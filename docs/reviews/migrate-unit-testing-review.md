# Task Review — Migrate unit testing to Gradle

`main...HEAD` · task delta `0055053e` · 2 files

> Apply-now findings only — the deferred finding was routed to `TODO.md`. Code review only; nothing was committed.

0 must-fix · 0 should-fix · 1 nice-to-have

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | Document the engine/launcher contract for test modules | improvement | S | Low |

- [x] **N1 — Document the engine/launcher contract for test modules**
    - **Location:** `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:45-49` (launcher, applied to every java module) ↔ per-leaf / `asapp.service-conventions` `spring-boot-starter-*-test` deps (engine); engine-less leaf: `libs/asapp-commons-url/build.gradle.kts`
    - **Description:** The base `asapp.java-conventions` plugin gives every module `useJUnitPlatform()` plus the JUnit Platform launcher, but the JUnit engine only arrives where a test starter is declared — so a pure-library leaf with no test starter has a `test` task with no engine on its classpath.
    - **Why it matters:** Harmless today (`asapp-commons-url` has no test sources, so its `test` task is `NO-SOURCE`), but the `.claude/rules/gradle.md` Testing section doesn't state the contract — adding a `*Tests` class to such a module without a test starter fails with a confusing `compileTestJava` error, and the platform wiring to grep for lives in a different file/layer.
    - **Recommended action:** Add one line to the `gradle.md` Testing section — the base plugin supplies only the platform launcher and execution config; every module that runs tests must bring its own engine via a `spring-boot-starter-*-test` starter. No code change.
    - **Resolver notes:** Do not add a base `testImplementation` on the JUnit engine — that would burden test-less libraries like `asapp-commons-url`; the documented contract is the intended fix.
    - **Applied:** Appended a bullet to the `## Testing` section of `.claude/rules/gradle.md` — the base plugin supplies only the platform launcher, not a JUnit engine, so each test-running module supplies its own via a `spring-boot-starter-*-test` starter.
