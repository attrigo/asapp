# Explicit dev/prod Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce an explicit `dev`/`prod` environment profile axis, orthogonal to the existing `docker`/`native` platform axis, that gates dev-only tooling (Swagger, full Actuator exposure, heapdump/shutdown access, `info.env`, verbose logging) behind `dev`, with a secure-by-default base.

**Architecture:** Base config (the three `application.properties` locations) is locked down; a sibling `application-dev.properties` overlay re-opens tooling. `SecurityConfiguration` is untouched — the split lives entirely in properties. `dev` is wired into `spring-boot:run` per service and into the `docker-compose` stack; a locked-down deploy is reached by switching the profile env var to `…,prod`.

**Tech Stack:** Spring Boot 4.0.5 properties files, Spring Cloud Config (native filesystem backend), `spring-boot-maven-plugin`, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-06-10-dev-prod-profiles-design.md`

> **Note (supersedes spec §12):** During planning we confirmed every service's tests are self-contained — each `src/test/resources/application.properties` sets `spring.cloud.config.enabled=false` (config-service uses its own `src/test/resources/central-config/` fixture) and carries its own permissive management block with springdoc default-on. Because test resources shadow the main file during tests, **locking down main + central-config has no effect on the test suite**. `SecurityConfigurationIT`, `OpenApiEndpointsIT`, and the config-service ITs keep passing **with no changes**. No Java or test edits are part of this plan. The locked-down (prod) posture is verified by manual smoke (Task 7), not automated tests.

---

## File Structure

### Modified — base config (locked down)

- `central-config/application.properties` — shared base for auth/tasks/users (served via config server). Lock Actuator exposure/heapdump/shutdown/`info.env`; disable springdoc.
- `central-config/asapp-authentication-service.properties` — remove redundant `com.bcn.asapp.authentication=INFO` logger pin.
- `central-config/asapp-tasks-service.properties` — remove redundant `com.bcn.asapp.tasks=INFO` logger pin.
- `central-config/asapp-users-service.properties` — remove redundant `com.bcn.asapp.users=INFO` logger pin.
- `services/asapp-config-service/src/main/resources/application.properties` — lock Actuator block; remove `com.bcn.asapp.config=INFO` pin; keep `spring.profiles.active=native`.
- `services/asapp-discovery-service/src/main/resources/application.properties` — lock Actuator block; remove `com.bcn.asapp.discovery=INFO` pin; move `spring.devtools.livereload.enabled=true` to the dev overlay.

### Created — dev overlays (re-open tooling)

- `central-config/application-dev.properties` — Actuator open, springdoc on, `com.bcn.asapp=DEBUG`, JDBC SQL logging. Served to auth/tasks/users when they run `dev`.
- `services/asapp-config-service/src/main/resources/application-dev.properties` — Actuator open + `com.bcn.asapp=DEBUG`.
- `services/asapp-discovery-service/src/main/resources/application-dev.properties` — Actuator open + `com.bcn.asapp=DEBUG` + `spring.devtools.livereload.enabled=true`.

### Modified — run wiring

- `services/asapp-authentication-service/pom.xml` — add `<profiles><profile>dev</profile></profiles>` to `spring-boot-maven-plugin`.
- `services/asapp-tasks-service/pom.xml` — same.
- `services/asapp-users-service/pom.xml` — same.
- `services/asapp-discovery-service/pom.xml` — same.
- `services/asapp-config-service/pom.xml` — add `<profiles><profile>native</profile><profile>dev</profile></profiles>`.
- `docker-compose.yaml` — update `SPRING_PROFILES_ACTIVE` for all five services.

### Modified — docs

- `README.md` (root) — add a Profiles section; mark Swagger as dev-only.
- `services/asapp-authentication-service/README.md`, `services/asapp-tasks-service/README.md`, `services/asapp-users-service/README.md`, `services/asapp-config-service/README.md`, `services/asapp-discovery-service/README.md` — add Profiles subsection; update `SPRING_PROFILES_ACTIVE` default; mark Swagger dev-only (the three with Swagger).
- `TODO.md` — tick the Profiles item.

### Why the redundant logger pins are removed

The dev overlay sets `logging.level.com.bcn.asapp=DEBUG` in the **shared** `application-dev.properties`. In Spring Cloud Config precedence, a service-specific source (`asapp-tasks-service.properties`) outranks the shared `application-dev.properties`, so an existing `com.bcn.asapp.tasks=INFO` pin would override the broad DEBUG and silence dev logging for the service's own package. Removing those pins (INFO is the framework default anyway, so prod behavior is unchanged) lets the broad `com.bcn.asapp=DEBUG` take effect in dev. This applies equally to config-service and discovery-service (local profile-specific `application-dev.properties` outranks their base `application.properties`, but their own INFO pin would still need removing for the broad DEBUG to apply cleanly).

---

## Task 1: Lock down base configuration (secure-by-default)

**Files:**
- Modify: `central-config/application.properties`
- Modify: `central-config/asapp-authentication-service.properties`
- Modify: `central-config/asapp-tasks-service.properties`
- Modify: `central-config/asapp-users-service.properties`
- Modify: `services/asapp-config-service/src/main/resources/application.properties`
- Modify: `services/asapp-discovery-service/src/main/resources/application.properties`

- [ ] **Step 1: Lock the shared management block + disable springdoc in `central-config/application.properties`**

Replace the `# Management properties` block (the four permissive lines) and append a new OpenAPI group. The block currently reads:

