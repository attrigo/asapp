# Maven → Gradle migration — design

**Version:** 0.5.0 · **Task:** `(build) Replace Maven with Gradle` (TODO.md)
**Date:** 2026-07-13
**Goal:** Move the build onto Gradle so every later build is cached, parallel, and incremental — at functional parity with today's Maven build.

---

## 1. Context

ASAPP is a Spring Boot 4.0.5 / Java 25 multi-module project on Maven, using `spring-boot-starter-parent` and a three-tier POM hierarchy:

- `asapp-parent` (root) → aggregates `libs` + `services`
- `asapp-libs` → `asapp-commons-url`, `asapp-http-clients`
- `asapp-services` → `asapp-authentication-service`, `asapp-config-service`, `asapp-discovery-service`, `asapp-tasks-service`, `asapp-users-service`

All modules are `com.attrigo.asapp:*:0.5.0-SNAPSHOT`.

**What the build does today** (must be preserved):

- Dependency management: Spring Boot BOM (via parent) + Spring Cloud BOM (import) + version-pinned deps + CVE overrides, centralized in the aggregator POMs.
- Compile (Java 25) with the MapStruct annotation processor on the domain services.
- Unit tests (surefire, `*Tests`) vs integration tests (failsafe, `*IT` / `*E2EIT`) — split **by class-name suffix**, all living together in `src/test/java`.
- Profiles: `-Pci` adds Spotless check; `-Pfull` adds JaCoCo + Javadoc + sources + Spotless.
- Mutation testing (PIT, threshold 100, domain + application-in-service packages) — on the 3 domain services only.
- Spring Boot: `bootBuildImage` (Paketo + health-checker → `ghcr.io/attrigo/<svc>:<ver>`), build-info, `dev`-profile run.
- Extras: CycloneDX SBOM, git-commit-id, Asciidoctor REST docs (from Spring REST Docs snippets), Liquibase.
- Git hooks installed on build: `pre-commit` (LF check + `mvn spotless:check`), `commit-msg` (Conventional Commits).

**Confirmed service matrix** (drives the convention tiers):

| Service | Liquibase | REST docs | Javadoc/sources | MapStruct | PIT |
|---|---|---|---|---|---|
| authentication, tasks, users | ✅ | ✅ | ✅ | ✅ | active |
| config, discovery | — | — | — | — | skipped |

**Confirmed facts:** no resource filtering (`@…@` tokens) anywhere; test suffixes are `*Tests` (99, unit), `*IT` (46) + `*E2EIT` (4, both integration); no custom surefire/failsafe includes (relies on Spring Boot parent defaults).

**Ripples beyond the POMs:** `ci.yml`, `release.yml`, the git hook scripts, `README.md`, `CLAUDE.md`, `.claude/rules/maven.md`, and the `asapp-release` skill (uses `mvn versions:set` / `mvn help:evaluate`).

---

## 2. Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build-script DSL | **Kotlin DSL** | Type-safe, IDE autocomplete, Gradle's current default; the stronger learning target and safest for authoring unfamiliar convention logic. |
| Migration mechanism | **`gradle init --type pom` as a scaffold, then hand-author** | The only supported Maven→Gradle converter; it harvests the dependency list + module skeleton to diff against, but reproduces none of the custom plugin wiring. Not a blind conversion. |
| Unit/IT split | **Name-filtered `Test` tasks** over the existing `src/test/java` | Gradle has no surefire/failsafe plugins — test execution is a first-class `Test` task type. A second filtered task reproduces the suffix split with **zero file movement** and keeps the shared Mother factories intact. |
| Dependency management | **Native `platform()` BOMs + a `libs.versions.toml` catalog** | Gradle-native, no legacy `io.spring.dependency-management` plugin; the approach Spring recommends for Gradle today. |
| Convention tiers | **4 tiers** (see §3) | Matches the confirmed thin/data service split; keeps dead plugins off the infra services. |
| Formatting gate | **Wire `spotlessCheck` into `check`** | Spotless is incremental + build-cacheable (near-zero when clean); makes local `./gradlew build` fail early on a formatting slip. An improvement over Maven, which gated it to CI. |
| Git hook install | **`git config core.hooksPath git/hooks`** | No copying, no install drift, no third-party plugin; hooks are used in place from version control. |
| Version source | **`gradle.properties`** | Bumping becomes a one-line text edit; simplifies the release skill. |
| Docker publish | **Native `bootBuildImage --publishImage`** | Builds and pushes in one step, dropping the shell push loop — the fuller migration of "Docker image publishing to Gradle". |

