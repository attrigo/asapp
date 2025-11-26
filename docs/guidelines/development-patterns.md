# Development Workflows

## Quick Reference

**Common Tasks**:
- Add Use Case → Domain, Application (port + service), Infrastructure (controller + DTOs)
- Add Database Table → Liquibase changeset, JPA entity, Repository adapter
- Add Service → Module structure, database, Liquibase, JWT config
- Extract JWT → `SecurityContextHolder.getContext().getAuthentication()`

**Changeset Naming**: `YYYYMMDD_N_description.xml` (N = sequence for that day)

**Release Process**: Remove SNAPSHOT → Tag database → Build → Commit → Tag → Next SNAPSHOT

## Key Patterns

### Adding a Use Case Checklist

1. ✅ Define domain models in `domain/` (aggregate, value objects)
2. ✅ Create use case interface in `application/<domain>/in/` (e.g., `AuthenticateUseCase`)
3. ✅ Define command DTO in `application/<domain>/in/command/` (e.g., `AuthenticateCommand`)
4. ✅ Implement service in `application/<domain>/in/service/` with `@ApplicationService`
5. ✅ Create REST API interface in `infrastructure/<domain>/in/` (e.g., `AuthenticationRestAPI`)
6. ✅ Create REST controller implementing API interface
7. ✅ Add request/response DTOs in `infrastructure/<domain>/in/request|response/`
8. ✅ Create MapStruct mapper in `infrastructure/<domain>/mapper/`
9. ✅ Write unit tests for domain logic
10. ✅ Write integration tests for controller and repository

### Adding a Database Table Checklist

1. ✅ Create changeset: `liquibase/db/changelog/v<version>/changesets/YYYYMMDD_N_description.xml`
2. ✅ Include pre-conditions to check table/column existence
3. ✅ Define rollback instructions
4. ✅ Update `v<version>-changelog.xml` to include new changeset
5. ✅ Test migration: `mvn liquibase:updateSQL`
6. ✅ Create JPA entity in `infrastructure/<domain>/out/entity/`
7. ✅ Create JDBC repository interface extending `CrudRepository`
8. ✅ Create repository adapter implementing output port

### Extracting Current User from JWT
```java
// In controller or service
JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
    .getContext().getAuthentication();

String username = auth.getName();
String role = auth.getRole();
```

### Service-to-Service Calls
```java
// JWT is automatically propagated via JwtInterceptor
// No need to manually add Authorization header
var response = restClient.get()
                         .uri("/api/users/{id}", userId)
                         .retrieve()
                         .body(UserResponse.class);
```

## Details

### Adding a New Service

**Steps**:
1. Create module under `services/` following existing structure
2. Add module to `services/pom.xml`
3. Create database and configure in `docker-compose.yaml`
4. Copy and adapt structure from existing service (domain, application, infrastructure)
5. Update shared libraries if new API endpoints needed (`asapp-commons-url`)
6. Configure Liquibase changelog (`db.changelog-master.xml`)
7. Add Swagger/OpenAPI configuration
8. Configure JWT authentication (copy security package)

### Release Process

**Steps**:
1. Checkout main: `git checkout main`
2. Remove SNAPSHOT: `mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false`
3. Add Liquibase tags to all `v<version>-changelog.xml` files:
   ```xml
   <changeSet id="tag_version_x_x_x" author="attrigo">
       <tagDatabase tag="x.x.x"/>
   </changeSet>
   ```
4. Build: `mvn clean install`
5. Commit: `git commit -m "chore: release version ${RELEASE_VERSION}"`
6. Tag: `git tag ${RELEASE_VERSION}`
7. Next SNAPSHOT: `mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false`
8. Commit: `git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"`
9. Push: `git push --atomic origin main ${RELEASE_VERSION}`

### Liquibase Changeset Pattern

**Location**: `src/main/resources/liquibase/db/changelog/v<version>/changesets/`

**Naming**: `YYYYMMDD_N_description.xml` where N is sequence number for that day

**Pattern**:
```xml
<changeSet id="20250113_1_create_users_table" author="yourname">
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

    <rollback>
        <dropTable tableName="users"/>
    </rollback>
</changeSet>
```

**Key Rules**:
- Always use pre-conditions
- Include rollback instructions
- Use `uuid_generate_v4()` for UUID PKs
- Tag database at version milestones
