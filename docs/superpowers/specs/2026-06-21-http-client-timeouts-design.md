# Timeouts on the Tasks HTTP Client — Design

**Date:** 2026-06-21
**Status:** Draft
**Targets:** `asapp-users-service` (`RestClientConfiguration`, `application.properties` ×2, `ResilienceConfigurationIT`, docs)

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Add timeouts to HTTP client — Tune the declarative HTTP clients: set
> connect/read timeouts (so a slow downstream fails fast and the breaker reacts
> to latency, not just failure rate).

It is the next item after the circuit breaker (commit `50ecb496`) and retry
(commit `1fcbbfbd`). The retry design
(`docs/superpowers/specs/2026-06-19-retry-http-clients-design.md` §10) explicitly
reserved this seam: *"Timeouts — connect/read timeouts on the declarative client;
separate task. Retry + read-timeout interplay is handled there."*

Today's relevant state:

- One declarative client exists: `TasksHttpClient` (`@HttpExchange`) in
  `asapp-rest-clients` (a pure contract — no wiring). No other HTTP clients.
- `asapp-users-service`'s `RestClientConfiguration.httpServiceGroupConfigurer`
  builds the transport for **every** HTTP service group: a hand-built
  `java.net.http.HttpClient` (redirects `NEVER`) wrapped in a
  `JdkClientHttpRequestFactory`, plus the `JwtInterceptor` and the optional
  `LoadBalancerInterceptor`.
- The tasks call is guarded by `@CircuitBreaker` + nested `@Retry` on
  `TasksGatewayAdapter.getTaskIdsByUserId`, tuned via `resilience4j.*` in both
  `application.properties` files.
- **No timeouts are set anywhere.** Spring Boot's `HttpClientSettings.defaults` is
  `new HttpClientSettings(null, null, null, null)` — every value null. For the JDK
  client that means **no read timeout** (the request waits forever for a response)
  and **no connect timeout** (the OS TCP default applies — tens of seconds). This
  is the gap: a slow or hung tasks-service stalls the user-facing
  `GET /users/{id}` indefinitely, and the breaker — which only sees *failures* —
  never reacts, because an infinite wait is never recorded as a failure.

The goal: a **slow** downstream is converted into a bounded, fail-fast error that
the existing retry/breaker machinery already understands, so the breaker reacts to
**latency**, not just to explicit errors.

---

## 2. Decisions (from brainstorming)

1. **Mechanism: Boot standard `spring.http.client.*` properties** (confirmed). Refactor
   `httpServiceGroupConfigurer` to build the request factory with
   `ClientHttpRequestFactoryBuilder.jdk()` driven by the injected, property-bound
   `HttpClientSettings`, instead of the hand-built `HttpClient`. **`.jdk()` is pinned
   explicitly** rather than injecting the auto-configured (detect-based)
   `ClientHttpRequestFactoryBuilder` bean: `detect()` would resolve the transport from
   the classpath and could silently switch away from JDK (e.g. to Reactor/Apache if
   ever pulled in transitively by Spring Cloud) and would *add* a virtual-thread
   executor the hand-built client never had — `.jdk()` faithfully preserves today's
   JDK transport. Chosen over custom
   `asapp.*` properties (more code, per-client granularity we don't need with one
   client) and over Java literals (not tunable, and — decisively — not overridable
   in tests, so a timeout IT couldn't shrink the read timeout to run fast). Idiomatic
   Boot 4, consistent with the externalized resilience config, and the redirect-never
   safety knob folds from Java into a property.
2. **Timeouts stay retryable** (confirmed). A read/connect timeout surfaces as
   `org.springframework.web.client.ResourceAccessException`, which is *already* in
   the retry whitelist and the breaker's record set. We make **no classification
   change**: a timeout is retried (2×), and on exhaustion the breaker records one
   failure and the fallback degrades to an empty list — exactly as a dropped
   connection does today. This preserves the retry design's invariant ("the retry
   set == the breaker-record set, so classification is reasoned about once",
   retry spec §3.3) and needs **zero new Java beyond the factory refactor**. The
   rejected alternative — *not* retrying timeouts for true per-call fail-fast —
   would require a custom Resilience4j `retryExceptionPredicate` bean to separate
   read-timeout from connect-fault (both are `ResourceAccessException`), trading the
   clean property-only config for code we don't need.
