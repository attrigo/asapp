# ASAPP Authentication Service

> JWT-based authentication and user credential management for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

The Authentication Service is a core microservice in the ASAPP ecosystem, responsible for managing user credentials and JWT lifecycle. It provides secure
authentication, token refresh, and revocation capabilities for all ASAPP services.

**Key Responsibilities**:

- 🔐 User credential management (username, password, role)
- 🎫 JWT generation (access + refresh tokens)
- 🔄 Token refresh and revocation
- ⚡ Redis-based token storage and revocation checks
- 👤 User CRUD operations (for authentication purposes)
- 🛡️ Security enforcement for downstream services

---

## Features

### Authentication Operations

- **Authenticate**: Issue JWT and store in Redis; returns access token (5 min expiry) + refresh token (1 hour expiry)
- **Refresh Authentication**: Get new tokens using refresh token; extends session without re-entering credentials
- **Revoke Authentication**: Invalidate active tokens; removes from Redis and database

### User Management Operations

- **Create User** - Register new user with credentials
- **Get User** - Retrieve user by ID
- **Get All Users** - List all registered users
- **Update User** - Modify user credentials or role
- **Delete User** - Remove user and revoke all tokens

---

## Requirements

- **Java**: 25+
- **Gradle**: 9.6.1 (via wrapper)
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **PostgreSQL**: 15+ (via Docker)
- **Redis**: 7+ (via Docker)

---

## Quick Start

### Run Locally (Development Mode)

```bash
# 1. Start the config service (in a separate terminal, from project root)
./gradlew :services:asapp-config-service:bootRun

# 2. Start the discovery service (in a separate terminal, from project root)
./gradlew :services:asapp-discovery-service:bootRun

# 3. Start PostgreSQL and Redis
docker-compose up -d asapp-authentication-postgres-db asapp-redis

# 4. Run the service
./gradlew :services:asapp-authentication-service:bootRun

# 5. Access Swagger UI
open http://localhost:8080/asapp-authentication-service/swagger-ui.html
```

### Run with Docker

```bash
# 1. Build Docker image
./gradlew :services:asapp-authentication-service:bootBuildImage

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
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}

# 3. Use access token
curl -X GET http://localhost:8080/asapp-authentication-service/api/users \
  -H "Authorization: Bearer <accessToken>"
```

---

## Configuration & Profiles

The service is **secure-by-default**: with no environment profile, Swagger UI and Boot-UI are fully off and the Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

- **Local** — `./gradlew :services:asapp-authentication-service:bootRun` activates `dev` (wired in the build script).
- **Docker stack** — `docker,dev`.
- **Locked-down deploy** — `SPRING_PROFILES_ACTIVE=docker,prod`.

### Property resolution

auth-service is a **config-server client**: at startup (via `spring.config.import`) it merges properties from two locations:

- **Local** — its own `src/main/resources/`.
- **Shared** — `central-config/`, served by the Config Service.

Local beats Shared, a profile overlay (`application-<profile>`) beats its base, and an overlay applies only when its profile is active. Highest precedence first:

```
Local   application-docker.properties                          (docker overlay)
Local   application.properties                                 (base)
Shared  central-config/asapp-authentication-service.properties (service-specific)
Shared  central-config/application-docker.properties           (docker overlay)
Shared  central-config/application-dev.properties              (dev overlay)
Shared  central-config/application.properties                  (base)
```

---

## Architecture

### Domain Model

**Aggregates**: User, JwtAuthentication  
**Value Objects**: Username, RawPassword, EncodedPassword, Role, Jwt, JwtPair, EncodedToken, Subject

### Security Model

- **API endpoints**: JWT Bearer token; issuance and refresh endpoints are public. Tokens are verified for signature, expiry, and active status in Redis
- **Management endpoints**: HTTP Basic authentication

### Data Stores

**PostgreSQL**: (`authenticationdb`) user credentials and active authentication records

- `users` — id, username, password, role
- `jwt_authentications` — id, user_id, access_token, refresh_token

**Redis**: token storage with TTL-based auto-expiration for revocation checks

- `jwt:access_token:<token>` — TTL: 5 min
- `jwt:refresh_token:<token>` — TTL: 1 hour

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

