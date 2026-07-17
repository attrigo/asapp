# Gradle dependency management — design spec

**Date**: 2026-07-16
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate dependency management to Gradle"
**Scope**: Every Maven `<dependency>`/`<dependencyManagement>` coordinate, version, and BOM import gets a Gradle equivalent — a version catalog, BOM imports, convention-plugin dependency blocks, and leaf-module `dependencies {}` blocks. No compiler/toolchain config, no Maven `<build><plugins>` migration, no `pom.xml` edits.

## 1. Context

The previous subtask ("Set up the Gradle project and module structure", `docs/superpowers/specs/v0.5.0/2026-07-16-gradle-project-module-structure-design.md`) stood up an empty Gradle skeleton: root `settings.gradle.kts`/`build.gradle.kts`, a `build-logic` composite build with three comment-only convention-plugin stubs (`asapp.java-conventions`, `asapp.library-conventions`, `asapp.service-conventions`), and 7 empty leaf `build.gradle.kts` files. Maven still builds the project unmodified.

Today, dependency management is spread across:
- Root `pom.xml`: `spring-boot-starter-parent` inheritance (gives every module, libs included, Spring Boot's version management transitively), an `archunit-junit5`/`pitest-junit5-plugin` dependencyManagement, and a Jackson CVE-override version property.
- `services/pom.xml` (aggregator, all 5 services): imports the `spring-cloud-dependencies` BOM, manages ~20 individually-versioned libraries, and manages 5 CVE-only version overrides (`guava`, `commons-beanutils`, `commons-io`, `bcpkix-jdk18on`, `rhino`) for libraries no leaf POM depends on directly — pure transitive-version pins against Spring Cloud/Eureka's Netflix dependency stack.
- 7 leaf POMs: each declares its own direct dependencies, inheriting versions from the two aggregators above.

This spec covers migrating all of that to Gradle: a version catalog for versions, BOM imports for what Spring/Spring Cloud manage, and dependency blocks in the right convention plugin or leaf module.

## 2. Goals

- Every current Maven dependency coordinate resolves in Gradle with the same effective version.
- BOM-managed versions (Spring Boot, Spring Cloud) stay centrally upgradable — no per-module version pins for artifacts the BOMs already manage.
- Dependency declarations live at the same "altitude" they do in Maven today: shared-and-identical → convention plugin; leaf-specific or varying → leaf `build.gradle.kts`.
- `./gradlew :module:dependencies` resolves cleanly for all 7 modules, with no version conflicts.

## 3. Non-goals

- Java toolchain, `release` version, compiler args, MapStruct annotation-processor compiler flags — next subtask ("Migrate compilation to Gradle").
- Any Maven `<build><plugins>` entry — jacoco, spotless, pitest, asciidoctor, cyclonedx, git-build-hook, git-commit-id, liquibase, spring-boot-maven-plugin. Each maps to its own later-listed TODO subtask (coverage, mutation testing, formatting, docs, packaging, git hooks).
- `compileJava`/`compileTestJava` actually succeeding — the `java`/`java-library` plugins are applied bare (no toolchain config), so real compilation isn't expected to work until the next subtask lands.
- Any `pom.xml` edit. Maven keeps building unchanged until the final "verify full parity, then remove Maven entirely" subtask.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| BOM/dependency management mechanism | **`io.spring.dependency-management` plugin** | Closer Maven parity than Gradle's native `platform()` — explicit `mavenBom` imports, property-style version overrides — and avoids relying on Gradle's implicit highest-version-wins resolution for a security-sensitive override (Jackson CVE fix). |
| Where the Spring Boot BOM import lives | `asapp.java-conventions` (base plugin, all 7 modules) | Mirrors today's reality: every module, libs included, inherits `spring-boot-starter-parent`'s version management via the parent chain. |
| Where the Spring Cloud BOM import lives | `asapp.service-conventions` (all 5 services) | Needed by all 5 — even `config`/`discovery` depend on BOM-versioned artifacts (`spring-cloud-config-server`, `spring-cloud-starter-netflix-eureka-server`) despite not being Eureka/Config *clients* themselves. |
| `java` vs `java-library` plugin | `asapp.java-conventions` applies bare `java` (only enough for `implementation`/`testImplementation` configurations to exist — no toolchain/release config); `asapp.library-conventions` upgrades the 2 libs to `java-library` for the `api`/`implementation` split | Reuses the taxonomy the module-structure subtask already stubbed. Services are leaf consumers with no downstream consumers of their own, so they never need `api`. |
| Maven `<optional>true</optional>` → Gradle | Plain `implementation` (not `api`) | Gradle's `implementation` is already non-transitive to consumers by default — Maven needed the `optional` flag to get that; Gradle doesn't need an equivalent. |
| Inter-module deps (`asapp-commons-url`, `asapp-http-clients`) | Gradle project dependencies: `project(":libs:asapp-commons-url")` | Replaces Maven's `groupId:artifactId:${project.version}` coordinate. No version needed for same-build project references. |
| CVE version pins (`guava`, `commons-beanutils`, `commons-io`, `bcpkix-jdk18on`, `bcprov-jdk18on`, `rhino`) | Gradle `constraints { }` block in `asapp.service-conventions`, applied to all 5 services | These force safe versions of artifacts pulled in transitively by Spring Cloud/Eureka's Netflix stack — a Gradle dependency constraint is the direct equivalent of a Maven dependencyManagement entry with no matching direct dependency. `bcprov-jdk18on` is the one exception that's *also* directly declared — `authentication-service` needs it explicitly for `PasswordEncoderFactories` — so it gets both the shared constraint (this row) and its own leaf-level `implementation` declaration (§6). |
| New convention plugin: `asapp.domain-service-conventions` | Applied to `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`, layered on top of `asapp.service-conventions` | These 3 share a large, identical dependency set (persistence stack, MapStruct, springdoc, Eureka/Config client starters, the full integration-test toolchain) that `config`/`discovery` — pure platform servers — don't need at all. Capturing it avoids tripling the same ~15 dependency declarations across 3 leaf files. |
| Placement principle for everything else | A dependency identical in coordinate *and* configuration (scope) across every module in a group (all-7 / all-libs / all-5-services / the-3-domain-services) goes in the matching convention plugin; anything varying per module (different scope, or only present in some modules) stays declared at the leaf `build.gradle.kts` | Keeps convention plugins faithful to what's actually shared. Existing inconsistencies (e.g. `postgresql` is `compile`-scope in `asapp-authentication-service` but `runtime`-scope in `asapp-tasks-service`/`asapp-users-service`, per the `JdbcConversionsConfiguration` comment) are preserved as-is, not silently normalized. |

## 5. Version catalog

A single root `gradle/libs.versions.toml`, with `[versions]` and `[libraries]` tables organized into the same comment-delimited groups the POMs already use (Compile / Test / CVE, further split by origin), so the mapping stays easy to audit against the current Maven files.

**In the catalog** (needs an explicit version): the two BOM coordinates (`spring-boot-dependencies`, `spring-cloud-dependencies`); `mapstruct`; `springdoc-openapi-starter-webmvc-ui`; `bootui-spring-boot-starter`; `nimbus-jose-jwt`; `resilience4j-spring-boot4`; `spring-restdocs-mockmvc`; `mockserver-client-java`; `mockserver-netty`; `testcontainers-junit-jupiter`/`-mockserver`/`-postgresql`; `testcontainers-redis`; `json-unit-assertj`; `bcpkix-jdk18on`; `bcprov-jdk18on`; `rhino`; `guava`; `commons-beanutils`; `commons-io`; `archunit-junit5`; `pitest-junit5-plugin`; `jackson-databind` (the `asapp-http-clients` test-only pin, carried over unchanged — it doesn't match the root Jackson override, which is existing behavior this migration doesn't edit); and the `jackson-bom` CVE-override coordinate/version.

**Not in the catalog:** `spring-boot-starter-*` and `spring-cloud-starter-*` artifacts have no version of their own — the BOM manages them — so they're written as plain coordinate strings (e.g. `"org.springframework.boot:spring-boot-starter-webmvc"`) directly in whichever `dependencies {}` block needs them, rather than given catalog entries that would carry no version information.

## 6. Convention plugin content plan

- **`asapp.java-conventions`** (all 7 modules): bare `java` plugin, `io.spring.dependency-management` plugin, Spring Boot BOM import, Jackson CVE-override property. No shared dependency declarations — every module's actual dependency list differs.
- **`asapp.library-conventions`** (`asapp-commons-url`, `asapp-http-clients`): adds `java-library` (upgrading from `java`). No shared dependencies — neither lib shares a common dependency with the other today.
- **`asapp.service-conventions`** (all 5 services): Spring Cloud BOM import, the CVE `constraints {}` block (`guava`, `commons-beanutils`, `commons-io`, `bcpkix-jdk18on`, `bcprov-jdk18on`, `rhino`), and what's identical across all 5: `spring-boot-starter-actuator`/`-security` (+ their `-test` variants), `spring-boot-starter-webmvc-test` (test-only — `config`/`discovery` pull web support in transitively via Config Server/Eureka Server and only need it for testing, with no main `spring-boot-starter-webmvc` of their own), `spring-boot-devtools` (runtime, optional), `json-unit-assertj` (test). `micrometer-registry-prometheus` stays at leaf level — its scope varies today (explicit `runtime` in `config`/`discovery`, implicit `compile` in `tasks`/`authentication`/`users`) so centralizing it would silently normalize an existing inconsistency.
- **`asapp.domain-service-conventions`** (`asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`; applied alongside `asapp.service-conventions`): `spring-cloud-starter-config`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-data-jdbc`/`-data-redis`/`-liquibase`/`-validation` (+ `-test` variants), the main (non-test) `spring-boot-starter-webmvc` only (its `-test` variant is already covered by `asapp.service-conventions` above), `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `nimbus-jose-jwt`, `bootui-spring-boot-starter` (runtime), the `asapp-commons-url` project dependency, and the shared integration-test set (`spring-restdocs-mockmvc`, `spring-boot-testcontainers`, `pitest-junit5-plugin`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `testcontainers-redis`, `archunit-junit5`).
- **Leaf `build.gradle.kts`** (all 7): everything that still varies even within its group — e.g. `postgresql` (different scope between `authentication-service` and `tasks-service`/`users-service`), `bcprov-jdk18on` (only `authentication-service`, with its `PasswordEncoderFactories` comment preserved), and every `users-service`-only dependency (`asapp-http-clients` project dep, `spring-boot-starter-aspectj`, `spring-boot-starter-restclient`, `spring-cloud-starter-loadbalancer`, `resilience4j-spring-boot4`, `mockserver-client-java`/`-netty`, `testcontainers-mockserver`).

## 7. Verification / Definition of Done

For every one of the 7 modules, `./gradlew :module:dependencies` resolves cleanly: every coordinate is found, BOM-managed versions are consistent, and there are no unresolved version conflicts — with only the bare `java`/`java-library` plugin applied (no toolchain/`release` config). `compileJava`/`compileTestJava` succeeding is **not** a criterion here; that's the next subtask's job.

`mvn clean install` must still succeed unchanged throughout — Maven and Gradle coexist until the final "remove Maven" subtask.

## 8. Out of scope / YAGNI

Java toolchain and compiler configuration · every Maven `<build><plugins>` entry (jacoco, spotless, pitest, asciidoctor, cyclonedx, git-build-hook, git-commit-id, liquibase, spring-boot-maven-plugin) · testing/coverage/mutation-testing/formatting/docs/packaging/Docker/git-hook/CI/release changes · any `pom.xml` edit.

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-2-dependencies`. Suggested commit slicing:

1. `build(gradle): add version catalog`
2. `build(gradle): add java and library convention plugin dependencies`
3. `build(gradle): add service and domain-service convention plugin dependencies`
4. `build(gradle): add leaf module dependency declarations`

## 10. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-07-16-gradle-dependency-management.md`) were written before implementation. The core change shipped substantially as designed — a single root `gradle/libs.versions.toml` version catalog, Spring Boot + Spring Cloud BOMs imported via the `io.spring.dependency-management` plugin (with the Jackson CVE override applied as a `bomProperty`), and a 4-layer convention-plugin stack (`asapp.java-conventions` → `asapp.library-conventions`/`asapp.service-conventions` → `asapp.domain-service-conventions`) with per-module dependency parity; Maven is untouched and still builds.

