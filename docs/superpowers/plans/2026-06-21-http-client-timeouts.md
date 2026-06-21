# HTTP Client Timeouts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the declarative tasks HTTP client a `1s` connect and `2s` read timeout so a slow downstream fails fast and the existing circuit breaker reacts to latency, not just to explicit errors.

**Architecture:** `asapp-users-service` builds the transport for every declarative HTTP service group in `RestClientConfiguration.httpServiceGroupConfigurer`. Today it hand-builds a `java.net.http.HttpClient` (redirects NEVER) with no timeouts. We replace that with `ClientHttpRequestFactoryBuilder.jdk().build(httpClientSettings)`, driven by Boot's property-bound `HttpClientSettings`, and pin the values via `spring.http.client.*`. A read timeout surfaces as `ResourceAccessException` — already retried and already counted by the breaker — so no resilience reclassification is needed.

**Tech Stack:** Java 25, Spring Boot 4.0.5 (`spring-boot-http-client`), Spring declarative HTTP clients (`@HttpExchange` / `@ImportHttpServices`), Resilience4j (circuit breaker + retry), JUnit + MockServer + Testcontainers, AssertJ.

**Spec:** `docs/superpowers/specs/2026-06-21-http-client-timeouts-design.md`

## Global Constraints

- **Values:** `spring.http.client.connect-timeout=1s`, `spring.http.client.read-timeout=2s`, `spring.http.client.redirects=dont-follow` — verbatim, in prod and mirrored into the test classpath.
- **Transport pinned to JDK:** use `ClientHttpRequestFactoryBuilder.jdk()` explicitly — never the auto-configured (detect-based) `ClientHttpRequestFactoryBuilder` bean.
- **Timeouts stay retryable:** make NO change to `resilience4j.*` exception classification; a timeout is a `ResourceAccessException`, already retried and already a breaker failure.
- **`asapp-rest-clients` stays a pure contract** — no timeout config, annotation, or dependency added there.
- **`spring.http.client.redirects=dont-follow` is required on the test classpath** — it replaces the hand-coded `Redirect.NEVER`, so `RestClientConfigurationIT.DoesNotFollowRedirect_*` keeps passing.
- **Integration tests (`*IT`) need Docker (Testcontainers / MockServer) and are slow — confirm with the developer before running them** (project convention).
- **`.claude/` edits are gated** — prepare the rule text for the developer; do NOT apply it.
- **Conventional Commits**; every commit message ends with the trailer `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
- **Run `mvn spotless:apply` before every commit** — the pre-commit hook fails on unformatted code.
- **Test conventions** (`.claude/rules/testing-*.md`): `<Behavior>_<Condition>` names, `// Given/When/Then` blocks, AssertJ (`assertThat`), no `@DisplayName`.

---

### Task 1: Wire connect/read timeouts into the declarative HTTP client

Refactor the request-factory wiring to read Boot's property-bound `HttpClientSettings`, pin the three properties, and prove the slow-downstream → timeout → retry → one breaker failure → empty-list chain with an integration test. Done test-first: the test fails meaningfully before the refactor and passes after.

**Files:**
- Modify (test): `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/ResilienceConfigurationIT.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/RestClientConfiguration.java`
- Modify: `services/asapp-users-service/src/main/resources/application.properties`
- Modify: `services/asapp-users-service/src/test/resources/application.properties`

**Interfaces:**
- Consumes: Boot bean `org.springframework.boot.http.client.HttpClientSettings` (auto-configured by `HttpClientAutoConfiguration`, bound from `spring.http.client.*`); builder `org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder.jdk()`.
- Consumes (existing): `TasksGateway.getTaskIdsByUserId(UserId) : List<UUID>` (degrades to empty list via the breaker fallback on `ResourceAccessException`); autowired `CircuitBreakerRegistry`; constants `MAX_ATTEMPTS = 3`, `TASKS_CLIENT_NAME = "tasks"` in the IT.
- Produces: no new public Java API — a `RestClientHttpServiceGroupConfigurer` bean whose factory now honors `spring.http.client.*`.

