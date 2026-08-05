---
paths:
  - "**/infrastructure/error/*.java"
  - "**/security/web/*EntryPoint.java"
---

Conventions for producing an HTTP error response — the exception handlers, their message constants, and the security entry points. `ports-adapters.md` owns the exception hierarchy and where a framework exception is translated; this rule owns what reaches the client.

## Exception Handler

- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and ends with an `@ExceptionHandler(Exception.class)` → 500 — without the superclass a framework-raised error (e.g. a type-conversion 400) is reported as a `critical` 500

## Response Shape

- An error response carries either no body or an RFC 7807 `ProblemDetail` — never a custom error format
- Omit the body when the status and headers already carry the full meaning — write them directly, since `sendError` re-dispatches to `/error`, which renders a body

## ProblemDetail Members

- Add an `error` property only when the code adds meaning beyond the status itself (e.g. `invalid_grant` on 401); omit it when the code would just restate the status (e.g. `server_error` on 500)
- Validation errors add a `fieldErrors` property — a list of `RequestValidationError(field, message)` sorted by field, then message
- A `detail` set in a handler is a fixed constant, never the exception's message
- Set `title` only when it adds meaning beyond the status (e.g. `Authentication Failed` on 401); omit it otherwise — unset, it falls back to the status reason phrase
- A declared `@ExceptionHandler` returning 500 adds `"critical": true` to `ProblemDetail` for monitoring alerts
