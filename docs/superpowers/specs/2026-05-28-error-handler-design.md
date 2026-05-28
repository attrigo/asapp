  # Error Handler Improvements — Design

**Date:** 2026-05-28
**Status:** Approved
**Targets:** `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`

---

## 1. Context

Each of the three services owns an identical-shaped `GlobalExceptionHandler`
under `infrastructure/error/` (extends `ResponseEntityExceptionHandler`,
annotated `@RestControllerAdvice`). They emit RFC 7807 `ProblemDetail`
responses and, for validation failures, an `errors` extension property
holding a list of `InvalidRequestParameter(entity, field, message)`.

Four TODOs from `TODO.md` (Version 0.4.0 → Quick Wins → Error Handler)
drive this work:

1. Return validation errors in a deterministic, sorted order.
2. Move all remaining inline error strings (titles, details, codes) into
   constants.
3. Use only fixed messages for `ProblemDetail.detail` (never exception
   messages).
4. Make `InvalidRequestParameter` correctly represent the origin of
   validation errors for non-body parameters (path/query).

The handler triplication is intentionally kept; no extraction to a shared
commons module is in scope.

## 2. Affected Files

Three services, same file layout in each:

- `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/`
  - `GlobalExceptionHandler.java`
  - `InvalidRequestParameter.java`
  - **NEW** `ErrorMessages.java`
  - **NEW** `ParameterLocation.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/`
  - Same four files (the `GlobalExceptionHandler` here also handles
    `ConstraintViolationException`)
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/`
  - Same four files (also handles `ConstraintViolationException`)

Tests:

- `services/*/src/test/java/**/error/GlobalExceptionHandlerTests.java` ×3
- `services/*/src/test/java/**/*RestControllerIT.java` (those that assert
  on error JSON — authentication/user/auth, tasks, users)

Rule file to update so docs stay in sync:

- `.claude/rules/rest.md` — line 19 documents the record as
  `(entity, field, message)`. Replace with `(location, field, message)`.

## 3. Detailed Design

### 3.1 Deterministic sort (Task 1)

A single static comparator on each handler:

```java
private static final Comparator<InvalidRequestParameter> SORT_ORDER =
        Comparator.comparing(InvalidRequestParameter::location)
                  .thenComparing(InvalidRequestParameter::field)
                  .thenComparing(InvalidRequestParameter::message);
