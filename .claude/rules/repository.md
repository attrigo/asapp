---
paths:
  - "**/*Repository.java"
---

# Repository

## Port Interface (`application/<aggregate>/out/`)

- Plain Java interface — no Spring annotations
- Parameters and return types use domain value objects only, never raw `UUID` or primitives
- Return types: `Optional<Domain>` for single finds, `Collection<Domain>` or `List<Domain>` for multi-finds
- Delete return types: `Boolean` when the caller needs to know if the entity existed, `void` for unconditional deletes, `Integer` for bulk deletes

## JDBC Repository (`infrastructure/<aggregate>/persistence/`)

- Extends `ListCrudRepository<JdbcEntity, UUID>` — never `CrudRepository` or `JpaRepository`
- Parameters use raw `UUID`, not domain value objects
- Custom deletes: `@Modifying` + `@Query("DELETE FROM table WHERE column = :param")` returning `Long`
