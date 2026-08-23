plugins {
    // ## ASAPP
    id("asapp.domain-service-conventions")
}

dependencies {
    // # Compile
    // ## ASAPP
    implementation(project(":libs:asapp-http-clients"))
    // ## Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    // ## Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    // ## Other
    implementation(libs.resilience4j.spring.boot4)

    // # Test
    // ## Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    // ## Other
    testImplementation(libs.mockserver.client.java)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.testcontainers.mockserver)
}

// # Extensions

// Points the shared Liquibase config at this service's database
liquibase {
    activities.named("main") {
        withGroovyBuilder { "url"("jdbc:postgresql://localhost:5434/usersdb") }
    }
}

// Targets mutation testing at this service's packages
pitest {
    val pitestPackages = setOf(
        "com.attrigo.asapp.users.domain.*",
        "com.attrigo.asapp.users.application.*.in.service.*",
    )
    targetClasses = pitestPackages
    targetTests = pitestPackages
}
