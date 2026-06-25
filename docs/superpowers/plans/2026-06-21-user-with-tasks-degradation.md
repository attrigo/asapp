# User-with-Tasks Degradation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When tasks-service is unavailable, `GET /users/{id}` returns the user with `taskIds: null` and a `warnings: ["tasks_unavailable"]` array (partial-success 200) instead of a misleading empty list, with the degrade policy owned by the application service.

**Architecture:** Split the resilience *mechanism* from the degrade *policy*. The adapter (`TasksGatewayAdapter`) keeps the Resilience4j circuit breaker + retry but, on a degradable outage, translates the failure into a typed `TasksUnavailableException`. The application service (`ReadUserService`) catches it and produces a degraded `UserWithTasksResult` (explicit `tasksAvailable` flag). The infrastructure mapper translates that flag into the `tasks_unavailable` warning code on the response DTO.

**Tech Stack:** Java 25, Spring Boot 4, Spring MVC, MapStruct, Resilience4j (circuit breaker + retry), Jackson, JUnit 5 + AssertJ + BDDMockito + MockServer + Testcontainers, Spring REST Docs.

**Spec:** `docs/superpowers/specs/2026-06-21-user-with-tasks-degradation-design.md`

## Global Constraints

These apply to **every** task. Each task's requirements implicitly include this section.

- **Copyright header:** every new `.java` file starts with the standard Apache-2.0 header used across the repo:
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
  ```
- **Javadoc:** production public classes/interfaces require `@since 0.4.0`; summary line starts with a verb; `@see` only for framework/library types. Package-private constant holders follow `ErrorMessages` (carry `@since 0.4.0`). Test classes omit `@since`.
- **Annotation ordering** (`code-style.md`): component role → config/routing → persistence → serialization (`@JsonInclude`) → validation → mapping (`@Mapping`).
- **Testing** (`testing-*.md`): `<Behavior>_<Condition>` names (no `@DisplayName`); `// Given/When/Then` blocks; AssertJ only; `catchThrowable()` for exceptions; BDDMockito (`given`/`willThrow`/`then`); `assertSoftly` with mandatory `.as()` before the assertion when asserting ≥3 properties on one root; mock fields without "Mock" suffix; result variable named `actual`; `@formatter:off/on` around aligned multi-line assertions.
- **REST** (`rest.md`): RFC 7807 for errors; camelCase fields; **no `@JsonProperty` renames** (`@JsonInclude` is an inclusion policy, allowed); one response record per endpoint.
- **Ports & adapters** (`ports-adapters.md`): translate infra exceptions into application exceptions only when the application service catches them; orchestration exceptions extend `RuntimeException` in `application/<aggregate>/`.
- **File ops:** never delete-and-recreate to rename; use `git mv`. (No renames in this plan.)
- **Build:** `mvn spotless:apply` before committing (pre-commit hook also checks). Fast tests run autonomously; **slow ITs (`*IT`) require asking the developer before running** — Tasks 2, 4, 5 below have slow ITs and call this out.
- **Commits:** Conventional Commits; pre-commit hook validates format + formatting.

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `application/user/TasksUnavailableException.java` (new) | Typed port-level exception signalling tasks-service outage | 2 |
| `application/user/in/result/UserWithTasksResult.java` | Add `tasksAvailable` flag + `available()`/`unavailable()` factories + invariant | 1 |
| `application/user/out/TasksGateway.java` | Update contract Javadoc (throws on outage) | 2 |
| `application/user/in/service/ReadUserService.java` | Catch `TasksUnavailableException` → degraded result (policy) | 3 |
| `application/user/in/ReadUserUseCase.java` | Update Javadoc | 3 |
| `infrastructure/user/out/TasksGatewayAdapter.java` | Fallback translates outage → `TasksUnavailableException` (mechanism) | 2 |
| `infrastructure/user/in/response/GetUserByIdResponse.java` | Add `warnings` (`@JsonInclude(NON_EMPTY)`); `taskIds` nullable | 4 |
| `infrastructure/user/mapper/WarningCodes.java` (new) | Package-private warning-code constants | 4 |
| `infrastructure/user/mapper/UserMapper.java` | Derive `warnings` from `tasksAvailable` | 4 |
| `src/docs/asciidoc/api-guide.adoc` | Document partial-success contract | 5 |
| `services/asapp-users-service/README.md` | Update Resilience section (degrade-to-empty → partial response) | 6 |
| `TODO.md` | Tick line 33 | 6 |

**Test files:** `UserWithTasksResultTests` (T1), `TasksGatewayAdapterIT` + `ResilienceConfigurationIT` (T2), `ReadUserServiceTests` (T3), `UserE2EIT` (T4), `UserRestControllerDocumentationIT` (T5).

