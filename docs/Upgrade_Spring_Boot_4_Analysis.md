# Upgrade Spring Boot 4 Analysis 

> **Date**: 2026-04-03  
> **Current state**: Spring Boot 3.4.3 / Java 21  
> **Target state**: Spring Boot 4.0.x / Java 25 (see Phase 10)

---

## 1. Executive Summary

Spring Boot 4 (released November 2025) is built on **Spring Framework 7**, **Spring Security 7**, **Jackson 3**, and **Jakarta EE 11**. The upgrade is achievable but non-trivial: most of the mechanical work (starter renames, property renames, test annotation updates, Jackson 3 migration) can be automated via **OpenRewrite**, while a smaller set of changes requires manual intervention — primarily security/testing adjustments and JJWT Jackson 3 compatibility.

This project is in a **favorable starting position**: it already uses Jakarta EE namespaces, `SecurityFilterChain`-based security, lambda-style DSL, `RestClient` (not `RestTemplate`), and Java 21 — all prerequisites that reduce migration effort significantly.

---

## 2. Spring Boot 4 — What Changed

Spring Boot 4.0 (released November 2025) is a major release that upgrades its entire dependency stack and introduces new capabilities.

### Dependency stack

| Dependency | Version |
|---|---|
| Spring Framework | 7.0 |
| Spring Security | 7.0 |
| Jackson | 3.x (`tools.jackson`) |
| Spring Data | 2025.1 |
| Jakarta EE | 11 |
| Minimum Java | 17 |
| Recommended Java | 25 (current LTS) |

### Requirements

| Requirement | Current | Required by SB 4 |
|---|---|---|
| Java | 21 | 17 minimum (21 or 25 LTS recommended) |
| Spring Framework | 6.2.x | 7.0 |
| Jakarta EE | 10 | 11 |
| Maven | 3.9.5 | 3.6.3+ |

### Breaking changes

| Area | Change |
|---|---|
| Auto-configuration | `spring-boot-autoconfigure` split into per-technology starters — e.g. `spring-boot-starter-web` → `spring-boot-starter-webmvc`; bare `liquibase-core` → `spring-boot-starter-liquibase` |
| Jackson | Group ID renamed `com.fasterxml.jackson` → `tools.jackson`; `ObjectMapper` → `JsonMapper`; customizer, annotation, and security module interfaces renamed |
| Tests | `@MockBean` / `@SpyBean` replaced by Mockito-native `@MockitoBean` / `@MockitoSpyBean`; `@SpringBootTest` no longer sets up MockMvc implicitly |
| Properties | Several `spring.*` and `management.*` keys restructured; `spring-boot-properties-migrator` reports all renames at startup |

### New capabilities

| Capability | Details |
|---|---|
| Spring Framework 7 | Removes long-deprecated APIs; JSpecify null-safety annotations across the portfolio; tightened Jakarta EE 11 baseline |
| Spring Security 7 | Streamlined authorization model; deprecated OAuth2/JWT adapter APIs removed |
| Spring Data 2025.1 | Repository abstraction refinements; improved query execution diagnostics |
| `@HttpExchange` auto-configuration | Declarative HTTP clients can now be auto-wired without manual `RestClient` bean setup |
| Built-in API versioning | `spring.mvc.apiversion.*` support for content-negotiation-based versioning in Spring MVC |
| First-class OpenTelemetry | New `spring-boot-starter-opentelemetry` starter for OTLP export — no Zipkin bridge required |

---

## 3. Key Risks

| Risk | Severity | Mitigation |
|---|---|---|
| `jjwt-jackson` with Jackson 3 | ~~High~~ Eliminated | `jjwt` replaced by `nimbus-jose-jwt` before the upgrade (see Prep step) — no Jackson dependency; same library Spring Security uses for its own JWT operations |
| Jackson 3 API changes across all services | Medium | Mostly automated by OpenRewrite `UpgradeJackson_2_3`; manual review of `JdbcConversionsConfiguration` required |
| `springdoc-openapi` SB4 incompatibility | Low | Automated by OpenRewrite `UpgradeSpringDoc_3_0` recipe |
| BouncyCastle + Spring Security 7 | Low | BouncyCastle 1.80 confirmed compatible with Spring Security 7.0.x |

