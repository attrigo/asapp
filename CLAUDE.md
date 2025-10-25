# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ASAPP is a Spring Boot 3.4.3 microservices application for task management, built with Java 21. It follows Hexagonal Architecture principles and consists of three main services:
- **asapp-authentication-service**: Manages user credentials and JWT token lifecycle
- **asapp-users-service**: Manages user profile information
- **asapp-tasks-service**: Manages tasks and projects

## Build & Development Commands

### Building the Project
```bash
# Clean and install all modules
mvn clean install

# Build Docker images for all services
mvn spring-boot:build-image

# Build specific service
cd services/asapp-authentication-service && mvn clean install
```

### Running Tests
```bash
# Run all tests (unit + integration)
mvn test verify

# Run only unit tests
mvn test

# Run only integration tests
mvn verify -DskipUnitTests

# Run tests for specific service
cd services/asapp-authentication-service && mvn test

# Run mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage
```

### Code Quality & Formatting
```bash
# Check code style (uses Spotless)
mvn spotless:check

# Apply code formatting
mvn spotless:apply

# Install git hooks (automatic on mvn install)
mvn git-build-hook:install
```

### Database Management (Liquibase)
```bash
# Generate migration SQL (from project root)
cd services/asapp-authentication-service
mvn liquibase:updateSQL

# Clear checksums (if needed)
mvn liquibase:clearCheckSums

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Running the Application
```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f asapp-authentication-service

# Stop and remove all services (with volumes)
docker-compose down -v

# Run single service locally (ensure database is available)
cd services/asapp-authentication-service
mvn spring-boot:run
```

### Accessing Services
- Authentication Service Swagger: http://localhost:8080/asapp-authentication-service/swagger-ui.html
- Users Service Swagger: http://localhost:8081/asapp-users-service/swagger-ui.html
- Tasks Service Swagger: http://localhost:8082/asapp-tasks-service/swagger-ui.html
- Grafana Dashboards: http://localhost:3000

## Architecture Overview

### Hexagonal Architecture Layers

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

### Key Package Structure Pattern
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

### Inter-Service Communication

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

### Database Migrations

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

### Security Architecture

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

## Shared Libraries

Located in `libs/`:

1. **asapp-commons-url**: Centralized API endpoint constants
   - `AuthenticationRestAPIURL`, `UserRestAPIURL`, `TaskRestAPIURL`
   - Use these constants instead of hardcoding paths

2. **asapp-rest-clients**: Shared REST client infrastructure
   - `UriHandler`: Interface for building service URIs
   - `DefaultUriHandler`: Implementation using base URL configuration
   - `FallbackRestClientConfiguration`: Provides RestClient.Builder beans

## Testing Strategy

### Test Types and Naming
- **Unit tests**: `*Tests.java` - Fast, isolated domain/application logic tests
- **Integration tests**: `*IT.java` - Use TestContainers for PostgreSQL
- **Controller tests**: `*ControllerIT.java` - WebTestClient for HTTP testing
- **E2E tests**: `*E2EIT.java` - Full application context and workflows

### Test Data Builders
Located in `testutil/` packages:
- `UserDataFaker`: Generate fake user data
- `JwtDataFaker`: Generate fake JWT tokens
- `TestDataFaker`: Domain-specific test data generators
- `TestContainerConfiguration`: PostgreSQL test container setup

### Running Specific Tests
```bash
# Run single test class
mvn test -Dtest=UserTests

# Run integration tests only
mvn verify -DskipUnitTests

# Run with coverage
mvn clean verify jacoco:report
```

## Code Style & Standards

### Formatting
- Uses Spotless Maven plugin with Eclipse formatter (`asapp_formatter.xml`)
- License header required (`header-license` file)
- Import order: `java|javax, org, com, , com.bcn`
- Line endings: UNIX (LF)
- Run `mvn spotless:apply` before committing

### Commit Messages
- Follow Conventional Commits standard
- Git hooks automatically validate commit messages
- Format: `<type>: <description>`
- Types: feat, fix, chore, docs, test, refactor

### Git Hooks
Installed automatically on `mvn install`:
- **pre-commit**: Checks code style with Spotless
- **commit-msg**: Validates commit message format

## Common Development Patterns

### Adding a New Use Case
1. Define domain models in `domain/` (aggregate, value objects)
2. Create use case interface in `application/<domain>/in/`
3. Define command DTOs in `application/<domain>/in/command/`
4. Implement service in `application/<domain>/in/service/` with `@ApplicationService`
5. Create REST controller in `infrastructure/<domain>/in/`
6. Add request/response DTOs and MapStruct mappers
7. Write unit tests for domain logic
8. Write integration tests for controller and repository

### Adding a New Database Table
1. Create changeset file: `YYYYMMDD_N_description.xml` in `liquibase/db/changelog/v<version>/changesets/`
2. Include pre-conditions to check existence
3. Define rollback instructions
4. Update version changelog to include new changeset
5. Test migration: `mvn liquibase:updateSQL`
6. Create JPA entity in `infrastructure/<domain>/out/entity/`
7. Create repository adapter implementing output port

### Adding a New Service
1. Create module under `services/` following existing structure
2. Add module to `services/pom.xml`
3. Create database and configure in `docker-compose.yaml`
4. Copy and adapt structure from existing service
5. Update shared libraries if new API endpoints are needed
6. Configure Liquibase changelog
7. Add Swagger/OpenAPI configuration
8. Configure JWT authentication if needed

### Working with JWT Tokens
- Extract current user from SecurityContext:
  ```java
  JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
      .getContext().getAuthentication();
  String username = auth.getName();
  String role = auth.getRole();
  ```
- For service-to-service calls, JWT is automatically propagated via `JwtInterceptor`
- No need to manually add Authorization header

## Release Process

Documented in README.md:
1. Checkout main branch
2. Remove SNAPSHOT from pom.xml: `mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false`
3. Add Liquibase database tags to all changelog files
4. Build: `mvn clean install`
5. Commit: `git commit -m "chore: release version ${RELEASE_VERSION}"`
6. Create tag: `git tag ${RELEASE_VERSION}`
7. Prepare next version: `mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false`
8. Commit and push: `git push --atomic origin main ${RELEASE_VERSION}`

## Technology Stack Reference

- **Java**: 21
- **Spring Boot**: 3.4.3
- **Database**: PostgreSQL (via Liquibase migrations)
- **Security**: Spring Security with JWT (JJWT library)
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, AssertJ, TestContainers, JSON-Unit
- **Code Quality**: Spotless, JaCoCo, PITest
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Observability**: Prometheus, Grafana

## Key Files

- `pom.xml`: Parent POM with build configuration
- `asapp_formatter.xml`: Eclipse code formatter configuration
- `header-license`: License header template
- `docker-compose.yaml`: Infrastructure services configuration
- `git/hooks/`: Git hook scripts for validation
- `.github/workflows/ci.yml`: CI pipeline configuration