# Circuit Breaker on the Tasks HTTP Client — Design

**Date:** 2026-06-14
**Status:** Implemented
**Targets:** `asapp-users-service` (and its `pom.xml` / `services/pom.xml` dependency management)

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Use circuit breaker pattern

The prior task ("Refactor REST clients by declarative HTTP clients", line 29)
left a clean seam for this one. Today:

- `asapp-rest-clients` is a **pure-contract** library: the `TasksHttpClient`
  `@HttpExchange` interface plus response DTOs. No wiring, no resilience.
- The single consumer, `asapp-users-service`, registers the client
  (`@ImportHttpServices(group = "tasks")`), configures it generically in
  `RestClientConfiguration` (redirect-disabled JDK factory, `JwtInterceptor`,
  optional `LoadBalancerInterceptor`), and bridges to the domain through
  `TasksGatewayAdapter` (implements the `TasksGateway` port).
- `TasksGatewayAdapter.getTaskIdsByUserId(...)` already **degrades gracefully**:
  `try { … } catch (RestClientException) { log.warn; return List.of(); }`. The
  previous design explicitly named this the seam the circuit breaker builds on.

The goal: when the Tasks Service is unhealthy, **fail fast** instead of hammering
it on every user lookup, and **recover automatically** — while preserving the
exact external behavior (empty task list → user lookup still succeeds).

---

## 2. Decisions (from brainstorming + research)

1. **Library: standalone Resilience4j (`resilience4j-spring-boot4`), not the
   Spring Cloud CircuitBreaker abstraction.** Both are Spring Boot 4 compatible
   (research below), so compatibility — the original tiebreaker — is moot. The
   standalone starter wins on: declarative `@CircuitBreaker` + properties-driven
   config (config-file tunables, no `Customizer` bean), highest learning value
   (raw Resilience4j is the stated project purpose), and auto-wired Micrometer
   metrics. Cost: one self-versioned dependency family (`io.github.resilience4j`),
   managed via its BOM.
2. **Retry is a separate task (line 31), handled by Spring Framework 7 native
   `@Retryable`.** SF7 ships `@Retryable` / `@ConcurrencyLimit` / `RetryTemplate`
   in core (`org.springframework.core.retry`, `org.springframework.resilience.annotation`)
   — but **no** circuit breaker, so the breaker must come from Resilience4j
   either way. This design leaves room for retry to slot *inside* the breaker
   later.
3. **The breaker lives on the consumer side, in `TasksGatewayAdapter`.** The
   `asapp-rest-clients` library stays a pure contract — no resilience dependency
   leaks into it.
4. **Graceful degradation is preserved but moves into a declarative fallback**
   (see §3.2). External behavior is unchanged; the difference is the breaker now
   *observes* failures, fast-fails when open, and auto-recovers.
5. **Tuning lives in the consumer's local `application.properties`**, co-located
   with `spring.http.serviceclient.tasks.base-url` — consistent with the prior
   design's choice to keep client config local rather than in central-config.

### Research backing (verified 2026-06)

- Spring Cloud 2025.1.0 "Oakwood" is built on Spring Framework 7 / Spring Boot 4;
  its `spring-cloud-circuitbreaker` is at 5.0.x (Resilience4j 2.3.0). A *new*
  module backed by SF7 resilience was added, but since SF7 has no real breaker
  it is thin; the full breaker remains the Resilience4j-backed starter.
- Resilience4j ships a dedicated **`resilience4j-spring-boot4`** module, latest
  **2.4.0** on Maven Central (the `-spring-boot3` starter targets Boot 3).
- Spring's own Boot 4 guidance pairs **native `@Retryable` (retry) + Resilience4j
  (circuit breaker)** directly.

---

## 3. Detailed Design

### 3.1 Where it sits

`asapp-users-service` → `infrastructure/tasks/out/TasksGatewayAdapter` →
`getTaskIdsByUserId(UserId)`. The adapter is a `@Component`, so Spring AOP
proxies it and the Resilience4j aspect engages.

