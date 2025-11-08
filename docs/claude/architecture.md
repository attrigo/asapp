# Architecture Overview

## Hexagonal Architecture Layers

Each service follows a strict three-layer structure:

1. **Domain Layer** (`domain/`): Pure business logic with no framework dependencies
   - Aggregates (e.g., `User`, `JwtAuthentication`)
   - Value Objects (e.g., `UserId`, `Username`, `Jwt`, `EncodedToken`)
   - State-based factory methods (e.g., `User.inactiveUser()`, `User.activeUser()`)
   - Domain rules and invariants

2. **Application Layer** (`application/`): Use cases and orchestration
   - Input ports (`in/`): Use case interfaces (e.g., `AuthenticateUseCase`)
   - Output ports (`out/`): Repository and external service interfaces
   - Command objects (`in/command/`): Input DTOs
   - Service implementations (`in/service/`): Annotated with `@ApplicationService`

3. **Infrastructure Layer** (`infrastructure/`): External concerns
   - REST controllers (`in/`): Handle HTTP requests/responses
   - Repository adapters (`out/`): JDBC/JPA implementations
   - Security components (`security/`): JWT handling, authentication filters
   - Configuration (`config/`): Spring beans and configurations

## Key Package Structure Pattern
```
com.bcn.asapp.<service>/
├── domain/<aggregate>/           # Pure domain models
├── application/<aggregate>/
│   ├── in/                       # Input ports (use cases)
│   │   ├── command/              # Input DTOs
│   │   └── service/              # Use case implementations
│   └── out/                      # Output ports (repositories)
└── infrastructure/
    ├── <aggregate>/in/           # REST controllers & DTOs
    ├── <aggregate>/out/          # Repository adapters & entities
    ├── security/                 # JWT components
    ├── config/                   # Spring configuration
    └── error/                    # Exception handling
```

## Inter-Service Communication

Services communicate via REST with JWT propagation:

1. **Authentication Flow**:
   - Client authenticates with authentication-service (POST /api/auth/token)
   - Receives access token (5 min expiry) and refresh token (1 hour expiry)
   - Client includes token in Authorization header: `Bearer <token>`

2. **JWT Validation**:
   - `JwtAuthenticationFilter` intercepts requests and validates tokens
   - Extracts claims (username, role, token_use) and sets SecurityContext
   - Token signature verified using HMAC-SHA with configured secret

3. **Service-to-Service Calls**:
   - `JwtInterceptor` automatically propagates JWT to downstream services
   - Configured via `RestClientConfiguration` in each service
   - Uses shared `asapp-rest-clients` library

## Database Migrations

Liquibase changesets follow this structure:
```
src/main/resources/liquibase/db/changelog/
├── db.changelog-master.xml          # Master changelog (includes versions)
└── v<version>/
    ├── v<version>-changelog.xml     # Version wrapper
    └── changesets/
        └── YYYYMMDD_N_description.xml
```

**Changeset naming**: `YYYYMMDD_N` where N is sequence number for that day

**Important patterns**:
- Always use pre-conditions to check table/column existence
- Include rollback instructions when possible
- Use `uuid_generate_v4()` for UUID primary keys
- Tag database at version milestones for rollback points

## Security Architecture

**JWT Token Structure**:
```json
Header: { "typ": "at+jwt" } // or "rt+jwt" for refresh tokens
Claims: {
  "sub": "username",
  "role": "ADMIN|USER",
  "token_use": "access|refresh",
  "iat": <timestamp>,
  "exp": <timestamp>
}
```

**Key Security Components**:
- `JwtIssuer`: Creates signed JWT tokens with configured expiration
- `JwtDecoder`: Validates JWT signature and extracts claims
- `JwtVerifier`: Ensures token type is correct (access vs refresh)
- `JwtAuthenticationFilter`: Intercepts requests and validates tokens
- `JwtInterceptor`: Propagates JWT to downstream service calls

**Configuration** (application.properties):
```properties
asapp.security.jwt-secret=<base64-encoded-secret>
asapp.security.access-token-expiration-time=300000    # 5 min
asapp.security.refresh-token-expiration-time=3600000  # 1 hour
```

**Whitelisted Endpoints** (no auth required):
- POST /api/auth/token (login)
- POST /api/auth/refresh
- Health/actuator endpoints
- Swagger UI