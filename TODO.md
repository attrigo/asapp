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
    * [ ] Define domain model following DDD
    * [ ] Adapt Tests
    * [ ] Adapt Javadoc
    * [ ] Adapt documentation
    * [ ] Use JMolecules https://github.com/xmolecules/jmolecules?tab=readme-ov-file#using-the-annotation-based-model
    * [ ] Add Spring Modulith?
* [X] Set up Observability Metrics
    * [X] Use Prometheus and Grafana
    * [X] Add JVM dashboard for each service
    * [X] Update documentation
* [ ] Add Spring Security
    * [X] Create UAA service
        * User's Username should be unique email not changeable
        * User's password should meet some regexp
        * Avoid invoking jwtAuthentication filter for non-protected endpoints
        * Avoid fetching the user from the DB in AuthenticationGranterAdapter
        * Set up configuration with custom Spring Configurer?
        * [X] Add authenticate endpoint
        * [X] Add refresh endpoint
        * [X] Add revoke endpoint
        * [X] Add basic CRUD endpoints for user (to be able to manage users)
        * [ ] Support for multiple authentications by user
        * [ ] Support multiple roles for a user
    * [X] Adapt other services
        * [X] Protect business endpoints
        * [X] Propagate JWT when performing HTTP calls
    * [ ] Use Redis to ensure JWT is still valid in the system
    * [X] Protect management endpoints
    * [ ] Update documentation
* [ ] Testing
    * Remove @DisplayName?
    * Review test coverage
    * Create a specific test for OpenApi and Actuator content (split from SecurityConfigurationIT)?
    * [X] Make PostgresQL TestContainer a singleton instance
    * [X] Create a test data fake factory to generate test data
    * [X] Replace Hamcrest assertions by AssertJ assertions
    * [ ] Add PiTest
    * [ ] Update documentation
* [ ] Improve management endpoints (Actuator)
    * [ ] Review SBOM plugin warnings
    * [X] Show more health details when authenticated
    * [X] Move management to a separate port
    * [X] Add management probes endpoints (including readyz and livez)
    * [X] Add env, Java, OS and process details to info endpoint
    * [X] Add git details to info endpoint (git-commit-id plugin)
    * [X] Add SBOM endpoint (cyclonedx-maven-plugin)
    * [X] Split Spring Security filter chain into several ones, one for api endpoints, one for management endpoints, and another one for root endpoints
    * [ ] Update documentation
* [X] Improve date / datetime formating
    * [X] Refactor LocalDateTime by Instance
    * [X] Update documentation
* [X] Improve Java formatter
    * [X] Separate license from the package in all java files
    * [X] Not put empty lines before "try {" and after "}"
    * [X] Records
    * [ ] Wrap if statements by control flow keywords (&& and ||)
    * [ ] Update documentation
* [ ] CI/CD
    * [X] Add a Unix (LF) line separator check to Git Hook
    * [ ] Improve GitHub actions
        * [ ] Build tags
    * [ ] Update documentation
* [ ] Technical improvements
    * [ ] Launch Openrewrite Spring Boot best practices
    * [ ] Upgrade to Spring Boot 4
    * [ ] Upgrade all external dependencies
    * [ ] Upgrade to Java 25
    * [ ] Upgrade maven wrapper
    * [X] Replace "/v1" with "/api" in the path of all endpoints
    * [X] Add "<relativePath>..</relativePath>" to libs and services poms
    * [X] Change the debug level of jdbc to info in application-docker.properties
    * [X] Rename database primary keys, from "*_id_pk" to "pk_"
    * [X] Improve data validation via Jakarta Annotations
    * [ ] Launch Sonar analysis
    * [ ] Launch security analysis
    * [ ] Improve README using AI

## Version 0.3.0

***

* [ ] Add Spring Cloud Config
* [ ] Add Spring Service discovery
* [ ] Improve logging
    * [ ] Show console logs in plain text
    * [ ] Save file logs in JSON format
* [ ] Set up Observability Logs & Traces
    * [ ] Use Loki for logs?
    * [ ] Use Tempo for traces?
* [ ] Improve GlobalExceptionHandler to return a sorted Map<Entity, LIst<FieldsError>>
* [ ] Create @ConfiguratioProperties to manage REST and Security properties

## Version 0.4.0

***

* [ ] Add business operation to the task service to generate a complete description of a task from a few words/fields
    * [ ] Use Spring IA

## Version x

***

## asapp-tasks-service

* [ ] Change the endpoint that gets all tasks to return a page of tasks
* [ ] Add more business to tasks service
    * [ ] Add more fields to task domain like: creation date, end date, estimation, status, subtasks, labels, user
    * [ ] Add operation to find tasks by list of ids
    * [ ] Add operation to update only certain fields

## asapp-projects-service

* [ ] Change the endpoint that gets all projects to return a page of projects
* [ ] Add more business to projects service
    * [ ] Add more fields to project domain like: creation date, end date, estimation, status, labels, user
    * [ ] Add operation to find projects by list of ids
    * [ ] Add operation to update only certain fields
    * [ ] Implement integration between CUD operations and tasks service

## asapp-uaa-service

* [ ] Change the endpoint that gets all users to return a page of users

# asapp-rest-clients

* [ ] Define Spring Stereotype of type Client?
* [ ] Add circuit breaker

### Tech

* [ ] Create custom starters
* [ ] Add Native support
* [ ] Add Graceful shutdown
* [ ] Enable shutdown endpoint
* [ ] Improve how docker volumes are created, to only create volumes when needed

### Observability

* [ ] Add database metrics dashboard

### Security

* [ ] Integrate Spring Oauth2 server
* [ ] Use public and private keys to encode the JWT
* [ ] Create a background process to automatically revoke expired JWT
* [ ] Add a double factor authentication
* [ ] Add email verification to the register process

### Tests

* [ ] Decrease the execution time of tests

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

### Analysis

* [ ] Review Request and Response naming (camelCase vs snake_case vs kebab-case)
* [ ] Analyze record-builder : https://github.com/Randgalt/record-builder
