---
paths:
  - "**/liquibase/**/*.xml"
---

# Liquibase

## Structure

Three-level hierarchy — master includes version changelogs; version changelogs use `<includeAll>` for changesets and end with `<tagDatabase>`:

```
liquibase/db/changelog/
├── db.changelog-master.xml
└── v<version>/
    ├── v<version_underscored>-changelog.xml   # e.g. v0_2_0-changelog.xml
    └── changesets/
        └── YYYYMMDD_N_description.xml
```

## Changeset Rules

- File name: `YYYYMMDD_N_description.xml` (e.g., `20250818_1_create_users_table.xml`)
- Changeset id: `YYYYMMDD_N-1` (e.g., `20250818_1-1`)
- Every changeset MUST include `<preConditions>`, `<rollback>`, and `<comment>`
- DDL changesets: `<preConditions onFail="MARK_RAN" onSqlOutput="TEST">`
- DML-only changesets (seed data): add `context="docker"`

## Naming Conventions

- Primary key: `pk_<tablename>`
- Unique constraint: `uc_<tablename>_<columnname>`
- Foreign key: `fk_<basetable>_<referencedtable>`

## Column Conventions

- Every column must have a `remarks` attribute
- UUID primary keys: `type="uuid" defaultValueComputed="uuid_generate_v4()"`

## Version Tagging

End each version changelog with a tag changeset:
- Changeset id: `tag_version_X_Y_Z`
- Body: `<tagDatabase tag="X.Y.Z"/>`
