# ASAPP Commons URL

> Centralized endpoint URL constants for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Maven](https://img.shields.io/badge/Maven-3.9.14+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Overview

`asapp-commons-url` is a shared library that provides type-safe, centralized endpoint URL constants for all ASAPP services. It ensures consistency across
services and eliminates hardcoded URL strings.

**Key Features**:

- ✅ Centralized endpoint definitions for Authentication, Users, and Tasks APIs
- ✅ Type-safe constants prevent typos and inconsistencies
- ✅ Shared across all ASAPP microservices
- ✅ Supports both relative paths and full paths
- ✅ Easy to maintain and version

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
    <artifactId>asapp-commons-url</artifactId>
    <version>${asapp.version}</version>
</dependency>
```

2. Import the relevant constants class and reference its constants directly:

```java
// In a controller — use relative paths
import static com.attrigo.asapp.url.authentication.AuthenticationApiUrl.*;

@PostMapping(AUTH_TOKEN_PATH)
public MyResponse myEndpoint(@RequestBody MyRequest request) { ... }

// In a REST client — use full paths
import static com.attrigo.asapp.url.tasks.TaskApiUrl.*;

restClient.post()
          .uri(TASKS_CREATE_FULL_PATH)
          .body(request)
          .retrieve()
          .body(MyResponse.class);
```

---

## Development

### Build

```bash
# Build and install
mvn clean install
```

### Code Quality

```bash
# Install git hooks (pre-commit, commit-msg)
mvn git-build-hook:install

# Apply formatting
mvn spotless:apply
```

---

## Reference

### Constants

- `com.attrigo.asapp.url.authentication.AuthenticationApiUrl`
- `com.attrigo.asapp.url.tasks.TaskApiUrl`
- `com.attrigo.asapp.url.users.UserApiUrl`

### Documentation

| Artifact | Location                         |
|----------|----------------------------------|
| Javadoc  | `target/site/apidocs/index.html` |

---

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Run `mvn spotless:apply` before committing
- Use Conventional Commits for commit messages

---

## Related Documentation

- [ASAPP Main Repository](../../README.md)
- [HTTP Clients](../asapp-http-clients/README.md)

---

## License

ASAPP Commons URL is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