---

### Task 1: Add `tasksAvailable` to `UserWithTasksResult` (behavior-preserving)

Introduce the degraded-state model and migrate all construction sites to factories. No degrade behavior yet — `available(...)` everywhere keeps current behavior identical.

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/result/UserWithTasksResult.java`
- Modify (call site): `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java:132`
- Modify (call site): `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java:102`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/result/UserWithTasksResultTests.java`

**Interfaces:**
- Produces:
  - `UserWithTasksResult(User user, List<UUID> taskIds, boolean tasksAvailable)` — canonical record
  - `static UserWithTasksResult available(User user, List<UUID> taskIds)` — `tasksAvailable=true`, requires non-null `taskIds`
  - `static UserWithTasksResult unavailable(User user)` — `tasksAvailable=false`, `taskIds=null`
  - Invariant messages: `"User must not be null"`, `"Task IDs list must not be null when tasks are available"`, `"Task IDs list must be null when tasks are unavailable"`

- [ ] **Step 1: Rewrite the failing test** `UserWithTasksResultTests.java`

Replace the whole `@Nested class CreateUserWithTasksResult` body with:

```java
    @Nested
    class CreateUserWithTasksResult {

        @Test
        void ReturnsAvailableResult_ValidUserAndTaskIds() {
            // Given
            var user = aUser();
            var taskId1 = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789");
            var taskId2 = UUID.fromString("b2c3d4e5-f6a7-4890-bcde-f01234567890");
            var taskIds = List.of(taskId1, taskId2);

            // When
            var actual = UserWithTasksResult.available(user, taskIds);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.user()).as("user").isEqualTo(user);
                softly.assertThat(actual.taskIds()).as("task IDs").containsExactly(taskId1, taskId2);
                softly.assertThat(actual.tasksAvailable()).as("tasks availability").isTrue();
                // @formatter:on
            });
        }

        @Test
        void ReturnsAvailableResult_ValidUserAndEmptyTaskIds() {
            // Given
            var user = aUser();
            var taskIds = List.<UUID>of();

            // When
            var actual = UserWithTasksResult.available(user, taskIds);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.taskIds()).as("task IDs").isEmpty();
                softly.assertThat(actual.tasksAvailable()).as("tasks availability").isTrue();
                // @formatter:on
            });
        }

        @Test
        void ReturnsUnavailableResult_TasksUnavailable() {
            // Given
            var user = aUser();

            // When
            var actual = UserWithTasksResult.unavailable(user);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.user()).as("user").isEqualTo(user);
                softly.assertThat(actual.taskIds()).as("task IDs").isNull();
                softly.assertThat(actual.tasksAvailable()).as("tasks availability").isFalse();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUser() {
            // When
            var actual = catchThrowable(() -> UserWithTasksResult.available(null, List.<UUID>of()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTaskIdsWhenAvailable() {
            // Given
            var user = aUser();

            // When
            var actual = catchThrowable(() -> new UserWithTasksResult(user, null, true));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task IDs list must not be null when tasks are available");
        }

        @Test
        void ThrowsIllegalArgumentException_NonNullTaskIdsWhenUnavailable() {
            // Given
            var user = aUser();

            // When
            var actual = catchThrowable(() -> new UserWithTasksResult(user, List.<UUID>of(), false));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task IDs list must be null when tasks are unavailable");
        }

    }
```

Update the class Javadoc coverage list to:
```java
 * <li>Creates an available result with user and task identifiers</li>
 * <li>Creates an available result with user and empty task list</li>
 * <li>Creates an unavailable result with null task list</li>
 * <li>Rejects null user</li>
 * <li>Rejects null task identifiers when tasks are available</li>
 * <li>Rejects non-null task identifiers when tasks are unavailable</li>
```

