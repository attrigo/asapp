---
paths:
  - "**/infrastructure/**/in/*Api.java"
  - "**/infrastructure/**/*RestController.java"
  - "**/infrastructure/**/in/request/*.java"
  - "**/infrastructure/**/in/response/*.java"
  - "**/asapp-commons-url/**/*.java"
  - "**/src/docs/asciidoc/api-guide.adoc"
---

Conventions for the external HTTP contract — the interfaces and DTOs that shape it, the constants behind its paths, and the adoc that documents it.

## REST Deviations

- Any deviation from REST standards (HTTP codes, verbs, resource naming) must be justified in the endpoint's Javadoc

## Endpoint Constants

- Centralized in `libs/asapp-commons-url` — reference a constant wherever a request path is built or matched, never a literal
- Use a relative `_PATH` on a method mapping, whose interface carries `@RequestMapping(<AGGREGATE>_ROOT_PATH)`; use `_FULL_PATH` (root + relative) in HTTP clients and tests

## API Interface Pattern

- A `<Aggregate>RestController` implements its `<Aggregate>Api` interface, delegating to use cases and mapping results to response DTOs; it declares no routing annotations
- A bodiless response carries a bare `content = { @Content }` — no schema, even on a 404 among bodied error responses
- Use `@ResponseStatus` for fixed HTTP status; use `ResponseEntity` without `@ResponseStatus` when the status is determined programmatically
- A missing resource is a 404 the controller returns from an empty `Optional` (or a `false` delete flag), never a thrown exception
- Annotate the controller class `@Validated` when its interface puts a constraint annotation (e.g. `@Size`) on a method parameter — without it the 400 loses its `fieldErrors`
- Add `@SecurityRequirement(name = "Bearer Authentication")` on every endpoint the filter chain protects; an endpoint whitelisted in `SecurityConfiguration` carries none
- Place `@SecurityRequirement` at the interface level when all endpoints are protected, per-method when the interface mixes protected and public endpoints

## Request / Response DTOs

- Validation annotations must include explicit error messages
- Validation constraint values reference the domain value object's constant, never a literal
- One request and one response record per endpoint — separate records even when fields are identical

## Spring REST Docs

- Each endpoint's method Javadoc description, `@Operation(description = ...)` and `api-guide.adoc` prose carry the same text
- "This endpoint requires authentication." (or equivalents) must not appear in the `@Operation` description or the adoc prose — auth scope is conveyed by `@SecurityRequirement` and the Overview
- Whenever a `*Api.java` or its `api-guide.adoc` changes, update the other to keep description, status codes, and parameters in sync

## Partial Success / Degraded Responses

- A degraded response is `200` with the primary data and a `warnings` array; a failed request is a `ProblemDetail` error
- In the response DTO, a degraded collection is empty, never null
- The `warnings` array is itself the degradation signal — omit it when empty
- A warning `code` names the missing data, never an internal service or topology
