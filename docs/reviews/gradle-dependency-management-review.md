# Task Review — Migrate dependency management to Gradle

`main...HEAD` · 24 changed files

> Apply-now findings only — deferred findings were routed to `TODO.md`. Code review only; nothing has been committed.

0 must-fix · 4 should-fix · 6 nice-to-have

## Should-fix

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| S1 | `spring-web` loses Maven `optional` contract in http-clients | issue | S | Med |
| S2 | Prometheus registry duplicated across all five services | improvement | S | Med |
| S3 | Postgres driver duplicated across the three domain services | improvement | S | Med |
| S4 | dependency-management plugin version hardcoded, bypasses catalog | improvement | S | Low |

- [x] **S1 — `spring-web` loses Maven `optional` contract in http-clients**
    - **Location:** `libs/asapp-http-clients/build.gradle.kts:10` (vs `libs/asapp-http-clients/pom.xml:36-40`)
    - **Description:** In this `java-library`, `implementation("org.springframework:spring-web")` still puts spring-web on every consumer's runtime classpath, unlike the POM's `optional=true` (consumer brings its own).
    - **Why it matters:** On a library boundary the `api`/`implementation`/`compileOnly` choice *is* the published contract; a future consumer without a web starter would silently receive spring-web transitively where Maven required an explicit declaration.
    - **Evidence:** `TasksHttpClient` is a public `@HttpExchange` interface exposing spring-web types; the POM marked spring-web `optional`. No breakage today only because the sole consumer (users-service) already brings spring-web via its own starters.
    - **Recommended action:** Change to `compileOnly("org.springframework:spring-web")` **and** add `testImplementation("org.springframework:spring-web")` — the module's own tests use `RestClient`/`RestClientAdapter`/`HttpServiceProxyFactory`, and `compileOnly` does not extend to the test classpath.
    - **Resolver notes:** Confirm no other consumer of `asapp-http-clients` relies on spring-web arriving transitively before merging.
    - **Applied:** Changed spring-web from `implementation` to `compileOnly` and added `testImplementation("org.springframework:spring-web")` in `libs/asapp-http-clients/build.gradle.kts`, reproducing the Maven `optional=true` contract. Test entry ordered Spring-before-Spring-Boot per the codebase convention. Sole consumer (users-service) unaffected — it brings spring-web via its own starters.

- [x] **S2 — Prometheus registry duplicated across all five services**
    - **Location:** `services/asapp-authentication-service/build.gradle.kts:13`, `services/asapp-tasks-service/build.gradle.kts:8`, `services/asapp-users-service/build.gradle.kts:16` (`implementation`); `services/asapp-config-service/build.gradle.kts:12`, `services/asapp-discovery-service/build.gradle.kts:12` (`runtimeOnly`)
    - **Description:** `micrometer-registry-prometheus` is declared in every service leaf instead of once in the shared `asapp.service-conventions` plugin — and with inconsistent scopes (3× `implementation`, 2× `runtimeOnly`).
    - **Why it matters:** A dependency required by 100% of a plugin's children belongs at that common ancestor; five copies invite scope/version skew as services are added.
    - **Recommended action:** Hoist `runtimeOnly("io.micrometer:micrometer-registry-prometheus")` into `asapp.service-conventions` and delete the five leaf declarations.
    - **Resolver notes:** It is an auto-configured runtime registry — verify the three `implementation` declarations have no compile-time reference, then collapse all five to a single `runtimeOnly`.
    - **Applied:** Hoisted a single `runtimeOnly("io.micrometer:micrometer-registry-prometheus")` into `asapp.service-conventions` (new `// Other` sub-group of `// Runtime`) and deleted all five leaf declarations, cleaning up orphaned scope/origin comments. Confirmed no `src/main` code references micrometer — all usage is auto-configured Actuator runtime wiring.

