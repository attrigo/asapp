plugins {
    id("asapp.java-conventions")
}

// Javadoc & sources jars — off the fast `build` path; invoked explicitly or via fullBuild.
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allJava)
}
