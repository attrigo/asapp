# TODO LIST

## Version 0.3.0

* [X] Add Spring Cloud Config for centralized configuration management
    * [X] Create `asapp-config-service` Spring Cloud Config Server module
    * [X] Create a git-backed config repository with per-service property files
    * [X] Add Spring Cloud Config Client dependency to all services
    * [X] Configure each service to fetch configuration from the config server at startup
    * [X] Add config server container to `docker-compose.yml`
    * [X] Update `CLAUDE.md` with the new service details
    * [X] Update README files of each service to reference the config server setup
    * [X] Test Spring Config refresh endpoint is present in actuator endpoints in three business service
    * [X] Test Spring Config endpoints are present in Spring Config service
    * [X] Sort all application properties alphabetically
    * [X] Secure `asapp-config-service` with HTTP Basic authentication
    * [X] Register `asapp-config-service` in Prometheus and Grafana
    * [X] Add `asapp-config-service` the release workflow
    * [X] Use readyz/livez in business service to ensure config service is up
* [X] Security Improvements
    * [X] Secure `/actuator/refresh` endpoints on business services (tasks, users, authentication)
    * [X] Review CSRF and CORS configuration
    * [X] Encode credentials for Actuator endpoints with Bcrypt
* [X] Add Spring Service discovery
    * [X] Create `asapp-discovery-service` Eureka Server module
    * [X] Add Eureka Client dependency to all services
    * [X] Configure service self-registration (service name, instance metadata) in each service
    * [X] Update `asapp-rest-clients` to resolve service URLs via Eureka instead of hardcoded hosts
    * [X] Add discovery server container to `docker-compose.yml`
    * [X] Update `CLAUDE.md` with the new service details
    * [X] Add HTTP Basic Authentication to eureka endpoints
    * [X] Review startup warning "Spring Cloud LoadBalancer is currently working with the default cache. While this cache implementation is useful for development and tests, it's recommended to use Caffeine cache in production.You can switch to using Caffeine cache, by adding it and org.springframework.cache.caffeine.CaffeineCacheManager to the classpath."
    * [X] Update README files of each service to reference the discovery setup
* [X] Add Spring Rest Docs for API documentation
    * [X] Add Spring REST Docs dependency and AsciiDoctor Maven plugin to each service
    * [X] Configure `RestDocumentationExtension` in the test setup of each service
    * [X] Document all REST endpoints (request params, request/response body fields, headers) in controller tests
    * [X] Write AsciiDoc index templates per service
    * [X] Configure the Maven build to generate and package HTML documentation from the snippets
    * [X] Update README files of each service to reference the generated documentation
* [X] Test Improvements
    * [X] Rename `JwtAuthenticationFilterIT` to `SecurityConfigurationIT` — the class covers broader security config (not just the JWT filter)
    * [X] Test Actuator endpoints under management port — verify `/actuator/refresh` requires HTTP Basic auth (401 without credentials), `/actuator/health` returns 200, `/actuator/prometheus` is accessible
    * [X] Test `livez` and `readyz` endpoints are served on the main port and are publicly accessible (no auth required)
    * [X] Create test fixture for `DecodedJwt` to reduce boilerplate in JWT-related unit tests and use it where appropiate
    * [X] Create test in `RestClientConfigurationTests` to validate JWT propagation
    * [X] Add .as to all assertThat (if not present add a guideline to appropriate Claude rule file)
    * [X] Remove `{ }` in lambdas with only one line (if not present add a guideline to appropriate Claude rule file)
    * [X] Replace SoftAssertions.assertSoftly by static imports
* [X] Technical Improvements
    * [X] Change constructor of configuration classes to package visibility
    * [X] Refactor `String body` by `var body` in test classes
    * [X] Review WARN log `Unable to start LiveReload server` shown at start up
    * [X] Add `proxyBeanMethods = false` to all `@Configuration` classes; refactor inter-`@Bean` method calls to parameter injection
* [X] CI/CD
    * [X] Rename release GitHub Actions workflow name from "Build, publish and release the project" to something release-focused (e.g. "Release")
    * [X] Refactor release GitHub Actions workflow to split into multiple jobs (build-and-test → publish-docker → create-release) for better UI clarity and failure isolation
    * [X] Simplify release command Step 6 from `mvn clean install` to `mvn clean compile` — CI already validates tests pass before release
    * [X] Upgrade GitHub Actions to Node.js 24 compatible versions (`docker/login-action@v3` deprecated; forced migration by June 2nd, 2026)
    * [X] Create a dedicated `ci` Maven profile (Spotless check only) and replace `-Pfull` in the CI workflow to avoid building release-only artifacts (Javadoc, sources JARs, coverage) on every push
    * [X] Automate the upgrade of `asapp-*` service image tags in `docker-compose.yml` as part of the release workflow