**OpenRewrite was considered and rejected for this task:** it has no Maven→Gradle conversion recipe (its build recipes stay within one build system). It is the right tool for the 0.6.0 upgrade work (Spring Boot, JUnit, CVEs), not for a build-system replacement.

---

## 3. Module structure & convention plugins

### Repository layout after migration

```
asapp/
├─ settings.gradle.kts            # includes 8 modules; registers catalog; includeBuild("build-logic")
├─ build.gradle.kts               # root: minimal (installGitHooks task; no allprojects/subprojects config)
├─ gradle.properties              # group + version + speed flags
├─ gradle/
│   ├─ libs.versions.toml         # the version catalog
│   └─ wrapper/                   # pinned Gradle version
├─ gradlew  /  gradlew.bat        # wrapper scripts (committed)
├─ build-logic/                   # included build: the convention plugins
│   ├─ settings.gradle.kts        # re-registers the catalog so plugins can read `libs`
│   ├─ build.gradle.kts           # applies `kotlin-dsl`; depends on the plugins it configures
│   └─ src/main/kotlin/
│       ├─ asapp.java-conventions.gradle.kts
│       ├─ asapp.library-conventions.gradle.kts
│       ├─ asapp.service-conventions.gradle.kts
│       └─ asapp.domain-service-conventions.gradle.kts
├─ libs/
│   ├─ asapp-commons-url/build.gradle.kts
│   └─ asapp-http-clients/build.gradle.kts
└─ services/
    ├─ asapp-authentication-service/build.gradle.kts
    ├─ asapp-config-service/build.gradle.kts
    ├─ asapp-discovery-service/build.gradle.kts
    ├─ asapp-tasks-service/build.gradle.kts
    └─ asapp-users-service/build.gradle.kts
```

`libs/` and `services/` stop being modules. Their aggregator POMs carried `dependencyManagement` / `pluginManagement`; that config moves into convention plugins, so `include(":services:asapp-tasks-service")` treats `services` as a bare path segment with no build script.

### Convention tiers (1:1 with the POM hierarchy + the thin/data split)

| Convention plugin | Applied by | Responsibilities |
|---|---|---|
| `asapp.java-conventions` | all 8 | Java 25 toolchain, UTF-8, `java`; Spring Boot BOM `platform()`; CVE `constraints`; Spotless; JaCoCo; `test` (unit `*Tests`) + `integrationTest` (`*IT`/`*E2EIT`) tasks; `group`/`version` from `gradle.properties`; `spotlessCheck` wired into `check`. |
| `asapp.library-conventions` | 2 libs | + Javadoc & sources jars; PIT off; not a Boot app. |
| `asapp.service-conventions` | all 5 services | + Spring Boot app (bootJar, **bootBuildImage** Paketo+health-checker→ghcr, build-info, `dev`-profile `bootRun`); Spring Cloud BOM `platform()`; CycloneDX SBOM; git properties. |
| `asapp.domain-service-conventions` | auth, tasks, users | + MapStruct processor; Liquibase; Asciidoctor REST docs; Javadoc & sources jars; **PIT active** (domain + app-service targeting). |

Chain: `java` → `{library}` and `java` → `service` → `domain-service`.

Leaf build scripts shrink to *pick a tier + list your own deps*:

```kotlin
// services/asapp-authentication-service/build.gradle.kts
plugins { id("asapp.domain-service-conventions") }
dependencies {
    implementation(project(":libs:asapp-commons-url"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // …exactly the deps from today's pom, version-less
}
```

