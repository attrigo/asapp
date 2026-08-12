---
paths:
  - "**/testutil/fixture/*.java"
---

Object Mother + Builder test-data factories: when to add one, how it is shaped, how its methods are named.

## When to add

Create a factory when both of these hold, or when a persisted aggregate needs both a domain and a JDBC representation:
- Used in 3+ test files
- Complex construction (3+ parameters or multi-step creation)

Add a semantic default — a Builder modifier bundling several fields behind a domain name (`accessToken()`, `expired()`, `asAdmin()`) — when all of these are met:
- Pattern appears 10+ times across test files
- Represents fixed configuration (not variable data)
- Has clear business meaning

## Structure

- Builder field defaults are shared by every test using the factory; a dynamic one must be safe for all of them (`TestFactoryConstants#generateRandomIssueAt`)
- Factories for persisted aggregates expose both `build()` (domain object) and `buildJdbc()` (JDBC entity)
- Complex factories delegate to simpler factories (e.g., `JwtMother.build()` delegates to `EncodedTokenMother`) rather than duplicating construction logic

## Naming

### Entry methods (static `a<Entity>()` / `an<Entity>Builder()`)

- No verb prefixes (`create`, `make`) — `build`/`buildJdbc` are reserved for the Builder's terminal methods
- Avoid artificial adjectives ("default", "valid", "standard") — use `a<Entity>()` instead
- Use adjective prefixes when multiple representations of the same concept exist to prevent import collisions and maintain call-site clarity (`encodedToken`, `decodedToken`)

### Withers

- Name IDs after the value-object type (`withTaskId`, not `withId`), every other attribute after the domain attribute; never abbreviate
- Never accept entities as parameters — use ID primitives instead; factory constructs value objects in `build()`