### Project Structure

```
src/main/java/com/attrigo/asapp/authentication/
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

---

## Technology Stack

- **Spring Boot**: 4.0.5
- **Spring Framework**: 7.x
- **Configuration**: Spring Cloud Config 5.x
- **Service Discovery**: Spring Cloud Netflix Eureka Client 5.x
- **Security**: Spring Security + Nimbus JOSE+JWT
- **Migrations**: Liquibase
- **Mapping**: MapStruct
- **Testing**: JUnit 5, AssertJ, TestContainers, PITest
- **Documentation**: SpringDoc OpenAPI
- **Observability**: Spring Boot Actuator, Micrometer

---

## Development

### Build

```bash
# Build project
./gradlew build

# Compile and package without running any checks
./gradlew assemble
```

### Test

```bash
# Run all tests (unit, integration, E2E)
./gradlew check

# Run mutation testing
./gradlew :services:asapp-authentication-service:pitest
```

### Run Locally

Run from the repository root, never from the module directory.

```bash
# Run the service
./gradlew :services:asapp-authentication-service:bootRun

# Override the active profiles
./gradlew :services:asapp-authentication-service:bootRun --args='--spring.profiles.active=dev'
```

### Code Quality

```bash
# Install git hooks (pre-commit, commit-msg)
./gradlew installGitHooks

# Apply formatting
./gradlew spotlessApply
```

### Database Management

```bash
# Start standalone database
docker-compose up -d asapp-authentication-postgres-db

# Preview the pending migrations as SQL (prints to stdout)
./gradlew :services:asapp-authentication-service:liquibaseUpdateSql

# ...or send that SQL to a file instead
./gradlew :services:asapp-authentication-service:liquibaseUpdateSql -PliquibaseOutputFile=migration.sql

# Apply Liquibase migrations
./gradlew :services:asapp-authentication-service:liquibaseUpdate