### 3.2 Behavior — fallback semantics (the key design point)

The current `try/catch` **swallows** `RestClientException` before it can reach
the AOP boundary, so the breaker would never see a failure and would never trip.
Degradation therefore moves into a declarative fallback:

```java
@CircuitBreaker(name = "tasks", fallbackMethod = "emptyTasksFallback")
public List<UUID> getTaskIdsByUserId(UserId userId) {
    var tasks = tasksHttpClient.getTasksByUserId(userId.value());
    if (tasks == null) {                       // valid "no tasks" response — NOT a breaker failure
        logger.warn("Received null response body from Tasks Service for user {}. Returning empty list.", userId.value());
        return List.of();
    }
    return tasks.stream()
                .map(TasksByUserIdResponse::taskId)
                .toList();
}

private List<UUID> emptyTasksFallback(UserId userId, Throwable t) {
    // fires on downstream failure (recorded by the breaker) AND on open-circuit
    // fast-fail (CallNotPermittedException)
    logger.warn("Tasks Service unavailable for user {}: {}. Returning empty list.", userId.value(), t.getMessage());
    return List.of();
}
```

- `@CircuitBreaker` is `io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker`.
- The fallback method must share the source method's signature plus a trailing
  `Throwable` (Resilience4j matches by type; a single `Throwable` overload covers
  both downstream exceptions and `CallNotPermittedException`).
- **Null body stays a success** from the breaker's perspective (empty list is a
  valid response, not a failure) and is handled inline.
- **External behavior is unchanged:** Tasks Service down → empty list → user
  lookup still succeeds. What changes: failures are counted, an open circuit
  fast-fails, and the breaker auto-recovers via half-open probing.

### 3.3 Which failures trip the breaker

- **Record (count as failure):** I/O / connect / read timeouts
  (`org.springframework.web.client.ResourceAccessException`) and 5xx
  (`HttpServerErrorException`) — i.e. the downstream is unhealthy.
- **Ignore (do not trip):** 4xx (`HttpClientErrorException`) — a bad request is
  *our* fault, not the downstream being sick. Configured via `ignore-exceptions`.

---

## 4. Dependency & configuration

### 4.1 Dependency management

- Import the **`io.github.resilience4j:resilience4j-bom`** (2.4.0) in
  `services/pom.xml` `<dependencyManagement>` (alphabetical placement per the
  POM conventions), so the starter is declared version-less downstream.
- Add **`io.github.resilience4j:resilience4j-spring-boot4`** to
  `asapp-users-service/pom.xml` (in the correct comment-delimited group).
- AOP: the breaker is annotation/AOP-driven. Confirm during implementation
  whether `resilience4j-spring-boot4` transitively brings the AOP support or
  `org.springframework.boot:spring-boot-starter-aop` must be added explicitly.
- Metrics: `resilience4j-micrometer` is expected transitively; confirm and add
  explicitly if not present.

### 4.2 Tuning (local `application.properties`)

Starting values, all tunable:

```properties
resilience4j.circuitbreaker.instances.tasks.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.tasks.sliding-window-size=10
resilience4j.circuitbreaker.instances.tasks.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.tasks.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.tasks.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.tasks.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=org.springframework.web.client.HttpClientErrorException
```

Lives in `asapp-users-service/src/main/resources/application.properties` (base);
inherited by the `docker` overlay. Test config may override thresholds for
determinism (see §6).

---

## 5. Observability

- **Metrics:** `resilience4j-micrometer` auto-publishes breaker metrics (state,
  call counts, failure rate, slow-call rate) to the existing Micrometer /
  Prometheus registry → Grafana.
- **Health:** register the breaker health indicator
  (`management.health.circuitbreakers.enabled=true` +
  `register-health-indicator=true`) but with
  **`allow-health-indicator-to-fail=false`**. An OPEN circuit here is *intended
  graceful degradation*, so it should appear in `/actuator/health` detail
  **without** flipping users-service to `DOWN` and failing its readiness probe.
