# ASAPP Tasks Service

> Task management and lifecycle operations for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

The Tasks Service manages task creation, updates, and lifecycle within the ASAPP ecosystem. It provides task CRUD operations and enables users to organize their
work items with titles, descriptions, and date ranges.

**Key Responsibilities**:

- 📝 Task CRUD operations (create, read, update, delete)
- 👤 User task ownership and queries
- 📅 Task scheduling (start date, end date)
- 🔗 Integration with Users service
- 🛡️ JWT-based authentication and authorization

---

## Features

### Task Operations

- **Create Task**: Create new task for a user
- **Get Task by ID**: Retrieve specific task
- **Get Tasks by User ID**: Retrieve all tasks for a user
- **Get Tasks**: List all tasks, optionally filtered by a list of IDs
- **Update Task**: Modify task title, description, and dates
- **Delete Task**: Remove task

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

# 3. Start PostgreSQL database
docker-compose up -d asapp-tasks-postgres-db

# 4. Run the service
./gradlew :services:asapp-tasks-service:bootRun

# 5. Access Swagger UI
open http://localhost:8081/asapp-tasks-service/swagger-ui.html
```

### Run with Docker

```bash
# 1. Build Docker image
./gradlew :services:asapp-tasks-service:bootBuildImage

# 2. Start service with database
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-tasks-service

# 4. Stop and clean
docker-compose down -v
```

### Example API Usage

```bash
# 1. Get JWT from Authentication service
TOKEN=$(curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user@asapp.com","password":"SecurePass123!"}' \
  | jq -r '.accessToken')

# 2. Create a task
curl -X POST http://localhost:8081/asapp-tasks-service/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "title": "Complete project documentation",
    "description": "Update all README files with current information",
    "startDate": "2025-01-15T09:00:00Z",
    "endDate": "2025-01-20T17:00:00Z"
  }'

# 3. Get tasks by user ID
curl -X GET http://localhost:8081/asapp-tasks-service/api/tasks/user/{userId} \
  -H "Authorization: Bearer $TOKEN"

# 4. Update task
curl -X PUT http://localhost:8081/asapp-tasks-service/api/tasks/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "title": "Updated title",
    "description": "Updated description",
    "startDate": "2025-01-16T09:00:00Z",
    "endDate": "2025-01-21T17:00:00Z"
  }'
```

---

## Configuration & Profiles

The service is **secure-by-default**: with no environment profile, Swagger UI and Boot-UI are fully off and the Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

- **Local** — `./gradlew :services:asapp-tasks-service:bootRun` activates `dev` (wired in the build script).
- **Docker stack** — `docker,dev`.
- **Locked-down deploy** — `SPRING_PROFILES_ACTIVE=docker,prod`.

### Property resolution

tasks-service is a **config-server client**: at startup (via `spring.config.import`) it merges properties from two locations —

- **Local** — its own `src/main/resources/`.
- **Shared** — `central-config/`, served by the Config Service.

Local beats Shared, a profile overlay (`application-<profile>`) beats its base, and an overlay applies only when its profile is active. Highest precedence first:

```
Local   application-docker.properties                 (docker overlay)
Local   application.properties                        (base)
Shared  central-config/asapp-tasks-service.properties (service-specific)
Shared  central-config/application-docker.properties  (docker overlay)
Shared  central-config/application-dev.properties     (dev overlay)
Shared  central-config/application.properties         (base)
```

---

## Architecture

### Domain Model

**Aggregate**: Task  
**Value Objects**: TaskId, UserId, Title, Description, StartDate, EndDate

### Security Model

- **API endpoints**: JWT Bearer token required. Tokens are verified for signature, expiry, and active status in Redis
- **Management endpoints**: HTTP Basic authentication

### Data Stores

**PostgreSQL**: (`tasksdb`) tasks records

- `tasks` — id, user_id, title, description, start_date, end_date

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

### Project Structure

```
src/main/java/com/attrigo/asapp/tasks/
├── domain/                           # Pure business logic
│   └── task/                         # Task aggregate
├── application/                      # Use cases
│   ├── task/in/                      # Task use cases
│   └── task/out/                     # Task repository (port)
└── infrastructure/                   # External concerns
    ├── task/in/                      # Task REST controllers
    ├── task/out/                     # Task repository adapter
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
./gradlew :services:asapp-tasks-service:pitest
```

### Run Locally

Run from the repository root, never from the module directory.

```bash
# Run the service
./gradlew :services:asapp-tasks-service:bootRun

# Override the active profiles
./gradlew :services:asapp-tasks-service:bootRun --args='--spring.profiles.active=dev'
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
docker-compose up -d asapp-tasks-postgres-db

# Preview the pending migrations as SQL (prints to stdout)
./gradlew :services:asapp-tasks-service:liquibaseUpdateSql

# ...or send that SQL to a file instead
./gradlew :services:asapp-tasks-service:liquibaseUpdateSql -PliquibaseOutputFile=migration.sql