- [ ] **Step 1: Add the failing timeout test to `ResilienceConfigurationIT`**

Add the import (with the other `java.util` imports):

```java
import java.util.concurrent.TimeUnit;
```

Add a line to the existing `@DynamicPropertySource` method so the read timeout is short enough to fire fast in the test (every other stub in this class responds instantly, so a 500 ms ceiling does not affect them):

```java
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state", () -> "1s");
        registry.add("resilience4j.retry.instances.tasks.max-attempts", () -> String.valueOf(MAX_ATTEMPTS));
        registry.add("spring.http.client.read-timeout", () -> "500ms");
    }
```

Add this test method immediately after `RetriesInsideCircuitBreaker_ServerErrorsPersist` (last `@Test`, before the private helpers):

```java
    @Test
    void DegradesToEmpty_DownstreamReadTimesOut() {
        // Given
        var userId = aUser().getId();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                              .toString()));

        mockServerClient.when(request)
                        .respond(response().withStatusCode(200)
                                           .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                           .withBody("[]")
                                           .withDelay(TimeUnit.SECONDS, 1));

        // When
        var actual = tasksGateway.getTaskIdsByUserId(userId);

        // Then
        assertThat(actual).isEmpty();
        assertThat(circuitBreaker.getMetrics()
                                 .getNumberOfFailedCalls()).isEqualTo(1);

        mockServerClient.verify(request, exactly(MAX_ATTEMPTS));
    }
```

Add a coverage line to the class Javadoc list (after the existing `<li>` items):

```java
 * <li>Degrades to an empty result and records a single breaker failure when a downstream read times out</li>
```

- [ ] **Step 2: Run the new test — verify it FAILS**

This run needs Docker and is slow — **confirm with the developer before running** (project convention).

Run: `mvn -pl services/asapp-users-service -am clean verify -Dit.test=ResilienceConfigurationIT`
Expected: **FAIL** on `DegradesToEmpty_DownstreamReadTimesOut`. Before the refactor the hand-built `HttpClient` ignores `spring.http.client.read-timeout`, so the 500 ms override has no effect: the 1 s-delayed `200 []` eventually returns, the gateway maps `[]` to an empty list (a *success*), the downstream is hit once, and the breaker records zero failures. So `getNumberOfFailedCalls()` is `0` (expected `1`) and `verify(..., exactly(3))` sees `1` — both assertions fail. (`assertThat(actual).isEmpty()` passes incidentally because `[]` is empty.)

- [ ] **Step 3: Refactor `RestClientConfiguration` to build the factory from `HttpClientSettings`**

In the imports: **remove** `import java.net.http.HttpClient;` and `import org.springframework.http.client.JdkClientHttpRequestFactory;`. **Add** (among the `org.springframework.boot.*` imports):

```java
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
```

Replace the entire `httpServiceGroupConfigurer` method (Javadoc + body) with:

```java
    /**
     * Configures every declarative HTTP service client group's {@link RestClient}.
     * <p>
     * Applies to each client, in order:
     * <ol>
     * <li>A JDK request factory built from the property-bound {@link HttpClientSettings} (connect/read timeouts and redirect handling come from {@code
     * spring.http.client.*}).</li>
     * <li>The {@link JwtInterceptor}, which propagates the caller's bearer token to the downstream call.</li>
     * <li>The {@link LoadBalancerInterceptor}, when one is available.</li>
     * </ol>
     * <p>
     * The interceptor is injected as an {@link ObjectProvider} and applied only when present: Spring Cloud LoadBalancer auto-configures the bean when it is on
     * the classpath, and the interceptor then resolves a base url that targets a Eureka service id into a concrete instance host and port. Without the bean
     * (e.g. in tests), the configured base-url host is called directly.
     *
     * @param loadBalancerInterceptor provider for the optional Spring Cloud load-balancer interceptor that resolves Eureka service ids to instances
     * @param httpClientSettings the property-bound HTTP client settings ({@code spring.http.client.*}) carrying connect/read timeouts and redirect handling
     * @return the group configurer for RestClient-backed HTTP services
     */
    @Bean
    RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor,
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

Leave the `restClientBuilder` and `defaultEurekaClientHttpRequestFactorySupplier` beans, and the `java.util.Set` import, unchanged.

- [ ] **Step 4: Add the three properties to production `application.properties`**

In `services/asapp-users-service/src/main/resources/application.properties`, replace the `# Http Service Client properties` block (currently one line) with (alphabetical order within the section):