---

## 4. Upgrade Strategy — Recommended Path (step-by-step) -> TODO: does this section fits analysis document or should be removed (currently present in the implementation plan document)

```
Spring Boot 3.4.3
      │
      ▼
[Phase 1] Target: Migrate jjwt to nimbus-jose-jwt
      │   Rationale: Eliminates Jackson 3 incompatibility before the upgrade — JWT behavior validated on stable SB3
      │
      ▼
[Phase 2] Target: Apply Spring Boot best practices
      │   Rationale: OpenRewrite cleans existing code before the upgrade — reduces noise in subsequent diffs
      │
      ▼
[Phase 3] Target: Upgrade to Spring Boot 3.5.x (zero deprecation warnings)
      │   Rationale: SB 3.5 preparation APIs surface everything that will be removed in SB 4
      │
      ▼
[Phase 4] Target: Upgrade to Spring Boot 4.0.x
      │   Rationale: OpenRewrite handles starters, properties, Jackson 3, Security 7, test annotations
      │
      ▼
[Phase 5] Target: Apply adjustments required by Spring Boot 4.0.x
      │   Rationale: Covers what OpenRewrite can't — Jackson property rename, property cleanup
      │
      ▼
Spring Boot 4.0.x ✓
      │
      ▼
[Phase 9] Target: Apply Spring Boot 4 idioms and upgrade dependencies
      │   Rationale: Structured logging, post-migration dep/plugin bumps
      │
      ▼
[Phase 10] Target: Upgrade to Java 25 LTS
      │   Rationale: SB4 has first-class Java 25 support — forward-looking upgrade
      │
      ▼
[Phase 11] Target: Upgrade Maven wrapper to 3.9.14
      │   Rationale: Wrapper JAR 3.2.0 → 3.3.4; distribution 3.9.5 → 3.9.14
      │
      ▼
Spring Boot 4.0.x / Java 25 / Maven 3.9.14 ✓
```

---

## 5. Impact Analysis — What Changes in This Project

### 5.1 POM Changes — Spring Boot Starters

> **Risk**: Low | **Automated**: Full (OpenRewrite `MigrateToModularStarters` + parent version bump)

**Purpose:** Spring Boot 4 modularizes `spring-boot-autoconfigure` — several previously bundled starters are split or renamed, and `liquibase-core` moves from a bare dependency to a dedicated starter.

**Scope:** Root `pom.xml`, all 3 service `pom.xml` files

**Current State → Target State (SB4):**

| Current | After Migration | Files |
|---|---|---|
| `spring-boot-starter-parent:3.4.3` | `spring-boot-starter-parent:4.0.x` | Root `pom.xml` |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | All 3 service POMs |
| `liquibase-core` (bare dep) | `spring-boot-starter-liquibase` | All 3 service POMs |

> BOM-managed test deps (`spring-boot-starter-test`, `spring-boot-testcontainers`, `spring-security-test`, `testcontainers:*`) are handled automatically by the SB4 BOM and OpenRewrite — no explicit POM changes required.

### 5.2 POM Changes — External Dependencies

> **Risk**: Medium | **Automated**: Partial (OpenRewrite handles SpringDoc; non-BOM test deps require manual version verification)

**Purpose:** Third-party dependencies used in compile/runtime and test scope that need version updates for SB4 compatibility. Only non-BOM-managed ones require explicit action in `services/pom.xml`.

**Scope:** `services/pom.xml` (manages versions centrally)

**Current State → Target State (SB4):**