- [x] **S3 — Postgres driver duplicated across the three domain services**
    - **Location:** `services/asapp-authentication-service/build.gradle.kts:11` (`implementation`), `services/asapp-tasks-service/build.gradle.kts:12` (`runtimeOnly`), `services/asapp-users-service/build.gradle.kts:20` (`runtimeOnly`)
    - **Description:** The Postgres JDBC driver is declared in each data-backed service rather than in their common `asapp.domain-service-conventions` parent.
    - **Why it matters:** All three (and only these three) children of `domain-service-conventions` need it — a textbook pull-up to the "data-backed microservice" tier.
    - **Recommended action:** Hoist `runtimeOnly("org.postgresql:postgresql")` into `asapp.domain-service-conventions`; keep auth's `implementation` as a documented compile-scope override (it composes to compile+runtime).
    - **Resolver notes:** auth needs compile scope for `JdbcConversionsConfiguration` (per its existing comment); a `runtimeOnly` hoist plus auth's `implementation` override composes correctly.
    - **Applied:** Hoisted `runtimeOnly("org.postgresql:postgresql")` into `asapp.domain-service-conventions` (`// Runtime` → new `// Org`); removed the tasks and users leaf declarations (tasks' now-empty `dependencies {}` block dropped); kept auth's compile-scope `implementation`. Verified via `dependencyInsight` that auth's runtimeClasspath resolves postgresql to a single node at 42.7.10 (BOM-selected) — no conflict between the two declarations.

- [x] **S4 — dependency-management plugin version hardcoded, bypasses the catalog**
    - **Location:** `build-logic/build.gradle.kts:13`
    - **Description:** `io.spring.gradle:dependency-management-plugin:1.1.7` pins a version inline, though the catalog is already imported into the build-logic build.
    - **Why it matters:** It is the only dependency version living outside `gradle/libs.versions.toml`, breaking the single-source-of-truth the rest of the setup upholds.
    - **Recommended action:** Add a catalog entry (version + library) for the plugin and reference it from `build-logic/build.gradle.kts`.
    - **Resolver notes:** `libs` is available in the build-logic build via `build-logic/settings.gradle.kts:6-8` (`from(files("../gradle/libs.versions.toml"))`). Low blast radius — consistency, not correctness.
    - **Applied:** Added a new `# Build` / `## Spring` scope group at the top of the catalog's `[versions]` (`spring-dependency-management = "1.1.7"`) and `[libraries]` (`spring-dependency-management-plugin`), referenced it from `build-logic/build.gradle.kts` via the type-safe accessor `libs.spring.dependency.management.plugin`, and documented the `Build` scope in `.claude/rules/gradle.md`. Verified with `./gradlew help` that build-logic re-evaluates and the accessor resolves. (`[libraries]`, not `[plugins]` — the artifact is consumed as an `implementation` classpath dependency, not applied via `plugins { alias }`.)

## Nice-to-have

| ID | Title | Kind | Effort | Impact |
|----|-------|------|--------|--------|
| N1 | BOM-mechanism rationale (why not native `platform()`) undocumented | improvement | S | Med |
| N2 | `repositoriesMode` unset — repository centralization not enforced | improvement | S | Low-Med |
| N3 | `gradle.properties` key-ordering nit vs own rule | issue | S | Low |
| N4 | `gradle.md` Origin-order rule contradicts the code/POMs | issue | S | Low |
| N5 | Redundant `jackson-databind` pin conflicts with the managed BOM version | issue | S | Low |
| N6 | Testcontainers trio could be a catalog bundle | improvement | S | Low |

- [x] **N1 — BOM-mechanism rationale not recorded**
    - **Location:** `build-logic/src/main/kotlin/asapp.java-conventions.gradle.kts:11-17`
    - **Description:** The choice of `io.spring.dependency-management` over Gradle-native `platform()` is correct but its rationale is not written down.
    - **Why it matters:** A future maintainer could "modernize" to native `platform()` and silently lose the CVE fix — the `bomProperty("jackson-bom.version", …)` nested-BOM-property override cannot be expressed by `platform()` in one line (it would require enumerating every Jackson-family artifact as an individual constraint).
    - **Evidence:** Spring Boot Gradle plugin docs frame the plugin's distinguishing benefit as "property-based customization of managed versions", which native `platform()` does not support.
    - **Recommended action:** Add a one-line comment noting the `bomProperty` override is why the plugin is used over native `platform()`.
    - **Applied:** Added a one-line comment in `asapp.java-conventions.gradle.kts` above the `dependencyManagement` block: "Uses this plugin over Gradle-native platform() for the bomProperty override".

- [x] **N2 — `repositoriesMode` unset**
    - **Location:** `settings.gradle.kts:6-10`
    - **Description:** `dependencyResolutionManagement` does not set `repositoriesMode`, so it defaults to `PREFER_PROJECT` — a stray project-level `repositories {}` would silently override the central declaration instead of failing the build.
    - **Why it matters:** Erodes the single-source-of-truth for repositories over time; no violation exists today, so this is pure hardening.
    - **Evidence:** Gradle "Centralizing Repository Declarations" docs recommend `FAIL_ON_PROJECT_REPOS` for drift-proof, centralized declarations.
    - **Recommended action:** Add `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` inside `dependencyResolutionManagement`.
    - **Applied:** Added `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` (with a short comment) inside `dependencyResolutionManagement` in `settings.gradle.kts`. Confirmed via `./gradlew help` that it evaluates and no existing module declares project-level repositories.

- [x] **N3 — `gradle.properties` key-ordering nit**
    - **Location:** `gradle.properties:6-8`
    - **Description:** `.claude/rules/gradle.md` requires `org.gradle.*` keys sorted alphabetically, but the `configuration-cache` deferral comment sits after `parallel` (it should precede `console`).
    - **Why it matters:** Consistency/reviewability against the project's own stated convention; zero functional effect.
    - **Recommended action:** Move the `# org.gradle.configuration-cache …` comment to directly after `org.gradle.caching=true`.
    - **Applied:** Moved the `# org.gradle.configuration-cache …` deferral comment to its alphabetical position (between `caching` and `console`) in `gradle.properties`; the `parallel` explanatory comment stays paired with `parallel`. Pure reorder, no value change.

- [x] **N4 — `gradle.md` Origin-order rule contradicts the code**
    - **Location:** `.claude/rules/gradle.md` (Origin-order line) vs `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts:32-34` and every POM Test section (e.g. `services/asapp-authentication-service/pom.xml:114-121`)
    - **Description:** The rule fixes Origin order as "…Spring Boot, Spring Cloud, Spring…", but every POM's Test block places `spring-restdocs` (Spring) *before* the `*-test` starters (Spring Boot) — and the convention plugin correctly mirrors the POM, so the **doc** is the thing that is wrong.
    - **Why it matters:** A contributor following the literal rule would reorder correct code away from the POM-parity baseline.
    - **Recommended action:** Correct the rule to reflect that the Spring/Spring-Boot relative order follows the POM per scope (Test: Spring before Spring Boot), rather than asserting one universal fixed sequence.
    - **Applied:** Resolved the reverse way (per developer decision) — conformed the **code** to `gradle.md`'s universal origin order rather than changing the doc. Reordered the Test blocks in `asapp.domain-service-conventions.gradle.kts` and `libs/asapp-http-clients/build.gradle.kts` so `Spring Boot` precedes `Spring`. `gradle.md` and the POMs left unchanged; the Gradle code now deliberately diverges from the POM's Test-scope ordering.

- [x] **N5 — Redundant `jackson-databind` pin conflicts with the managed BOM version**
    - **Location:** `gradle/libs.versions.toml:28` (`jackson-databind = "3.0.4"`) and `:66` (library); vs the CVE-overridden `jackson-bom = "3.1.1"` at `:32`; consumed at `libs/asapp-http-clients/build.gradle.kts:16`
    - **Description:** The catalog pins `tools.jackson.core:jackson-databind` to 3.0.4 while the rest of the Jackson family is managed to 3.1.1 by the overridden Spring Boot BOM — two managed versions for one library family.
    - **Why it matters:** The explicit 3.0.4 pin is redundant at best and inconsistent with the CVE-fixed 3.1.1 the BOM enforces elsewhere; it undercuts the single-source-of-truth the catalog otherwise upholds.
    - **Evidence:** Pre-existing Maven carry-over — `jackson-databind` was pinned to 3.0.4 in `libs/asapp-http-clients/pom.xml` while the root POM overrode `jackson-bom` to 3.1.1.
    - **Recommended action:** Drop the explicit version — remove the `jackson-databind` entry from `[versions]` and the `version.ref` from the library so the overridden BOM (3.1.1) governs it.
    - **Resolver notes:** Test-only dependency (`testImplementation` in http-clients); after removing the pin, confirm the module's tests still compile/run against 3.1.1 (a minor bump). If it ever needs re-pinning, point the ref at `jackson-bom` rather than a separate version.
    - **Applied:** Removed the `jackson-databind = "3.0.4"` entry from `[versions]` and dropped `version.ref` from the `[libraries]` entry (now module-only), letting the overridden Spring Boot BOM govern it. Verified via `dependencyInsight` that it resolves to `3.1.1` (by constraint from `tools.jackson:jackson-bom:3.1.1`) and via `compileTestJava` that http-clients tests compile against `3.1.1`.

- [x] **N6 — Testcontainers trio could be a catalog bundle**
    - **Location:** `gradle/libs.versions.toml` (no `[bundles]` table) and the Test block of `build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts` (`testcontainers-junit-jupiter`, `testcontainers-postgresql`, `testcontainers-redis`)
    - **Description:** The three always-together testcontainers dependencies are declared as three separate `testImplementation` entries; a version-catalog `[bundle]` groups them under one alias.
    - **Why it matters:** A readability/maintainability win — one bundle reference instead of three parallel declarations that must be kept in sync.
    - **Recommended action:** Add a `[bundles]` entry (e.g. `testcontainers = ["testcontainers-junit-jupiter", "testcontainers-postgresql", "testcontainers-redis"]`) and replace the three separate `testImplementation(libs.findLibrary(...).get())` calls in `asapp.domain-service-conventions` with a single `testImplementation(libs.findBundle("testcontainers").get())`.
    - **Resolver notes:** Partial coverage by design — users-service additionally needs `testcontainers-mockserver`, which stays a separate leaf declaration and isn't part of the shared bundle. Optional polish; skip if the partial bundle reads as misleading.
    - **Applied:** Added a `[bundles]` table to the catalog with `testcontainers-shared = [junit-jupiter, postgresql, redis]` and replaced the three separate `testImplementation` calls in `asapp.domain-service-conventions` with one `findBundle("testcontainers-shared")` (under `// Org`). Named `-shared` (per developer decision) to signal the common subset and make the mockserver exclusion self-evident. `testcontainers-mockserver` stays a users-service leaf dep. Verified via `dependencies` that all three artifacts resolve on a domain service's test classpath.
