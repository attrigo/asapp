---
paths:
  - "**/build.gradle.kts"
  - "**/settings.gradle.kts"
  - "**/src/main/kotlin/*.gradle.kts"
  - "**/gradle.properties"
  - "**/gradle-wrapper.properties"
  - "**/libs.versions.toml"
---

How the Gradle build is structured, where its dependencies and derived values come from, what runs when, how a build script is laid out and commented, and how a developer drives it — the constraints that fail quietly when broken. Covers every build script, `build-logic`'s included; the custom task types under `build-logic/src` are in `gradle-task-types.md`.

Sections: **build** — Convention plugins, Dependencies & configurations, Root project & settings, Values & paths, Task graph · **script style** — Block order, Comments · **driving it** — Developer workflow, Expected output.

## Convention plugins

- Cross-cutting config — compiler, tests, anything shared — only in a `build-logic` convention plugin; never a build script, `subprojects {}`, `allprojects {}` or `buildSrc`
- Every `build.gradle.kts` applies exactly one plugin: its archetype convention plugin (`root`, `library`, `service`, `domain-service`)
- Add a concern plugin only where two or more archetypes need the same config and no ancestor archetype reaches them all — otherwise it belongs in that ancestor
- A concern plugin covers one concern and applies the archetype plugin it builds on

## Dependencies & configurations

- BOMs enter `build-logic` through `platform(...)` alone — force and managed-version-wins semantics (`enforcedPlatform(...)`, `io.spring.dependency-management`) downgrade the `kotlin-stdlib` the Kotlin DSL compiles against
- Declare a configuration by role — `dependencyScope(...)` for what dependencies are declared into, `resolvable("<name>Classpath")` for what a task reads — never `create(...)` (eager) or `register(...)` (every role at once)

## Root project & settings

- Never register a root task that only fans out to the subprojects (e.g. `fullBuild`, `bootBuildImage`) — `./gradlew <task>` already runs it in every project that has one, so the root copy only makes `./gradlew :<task>` a silent success; a root task is legitimate only when it carries a real edge of its own (e.g. `ciBuild`)
- Never apply Gradle's built-in `base` plugin to the root project — nor `java` / `application`, which apply it transitively, nor any plugin registering a root `clean` / `check` / `assemble` / `build`: absent, a qualified `:clean` fails loudly with "not found"; registered, it succeeds while doing nothing
- No `repositoriesMode` in `build-logic/settings.gradle.kts`, deliberately — `FAIL_ON_PROJECT_REPOS` on an included build breaks IntelliJ import

## Values & paths

