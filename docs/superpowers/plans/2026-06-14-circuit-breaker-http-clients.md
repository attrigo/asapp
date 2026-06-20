# Circuit Breaker on the Tasks HTTP Client — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guard `asapp-users-service`'s outbound call to the Tasks Service with a Resilience4j circuit breaker that fast-fails when the downstream is unhealthy and auto-recovers, while preserving today's graceful degradation (empty task list → user lookup still succeeds).

**Architecture:** Standalone Resilience4j (`resilience4j-spring-boot4`). A declarative `@CircuitBreaker` on `TasksGatewayAdapter.getTaskIdsByUserId` records downstream failures; its `fallbackMethod` returns an empty list both on failure and while the circuit is open. The `asapp-rest-clients` library stays a pure contract — no resilience dependency leaks into it. Tuning lives in the consumer's local `application.properties`.

**Tech Stack:** Java 25, Spring Boot 4.0.5 / Spring Framework 7, Resilience4j 2.4.0 (`resilience4j-spring-boot4` via `resilience4j-bom`), Maven, JUnit 5 + AssertJ + Mockito + Testcontainers.

**Spec:** `docs/superpowers/specs/2026-06-14-circuit-breaker-http-clients-design.md`

**Conventions the engineer must follow:**
- Run `mvn spotless:apply` before every commit (formatting + import ordering). Pre-commit hooks validate format.
- Maven POM entries are sorted by `<groupId>`/`<artifactId>` within each comment-delimited group (`maven.md`).
- Tests follow `.claude/rules/testing-*.md`: `<Behavior>_<Condition>` names, `// Given/When/Then` blocks, AssertJ only, `catchThrowable` for exceptions, BDDMockito (`given`/`then().should()`).
- **Maven permission convention:** fast commands (unit tests / no integration tests) run autonomously; **slow commands that run integration tests (`*IT`) or mutation tests are gated — confirm with the developer before running them.** Integration tests need Docker (Testcontainers).
- **`.claude/` is gated:** do NOT write under `.claude/`. Task 7 only *outputs* text for the developer to apply.
- Use `git mv` for any rename (none required here).

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `services/pom.xml` | Modify | Add `resilience4j-bom` import + `resilience4j.version` property |
| `services/asapp-users-service/pom.xml` | Modify | Add `resilience4j-spring-boot4` + `spring-boot-starter-aop` |
| `.../users/infrastructure/user/out/TasksGatewayAdapter.java` | Modify | Add `@CircuitBreaker` + `emptyTasksFallback`; drop the in-method `try/catch` |
| `.../test/.../user/out/TasksGatewayAdapterTests.java` | Modify | Unit: core method now propagates failures (fallback is an AOP concern) |
| `.../test/.../user/out/TasksGatewayCircuitBreakerIT.java` | Create | Integration: breaker opens on 5xx, ignores 4xx, fast-fails to empty list |
| `services/asapp-users-service/src/main/resources/application.properties` | Modify | `tasks` breaker tuning + health/metrics toggles |
| `services/asapp-users-service/README.md` | Modify | Technology Stack bullet + `### Resilience` subsection |
| `libs/asapp-rest-clients/README.md` | Modify | One-word accuracy tweak to the wiring-free bullet |
| `CLAUDE.md` (root) | Modify | Add Resilience4j to the Stack line |
| `.claude/rules/development-patterns.md` | **Prepare text only** (gated) | Service-to-Service Calls addendum |
| `TODO.md` | Modify | Tick line 30 |

---

## Task 1: Add the Resilience4j dependencies

**Files:**
- Modify: `services/pom.xml` (properties block ~line 22; dependencyManagement ~line 60)
- Modify: `services/asapp-users-service/pom.xml` (Spring Boot group ~line 30; Other group ~line 85)

- [ ] **Step 1: Add the `resilience4j.version` property to `services/pom.xml`**

In `services/pom.xml`, under `<!-- # BOM Dependencies -->`, add a Resilience4j group **before** the Spring Cloud group (groupId `io.github.resilience4j` sorts before `org.springframework.cloud`). Change:

```xml
        <!-- # BOM Dependencies -->
        <!-- ## Spring Cloud Dependencies -->
        <spring-cloud.version>2025.1.1</spring-cloud.version>
```

to:

```xml
        <!-- # BOM Dependencies -->
        <!-- ## Resilience4j Dependencies -->
        <resilience4j.version>2.4.0</resilience4j.version>
        <!-- ## Spring Cloud Dependencies -->
        <spring-cloud.version>2025.1.1</spring-cloud.version>
```

