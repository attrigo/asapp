plugins {
    id("asapp.domain-service-conventions")
}

dependencies {
    // Compile
    // Org
    // Required by Spring Security PasswordEncoderFactories
    implementation(libs.bcprov.jdk18on)
    // Must be compile-scope to be used by JdbcConversionsConfiguration
    implementation("org.postgresql:postgresql")
}

pitest {
    targetClasses = setOf(
        "com.attrigo.asapp.authentication.domain.*",
        "com.attrigo.asapp.authentication.application.*.in.service.*",
    )
    targetTests = setOf(
        "com.attrigo.asapp.authentication.domain.*",
        "com.attrigo.asapp.authentication.application.*.in.service.*",
    )
}
