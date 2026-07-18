# TODO LIST

---

## 0.5.0 · Build speed with Gradle

Goal: move the build onto Gradle so every later build is cached, parallel, and incremental.

### Technical

- [ ] (build) Replace Maven with Gradle
    - [X] Set up the Gradle project and module structure
    - [X] Migrate dependency management to Gradle
    - [X] Migrate compilation to Gradle
    - [X] Migrate unit testing to Gradle
    - [X] Migrate integration testing to Gradle
    - [X] Migrate coverage reporting to Gradle
    - [ ] Migrate mutation testing to Gradle
        - **Note:** move the PIT plugin dependency off the test compile classpath onto the mutation tool's own configuration
    - [ ] Migrate formatting checks to Gradle
    - [ ] Migrate API documentation generation to Gradle
    - [ ] Migrate javadoc and sources jar generation to Gradle
    - [ ] Migrate packaging to Gradle
        - **Warning:** move Spring Boot devtools off the runtime classpath once the Spring Boot plugin is applied, or it will ship inside the production jar
        - **Note:** when the Spring Boot plugin is applied, confirm its automatic BOM import doesn't duplicate or conflict with the manual Spring Boot BOM import kept for the jackson CVE override
        - **Note:** when the Spring Boot plugin is applied, drop the manual -parameters compiler arg it now auto-adds
        - **Note:** generate both build-info (via the Spring Boot plugin) and git.properties (no Gradle equivalent tracked yet) so the actuator /info endpoint exposes build and git details
        - **Note:** delete the temporary integrationTest filter excluding ActuatorEndpointsIT's /info test (all 5 services) — added pre-packaging to keep the integration tier and its coverage reports green; the test needs build-info and git.properties the Spring Boot plugin generates; confirm ./gradlew check and build go green after removal
    - [ ] Migrate the full build to Gradle
        - **Note:** aggregate build, coverage reports, the formatting check, API docs, and javadoc/sources jars into one lifecycle task — the `mvn install -Pfull` / `mvn clean verify -Pfull` equivalent consumed by the running-locally, CI, release, and build-documentation subtasks
    - [ ] Migrate running the app locally to Gradle
    - [ ] Migrate Docker image building to Gradle
    - [ ] Migrate git hook installation to Gradle
    - [ ] Migrate the CI workflow to Gradle
    - [ ] Migrate the release workflow to Gradle
    - [ ] Migrate build documentation to Gradle
    - [ ] Keep Claude Code files in sync with the migration
        - **Note:** define rule to establish the order of the different build script blocks (tasks, dependencies, etc.)
        - **Note:** clean rule file
        - **Note:** document the integration tier's 1g test heap and its Failsafe-uncapped rationale in the Gradle rules' Testing section
    - [ ] Verify full parity, then remove Maven entirely
        - **Note:** remove the migration-time verbose console setting from gradle.properties
- [ ] (architecture) Add an ArchUnit layering and boundary guardrail
    - [ ] Enforce the infrastructure → application → domain dependency direction
    - [ ] Keep the domain free of framework and infrastructure dependencies
    - [ ] Confine cross-layer access to the declared input and output ports
    - **Note:** a lightweight safety net for the Gradle, OAuth, and Modulith refactors; the full JMolecules suite lands in 0.10

---

## 0.6.0 · Platform & dependency upgrades

Goal: upgrade to Spring Boot 4.1 and capture the upgrade process as reusable tooling.

### Technical

- [ ] (deps) Upgrade Spring Boot to 4.1
    - [ ] Replace JUnit 5 with JUnit 6
    - [ ] Review CVEs
- [ ] (deps) Upgrade remaining dependencies and build plugins
- [ ] (deps) Upgrade Docker images
- [ ] (ci) Upgrade GitHub Actions
- [ ] (tests) Upgrade JMeter
    - **Note:** the pinned engine forces an older Java (17/21) for the stress plan — confirm the new version lifts that constraint
- [ ] (observability) Adopt the Spring Boot OpenTelemetry starter
    - [ ] Export traces and metrics from every service
    - [ ] Add a telemetry collector and trace backend to the stack
    - **Note:** replaces the manual Micrometer tracing and exporter wiring; the shared config is folded into the Observability starter in 0.7

### Docs & Tooling

- [ ] (docs) Replace Swagger with Scalar
- [ ] (ai) Create custom skills to automate the project's tech upgrades
    - [ ] Create a custom skill to upgrade Spring Boot and its dependencies
        - **Note:** check for migration guides
        - **Note:** produce a design file (like superpowers brainstorming)
    - [ ] Create a custom skill to upgrade the Java version
    - [ ] Create a custom skill to upgrade infrastructure (Gradle wrapper, Docker images, GitHub Actions, …)

---

## 0.7.0 · Custom starter architecture

Goal: extract the shared service configuration into a custom Spring starter architecture.

### Technical

- [ ] (config) Consolidate custom application properties into typed configuration classes
- [ ] (architecture) Create a custom architecture based on Spring starters
    - [ ] Extract a Web starter
    - [ ] Extract a Data starter
    - [ ] Extract an Observability starter
        - **Note:** consolidate the OpenTelemetry configuration adopted in 0.6
    - [ ] Extract a Testing starter
    - **Note:** extract from the existing services; the Security starter follows OAuth in 0.8

---

## 0.8.0 · OAuth2

