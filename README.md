# ASAPP

> A Spring Boot microservices application for task management with JWT authentication

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://img.shields.io/github/actions/workflow/status/attrigo/asapp/ci.yml?branch=main)](https://github.com/attrigo/asapp/actions)

## Overview

ASAPP (Application for Task Management) is a production-ready microservices application built with Spring Boot 3.4.3 and Java 21. It demonstrates modern enterprise architecture patterns including Hexagonal Architecture, Domain-Driven Design, and comprehensive observability.

**Key Features**:
- 🏗️ **Hexagonal Architecture** - Clean separation of concerns with ports and adapters
- 🎯 **Domain-Driven Design** - Rich domain models with explicit aggregates and value objects
- 🔐 **JWT Authentication** - Secure token-based authentication with refresh capability
- 📊 **Observability** - Prometheus metrics and Grafana dashboards
- 🧪 **Test Coverage** - JaCoCo coverage reports and PITest mutation testing
- 🐳 **Docker Support** - Full Docker Compose setup for local development
- 📝 **OpenAPI Documentation** - Interactive Swagger UI for all services

## Architecture

### Microservices

ASAPP consists of three independent microservices:

| Service | Port | Purpose | Database |
|---------|------|---------|----------|
| **Authentication** | 8080 | User credentials & JWT tokens | authenticationdb |
| **Users** | 8082 | User profile management | usersdb |
| **Tasks** | 8081 | Task CRUD operations | tasksdb |

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                               │
└────────────┬────────────────────────────────────────────────┘
             │ JWT Bearer Token
             ▼
    ┌────────────────────┐
    │  Authentication    │ :8080
    │     Service        │──────► authenticationdb
    └────────────────────┘
             │
             │ JWT Token
             ▼
    ┌────────────────────┐         ┌────────────────────┐
    │      Users         │ :8082   │      Tasks         │ :8081
    │     Service        │◄───────►│     Service        │
    └─────────┬──────────┘         └─────────┬──────────┘
              │                              │
              ▼                              ▼
          usersdb                        tasksdb

    ┌────────────────────┐         ┌────────────────────┐
    │    Prometheus      │ :9090   │     Grafana        │ :3000
    │   (Metrics DB)     │◄────────│  (Visualization)   │
    └────────────────────┘         └────────────────────┘
```

### Architectural Patterns

**Hexagonal Architecture** (Ports & Adapters):
- **Domain Layer**: Pure business logic (aggregates, value objects, domain services)
- **Application Layer**: Use cases with input/output ports
- **Infrastructure Layer**: Adapters for REST, database, security

**Domain-Driven Design**:
- **4 Aggregates**: User (auth), JwtAuthentication, User (profile), Task
- **30+ Value Objects**: Type-safe domain concepts
- **Bounded Contexts**: Each service is an independent context
- **Ubiquitous Language**: Consistent terminology across layers

## Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** & **Docker Compose**
- **Git**

### Installation

```bash
# Clone repository
git clone https://github.com/attrigo/asapp.git
cd asapp

# Build all services
mvn clean install
```

### Running the Application

```bash
# 1. Build Docker images
mvn spring-boot:build-image

# 2. Start all services
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# 4. Access the application
# - Authentication Swagger: http://localhost:8080/asapp-authentication-service/swagger-ui.html
# - Users Swagger: http://localhost:8082/asapp-users-service/swagger-ui.html
# - Tasks Swagger: http://localhost:8081/asapp-tasks-service/swagger-ui.html
# - Grafana Dashboards: http://localhost:3000 (admin/secret)
# - Prometheus: http://localhost:9090
```

### Example Workflow

```bash
# 1. Create a user (via Authentication service)
curl -X POST http://localhost:8080/asapp-authentication-service/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe@asapp.com",
    "password": "SecurePass123!",
    "role": "USER"
  }'

# 2. Authenticate and get JWT token
TOKEN=$(curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe@asapp.com","password":"SecurePass123!"}' \
  | jq -r '.access_token')

# 3. Create user profile
USER_ID=$(curl -X POST http://localhost:8082/asapp-users-service/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@asapp.com",
    "phoneNumber": "+1-555-0123"
  }' | jq -r '.user_id')

# 4. Create a task
curl -X POST http://localhost:8081/asapp-tasks-service/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "user_id": "'$USER_ID'",
    "title": "Complete documentation",
    "description": "Update all README files",
    "start_date": "2025-01-15T09:00:00Z",
    "end_date": "2025-01-20T17:00:00Z"
  }'