3. **Values: connect `1s`, read `2s`** (confirmed). A timeout is a *failure
   threshold*, not the expected latency — it must sit well above the worst
   *acceptable* case, or routine hiccups (a GC pause, a single lost packet, brief
   load) false-fail a healthy downstream and trigger needless retries.
   - **connect `1s`** — the floor. A TCP handshake on the cluster is sub-millisecond,
     but if a single SYN packet is dropped the OS retransmits at ~1s (RFC 6298
     initial RTO). A connect timeout below ~1s false-fails a healthy host on one lost
     packet, and buys nothing for a *down* host (connection refused returns
     instantly).
   - **read `2s`** — generous headroom (~20–40× the expected tens-of-ms response,
     ~4–10× a slow-but-healthy response) so a GC pause or load spike on tasks-service
     does not spuriously time out and provoke a retry storm against a downstream that
     was merely slow, not broken.
4. **Scope: `asapp-users-service` only** — the sole HTTP client. Because
   `spring.http.client.*` is global, the `@Primary` plain `RestClient.Builder` (via
   `RestClientBuilderConfigurer`) inherits the same timeouts — desirable, any
   unqualified RestClient now fails fast too. Eureka is unaffected: it keeps its own
   isolated `DefaultEurekaClientHttpRequestFactorySupplier`.
5. **Library stays a pure contract** — no timeout config or annotation leaks into
   `asapp-rest-clients`. No new dependency anywhere (`spring-boot-http-client` is
   already transitively present).

### Research backing (verified 2026-06-21 against the resolved jars: `spring-boot-http-client-4.0.5`)

- **`HttpClientSettings`** (`org.springframework.boot.http.client`) is a record
  `(@Nullable HttpRedirects redirects, @Nullable Duration connectTimeout,
  @Nullable Duration readTimeout, @Nullable SslBundle sslBundle)` with withers
  (`withConnectTimeout`, `withReadTimeout`, `withRedirects`, …) and
  `defaults() == new HttpClientSettings(null, null, null, null)`.
- **It is an injectable bean.** `HttpClientAutoConfiguration`
  (`org.springframework.boot.http.client.autoconfigure`) declares
  `@Bean HttpClientSettings httpClientSettings(...)`, bound from `HttpClientsProperties`
  (prefix `spring.http.client`: `connect-timeout`, `read-timeout`, `redirects`).
- **`ClientHttpRequestFactoryBuilder.jdk()`** returns a `JdkClientHttpRequestFactoryBuilder`
  directly — no classpath detection, so the JDK transport is guaranteed regardless of
  what else is on the classpath. (The auto-configured `ClientHttpRequestFactoryBuilder<?>`
  bean exists too, but it resolves via `detect()` and is *not* used here — see §2.1.)
- **`ClientHttpRequestFactoryBuilder.build(HttpClientSettings)`** →
  `JdkClientHttpRequestFactoryBuilder.createClientHttpRequestFactory` builds the
  `HttpClient` from the settings (connect timeout + redirects on the client) and
  maps `settings.readTimeout()` onto `JdkClientHttpRequestFactory.setReadTimeout`
  (i.e. the JDK `HttpRequest.timeout()`). So **connect timeout + redirects live on
  the `HttpClient`; the read timeout lives on the request factory.**
- **`HttpRedirects`** enum: `FOLLOW_WHEN_POSSIBLE`, `FOLLOW`, `DONT_FOLLOW`.
  `DONT_FOLLOW` reproduces today's `HttpClient.Redirect.NEVER`.
- **Failure mapping** (Spring `RestClient`): a JDK read timeout throws
  `java.net.http.HttpTimeoutException`; a connect timeout throws
  `HttpConnectTimeoutException`; a refused connection throws
  `java.net.ConnectException` — all are `IOException`s that Spring wraps into
  `ResourceAccessException`. They are indistinguishable at the Spring layer without
  inspecting the cause, which is why decision §2.2 keeps them uniformly retryable.

---

## 3. Detailed Design

### 3.1 The factory refactor — `RestClientConfiguration.httpServiceGroupConfigurer`

Replace the hand-built `HttpClient` with Boot's builder + the injected, property-bound
settings. The interceptor wiring (JWT, optional load balancer) is unchanged.

