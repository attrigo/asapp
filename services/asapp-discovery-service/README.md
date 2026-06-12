# ASAPP Discovery Service

> Service registration and discovery for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.1-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

The Discovery Service is a Spring Cloud Netflix Eureka Server that acts as a service registry for all ASAPP microservices. It enables client-side service
discovery, allowing services to register themselves and look up other services by name rather than by hardcoded host and port.

**Key Responsibilities**:

- 📋 Maintain a live registry of all running service instances
- 🔍 Serve service location information to client services on request
- 🌐 Expose the Eureka dashboard for visualizing registered services
- 🔒 Protect all endpoints with HTTP Basic authentication

---

## Features

- **Service registry**: Maintains a live registry of all running microservice instances
- **Client-side discovery**: Services register by name and resolve each other without hardcoded addresses
- **Eureka dashboard**: Web UI for visualizing all registered services and their status
- **Self-preservation disabled**: Stale registrations are removed immediately, keeping the registry accurate during development

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
cd services/asapp-discovery-service
mvn spring-boot:run

# 2. Open the Eureka dashboard
open http://localhost:8761/asapp-discovery-service

# 3. Verify the service registry via API
curl -u user:secret \
  -H "Accept: application/json" \
  http://localhost:8761/asapp-discovery-service/eureka/apps
```

### Run with Docker

```bash
# 1. Build Docker image
mvn spring-boot:build-image

# 2. Start the full stack
docker-compose up -d

# 3. View logs
docker-compose logs -f asapp-discovery-service

# 4. Stop and clean
docker-compose down -v
```

---

## Configuration & Profiles

The service is **secure-by-default**: with no environment profile, the Actuator exposes only `health`, `info`, `prometheus`, and `sbom`. Activating `dev` re-enables the full tooling.

- **Local** — `mvn spring-boot:run` activates `dev` (wired in the POM).
- **Docker stack** — `docker,dev`.
- **Locked-down deploy** — `SPRING_PROFILES_ACTIVE=docker,prod`.

### Property resolution

discovery-service is self-contained — it reads only its own `src/main/resources/` files (it does not consume the config server). A profile overlay (`application-<profile>`) beats the base and applies only when its profile is active. Highest precedence first:

```
application-docker.properties (docker overlay)
application-dev.properties    (dev overlay)
application.properties        (base)
```

---

## Client Setup

Services that need to register with and discover other services via this server declare:

```properties
# application.properties
eureka.client.service-url.defaultZone=http://user:secret@localhost:8761/asapp-discovery-service/eureka
eureka.instance.health-check-url-path=${server.servlet.context-path}/actuator/health
eureka.instance.home-page-url-path=${server.servlet.context-path}
eureka.instance.instance-id=${spring.application.name}:${server.port}
eureka.instance.prefer-ip-address=true
eureka.instance.status-page-url-path=${server.servlet.context-path}/actuator/info
```

In Docker, `docker-compose.yaml` sets `DISCOVERY_HOST`, `DISCOVERY_USERNAME`, and `DISCOVERY_PASSWORD` environment variables so the correct container hostname
and credentials are used without any profile-specific override:

```properties
# application-docker.properties
eureka.client.service-url.defaultZone=http://${DISCOVERY_USERNAME}:${DISCOVERY_PASSWORD}@${DISCOVERY_HOST}/eureka
```

> The discovery server is an **optional** dependency, services will start and function without it, but service-to-service lookups by name will fail. Start
`asapp-discovery-service` before starting any other service that performs load-balanced calls.

---

## Architecture

The Discovery Service is a thin infrastructure service with no business logic. It is built on **Spring Cloud Netflix Eureka Server** and acts solely as a
registry.

Self-preservation mode is disabled (`enable-self-preservation=false`) to avoid retaining stale registrations during development and testing.

### Security Model

- **All endpoints**: HTTP Basic authentication

---

## Technology Stack

- **Spring Boot**: 4.0.5
- **Service Discovery**: Spring Cloud Netflix Eureka Server 5.x
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

Listed highest-precedence first; `application-<profile>` rows apply only when that profile is active.

| File                            | Source | Scope          |
|---------------------------------|--------|----------------|
| `application-docker.properties` | Local  | docker profile |
| `application-dev.properties`    | Local  | dev profile    |
| `application.properties`        | Local  | all profiles   |

### Docker Environment Variables

| Variable                 | Description                                       | Default                                                |
|--------------------------|---------------------------------------------------|--------------------------------------------------------|
| `JAVA_OPTS`              | JVM runtime options                               | (see docker-compose.yaml)                              |
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles                            | `docker,dev`                                           |
| `SERVER_PORT`            | HTTP server port                                  | `8761`                                                 |
| `MANAGEMENT_PORT`        | Actuator management port                          | `8791`                                                 |
| `DISCOVERY_HOST`         | Eureka server host used for self-registration URL | `asapp-discovery-service:8761/asapp-discovery-service` |
| `DISCOVERY_USERNAME`     | Username used in the Eureka `defaultZone` URL     | `user`                                                 |
| `DISCOVERY_PASSWORD`     | Password used in the Eureka `defaultZone` URL     | `secret`                                               |
| `SERVICE_USERNAME`       | HTTP Basic username for all endpoints             | `user`                                                 |
| `SERVICE_PASSWORD`       | HTTP Basic password for all endpoints             | `secret`                                               |
| `THC_PORT`               | Health check port for readiness                   | `8761`                                                 |
| `THC_PATH`               | Health check path for readiness                   | `/asapp-discovery-service/readyz`                      |

### API Endpoints

**Eureka Server Endpoints**

| Method | Endpoint                            | Description                         | Auth Required |
|--------|-------------------------------------|-------------------------------------|---------------|
| GET    | `/`                                 | Eureka dashboard UI                 | ✅             |
| GET    | `/eureka/apps`                      | Get all registered applications     | ✅             |
| GET    | `/eureka/apps/{appId}`              | Get all instances of an application | ✅             |
| GET    | `/eureka/apps/{appId}/{instanceId}` | Get a specific instance             | ✅             |

**Management Endpoints**

Actuator endpoints are on port `8791` at `/asapp-discovery-service/actuator`; use `GET /actuator` to list them.

- `/actuator/health` — public
- All other actuator endpoints — HTTP Basic authentication (`user` / `secret`)

Health probes are on the server port (`8761`) at `/asapp-discovery-service` and are public:

- `/asapp-discovery-service/readyz`
- `/asapp-discovery-service/livez`

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

- Run `mvn spotless:apply` before committing
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Spring Cloud Netflix Eureka Reference](https://docs.spring.io/spring-cloud-netflix/reference/)

---

## License

ASAPP Discovery Service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