- [ ] **Step 2: Run test to verify it fails to compile/run**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=UserWithTasksResultTests`
Expected: FAIL — compilation error (`available`/`unavailable` not defined, 3-arg constructor absent).

- [ ] **Step 3: Implement the record change**

Replace the `UserWithTasksResult` record body (keep license header + update Javadoc params) with:

```java
public record UserWithTasksResult(
        User user,
        List<UUID> taskIds,
        boolean tasksAvailable
) {

    public UserWithTasksResult {
        validateUserIsNotNull(user);
        validateTaskIdsConsistency(taskIds, tasksAvailable);
    }

    /**
     * Creates a result for a user whose tasks were retrieved successfully.
     *
     * @param user    the user entity
     * @param taskIds the retrieved task identifiers (never {@code null}; empty when the user has no tasks)
     * @return an available {@code UserWithTasksResult}
     */
    public static UserWithTasksResult available(User user, List<UUID> taskIds) {
        return new UserWithTasksResult(user, taskIds, true);
    }

    /**
     * Creates a degraded result for a user whose tasks could not be retrieved because tasks-service is unavailable.
     *
     * @param user the user entity
     * @return an unavailable {@code UserWithTasksResult} with {@code null} task identifiers
     */
    public static UserWithTasksResult unavailable(User user) {
        return new UserWithTasksResult(user, null, false);
    }

    private static void validateUserIsNotNull(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
    }

    private static void validateTaskIdsConsistency(List<UUID> taskIds, boolean tasksAvailable) {
        if (tasksAvailable && taskIds == null) {
            throw new IllegalArgumentException("Task IDs list must not be null when tasks are available");
        }
        if (!tasksAvailable && taskIds != null) {
            throw new IllegalArgumentException("Task IDs list must be null when tasks are unavailable");
        }
    }

}
```

Update the record-level Javadoc to document `tasksAvailable` (e.g. `@param tasksAvailable whether the task references were successfully retrieved; {@code false} indicates degraded data`).

- [ ] **Step 4: Migrate the two construction call sites (compile fix, behavior-preserving)**

In `ReadUserService.java:132`, change:
```java
        var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
        return new UserWithTasksResult(user, taskIds);
```
to:
```java
        var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
        return UserWithTasksResult.available(user, taskIds);
```

In `UserRestControllerDocumentationIT.java:102`, change:
```java
            var userWithTasksResult = new UserWithTasksResult(user, List.of());
```
to:
```java
            var userWithTasksResult = UserWithTasksResult.available(user, List.of());
```

- [ ] **Step 5: Run the test + compile to verify they pass**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=UserWithTasksResultTests`
Expected: PASS (6 tests). Also `mvn -q -pl services/asapp-users-service test-compile` succeeds.

- [ ] **Step 6: Format + commit**

```bash
mvn -q spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/result/UserWithTasksResult.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/result/UserWithTasksResultTests.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java
git commit -m "refactor(users): model task availability in UserWithTasksResult

Add a tasksAvailable flag plus available()/unavailable() factories and a consistency invariant, migrating construction sites to the factories. Behavior is unchanged; this is the seam the degrade policy builds on.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Translate tasks-service outages to `TasksUnavailableException`

The adapter stops degrading-to-empty and instead throws a typed exception on outage. Mechanism (breaker/retry) is unchanged; only the fallback's *outcome* changes.

**Files:**
- Create: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/TasksUnavailableException.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java`
- Modify (Javadoc): `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/out/TasksGateway.java`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterIT.java`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/ResilienceConfigurationIT.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `com.bcn.asapp.users.application.user.TasksUnavailableException extends RuntimeException` with constructor `(String message, Throwable cause)`. The adapter throws it from its `@CircuitBreaker` fallback on 5xx / I/O / open-circuit; `getTaskIdsByUserId` still returns `List<UUID>` on success and propagates 4xx (`HttpClientErrorException`) and unexpected errors.

- [ ] **Step 1: Create the exception**

`application/user/TasksUnavailableException.java` (with the standard header):

```java
package com.bcn.asapp.users.application.user;

/**
 * Signals that the tasks-service could not be reached or returned a server error, so task data is temporarily unavailable.
 * <p>
 * Thrown by the tasks gateway adapter when a downstream outage (5xx, I/O failure, or open circuit) is detected, and caught by the application service to degrade
 * the user read gracefully.
 *
 * @since 0.4.0
 * @author attrigo
 */
public class TasksUnavailableException extends RuntimeException {

