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

* [X] Upgrade to Spring Boot 3.3.x
* [X] Update all external dependencies
* [X] Improve date / datetime formating
    * [X] Refactor LocalDateTime by Instance
    * [X] Review JacksonMapperConfiguration class
    * [X] Update documentation
* [X] Setup Observability Metrics
    * [X] Use Prometheus and Grafana
    * [X] Add JVM dashboard for each service
    * [X] Update documentation
* [ ] Add Spring Security
    * [ ] Analyze which Auth Server to use
    * [ ] Set up the Auth server
    * [ ] Protect business endpoints
    * [ ] Protect actuator endpoints
    * [ ] Update documentation
* [ ] Improve GitHub actions
    * [ ] Build tags

## Version 0.3.0

***

* [ ] Add business operation to tasks service to generate a complete description of a task from a few words
    * [ ] Use Spring IA

## Version 0.4.0

***

* [ ] Improve logging
    * [ ] Show console logs in plain test
    * [ ] Save file logs in JSON format
* [ ] Setup Observability Logs & Traces
    * [ ] Use Loki for logs?
    * [ ] Use Tempo for traces?
* [ ] Add Spring Cloud Config
* [ ] Add Spring Service discovery

## Version x

***

## asapp-tasks-service

* [ ] Add more business to tasks service
    * [ ] Add more fields to task domain like: creation date, end date, estimation, status, subtasks, labels, user
    * [ ] Add operation to find tasks by list of ids
    * [ ] Add operation to update only certain fields
* [ ] Add GraphQL
    * [ ] Create CRUD operations

## asapp-projects-service

* [ ] Add more business to projects service
    * [ ] Add more fields to project domain like: creation date, end date, estimation, status, labels, user
    * [ ] Add operation to find projects by list of ids
    * [ ] Add operation to update only certain fields
    * [ ] Implement integration between CUD operations and tasks service
* [ ] Add GraphQL
    * [ ] Create CRUD operations

# asapp-rest-clients

* [ ] Add circuit breaker

### Tech

* [ ] Add Native support
* [ ] Add Graceful shutdown
* [ ] Create parent project/starters

### Observability

* [ ] Add database metrics dashboard
* [ ] Add endpoints metrics dashboard

### Security

* [ ] Add security to Observability tools
* [ ] Enable shutdown endpoint

### Tests

* [ ] Reuse db TestContainers instance in tests ([ref](https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1))

### Git

* [ ] Add Unix (LF) line separator check to Git Hook

### CI

* [ ] Publish jar packages and Docker images to GitHub packages
* [ ] Automatize versioning process
* [ ] Generate CHANGELOG.md file automatically from commits

### Tools

* [ ] Refactor Maven by Gradle
* [ ] Improve code formatting
    * [ ] Add code formatter for .xml files
    * [ ] Add code formatter for .md files
    * [ ] Add code formatter for .yaml files

### Doc

* [ ] Add Javadoc to mapper implementations, requires version 1.6.0 of mapstruct ([ref](https://github.com/mapstruct/mapstruct/pull/3219))

### Analysis

* [ ] Analyze how to integrate Spring Modulith
* [ ] Review Request and Response naming (camelCase vs snake_case vs kebab-case)