---
paths:
  - "**/*Mapper.java"
  - "**/*ObjectFactory.java"
---

# Mapping

## Mapper Interface

- `componentModel = "spring"` on all mappers
- Use dot notation for nested source properties: `source = "jwtPair.accessToken"`
- Entity → domain: use `@ObjectFactory`; explicitly declare `@Mapping(target = "field", ignore = true)` for every domain field — MapStruct requires it even when the factory handles construction
- Enum mappers: add `@ValueMapping(source = ANY_REMAINING, target = THROW_EXCEPTION)` to reject unmapped values at runtime

## Value Object Mappers

- Primitive → VO: abstract method named `toXxx(primitive)` (e.g., `toTitle(String)`, `toUserId(UUID)`)
- VO → primitive: `default` method named after the target type (`toString()`, `toUUID()`, `toInstant()`) with null guard: `return vo != null ? vo.value() : null`

## ObjectFactory

- Declare as `@Component`, not `@Mapper`
- Inject the VO mappers needed; construct the domain object via its factory method (`reconstitute()`, `activeUser()`, etc.)
