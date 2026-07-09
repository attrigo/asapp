# ASAPP

> A Spring Boot microservices application for task management with JWT authentication

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://img.shields.io/github/actions/workflow/status/attrigo/asapp/ci.yml?branch=main)](https://github.com/attrigo/asapp/actions)

---

## Overview

ASAPP (Application for Task Management) is a production-ready microservices application built with Spring Boot and Java. It demonstrates modern enterprise
architecture patterns including Hexagonal Architecture, Domain-Driven Design, and comprehensive observability.

**Key Features**:

- 🏗️ **Hexagonal Architecture** - Clean separation of concerns with ports and adapters
- 🎯 **Domain-Driven Design** - Rich domain models with explicit aggregates and value objects
- 🔐 **JWT Authentication** - Secure token-based authentication with refresh capability
- 📊 **Observability** - Prometheus metrics and Grafana dashboards
- 🧪 **Test Coverage** - JaCoCo coverage reports and PITest mutation testing
- 🐳 **Docker Support** - Full Docker Compose setup for local development
- 📝 **OpenAPI Documentation** - Interactive Swagger UI for all services
- 📖 **Self-Documented APIs** - Spring REST Docs generates accurate API documentation from tests
- ⚙️ **Centralized Configuration** - Spring Cloud Config Server for unified configuration management across all services

---

## Services

ASAPP consists of five microservices:

| Service            | Port      | Purpose                          | Documentation                                             |
|--------------------|-----------|----------------------------------|-----------------------------------------------------------|
| **Authentication** | 8080/8090 | User credentials & JWT tokens    | [README](services/asapp-authentication-service/README.md) |
| **Config**         | 8888/8898 | Centralized configuration server | [README](services/asapp-config-service/README.md)         |
| **Discovery**      | 8761/8791 | Service registry (Eureka)        | [README](services/asapp-discovery-service/README.md)      |
| **Tasks**          | 8081/8091 | Task operations                  | [README](services/asapp-tasks-service/README.md)          |
| **Users**          | 8082/8092 | User profile management          | [README](services/asapp-users-service/README.md)          |

---

## Requirements

- **Java**: 25+
- **Maven**: 3.9.14+
- **PostgreSQL**: 15+ (via Docker)
- **Redis**: 7+ (via Docker)
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Git**: 2.30+

---