| Artifact | Current | Scope | Services | How |
|---|---|---|---|---|
| `springdoc-openapi-starter-webmvc-ui` | `2.8.5` | compile | All 3 | Automated — OpenRewrite `UpgradeSpringDoc_3_0` → `3.x` |
| `com.redis:testcontainers-redis` | `2.2.4` | test | All 3 | Manual — not in TC BOM; verify or replace with `org.testcontainers:redis` |
| `net.javacrumbs.json-unit:json-unit-assertj` | `4.1.1` | test | All 3 | Manual — verify against SB4 test classpath |
| `org.mock-server:mockserver-client-java` | `5.15.0` | test | `asapp-users-service` | Manual — verify against TC 2.x |

**Manual Actions After Automatic Migration:**
- Verify `com.redis:testcontainers-redis:2.2.4` works after `Testcontainers2Migration` runs; if not, replace with `org.testcontainers:redis` — update version in `services/pom.xml`
- Verify `net.javacrumbs.json-unit:json-unit-assertj:4.1.1` compatibility with SB4 test classpath — update version in `services/pom.xml`
- Verify `org.mock-server:mockserver-client-java:5.15.0` compatibility with TC 2.x — update version in `services/pom.xml` (`asapp-users-service` only)

### 5.3 Application Properties

> **Risk**: Medium | **Automated**: Partial (OpenRewrite `SpringBootProperties_4_0` handles most renames; Jackson property requires manual update)

**Purpose:** Spring Boot 4 renames several property keys. The Jackson inclusion property follows a new naming scheme that must be updated manually in all 3 services.

**Scope:** All `application.properties` files across all 3 services

**Current State → Target State (SB4):**

| Property | Action | Services |
|---|---|---|
| `spring.jackson.default-property-inclusion=NON_NULL` | Rename → `spring.jackson.json.write.default-property-inclusion=NON_NULL` | All 3 |
| `management.endpoint.health.probes.enabled=true` | Remove — now the default in SB4; keeping it adds noise | All 3 |

**Manual Actions After Automatic Migration:**
- Update manually `spring.jackson.default-property-inclusion` in all 3 `application.properties` — the Jackson property rename is **not automated**
- Remove `management.endpoint.health.probes.enabled=true` from all 3 `application.properties`
- Add `spring-boot-properties-migrator` as a runtime dependency; run each service and capture `WARN` output for any remaining renames

### 5.4 Jackson 3

> **Risk**: High | **Automated**: Partial (OpenRewrite `UpgradeJackson_2_3` handles package/class renames; `JdbcConversionsConfiguration` and JJWT require manual verification)

**Purpose:** Jackson 3 changes the Maven group ID from `com.fasterxml.jackson` to `tools.jackson` and renames several core classes and methods. This is the largest API-level breaking change in SB4.

**Scope:** All services (transitive), `asapp-authentication-service/infrastructure/config/JdbcConversionsConfiguration.java` (direct `ObjectMapper` usage for PostgreSQL JSONB ↔ JWT claims conversion)

**Current State → Target State (SB4):**

