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
* [ ] Push to GitHub
* [ ] Upgrade maven dependencies to latest version
* [ ] Move common DTOs to Java Records
    * [ ] Remove lombok dependency
    * [ ] Remove lombok references
* [ ] Create projects service
    * [ ] Copy from tasks service and rename
* [ ] Create commons URI library
    * [ ] Build project
    * [ ] Add tasks URIs
    * [ ] Add project URIs
    * [ ] Refactor all endpoints to use commons URI
    * [ ] Add README file
* [ ] Create REST client library
    * [ ] Copy from bk
    * [ ] Review Javadoc
    * [ ] Review tests
    * [ ] Create README file
* [ ] Create relationship between projects and tasks
    * [ ] asapp-tasks-service
        * [ ] Add project_id column to task table via Liquibase
        * [ ] Add endpoint to get tasks by project id
        * [ ] Add tests
        * [ ] Add Javadoc
        * [ ] Update documentation
    * [ ] asapp-rest-clients
        * [ ] Add client for tasks
            * [ ] Add operation to get tasks by project id
        * [ ] Add tests
        * [ ] Update documentation
    * [ ] asapp-projects-service
        * [ ] Update get project by id endpoint to include the tasks
        * [ ] Update tests
            * [ ] Use MockServer for E2EIT [ref](https://testcontainers.com/guides/testing-rest-api-integrations-using-mockserver/)
        * [ ] Update Javadoc
        * [ ] Update documentation
    * [ ] Update parent docker-compose file
    * [ ] Update parent documentation

## Version 0.2.0

***

* [ ] Setup Observability (Spring Actuator)
    * [ ] Metrics (Prometheus and Grafana)
    * [ ] Logs (Loki)
    * [ ] Traces (Tempo)
    * [ ] Improve tempo volume (tempo-data\blocks)
    * [ ] Fix development tracing error
    * [ ] Review TraceId and SpanId propagation
        * [ ] Test TraceId and SpanId propagation with another service
    * [ ] Review logs format
        * [ ] Review logs format of development mode
        * [ ] Review logs format of docker mode
        * [ ] Review logs format of tests
        * [ ] Review logs format for errors
    * [ ] Update documentation

## Version 0.3.0

***

* [ ] Add Spring Security
    * [ ] Integrate with OAuth2 Server (Keycloak)
    * [ ] Protect actuator endpoints
    * [ ] Adapt services tests
    * [ ] Update documentation

## Version 0.4.0

***

* [ ] Add Spring Cloud Config
* [ ] Add Spring Service discovery (eureka)

## Version x

***

## asapp-tasks-service

* [ ] Add more fields to task domain: End date, Estimation, Status, Subtasks, Labels
* [ ] Add GraphQL

## asapp-projects-service

* [ ] Add GraphQL

# asapp-rest-clients

* [ ] Add circuit breaker to REST clients
* [ ] Improve the way of build URIs

### Tech

* [ ] Improve date format exception handling
* [ ] Add Native support
* [ ] Add Graceful
  shutdown ([ref](https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/html/spring-boot-features.html#boot-features-graceful-shutdown))
* [ ] Create parent project/starters

### Observability

* [ ] Add database metrics dashboard
* [ ] Add endpoints metrics dashboard
* [ ] Show logs in JSON format

### Security

* [ ] Add security to Observability tools
* [ ] Enable shutdown endpoint

### Tests

* [ ] Reuse db TestContainers instance ([ref](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1))

### Git

* [ ] Add Unix (LF) line separator check to Git Hook

### CI

* [ ] Publish jar packages and Docker images to GitHub packages
* [ ] Automatize versioning process
* [ ] Generate CHANGELOG.md file automatically from commits
* [ ] Improve GitHub actions
* [ ] Add Autoasign ([ref](https://github.com/apps/auto-assign))
* [ ] Add
  CODEOWNERS ([ref](https://docs.github.com/es/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners))

### Tools

* [ ] Refactor Maven by Gradle
* [ ] Add formatter for .xml files
* [ ] Add formatter for .md files
* [ ] Check formatter for .yaml files

### Doc

* [ ] Add Javadoc to mapper implementations, requires version 1.6.0 of mapstruct ([ref](https://github.com/mapstruct/mapstruct/pull/3219))

### Analysis

* [ ] Review best way to manage responses in API layer (Publisher<ResponseEntity<DTO>> vs ResponseEntity<Publisher<DTO>> vs Publisher<DTO>)
* [ ] Review Request and Response key naming (camelCase vs snake_case)
* [ ] Analyze how to add Integration Tests to REST clients