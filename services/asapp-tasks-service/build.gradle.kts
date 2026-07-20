plugins {
    id("asapp.domain-service-conventions")
}

pitest {
    targetClasses = setOf(
        "com.attrigo.asapp.tasks.domain.*",
        "com.attrigo.asapp.tasks.application.*.in.service.*",
    )
    targetTests = setOf(
        "com.attrigo.asapp.tasks.domain.*",
        "com.attrigo.asapp.tasks.application.*.in.service.*",
    )
}
