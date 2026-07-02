# Generic Fallback Exception Handler

**Status**: Implemented

## Context

Each REST service — `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service` —
has a `GlobalExceptionHandler` (`infrastructure/error/`) extending Spring's
`ResponseEntityExceptionHandler`, backed by a per-service `ErrorMessages` constants class
(identical content, duplicated by design — there is no shared error library). Between them the
handlers map validation (400), authentication (401), server (500), and unavailability (503)
failures to RFC 7807 `ProblemDetail` bodies.

**The gap.** None of the three handlers has a catch-all `@ExceptionHandler(Exception.class)`. Any
exception without a dedicated handler escapes the advice and falls through to Spring Boot's
default `/error` handling, producing a raw framework error response instead of the project's
RFC 7807 `ProblemDetail`. This is line 68 of `TODO.md`.

**A pre-existing inconsistency this task resolves.** `.claude/rules/rest.md` states *"5xx
responses in `GlobalExceptionHandler` add `\"critical\": true` to `ProblemDetail` for monitoring
alerts."* In the code, only `CompensatingTransactionException` (auth, 500) actually sets it —
`DataAccessException` (all three services, 500) and `JwtIssuanceException` (auth, 500) do not. The
503 handlers (`TokenStoreException`, `RedisConnectionFailureException`) also do not. The new
fallback is itself a 500 handler, so the flag policy must be settled to design it.

The two Spring Cloud services (`config`, `discovery`) are out of scope — they are not REST
business services and do not carry a `GlobalExceptionHandler`.

## Goal

Add a last-resort `@ExceptionHandler(Exception.class)` to each of the three `GlobalExceptionHandler`s
so any otherwise-unhandled exception maps to a generic 500 `ProblemDetail` (logged at ERROR with
its stack trace) instead of a raw Spring error, and align the existing 500 handlers and the
`rest.md` rule on a single, coherent `critical` policy.

## Decisions

Resolved up front:

1. **What it catches** → `Exception`, never `Throwable`. A catch-all must not swallow `Error`
   (e.g. `OutOfMemoryError`).
2. **`critical` policy** → the flag marks **500** responses (genuine server faults), not all 5xx.
   The fallback sets `critical: true`; the existing 500 handlers are retrofitted to match; the
   503 handlers are left as-is; the `rest.md` wording is tightened from "5xx" to "500". Rationale:
   a 503 is a transient degradation already covered by the resilience patterns (circuit breaker /
   retry) — an expected, self-healing condition, not a page-someone fault.
3. **Where it lives** → per service, following the existing duplicated-handler pattern. Extracting
   a shared error library is a larger refactor, out of scope, and against the established
   convention.
4. **Scope** → the three REST services only.

## Ordering & precedence

`@ExceptionHandler(Exception.class)` is the broadest possible match, so Spring routes to it only
when no more specific handler applies — neither our typed handlers (400/401/500/503) nor
`ResponseEntityExceptionHandler`'s built-in handlers for the standard Spring MVC exceptions. It is
a genuine last resort; existing behavior is unchanged.

The new method is named `handleUnexpectedException`, deliberately **not** `handleException`:
`ResponseEntityExceptionHandler` already declares a `final handleException(...)` over a fixed set
of framework exceptions, and a distinct name avoids any confusion with it.

**Scope boundary.** The fallback covers exceptions raised during controller/service dispatch.
Security-filter failures (401/403) are handled by the `AuthenticationEntryPoint` /
access-denied handler *before* the `DispatcherServlet`, so they never reach the advice and are
unaffected — as intended.

## Change 1 — Add the fallback handler (all three services)

In each `GlobalExceptionHandler`, in the "500 Internal Server Error" section, as the
least-specific handler:

```java
@ExceptionHandler(Exception.class)
protected ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
    problemDetail.setTitle(INTERNAL_SERVER_ERROR_TITLE);
    problemDetail.setProperty(ERROR_PROPERTY, SERVER_ERROR);
    problemDetail.setProperty(CRITICAL_PROPERTY, true);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                         .body(problemDetail);
}
```

Reuses the existing constants (`INTERNAL_ERROR_DETAIL` = "An internal error occurred",
`INTERNAL_SERVER_ERROR_TITLE`, `SERVER_ERROR`). The response never carries the raw exception
message; the stack trace goes to the ERROR log only.

## Change 2 — Introduce a `CRITICAL_PROPERTY` constant (all three services)

The `"critical"` property name is currently an inline literal in the auth handler. Because it is
about to appear in several places (the fallback plus the retrofits below), add
`CRITICAL_PROPERTY = "critical"` to each `ErrorMessages` — under the existing "Property names"
group, alongside `FIELD_ERRORS_PROPERTY` — and switch all usages (including the existing auth
`CompensatingTransactionException` handler) to the constant.

## Change 3 — Retrofit the existing 500 handlers

Add `problemDetail.setProperty(CRITICAL_PROPERTY, true);` to every existing 500 handler that
lacks it, so all 500 responses are consistent with the rule:

| Service | Handler |
|---|---|
| authentication | `handleDataAccessException`, `handleJwtIssuanceException` |
| tasks | `handleDataAccessException` |
| users | `handleDataAccessException` |

`handleCompensatingTransactionException` (auth) already sets it — only its literal is swapped for
the constant (Change 2). The 503 handlers (`handleTokenStoreException`, `handleRedisException`)
are intentionally left without the flag.

## Change 4 — Tighten the rule (`.claude/rules/rest.md`)

Change *"5xx responses in `GlobalExceptionHandler` add `\"critical\": true`…"* to *"500 responses…"*,
so the documented policy matches the code: `critical` marks server faults (500), not transient
503 unavailability, which the resilience layer already owns.