```java
@Bean
RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(
        ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor,
        HttpClientSettings httpClientSettings) {

    var requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                                                        .build(httpClientSettings);

    return groupConfigurer -> groupConfigurer.forEachClient((_, clientBuilder) -> {
        clientBuilder.requestFactory(requestFactory)
                     .requestInterceptor(new JwtInterceptor());
        loadBalancerInterceptor.ifAvailable(clientBuilder::requestInterceptor);
    });
}
```

- **Imports:** drop `java.net.http.HttpClient` and
  `org.springframework.http.client.JdkClientHttpRequestFactory`; add
  `org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder` and
  `org.springframework.boot.http.client.HttpClientSettings`.
- **Redirect-never** is no longer coded in Java — it comes from
  `spring.http.client.redirects=dont-follow` (§4). The injected `HttpClientSettings`
  carries it, the builder applies it, and `RestClientConfigurationIT`'s
  `DoesNotFollowRedirect_ServerReturnsRedirect` test still passes.
- **One shared factory:** the bean builds a single `requestFactory` (one underlying
  `HttpClient`) shared across every client — same connection-pooling shape as today.
- **Transport stays JDK:** `ClientHttpRequestFactoryBuilder.jdk()` is pinned, so the
  JDK `HttpClient` is used unconditionally — no dependence on classpath detection
  (§2.1).
- **Javadoc:** update the `forEachClient` description — the factory is now built from
  Boot's `ClientHttpRequestFactoryBuilder` and the property-bound `HttpClientSettings`
  (connect/read timeouts and redirect handling come from `spring.http.client.*`),
  no longer a hand-built redirect-disabled JDK client.

### 3.2 Behavior — how a slow downstream now flows

| Downstream condition | Per attempt | With retry (2 retries, 200→400 ms backoff) | Breaker | Net |
|---|---|---|---|---|
| Healthy, fast (tens of ms) | returns well under the limits | n/a | success | tasks returned, unchanged |
| Slow, > read timeout (hung / black-hole) | read times out at 2s → `ResourceAccessException` | retried 2× → all time out | records **1** failure | degrade to empty list; **worst case ≈ 3×2s + 0.6s = 6.6s** |
| Down (connection refused) | fails instantly → `ResourceAccessException` | retried 2× (each instant) | records **1** failure | degrade to empty list; ≈ 0.6s (backoff only) |
| Unreachable (SYN black-hole) | connect times out at 1s → `ResourceAccessException` | retried 2× | records **1** failure | degrade to empty list; ≈ 3×1s + 0.6s |

The 6.6s worst case is a **first-wave, transient** cost only. Once sustained timeouts
push the failure rate past the threshold (`minimum-number-of-calls=5`,
`failure-rate-threshold=50%`, window `10`), the breaker **opens** and subsequent
calls short-circuit (`CallNotPermittedException` → fallback → empty list) in sub-ms
for `wait-duration-in-open-state=10s`, then half-open to probe. This is precisely
"the breaker reacts to latency" — the read timeout is the mechanism that turns
latency into the failure signal the breaker counts.

The healthy path is unaffected: a normal response (~tens of ms) finishes far inside
both limits.

---

## 4. Configuration & tunability

Per `.claude/rules/configuration.md` (record decisions and safety knobs, not
defaults): the three properties pin values that deviate from Boot's "no timeout"
default plus the redirect-never safety knob (previously hand-coded in Java). Mirrored
into both classpaths so test wiring matches prod, following the resilience config's
pattern.

### 4.1 Production — `src/main/resources/application.properties`

Append to the existing `# Http Service Client properties` block:

```properties
spring.http.client.connect-timeout=1s
spring.http.client.read-timeout=2s
spring.http.client.redirects=dont-follow
```

### 4.2 Test — `src/test/resources/application.properties`

Mirror the same three lines:

```properties
spring.http.client.connect-timeout=1s
spring.http.client.read-timeout=2s
spring.http.client.redirects=dont-follow
```

- `redirects=dont-follow` is **required** here: it replaces the hand-built
  `Redirect.NEVER`, so `RestClientConfigurationIT.DoesNotFollowRedirect_*` keeps
  passing.
- `read-timeout=2s` is a comfortable ceiling for the other ITs — every existing stub
  responds instantly on localhost MockServer, far under 2s, so no test slows down or
  flakes. The timeout IT (§5) shrinks the read timeout locally via
  `@DynamicPropertySource` to run fast.

