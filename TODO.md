# TODO LIST

## Version 0.4.0

### Goal - Establish SDD and AI Agents

* [X] Setup SDD flow and tool
* [X] Setup AI agents

### Quick Wins

* Functional Improvements
    * [X] Add operation to find tasks by list of ids
    * [X] Add operation to find users by list of ids
    * [X] Join tasks and users getAll and getAllByIds endpoints into one unique endpoint (avoid swagger collision)
    * [X] Add boot-ui (requires dev/prod profiles) : https://github.com/jdubois/boot-ui
* Technical Improvements
    * Error Handler
        * [X] Return validation errors in a deterministic, sorted order
        * [X] Move all remaining inline error strings (titles, details, codes) into constants
        * [X] Use only fixed messages for `ProblemDetail.detail` (never exception messages)
        * [X] ~~Make `RequestValidationError` correctly represent the origin of validation errors for non-body parameters (path/query)~~
        * [X] Preserve the full field path in validation errors so nested or duplicate field names don't collide
    * JSON Naming
        * [X] Enforce request/response camelCase globally
    * Load Tests via JMeter
        * [X] Add regression test
        * [X] Add stress test
    * Improve HTTP clients
        * [X] Refactor REST clients by declarative HTTP clients
        * [X] Use circuit breaker pattern
        * [X] Use retry pattern
        * [X] Add timeouts to HTTP client
        * [X] Review the user-with-tasks degradation behavior when tasks-service is unavailable
        * [X] Rename library asapp-rest-clients to asapp-http-clients
    * Profiles
        * [X] Introduce explicit dev and prod profiles to gate dev-only tooling and align with docker/native
    * Test
        * [X] Standardize extracting() usage
        * [X] Rewrite E2EIT to assert Json instead of Java DTOs
    * CI/CD
        * [X] Update commit-msg skill to include a bulleted body in generated commit messages
        * [X] ~~Change commit-msg to perform all operations in a dedicated agent and then response with the message~~
    * Tools
        * [X] Support fixup! commits in the commit-msg git hook
        * [X] Make commit-msg git hook compatible with Linux (WSDL)
    * Docs
        * [X] Remove all reference to "docs/guidelines/" from README
        * [X] Add missing reference to Boot-UI in README
        * [X] Add Setup section to Javadocs of all test classes
        * [X] Add Javadoc to all custom public constructors to recommend use factory methods
        * [X] Synchronize api-guide.adoc files with OpenApi docs
        * [X] Update ### Docker Environment Variables with Resilience props in READMEs (sort them)
    * AI Code Assistant
        * [X] Create custom Skill to review a task
        * [X] Create custom Skill to resolve identified issues
        * [X] Create custom Skill to close a task
    * Other
        * [X] Rename all references from com.bcn to com.attrigo
        - [X] Remove "Rest" word in non-involved Rest classes "*RestAPI" classes by "*API" and "*RestAPIURL" by "*APIURL"
        - [X] Test OpenApi properly exposes contract/endpoints in OpenApiEndpointsIT (modify ReturnsStatusOkAndBodyWithApiSpec_OnOpenApiDocs to assert response contains only business operations)
        - [X] Add the value of @Operation description to the Javadoc of each API method as new <p>
        - [X] Remove the Javadoc of implementation methods when their interface says the same (same content)
        - [X] Complete the exception contracts on interfaces
        - [ ] Add task progress to both release Claude commands
        - [ ] Improve release skill to add specs files in version folder

### Analyze

* [X] ~~Replace Liquibase by Flyway~~ — Rejected: Flyway is SQL-dialect-first and would couple persistence to PostgreSQL; Liquibase's database-agnostic change
  abstraction is the deciding factor. Follow-up captured under Database → Database Migrations.

---

## Version 0.5.0

### Goal - Tech upgrade

* [ ] Replace Maven by Gradle
* [ ] Create Skills to automatize the project's tech upgrades
    * [ ] Create Skill to upgrade Spring Boot and dependencies version
        * Check for Migration guides
        * Produce a design file (kind of superpowers brainstorming)
    * [ ] Create Skill to upgrade Java version
    * [ ] Create Skill to upgrade infrastructure version (Gradle version/wrapper, Docker images, GitHub actions, etc)
* [ ] Upgrade Spring Boot version to 4.1
    * Replace JUnit 5 by JUnit 6
    * Fully Migrate to Jackson 3 (ObjectMapper -> JsonMapper)
    * Review CVEs
* [ ] Review and if needed upgrade project dependencies
* [ ] Review and if needed upgrade Docker images
* [ ] Review and if needed upgrade GitHub actons
* [ ] Review and if needed upgrade JMeter tool

### Quick Wins

* [ ] Replace Swagger by Scalar
* [ ] Use ConfigurationProperties to manage custom application properties

---

## Version 0.6.0

