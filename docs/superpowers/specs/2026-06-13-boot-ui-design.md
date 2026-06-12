# Add boot-ui developer console — design spec

**Date**: 2026-06-13
**Status**: Proposed
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Functional Improvements → "Add boot-ui (requires dev/prod profiles)" (line 15) — <https://github.com/jdubois/boot-ui>
**Services affected**: `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service` (the three config-server clients) plus `services/pom.xml`, `central-config/application-dev.properties`, `tools/jmeter/` (exposure plan + README), service/root READMEs, and `TODO.md`

## 1. Context

[boot-ui](https://github.com/jdubois/boot-ui) (`com.julien-dubois.bootui:bootui-spring-boot-starter`, on Maven Central) is an embedded, **local-only developer console** for Spring Boot: a Vue.js UI served in-process at `/bootui` (its API at `/bootui/api`), with panels for health, beans, configuration, loggers, mappings, metrics, threads, SQL tracing, startup timeline, JDBC pools, Flyway/Liquibase, exceptions, HTTP exchanges, and more.

Its activation model maps almost exactly onto the dev/prod posture this repo adopted in `339335fc` (`docs/superpowers/specs/2026-06-10-dev-prod-profiles-design.md`). boot-ui `AUTO` mode activates on the `dev`/`local` profiles (or when DevTools is on the classpath) and **force-disables itself on `prod`/`production`** — the same secure-by-default split, expressed by the tool itself. It is therefore a natural addition now that the prerequisite profiles exist.

This spec adds boot-ui to the three API services as a dev-only tool, mirroring how `spring-boot-devtools` is already wired, with zero security-code changes.

## 2. Goals

- Make the boot-ui console available on `asapp-authentication-service`, `asapp-tasks-service`, and `asapp-users-service` during development.
- Reach it both from a local `mvn spring-boot:run` and from the `docker-compose` dev stack (`docker,dev`).
- Keep it strictly dev-only: never active under `prod`, consistent with the existing tooling-gating model.
- Add no security-code changes; rely on boot-ui's own filter chain and localhost gate.
- Keep the existing test suite behaviorally unchanged, and extend the existing lock-down regression surfaces (`DevToolingLockdownIT`, `asapp-tooling-exposure.jmx`) to cover boot-ui.
- De-risk the one real unknown — boot-ui (built against Spring Boot 4.1.x) on this repo's **4.0.5** — before touching three modules.

## 3. Non-goals

- **Not the config-service or discovery-service.** They are thin infrastructure with little to inspect, and discovery already ships the Eureka dashboard. boot-ui is added only to the three API services.
- **No security-code changes.** No edits to any `SecurityConfiguration`, filter chain, or whitelist (see §7).
- **No prod artifact exclusion.** boot-ui ships in the build like DevTools and is gated at runtime by its `prod` force-disable; it is not stripped from the jar/image via a Maven profile (the B alternative, considered and rejected).
- **No Spring Boot 4.1 upgrade.** That is the v0.5.0 goal. This change targets the current 4.0.5; if boot-ui is incompatible, §11 defines the fallback rather than pulling the upgrade forward.
- **No management-port / Actuator security changes.** boot-ui reads Actuator data in-process; the Basic-auth'd management port is untouched and irrelevant to it (see §7).
- **No new prod stack artifacts.** A locked-down stack remains reached by `…,prod`, exactly as the profiles spec established.

## 4. How boot-ui sources its data (why Actuator security is a non-issue)

boot-ui gathers everything **in-process from the Spring `ApplicationContext`**, not by calling Actuator over HTTP. Per its specification: *"Neither the management port nor HTTP Basic auth is needed. BootUI runs in-process and serves its UI locally,"* accessing beans such as `MeterRegistry`, `HealthEndpoint`, and `LoggingSystem` directly.

Where panels (Loggers, Beans, Conditions, Mappings) are described as sourcing "from Actuator endpoints," boot-ui invokes the Actuator **endpoint beans** via the endpoint-invoker mechanism — not an HTTP `GET /actuator/...`. Those beans exist whenever `spring-boot-starter-actuator` is on the classpath (it is, in all three services); they are gated by `management.endpoint.<id>.enabled`, **not** by `management.endpoints.web.exposure.include`, which controls only the HTTP surface.

Consequence: the repo's separate management port (809x) and its `actuatorFilterChain` HTTP Basic auth sit at the HTTP layer that boot-ui never crosses. boot-ui is unaffected by management-port security and by how narrowly Actuator HTTP exposure is set. It is also self-sufficient for infrastructure-backed panels: it installs its own `BufferingApplicationStartup` (startup timeline), contributes an in-memory `HttpExchangeRepository` and `AuditEventRepository` when absent, and wraps `DataSource` beans itself for SQL-trace. It registers only **low-precedence** `management.*` defaults, so it will not override the repo's explicit prod narrowing.

## 5. Dependency and version management

Manage the version once in `services/pom.xml` `<dependencyManagement>` (version `1.4.0`), following the same convention used for `springdoc-openapi-starter-webmvc-ui`. Declare the dependency in each of the three service POMs, mirroring the existing `spring-boot-devtools` block exactly (`runtime` scope, `optional`):

```xml
<dependency>
    <groupId>com.julien-dubois.bootui</groupId>
    <artifactId>bootui-spring-boot-starter</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Java requirement is 17+ (repo is on 25). The exact version-property style (inline vs `<bootui.version>` property) follows whatever the springdoc entry already does; POM ordering follows `.claude/rules/maven.md`.

## 6. Configuration

boot-ui needs almost no configuration — `AUTO` activation already lines up with the repo's profiles.

- **Profile gating** — left to boot-ui's `AUTO` default: active under `dev`, force-disabled under `prod`. The three services already activate `dev` for local runs (via the `spring-boot-maven-plugin`, per the profiles spec §9) and `docker,dev` in compose. No `bootui.enabled` override in main config.
- **Docker-stack reachability** — boot-ui is loopback-only by default (`bootui.allow-non-localhost=false`). A host-browser request arriving through the published port looks non-local to the container, so add to the shared dev overlay **`central-config/application-dev.properties`** (served to all three clients when `dev` is active):

  ```properties
  # boot-ui developer console — accept the docker bridge gateway so the
  # console is reachable from the host through the published port (dev only)
  bootui.trust-container-gateway=ON
  ```

  Local `mvn spring-boot:run` needs nothing — those requests are genuine loopback. The property is harmless on a local run (no container gateway to detect). It lives in the `dev` overlay, so it is never present under `prod`.
- **Secure defaults kept** — masked secrets (`bootui.mask-secrets=true`, `bootui.expose-values=MASKED`), loopback gate, and confirm-before-mutate stay at their defaults. Knobs such as `bootui.expose-values=FULL`, `bootui.read-only`, and `bootui.sql-trace.capture-parameters` are available but are **not** set by this change; they can be tuned later in the dev overlay if desired.

## 7. Security — unchanged

No edits to any `SecurityConfiguration`, filter chain, or whitelist.

- When active, boot-ui *"contributes a highest-precedence `/bootui/**` permit-all security chain"* itself, so its paths bypass the app's authenticated `/**` chain. The real gate is its localhost servlet filter (source-IP, `Host`-header, and `Origin`/`Sec-Fetch-Site` checks), independent of Spring Security.
- When inactive (`prod`, or disabled in tests), boot-ui contributes nothing. `/bootui` is **not** in any app whitelist, so it falls under the authenticated `/**` chain and is rejected with **401**. This differs from Swagger's 404-when-disabled precisely because Swagger paths *are* pre-whitelisted while `/bootui` is not — and a 401 is the stronger lock-down signal. (Exact status confirmed empirically in §8/§9.)
- The management port and its Basic auth are untouched (§4).

## 8. Tests

Two concerns, both extending existing infrastructure; no new test classes beyond the assertions below.

- **Keep the existing suite unchanged.** `spring-boot-devtools` is `scope=runtime` in every service, so it is on the *test* classpath; boot-ui's `AUTO` mode could therefore self-activate in ITs (no `prod` profile to force it off), wrapping `DataSource` beans and adding its filter chain. To keep the existing `*IT` suite behaviorally identical, pin boot-ui off in test resources for the three services:

  ```properties
  # src/test/resources/application.properties (auth, tasks, users)
  bootui.enabled=OFF
  ```

  (Exact test-resource file/mechanism confirmed during implementation — the services' test contexts already shadow main config, e.g. for the config-server import.)
- **Assert the lock-down.** Extend each service's existing `DevToolingLockdownIT` (which already forces the narrowed posture via `@TestPropertySource` and asserts Swagger/OpenAPI 404 + narrow Actuator) with a `BootUiExposure` nested class asserting `/bootui` (and `/bootui/api`) is **not reachable** — expected `401 Unauthorized`, per §7. Add `bootui.enabled=OFF` to that IT's `@TestPropertySource` to make the disabled state explicit and deterministic.

The dev-active ("console reachable") path is covered end-to-end by the JMeter plan (§9) rather than an IT, since exercising the live localhost gate and container-gateway trust is exactly what that black-box plan is for.

## 9. Load tests (JMeter)

Extend the existing `tools/jmeter/asapp-tooling-exposure.jmx`, which already asserts per-profile exposure of Swagger/OpenAPI/Actuator across the three services in two `IfController` branches:

- **Dev branch** (`-Jprofile=dev`, open tooling): add a `/bootui` sampler per service asserting **reachable (2xx**, following redirects — the UI root may redirect to `/bootui/`). Because JMeter hits the published port (non-local), this passing depends on `bootui.trust-container-gateway=ON` (§6) — so the assertion doubles as an end-to-end check of the docker-access config.
- **Prod branch** (`--profile prod`, locked down): add a `/bootui` sampler per service asserting **401** (boot-ui force-disabled → no permit-all chain → authenticated `/**`).

Samplers use the existing `${auth.context}` / `${tasks.context}` / `${users.context}` variables on the main app port (boot-ui serves on `server.port`, not the management port), numbered consistently with the existing 01–12 scheme. Update `tools/jmeter/README.md`'s plan description to list boot-ui alongside Swagger/Actuator.

The `regression` and `stress` plans get no boot-ui paths. Note for the README: the stress plan runs against the default `docker,dev` stack, where boot-ui's SQL-trace adds minor per-query overhead — the same caveat that already applies to Swagger and DEBUG logging being on in dev; for clean numbers run stress against a `docker,prod` stack (boot-ui off). This is a documented caveat, not a code change.

## 10. Documentation

- **Service READMEs** (`services/asapp-authentication-service`, `-tasks-service`, `-users-service`): document the console — URL (`http://localhost:<port>/bootui`), that it is dev-only (active under `dev`, force-disabled under `prod`), and that it is reachable both locally and from the docker dev stack. List it in each README's dev-tooling surface alongside Swagger / REST Docs / Actuator.
- **Root `README.md`**: add boot-ui to wherever dev tooling (Swagger, REST Docs) is enumerated in the Configuration & Profiles material.
- **`tools/jmeter/README.md`**: per §9.
- **`TODO.md`**: tick line 15 `[X]` once the change lands (housekeeping commit).

## 11. Rollout and version risk

The primary risk: boot-ui `1.4.0` advertises Spring Boot **4.1.x** (README) / minimum **4.x** (setup page); this repo is on **4.0.5**. Compatibility is unknown until built and run, so the work is sequenced pilot-first.

1. **Pilot on `asapp-tasks-service`**: add the `dependencyManagement` entry + the service dependency, pin `bootui.enabled=OFF` in its test resources, and extend its `DevToolingLockdownIT`. Build (`mvn clean install`), run with `dev`, confirm `/bootui` loads in a browser, and confirm `mvn verify` is green.
2. **Compatibility gate**: if it will not compile or boot on 4.0.5, **stop** and surface the finding — only one module is touched. Options then become: (a) pin an older boot-ui release compatible with 4.0.x, or (b) defer the task to v0.5.0 (the 4.1 upgrade). Do not proceed to the other services.
3. **Replicate** to `asapp-authentication-service` and `asapp-users-service` (deps + test pins + `DevToolingLockdownIT`), add the shared `central-config/application-dev.properties` line, extend the JMeter exposure plan, and update docs.

## 12. Validation

- **Build**: `mvn clean install` passes.
- **Tests**: `mvn clean verify` passes; existing ITs unchanged, the three `DevToolingLockdownIT` boot-ui assertions green.
- **Dev smoke (local)**: `cd services/asapp-tasks-service && mvn spring-boot:run`; open `http://localhost:8081/bootui`; confirm the console loads and panels populate (beans, config, loggers, SQL-trace after a request).
- **Dev smoke (compose)**: `docker-compose up -d`; confirm `/bootui` is reachable from the host browser for all three services (validates `trust-container-gateway`).
- **Prod smoke**: start a service with `…,prod`; confirm `/bootui` returns `401` and the service still passes its healthcheck.
- **Exposure plan**: `./run-tooling-exposure.sh` (dev) and `./run-tooling-exposure.sh --profile prod` (against a `docker,prod` stack) both pass with the new boot-ui samplers.

## 13. Git workflow

Work continues on the `add-boot-ui` branch. Commits sliced per concern, each green at commit time. Indicative slices:

1. `build(deps): manage boot-ui starter version` + pilot dependency on tasks-service
2. `test: pin boot-ui off in tasks-service tests and assert /bootui lockdown`
3. *(compatibility gate — pilot verified before continuing)*
4. `feat: add boot-ui developer console to auth and users services`
5. `feat(config): make boot-ui reachable from the docker dev stack` (`trust-container-gateway` in the dev overlay)
6. `test: assert /bootui lockdown in auth and users DevToolingLockdownIT`
7. `test(jmeter): assert boot-ui exposure per profile`
8. `docs: document the boot-ui developer console`
9. `docs(todo): mark boot-ui item as done`

Commit type/`!` markers finalized via the `commit-msg` skill at commit time. Before opening the PR: dispatch `architect-reviewer`, `code-reviewer`, and `security-auditor` in parallel against the branch diff; address all findings.

## 14. Open questions for the plan

- Whether to set `bootui.sql-trace.capture-parameters=true` in the dev overlay (useful for a learning project; off by default to avoid logging bound values). Leaning leave-default; trivially reversible.
- Exact test-resource mechanism for the `bootui.enabled=OFF` pin per service (main `src/test/resources/application.properties` vs an existing shared test config), confirmed against how each service's test context already overrides config-server import.
