plugins {
    id("asapp.domain-service-conventions")
}

pitest {
    val pitestPackages = setOf(
        "com.attrigo.asapp.tasks.domain.*",
        "com.attrigo.asapp.tasks.application.*.in.service.*",
    )
    targetClasses = pitestPackages
    targetTests = pitestPackages
}