    /**
     * Constructs a new {@code TasksUnavailableException} with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying downstream failure
     */
    public TasksUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

- [ ] **Step 2: Update the failing adapter IT** `TasksGatewayAdapterIT.java`

Add import:
```java
import com.bcn.asapp.users.application.user.TasksUnavailableException;
```
Replace the two `ReturnsEmptyList_*` test methods with:

```java
        @Test
        void ThrowsTasksUnavailableException_TasksServiceReturnsServerError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(TasksUnavailableException.class);
        }

        @Test
        void ThrowsTasksUnavailableException_TasksServiceUnreachable() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());
            mockServerClient.when(request)
                            .error(error().withDropConnection(true));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(TasksUnavailableException.class);
        }
```

Update the class Javadoc coverage list — replace the two "Degrades to an empty list…" lines with:
```java
 * <li>Throws {@link TasksUnavailableException} when the Tasks Service responds with a server error (5xx)</li>
 * <li>Throws {@link TasksUnavailableException} when the Tasks Service connection is dropped</li>
```

- [ ] **Step 3: Fix `ResilienceConfigurationIT` outage call sites**

These tests call the gateway on an outage path and previously relied on degrade-to-empty (no throw). Wrap each such call in `catchThrowable(...)` so the now-thrown `TasksUnavailableException` is swallowed (the assertions are on breaker state / call counts, not the return value). `catchThrowable` is already imported.

In `KeepsCircuitClosed_FailuresBelowMinimumCalls`, change:
```java
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS - 1)
                 .forEach(_ -> tasksGateway.getTaskIdsByUserId(userId));
```
to:
```java
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS - 1)
                 .forEach(_ -> catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId)));
```

In the `openCircuit(...)` helper, change:
```java
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS)
                 .forEach(_ -> tasksGateway.getTaskIdsByUserId(userId));
```
to:
```java
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS)
                 .forEach(_ -> catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId)));
```

In `OpensCircuit_ServerErrorsExceedFailureThreshold`, change the single triggering call:
```java
        // When
        tasksGateway.getTaskIdsByUserId(userId);
```
to:
```java
        // When
        catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));
```

Apply the identical `catchThrowable(...)` wrap to the single `tasksGateway.getTaskIdsByUserId(userId);` call under `// When` in **`RetriesInsideCircuitBreaker_ServerErrorsPersist`** and in **`RetriesInsideCircuitBreaker_DownstreamReadTimesOut`**.

Do **not** change `KeepsCircuitClosed_ClientErrorsExceedFailureThreshold` (4xx still throws `HttpClientErrorException`), `RetriesCall_TransientDownstreamServerErrors` (ends in 200), or `ClosesCircuit_DownstreamServiceRecovers` (recovery path returns 200; its only outage calls are inside `openCircuit`, fixed above).

In the class Javadoc, change the connect-timeout note `(same I/O failure, retry, circuit breaker, degrade-to-empty)` to `(same I/O failure, retry, circuit breaker, translate-to-TasksUnavailableException)`.

- [ ] **Step 4: Implement the adapter translation**

In `TasksGatewayAdapter.java`:
1. Add import: `import com.bcn.asapp.users.application.user.TasksUnavailableException;`
2. Change the annotation fallback name:
```java
    @CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "tasksUnavailableFallback")
```
3. Replace the `emptyTasksFallback` method with:
```java
    /**
     * Translates a tasks-service outage into a {@link TasksUnavailableException}, or rethrows non-outage failures.
     * <p>
     * Invoked reflectively by Resilience4j as the {@code tasks} circuit breaker fallback:
     * <ul>
     * <li>Server (5xx) errors ({@link HttpServerErrorException}), I/O failures ({@link ResourceAccessException}), and the open-circuit
     * {@link CallNotPermittedException} are translated into a {@link TasksUnavailableException} so the application service can degrade gracefully.</li>
     * <li>Client (4xx) errors and any unexpected failure are rethrown so callers and the error handler can surface them.</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @param cause  the failure that triggered the fallback
     * @return never returns normally for an outage
     * @throws TasksUnavailableException when the downstream service is unavailable or the circuit is open
     * @throws Throwable                 the original failure when it is not a recoverable downstream outage (e.g. a 4xx client error or a bug)
     */
    private List<UUID> tasksUnavailableFallback(UserId userId, Throwable cause) throws Throwable {
        if (cause instanceof HttpServerErrorException || cause instanceof ResourceAccessException || cause instanceof CallNotPermittedException) {
            var className = cause.getClass()
                                 .getSimpleName();
            var message = cause.getMessage();
            logger.warn("Tasks Service unavailable for user {}: {} - {}.", userId.value(), className, message);
            throw new TasksUnavailableException("Tasks Service is unavailable", cause);
        }

        throw cause;
    }
```
4. Update the `getTaskIdsByUserId` Javadoc: the "Degradation" bullet now reads that outages are translated into a `TasksUnavailableException` (no longer "degrade to an empty list"), and the `@return` drops the "or an empty list if … the tasks circuit breaker degrades a downstream outage" clause — it returns task UUIDs (empty only for a genuine no-tasks/null-body response).

- [ ] **Step 5: Update the port contract Javadoc** `TasksGateway.java`

Replace the "should handle communication failures gracefully…" paragraph and the `@return` tail with:
```java
     * <p>
     * When tasks-service is unavailable (server error, I/O failure, or open circuit), the implementation throws {@link TasksUnavailableException} so the caller
     * can decide how to degrade. A genuine empty result (the user has no tasks) is returned as an empty list.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, or an empty list if the user has no tasks
     * @throws com.bcn.asapp.users.application.user.TasksUnavailableException if tasks-service is unavailable
```

- [ ] **Step 6: Run the slow ITs (ASK DEVELOPER FIRST)**

These are slow (`@SpringBootTest` + MockServer + Testcontainers). Per project convention, confirm with the developer before running.

Run: `mvn -q -pl services/asapp-users-service test -Dtest=TasksGatewayAdapterIT,ResilienceConfigurationIT`
Expected: PASS — adapter throws `TasksUnavailableException` on 5xx/connection-drop; 4xx still throws `HttpClientErrorException`; breaker state/count assertions unchanged.

- [ ] **Step 7: Format + commit**

```bash
mvn -q spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/TasksUnavailableException.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/out/TasksGateway.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterIT.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/ResilienceConfigurationIT.java
git commit -m "feat(users): translate tasks-service outages into a typed exception

Replace the circuit-breaker fallback's degrade-to-empty with a typed TasksUnavailableException on 5xx / I/O / open-circuit, so the degrade-vs-fail decision can move to the application service. 4xx and unexpected errors still propagate; the resilience mechanism is unchanged.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Move the degrade policy into `ReadUserService`

The application service catches `TasksUnavailableException` and returns a degraded result.

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java`
- Modify (Javadoc): `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/ReadUserUseCase.java`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/service/ReadUserServiceTests.java`

**Interfaces:**
- Consumes: `UserWithTasksResult.available/unavailable` (Task 1), `TasksUnavailableException` (Task 2).
- Produces: `ReadUserService.getUserById` returns `Optional.of(UserWithTasksResult.unavailable(user))` when the gateway throws `TasksUnavailableException`; other gateway exceptions still propagate.

- [ ] **Step 1: Add the failing test** in `ReadUserServiceTests.java` `@Nested class GetUserById`

Add import:
```java
import com.bcn.asapp.users.application.user.TasksUnavailableException;
```
Add this test after `ReturnsUserWithTasks_UserHasTasks`:

```java
        @Test
        void ReturnsDegradedResult_TasksUnavailable() {
            // Given
            var user = aUser();
            var userId = user.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            willThrow(new TasksUnavailableException("Tasks Service is unavailable", new RuntimeException("boom"))).given(tasksGateway)
                                                                                                                  .getTaskIdsByUserId(userId);

            // When
            var actual = readUserService.getUserById(userId.value());

            // Then
            assertThat(actual).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().user()).as("user").isEqualTo(user);
                softly.assertThat(actual.get().taskIds()).as("task IDs").isNull();
                softly.assertThat(actual.get().tasksAvailable()).as("tasks availability").isFalse();
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
        }
