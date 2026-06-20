# Retry on the Tasks HTTP Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retry transient I/O / 5xx failures of the `asapp-users-service` → tasks-service call with exponential backoff, nested **inside** the existing Resilience4j circuit breaker, so a momentary blip recovers transparently while sustained failure still trips the breaker.

**Architecture:** Add a Resilience4j `@Retry(name = "tasks")` co-located with the existing `@CircuitBreaker` on `TasksGatewayAdapter.getTaskIdsByUserId`. Both are Resilience4j aspects that compose; the breaker is pinned as the **outer** aspect by swapping the two aspect-order properties (verified defaults below), so an exhausted-retry sequence counts as exactly **one** breaker failure and an open breaker short-circuits before any retry. Retry auto-configures from the `resilience4j-spring-boot4` starter — **no enabler class, no new Java files**. R4j retry has no global off-switch, so the test classpath neutralizes it (`max-attempts=1`) to keep existing ITs/E2E green; the retry IT opts in.

**Tech Stack:** Spring Boot 4.0.5, Java 25, Resilience4j 2.4.0 (`resilience4j-spring-boot4` / `resilience4j-spring6` / `resilience4j-framework-common`), JUnit 5 + AssertJ + MockServer + Testcontainers.

**Spec:** `docs/superpowers/specs/2026-06-19-retry-http-clients-design.md`

## Global Constraints

These apply to every task. Exact values are confirmed against the resolved jars on this project's classpath (`resilience4j-*-2.4.0`).

- **R4j `@Retry` confirmed API** (resolves the disproven SF7 premise — verified from the jars):
  - Annotation: `io.github.resilience4j.retry.annotation.Retry`. Attribute used: `name` (the instance name → `resilience4j.retry.instances.<name>.*`). **No `fallbackMethod`** — retry rethrows after exhaustion so the outer breaker records the failure and runs its own fallback.
  - Auto-configured by `resilience4j-spring-boot4` (`RetryAutoConfiguration`); active as soon as a method carries `@Retry`. **No `@EnableResilientMethods` equivalent — create no new class.** An unconfigured instance defaults to `maxAttempts=3` + retry-on-all, which is why the test classpath sets `max-attempts=1`.
  - Instance properties (kebab-case): `max-attempts` (Integer, **total** attempts incl. the first), `wait-duration` (Duration), `enable-exponential-backoff` (Boolean), `exponential-backoff-multiplier` (Double), `retry-exceptions` (`Class[]`, whitelist).
- **Aspect ordering — breaker must be the OUTER aspect; both aspects implement Spring `Ordered` where lower value = higher precedence = outer:**
  - Verified defaults: CircuitBreaker aspect order = `Ordered.LOWEST_PRECEDENCE - 4` = `2147483643`; Retry aspect order = `Ordered.LOWEST_PRECEDENCE - 5` = `2147483642`. Default ⇒ Retry outer, breaker inner (**wrong**).
  - Fix: **swap the two values** so the breaker takes the lower one — `resilience4j.circuitbreaker.circuit-breaker-aspect-order=2147483642` and `resilience4j.retry.retry-aspect-order=2147483643`. Same two lines in **both** `application.properties` files.
  - **Proven, not assumed**: the new `TasksGatewayAdapterIT` tests assert a sustained-failure call hits the downstream `max-attempts` (3) times yet the breaker records exactly **one** failure, and that a 500/500/200 sequence returns the real ids. If those fail, the order regressed — see Task 1, Step 9 (contingency) before loosening any assertion.
