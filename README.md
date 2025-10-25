# ASAPP

ASAPP is a web application focused on task management.

## Features

***

ASAPP application provides the following operations to work with projects and tasks:

* Tasks operations
* Authentication operations

The application also comes with a [Grafana](https://grafana.com/) instance; this provides some metrics dashboards to monitor the services.

## Requirements

***

* [Java 21 (Java SDK 21)](https://www.oracle.com/es/java/technologies/downloads/#java21)
* [Apache Maven](https://maven.apache.org/download.cgi)
* [Docker](https://www.docker.com/)
* [Docker Compose](https://docs.docker.com/compose/)

## Installation

***

1. Clone the project:
    ```sh
    git clone https://github.com/attrigo/asapp.git
    ```

2. Navigate to the project:
    ```sh
    cd asapp
    ```

3. Install the project:
    ```sh
    mvn clean install
    ```

## Getting Started

***

### Start up

1. Build the application:
    ```sh
    mvn spring-boot:build-image
    ```

2. Launch the application:
    ```sh
    docker-compose up -d
    ```

### Usage

Each service brings with an embedded [Swagger UI](https://swagger.io/tools/swagger-ui/), a web tool that facilitates the endpoint visualization and
interaction. \
You can use this Swagger UI or any other HTTP client to consume the API.

> Dates sent in requests must follow a standard ISO-8601 format.

You can access to metrics dashboards opening [Grafana](http://localhost:3000) tool in the web browser.

### Shut down and clean

To avoid wasting local machine resources, it is recommended to stop all started Docker services once they are no longer necessary.

* To stop the application:
    ```sh
    docker-compose down -v
    ```

> The -v flag is optional, it deletes the volumes.

## Contributing

***

To ensure the project standards, the code quality and the correct application behavior, each addition or modification must pass several validations.

There are a few local validations triggered as [git hooks](git/hooks) that checks:

1. Code follows a specific code style.
2. Commit message pursues [Conventional Commits standard](https://www.conventionalcommits.org/en/v1.0.0/).

There is also a CI [pipeline](.github/workflows/ci.yml) triggered as GitHub actions that checks:

1. Code compiles and tests passes.

## Release

***

Steps to release a version

1. Checkout main branch
    ```sh
    git checkout main
    ```

2. Release the current version
    1. Remove SNAPSHOT version in pom files
        ```sh
        mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
        ```
    2. Add the Liquibase changeset to create database tags to all vx_x_x-changelog.xml files
        ```xml
        <changeSet id="tag_version_x_x_x" author="attrigo">
            <tagDatabase tag="x.x.x"/>
        </changeSet>
        ```
    3. Remove all occurrences to a SNAPSHOT version in all files
    4. Build the project
        ```sh
        mvn clean install
        ```
    5. Commit all changes
        ```sh
        RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
        git add .
        git commit -m "chore: release version ${RELEASE_VERSION}"
        ```

3. Create tag
    ```sh
    git tag ${RELEASE_VERSION}
    ```

4. Prepare the project for the next development version
    1. Update pom files to the next SNAPSHOT version
        ```sh
        mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true  -DgenerateBackupPoms=false
        ```
    2. Update all occurrences to a NON-SNAPSHOT version to the next SNAPSHOT version in all files
    3. Build the project
        ```sh
        mvn clean install
        ```
    4. Commit all changes
        ```sh
        NEXT_DEV_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
        git add .
        git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"
        ```

5. Push all changes
    ```sh
    git push --atomic origin main ${RELEASE_VERSION}
    ```

## License

***

ASAPP is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0").
