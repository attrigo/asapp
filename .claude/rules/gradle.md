---
paths:
  - "**/*.gradle.kts"
  - "**/gradle.properties"
  - "**/libs.versions.toml"
---

# Gradle Build Conventions

## DSL

- Use Kotlin DSL (`.gradle.kts`) for every Gradle build script
- Never use Groovy DSL (`.gradle`)

## Shared Build Configuration

- Place cross-cutting build configuration only in precompiled convention plugins inside the `build-logic` composite build
- Reference `build-logic` from the root `settings.gradle.kts` via `pluginManagement { includeBuild("build-logic") }`
- Never place shared config in root `subprojects {}` / `allprojects {}` conditional blocks
- Never use `buildSrc`
- Namespace convention plugin IDs `asapp.<concern>-conventions`, e.g. `asapp.java-conventions`, `asapp.service-conventions`

## Module Structure

- Mirror module project paths exactly on the physical folder layout — no `projectDir` remapping, no flattening
- Declare each leaf module with its own colon-path `include(...)` call in the root `settings.gradle.kts`
- Group `include(...)` calls by top-level folder (`libs`, `services`); sort alphabetically by module name within each group:
  ```kotlin
  include("libs:asapp-commons-url")
  include("libs:asapp-http-clients")
  include("services:asapp-authentication-service")
  include("services:asapp-config-service")
  include("services:asapp-discovery-service")
  include("services:asapp-tasks-service")
  include("services:asapp-users-service")
  ```
- Never give `libs` or `services` their own `build.gradle.kts` or explicit `include(...)` — Gradle creates them automatically as empty intermediate projects

## Root Project Identity

- Set `rootProject.name = "asapp"` in the root `settings.gradle.kts`
- Never use `asapp-parent` — Gradle's root project isn't a deployable artifact

## Versioning

- Single-source `group` and `version` in root `gradle.properties` (`group=com.attrigo.asapp`, `version=...`) — Gradle propagates both to every project in the build automatically, no `allprojects`/`subprojects` block needed
- Never set `group` or `version` in a module's own build script

## Compilation