## Quick Start

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
```

> The `docker-compose.yaml` is a **local-development convenience stack, not a deployable artifact** — it always runs the `dev` profile, so dev tools (Actuator, Swagger, BootUI, etc.) are intentionally wide open behind default credentials. See **Configuration & Profiles** below for the secure-by-default (`prod`) posture.

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
  | jq -r '.accessToken')

# 3. Create user profile
USER_ID=$(curl -X POST http://localhost:8082/asapp-users-service/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@asapp.com",
    "phoneNumber": "+1-555-0123"
  }' | jq -r '.userId')

# 4. Create a task
curl -X POST http://localhost:8081/asapp-tasks-service/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "title": "Complete documentation",
    "description": "Update all README files",
    "startDate": "2025-01-15T09:00:00Z",
    "endDate": "2025-01-20T17:00:00Z"
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

---

## Configuration & Profiles

ASAPP uses two orthogonal profile axes — a running service activates one value from each:

| Axis            | Values                        | Controls                                                                            |
|-----------------|-------------------------------|-------------------------------------------------------------------------------------|
| **Environment** | `dev`, `prod`                 | Swagger UI, Actuator exposure, heapdump/shutdown access, `info.env`, log verbosity  |
| **Platform**    | (default), `docker`, `native` | Service wiring (localhost vs env vars), config-server backend (config-service only) |

The base configuration is **secure-by-default**: with no environment profile active, a service is production-safe — Swagger and Boot-UI are fully off and Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

### Profile values

- **`dev`** — re-enables tooling; loads the `application-dev.properties` overlays.
- **`prod`** — the secure-by-default base; **no overlay file**, an explicit production marker (the base is already locked down).
- **`docker`** — container wiring via environment variables; loads `application-docker.properties`.
- **`native`** — **config-service only**; serves configuration from the `central-config/` filesystem. No other service uses it.

### Activating a profile

- **Local development** (default) — `mvn spring-boot:run` activates `dev` automatically; it's wired into each service's Maven plugin.
- **Docker stack** (default; local development only) — the committed `docker-compose.yaml` is a development convenience, not a deployable artifact: `docker-compose up -d` always runs `docker,dev`, leaving Swagger, BootUI, full Actuator, and heapdump/shutdown open behind default credentials.
- **Any other posture** — list the profiles explicitly: locally with `-Dspring-boot.run.profiles=…`, or for a packaged jar / container via the `SPRING_PROFILES_ACTIVE` environment variable. Example — the secure-by-default posture behind the Docker stack: `SPRING_PROFILES_ACTIVE=docker,prod`.

With no profile set at all (a bare `java -jar` or container), a service runs locked down — the secure-by-default baseline.

> config-service also needs `native` in every posture (it's the config-server backend), so prefix its profiles: `native,dev`, `native,docker,prod`, etc. See the [config-service README](services/asapp-config-service/README.md) for details.

### Property resolution

Properties come from up to two locations:

- **Local** — the service's own `src/main/resources/`.
- **Shared** — `central-config/`, served by the Config Service to its clients (Authentication, Tasks, Users).

> Config and Discovery are **not** config-server clients — they read only their local files (no shared layer).

The more specific source wins: a profile overlay (`application-<profile>.properties`) beats the base (`application.properties`), and Local beats Shared. An overlay loads only when its profile is active. Highest precedence first:

**Config-server client** (Auth, Tasks, Users)

```
Local   application.properties                          (own base)
Shared  central-config/<service>.properties             (service-specific)
Shared  central-config/application-<profile>.properties (overlay)
Shared  central-config/application.properties           (shared base)
```

**Self-contained service** (Config, Discovery)

```
application-<profile>.properties   (overlay)
application.properties             (base)
```

Each service's *Property Sources* table (under **Reference**) lists its full order.

---

## Architecture

### System Architecture

**Startup**: Config Service distributes configuration to the three business services before they register with the Discovery server.

```
    ┌───────────────────────────────────────────────────────────┐
    │                  Config Service  :8888                    │
    │               (native: central-config/)                   │
    └──────────┬──────────────────┬──────────────────┬──────────┘
               │ config           │ config           │ config
               ▼                  ▼                  ▼
    ┌──────────┴───────┐    ┌─────┴─────┐      ┌─────┴─────┐
    │  Authentication  │    │   Tasks   │      │   Users   │
    │      Service     │    │  Service  │      │  Service  │
    └──────────┬───────┘    └─────┬─────┘      └─────┬─────┘
               │                  │                  │
               └──────────────────┴──────────────────┘
                                  │ register
                                  ▼
                     ┌──────────────────────────┐
                     │     Discovery  :8761     │
                     └──────────────────────────┘
```

**Runtime**: Client authenticates and then calls the business services directly using a Bearer JWT.

```
    ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
    │                                              Client                                         │
    └───────────────────────┬────────────────────────────────────────────┬────────────────────────┘
                            │ 1. credentials                             │ 2. Bearer JWT
                            ▼                               ┌────────────┴───────────┐
                 ┌────────────────────┐                     ▼                        ▼
                 │  Authentication    │           ┌────────────────────┐   ┌────────────────────┐
                 │   Service  :8080   │           │      Users         │   │      Tasks         │
                 └──────────┬─────────┘           │   Service  :8082   │◄─►│   Service  :8081   │
                            │                     └────────┬───────────┘   └────────┬───────────┘
    authenticationdb ◄──────┴──────► Redis                 │                        │
                                                           ▼                        ▼
                                                        usersdb                  tasksdb

    ┌────────────────────┐    ┌─────────────────┐
    │  Prometheus :9090  │───►│  Grafana :3000  │ 
    └────────────────────┘    └─────────────────┘
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

### Security Model

**JWT Authentication Flow**:

1. Client authenticates with credentials → `POST /api/auth/token`
2. Receives access token (5 min) + refresh token (1 hour)
3. Tokens stored in Redis with TTL for revocation checks
4. Client includes `Authorization: Bearer <accessToken>` in requests
5. Services validate JWT signature and check Redis for revocation
6. When access token expires, use refresh token → `POST /api/auth/refresh`

**Token Structure**:

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

### Data Stores

**PostgreSQL** — one dedicated database per business service:

- `authenticationdb` — user credentials and active JWT sessions (Authentication service)
- `tasksdb` — task records (Tasks service)
- `usersdb` — user profiles (Users service)

**Redis** — shared across Authentication, Tasks, and Users services for JWT revocation checks (TTL-based key expiry)

**Migrations**: All PostgreSQL schemas managed by Liquibase

### Project Structure

