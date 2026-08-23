# TODO LIST

---

## 0.5.0 · Build speed with Gradle

Goal: move the build onto Gradle so every later build is cached, parallel, and incremental.

### Technical

- [X] (build) Replace Maven with Gradle
    - [X] Set up the Gradle project and module structure
    - [X] Migrate dependency management to Gradle
    - [X] Migrate compilation to Gradle
    - [X] Migrate unit testing to Gradle
    - [X] Migrate integration testing to Gradle
    - [X] Migrate coverage reporting to Gradle
    - [X] Migrate mutation testing to Gradle
    - [X] Migrate formatting checks to Gradle
    - [X] Migrate API documentation generation to Gradle
    - [X] Migrate Javadoc and sources jar generation to Gradle
    - [X] Migrate packaging to Gradle
    - [X] Migrate the full build to Gradle
    - [X] Migrate running the app locally to Gradle
    - [X] Migrate Docker image building to Gradle
    - [X] Migrate database migration commands to Gradle
    - [X] Migrate git hook installation to Gradle
    - [X] Add automated tests for the build's custom tasks
    - [X] Reuse the Spring Boot BOM for the build's own dependency versions
    - [X] Restore the software bill of materials in the packaged services
    - [X] Migrate the CI workflow to Gradle
    - [X] Evaluate a single task that runs every CI check
    - [X] Migrate the release workflow to Gradle
    - [X] Keep Claude Code files in sync with the migration
    - [X] Re-enable parallel builds
    - [X] Clean up the Gradle build scripts
        - [X] Decide how the convention plugins are split
        - [X] Apply a consistent block order to every build script
        - [X] Order the plugin blocks by the same origin rule as the dependency blocks
        - [X] Split the build's dependency block by kind, plugins apart from libraries
        - [X] Merge the Org and Other dependency groups into one sorted group
        - [X] Replace the tool-named dependency groups with one shared group
        - [X] Anchor the formatter config files to the settings directory
        - [X] Replace the eager value lookups with lazy providers
        - [X] Clear the deprecations that block the next Gradle major
        - [X] Resolve the remaining entries in the build's problems report
        - [X] Clear the warnings the IDE raises on the build scripts
        - [X] Clean, simplify and standardize comments
        - [X] Separate a task's metadata from its body with a blank line
        - [X] Sort the tasks by build lifecycle phase
        - [X] Group the tasks under phase headings
        - [X] Document the build-script conventions
    - [X] Migrate build documentation to Gradle
    - [X] Verify full parity, then remove Maven entirely
- [ ] (tests) Assert the packaged bill of materials lists real components
    - **Note:** the actuator endpoint lists the `application` id whenever a readable file exists at the classpath location, so asserting on the id list passes even for a zero-component file — the guard must read `components`
    - **Note:** `spring-boot-starter-actuator` is the only shipped coordinate present in all five services, with an identical identity under Maven and Gradle; assert on parsed nodes rather than the response body, which is roughly 556 KB
    - **Note:** do not assert `scope`, `properties` or `modified` — Maven and Gradle emit different optional field sets for a component
    - **Note:** this is also the only red-test guard on the `includeConfigs` allowlist — the filter is a full-string regex over a Boot-owned configuration name, so an upstream rename would silently empty the file with no other signal
- [X] (architecture) Add an ArchUnit layering and boundary guardrail
    - [X] Enforce the infrastructure → application → domain dependency direction
    - [X] Keep the domain and application layers free of framework dependencies
    - [X] Confine cross-layer access to the declared input and output ports
    - [X] Pin ArchUnit and keep it out of the runtime image
    - [X] Settle a naming and formatting convention for architecture rules
    - [X] Settle how finely to split the architecture rule classes
- [X] (persistence) Wrap authentication user create and update in a transaction
- [X] (architecture) Reconcile driven-adapter conventions and align the code
    - [X] Settle the driven-adapter naming, implementation, and placement conventions
    - [X] Refactor the mismatched adapters to match the settled conventions
    - [X] Narrow or drop bullet 3 of the `## Driven Adapters` convention
    - [X] Remove the unreachable `TokenStore` existence-check methods
    - [X] Settle how `TokenIssuerAdapter`'s issuance failure crosses the port boundary
    - [X] Decide where `JwtIssuanceException` belongs now that its only thrower left `security/`
    - [X] Reconsider the deferred ArchUnit rule for adapter naming and placement
    - [X] Replace `RedisJwtStore.save`'s four positional parameters with a paired type
    - [X] Settle whether the time-to-live calculation belongs in the token entry
    - [X] Replace the Redis store's raw token strings with `EncodedToken`
- [ ] (error-handling) Make encoded-token validation failures consistent with other domain errors
    - **Note:** `InvalidEncodedTokenException` extends `RuntimeException` while the other custom domain exceptions extend `IllegalArgumentException`; surfaced by the domain-design.md S3 review (docs/reviews/2026-07-24-domain-design-review.md)

### Docs & Tooling

- [X] (ai) Establish authoring conventions for Claude rules and agents
    - [X] Define a rule for authoring rule files
    - [X] Align existing rule files with the new authoring rule
    - [X] Define a rule for authoring agent files
    - [X] Align existing agent files with the new authoring rule
- [X] (ai) Reconcile the subagent roster with the authoring rules
    - [X] Reconcile the Claude maintenance agent with the authoring rules
    - [X] Reconcile the review roster with what the review skills need
    - [X] Reconcile the code reviewer's rule routing with the rule globs
- [X] (ai) Sharpen the task workflow skills
    - [X] Right-size the review skills' delegation to the change
    - [X] Reserve code-reviewer for judging code quality
    - [X] Cap review findings to short, plain-language blocks
    - [X] Trim what the review and resolve skills show in chat
    - [X] Auto-generate a full findings report from both review skills
    - [X] Add a triage gate before resolve-review-issues explores an issue
    - [X] Shorten resolve-review-issues' applied note to one line
    - [X] Cap close-task' post implementation note
    - [X] Make prepare-version and refine-task emit commit-sized outcomes
    - [X] Generalize close-task's doc-commit step wording
    - [X] Cap draft-commit-msg's lead paragraph and bullets in words
- [X] (ai) Scope the subagent dispatch rule to agent choice, not count

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
* Decouple the application layer from every framework dependency

#### security

* Use public and private keys to sign the JWT
* Add OTT authentication
* Add MFA via OTP
* Refactor JWT algorithm selection to use primitive type patterns in switch (pending stable Java support)
* Support dynamic refresh of the expired JWT cleanup scheduler
* Key stored JWTs by hash instead of the raw token
* Make JWT store saves and deletes atomic

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
