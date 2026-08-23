---
paths:
  - "**/build-logic/src/**/*.kt"
---

Rules for the build's own Kotlin sources — how a custom task type is laid out and how it is tested. Every build script, including `build-logic`'s own, is covered by `gradle.md`.

## Layout

- A task class lives in `com/attrigo/asapp/gradle/`; the line that registers it lives in a convention plugin — never both in a build script. The `asapp.*.gradle.kts` plugin files stay at the top of `src/main/kotlin`, with no package folder

## Functional tests

- Test a custom task type by running a real build against it — Gradle's `ProjectBuilder` never runs tasks, so it can only check configuration
- How these tests are written:
    - `<behavior>_<Condition>` method names, first letter lower-cased per Kotlin convention
    - A flat class — no `@Nested`
    - `// Given` / `// When` / `// Then` blocks in every test
    - A class KDoc with a Coverage list, written as behaviours rather than class, field or method names
    - Fixed fixture values, never random ones
    - `@BeforeEach` resets whatever the tests share
    - `actual` for the result; when a test runs two builds, name each result instead
    - `assertSoftly` when two or more assertions check the same value, a separate block per value
    - `describedAs(...)` before each soft assertion, never `as(...)` — `as` is a Kotlin keyword. The description can be a full sentence
