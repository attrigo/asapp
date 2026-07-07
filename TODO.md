# TODO LIST

---

## 0.4.0 · Establish SDD & AI Agents

Goal: stand up the spec-driven development flow and AI agent tooling for the project.

### Features

- [X] (tasks) Add operation to find tasks by a list of ids
- [X] (users) Add operation to find users by a list of ids
- [X] (api) Join tasks and users `getAll` / `getAllByIds` into one endpoint (avoid Swagger collision)

### Bugfix

- [X] (error-handling) A bad JWT now returns an RFC 7807 ProblemDetail instead of an empty-body 401 from `JwtAuthenticationEntryPoint`

### Technical

- [X] (error-handling) Return validation errors in a deterministic, sorted order
- [X] (error-handling) Preserve the full validation field path so nested / duplicate field names don't collide
- [X] (api) Enforce request / response camelCase globally
- [X] (api) Assert OpenApi exposes only business operations in OpenApiEndpointsIT
- [X] (api) Drop "Rest" from non-REST classes (`*RestAPI` → `*API`, `*RestAPIURL` → `*APIURL`)
- [X] (error-handling) Move remaining inline error strings (titles, details, codes) into constants
- [X] (error-handling) Use only fixed messages for `ProblemDetail.detail` (never exception messages)
- [X] (error-handling) Add a generic fallback exception handler — unhandled exceptions map to a 500 ProblemDetail (logged at ERROR)
- [X] (error-handling) Broaden GlobalExceptionHandlerIT to assert exception → handler + status routing
- [X] (error-handling) Complete the exception contracts on interfaces
- [X] (clients) Refactor REST clients to declarative HTTP clients
- [X] (clients) Add the circuit breaker pattern to the HTTP clients
- [X] (clients) Add the retry pattern to the HTTP clients
- [X] (clients) Add timeouts to the HTTP clients
- [X] (clients) Review user-with-tasks degradation when tasks-service is unavailable
- [X] (clients) Rename library `asapp-rest-clients` → `asapp-http-clients`
- [X] (config) Introduce explicit dev and prod profiles to gate dev-only tooling
- [X] (security) Redact raw JWT tokens from `JwtVerifier` messages and filter logs
- [X] (tests) Add the JMeter regression test
- [X] (tests) Add the JMeter stress test
- [X] (tests) Standardize `extracting()` usage in tests
- [X] (tests) Rewrite E2EIT to assert JSON instead of Java DTOs
- [X] (tests) Refactor MockMvcTester tests to extract the response into `actual` before asserting
- [X] (tests) Rename all `ThrowsRuntimeException_` → `ThrowsException_`
- [X] (architecture) Rename all references `com.bcn` → `com.attrigo`

### Docs & Tooling

- [X] (ai) Set up the SDD flow and tool
- [X] (ai) Set up AI agents
- [X] (ai) Create a custom skill to review a task
- [X] (ai) Create a custom skill to resolve identified issues
- [X] (ai) Create a custom skill to close a task
- [X] (ai) Update the commit-msg skill to include a bulleted body
- [X] (observability) Add the boot-ui admin UI (gated behind dev / prod profiles)
- [X] (ai) Add task progress to both release commands
- [X] (ai) Improve the release skill to add spec files in the version folder
- [X] (ci) Support `fixup!` commits in the commit-msg git hook
- [X] (ci) Make the commit-msg git hook compatible with Linux (WSL)
- [X] (docs) Remove all references to `docs/guidelines/` from the README
- [X] (docs) Add the missing reference to boot-ui in the README
- [X] (docs) Update the Docker env-var docs with Resilience props (sorted)
- [X] (docs) Add a Setup section to the Javadoc of all test classes
- [X] (docs) Add Javadoc to all custom public constructors (recommend factory methods)
- [X] (docs) Add each `@Operation` description as a new `<p>` on the API method
- [X] (docs) Remove implementation-method Javadoc that duplicates the interface
- [X] (docs) Rewrite GEH Javadoc from "Thrown by …" to "Catches … / Handles …"
- [X] (docs) Synchronize the api-guide.adoc files with the OpenApi docs

### Decisions

- **Rejected:** replace Liquibase with Flyway. Flyway is SQL-dialect-first and would couple persistence to PostgreSQL; Liquibase's database-agnostic change
  abstraction is the deciding factor. Follow-up captured under Backlog → Database → Database Migrations.
- **Dropped:** make `RequestValidationError` represent the origin of non-body (path / query) validation errors. Superseded.
- **Dropped:** move commit-msg operations into a dedicated agent that returns the message. Superseded.