# 5. Get user with tasks
curl -X GET http://localhost:8082/asapp-users-service/api/users/$USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Stopping the Application

```bash
# Stop all services and remove volumes
docker-compose down -v
```

## Project Structure

```
asapp/
├── libs/                                    # Shared libraries
│   ├── asapp-commons-url/                   # API endpoint constants
│   └── asapp-rest-clients/                  # REST client infrastructure
├── services/                                # Microservices
│   ├── asapp-authentication-service/        # JWT & credentials
│   ├── asapp-users-service/                 # User profiles
│   └── asapp-tasks-service/                 # Task management
├── tools/                                   # Monitoring tools
│   ├── prometheus/                          # Prometheus config
│   └── grafana/                             # Grafana dashboards
├── docs/claude/                             # AI-optimized documentation
│   ├── architecture.md
│   ├── testing.md
│   ├── api-conventions.md
│   ├── code-style.md
│   └── development-patterns.md
├── git/hooks/                               # Git hooks (pre-commit, commit-msg)
├── docker-compose.yaml                      # Docker services configuration
├── pom.xml                                  # Parent POM
└── CLAUDE.md                                # AI assistant guidance
```

## Services Overview

### Authentication Service

**Port**: 8080 (app), 8090 (actuator)

**Responsibilities**:
- User credential storage and validation
- JWT token generation (access + refresh)
- Token refresh and revocation
- User authentication management

**Database**: `authenticationdb` (port 5432)

**API Endpoints**:
- `POST /api/auth/token` - Authenticate
- `POST /api/auth/refresh` - Refresh tokens
- `POST /api/auth/revoke` - Revoke tokens
- `/api/users/*` - User CRUD

[View README](services/asapp-authentication-service/README.md)

### Users Service

**Port**: 8082 (app), 8092 (actuator)

**Responsibilities**:
- User profile management (firstName, lastName, email, phoneNumber)
- Task aggregation via Tasks service integration
- User queries and searches

**Database**: `usersdb` (port 5434)

**API Endpoints**:
- `POST /api/users` - Create profile
- `GET /api/users/{id}` - Get profile (with tasks)
- `PUT /api/users/{id}` - Update profile
- `DELETE /api/users/{id}` - Delete profile

[View README](services/asapp-users-service/README.md)

### Tasks Service

**Port**: 8081 (app), 8091 (actuator)

**Responsibilities**:
- Task lifecycle management
- Task CRUD operations
- User task ownership and queries

**Database**: `tasksdb` (port 5433)

**API Endpoints**:
- `POST /api/tasks` - Create task
- `GET /api/tasks/{id}` - Get task
- `GET /api/tasks/user/{id}` - Get user's tasks
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task

[View README](services/asapp-tasks-service/README.md)

## Technology Stack

### Core Technologies

- **Java**: 21 (LTS)
- **Spring Boot**: 3.4.3
- **Spring Framework**: 6.x
- **Database**: PostgreSQL 15+
- **Build Tool**: Apache Maven 3.9+

### Key Dependencies

- **Security**: Spring Security 6.x, JJWT 0.12.x
- **Data Access**: Spring Data JDBC
- **Migrations**: Liquibase 4.x
- **Mapping**: MapStruct 1.6.3
- **Validation**: Jakarta Validation
- **Documentation**: SpringDoc OpenAPI 2.8.5

### Testing Stack

- **Framework**: JUnit 5
- **Assertions**: AssertJ
- **Mocking**: Mockito (BDDMockito style)
- **Coverage**: JaCoCo 0.8.12
- **Mutation Testing**: PITest 1.20.4
- **Integration**: TestContainers (PostgreSQL)
- **E2E Mocking**: MockServer 5.15.0

### Observability

- **Metrics**: Spring Boot Actuator + Micrometer
- **Monitoring**: Prometheus 2.x
- **Visualization**: Grafana 10.x
- **Dashboards**: JVM Micrometer (pre-configured)

### Code Quality

- **Formatting**: Spotless Maven Plugin
- **Style**: Eclipse formatter (asapp_formatter.xml)
- **Git Hooks**: Pre-commit (style check), commit-msg (conventional commits)
- **CI/CD**: GitHub Actions

