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
- **Maven**: 3.9.14+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **PostgreSQL**: 15+ (via Docker)
- **Redis**: 7+ (via Docker)

---

## Quick Start

### Run Locally (Development Mode)

```bash
# 1. Start the config service (in a separate terminal, from project root)
cd services/asapp-config-service && mvn spring-boot:run

# 2. Start the discovery service (in a separate terminal, from project root)
cd services/asapp-discovery-service && mvn spring-boot:run

# 3. Start PostgreSQL and Redis
docker-compose up -d asapp-authentication-postgres-db asapp-redis

# 4. Run the service
mvn spring-boot:run

# 5. Access Swagger UI
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

---

## Configuration & Profiles

The service is **secure-by-default**: with no environment profile, Swagger UI is off and the Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

- **Local** — `mvn spring-boot:run` activates `dev` (wired in the POM).
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
mvn clean install

# Build skipping tests
mvn clean install -DskipTests
```

### Test

```bash
# Run all tests
mvn clean verify

# Run mutation testing
mvn org.pitest:pitest-maven:mutationCoverage
```

### Code Quality

```bash
# Install git hooks (pre-commit, commit-msg)
mvn git-build-hook:install

# Apply formatting
mvn spotless:apply
```

### Database Management

```bash
# Start standalone database
docker-compose up -d asapp-authentication-postgres-db

# Generate migration SQL (dry-run)
mvn liquibase:updateSQL

# Apply Liquibase migrations
mvn liquibase:update

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Generate Documentation

```bash
# Generate reports
mvn clean verify -Pfull

# Generate Spring REST API docs (no tests needed)
mvn asciidoctor:process-asciidoc@generate-docs
```

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

| Variable                                       | Description                                   | Default                                                            |
|------------------------------------------------|-----------------------------------------------|--------------------------------------------------------------------|
| `JAVA_OPTS`                                    | JVM runtime options                           | (see docker-compose.yaml)                                          |
| `SPRING_PROFILES_ACTIVE`                       | Active Spring profiles                        | `docker,dev`                                                       |
| `SERVER_PORT`                                  | HTTP server port                              | `8080`                                                             |
| `MANAGEMENT_PORT`                              | Actuator management port                      | `8090`                                                             |
| `DB_HOST`                                      | PostgreSQL hostname                           | `asapp-authentication-postgres-db`                                 |
| `DB_PORT`                                      | PostgreSQL port                               | `5432`                                                             |
| `DB_NAME`                                      | PostgreSQL database name                      | `authenticationdb`                                                 |
| `DB_USERNAME`                                  | PostgreSQL username                           | `user`                                                             |
| `DB_PASSWORD`                                  | PostgreSQL password                           | `secret`                                                           |
| `REDIS_HOST`                                   | Redis hostname                                | `asapp-redis`                                                      |
| `REDIS_PORT`                                   | Redis port                                    | `6379`                                                             |
| `REDIS_PASSWORD`                               | Redis password                                | `secret`                                                           |
| `CONFIG_SERVER_URI`                            | Config server base URI                        | `http://asapp-config-service:8888/asapp-config-service`            |
| `CONFIG_SERVER_USERNAME`                       | Config server HTTP Basic username             | `user`                                                             |
| `CONFIG_SERVER_PASSWORD`                       | Config server HTTP Basic password             | `secret`                                                           |
| `DISCOVERY_HOST`                               | Eureka server hostname                        | `asapp-discovery-service:8761/asapp-discovery-service`             |
| `DISCOVERY_USERNAME`                           | Eureka server username                        | `user`                                                             |
| `DISCOVERY_PASSWORD`                           | Eureka server password                        | `secret`                                                           |
| `SERVICE_USERNAME`                             | HTTP Basic username for actuator endpoints    | `user`                                                             |
| `SERVICE_PASSWORD`                             | HTTP Basic password for actuator endpoints    | `secret`                                                           |
| `ASAPP_SECURITY_JWT_SECRET`                    | HMAC-SHA secret for signing JWT tokens        | `qPxa4PP692Q4fx6voNBX25WoQrzjCoLWLW3VnABjZaOImy0cQaTad5DqBZk3qPxi` |
| `ASAPP_SECURITY_ACCESS_TOKEN_EXPIRATION_TIME`  | Access token expiration in milliseconds       | `300000`                                                           |
| `ASAPP_SECURITY_REFRESH_TOKEN_EXPIRATION_TIME` | Refresh token expiration in milliseconds      | `3600000`                                                          |
| `ASAPP_SECURITY_JWT_CLEANUP_ENABLED`           | Enable expired JWT cleanup background job     | `true`                                                             |
| `ASAPP_SECURITY_JWT_CLEANUP_CRON_EXPRESSION`   | Cron expression for expired token cleanup job | `0 0 2 * * ?`                                                      |

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

| Artifact        | Location                                                             |
|-----------------|----------------------------------------------------------------------|
| REST API docs   | `target/generated-docs/api-guide.html`                               |
| Swagger UI      | `http://localhost:8080/asapp-authentication-service/swagger-ui.html` (dev profile only) |
| Test coverage   | `target/site/jacoco-aggregate/index.html`                            |
| Mutation report | `target/pit-reports/<timestamp>/index.html`                          |
| Javadoc         | `target/site/apidocs/index.html`                                     |

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
- Ensure all tests pass (`mvn verify`)
- Run `mvn spotless:apply` before committing
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

---

## License

ASAPP Authentication Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