```properties
# Management properties
management.endpoint.health.cache.time-to-live=30s
management.endpoint.health.probes.add-additional-paths=true
management.endpoint.health.show-details=when_authorized
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
management.endpoints.web.exposure.include=*
management.info.env.enabled=true
management.info.java.enabled=true
management.info.os.enabled=true
management.info.process.enabled=true
```

Change it to (only the four marked values change; add the OpenAPI group right after):

```properties
# Management properties
management.endpoint.health.cache.time-to-live=30s
management.endpoint.health.probes.add-additional-paths=true
management.endpoint.health.show-details=when_authorized
management.endpoint.heapdump.access=none
management.endpoint.shutdown.access=none
management.endpoints.web.exposure.include=health,info,prometheus
management.info.env.enabled=false
management.info.java.enabled=true
management.info.os.enabled=true
management.info.process.enabled=true

# OpenAPI properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

- [ ] **Step 2: Remove the redundant per-service logger pins from the three central-config service files**

In `central-config/asapp-authentication-service.properties`, delete the line:
```properties
logging.level.com.bcn.asapp.authentication=INFO
```
In `central-config/asapp-tasks-service.properties`, delete the line:
```properties
logging.level.com.bcn.asapp.tasks=INFO
```
In `central-config/asapp-users-service.properties`, delete the line:
```properties
logging.level.com.bcn.asapp.users=INFO
```
Leave the `logging.level.org.springframework=INFO` line in each file unchanged.

- [ ] **Step 3: Lock the management block in `services/asapp-config-service/src/main/resources/application.properties`**

Change these three lines from `unrestricted`/`*`/`true` to:
```properties
management.endpoint.heapdump.access=none
management.endpoint.shutdown.access=none
management.endpoints.web.exposure.include=health,info,prometheus
management.info.env.enabled=false
```
(Leave `management.metrics.tags.application`, `management.server.base-path`, `management.server.port=8898`, `spring.profiles.active=native`, and the config-server settings unchanged.) Then remove the logger pin line:
```properties
logging.level.com.bcn.asapp.config=INFO
```
(keep `logging.level.org.springframework=INFO`).

- [ ] **Step 4: Lock the management block in `services/asapp-discovery-service/src/main/resources/application.properties`**

Change the four permissive values:
```properties
management.endpoint.heapdump.access=none
management.endpoint.shutdown.access=none
management.endpoints.web.exposure.include=health,info,prometheus
management.info.env.enabled=false
```
Remove the logger pin line:
```properties
logging.level.com.bcn.asapp.discovery=INFO
```
(keep `logging.level.org.springframework=INFO`). Also remove the dev-only line near the top (it moves to the dev overlay in Task 2):
```properties
spring.devtools.livereload.enabled=true
```

- [ ] **Step 5: Verify line endings and parse**

Run: `git add central-config services/asapp-config-service/src/main/resources/application.properties services/asapp-discovery-service/src/main/resources/application.properties`
Then: `git diff --cached --check`
Expected: no whitespace/EOL errors reported.

- [ ] **Step 6: Commit**

```bash
git commit -m "refactor(config)!: lock down Actuator and Swagger by default" \
  -m "Make the base configuration production-safe; dev-only tooling becomes opt-in via the dev profile (added next)." \
  -m "- Narrow Actuator web exposure to health, info, prometheus in all base configs
