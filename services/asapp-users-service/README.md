# ASAPP Users Service

> User profile management and task aggregation for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

The Users Service manages user profile information within the ASAPP ecosystem. It maintains personal details (name, email, phone) and integrates with the Tasks
service to provide aggregated user data.

**Key Responsibilities**:

- 👤 User profile management (firstName, lastName, email, phoneNumber)
- 📋 Task aggregation via TasksGateway
- 🔍 User queries and searches
- 🔗 Inter-service communication with Tasks service
- 🛡️ JWT-based authentication and authorization

---

## Features

### User Profile Operations

- **Create User**: Register new user profile
- **Get User by ID**: Retrieve user profile with tasks; includes tasks from Tasks service
- **Get All Users**: List all user profiles without tasks
- **Update User**: Modify firstName, lastName, email, and phoneNumber
- **Delete User**: Remove user profile

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

# 3. Start PostgreSQL database
docker-compose up -d asapp-users-postgres-db

# 4. Run the service
mvn spring-boot:run

# 5. Access Swagger UI
open http://localhost:8082/asapp-users-service/swagger-ui.html
```

### Run with Docker

```bash
# 1. Build Docker image
mvn spring-boot:build-image

# 2. Start service with database
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-users-service

# 4. Stop and clean
docker-compose down -v
```

### Example API Usage

```bash
# 1. Get JWT from Authentication service
TOKEN=$(curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user@asapp.com","password":"SecurePass123!"}' \
  | jq -r '.access_token')

# 2. Create user profile
curl -X POST http://localhost:8082/asapp-users-service/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@asapp.com",
    "phoneNumber": "+1-555-0123"
  }'

# 3. Get user with tasks
curl -X GET http://localhost:8082/asapp-users-service/api/users/{id} \
  -H "Authorization: Bearer $TOKEN"

# Response includes tasks:
{
  "user_id": "...",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@asapp.com",
  "phone_number": "+1-555-0123",
  "tasks": ["task-id-1", "task-id-2"]
}
```

---

## Configuration & Profiles

The service is **secure-by-default**: with no environment profile, Swagger UI and Boot-UI are fully off and the Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

- **Local** — `mvn spring-boot:run` activates `dev` (wired in the POM).
- **Docker stack** — `docker,dev`.
- **Locked-down deploy** — `SPRING_PROFILES_ACTIVE=docker,prod`.

### Property resolution

users-service is a **config-server client**: at startup (via `spring.config.import`) it merges properties from two locations —

- **Local** — its own `src/main/resources/`.
- **Shared** — `central-config/`, served by the Config Service.

Local beats Shared, a profile overlay (`application-<profile>`) beats its base, and an overlay applies only when its profile is active. Highest precedence first:

```
Local   application-docker.properties                 (docker overlay)
Local   application.properties                        (base)
Shared  central-config/asapp-users-service.properties (service-specific)
Shared  central-config/application-docker.properties  (docker overlay)
Shared  central-config/application-dev.properties     (dev overlay)
Shared  central-config/application.properties         (base)
```

---

## Architecture

### Domain Model

**Aggregate**: User  
**Value Objects**: UserId, FirstName, LastName, Email, PhoneNumber

### Security Model

- **API endpoints**: JWT Bearer token required. Tokens are verified for signature, expiry, and active status in Redis
- **Management endpoints**: HTTP Basic authentication

### Data Stores

**PostgreSQL**: (`usersdb`) users records

- `users` — id, first_name, last_name, email, phone_number

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

### Resilience

Outbound gateway calls pass through a Resilience4j **circuit breaker**:

- Repeated 5xx or I/O failures open the breaker, which then fast-fails.
- While open (or on any single downstream failure) the gateway degrades to an empty result, so the primary request still succeeds.
- Client (4xx) errors do not count as failures and never trip the breaker.
- Breaker state and metrics are exported to Prometheus and surfaced as a non-failing `/actuator/health` component.

Tuning lives in `application.properties` under `resilience4j.circuitbreaker.instances.<name>.*`:

| Property                                              | Purpose                                             |
|-------------------------------------------------------|-----------------------------------------------------|
| `sliding-window-size`                                 | How many recent calls the breaker looks at          |
| `minimum-number-of-calls`                             | Minimum calls before the breaker evaluates failures |
| `failure-rate-threshold`                              | Failure rate (%) that opens the breaker             |
| `wait-duration-in-open-state`                         | How long it stays open before retrying              |
| `permitted-number-of-calls-in-half-open-state`        | Trial calls allowed while recovering                |
| `automatic-transition-from-open-to-half-open-enabled` | Start recovering automatically after the wait       |
| `register-health-indicator`                           | Show breaker state in the health endpoint           |
| `allow-health-indicator-to-fail`                      | Whether an open breaker marks health DOWN           |
| `ignore-exceptions`                                   | Exceptions that don't count as failures             |

### Project Structure

```
src/main/java/com/bcn/asapp/users/
├── domain/                           # Pure business logic
│   └── user/                         # User aggregate (profile)
├── application/                      # Use cases
│   ├── user/in/                      # User use cases
│   ├── user/out/                     # User repository (port)
│   └── tasks/out/                    # Tasks gateway (port)
└── infrastructure/                   # External concerns
    ├── user/in/                      # User REST controllers
    ├── user/out/                     # User repository adapter
    ├── tasks/out/                    # Tasks client adapter
    ├── security/                     # JWT validation components
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
- **REST Clients**: Spring HTTP Interfaces (@HttpExchange)
- **Resilience**: Resilience4j (circuit breaker)

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
docker-compose up -d asapp-users-postgres-db

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