- Set Java version, encoding, and compiler args only in `asapp.java-conventions` — never per-leaf
- Pin the Java version with a toolchain **plus** `options.release` on `JavaCompile` (toolchain = which JDK compiles/tests; `release` = bytecode/API level)
- In `tasks.withType<JavaCompile>().configureEach { }`, set `options.encoding = "UTF-8"` and add `-parameters` (Spring name-based binding) to `options.compilerArgs`
- Set encoding per-task, never daemon-wide via `org.gradle.jvmargs` (that clobbers Gradle's default JVM args)

## Testing

- Configure test execution only in convention plugins — never per-leaf
- Enable the JUnit Platform once via `tasks.withType<Test>().configureEach { useJUnitPlatform() }` in `asapp.java-conventions` (JUnit 5 isn't Gradle's default) — it covers every tier, including tasks registered in other plugins
- Unit tier: `tasks.named<Test>("test")` in `asapp.java-conventions` sets `include("**/*Tests.class")`
- Integration tier: register one `integrationTest` `Test` task in `asapp.service-conventions` (services only), reusing the `test` source set (`testClassesDirs`/`classpath`) with `include("**/*IT.class")` (also matches `*E2EIT`); order it `shouldRunAfter(tasks.named("test"))` and wire `check.dependsOn(integrationTest)`
- Declare `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — Gradle 9 no longer auto-provides it and the Spring Boot starters don't bundle it (versionless, BOM-managed)
- The base plugin adds only the platform launcher, not a JUnit engine — each module that runs tests supplies its own via a `spring-boot-starter-*-test` starter

## Coverage

- Apply the core `jacoco` plugin in `asapp.java-conventions` (all modules) and pin the tool from the catalog: `jacoco { toolVersion = libs.findVersion("jacoco").get().requiredVersion }` — `0.8.14` for official Java 25 support (also Gradle's default)
- The unit-tier report is the plugin's own `jacocoTestReport` task (all modules); add `dependsOn(tasks.named("test"))` since the plugin does not wire it to run the tests
- Register the integration-tier (`jacocoIntegrationTestReport`) and merged (`jacocoMergedReport`) reports in `asapp.service-conventions` (services only); reference the `Test` tasks explicitly in `executionData()`/`dependsOn()` (integration → `integrationTest`; merged → `test` + `integrationTest`) — passing a live `tasks.withType<Test>()` collection to `executionData()` fails at task realization (`DefaultTaskCollection#all` context error)
- Keep reports off the `check`/`build` path — opt in by naming the report task (the flag-free analog of Maven's `-Pfull`); the JaCoCo agent is auto-attached to every `Test` task, so there is no activation flag
- HTML only; no coverage thresholds or enforcement (report-only, at Maven parity) — set once for every report task via `tasks.withType<JacocoReport>().configureEach` in `asapp.java-conventions`

## Formatting

- Put the `com.diffplug.spotless` plugin on the `build-logic` classpath (catalog `spotless` version + `spotless-plugin` library + `implementation(libs.spotless.plugin)` in `build-logic/build.gradle.kts`), then apply it with a versionless `id("com.diffplug.spotless")` in `asapp.java-conventions` (all 7 modules) — the same altitude as the other all-module Java concerns
- Pin the version from the catalog (`spotless = "8.8.0"`)
- Preserve Maven's formatting exactly: `eclipse("4.35").configFile(rootProject.file("asapp_formatter.xml"))`; `importOrder("java|javax", "org", "com", "", "com.attrigo")` with the empty catch-all group and trailing `com.attrigo`; `licenseHeaderFile(rootProject.file("header-license"), "package ")` (delimiter set explicitly to `package ` to match Maven's `<delimiter>`, rather than relying on Spotless's broader built-in default `(package|import|public|class|module) `); `lineEndings = LineEnding.UNIX` at the extension level
- Anchor both config files (`asapp_formatter.xml`, `header-license`) with `rootProject.file(...)` — the convention plugin applies per-subproject, so a bare string would resolve against each module's dir instead of the repo root
- `removeUnusedImports("cleanthat-javaparser-unnecessaryimport")` — a deliberate divergence from Maven's default google-java-format engine, which needs daemon-wide `--add-exports jdk.compiler/…` on JDK 16+ that the project's conventions forbid; cleanthat needs none
- Keep `spotlessCheck` wired into `check` (the Gradle default) — a deliberate divergence from Maven's default-skip, faithful to the `ci`-profile intent that gated formatting on every CI build; skip it for a fast local loop with `./gradlew build -x spotlessCheck` (the Gradle analog of Maven's local `spotless.check.skip=true` default)
- `./gradlew spotlessApply` replaces `mvn spotless:apply`; `./gradlew spotlessCheck` replaces `mvn spotless:check`

## Mutation testing

- Put the `info.solidsoft.pitest` plugin on the `build-logic` classpath (catalog `gradle-pitest-plugin` library + `implementation(libs.gradle.pitest.plugin)` in `build-logic/build.gradle.kts`), then apply it with a versionless `id("info.solidsoft.pitest")` **only** in `asapp.domain-service-conventions` (the 3 domain services) — libs and infra services get no `pitest` task, the flag-free analog of Maven's `<skip>`
- Pin both versions from the catalog: `pitestVersion` from `pitest` (`1.25.8`, for official Java 25 / ASM 9.10.1 support) and `junit5PluginVersion` from `pitest-junit5-plugin` (`1.2.3`) — the plugin's default PIT (`1.22.1`) would otherwise drift with plugin upgrades
- `junit5PluginVersion` puts the JUnit 5 bridge on the plugin's own `pitest` configuration and sets `testPlugin=junit5` — **never** declare `pitest-junit5-plugin` on `testImplementation` (it belongs on the mutation tool's classpath, not the test compile classpath)
- Set `addJUnitPlatformLauncher = false` — the plugin's experimental launcher auto-add can't map JUnit 6's `6.0.x` platform versioning and injects an unaligned `junit-platform-launcher` (`1.12.2`) onto the coverage-minion classpath, crashing discovery with `OutputDirectoryCreator not available`; disabling it leaves the BOM-aligned `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` (from `asapp.java-conventions`) as the minion's only launcher
- Set `mutationThreshold = 100` in `asapp.domain-service-conventions` (Maven parity — the build fails below 100% mutation kill)
- Pin `jvmPath = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }.map { it.executablePath }` (lazy `.map`, not `.get()`, so the toolchain resolves only when `pitest` runs, not on every configuration) — the plugin otherwise forks PIT on the Gradle daemon JVM, not the toolchain (szpak/gradle-pitest-plugin#301), which fails on Java 25 bytecode when the daemon runs an older JDK
- `timestampedReports = false` for a stable `build/reports/pitest` path
- Per-service `targetClasses` / `targetTests` (`com.attrigo.asapp.<svc>.domain.*` + `…application.*.in.service.*`) live in each service's build script, not the convention plugin — they are per-service data, not shared policy
- Keep the `pitest` task off the `check`/`build` path (run `./gradlew pitest`) — Maven bound PIT to no phase

## API documentation

- Put the `org.asciidoctor.jvm.convert` plugin on the `build-logic` classpath (catalog `asciidoctor-gradle` version + `asciidoctor-gradle-plugin` library + `implementation(libs.asciidoctor.gradle.plugin)` in `build-logic/build.gradle.kts`), then apply it with a versionless `id("org.asciidoctor.jvm.convert")` **only** in `asapp.domain-service-conventions` (the 3 API-documented domain services — authentication, tasks, users) — libs and infra services get no `asciidoctor` task, the flag-free analog of Maven's per-service plugin activation
- Pin the plugin from the catalog (`asciidoctor-gradle = "4.0.5"`) — runs on Gradle 9.x with only a benign transitive-Grolifant `StartParameter.isConfigurationCacheRequested` deprecation warning
- Pin AsciidoctorJ to `3.0.0` via `asciidoctorj { setVersion(libs.findVersion("asciidoctorj").get().requiredVersion) }` — the plugin's default `2.5.7` predates the `Preprocessor.process(Document, PreprocessorReader)` signature that `spring-restdocs-asciidoctor` 4.0.0 is compiled against, so conversion dies with `AbstractMethodError`; `3.0.0` also ships JRuby 9.4.x, off 2.5.7's CVE-bearing JRuby. Use `setVersion(...)`, **not** `version = …` — the extension exposes `version` as a read-only val in Kotlin, so assignment doesn't compile inside the precompiled convention plugin
- `spring-restdocs-asciidoctor` (catalog version reused from `spring-restdocs`) rides a custom `asciidoctorExt` configuration and **must** be activated on the task via `configurations("asciidoctorExt")` — it supplies the `operation::` block macro and the `snippets` attribute; omit the activation and both silently fail
- Snippets come from the `*ApiDocumentationIT` tests in the `integrationTest` tier (`RestDocumentationExtension` is no-arg, so there is no test-source change) — wire `asciidoctor.dependsOn(integrationTest)` with `integrationTest.outputs.dir(snippetsDir)` / `asciidoctor.inputs.dir(snippetsDir)` for producer→consumer up-to-date tracking
- Snippets currently resolve to `target/generated-snippets`, not `build/generated-snippets` — a temporary Maven-coexistence artifact: REST Docs' `ManualRestDocumentation` detects the still-present `pom.xml` and writes to `target/`, while `spring-restdocs-asciidoctor` defaults the `snippets` attribute to `build/` (it detects Gradle via an unset `maven.home`); reconcile by pointing `snippetsDir` at `target/generated-snippets` **and** setting the `snippets` attribute explicitly to that path. Once Maven is removed (no `pom.xml`), both converge on `build/generated-snippets`: revert `snippetsDir` to `layout.buildDirectory.dir("generated-snippets")` and drop the explicit `snippets` attribute (tracked in the Maven-removal subtask)
- Output is the plugin default `build/docs/asciidoc/api-guide.html` (single-backend `html5`, no subfolder) — no backend/doctype override, `:doctype: book` already lives in the `.adoc` header
- Keep the `asciidoctor` task off the `check`/`build` path — opt in via `./gradlew asciidoctor` (or `:services:<svc>:asciidoctor`), matching the coverage/pitest reports; it replaces `mvn asciidoctor:process-asciidoc@generate-docs`

## Javadoc & sources jars

- Register plain `javadocJar` / `sourcesJar` `Jar` tasks (classifiers `javadoc` / `sources`) directly in `asapp.library-conventions` **and** `asapp.domain-service-conventions` — the 5 modules Maven activates these plugins in (the 2 libs + the 3 domain services authentication, tasks, users); config/discovery (service-conventions only) get nothing, the flag-free analog of Maven's per-module `<build><plugins>` re-declaration
- The block is repeated in both archetype plugins **by design** — javadoc/sources spans two archetypes with no shared node (the common ancestor `asapp.java-conventions` is all 7 and would wrongly include the infra services), so it stays on the module-archetype axis rather than adding a concern plugin (cf. the duplicated version-catalog accessor); **never** put it in `asapp.java-conventions` (breaks parity and doc-lints never-linted infra source)
- **Never** `java.withJavadocJar()` / `withSourcesJar()` — those register the jars as documentation variants **and add them as a dependency of `assemble`**, forcing javadoc onto every `./gradlew build`; plain `Jar` tasks keep generation opt-in and off the default path (Maven's `-Pfull` parity)
- Core Gradle only (`java` / `java-library` plugin) — no `build-logic` classpath entry, no catalog version/library, no `build-logic/build.gradle.kts` change (unlike asciidoctor/pitest/spotless)
- `javadocJar` packages `from(tasks.named("javadoc"))` (replaces `maven-javadoc-plugin:javadoc-no-fork`); `sourcesJar` packages `project.the<SourceSetContainer>()["main"].allSource` (replaces `maven-source-plugin:jar-no-fork`) — jars land in `build/libs/<name>-<version>-{javadoc,sources}.jar`
- Set doclint on the `javadoc` task with `(options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)` → `-Xdoclint:all,-missing`, matching Maven's `<doclint>all,-missing</doclint>` (report every javadoc problem except missing comments); the `javadoc` task runs on the `asapp.java-conventions` toolchain (JDK 25) automatically
- Keep both tasks off the `check`/`build`/`assemble` path — opt in via `./gradlew javadocJar sourcesJar` (or `:services:<svc>:javadocJar`), matching the coverage/pitest/asciidoctor reports; replaces the `-Pfull`-gated `maven.javadoc.skip` / `maven.source.skip` executions
- MapStruct-generated `*MapperImpl` classes are not javadoc'd (Gradle's `javadoc` sources from `main.allJava`, which excludes annotation-processor output) — expected, matching the "Add Javadoc to mapper implementations" backlog gap, not a regression

## Ordering

Outer **scope** comment, inner **origin** comment, alphabetical within each origin. Reordering never changes a value, coordinate, or key.

- **Scope** groups: `Build`, `BOM`, `Compile`, `Runtime`, `Test`, `CVE` — `Build` holds build-logic/plugin catalog entries; the rest mirror Maven scopes
- **Origin** order: `ASAPP`, `Spring Boot`, `Spring Cloud`, `Spring`, `Org`, `Other`
- **Version catalog** (`libs.versions.toml`): scope with `#`, origin with `##`; keep the `jackson-bom` override under `# Overrides SB4 BOM`
- **Dependency blocks** (`*.gradle.kts`): scope then origin with `//`; the `CVE` `constraints {}` leads `dependencies {}`; put each `annotationProcessor(...)` right after its `implementation(...)` (e.g. `mapstruct-processor` after `mapstruct`)
- **`gradle.properties`**: identity keys (`group`, `version`) first, then `org.gradle.*`, alphabetical within each group