# Rollback last changeset
./gradlew :services:asapp-authentication-service:liquibaseRollbackCount -PliquibaseCount=1
```

### Generate Documentation

```bash
# Generate all reports (except `pitest`)
./gradlew :services:asapp-authentication-service:fullBuild
```

Generate specific report: `./gradlew :services:asapp-authentication-service:<command>`

| Command                       | Generates            |
|-------------------------------|----------------------|
| `asciidoctor`                 | REST API docs        |
| `jacocoTestReport`            | Unit coverage        |
| `jacocoIntegrationTestReport` | Integration coverage |
| `jacocoMergedReport`          | Merged coverage      |
| `javadoc`                     | Javadoc              |
| `pitest`                      | Mutation report      |

> `asciidoctor` runs the integration tier first, so Docker must be up.

---

## Reference

### Property Sources

Listed highest-precedence first; `application-<profile>` rows apply only when that profile is active.

| File                                      | Source      | Scope                  |
|-------------------------------------------|-------------|------------------------|
| `application-docker.properties`           | Local       | docker profile         |
| `application.properties`                  | Local       | all profiles           |
| `asapp-authentication-service.properties` | Centralized | service-specific       |
| `application-docker.properties`           | Centralized | shared, docker profile |
| `application-dev.properties`              | Centralized | shared, dev profile    |
| `application.properties`                  | Centralized | shared                 |

### Docker Environment Variables

| Variable                                       | Description                                   |
|------------------------------------------------|-----------------------------------------------|
| `JAVA_OPTS`                                    | JVM runtime options                           |
| `SPRING_PROFILES_ACTIVE`                       | Active Spring profiles                        |
| `CONFIG_SERVER_URI`                            | Config server base URI                        |
| `CONFIG_SERVER_USERNAME`                       | Config server HTTP Basic username             |
| `CONFIG_SERVER_PASSWORD`                       | Config server HTTP Basic password             |
| `SERVER_PORT`                                  | HTTP server port                              |
| `SERVICE_USERNAME`                             | HTTP Basic username for actuator endpoints    |
| `SERVICE_PASSWORD`                             | HTTP Basic password for actuator endpoints    |
| `DB_HOST`                                      | PostgreSQL hostname                           |
| `DB_PORT`                                      | PostgreSQL port                               |
| `DB_NAME`                                      | PostgreSQL database name                      |
| `DB_USERNAME`                                  | PostgreSQL username                           |
| `DB_PASSWORD`                                  | PostgreSQL password                           |
| `REDIS_HOST`                                   | Redis hostname                                |
| `REDIS_PORT`                                   | Redis port                                    |
| `REDIS_PASSWORD`                               | Redis password                                |
| `MANAGEMENT_PORT`                              | Actuator management port                      |
| `DISCOVERY_HOST`                               | Eureka server hostname                        |
| `DISCOVERY_USERNAME`                           | Eureka server username                        |
| `DISCOVERY_PASSWORD`                           | Eureka server password                        |
| `ASAPP_SECURITY_JWT_SECRET`                    | HMAC-SHA secret for signing JWT tokens        |
| `ASAPP_SECURITY_ACCESS_TOKEN_EXPIRATION_TIME`  | Access token expiration in milliseconds       |
| `ASAPP_SECURITY_REFRESH_TOKEN_EXPIRATION_TIME` | Refresh token expiration in milliseconds      |
| `ASAPP_SECURITY_JWT_CLEANUP_ENABLED`           | Enable expired JWT cleanup background job     |
| `ASAPP_SECURITY_JWT_CLEANUP_CRON_EXPRESSION`   | Cron expression for expired token cleanup job |

### API Endpoints

**Authentication Endpoints**

| Method | Endpoint            | Description       | Auth Required |
|--------|---------------------|-------------------|---------------|
| POST   | `/api/auth/token`   | Authenticate user | ❌             |
| POST   | `/api/auth/refresh` | Refresh tokens    | ❌             |
| POST   | `/api/auth/revoke`  | Revoke tokens     | ✅             |

**User Management Endpoints**

| Method | Endpoint          | Description    | Auth Required |
|--------|-------------------|----------------|---------------|
| POST   | `/api/users`      | Create user    | ❌             |
| GET    | `/api/users`      | Get all users  | ✅             |
| GET    | `/api/users/{id}` | Get user by ID | ✅             |
| PUT    | `/api/users/{id}` | Update user    | ✅             |
| DELETE | `/api/users/{id}` | Delete user    | ✅             |

**Management Endpoints**

Actuator endpoints are on port `8090` at `/asapp-authentication-service/actuator`; use `GET /actuator` to list them.

- `/actuator/health` — public
- All other actuator endpoints — HTTP Basic authentication (`user` / `secret`)

Health probes are on the server port (`8080`) at `/asapp-authentication-service` and are public.

- `/asapp-authentication-service/readyz`
- `/asapp-authentication-service/livez`

### Documentation

| Artifact             | Location                                                                                |
|----------------------|-----------------------------------------------------------------------------------------|
| REST API docs        | `build/docs/asciidoc/api-guide.html`                                                    |
| Swagger UI           | `http://localhost:8080/asapp-authentication-service/swagger-ui.html` (dev profile only) |
| BootUI console       | `http://localhost:8080/asapp-authentication-service/bootui` (dev profile only)          |
| Unit coverage        | `build/reports/jacoco/test/html/index.html`                                             |
| Integration coverage | `build/reports/jacoco/jacocoIntegrationTestReport/html/index.html`                      |
| Merged coverage      | `build/reports/jacoco/jacocoMergedReport/html/index.html`                               |
| Mutation report      | `build/reports/pitest/index.html`                                                       |
| Javadoc              | `build/docs/javadoc/index.html`                                                         |

### Monitoring

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:

- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Custom business metrics

### Dependencies

**Internal Dependencies**:

- `asapp-commons-url` - Endpoint constants

---

## Contributing

This service is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Follow Hexagonal Architecture and DDD patterns
- Update OpenAPI documentation for API changes
- Add tests for new code
- Ensure all tests pass (`./gradlew check`)
- Run `./gradlew spotlessApply` before committing
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Discovery Service](../asapp-discovery-service/README.md)

---

## External Resources

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)
- [Liquibase](https://docs.liquibase.com/)
- [BootUI](https://github.com/jdubois/boot-ui)

---

## License

ASAPP Authentication Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
