# CLAUDE.md

ASAPP is a Spring Boot 3.4.3 / Java 21 task management application.
Three services: `asapp-authentication-service` (8080), `asapp-users-service` (8082), `asapp-tasks-service` (8081).
Architecture: Hexagonal (Ports & Adapters) + DDD. Guidelines auto-load per file type — see `.claude/rules/`.

## Build
- Build + test: `mvn clean verify`
- Run service: `cd services/<name> && mvn spring-boot:run`
- Docker: `docker-compose up -d`
- Fix formatting: `mvn spotless:apply`

## Testing
- Unit: `*Tests.java` | Integration: `*IT.java` | E2E: `*E2EIT.java`
- Testing rules auto-load for test files. See `.claude/rules/testing-*.md`.

## Git
- Commit format: `<type>(<scope>): <description>`
- Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `build`, `perf`, `revert`, `style`
