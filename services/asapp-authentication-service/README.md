# ASAPP Authentication Service

> JWT-based authentication and user credential management for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

The Authentication Service is a core microservice in the ASAPP ecosystem, responsible for managing user credentials and JWT token lifecycle. It provides secure authentication, token refresh, and revocation capabilities for all ASAPP services.

**Key Responsibilities**:
- 🔐 User credential management (username, password, role)
- 🎫 JWT token generation (access + refresh tokens)
- 🔄 Token refresh and revocation
- ⚡ Redis-based token storage and revocation checks
- 👤 User CRUD operations (for authentication purposes)
- 🛡️ Security enforcement for downstream services

## Features

### Authentication Operations

- **Authenticate** - Issue JWT tokens and store in Redis for validation
  - `POST /api/auth/token`
  - Returns access token (5 min expiry) + refresh token (1 hour expiry)

- **Refresh Authentication** - Get new tokens using refresh token
  - `POST /api/auth/refresh`
  - Extends session without re-entering credentials

- **Revoke Authentication** - Invalidate active tokens (removes from Redis and database)
  - `POST /api/auth/revoke`
  - Immediately revokes tokens by removing from Redis and database

### User Management Operations

- **Create User** - Register new user with credentials
  - `POST /api/users`
  - Assigns role (ADMIN or USER)

- **Get User** - Retrieve user by ID
  - `GET /api/users/{id}`

- **Get All Users** - List all registered users
  - `GET /api/users`

- **Update User** - Modify user credentials or role
  - `PUT /api/users/{id}`

- **Delete User** - Remove user and revoke all tokens
  - `DELETE /api/users/{id}`

### Observability

- **Health Check** - Service health and dependencies
  - `GET /actuator/health`

- **Metrics** - Prometheus-formatted application metrics
  - `GET /actuator/prometheus`

- **API Documentation** - Interactive Swagger UI
  - `http://localhost:8080/asapp-authentication-service/swagger-ui.html`

## Architecture

### Hexagonal Architecture

The service follows **Hexagonal Architecture** (Ports & Adapters) with clear separation of concerns:

**Domain Layer** (`domain/`):
- Pure business logic with no framework dependencies
- **Aggregates**: User, JwtAuthentication
- **Value Objects**: Username, Password, Role, Jwt, EncodedToken, Subject, etc.
- **Domain Services**: PasswordService

**Application Layer** (`application/`):
- Use cases and orchestration
- **Input Ports**: AuthenticateUseCase, CreateUserUseCase, etc.
- **Output Ports**: UserRepository, JwtAuthenticationRepository, Authenticator
- **Services**: Annotated with `@ApplicationService`

**Infrastructure Layer** (`infrastructure/`):
- External concerns (REST, database, security)
- REST controllers and DTOs
- Repository adapters (Spring Data JDBC)
- Security components (JWT handling, filters)
- Redis adapter (token store with TTL management)

### Domain-Driven Design

The service implements **DDD patterns**:

**Aggregates**:
- `User` - Two-state pattern (inactive/active)
- `JwtAuthentication` - Manages token pairs with lifecycle

**Value Objects**: 15+ immutable records with validation
- Username (email format), RawPassword, EncodedPassword, Role
- Jwt, JwtPair, EncodedToken, Subject, JwtClaims, Issued, Expiration

**Factories**: State-based creation
- `User.inactiveUser()` / `User.activeUser()`
- `JwtAuthentication.unAuthenticated()` / `authenticated()`

### Security Model

**JWT Token Structure**:
```json
{
  "typ": "at+jwt",
  "sub": "user@asapp.com",
  "role": "USER",
  "token_use": "access",
  "iat": 1234567890,
  "exp": 1234567990
}
```

**Token Types**:
- **Access Token** (`at+jwt`) - 5-minute expiry, used for API access
- **Refresh Token** (`rt+jwt`) - 1-hour expiry, used to obtain new access tokens

