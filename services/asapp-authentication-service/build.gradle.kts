plugins {
    // ## ASAPP
    id("asapp.domain-service-conventions")
}

dependencies {
    // # Compile
    // ## Other
    // Required by Spring Security PasswordEncoderFactories
    implementation(libs.bcprov.jdk18on)
    // Must be compile-scope to be used by JdbcConversionsConfiguration
    implementation("org.postgresql:postgresql")
}

// # Extensions

// Points the shared Liquibase config at this service's database
liquibase {
    activities.named("main") {
        withGroovyBuilder { "url"("jdbc:postgresql://localhost:5432/authenticationdb") }
    }
}

// Targets mutation testing at this service's packages
pitest {
    val pitestPackages = setOf(
        "com.attrigo.asapp.authentication.domain.*",
        "com.attrigo.asapp.authentication.application.*.in.service.*",
    )
    targetClasses = pitestPackages
    targetTests = pitestPackages
}