- [ ] **Step 2: Import the `resilience4j-bom` in `services/pom.xml` `<dependencyManagement>`**

Under `<dependencyManagement><dependencies>`, in the `<!-- # BOM Dependencies -->` block, add the Resilience4j BOM **before** the Spring Cloud BOM:

```xml
            <!-- # BOM Dependencies -->
            <!-- ## Resilience4j Dependencies -->
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-bom</artifactId>
                <version>${resilience4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- ## Spring Cloud Dependencies -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

- [ ] **Step 3: Add the starter + AOP starter to `services/asapp-users-service/pom.xml`**

In the `<!-- ## Spring Boot Dependencies -->` group, add `spring-boot-starter-aop` (sorts after `actuator`, before `data-jdbc`) — this guarantees the `@CircuitBreaker` aspect is proxied:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jdbc</artifactId>
        </dependency>
```

In the `<!-- ## Other Dependencies -->` group, add `resilience4j-spring-boot4` (no version — managed by the BOM) between `com.nimbusds` and `io.micrometer`:

```xml
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot4</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
```

- [ ] **Step 4: Verify the dependencies resolve and the module still compiles**

Run: `mvn -pl services/asapp-users-service -am clean install -DskipTests`
Expected: `BUILD SUCCESS`. (Fast — no integration tests. Confirms `resilience4j-spring-boot4` 2.4.0 resolves from the BOM.)

- [ ] **Step 5: Commit**

```bash
mvn spotless:apply
git add services/pom.xml services/asapp-users-service/pom.xml
git commit -m "build(http-clients): add resilience4j-spring-boot4 dependency"
```

---

## Task 2: Add the circuit breaker to the tasks gateway

The unit test (`@InjectMocks`, no Spring proxy) verifies the **core method** contract only — and that contract changes: it no longer swallows failures (the breaker/fallback, an AOP concern, now owns degradation). The fallback and open-circuit behavior are verified by the IT in Task 4.

**Files:**
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterTests.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java`

- [ ] **Step 1: Update the failing unit test**

In `TasksGatewayAdapterTests.java`, replace the `ReturnsEmptyList_TasksServiceFails` test (lines ~102-114) with a test asserting the core method now **propagates** the failure (no in-method catch). Add the static import for `catchThrowable` at the top with the other AssertJ imports:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
```

Replace the test method:

```java
        @Test
        void ThrowsException_TasksServiceFails() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var tasksServiceError = new RestClientException("connection refused");

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(tasksServiceError);

            // When
            var actual = catchThrowable(() -> tasksGatewayAdapter.getTaskIdsByUserId(userId));

            // Then
            assertThat(actual).isInstanceOf(RestClientException.class);
        }
```

Also update the class Javadoc coverage list (lines ~37-45) — replace the last bullet:

```java
/**
 * Tests {@link TasksGatewayAdapter} task id mapping and failure propagation.
 * <p>
 * Coverage:
 * <li>Maps task responses to their task ids</li>
 * <li>Returns an empty list when the user has no tasks</li>
 * <li>Returns an empty list when the client yields a null response body</li>
 * <li>Propagates the exception when the Tasks Service call fails</li>
 */
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl services/asapp-users-service test -Dtest=TasksGatewayAdapterTests`
Expected: FAIL — `ThrowsException_TasksServiceFails` fails because the current adapter catches `RestClientException` and returns an empty list instead of propagating.

- [ ] **Step 3: Modify the adapter — add `@CircuitBreaker`, add the fallback, drop the catch**

In `TasksGatewayAdapter.java`:

Replace the import of `RestClientException` with the Resilience4j annotation import (place per import grouping — `mvn spotless:apply` will order it):

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
```

(Remove `import org.springframework.web.client.RestClientException;` — no longer used.)

Update the class Javadoc to document the breaker:

```java
/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating to the declarative {@link TasksHttpClient} and mapping task responses to their
 * identifiers. The call is guarded by a Resilience4j circuit breaker (instance {@code tasks}): repeated I/O or server errors open the circuit and fast-fail,
 * and the breaker recovers automatically once the Tasks Service is healthy again.
 * <p>
 * On any downstream failure or while the circuit is open, the {@code emptyTasksFallback} method logs a warning and returns an empty list, preventing cascading
 * failures so the user lookup still succeeds. Client (4xx) errors do not open the circuit.
 *
 * @since 0.2.0
 * @see CircuitBreaker
 * @author attrigo
 */
