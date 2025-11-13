# ASAPP REST Clients

> Shared REST client infrastructure and inter-service communication for ASAPP microservices

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

`asapp-rest-clients` provides reusable REST client infrastructure and service-specific clients for inter-service communication within the ASAPP microservices ecosystem.

**Key Features**:
- ✅ URI building utilities for dynamic service URLs
- ✅ Pre-configured REST clients for Tasks service
- ✅ Fallback configuration for Spring RestClient
- ✅ Automatic JWT propagation for service-to-service calls
- ✅ Type-safe response models

## Installation

### As a Maven Dependency

Add this library to your ASAPP service `pom.xml`:

```xml
<dependency>
    <groupId>com.bcn.asapp</groupId>
    <artifactId>asapp-rest-clients</artifactId>
    <version>${asapp.version}</version>
</dependency>
```

### Build from Source

```bash
# Clone the ASAPP repository
git clone https://github.com/attrigo/asapp.git
cd asapp/libs/asapp-rest-clients

# Build and install
mvn clean install
```

## Usage

### Tasks REST Client

The `TasksClient` provides a high-level interface for communicating with the Tasks service:

```java
@Service
public class UserService {

    private final TasksClient tasksClient;

    public UserService(TasksClient tasksClient) {
        this.tasksClient = tasksClient;
    }

    public List<UUID> getUserTasks(UUID userId) {
        // Automatically uses configured base URL and propagates JWT
        return tasksClient.getTasksByUserId(userId);
    }
}
```

**Configuration** (`application.properties`):
```properties
asapp.client.tasks.base-url=http://localhost:8081/asapp-tasks-service
```

**Client Methods**:
- `getTasksByUserId(UUID userId)` - Retrieves all tasks for a user

### URI Handler

Build dynamic URIs with base URL and path segments:

```java
@Component
public class MyService {

    private final UriHandler uriHandler;

    public MyService(UriHandler uriHandler) {
        this.uriHandler = uriHandler;
    }

    public void makeRequest() {
        String uri = uriHandler.buildUri("/api", "users", userId.toString());
        // Result: http://localhost:8082/asapp-users-service/api/users/{userId}
    }
}
```

**DefaultUriHandler Configuration**:
```java
@Bean
public UriHandler uriHandler(@Value("${asapp.client.users.base-url}") String baseUrl) {
    return new DefaultUriHandler(baseUrl);
}
```

### Fallback RestClient Configuration

Provides default `RestClient.Builder` bean if services don't define custom configuration:

```java
// Automatically available in Spring context
@Autowired
private RestClient.Builder restClientBuilder;

public void makeHttpCall() {
    var response = restClientBuilder.build()
                                    .get()
                                    .uri("http://example.com/api/data")
                                    .retrieve()
                                    .body(String.class);
}
```

**Can be overridden** in individual services for custom interceptors (e.g., JWT propagation).

## Library Structure

```
src/main/java/com/bcn/asapp/clients/
├── config/
│   └── FallbackRestClientConfiguration.java  # Default RestClient.Builder bean
├── tasks/
│   ├── TasksClient.java                      # High-level Tasks service client
│   ├── TasksRestClient.java                  # Low-level REST implementation
│   ├── TasksClientConfiguration.java         # Client configuration
│   └── response/
│       └── TasksByUserIdResponse.java        # Response models
└── util/
    ├── UriHandler.java                       # URI building interface
    └── DefaultUriHandler.java                # Default implementation
```

## Components

### TasksClient

**Interface**: High-level abstraction for Tasks service communication

**Methods**:
- `List<UUID> getTasksByUserId(UUID userId)` - Get tasks by user ID

**Features**:
- Automatic JWT token propagation
- Base URL configuration via properties
- Type-safe response handling
- Exception handling with graceful degradation

### UriHandler

**Purpose**: Build URIs from base URL and path segments

**Methods**:
- `String buildUri(String... pathSegments)` - Concatenates base URL with segments

**Example**:
```java
uriHandler.buildUri("/api", "tasks", "user", userId)
// → http://localhost:8081/asapp-tasks-service/api/tasks/user/{userId}
```

### FallbackRestClientConfiguration

**Purpose**: Provides default `RestClient.Builder` bean

**Condition**: Only activated if no other `RestClient.Builder` bean exists (`@ConditionalOnMissingBean`)

**Usage**: Allows services to override with custom configuration (interceptors, error handlers)

## Development

### Code Formatting

This library uses [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-maven) with the ASAPP Eclipse formatter:

```bash
# Check code style
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

### Generate Javadoc

```bash
# Generate documentation
mvn clean verify

# View Javadoc
open target/asapp-rest-clients-<version>-javadoc.jar
# Or extract and open: target/site/apidocs/index.html
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean verify
```

## Configuration Properties

Services using this library should configure base URLs in `application.properties`:

```properties
# Tasks service base URL
asapp.client.tasks.base-url=http://localhost:8081/asapp-tasks-service

# For Docker environment (application-docker.properties)
asapp.client.tasks.base-url=http://asapp-tasks-service:8081/asapp-tasks-service
```

## Requirements

- **Java**: 21 or higher
- **Maven**: 3.9.0 or higher
- **Spring Framework**: 6.x (RestClient support)
- **Dependencies**: `asapp-commons-url`

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:
- Follow Conventional Commits for commit messages
- Run `mvn spotless:apply` before committing
- Ensure all tests pass (`mvn verify`)
- Add tests for new REST clients

## Related Documentation

- [Spring RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [ASAPP Commons URL](../asapp-commons-url/README.md) - Endpoint constants
- [ASAPP Architecture](../../docs/claude/architecture.md) - Service communication patterns

## License

ASAPP REST Clients is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
