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

// spring-restdocs-asciidoctor 4.0.0 requires AsciidoctorJ 3.0+ (its DefaultAttributesPreprocessor
// implements the 3.0 Preprocessor API that returns a Reader). The plugin defaults to the 2.5.x
// line, which throws AbstractMethodError at render time — pin the runtime to a compatible 3.x.
asciidoctorj {
    // getVersion(): String but setVersion(Object) — Kotlin won't treat them as a property pair,
    // so assign via the setter method rather than `version = ...`.
    setVersion(libs.versions.asciidoctorj.get())
}

// REST Docs snippets are produced during integrationTest; asciidoctor consumes them.
tasks.named<AsciidoctorTask>("asciidoctor") {
    // The asciidoctor plugin (4.0.x) captures live Configuration objects as task state, which the
    // configuration cache cannot serialize. Opt this task out so CC-enabled builds degrade gracefully
    // instead of failing when the docs are generated.
    notCompatibleWithConfigurationCache("org.asciidoctor.jvm.convert is not configuration-cache compatible")
    dependsOn(tasks.named("integrationTest"))
    // Pass the configuration NAME, not the Configuration object: the asciidoctor plugin's grolifant
    // helper mis-resolves a Configuration instance into its jar paths and then looks each up as a
    // config name ("Item cannot be resolved"). A String name takes the correct getByName path.
    configurations(asciidoctorExtensions.name)
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