- **`asapp-rest-clients` stays a pure contract** — no retry dependency or annotation. R4j is already on the consuming service's classpath via the circuit-breaker starter; no new dependency anywhere.
- **License header:** any new `.java` would start with the project's Apache-2.0 header (lines 1–15 of any existing source). *This plan creates no new `.java` files.*
- **Javadoc:** keep `@since 0.2.0` on `TasksGatewayAdapter` (the class is unchanged in age; a method annotation does not bump it). `@see` only for framework classes. No `@since` in test classes.
- **Formatting & hooks:** run `mvn spotless:apply` before every commit (pre-commit hook validates formatting and import ordering — add imports anywhere sensible and let Spotless sort).
- **Integration tests are developer-gated:** all `*IT` runs need Docker (Testcontainers Postgres + embedded MockServer). Per project convention, **confirm with the developer before running any IT / `mvn verify`**. Fast commands (`compile`, `spotless:apply`) run freely.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java` | Modify | Add `@Retry(name = TASKS_CLIENT_NAME)` co-located with `@CircuitBreaker` on `getTaskIdsByUserId`; update class Javadoc. |
| `services/asapp-users-service/src/main/resources/application.properties` | Modify | Add the `tasks` retry instance (`max-attempts=3`, backoff, `retry-exceptions`) + the two aspect-order lines. |
| `services/asapp-users-service/src/test/resources/application.properties` | Modify | Add the neutralized `tasks` retry instance (`max-attempts=1`, `wait-duration=10ms`, `retry-exceptions`) + the two aspect-order lines. |
| `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterIT.java` | Modify | Opt into retry via `@DynamicPropertySource` (`max-attempts=3`); add recovery + breaker-outer proof tests; strengthen the 4xx test. |
| `services/asapp-users-service/README.md` | Modify | Technology Stack bullet + Architecture → Resilience section + a docs link. |
| `CLAUDE.md` | Modify | Add Resilience4j (circuit breaker + retry) to the Stack line. |
| `TODO.md` | Modify | Tick line 31 (`Use retry pattern`). |
| `.claude/rules/development-patterns.md` | **Prepared text only — NOT applied** | Extend "Service-to-Service Calls". Writes under `.claude/` are permission-gated; deliver the text for the developer to apply. |

---

### Task 1: Add R4j `@Retry` and prove breaker-outer / retry-inner ordering (TDD)

This is the core task: the `@Retry` annotation, both property files, and the integration tests are one inseparable, independently-testable deliverable ("retry works and is correctly nested in the breaker"). The IT is the single hard guarantee — write it first.

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java`
- Modify: `services/asapp-users-service/src/main/resources/application.properties`
- Modify: `services/asapp-users-service/src/test/resources/application.properties`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterIT.java`

**Interfaces:**
- Consumes: `TasksGateway.getTaskIdsByUserId(UserId) : List<UUID>` (existing port); `TasksHttpClientConfiguration.TASKS_CLIENT_NAME = "tasks"` (existing constant, already statically imported in both the adapter and the IT); `CircuitBreakerRegistry` (already autowired in the IT); `TasksByUserIdResponse(UUID taskId)` (the JSON shape the recovery test returns); MockServer + Testcontainers harness (existing in `TasksGatewayAdapterIT`).
- Produces: the `@Retry`-annotated `getTaskIdsByUserId`; the property keys `resilience4j.retry.instances.tasks.*`, `resilience4j.circuitbreaker.circuit-breaker-aspect-order`, `resilience4j.retry.retry-aspect-order`.

---

- [ ] **Step 1: Write the failing integration tests**

Edit `TasksGatewayAdapterIT.java`.

**1a.** Add these imports (Spotless will sort them into the right groups):

```java
import static org.mockserver.verify.VerificationTimes.exactly;

import java.util.UUID;

import org.mockserver.matchers.Times;
import org.springframework.http.MediaType;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
```

**1b.** Opt this class into retry by adding one line to the existing `@DynamicPropertySource` method (`max-attempts` overrides the test-classpath default of `1`; `wait-duration=10ms` and `retry-exceptions` come from `src/test/resources/application.properties`, added in Step 5):

```java
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("resilience4j.retry.instances.tasks.max-attempts", () -> "3");
    }
```

**1c.** Inside `@Nested class GetTaskIdsByUserId`, add the recovery test as the **first** method (success case first, per testing-core §1.3):

```java
        @Test
        void ReturnsTaskIds_TasksServiceRecoversAfterTransientErrors() {
            // Given
            var userId = aUser().getId();
            var taskId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request, Times.exactly(2))
                            .respond(response().withStatusCode(500));
            mockServerClient.when(request)
                            .respond(response().withStatusCode(200)
                                               .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                               .withBody("[{\"taskId\":\"" + taskId + "\"}]"));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).containsExactly(taskId);
            assertThat(circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME)
                                             .getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            mockServerClient.verify(request, exactly(3));
        }
