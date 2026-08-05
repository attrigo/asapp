---
paths:
  - "**/*Repository.java"
  - "**/*Entity.java"
---

## Port Interface

- Parameters and return types are domain types — value objects for identity/criteria, aggregate roots for entities; never a raw `UUID`. A purely technical argument (e.g. a cutoff `Instant`) may be raw
- Delete return type follows what the caller observes: `Boolean` for existence, `Integer` for the affected-row count, `void` otherwise

## JDBC Entity

- No domain value objects — fields are raw types or nested `Jdbc*` components
- On the root entity, flatten an embedded value-component field with `@Embedded.Nullable(prefix = "column_prefix_")`

## JDBC Repository

- A delete-by-id backing a `Boolean` port method is a custom `@Modifying @Query` returning the affected row count (`Long`); the adapter maps `> 0` to `Boolean`