# Apply Liquibase migrations
./gradlew :services:asapp-tasks-service:liquibaseUpdate

# Rollback last changeset
./gradlew :services:asapp-tasks-service:liquibaseRollbackCount -PliquibaseCount=1
```

### Generate Documentation

```bash
# Generate all reports (except `pitest`)
./gradlew :services:asapp-tasks-service:fullBuild
```

Generate specific report: `./gradlew :services:asapp-tasks-service:<command>`

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

| File                             | Source      | Scope                  |
|----------------------------------|-------------|------------------------|
| `application-docker.properties`  | Local       | docker profile         |
| `application.properties`         | Local       | all profiles           |
| `asapp-tasks-service.properties` | Centralized | service-specific       |
| `application-docker.properties`  | Centralized | shared, docker profile |
| `application-dev.properties`     | Centralized | shared, dev profile    |
| `application.properties`         | Centralized | shared                 |

### Docker Environment Variables

| Variable                    | Description                                |
|-----------------------------|--------------------------------------------|
| `JAVA_OPTS`                 | JVM runtime options                        |
| `SPRING_PROFILES_ACTIVE`    | Active Spring profiles                     |
| `CONFIG_SERVER_URI`         | Config server base URI                     |
| `CONFIG_SERVER_USERNAME`    | Config server HTTP Basic username          |
| `CONFIG_SERVER_PASSWORD`    | Config server HTTP Basic password          |
| `SERVER_PORT`               | HTTP server port                           |
| `SERVICE_USERNAME`          | HTTP Basic username for actuator endpoints |
| `SERVICE_PASSWORD`          | HTTP Basic password for actuator endpoints |
| `DB_HOST`                   | PostgreSQL hostname                        |
| `DB_PORT`                   | PostgreSQL port                            |
| `DB_NAME`                   | PostgreSQL database name                   |
| `DB_USERNAME`               | PostgreSQL username                        |
| `DB_PASSWORD`               | PostgreSQL password                        |
| `REDIS_HOST`                | Redis hostname                             |
| `REDIS_PORT`                | Redis port                                 |
| `REDIS_PASSWORD`            | Redis password                             |
| `MANAGEMENT_PORT`           | Actuator management port                   |
| `DISCOVERY_HOST`            | Eureka server hostname                     |
| `DISCOVERY_USERNAME`        | Eureka server username                     |
| `DISCOVERY_PASSWORD`        | Eureka server password                     |
| `ASAPP_SECURITY_JWT_SECRET` | HMAC-SHA secret for signing JWT tokens     |

### API Endpoints

**Task Endpoints**

| Method | Endpoint               | Description                   | Auth Required |
|--------|------------------------|-------------------------------|---------------|
| POST   | `/api/tasks`           | Create task                   | ✅             |
| GET    | `/api/tasks`           | Get tasks (optionally by IDs) | ✅             |
| GET    | `/api/tasks/{id}`      | Get task by ID                | ✅             |
| GET    | `/api/tasks/user/{id}` | Get tasks by user ID          | ✅             |
| PUT    | `/api/tasks/{id}`      | Update task                   | ✅             |
| DELETE | `/api/tasks/{id}`      | Delete task                   | ✅             |

**Management Endpoints**

Actuator endpoints are on port `8091` at `/asapp-tasks-service/actuator`; use `GET /actuator` to list them.

- `/actuator/health` — public
- All other actuator endpoints — HTTP Basic authentication (`user` / `secret`)

Health probes are on the server port (`8081`) at `/asapp-tasks-service` and are public.

- `/asapp-tasks-service/readyz`
- `/asapp-tasks-service/livez`

### Documentation

| Artifact             | Location                                                                       |
|----------------------|--------------------------------------------------------------------------------|
| REST API docs        | `build/docs/asciidoc/api-guide.html`                                           |
| Swagger UI           | `http://localhost:8081/asapp-tasks-service/swagger-ui.html` (dev profile only) |
| BootUI console       | `http://localhost:8081/asapp-tasks-service/bootui` (dev profile only)          |
| Unit coverage        | `build/reports/jacoco/test/html/index.html`                                    |
| Integration coverage | `build/reports/jacoco/jacocoIntegrationTestReport/html/index.html`             |
| Merged coverage      | `build/reports/jacoco/jacocoMergedReport/html/index.html`                      |
| Mutation report      | `build/reports/pitest/index.html`                                              |
| Javadoc              | `build/docs/javadoc/index.html`                                                |

### Monitoring

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:

- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Task-specific business metrics

### Dependencies

**Internal Dependencies**:

- `asapp-commons-url` - Endpoint constants

**External Dependencies**:

- `asapp-authentication-service` - For JWT validation

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
- [Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/jdbc.html)
- [Liquibase](https://docs.liquibase.com/)
- [BootUI](https://github.com/jdubois/boot-ui)

---

## License

ASAPP Tasks Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