### 4.3 Operational tunability — how to change the values if something goes wrong

- **Baseline** ships in the service jar (`src/main/resources/application.properties`,
  §4.1) — a sensible default, same home as the resilience4j config.
- **Startup override, no rebuild.** Spring property precedence lets either layer
  override the jar baseline:
  - **Config-service** — add e.g. `spring.http.client.read-timeout` to
    `central-config/asapp-users-service.properties`; it is imported via
    `spring.config.import=configserver:…` and takes precedence over the jar.
  - **Docker** — set `SPRING_HTTP_CLIENT_READ_TIMEOUT` /
    `SPRING_HTTP_CLIENT_CONNECT_TIMEOUT` in the service's compose `environment:`
    block (relaxed binding, highest precedence).

  Either path requires a **service restart** to take effect.
- **Hot refresh (`/actuator/refresh`) does NOT apply.** The request factory and the
  declarative `@ImportHttpServices` client proxies are built once at context startup
  and are not `@RefreshScope`-aware, so re-binding the `Environment` does not re-wire
  the live clients. Making timeouts hot-swappable would require refresh-scoping
  framework-created proxies — non-trivial and unnecessary here: timeouts are
  infrastructure knobs, the circuit breaker is the real-time safety net, and this
  matches the neighboring resilience4j config (also startup-bound). Out of scope (§9).

---

## 5. Testing strategy

The timeout's meaningful behavior for this task is its **interplay with retry and the
breaker** (latency → `ResourceAccessException` → retried → one breaker failure →
degrade), so the test belongs with the consolidated `ResilienceConfigurationIT`
(`@SpringBootTest(NONE)` + embedded MockServer + autowired `TasksGateway` and
`CircuitBreakerRegistry`), alongside the existing retry/breaker tests.

**Modified — `ResilienceConfigurationIT`.** Shrink the read timeout for this class so
the timeout test is fast and deterministic, by extending the existing
`@DynamicPropertySource`:

```java
registry.add("spring.http.client.read-timeout", () -> "500ms");
```

500 ms is still a comfortable ceiling for the class's other stubs (instant localhost
responses), so the breaker/retry tests are unaffected. Add (following
`.claude/rules/testing-*.md`, success-case ordering, `<Behavior>_<Condition>`
naming):

- `DegradesToEmpty_DownstreamReadTimesOut` — stub MockServer with a response delayed
  beyond the read timeout (e.g. `withDelay(TimeUnit.SECONDS, 1)`, > 500 ms); call
  `tasksGateway.getTaskIdsByUserId(userId)`. Assert the result is an **empty list**
  (degraded via the breaker fallback), the downstream was hit **`MAX_ATTEMPTS` (3)**
  times (the timeout was retried), and the breaker recorded exactly **one** failure
  (`getNumberOfFailedCalls() == 1`). This proves the full chain: read timeout →
  `ResourceAccessException` → retried inside the breaker → one recorded failure →
  fallback. Its shape mirrors the existing
  `RetriesInsideCircuitBreaker_ServerErrorsPersist`, swapping a `500` response for a
  delayed one.

Update the class Javadoc coverage list with a line for the read-timeout case.

**Unchanged (regression — must stay green):**

- `ResilienceConfigurationIT`'s existing tests — the 500 ms read ceiling does not
  affect instant-response stubs; their downstream-call counts and breaker-state
  assertions hold.
- `RestClientConfigurationIT` — `DoesNotFollowRedirect_*` stays green because
  `spring.http.client.redirects=dont-follow` is on the test classpath (§4.2); the
  load-balancer and JWT-propagation tests are unaffected by timeouts.
- `TasksGatewayAdapterIT`, `UserE2EIT`, `TasksGatewayAdapterTests` — instant responses,
  no timing change.

**Verify:** `mvn clean verify`. ITs are slow and need Docker (Testcontainers /
MockServer) — confirm with the developer before running them, per project convention.

---

## 6. Observability