- Template from `project.name` only where a wrong value fails loudly — for instance `imageName` qualifies (a wrong name fails at push), pitest's `targetClasses` does not (a narrowed glob still clears its 100% *ratio* threshold)
- Never retype a version the toolchain can supply — read it back from `java.toolchain.languageVersion`
- Anchor repo-wide files with `layout.settingsDirectory`, per-module files with `layout.projectDirectory` (Liquibase's `searchPath`) — never `rootDir` or `rootProject.*`, which resolve the same today but reach across projects to do it

## Task graph

- Read a provider with `.map { }`, never `.get()` outside a `doLast` — an eager `.get()` resolves the toolchain, the build directory or another task's output at configuration time; the version catalog's `Optional.get()` is not a provider read
- Order tasks with `mustRunAfter`, never `shouldRunAfter` — Gradle drops a `shouldRunAfter` edge once the task's real dependencies are met, so the ordering silently stops holding the day `org.gradle.parallel` is enabled
- **Never** wire the `build-logic` tests to run automatically — `jar.dependsOn(test)` is a cycle Gradle refuses, and `jar.finalizedBy(test)` works but then runs them on every command
- Javadoc and sources jars are plain `Jar` tasks — never `java.withJavadocJar()` / `withSourcesJar()`, which add them to `assemble`

## Block order

- Build script order: `import`, `plugins`, declarations (catalog accessor first), dependency wiring (`extra`, `dependencyManagement`, `dependencies`), extension config, task config
- Settings script order: `pluginManagement` (Gradle requires it first), `rootProject.name`, `dependencyResolutionManagement`, `include(...)`
- Import only what Gradle's default imports do not provide — every `org.gradle.*` type resolves unimported in a script, a third-party one (Spotless, Asciidoctor, CycloneDX, Boot) does not
- One block per kind — never a second `dependencies` or `withType<Test>`
- `group` before `description` in a task registration, then a blank line before the body

The axes below are cosmetic — reordering changes no value.

| Block | Order by | Then alphabetical by |
| --- | --- | --- |
| `plugins` | origin alone — `Gradle` (the built-in), `ASAPP`, `Spring`, `Other` | plugin id |
| catalog, every `dependencies` | scope, then origin — `ASAPP`, `Spring Boot`, `Spring Cloud`, `Spring`, `Other` | declared name: the catalog alias, or the coordinate's artifact id, never the group id |
| extension blocks | the `plugins` origins, since each block configures a plugin declared there — Gradle's own (`jacoco`, `java`, `normalization`) lead | extension name |
| task blocks | lifecycle phase, then the broader config (`withType` before `named` / `register`), then run order | — |

- Scopes: `Build` (build-logic's own entries), `Plugin` (plugin markers), `BOM`, `Compile`, `Runtime`, `Test`, `Tool` (a plugin's own classpath), `CVE`; `BOM` and `CVE` lead a dependency block
- Phases: `Clean`, `Compile`, `Test`, `Coverage`, `Documentation`, `Packaging`, `Developer commands`, `Aggregates`; a `dependsOn` list follows them too
- Label every group, the one holding a block's only entry included, and mark its level — `#` scope, `##` origin, in `plugins` and `constraints` blocks too, `#` section in the extension and task sections and `##` phase inside the task one; an unmarked comment is prose

## Comments

- Every block opens with one line saying what the block does — never a requirement, an instruction or one setting inside it; a task's own `description` is no substitute, the two serve different readers
- Inside a block, comment only what the line does not already say — which value and why, never that it is being set
- One note, above the line it explains and inside the block — never a `Label: explanation` header list above the block
- Opens with a third-person verb (`Pins…`, `Disables…`, `Reports…`), never a noun phrase (`The unit tier…`) or a bare instruction (`Run once per clone`), matching every task `description` and the KDoc under `build-logic/src`; imperative only for a warn-off aimed at whoever edits the line next
- Plain over complete — name the effect, not the mechanism behind it (`Pins Kotlin to the version Gradle itself uses`, not the BOM-and-compiler chain that makes it matter); reach for a second line only for a genuinely separate point
- A period only from the second sentence on
- Capitalize the first word unless it is an identifier the line uses (`environment is a MapProperty: …`)
- One line wherever it fits; wrap at 160 columns, the width `asapp_formatter.xml` sets for Java

## Developer workflow

- Run `./gradlew build`, never `./gradlew :build` — with no `build` task on the root project, Gradle reads the name as an abbreviation and runs `:buildEnvironment` instead: green, and nothing built
- Every wrapper upgrade **must** pass `--gradle-distribution-sha256-sum <sha256>` — `./gradlew wrapper` re-emits the properties file, so a bump otherwise deletes the distribution's only integrity check
- Never run `spotlessInstallGitPrePushHook`, a task the Spotless plugin registers — it writes its own `pre-push` into the hooks directory `installGitHooks` manages from the tracked `git/hooks/`
- Never run two Gradle builds against one working tree — they race Gradle's unlocked test-results writer and fail any `Test` task with `EOFException` or `NoSuchFileException`; give each parallel agent its own worktree
- The `build-logic` tests never run on their own — `./gradlew build` skips them. Run them by full name, `./gradlew :build-logic:check`, which also validates the custom task class

## Expected output

- Never pass `--warning-mode=none` — the `incompatible with Gradle 10` banner covers two upstream deprecations (Liquibase, Asciidoctor's grolifant) and nothing else, so a third one would be ours
- Leave the problems report's javac `unknown enum constant When.MAYBE` on the authentication service's `compileJava` — it is upstream's; never silence it with a `jsr305` `compileOnly` or `-Xlint:-classfile`

## Further reading

- Full rationale for every decision here — measurements, rejected alternatives, upstream issues — lives in the migration design specs under `docs/superpowers/specs/v0.5.0/`
- Gradle's [Best Practices for Structuring Builds](https://docs.gradle.org/current/userguide/structuring_software_products.html) on what belongs at the root versus in a subproject
