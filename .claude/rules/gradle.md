# Gradle Build Conventions

## Version catalog — `gradle/libs.versions.toml`

Single source of truth for versions. Within every table (`[versions]`, `[libraries]`, `[bundles]`, `[plugins]`), keep the POM-style comment groups and sort alphabetically **within** each group.

## Convention plugins — `build-logic/src/main/kotlin/`

Four tiers, chained `java` → `library` / `service` → `domain-service`:

- `asapp.java-conventions` — all modules: Java 25 toolchain, UTF-8, `-parameters`, Spring Boot BOM `platform()`, Spotless, JaCoCo, `test` (`*Tests`) + `integrationTest` (`*IT`/`*E2EIT`) tasks.
- `asapp.library-conventions` — the two libs: + javadoc/sources jars.
- `asapp.service-conventions` — all five services: + Spring Boot app, Spring Cloud BOM, Jackson + transitive CVE overrides, SBOM, git properties, `bootBuildImage`, `bootRun` (dev profile), plain jar disabled.
- `asapp.domain-service-conventions` — auth/tasks/users: + MapStruct, PIT (threshold 100), REST-docs asciidoctor, javadoc/sources jars.

A leaf `build.gradle.kts` = pick a tier + list its own dependencies (version-less, resolved from the catalog or the imported BOMs).
