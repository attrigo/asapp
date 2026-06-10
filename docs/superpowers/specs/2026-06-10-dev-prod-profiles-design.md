# Explicit dev/prod profiles — design spec

**Date**: 2026-06-10
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Technical Improvements → Profiles → "Introduce explicit dev and prod profiles to gate dev-only tooling and align with docker/native"
**Services affected**: all five (`asapp-authentication-service`, `asapp-config-service`, `asapp-discovery-service`, `asapp-tasks-service`, `asapp-users-service`) plus `central-config` and `docker-compose.yaml`

## 1. Context

Today there is no explicit notion of a production posture. Spring profiles in the repo describe only *how a service is wired*, never *what environment it runs in*:

- **No profile (local)** — hardcoded `localhost` URLs and secrets baked into each service's `application.properties`. Swagger UI on, DevTools on the classpath, Actuator fully exposed (`management.endpoints.web.exposure.include=*`, `heapdump.access=unrestricted`, `shutdown.access=unrestricted`, `management.info.env.enabled=true`).
- **`docker` profile** (`application-docker.properties`, every service) — env-var-driven wiring for `docker-compose`. Inherits the same wide-open tooling from the base/shared config.
- **`native` profile** (config-service only) — Spring Cloud Config Server's *filesystem* backend (`spring.cloud.config.server.native.searchLocations`). Not GraalVM; no GraalVM native build exists in any POM.

The consequence: dev-only tooling (Swagger, full Actuator, unrestricted heapdump/shutdown, `info.env`) is always on, including in the Docker stack, and there is no clean switch to lock a deployment down.

This spec introduces an **environment axis** (`dev`/`prod`) orthogonal to the existing **platform axis** (default/`docker`/`native`), and inverts the configuration to **secure-by-default**: base config is locked down, and a `dev` overlay opts tooling back on.

## 2. Goals

- Add explicit `dev` and `prod` semantics, independent of `docker`/`native`.
- Make the locked-down posture the **default** (secure-by-default): a service started with no environment profile is production-safe; tooling is opt-in via `dev`.
- Gate dev-only tooling (Swagger, Actuator exposure breadth, heapdump/shutdown access, `info.env`, verbose logging) behind `dev`.
- Keep `SecurityConfiguration` **profile-agnostic** — the entire split lives in properties.
- Compose cleanly with the existing `docker` and `native` profiles without modifying their meaning.
- Keep the local `docker-compose` stack debuggable (`docker,dev`); make a locked-down stack reachable by changing one env var.

## 3. Non-goals

- **No GraalVM native image.** "native" in this repo is the config-server filesystem backend. GraalVM native support remains a separate backlog item.
- **No `pre`/staging profile** yet. The model leaves room for it (a third environment value), but it is out of scope here.
- **No security-code changes.** Filter chains, the Actuator Basic-auth rule, the probe whitelist, and the Swagger whitelist are untouched. See §6.
- **No renaming of the `docker` profile** and no collapsing of platform into environment.
- **No new prod deployment artifacts** (k8s manifests, a prod compose override file). A locked-down stack is reached by setting `SPRING_PROFILES_ACTIVE=…,prod`; documenting that is in scope, shipping manifests is not.
- **No change to error-response detail.** `GlobalExceptionHandler` already uses fixed `ProblemDetail` messages, so it is prod-safe regardless of profile.
- **No secret management changes.** Secrets in `central-config` / `docker-compose.yaml` are out of scope; this is a tooling-gating change, not a secrets change.

## 4. Profile model

Two orthogonal axes. A running service activates **one value from each axis**, and they stack.

| Axis | Question | Values | Controls |
|---|---|---|---|
| Environment | *What posture?* | `dev`, `prod` (`pre` later) | Swagger, Actuator exposure, heapdump/shutdown access, `info.env`, logging levels |
| Platform / wiring | *Wired how / config from where?* | default (local), `docker`, `native` | URLs & credentials (localhost vs env vars), config-server backend |

