# ASAPP

Spring Boot 3.4.3 / Java 21 task management application
Architecture: Hexagonal (Ports & Adapters) + DDD
Stack: Spring MVC, Spring Data JDBC · Spring Security (JWT) · PostgreSQL · Redis · Liquibase · Prometheus (9090) · Grafana (3000)
Services: `asapp-authentication-service` (8080/8090), `asapp-tasks-service` (8081/8091), `asapp-users-service` (8082/8092)
Libs: `asapp-commons-url` (endpoint URL constants), `asapp-rest-clients` (service-to-service HTTP)
Guidelines: see `.claude/rules/`

## Build
- Build: `mvn clean install`
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
