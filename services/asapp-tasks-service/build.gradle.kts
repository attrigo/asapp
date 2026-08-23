plugins {
    // ## ASAPP
    id("asapp.domain-service-conventions")
}

// # Extensions

// Points the shared Liquibase config at this service's database
liquibase {
    activities.named("main") {
        withGroovyBuilder { "url"("jdbc:postgresql://localhost:5433/tasksdb") }
    }
}

// Targets mutation testing at this service's packages
pitest {
    val pitestPackages = setOf(
        "com.attrigo.asapp.tasks.domain.*",
        "com.attrigo.asapp.tasks.application.*.in.service.*",
    )
    targetClasses = pitestPackages
    targetTests = pitestPackages
}