```
asapp/
├── central-config/                          # Centralized configuration (Spring Cloud Config native backend)
├── libs/                                    # Shared libraries
│   ├── asapp-commons-url/                   # API endpoint constants
│   └── asapp-http-clients/                  # Declarative HTTP client contracts
├── services/                                # Microservices
│   ├── asapp-authentication-service/        # JWT & credentials
│   ├── asapp-config-service/                # Centralized configuration server
│   ├── asapp-discovery-service/             # Service registry (Eureka)
│   ├── asapp-tasks-service/                 # Task management
│   └── asapp-users-service/                 # User profiles
├── tools/                                   # Monitoring & load-testing tools
│   ├── grafana/                             # Grafana dashboards
│   ├── jmeter/                              # JMeter load tests (regression + stress)
│   └── prometheus/                          # Prometheus config
├── git/hooks/                               # Git hooks (pre-commit, commit-msg)
├── docker-compose.yaml                      # Docker services configuration
├── pom.xml                                  # Parent POM
└── CLAUDE.md                                # AI assistant guidance
```

---

## Technology Stack

### Framework

- **Spring Boot**: 4.0.5
- **Spring Cloud**: 2025.1.1 (Oakwood)
- **Spring Framework**: 7.x

### Key Dependencies

- **Configuration**: Spring Cloud Config 5.x
- **Security**: Spring Security 7.x, Nimbus JOSE+JWT 9.x
- **Data Access**: Spring Data JDBC
- **Migrations**: Liquibase 4.x
- **Mapping**: MapStruct 1.x
- **Validation**: Jakarta Validation
- **Documentation**: SpringDoc OpenAPI 3.x

### Testing

- **Framework**: JUnit 5
- **Assertions**: AssertJ
- **Mocking**: Mockito (BDDMockito style)
- **Coverage**: JaCoCo 1.x
- **Mutation Testing**: PITest 1.x
- **Integration**: TestContainers 2.x (PostgreSQL)
- **E2E Mocking**: MockServer 5.x

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

---

## Development

### Build

```bash
# Build all modules
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

### Generate Documentation

```bash
# Generate all reports (coverage, javadoc, sources, REST docs)
mvn clean verify -Pfull
```

### Load Testing

```bash
# Pre-release go/no-go gate (deterministic full journey, asserts correctness)
./run-regression.sh

