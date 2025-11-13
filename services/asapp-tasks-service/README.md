# ASAPP Tasks Service

> Task management and lifecycle operations for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

The Tasks Service manages task creation, updates, and lifecycle within the ASAPP ecosystem. It provides task CRUD operations and enables users to organize their work items with titles, descriptions, and date ranges.

**Key Responsibilities**:
- 📝 Task CRUD operations (create, read, update, delete)
- 👤 User task ownership and queries
- 📅 Task scheduling (start date, end date)
- 🔗 Integration with Users service
- 🛡️ JWT-based authentication and authorization

## Features

### Task Operations

- **Create Task** - Create new task for a user
  - `POST /api/tasks`
  - Requires: userId, title, description, startDate, endDate

- **Get Task by ID** - Retrieve specific task
  - `GET /api/tasks/{id}`
  - Returns task details

- **Get Tasks by User ID** - Retrieve all tasks for a user
  - `GET /api/tasks/user/{id}`
  - Supports filtering by user

- **Get All Tasks** - List all tasks in system
  - `GET /api/tasks`
  - Returns all tasks across all users

- **Update Task** - Modify task details
  - `PUT /api/tasks/{id}`
  - Updates title, description, dates

- **Delete Task** - Remove task
  - `DELETE /api/tasks/{id}`
  - Cascade delete from database

### Date Handling

- **ISO-8601 Format**: All dates use standard ISO-8601 format
- **Optional Dates**: Start and end dates are optional
- **Validation**: Dates validated at domain level

### Observability

- **Health Check** - Service health and dependencies
  - `GET /actuator/health`

- **Metrics** - Prometheus-formatted application metrics
  - `GET /actuator/prometheus`

- **API Documentation** - Interactive Swagger UI
  - `http://localhost:8081/asapp-tasks-service/swagger-ui.html`

## Architecture

### Hexagonal Architecture

The service follows **Hexagonal Architecture** (Ports & Adapters) with clear separation of concerns:

**Domain Layer** (`domain/`):
- Pure business logic with no framework dependencies
- **Aggregate**: Task
- **Value Objects**: TaskId, UserId, Title, Description, StartDate, EndDate

**Application Layer** (`application/`):
- Use cases and orchestration
- **Input Ports**: CreateTaskUseCase, ReadTaskUseCase, UpdateTaskUseCase, DeleteTaskUseCase
- **Output Ports**: TaskRepository
- **Services**: Annotated with `@ApplicationService`

**Infrastructure Layer** (`infrastructure/`):
- External concerns (REST, database, security)
- REST controllers and DTOs
- Repository adapters (Spring Data JDBC)
- Security components (JWT validation)

### Domain-Driven Design

The service implements **DDD patterns**:

**Aggregate**:
- `Task` - Two-state pattern (create/reconstitute)
- Encapsulates: userId (owner), title, description, startDate, endDate

**Value Objects**: Immutable records with validation
- TaskId, UserId, Title, Description, StartDate, EndDate

**Bounded Context**:
- Manages tasks independently
- References users via userId (no direct dependency on Users service)
- Provides task queries for Users service integration

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL (via Docker)
- Authentication Service (for JWT validation)

### Run Locally (Development Mode)

```bash
# 1. Start PostgreSQL database
docker-compose up -d asapp-tasks-postgres-db

# 2. Run the service
mvn spring-boot:run

# 3. Access Swagger UI
open http://localhost:8081/asapp-tasks-service/swagger-ui.html
```

### Run with Docker

```bash
# 1. Build Docker image
mvn spring-boot:build-image

# 2. Start service with database
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-tasks-service

# 4. Stop and clean
docker-compose down -v
```

### Example API Usage