### Goal - Introduce Event-Driven Design

* [ ] Send a confirmation notification on user creation
    * [ ] Publish an event when a user is created
    * [ ] React to user creation by sending a confirmation notification
    * [ ] Keep user creation unaffected by notification failures
    * [ ] Retry failed notification deliveries

### Quick Wins

* [ ] Paginate list endpoints

---

## Version 0.7.0

### Goal - Setup OAuth2

* [ ] Introduce OAuth2 authentication
    * [ ] Stand up an OAuth2 authorization server
    * [ ] Support user login and service-to-service token flows
    * [ ] Secure services as OAuth2 resource servers
    * [ ] Retire the custom JWT authentication
    * [ ] Document the OAuth2 authentication flow

### Quick Wins

* [ ] Support automatic password format migration on authentication
* [ ] Replace Spring SecurityFilterChain by Customizers

---

## Version 0.8.0

### Goal - Adopt Modulith with Domain Events & CQRS

* [ ] Introduce modularization with Spring Modulith (ArchUnit & JMolecules)
    * [ ] Add an ArchUnit/test asserting every request DTO `@JsonProperty` value equals the camelCase of its Java component name (already enforced per service
      by `JsonNamingConventionTest`; fold into the Modulith ArchUnit suite)
* [ ] Adopt domain events following DDD principles
    * [ ] Handle domain CUD operations via events
    * [ ] Use CQRS pattern
    * [ ] Externalize domain events to RabbitMQ for cross-service synchronization

---

## Version 0.9.0

### Goal - Modularize with custom Starters

* [ ] Create custom architecture based on Spring starters
    * [ ] Create custom starter for Web
    * [ ] Create custom starter for Security
    * [ ] Create custom starter for Data
    * [ ] Create custom starter for Observability
    * [ ] Create custom starter for Testing

---

## Version 0.10.0

### Goal - Enforce Cross-Service Data Consistency

* [ ] Create or update a user in one service must replicate in other service
* [ ] Create and update a task must check if userId exists in user service
* [ ] Change user's username or password must revoke all user authentications
* [ ] Make User's username unique, user creation must not create the user if there is another one with the same username (email)

---

## Version 0.11.0

### Goal - Improve testing

* [ ] Add spring-test-profiler
* [ ] Review Spring test context usage

---

## Backlog

### Functional Improvements

#### asapp-authentication-service

* Support multiple roles for a user
* Add task status transitions with business rules (open → in-progress → done, no skipping)
* Reject compromised passwords
* Support partial user updates

#### asapp-tasks-service

* Enrich task domain (dates, status, estimation, labels, subtasks, assignee)
* Support partial task updates
* Generate full task descriptions from minimal input using AI

#### asapp-users-service

* Support partial user updates

### Technical Improvements

#### Cleaning

* Removes NPEs with JSpecify
* Remove unused runtime dependencies from the packaged jar/image to reduce artifact size

#### Security

* Use public and private keys to sign the JWT
* Add OTT authentication
* Add MFA via OTP
* Refactor JWT algorithm selection to use primitive type patterns in switch (pending stable Java support)
* Support dynamic refresh of the expired JWT cleanup scheduler

#### Performance

* Add AOP/Native support

#### Tests

* Re-enable JUnit 5 tree reporter once a compatible version is released
* Add load test with Gatling
* Add a custom `@WithMockJwt` test annotation (backed by a `WithSecurityContextFactory`) to declaratively seed a `JwtAuthenticationToken`

#### Database

* Database Migrations
    * Make Liquibase changelogs database-agnostic so persistence stays decoupled from PostgreSQL
        * Scope or replace the `uuid-ossp` extension and `uuid_generate_v4()` defaults (PostgreSQL-specific)
        * Provide `dbms`-scoped variants for the `jsonb` columns (JWT access/refresh token claims)
        * Replace the `pg_available_extensions` precondition checks with portable equivalents

#### Observability

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
* Observe when Circuit Breaker open and closes the circuit
* Add dashboard for DBs (PostgresQL and Redis)

#### CI/CD

* Add Pitest to the CI pipeline
* Limit CI workflow triggers to relevant source changes
* Support patch version releases in the release skill

#### Infrastructure

#### Tools

* Improve code formatting
    * Configure wrapping rules for chained method invocations (pending formatter support)
    * Add code formatter for .xml files
    * Add code formatter for .yaml files
    * Add code formatter for .md files
    * Automatically order annotations following project conventions
    * Preserve inline chained calls inside method parameters

#### Doc

* Add Javadoc to mapper implementations (pending MapStruct 1.6.0)

#### Misc

* Build a BFF with GraphQL
* Add Graceful shutdown

#### AI Code Assistant

* Create Claude code custom command to review code coverage of all modules and generate a report
* Create Claude code custom command to perform static code analysis and generate a report
