# ASAPP-TASKS-SERVICE

asapp-tasks-service is a REST service application that publishes tasks operations. \
These operations are exposed via HTTP endpoints. \
There are also exposed a bunch of non-business endpoints which are produced by Spring Boot Actuator.

The architecture is mainly based on Java 21 and Spring Boot 3.2, and it follows some of the [Microservice Architecture Principles](https://microservices.io/): \
The [Database per Service](https://microservices.io/patterns/data/database-per-service.html) pattern, therefore the Database is implemented by the service
itself, specifically the management of the database changes are delegated on Liquibase.

## Requirements

***

* [Java 21 (Java SDK 21)](https://www.oracle.com/es/java/technologies/downloads/#java21)
* [Apache Maven](https://maven.apache.org/download.cgi)
* [Docker](https://www.docker.com/)
* [Docker Compose](https://docs.docker.com/compose/)

## Installation

***

The recommend way to install this project is doing it from the parent ASAPP project, which fully install the service and its dependencies. \
In any case, if you prefer there is a way to only install this service:

1. Clone the project:
    ```sh
    git clone https://github.com/attrigo/asapp-tasks-service.git
    ```

2. Navigate to the project:
    ```sh
    cd asapp-tasks-service
    ```

3. Install the project:
    ```sh
    mvn clean install
    ```

## Getting Started

***

### Start up

The application can be started in dev or docker mode.

* Development mode.
    1. Start an standalone database:
        ```sh
        docker-compose up -d asapp-tasks-postgres-db
        ```

    2. Launch the main class [AsappTasksServiceApplication](src/main/java/com/bcn/asapp/tasks/AsappTasksServiceApplication.java).

* Docker mode.

> This mode starts the application and all its services at once

1. Build the application's Docker image:
    ```sh
    mvn spring-boot:build-image
    ```

2. Launch application's docker-compose:
    ```sh
    docker-compose up -d
    ```

### Usage

The project brings with an embedded [Swagger UI](https://swagger.io/tools/swagger-ui/), a web tool that facilitates the endpoints visualization and
interaction. \
You can use this [Swagger UI](http://localhost:8080/asapp-tasks-service/swagger-ui.html) or any other HTTP client to consume the API.

### Shut down and clean

In order to avoid wasting local machine resources it is recommended to stop all started Docker services once they are no longer necessary.

* To stop the standalone database:
    ```sh
    docker-compose down -v asapp-tasks-postgres-db
    ```

* To stop all Docker service:
    ```sh
    docker-compose down -v
    ```

> The -v flag is optional, it deletes the volumes.

## Dev features

***

### Generate Docker image

To build the Docker image:

```sh
mvn spring-boot:build-image
```

### Start up a standalone database

To start up the database in standalone mode:

```sh
docker-compose up -d asapp-tasks-postgres-db
```

> This option creates an empty database, to update the database with the appropriate objects use Liquibase.

### Managing Database changes

* To apply the changes:
    ```sh
    mvn liquibase:update
    ```

* To roll back the changes:
    ```sh
    mvn liquibase:rollback
    ```

> For more information about Liquibase actions visit [Liquibase docs](https://docs.liquibase.com/home.html)

### Generate the test coverage report

To launch the tests and generate the coverage report:

1. Generate the test report:
    ```sh
    mvn clean verify
    ```

2. Open the report: [index.html](target/site/jacoco-aggregate/index.html)

> The coverage report includes unit tests and integration tests

### Generate the Javadoc

To generate the Javadoc:

1. Generate the Javadoc files:
    ```sh
    mvn clean package
    ```

2. Open the Javadoc: [index.html](target/site/apidocs/index.html)

### Format code

The project uses [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-maven) maven plugin to properly format the code. \
Java code is formatted following style defined in [asapp_formatter.xml](../../asapp_formatter.xml) file.

* To check code style: identifies code not well formatted.
    ```sh
    mvn spotless:check
    ```

* To format files: formats any unformatted code.
    ```sh
    mvn spotless:apply
    ```

## Resources

***

### Reference Documentation

For further reference, please consider the following sections:

* Spring Boot
    * [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
    * [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator)
    * [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
    * [Spring Boot Test](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
* Spring
    * [Spring Reactive Web](https://docs.spring.io/spring-framework/reference/web/webflux.html)
    * [Spring Data R2DBC](https://docs.spring.io/spring-data/relational/reference/r2dbc.html)
    * [Spring Validation](https://docs.spring.io/spring-framework/reference/core/validation.html)
    * [Spring OpenAPI](https://springdoc.org/)
* Database
    * [PostgresQL](https://www.postgresql.org/docs/current/)
    * [Liquibase Migration](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/index.html#howto.data-initialization.migration-tool.liquibase)
* Testing
    * [Junit](https://junit.org/junit5/docs/current/user-guide/)
    * [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
    * [TestContainers](https://java.testcontainers.org/)
* Tools
    * [MapStruct](https://mapstruct.org/documentation/)

## License

***

asapp-tasks-service is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0").