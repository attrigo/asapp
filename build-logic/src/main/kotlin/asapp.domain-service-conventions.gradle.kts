import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("asapp.service-conventions")
    id("info.solidsoft.pitest")
    id("org.asciidoctor.jvm.convert")
}

// Same explicit bridge as asapp.java-conventions — see gradle/gradle#15383.
val libs = the<LibrariesForLibs>()

// org.asciidoctor.jvm.convert does not auto-create a configuration for doc extensions
// (only the `asciidoctor` task); this one is wired into the task below via `configurations(...)`.
val asciidoctorExtensions: Configuration by configurations.creating

dependencies {
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // REST Docs asciidoctor extension (operation:: macros).
    asciidoctorExtensions(libs.spring.restdocs.asciidoctor)
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

pitest {
    pitestVersion.set(libs.versions.pitest.get())
    junit5PluginVersion.set(libs.versions.pitestJunit5.get())
    mutationThreshold.set(100)
    targetClasses.set(
        setOf(
            "com.attrigo.asapp.*.domain.*",
            "com.attrigo.asapp.*.application.*.in.service.*",
        ),
    )
    targetTests.set(
        setOf(
            "com.attrigo.asapp.*.domain.*",
            "com.attrigo.asapp.*.application.*.in.service.*",
        ),
    )
}

// REST Docs snippets are produced during integrationTest; asciidoctor consumes them.
tasks.named<AsciidoctorTask>("asciidoctor") {
    dependsOn(tasks.named("integrationTest"))
    configurations(asciidoctorExtensions)
    setSourceDir(file("src/docs/asciidoc"))
    baseDirFollowsSourceDir()
    options(mapOf("doctype" to "book"))
    attributes(
        mapOf(
            "snippets" to layout.buildDirectory.dir("generated-snippets").get().asFile,
        ),
    )
    outputOptions {
        backends("html5")
    }
}