- Exact property names/placement confirmed during implementation.

---

## 6. Testing strategy

- **Unit (Mockito) — `TasksGatewayAdapter`:** happy-path mapping; null body →
  empty list; and the fallback directly:
  `emptyTasksFallback(userId, new ResourceAccessException(...))` → empty list.
  (Note: the bare method now *throws* on `RestClientException` instead of
  swallowing it — the swallow behavior moved to the fallback, which only runs
  under the proxy.)
- **Integration (breaker engaged):** a focused Spring test that drives the
  *proxied* adapter — mock `TasksHttpClient` to throw repeatedly, assert the
  breaker transitions to OPEN past the threshold and subsequent calls fast-fail
  to empty list **without** invoking the client. Assert via `CircuitBreakerRegistry`
  state, or drive it deterministically with `transitionToOpenState()`.
- **Unchanged:** existing users→tasks integration / E2E tests (mockserver) should
  stay green — they exercise behavior (down → empty list), which is preserved.
- **Verify:** `mvn clean verify`. ITs are slow — confirm with the developer
  before running them, per project convention.

---

## 7. Documentation

No `CHANGELOG` exists in the repo (generated at release time), so there is
nothing to update there.

**`services/asapp-users-service/README.md` (primary):**
- *Technology Stack* — add `- **Resilience**: Resilience4j (circuit breaker)`.
- *Architecture* — add a short `### Resilience` subsection: the `tasks` gateway
  is guarded by a Resilience4j circuit breaker; on downstream failure or open
  circuit it degrades to an empty task list so user lookup still succeeds; state
  and metrics surface via Actuator + Prometheus; tunables live in
  `application.properties`.

**`TasksGatewayAdapter` Javadoc (code-level):** update the class summary
(currently "logs a warning and returns an empty list…") to note the breaker
fast-fails when the Tasks Service is unhealthy and auto-recovers, with the
fallback returning the empty list.

**`libs/asapp-rest-clients/README.md`:** no structural change. One-word accuracy
tweak to the "wiring-free contracts" bullet — base URL, auth, load balancing,
**and resilience** are owned by the consuming service.

**Root `CLAUDE.md`** (editable — at repo root, not under the gated `.claude/`):
add Resilience4j to the Stack line.

**`.claude/rules/development-patterns.md`** (gated — text **prepared** for the
developer to apply): extend "Service-to-Service Calls" — calls go through the
declarative client; the gateway adapter is guarded by a `@CircuitBreaker` that
degrades to a neutral result on failure / open circuit; never swallow the
exception before it reaches the breaker.

**Root `README.md`:** no change — line 303 still accurately describes the library
as declarative HTTP client contracts; the library gains no resilience.

**`TODO.md`:** tick line 30 (`[ ]` → `[X]`).

**`api-guide.adoc` / generated REST docs:** no change — client-side only, no
endpoint changes.

---

## 8. Sequencing

1. **Dependencies:** import `resilience4j-bom` in `services/pom.xml`; add
   `resilience4j-spring-boot4` to `asapp-users-service/pom.xml`; confirm AOP /
   micrometer transitivity.
2. **Adapter:** annotate `getTaskIdsByUserId` with `@CircuitBreaker`, remove the
   in-method `try/catch`, add the `emptyTasksFallback` method, update the
   Javadoc.
3. **Config:** add the `resilience4j.circuitbreaker.instances.tasks.*` block and
   the health-indicator settings to `application.properties`.
4. **Tests:** add fallback unit coverage; add the breaker-engaged integration
   test; confirm existing IT/E2E stay green.
5. **Verify:** `mvn clean verify` (confirm before running slow ITs).
6. **Docs:** users-service README (+ Javadoc), library README tweak, `CLAUDE.md`
   Stack line; prepare the `.claude/rules` text for the developer.
7. **TODO.md:** tick line 30.