```

**1d.** Add the breaker-outer proof test immediately **before** `PropagatesError_TasksServiceReturnsClientError` (failure-accounting case, ahead of the propagation case):

```java
        @Test
        void RecordsOneFailurePerCall_ServerErrorsPersist() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME)
                                             .getMetrics()
                                             .getNumberOfFailedCalls()).isEqualTo(1);

            mockServerClient.verify(request, exactly(3));
        }
```

**1e.** Strengthen the existing `PropagatesError_TasksServiceReturnsClientError` — add a `verify` after the `// Then` assertion to prove 4xx is **not** retried:

```java
            // Then
            assertThat(thrown).isInstanceOf(HttpClientErrorException.class);

            mockServerClient.verify(request, exactly(1));
```

- [ ] **Step 2: Run the new tests to verify they fail**

Developer-gated (Docker). Run:

```bash
mvn -pl services/asapp-users-service -am verify -Dit.test=TasksGatewayAdapterIT -DfailIfNoTests=false
```

Expected: FAIL (the adapter has no `@Retry` yet, so no retry happens regardless of the property).
- `ReturnsTaskIds_TasksServiceRecoversAfterTransientErrors` — the first 500 trips the breaker fallback → empty list; `containsExactly(taskId)` fails (and `verify(exactly(3))` sees 1 call).
- `RecordsOneFailurePerCall_ServerErrorsPersist` — `verify(exactly(3))` sees 1 downstream call.
- `PropagatesError_TasksServiceReturnsClientError` — already passes (4xx is never retried); it stays green.

(The code compiles — these tests only use existing types and properties.)

- [ ] **Step 3: Add `@Retry` to the gateway and update its Javadoc**

In `TasksGatewayAdapter.java`:

**3a.** Add the import (Spotless will place it in the `io.github.resilience4j` group, next to the existing `CircuitBreaker` import):

```java
import io.github.resilience4j.retry.annotation.Retry;
```

**3b.** Replace the class Javadoc (the block immediately above `@Component public class TasksGatewayAdapter`) with:

```java
/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating to the declarative {@link TasksHttpClient} and mapping task responses to their
 * identifiers.
 * <p>
 * The call is guarded by a Resilience4j circuit breaker (instance {@code tasks}): repeated server or I/O errors open the circuit and fast-fail, and the breaker
 * recovers automatically once the Tasks Service is healthy again. Transient downstream outages degrade to an empty list so the user lookup still succeeds,
 * while client errors and unexpected failures propagate — see {@code emptyTasksFallback} for the exact classification.
 * <p>
 * Transient server (5xx) and I/O failures are first retried with exponential backoff by a Resilience4j {@link Retry} (instance {@code tasks}), nested
 * <em>inside</em> the breaker: a momentary blip recovers transparently, and a call that exhausts its retries counts as a single breaker failure. Client (4xx)
 * errors are not retried. Retry attempts and backoff are tuned under {@code resilience4j.retry.instances.tasks.*}.
 *
 * @since 0.2.0
 * @see CircuitBreaker
 * @see Retry
 * @author attrigo
 */
```

**3c.** Add `@Retry` between `@CircuitBreaker` and the method signature (no `fallbackMethod` — retry rethrows to the outer breaker; the retry shape lives entirely in properties):

```java
    @Override
    @CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "emptyTasksFallback")
    @Retry(name = TASKS_CLIENT_NAME)
    public List<UUID> getTaskIdsByUserId(UserId userId) {
```

(Leave `emptyTasksFallback` and the method body unchanged.)

- [ ] **Step 4: Add the production retry config**

In `services/asapp-users-service/src/main/resources/application.properties`, append after the circuit-breaker block (the last `resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=…` line):

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

