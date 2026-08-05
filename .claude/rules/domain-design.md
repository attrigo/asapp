---
paths:
  - "**/domain/**/*.java"
---

## Infrastructure Independence

- Domain code is only allowed to use the JDK and other domain types — no framework, library, or logging dependencies

## Aggregate Roots

- Private constructors
- Each root exposes a transient creator (no ID) and a reconstitution creator (with ID) — the persistence layer assigns the ID
- Primitive → value object translation lives in a separate `<Aggregate>Factory` (`TaskFactory`), delegating to the root's package-private creators; when the caller already holds value objects, the factories are public on the root
- Identity-based `equals`/`hashCode` — equal by ID
- Transient instances (null ID) are never equal, unless the root keys equality on a natural business key (auth `User` → username)

## Value Object Pattern

- Never instantiate with `new` from outside the domain
- Scalar value objects: `value()` accessor
- Optional value objects: use `ofNullable()` factory

## Bounded Context Isolation

- Do not extract shared domain types across services — duplicate per bounded context

## Validation Strategy

- Default: `IllegalArgumentException` for all domain validation failures
- Use a custom domain exception when the caller needs to distinguish the failure type
- Custom domain exceptions extend `IllegalArgumentException`