```

> Note: the existing `ThrowsRuntimeException_TaskGatewayOperationFails` test (a generic `RuntimeException`) stays unchanged — it now documents that *non-degradable* gateway failures still propagate (only `TasksUnavailableException` is caught).

Add to the class Javadoc coverage list:
```java
 * <li>Returns a degraded result when tasks-service is unavailable</li>
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=ReadUserServiceTests`
Expected: FAIL — `ReturnsDegradedResult_TasksUnavailable` errors (the exception propagates instead of degrading; `actual` is never assigned).

- [ ] **Step 3: Implement the catch in `ReadUserService`**

Add imports:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bcn.asapp.users.application.user.TasksUnavailableException;
```
Add a logger field (top of class body):
```java
    private static final Logger logger = LoggerFactory.getLogger(ReadUserService.class);
```
Replace `enrichUserWithTasks` with:
```java
    private UserWithTasksResult enrichUserWithTasks(User user) {
        try {
            var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
            return UserWithTasksResult.available(user, taskIds);
        } catch (TasksUnavailableException ex) {
            logger.warn("Tasks unavailable for user {}; returning degraded result.", user.getId()
                                                                                          .value());
            return UserWithTasksResult.unavailable(user);
        }
    }
```
Update the `getUserById` and `enrichUserWithTasks` Javadoc: on tasks-service unavailability the result contains the user with `tasksAvailable=false` and null task IDs (degraded), instead of "an empty task list".

- [ ] **Step 4: Update `ReadUserUseCase` Javadoc**

In `getUserById`'s Javadoc, replace "the result will contain the user with an empty task list, allowing graceful degradation." with "the result is returned with the tasks marked unavailable (no task identifiers), allowing graceful degradation."

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=ReadUserServiceTests`
Expected: PASS (all `GetUserById` tests including the new degraded case).

- [ ] **Step 6: Format + commit**

```bash
mvn -q spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/ReadUserUseCase.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/service/ReadUserServiceTests.java
git commit -m "feat(users): degrade user read when tasks-service is unavailable

ReadUserService now catches TasksUnavailableException and returns a degraded UserWithTasksResult (tasks marked unavailable) so the user lookup still succeeds. The degrade-vs-fail policy now lives in the application service, per the ports-and-adapters rule.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Surface `warnings` in the response contract (validated end-to-end)