```bash
# 1. Get JWT token from Authentication service
TOKEN=$(curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user@asapp.com","password":"SecurePass123!"}' \
  | jq -r '.access_token')

# 2. Create a task
curl -X POST http://localhost:8081/asapp-tasks-service/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "user_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "title": "Complete project documentation",
    "description": "Update all README files with current information",
    "start_date": "2025-01-15T09:00:00Z",
    "end_date": "2025-01-20T17:00:00Z"
  }'

# 3. Get tasks by user ID
curl -X GET http://localhost:8081/asapp-tasks-service/api/tasks/user/{userId} \
  -H "Authorization: Bearer $TOKEN"

# 4. Update task
curl -X PUT http://localhost:8081/asapp-tasks-service/api/tasks/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Updated title",
    "description": "Updated description",
    "start_date": "2025-01-16T09:00:00Z",
    "end_date": "2025-01-21T17:00:00Z"
  }'
```

## Configuration

### Application Properties

**Key Configuration** (`application.properties`):

```properties
# Server
server.port=8081
server.servlet.context-path=/asapp-tasks-service

# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/tasksdb
spring.datasource.username=user
spring.datasource.password=secret

# JWT Security
asapp.security.jwt-secret=<base64-encoded-secret>

# Actuator (management port)
management.server.port=8091
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
docker-compose up -d asapp-tasks-postgres-db

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
open target/asapp-tasks-service-<version>-javadoc.jar
# Or: target/site/apidocs/index.html

# View Test Coverage
open target/site/jacoco-aggregate/index.html

# View Mutation Testing Report
open target/pit-reports/<timestamp>/index.html
```

## API Endpoints

### Task Endpoints (All Protected)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/tasks` | Create task | ✅ |
| GET | `/api/tasks` | Get all tasks | ✅ |
| GET | `/api/tasks/{id}` | Get task by ID | ✅ |
| GET | `/api/tasks/user/{id}` | Get tasks by user ID | ✅ |
| PUT | `/api/tasks/{id}` | Update task | ✅ |
| DELETE | `/api/tasks/{id}` | Delete task | ✅ |

### Actuator Endpoints (Protected)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health status |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/actuator/metrics` | Available metrics list |
| GET | `/actuator/info` | Application info |

**Actuator Port**: `8091` (separate from application port `8081`)

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.4.3
- **Database**: PostgreSQL
- **Migrations**: Liquibase
- **Security**: Spring Security + JWT validation
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, AssertJ, TestContainers, PITest
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Observability**: Spring Boot Actuator, Micrometer

## Project Structure

```
src/main/java/com/bcn/asapp/tasks/
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

## Database Schema

**Tables**:
- `tasks` - Tasks (id, user_id, title, description, start_date, end_date)

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

**Indexes**:
- Primary key on `id`
- Foreign key reference to user via `user_id`
- Index on `user_id` for efficient user task queries

## Testing

**Test Types**:
- Unit Tests (`*Tests.java`) - Domain and application logic
- Integration Tests (`*IT.java`) - With TestContainers PostgreSQL
- Controller Tests (`*ControllerIT.java`) - WebMvcTest slice
- E2E Tests (`*E2EIT.java`) - Full application context

**Coverage**: JaCoCo reports (unit, integration, aggregate)
**Mutation Testing**: PITest for domain layer
**Test Containers**: PostgreSQL for integration tests

## Monitoring

**Actuator Endpoints**: `http://localhost:8091/asapp-tasks-service/actuator`

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Task-specific business metrics

## Dependencies

**Internal Dependencies**:
- `asapp-commons-url` - Endpoint constants

**External Dependencies**:
- Authentication Service (for JWT validation)

## Requirements

- **Java**: 21 or higher
- **Maven**: 3.9.0 or higher
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **PostgreSQL**: 15+ (via Docker)

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
- [Testing Strategy](../../docs/claude/testing.md)
- [API Conventions](../../docs/claude/api-conventions.md)

## External Resources

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/jdbc.html)
- [Liquibase](https://docs.liquibase.com/)

## License

ASAPP Tasks Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
