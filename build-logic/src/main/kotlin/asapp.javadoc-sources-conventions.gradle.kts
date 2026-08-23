plugins {
    // ## ASAPP
    id("asapp.java-conventions")
}

// # Tasks

// ## Documentation

// Sets the Javadoc generation options
tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).apply {
        // Reports all Javadoc problems except missing comments
        addBooleanOption("Xdoclint:all,-missing", true)
        // Pinned so the source, output and HTML charsets never follow the OS default
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        charSet = "UTF-8"
    }
}

// ## Packaging

// Packages the Javadoc output; classifier "javadoc"
tasks.register<Jar>("javadocJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main Javadoc."

    archiveClassifier = "javadoc"
    from(tasks.named("javadoc"))
}

// Packages the main sources; classifier "sources"
tasks.register<Jar>("sourcesJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main sources."

    archiveClassifier = "sources"
    val main = project.the<SourceSetContainer>()["main"]
    val resourceDirs = main.resources.srcDirs
    from(main.allJava)
    // Leaves out the resource dirs under the build directory: generated output, not sources
    from(layout.buildDirectory.map { buildDir -> resourceDirs.filterNot { it.startsWith(buildDir.asFile) } })
}

// ## Aggregates

// Adds the javadoc and sources jars to the full build of every archetype that applies this plugin
tasks.named("fullBuild") {
    dependsOn("javadocJar", "sourcesJar")
}
