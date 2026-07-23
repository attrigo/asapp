import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("asapp.java-conventions")
    `java-library`
}

// Report all javadoc problems except missing comments.
tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

// Package the javadoc output; classifier `javadoc`.
tasks.register<Jar>("javadocJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main Javadoc."
    archiveClassifier = "javadoc"
    // from: what to package; the javadoc task's output.
    from(tasks.named("javadoc"))
}

// Package the main sources; classifier `sources`.
tasks.register<Jar>("sourcesJar") {
    group = "documentation"
    description = "Assembles a jar archive containing the main sources."
    archiveClassifier = "sources"
    // from: what to package; the main source set's files (allSource = Java sources + resources).
    from(project.the<SourceSetContainer>()["main"].allSource)
}
