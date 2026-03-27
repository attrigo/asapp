---
name: use-case-definition
description: >
  Defines application-layer use cases in the hexagonal architecture: input ports, commands, result records,
  application services, output ports, and application exceptions. Use when adding a use case, application service,
  command, result, output port, or application exception. Not for domain, controllers, or persistence.
---

# Use Case Definition

## Quick Reference

| Artifact              | Location                                    | Naming                          | Type       |
|-----------------------|---------------------------------------------|---------------------------------|------------|
| Input port            | `application/<agg>/in/`                     | `<Verb><Domain>UseCase`         | Interface  |
| Command               | `application/<agg>/in/command/`             | `<Verb><Domain>Command`         | Record     |
| Result                | `application/<agg>/in/result/`              | `<Domain>With<Extra>Result`     | Record     |
| Application service   | `application/<agg>/in/service/`             | `<Verb><Domain>Service`         | Class      |
| Output port           | `application/<agg>/out/`                    | Descriptive, framework-agnostic | Interface  |
| Application exception | `application/<agg>/`                        | `<Domain><Reason>Exception`     | Class      |
| Meta-annotation       | `application/`                              | `ApplicationService`            | Annotation |

## Core Workflow

### 1. Define the Input Port (Use Case Interface)

Create an interface in `application/<aggregate>/in/` named `<Verb><Domain>UseCase`.

```java
package com.bcn.asapp.<service>.application.<aggregate>.in;

public interface CreateTaskUseCase {

    Task createTask(CreateTaskCommand command);

}
```

Rules:
- One use case per interface (Single Responsibility)
- Method accepts a `*Command` record for write operations, or domain value objects / primitives for reads
- Return domain objects (`Task`, `Optional<Task>`, `List<Task>`) or `*Result` records
- Document `@throws` for all checked and expected runtime exceptions
- Read use cases may have multiple query methods in a single interface (see `ReadTaskUseCase`)

### 2. Define the Command Record

Create an immutable record in `application/<aggregate>/in/command/` named `<Verb><Domain>Command`.

```java
package com.bcn.asapp.<service>.application.<aggregate>.in.command;

public record CreateTaskCommand(
        UUID userId,
        String title,
        String description,
        Instant startDate,
        Instant endDate
) {}
```

Rules:
- Use raw Java types (`UUID`, `String`, `Instant`), never domain value objects
- No validation logic -- validation happens in domain value objects during service mapping
- No default values or builder -- records are the DTO boundary between infrastructure and application

### 3. Define the Result Record (When Needed)

Create a record in `application/<aggregate>/in/result/` only when aggregating output from multiple ports.

```java
package com.bcn.asapp.<service>.application.<aggregate>.in.result;

public record UserWithTasksResult(
        User user,
        List<UUID> taskIds
) {

    public UserWithTasksResult {
        validateUserIsNotNull(user);
        validateTaskIdsIsNotNull(taskIds);
    }

    private static void validateUserIsNotNull(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
    }

    private static void validateTaskIdsIsNotNull(List<UUID> taskIds) {
        if (taskIds == null) {
            throw new IllegalArgumentException("Task IDs list must not be null");
        }
    }

}
```

Rules:
- Validate non-null invariants in the compact constructor
- Each validation in its own `private static` method with descriptive name
- Components are domain objects or raw types, never infrastructure types

### 4. Implement the Application Service

Create a class in `application/<aggregate>/in/service/` named `<Verb><Domain>Service`.

```java
package com.bcn.asapp.<service>.application.<aggregate>.in.service;

@ApplicationService
public class CreateTaskService implements CreateTaskUseCase {

    private final TaskRepository taskRepository;

    public CreateTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public Task createTask(CreateTaskCommand command) {
        var task = mapCommandToDomain(command);
        return persistTask(task);
    }

    private Task mapCommandToDomain(CreateTaskCommand command) {
        var userId = UserId.of(command.userId());
        var title = Title.of(command.title());
        var description = Description.ofNullable(command.description());
        var startDate = StartDate.ofNullable(command.startDate());
        var endDate = EndDate.ofNullable(command.endDate());
        return Task.create(userId, title, description, startDate, endDate);
    }

    private Task persistTask(Task task) {
        return taskRepository.save(task);
    }

}
```

Rules:
- Annotate with `@ApplicationService` (never `@Service`)
- Constructor injection only, no field injection
- `@Transactional` when coordinating multiple ports or compensation; omit for read-only or single-port
- Map command fields to domain value objects via `ValueObject.of()` / `ValueObject.ofNullable()`
- Each orchestration step in a dedicated private method
- Logger only for critical multi-step orchestrations (`debug` at entry/exit, `trace` per step)

### 5. Define Output Port Interfaces

Create interfaces in `application/<agg>/out/`. See [references/output-ports.md](references/output-ports.md).

Rules:
- Plain Java interface, no Spring annotations
- Parameters and return types use domain value objects, never raw `UUID`
- Framework-agnostic naming (no `Jdbc`, `Redis`, `Rest` prefixes)
- Port categories: `*Repository` (persistence), `*Gateway` (cross-service), `*Store` (cache), `*Issuer`/`*Authenticator` (infrastructure capabilities)

### 6. Define Application Exceptions

Create exceptions directly in `application/<aggregate>/` (not in a sub-package).

```java
// Base exception
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

}

// Specific subtype
public class AuthenticationNotFoundException extends AuthenticationException {

    public AuthenticationNotFoundException(String message) {
        super(message);
    }

}
```

Rules:
- Always extend `RuntimeException` (or another application exception)
- Constructor takes `String message`; add `(String message, Throwable cause)` only when wrapping
- Hierarchy: base exception per aggregate, subtypes for specific failures
- Only create when the application service needs to catch/throw them

## Common Pitfalls

- **Using `@Service` instead of `@ApplicationService`** -- always use the custom meta-annotation
- **Putting domain value objects in commands** -- commands use raw types; mapping happens in the service
- **Validating in commands** -- validation belongs in domain value objects, not command records
- **Naming output ports with framework terms** -- use `TokenIssuer` not `JwtTokenService`
- **Adding logger to simple services** -- only log multi-step orchestrations
- **Skipping `@Transactional`** -- required when coordinating multiple output ports
- **Creating result records for single-port output** -- return the domain object directly
- **Putting exceptions in sub-packages** -- place them directly in `application/<aggregate>/`