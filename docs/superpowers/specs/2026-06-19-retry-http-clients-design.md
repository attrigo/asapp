# Retry on the Tasks HTTP Client — Design

**Date:** 2026-06-19
**Status:** Implemented
**Targets:** `asapp-users-service` (adapter, `application.properties` ×2, tests, docs)

> **Supersedes the Spring Framework 7 `@Retryable` premise.** An earlier revision
> of this spec specified SF7 native `@Retryable` + `@EnableResilientMethods`. That
> approach was disproven in practice: SF7's `RetryAnnotationBeanPostProcessor`
> hardcodes `setBeforeExistingAdvisors(true)`, forcing the retry advisor to be the
> **outermost** aspect with no supported property to nest it inside the Resilience4j
> breaker. A runtime advisor-chain probe confirmed retry ended up outer / breaker
> inner, so the breaker's 5xx fallback "succeeded" with an empty list on the first
> attempt and retry never engaged. This revision uses **Resilience4j `@Retry`** —
> a sibling aspect of the breaker, designed to compose, with documented order knobs.

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Use retry pattern

It is the next item after the circuit breaker (line 30), which landed in commit
`4121086b`. The circuit-breaker design reserved this seam (retry "wraps inside the
breaker (the breaker records one failure per exhausted-retry)").

Today's relevant state:

- `asapp-rest-clients` is a **pure-contract** library — the `TasksHttpClient`
  `@HttpExchange` interface plus response DTOs. No wiring, no resilience.
- `asapp-users-service`'s `TasksGatewayAdapter.getTaskIdsByUserId(UserId)` is a
  `@Component` guarded by a Resilience4j
  `@CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "emptyTasksFallback")`.
  The fallback degrades 5xx (`HttpServerErrorException`), I/O
  (`ResourceAccessException`), and open-circuit (`CallNotPermittedException`) to
  an empty list; it rethrows 4xx (`HttpClientErrorException`) and anything
  unexpected.
- Breaker tuning lives in **both** `application.properties` files
  (`resilience4j.circuitbreaker.instances.tasks.*` is mirrored into
  `src/test/resources` so `CircuitBreakerConfigurationIT` exercises prod-like
  tuning).

The goal: a **single transient blip** (a dropped connection, a momentary 503) is
retried with backoff and recovers transparently — *before* the breaker/fallback
degrades the result — while sustained failure still trips the breaker exactly as
it does today.

---

## 2. Decisions (from brainstorming + research)

1. **Mechanism: Resilience4j `@Retry`** (`io.github.resilience4j.retry.annotation.Retry`),
   confirmed by the developer. It is a sibling of the existing
   `@CircuitBreaker` aspect — same library, designed to compose, with documented
   aspect-order knobs (§4). Chosen over SF7 native `@Retryable`, which cannot be
   nested inside the breaker (see the supersede note above). Bonuses: one
   resilience library instead of two, and R4j retry **emits Micrometer metrics**
   (§8) — SF7 retry emits none.
2. **Nesting: breaker outer, retry inner** — confirmed. A transient failure that
   exhausts all attempts counts as **one** breaker failure; an OPEN breaker
   short-circuits *before* retry runs (no hammering a known-down service). The
   inverse ordering is the disproven dead-end: with the breaker inner, its 5xx
   fallback returns an empty list on the first attempt, so retry sees a
   "successful" return and never fires.
3. **No enabler class.** R4j retry auto-configures from the
   `resilience4j-spring-boot4` starter (`RetryAutoConfiguration`), exactly like the
   breaker. There is **no** `@EnableResilientMethods` equivalent to add, so this
   task introduces **zero** new Java classes — leaner than the superseded SF7
   design, which needed a dedicated `@Configuration`.
4. **What's retried mirrors what the breaker records** — transient I/O
   (`ResourceAccessException`) and 5xx (`HttpServerErrorException`), via the retry
   `retry-exceptions` whitelist. 4xx (`HttpClientErrorException`) is *our* fault,
   not a transient downstream fault — absent from the whitelist, so never retried.
5. **Backoff: 2 retries, 200 ms base, ×2 multiplier** (`max-attempts=3`,
   `wait-duration=200ms`, `enable-exponential-backoff=true`,
   `exponential-backoff-multiplier=2`). Config-driven, a safe default. The
   200 ms → 400 ms ramp fails fast enough for a user-facing GET; with 2 retries the
   peak delay is 400 ms, so no `exponential-max-wait-duration` cap is needed.
6. **Library stays a pure contract** — no retry dependency or annotation leaks into
   `asapp-rest-clients`. R4j is already on the consuming service's classpath via the
   circuit-breaker starter; no new dependency anywhere.

### Research backing (verified 2026-06-19 against the resolved jars: `resilience4j-spring6-2.4.0`, `resilience4j-spring-boot4-2.4.0`, `resilience4j-framework-common-2.4.0`)

- **`@Retry` annotation** lives in `io.github.resilience4j.retry.annotation.Retry`
  (module `resilience4j-annotations`). Attributes used here: `name` (the instance
  name, matched to `resilience4j.retry.instances.<name>.*`). `fallbackMethod` is
  **not** used — retry rethrows after exhaustion so the outer breaker records the
  failure and runs *its* fallback.
- **Auto-configuration:** `io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration`
  is on the classpath via `resilience4j-spring-boot4`. Retry is active as soon as a
  method carries `@Retry` — there is **no opt-in default switch** (contrast SF7,
  whose `maxRetries` defaulted to a value, and contrast a hypothetical "off"
  property). An *unconfigured* instance falls back to R4j's default `RetryConfig`
  (`maxAttempts=3`, retry on **all** exceptions) — which is why the test classpath
  must explicitly neutralize it (§5, §7).
- **Retry instance properties** (kebab-case, confirmed setter names on
  `CommonRetryConfigurationProperties.InstanceProperties`): `max-attempts` (Integer,
  **total** attempts including the first), `wait-duration` (Duration),
  `enable-exponential-backoff` (Boolean), `exponential-backoff-multiplier` (Double),
  `retry-exceptions` (`Class<? extends Throwable>[]` — whitelist), `ignore-exceptions`
  (blacklist).
- **Retry emits metrics:** `resilience4j-micrometer` binds `resilience4j.retry.calls`
  counters (tagged by `kind` = `successful_with_retry` / `failed_with_retry` / …),
  exported through the service's existing Prometheus setup.

---

## 3. Detailed Design

### 3.1 Where it sits

`asapp-users-service` → `infrastructure/user/out/TasksGatewayAdapter` →
`getTaskIdsByUserId(UserId)`. The adapter is already an AOP-proxied `@Component`
with the breaker aspect; adding `@Retry` engages the Resilience4j retry aspect on
the same method. Aspect order (§4) places the breaker outer.

### 3.2 The annotated method (shape, not final code)

```java
@Override
@CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "emptyTasksFallback")
@Retry(name = TASKS_CLIENT_NAME)
public List<UUID> getTaskIdsByUserId(UserId userId) {
    var tasks = tasksHttpClient.getTasksByUserId(userId.value());

    if (tasks == null) {
        logger.warn("Received null response body from Tasks Service for user {}. Returning empty list.", userId.value());
        return List.of();
    }

    return tasks.stream()
                .map(TasksByUserIdResponse::taskId)
                .toList();
}
```

- The retry **instance name reuses `TASKS_CLIENT_NAME` (`"tasks"`)** — the same
  constant the breaker uses — so retry and breaker config share one logical name.
- `@Retry` has **no `fallbackMethod`**: after the attempts exhaust it rethrows, the
  outer breaker records exactly one failure, and `emptyTasksFallback` (unchanged)
  classifies it.
- The retry *shape* (attempts, backoff, exception whitelist) lives entirely in
  `application.properties` under `resilience4j.retry.instances.tasks.*` — nothing
  retry-specific is hardcoded in the annotation beyond the instance name.

### 3.3 Failure classification (retry ∩ breaker)

| Failure | Retried? | Breaker records? | Net behavior |
|---|---|---|---|
| I/O / connect / read (`ResourceAccessException`) | yes (to `max-attempts`) | once, only if all attempts fail | retry recovers a blip; sustained → 1 breaker failure → empty list |
| 5xx (`HttpServerErrorException`) | yes | once, only if all attempts fail | same as above |
| 4xx (`HttpClientErrorException`) | **no** (not in `retry-exceptions`) | no (in `ignore-exceptions`) | propagates immediately; fallback rethrows |
| Open circuit (`CallNotPermittedException`) | **no** (breaker is outer; thrown before retry) | n/a | fast-fail → fallback → empty list |
| `null` body | n/a (success, not an exception) | no | empty list, inline |

The retry `retry-exceptions` set is intentionally the same as the breaker's
"record" set — the two stay in lockstep so classification is reasoned about once.
`retry-exceptions` is a whitelist (only assignable types retry), so no
`ignore-exceptions` is needed on the retry instance.

**Known exclusion:** *retryable* 4xx — `429 Too Many Requests` and
`408 Request Timeout` — are subclasses of `HttpClientErrorException`, so this set
does **not** retry them. That is acceptable here: these internal services do not
rate-limit, and proper 429 handling needs `Retry-After`. If a downstream ever
returns 429, add `HttpClientErrorException.TooManyRequests` to `retry-exceptions`
(and honor `Retry-After`) as a follow-up.

---

## 4. Aspect ordering — making the breaker outer

Both `@CircuitBreaker` and `@Retry` advisors implement Spring's `Ordered`, where
**a lower order value = higher precedence = the outer aspect**. The wiring we want
is `CircuitBreaker( Retry( call ) )`, so the breaker needs the **lower** value.

**Verified defaults (resilience4j 2.4.0):**

| Aspect | `getOrder()` source | Default value |
|---|---|---|
| CircuitBreaker | `CircuitBreakerConfigurationProperties.circuitBreakerAspectOrder` | `Ordered.LOWEST_PRECEDENCE - 4` = `2147483643` |
| Retry | `RetryConfigurationProperties.retryAspectOrder` | `Ordered.LOWEST_PRECEDENCE - 5` = `2147483642` |

By default Retry (`MAX-5`) < CircuitBreaker (`MAX-4`) ⇒ **Retry is outer, breaker
inner** — the wrong way. (This is consistent with R4j's documented default
"Retry is the outermost aspect", which confirms the lower-value-is-outer direction
and resolves the apparent contradiction in some prose docs.)

**Override — swap the two defaults** so the breaker takes the lower value:

```properties
## Aspect order — circuit breaker wraps retry (lower value = outer aspect)
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2147483642
resilience4j.retry.retry-aspect-order=2147483643
```

This is the surgically minimal change: it keeps both aspects in R4j's intended
precedence neighborhood (innermost relative to any other / future aspect) and only
flips them relative to each other. The same two lines go in **both**
`application.properties` files so test and prod wiring are identical.

**Proven, not assumed.** The §7 IT asserts that a single sustained-failure call
hits the downstream `max-attempts` times yet the breaker records exactly **one**
failure, and that the recovery case returns the real ids. If the breaker were inner,
its fallback would short-circuit the first attempt: the downstream would be hit
once and the recovery case would return an empty list — both assertions would fail.

**Contingency** (not expected to trigger, given the verified defaults): if the proof
test shows the wrong nesting, flip the two order values (or fall back to structural
nesting — move the HTTP call into a second `@Retry`-only `@Component` and have the
`@CircuitBreaker` adapter delegate to it; two proxies guarantee breaker-outer). See
the plan's contingency step.

---

## 5. Configuration & tunability

Per `.claude/rules/configuration.md` (record decisions, not defaults), the retry
instance pins the policy that deviates from R4j defaults and the headline aspect
order. R4j retry has **no global off-switch**, so — unlike the superseded SF7
design, which left the test classpath untouched — both classpaths must carry retry
config, with the **test classpath neutralizing it** (`max-attempts=1`).

### 5.1 Production — `src/main/resources/application.properties`

Append after the circuit-breaker block:

```properties
## Tasks service client retry properties
resilience4j.retry.instances.tasks.max-attempts=3
resilience4j.retry.instances.tasks.wait-duration=200ms
resilience4j.retry.instances.tasks.enable-exponential-backoff=true
resilience4j.retry.instances.tasks.exponential-backoff-multiplier=2
resilience4j.retry.instances.tasks.retry-exceptions=org.springframework.web.client.ResourceAccessException,org.springframework.web.client.HttpServerErrorException
## Aspect order — circuit breaker wraps retry (lower value = outer aspect)
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2147483642
resilience4j.retry.retry-aspect-order=2147483643
```

`max-attempts=3` = the initial call + 2 retries.

### 5.2 Test — `src/test/resources/application.properties`

Append after the mirrored circuit-breaker block:

```properties
## Tasks service client retry properties (disabled by default — max-attempts=1; TasksGatewayAdapterIT opts in)
resilience4j.retry.instances.tasks.max-attempts=1
resilience4j.retry.instances.tasks.wait-duration=10ms
resilience4j.retry.instances.tasks.retry-exceptions=org.springframework.web.client.ResourceAccessException,org.springframework.web.client.HttpServerErrorException
## Aspect order — circuit breaker wraps retry (lower value = outer aspect)
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2147483642
resilience4j.retry.retry-aspect-order=2147483643
```

- **`max-attempts=1` is the off-switch** (1 total attempt = 0 retries). It keeps
  `CircuitBreakerConfigurationIT` and `UserE2EIT` behaving exactly as today: the
  retry aspect wraps the call but is a no-op pass-through, so their exact
  downstream-call-count and breaker-state-machine assertions stay valid **with no
  edits**. Without this line, the unconfigured instance would default to
  `max-attempts=3` + retry-on-all and silently break those counts.
- `wait-duration=10ms` keeps the new retry tests fast once they opt in (exponential
  backoff is omitted here — fixed 10 ms waits are enough for tests).
- `retry-exceptions` and the aspect orders **mirror prod** so the IT exercises the
  real wiring; only `max-attempts` differs (1 in test base, 3 in prod / the opt-in
  IT) — the same pattern the circuit-breaker config already follows.

---

## 6. Testing strategy

Retry is configured per named instance (`tasks`) and exercised on the gateway
method, so its tests live with the endpoint behavior — **extending
`TasksGatewayAdapterIT`** (the gateway-behavior IT, already
`@SpringBootTest(NONE)` + embedded MockServer + autowired `CircuitBreakerRegistry`).
The breaker keeps its standalone `CircuitBreakerConfigurationIT` (its
properties-driven named instance is endpoint-agnostic machinery); retry has no
analogous shared machinery to test in isolation.

**Modified — `TasksGatewayAdapterIT`.** Opt this class into retry by extending its
existing `@DynamicPropertySource`:

```java
registry.add("resilience4j.retry.instances.tasks.max-attempts", () -> "3");
```

(`wait-duration=10ms`, `retry-exceptions`, and the aspect orders are already on the
test classpath from §5.2.) Add, following `.claude/rules/testing-*.md`:

- `ReturnsTaskIds_TasksServiceRecoversAfterTransientErrors` (success case, placed
  first) — MockServer responds 500, 500, then 200 with a task list → gateway returns
  the mapped ids; downstream hit exactly **3** times (`max-attempts`); breaker stays
  CLOSED.
- `RecordsOneFailurePerCall_ServerErrorsPersist` (placed before the 4xx test) —
  sustained 500 → downstream hit **3** times for a single gateway call, yet the
  breaker records **one** failure (`getNumberOfFailedCalls() == 1`, not `3`). This is
  the breaker-outer / retry-inner guarantee: if retry sat outside the breaker, each
  attempt would inflate the count; if the breaker sat inside, the downstream would be
  hit once.
- Strengthen `PropagatesError_TasksServiceReturnsClientError` — add
  `mockServerClient.verify(request, exactly(1))` to prove 4xx is **not** retried.

The class's other existing tests still hold with retry on: `5xx` / I-O now degrade
*after* retries exhaust (closer to production), and the assertions are result-based
(`isEmpty()`), not count-based.

**Unchanged (regression — must stay green):**

- `CircuitBreakerConfigurationIT` — retry off (`max-attempts=1` on the test
  classpath, no override), so its `mockServerClient.verify(…, exactly(N))`
  breaker-state-machine assertions are untouched. If it turns red on counts, retry
  leaked active onto the test classpath — fix the `max-attempts=1` default, **do not
  loosen the assertions**.
- `TasksGatewayAdapterTests` (unit) — the `@Retry`/`@CircuitBreaker` annotations are
  inert without a Spring proxy, so the bare method still propagates on failure. No
  change.
- `UserE2EIT` — retry off on the test classpath, so degradation behavior (Tasks down
  → empty list → user resolves) is preserved with no count/timing change.

**Verify:** `mvn clean verify`. ITs are slow and need Docker (Testcontainers /
MockServer) — confirm with the developer before running them, per project
convention.

---

## 7. Observability

- **R4j retry emits Micrometer metrics** (`resilience4j.retry.calls`, tagged by
  `kind`: `successful_without_retry`, `successful_with_retry`, `failed_with_retry`,
  `failed_without_retry`), exported through the service's existing Prometheus setup —
  a concrete improvement over the superseded SF7 design, which provided no retry
  metrics. No extra wiring: the `resilience4j-micrometer` binding is already active.
- The breaker's existing Prometheus metrics continue to signal downstream health;
  `emptyTasksFallback` already logs the final degradation.
- A `RetryRegistry` event listener for per-attempt debug logging is **out of scope**
  (the metrics above cover visibility); the seam exists if it's ever wanted.

---

## 8. Documentation

- **`services/asapp-users-service/README.md`** — *Technology Stack*: update the
  `Resilience` bullet to `Resilience4j (circuit breaker + retry)`. *Architecture →
  ### Resilience*: add that the tasks call is retried on transient I/O / 5xx with
  exponential backoff, nested **inside** the breaker (transient blip recovers
  transparently; sustained failure still trips the breaker); 4xx is not retried;
  retry is tuned by `resilience4j.retry.instances.tasks.*`.
- **`TasksGatewayAdapter` Javadoc** — note that transient downstream failures are
  retried with backoff before the breaker records a failure and the fallback
  degrades to an empty list.
- **Root `CLAUDE.md`** (editable) — Stack line lists no resilience tech today; add
  `Resilience4j (circuit breaker + retry)`.
- **`.claude/rules/development-patterns.md`** (gated — text **prepared** for the
  developer to apply): extend "Service-to-Service Calls" — guard each outbound
  gateway with a `@CircuitBreaker`, and where transient faults are expected add a
  Resilience4j `@Retry` **nested inside** the breaker (breaker pinned as the outer
  aspect via aspect order); retry only idempotent calls and only transient faults
  (I/O / 5xx), never 4xx.
- **`libs/asapp-rest-clients/README.md`** — no change; "resilience owned by the
  consuming service" already covers retry.
- **`TODO.md`** — tick line 31 (`[ ]` → `[X]`).

---

## 9. Sequencing

1. **Annotate:** add `@Retry(name = TASKS_CLIENT_NAME)` to `getTaskIdsByUserId`
   (co-located with the breaker); update the adapter Javadoc. (No enabler class.)
2. **Config (prod):** add the retry instance + the two aspect-order lines to
   `src/main/resources/application.properties`.
3. **Config (test):** add the neutralized retry instance (`max-attempts=1`) + the two
   aspect-order lines to `src/test/resources/application.properties`.
4. **Tests:** extend `TasksGatewayAdapterIT` (opt into retry; recovery + breaker-outer
   proof + strengthened 4xx); confirm `CircuitBreakerConfigurationIT` / `UserE2EIT`
   stay green (retry off there, no edits).
5. **Verify:** `mvn clean verify` (confirm before running slow ITs).
6. **Docs:** README (+ Javadoc), `CLAUDE.md`; prepare the `.claude/rules` text.
7. **TODO.md:** tick line 31.

---

## 10. Out of scope

- **Timeouts** (TODO lines 33–34) — connect/read timeouts on the declarative client;
  separate task. Retry + read-timeout interplay is handled there.
- **Library rename** `asapp-rest-clients` → `asapp-http-clients` (TODO line 32).
- **User-with-tasks degradation review** (TODO line 35) — the degrade-to-empty policy
  is unchanged here.
- **Per-attempt retry event logging** — covered by metrics (§7).
- **Other clients** (auth, users) — none exist today.

---

## 11. Acceptance criteria

- `getTaskIdsByUserId` retries transient I/O / 5xx failures with exponential backoff
  (2 retries, 200 ms → 400 ms), nested **inside** the `tasks` circuit breaker, with
  no new Java class.
- The `TasksGatewayAdapterIT` retry tests prove breaker-outer ordering: a single
  sustained-failure gateway call hits the downstream `max-attempts` (3) times yet the
  breaker records exactly **one** failure.
- A transient blip (500, 500, 200) recovers transparently — the gateway returns the
  real task ids without tripping the breaker.
- 4xx is not retried and still propagates (downstream hit exactly once); open-circuit
  still fast-fails to an empty list; `null` body still maps to an empty list.
- Retry config lives in `src/main/resources/application.properties`; the test
  classpath's `max-attempts=1` keeps retry off so existing ITs/E2E stay green with no
  edits; the two aspect-order lines appear in both files.
- `asapp-rest-clients` carries no retry dependency or annotation.
- Docs updated per §8; `.claude/rules` addendum prepared for the developer.
- `mvn clean verify` is green.

---

## 12. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-19-retry-http-clients.md`) were written before implementation, and the retry shipped almost exactly as designed. `TasksGatewayAdapter.getTaskIdsByUserId` now carries `@Retry(name = TASKS_CLIENT_NAME)` co-located with the existing `@CircuitBreaker` — no `fallbackMethod`, **no new Java class** (§2.3/§3.2). The breaker was pinned as the outer aspect by swapping the two order properties to the exact predicted values (`circuit-breaker-aspect-order=2147483642`, `retry-aspect-order=2147483643`) in **both** `application.properties` files (§4/§5); the §4/Step-9 contingency (small-integer orders, or structural two-proxy nesting) was **not needed** — the swap proved breaker-outer on the first pass. Production retry config (`max-attempts=3`, `wait-duration=200ms`, `enable-exponential-backoff`, `×2`, `retry-exceptions` = I/O + 5xx) and the test-classpath neutralizer (`max-attempts=1`) landed verbatim (§5), so 4xx is never retried, a 500/500/200 blip recovers transparently, and an exhausted-retry sequence still counts as exactly one breaker failure (§3.3/§11). The library stayed a pure contract — no retry dependency or annotation (§2.6/§11) — and retry metrics flow through the existing Prometheus binding with no extra wiring (§7). The sections above record the original design intent; **the canonical implementation is the current state of `TasksGatewayAdapter`, the `resilience4j.retry.*` + aspect-order blocks in both `application.properties` files, and `ResilienceConfigurationIT` on this branch**, not this document. Notable deltas:

- **Retry tests were merged into a renamed `ResilienceConfigurationIT`, not added to `TasksGatewayAdapterIT` (reverses §6).** §6 decided retry tests would extend `TasksGatewayAdapterIT` (gateway behavior) while the breaker kept its standalone `CircuitBreakerConfigurationIT`. The implementation followed that first (`0c1c3c3c`), then reversed it (`192493e4`): `CircuitBreakerConfigurationIT` was **renamed to `ResilienceConfigurationIT`** with retry enabled so it covers the breaker, retry, **and their nesting** together; the transient-recovery and breaker-outer ordering proofs moved out of `TasksGatewayAdapterIT` into it (with the breaker tests' downstream-call counts scaled through a `MAX_ATTEMPTS` constant); and `TasksGatewayAdapterIT` was reverted to retry-off, result-based gateway behavior. Rationale: those proofs assert circuit-breaker *internals* (state, failure counts), which belong with the resilience machinery rather than the gateway's business behavior, and enabling retry in that IT exercises the production-like wiring (retry always nested in the breaker). The §6 regression guarantee still holds — `TasksGatewayAdapterIT` and `UserE2EIT` run retry-off.

- **Retry actuator endpoints are asserted, beyond §7's metrics-only observability.** §7 scoped retry observability to the `resilience4j.retry.calls` Prometheus metrics. `test(users)` (`0dda435f`) additionally extended `ActuatorEndpointsIT` to assert the `retries`/`retryevents` actuator endpoints are exposed, mirroring the circuit-breaker actuator coverage added in the prior task.

- **Incidental library/config doc and test-naming cleanups were folded into the branch (beyond §6/§8, which left the library untouched).** Although retry added nothing to `asapp-rest-clients`, the branch also polished neighboring resilience/HTTP-client docs and tests: the `TasksHttpClient` contract test gained its missing-case documentation and a server-error test renamed by its thrown exception (`a2a4104e`, `9cfd753a`); the client-configuration ownership note and the HTTP-client/resilience Javadoc were simplified (`8ca5c578`, `226bd568`, `48915719` — which also moved the retry detail from the class Javadoc to the gateway method); the README retry bullets were trimmed (`d983f79b`); and a resilience IT was refocused to assert config/resilience behavior rather than client return values (`71a7026e`). None change behavior. The §8 docs (users README Stack + Resilience section, root `CLAUDE.md` Stack line now reading `Resilience4j (circuit breaker + retry)`, TODO line 31) landed as planned.

**For future resilience work** (timeouts, TODO lines 33–34, are the next seam — connect/read timeouts on the declarative client, where retry + read-timeout interplay is handled), treat the current `TasksGatewayAdapter` (`@CircuitBreaker` + nested `@Retry`, breaker pinned outer via swapped aspect orders), the consumer-local `resilience4j.retry.instances.tasks.*` block, and the consolidated `ResilienceConfigurationIT` (breaker + retry + nesting, with `TasksGatewayAdapterIT` kept retry-off for gateway behavior) as the template; this spec is preserved as a record of the original design intent — including the per-IT test placement the implementation consolidated.
