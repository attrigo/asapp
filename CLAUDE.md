# ASAPP

Spring Boot 4.0.5 / Java 25 task management application
Architecture: Hexagonal (Ports & Adapters) + DDD
Stack: Spring MVC, Spring Data JDBC · Spring Security (JWT) · PostgreSQL · Redis · Liquibase · Resilience4j (circuit breaker + retry) · Prometheus (9090) · Grafana (3000)
Services: `asapp-authentication-service` (8080/8090), `asapp-config-service` (8888/8898), `asapp-discovery-service` (8761/8791), `asapp-tasks-service` (8081/8091), `asapp-users-service` (8082/8092)
Libs: `asapp-commons-url` (endpoint URL constants), `asapp-http-clients` (declarative HTTP client interfaces + DTOs)

## Docs
Guidelines: `.claude/rules/`
Subagents: `.claude/agents/`
Skills: `.claude/skills/`
Plans and specs: `docs/superpowers/`

## Build
- Build: `./gradlew build`
- Full build (coverage + javadoc + sources + style check): `./gradlew fullBuild`
- Docker images: `./gradlew bootBuildImage`
- Format (Eclipse config: `asapp_formatter.xml`): `./gradlew spotlessApply`

## Run
- Service: `./gradlew :services:<name>:bootRun`
- Full stack: `docker-compose up -d` (services, PostgreSQL ×3, Redis, Prometheus, Grafana)

## Testing
- Test: `./gradlew build` (or `./gradlew test integrationTest`)
- Mutation testing: `./gradlew pitest`

## Git
- Follows Conventional Commits
- Pre-commit hooks validate format and formatting

## File Operations
- When renaming any file, always use `git mv <old> <new>` — never delete and recreate

## Subagent dispatch
- For every Agent tool call, pick the most specific match from `.claude/agents/`; `general-purpose` is a last resort
