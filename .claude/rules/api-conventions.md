---
paths:
  - "**/infrastructure/**/in/**/*.java"
  - "**/*Mapper.java"
  - "**/asapp-commons-url/**/*.java"
---

# API Conventions

## Naming

- Request DTOs: `<Verb><Domain>Request` (e.g., `AuthenticateRequest`)
- Response DTOs: `<Verb><Domain>Response` (e.g., `AuthenticateResponse`)
- Use case interfaces: `<Verb><Domain>UseCase`
- Commands: `<Verb><Domain>Command`
- Endpoint constants: centralized in `libs/asapp-commons-url` — never hardcode paths in controllers

## HTTP Status Codes

- `200 OK` — retrieve or action returning data
- `201 Created` — resource created
- `204 No Content` — delete or action with no return value
- `400 Bad Request` — validation failure
- `401 Unauthorized` — authentication failure
- `404 Not Found` — resource not found

## REST API Interface Pattern

```java
@RequestMapping(AUTH_ROOT_PATH)
@Tag(name = "Authentication operations")          // OpenAPI on the interface, NOT the controller
public interface AuthenticationRestAPI {

    @PostMapping(AUTH_TOKEN_PATH)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticates a user")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    AuthenticateResponse authenticate(@RequestBody @Valid AuthenticateRequest request);
}
```

**Rules**:
- OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`) go on the **interface**, not the controller
- `@ResponseStatus` on the interface method defines the default HTTP status
- `@Valid` on `@RequestBody` to trigger Jakarta Bean Validation

## Controller Pattern

```java
@RestController
public class AuthenticationRestController implements AuthenticationRestAPI {

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        var command = mapper.toCommand(request);    // 1. Map request → command
        var result  = useCase.execute(command);     // 2. Execute use case
        return mapper.toResponse(result);           // 3. Map domain → response
    }
}
```

**Rules**:
- Controller only maps and delegates — zero business logic
- Always 3 steps: map in → execute → map out

## Request / Response DTOs

**Rules**:
- Java `record` (immutable)
- Requests: Jakarta validation annotations (`@NotBlank`, `@NotNull`) with explicit messages
- Responses: `@JsonProperty("snake_case")` for JSON field name mapping
- No business logic in DTOs

## MapStruct Mapper

**Rules**:
- `componentModel = "spring"` on all mappers
- Use dot notation for nested source properties: `source = "jwtPair.accessToken"`
- `@InheritInverseConfiguration` for reverse mappings (e.g., entity ↔ domain)

## Error Response Format

RFC 7807 `ProblemDetail` — do not invent a custom error format.
Validation errors extend with `InvalidRequestParameter(objectName, field, message)`.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/token",
  "errors": [{ "objectName": "authenticateRequest", "field": "username", "message": "must not be blank" }]
}
```
