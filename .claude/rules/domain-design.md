---
paths:
  - "**/domain/**/*.java"
---

# Domain Design

## Aggregate Factories (Two-State Pattern)

Aggregates have two distinct states created by specific factory methods — never use constructors directly.

| Aggregate | Before Persistence | After Persistence |
|---|---|---|
| `User` (auth) | `inactiveUser(username, password, role)` | `activeUser(id, username, role)` |
| `JwtAuthentication` | `unAuthenticated(userId, jwtPair)` | `authenticated(id, userId, jwtPair)` |
| `User` (users) | `create(firstName, lastName, email, phone)` | `reconstitute(id, firstName, ...)` |
| `Task` | `create(userId, title, ...)` | `reconstitute(id, userId, title, ...)` |

**Rules**:
- Before-persistence state: no `id` field
- After-persistence state: `id` required
- `equals()` delegates to `id` when present, to natural key (e.g., `username`) when absent

## Value Object Pattern

```java
public record Username(String username) {
    public Username {                           // compact constructor = validation
        validateNotBlank(username);
        validateEmailPattern(username);
    }

    public static Username of(String username) { return new Username(username); }
    public String value() { return this.username; }
}
```

**Rules**:
- Implement as Java `record` (immutable by default)
- All validation in compact constructor — fail-fast
- Factory method: `of(primitive)` — never call `new Username(...)` from outside the domain
- Accessor: `value()` — never expose the raw record component directly
- No Spring annotations — pure Java only
- Some value objects have multiple factories: `Issued.now()` / `Issued.of(Instant)`, `StartDate.ofNullable(Instant)`

## Validation Strategy

**Rules**:
- Throw `IllegalArgumentException` for all domain validation failures — no custom exception types
- Validate at construction time (compact constructor), not at call sites
- State-based validation: check `id == null` (before persistence) vs `id != null` (after persistence) when invariants differ by state
