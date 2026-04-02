---
paths:
  - "**/domain/**/*.java"
---

# Domain Design

Domain classes are infrastructure-agnostic — no Spring annotations, no logging.

## Aggregate Factories

- Aggregate roots are always created via static factory methods — never instantiate directly
- Transient factory methods must not accept an ID parameter — the ID is assigned by the persistence layer

## Value Object Pattern

- Factory method is named `of(...)`
- Never instantiate with `new` from outside the domain
- Scalar VOs: single `value()` accessor
- Compound VOs: named accessors or domain helpers instead of `value()`
- Optional domain concepts: use `ofNullable()` factory

## Bounded Context Isolation

- Domain types could be duplicated across services — do not extract shared types

## Validation Strategy

- Default: `IllegalArgumentException` for all domain validation failures
- Use a custom domain exception when the caller needs to distinguish the failure type (e.g., invalid format)
