# Changeset Examples

## Enable UUID Extension

The first changeset in a new database must enable the `uuid-ossp` extension before any table uses UUID columns. Use a `sqlCheck` preCondition.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="20250311_1-1" author="attrigo">
        <preConditions onFail="MARK_RAN" onSqlOutput="TEST">
            <sqlCheck expectedResult="0">
                SELECT COUNT(*)
                FROM pg_available_extensions
                WHERE name = 'uuid-ossp'
                  AND installed_version IS NOT NULL
            </sqlCheck>
        </preConditions>

        <sql>
            CREATE EXTENSION IF NOT EXISTS "uuid-ossp"
        </sql>

        <rollback>
            <sql>DROP EXTENSION IF EXISTS "uuid-ossp"</sql>
        </rollback>

        <comment>Adds support for UUID types to the database</comment>
    </changeSet>

</databaseChangeLog>
```

## DML Changeset (Seed Data)

DML-only changesets add `context="docker"` so they only run in Docker environments. The preConditions omit `onSqlOutput="TEST"`.

```xml
<changeSet id="20250804_1-1" author="attrigo" context="docker">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="0">
            SELECT COUNT(*)
            FROM public.users
            WHERE username = 'sa.prometheus@asapp.com'
        </sqlCheck>
    </preConditions>

    <insert schemaName="public" tableName="users">
        <column name="id" valueComputed="uuid_generate_v4()"/>
        <column name="username" value="sa.prometheus@asapp.com"/>
        <column name="password" value="{bcrypt}$2a$10$..."/>
        <column name="role" value="ADMIN"/>
    </insert>

    <rollback>
        <delete schemaName="public" tableName="users">
            <where>username = 'sa.prometheus@asapp.com'</where>
        </delete>
    </rollback>

    <comment>Add service account user for Prometheus monitoring</comment>
</changeSet>
```

## Foreign Keys and Indexes

Add foreign keys with `<addForeignKeyConstraint>` after `<createTable>`, inside the same changeset. Add indexes with `<createIndex>`.

```xml
<addForeignKeyConstraint baseTableName="jwt_authentications"
                         baseColumnNames="user_id"
                         constraintName="fk_jwt_authentications_users"
                         referencedTableName="users"
                         referencedColumnNames="id"/>

<createIndex tableName="users" indexName="idx_users_username">
    <column name="username"/>
</createIndex>
```

## Full Create Table with Foreign Key

From `20250818_2_create_jwt_authentications_table.xml` -- shows a table with UUID PK, foreign key, and multiple column types:

```xml
<changeSet id="20250818_2-1" author="attrigo">
    <preConditions onFail="MARK_RAN" onSqlOutput="TEST">
        <not>
            <tableExists tableName="jwt_authentications"/>
        </not>
    </preConditions>

    <createTable tableName="jwt_authentications">
        <column name="id" type="uuid" defaultValueComputed="uuid_generate_v4()"
                remarks="The JWT authentication's unique identifier">
            <constraints primaryKey="true" primaryKeyName="pk_jwt_authentication"/>
        </column>
        <column name="user_id" type="uuid"
                remarks="The JWT authentication's user unique identifier">
            <constraints nullable="false"/>
        </column>
        <column name="access_token_token" type="varchar2(2000)"
                remarks="The access token encoded JWT">
            <constraints nullable="false"/>
        </column>
        <column name="access_token_claims" type="jsonb"
                remarks="The access token JWT claims">
            <constraints nullable="false"/>
        </column>
        <column name="access_token_issued" type="timestamp"
                remarks="The access token issued timestamp">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <addForeignKeyConstraint baseTableName="jwt_authentications"
                             baseColumnNames="user_id"
                             constraintName="fk_jwt_authentications_users"
                             referencedTableName="users"
                             referencedColumnNames="id"/>

    <rollback>
        <dropTable tableName="jwt_authentications"/>
    </rollback>

    <comment>Creates the Jwt authentication table and foreign key with user table</comment>
</changeSet>
```