---

## 9. Out of scope

- **Retry** (TODO line 31) — separate task, via SF7 native `@Retryable`. This
  design leaves the breaker as the seam retry will wrap *inside* (the breaker
  records one failure per exhausted-retry).
- **Library rename** `asapp-rest-clients` → `asapp-http-clients` (TODO line 32).
- The Spring Cloud CircuitBreaker abstraction (rejected in §2).
- Central-config externalization of resilience tuning (kept local in §2).
- Slow-call configuration beyond defaults (can be added when tuning).
- New clients (auth, users) — none exist today.

---

## 10. Acceptance criteria

- `TasksGatewayAdapter.getTaskIdsByUserId` is guarded by a Resilience4j
  `@CircuitBreaker(name = "tasks")` with an `emptyTasksFallback`; the in-method
  `try/catch` is gone and failures reach the breaker.
- The breaker trips on repeated I/O / 5xx failures, fast-fails while OPEN, and
  auto-transitions to HALF_OPEN → CLOSED on recovery; 4xx do not trip it.
- External behavior preserved: Tasks Service down → empty list → user lookup
  still succeeds.
- Breaker metrics reach Prometheus; an OPEN circuit shows in `/actuator/health`
  detail **without** marking users-service `DOWN`.
- `asapp-rest-clients` carries no resilience dependency.
- Docs updated per §7; `.claude/rules` change prepared for the developer.
- `mvn clean verify` is green.

---

## 11. Open questions for the plan

- Whether `resilience4j-spring-boot4` 2.4.0 brings AOP support transitively or
  `spring-boot-starter-aop` must be added explicitly.
- Whether `resilience4j-micrometer` is transitive (else declare it).
- Exact health-indicator property names/placement for the
  "visible but non-failing" behavior (`register-health-indicator`,
  `management.health.circuitbreakers.enabled`, `allow-health-indicator-to-fail`).
- Cleanest way to engage the proxied breaker in the integration test (Spring
  context slice vs. registry-driven `transitionToOpenState()`).

---

## 12. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-14-circuit-breaker-http-clients.md`) were written before implementation. The breaker shipped substantially as designed — standalone Resilience4j (`resilience4j-spring-boot4` 2.4.0) guards the consumer side: `TasksGatewayAdapter.getTaskIdsByUserId` carries `@CircuitBreaker(name = "tasks", fallbackMethod = "emptyTasksFallback")`, the in-method `try/catch` is gone so failures reach the breaker, 5xx/I/O are recorded while 4xx are listed in `ignore-exceptions` (§3.2–§3.3); tuning lives in the consumer's local `application.properties` (§4.2); the health indicator is visible-but-non-failing (`management.health.circuitbreakers.enabled=true` + `register-health-indicator=true` + `allow-health-indicator-to-fail=false`, §5); breaker metrics reach Prometheus; the `asapp-rest-clients` library gained no resilience dependency (§2.3/§10); and the users README + adapter Javadoc were updated (§7). The §11 open questions resolved benignly — `resilience4j-micrometer` is transitive (no explicit declaration needed), and the health-indicator properties applied as named. The sections above record the original design intent; **the canonical implementation is the current state of `TasksGatewayAdapter`, the `resilience4j.*` block in `application.properties`, the `CircuitBreakerConfigurationIT` / `TasksGatewayAdapterIT` / `RestClientConfigurationIT` trio, and the users-service README on this branch**, not this document. Notable deltas:

- **The fallback was narrowed from catch-all to outages-only (revises §3.2/§3.3/§10).** §3.2 specified a single `emptyTasksFallback(UserId, Throwable)` that degraded *every* `Throwable` to an empty list, with 4xx merely kept from tripping the breaker via `ignore-exceptions`. In practice that blanket fallback masked client (4xx) errors and genuine bugs as "no tasks". `fix(http-clients)` (`585a9ce4`) re-typed the fallback to `throws Throwable` and degrades to an empty list **only** on `HttpServerErrorException` (5xx), `ResourceAccessException` (I/O), and the open-circuit `CallNotPermittedException`; 4xx and any unexpected exception are rethrown so the caller and error handler can surface them. The §6 test `KeepsCircuitClosed_TasksServiceReturnsClientError` (which asserted 4xx → empty list) became `PropagatesClientError_…` (4xx propagates, breaker stays CLOSED), and a `PropagatesError_TasksServiceFailsUnexpectedly` case was added. External behavior for the *outage* path (Tasks Service down → empty list → user lookup still succeeds) is unchanged.

- **Testing went well beyond §6's "one breaker-engaged integration test," onto an embedded MockServer (revises §6 + plan Task 4).** §6/plan envisioned a Mockito unit fallback test plus a single breaker IT that mocks `TasksHttpClient`. The shipped suite instead drives the **real declarative client against an embedded MockServer** (real 5xx/4xx/connection-drop responses) and is split by concern into three ITs: `CircuitBreakerConfigurationIT` (closed-below-minimum, opens-past-threshold-and-stops-calling, ignores-4xx, **and closes again on recovery** — the half-open→closed case the spec only described in §10), `TasksGatewayAdapterIT` (the fallback degrade/propagate branches), and `RestClientConfigurationIT`. That last one **replaced the prior declarative-clients task's `RestClientConfigurationTests`** (the in-process `com.sun.net.httpserver.HttpServer` slice), rewriting its three guarantees — load-balanced routing, redirect-not-followed, JWT propagation — onto the same MockServer harness end-to-end. `ActuatorEndpointsIT` was also extended (`1bb8d2b1`) to assert the `circuitbreakers`/`circuitbreakerevents` actuator links and the non-failing `circuitBreakers` health component, and the breaker config was mirrored into the shared test `application.properties` (revising the plan Task 3 note that test config would omit it and keep Resilience4j defaults).

- **Dependency wiring diverged from §4.1 on three points.** (1) AOP is provided by `spring-boot-starter-aspectj` (main) + `spring-boot-starter-aspectj-test` (test), not the plan's `spring-boot-starter-aop` — resolving the §11 AOP open question against Spring Boot 4's AspectJ starter. (2) The `resilience4j-bom` was imported and then **dropped as redundant** (`011a1b8b`): it managed only `resilience4j-spring-boot4`, which self-pins, so it left the tree unchanged. (3) The version is instead centralized by declaring `resilience4j-spring-boot4` directly in the services-parent `<dependencyManagement>` against a `resilience4j.version` property (`861fa7e5`), so the consumer declares it version-less.

- **A default-valued property was trimmed and a new configuration rule emerged (revises §4.2/§7).** `resilience4j.circuitbreaker.instances.tasks.sliding-window-type=COUNT_BASED` was removed from both main and test config (and the README tuning table) because it merely restates the Resilience4j default (`2ea220fa`). This was codified into a **new `.claude/rules/configuration.md`** guideline — "record decisions, not defaults" — beyond §7, which had only planned a `development-patterns.md` addendum.

- **The CLAUDE.md Stack-line edit was reverted within this task (revises §7).** Task 6 added `Resilience4j` to the root `CLAUDE.md` Stack summary, then `5a27f755` removed it again, so this task left `CLAUDE.md` net-unchanged. Resilience4j was reintroduced to the Stack line — as `Resilience4j (circuit breaker + retry)` — by the later retry work, not here.

**For future resilience or declarative-client work** (the retry TODO item slots `@Retryable` *inside* this breaker per §9), treat the current `TasksGatewayAdapter` (narrowed outages-only fallback that rethrows non-outages), the consumer-local `resilience4j.circuitbreaker.instances.tasks.*` block, the MockServer-backed IT trio, and the `.claude/rules/configuration.md` "record decisions, not defaults" guideline as the template; this spec is preserved as a record of the original design intent — including the catch-all fallback the implementation deliberately narrowed.