Add the `warnings` field + warning code + mapper derivation, and validate the full contract through `UserE2EIT` (real mapper + gateway + Jackson + breaker).

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUserByIdResponse.java`
- Create: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/WarningCodes.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/UserMapper.java`
- Test: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java`

**Interfaces:**
- Consumes: `UserWithTasksResult` w/ `tasksAvailable` (T1), degrade behavior (T2+T3).
- Produces:
  - `GetUserByIdResponse(UUID userId, String firstName, String lastName, String email, String phoneNumber, List<UUID> taskIds, List<String> warnings)` — `warnings` annotated `@JsonInclude(NON_EMPTY)`; `taskIds` null when degraded.
  - `WarningCodes.TASKS_UNAVAILABLE == "tasks_unavailable"` (package-private).
  - `UserMapper.toGetUserByIdResponse` maps `tasksAvailable` → `warnings` (`[]` when available, `["tasks_unavailable"]` when not).

- [ ] **Step 1: Update the E2E tests (the failing tests)** in `UserE2EIT.java`

(a) `_UserExistsWithoutTasks` (≈line 143): add the `warnings` arg (`null`, since NON_EMPTY omits the empty list and the client deserializes the absent key to `null`):
```java
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), Collections.emptyList(), null);
```
Also change its log negative-assertion to the new message:
```java
            assertThat(output.getAll()).doesNotContain("Tasks Service unavailable for user");
```

(b) `_UserExistsWithTasks` (≈line 179): add `warnings` arg `null`:
```java
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), taskIds, null);
```

(c) Rewrite the degraded test (≈line 203) — rename, expect `taskIds: null` + `warnings: ["tasks_unavailable"]`, and assert the new log message:
```java
        @Test
        void ReturnsStatusOKAndBodyWithTasksUnavailableWarning_UserExistsAndTasksServiceFails(CapturedOutput output) {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), null, List.of("tasks_unavailable"));

            mockRequestToGetTasksByUserIdWithServerErrorResponse(userId);

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(GetUserByIdResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEqualTo(response);

            // Assert the degradation is logged for operators
            assertThat(output.getAll()).contains("Tasks Service unavailable for user " + createdUser.id());
        }
```
Update the class Javadoc coverage line "task enrichment via external gateway (graceful degradation on failure)" → "task enrichment via external gateway (partial-success degradation surfacing a tasks_unavailable warning on failure)".

- [ ] **Step 2: Run the E2E to verify it fails (ASK DEVELOPER FIRST — slow)**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=UserE2EIT`
Expected: FAIL — `GetUserByIdResponse` 7-arg constructor does not exist yet (compile error).

- [ ] **Step 3: Add the `warnings` field to `GetUserByIdResponse`**

Add import `import com.fasterxml.jackson.annotation.JsonInclude;`. Replace the record components with:
```java
public record GetUserByIdResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<UUID> taskIds,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> warnings
) {}
```
Update the record Javadoc: add `@param taskIds the user's task identifiers, or {@code null} when tasks-service is unavailable` and `@param warnings machine-readable degradation codes (e.g. {@code tasks_unavailable}); omitted when empty`.

- [ ] **Step 4: Create `WarningCodes`**

`infrastructure/user/mapper/WarningCodes.java` (with the standard header):
```java
package com.bcn.asapp.users.infrastructure.user.mapper;

/**
 * Centralizes machine-readable warning codes surfaced in successful (degraded) responses.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class WarningCodes {

    static final String TASKS_UNAVAILABLE = "tasks_unavailable";

    private WarningCodes() {}

}
```

- [ ] **Step 5: Derive `warnings` in `UserMapper`**

Add import `import java.util.List;`. Add `@Mapping(target = "warnings", source = "tasksAvailable")` after the existing `@Mapping(target = "taskIds", source = "taskIds")` on `toGetUserByIdResponse`, and add the default helper at the end of the interface:
```java
    /**
     * Derives the response warning codes from the task-availability flag.
     *
     * @param tasksAvailable whether tasks were successfully retrieved
     * @return an empty list when available, or a single {@code tasks_unavailable} code when not
     */
    default List<String> toWarnings(boolean tasksAvailable) {
        return tasksAvailable ? List.of() : List.of(WarningCodes.TASKS_UNAVAILABLE);
    }
```

- [ ] **Step 6: Run the E2E to verify it passes (ASK DEVELOPER FIRST — slow)**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=UserE2EIT`
Expected: PASS — happy paths return `taskIds` populated and no `warnings`; the degraded path returns `taskIds: null` + `warnings: ["tasks_unavailable"]` and logs the outage.

- [ ] **Step 7: Format + commit**

```bash
mvn -q spotless:apply
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUserByIdResponse.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/WarningCodes.java \
        services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/UserMapper.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java