```

Replace the `getTaskIdsByUserId` method body (remove the `try/catch`) and add the fallback. The fallback's warning message **must keep the existing wording** (`"Failed to retrieve tasks for user ..."` / `"Returning empty list."`) — `UserE2EIT` asserts on it:

```java
    @CircuitBreaker(name = "tasks", fallbackMethod = "emptyTasksFallback")
    @Override
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

    /**
     * Returns an empty task id list when the Tasks Service call fails or the circuit is open.
     * <p>
     * Invoked reflectively by Resilience4j as the {@code tasks} circuit breaker fallback; the trailing {@link Throwable} carries the downstream failure or the
     * open-circuit {@code CallNotPermittedException}.
     *
     * @param userId the user's unique identifier
     * @param t      the failure that triggered the fallback
     * @return an empty {@link List}
     */
    private List<UUID> emptyTasksFallback(UserId userId, Throwable t) {
        logger.warn("Failed to retrieve tasks for user {}: {}. Returning empty list.", userId.value(), t.getMessage());
        return List.of();
    }
```

- [ ] **Step 4: Run the unit tests to verify they pass**

Run: `mvn -pl services/asapp-users-service test -Dtest=TasksGatewayAdapterTests`
Expected: PASS — all four tests green (mapping, no-tasks, null body, throws-on-failure). The `@CircuitBreaker` annotation is inert here (no Spring proxy in a Mockito unit test), so the bare method propagates the exception as asserted.

- [ ] **Step 5: Commit**

```bash
mvn spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterTests.java
git commit -m "feat(http-clients): guard tasks gateway with a circuit breaker"
```

---

## Task 3: Configure the `tasks` circuit breaker

**Files:**
- Modify: `services/asapp-users-service/src/main/resources/application.properties`

- [ ] **Step 1: Add the breaker tuning and health/metrics toggles**

Append the breaker tuning under the existing `## Tasks service client properties` block (end of file):

```properties
## Tasks service client circuit breaker properties
resilience4j.circuitbreaker.instances.tasks.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.tasks.sliding-window-size=10
resilience4j.circuitbreaker.instances.tasks.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.tasks.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.tasks.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.tasks.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.instances.tasks.register-health-indicator=true
resilience4j.circuitbreaker.instances.tasks.allow-health-indicator-to-fail=false
resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=org.springframework.web.client.HttpClientErrorException
```

In the `# Management properties` block, add the circuit-breaker health toggle (alphabetical — `health` before `server`):

```properties
# Management properties
management.health.circuitbreakers.enabled=true
management.server.base-path=${server.servlet.context-path}
management.server.port=8092
```

> **Note:** these properties live only in `src/main/resources` (runtime). Tests load `src/test/resources/application.properties` (which does not include them); the Task 4 IT supplies its own breaker config via `@TestPropertySource`, so existing tests keep Resilience4j defaults (window/min-calls = 100 → the breaker never opens in `UserE2EIT`, preserving current behavior).

- [ ] **Step 2: Verify the module still builds**

Run: `mvn -pl services/asapp-users-service -am clean install -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add services/asapp-users-service/src/main/resources/application.properties
git commit -m "feat(http-clients): tune the tasks circuit breaker"
```

---

## Task 4: Integration test — breaker opens on 5xx, ignores 4xx

This is the test that exercises the **real AOP wiring**: it boots the application context (like `UserE2EIT`), replaces the HTTP client proxy with a Mockito mock, drives failures through the proxied adapter, and asserts breaker state via the autowired `CircuitBreakerRegistry`.

**Files:**
- Create: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayCircuitBreakerIT.java`

- [ ] **Step 1: Write the integration test**

```java
/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.users.infrastructure.user.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Tests {@link TasksGatewayAdapter} circuit breaker behavior through the proxied bean.
 * <p>
 * Coverage:
 * <li>Opens the circuit and stops calling the Tasks Service once the failure rate threshold is exceeded</li>
 * <li>Degrades to an empty list while the circuit is open</li>
 * <li>Does not open the circuit on client (4xx) errors</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfiguration.class)
@Testcontainers
@TestPropertySource(properties = { "resilience4j.circuitbreaker.instances.tasks.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.tasks.sliding-window-size=10", "resilience4j.circuitbreaker.instances.tasks.minimum-number-of-calls=5",
        "resilience4j.circuitbreaker.instances.tasks.failure-rate-threshold=50", "resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state=10s",
        "resilience4j.circuitbreaker.instances.tasks.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=org.springframework.web.client.HttpClientErrorException" })