The canonical implementation is the current state of the Gradle build files on this branch — `gradle/libs.versions.toml`, `settings.gradle.kts`, `gradle.properties`, the `build-logic/` convention plugins, the leaf `build.gradle.kts` files, and `.claude/rules/gradle.md` — not this document.

**Notable deltas:**

- **`micrometer-registry-prometheus` hoisted into `asapp.service-conventions` as `runtimeOnly` (reverses §6).** §6 deliberately kept prometheus at leaf level to avoid normalizing an existing scope inconsistency. Shipped, it is declared once in `build-logic/src/main/kotlin/asapp.service-conventions.gradle.kts` for all five services and no leaf redeclares it — so the compile→runtimeOnly normalization §6 refused is now intentional.
- **`postgresql` hoisted into `asapp.domain-service-conventions` as `runtimeOnly` (reverses §4/§6 "stays at leaf level").** Shipped puts `runtimeOnly("org.postgresql:postgresql")` in `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts` (tasks/users common case) while `services/asapp-authentication-service/build.gradle.kts` re-adds `implementation("org.postgresql:postgresql")` to restore compile scope for `JdbcConversionsConfiguration`. Parity preserved via a leaf override on the hoisted default rather than pure-leaf placement.
- **`jackson-databind` test pin dropped, now BOM-managed (reverses §5 "carried over unchanged").** §5 said to carry the `asapp-http-clients` test-only 3.0.4 pin verbatim. Shipped, `gradle/libs.versions.toml` declares `jackson-databind` with no version and no `[versions]` entry, so the http-clients test dependency resolves through the overridden `jackson-bom` (3.1.1) — a real effective-version shift for that test scope.
- **`spring-web` scoped `compileOnly` on the library boundary (diverges from §4's `optional → implementation` mapping).** Maven marked spring-web `optional=true`; §4 maps that to plain `implementation`. Shipped, `libs/asapp-http-clients/build.gradle.kts` uses `compileOnly("org.springframework:spring-web")` plus `testImplementation` for the library's own tests — stronger than the designed mapping, keeping spring-web off consumers' classpath so each consumer brings its own.
- **`io.spring.dependency-management` plugin coordinate added to the catalog under a new `# Build` scope group (beyond §5's inventory).** §5 did not list the plugin. Shipped adds `spring-dependency-management` version + `spring-dependency-management-plugin` library to `gradle/libs.versions.toml` and consumes it from `build-logic/build.gradle.kts`, introducing a `# Build` catalog scope the Compile/Test/CVE taxonomy never named.
- **Root `settings.gradle.kts` gains a repository declaration and repo-mode hardening (design declared no repo for the main build).** Shipped adds `dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { mavenCentral() } }`. `mavenCentral()` was a necessary out-of-brief fix (no dependency resolution worked without it); `FAIL_ON_PROJECT_REPOS` is added hardening against leaf modules reintroducing per-project `repositories {}`.
- **`testcontainers-shared` catalog bundle added (§5/§6 listed the artifacts individually).** Shipped adds `[bundles] testcontainers-shared` to `gradle/libs.versions.toml` and consumes it via `libs.findBundle("testcontainers-shared")` in `asapp.domain-service-conventions.gradle.kts`; `testcontainers-mockserver` stays a users-service leaf dependency.
- **A durable ordering convention was codified in `.claude/rules/gradle.md` and applied throughout (extends §5's informal grouping).** §5 asked only for the POMs' `# Compile / # Test / # CVE` group comments. Shipped adds an "Ordering" section defining six scope groups (Build, BOM, Compile, Runtime, Test, CVE) and a fixed origin order (ASAPP, Spring Boot, Spring Cloud, Spring, Org, Other), applied as two-level `#`/`##` comments in `gradle/libs.versions.toml` and `//` comments in every dependency block, alphabetized within each origin group.
- **`gradle.properties` gains a temporary migration setting and repays a latent bug (not in the design).** Shipped adds `org.gradle.console=verbose` (a migration-time aid, to be removed when Maven is retired), moves an inline `#` comment off `org.gradle.parallel=false` onto its own line (fixing a value-corruption bug inherited from the earlier skeleton subtask), and alphabetizes the `org.gradle.*` keys.
- **Convention plugins resolve the catalog via `VersionCatalogsExtension`/`findLibrary` while leaf modules use type-safe `libs.*` accessors (implementation reality not anticipated by the design).** Precompiled script plugins in the `build-logic` included build cannot see the generated `libs` accessor, so `build-logic/src/main/kotlin/*.gradle.kts` use `extensions.getByType(VersionCatalogsExtension::class.java).named("libs")` + `findLibrary`/`findBundle`; leaf `build.gradle.kts` files use the generated accessors. Recorded so the split style reads as intentional.

For future Gradle dependency-management edits, treat these build files as the template; this spec is preserved as a record of the original design intent.
