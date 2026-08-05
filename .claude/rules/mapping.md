---
paths:
  - "**/src/main/java/**/infrastructure/**/mapper/*.java"
---

## Mapper Interface

- When mapping an entity to an aggregate root, build it with an `@ObjectFactory` — the root's private constructors prevent MapStruct from using constructor mapping
- When an `@ObjectFactory` builds the object, declare `@Mapping(target = "field", ignore = true)` for every domain field — suppresses MapStruct's unmapped-target-property warnings
- Keep `@Mapping` declarative — avoid inline `expression = "java(...)"`; put logic in a `default`/`@Named` helper

## Value Object Mappers

- Primitive → Value Object: abstract method named `toXxx(primitive)` (e.g., `toEncodedToken(String)`, `toIssued(Instant)`)
- Value Object → primitive: `default` method named after the target type (e.g., `toString()`, `toUUID()`, `toInstant()`)

## ObjectFactory

- Declare as `@Component`, not `@Mapper`