**Token Validation Flow**:
1. Signature validation (HMAC-SHA with secret key)
2. Expiration check (iat/exp claims validation)
3. Redis existence check (revocation verification)
4. Claims extraction (username, role, token_use)

**Security Components**:
- `JwtIssuer` - Creates signed tokens with HMAC-SHA
- `JwtDecoder` - Validates signatures and extracts claims
- `JwtVerifier` - Ensures correct token type
- `JwtAuthenticationFilter` - Intercepts and validates requests

### Data Stores

**PostgreSQL** (`authenticationdb`):
- Durable storage for user credentials and authentication records
- Schema managed via Liquibase migrations

**Redis** (`:6379`):
- Fast token existence checks (O(1) lookups)
- TTL-based auto-expiration matching token lifetime
- Key patterns:
  - `jwt:access_token:<token>` - Access token validation
  - `jwt:refresh_token:<token>` - Refresh token validation
- Values: Empty strings (existence only)
- Consistency: Best-effort (Redis and DB operations not atomic)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL (via Docker)
- Redis (via Docker)

### Run Locally (Development Mode)

```bash
# 1. Start PostgreSQL and Redis
docker-compose up -d asapp-authentication-postgres-db asapp-redis

# 2. Run the service
mvn spring-boot:run

# 3. Access Swagger UI
open http://localhost:8080/asapp-authentication-service/swagger-ui.html
```

### Run with Docker

```bash
# 1. Build Docker image
mvn spring-boot:build-image

# 2. Start service with database
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-authentication-service

# 4. Stop and clean
docker-compose down -v
```

### Example API Usage

```bash
# 1. Create a user
curl -X POST http://localhost:8080/asapp-authentication-service/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@asapp.com",
    "password": "SecurePass123!",
    "role": "USER"
  }'

# 2. Authenticate
curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@asapp.com",
    "password": "SecurePass123!"
  }'

# Response:
{
  "access_token": "eyJhbGc...",
  "refresh_token": "eyJhbGc..."
}

# 3. Use access token
curl -X GET http://localhost:8080/asapp-authentication-service/api/users \
  -H "Authorization: Bearer <access_token>"
```

## Configuration

### Application Properties

**Key Configuration** (`application.properties`):

```properties
# Server
server.port=8080
server.servlet.context-path=/asapp-authentication-service

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/authenticationdb
spring.datasource.username=user
spring.datasource.password=secret

# JWT Security
asapp.security.jwt-secret=<base64-encoded-secret>
asapp.security.access-token-expiration-time=300000    # 5 minutes
asapp.security.refresh-token-expiration-time=3600000  # 1 hour

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=secret

# Actuator (management port)
management.server.port=8090
management.endpoints.web.exposure.include=*
```

### Environment-Specific Configuration

- `application.properties` - Default (local development)
- `application-docker.properties` - Docker Compose environment

## Development

### Build and Test

```bash
# Build project
mvn clean install

# Run all tests (unit + integration)
mvn clean verify

# Run unit tests only
mvn test

# Run integration tests only
mvn verify -DskipUnitTests

# Run mutation testing
mvn org.pitest:pitest-maven:mutationCoverage
```

### Code Quality

```bash
# Check code formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply

# Install git hooks (pre-commit, commit-msg)
mvn git-build-hook:install
```

### Database Management

```bash
# Start standalone database
docker-compose up -d asapp-authentication-postgres-db

# Apply Liquibase migrations
mvn liquibase:update

# Generate migration SQL (dry-run)
mvn liquibase:updateSQL

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Generate Documentation

```bash
# Generate Javadoc
mvn clean verify

# View Javadoc
open target/asapp-authentication-service-<version>-javadoc.jar
# Or: target/site/apidocs/index.html

# View Test Coverage
open target/site/jacoco-aggregate/index.html

