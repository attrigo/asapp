---
paths:
  - "**/infrastructure/**/in/*Api.java"
  - "**/infrastructure/**/*RestController.java"
  - "**/infrastructure/**/*Request.java"
  - "**/infrastructure/**/*Response.java"
  - "**/infrastructure/error/**"
  - "**/asapp-commons-url/**/*.java"
  - "**/src/docs/asciidoc/api-guide.adoc"
---

# API Conventions

- Any deviation from REST standards (HTTP codes, verbs, resource naming) must be justified with a comment in the code

## Endpoint Constants

- Centralized in `libs/asapp-commons-url` — never hardcode paths in controllers
- Constant naming: `<DOMAIN_PLURAL>_<VERB>_<QUALIFIER>_PATH` for relative paths; append `_FULL_PATH` for absolute

## API Interface Pattern

- OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`) go on the interface, not the controller
- Use `@ResponseStatus` for fixed HTTP status; use `ResponseEntity` without `@ResponseStatus` when the status is determined programmatically
- Always pair `@RequestBody` with `@Valid` on the interface method parameter to trigger bean validation
- Add `@SecurityRequirement(name = "Bearer Authentication")` at the interface level for all protected services (not on the auth service)

## Request / Response DTOs

- Validation annotations must include explicit error messages
- Request and response fields use camelCase serialization names
- Do not use `@JsonProperty` to rename fields
- One response record per endpoint — create separate records even if fields are identical

## Spring REST Docs

- `api-guide.adoc` and the OpenAPI annotations are one source of truth: each endpoint's adoc prose MUST be verbatim-identical to its `@Operation(description = ...)`
- "This endpoint requires authentication." (or equivalents) MUST NOT appear in the `@Operation` description or the adoc prose — auth scope is conveyed by `@SecurityRequirement` and the Overview
- Whenever a `*API.java` or its `api-guide.adoc` changes, update the other to keep description, status codes, and parameters in sync

## Error Response Format

- All errors must follow RFC 7807 `ProblemDetail` — do not invent a custom error format
- Add an `error` property only when the code adds meaning beyond the status itself (e.g. `invalid_grant` on 401); omit it when the code would just restate the status (e.g. `server_error` on 500)
- Validation errors extend `ProblemDetail` with a `fieldErrors` property containing a list of `RequestValidationError(field, message)`
- Always set `title` and `detail` via `ProblemDetail.forStatusAndDetail(...)`
- 500 responses in `GlobalExceptionHandler` add `"critical": true` to `ProblemDetail` for monitoring alerts

## Partial Success / Degraded Responses

- Degrade-vs-fail is an application-service policy, decided per dependency (see `ports-adapters.md`)
- Soft (non-critical) dependency → return `200` with the primary data and a `warnings` entry; hard (critical) dependency → fail with a `ProblemDetail` error
- When the degraded data field is a collection, return it empty rather than null so clients avoid null-checks
- The warnings array is itself the degradation signal — omit it when empty, so its presence alone marks a degraded response
- A warning `code` names the missing data, never an internal service or topology