## Development

### Building the Project

```bash
# Build all modules
mvn clean install

# Build with tests and coverage
mvn clean verify

# Skip tests (faster)
mvn clean install -DskipTests

# Build Docker images
mvn spring-boot:build-image
```

### Running Tests

```bash
# All tests (unit + integration)
mvn clean verify

# Unit tests only
mvn test

# Integration tests only
mvn verify -DskipUnitTests

# Specific test class
mvn test -Dtest=UserTests

# Mutation testing (domain layer)
mvn org.pitest:pitest-maven:mutationCoverage
```

### Code Quality

```bash
# Check code formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply

# Install git hooks
mvn git-build-hook:install
```

### Database Management

```bash
# Navigate to specific service
cd services/asapp-authentication-service

# Apply Liquibase migrations
mvn liquibase:update

# Generate migration SQL (dry-run)
mvn liquibase:updateSQL

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Running Services Locally

```bash
# Start specific database
docker-compose up -d asapp-authentication-postgres-db

# Run service
cd services/asapp-authentication-service
mvn spring-boot:run

# Or run specific service via IDE (main class: AsappAuthenticationServiceApplication)
```

## Monitoring and Observability

### Accessing Monitoring Tools

| Tool | URL | Credentials | Purpose |
|------|-----|-------------|---------|
| **Grafana** | http://localhost:3000 | admin/secret | Metrics visualization |
| **Prometheus** | http://localhost:9090 | - | Metrics database |
| **Authentication Actuator** | http://localhost:8090/asapp-authentication-service/actuator | JWT | Health & metrics |
| **Users Actuator** | http://localhost:8092/asapp-users-service/actuator | JWT | Health & metrics |
| **Tasks Actuator** | http://localhost:8091/asapp-tasks-service/actuator | JWT | Health & metrics |

### Pre-configured Dashboards

**JVM Micrometer Dashboard**: Available in Grafana under "Services" folder

**Metrics Include**:
- JVM memory, GC, threads
- HTTP request rates and response times
- Database connection pool usage
- Custom business metrics

### Prometheus Service Account

Create this user for Prometheus to scrape metrics:

```bash
curl -X POST http://localhost:8080/asapp-authentication-service/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sa.prometheus@asapp.com",
    "password": "sa-secret",
    "role": "ADMIN"
  }'