No new wiring. The read timeout produces a `ResourceAccessException` that flows through
the **existing** retry and circuit-breaker Micrometer metrics
(`resilience4j.retry.calls`, the breaker's call/failure metrics) exported via the
service's Prometheus setup — so a latency-induced degrade is already visible as
retried + failed calls and, if sustained, a breaker state transition. The
`emptyTasksFallback` log already records the final degradation.

---

## 7. Documentation

- **`services/asapp-users-service/README.md`** — *Architecture → Resilience*: add that
  the tasks call has a `1s` connect / `2s` read timeout, so a slow downstream times out
  as a transient I/O failure that is retried and then counts as one breaker failure —
  the breaker reacts to latency, not only to explicit errors. Note the values are
  tunable via `spring.http.client.*`.
- **`RestClientConfiguration` Javadoc** — per §3.1 (factory now built from Boot's
  builder + property-bound settings; timeouts and redirects from `spring.http.client.*`).
- **Root `CLAUDE.md`** — no change. The Stack line already reads "Resilience4j (circuit
  breaker + retry)"; timeouts are an HTTP-client transport concern, not a new library.
- **`.claude/rules/development-patterns.md`** (gated — text **prepared** for the
  developer to apply, since `.claude/` edits hit the auto-mode permission gate): extend
  "Service-to-Service Calls" with a line that outbound clients set connect/read timeouts
  via `spring.http.client.*` so a slow downstream fails fast and the breaker reacts to
  latency.
- **`libs/asapp-rest-clients/README.md`** — no change; "resilience owned by the
  consuming service" already covers timeouts.
- **`TODO.md`** — tick line 32 (`[ ]` → `[X]`).

---

## 8. Sequencing

1. **Refactor:** rewrite `httpServiceGroupConfigurer` to inject `HttpClientSettings` and
   build the factory with `ClientHttpRequestFactoryBuilder.jdk().build(httpClientSettings)`;
   drop the hand-built `HttpClient`; fix imports; update the class Javadoc.
2. **Config (prod):** add the three `spring.http.client.*` lines to
   `src/main/resources/application.properties`.
3. **Config (test):** mirror the three lines into `src/test/resources/application.properties`.
4. **Tests:** add `DegradesToEmpty_DownstreamReadTimesOut` to `ResilienceConfigurationIT`
   (with the class-level `read-timeout=500ms` override); update its Javadoc; confirm the
   redirect and other ITs stay green.
5. **Verify:** `mvn clean verify` (confirm before running slow ITs).
6. **Docs:** users-service README + `RestClientConfiguration` Javadoc; prepare the
   `.claude/rules` text.
7. **TODO.md:** tick line 32.

---

## 9. Out of scope

- **Library rename** `asapp-rest-clients` → `asapp-http-clients` (TODO line 37).
- **User-with-tasks degradation review** (TODO lines 34–36) — the degrade-to-empty
  policy is unchanged here.
- **Non-retryable timeouts** — the per-call fail-fast variant (custom retry predicate)
  is rejected in §2.2.
- **Per-client timeout granularity** — `spring.http.client.*` is global; fine with one
  client. The seam to go per-client (custom properties) stays open if a second client
  ever appears.
- **Hot (no-restart) timeout reconfiguration** via `/actuator/refresh` — the live
  clients are built once at startup and not refresh-scoped (§4.3). Startup override via
  config-server or docker env (§4.3) is the supported and sufficient adjustment path.
- **Other clients** (auth, users) — none exist today.

---

## 10. Acceptance criteria

- `httpServiceGroupConfigurer` builds the request factory with
  `ClientHttpRequestFactoryBuilder.jdk().build(httpClientSettings)` (injected,
  property-bound `HttpClientSettings`); the hand-built `java.net.http.HttpClient` is gone.
- Production `application.properties` sets `spring.http.client.connect-timeout=1s`,
  `read-timeout=2s`, `redirects=dont-follow`; the test classpath mirrors all three.
- A downstream response slower than the read timeout is converted to a
  `ResourceAccessException`, retried (2×), recorded as exactly **one** breaker failure,
  and degraded to an empty list — proven by `DegradesToEmpty_DownstreamReadTimesOut`.
- `RestClientConfigurationIT.DoesNotFollowRedirect_*` and all existing
  retry/breaker/E2E tests stay green with no assertion changes.
- The `@Primary` plain `RestClient.Builder` inherits the timeouts; Eureka stays
  isolated.
- `asapp-rest-clients` carries no timeout config or dependency.
- Docs updated per §7; `.claude/rules` addendum prepared for the developer; TODO line 32
  ticked.
- `mvn clean verify` is green.