**`build-logic`, not `buildSrc`** — `build-logic` is an *explicitly included* build (wired via `includeBuild` in the root settings), so changing a convention plugin re-runs only the modules that apply it. `buildSrc` is auto-applied to the whole build, giving a change a wider invalidation reach. Both work; the included build is Gradle's current recommendation for convention plugins.

---

## 4. Dependency management

### 4.1 Version catalog — `gradle/libs.versions.toml`

Single source of truth for every version, library alias, plugin alias, and bundle, replacing the scattered `<x.version>` properties. `libs` is the catalog's *accessor namespace* (from the filename), not a claim about contents — it holds `[versions]`, `[libraries]`, `[bundles]`, `[plugins]`.

```toml
# Grouped by kind, alphabetical within each group — same discipline as the POM <properties>
[versions]
# BOM
springBoot   = "4.0.5"
springCloud  = "2025.1.1"
# Compile
mapstruct    = "1.6.3"
resilience4j = "2.4.0"
# CVE overrides (bump above what the BOMs pin)
guava        = "33.4.8-jre"
jackson      = "3.1.1"

[libraries]
spring-boot-bom  = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "springBoot" }
spring-cloud-bom = { module = "org.springframework.cloud:spring-cloud-dependencies", version.ref = "springCloud" }
guava            = { module = "com.google.guava:guava", version.ref = "guava" }
# …every other pinned dependency

[bundles]
testcontainers = ["testcontainers-junit", "testcontainers-postgresql", "testcontainers-redis"]

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
spotless    = { id = "com.diffplug.spotless", version = "…" }
pitest      = { id = "info.solidsoft.pitest", version = "…" }
```

Every table (`[versions]`, `[libraries]`, `[bundles]`, `[plugins]`) keeps the same comment-grouped, alphabetical-within-group ordering as today's POM `<properties>` — codified in the replacement `gradle.md` rule (§8).

### 4.2 BOMs as native platforms

```kotlin
dependencies {
    implementation(platform(libs.spring.boot.bom))     // java-conventions
    implementation(platform(libs.spring.cloud.bom))    // service-conventions
}
```

The Spring Boot Gradle plugin does **not** auto-apply dependency management — the `platform()` import is mandatory.

### 4.3 CVE / transitive overrides

Direct deps (e.g. bouncycastle) take the catalog version. Purely transitive bumps (guava, commons-io, commons-beanutils, rhino, jackson) are forced via `constraints {}` in `java-conventions`:

```kotlin
dependencies {
    constraints {
        implementation(libs.guava)      // forces the fixed version even when only pulled transitively
        implementation(libs.commons.io)
    }
}
```

### 4.4 Internal modules

`asapp-commons-url` / `asapp-http-clients` become `implementation(project(":libs:asapp-commons-url"))` — no GAV, no `${project.version}`.

---

## 5. Build lifecycle & checks

Gradle has no profiles; leaning on `-P` conditionals would fight caching. The design maps Maven's *outcomes* onto Gradle's task graph.

| Need | Maven today | Gradle target |
|---|---|---|
| Fast local build + tests | `mvn clean install` (checks skipped) | `./gradlew build` → compile + `test` + `integrationTest` + jars + `spotlessCheck` |
| Format sources | `mvn spotless:apply` | `./gradlew spotlessApply` |
| Coverage report | `mvn verify -Pfull` | `./gradlew jacocoTestReport` (per module, UT+IT combined) |
| Javadoc + sources jars | `-Pfull` | `./gradlew javadocJar sourcesJar` (explicit, off the fast path) |
| Mutation testing | `mvn …:mutationCoverage` | `./gradlew pitest` (auth/tasks/users only) |
| Full CI / release verify | `mvn install -Pfull` | `./gradlew fullBuild` |

**`fullBuild` aggregate** — the `-Pfull` equivalent as one memorable task instead of four. The underlying task names are plugin-owned (`jacocoTestReport`←`jacoco`, `javadocJar`/`sourcesJar`←`java`, etc.) and are *not* renamed; the alias just groups them:

