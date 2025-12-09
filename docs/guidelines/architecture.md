# Architecture & Domain Design

## Quick Reference

**Architecture**: Hexagonal (Ports & Adapters) + Domain-Driven Design

**Three Layers**:
- **Domain**: Pure business logic (aggregates, value objects, domain services)
- **Application**: Use cases and orchestration (input/output ports, `@ApplicationService`)
- **Infrastructure**: External concerns (REST, database, security, config)

**Three Services** (Bounded Contexts):
- `asapp-authentication-service` (port 8080) - Credentials, JWT lifecycle & Redis-based revocation
- `asapp-users-service` (port 8082) - User profiles
- `asapp-tasks-service` (port 8081) - Task management

**DDD Building Blocks**:
- **Aggregates** (4): User (auth), JwtAuthentication, User (users), Task
- **Value Objects** (30+): Username, Jwt, Role, Title, Email, etc.
- **Repositories**: Collection-like interfaces (output ports)
- **Domain Services**: PasswordService

**Communication**: REST with JWT propagation

**Shared Libraries**: `asapp-commons-url` (endpoints), `asapp-rest-clients` (REST client)

## Key Patterns

### Package Structure (Hexagonal)
```
com.bcn.asapp.<service>/
├── domain/<aggregate>/           # Aggregates, value objects, domain services
├── application/<aggregate>/
│   ├── in/                       # Input ports (use cases)
│   │   ├── command/              # Commands (DTOs)
│   │   └── service/              # @ApplicationService implementations
│   └── out/                      # Output ports (repositories)
└── infrastructure/
    ├── <aggregate>/in/           # REST controllers & DTOs
    ├── <aggregate>/out/          # Repository adapters & entities
    ├── security/                 # JWT components
    └── config/                   # Spring configuration
```

### Dependency Flow
```
REST Controller → Use Case → Application Service → Domain Model → Repository → Adapter → Database
```

### Two-State Aggregate (DDD)
```java
// State 1: Inactive (before persistence)
public static User inactiveUser(Username username, EncodedPassword password, Role role)

// State 2: Active (from database)
public static User activeUser(UserId id, Username username, Role role)

// Equality based on state
equals() → id == null ? compare(username) : compare(id)
```

### Value Object Pattern (DDD)
```java
public record Username(String username) {
    public Username {
        validateNotBlank(username);
        validateEmailPattern(username);
    }

    public static Username of(String username) {
        return new Username(username);
    }

    public String value() { return this.username; }
}
```

### Repository Pattern (DDD + Hexagonal)
```java
// Application layer (output port)
public interface UserRepository {
    Optional<User> findById(UserId userId);
    User save(User user);
    Boolean deleteById(UserId userId);
}

// Infrastructure layer (adapter)
@Component
public class UserRepositoryAdapter implements UserRepository {
    // Maps domain ↔ database entities
}
```

### Application Service Marker
```java
@ApplicationService  // Custom annotation (not Spring's @Service)
public class AuthenticateService implements AuthenticateUseCase {
    // Orchestrates domain logic, delegates to output ports
}
```

## Details

### Hexagonal Architecture Layers

**1. Domain Layer** (`domain/`):
- **Aggregates**: Entities with identity, protect invariants (User, Task, JwtAuthentication)
- **Value Objects**: Immutable records with validation (Username, Jwt, Title, etc.)
- **Domain Services**: Stateless operations (PasswordService)
- **No Framework Dependencies**: Pure Java, no Spring annotations

**2. Application Layer** (`application/`):
- **Input Ports**: Use case interfaces (`<Verb><Domain>UseCase`)
- **Output Ports**: Repository/service interfaces (`<Domain>Repository`)
- **Commands**: DTOs as records (`<Verb><Domain>Command`)
- **Services**: Implementations with `@ApplicationService` annotation

**3. Infrastructure Layer** (`infrastructure/`):
- **REST** (`in/`): Controllers, Request/Response DTOs, API interfaces
- **Persistence** (`out/`): Repository adapters, JPA entities, JDBC repositories
- **Security** (`security/`): JwtDecoder, JwtVerifier, JwtIssuer, JwtAuthenticationFilter, Redis token store adapter, TTL management
- **Config** (`config/`): Component scanning, security filter chain, JDBC config

### DDD Aggregates and Invariants

**User Aggregate** (Authentication Service):
- **Purpose**: Authentication credentials
- **Invariants**: Email username, password 8-64 chars, role ADMIN/USER
- **States**: Inactive (with password) vs Active (without password)
- **Factories**: `inactiveUser()`, `activeUser()`

**JwtAuthentication Aggregate**:
- **Purpose**: Token lifecycle management
- **Invariants**: Both tokens required, type matches token_use claim, issued < expiration, tokens must exist in Redis for validation (source of truth for active sessions)
- **States**: Unauthenticated (no ID) vs Authenticated (with ID)
- **Factories**: `unAuthenticated()`, `authenticated()`

**User Aggregate** (Users Service):
- **Purpose**: User profile information
- **Invariants**: All fields required (firstName, lastName, email, phoneNumber)
- **States**: Create (no ID) vs Reconstitute (with ID)
- **Factories**: `create()`, `reconstitute()`

**Task Aggregate**:
- **Purpose**: Task management
- **Invariants**: Must have userId, title required
- **States**: Create (no ID) vs Reconstitute (with ID)
- **Factories**: `create()`, `reconstitute()`

