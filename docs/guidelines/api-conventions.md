# API Conventions

## Quick Reference

**Endpoint Pattern**: `/api/<resource>/<action>` or `/api/<resource>/{id}`

**HTTP Methods**:
- GET = Retrieve, POST = Create/Action, PUT = Full Update, PATCH = Partial Update, DELETE = Delete

**DTO Naming**:
- Requests: `<Verb><Domain>Request` (e.g., `AuthenticateRequest`)
- Responses: `<Verb><Domain>Response` (e.g., `AuthenticateResponse`)

**Status Codes**:
- 200 OK, 201 Created, 204 No Content
- 400 Bad Request, 401 Unauthorized, 404 Not Found

**Error Format**: RFC 7807 Problem Details (via Spring's `ProblemDetail`)

**Endpoint Constants**: Centralized in `libs/asapp-commons-url`

## Key Patterns

### Endpoint Constants
```java
// libs/asapp-commons-url/AuthenticationRestAPIURL.java
public static final String AUTH_ROOT_PATH = "/api/auth";
public static final String AUTH_TOKEN_PATH = "/token";
public static final String AUTH_TOKEN_FULL_PATH = AUTH_ROOT_PATH + AUTH_TOKEN_PATH;
```

### Request/Response DTOs
```java
// Request (with validation)
public record AuthenticateRequest(
    @NotBlank(message = "Username must not be empty")
    String username,
    @NotBlank(message = "Password must not be empty")
    String password
) {}

// Response (with JSON snake_case mapping)
public record AuthenticateResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken
) {}
```

### REST API Interface Pattern
```java
@RequestMapping(AUTH_ROOT_PATH)
@Tag(name = "Authentication operations")
public interface AuthenticationRestAPI {

    @PostMapping(AUTH_TOKEN_PATH)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticates a user")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    AuthenticateResponse authenticate(@RequestBody @Valid AuthenticateRequest request);
}
```

### Controller Implementation
```java
@RestController
public class AuthenticationRestController implements AuthenticationRestAPI {

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        // 1. Map request → command
        var command = mapper.toCommand(request);

        // 2. Execute use case
        var result = useCase.execute(command);

        // 3. Map domain → response
        return mapper.toResponse(result);
    }
}
```

### MapStruct Mapper
```java
@Mapper(componentModel = "spring", uses = { ... })
public interface JwtAuthenticationMapper {

    // Request → Command
    AuthenticateCommand toCommand(AuthenticateRequest request);

    // Domain → Response (with nested property access)
    @Mapping(target = "accessToken", source = "jwtPair.accessToken")
    @Mapping(target = "refreshToken", source = "jwtPair.refreshToken")
    AuthenticateResponse toResponse(JwtAuthentication authentication);

    // Domain → Entity
    JwtAuthenticationEntity toEntity(JwtAuthentication authentication);

    // Entity → Domain
    JwtAuthentication toDomain(JwtAuthenticationEntity entity);
}
```

## Details

### Shared Endpoint Constants

**Location**: `libs/asapp-commons-url/`

**Classes**:
- `AuthenticationRestAPIURL` - `/api/auth/*` endpoints
- `UserRestAPIURL` - `/api/users/*` endpoints
- `TaskRestAPIURL` - `/api/tasks/*` endpoints

**Usage**: Import constants instead of hardcoding paths

### Request DTO Characteristics

**Location**: `infrastructure/<aggregate>/in/request/`

- Java records for immutability
- Jakarta validation annotations (`@NotBlank`, `@NotNull`, `@Valid`)
- Clear validation messages
- No business logic

### Response DTO Characteristics

**Location**: `infrastructure/<aggregate>/in/response/`

- Java records for immutability
- `@JsonProperty` for snake_case JSON mapping
- Can have nested structures
- No business logic

### Error Response Format

**Structure** (RFC 7807):
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/token",
  "errors": [
    {
      "objectName": "authenticateRequest",
      "field": "username",
      "message": "Username must not be empty"
    }
  ]
}
```

**Custom Error Parameter**: `InvalidRequestParameter(objectName, field, message)`

### REST API Interface

**Purpose**: Separate interface defines API contract with OpenAPI documentation

**Location**: `infrastructure/<aggregate>/in/<Domain>RestAPI.java`

**Key Annotations**:
- `@Tag` - Groups related endpoints
- `@Operation` - Describes endpoint
- `@RequestBody` - Documents request
- `@ApiResponse` - Documents responses
- `@ResponseStatus` - Default status code

### OpenAPI/Swagger Access

- Authentication Service: `http://localhost:8080/asapp-authentication-service/swagger-ui.html`
- Users Service: `http://localhost:8081/asapp-users-service/swagger-ui.html`
- Tasks Service: `http://localhost:8082/asapp-tasks-service/swagger-ui.html`

### MapStruct Configuration

**Component Model**: `spring` (generates Spring beans)

**Uses Parameter**: Specify custom mappers and object factories

**Mapping Annotations**:
- `@Mapping(target, source)` - Field mapping
- Dot notation for nested properties: `source = "jwtPair.accessToken"`
- `@InheritInverseConfiguration` - Reverse mapping

### HTTP Status Codes

**Success**: 200 OK, 201 Created, 204 No Content
**Client Error**: 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
**Server Error**: 500 Internal Server Error
