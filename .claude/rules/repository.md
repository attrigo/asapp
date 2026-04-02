---
paths:
  - "**/*Repository.java"
  - "**/*Entity.java"
---

# Repository

## Port Interface

- Plain Java interface — no Spring annotations
- Parameters and return types use domain value objects only, never raw `UUID` or primitives
- Deletes return `Boolean` (caller checks existence), `void` (unconditional), or `Integer` (bulk)

## JDBC Entity

- Raw types only — no domain value objects
- Embedded components use `@Embedded.Nullable(prefix = "column_prefix_")`

## JDBC Repository

- Extends `ListCrudRepository<JdbcEntity, UUID>` — never `CrudRepository` or `JpaRepository`
- Custom deletes: `@Modifying` + `@Query` returning `Long`

## Repository Adapter

- Unwrap domain VOs via `.value()` before calling the JDBC repository
