# TODO LIST

---

## 0.5.0 · Build speed with Gradle

Goal: move the build onto Gradle so every later build is cached, parallel, and incremental.

### Technical

- [ ] (build) Replace Maven with Gradle
    - [ ] Migrate the module structure and dependency management to Gradle
    - [ ] Migrate coverage, mutation testing, and formatting checks to Gradle
    - [ ] Migrate git hook installation to Gradle
    - [ ] Update CI and release workflows to build with Gradle
    - [ ] Migrate Docker image publishing to Gradle
- [ ] (architecture) Add an ArchUnit layering and boundary guardrail
    - [ ] Enforce the infrastructure → application → domain dependency direction
    - [ ] Keep the domain free of framework and infrastructure dependencies
    - [ ] Confine cross-layer access to the declared input and output ports
    - **Note:** a lightweight safety net for the Gradle, OAuth, and Modulith refactors; the full JMolecules suite lands in 0.10

### Docs & Tooling

- [ ] (ai) Establish authoring conventions for Claude rules and agents
  - [X] Define a rule for authoring rule files
  - [ ] Align existing rule files with the new authoring rule
    - **Note:** existing drift to reconcile — `todo.md` has no H1; H1 titles don't track filenames
    - Author an `http-clients.md` rule for the declarative HTTP client model (JWT propagation, redirects-off, Resilience4j); replaces the stale service-to-service guidance dropped from `development-patterns.md` in the N4 fix
  - [X] Define a rule for authoring agent files
  - [ ] Align existing agent files with the new authoring rule
    - Sync `code-reviewer`'s rule-routing list with the actual rule globs — its `rest.md` entry still shows the pre-M1 `*API.java` glob
  - **Note:** mirror the existing skill-authoring rule; keep both rules aligned with the claude-docs-maintainer agent instead of duplicating its checklists

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

#### build

* Add AOP/Native support
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