- Set heapdump and shutdown endpoint access to none
- Disable management.info.env to stop env leakage via /info
- Disable springdoc api-docs and swagger-ui in shared central-config
- Drop redundant per-service com.bcn.asapp.* INFO logger pins (INFO is the default)
- Move discovery-service spring.devtools.livereload.enabled out of base" \
  -m "BREAKING CHANGE: services started without the dev profile now expose only health/info/prometheus actuator endpoints, disable Swagger UI and /v3/api-docs, and deny heapdump/shutdown. Activate the dev profile for full local tooling." \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

(The pre-commit hook runs the Maven reactor scan, an LF-endings check, and Conventional-Commit validation. If it reports CRLF on any file, run `git add --renormalize <file>` and re-commit. Finalize type/scope via the `commit-msg` skill if preferred.)

---

## Task 2: Add the dev profile overlays

**Files:**
- Create: `central-config/application-dev.properties`
- Create: `services/asapp-config-service/src/main/resources/application-dev.properties`
- Create: `services/asapp-discovery-service/src/main/resources/application-dev.properties`

- [ ] **Step 1: Create `central-config/application-dev.properties`**

```properties
# Shared dev configuration for all ASAPP services

# Management properties
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
management.endpoints.web.exposure.include=*
management.info.env.enabled=true

# OpenAPI properties
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

# Logger properties
logging.level.com.bcn.asapp=DEBUG
logging.level.org.springframework.jdbc.core=DEBUG
```

- [ ] **Step 2: Create `services/asapp-config-service/src/main/resources/application-dev.properties`**

(config-service has no springdoc and no datasource, so no Swagger or JDBC lines.)

```properties
# Dev configuration for asapp-config-service

# Management properties
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
management.endpoints.web.exposure.include=*
management.info.env.enabled=true

# Logger properties
logging.level.com.bcn.asapp=DEBUG
```

- [ ] **Step 3: Create `services/asapp-discovery-service/src/main/resources/application-dev.properties`**

(Includes the LiveReload line moved out of the base in Task 1.)

```properties
# Dev configuration for asapp-discovery-service

# Spring Base Application properties
spring.devtools.livereload.enabled=true

# Management properties
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
management.endpoints.web.exposure.include=*
management.info.env.enabled=true

# Logger properties
logging.level.com.bcn.asapp=DEBUG
```

- [ ] **Step 4: Verify line endings**

Run: `git add central-config/application-dev.properties services/asapp-config-service/src/main/resources/application-dev.properties services/asapp-discovery-service/src/main/resources/application-dev.properties`
Then: `git diff --cached --check`
Expected: no whitespace/EOL errors.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(config): add dev profile overlays re-enabling tooling" \
  -m "- Add central-config/application-dev.properties opening Actuator, Swagger, and DEBUG logging for auth/tasks/users
- Add config-service and discovery-service application-dev.properties opening Actuator and DEBUG logging
- Restore discovery-service LiveReload under the dev profile" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Wire the dev profile into `spring-boot:run`

**Files:**
- Modify: `services/asapp-authentication-service/pom.xml` (spring-boot-maven-plugin block)
- Modify: `services/asapp-tasks-service/pom.xml`
- Modify: `services/asapp-users-service/pom.xml`
- Modify: `services/asapp-discovery-service/pom.xml`
- Modify: `services/asapp-config-service/pom.xml`

