# ASAPP REST Clients

> Shared REST client infrastructure and inter-service communication for ASAPP microservices

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Maven](https://img.shields.io/badge/Maven-3.9.14+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

`asapp-rest-clients` provides reusable REST client infrastructure and service-specific clients for inter-service communication within the ASAPP microservices
ecosystem.

**Key Features**:

- ✅ URI building utilities for dynamic service URLs
- ✅ Pre-configured REST clients for Tasks service
- ✅ Automatic JWT propagation for service-to-service calls
- ✅ Type-safe response models

---

## Requirements

- **Java**: 25+
- **Maven**: 3.9.14+

---

## Usage

1. Add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.bcn.asapp</groupId>
    <artifactId>asapp-rest-clients</artifactId>
    <version>${asapp.version}</version>
</dependency>
```

2. Configure the base URL in `application.properties`:

```properties
asapp.client.tasks.base-url=http://localhost:8081/asapp-tasks-service
```

In Docker (`application-docker.properties`):

```properties
asapp.client.tasks.base-url=http://asapp-tasks-service:8081/asapp-tasks-service
```

3. Inject the client and call it — JWT propagation and base URL resolution are handled automatically:

```java
@Service
public class MyService {

    private final TasksClient tasksClient;

    public MyService(TasksClient tasksClient) {
        this.tasksClient = tasksClient;
    }

    public void myMethod(UUID userId) {
        List<UUID> taskIds = tasksClient.getTasksByUserId(userId);
        // ...
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

- `com.bcn.asapp.clients.tasks.TasksClient`

### Documentation

| Artifact      | Location                                  |
|---------------|-------------------------------------------|
| Test coverage | `target/site/jacoco-aggregate/index.html` |
| Javadoc       | `target/site/apidocs/index.html`          |

---

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Add tests for new REST clients
- Run `mvn spotless:apply` before committing
- Ensure all tests pass (`mvn verify`)
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [Commons URL](../asapp-commons-url/README.md)

---

## External Resources

- [Spring RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)

---

## License

ASAPP REST Clients is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
