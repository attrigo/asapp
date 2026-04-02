---
paths:
  - "**/infrastructure/**/mapper/*.java"
---

# Mapping

## Mapper Interface

- All mappers use `componentModel = "spring"`
- Use dot notation for nested source properties (e.g., `source = "jwtPair.accessToken"`)
- Entity → domain mapping uses `@ObjectFactory`
- Declare `@Mapping(target = "field", ignore = true)` for every domain field — required even when `@ObjectFactory` handles construction
- Enum mappers: add `@ValueMapping(source = ANY_REMAINING, target = THROW_EXCEPTION)` to reject unmapped values at runtime

## Value Object Mappers

- Primitive → VO : abstract method named `toXxx(primitive)` (e.g., `toTitle(String)`, `toUserId(UUID)`)
- VO → primitive: `default` method named after the target type (e.g., `toUUID()`, `toInstant()`)

## ObjectFactory

- Declare as `@Component`, not `@Mapper`
- Construct the domain object via its factory method
