---
name: domain-modeling
description: >-
  Creates aggregate roots, value objects, domain services, and domain exceptions
  following project DDD patterns. Use when adding a new entity, value object, domain
  type, aggregate, domain service, or domain exception. Not for application services,
  controllers, DTOs, or persistence.
---

# Domain Modeling

## Quick Reference

| Concept            | Class Style  | Constructor   | Factory Method(s)                          | Equality            |
|--------------------|------------- |---------------|--------------------------------------------|---------------------|
| Aggregate root     | `final class`| `private`     | Named statics (state-based pairs)          | ID-based            |
| Scalar value object| `record`     | Compact canon.| `of(rawValue)`                             | Record default      |
| Compound value obj.| `record`     | Compact canon.| `of(...)` with multiple params             | Record default      |
| Domain service     | `interface`  | N/A           | N/A                                        | N/A                 |
| Domain exception   | `class extends RuntimeException` | `(String message)` | N/A            | N/A                 |

## Core Workflow

### 1. Aggregate Root

Aggregate roots are `final` classes with private constructors and dual-state factory methods (transient vs. persistent). Place in `domain/<aggregate>/`.

```java
public final class Task {

    private final TaskId id;

    private UserId userId;

    private Title title;

    // Transient constructor -- id is null
    private Task(UserId userId, Title title) {
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.id = null;
        this.userId = userId;
        this.title = title;
    }

    // Persistent constructor -- id is required
    private Task(TaskId id, UserId userId, Title title) {
        validateIdIsNotNull(id);
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.id = id;
        this.userId = userId;
        this.title = title;
    }

    // Transient factory -- before persistence
    public static Task create(UserId userId, Title title) {
        return new Task(userId, title);
    }

    // Persistent factory -- reconstituting from DB
    public static Task reconstitute(TaskId id, UserId userId, Title title) {
        return new Task(id, userId, title);
    }

    // Domain behavior as instance methods
    public void update(UserId userId, Title title) {
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.userId = userId;
        this.title = title;
    }
}
```

**Factory method naming conventions** -- choose names that describe the domain state:

| Service           | Transient Factory          | Persistent Factory     |
|-------------------|---------------------------|------------------------|
| tasks-service     | `create(...)`             | `reconstitute(...)`    |
| users-service     | `create(...)`             | `reconstitute(...)`    |
| auth-service User | `inactiveUser(...)`       | `activeUser(...)`      |
| auth-service JWT  | `unAuthenticated(...)`    | `authenticated(...)`   |

**Equals/hashCode rules:**
- `equals`: compare by ID only; if either ID is `null`, return `false` (transient instances are never equal)
- `hashCode`: use `Objects.hashCode(this.id)` when ID is present, `System.identityHashCode(this)` when `null`
- Exception: auth `User` uses username equality for inactive (transient) state

### 2. Value Object (Scalar)

Scalar VOs wrap a single primitive/JDK type. Use a Java `record` with compact canonical constructor.

```java
public record Title(
        String title
) {

    public Title {
        validateTitleIsNotBlank(title);
    }

    public static Title of(String title) {
        return new Title(title);
    }

    public String value() {
        return this.title;
    }

    private static void validateTitleIsNotBlank(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }
    }
}
```

**Validation strategy by inner type:**

| Inner Type | Null Check           | Format Check                            | Example VO       |
|------------|---------------------|-----------------------------------------|------------------|
| `UUID`     | `== null`           | None (UUID is self-validating)          | `TaskId`         |
| `String`   | `== null \|\| isBlank()` | Regex via `matches()` if needed    | `Email`, `Title` |
| `Instant`  | `== null`           | None                                    | `Expiration`     |

**Always expose `value()` accessor** -- never rely on the raw record component name externally.

### 3. Value Object (Compound)

Same `record` pattern as scalar VOs but with multiple fields, named accessors (no `value()`), cross-field validation after individual null checks, and domain helper methods.

### 4. Domain Service

An interface in the domain layer abstracting infrastructure concerns (e.g., `PasswordService`). Implementation lives in the infrastructure layer.

### 5. Domain Exception

Extend `RuntimeException` with a single `String message` constructor. Use for specific validation failures where `IllegalArgumentException` is too generic (e.g., `InvalidPasswordException`, `InvalidUsernameException`).

## Validation Method Pattern

Each constraint is a separate `private static` method named `validate<Field>IsNot<Condition>`:

```java
private static void validateIdIsNotNull(TaskId id) {
    if (id == null) {
        throw new IllegalArgumentException("ID must not be null");
    }
}

private static void validateTitleIsNotBlank(String title) {
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title must not be null or empty");
    }
}
```

- Naming: `validate` + field name + `IsNot` + violated condition (`Null`, `Blank`)
- Always throw `IllegalArgumentException` unless a custom domain exception applies
- Message format: `"<Field name> must not be null"` or `"<Field name> must not be null or empty"`

## Common Pitfalls

- **Never use `new` from outside domain classes** -- always use `of(...)` for VOs and named factories for aggregates
- **Never add Spring/framework annotations** to domain classes -- domain is infrastructure-agnostic
- **Never share domain types across services** -- each bounded context owns its types (e.g., `UserId` exists in both tasks-service and auth-service independently)
- **Never skip `value()` on scalar VOs** -- even though `record` exposes `id()`, always add an explicit `value()` method
- **Never use `Objects.requireNonNull`** -- use explicit `private static validate*` methods with `IllegalArgumentException`
- **ID field is always `final`** -- mutable fields are the non-ID attributes only
- **Cross-field validation** belongs in the compact constructor, after individual null checks (see `Jwt` timestamp/type consistency)