```properties
# Http Service Client properties
spring.http.client.connect-timeout=1s
spring.http.client.read-timeout=2s
spring.http.client.redirects=dont-follow
spring.http.serviceclient.tasks.base-url=http://asapp-tasks-service/asapp-tasks-service
```

- [ ] **Step 5: Mirror the three properties into the test `application.properties`**

In `services/asapp-users-service/src/test/resources/application.properties`, replace the `# Http Service Client properties` block with:

```properties
# Http Service Client properties
spring.http.client.connect-timeout=1s
spring.http.client.read-timeout=2s
spring.http.client.redirects=dont-follow
spring.http.serviceclient.tasks.base-url=http://localhost:8081/asapp-tasks-service
```

(`redirects=dont-follow` here keeps `RestClientConfigurationIT.DoesNotFollowRedirect_*` green now that redirect-never is no longer hand-coded. `read-timeout=2s` is a harmless ceiling for instant-response stubs; the timeout test shrinks it to 500 ms via `@DynamicPropertySource` from Step 1.)

- [ ] **Step 6: Format and compile**

Run: `mvn -q -pl services/asapp-users-service spotless:apply`
Run: `mvn -q -pl services/asapp-users-service -am clean test-compile`
Expected: BUILD SUCCESS (no compile errors; the dropped imports are gone, the new ones resolve).

- [ ] **Step 7: Run the new test — verify it PASSES**

Needs Docker, slow — **confirm with the developer before running**.

Run: `mvn -pl services/asapp-users-service -am clean verify -Dit.test=ResilienceConfigurationIT`
Expected: **PASS**. The 1 s-delayed response now exceeds the 500 ms read timeout on each attempt → `ResourceAccessException` → retried `MAX_ATTEMPTS` (3) times → all time out → breaker records exactly one failure → fallback returns an empty list. `getNumberOfFailedCalls() == 1`, downstream hit 3 times, result empty.

- [ ] **Step 8: Run the users-service suite — verify no regressions**

Needs Docker, slow — **confirm with the developer before running**.

Run: `mvn -pl services/asapp-users-service -am clean verify`
Expected: **PASS**, in particular `RestClientConfigurationIT.DoesNotFollowRedirect_ServerReturnsRedirect` (redirect-never now comes from `spring.http.client.redirects=dont-follow`), the other `ResilienceConfigurationIT` tests, and `UserE2EIT`. If the redirect test fails, the `redirects=dont-follow` line is missing from the test `application.properties` (Step 5).

- [ ] **Step 9: Commit**

```bash
mvn -q -pl services/asapp-users-service spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/RestClientConfiguration.java \
        services/asapp-users-service/src/main/resources/application.properties \
        services/asapp-users-service/src/test/resources/application.properties \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/ResilienceConfigurationIT.java
git commit -m "feat(http-clients): add connect/read timeouts to the declarative tasks client

- Build the request factory with ClientHttpRequestFactoryBuilder.jdk()
  driven by the injected HttpClientSettings; drop the hand-built HttpClient
- Set spring.http.client.connect-timeout=1s / read-timeout=2s /
  redirects=dont-follow (prod + mirrored test); redirect-never moves from
  Java into the property
- A slow downstream now times out as a ResourceAccessException, is retried,
  and counts as one breaker failure, so the breaker reacts to latency
- Prove the chain in ResilienceConfigurationIT.DegradesToEmpty_DownstreamReadTimesOut

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Documentation

Document the timeouts in the users-service README, tick the TODO item, and prepare (but do not apply) the `.claude` rule addendum.

**Files:**
- Modify: `services/asapp-users-service/README.md`
- Modify: `TODO.md`
- Prepared text only (gated, NOT edited): `.claude/rules/development-patterns.md`

**Interfaces:**
- Consumes: nothing (prose only).
- Produces: nothing consumed by later tasks (final task).

- [ ] **Step 1: Add the timeouts paragraph to the README resilience section**

In `services/asapp-users-service/README.md`, after the aspect-order line that ends the Resilience section (the line beginning *"The breaker is pinned as the outer aspect…"*, currently line 212) and before `### Project Structure`, insert:

```markdown

Outbound calls also carry transport **timeouts** (`1s` connect, `2s` read), set via `spring.http.client.*`. A downstream that is *slow* rather than failing now times out as a transient I/O failure — which is retried and, on exhaustion, counts as a single breaker failure — so the breaker reacts to latency, not just to explicit errors. Because retry wraps the call, a fully unresponsive downstream is bounded at roughly `3 × read-timeout + backoff` before degrading to an empty result, after which the open breaker fast-fails.

| Property                             | Purpose                                       |
|--------------------------------------|-----------------------------------------------|
| `spring.http.client.connect-timeout` | How long to wait to establish the connection  |
| `spring.http.client.read-timeout`    | How long to wait for the response             |
| `spring.http.client.redirects`       | Whether the client follows HTTP redirects     |
```

(Leave the *Technology Stack* line `**Resilience**: Resilience4j (circuit breaker + retry)` unchanged — timeouts are an HTTP-transport concern, not a new library.)

- [ ] **Step 2: Tick the TODO item**

In `TODO.md`, change line 32 from:

```markdown
        * [ ] Add timeouts to HTTP client
```

to:

```markdown
        * [X] Add timeouts to HTTP client
```

(Leave the indented sub-bullet on line 33 unchanged.)

- [ ] **Step 3: Prepare the `.claude/rules` addendum (do NOT apply)**

Edits under `.claude/` hit the auto-mode permission gate, so do **not** edit the file. Instead, surface this text to the developer to paste into `.claude/rules/development-patterns.md` under **## Service-to-Service Calls** as a new bullet:

```markdown
- Outbound clients set connect/read timeouts via `spring.http.client.*` so a slow downstream fails fast and the circuit breaker reacts to latency, not just to explicit errors
```

- [ ] **Step 4: Commit**

```bash
git add services/asapp-users-service/README.md TODO.md
git commit -m "docs(http-clients): document HTTP client timeouts and tick TODO

- Describe the 1s connect / 2s read timeout in the users-service README
  resilience section, with a tuning-property table
- Tick the 'Add timeouts to HTTP client' TODO item

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**1. Spec coverage**
- §3.1 factory refactor → Task 1 Step 3 ✓
- §3.1 `RestClientConfiguration` Javadoc → Task 1 Step 3 ✓
- §4.1 prod properties → Task 1 Step 4 ✓
- §4.2 test properties (incl. `redirects=dont-follow`) → Task 1 Step 5 ✓
- §5 timeout IT + class-level read-timeout override + IT Javadoc → Task 1 Steps 1, 7 ✓
- §6 observability (no new wiring) → nothing to do; covered by existing metrics ✓
- §7 README → Task 2 Step 1; CLAUDE.md unchanged (noted); `.claude/rules` prepared → Task 2 Step 3; TODO tick → Task 2 Step 2 ✓
- §10 acceptance criteria → all map to Task 1 (wiring, properties, IT, regressions) and Task 2 (docs) ✓
- §9 out-of-scope items (library rename, degradation review, non-retryable timeouts, per-client granularity, hot refresh) → correctly absent ✓

**2. Placeholder scan** — no TBD/TODO/"handle errors"/"similar to"; every code and prose block is complete. ✓

**3. Type consistency** — `HttpClientSettings` / `ClientHttpRequestFactoryBuilder.jdk()` (Task 1 Step 3) match the Consumes block and the spec's verified API; `getTaskIdsByUserId(...) : List<UUID>`, `MAX_ATTEMPTS`, `circuitBreaker.getMetrics().getNumberOfFailedCalls()`, and `mockServerClient.verify(..., exactly(MAX_ATTEMPTS))` match the existing `ResilienceConfigurationIT` members used in Step 1. ✓