```

Both `buildInvalidParameters` overloads end with `.sorted(SORT_ORDER).toList()`
before the result is assigned to the `errors` extension property.

Stability rationale: `ParameterLocation` is an enum, so its natural order
is declaration order (BODY, PATH, QUERY, HEADER) — body-driven errors
always appear first, then path, then query, then header.

### 3.2 Per-service `ErrorMessages` class (Task 2)

One new file per service: `ErrorMessages.java`, in the same package as the
handler. Package-private final class, private no-arg constructor,
package-private `static final` String constants (only the same-package
`GlobalExceptionHandler` consumes them, so neither the class nor its
fields are `public`). Single class (not three split classes) — content is
small and keeping titles/details/codes together makes them easier to scan.

Constants migrating out of `GlobalExceptionHandler` (existing 8–10) plus
the three remaining inline strings:

| Constant                            | Value                            | Used by                                                            |
| ----------------------------------- | -------------------------------- | ------------------------------------------------------------------ |
| `BAD_REQUEST_TITLE`                 | `"Bad Request"`                  | `handleMethodArgumentNotValid`, `handleConstraintViolationException` |
| `VALIDATION_FAILED_DETAIL`          | `"Request validation failed"`    | both validation handlers (replaces `ex.getLocalizedMessage()`)     |
| `INVALID_ARGUMENT_TITLE`            | `"Invalid Argument"`             | `handleIllegalArgumentException`                                   |
| `INVALID_ARGUMENT_DETAIL`           | `"Invalid argument provided"`    | `handleIllegalArgumentException` (replaces `ex.getMessage()`)      |
| `AUTHENTICATION_FAILED_TITLE`       | `"Authentication Failed"`        | 401 handlers                                                       |
| `INVALID_CREDENTIALS_DETAIL`        | `"Invalid credentials"`          | 401 handlers                                                       |
| `INVALID_TOKEN_DETAIL`              | `"Invalid token"`                | `handleUnexpectedJwtTypeException` (authentication & tasks)        |
| `INVALID_GRANT_ERROR`               | `"invalid_grant"`                | 401 handlers (error code)                                          |
| `INTERNAL_SERVER_ERROR_TITLE`       | `"Internal Server Error"`        | 500 handlers                                                       |
| `INTERNAL_ERROR_DETAIL`             | `"An internal error occurred"`   | 500 handlers                                                       |
| `SERVER_ERROR`                      | `"server_error"`                 | 500 handlers (error code)                                          |
| `SERVICE_UNAVAILABLE_TITLE`         | `"Service Unavailable"`          | 503 handlers                                                       |
| `SERVICE_UNAVAILABLE_DETAIL`        | `"Service temporarily unavailable"` | 503 handlers                                                    |
| `TEMPORARILY_UNAVAILABLE_ERROR`     | `"temporarily_unavailable"`      | 503 handlers (error code)                                          |
| `ERROR_PROPERTY`                    | `"error"`                        | all handlers that set the error-code property (already extracted)  |

Property keys (`"errors"`, `"critical"`) are not titles/details/codes and
fall outside Task 2's stated scope; they remain inline as today.

Per-service tables may include extra entries for authentication-only
exceptions (e.g., `JwtIssuanceException`, `TokenStoreException` titles/details
already defined inline today). Each service's `ErrorMessages` only contains
the constants its handler actually uses.

### 3.3 Fixed `ProblemDetail.detail` (Task 3)

Every `ProblemDetail.forStatusAndDetail(...)` call uses a constant from
`ErrorMessages`. The three sites that currently echo the exception message:

| Handler method                          | Current detail source        | New detail source                          |
| --------------------------------------- | ---------------------------- | ------------------------------------------ |
| `handleMethodArgumentNotValid`          | `ex.getLocalizedMessage()`   | `ErrorMessages.VALIDATION_FAILED_DETAIL`   |
| `handleConstraintViolationException`    | `ex.getLocalizedMessage()`   | `ErrorMessages.VALIDATION_FAILED_DETAIL`   |
| `handleIllegalArgumentException`        | `ex.getMessage()`            | `ErrorMessages.INVALID_ARGUMENT_DETAIL`    |

Per-field detail (which constraint failed, which value was invalid) is not
lost — it remains in the `errors[]` array (the `message` of each
`InvalidRequestParameter`). Logging in each handler continues to call
`log.warn("... {}", ex.getMessage())` / `log.error(..., ex)` so the raw
detail is still captured server-side.

### 3.4 `ParameterLocation` enum and updated record (Task 4)

New enum, colocated with the record:

```java
public enum ParameterLocation { BODY, PATH, QUERY, HEADER }
```

Record becomes:

```java
public record InvalidRequestParameter(
        ParameterLocation location,
        String field,
        String message
) {}
```

The previous `entity` field (binding-object DTO name for body, empty string
for constraint violations) is removed.

Population rules:

- **Body validation** — `handleMethodArgumentNotValid` builds each
  parameter with `location = BODY`. `FieldError.getField()` supplies
  `field`; `FieldError.getDefaultMessage()` supplies `message`. The DTO
  binding-object name (`FieldError.getObjectName()`) is no longer surfaced
  in the response.
- **Method-parameter validation** — `handleConstraintViolationException`
  resolves `location` per violation via a new private static helper:

  ```java
  private static ParameterLocation resolveLocation(ConstraintViolation<?> v) {
      Class<?> rootClass = v.getRootBeanClass();
      // Walk property path: first node is the method (kind METHOD),
      // second node is the parameter (kind PARAMETER) — name() gives
      // the parameter name, or fall back to argN index.
      // Locate the Method on rootClass, then the matching Parameter,
      // then check for @PathVariable / @RequestParam / @RequestHeader.
      // Default = QUERY (Spring treats unannotated controller params as
      // query parameters).
  }
  ```

  Implementation notes (carry into the plan):
  - Match the method by name and parameter count from the path's first
    node; if multiple overloads exist, additionally match by parameter
    index from the second node (`argN` index or named lookup).
  - Use the existing `field` extraction logic (strip the method-name
    prefix from the property path) — that behavior already works.
  - The helper is package-private only if a test needs to invoke it
    directly; otherwise keep it `private static`.

### 3.5 Logging policy (unchanged, restated for the plan)

Per `.claude/rules/ports-adapters.md`, the handler logs:

- 4xx → `log.warn("... {}", ex.getMessage())` (no stack trace)
- 5xx → `log.error("...", ex)` (with stack trace)

This behavior must be preserved so that the rule still holds after the
`detail` field is sanitized — the raw exception message remains visible
to operators.

### 3.6 Rule update

`.claude/rules/rest.md` currently states (line 19):

> Validation errors extend `ProblemDetail` with an `errors` property
> containing a list of `InvalidRequestParameter(entity, field, message)`

Replace `entity` with `location` and add a one-line note that `location`
is a `ParameterLocation` enum (`BODY`, `PATH`, `QUERY`, `HEADER`).

## 4. Testing Strategy

### 4.1 Unit tests — `GlobalExceptionHandlerTests` ×3

Existing tests already cover each handler method via direct invocation.
Extend them:

- **Sort order**: build a fixture with at least one same-field two-violation
  case and at least two different `location` values; assert
  `errors` equals a hand-written ordered list.
- **Fixed detail**: assert `problemDetail.getDetail()` equals the
  `ErrorMessages` constant exactly (no string interpolation).
- **InvalidRequestParameter shape**: assert each emitted record has
  `location` equal to the expected `ParameterLocation` enum constant (the
  removal of `entity` is enforced by the record type signature; no
  runtime assertion needed).
- **Origin detection** (tasks-service, users-service): construct fake
  `ConstraintViolation`s whose root bean is a small test controller with
  `@PathVariable` / `@RequestParam` / `@RequestHeader` annotated methods;
  assert each violation resolves to the expected `ParameterLocation`.

### 4.2 Integration tests — `*RestControllerIT`

Update existing assertions:

- Body validation ITs (authentication, tasks, users): change `entity`
  assertions to `location = "BODY"`.
- Add new ITs for path-variable validation failures on tasks-service and
  users-service (a GET on `/v1/tasks/{id}` or `/v1/users/{id}` with an
  invalid UUID format if path-level `@Validated` is wired; otherwise pick
  an existing `@Size`/`@Pattern` annotated path/query controller method).

### 4.3 No new test tier

No `*E2EIT` additions — existing E2E coverage is not error-specific and
the four tasks don't justify a new tier.

## 5. Sequencing

Suggested order, one task per change set, so each can land independently:

1. **Task 4 (origin)** first — biggest blast radius (record shape change),
   touches integration tests across all three services. Land this alone so
   reviewers can focus on the API contract change.
2. **Task 1 (sort)** — small, localized; trivially added to the existing
   `buildInvalidParameters` methods now that `location` exists.
3. **Task 2 (constants)** — purely mechanical extraction; reviewing it on
   top of the prior two reads as a clean refactor.
4. **Task 3 (fixed detail)** — once constants exist, the three call sites
   are one-line swaps each.

A single PR per task per service trio (one PR touching all 3 services
together), since the changes are symmetric.

## 6. Out of Scope

- Extracting a shared base handler to a commons library — duplication
  across services is preserved.
- New handlers for `HandlerMethodValidationException`,
  `MissingServletRequestParameterException`,
  `MethodArgumentTypeMismatchException` — not on the TODO.
- Changes to the `error`, `critical`, or other existing extension
  properties.
- Removing or modifying `buildValidationErrorMessage` (the logging-side
  helper) — still used as-is.

## 7. Acceptance Criteria

A change set satisfying this design must:

- For every validation failure: produce an `errors` array sorted by
  `(location, field, message)`; verified by unit and integration tests.
- Contain zero inline String literals for titles, details, or error codes
  inside any `GlobalExceptionHandler.java` (search confirms).
- Set `ProblemDetail.detail` from a constant in `ErrorMessages` for every
  `@ExceptionHandler` method; no exception-message echo remains.
- Expose `InvalidRequestParameter` as `(ParameterLocation, String, String)`
  with the enum serialized as its uppercase name in JSON
  (`"BODY"`/`"PATH"`/`"QUERY"`/`"HEADER"`).
- Correctly resolve `location` for `@PathVariable` (→ PATH),
  `@RequestParam` (→ QUERY), `@RequestHeader` (→ HEADER), and unannotated
  controller parameters (→ QUERY); verified by tasks-service and
  users-service integration tests.
- Keep `.claude/rules/rest.md` accurate.
- Pass `mvn clean verify` across all three services.
