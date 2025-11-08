# Technology Stack Reference

- **Java**: 21
- **Spring Boot**: 3.4.3
- **Database**: PostgreSQL (via Liquibase migrations)
- **Security**: Spring Security with JWT (JJWT library)
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, AssertJ, TestContainers, JSON-Unit
- **Code Quality**: Spotless, JaCoCo, PITest
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Observability**: Prometheus, Grafana

## Key Files

- `pom.xml`: Parent POM with build configuration
- `asapp_formatter.xml`: Eclipse code formatter configuration
- `header-license`: License header template
- `docker-compose.yaml`: Infrastructure services configuration
- `git/hooks/`: Git hook scripts for validation
- `.github/workflows/ci.yml`: CI pipeline configuration
