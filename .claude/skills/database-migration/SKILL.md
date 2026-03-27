---
name: database-migration
description: Create Liquibase database migrations with three-level changelog hierarchy, naming conventions, preConditions, rollback, comment, and version tagging. Use when adding table, column, index, constraint, migration, changeset, changelog, schema change, seed data, Liquibase.
---

# Database Migration

## Quick Reference

| Item                  | Convention                                                               |
|-----------------------|--------------------------------------------------------------------------|
| Changelog root        | `liquibase/db/changelog/db.changelog-master.xml`                         |
| Version changelog     | `v<X.Y.Z>/v<X_Y_Z>-changelog.xml`                                       |
| Changeset file        | `v<X.Y.Z>/changesets/YYYYMMDD_N_description.xml`                         |
| Changeset id          | `YYYYMMDD_N-1` (date + sequence + sub-sequence)                          |
| Tag changeset id      | `tag_version_X_Y_Z`                                                      |
| Author                | `attrigo`                                                                |
| Primary key name      | `pk_<tablename>`                                                         |
| Unique constraint     | `uc_<tablename>_<columnname>`                                            |
| Foreign key           | `fk_<basetable>_<referencedtable>`                                       |
| Index name            | `idx_<tablename>_<columnname>`                                           |
| UUID columns          | `type="uuid" defaultValueComputed="uuid_generate_v4()"`                  |
| DDL preConditions     | `onFail="MARK_RAN" onSqlOutput="TEST"`                                   |
| DML seed data         | Add `context="docker"` to changeSet; preConditions use `onFail="MARK_RAN"` only |
| Spring config key     | `spring.liquibase.change-log=liquibase/db/changelog/db.changelog-master.xml` |
| Plugin properties     | `src/main/resources/liquibase/config/mvn-liquibase.properties`           |

## Core Workflow

### 1. Determine Target Service and Version

Identify the service (`asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`) and the current version directory. The base path is:

```
services/<service>/src/main/resources/liquibase/db/changelog/
```

### 2. Three-Level Changelog Hierarchy

**Level 1 -- Master changelog** (`db.changelog-master.xml`): includes version changelogs.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <include file="v0.2.0/v0_2_0-changelog.xml" relativeToChangelogFile="true"/>

</databaseChangeLog>
```

**Level 2 -- Version changelog** (`v<X_Y_Z>-changelog.xml`): uses `<includeAll>` for changesets, ends with a tag.

```xml
<databaseChangeLog ...>

    <includeAll path="changesets/" relativeToChangelogFile="true"/>

    <changeSet id="tag_version_0_2_0" author="attrigo">
        <tagDatabase tag="0.2.0"/>
    </changeSet>

</databaseChangeLog>
```

**Level 3 -- Changeset files** (`YYYYMMDD_N_description.xml`): individual migration operations.

### 3. Create a DDL Changeset (Create Table)

Every changeset MUST include `<preConditions>`, `<rollback>`, and `<comment>`. Every column MUST have a `remarks` attribute.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="20250818_1-1" author="attrigo">
        <preConditions onFail="MARK_RAN" onSqlOutput="TEST">
            <not>
                <tableExists tableName="users"/>
            </not>
        </preConditions>

        <createTable tableName="users">
            <column name="id" type="uuid" defaultValueComputed="uuid_generate_v4()"
                    remarks="The user's unique identifier">
                <constraints primaryKey="true" primaryKeyName="pk_user"/>
            </column>
            <column name="username" type="varchar(100)" remarks="The user's username">
                <constraints nullable="false" unique="true"
                             uniqueConstraintName="uc_users_username"/>
            </column>
        </createTable>

        <createIndex tableName="users" indexName="idx_users_username">
            <column name="username"/>
        </createIndex>

        <rollback>
            <dropTable tableName="users"/>
        </rollback>

        <comment>Creates the user table with columns id and username</comment>
    </changeSet>

</databaseChangeLog>
```

### 4. DML Seed Data and UUID Extension

DML changesets use `context="docker"` and omit `onSqlOutput="TEST"` from preConditions. New databases need a `uuid-ossp` extension changeset first. Foreign keys use `<addForeignKeyConstraint>` after `<createTable>` in the same changeset.

See `references/changeset-examples.md` for full examples of seed data, UUID extension, foreign keys, and indexes.

### 5. Wire a New Version

1. Create directory `v<X.Y.Z>/changesets/` under the service changelog path
2. Create `v<X_Y_Z>-changelog.xml` with `<includeAll>` and `<tagDatabase>`
3. Add `<include>` entry in `db.changelog-master.xml`

### 6. Verify Migration

```bash
cd services/<service-name>
mvn liquibase:updateSQL          # preview generated SQL
mvn liquibase:clearCheckSums     # reset checksums after fixes
mvn liquibase:rollback -Dliquibase.rollbackCount=1  # rollback last changeset
```

## Common Pitfalls

- **Missing `<comment>`**: every changeSet requires a `<comment>` element
- **Missing `remarks`**: every `<column>` must have a `remarks` attribute
- **Wrong changeset id format**: use `YYYYMMDD_N-1`, not the file name
- **Forgetting `onSqlOutput="TEST"`**: DDL preConditions need both `onFail="MARK_RAN"` and `onSqlOutput="TEST"`; DML preConditions use only `onFail="MARK_RAN"`
- **Missing UUID extension**: first changeset in a new database must enable `uuid-ossp`
- **Seed data without `context="docker"`**: DML-only changesets must restrict execution
- **Wrong tag id**: tag changeset id is `tag_version_X_Y_Z` (underscores), tag value is `X.Y.Z` (dots)
- **Missing `<rollback>`**: every changeSet must be reversible; DDL uses `<dropTable>`, DML uses `<delete>`
