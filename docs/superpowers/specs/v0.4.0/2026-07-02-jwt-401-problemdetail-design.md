# JWT 401 ProblemDetail (Entry Point)

**Status**: Implemented

## Context

The three REST services — `asapp-authentication-service`, `asapp-tasks-service`,
`asapp-users-service` — protect their business endpoints with a `JwtAuthenticationFilter` +
`JwtAuthenticationEntryPoint` pair (`infrastructure/security/web/`, identical across services by
the project's duplicated-per-service convention). On every protected endpoint the OpenAPI contract
advertises `401 → ProblemDetail` (`@ApiResponse(responseCode = "401", … @Schema(implementation =
ProblemDetail.class))`), and `.claude/rules/rest.md` states *"All errors must follow RFC 7807
`ProblemDetail`."*

**The bug (TODO.md line 69).** For a bad JWT the filter deliberately *swallows*
`InvalidJwtException` / `UnexpectedJwtTypeException` / `AuthenticationNotFoundException` (thrown only
by `JwtVerifier`, called only from the filter) and continues the chain **anonymously**. Spring
authorization then denies the request and invokes `JwtAuthenticationEntryPoint.commence`, which
calls `response.sendError(401, …)` → an **empty-body 401**. So the response violates both the
per-endpoint OpenAPI contract and the RFC 7807 rule.

**The dead handlers.** In the `tasks` and `users` `GlobalExceptionHandler`s, the three
`@ExceptionHandler` methods for those JWT exceptions can never fire on the HTTP path:
`@RestControllerAdvice` only sees exceptions raised during `DispatcherServlet` dispatch, never
exceptions thrown inside a Security filter. The exceptions are also swallowed by the filter before
they could propagate anywhere. Only the unit tests exercise these handlers — by calling them
directly — giving false coverage for code that is unreachable in production.

**Auth is different in one respect only.** The `authentication` service throws its 401 exception
types on the *use-case* path (login / refresh / logout, on whitelisted endpoints that reach the
controller), so its `GlobalExceptionHandler` 401 handlers are genuinely reachable and correct — no
dead handlers there. But auth's protected `/api/users` GET sits behind the *same*
filter + entry point, so auth returns the *same* empty-body 401 on a bad/missing JWT. The
"auth is unaffected" note in the TODO is true for the dead handlers, **not** for the entry point.

**Security framing (researched).** The information-disclosure risk in authentication errors is
about *content specificity* (revealing the failure reason, or whether an account exists), not the
*presence of a body*. OWASP (Authentication Cheat Sheet, WSTG account-enumeration, Top 10:2025
A07) requires the same generic response regardless of reason; RFC 6750 standardises returning
error info on a bad-token 401 while deliberately collapsing "expired, revoked, malformed, invalid"
into one `invalid_token` code; RFC 9457's Security Considerations only warn against leaking
implementation internals. A **generic, fixed** ProblemDetail therefore leaks nothing the `401`
status doesn't already broadcast — and here the reason is *erased by construction*: the filter has
already degraded to anonymous, so `commence` only ever sees a generic
`InsufficientAuthenticationException`.

## Goal

Make a bad or missing JWT on a protected endpoint return an RFC 7807 `ProblemDetail` (generic 401)
instead of an empty body, honouring the existing OpenAPI contract, in **all three** services; and
delete the now-provably-dead JWT 401 handlers (and their unit tests) in **tasks and users**.

## Decisions

Resolved up front:

1. **Where the 401 is rendered → the `AuthenticationEntryPoint`, not the `GlobalExceptionHandler`.**
   Filter-chain failures never reach the advice, and the entry point is the single place every
   401 on the JWT path (missing token, bad token, any auth failure) actually surfaces. It becomes
   the sole 401 renderer.
2. **Body → generic and fixed.** `status 401`, `title "Authentication Failed"`,
   `detail "Invalid credentials"`, `error "invalid_grant"`. No reason, no subject/username, no
   token bytes, no stack trace, no internal names. The raw exception message is **never** placed
   in the body (it would leak the token — `JwtVerifier` builds `"Access token is not valid:
   <token>"`).
3. **Scope → entry-point fix in all three services; dead-handler removal in tasks/users only.**
   Auth's `GlobalExceptionHandler` 401 handlers are reachable and stay.
4. **Placement of the 401 constants → the security package, beside the entry point** (its sole
   remaining consumer on the JWT path). Avoids exposing the package-private `error/ErrorMessages`
   cross-package and adds no indirection. See Change 2 for the auth nuance.
5. **Serialization → the Spring-managed `ObjectMapper` bean** (which carries Spring's RFC 7807
   `ProblemDetail` serialization support). Never `new ObjectMapper()`, or the problem+json shape is
   lost. Response is written directly (status + `application/problem+json` + body); `sendError` is
   dropped so no container error-dispatch blanks the body.
6. **Logging → `WARN`, not `ERROR`.** A 401 is a client-side condition; per the project's
   4xx→warn logging convention, an ERROR-per-anonymous-request is noise (and a mild log-flood
   vector).
7. **Filter unchanged.** Its swallow-and-continue is correct (ports-adapters: *never* wrap a Spring
   `AuthenticationException` inside a Security filter). `JwtVerifier` and the three exception
   classes are unchanged (still thrown/caught in the filter).

**Alternatives considered and rejected.**

- *Keep the empty body, remove the handlers, and retract the docs.* Empty-body 401 is a legitimate,
  standards-compliant choice on its own, but every endpoint's OpenAPI already promises a
  ProblemDetail — so this path forces a contract downgrade (`@ApiResponse(401)` + adoc on every
  endpoint) and leaves an asymmetric error model (400/500 = ProblemDetail, 401 = nothing). Chosen
  approach honours the published contract instead.
- *Keep the handlers alive via `HandlerExceptionResolver` in the filter.* Routes filter exceptions
  into the advice, but (a) still needs the entry point for the missing-token case, creating two
  parallel 401 renderers, and (b) buys distinct per-reason messages we deliberately do not want.
  More surface, no benefit.

## Change 1 — Entry point emits a generic ProblemDetail (auth, tasks, users)

Rewrite `JwtAuthenticationEntryPoint.commence` in each service:

```java
@Override
public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException {
    log.warn("Unauthorized request to {} {}: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
    problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);
    problemDetail.setProperty(ERROR_PROPERTY, INVALID_GRANT_ERROR);

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getWriter(), problemDetail);
}
```

- `ObjectMapper` is injected via constructor (the Spring-managed bean).
- The class Javadoc summary ("Returns an HTTP 401 response with an empty body…") is updated to
  describe the RFC 7807 body.

## Change 2 — Place the 401 constants in the security package

The entry point needs `AUTHENTICATION_FAILED_TITLE` ("Authentication Failed"),
`INVALID_CREDENTIALS_DETAIL` ("Invalid credentials"), `INVALID_GRANT_ERROR` ("invalid_grant"), and
the `ERROR_PROPERTY` key ("error"). Define them as package-private constants in the security
package (e.g. on the entry point itself, or a small `AuthenticationErrorMessages` holder beside it),
in each of the three services.

- **tasks / users:** these strings then leave `error/ErrorMessages` entirely (see Change 3).
- **authentication:** `error/ErrorMessages` *keeps* its copies (the reachable GEH 401 handlers
  still use them), so auth carries the same three strings in two packages. This is an accepted,
  documented duplication: the two render paths are distinct (GEH for use-case-path 401s, entry
  point for filter-path 401s), the values are semantically identical, and the project already
  tolerates per-service duplication with no shared error library. `ERROR_PROPERTY` remains in auth's
  `ErrorMessages` for the GEH and is duplicated (a 5-char key) in the security package.

## Change 3 — Remove the dead handlers (tasks, users only)

In the `tasks` and `users` `GlobalExceptionHandler`:

- Delete the three `@ExceptionHandler` methods `handleAuthenticationNotFoundException`,
  `handleUnexpectedJwtTypeException`, `handleInvalidJwtException`, and the
  `401 UNAUTHORIZED - Authentication Failures` banner section.
- Remove the now-unused imports (`AuthenticationNotFoundException`, `InvalidJwtException`,
  `UnexpectedJwtTypeException`).
- Update the class Javadoc: drop the "Authentication errors (401)" bullet from the strategy list
  (these services' advice no longer produces any 401).

In the `tasks` and `users` `ErrorMessages`:

- Remove `INVALID_TOKEN_DETAIL` (used only by the deleted `handleUnexpectedJwtTypeException` — the
  entry point uses "Invalid credentials", not "Invalid token").
- The strings the entry point now owns (`AUTHENTICATION_FAILED_TITLE`, `INVALID_CREDENTIALS_DETAIL`,
  `INVALID_GRANT_ERROR`) move out to the security package (Change 2); delete them here.
- Keep `ERROR_PROPERTY` (still used by the remaining 400/500/503 handlers).

The three exception classes and the filter's catch blocks are untouched.

## Change 4 — Add the non-obvious rendering rule (`.claude/rules/rest.md`)

Add a one-liner under the error-format guidance so a future contributor does not re-introduce dead
JWT handlers in the advice:

> Filter-chain 401s are rendered by the `AuthenticationEntryPoint`, not the
> `GlobalExceptionHandler` — `@RestControllerAdvice` never sees exceptions thrown in a Security
> filter.

**Permission note:** edits under `.claude/` are denied by the auto-mode classifier; the developer
has approved this change and will apply/allow it rather than it being written silently.

## Testing impact

**Flip empty-body 401 assertions → generic ProblemDetail (~48).** Every test that currently asserts
`.isUnauthorized()` + an empty body on the JWT path is updated to assert the ProblemDetail
(`application/problem+json`, `detail` "Invalid credentials", `error` "invalid_grant"), renaming
`…AndEmptyBody_…` → `…AndProblemDetail_…`, with a shared private assertion helper per file:

| File | Count | Note |
|---|---|---|
| `SecurityConfigurationIT.ApiAuthentication` (tasks / users / auth) | 11 × 3 = 33 | JWT API path |
| `TaskE2EIT` | 6 | resource path |
| `UserE2EIT` (users) | 5 | resource path |
| `UserE2EIT` (auth) | 4 | resource path (`/api/users`) |

Unchanged: actuator HTTP-Basic 401s (`SecurityConfigurationIT.ActuatorAuthentication`, 4 × 3 = 12 —
a different, default entry point); `AuthenticationE2EIT` (17 — application-path 401s that already
return a ProblemDetail via the reachable auth GEH); and the `config` / `discovery`
`SecurityConfigurationIT`s (HTTP Basic, non-JWT, out of scope).

**New `JwtAuthenticationEntryPointTests` (one per service).** Unit test using
`MockHttpServletResponse`: invoke `commence(...)` with a stub `AuthenticationException` and assert
status 401, `Content-Type: application/problem+json`, and the generic body (`title`, `detail`,
`error`). This is the real coverage for the now-live renderer.

**Remove dead-handler unit tests (tasks, users).** Delete the `HandleAuthenticationNotFoundException`,
`HandleUnexpectedJwtTypeException`, and `HandleInvalidJwtException` nested classes from
`GlobalExceptionHandlerTests`, and trim the "Translates authentication failures to 401 …" line from
its coverage Javadoc. Auth's `GlobalExceptionHandlerTests` is unchanged.

**Documentation tests (tasks, users, auth `UserApiDocumentationIT`).** `DocumentsUnauthorized` uses
`@WithAnonymousUser` and hits the real entry point in the WebMvc slice, so the generated
`error-unauthorized/http-response.adoc` snippet regenerates to the new ProblemDetail body
automatically. Strengthen each test to also assert the body (content-type + `detail`/`error`) so it
verifies what it publishes. The adoc directive stays minimal —
`operation::error-unauthorized[snippets='http-response']` (no `response-fields`).

## Documentation

- **`api-guide.adoc` (×3):** no manual change. The 401 section renders the auto-regenerated
  `http-response` snippet; the prose ("Returned when the request is missing a valid Bearer token")
  and the `=== Errors` "All error responses follow RFC 7807" intro remain accurate.
- **READMEs, CLAUDE.md:** no change — none document the error contract (the only 401 mention,
  `tools/jmeter/README.md`, is unrelated to the error body).
- **OpenAPI annotations (`@ApiResponse`, `@Operation`):** no change — already correct; this task
  makes them truthful.

## Verification

- `mvn clean install` — compiles.
- `mvn spotless:apply` — run after edits (handler/import removal can orphan imports; the pre-commit
  hook won't flag them).
- `mvn clean verify` — exercises the flipped ITs/E2EITs, the new entry-point unit tests, and the
  regenerated doc snippets (slow tier; confirm with the developer before running per the
  Maven-permissions convention).

## Out of scope / non-goals

- **`WWW-Authenticate: Bearer` header.** RFC 6750 / RFC 9110 say a 401 *SHOULD* carry it; neither
  the old nor the new code sets it. Deferred (also involves RFC 6750's "no error code when the
  request carries no credentials" nuance) — to be captured as a new TODO backlog item.
- The filter's swallow-and-continue behaviour and `JwtVerifier`.
- Auth's `GlobalExceptionHandler` (handlers reachable) and its `error-invalid-credentials` doc
  snippet (application-path, already ProblemDetail).
- Actuator HTTP-Basic 401s and the `config` / `discovery` services.
- TODO.md line 73 (add routing coverage to `GlobalExceptionHandlerIT`) and line 76 (reword GEH
  Javadoc "Thrown by" → "Catches") — separate tasks; this change makes line 73's "JWT 401s
  unreachable" note accurate.
- Extracting a shared error/security library to de-duplicate the three entry points.

## 5. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-07-02-jwt-401-problemdetail.md`) were written
before implementation. The core change shipped substantially as designed — all three REST services
now render a fixed generic RFC 7807 `ProblemDetail` (401, title "Authentication Failed", detail
"Invalid credentials", `error=invalid_grant`) for a bad JWT directly from
`JwtAuthenticationEntryPoint.commence` (writing `application/problem+json` via the Spring
`ObjectMapper` instead of `sendError`), and the provably-dead JWT-401 `@ExceptionHandler` methods
were removed from the tasks and users `GlobalExceptionHandler`/`ErrorMessages`.

The canonical implementation is the current state of the real artifacts on this branch, not this
document — namely `JwtAuthenticationEntryPoint` and `EmptyBodyBasicAuthenticationEntryPoint` (and
their `*Tests`) under `infrastructure/security/web/`, `SecurityConfiguration` under
`infrastructure/config/`, `SecurityConfigurationIT`, and the `*ApiDocumentationIT` doc tests — all
×3 across the auth, tasks, and users services.

**Notable deltas:**

- **`WWW-Authenticate: Bearer` header brought in-scope (reverses the spec's "Out of scope /
  non-goals").** The spec deferred this header to a backlog item; manual review implemented it
  instead for RFC 6750/9110 compliance. Artifacts: `JwtAuthenticationEntryPoint` — new
  `BEARER_CHALLENGE` constant + `response.setHeader(HttpHeaders.WWW_AUTHENTICATE, …)`, asserted in
  `JwtAuthenticationEntryPointTests` (all three services).
- **New `EmptyBodyBasicAuthenticationEntryPoint` for the actuator chain (unanticipated blast radius
  of Change 1 / Decision 5).** Dropping `sendError` on the JWT path surfaced a regression on the
  *adjacent* actuator HTTP-Basic chain: its default `BasicAuthenticationEntryPoint` still calls
  `sendError`, which re-dispatches to `/error`, which `rootFilterChain` (`/**`) matches and
  `JwtAuthenticationEntryPoint` then renders — so actuator 401s wrongly returned the JWT
  ProblemDetail body. Fixed with a dedicated empty-body entry point that writes
  `WWW-Authenticate: Basic` + a bodyless 401 and never calls `sendError`. Artifacts: new
  `infrastructure/security/web/EmptyBodyBasicAuthenticationEntryPoint.java` +
  `EmptyBodyBasicAuthenticationEntryPointTests.java` (all three services), and
  `SecurityConfiguration.actuatorFilterChain` rewired to it; `SecurityConfigurationIT`'s actuator
  case now exercises it.
- **Spec Change 4 (the `.claude/rules/rest.md` filter-chain-401 note) did not ship — net zero.** The
  note was added then reverted; `.claude/rules/rest.md` currently has no such note. Consequence to
  record: there is no written guard against re-introducing dead filter-chain advice handlers —
  either re-land the note or accept the gap explicitly.
- **Doc tests + AsciiDoc directive aligned to the sibling `response-fields` pattern (reverses the
  spec's "Documentation tests" and "Documentation" decisions).** The spec called for a minimal
  `http-response`-only directive plus strengthened jsonPath/content-type assertions in the doc test;
  instead the doc tests now document the body via `relaxedResponseFields(title, status, detail,
  error)` and drop the jsonPath/content-type checks (redundant with the entry-point unit test +
  `SecurityConfigurationIT`), and the directive switched to
  `snippets='http-response,response-fields'`, matching every other documented error. Artifacts:
  `TaskApiDocumentationIT` / `UserApiDocumentationIT` (`Errors.DocumentsUnauthorized`) and
  `src/docs/asciidoc/api-guide.adoc` across the services.
- **Actuator entry-point wiring tightened; entry-point bean lifecycles are intentionally
  asymmetric.** Post-fix follow-ups dropped the redundant actuator `exceptionHandling` entry-point
  registration (kept only `httpBasic`), inlined the single-use entry point as `new
  EmptyBodyBasicAuthenticationEntryPoint()` in `actuatorFilterChain`, and named the realm via a
  `BASIC_CHALLENGE` constant. Note for maintainers: `JwtAuthenticationEntryPoint` is a
  constructor-injected `@Component` while `EmptyBodyBasicAuthenticationEntryPoint` is a plain
  `new`-instantiated, actuator-chain-local class — a deliberate divergence from a single "all entry
  points are beans" convention. Artifacts: `SecurityConfiguration` (all three services).

For future authentication / entry-point edits, treat the `infrastructure/security/web/` entry
points and their tests (and `SecurityConfiguration`) as the template; this spec is preserved as a
record of the original design intent.