git commit -m "feat(users): surface a tasks_unavailable warning on degraded user reads

Add an optional warnings array to GetUserByIdResponse (omitted on the happy path) and a null taskIds when tasks-service is unavailable, mapped from the result's tasksAvailable flag via a package-private WarningCodes holder. Validated end-to-end in UserE2EIT.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Document the contract in REST Docs

Update the Spring REST Docs test (response field for `warnings`, plus a degraded snippet) and the API guide.

**Files:**
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java`
- Modify: `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc`

**Interfaces:**
- Consumes: `GetUserByIdResponse` 7-arg (T4), `UserWithTasksResult.unavailable` (T1).

- [ ] **Step 1: Update the happy-path doc test + add the degraded doc test** in `UserRestControllerDocumentationIT.java`

In `DocumentsGetUserById_UserFound`: change the response construction to 7-arg and document `warnings` as optional. Replace the `response` line and the `responseFields(...)` block:
```java
            var response = new GetUserByIdResponse(userIdValue, firstNameValue, lastNameValue, emailValue, phoneNumberValue, taskIds, List.of());
```
```java
                           responseFields(
                                   fieldWithPath("userId").description("The user's unique identifier"),
                                   fieldWithPath("firstName").description("The user's first name"),
                                   fieldWithPath("lastName").description("The user's last name"),
                                   fieldWithPath("email").description("The user's email address"),
                                   fieldWithPath("phoneNumber").description("The user's phone number"),
                                   fieldWithPath("taskIds").description("The identifiers of tasks associated with the user, or null when tasks-service is unavailable"),
                                   fieldWithPath("warnings").description("Machine-readable degradation codes (e.g. tasks_unavailable); omitted when none").optional()
                           )
```

Add a degraded doc test inside `@Nested class GetUserById`:
```java
        @Test
        void DocumentsGetUserById_TasksUnavailable() throws Exception {
            // Given
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var response = new GetUserByIdResponse(userIdValue, user.getFirstName()
                                                                    .value(),
                    user.getLastName()
                        .value(),
                    user.getEmail()
                        .value(),
                    user.getPhoneNumber()
                        .value(),
                    null, List.of("tasks_unavailable"));

            given(readUserUseCase.getUserById(any(UUID.class))).willReturn(Optional.of(UserWithTasksResult.unavailable(user)));
            given(userMapper.toGetUserByIdResponse(any(UserWithTasksResult.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON)
                                                                       .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-user-by-id-tasks-unavailable",
                           relaxedResponseFields(
                                   fieldWithPath("taskIds").description("Null because tasks-service is unavailable"),
                                   fieldWithPath("warnings").description("Machine-readable degradation codes; contains tasks_unavailable")
                           )
                       )
                       // @formatter:on
                   );
        }
```

- [ ] **Step 2: Update `api-guide.adoc`**

Replace the "Get User by ID" prose (lines 76–77) and add the degraded snippet:
```asciidoc
=== Get User by ID

Retrieves detailed information about a specific user by their unique identifier, including a list of associated task identifiers.
If tasks-service is unavailable, the user is still returned (HTTP 200) with `taskIds` set to `null` and a `warnings` array containing `tasks_unavailable`.

operation::get-user-by-id[snippets='path-parameters,http-request,response-fields,curl-request,http-response']

When tasks-service is unavailable the response is degraded as follows:

operation::get-user-by-id-tasks-unavailable[snippets='http-response']
```

- [ ] **Step 3: Run the doc IT (ASK DEVELOPER FIRST — slow)**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=UserRestControllerDocumentationIT`
Expected: PASS — both `get-user-by-id` and `get-user-by-id-tasks-unavailable` snippets generate; the `warnings` field documents cleanly (optional on the happy path).

- [ ] **Step 4: Format + commit**