Activation matrix:

| Scenario | Active profiles |
|---|---|
| Local dev (any service except config) | `dev` |
| Local dev, config-service | `native,dev` |
| Debuggable compose stack (default) | `docker,dev` |
| Debuggable compose stack, config-service | `native,docker,dev` |
| Locked-down deploy | `docker,prod` (config: `native,docker,prod`) |
| Bare `java -jar`, nothing set | locked down; config-service still `native` (see §7) |

The platform axis is **not uniform across services**: only config-service ever uses `native`. That is expected.

## 5. Secure-by-default placement

Base files hold the **locked-down** values; a sibling `application-dev.properties` re-opens tooling. The hardening touches three independent base locations, because the four "app" services split their config two ways:

- `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service` import the config server (`spring.config.import=configserver:…`, non-optional). Their Actuator/tooling config comes from **`central-config/application.properties`** (shared).
- `asapp-config-service` and `asapp-discovery-service` do **not** read the config server; each carries its own full management block in its own `application.properties`.

| Base file (locked down) | New dev overlay | Applies to |
|---|---|---|
| `central-config/application.properties` | `central-config/application-dev.properties` | auth, tasks, users (served via config server) |
| `services/asapp-config-service/src/main/resources/application.properties` | `services/asapp-config-service/src/main/resources/application-dev.properties` | config-service |
| `services/asapp-discovery-service/src/main/resources/application.properties` | `services/asapp-discovery-service/src/main/resources/application-dev.properties` | discovery-service |

**Config-server overlay mechanics**: when a client (e.g. tasks-service) activates `dev`, it sends that profile to the config server, which returns `central-config/application-dev.properties` as a high-precedence overlay on top of `central-config/application.properties`. No per-service dev file is needed for the three config-server clients. config-service and discovery-service resolve their own `application-dev.properties` locally via standard Spring profile resolution.

## 6. The property split

### 6.1 Base / prod (locked down)

Edit each of the three base files (§5) to replace the current permissive values with:

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.heapdump.access=none
management.endpoint.shutdown.access=none
management.info.env.enabled=false
```

Additionally, in **`central-config/application.properties` only** (Swagger lives only in the three API services, which read central-config):

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

Notes:
- `management.endpoint.health.show-details=when_authorized` stays as-is (already prod-safe; detailed health only for authenticated callers).
- `management.info.java.enabled` / `os.enabled` / `process.enabled` may remain enabled — they expose build/runtime metadata, not config/secrets. (Final call during implementation; the only `info.*` toggle this spec mandates is `env`.)
- config-service's own `application.properties` also currently sets `management.endpoints.web.exposure.include=*` etc.; it gets the same lock-down. Its `spring.profiles.active=native` line stays (see §7).

### 6.2 `application-dev.properties` (re-opens tooling)

Each of the three dev overlays sets:

```properties
management.endpoints.web.exposure.include=*
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
management.info.env.enabled=true
logging.level.com.bcn.asapp=DEBUG
logging.level.org.springframework.jdbc.core=DEBUG
```

And, in **`central-config/application-dev.properties` only**:

```properties
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```

`logging.level.org.springframework.jdbc.core=DEBUG` surfaces Spring Data JDBC SQL in dev; it is harmless where there is no datasource (discovery-service). Per-service log package levels (`logging.level.com.bcn.asapp.<service>=…`) currently sit in the per-service `central-config/<service>.properties` and per-service `application.properties`; the broad `com.bcn.asapp=DEBUG` dev override is sufficient and avoids touching those.

### 6.3 What does NOT move

- `application-docker.properties` (all services + central-config) — pure wiring; unchanged.
- All `localhost`/secret values in the base files — wiring, not tooling; unchanged.

## 7. config-service `native` wrinkle

`native` selects the config server's filesystem backend and is **structurally required for config-service in every environment** (dev and prod alike) — without it the server has no backend.

Resolution:
- Keep `spring.profiles.active=native` baked into `asapp-config-service/.../application.properties` as the structural default. A bare `java -jar` therefore runs `native` + locked-down tooling — the correct secure-by-default behavior.
- Explicit runs **compose** `native` with the environment profile rather than replacing it:
  - Local: activate `native,dev` (via the Maven plugin, §9).
  - Compose: `SPRING_PROFILES_ACTIVE=native,docker,dev`.
  - Locked-down deploy: `native,docker,prod`.

Because both the Maven `spring-boot.run.profiles` and the `SPRING_PROFILES_ACTIVE` env var *replace* (not append to) the file's `spring.profiles.active`, every explicit activation for config-service must list `native` itself. This is the one service where the environment profile is never activated alone.

## 8. Security — unchanged

No edits to any `SecurityConfiguration`, filter chain, or whitelist. Rationale, per endpoint class:

- **Actuator Basic auth** stays on in all environments. `actuatorFilterChain` (`@Order(2)`) keeps requiring HTTP Basic for every Actuator endpoint except `/health` (permitAll). The dev/prod difference is carried entirely by *exposure* and *access*, never by *authentication* — so the same auth rules run everywhere and dev exercises what prod enforces.
- **Probes** (`/readyz`, `/livez`, from `management.endpoint.health.probes.add-additional-paths=true`) stay in `MANAGEMENT_WHITELIST_URLS` (permitAll) in all environments. Orchestrators (the `docker-compose` healthchecks hit `…/readyz`; future k8s kubelet) cannot present credentials, and the probes expose only `UP`/`DOWN`.
- **Swagger whitelist** (`ROOT_WHITELIST_URLS`: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`) stays static. In prod, springdoc is disabled (§6.1), so those paths 404 and the whitelist is harmless dead weight; no need to make it profile-conditional.