Context: each child POM declares the plugin minimally (it inherits the `<image>` config from `services/pom.xml` pluginManagement). Adding a `<configuration><profiles>` block merges with the inherited config and applies to the `spring-boot:run` goal only — it does not affect packaging, `build-image`, or tests.

- [ ] **Step 1: Add the dev profile to auth/tasks/users/discovery POMs**

In each of `asapp-authentication-service/pom.xml`, `asapp-tasks-service/pom.xml`, `asapp-users-service/pom.xml`, `asapp-discovery-service/pom.xml`, find the block:

```xml
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
```

Replace it with:

```xml
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <profiles>
                        <profile>dev</profile>
                    </profiles>
                </configuration>
            </plugin>
```

- [ ] **Step 2: Add `native` + `dev` to the config-service POM**

In `services/asapp-config-service/pom.xml`, replace the same minimal plugin block with (config-service must keep `native` active alongside `dev`):

```xml
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <profiles>
                        <profile>native</profile>
                        <profile>dev</profile>
                    </profiles>
                </configuration>
            </plugin>
```

- [ ] **Step 3: Verify the build still resolves**

Run: `mvn -q -DskipTests clean install`
Expected: `BUILD SUCCESS` for all 10 reactor modules (POMs parse, plugin config is valid, jars package). No tests run.

- [ ] **Step 4: Commit**

```bash
git add services/asapp-authentication-service/pom.xml services/asapp-tasks-service/pom.xml services/asapp-users-service/pom.xml services/asapp-discovery-service/pom.xml services/asapp-config-service/pom.xml
git commit -m "build(services): activate dev profile for spring-boot:run" \
  -m "- Configure spring-boot-maven-plugin to run with the dev profile on auth, tasks, users, and discovery
- Run config-service with native and dev so the config backend stays active locally" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Switch the docker-compose stack to `docker,dev`

**Files:**
- Modify: `docker-compose.yaml`

- [ ] **Step 1: Update `SPRING_PROFILES_ACTIVE` for all five services**

Make these exact replacements in `docker-compose.yaml`:

- `asapp-authentication-service`: `- SPRING_PROFILES_ACTIVE=docker` → `- SPRING_PROFILES_ACTIVE=docker,dev`
- `asapp-config-service`: `- SPRING_PROFILES_ACTIVE=native,docker` → `- SPRING_PROFILES_ACTIVE=native,docker,dev`
- `asapp-discovery-service`: `- SPRING_PROFILES_ACTIVE=docker` → `- SPRING_PROFILES_ACTIVE=docker,dev`
- `asapp-tasks-service`: `- SPRING_PROFILES_ACTIVE=docker` → `- SPRING_PROFILES_ACTIVE=docker,dev`
- `asapp-users-service`: `- SPRING_PROFILES_ACTIVE=docker` → `- SPRING_PROFILES_ACTIVE=docker,dev`

(There are four identical `=docker` lines; replace each within its own service block. The `native,docker` line is unique to config-service.)

- [ ] **Step 2: Validate compose syntax**

Run: `docker compose config --quiet`
Expected: no output and exit code 0 (the file parses). If `docker compose` is unavailable in this shell, this step is performed by the developer.

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yaml
git commit -m "build(docker): run compose stack with the dev profile" \
  -m "- Append dev to SPRING_PROFILES_ACTIVE for all five services so the local stack stays debuggable (Swagger and full Actuator on)" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Document the profile model

**Files:**
- Modify: `README.md`
- Modify: `services/asapp-authentication-service/README.md`
- Modify: `services/asapp-tasks-service/README.md`
- Modify: `services/asapp-users-service/README.md`
- Modify: `services/asapp-config-service/README.md`
- Modify: `services/asapp-discovery-service/README.md`

- [ ] **Step 1: Add a Profiles section to the root `README.md`**

Insert this new section immediately after the `### Stopping the Application` block (i.e., before the `---` that precedes `## Architecture`):