* [X] Infrastructure improvements
    * [X] Remove obsolete top-level `version` attribute from `docker-compose.yml` (deprecated and ignored in Compose v2+)
    * [X] Expose management ports (8090, 8091, 8092, 8898) in `docker-compose.yml` to allow local access to actuator endpoints
    * [X] Add healthchecks to PostgreSQL containers (`pg_isready`) and Redis (`redis-cli ping`) in `docker-compose.yml`
    * [X] Add `depends_on` with healthcheck condition for Redis in services that use it (authentication-service for token blacklist)
    * [X] Add JVM tuning options (`-Xmx`, `-XX:+ExitOnOutOfMemoryError`, `-XX:+HeapDumpOnOutOfMemoryError`, `-XX:HeapDumpPath`) via `JAVA_OPTS` env var in `docker-compose.yml`
    * [X] Improve Docker volumes definition and usage
    * [X] Replace `restart: unless-stopped` with `restart: on-failure:3` on business services to cap infinite restart loops
    * [X] Reorder env vars in docker-compose
* [X] AI Code Assistant
    * [X] Remove manual mode — require `gh` CLI; abort with a clear message if not available
    * [X] Use `.github/changelog-draft.md` as the working file instead of `/tmp` (cross-platform, no `gh` path resolution issues on Windows)
    * [X] Delete `.github/changelog-draft.md` automatically after updating the release
* [X] Doc
    * [X] Improve READMEs

---

## Version 0.4.0

### Goal

* [ ] Put in place SDD
* [ ] Setup AI agents
* [ ] Add load test with JMeter
* [ ] Replace REST clients by declarative HTTP clients
    * Use circuit breaker pattern
    * Use retry pattern

### Quick Wins

* Technical Improvements
    * [ ] Improve error responses to return sorted, structured validation errors
    * [ ] Extract custom REST and security settings into dedicated configuration properties
* CI/CD
    * [ ] Update commit-sg skill to include a bulleted body in generated commit messages
    * [ ] Limit CI workflow triggers to relevant source changes
* Tools
    * [ ] Support fixup! commits in the commit-msg git hook

### Analyze

* [ ] Replace Liquibase by Flyway

---

## Version 0.5.0

### Goal

* Send a confirmation notification on user creation

### Quick Wins

* Support automatic password format migration on authentication
* Paginate list endpoints
* Support patch version releases in the release skill

---

## Version 0.6.0

### Goal

* Introduce OAuth2 authentication

### Analyze

* Choose between Spring Authorization Server and Keycloak

---

## Version 0.7.0

### Goal

* Introduce modularization with Spring Modulith (ArchUnit & JMolecules)
* Adopt domain events following DDD principles
  * Handle domain CUD operations via events
  * Use CQRS pattern
  * Externalize domain events to RabbitMQ for cross-service synchronization

---

## Version 0.8.0

### Goal

* Create or update a user in one service must replicate in other service
* Create and update a task must check if user_id exists in user service
* Change user's username or password must revoke all user authentications
* Make User's username unique, user creation must not create the user if there is another one with the same username (email)

---

## Version 0.9.0

### Goal

* Create custom architecture based on Spring starters
  * Create custom starter for Web
  * Create custom starter for Security
  * Create custom starter for Data
  * Create custom starter for Observability
  * Create custom starter for Testing

---

## Version 0.10.0

### Goal

* Add spring-test-profiler
* Review Spring test context usage

---

## Backlog

### asapp-authentication-service

* Support multiple roles for a user
* Reject compromised passwords

### asapp-tasks-service

* Enrich task domain (dates, status, estimation, labels, subtasks, assignee)
* Add operation to find tasks by list of ids
* Support partial task updates
* Generate full task descriptions from minimal input using AI

### asapp-users-service

*

### Tech

* Refactor JWT algorithm selection to use primitive type patterns in switch (pending stable Java support)
* Add AOP/Native support
* Add Graceful shutdown
* Support dynamic refresh of the expired JWT cleanup scheduler

### Observability

* Tracing
    * Console logs in plain text
    * File logs in structured JSON format (ECS)
    * Reduce logs verbosity in test executions
    * Add distributed traces
* Metrics
    * Bump version of Grafana dashboard
    * Add Hikari Grafana dashboard
    * Add Spring Boot Observability Grafana dashboard
    * Add Redis Grafana dashboard
    * Add RabbitMQ Grafana dashboard

### Security

* Use public and private keys to sign the JWT
* Add OTT authentication
* Add MFA via OTP

### Tests

* Re-enable JUnit 5 tree reporter once a compatible version is released
* Add load test with Gatling

### CI/CD

* Add Pitest to the CI pipeline

### Infrastructure

*

### Tools

* Migrate from Maven to Gradle
* Improve code formatting
    * Configure wrapping rules for chained method invocations (pending formatter support)
    * Add code formatter for .xml files
    * Add code formatter for .yaml files
    * Add code formatter for .md files
    * Automatically order annotations following project conventions
    * Preserve inline chained calls inside method parameters

### Doc

* Add Javadoc to mapper implementations (pending MapStruct 1.6.0)

### AI Code Assistant

* Create Claude code custom command to review code coverage of all modules and generate a report
* Create Claude code custom command to perform static code analysis and generate a report
