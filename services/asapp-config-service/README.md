# ASAPP Config Service

> Centralized configuration management for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.1-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

The Config Service is a Spring Cloud Config Server that centralises configuration for all ASAPP microservices. It serves shared and per-service property files
from a native filesystem backend (`central-config/`), enabling runtime configuration refresh without rebuilding or redeploying services.

**Key Responsibilities**:

- 📦 Serve shared configuration (actuator settings, MVC format, Jackson, Redis, JWT secret) to all services
- 🔧 Serve per-service configuration (logging levels, service-specific tuning)
- 🔄 Enable runtime configuration refresh on client services without redeploying
- 🔒 Protect all configuration endpoints with HTTP Basic authentication

---

## Features

- **Centralized configuration**: Serves shared and per-service properties to all microservices from a single source
- **Profile-aware resolution**: Merges property sources by profile, supporting environment-specific overrides without code changes
- **Runtime configuration refresh**: Client services can reload properties without restart via `@RefreshScope` and `@ConfigurationProperties`

---

## Requirements

- **Java**: 25+
- **Maven**: 3.9.14+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

---

## Quick Start

### Run Locally (Development Mode)

```bash
# 1. Run the service from the module directory
cd services/asapp-config-service
mvn spring-boot:run

# 2. Verify configuration is served
curl -u user:secret http://localhost:8888/asapp-config-service/asapp-tasks-service/default
```

### Run with Docker

```bash
# 1. Build Docker image
mvn spring-boot:build-image

# 2. Start the full stack
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-config-service

# 4. Stop and clean
docker-compose down -v
```

### Profiles

The service is **secure-by-default**: with no environment profile, full Actuator tooling is off and Actuator exposes only `health`, `info`, and `prometheus`. The `dev` profile re-enables it.

- `mvn spring-boot:run` activates `dev` automatically (config-service also keeps `native`, so it runs `native,dev`).
- The Docker stack runs `native,docker,dev`.
- For a locked-down deployment set `SPRING_PROFILES_ACTIVE=native,docker,prod`.

### Trigger Runtime Configuration Refresh

After updating a property file in `central-config/`, signal the related client service to reload without redeploying it.

```bash
curl -X POST http://localhost:8090/asapp-authentication-service/actuator/refresh
curl -X POST http://localhost:8091/asapp-tasks-service/actuator/refresh
curl -X POST http://localhost:8092/asapp-users-service/actuator/refresh
```

> **Note**: Only properties consumed by `@RefreshScope` beans or `@ConfigurationProperties` classes are applied at runtime.
> Properties that drive Spring Boot auto-configuration decisions are evaluated once at startup and require a service restart to take effect.

---

## Client Setup

Services consuming configuration from this server declare:

```properties
# application.properties
spring.cloud.config.password=secret
spring.cloud.config.username=user
spring.config.import=configserver:${CONFIG_SERVER_URI:http://localhost:8888/asapp-config-service}
```

The `CONFIG_SERVER_URI` placeholder defaults to `localhost:8888` for local development.

In Docker, `docker-compose.yaml` sets `CONFIG_SERVER_URI=http://asapp-config-service:8888/asapp-config-service` so the correct container hostname is used
without any profile-specific override.

> The config server is a **required** dependency — services will refuse to start if it is unreachable. Start `asapp-config-service` before starting any other
> service.

---

## Architecture

The Config Service is a thin infrastructure service with no business logic. It is built on **Spring Cloud Config Server** (`@EnableConfigServer`) using the *
*native** profile, which reads property files directly from the filesystem (`central-config/`). No git history is required, the directory is part of the main
repository.

### Configuration Files

```
central-config/
├── application.properties                    # Shared by all services (all profiles)
├── application-docker.properties             # Shared by all services (docker profile only)
├── asapp-authentication-service.properties   # Auth service specific
├── asapp-tasks-service.properties            # Tasks service specific
└── asapp-users-service.properties            # Users service specific
```

### Property Resolution

When a client requests its configuration, the server merges property sources in priority order (highest first):

| File                               | Scope                         |
|------------------------------------|-------------------------------|
| `{service}-{profile}.properties`   | per-service, profile-specific |
| `{service}.properties`             | per-service, all profiles     |
| `application-{profile}.properties` | shared, profile-specific      |
| `application.properties`           | shared, all profiles          |

### Security Model

- **All endpoints**: HTTP Basic authentication

---

## Technology Stack

- **Spring Boot**: 4.0.5
- **Configuration**: Spring Cloud Config Server 5.x
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

### Generate Documentation

```bash
# Generate reports
mvn clean verify -Pfull
```

---

## Reference

### Property Sources

| File                            | Source | Scope          |
|---------------------------------|--------|----------------|
| `application-docker.properties` | Local  | docker profile |
| `application.properties`        | Local  | all profiles   |

### Docker Environment Variables

| Variable                 | Description                           | Default                        |
|--------------------------|---------------------------------------|--------------------------------|
| `JAVA_OPTS`              | JVM runtime options                   | (see docker-compose.yaml)      |
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles                | `native,docker,dev`            |
| `SERVER_PORT`            | HTTP server port                      | `8888`                         |
| `MANAGEMENT_PORT`        | Actuator management port              | `8898`                         |
| `SERVICE_USERNAME`       | HTTP Basic username for all endpoints | `user`                         |
| `SERVICE_PASSWORD`       | HTTP Basic password for all endpoints | `secret`                       |
| `THC_PORT`               | Health check port for readiness       | `8888`                         |
| `THC_PATH`               | Health check path for readiness       | `/asapp-config-service/readyz` |

### API Endpoints

**Spring Cloud Config Endpoints**

| Method | Endpoint                              | Description                            | Auth Required |
|--------|---------------------------------------|----------------------------------------|---------------|
| GET    | `/{application}/{profile}`            | Get merged configuration for a service | ✅             |
| GET    | `/{application}/{profile}/{label}`    | Get configuration for a specific label | ✅             |
| GET    | `/{application}-{profile}.properties` | Get raw properties file                | ✅             |

**Management Endpoints**

Actuator endpoints are on port `8898` at `/asapp-config-service/actuator`; use `GET /actuator` to list them.

- All actuator endpoints — HTTP Basic authentication (`user` / `secret`)

Health probes are on the server port (`8888`) at `/asapp-config-service` and are public:

- `/asapp-config-service/readyz`
- `/asapp-config-service/livez`

### Documentation

| Artifact        | Location                                    |
|-----------------|---------------------------------------------|
| Test coverage   | `target/site/jacoco-aggregate/index.html`   |
| Mutation report | `target/pit-reports/<timestamp>/index.html` |
| Javadoc         | `target/site/apidocs/index.html`            |

### Monitoring

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:

- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)

---

## Contributing

This service is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Update `central-config/` property files when adding new shared configuration
- Run `mvn spotless:apply` before committing
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Spring Cloud Config Reference](https://docs.spring.io/spring-cloud-config/reference/)

---

## License

ASAPP Config Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
