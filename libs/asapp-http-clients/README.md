# ASAPP HTTP Clients

> Shared HTTP client contracts and inter-service communication for ASAPP microservices

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-blue.svg)](https://gradle.org/)
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
- **Gradle**: 9.6.1 (via wrapper)

---

## Usage

1. Add the dependency to the consuming module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":libs:asapp-http-clients"))
}
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
# Build the library
./gradlew build

# Compile and package without running any checks
./gradlew assemble
```

### Test

```bash
# Run all tests
./gradlew check
```

### Code Quality

```bash
# Install git hooks (pre-commit, commit-msg)
./gradlew installGitHooks

# Apply formatting
./gradlew spotlessApply
```

### Generate Documentation

```bash
# Generate all reports
./gradlew :libs:asapp-http-clients:fullBuild
```

Generate specific report: `./gradlew :libs:asapp-http-clients:<command>`

| Command            | Generates     |
|--------------------|---------------|
| `jacocoTestReport` | Test coverage |
| `javadoc`          | Javadoc       |

---

## Reference

### Clients

- `com.attrigo.asapp.http.clients.tasks.TasksHttpClient`

### Documentation

| Artifact      | Location                                    |
|---------------|---------------------------------------------|
| Test coverage | `build/reports/jacoco/test/html/index.html` |
| Javadoc       | `build/docs/javadoc/index.html`             |

---

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Add tests for new HTTP clients
- Run `./gradlew spotlessApply` before committing
- Ensure all tests pass (`./gradlew check`)
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