```markdown
### Profiles

ASAPP uses two orthogonal profile axes:

| Axis | Values | Controls |
|------|--------|----------|
| **Environment** | `dev`, `prod` | Swagger UI, Actuator exposure, heapdump/shutdown access, `info.env`, log verbosity |
| **Platform** | (default), `docker`, `native` | Service wiring (localhost vs env vars), config-server backend (config-service only) |

The base configuration is **secure-by-default**: with no environment profile active a service is production-safe — Swagger and the heapdump/shutdown endpoints are off and Actuator exposes only `health`, `info`, and `prometheus`. The `dev` profile re-enables the full tooling.

- **Local development** — `mvn spring-boot:run` activates `dev` automatically (config-service runs `native,dev`).
- **Docker stack** — `docker-compose up -d` runs `docker,dev` (debuggable: Swagger and full Actuator on).
- **Locked-down deploy** — set `SPRING_PROFILES_ACTIVE` to `docker,prod` (config-service: `native,docker,prod`).
```

- [ ] **Step 2: Mark Swagger as dev-only in the root `README.md` Reference section**

In the `### API Endpoints` block, change the line:
```markdown
Each service provides interactive Swagger UI:
```
to:
```markdown
Each service provides interactive Swagger UI (available under the `dev` profile only):
```

- [ ] **Step 3: Add a Profiles subsection to each service README**

In `asapp-authentication-service/README.md`, `asapp-tasks-service/README.md`, and `asapp-users-service/README.md`, add this subsection at the end of the `## Quick Start` section (after `### Run with Docker`):

```markdown
### Profiles

The service is **secure-by-default**: with no environment profile, Swagger and full Actuator tooling are off and Actuator exposes only `health`, `info`, and `prometheus`. The `dev` profile re-enables them.

- `mvn spring-boot:run` activates `dev` automatically (Swagger UI is available locally).
- The Docker stack runs `docker,dev`.
- For a locked-down deployment set `SPRING_PROFILES_ACTIVE=docker,prod`.
```

For `asapp-config-service/README.md` and `asapp-discovery-service/README.md`, add the same subsection but **without** the "Swagger UI is available" parenthetical (these services have no Swagger), and for config-service note the platform profile:

```markdown
### Profiles

The service is **secure-by-default**: with no environment profile, full Actuator tooling is off and Actuator exposes only `health`, `info`, and `prometheus`. The `dev` profile re-enables it.

- `mvn spring-boot:run` activates `dev` automatically (config-service also keeps `native`, so it runs `native,dev`).
- The Docker stack runs `native,docker,dev`.
- For a locked-down deployment set `SPRING_PROFILES_ACTIVE=native,docker,prod`.
```

(For discovery-service, drop the `native` mentions and use `dev` / `docker,dev` / `docker,prod`.)

- [ ] **Step 4: Update the `SPRING_PROFILES_ACTIVE` default in each service README's Docker Environment Variables table**

In each service README that has a `### Docker Environment Variables` table, change the `SPRING_PROFILES_ACTIVE` default cell:
- auth/tasks/users/discovery: `docker` → `docker,dev`
- config: `native,docker` → `native,docker,dev`

- [ ] **Step 5: Mark Swagger dev-only in the three Swagger-bearing service READMEs**

In `asapp-authentication-service/README.md`, `asapp-tasks-service/README.md`, `asapp-users-service/README.md`, in the `### Documentation` table row for `Swagger UI`, append ` (dev profile only)` to the description/URL cell. Also, in the `### Run Locally (Development Mode)` step that opens Swagger, no change is needed since local runs use `dev`.

- [ ] **Step 6: Commit**

```bash
git add README.md services/asapp-authentication-service/README.md services/asapp-tasks-service/README.md services/asapp-users-service/README.md services/asapp-config-service/README.md services/asapp-discovery-service/README.md
git commit -m "docs: document the dev/prod profile model" \
  -m "- Add a Profiles section to the root and service READMEs explaining the environment/platform axes and secure-by-default posture
- Update SPRING_PROFILES_ACTIVE defaults to include dev
- Note that Swagger UI is available under the dev profile only" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Mark the TODO item done

**Files:**
- Modify: `TODO.md`

> Note: `TODO.md` already has an unrelated uncommitted modification in the working tree. Stage **only** the single line changed in this step (`git add -p TODO.md`), not the whole file, so the unrelated change is not swept into this commit.

- [ ] **Step 1: Tick the Profiles item**

In `TODO.md`, under `## Version 0.4.0` → `### Quick Wins` → Technical Improvements → Profiles, change:
```markdown
        * [ ] Introduce explicit dev and prod profiles to gate dev-only tooling and align with docker/native
```
to:
```markdown
        * [X] Introduce explicit dev and prod profiles to gate dev-only tooling and align with docker/native
```

