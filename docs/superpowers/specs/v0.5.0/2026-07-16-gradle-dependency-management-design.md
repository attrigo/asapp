# Gradle dependency management — design spec

**Date**: 2026-07-16
**Status**: Draft
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
