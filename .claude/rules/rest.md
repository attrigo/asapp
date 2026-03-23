---
paths:
  - "**/infrastructure/**/*RestAPI.java"
  - "**/infrastructure/**/*RestController.java"
  - "**/infrastructure/**/*Request.java"
  - "**/infrastructure/**/*Response.java"
  - "**/asapp-commons-url/**/*.java"
---

# API Conventions

- This project strictly follows REST standards (HTTP codes, verbs, resource naming). Any deviation must be justified with a comment in the code

## Endpoint Constants

- Centralized in `libs/asapp-commons-url` — never hardcode paths in controllers

## REST API Interface Pattern

- OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`) go on the **interface**, not the controller
- Use `@ResponseStatus` for fixed HTTP status; use `ResponseEntity` without `@ResponseStatus` when the status is determined programmatically

## Controller Pattern

- The controller layer is responsible for request data validation

## Request / Response DTOs

- Validation annotations must include explicit error messages
- Request and response fields are exposed in `snake_case`, use `@JsonProperty("snake_case")` for multi-word field names

## Error Response Format

- All errors must follow RFC 7807 `ProblemDetail` — do not invent a custom error format
- Validation errors extend `ProblemDetail` with an `errors` property containing a list of `InvalidRequestParameter(entity, field, message)`
