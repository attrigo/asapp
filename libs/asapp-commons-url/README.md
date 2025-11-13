# ASAPP Commons URL

> Centralized REST API endpoint constants for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

`asapp-commons-url` is a shared library that provides type-safe, centralized REST API endpoint constants for all ASAPP services. It ensures consistency across services and eliminates hardcoded URL strings.

**Key Features**:
- ✅ Centralized endpoint definitions for Authentication, Users, and Tasks APIs
- ✅ Type-safe constants prevent typos and inconsistencies
- ✅ Shared across all ASAPP microservices
- ✅ Supports both relative paths and full paths
- ✅ Easy to maintain and version

## Installation

### As a Maven Dependency

Add this library to your ASAPP service `pom.xml`:

```xml
<dependency>
    <groupId>com.bcn.asapp</groupId>
    <artifactId>asapp-commons-url</artifactId>
    <version>${asapp.version}</version>
</dependency>
```

### Build from Source

```bash
# Clone the ASAPP repository
git clone https://github.com/attrigo/asapp.git
cd asapp/libs/asapp-commons-url

# Build and install
mvn clean install
```

## Usage

### Authentication API URLs

```java
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.*;

// Use constants instead of hardcoded strings
@PostMapping(AUTH_TOKEN_PATH)
public AuthenticateResponse authenticate(@RequestBody AuthenticateRequest request) {
    // ...
}

// For REST clients, use full paths
var response = restClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .body(request)
                         .retrieve()
                         .body(AuthenticateResponse.class);
```

**Available Constants**:
- `AUTH_ROOT_PATH` = `/api/auth`
- `AUTH_TOKEN_PATH` = `/token`
- `AUTH_TOKEN_FULL_PATH` = `/api/auth/token`
- `AUTH_REFRESH_TOKEN_PATH` = `/refresh`
- `AUTH_REVOKE_PATH` = `/revoke`

### User API URLs

```java
import static com.bcn.asapp.url.users.UserRestAPIURL.*;

// Root path
USERS_ROOT_PATH = "/api/users"

// Endpoints
USERS_CREATE_FULL_PATH = "/api/users"
USERS_GET_BY_ID_FULL_PATH = "/api/users/{id}"
USERS_UPDATE_BY_ID_FULL_PATH = "/api/users/{id}"
USERS_DELETE_BY_ID_FULL_PATH = "/api/users/{id}"
USERS_GET_ALL_FULL_PATH = "/api/users"
```

### Task API URLs

```java
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.*;

// Root path
TASKS_ROOT_PATH = "/api/tasks"

// Endpoints
TASKS_CREATE_FULL_PATH = "/api/tasks"
TASKS_GET_BY_ID_FULL_PATH = "/api/tasks/{id}"
TASKS_GET_BY_USER_ID_FULL_PATH = "/api/tasks/user/{id}"
TASKS_UPDATE_BY_ID_FULL_PATH = "/api/tasks/{id}"
TASKS_DELETE_BY_ID_FULL_PATH = "/api/tasks/{id}"
TASKS_GET_ALL_FULL_PATH = "/api/tasks"
```

## Library Structure

```
src/main/java/com/bcn/asapp/url/
├── authentication/
│   ├── AuthenticationRestAPIURL.java  # Auth endpoints
│   └── UserRestAPIURL.java            # User management (in auth service)
├── users/
│   └── UserRestAPIURL.java            # User profile endpoints
└── tasks/
    └── TaskRestAPIURL.java            # Task endpoints
```

## Benefits

**Consistency**: All services use the same endpoint definitions
**Type Safety**: Constants prevent runtime typos
**Maintainability**: Change URL once, propagates to all services
**Refactoring**: Rename endpoints easily with IDE support
**Documentation**: Central reference for all API paths

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
open target/asapp-commons-url-<version>-javadoc.jar
# Or extract and open: target/site/apidocs/index.html
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean verify
```

## Requirements

- **Java**: 21 or higher
- **Maven**: 3.9.0 or higher
- **Build Tool**: Apache Maven

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:
- Follow Conventional Commits for commit messages
- Run `mvn spotless:apply` before committing
- Ensure all tests pass (`mvn verify`)
- Update this README if adding new endpoint constants

## License

ASAPP Commons URL is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

---

**Part of the [ASAPP Project](../../README.md)** - A Spring Boot microservices application for task management.
