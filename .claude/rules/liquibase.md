---
paths:
  - "**/src/main/resources/liquibase/**/*.xml"
---

## Structure

Three-level hierarchy:

```
liquibase/db/changelog/
├── db.changelog-master.xml
└── vX.Y.Z/
    ├── vX_Y_Z-changelog.xml   # e.g. v0_2_0-changelog.xml
    └── changesets/
        └── <changeset>.xml
```

## Changeset Rules

- File name: `YYYYMMDD_N_description.xml` (e.g., `20250818_1_create_users_table.xml`)
- Changeset id: `YYYYMMDD_N-1` (e.g., `20250818_1-1`)
- Every DDL/DML changeset must include `<preConditions>`, `<rollback>`, and `<comment>` (the tag changeset is exempt)
- DDL changesets use `<preConditions onFail="MARK_RAN" onSqlOutput="TEST">`
- DML-only changesets use `context="docker"`

## Naming Conventions

- Primary key: `pk_<entity>` (singular, e.g. `pk_task` for table `tasks`)
- Unique constraint: `uc_<tablename>_<columnname>`
- Foreign key: `fk_<basetable>_<referencedtable>`
- Index: `idx_<tablename>_<columnname>`

## Column Conventions

- Every column definition must have a `remarks` attribute
- UUID primary keys: `type="uuid" defaultValueComputed="uuid_generate_v4()"`
