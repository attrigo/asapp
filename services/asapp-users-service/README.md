# ASAPP Users Service

> User profile management and task aggregation for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

The Users Service manages user profile information within the ASAPP ecosystem. It maintains personal details (name, email, phone) and integrates with the Tasks service to provide aggregated user data.

**Key Responsibilities**:
- 👤 User profile management (firstName, lastName, email, phoneNumber)
- 📋 Task aggregation via TasksGateway
- 🔍 User queries and searches
- 🔗 Inter-service communication with Tasks service
- 🛡️ JWT-based authentication and authorization

## Features

### User Profile Operations

- **Create User** - Register new user profile
  - `POST /api/users`
  - Validates email format and required fields

- **Get User by ID** - Retrieve user profile with tasks
  - `GET /api/users/{id}`
  - Includes user's tasks from Tasks service
  - Graceful degradation if Tasks service unavailable

- **Get All Users** - List all user profiles
  - `GET /api/users`
  - Returns user summaries without tasks

- **Update User** - Modify user profile information
  - `PUT /api/users/{id}`
  - Updates firstName, lastName, email, phoneNumber

- **Delete User** - Remove user profile
  - `DELETE /api/users/{id}`
  - Cascade delete from database

### Inter-Service Integration

- **Tasks Integration**: Uses `TasksClient` to retrieve user's tasks
- **JWT Propagation**: Automatically forwards authentication context
- **Graceful Degradation**: Returns empty task list if Tasks service fails

### Observability

- **Health Check** - Service health and dependencies
  - `GET /actuator/health`

- **Metrics** - Prometheus-formatted application metrics
  - `GET /actuator/prometheus`

- **API Documentation** - Interactive Swagger UI
  - `http://localhost:8082/asapp-users-service/swagger-ui.html`

## Architecture

### Hexagonal Architecture

The service follows **Hexagonal Architecture** (Ports & Adapters) with clear separation of concerns:

**Domain Layer** (`domain/`):
- Pure business logic with no framework dependencies
- **Aggregate**: User (profile entity)
- **Value Objects**: UserId, FirstName, LastName, Email, PhoneNumber

**Application Layer** (`application/`):
- Use cases and orchestration
- **Input Ports**: CreateUserUseCase, ReadUserUseCase, UpdateUserUseCase, DeleteUserUseCase
- **Output Ports**: UserRepository, TasksGateway
- **Services**: Annotated with `@ApplicationService`

**Infrastructure Layer** (`infrastructure/`):
- External concerns (REST, database, security, external services)
- REST controllers and DTOs
- Repository adapters (Spring Data JDBC)
- Security components (JWT validation)
- Tasks service client integration

### Domain-Driven Design

The service implements **DDD patterns**:

**Aggregate**:
- `User` - Two-state pattern (create/reconstitute)
- Encapsulates: firstName, lastName, email, phoneNumber

**Value Objects**: Immutable records with validation
- FirstName, LastName, Email, PhoneNumber, UserId

**Bounded Context**:
- Manages user profiles (separate from authentication User)
- References tasks via userId
- Queries Tasks service through anti-corruption layer (TasksGateway)

### Security Model

**JWT Validation**:
- Validates tokens issued by Authentication service
- Token validation includes signature verification and Redis-based revocation checks
- Extracts claims (username, role) for authorization
- Revoked tokens (removed from Redis) return 401 Unauthorized

**Protected Endpoints**: All `/api/users/*` endpoints require Bearer token (except POST for creation)

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
docker-compose up -d asapp-users-postgres-db

# 2. Run the service
mvn spring-boot:run

# 3. Access Swagger UI
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

## Configuration

### Application Properties

**Key Configuration** (`application.properties`):

```properties
# Server
server.port=8082
server.servlet.context-path=/asapp-users-service

# Database
spring.datasource.url=jdbc:postgresql://localhost:5434/usersdb
spring.datasource.username=user
spring.datasource.password=secret

# JWT Security
asapp.security.jwt-secret=<base64-encoded-secret>

# Tasks Service Client
asapp.client.tasks.base-url=http://localhost:8081/asapp-tasks-service

# Actuator (management port)
management.server.port=8092
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
docker-compose up -d asapp-users-postgres-db

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
open target/asapp-users-service-<version>-javadoc.jar
# Or: target/site/apidocs/index.html

# View Test Coverage
open target/site/jacoco-aggregate/index.html

# View Mutation Testing Report
open target/pit-reports/<timestamp>/index.html
```

## API Endpoints

### User Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/users` | Create user profile | ✅ |
| GET | `/api/users` | Get all users | ✅ |
| GET | `/api/users/{id}` | Get user by ID (with tasks) | ✅ |
| PUT | `/api/users/{id}` | Update user profile | ✅ |
| DELETE | `/api/users/{id}` | Delete user | ✅ |

### Actuator Endpoints (Protected)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health status |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/actuator/metrics` | Available metrics list |
| GET | `/actuator/info` | Application info |

**Actuator Port**: `8092` (separate from application port `8082`)

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.4.3
- **Database**: PostgreSQL
- **Migrations**: Liquibase
- **Security**: Spring Security + JWT validation
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, AssertJ, TestContainers, MockServer, PITest
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Observability**: Spring Boot Actuator, Micrometer
- **REST Clients**: Spring RestClient (for Tasks service integration)

## Project Structure

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

## Database Schema

**Tables**:
- `users` - User profiles (id, first_name, last_name, email, phone_number)

**Migrations**: Managed by Liquibase in `src/main/resources/liquibase/db/changelog/`

## Testing

**Test Types**:
- Unit Tests (`*Tests.java`) - Domain and application logic
- Integration Tests (`*IT.java`) - With TestContainers PostgreSQL
- Controller Tests (`*ControllerIT.java`) - WebMvcTest slice
- E2E Tests (`*E2EIT.java`) - Full application context with MockServer

**E2E Testing**: Uses MockServer to mock Tasks service responses

**Coverage**: JaCoCo reports (unit, integration, aggregate)
**Mutation Testing**: PITest for domain layer
**Test Containers**: PostgreSQL for integration tests

## Monitoring

**Actuator Endpoints**: `http://localhost:8092/asapp-users-service/actuator`

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- REST client metrics (tasks service calls)

## Dependencies

**Internal Dependencies**:
- `asapp-commons-url` - Endpoint constants
- `asapp-rest-clients` - Tasks service client

**External Dependencies**:
- Authentication Service (for JWT validation)
- Tasks Service (for task queries via REST)

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
- [TestContainers](https://java.testcontainers.org/)
- [MockServer](https://www.mock-server.com/)

## License

ASAPP Users Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
