# ASAPP

asapp is a web application focused on task management.

## Features

***

Provides the following basic CRUD operations to manage projects and tasks:
* Create projects and tasks
* Update projects and tasks
* Find projects and tasks
* Delete projects and tasks

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

1. Build the application's docker image:
    ```sh
    mvn spring-boot:build-image
    ```

2. Launch application's docker-compose:
    ```sh
    docker-compose up -d
    ```

### Usage

Each service brings with an embedded [Swagger UI](https://swagger.io/tools/swagger-ui/), a web tool that facilitates the endpoints visualization and
interaction. \
You can use this Swagger UI or any other HTTP client to consume the API.

### Shut down and clean

In order to avoid wasting local machine resources it is recommended to stop all started Docker services once they are no longer necessary.

* To stop all Docker service:
    ```sh
    docker-compose down -v
    ```

> The -v flag is optional, it deletes the volumes.

## Contributing

***

To ensure the project standards, the code quality and the correct application behaviour each addition or modification must pass several validations.

There are a few local validations triggered as [git hooks](git/hooks) that checks:

1. Code follows specific code style.
2. Commit message pursues [Conventional Commits standard](https://www.conventionalcommits.org/en/v1.0.0/).

There is also a CI [pipeline](.github/workflows/ci.yml) triggered as GitHub actions that checks:

1. Code compiles and tests passes.

## License

***

asapp is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0").