Goal: adopt OAuth2 authentication and retire the custom JWT.

### Features

- [ ] (security) Introduce OAuth2 authentication
    - [ ] Stand up an OAuth2 authorization server
    - [ ] Support user login and service-to-service token flows
    - [ ] Secure services as OAuth2 resource servers
    - [ ] Retire the custom JWT authentication
    - [ ] Document the OAuth2 authentication flow

### Technical

- [ ] (security) Replace Spring `SecurityFilterChain` with Customizers
    - **Note:** do this before OAuth so the security config is clean going in
- [ ] (security) Support automatic password-format migration on authentication
- [ ] (architecture) Extract a Security starter
    - **Note:** completes the starter set from 0.7, now that OAuth has finalized the security configuration

---

## 0.9.0 · Event-driven notifications

Goal: send a confirmation notification on user creation via domain events.

### Features

- [ ] (notifications) Send a confirmation notification on user creation
    - [ ] Publish an event when a user is created
    - [ ] React to user creation by sending a confirmation notification
    - [ ] Keep user creation unaffected by notification failures
    - [ ] Retry failed notification deliveries
- [ ] (api) Paginate list endpoints

---

## 0.10.0 · Modulith with domain events & CQRS

Goal: modularize with Spring Modulith and drive the domain through events and CQRS.

### Technical

- [ ] (architecture) Introduce modularization with Spring Modulith (ArchUnit & JMolecules)
    - [ ] Fold the JSON naming-convention check into the Modulith ArchUnit suite
        - **Note:** already enforced per service by `JsonNamingConventionTest`
- [ ] (architecture) Adopt domain events following DDD principles
    - [ ] Handle domain CUD operations via events
    - [ ] Use the CQRS pattern
    - [ ] Externalize domain events to RabbitMQ for cross-service synchronization

---

## 0.11.0 · Cross-service data consistency

Goal: keep user and task data consistent across services.

### Features

- [ ] (tasks) Reject task create / update when the `userId` doesn't exist in the users service
- [ ] (users) Make username (email) unique — reject user creation when one already exists

### Technical

- [ ] (architecture) Replicate user create / update across services
- [ ] (security) Revoke all of a user's authentications when their username or password changes

---

## 0.12.0 · Observability expansion

Goal: round out observability with operational dashboards and finer-grained instrumentation.

### Technical

- [ ] (observability) Add operational Grafana dashboards
    - [ ] Update the JVM dashboard to the latest revision
    - [ ] Add a Hikari dashboard
    - [ ] Add a Spring Boot Observability dashboard
    - [ ] Add a Redis dashboard
    - [ ] Add a RabbitMQ dashboard
- [ ] (observability) Track circuit breaker open/close transitions
- [ ] (observability) Add domain-specific metrics and traces

---

## Backlog

### Features

#### authentication

* Support multiple roles for a user
* Reject compromised passwords

#### tasks

* Enrich task domain (dates, status, estimation, labels, subtasks, assignee)
* Add task status transitions with business rules (open → in-progress → done, no skipping)
* Support partial task updates
* Generate full task descriptions from minimal input using AI

#### users

* Support partial user updates

### Technical

#### architecture

* Removes NPEs with JSpecify
* Build a BFF with GraphQL

#### security

* Use public and private keys to sign the JWT
* Add OTT authentication
* Add MFA via OTP
* Refactor JWT algorithm selection to use primitive type patterns in switch (pending stable Java support)
* Support dynamic refresh of the expired JWT cleanup scheduler

#### tests

* Add `spring-test-profiler` to measure Spring context loads
* Review Spring test-context usage to cut integration-test time
* Re-enable JUnit 5 tree reporter once a compatible version is released
* Add load test with Gatling
* Add a custom `@WithMockJwt` test annotation (backed by a `WithSecurityContextFactory`) to declaratively seed a `JwtAuthenticationToken`

#### persistence

* Database Migrations
    * Make Liquibase changelogs database-agnostic so persistence stays decoupled from PostgreSQL
        * Scope or replace the `uuid-ossp` extension and `uuid_generate_v4()` defaults (PostgreSQL-specific)
        * Provide `dbms`-scoped variants for the `jsonb` columns (JWT access/refresh token claims)
        * Replace the `pg_available_extensions` precondition checks with portable equivalents

#### observability

* Console logs in plain text
* File logs in structured JSON format (ECS)
* Reduce logs verbosity in test executions

#### ci

* Add Pitest to the CI pipeline
* Limit CI workflow triggers to relevant source changes
* Support patch version releases in the release skill

#### deps

* Remove unused runtime dependencies from the packaged jar/image to reduce artifact size
* Add dependency locking for reproducible builds
* Add dependency verification for supply-chain integrity

#### build

* Add AOP/Native support
* Generate mappers declared in test sources
* Extract a shared version-catalog accessor across convention plugins
* Improve code formatting
    * Configure wrapping rules for chained method invocations (pending formatter support)
    * Add code formatter for .xml files
    * Add code formatter for .yaml files
    * Add code formatter for .md files
    * Automatically order annotations following project conventions
    * Preserve inline chained calls inside method parameters

#### docs

* Add Javadoc to mapper implementations (pending MapStruct 1.6.0)

#### config

* Add Graceful shutdown

#### ai

* Create Claude code custom command to review code coverage of all modules and generate a report
* Create Claude code custom command to perform static code analysis and generate a report
