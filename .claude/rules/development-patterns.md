---
paths:
  - "**/application/**/*.java"
  - "**/domain/**/*.java"
  - "**/liquibase/**/*.xml"
---

# Development Patterns

## Adding a Use Case

1. Define aggregate + value objects in `domain/<aggregate>/`
2. Create `<Verb><Domain>UseCase` interface in `application/<aggregate>/in/`
3. Create `<Verb><Domain>Command` record in `application/<aggregate>/in/command/`
4. Implement `<Verb><Domain>Service` with `@ApplicationService` in `application/<aggregate>/in/service/`
5. Create `<Domain>RestAPI` interface in `infrastructure/<aggregate>/in/`
6. Create `<Domain>RestController` implementing the API interface
7. Add request/response DTOs in `infrastructure/<aggregate>/in/request|response/`
8. Create MapStruct mapper in `infrastructure/<aggregate>/mapper/`
9. Write unit tests (`*Tests.java`) for domain logic
10. Write integration tests (`*IT.java`) for controller and repository
11. Add Redis operations if the use case involves token lifecycle (store, validate, revoke)

## Adding a Database Table

1. Create changeset: `liquibase/db/changelog/v<version>/changesets/YYYYMMDD_N_description.xml`
2. Add `preConditions` (check table existence before creating)
3. Add `<rollback>` instructions
4. Include the changeset in `v<version>-changelog.xml`
5. Verify: `mvn liquibase:updateSQL`
6. Create JPA entity in `infrastructure/<aggregate>/out/entity/`
7. Create JDBC repository extending `CrudRepository`
8. Create repository adapter implementing the output port

## Liquibase Changeset Pattern

```xml
<changeSet id="20250113_1_create_users_table" author="attrigo">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="users"/></not>
    </preConditions>

    <createTable tableName="users">
        <column name="id" type="uuid" defaultValueComputed="uuid_generate_v4()">
            <constraints primaryKey="true"/>
        </column>
        <column name="username" type="varchar(255)">
            <constraints nullable="false" unique="true"/>
        </column>
    </createTable>

    <rollback><dropTable tableName="users"/></rollback>
</changeSet>
```

**Rules**:
- Changeset ID: `YYYYMMDD_N_description` (N = sequence for that day)
- Always `preConditions` with `onFail="MARK_RAN"`
- Always `<rollback>`
- UUID primary keys: `defaultValueComputed="uuid_generate_v4()"`

## Common Snippets

**Extract current user from JWT** (token already Redis-validated by `JwtAuthenticationFilter`):
```java
JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
    .getContext().getAuthentication();
String username = auth.getName();
String role     = auth.getRole();
```

**Service-to-service calls** — JWT is auto-propagated via `JwtInterceptor`, no manual header needed:
```java
var response = restClient.get()
                         .uri("/api/users/{id}", userId)
                         .retrieve()
                         .body(UserResponse.class);
```

## Liquibase CLI

```bash
cd services/<service-name>
mvn liquibase:updateSQL                                      # preview migration SQL
mvn liquibase:clearCheckSums                                 # reset checksums
mvn liquibase:rollback -Dliquibase.rollbackCount=1           # rollback last changeset
```