class TasksGatewayCircuitBreakerIT {

    @MockitoBean
    private TasksHttpClient tasksHttpClient;

    @Autowired
    private TasksGateway tasksGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void beforeEach() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("tasks");
        circuitBreaker.reset();
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void OpensCircuitAndStopsCallingTasksService_FailureRateThresholdExceeded() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // When
            for (int i = 0; i < 5; i++) {
                assertThat(tasksGateway.getTaskIdsByUserId(userId)).isEmpty();
            }
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            then(tasksHttpClient).should(times(5))
                                 .getTasksByUserId(userId.value());
        }

        @Test
        void KeepsCircuitClosed_TasksServiceReturnsClientError() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(circuitBreaker.getMetrics()
                                     .getNumberOfFailedCalls()).isZero();
        }

    }

}
```

- [ ] **Step 2: Run the integration test (GATED — confirm with the developer first; needs Docker)**

Run: `mvn -pl services/asapp-users-service verify -Dit.test=TasksGatewayCircuitBreakerIT -Dtest=void -DfailIfNoTests=false`
Expected: PASS — both tests green.

What each test proves:
- `OpensCircuitAndStopsCallingTasksService_...`: 5 forced 5xx failures (≥ `minimum-number-of-calls`, 100% > 50% threshold) trip the breaker; the 6th call is short-circuited → client invoked exactly 5 times, state `OPEN`, degraded to empty list.
- `KeepsCircuitClosed_...`: a 4xx is in `ignore-exceptions`, so the breaker records no failure (stays `CLOSED`) yet the fallback still degrades to an empty list.

> **If `KeepsCircuitClosed_...` errors instead of returning empty** (i.e. the ignored exception propagates rather than reaching the fallback), that means Resilience4j does not route ignored exceptions through `fallbackMethod` on this version — escalate to the developer before changing the degradation contract (do not silently widen `ignore-exceptions` handling).

- [ ] **Step 3: Commit**

```bash
mvn spotless:apply
git add services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayCircuitBreakerIT.java
git commit -m "test(http-clients): cover tasks circuit breaker open and ignore-4xx behavior"
```

---

## Task 5: Verify the existing E2E degradation still passes

`UserE2EIT.ReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsAndTasksServiceFails` returns a 500 from MockServer and asserts the user still resolves with an empty task list **and** that the logs contain `"Failed to retrieve tasks for user <id>"` and `"Returning empty list"`. Task 2 preserved that log wording, and Task 3's config is absent from the test classpath, so the breaker keeps defaults and never opens during this run.

**Files:** none (verification only).

- [ ] **Step 1: Run `UserE2EIT` (GATED — confirm with the developer first; needs Docker)**

Run: `mvn -pl services/asapp-users-service verify -Dit.test=UserE2EIT -Dtest=void -DfailIfNoTests=false`
Expected: PASS — all `UserE2EIT` tests green, including the tasks-service-failure degradation test (unchanged behavior).

- [ ] **Step 2: No commit** unless a change was required. If the log assertion failed, the fallback wording in `TasksGatewayAdapter.emptyTasksFallback` drifted from the original — restore it to `"Failed to retrieve tasks for user {}: {}. Returning empty list."`, re-run, then commit the fix:

```bash
mvn spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java
git commit -m "fix(http-clients): keep tasks fallback log wording stable"
```

---

## Task 6: Documentation

**Files:**
- Modify: `services/asapp-users-service/README.md`
- Modify: `libs/asapp-rest-clients/README.md`
- Modify: `CLAUDE.md` (root)

- [ ] **Step 1: Add the Technology Stack bullet (`services/asapp-users-service/README.md`)**

In the `## Technology Stack` list, add a `Resilience` bullet after the `REST Clients` line:

```markdown
- **REST Clients**: Spring HTTP Interfaces (@HttpExchange)
- **Resilience**: Resilience4j (circuit breaker)
```

- [ ] **Step 2: Add the `### Resilience` subsection (`services/asapp-users-service/README.md`)**

In the `## Architecture` section, add this subsection immediately after the `### Data Stores` block (before `### Project Structure`):

```markdown
### Resilience

Outbound calls to the Tasks Service go through a Resilience4j **circuit breaker** (instance `tasks`) on the tasks gateway. On repeated I/O or 5xx failures the breaker opens and fast-fails; while open — or on any single downstream failure — the gateway degrades to an empty task list, so user lookups still succeed. Client (4xx) errors do not trip the breaker. Breaker state and metrics are exported to Prometheus, and the breaker contributes a non-failing component to `/actuator/health`. Thresholds are tunable in `application.properties` (`resilience4j.circuitbreaker.instances.tasks.*`).
```