**Permission note:** edits under `.claude/` are denied by the auto-mode classifier. This rule
change will be surfaced for the developer to approve/apply rather than written silently.

## Testing impact

Each test verifies a distinct property; the assertions do not overlap.

**Unit — mapping** (`GlobalExceptionHandlerTests`, one per service). Add a
`HandleUnexpectedException` nested class that calls `handleUnexpectedException(new RuntimeException(...))`
directly and asserts the full body: status 500, title "Internal Server Error", detail "An internal
error occurred", `error` = "server_error", `critical` = true. Proves the exception→`ProblemDetail`
mapping.

Also update the existing 500-handler unit tests retrofitted in Change 3 to assert `critical` = true:
`HandleDataAccessException` (all three services) and `HandleJwtIssuanceException` (auth).

**IT — routing** (new `GlobalExceptionHandlerIT`, one per service, in
`src/test/.../infrastructure/error/`, extending the existing `WebMvcTestContext`). Stubs one
`@MockitoBean` use-case to throw a raw `RuntimeException` (a type with no dedicated handler),
performs a real request through `mockMvcTester`, and asserts only the routing-specific facts:
status 500, content type `application/problem+json`, and — to confirm it is *our* fallback body,
not some other response — `detail` "An internal error occurred" and `critical` true. It does not
re-assert every field (that is the unit test's job). This is the piece that proves an unhandled
exception is actually intercepted by the advice instead of escaping as a raw Spring error — which
a direct method call cannot show.

- The IT hits an endpoint whose stubbed use-case throws, using a request that passes validation so
  execution reaches the use-case (a valid path/body). Auth can use its public login endpoint;
  tasks/users use `@WithMockUser` on a secured read endpoint, as the existing controller ITs do.
- Adding no new beans or configuration, the IT reuses the cached `@WebMvcTest` context from
  `WebMvcTestContext` — no extra Spring context is created.

**Rationale for both.** The sibling typed handlers get by with unit tests only because their
routing (specific type → specific handler) is low-risk. The fallback is keyed to `Exception.class`,
so "does Spring route here, and only as a last resort" is the one live risk and the literal goal
of this task; the IT is the only test that can verify it.

`GlobalExceptionHandlerIT` is an integration test (slow tier); per the project's Maven-permissions
convention the developer is asked before the slow suites run.

## Verification

- `mvn clean install` — compiles.
- `mvn spotless:apply` — run after edits to catch formatting/orphaned imports.
- `mvn clean verify` — exercises the new unit tests and the three `GlobalExceptionHandlerIT`s
  (slow tier; confirm first).

## Out of scope / non-goals

- `asapp-config-service`, `asapp-discovery-service` — not REST business services.
- Extracting a shared error library to de-duplicate the three handlers / `ErrorMessages`.
- Marking 503 responses `critical`.
- Any change to the security-filter (401/403) error path or its response shape.
- Wrapping infrastructure exceptions into new application types to make them declarable.

## Post-implementation notes

This spec and its plan
(`docs/superpowers/plans/2026-07-01-generic-fallback-exception-handler.md`) were written before
implementation. The core change shipped substantially as designed — a last-resort
`@ExceptionHandler(Exception.class)` named `handleUnexpectedException` now maps any
otherwise-unhandled exception to a generic 500 `ProblemDetail` (logged at ERROR, raw message never
exposed) in all three REST services, and every 500 handler plus the `rest.md` rule are aligned on a
single `critical: true` policy through the new `CRITICAL_PROPERTY` constant.

As always, the canonical implementation is the current state of the real artifacts on this
branch — the three `GlobalExceptionHandler` and `ErrorMessages` classes, their
`GlobalExceptionHandlerTests` and the new `GlobalExceptionHandlerIT`s, and `.claude/rules/rest.md`
— not this document.

Notable deltas:

- **Fallback placed in its own `FALLBACK` section after the 503 handlers, not inside the 500
  block (revises "Change 1" and "Ordering & precedence").** The spec placed
  `handleUnexpectedException` "in the '500 Internal Server Error' section, as the least-specific
  handler." As shipped, it sits at the very end of each `GlobalExceptionHandler` under a new banner
  comment `FALLBACK - Any Otherwise-Unhandled Exception (500 Internal Server Error)`, i.e. after
  the 503 handlers, and the matching `HandleUnexpectedException` nested class in each
  `GlobalExceptionHandlerTests` was moved to last. A source-organization choice so the broadest
  catch-all reads as the final last resort; it has zero runtime effect, since Spring routes by
  exception specificity, not source order.

- **The `rest.md` rule shipped without the "503 is transient / owned by the resilience layer"
  parenthetical (revises "Change 4").** Change 4's intent — tightening "5xx" to "500" — landed
  exactly: the line now reads `500 responses in GlobalExceptionHandler add "critical": true to
  ProblemDetail for monitoring alerts`. The rationale clause the plan spelled out was added and
  then dropped in review as inferable, per the project's "rules capture only non-obvious decisions"
  convention.

- **The routing ITs arrange request setup before the throwing stub (refines the "Testing impact"
  IT sketch).** In each new `GlobalExceptionHandlerIT`, the `// Given` block builds the request
  (and `taskId`/`userId`) first and places the `given(...).willThrow(...)` stub last, consistent
  across the three services. A test-arrangement choice only — no behavioral or coverage change.

One item was consciously left as-is: the class-level Javadoc in each `GlobalExceptionHandler`
still frames server errors as "5xx" while the rule now says "500"; repaying that minor doc-vs-rule
drift is out of this task's scope.

For future exception-handling edits, treat the three `GlobalExceptionHandler` / `ErrorMessages`
classes and their tests as the template; this spec is preserved as a record of the original design
intent.
