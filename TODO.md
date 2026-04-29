# TODO LIST

## Version 0.1.0

* [X] Create Monorepo (maven multi-module)
    * [X] Create services module
    * [X] Create libs module
    * [X] Create parent pom
    * [X] Configure git hooks
    * [X] Configure GitHub workflows
    * [X] Create RELEASE file
    * [X] Create parent README file
* [X] Create tasks service
    * [X] Build project
    * [X] Add Liquibase
    * [X] Add basic CRUD endpoints
    * [X] Add tests
    * [X] Add docker-compose file
    * [X] Add README file
* [X] Create commons DTO library
    * [X] Build project
    * [X] Add README file
* [X] Push to GitHub
* [X] Upgrade maven dependencies to latest version
* [X] Move common DTOs to Java Records
    * [X] Remove lombok dependency
    * [X] Remove lombok references
* [X] Create projects service
* [X] Create commons URL library
    * [X] Build project
    * [X] Add tasks URIs
    * [X] Add project URIs
    * [X] Refactor all endpoints to use commons URI
    * [X] Add README file
* [ ] Create relationship between projects and tasks
    * [X] asapp-tasks-service
        * [X] Add project_id column to task table via Liquibase
        * [X] Add endpoint to get tasks by project id
        * [X] Add tests
        * [X] Add Javadoc
        * [X] Update documentation
    * [X] asapp-rest-clients
        * [X] Create REST client library
            * [X] Add Javadoc
            * [X] Add fallback configurations
            * [X] Add client for tasks
            * [X] Add operation to get tasks by project id
            * [X] Add tests
            * [X] Update documentation
    * [X] asapp-projects-service
        * [X] Update get project by id endpoint to include the tasks
        * [X] Update tests
            * [X] Use MockServer for E2EIT
        * [X] Update Javadoc
        * [X] Update documentation
    * [X] Update parent docker-compose file

---

## Version 0.2.0

* [X] Refactor services to Hexagonal Architecture + DDD
    * [X] Move to Hexagonal packaging
    * [X] Define domain model following DDD
    * [X] Adapt Tests
    * [X] Adapt Javadoc
* [X] Set up Observability Metrics
    * [X] Use Prometheus and Grafana
    * [X] Add a JVM dashboard for each service
* [X] Add Spring Security
    * [X] Create an authentication service
        * [X] Add authenticate endpoint
        * [X] Add refresh endpoint
        * [X] Add revoke endpoint
        * [X] Add basic CRUD endpoints for user (to be able to manage users)
        * [X] Support for multiple authentications by user
        * [X] Add trace logging in all auth operations
        * [X] Subject should validate is a valid email format
        * [X] EncodedToken should validate is a valid JWT format
    * [X] Adapt other services
        * [X] Protect business endpoints
        * [X] Propagate JWT when performing HTTP calls
    * [X] Use Redis to ensure JWT is still valid (source of truth)
    * [X] Protect management endpoints
    * [X] Create a background process (cron) to automatically delete expired JWT
    * [X] Create custom DelegatingPasswordEncoder to support a set of modern and reliable password encoding formats
* [X] Create asapp-users-service
    * [X] Build project
    * [X] Add Liquibase
    * [X] Add basic CRUD endpoints
    * [X] Add management endpoints
    * [X] Add unit and integration tests
    * [X] Add a docker-compose file
    * [X] Add service to prometheus
    * [X] Add documentation as a README file
    * [X] Get user by id endpoint should fetch tasks from asap-tasks-service
* [X] Improvements on asapp-tasks-service
    * [X] Add the end date field to the task
    * [X] Add endpoint to get tasks by user id
    * [X] Remove gets tasks by project id endpoint
* [X] Testing
    * [X] Make PostgresQL TestContainer a singleton instance
    * [X] Create a test data fake factory to generate test data
    * [X] Refactor test factories following Object Mother pattern
        * [X] asapp-tasks-service
        * [X] asapp-authentication-service
        * [X] asapp-users-service
    * [X] Replace Hamcrest assertions by AssertJ assertions
    * [X] Add PiTest
    * [X] Add maven-surefire-junit5-tree-reporter
    * [X] Migrate integration tests from `WebTestClient` to `RestTestClient` (SB4 modern sync alternative based on `RestClient`; `spring-boot-webtestclient` added as a transitional dep during SB4 migration)
