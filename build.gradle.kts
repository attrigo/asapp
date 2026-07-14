plugins {
    base
}

val leafModules = listOf(
    ":libs:asapp-commons-url",
    ":libs:asapp-http-clients",
    ":services:asapp-authentication-service",
    ":services:asapp-config-service",
    ":services:asapp-discovery-service",
    ":services:asapp-tasks-service",
    ":services:asapp-users-service",
)

// Modules that publish javadoc & sources jars (library + domain-service tiers).
val jarTierModules = listOf(
    ":libs:asapp-commons-url",
    ":libs:asapp-http-clients",
    ":services:asapp-authentication-service",
    ":services:asapp-tasks-service",
    ":services:asapp-users-service",
)

val installGitHooks by tasks.registering(Exec::class) {
    description = "Points git at the version-controlled hooks in git/hooks (idempotent)."
    group = "build setup"
    val currentHooksPath = providers.exec {
        commandLine("git", "config", "--get", "core.hooksPath")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }
    onlyIf { currentHooksPath.getOrElse("") != "git/hooks" }
    commandLine("git", "config", "core.hooksPath", "git/hooks")
}

tasks.named("build") {
    dependsOn(installGitHooks)
}

tasks.register("fullBuild") {
    group = "build"
    description = "Full verification (the Maven -Pfull equivalent): build + coverage + javadoc/sources jars."
    dependsOn(leafModules.map { "$it:build" })
    dependsOn(leafModules.map { "$it:jacocoTestReport" })
    dependsOn(jarTierModules.flatMap { listOf("$it:javadocJar", "$it:sourcesJar") })
}
