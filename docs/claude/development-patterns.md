# Common Development Patterns

## Adding a New Use Case
1. Define domain models in `domain/` (aggregate, value objects)
2. Create use case interface in `application/<domain>/in/`
3. Define command DTOs in `application/<domain>/in/command/`
4. Implement service in `application/<domain>/in/service/` with `@ApplicationService`
5. Create REST controller in `infrastructure/<domain>/in/`
6. Add request/response DTOs and MapStruct mappers
7. Write unit tests for domain logic
8. Write integration tests for controller and repository

## Adding a New Database Table
1. Create changeset file: `YYYYMMDD_N_description.xml` in `liquibase/db/changelog/v<version>/changesets/`
2. Include pre-conditions to check existence
3. Define rollback instructions
4. Update version changelog to include new changeset
5. Test migration: `mvn liquibase:updateSQL`
6. Create JPA entity in `infrastructure/<domain>/out/entity/`
7. Create repository adapter implementing output port

## Adding a New Service
1. Create module under `services/` following existing structure
2. Add module to `services/pom.xml`
3. Create database and configure in `docker-compose.yaml`
4. Copy and adapt structure from existing service
5. Update shared libraries if new API endpoints are needed
6. Configure Liquibase changelog
7. Add Swagger/OpenAPI configuration
8. Configure JWT authentication if needed

## Working with JWT Tokens
- Extract current user from SecurityContext:
  ```java
  JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
      .getContext().getAuthentication();
  String username = auth.getName();
  String role = auth.getRole();
  ```
- For service-to-service calls, JWT is automatically propagated via `JwtInterceptor`
- No need to manually add Authorization header

## Release Process
Documented in README.md:
1. Checkout main branch
2. Remove SNAPSHOT from pom.xml: `mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false`
3. Add Liquibase database tags to all changelog files
4. Build: `mvn clean install`
5. Commit: `git commit -m "chore: release version ${RELEASE_VERSION}"`
6. Create tag: `git tag ${RELEASE_VERSION}`
7. Prepare next version: `mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false`
8. Commit and push: `git push --atomic origin main ${RELEASE_VERSION}`