* [X] Improve management endpoints (Actuator)
    * [X] Show more health details when authenticated
    * [X] Move management to a separate port
    * [X] Add management probes endpoints (including readyz and livez)
    * [X] Add env, Java, OS and process details to info endpoint
    * [X] Add git details to info endpoint (git-commit-id plugin)
    * [X] Add SBOM endpoint (cyclonedx-maven-plugin)
    * [X] Enable shutdown endpoint
* [X] Improve date / datetime formating
    * [X] Refactor LocalDateTime by Instance
    * [X] Update documentation
* [X] Improve Java formatter
    * [X] Separate license from the package in all java files
    * [X] Not put empty lines before "try {" and after "}"
    * [X] Records
    * [X] Wrap if statement conditions at logical operators (&& and ||) one per line, only when line exceeds 160 chars
* [X] CI/CD
    * [X] Add a Unix (LF) line separator check to Git Hook
    * [X] Add maven profiles to avoid some steps during local builds
    * [X] Improve GitHub actions
        * [X] Build tags
* [X] Technical improvements
    * [X] Replace "/v1" with "/api" in the path of all endpoints
    * [X] Add "<relativePath>..</relativePath>" to libs and services poms
    * [X] Change the debug level of jdbc to info in application-docker.properties
    * [X] Rename database primary keys, from "*_id_pk" to "pk_"
    * [X] Improve data validation via Jakarta Annotations
    * [X] Configure Claude Code
        * [X] Add CLAUDE.md with project documentation
        * [X] Add coding rules (architecture, DDD, testing, REST, Maven, etc.)
        * [X] Add skills (release, commit-msg, improve-changelog)
    * [X] Launch OpenRewrite Spring Boot best practices
    * [X] Upgrade to Spring Boot 4
    * [X] Upgrade all external dependencies
    * [X] Upgrade to Java 25
    * [X] Upgrade maven wrapper
    * [X] Review and fix console warnings
        * Known warning: `javax.annotation.meta.When.MAYBE` (compile) — Spring 7 JSR-305 ref, will self-resolve when Spring drops `@Nullable`
        * Known warning: `sun.misc.Unsafe::objectFieldOffset` (test) — Byte Buddy + Java 25, tracked in [mockito/mockito#3754](https://github.com/mockito/mockito/issues/3754), will self-resolve on next Spring Boot BOM update
        * Known warning: `Unknown keyword meta:enum / deprecated` (build, CycloneDX) — non-standard JSON Schema keywords in CycloneDX schema not recognized by NetworkNT validator, tracked in [CycloneDX/cyclonedx-maven-plugin#564](https://github.com/CycloneDX/cyclonedx-maven-plugin/issues/564), BOM output is unaffected
    * [X] Perform IntelliJ problems analysis
    * [X] Perform security analysis
    * [X] Perform SonarLint analysis
    * [X] Enrich Javadocs
    * [X] Enrich README
    * [X] Clean branches

---

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
* [ ] Add Spring Rest Docs for API documentation
    * [ ] Add Spring REST Docs dependency and AsciiDoctor Maven plugin to each service
    * [ ] Configure `RestDocumentationExtension` in the test setup of each service
    * [ ] Document all REST endpoints (request params, request/response body fields, headers) in controller tests
    * [ ] Write AsciiDoc index templates per service
    * [ ] Configure the Maven build to generate and package HTML documentation from the snippets
    * [ ] Update README files of each service to reference the generated documentation
* [X] Test Improvements
    * [X] Rename `JwtAuthenticationFilterIT` to `SecurityConfigurationIT` — the class covers broader security config (not just the JWT filter)
    * [X] Test Actuator endpoints under management port — verify `/actuator/refresh` requires HTTP Basic auth (401 without credentials), `/actuator/health` returns 200, `/actuator/prometheus` is accessible
    * [X] Test `livez` and `readyz` endpoints are served on the main port and are publicly accessible (no auth required)
    * [X] Create test fixture for `DecodedJwt` to reduce boilerplate in JWT-related unit tests and use it where appropiate
    * [X] Create test in `RestClientConfigurationTests` to validate JWT propagation
    * [X] Add .as to all assertThat (if not present add a guideline to appropriate Claude rule file)
    * [X] Remove `{ }` in lambdas with only one line (if not present add a guideline to appropriate Claude rule file)
    * [X] Replace SoftAssertions.assertSoftly by static imports
* [ ] Technical Improvements
    * [X] Change constructor of configuration classes to package visibility
    * [ ] Refactor `String body` by `var body` in test classes
    * [X] Review WARN log `Unable to start LiveReload server` shown at start up
* [ ] CI/CD
    * [ ] Rename release GitHub Actions workflow name from "Build, publish and release the project" to something release-focused (e.g. "Release")
    * [ ] Refactor release GitHub Actions workflow to split into multiple jobs (build-and-test → publish-docker → create-release) for better UI clarity and failure isolation
    * [ ] Simplify release skill Step 6 from `mvn clean install` to `mvn clean compile` — CI already validates tests pass before release
    * [ ] Upgrade GitHub Actions to Node.js 24 compatible versions (`docker/login-action@v3` deprecated; forced migration by June 2nd, 2026)
    * [ ] Create a dedicated `ci` Maven profile (Spotless check only) and replace `-Pfull` in the CI workflow to avoid building release-only artifacts (Javadoc, sources JARs, coverage) on every push
    * [ ] Automate the upgrade of `asapp-*` service image tags in `docker-compose.yml` as part of the release workflow
* [X] Infrastructure improvements
    * [X] Remove obsolete top-level `version` attribute from `docker-compose.yml` (deprecated and ignored in Compose v2+)
    * [X] Expose management ports (8090, 8091, 8092, 8898) in `docker-compose.yml` to allow local access to actuator endpoints
    * [X] Add healthchecks to PostgreSQL containers (`pg_isready`) and Redis (`redis-cli ping`) in `docker-compose.yml`
    * [X] Add `depends_on` with healthcheck condition for Redis in services that use it (authentication-service for token blacklist)
    * [X] Add JVM tuning options (`-Xmx`, `-XX:+ExitOnOutOfMemoryError`, `-XX:+HeapDumpOnOutOfMemoryError`, `-XX:HeapDumpPath`) via `JAVA_OPTS` env var in `docker-compose.yml`
    * [X] Improve Docker volumes definition and usage
    * [X] Replace `restart: unless-stopped` with `restart: on-failure:3` on business services to cap infinite restart loops
* [ ] AI Code Assistant
    * [ ] Remove manual mode — require `gh` CLI; abort with a clear message if not available
    * [ ] Use `.github/changelog-draft.md` as the working file instead of `/tmp` (cross-platform, no `gh` path resolution issues on Windows)
    * [ ] Delete `.github/changelog-draft.md` automatically after updating the release
* [ ] Doc
    * [ ] Improve READMEs
        * [ ] Clean docs/claude
        * [ ] Format tables to make them human-readable
        * [ ] Clean previous versions in TODO (they are now in changelog in github)?
        * [ ] Remove ### Protected vs Public Endpoints (hard to maintain)_
        * [ ] Rename ## Related Documentation by ## Documentation?
        * [ ] Move ## Project Structure inside ## Architecture?
        * [ ] Move ## Services Overview before ## Requirements? join with ### Microservices?
        * [ ] Move ## Technology Stack just after ## Requirements? or inside ## Architecture?
        * [ ] Move ## Configuration before ## Quick Start (understand how configuration works before we start)?

### Version 0.4.0

* [ ] Replace Liquibase by Flyway
* [ ] Technical improvements
    * [ ] Improve GlobalExceptionHandler to return a sorted Map<Entity, List<FieldsError>>
    * [ ] Create @ConfigurationProperties to manage custom REST and Security properties

### Version 0.5.0

* [ ] Introduce event-driven architecture with asapp-notifications-service (RabbitMQ)
    * [ ] Create asapp-notifications-service
        * [ ] Build project
        * [ ] Add RabbitMQ consumer for user registration events
        * [ ] Configure Dead Letter Queue for failed message handling
        * [ ] Add email sending support (MailHog for local development)
        * [ ] Add tests
        * [ ] Add docker-compose file
        * [ ] Add service to Prometheus
        * [ ] Add JVM Grafana dashboard
        * [ ] Add README file
    * [ ] asapp-authentication-service: publish UserRegisteredEvent to RabbitMQ on user creation
    * [ ] Add RabbitMQ to parent docker-compose file

### Version 0.6.0

* [ ] Change user's username or password must revoke all user authentications
* [ ] Create user must not create a user with the same username (email)
* [ ] Modularize asapp-authentication-service with Spring Modulith
    * [ ] Add Spring Modulith
    * [ ] Add ArchUnit
    * [ ] Add JMolecules
* [ ] Adopt domain events following DDD
    * [ ] Create domain events for user created, updated and deleted
    * [ ] Create domain events for task created, updated and deleted
    * [ ] Use CQRS pattern

### Version 0.7.0

* [ ] Externalize domain events to RabbitMQ for cross-service synchronization
    * [ ] Replace direct RabbitTemplate publishing with Spring Modulith @Externalized events
    * [ ] Synchronize user lifecycle events (created, updated, deleted) across services
    * [ ] Update asapp-notifications-service to consume Modulith integration events

---

## Version 0.8.0

* [ ] Create custom architecture based on Spring starters
    * [ ] Create custom starter for Web
    * [ ] Create custom starter for Security
    * [ ] Create custom starter for Data
    * [ ] Create custom starter for Observability
    * [ ] Create custom starter for Testing

---

## Version x

### asapp-authentication-service

* [ ] Support multiple roles for a user
* [ ] Put in place Spring's CompromisedPassword
* [ ] Change the endpoint that gets all users to return a page of users
* [ ] Migrate passwords with .setEnableUpatePassword(true) to allow users to update their password when they try to authenticate with an old password format (Spring Security 6.2+)


### asapp-tasks-service

* [ ] Change the endpoint that gets all tasks to return a page of tasks
* [ ] Add more business to tasks service
    * [ ] Add more fields to task domain like: creation date, end date, estimation, status, subtasks, labels, user
    * [ ] Add operation to find tasks by list of ids
    * [ ] Add operation to update only certain fields
* [ ] Create and update a task must check if user_id exists in user service
* [ ] Add business operation to generate a complete tasks description from a few words/fields using AI
    * [ ] Use Spring IA

### asapp-users-service

* [ ] Change the endpoint that gets all tasks to return a page of tasks

### asapp-rest-clients

* [ ] Refactor current REST clients to use declarative HTTP clients instead of RestClient
* [ ] Add circuit breaker pattern
* [ ] Add retry pattern

### Tech

* [ ] Refactor `JwtIssuer` and `EncodedTokenFactory` algorithm selection to use primitive type patterns in `switch` once primitive patterns are no longer preview in Java
* [ ] Add AOP/Native support
* [ ] Add Graceful shutdown
* [ ] Make `ExpiredJwtCleanupScheduler` dynamically refreshable via `/actuator/refresh`

### Observability

* [ ] Tracing
    * [ ] Console logs in plain text
    * [ ] File logs in structured JSON format (ECS) — use `logging.structured.format.file=ecs` with a `logging.file.name` pointing to a log file; console stays plain text
    * [ ] Reduce logs verbosity in test executions
    * [ ] Add distributed traces
* [ ] Metrics
    * [ ] Bump version of Grafana dashboard
    * [ ] Add Hikari Grafana dashboard
    * [ ] Add Spring Boot Observability Grafana dashboard
    * [ ] Add Redis Grafana dashboard (requires redis-exporter in docker-compose)
    * [ ] Add RabbitMQ Grafana dashboard

### Security

* [ ] Add OAuth2 authentication
    * [ ] Integrate Spring Authorization Server
* [ ] Use public and private keys to encode the JWT
* [ ] Add OTT authentication
* [ ] Add MFA via OTP using asapp-notifications-service (email or SMS)
* [ ] Add email verification to the register process

### Tests

* [ ] Enable maven-surefire-junit5-tree-reporter once version 2.x.x is released (compatible with maven-surefire-plugin 3.5.4+). Removed during SB4 migration due to incompatibility with surefire 3.5.5 brought in by SB 3.5.13.
* [ ] Add spring-test-profiler
* [ ] Review Spring test context usage
* [ ] Add load test with Gatling

### Git

### CI/CD

* [ ] Publish jar packages and Docker images to GitHub packages
* [ ] Automatize versioning process
* [ ] Generate CHANGELOG.md file automatically from commits
* [ ] Add Pitest to the CI pipeline
* [ ] Improve release skill to support patch version releases (e.g. 0.2.1)

### Infrastructure

### Tools

* [ ] Refactor Maven by Gradle
* [ ] Improve code formatting
    * [ ] Wrap chained method invocations keeping two method calls (a.c().d()) in the same line, only when line exceeds 160 chars (Not Supported by Eclipse formatter 4.35 - 2025-03)
    * [ ] Add code formatter for .xml files
    * [ ] Add code formatter for .yaml files
    * [ ] Add code formatter for .md files
    * [ ] Automatically order annotations following project conventions
    * [ ] Do not wrap elements of chained method invocations in method parameters (myMethod(a.b().c().d()))

### Doc

* [ ] Add Javadoc to mapper implementations requires version 1.6.0 of mapstruct ([ref](https://github.com/mapstruct/mapstruct/pull/3219))

### AI Code Assistant

* [ ] Create Claude code custom command to review code coverage of all modules and generate a report
* [ ] Create Claude code custom command to perform static code analysis and generate a report