---

## 0.5.0 · Tech upgrade

Goal: move the build and platform onto Gradle and Spring Boot 4.1.

### Technical

- [ ] (build) Replace Maven with Gradle
- [ ] (deps) Upgrade Spring Boot to 4.1
    - [ ] Replace JUnit 5 with JUnit 6
    - [ ] Fully migrate to Jackson 3 (`ObjectMapper` → `JsonMapper`)
    - [ ] Review CVEs
- [ ] (deps) Upgrade project dependencies
- [ ] (deps) Upgrade Docker images
- [ ] (ci) Upgrade GitHub Actions
- [ ] (tests) Upgrade the JMeter tool
- [ ] (config) Use `@ConfigurationProperties` for custom application properties
- [ ] (observability) Improve observability
    - [ ] Use spring-boot-starter-opentelemetry
    - [ ] Bump version of Grafana dashboard
    - [ ] Add Hikari Grafana dashboard
    - [ ] Add Spring Boot Observability Grafana dashboard
    - [ ] Add Redis Grafana dashboard
    - [ ] Add RabbitMQ Grafana dashboard
    - [ ] Observe when the circuit breaker opens and closes the circuit

### Docs & Tooling

- [ ] (docs) Replace Swagger with Scalar
- [ ] (ai) Create skills to automate the project's tech upgrades
    - [ ] Skill to upgrade Spring Boot and its dependencies
        - **Note:** check for migration guides
        - **Note:** produce a design file (like superpowers brainstorming)
    - [ ] Skill to upgrade the Java version
    - [ ] Skill to upgrade infrastructure (Gradle wrapper, Docker images, GitHub Actions, …)

---

## 0.6.0 · Introduce Event-Driven Design

Goal: send a confirmation notification on user creation via domain events.

### Features

- [ ] (notifications) Send a confirmation notification on user creation
    - [ ] Publish an event when a user is created
    - [ ] React to user creation by sending a confirmation notification
    - [ ] Keep user creation unaffected by notification failures
    - [ ] Retry failed notification deliveries
- [ ] (api) Paginate list endpoints

---

## 0.7.0 · Setup OAuth2

Goal: adopt OAuth2 authentication and retire the custom JWT.

### Features

- [ ] (security) Introduce OAuth2 authentication
    - [ ] Stand up an OAuth2 authorization server
    - [ ] Support user login and service-to-service token flows
    - [ ] Secure services as OAuth2 resource servers
    - [ ] Retire the custom JWT authentication
    - [ ] Document the OAuth2 authentication flow

### Technical

- [ ] (security) Support automatic password-format migration on authentication
- [ ] (security) Replace Spring `SecurityFilterChain` with Customizers

---

## 0.8.0 · Adopt Modulith with Domain Events & CQRS

Goal: modularize with Spring Modulith and drive the domain through events + CQRS.

### Technical

- [ ] (architecture) Introduce modularization with Spring Modulith (ArchUnit & JMolecules)
    - [ ] Add an ArchUnit test asserting every request DTO `@JsonProperty` value equals the camelCase of its Java component name
        - **Note:** already enforced per service by `JsonNamingConventionTest`; fold it into the Modulith ArchUnit suite
- [ ] (architecture) Adopt domain events following DDD principles
    - [ ] Handle domain CUD operations via events
    - [ ] Use the CQRS pattern
    - [ ] Externalize domain events to RabbitMQ for cross-service synchronization

---

## 0.9.0 · Modularize with custom Starters

Goal: build a custom architecture out of Spring starters.

### Technical

- [ ] (architecture) Create a custom architecture based on Spring starters
    - [ ] Custom starter for Web
    - [ ] Custom starter for Security
    - [ ] Custom starter for Data
    - [ ] Custom starter for Observability
    - [ ] Custom starter for Testing

---

## 0.10.0 · Enforce Cross-Service Data Consistency

Goal: keep user and task data consistent across services.

### Features

- [ ] (tasks) Reject task create / update when the `userId` doesn't exist in the users service
- [ ] (users) Make username (email) unique — reject user creation when one already exists

### Technical

- [ ] (architecture) Replicate user create / update across services
- [ ] (security) Revoke all of a user's authentications when their username or password changes

---

## 0.11.0 · Improve testing

Goal: strengthen the test suite and its Spring context usage.

### Technical

- [ ] (tests) Add `spring-test-profiler`
- [ ] (tests) Review Spring test-context usage

---

## Backlog

### Features

#### auth

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
