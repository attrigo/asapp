# ASAPP-COMMONS-URL

asapp-commons-url is a library that provides all urls used by ASAPP REST services.

## Requirements

***

* [Java 21 (Java SDK 21)](https://www.oracle.com/es/java/technologies/downloads/#java21)
* [Apache Maven](https://maven.apache.org/download.cgi)

## Installation

***

1. Clone the project:
    ```sh
    git clone https://github.com/attrigo/asapp-commons-url.git
    ```

2. Navigate to the project:
    ```sh
    cd asapp-commons-url
    ```

3. Install the project:
    ```sh
    mvn clean install
    ```

## Getting Started

***

### Usage

The simplest way to use this library is importing it as a maven dependency in the target ASAPP service like this:

```xml

<dependency>
    <groupId>com.bcn.asapp</groupId>
    <artifactId>asapp-commons-url</artifactId>
</dependency>
```

## Dev features

***

### Generate the Javadoc

To generate the Javadoc:

1. Generate the Javadoc files:
    ```sh
    mvn clean package
    ```

2. Open the Javadoc: [index.html](target/site/apidocs/index.html)

### Format code

The project uses [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-maven) to properly format Java code following style defined
in [asapp_formatter.xml](../../asapp_formatter.xml) file.

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

## License

***

asapp-commons-url is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0").
