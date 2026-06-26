# ASAPP HTTP Clients

> Shared HTTP client contracts and inter-service communication for ASAPP microservices

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Maven](https://img.shields.io/badge/Maven-3.9.14+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

`asapp-http-clients` provides declarative HTTP client contracts (Spring `@HttpExchange` interfaces) and response DTOs for inter-service communication within the ASAPP microservices
ecosystem.

**Key Features**:

- ✅ Declarative `@HttpExchange` client interfaces
- ✅ Type-safe response models
- ✅ Wiring-free contracts — base URL, auth, load balancing, and resilience are owned by the consuming service

---

## Requirements

- **Java**: 25+
- **Maven**: 3.9.14+

---

## Usage

1. Add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.attrigo.asapp</groupId>
    <artifactId>asapp-http-clients</artifactId>
    <version>${asapp.version}</version>
</dependency>
```

2. Configure the base URL in `application.properties`:

```properties
spring.http.serviceclient.tasks.base-url=http://localhost:8081/asapp-tasks-service
```

In Docker (`application-docker.properties`):

```properties
spring.http.serviceclient.tasks.base-url=http://asapp-tasks-service/asapp-tasks-service
```

3. Register the declarative client in a configuration class and inject it:

```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "tasks", types = TasksHttpClient.class)
public class HttpClientsConfiguration { }

@Component
public class MyAdapter {

    private final TasksHttpClient tasksHttpClient;

    public MyAdapter(TasksHttpClient tasksHttpClient) {
        this.tasksHttpClient = tasksHttpClient;
    }

    public List<TasksByUserIdResponse> myMethod(UUID userId) {
        return tasksHttpClient.getTasksByUserId(userId);
    }
}
```

---

## Development

### Build

```bash
# Build and install
mvn clean install

# Build skipping tests
mvn clean install -DskipTests
```

### Test

```bash
# Run all tests
mvn clean verify
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

### Clients

- `com.attrigo.asapp.http.clients.tasks.TasksHttpClient`

### Documentation

| Artifact      | Location                                  |
|---------------|-------------------------------------------|
| Test coverage | `target/site/jacoco-aggregate/index.html` |
| Javadoc       | `target/site/apidocs/index.html`          |

---

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Add tests for new HTTP clients
- Run `mvn spotless:apply` before committing
- Ensure all tests pass (`mvn verify`)
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Commons URL](../asapp-commons-url/README.md)

---

## External Resources

- [Spring HTTP Interfaces](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)

---

## License

ASAPP HTTP Clients is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
