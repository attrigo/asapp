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
- Request and response fields use snake_case serialization names
- Use `@JsonProperty("field_name")` for multi-word field names — single-word fields need no annotation
- One response record per endpoint — create separate records even if fields are identical

## Spring REST Docs

- Whenever a `*RestAPI.java` changes, update its `api-guide.adoc` section to keep description, status codes, and parameters in sync
- Strip "This endpoint requires authentication." from adoc descriptions; authentication scope is already stated in the Overview

## Error Response Format

- All errors must follow RFC 7807 `ProblemDetail` — do not invent a custom error format
- Validation errors extend `ProblemDetail` with an `errors` property containing a list of `InvalidRequestParameter(entity, field, message)`
- Always set `title` and `detail` via `ProblemDetail.forStatusAndDetail(...)`
- Add `error` property for machine-readable error codes (e.g., `"invalid_grant"`, `"server_error"`)
- 5xx responses in `GlobalExceptionHandler` add `"critical": true` to `ProblemDetail` for monitoring alerts
