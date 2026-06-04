# TODO LIST

## Version 0.4.0

### Goal - Establish SDD and AI Agents

* [X] Setup SDD flow and tool
* [X] Setup AI agents

### Quick Wins

* Functional Improvements
    * [X] Add operation to find tasks by list of ids
    * [X] Add operation to find users by list of ids
* Technical Improvements
    * Error Handler
        * [X] Return validation errors in a deterministic, sorted order
        * [X] Move all remaining inline error strings (titles, details, codes) into constants
        * [X] Use only fixed messages for `ProblemDetail.detail` (never exception messages)
        * [X] ~~Make `RequestValidationError` correctly represent the origin of validation errors for non-body parameters (path/query)~~
        * [X] Preserve the full field path in validation errors so nested or duplicate field names don't collide
    * JSON Naming
        * [X] Enforce request/response camelCase globally
    * [ ] Add load test with JMeter
    * [ ] Replace REST clients by declarative HTTP clients
        * Use circuit breaker pattern
        * Use retry pattern
* CI/CD
    * [X] Update commit-msg skill to include a bulleted body in generated commit messages
    * [X] ~~Change commit-msg to perform all operations in a dedicated agent and then response with the message~~
* Tools
    * [ ] Support fixup! commits in the commit-msg git hook
* Docs
    * [ ] Remove all reference to "docs/guidelines/" from README files

### Analyze

* [ ] Replace Liquibase by Flyway

---

## Version 0.5.0

### Goal - Introduce Event-Driven Design

* Send a confirmation notification on user creation

### Quick Wins

* Functional Improvements
    * Paginate list endpoints
* Technical Improvements
    * Extract custom REST and security settings into dedicated configuration properties
* Security Improvements
    * Support automatic password format migration on authentication
* CI/CD
    * Limit CI workflow triggers to relevant source changes
    * Support patch version releases in the release skill

---

## Version 0.6.0

### Goal - Setup OAuth2

* Introduce OAuth2 authentication

### Analyze

* Choose between Spring Authorization Server and Keycloak

---

## Version 0.7.0

### Goal - Adopt Modulith with Domain Events & CQRS

* Introduce modularization with Spring Modulith (ArchUnit & JMolecules)
    * Add an ArchUnit/test asserting every request DTO `@JsonProperty` value equals the camelCase of its Java component name (already enforced per service by `JsonNamingConventionTest`; fold into the Modulith ArchUnit suite)
* Adopt domain events following DDD principles
    * Handle domain CUD operations via events
    * Use CQRS pattern
    * Externalize domain events to RabbitMQ for cross-service synchronization

---

## Version 0.8.0

### Goal - Modularize with custom Starters

* Create custom architecture based on Spring starters
    * Create custom starter for Web
    * Create custom starter for Security
    * Create custom starter for Data
    * Create custom starter for Observability
    * Create custom starter for Testing

---

## Version 0.9.0

### Goal - Enforce Cross-Service Data Consistency

* Create or update a user in one service must replicate in other service
* Create and update a task must check if userId exists in user service
* Change user's username or password must revoke all user authentications
* Make User's username unique, user creation must not create the user if there is another one with the same username (email)

---

## Version 0.10.0

### Goal - Improve testing

* Add spring-test-profiler
* Review Spring test context usage

---

## Backlog

### asapp-authentication-service

* Support multiple roles for a user
* Add task status transitions with business rules (open → in-progress → done, no skipping)
* Reject compromised passwords
* Support partial user updates

### asapp-tasks-service

* Enrich task domain (dates, status, estimation, labels, subtasks, assignee)
* Support partial task updates
* Generate full task descriptions from minimal input using AI

### asapp-users-service

* Support partial user updates

### Tech

* Refactor JWT algorithm selection to use primitive type patterns in switch (pending stable Java support)
* Add AOP/Native support
* Add Graceful shutdown
* Support dynamic refresh of the expired JWT cleanup scheduler
* Build a BFF with GraphQL
* Removes NPEs with JSpecify

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
