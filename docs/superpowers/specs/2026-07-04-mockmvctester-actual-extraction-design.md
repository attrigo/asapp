# Refactor MockMvcTester Tests to Extract the Response into `actual` — Design

**Date:** 2026-07-04
**Status:** Implemented
**Targets:** `asapp-tasks-service`, `asapp-users-service`, `asapp-authentication-service`

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Other) drives this work:

> Refactor all MockMvcTester tests to extract the response into `actual` before
> asserting (align AAA structure with RestTestClient pattern)

The `@WebMvcTest` slice tests assert the HTTP response **inline**, chaining every
assertion directly off `perform(...)` under a single `// When & Then` block, with no
result variable:

```java
// When & Then
mockMvcTester.perform(requestBuilder)
             .assertThat()
             .hasStatus(HttpStatus.BAD_REQUEST)
             .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
             .bodyJson()
             .convertTo(String.class)
             .satisfies(json -> assertThatJson(json).isObject()...);
```

The `*E2EIT` tests already follow the project's AAA convention: bind the SUT response
to `var actual` in `// When`, then assert on it in a separate `// Then`
(`2026-06-27-e2eit-json-assertions-design.md`). This task brings the MockMvcTester tier
into the same shape, so the whole test suite reads consistently and honours the
`actual` naming rule (`testing-core.md` §3.1: the method result is named `actual`).

## 2. Decisions (from brainstorming)

1. **`actual` binds to the `MvcTestResult`** — the SUT's response object. MockMvcTester's
   AssertJ API has **no getter that returns the raw body as a `String`** (confirmed
   against the Spring 7 Javadoc: `body()`/`bodyText()`/`bodyJson()` all return assertion
   objects); the RestTestClient tests' `actual` is a `String` only because
   `.getResponseBody()` hands one back. For MockMvcTester, the response *is* the
   `MvcTestResult`, so that is what `actual` holds. This is the officially documented
   act/assert split — the Spring reference shows
   `MvcTestResult result = mvc...exchange(); assertThat(result)...` for exactly this
   "run multiple assertions on one result" case.
2. **Assert with static `assertThat(actual)`** (AssertJ's
   `org.assertj.core.api.Assertions.assertThat`). `MvcTestResult` is an `AssertProvider`,
   so `assertThat(actual)` returns the same `MvcTestResultAssert` the current
   `.assertThat()` call yields — every downstream assertion is unchanged. Chosen over the
   instance form `actual.assertThat()` because it reads as a normal assertion and matches
   how `actual` is asserted elsewhere in the suite (`assertThat(actual)` /
   `assertThatJson(actual)`).
3. **The body-assertion chain is unchanged.** Everything from `.hasStatus(...)` through
   `.hasContentType(...).bodyJson().convertTo(String.class).satisfies(json -> assertThatJson(json)...)`
   is copied verbatim; only its head moves from `mockMvcTester.perform(requestBuilder).assertThat()`
   to `assertThat(actual)`. The JsonUnit body-assertion convention stays as-is.
4. **`// When & Then` splits into `// When` + `// Then`.** The `// When` block performs the
   request and binds `actual`; the `// Then` block asserts on `actual`. `testing-core.md`
   §2.1 stays accurate as written — its "use `// When & Then` when fluently chained"
   principle is behaviour-based, and it still applies to the plain-MockMvc RestDocs tests
   (see §5), which remain chained. **No rule-doc change.**
5. **Request building is unchanged.** Requests keep the existing
   `perform(requestBuilder)` form built with static `MockMvcRequestBuilders.get/post/put/delete`.
   The fluent `mockMvcTester.get().uri(...).exchange()` API would mirror RestTestClient
   even more closely but changes request *building*, not extraction — out of scope.

## 3. The transformation

Uniform, mechanical, applied to every MockMvcTester `@Test` method:

**Before**
```java
// When & Then
mockMvcTester.perform(requestBuilder)
             .assertThat()
             .hasStatus(HttpStatus.BAD_REQUEST)
             .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
             .bodyJson()
             .convertTo(String.class)
             .satisfies(json -> assertThatJson(json).isObject()
                                                    .containsEntry("title", "Bad Request")
                                                    .containsEntry("status", 400)
                                                    .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                    .containsEntry("instance", "/api/tasks/1"));
```

**After**
```java
// When
var actual = mockMvcTester.perform(requestBuilder);

// Then
assertThat(actual).hasStatus(HttpStatus.BAD_REQUEST)
                  .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                  .bodyJson()
                  .convertTo(String.class)
                  .satisfies(json -> assertThatJson(json).isObject()
                                                         .containsEntry("title", "Bad Request")
                                                         .containsEntry("status", 400)
                                                         .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                         .containsEntry("instance", "/api/tasks/1"));
```

Per-file: add `import static org.assertj.core.api.Assertions.assertThat;` (once, only if
not already present). The `// Given` block is untouched.

## 4. Affected files

All changes are test-only; no production code is touched. 91 `@Test` methods across 7 files:

- `services/asapp-tasks-service/.../infrastructure/task/in/TaskRestControllerIT.java` (19)
- `services/asapp-tasks-service/.../infrastructure/error/GlobalExceptionHandlerIT.java` (3)
- `services/asapp-users-service/.../infrastructure/user/in/UserRestControllerIT.java` (21)
- `services/asapp-users-service/.../infrastructure/error/GlobalExceptionHandlerIT.java` (3)
- `services/asapp-authentication-service/.../infrastructure/user/in/UserRestControllerIT.java` (22)
- `services/asapp-authentication-service/.../infrastructure/authentication/in/AuthenticationRestControllerIT.java` (12)
- `services/asapp-authentication-service/.../infrastructure/error/GlobalExceptionHandlerIT.java` (11)

No POM changes. The `WebMvcTestContext` base classes (which declare the
`MockMvcTester` field) are unchanged.

## 5. Out of scope

- **Plain-MockMvc RestDocs tests** (`TaskApiDocumentationIT`, `UserApiDocumentationIT`,
  `AuthenticationApiDocumentationIT` extending `RestDocsWebMvcTestContext`). These use
  classic `mockMvc.perform(...).andExpect(...).andDo(document(...))`, are genuinely
  fluently chained, and keep `// When & Then`. Not MockMvcTester.
- Rewriting requests to the fluent `mockMvcTester.get().uri(...).exchange()` API.
- Any change to the `.bodyJson().convertTo(String.class).satisfies(assertThatJson...)`
  body-assertion mechanism.
- Any new test scenarios, endpoints, or production-code changes.

## 6. Watch-points

- **Import reconciliation.** Add `org.assertj.core.api.Assertions.assertThat` only where
  absent; do not duplicate. If a file already statically imports a different `assertThat`,
  reconcile so `assertThat(actual)` resolves to the AssertJ one.
- **Inline request builders.** A few tests may pass the request builder inline rather than
  via a `requestBuilder` local; bind the `perform(...)` result to `actual` regardless of
  how the request is constructed.
- **Formatting.** Run `mvn spotless:apply` from the repo root with `-pl <module>` after
  edits — the assertion chain re-aligns under `assertThat(actual)`; Spotless handles it.

## 7. Testing strategy

Like-for-like structural refactor — **no new test scenarios**, each test still exercises
the same case and must still pass. These are `@WebMvcTest` slice `*IT` tests (Failsafe, no
Testcontainers — lightweight).

Verification: `mvn clean verify` for the three target services (or the 7 test classes
targeted). This runs integration tests, so the run is gated on developer approval per the
Maven-permissions convention.

## 8. Acceptance criteria

A change set satisfying this design must:

- Bind the response of every MockMvcTester test to `var actual = mockMvcTester.perform(...)`
  under `// When`, and assert on it under a separate `// Then`.
- Assert via static `assertThat(actual)` — **no** `mockMvcTester.perform(...).assertThat()`
  inline chains remain in the 7 target files.
- Contain no `// When & Then` blocks in the 7 target files (all split into `// When` / `// Then`).
- Add `import static org.assertj.core.api.Assertions.assertThat;` to each target file (once).
- Leave the `.hasStatus(...)...bodyJson().convertTo(String.class).satisfies(assertThatJson...)`
  chain and every `// Given` block otherwise unchanged.
- Leave the RestDocs `*ApiDocumentationIT` tests and all production code untouched.
- Be Spotless-formatted and pass `mvn clean verify` for the three services (run by the developer).

## 9. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-07-04-mockmvctester-actual-extraction.md`) were written before implementation. The core change shipped substantially as designed — every MockMvcTester `@Test` method across the 7 target `*IT` files now binds the response to `var actual = mockMvcTester.perform(...)` under `// When` and asserts via static `assertThat(actual)` under a separate `// Then`, with the `.hasStatus(...)...bodyJson().convertTo(String.class).satisfies(assertThatJson...)` chain and every `// Given` block left intact.

The canonical implementation is the current state of the seven `*IT` test files (the `TaskRestControllerIT`, `UserRestControllerIT`, `AuthenticationRestControllerIT`, and per-service `GlobalExceptionHandlerIT` classes in `asapp-tasks-service`, `asapp-users-service`, and `asapp-authentication-service`) plus `.claude/rules/testing-core.md`, not this document.

**Notable deltas:**

- **`testing-core.md` §2.1 rule updated — reverses Decision 4 (§2) and the §5 out-of-scope claim of "No rule-doc change."** Decision 4 asserted `testing-core.md` §2.1 could stay unchanged because its "use `// When & Then` when fluently chained" guidance still applied. In practice, once MockMvcTester tests bind to `actual`, the rule's own example list (which named RestTestClient alongside MockMvc as a `// When & Then` case) became inaccurate — RestTestClient also binds to `actual`. As a developer-approved follow-up, `.claude/rules/testing-core.md` §2.1 was narrowed from "Use `// When & Then` when the assertion is fluently chained to the action (e.g., MockMvc, RestTestClient)" to "Use `// When & Then` only when the assertion chains directly onto the action (plain-MockMvc RestDocs)". The durable artifact is `.claude/rules/testing-core.md` §2.1.

For future MockMvcTester or test-AAA edits, treat the seven `*IT` files and `.claude/rules/testing-core.md` §2.1 as the template; this spec is preserved as a record of the original design intent.
