# ASAPP Commons URL

> Centralized endpoint URL constants for the ASAPP microservices ecosystem

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java25)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-blue.svg)](https://gradle.org/)
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
- **Gradle**: 9.6.1 (via wrapper)

---

## Usage

1. Add the dependency to the consuming module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":libs:asapp-commons-url"))
}
```

2. Import the relevant constants class and reference its constants directly:

```java
// In a controller — use relative paths
import static com.attrigo.asapp.url.authentication.AuthenticationApiUrl.*;

@PostMapping(AUTH_TOKEN_PATH)
public MyResponse myEndpoint(@RequestBody MyRequest request) { ... }

// In a declarative HTTP client — use full paths
import static com.attrigo.asapp.url.tasks.TaskApiUrl.*;

@HttpExchange
public interface TasksHttpClient {

    @PostExchange(TASKS_CREATE_FULL_PATH)
    MyResponse createTask(@RequestBody MyRequest request);

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
./gradlew :libs:asapp-commons-url:fullBuild
```

Generate specific report: `./gradlew :libs:asapp-commons-url:<command>`

| Command   | Generates |
|-----------|-----------|
| `javadoc` | Javadoc   |

---

## Reference

### Constants

- `com.attrigo.asapp.url.authentication.AuthenticationApiUrl`
- `com.attrigo.asapp.url.tasks.TaskApiUrl`
- `com.attrigo.asapp.url.users.UserApiUrl`

### Documentation

| Artifact | Location                        |
|----------|---------------------------------|
| Javadoc  | `build/docs/javadoc/index.html` |

---

## Contributing

This library is part of the ASAPP monorepo. See the [main repository](../../README.md) for contribution guidelines.

**Key Guidelines**:

- Run `./gradlew spotlessApply` before committing
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
