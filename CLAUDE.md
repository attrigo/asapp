# ASAPP

Spring Boot 4.0.5 / Java 25 task management application
Architecture: Hexagonal (Ports & Adapters) + DDD
Stack: Spring MVC, Spring Data JDBC · Spring Security (JWT) · PostgreSQL · Redis · Liquibase · Prometheus (9090) · Grafana (3000)
Services: `asapp-authentication-service` (8080/8090), `asapp-config-service` (8888/8898), `asapp-discovery-service` (8761/8791), `asapp-tasks-service` (8081/8091), `asapp-users-service` (8082/8092)
Libs: `asapp-commons-url` (endpoint URL constants), `asapp-rest-clients` (declarative HTTP client interfaces + DTOs)

## Docs
Guidelines: `.claude/rules/`
Subagents: `.claude/agents/`
Plans and specs: `docs/superpowers/`

## Build
- Build: `mvn clean install`
- Full build (coverage + javadoc + sources + style check): `mvn clean install -Pfull`
- Docker images: `mvn spring-boot:build-image`
- Format (Eclipse config: `asapp_formatter.xml`): `mvn spotless:apply`

## Run
- Service: `cd services/<name> && mvn spring-boot:run`
- Full stack: `docker-compose up -d` (services, PostgreSQL ×3, Redis, Prometheus, Grafana)

## Testing
- Test: `mvn clean verify`
- Mutation testing: `mvn org.pitest:pitest-maven:mutationCoverage`

## Git
- Follows Conventional Commits
- Pre-commit hooks validate format and formatting

## File Operations
- When renaming any file, always use `git mv <old> <new>` — never delete and recreate

## Subagent dispatch
- For every Agent tool call, pick the most specific match from `.claude/agents/`; `general-purpose` is a last resort
