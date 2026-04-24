# ASAPP Config Service

> Centralized configuration management for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.1-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

The Config Service is a Spring Cloud Config Server that centralises configuration for all ASAPP microservices. It serves shared and per-service property files from a native filesystem backend (`central-config/`), enabling runtime configuration refresh without rebuilding or redeploying services.

**Key Responsibilities**:
- 📦 Serve shared configuration (actuator settings, MVC format, Jackson, Redis, JWT secret) to all services
- 🔧 Serve per-service configuration (logging levels, service-specific tuning)
- 🔄 Enable runtime configuration refresh on client services without redeploying
- 🔒 Protect all configuration endpoints with HTTP Basic authentication

## Features

### Configuration Endpoints

Spring Cloud Config Server exposes the following endpoints for clients:

- **Get configuration** — Returns merged property sources for a service and profile
  - `GET /{application}/{profile}`
  - `GET /{application}/{profile}/{label}`

- **Get raw file** — Returns a raw configuration file from the repository
  - `GET /{application}-{profile}.properties`
  - `GET /{label}/{application}-{profile}.properties`

### Observability

- **Health Check** - Service health
  - `GET /actuator/health`

- **Metrics** - Prometheus-formatted application metrics
  - `GET /actuator/prometheus`

## Architecture

The Config Service is a thin infrastructure service with no business logic. It is built on **Spring Cloud Config Server** (`@EnableConfigServer`) using the **native** profile, which reads property files directly from the filesystem (`central-config/`). No git history is required — the directory is part of the main repository.

### Configuration Repository Structure

```
central-config/
├── application.properties                    # Shared by all services (all profiles)
├── application-docker.properties             # Shared by all services (docker profile only)
├── asapp-authentication-service.properties   # Auth service specific
├── asapp-tasks-service.properties            # Tasks service specific
└── asapp-users-service.properties            # Users service specific
```

### What Is Centralized

**`application.properties`**: shared across all services and profiles:
**`application-docker.properties`**: shared docker-only config:
**Per-service files**: service-specific tuning:

### Property Resolution

When a client requests its configuration, the server merges property sources in priority order (highest first):

| File | Scope |
|------|-------|
| `{service}-{profile}.properties` | per-service, profile-specific |
| `{service}.properties` | per-service, all profiles |
| `application-{profile}.properties` | shared, profile-specific |
| `application.properties` | shared, all profiles |

### Security Model

All config server endpoints are protected with **HTTP Basic authentication**. Credentials are configured via `spring.security.user.name` and `spring.security.user.password` (locally) or `CONFIG_USERNAME` / `CONFIG_PASSWORD` environment variables (Docker).

Client services supply credentials through `spring.cloud.config.username` and `spring.cloud.config.password`, which are resolved before the application context starts — they cannot be fetched from the config server itself.

## Requirements

- **Java**: 25+
- **Maven**: 3.9.14+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

## Quick Start

### Run Locally (Development Mode)

```bash
# 1. Run the service from the module directory
cd services/asapp-config-service
mvn spring-boot:run

# 2. Verify configuration is served
curl -u config-user:config-secret http://localhost:8888/asapp-config-service/asapp-tasks-service/default
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

### Trigger Runtime Configuration Refresh

After updating a property file in `central-config/`, signal the related client service to reload without redeploying it.

```bash
curl -X POST http://localhost:8090/asapp-authentication-service/actuator/refresh
curl -X POST http://localhost:8091/asapp-tasks-service/actuator/refresh
curl -X POST http://localhost:8092/asapp-users-service/actuator/refresh
```

## Configuration

### Property Sources

| File | Source | Scope |
|------|--------|-------|
| `application-docker.properties` | Local | docker profile |
| `application.properties` | Local | all profiles |

### Docker Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CONFIG_PASSWORD` | HTTP Basic password | `config-secret` |
| `CONFIG_USERNAME` | HTTP Basic username | `config-user` |
| `MANAGEMENT_PORT` | Actuator management port | `8898` |
| `SERVER_PORT` | HTTP server port | `8888` |

## Development

### Build and Test

```bash
# Build project
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Run unit tests only
mvn test
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

## API Endpoints

### Spring Cloud Config Endpoints (Protected)

| Method | Endpoint | Description | Auth Required |
|--------|---|---|---|
| GET | `/{application}/{profile}` | Get merged configuration for a service | ✅ |
| GET | `/{application}/{profile}/{label}` | Get configuration for a specific label | ✅ |
| GET | `/{application}-{profile}.properties` | Get raw properties file | ✅ |

### Actuator Endpoints (Protected)

| Method | Endpoint | Description | Auth Required |
|--------|---|---|---|
| GET | `/actuator/health` | Health status | ✅ |
| GET | `/actuator/prometheus` | Prometheus metrics | ✅ |
| GET | `/actuator/metrics` | Available metrics list | ✅ |
| GET | `/actuator/info` | Application info | ✅ |

**Actuator Port**: `8898` (separate from application port `8888`)

## Technology Stack

- **Spring Boot**: 4.0.5
- **Configuration**: Spring Cloud Config Server 5.x
- **Observability**: Spring Boot Actuator, Micrometer

## Client Setup

Services consuming configuration from this server declare:

```properties
# application.properties (local dev)
spring.cloud.config.password=config-secret
spring.cloud.config.username=config-user
spring.config.import=configserver:http://localhost:8888/asapp-config-service

# application-docker.properties (Docker)
spring.cloud.config.password=${CONFIG_PASSWORD:config-secret}
spring.cloud.config.username=${CONFIG_USERNAME:config-user}
spring.config.import=configserver:http://asapp-config-service:8888/asapp-config-service
```

The config server is a **required** dependency — services will refuse to start if it is unreachable. Start `asapp-config-service` before starting any other service.

## Monitoring

**Actuator Endpoints**: `http://localhost:8898/asapp-config-service/actuator`

**Prometheus Integration**: Metrics scraped every 15s for monitoring

**Available Metrics**:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (rate, duration, errors)

## Contributing

This service is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:
- Use Conventional Commits (`feat:`, `fix:`, `chore:`, etc.)
- Run `mvn spotless:apply` before committing
- Update `central-config/` property files when adding new shared configuration

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Spring Cloud Config Reference](https://docs.spring.io/spring-cloud-config/reference/)

## License

ASAPP Config Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