(`max-attempts=3` = the initial call + 2 retries; `2147483642`/`2147483643` are `LOWEST_PRECEDENCE-5`/`-4`, swapping R4j's defaults so the breaker is outer.)

- [ ] **Step 5: Add the neutralized retry config to the test classpath**

In `services/asapp-users-service/src/test/resources/application.properties`, append after the circuit-breaker block (the last `resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=…` line):

```properties
## Tasks service client retry properties (disabled by default — max-attempts=1; TasksGatewayAdapterIT opts in)
resilience4j.retry.instances.tasks.max-attempts=1
resilience4j.retry.instances.tasks.wait-duration=10ms
resilience4j.retry.instances.tasks.retry-exceptions=org.springframework.web.client.ResourceAccessException,org.springframework.web.client.HttpServerErrorException
## Aspect order — circuit breaker wraps retry (lower value = outer aspect)
resilience4j.circuitbreaker.circuit-breaker-aspect-order=2147483642
resilience4j.retry.retry-aspect-order=2147483643
```

(`max-attempts=1` = 0 retries — retry is a no-op pass-through for every IT that does not override it, so `CircuitBreakerConfigurationIT` / `UserE2EIT` keep their exact counts. `TasksGatewayAdapterIT`'s `@DynamicPropertySource` raises it to `3` and inherits `wait-duration=10ms` + `retry-exceptions` from here. Do **not** add `enable-exponential-backoff` here — fixed 10 ms waits keep the tests fast.)

- [ ] **Step 6: Compile**

```bash
mvn -q -pl services/asapp-users-service -am compile
```

Expected: BUILD SUCCESS (confirms `@Retry` resolves against the R4j API).

- [ ] **Step 7: Run the integration tests to verify they pass**

Developer-gated (Docker). Run:

```bash
mvn -pl services/asapp-users-service -am verify -Dit.test=TasksGatewayAdapterIT -DfailIfNoTests=false
```

Expected: PASS — all of `TasksGatewayAdapterIT`. The recovery test returns the real id (downstream hit ×3, breaker CLOSED); the proof test shows downstream hit ×3 with exactly 1 breaker failure (breaker-outer / retry-inner confirmed); the 4xx test shows downstream hit ×1.

- [ ] **Step 8: Confirm the regression ITs stay green (retry off there)**

Developer-gated (Docker). Both must pass **unchanged** (neither overrides `max-attempts`, so retry stays `1` = off):

```bash
mvn -pl services/asapp-users-service -am verify -Dit.test=CircuitBreakerConfigurationIT,UserE2EIT -DfailIfNoTests=false
```

Expected: PASS. If `CircuitBreakerConfigurationIT` fails on its `verify(…, exactly(N))` counts, retry leaked active onto the test classpath — fix the `max-attempts=1` default in `src/test/resources/application.properties`, **do not loosen the assertions**.

- [ ] **Step 9: (Contingency — only if Step 7's `RecordsOneFailurePerCall` proves the wrong order)**

**The proof test is the arbiter of direction.** Breaker-outer (correct) ⇒ downstream hit `3`× and `getNumberOfFailedCalls()==1`. Breaker-inner (wrong) ⇒ downstream hit `1`× (the breaker's 5xx fallback short-circuits the first attempt) and the recovery test returns empty. If the proof shows breaker-inner, remediate in this order, re-running Step 7 after each:
1. **Sanity-check the config took.** Confirm both order lines are present and identical in `src/main` and `src/test` properties and nothing else overrides them. A typo or a missing test-classpath copy is the likeliest cause.
2. **Make the breaker's order unambiguously the lowest.** Replace the near-`MAX` values with small integers in **both** files: `resilience4j.circuitbreaker.circuit-breaker-aspect-order=1`, `resilience4j.retry.retry-aspect-order=2`. (Same relationship — breaker lower — but immune to any arithmetic surprise near `Integer.MAX_VALUE`.)
3. **If the IT *still* shows breaker-inner, the direction is genuinely inverted on this version** (lower = inner, contradicting Spring's documented `Ordered` contract and R4j's own defaults). Flip the relationship so the breaker takes the **higher** value: `circuit-breaker-aspect-order=2`, `retry-aspect-order=1`, in both files. Re-run — the IT now confirms the correct direction empirically.
4. **If no ordering nests them** (a cross-proxy issue, not relative order), fall back to structural nesting: extract the HTTP call into a new inner `@Component` annotated `@Retry(name = TASKS_CLIENT_NAME)` (method returning `List<TasksByUserIdResponse>` or the mapped ids), and have `TasksGatewayAdapter` (keeping `@CircuitBreaker`) delegate to it. Two separate proxies guarantee breaker-outer / retry-inner regardless of aspect order.

(Based on the verified default orders — CB `MAX-4`, Retry `MAX-5` — and Spring's `Ordered` contract, the Step-4/5 swap should pass Step 7 on the first run; steps 2–4 are escape hatches the user explicitly asked to keep, since the higher-vs-lower direction is to be confirmed empirically, not assumed.)

- [ ] **Step 10: Format and commit**

```bash
mvn -q -pl services/asapp-users-service spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java \
        services/asapp-users-service/src/main/resources/application.properties \
        services/asapp-users-service/src/test/resources/application.properties \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterIT.java
git commit -m "feat(users): retry transient tasks-service failures inside the circuit breaker

Add a Resilience4j @Retry (instance \"tasks\") on TasksGatewayAdapter.getTaskIdsByUserId,
nested inside the existing tasks circuit breaker via swapped aspect-order properties so the
breaker stays the outer aspect. Transient I/O and 5xx failures are retried with exponential
backoff (2 retries, 200ms base, x2 multiplier); 4xx is not retried. The test classpath
neutralizes retry (max-attempts=1) so existing breaker ITs stay unchanged.
TasksGatewayAdapterIT proves breaker-outer/retry-inner ordering.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Documentation and TODO

Narrative docs only — no code. One reviewer gate. The `.claude/rules` addendum is **prepared text**, not an applied edit (writes under `.claude/` are permission-gated).

**Files:**
- Modify: `services/asapp-users-service/README.md`
- Modify: `CLAUDE.md`
- Modify: `TODO.md`
- Prepared text (hand to developer): `.claude/rules/development-patterns.md`

**Interfaces:**
- Consumes: the property keys and behavior delivered by Task 1 (`resilience4j.retry.instances.tasks.*`, the aspect-order keys).
- Produces: nothing code-facing.

---

- [ ] **Step 1: Extend the README Architecture → Resilience section**

In `services/asapp-users-service/README.md`, insert the following immediately after the circuit-breaker tuning table (after the `| `ignore-exceptions` | … |` row at line 192, before the blank line and `### Project Structure`):

```markdown

The same gateway calls are also wrapped in a Resilience4j **retry**, nested **inside** the circuit breaker:

- Transient I/O failures and server (5xx) errors are retried with exponential backoff (default: 2 retries, 200 ms base delay, ×2 multiplier) before the breaker records a failure — a momentary blip recovers transparently.
- A call that exhausts all retries counts as a **single** breaker failure (the breaker is the outer aspect), so sustained failure still trips the breaker exactly as before.
- An open breaker fast-fails *before* any retry runs — no hammering a known-down service.
- Client (4xx) errors are not retried.

Tuning lives in `application.properties` under `resilience4j.retry.instances.<name>.*`:

| Property                         | Purpose                                                      |
|----------------------------------|--------------------------------------------------------------|
| `max-attempts`                   | Total attempts including the first call (`1` disables retry) |
| `wait-duration`                  | Base delay before the first retry                            |
| `enable-exponential-backoff`     | Grow the delay between retries                               |
| `exponential-backoff-multiplier` | Factor the delay grows by on each retry                      |
| `retry-exceptions`               | Exception types that trigger a retry                         |

The breaker is pinned as the outer aspect by swapping the two aspect orders (`resilience4j.circuitbreaker.circuit-breaker-aspect-order` below `resilience4j.retry.retry-aspect-order`).
```

- [ ] **Step 2: Update the README Technology Stack bullet**

In `services/asapp-users-service/README.md`, replace the Resilience bullet (line 228):

```markdown
- **Resilience**: Resilience4j (circuit breaker + retry)
```

(Old line: `- **Resilience**: Resilience4j (circuit breaker)`.)

- [ ] **Step 3: Add the Resilience4j Retry docs link**

In `services/asapp-users-service/README.md`, add a link immediately after the `Resilience4j Circuit Breaker` link (line 422):

```markdown
- [Resilience4j Retry](https://resilience4j.readme.io/docs/retry)
```

- [ ] **Step 4: Note retry on the root `CLAUDE.md` Stack line**

In `CLAUDE.md`, replace the `Stack:` line (line 5) — insert `· Resilience4j (circuit breaker + retry)` between `Liquibase` and `Prometheus (9090)`:

```markdown
Stack: Spring MVC, Spring Data JDBC · Spring Security (JWT) · PostgreSQL · Redis · Liquibase · Resilience4j (circuit breaker + retry) · Prometheus (9090) · Grafana (3000)
```

- [ ] **Step 5: Tick the TODO item**

In `TODO.md`, change line 31:

```markdown
        * [X] Use retry pattern
```

(Old: `        * [ ] Use retry pattern`.)

> **Caution — `TODO.md` has unrelated working-tree edits.** Before committing, run `git diff TODO.md` and confirm what will be staged. If the file carries developer edits beyond this one tick, do **not** bundle them into the docs commit — surface it to the developer and stage only what they approve (the retry tick may belong in their own `TODO.md` commit instead).

- [ ] **Step 6: Commit the docs**

```bash
git add services/asapp-users-service/README.md CLAUDE.md
# Stage TODO.md ONLY after confirming it carries just the retry tick (see Step 5 caution):
# git add TODO.md
git commit -m "docs(users): document retry pattern on the tasks HTTP client

Note the Resilience4j @Retry (nested inside the circuit breaker) in the users-service
README Technology Stack and Resilience sections, add it to the root CLAUDE.md stack line,
and tick the retry-pattern TODO item.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 7: Hand the `.claude/rules` addendum to the developer (do NOT edit `.claude/`)**

Writes under `.claude/` are blocked by the permission gate. Present this to the developer to paste into `.claude/rules/development-patterns.md`, replacing the current `## Service-to-Service Calls` bullet list:

```markdown
## Service-to-Service Calls

- Use the injected `RestClient` bean directly — never construct a new `RestClient` or add an `Authorization` header manually
- `JwtInterceptor` automatically propagates the current request's JWT to all outgoing service calls
- Guard each outbound gateway call with a Resilience4j `@CircuitBreaker` (named instance + a fallback that degrades transient outages to a safe default and rethrows client errors)
- Where transient faults are expected, add a Resilience4j `@Retry` (same instance name) **nested inside** the breaker — swap the aspect orders so the breaker is outer (`circuit-breaker-aspect-order` below `retry-aspect-order`); a call that exhausts its retries counts as one breaker failure, and an open breaker short-circuits before any retry. R4j retry auto-configures from the starter — no enabler class
- Retry only **idempotent** calls and only **transient** faults — I/O errors and server (5xx) responses; never client (4xx) errors
```

- [ ] **Step 8: Final acceptance — full verify**

Developer-gated (Docker). Run the whole build to confirm acceptance criteria (spec §11):

```bash
mvn clean verify
```

Expected: BUILD SUCCESS, all modules green.

---

## Notes on scope (from the spec)

- **No enabler class, no new Java files** — R4j retry auto-configures from `resilience4j-spring-boot4`. (The superseded SF7 design needed an `@EnableResilientMethods` `@Configuration`; this does not.)
- **Retry metrics come for free** — `resilience4j.retry.calls` is exported through the existing Prometheus setup; no wiring needed (spec §7). Per-attempt event logging is out of scope.
- **Out of scope** (spec §10): timeouts (TODO 33–34), the `asapp-rest-clients` → `asapp-http-clients` rename (TODO 32), the user-with-tasks degradation review (TODO 35), and other clients (none exist).