- [ ] **Step 2: Stage only that line and commit**

```bash
git add -p TODO.md   # stage only the Profiles checkbox hunk; answer 'n' to any unrelated hunk
git commit -m "docs(todo): mark dev/prod profiles item as done" \
  -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Validate (regression + runtime smoke)

No new automated tests are added (see the note under the plan header). Validation is a regression run plus manual runtime smoke.

- [ ] **Step 1: Full regression suite (developer go-ahead required — slow, runs integration tests)**

Run: `mvn clean verify`
Expected: `BUILD SUCCESS`; unit (`*Tests`), integration (`*IT`), and E2E (`*E2EIT`) tests all pass unchanged. Rationale: test resources are self-contained and permissive, so the lockdown does not affect them.

> Per project convention, integration-test runs are slow and need the developer's go-ahead before running. Hand this command to the developer rather than running it autonomously.

- [ ] **Step 2: Secure-by-default smoke (developer-run)**

Start one service with no environment profile (prod posture), e.g. from a packaged jar or `SPRING_PROFILES_ACTIVE=docker,prod` for one compose service, and confirm:
- `GET /swagger-ui.html` → 404
- `GET /actuator` (with Basic auth) lists only `health`, `info`, `prometheus`
- `POST /actuator/shutdown` → 404 (access denied/none)
- `/actuator/info` contains no `env` section
- `/readyz` and `/livez` → 200 without credentials (probes still open)

- [ ] **Step 3: Dev smoke via the Docker stack (developer-run)**

Run: `mvn spring-boot:build-image` then `docker-compose up -d` (stack is `docker,dev`). Confirm:
- `http://localhost:8081/asapp-tasks-service/swagger-ui.html` loads
- Actuator full exposure returns
- `docker-compose ps` shows all services healthy (probes unauthenticated)
- service logs show `com.bcn.asapp` DEBUG output

> This session is a PowerShell console; Docker/WSL commands are handed to the developer to run.

---

## Self-Review

**Spec coverage** (against `2026-06-10-dev-prod-profiles-design.md`):
- §4 profile model → Tasks 1–4 realize the two axes; Task 5 documents them. ✓
- §5 secure-by-default placement (three base locations + three dev overlays) → Tasks 1 & 2. ✓
- §6 property split (exposure, heapdump/shutdown, info.env, springdoc, logging) → Tasks 1 & 2. ✓ (Logging-precedence refinement documented in File Structure; surfaced as a deliberate deviation.)
- §7 config-service `native` wrinkle → Task 1 keeps `spring.profiles.active=native`; Task 3 runs `native,dev`; Task 4 runs `native,docker,dev`. ✓
- §8 security unchanged → no SecurityConfiguration edits in any task. ✓
- §9 local-run ergonomics → Task 3. ✓
- §10 docker-compose → Task 4. ✓
- §11 docs → Task 5 (READMEs) + Task 6 (TODO). ✓
- §12 tests → corrected: no changes needed (note under header); regression in Task 7. ✓
- §13 validation → Task 7. ✓
- §15 open questions → resolved: `info.java/os/process` left enabled (Task 1 keeps them); discovery `devtools.livereload` moved to the dev overlay (Tasks 1 & 2). ✓

**Placeholder scan:** No TBD/TODO/“add appropriate …”. Every edit shows exact before/after content and exact commands.

**Type/value consistency:** Profile names (`dev`, `prod`, `native`, `docker`), property keys, and `SPRING_PROFILES_ACTIVE` values are identical across Tasks 1–5 and the READMEs (`docker,dev`; config-service `native,docker,dev`; locked-down `…,prod`).
