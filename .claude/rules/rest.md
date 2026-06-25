---
paths:
  - "**/infrastructure/**/*RestAPI.java"
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

## REST API Interface Pattern

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

- Whenever a `*RestAPI.java` changes, update its `api-guide.adoc` section to keep description, status codes, and parameters in sync
- Strip "This endpoint requires authentication." from adoc descriptions; authentication scope is already stated in the Overview

## Error Response Format

- All errors must follow RFC 7807 `ProblemDetail` — do not invent a custom error format
- All error responses carry an `error` property with a machine-readable error code (e.g., `"invalid_request"`, `"invalid_grant"`, `"server_error"`)
- Validation errors extend `ProblemDetail` with a `fieldErrors` property containing a list of `RequestValidationError(field, message)`
- Always set `title` and `detail` via `ProblemDetail.forStatusAndDetail(...)`
- 5xx responses in `GlobalExceptionHandler` add `"critical": true` to `ProblemDetail` for monitoring alerts

## Partial Success / Degraded Responses

- Degrade-vs-fail is an application-service policy, decided per dependency (see `ports-adapters.md`)
- Soft (non-critical) dependency → return `200` with the primary data and a `warnings` entry; hard (critical) dependency → fail with a `ProblemDetail` error
- When the degraded data field is a collection, return it empty rather than null so clients avoid null-checks
- The warnings array is itself the degradation signal — omit it when empty, so its presence alone marks a degraded response
- A warning `code` names the missing data, never an internal service or topology