- [ ] **Step 3: Tweak the library README wiring bullet (`libs/asapp-rest-clients/README.md`)**

In `## Overview` → **Key Features**, change the wiring-free bullet (line ~20):

```markdown
- ✅ Wiring-free contracts — base URL, auth, load balancing, and resilience are owned by the consuming service
```

- [ ] **Step 4: Add Resilience4j to the Stack line (`CLAUDE.md`, root)**

Change the `Stack:` line to insert Resilience4j after Spring Security:

```markdown
Stack: Spring MVC, Spring Data JDBC · Spring Security (JWT) · Resilience4j · PostgreSQL · Redis · Liquibase · Prometheus (9090) · Grafana (3000)
```

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service/README.md libs/asapp-rest-clients/README.md CLAUDE.md
git commit -m "docs(http-clients): document the tasks circuit breaker"
```

---

## Task 7: Prepare the `.claude/rules` update for the developer (gated)

`.claude/` is gated — do **not** edit it. This task produces the exact replacement text and hands it to the developer.

**Files:** none written by the agent.

- [ ] **Step 1: Output the proposed replacement for the "Service-to-Service Calls" section**

Present this to the developer with the instruction: *"Apply this to `.claude/rules/development-patterns.md` (it also updates the now-stale `RestClient` wording from before the declarative-clients refactor)."*

Replace the current section:

```markdown
## Service-to-Service Calls

- Use the injected `RestClient` bean directly — never construct a new `RestClient` or add an `Authorization` header manually
- `JwtInterceptor` automatically propagates the current request's JWT to all outgoing service calls
```

with:

```markdown
## Service-to-Service Calls

- Outgoing calls go through an injected declarative `@HttpExchange` client proxy (e.g. `TasksHttpClient`) — never construct a `RestClient` or add an `Authorization` header manually
- JWT propagation is automatic: the group configurer's `JwtInterceptor` re-emits the caller's bearer token on every outgoing call
- Guard each outbound gateway adapter with a Resilience4j `@CircuitBreaker` whose `fallbackMethod` degrades to a neutral result (e.g. empty list); let the downstream exception reach the breaker — never swallow it in a `try/catch` before the breaker sees it
```

- [ ] **Step 2: No commit** (no agent-writable files changed).

---

## Task 8: Tick the TODO and run final verification

**Files:**
- Modify: `TODO.md` (line 30)

- [ ] **Step 1: Tick the circuit breaker item**

In `TODO.md`, under `* Improve HTTP clients`, change:

```markdown
        * [ ] Use circuit breaker pattern
```

to:

```markdown
        * [X] Use circuit breaker pattern
```

- [ ] **Step 2: Full verification (GATED — confirm with the developer; runs all integration tests, needs Docker)**

Run: `mvn -pl services/asapp-users-service -am clean verify`
Expected: `BUILD SUCCESS` — all unit + integration tests green.

- [ ] **Step 3: Manual observability check (GATED — runs the app)**

Suggest the developer run the service (`cd services/asapp-users-service && mvn spring-boot:run`) and confirm:
- `GET http://localhost:8092/asapp-users-service/actuator/health` (HTTP Basic `user`/`secret`) shows a `circuitBreakers` component reporting the `tasks` breaker, and overall status stays `UP` even if it is open.
- `GET http://localhost:8092/asapp-users-service/actuator/prometheus` exposes `resilience4j_circuitbreaker_*` metrics for `name="tasks"`.

If the `circuitBreakers` health component or the metrics are absent, `resilience4j-micrometer` was not pulled transitively — add `io.github.resilience4j:resilience4j-micrometer` (version-managed by the BOM) to `services/asapp-users-service/pom.xml` in the `## Other Dependencies` group, then re-verify.

- [ ] **Step 4: Commit**

```bash
git add TODO.md
git commit -m "docs(http-clients): mark circuit breaker task done"
```

---

## Done

After Task 8, the tasks gateway is guarded by a Resilience4j circuit breaker: it records I/O/5xx failures, fast-fails when the Tasks Service is unhealthy, auto-recovers, and preserves the empty-list degradation — with metrics in Prometheus and a non-failing health component. The library remains a pure contract, and the `.claude/rules` update is queued for the developer to apply.