```

## API Documentation

Each service provides interactive Swagger UI:

- **Authentication**: http://localhost:8080/asapp-authentication-service/swagger-ui.html
- **Users**: http://localhost:8082/asapp-users-service/swagger-ui.html
- **Tasks**: http://localhost:8081/asapp-tasks-service/swagger-ui.html

## Security

### JWT Authentication Flow

1. Client authenticates with credentials → `POST /api/auth/token`
2. Receives access token (5 min) + refresh token (1 hour)
3. Client includes `Authorization: Bearer <access_token>` in requests
4. Services validate JWT signature and extract claims
5. When access token expires, use refresh token → `POST /api/auth/refresh`

### Token Structure

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

### Protected vs Public Endpoints

**Public** (no authentication):
- `POST /api/auth/token` - Login
- `POST /api/auth/refresh` - Refresh token
- `POST /api/users` - Create user (authentication service)
- `/actuator/health` - Basic health check
- `/swagger-ui.html` - API documentation

**Protected** (requires JWT):
- All other `/api/*` endpoints
- `/actuator/prometheus` - Detailed metrics
- `/actuator/*` - Most actuator endpoints

## Contributing

### Code Standards

- **Architecture**: Follow Hexagonal Architecture and DDD patterns
- **Formatting**: Run `mvn spotless:apply` before committing
- **Testing**: Maintain high test coverage (unit + integration + E2E)
- **Commits**: Use Conventional Commits format
- **Documentation**: Update OpenAPI docs for API changes

### Commit Message Format

```
<type>(<scope>): <description>

Types: feat, fix, chore, docs, test, refactor, ci, build, perf, revert, style
```

**Examples**:
```
feat(auth): add refresh token endpoint
fix(tasks): resolve null pointer in update
test(users): add integration tests for profile creation
docs: update architecture documentation
```

### Git Hooks

Automatically installed on `mvn install`:

- **pre-commit**: Validates code formatting and line endings
- **commit-msg**: Validates commit message format

**Manual installation**:
```bash
mvn git-build-hook:install
```

### Pull Request Process

1. Create feature branch from `main`
2. Make changes following code standards
3. Run `mvn clean verify` (all tests must pass)
4. Commit with conventional commit messages
5. Push and create PR to `main`
6. CI pipeline runs automatically
7. Merge after review and CI success

## Release Process

### Creating a Release

```bash
# 1. Checkout main
git checkout main

# 2. Remove SNAPSHOT version
mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false

# 3. Add Liquibase database tags to all v<version>-changelog.xml files
# (Manual step - add tagDatabase changeset)

# 4. Build and verify
mvn clean install

# 5. Commit release
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: release version ${RELEASE_VERSION}"

# 6. Create tag
git tag ${RELEASE_VERSION}

# 7. Prepare next development version
mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false

# 8. Commit next version
NEXT_DEV_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"

# 9. Push atomically
git push --atomic origin main ${RELEASE_VERSION}
```

## Documentation

### For Developers

Comprehensive guides for working with ASAPP:

- [Architecture & Domain Design](docs/guidelines/architecture.md) - Hexagonal layers, DDD patterns
- [Testing Strategy](docs/guidelines/testing.md) - Test types, coverage, tools
- [API Conventions](docs/guidelines/api-conventions.md) - REST patterns, DTOs, OpenAPI
- [Code Style & Build](docs/guidelines/code-style.md) - Formatting, git hooks, commands
- [Development Workflows](docs/guidelines/development-patterns.md) - Adding features, releases

### For AI Assistants

[CLAUDE.md](CLAUDE.md) - Guidance for AI tools like Claude Code

### Service-Specific

- [Authentication Service](services/asapp-authentication-service/README.md)
- [Users Service](services/asapp-users-service/README.md)
- [Tasks Service](services/asapp-tasks-service/README.md)

### Shared Libraries

- [Commons URL](libs/asapp-commons-url/README.md)
- [REST Clients](libs/asapp-rest-clients/README.md)

## Troubleshooting

### Services Not Starting

```bash
# Check Docker containers
docker-compose ps

# View service logs
docker-compose logs -f asapp-authentication-service

# Restart specific service
docker-compose restart asapp-authentication-service
```

### Database Issues

```bash
# Reset databases
docker-compose down -v  # -v removes volumes
docker-compose up -d

# Check database connectivity
docker exec -it asapp-authentication-postgres-db psql -U user -d authenticationdb
```

### JWT Authentication Errors

**Issue**: 401 Unauthorized

**Solutions**:
1. Verify token hasn't expired (access tokens expire after 5 minutes)
2. Use refresh token to get new access token
3. Re-authenticate if refresh token expired
4. Ensure `Authorization: Bearer <token>` header is included

### Prometheus Not Scraping

```bash
# 1. Check Prometheus targets
open http://localhost:9090/targets

# 2. Verify Prometheus service account exists
curl -X POST http://localhost:8080/asapp-authentication-service/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"sa.prometheus@asapp.com","password":"sa-secret"}'

# 3. Check authentication container logs
docker-compose logs asapp-prometheus-authentication
```

## CI/CD

### GitHub Actions Workflow

**File**: `.github/workflows/ci.yml`

**Triggers**:
- Push to `main` branch
- Pull requests to `main`

**Pipeline Steps**:
1. Checkout code
2. Setup JDK 21 (Temurin)
3. Maven dependency caching
4. Build and test (`mvn verify`)

**Reports Generated**:
- JaCoCo coverage (unit, integration, aggregate)
- Surefire test results (unit tests)
- Failsafe test results (integration tests)

## Requirements

### Runtime Requirements

- **Java Runtime**: JDK 21 or higher
- **Database**: PostgreSQL 15+ (provided via Docker)
- **Container Runtime**: Docker 20.10+ & Docker Compose 2.0+

### Development Requirements

- **Java Development Kit**: JDK 21 (Temurin, Oracle, or OpenJDK)
- **Build Tool**: Apache Maven 3.9.0+
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git**: 2.30+

## License

ASAPP is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

```
Copyright 2023 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Authors

- **Antonio Trigo** - [@attrigo](https://github.com/attrigo)

## Acknowledgments

- Spring Boot team for the excellent framework
- Hexagonal Architecture pattern by Alistair Cockburn
- Domain-Driven Design principles by Eric Evans
- All open-source contributors to the dependencies used in this project