### Entities vs Value Objects

**Entities** (have identity):
- Equality based on ID
- Mutable state via business methods
- Examples: User, Task, JwtAuthentication

**Value Objects** (compared by value):
- Implemented as Java records (immutable)
- Validation in compact constructor
- Factory method: `of()`
- Access via `value()` method
- Categories:
  - **Identity**: UserId, TaskId, JwtAuthenticationId
  - **User**: Username, Password, Role, FirstName, Email, PhoneNumber
  - **JWT**: Jwt, JwtPair, EncodedToken, Subject, JwtClaims, Issued, Expiration
  - **Task**: Title, Description, StartDate, EndDate

### Factories

**Aggregate Factories** (state-based):
```java
User.inactiveUser() / User.activeUser()
JwtAuthentication.unAuthenticated() / authenticated()
Task.create() / Task.reconstitute()
```

**Value Object Factories**:
```java
Username.of(String)              // Standard
Issued.now() / Issued.of(Instant) // Multiple factories
StartDate.ofNullable(Instant)    // Optional support
```

### Repositories (Output Ports)

**Pattern**: Defined in application layer, implemented in infrastructure

**Location**: `application/<aggregate>/out/<Aggregate>Repository.java`

**Key Methods**:
- `findById(ValueObjectId)` - Query by value object, not primitives
- `save(Aggregate)` - Handles insert and update
- `deleteById(ValueObjectId)` - Returns boolean or count

**Principles**:
- Work with aggregate roots only (never internal entities)
- Query parameters use value objects for type safety
- Return domain objects (never expose database entities)

### Ubiquitous Language

**Authentication Context**: User, Username, Password, Role, Authenticate, Grant, Revoke, Jwt, Token
**Users Context**: User, FirstName, Email, Profile, Create, Update, Delete
**Tasks Context**: Task, Title, Description, Owner, StartDate, EndDate

Language remains consistent across domain → application → infrastructure layers.

### Bounded Contexts (Services)

**Three Independent Microservices**:
- **Authentication**: Manages credentials, issues JWT tokens
- **Users**: Manages profiles, queries tasks via TasksGateway
- **Tasks**: Manages tasks, references userId

**Communication**: REST with JWT bearer token
**Shared Concept**: UserId (local copy in each service)
**Independence**: Separate databases, deployments, API boundaries

### Validation Strategy

**Constructor-level** (primary):
```java
public Username { validateNotBlank(); validatePattern(); }
```

**State-based**:
```java
if (inactive) { validatePassword(); } else { validateId(); }
```

**Exception Strategy**: Throw `IllegalArgumentException` with clear messages, fail-fast

### Inter-Service Communication

**Authentication Flow**:
1. Client → POST `/api/auth/token` (username + password)
2. Receives access token (5 min) + refresh token (1 hour)
3. Client includes `Authorization: Bearer <token>` in requests

**JWT Validation**:
- `JwtAuthenticationFilter` intercepts requests
- `JwtDecoder` validates signature, expiration, and structure
- Verifies token type is "access"
- **Redis check**: `jwtStore.accessTokenExists()` - fails if token revoked
- Extracts claims (username, role, token_use)
- Sets `SecurityContext`

**Service-to-Service**: `JwtInterceptor` auto-propagates JWT via `asapp-rest-clients`

### Database Migrations (Liquibase)

**Structure**:
```
liquibase/db/changelog/
├── db.changelog-master.xml
└── v<version>/
    ├── v<version>-changelog.xml
    └── changesets/YYYYMMDD_N_description.xml
```

**Key Rules**:
- Changeset naming: `YYYYMMDD_N` (N = sequence for that day)
- Always use pre-conditions
- Include rollback instructions
- Use `uuid_generate_v4()` for PKs
- Tag database at version milestones

### Security Architecture

**JWT Structure**:
```json
Header: { "typ": "at+jwt" }
Claims: { "sub": "user", "role": "USER", "token_use": "access", "iat": ..., "exp": ... }
```

**Components**:
- `JwtIssuer` - Creates signed tokens with configured expiration
- `JwtDecoder` - Validates signature, extracts claims
- `JwtVerifier` - Ensures correct token type (access vs refresh)
- `JwtAuthenticationFilter` - Intercepts requests, validates tokens

**Whitelisted Endpoints** (no auth):
- `/api/auth/token`, `/api/auth/refresh`, health/actuator, Swagger UI

### Redis Token Store

**Purpose**: Fast token validation and revocation checks

**Key Pattern**:
- Access tokens: `jwt:access_token:<token_value>`
- Refresh tokens: `jwt:refresh_token:<token_value>`
- Values: Empty strings (existence check only)

**TTL Management**:
- Auto-calculated from token expiration timestamp
- Access: 5 minutes, Refresh: 1 hour
- Minimum TTL: 1 second
- Auto-cleanup on expiration

**Consistency Model**: Best-effort (Redis and PostgreSQL operations not atomic)

### Observability

**Access URLs**:
- Swagger: `http://localhost:808X/<service>/swagger-ui.html`
- Actuator: `http://localhost:809X/<service>/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/secret)

**Monitoring Stack**:
- Spring Boot Actuator exposes metrics on management ports (8090, 8091, 8092)
- Prometheus scrapes metrics every 15s with JWT authentication
- Grafana visualizes with pre-configured JVM Micrometer dashboard