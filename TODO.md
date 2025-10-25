# TODO LIST

## Version 0.1.0

***

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

## Version 0.2.0

***

* [ ] Refactor services to Hexagonal Architecture + DDD
    * [X] Move to Hexagonal packaging
    * [X] Define domain model following DDD
    * [X] Adapt Tests
    * [X] Adapt Javadoc
    * [X] Adapt documentation
    * [ ] Delete and update a user use case should delete authentications via events
* [X] Set up Observability Metrics
    * [X] Use Prometheus and Grafana
    * [X] Add a JVM dashboard for each service
    * [X] Update documentation
* [ ] Add Spring Security
    * [X] Create an authentication service
        * [X] Add authenticate endpoint
        * [X] Add refresh endpoint
        * [X] Add revoke endpoint
        * [X] Add basic CRUD endpoints for user (to be able to manage users)
        * [X] Support for multiple authentications by user
        * [X] Add trace logging in all auth operations
        * [ ] Change user's username or password must revoke all user authentications
        * [ ] Create user must not create a user with the same username (email)
    * [X] Adapt other services
        * [X] Protect business endpoints
        * [X] Propagate JWT when performing HTTP calls
    * [ ] Use Redis to ensure JWT is still valid in the system
        * (authentication-service) Move JwtVerifier to security package
        * (authentication-service) Add DecodedToken
    * [X] Protect management endpoints
    * [ ] Update documentation
* [ ] Create asapp-users-service
    * [X] Build project
    * [X] Add Liquibase
    * [X] Add basic CRUD endpoints
    * [X] Add management endpoints
    * [X] Add unit and integration tests
    * [X] Add a docker-compose file
    * [X] Add service to prometheus
    * [X] Add documentation as a README file
    * [ ] Get endpoints should fetch tasks from asap-tasks-service
    * [ ] Delete user must propagate deletion of user on authentication and tasks systems
* [ ] Improvements on asapp-tasks-service
    * [ ] Add the end date field to the task
    * [ ] Remove gets tasks by project id endpoint
    * [ ] Add endpoint to get tasks by user id
    * [ ] Create and update a task must check user_id exists
* [ ] Testing
    * [X] Make PostgresQL TestContainer a singleton instance
    * [X] Create a test data fake factory to generate test data
    * [X] Replace Hamcrest assertions by AssertJ assertions
    * [X] Add PiTest
    * [-] Review test coverage
    * [-] Create a specific test for OpenApi and Actuator content (split from SecurityConfigurationIT)?
    * [-] Improve domain tests checking fields are not blank
    * [-] Improve Test faker
    * [ ] Update documentation
* [ ] Improve management endpoints (Actuator)
    * [X] Show more health details when authenticated
    * [X] Move management to a separate port
    * [X] Add management probes endpoints (including readyz and livez)
    * [X] Add env, Java, OS and process details to info endpoint
    * [X] Add git details to info endpoint (git-commit-id plugin)
    * [X] Add SBOM endpoint (cyclonedx-maven-plugin)
    * [X] Enable shutdown endpoint
    * [ ] Update documentation
* [X] Improve date / datetime formating
    * [X] Refactor LocalDateTime by Instance
    * [X] Update documentation
* [X] Improve Java formatter
    * [X] Separate license from the package in all java files
    * [X] Not put empty lines before "try {" and after "}"
    * [X] Records
    * [-] Wrap chained method invocations keeping two method calls
    * [ ] Wrap if statements by control flow keywords (&& and ||)
    * [ ] Update documentation
* [ ] CI/CD
    * [X] Add a Unix (LF) line separator check to Git Hook
    * [ ] Add maven profiles to avoid some steps during local builds
    * [ ] Improve GitHub actions
        * [ ] Build tags
    * [ ] Update documentation
* [ ] Technical improvements
    * [X] Replace "/v1" with "/api" in the path of all endpoints
    * [X] Add "<relativePath>..</relativePath>" to libs and services poms
    * [X] Change the debug level of jdbc to info in application-docker.properties
    * [X] Rename database primary keys, from "*_id_pk" to "pk_"
    * [X] Improve data validation via Jakarta Annotations
    * [-] Review console warnings
    * [ ] Launch Openrewrite Spring Boot best practices
    * [ ] Upgrade to Spring Boot 4
    * [ ] Upgrade all external dependencies
    * [ ] Upgrade to Java 25
    * [ ] Upgrade maven wrapper
    * [ ] Launch Sonar analysis
    * [ ] Launch security analysis
    * [ ] Improve README using AI

## Version 0.3.0

***

* [ ] Add Spring Cloud Config
* [ ] Add Spring Service discovery
* [ ] Improvements to asapp-projects-service
    * [ ] Remake asapp-projects-service to asapp-agenda-service
* [ ] Improvements to asapp-authentication-service
    * [ ] Support multiple roles for a user
    * [ ] Put in place Spring's CompromisedPassword
    * [ ] Add ArchUnit
    * [ ] Add JMolecules
    * [ ] Add Spring Modulith?
        * [ ] Change @ApplicationService by @UseCase
* [ ] Logging
    * [ ] Console logs in plain text
    * [ ] File logs in JSON format
* [ ] Observability
    * [ ] Update Grafana dashboard
    * [ ] Add support for traces
* [ ] Testing
    * [ ] Add Spring Test Profiler
* [ ] CI/CD
    * Add Pitest to the CI pipeline
* [ ] Technical improvements
    * [ ] Improve GlobalExceptionHandler to return a sorted Map<Entity, List<FieldsError>>
    * [ ] Create @ConfiguratioProperties to manage custom REST and Security properties

## Version 0.4.0

***

* [ ] Add business operation to the task service to generate a complete description of a task from a few words/fields
    * [ ] Use Spring IA

## Version x

***

## asapp-authentication-service

* [ ] Change the endpoint that gets all users to return a page of users

## asapp-tasks-service

* [ ] Change the endpoint that gets all tasks to return a page of tasks
* [ ] Add more business to tasks service
    * [ ] Add more fields to task domain like: creation date, end date, estimation, status, subtasks, labels, user
    * [ ] Add operation to find tasks by list of ids
    * [ ] Add operation to update only certain fields

## asapp-users-service

* [ ] Change the endpoint that gets all tasks to return a page of tasks

# asapp-rest-clients

* [ ] Define Spring Stereotype of type Client?
* [ ] Add circuit breaker
* [ ] Add Retries

### Tech

* [ ] Create custom starters
* [ ] Add Native support
* [ ] Add Graceful shutdown
* [ ] Improve how docker volumes are created, to only create volumes when needed
* [ ] Add Spring Rest Docs

### Observability

* [ ] Add database metrics dashboard

### Security

* [ ] Integrate Spring Oauth2 server
* [ ] Use public and private keys to encode the JWT
* [ ] Create a background process to automatically revoke expired JWT
* [ ] Add OTT authentication
* [ ] Add email verification to the register process

### Tests

### Git

### CI

* [ ] Publish jar packages and Docker images to GitHub packages
* [ ] Automatize versioning process
* [ ] Generate CHANGELOG.md file automatically from commits

### Tools

* [ ] Refactor Maven by Gradle
* [ ] Improve code formatting
    * [ ] Add code formatter for .xml files
    * [ ] Add code formatter for .yaml files
    * [ ] Add code formatter for .md files
    * [ ] Automatically order annotations following project conventions
    * [ ] Do not wrap elements of chained method invocations in method parameters (myMethod(a.b().c().d()))

### Doc

* [ ] Add Javadoc to mapper implementations requires version 1.6.0 of mapstruct ([ref](https://github.com/mapstruct/mapstruct/pull/3219))
