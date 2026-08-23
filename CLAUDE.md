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
- Full build (coverage + javadoc + sources + API docs): `./gradlew fullBuild`
- Docker images: `./gradlew bootBuildImage`
- Format (Eclipse config: `asapp_formatter.xml`): `./gradlew spotlessApply`

## Run
- Service: `./gradlew :services:<name>:bootRun` (from the repo root, never `cd`)
- Full stack: `docker-compose up -d` (services, PostgreSQL ×3, Redis, Prometheus, Grafana)

## Testing
- Test: `./gradlew check`
- Mutation testing: `./gradlew pitest`

## Git
- Follows Conventional Commits
- Pre-commit hooks validate format and formatting
