---
paths:
  - "**/domain/**/*.java"
---

# Domain Design

## Aggregate Factories

- Aggregate roots are always created via static factory methods — never instantiate directly; constructors are private
- Before persistence, the aggregate has no assigned identifier
- No Spring annotations — domain classes are framework-free

## Aggregate Behavior

- Domain operations live on the aggregate as methods

## Value Object Pattern

- Factory: `of(...)` — never instantiate with `new` from outside the domain
- Scalar VOs: single `value()` accessor — never expose raw record component directly
- Compound VOs: named accessors or domain helpers instead of `value()`
- Optional domain concepts: use `ofNullable()` factory
- No Spring annotations — domain classes are framework-free

## Bounded Context Isolation

- Each service owns its own domain types, these could be intentionally duplicated
- Do not extract shared types across services

## Validation Strategy

- Default: `IllegalArgumentException` for all domain validation failures
- Custom domain exceptions are allowed for specific validation cases (e.g., invalid format)
- Validate at construction time, not at call sites