| Element | Current | After Migration |
|---|---|---|
| Group ID | `com.fasterxml.jackson.*` | `tools.jackson.*` |
| Main mapper | `ObjectMapper` / `new ObjectMapper()` | `JsonMapper` / `JsonMapper.builder().build()` |
| Customizer | `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| Annotations | `@JsonComponent`, `@JsonMixin` | `@JacksonComponent`, `@JacksonMixin` |
| Security module | `SecurityJackson2Modules` | `SecurityJacksonModules` |
| JJWT integration | `jjwt-jackson:0.12.6` (ships with Jackson 2 internally) | N/A — replaced by `nimbus-jose-jwt` in Prep step |

**Manual Actions After Automatic Migration:**
- Verify `JdbcConversionsConfiguration` (JSONB ↔ JWT claims conversion in `asapp-authentication-service`) compiled and behaves correctly with Jackson 3 API

### 5.5 Spring Security 7

> **Risk**: Low | **Automated**: Full (OpenRewrite `UpgradeSpringSecurity_7_0`)

**Purpose:** Spring Security 7 removes several deprecated APIs from 6.x. This project uses a custom JWT filter (Nimbus, after Prep step) rather than the OAuth2 resource server DSL, which significantly limits the blast radius.

**Scope:** `SecurityConfiguration.java` in all 3 services, `JwtAuthenticationFilter` / `JwtVerifier` in `asapp-authentication-service`

**Current State:**
- Spring Security 6.x
- `SecurityFilterChain` + lambda DSL (already modern)
- `PasswordEncoderFactories` with BouncyCastle `bcprov-jdk18on:1.80`
- Custom `JwtAuthenticationFilter` / `JwtVerifier` using `nimbus-jose-jwt` (after Prep step)

**Target State (SB4):**
- Spring Security 7.x — no structural changes required in `SecurityConfiguration`
- `SecurityJackson2Modules` → `SecurityJacksonModules` (automated)

**Not Applicable To This Project:**
- JWT `typ` header auto-validation change — project uses Nimbus directly
- `BearerTokenAuthenticationFilter` deprecations — not in use
- `WebSecurityConfigurerAdapter` removal — already removed

**Manual Actions After Automatic Migration:**
- Verify `PasswordEncoderFactories` API surface with BouncyCastle 1.80 + Spring Security 7
- Test password encoding/verification end-to-end

### 5.6 Test Changes

> **Risk**: Medium | **Automated**: Partial (OpenRewrite `ReplaceMockBeanAndSpyBean` handles annotation renames; `@AutoConfigureMockMvc` additions require manual review)

**Purpose:** Spring Boot 4 removes `@MockBean` and `@SpyBean` in favour of Mockito-native equivalents. `@SpringBootTest` no longer sets up MockMvc implicitly — integration tests must add `@AutoConfigureMockMvc` explicitly.

**Scope:** All test classes across all 3 services (`*Test.java`, `*IT.java`)

**Current State → Target State (SB4):**

| Element | Current | After Migration | Scope |
|---|---|---|---|
| `@MockBean` | `@MockBean` | `@MockitoBean` | All 3 services — test classes |
| `@SpyBean` | `@SpyBean` | `@MockitoSpyBean` | All 3 services — test classes |
| MockMvc setup | `@SpringBootTest` (implicit) | `@SpringBootTest` + `@AutoConfigureMockMvc` (explicit) | Integration tests (`*IT.java`) |

**Manual Actions After Automatic Migration:**
- Review all `*IT.java` files to confirm `@AutoConfigureMockMvc` was correctly added — OpenRewrite may miss non-standard patterns
- If `MockitoTestExecutionListener` is used anywhere, replace with `MockitoExtension`

### 5.7 Actuator & Observability

> **Risk**: Low | **Automated**: Full (version bumps via Spring Boot BOM)

No Zipkin, OTLP, or custom tracing config detected. Changes relevant to this project:

| Change | Impact |
|---|---|
| `micrometer-registry-prometheus` artifact name | Verify against SB4 BOM |
| `management.tracing.enabled` → `management.tracing.export.enabled` | Not in use — no action |
| Liveness/readiness probes enabled by default | Already explicitly enabled in all 3 `application.properties` — no regression |

### 5.8 Spring Data JDBC

> **Risk**: Low | **Automated**: Full (version bump via Spring Boot BOM)

This project uses **Spring Data JDBC** (not JPA/Hibernate). All Hibernate-related changes (session API removals, annotation migrations, `hibernate-jpamodelgen` → `hibernate-processor`) do not apply.

Spring Data JDBC 4.0 (via Spring Data `2025.1`) is a stable release; no breaking API changes expected in `CrudRepository` / `ListCrudRepository` usage.

### 5.9 Not Impacted

- **Jakarta EE namespaces**: Already migrated in Spring Boot 3.x
- **RestTemplate** deprecation: Already using `RestClient` in `asapp-rest-clients`
- **Undertow removal**: Not in use (Tomcat embedded)
- **`AntPathMatcher`**: Not in use — codebase scan confirmed no direct imports or usage in application code
- **JUnit 4**: Already on JUnit 5 — no action needed
- **`ListenableFuture`**: Not in use

---

## 6. Dependency Compatibility Matrix

| Dependency | Current | Required | SB4 BOM | Compatible | Notes |
|---|---|---|---|---|---|
| `jjwt` | 0.12.6 | Replaced | Not managed | N/A | Replaced by `nimbus-jose-jwt` in Prep step — no Jackson dependency; same library Spring Security uses for its own JWT operations |
| `mapstruct` | 1.6.3 | — | Not managed | Yes | Annotation processor, unaffected |
| `springdoc-openapi` | 2.8.5 | 3.x | Not managed | Yes (via OpenRewrite) | Automated by `UpgradeSpringDoc_3_0` recipe |
| `bcprov-jdk18on` | 1.80 | — | Not managed | Yes | Spring Security 7.0.x pins exactly 1.80 — confirmed compatible |
| `testcontainers-redis` | 2.2.4 | — | Managed (2.2.4) | Yes | SB4 BOM explicitly manages `com.redis:testcontainers-redis:2.2.4` — confirmed compatible with TC 2.x. No replacement needed; there is no official `org.testcontainers:redis` module |
| `json-unit-assertj` | 4.1.1 | — | Not managed | Yes | Compatible with JUnit 5 and AssertJ 3.x. Do not upgrade to 5.x — it requires JUnit 6 |
| `mockserver-client-java` | 5.15.0 | — | Not managed | Yes | Latest available version; no TC 2.x coupling issue. Note: TC 2.x renamed the artifact `org.testcontainers:mockserver` → `org.testcontainers:testcontainers-mockserver` |
| `micrometer-registry-prometheus` | BOM | — | SB4 BOM | Yes | Rename if needed per actuator changes |
| `pitest-maven` | 1.20.4 | — | Not managed | Yes | Compatible with Java 21 and SB4. Latest is 1.23.0 — upgrade optional |
| `pitest-junit5-plugin` | 1.2.3 | — | Not managed | Yes | Companion to `pitest-maven`; no Spring dependency |
| `spotless-maven-plugin` | 2.46.1 | — | Not managed | Yes | Build plugin, unaffected |

---

## 7. OpenRewrite Reference

### Tools

| Artifact | Role | Version | License |
|---|---|---|---|
| `rewrite-maven-plugin` | Execution engine — provides `rewrite:dryRun` and `rewrite:run` Maven goals | `6.35.0` | Apache 2.0 |
| `rewrite-spring` | Recipe library — Spring Boot, Framework, Security, Data, Jackson, SpringDoc | `6.28.2` | Moderne Source Available |
| `rewrite-testing-frameworks` | Recipe library — TestContainers, Mockito, and other testing framework migrations | `2.28.2` | Moderne Source Available |

### Recipes

| Step | Recipe | ID | Library | License |
|---|---|---|---|---|
| 0 | Spring Boot 3 best practices | `org.openrewrite.java.spring.boot3.SpringBoot3BestPracticesOnly` | `rewrite-spring` | Community |
| 1 | Spring Boot 3.5 upgrade | `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5` | `rewrite-spring` | Moderne Source Available |
| 2 | Spring Boot 4 migration (composite) | `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` | `rewrite-spring` | Moderne Source Available |
| 2 | ⚠ SpringDoc 3 | `org.openrewrite.java.springdoc.UpgradeSpringDoc_3_0` | `rewrite-spring` | Moderne Source Available |
| 2 | ⚠ TestContainers 2 | `org.openrewrite.java.testing.testcontainers.Testcontainers2Migration` | `rewrite-testing-frameworks` | Moderne Source Available |

> ⚠ Not bundled in `UpgradeSpringBoot_4_0` — add separately to `<activeRecipes>`.
>
> Sub-recipes within `UpgradeSpringBoot_4_0` can be run independently for targeted automation. See the [recipe index](https://docs.openrewrite.org/recipes/java/spring/boot4).
>
> Run OpenRewrite in **dry-run mode** first (`rewrite:dryRun`) to review changes before applying.
>
> Moderne Source Available License: organizations may freely run recipes against their own code; commercializing them is prohibited. See [docs.openrewrite.org/licensing](https://docs.openrewrite.org/licensing/openrewrite-licensing).

---

## 8. Dependency Upgrades

Dependencies that are compatible with SB4 as-is but worth upgrading while the codebase is already being touched. Covered by **Phase 9** in the implementation plan.

| Dependency | Current | Target | Reason |
|---|---|---|---|
| `bcprov-jdk18on` | 1.80 | 1.83 | Spring Security main already uses 1.83; picks up security fixes from 1.81–1.83 |

---

## 9. Plugin Upgrades

Maven build plugins that are compatible as-is but have newer versions available. Covered by **Phase 9** in the implementation plan.

| Plugin | Current | Target | Reason |
|---|---|---|---|
| `spotless-maven-plugin` | 2.46.1 | 3.4.0 | Major version bump — review migration guide before upgrading; formatter config may need updates |
| `jacoco-maven-plugin` | 0.8.12 | 0.8.13 | Minor patch — safe drop-in upgrade |
| `pitest-maven` | 1.20.4 | 1.23.0 | Latest stable release |

---

## 10. Clarifications — Resolved

### Recipe availability
`UpgradeSpringBoot_4_0` is hosted on GitHub at [openrewrite/rewrite-spring](https://github.com/openrewrite/rewrite-spring) under the **Moderne Source Available License** (changed from Apache 2.0 in Dec 2024). Organizations can freely run it against their own code. There are also targeted sub-recipes for properties, starters, security, Jackson, SpringDoc, and TestContainers that can be run independently (see section 7).

### Build after each step
The upgrade strategy in section 4 includes `mvn clean verify` checkpoints after every step. Do not proceed to the next step until the build is green.

### Refactoring opportunity
Covered by **Phase 9** in the implementation plan. Focus areas: Spring Boot 4 structured logging (`logging.structured.format.console=ecs`), dependency and plugin bumps (BouncyCastle 1.83, jacoco 0.8.13, pitest 1.23.0, spotless 3.4.0).

### Upgrade all other dependencies
Partially automated by OpenRewrite (SpringDoc → 3.x, TestContainers → 2.x). Manual upgrades (BouncyCastle, MapStruct) can be done alongside the upgrade. Non-critical deps (pitest, spotless) are covered by Phase 9.

### Java LTS upgrade
Java 25 (LTS, released Sept 2025) has first-class support in Spring Boot 4.0. Covered by **Phase 10** in the implementation plan.

### Maven & Maven wrapper upgrade
Current: Apache Maven **3.9.5** with wrapper JAR **3.2.0** — already meets SB4 minimum (3.6.3+). Covered by **Phase 11** in the implementation plan — upgrading to Maven 3.9.14 and wrapper JAR 3.3.4.

### `spring.data.redis.host/port` (was "verify" on line 90)
**Confirmed unchanged** — `spring.data.redis.host` and `spring.data.redis.port` are not renamed in Spring Boot 4. The "verify" annotation has been removed from section 5.3.

Additionally confirmed by codebase scan: `spring.jackson.default-property-inclusion=NON_NULL` is present in **all 3 services** `application.properties` (not just `asapp-authentication-service` as previously noted). Section 5.3 updated accordingly.