| File                             | Source      | Scope                  |
|----------------------------------|-------------|------------------------|
| `application-docker.properties`  | Local       | docker profile         |
| `application.properties`         | Local       | all profiles           |
| `asapp-users-service.properties` | Centralized | service-specific       |
| `application-docker.properties`  | Centralized | shared, docker profile |
| `application-dev.properties`     | Centralized | shared, dev profile    |
| `application.properties`         | Centralized | shared                 |

### Docker Environment Variables

| Variable                      | Description                                | Default                                                            |
|-------------------------------|--------------------------------------------|--------------------------------------------------------------------|
| `JAVA_OPTS`                   | JVM runtime options                        | (see docker-compose.yaml)                                          |
| `SPRING_PROFILES_ACTIVE`      | Active Spring profiles                     | `docker,dev`                                                       |
| `SERVER_PORT`                 | HTTP server port                           | `8082`                                                             |
| `MANAGEMENT_PORT`             | Actuator management port                   | `8092`                                                             |
| `DB_HOST`                     | PostgreSQL hostname                        | `asapp-users-postgres-db`                                          |
| `DB_PORT`                     | PostgreSQL port                            | `5432`                                                             |
| `DB_NAME`                     | PostgreSQL database name                   | `usersdb`                                                          |
| `DB_USERNAME`                 | PostgreSQL username                        | `user`                                                             |
| `DB_PASSWORD`                 | PostgreSQL password                        | `secret`                                                           |
| `REDIS_HOST`                  | Redis hostname                             | `asapp-redis`                                                      |
| `REDIS_PORT`                  | Redis port                                 | `6379`                                                             |
| `REDIS_PASSWORD`              | Redis password                             | `secret`                                                           |
| `CONFIG_SERVER_URI`           | Config server base URI                     | `http://asapp-config-service:8888/asapp-config-service`            |
| `CONFIG_SERVER_USERNAME`      | Config server HTTP Basic username          | `user`                                                             |
| `CONFIG_SERVER_PASSWORD`      | Config server HTTP Basic password          | `secret`                                                           |
| `DISCOVERY_HOST`              | Eureka server hostname                     | `asapp-discovery-service:8761/asapp-discovery-service`             |
| `DISCOVERY_USERNAME`          | Eureka server username                     | `user`                                                             |
| `DISCOVERY_PASSWORD`          | Eureka server password                     | `secret`                                                           |
| `SERVICE_USERNAME`            | HTTP Basic username for actuator endpoints | `user`                                                             |
| `SERVICE_PASSWORD`            | HTTP Basic password for actuator endpoints | `secret`                                                           |
| `ASAPP_SECURITY_JWT_SECRET`   | HMAC-SHA secret for signing JWT tokens     | `qPxa4PP692Q4fx6voNBX25WoQrzjCoLWLW3VnABjZaOImy0cQaTad5DqBZk3qPxi` |
| `ASAPP_CLIENT_TASKS_BASE_URL` | Tasks service base URL                     | `http://asapp-tasks-service/asapp-tasks-service`                   |

### API Endpoints

**User Endpoints**

| Method | Endpoint          | Description                 | Auth Required |
|--------|-------------------|-----------------------------|---------------|
| POST   | `/api/users`      | Create user profile         | ✅             |
| GET    | `/api/users`      | Get all users               | ✅             |
| GET    | `/api/users/{id}` | Get user by ID (with tasks) | ✅             |
| PUT    | `/api/users/{id}` | Update user profile         | ✅             |
| DELETE | `/api/users/{id}` | Delete user                 | ✅             |

**Management Endpoints**

Actuator endpoints are on port `8092` at `/asapp-users-service/actuator`; use `GET /actuator` to list them.

- `/actuator/health` — public
- All other actuator endpoints — HTTP Basic authentication (`user` / `secret`)

Health probes are on the server port (`8082`) at `/asapp-users-service` and are public.

- `/asapp-users-service/readyz`
- `/asapp-users-service/livez`

### Documentation

| Artifact        | Location                                                                       |
|-----------------|--------------------------------------------------------------------------------|
| REST API docs   | `target/generated-docs/api-guide.html`                                         |
| Swagger UI      | `http://localhost:8082/asapp-users-service/swagger-ui.html` (dev profile only) |
| BootUI console  | `http://localhost:8082/asapp-users-service/bootui` (dev profile only)          |
| Test coverage   | `target/site/jacoco-aggregate/index.html`                                      |
| Mutation report | `target/pit-reports/<timestamp>/index.html`                                    |
| Javadoc         | `target/site/apidocs/index.html`                                               |

### Monitoring

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:

- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- REST client metrics (tasks service calls)

### Dependencies

**Internal Dependencies**:

- `asapp-commons-url` - Endpoint constants
- `asapp-rest-clients` - Tasks service HTTP client contract

**External Dependencies**:

- `asapp-authentication-service` - For JWT validation
- `asapp-tasks-service` - For task queries via REST

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
- [Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/jdbc.html)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [TestContainers](https://java.testcontainers.org/)
- [MockServer](https://www.mock-server.com/)

---

## License

ASAPP Users Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