# Tunable concurrent load to watch in Grafana (http://localhost:3000)
./run-stress.sh -Jthreads=50 -Jduration=600
```

See [tools/jmeter/README.md](tools/jmeter/README.md) for more details.

---

## Reference

### API Endpoints

All request and response bodies use **camelCase** JSON field names.

Each service provides interactive Swagger UI (available under the `dev` profile only):

| Service            | Swagger UI                                                         |
|--------------------|--------------------------------------------------------------------|
| **Authentication** | http://localhost:8080/asapp-authentication-service/swagger-ui.html |
| **Tasks**          | http://localhost:8081/asapp-tasks-service/swagger-ui.html          |
| **Users**          | http://localhost:8082/asapp-users-service/swagger-ui.html          |

### BootUI Developer Console

The three API services embed the [BootUI](https://github.com/jdubois/boot-ui) developer console (available under the `dev` profile):

| Service            | BootUI Console                                              |
|--------------------|-------------------------------------------------------------|
| **Authentication** | http://localhost:8080/asapp-authentication-service/bootui   |
| **Tasks**          | http://localhost:8081/asapp-tasks-service/bootui            |
| **Users**          | http://localhost:8082/asapp-users-service/bootui            |

### Documentation

Generated per service under `target/` after `mvn clean verify -Pfull`:

| Artifact        | Location                                    |
|-----------------|---------------------------------------------|
| REST API docs   | `target/generated-docs/api-guide.html`      |
| Test coverage   | `target/site/jacoco-aggregate/index.html`   |
| Mutation report | `target/pit-reports/<timestamp>/index.html` |
| Javadoc         | `target/site/apidocs/index.html`            |

### Monitoring

| Tool                        | URL                                                         | Credentials  | Purpose               |
|-----------------------------|-------------------------------------------------------------|--------------|-----------------------|
| **Grafana**                 | http://localhost:3000                                       | admin/secret | Metrics visualization |
| **Prometheus**              | http://localhost:9090                                       | -            | Metrics database      |
| **Config Actuator**         | http://localhost:8898/asapp-config-service/actuator         | -            | Health & metrics      |
| **Authentication Actuator** | http://localhost:8090/asapp-authentication-service/actuator | HTTP Basic   | Health & metrics      |
| **Tasks Actuator**          | http://localhost:8091/asapp-tasks-service/actuator          | HTTP Basic   | Health & metrics      |
| **Users Actuator**          | http://localhost:8092/asapp-users-service/actuator          | HTTP Basic   | Health & metrics      |

**JVM Micrometer Dashboard**: Available in Grafana under "Services" folder

**Metrics Include**:

- JVM memory, GC, threads
- HTTP request rates and response times
- Database connection pool usage
- Custom business metrics

---

## Contributing

### Code Standards

- **Architecture**: Follow Hexagonal Architecture and DDD patterns
- **Testing**: Maintain high test coverage (unit + integration + E2E)
- **Formatting**: Run `mvn spotless:apply` before committing
- **Commits**: Use Conventional Commits format
- **Documentation**: Update OpenAPI docs for API changes

### Git Hooks

Automatically installed on `mvn install`:

- **pre-commit**: Validates code formatting and line endings
- **commit-msg**: Validates commit message format

---

## CI/CD

### Continuous Integration

Builds and tests the project on every push and pull request to `main`.

**File**: `.github/workflows/ci.yml`

**Triggers**:

- Push to `main` branch
- Pull requests to `main`

**Pipeline Steps**:

1. Checkout code
2. Setup JDK (Temurin)
3. Maven dependency caching
4. Build and test (`mvn verify -Pfull`)

**Reports Generated**:

- JaCoCo coverage (unit, integration, aggregate)
- Surefire test results (unit tests)
- Failsafe test results (integration tests)

### Continuous Delivery

Automates the full release cycle in three stages: generating the release, building and publishing artifacts, and optionally polishing the changelog.

#### Release Generation

The release cycle is automated via the `/release` Claude Code command. Run it from the `main` branch with a clean working tree:

```
/release
```

The command handles the full cycle and asks for confirmation before pushing:

1. Validates preconditions (on `main`, clean working tree)
2. Removes `-SNAPSHOT` suffix from all POM versions
3. Adds Liquibase database tags to version changelog files
4. Builds and verifies the project (`mvn clean install`)
5. Commits the release and creates a git tag (`vX.Y.Z`)
6. Bumps to the next development SNAPSHOT version
7. Commits the next development version
8. Pushes commits and tag atomically (`git push --atomic origin main vX.Y.Z`)

#### Release Build

Once the tag is pushed, the release pipeline runs automatically:

**File**: `.github/workflows/release.yml`

**Triggers**:

- Push of a version tag (`v*`)

**Pipeline Steps**:

1. Build and test the project
2. Build and publish Docker images
3. Generate changelog from Conventional Commits
4. Create a GitHub Release

**Resources Published**:

- Docker images → `ghcr.io`
- GitHub Release with changelog and JAR artifacts

#### Changelog Improvement (Optional)

Once the GitHub Release is published, the changelog can be polished via the `/improve-changelog` Claude Code command:

```
/improve-changelog vX.Y.Z
```

The command applies AI editorial judgment to the generated changelog and asks for confirmation before updating the GitHub Release:

- Merges entries that cover the same feature across multiple commits
- Removes low-value entries with no user-facing impact
- Rewrites terse or unclear messages into plain language
- Preserves commit links, section structure, and all breaking change entries

---

## Related Documentation

### For AI Assistants

- [CLAUDE.md](CLAUDE.md) — Guidance for AI tools like Claude Code
- [docs/superpowers/](docs/superpowers/) — Specs and plans for spec-driven development
- [.claude/agents/](.claude/agents/) — Project-tailored Claude Code subagent roster (13 agents across design, implementation, review, documentation phases)
- [.claude/rules/](.claude/rules/) — Path-scoped project rules auto-attached when agents touch matching files
- AI workflow is built on the [superpowers](https://github.com/obra/superpowers) Claude Code plugin

### Service-Specific

- [Config Service](services/asapp-config-service/README.md)
- [Authentication Service](services/asapp-authentication-service/README.md)
- [Users Service](services/asapp-users-service/README.md)
- [Tasks Service](services/asapp-tasks-service/README.md)

### Shared Libraries

- [Commons URL](libs/asapp-commons-url/README.md)
- [HTTP Clients](libs/asapp-http-clients/README.md)

---

## License

ASAPP is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

```
Copyright 2023 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Authors

- **Antonio Trigo** - [@attrigo](https://github.com/attrigo)

---

## Acknowledgments

- Spring Boot team for the excellent framework
- Hexagonal Architecture pattern by Alistair Cockburn
- Domain-Driven Design principles by Eric Evans
- All open-source contributors to the dependencies used in this project