## 9. Local-run ergonomics

Secure-by-default means `dev` must be explicitly activated for local development. Wire it into each service's `spring-boot-maven-plugin` so `mvn spring-boot:run` activates it automatically:

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

- auth, discovery, tasks, users → `dev`.
- config-service → `native,dev` (must keep `native`, per §7).

This preserves "no profile = locked down" (a packaged jar or container with nothing set is prod-safe) while keeping the common local command frictionless. The exact insertion point and whether the plugin block already exists per POM is confirmed during implementation; POM ordering follows `.claude/rules/maven.md`.

## 10. docker-compose

Update `SPRING_PROFILES_ACTIVE` in `docker-compose.yaml` to the debuggable stack:

| Service | Before | After |
|---|---|---|
| `asapp-authentication-service` | `docker` | `docker,dev` |
| `asapp-config-service` | `native,docker` | `native,docker,dev` |
| `asapp-discovery-service` | `docker` | `docker,dev` |
| `asapp-tasks-service` | `docker` | `docker,dev` |
| `asapp-users-service` | `docker` | `docker,dev` |

A locked-down stack is reached by setting the corresponding `…,prod` value — documented in the READMEs (§11), not shipped as a default or override file.

## 11. Documentation

- **Service READMEs** (`services/asapp-*/README.md`) and **root `README.md`**: document the two-axis model — what `dev`/`prod` control, how to run locally (`mvn spring-boot:run` activates `dev`), what the `docker-compose` stack defaults to (`docker,dev`), and how to run a locked-down stack (`…,prod`). Where a README lists Swagger/Actuator URLs, note they are dev-only.
- **`TODO.md`**: tick the Profiles item under v0.4.0 → Quick Wins → Technical Improvements as `[X]` once the change lands (separate housekeeping commit). Line 40's "remove docs/guidelines references" item is unrelated and untouched.

## 12. Tests

The base context is now locked down, so context-loading tests run prod-safe by default. Tests that assume live dev tooling must opt into `@ActiveProfiles("dev")`.