```kotlin
tasks.register("fullBuild") { dependsOn("build", "jacocoTestReport", "javadocJar", "sourcesJar") }
```

**Spotless** — same config one-to-one: Eclipse formatter (`asapp_formatter.xml`, 4.35), the `java|javax,org,com,,com.attrigo` import order, `header-license`, UNIX line endings, remove-unused-imports. In `java-conventions`; `spotlessCheck` wired into `check`.

**JaCoCo** — per module, the report combines both test tasks (matches Maven's UT+IT merge):

```kotlin
tasks.jacocoTestReport { executionData(tasks.test.get(), tasks.named("integrationTest").get()) }
```

**PIT** — in `domain-service-conventions` only. Shared config uses a wildcard each module resolves against its own classpath (improving on Maven's hardcoded per-service package):

```kotlin
pitest {
    junit5PluginVersion = libs.versions.pitestJunit5.get()
    mutationThreshold = 100
    targetClasses = listOf("com.attrigo.asapp.*.domain.*", "com.attrigo.asapp.*.application.*.in.service.*")
    targetTests   = listOf("com.attrigo.asapp.*.domain.*", "com.attrigo.asapp.*.application.*.in.service.*")
}
```

**Unit/IT split** (in `java-conventions`):

```kotlin
tasks.test { filter { includeTestsMatching("*Tests") } }
val integrationTest by tasks.registering(Test::class) {
    filter { includeTestsMatching("*IT") }   // matches *IT and *E2EIT
    shouldRunAfter(tasks.test)
}
tasks.check { dependsOn(integrationTest) }
```

---

## 6. Git hooks

**Install** — `installGitHooks` task in the root build, wired into `build`, idempotent:

```kotlin
val installGitHooks by tasks.registering(Exec::class) {
    commandLine("git", "config", "core.hooksPath", "git/hooks")
    onlyIf { /* current core.hooksPath != "git/hooks" */ }
}
tasks.named("build") { dependsOn(installGitHooks) }
```

Points git directly at the version-controlled `git/hooks/` — no copy, no drift.

**Contents** — `git/hooks/pre-commit`: the LF check is unchanged; `mvn spotless:check` → `./gradlew --quiet spotlessCheck`. `git/hooks/commit-msg`: untouched.

---

## 7. Version, wrapper, CI, release, Docker

### 7.1 Version source — `gradle.properties`

```properties
group=com.attrigo.asapp
version=0.5.0-SNAPSHOT
```

`java-conventions` stamps `group`/`version` onto every module (same effect as parent inheritance).

### 7.2 Wrapper + speed flags

Commit the wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` + `.properties`) pinning an exact Gradle version. `.gitignore`: drop the stale `target/` + `.mvn/wrapper` lines (`build/` already handled). `.gitattributes`: `gradlew`=LF, `gradlew.bat`=CRLF, wrapper jar=binary. The task's payoff lives in `gradle.properties`:

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

### 7.3 CI (`ci.yml`)

- `mvn verify -B --no-transfer-progress -Pci` → `./gradlew build`.
- `actions/setup-java` `cache: maven` → `gradle/actions/setup-gradle@v4` (dependency + build caching across runs).

### 7.4 Release (`release.yml`)

| Job | Maven | Gradle |
|---|---|---|
| build-and-test | `mvn install -Pfull` + upload `services/*/target/*.jar` | `./gradlew fullBuild` + upload `services/*/build/libs/*.jar` |
| publish-docker | `mvn spring-boot:build-image -DskipTests` + docker push loop | `./gradlew bootBuildImage --publishImage` (builds + pushes; ghcr creds via env) |
| create-release | git-cliff + `gh release create …/target/*.jar` | unchanged except the jar path |

All jobs swap `cache: maven` → `gradle/actions/setup-gradle`. `bootBuildImage` depends only on `bootJar`, not `test` — no `-DskipTests` equivalent needed.

### 7.5 `asapp-release` skill (must change, or the next release breaks)

Mechanism-only changes; all other steps (Liquibase tagging, OpenAPI/docker-compose edits, git tag, push gate) are build-tool-agnostic.

| Step | Maven | Gradle |
|---|---|---|
| 2 detect version | read root `pom.xml` | read `gradle.properties` |
| 5 remove SNAPSHOT | `mvn versions:set -DremoveSnapshot=true` | edit `version=` in `gradle.properties` |
| 7 preflight | `mvn clean test` | `./gradlew test` (unit only) |
| 8 read version | `mvn help:evaluate …` | parse `gradle.properties` |
| 9 next SNAPSHOT | `mvn versions:set -DnextSnapshot=true` | edit `version=` in `gradle.properties` |

---

## 8. Docs & tooling ripple

- **`README.md`** — prerequisites (no Maven install; wrapper handles it), all build/test/run/format commands, report paths (`target/site/jacoco-aggregate` → `build/reports/jacoco/…`, `target/generated-docs` → `build/docs/…`, `pit-reports` → `build/reports/pitest`).
- **`CLAUDE.md`** — the Build / Run / Testing command blocks.
- **`.claude/rules/maven.md`** — obsolete; replace with a short `gradle.md` (catalog ordering + convention-plugin conventions) or drop.
- **Skill audit** — grep the other `.claude/skills` for `mvn` verification calls and update any found. (The "run Spotless per-submodule" memory note stops applying — flag, do not edit memory without confirmation.)

---

## 9. Execution plan

Single branch, big-bang cutover, squash-merge. Maven and Gradle coexist *on the branch* during authoring (to diff outputs); Maven is deleted before merge.

1. `gradle init --type pom` in a scratch copy → harvest dependency list + module skeleton as a cross-check.
2. Author `settings.gradle.kts`, `gradle.properties`, wrapper, `libs.versions.toml`.
3. Author the 4 convention plugins in `build-logic`.
4. Author the 8 leaf build scripts (deps diffed against the init output so nothing drops).
5. Get `./gradlew build` green.
6. Verify the extras reproduce Maven (§10).
7. Git hooks → CI/release YAML → `asapp-release` skill → docs.
8. Delete all 11 `pom.xml` files (keep `asapp_formatter.xml` + `header-license`); remove `maven.md`.
9. Final full verification, then close via the normal task flow.

---

## 10. Verification — prove parity, don't assume it

Capture a Maven baseline first, then assert Gradle reproduces it:

- **Tests:** all 149 classes run (99 `*Tests`, 46 `*IT`, 4 `*E2EIT`) and pass — compare counts so nothing is silently skipped.
- **Artifacts:** 5 runnable boot jars; 5 images from `bootBuildImage` with the exact `ghcr.io/attrigo/<svc>:<ver>` name and Paketo + health-checker buildpacks.
- **Reports:** per-module JaCoCo (UT+IT combined) on all 8; PIT green at threshold 100 on the 3 domain services; Asciidoctor REST-docs HTML for the 3 domain services; SBOM + `git.properties` embedded in jars.
- **Formatting:** `spotlessCheck` passes on the same `asapp_formatter.xml` — zero reformatting churn.

---

## 11. Known tricky bits

- Reading the `libs` catalog inside precompiled convention plugins needs the generated-accessor wiring in `build-logic`.
- Asciidoctor must depend on `integrationTest` (REST Docs snippets are produced during IT).
- The Spring Boot BOM `platform()` import is mandatory — the plugin does not auto-apply it.
- Confirm the Spotless plugin version supports the Eclipse formatter `4.35` profile.
- `bootRun` must activate the `dev` profile (the Maven plugin does this today).

---

## 12. Out of scope

- **ArchUnit layering guardrail** — the separate 0.5.0 task (TODO), its own branch.
- **Dependency upgrades** — every version migrates as-is (Spring Boot stays 4.0.5); upgrades are 0.6.0.
- **JMeter tools** — invoke JMeter directly, not Maven; untouched.
- **Cross-module coverage aggregation** — a later enhancement, not part of parity.