```bash
mvn -q spotless:apply
git add services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java \
        services/asapp-users-service/src/docs/asciidoc/api-guide.adoc
git commit -m "docs(users): document the partial-success user-with-tasks contract

Document the optional warnings field and add a degraded-response snippet for GET /users/{id} when tasks-service is unavailable.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Narrative docs, TODO, and prepare gated rules

Final docs sweep, full verification, and prepare the gated `.claude/rules` text for the developer.

**Files:**
- Modify: `services/asapp-users-service/README.md`
- Modify: `TODO.md`
- (Prepared text only — NOT edited by the agent) `.claude/rules/rest.md`, `.claude/rules/ports-adapters.md`

- [ ] **Step 1: Update the README Resilience section**

In `services/asapp-users-service/README.md`:
- Line ~173: change "degrades to an empty task list instead of failing the user request." → "degrades to a partial response — the user is returned with `taskIds: null` and a `warnings: [\"tasks_unavailable\"]` entry — instead of failing the user request."
- Line ~185: change "the gateway degrades to an empty result, so the request still succeeds." → "the application service degrades the read — the user is returned with `taskIds: null` and a `tasks_unavailable` warning — so the request still succeeds."

- [ ] **Step 2: Tick the TODO item**

In `TODO.md`, change line 33 `* [ ] Review the user-with-tasks degradation behaviour when tasks-service is unavailable` to `* [X]`, and remove its two now-resolved decision sub-bullets (the "Decide what GET user/{id} should do…" and "Decide where the degrade policy belongs…" lines).

- [ ] **Step 3: Mark the spec implemented**

In `docs/superpowers/specs/2026-06-21-user-with-tasks-degradation-design.md`, change `**Status:** Draft` to `**Status:** Implemented`.

- [ ] **Step 4: Full verification (ASK DEVELOPER FIRST — runs slow ITs)**

Run: `mvn -q -pl services/asapp-users-service clean verify`
Expected: BUILD SUCCESS — all unit tests + ITs + E2E green; Spotless + REST Docs snippets generated.

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service/README.md TODO.md docs/superpowers/specs/2026-06-21-user-with-tasks-degradation-design.md
git commit -m "docs(users): update README/TODO for tasks degradation contract

Update the users-service Resilience section to describe the partial-success degradation, tick the TODO item, and mark the design spec implemented.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 6: Hand the gated `.claude/rules` text to the developer**

`.claude/` edits are blocked by the auto-mode permission gate — do **not** edit these files. Present the two additions for the developer to apply:

**Add to `.claude/rules/rest.md`** (new section after "Error Response Format"):
```markdown
## Partial Success / Degraded Responses

- When an endpoint enriches its primary resource with a **non-critical** secondary source and that source is unavailable, return `200` with the primary data and signal the degradation — never silently return an empty/default value, never fail the whole request for a soft dependency
- Signal via an optional `warnings` array of machine-readable string codes (e.g. `tasks_unavailable`); omit the field on the happy path (`@JsonInclude(NON_EMPTY)`)
- The unavailable field is `null` (not `[]`/`0`) so it stays distinguishable from a genuine empty value
- Warning codes name the missing **data**, never internal services/topology — consistent with the error handler's no-internal-disclosure stance
- The degrade-vs-fail decision (soft vs hard dependency) is an application-service policy, not an adapter concern — see `ports-adapters.md`
```

**Add to `.claude/rules/ports-adapters.md`** (under "Exception Handling"):
```markdown
- Resilience policy split: the adapter owns the *mechanism* (circuit breaker, retry, timeout) and translates a downstream outage into a typed gateway exception (e.g. `TasksUnavailableException`); the *degrade-vs-fail policy* belongs in the application service that catches it.
```

---

## Self-Review

**1. Spec coverage:**
- §2 decision 1 (partial-success 200 + warnings) → Tasks 4 (impl), 5 (docs). ✔
- §2 decision 2 (mechanism in adapter / policy in service via `TasksUnavailableException`) → Tasks 2 + 3. ✔
- §2 decision 3 (`tasksAvailable` flag + factories) → Task 1. ✔
- §3.1 exception → Task 2. §3.2 adapter → Task 2. §3.3 port Javadoc → Task 2. §3.4 service → Task 3. §3.5 result → Task 1. §3.6 response DTO → Task 4. §3.7 mapper + `WarningCodes` → Task 4. §3.8 no handler change → honored (no `GlobalExceptionHandler` task; service catches). ✔
- §4 testing: result unit (T1), adapter ITs (T2), service unit (T3), E2E contract (T4), REST Docs (T5). ✔
- §5 docs: api-guide (T5), README + Javadoc (T2/T3/T4 inline + T6), TODO (T6), gated rules (T6). ✔

**2. Placeholder scan:** No TBD/TODO/"add error handling"/"similar to". Every code step shows complete code. ✔

**3. Type consistency:** `tasksAvailable()`, `available(User, List<UUID>)`, `unavailable(User)`, `TasksUnavailableException(String, Throwable)`, `toWarnings(boolean)→List<String>`, `WarningCodes.TASKS_UNAVAILABLE`, `GetUserByIdResponse(...,List<UUID> taskIds, List<String> warnings)` are used consistently across Tasks 1–5. ✔

**Deliberate refinement vs spec §3.4:** the spec showed both the adapter *and* the service logging a WARN. This plan logs only in the adapter (the technical cause) and keeps the service catch silent, to avoid double-logging one event; the degraded state is also observable in the response. The `UserE2EIT` degraded test asserts the adapter's message.