Known impact (exact set confirmed during implementation):

- **`OpenApiEndpointsIT`** (auth, tasks, users) — asserts the OpenAPI/Swagger endpoints are served. With springdoc disabled in the base, these must add `@ActiveProfiles("dev")` (or an equivalent test property) so springdoc is enabled.
- **`SecurityConfigurationIT`** (auth, tasks, users) — review any assertion that expects a live Swagger endpoint (e.g. a `2xx`/non-`401` on `/v3/api-docs`). With springdoc disabled the path 404s; a permitAll assertion that checks "not unauthorized" still holds, but one that checks `200` does not. Add `@ActiveProfiles("dev")` where needed.
- **Other `*IT` / context tests** — most do not touch Swagger or Actuator exposure and need no change. Any test that relies on `exposure=*` or on a specific Actuator endpoint being exposed gets `@ActiveProfiles("dev")`.
- **config-service tests** — verify the existing test config still activates `native` (its test resources already set profiles); confirm no regression from the base lock-down.

No new test classes are introduced by this change; edits are limited to profile activation on existing tests.

## 13. Validation

- **Build**: `mvn clean install` passes.
- **Tests**: `mvn clean verify` passes after the §12 profile annotations are added.
- **Secure-by-default smoke**: start a service with no environment profile (or `prod`); confirm `/swagger-ui.html` 404s, `GET /actuator` exposes only `health,info,prometheus`, `POST /actuator/shutdown` is unavailable, and `/actuator/info` omits env.
- **Dev smoke**: start with `dev`; confirm Swagger UI loads, full Actuator exposure returns, and `com.bcn.asapp` DEBUG logging appears.
- **Compose smoke**: `docker-compose up -d`; confirm the stack comes up healthy (probes unauthenticated) and Swagger is reachable (because the stack is `docker,dev`).
- **Prod-compose smoke (optional)**: set one service to `…,prod`, restart, confirm Swagger 404s while the service still passes its healthcheck.

## 14. Git workflow

Single feature branch (current branch is `add-boot-ui`; this work gets its own branch off `main`). Commits sliced per concern, each green at commit time. Indicative slices:

1. `refactor(config)!: lock down Actuator/Swagger by default across base config` (the three base files)
2. `feat(config): add dev profile overlays re-enabling tooling` (the three `application-dev.properties`)
3. `build: activate dev profile for local spring-boot:run` (Maven plugin per service)
4. `feat(docker): run compose stack with docker,dev profiles`
5. `test: activate dev profile on tests asserting live dev tooling`
6. `docs: document dev/prod profile model in READMEs`
7. `docs(todo): mark dev/prod profiles item as done`

Commit type/`!` markers finalized via the `commit-msg` skill at commit time (the base lock-down is a behavioral change for anyone relying on default-open tooling, hence the `!` candidate on slice 1).

Before opening the PR: dispatch `architect-reviewer`, `code-reviewer`, and `security-auditor` in parallel against the branch diff; address all findings.

## 15. Open questions for the plan

- Whether `management.info.java/os/process.enabled` should also be gated to `dev` (§6.1) or left on in prod — leaning "leave on" (metadata, not secrets).
- Whether discovery-service's existing `spring.devtools.livereload.enabled=true` (in its base `application.properties`) should move to the dev overlay — it is a dev-only convenience and is a natural fit for `application-dev.properties`, but DevTools is already inert in packaged jars, so it is low-stakes either way.

## 16. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-10-dev-prod-profiles.md`) were written before implementation. The core model shipped substantially as designed — the two-axis `dev`/`prod` × default/`docker`/`native` split, secure-by-default base, the three base lock-downs and three `dev` overlays, `SecurityConfiguration` left untouched, the Maven `spring-boot:run` wiring (§9), and the `docker,dev` compose default (§10) all landed as specified. The §15 open questions resolved as anticipated: `management.info.java/os/process` stay enabled in prod, and discovery-service's `spring.devtools.livereload.enabled` moved to its dev overlay. The sections above record the original design intent; **the canonical configuration is the current state of the property files, POMs, `docker-compose.yaml`, and `*IT` tests on this branch**, not this document. Notable deltas:

- **SBOM endpoint added to the locked-down exposure (new, not in §6.1).** The base exposure list became `health,info,prometheus,sbom` rather than the spec's `health,info,prometheus`. The CycloneDX bill-of-materials is build metadata (no secrets or config) and is useful for supply-chain / vulnerability scanning, so it is safe under the prod posture; the mutating BOM-refresh endpoint stays dev-only. Applied to all three base configs (`central-config`, config-service, discovery-service).

- **The `docker` platform profile was locked down too (reverses §6.3's "`application-docker.properties` … unchanged").** §6.3 declared the docker overlays pure wiring and out of scope. Manual review found that config-service's and discovery-service's `application-docker.properties` each carried a permissive management block (`exposure=*`, heapdump/shutdown unrestricted, `info.env` on) that overrode the locked base under *any* docker activation — including `docker,prod` — leaving a prod-safety hole the original design missed. `fix(config)!` (`346d525a`) removed those blocks so the locked base holds under `docker,prod`, while the dev overlay still re-opens tooling under `docker,dev`. (auth/tasks/users had no such block — they inherit tooling from `central-config`.)

- **New integration tests were added (reverses the plan-header note "No Java or test edits are part of this plan").** The plan reasoned that self-contained, permissive test resources shadow the locked main config, so the lock-down needed no test changes — and that held for the *existing* suite (`OpenApiEndpointsIT`, `SecurityConfigurationIT`, and the config-service ITs all passed unchanged, confirming the §12 correction). But a new `DevToolingLockdownIT` was added to all five services to cover the locked posture directly: it forces the narrowed exposure via `@TestPropertySource` (since test resources default permissive) and asserts Swagger/OpenAPI return `404` and that `/actuator` exposes only the `health`/`info`/`prometheus`/`sbom` links. The class was first named `SecureByDefaultEndpointsIT`, then renamed to `DevToolingLockdownIT` for sibling-naming consistency. So the prod posture is now automated, not only manually smoke-tested (§13).

- **A JMeter tooling-exposure plan was added (beyond §13's manual smoke).** The `*IT` tests only *simulate* the posture via `@TestPropertySource`; nothing exercised the real profile, config-server overlay, and compose wiring end-to-end. `tools/jmeter/asapp-tooling-exposure.jmx` fills that gap: `-Jprofile=dev|prod` asserts open vs locked-down exposure of Swagger, OpenAPI docs, and Actuator across auth/tasks/users. It shipped with `run-tooling-exposure.sh --profile` (default `dev`), a shared `evaluate_gate` extracted into `scripts/common.sh`, and management-port / Actuator-credential / profile tunables in `local.properties`. (Interim `info` env-endpoint assertions were dropped during refinement.)

- **Documentation went broader than §11.** Rather than the planned per-README "Profiles" subsection, the docs were reworked into a dedicated **Configuration & Profiles** section across the root and all five service READMEs, documenting per-service property resolution (config-server clients vs self-contained) with precedence diagrams, listing the `application-dev.properties` overlay in every Property Sources table, and renaming config-service's served-config merge section to **Served Configuration Resolution**. The service READMEs also flag **REST Docs** as dev-only alongside Swagger and Actuator — a tooling surface the spec's gating list (§6) did not mention.

**For future profile or tooling-gating edits**, treat the current property files (`central-config/application{,-dev}.properties` plus each service's `application{,-dev,-docker}.properties`), the per-service `DevToolingLockdownIT`, and the JMeter `asapp-tooling-exposure.jmx` plan as the template; this spec is preserved as a record of the original design intent and the manual-review reasoning that reshaped it.