# View Mutation Testing Report
open target/pit-reports/<timestamp>/index.html
```

## API Endpoints

### Authentication Endpoints (Public)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/token` | Authenticate user | ❌ |
| POST | `/api/auth/refresh` | Refresh tokens | ❌ |
| POST | `/api/auth/revoke` | Revoke tokens | ✅ |

### User Management Endpoints (Protected)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/users` | Create user | ❌ |
| GET | `/api/users` | Get all users | ✅ |
| GET | `/api/users/{id}` | Get user by ID | ✅ |
| PUT | `/api/users/{id}` | Update user | ✅ |
| DELETE | `/api/users/{id}` | Delete user | ✅ |

### Actuator Endpoints (Protected)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health status |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/actuator/metrics` | Available metrics list |
| GET | `/actuator/info` | Application info |

**Actuator Port**: `8090` (separate from application port `8080`)

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.4.3
- **Database**: PostgreSQL
- **Cache/Store**: Redis
- **Migrations**: Liquibase
- **Security**: Spring Security + JJWT (JWT library)
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, AssertJ, TestContainers, PITest
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Observability**: Spring Boot Actuator, Micrometer

## Project Structure

```
src/main/java/com/bcn/asapp/authentication/
├── domain/                           # Pure business logic
│   ├── user/                         # User aggregate
│   └── authentication/               # JwtAuthentication aggregate
├── application/                      # Use cases
│   ├── user/in/                      # User use cases
│   ├── user/out/                     # User repositories (ports)
│   ├── authentication/in/            # Auth use cases
│   └── authentication/out/           # Auth repositories (ports)
└── infrastructure/                   # External concerns
    ├── user/in/                      # User REST controllers
    ├── user/out/                     # User repository adapters
    ├── authentication/in/            # Auth REST controllers
    ├── authentication/out/           # Auth repository adapters
    ├── security/                     # JWT components, filters
    ├── config/                       # Spring configuration
    └── error/                        # Exception handling
```

## Database Schema

**PostgreSQL Tables**:
- `users` - User credentials (id, username, password, role)
- `jwt_authentications` - Active token sessions (id, user_id, access_token, refresh_token)

**Redis Keys**:
- `jwt:access_token:<token>` - Access token existence (TTL: 5 min)
- `jwt:refresh_token:<token>` - Refresh token existence (TTL: 1 hour)

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

## Testing

**Test Types**:
- Unit Tests (`*Tests.java`) - Domain and application logic
- Integration Tests (`*IT.java`) - With TestContainers PostgreSQL and Redis
- Controller Tests (`*ControllerIT.java`) - WebMvcTest slice
- E2E Tests (`*E2EIT.java`) - Full application context

**Coverage**: JaCoCo reports (unit, integration, aggregate)
**Mutation Testing**: PITest for domain layer
**Test Containers**: PostgreSQL and Redis for integration tests

## Monitoring

**Actuator Endpoints**: `http://localhost:8090/asapp-authentication-service/actuator`

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Custom business metrics

## Requirements

- **Java**: 21 or higher
- **Maven**: 3.9.0 or higher
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **PostgreSQL**: 15+ (via Docker)
- **Redis**: 7+ (via Docker)

## Contributing

This service is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:
- Follow Hexagonal Architecture and DDD patterns
- Use Conventional Commits (`feat:`, `fix:`, `test:`, etc.)
- Run `mvn spotless:apply` before committing
- Ensure all tests pass (`mvn verify`)
- Update OpenAPI documentation for API changes

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Architecture Guide](../../docs/claude/architecture.md)
- [Domain-Driven Design Patterns](../../docs/claude/domain-driven-design.md)
- [Testing Strategy](../../docs/claude/testing.md)
- [API Conventions](../../docs/claude/api-conventions.md)

## External Resources

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [JJWT (Java JWT)](https://github.com/jwtk/jjwt)
- [Liquibase](https://docs.liquibase.com/)

## License

ASAPP Authentication Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
