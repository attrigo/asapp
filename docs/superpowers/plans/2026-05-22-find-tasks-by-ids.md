# Find Tasks by IDs — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `GET /api/tasks?ids=u1,u2,...` batch read operation on `asapp-tasks-service` that returns tasks whose IDs are in the provided list, with server-side dedup and a 1–50 size cap.

**Architecture:** Hexagonal slice that mirrors the existing `getTasksByUserId` flow end-to-end. A new use case method delegates to a new outbound port method, implemented by an adapter that calls Spring Data's inherited `ListCrudRepository.findAllById`. A new query-string filter on the existing `/api/tasks` collection is routed via Spring `@GetMapping(params = ...)` alongside the existing `getAllTasks`.

**Tech Stack:** Spring Boot 4.0 · Spring MVC · Spring Data JDBC · Spring Security · MapStruct · Spring REST Docs · JUnit 5 · Mockito (BDD) · AssertJ · Testcontainers (PostgreSQL).

**Source spec:** `docs/superpowers/specs/2026-05-22-find-tasks-by-ids-design.md`

**Deviation from spec:** §7.2 placed happy-path tests in `TaskRestControllerIT`. This plan moves them to `TaskRestControllerDocumentationIT` and `TaskE2EIT`, matching the existing project pattern where `*RestControllerIT` is validation/error-only.

**Agent ownership convention:** Each task is tagged with a `**Agent:**` line. When executing under `subagent-driven-development`, dispatch a fresh subagent of that type for the task. Plain tasks (no domain expertise needed, e.g., URL constants) can be handled by `spring-boot-developer`.

---

## File Inventory

**Modified:**
- `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/ReadTaskUseCase.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskService.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/out/TaskRepository.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/out/TaskRepositoryAdapter.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/mapper/TaskMapper.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestAPI.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestController.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskServiceTests.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/TaskE2EIT.java`
- `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc`

**Created:**
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/response/GetTasksByIdsResponse.java`

---

## Task 1: URL constant

**Agent:** `spring-boot-developer`

**Files:**
- Modify: `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java`

- [ ] **Step 1: Add the new query-parameter constant**

Insert after `TASKS_DELETE_BY_ID_PATH` (and before `TASKS_GET_BY_ID_FULL_PATH`):

```java
    public static final String TASKS_GET_BY_IDS_PARAM = "ids";
```

- [ ] **Step 2: Run the libs build to confirm it compiles**

```
mvn -pl libs/asapp-commons-url -am compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```
git add libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java
git commit -m "feat(commons): add ids query-parameter constant for tasks"
```

---

## Task 2: Repository port + adapter

**Agent:** `spring-boot-developer`

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/out/TaskRepository.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/out/TaskRepositoryAdapter.java`

- [ ] **Step 1: Add `findByIds` to the repository port**

Insert into `TaskRepository.java` after the existing `findByUserId` method:

```java
    /**
     * Finds tasks by their unique identifiers.
     *
     * @param taskIds the collection of task identifiers
     * @return a {@link Collection} of {@link Task} entities found; missing identifiers are silently omitted
     */
    Collection<Task> findByIds(Collection<TaskId> taskIds);
```

- [ ] **Step 2: Implement `findByIds` on the adapter**

Insert into `TaskRepositoryAdapter.java` after the existing `findByUserId` method:

```java
    /**
     * Finds tasks by their unique identifiers.
     *
     * @param taskIds the collection of task identifiers
     * @return a {@link Collection} of {@link Task} entities found; missing identifiers are silently omitted
     */
    @Override
    public Collection<Task> findByIds(Collection<TaskId> taskIds) {
        var ids = taskIds.stream()
                         .map(TaskId::value)
                         .toList();

        return taskRepository.findAllById(ids)
                             .stream()
                             .map(taskMapper::toTask)
                             .toList();
    }
```

- [ ] **Step 3: Run the tasks-service compile to confirm interface is satisfied**

```
mvn -pl services/asapp-tasks-service -am compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit (deferred — bundle with Task 3)**

This task's changes only compile because no caller exercises them yet. Defer the commit until the use-case + service add their callers in Task 3, so the commit is a self-contained slice.

---

## Task 3: Use case + service with unit tests (TDD)

**Agent:** `test-automator` for Steps 1-2 (write tests), `spring-boot-developer` for Steps 3-5 (implement & green)

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskServiceTests.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/ReadTaskUseCase.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskService.java`

- [ ] **Step 1: Write failing service unit tests**

Add a new `@Nested class GetTasksByIds` to `ReadTaskServiceTests.java`, after the existing `GetTasksByUserId` nested class. Add this import block to the imports section:

```java
import static org.mockito.ArgumentMatchers.anyCollection;

import java.util.Collection;
import java.util.Set;
import org.mockito.ArgumentCaptor;
```

```java
    @Nested
    class GetTasksByIds {

        @Test
        void ReturnsTasks_AllIdsExist() {
            // Given
            var firstTaskIdValue = UUID.fromString("11111111-1111-4111-8111-111111111111");
            var secondTaskIdValue = UUID.fromString("22222222-2222-4222-8222-222222222222");
            var firstTask = aTaskBuilder().withTaskId(firstTaskIdValue).build();
            var secondTask = aTaskBuilder().withTaskId(secondTaskIdValue).build();
            var requestedIds = List.of(firstTaskIdValue, secondTaskIdValue);

            given(taskRepository.findByIds(Set.of(TaskId.of(firstTaskIdValue), TaskId.of(secondTaskIdValue))))
                    .willReturn(List.of(firstTask, secondTask));

            // When
            var actual = readTaskService.getTasksByIds(requestedIds);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(firstTask, secondTask);

            then(taskRepository).should(times(1))
                                .findByIds(Set.of(TaskId.of(firstTaskIdValue), TaskId.of(secondTaskIdValue)));
        }

        @Test
        void ReturnsFoundTasks_SomeIdsExist() {
            // Given
            var existingIdValue = UUID.fromString("11111111-1111-4111-8111-111111111111");
            var missingIdValue = UUID.fromString("22222222-2222-4222-8222-222222222222");
            var existingTask = aTaskBuilder().withTaskId(existingIdValue).build();
            var requestedIds = List.of(existingIdValue, missingIdValue);

            given(taskRepository.findByIds(Set.of(TaskId.of(existingIdValue), TaskId.of(missingIdValue))))
                    .willReturn(List.of(existingTask));

            // When
            var actual = readTaskService.getTasksByIds(requestedIds);

            // Then
            assertThat(actual).containsExactly(existingTask);

            then(taskRepository).should(times(1))
                                .findByIds(Set.of(TaskId.of(existingIdValue), TaskId.of(missingIdValue)));
        }

        @Test
        void ReturnsEmptyList_NoIdsExist() {
            // Given
            var firstIdValue = UUID.fromString("11111111-1111-4111-8111-111111111111");
            var secondIdValue = UUID.fromString("22222222-2222-4222-8222-222222222222");
            var requestedIds = List.of(firstIdValue, secondIdValue);

            given(taskRepository.findByIds(Set.of(TaskId.of(firstIdValue), TaskId.of(secondIdValue))))
                    .willReturn(List.of());

            // When
            var actual = readTaskService.getTasksByIds(requestedIds);

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findByIds(Set.of(TaskId.of(firstIdValue), TaskId.of(secondIdValue)));
        }

        @Test
        void DedupesIds_DuplicatesInInput() {
            // Given
            var idValue = UUID.fromString("11111111-1111-4111-8111-111111111111");
            var task = aTaskBuilder().withTaskId(idValue).build();
            var requestedIds = List.of(idValue, idValue, idValue);

            given(taskRepository.findByIds(anyCollection())).willReturn(List.of(task));

            var captor = ArgumentCaptor.forClass(Collection.class);

            // When
            var actual = readTaskService.getTasksByIds(requestedIds);

            // Then
            assertThat(actual).containsExactly(task);

            then(taskRepository).should(times(1))
                                .findByIds(captor.capture());
            assertThat(captor.getValue()).containsExactly(TaskId.of(idValue));
        }

        @Test
        void ThrowsException_NullIdInList() {
            // Given
            var validIdValue = UUID.fromString("11111111-1111-4111-8111-111111111111");
            var requestedIds = new java.util.ArrayList<UUID>();
            requestedIds.add(validIdValue);
            requestedIds.add(null);

            // When
            var actual = catchThrowable(() -> readTaskService.getTasksByIds(requestedIds));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class);
        }

    }
```

- [ ] **Step 2: Run unit tests, expect compile failure**

```
mvn -pl services/asapp-tasks-service test -Dtest=ReadTaskServiceTests
```

Expected: `BUILD FAILURE` — compile error: `cannot find symbol method getTasksByIds(List<UUID>)` on `ReadTaskService` (or `ReadTaskUseCase`).

- [ ] **Step 3: Add `getTasksByIds` to the use case interface**

Insert into `ReadTaskUseCase.java` after the existing `getTasksByUserId` method:

```java
    /**
     * Retrieves tasks by their unique identifiers.
     *
     * @param ids the list of task identifiers; duplicates are deduped
     * @return a {@link List} of {@link Task} entities found; missing ids are silently omitted
     * @throws IllegalArgumentException if any id is invalid
     */
    List<Task> getTasksByIds(List<UUID> ids);
```

- [ ] **Step 4: Implement the use case in `ReadTaskService`**

Add the following imports to `ReadTaskService.java`:

```java
import java.util.stream.Collectors;
```

Insert after the existing `getTasksByUserId` method:

```java
    /**
     * Retrieves tasks by their unique identifiers.
     *
     * @param ids the list of task identifiers; duplicates are deduped
     * @return a {@link List} of {@link Task} entities found; missing ids are silently omitted
     * @throws IllegalArgumentException if any id is invalid
     */
    @Override
    public List<Task> getTasksByIds(List<UUID> ids) {
        var taskIds = ids.stream()
                         .map(TaskId::of)
                         .collect(Collectors.toUnmodifiableSet());

        return taskRepository.findByIds(taskIds)
                             .stream()
                             .toList();
    }
```

- [ ] **Step 5: Run unit tests, expect all green**

```
mvn -pl services/asapp-tasks-service test -Dtest=ReadTaskServiceTests
```

Expected: `BUILD SUCCESS`, all `GetTasksByIds` tests pass.

- [ ] **Step 6: Commit (bundled with Task 2)**

```
git add libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/out services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/application
git commit -m "feat(tasks): add application slice for find tasks by ids

- Add findByIds outbound port and JDBC adapter using findAllById
- Add getTasksByIds use case with server-side dedup via Set
- Add ids query-parameter constant in commons-url
- Add unit tests covering happy path, partial misses, dedupe, empty, null"
```

---

## Task 4: Response DTO + mapper

**Agent:** `spring-boot-developer`

**Files:**
- Create: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/response/GetTasksByIdsResponse.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/mapper/TaskMapper.java`

- [ ] **Step 1: Create the response record**

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

package com.bcn.asapp.tasks.infrastructure.task.in.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for retrieving tasks by a list of unique identifiers.
 *
 * @param taskId      the task's unique identifier
 * @param userId      the task's user unique identifier
 * @param title       the task's title
 * @param description the task's description
 * @param startDate   the task's start date
 * @param endDate     the task's end date
 * @since 0.4.0
 * @author attrigo
 */
public record GetTasksByIdsResponse(
        @JsonProperty("task_id") UUID taskId,
        @JsonProperty("user_id") UUID userId,
        String title,
        String description,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate
) {}
```

- [ ] **Step 2: Add the mapper method**

Add the `GetTasksByIdsResponse` import to `TaskMapper.java`:

```java
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByIdsResponse;
```

Insert into `TaskMapper.java` after `toGetTasksByUserIdResponse`:

```java
    /**
     * Maps a domain {@link Task} to a {@link GetTasksByIdsResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link GetTasksByIdsResponse}
     */
    @Mapping(target = "taskId", source = "id")
    GetTasksByIdsResponse toGetTasksByIdsResponse(Task task);
```

- [ ] **Step 3: Compile to verify mapper generation**

```
mvn -pl services/asapp-tasks-service compile
```

Expected: `BUILD SUCCESS`. MapStruct generates `TaskMapperImpl.toGetTasksByIdsResponse` in `target/generated-sources`.

- [ ] **Step 4: Commit**

```
git add services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/response/GetTasksByIdsResponse.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/mapper/TaskMapper.java
git commit -m "feat(tasks): add GetTasksByIdsResponse DTO and mapper method"
```

---

## Task 5: REST API + controller method (compile-green, no tests yet)

**Agent:** `api-designer` for §1 (interface annotations), `spring-boot-developer` for §2 (controller impl)

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestAPI.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestController.java`

- [ ] **Step 1: Add `params = "!ids"` to the existing `getAllTasks` mapping**

Locate the existing method in `TaskRestAPI.java`:

```java
    @GetMapping(value = TASKS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets all tasks", ...)
    ...
    List<GetAllTasksResponse> getAllTasks();
```

Change the `@GetMapping` annotation to include `params = "!ids"`:

```java
    @GetMapping(value = TASKS_GET_ALL_PATH, params = "!ids", produces = "application/json")
```

- [ ] **Step 2: Add the new method to `TaskRestAPI`**

Add the static imports at the top of `TaskRestAPI.java`:

```java
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_IDS_PARAM;
```

Add the validation imports:

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
```

Add the response-DTO import:

```java
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByIdsResponse;
```

Add a `RequestParam` import:

```java
import org.springframework.web.bind.annotation.RequestParam;
```

Insert the new method after `getAllTasks`:

```java
    /**
     * Gets tasks by their unique identifiers.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Tasks retrieved successfully (may be empty if no ids match).</li>
     * <li>400-BAD_REQUEST: ids is empty, exceeds 50 elements, or contains a malformed UUID.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param ids the list of task identifiers (1 to 50 elements)
     * @return a {@link List} of {@link GetTasksByIdsResponse} containing the tasks found; missing identifiers are silently omitted
     */
    @GetMapping(value = TASKS_GET_ALL_PATH, params = "ids", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets tasks by their unique identifiers", description = "Retrieves a list of tasks whose identifiers are in the provided list. This endpoint requires authentication. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The list must contain between 1 and 50 identifiers.")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = {
            @Content(schema = @Schema(implementation = GetTasksByIdsResponse.class)) })
    @ApiResponse(responseCode = "400", description = "ids is empty, exceeds 50 elements, or contains a malformed UUID", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetTasksByIdsResponse> getTasksByIds(
            @RequestParam(TASKS_GET_BY_IDS_PARAM) @NotEmpty(message = "ids must not be empty") @Size(max = 50, message = "ids must contain at most 50 elements") @Parameter(description = "List of task identifiers (1 to 50 elements, comma-separated)") List<UUID> ids);
```

- [ ] **Step 3: Add `@Validated` to `TaskRestController`**

Open `TaskRestController.java`. Add the import:

```java
import org.springframework.validation.annotation.Validated;
```

Add the annotation above the class declaration (in annotation-order group "Component role"):

```java
@RestController
@Validated
public class TaskRestController implements TaskRestAPI {
```

- [ ] **Step 4: Implement `getTasksByIds` in `TaskRestController`**

Add the `GetTasksByIdsResponse` import:

```java
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByIdsResponse;
```

Insert after the existing `getAllTasks` method:

```java
    /**
     * Gets tasks by their unique identifiers.
     *
     * @param ids the list of task identifiers
     * @return a {@link List} of {@link GetTasksByIdsResponse} containing the tasks found; missing identifiers are silently omitted
     */
    @Override
    public List<GetTasksByIdsResponse> getTasksByIds(List<UUID> ids) {
        return readTaskUseCase.getTasksByIds(ids)
                              .stream()
                              .map(taskMapper::toGetTasksByIdsResponse)
                              .toList();
    }
```

- [ ] **Step 5: Compile**

```
mvn -pl services/asapp-tasks-service compile
```

Expected: `BUILD SUCCESS`. (No tests yet for this layer — Task 6 adds them.)

- [ ] **Step 6: Commit (deferred — bundle with Task 6)**

The controller wiring lands together with its WebMvc tests so the commit ships a tested slice.

---

## Task 6: Validation WebMvc tests (TDD)

**Agent:** `test-automator`

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java`

- [ ] **Step 1: Write failing WebMvc validation tests**

Add the static imports (only the ones not already present) to `TaskRestControllerIT.java`:

```java
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.stream.IntStream;
```

Add a new `@Nested class GetTasksByIds` after the existing `GetTasksByUserId` nested class:

```java
    @Nested
    class GetTasksByIds {

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyIds() {
            // Given
            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH + "?ids=");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_TooManyIds() {
            // Given
            var fiftyOneIds = IntStream.range(0, 51)
                                       .mapToObj(i -> UUID.randomUUID().toString())
                                       .reduce((a, b) -> a + "," + b)
                                       .orElseThrow();
            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH + "?ids=" + fiftyOneIds);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MalformedUuid() {
            // Given
            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH + "?ids=not-a-uuid");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400));
        }

        @Test
        void RoutesToGetAllTasks_IdsAbsent() {
            // Given
            given(readTaskUseCase.getAllTasks()).willReturn(List.of());

            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatusOk()
                         .hasContentType(MediaType.APPLICATION_JSON);

            then(readTaskUseCase).should(times(1)).getAllTasks();
            then(readTaskUseCase).should(never()).getTasksByIds(anyList());
        }

    }
```

- [ ] **Step 2: Run tests, expect green (because Task 5 already shipped the production code)**

```
mvn -pl services/asapp-tasks-service test -Dtest=TaskRestControllerIT
```

Expected: `BUILD SUCCESS`, all four new tests in `GetTasksByIds` pass.

If `RoutesToGetAllTasks_IdsAbsent` fails because `getAllTasks` returns `null` (mock not stubbed for this method elsewhere), check the test class — it should mock `readTaskUseCase.getAllTasks()` per the `given` line above.

- [ ] **Step 3: Commit (bundles Task 5 + Task 6)**

```
git add services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestAPI.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestController.java services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java
git commit -m "feat(tasks): expose GET /api/tasks?ids= batch find endpoint

- Add getTasksByIds method to TaskRestAPI with OpenAPI annotations
- Add params=\"!ids\" to getAllTasks to disambiguate the route
- Add @Validated on TaskRestController to enable param-level validation
- Add WebMvc tests covering empty, too-many, malformed UUID, and routing regression"
```

---

## Task 7: RestDocs documentation test

**Agent:** `documentation-engineer`

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java`

- [ ] **Step 1: Add the documentation test**

Add static imports (only those not already present):

```java
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
```

Add the `GetTasksByIdsResponse` import:

```java
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByIdsResponse;
```

Add a new `@Nested class GetTasksByIds` after the existing `GetTasksByUserId` nested class:

```java
    @Nested
    class GetTasksByIds {

        @Test
        void DocumentsGetTasksByIds() throws Exception {
            // Given
            var task = aTask();
            var taskIdValue = task.getId().value();
            var taskUserIdValue = task.getUserId().value();
            var taskTitleValue = task.getTitle().value();
            var taskDescriptionValue = task.getDescription().value();
            var taskStartDateValue = task.getStartDate().value();
            var taskEndDateValue = task.getEndDate().value();
            var response = new GetTasksByIdsResponse(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);

            given(readTaskUseCase.getTasksByIds(anyList())).willReturn(List.of(task));
            given(taskMapper.toGetTasksByIdsResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(TASKS_GET_ALL_FULL_PATH).param("ids", taskIdValue.toString())
                                                       .accept(APPLICATION_JSON)
                                                       .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-tasks-by-ids",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           queryParameters(parameterWithName("ids").description("Comma-separated list of task identifiers (1 to 50 elements)")),
                           relaxedResponseFields(
                                   fieldWithPath("[].task_id").description("The task's unique identifier"),
                                   fieldWithPath("[].user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("[].title").description("The task's title"),
                                   fieldWithPath("[].description").description("The task's description"),
                                   fieldWithPath("[].start_date").description("The task's start date in ISO 8601 format"),
                                   fieldWithPath("[].end_date").description("The task's end date in ISO 8601 format"))
                       )
                       // @formatter:on
                   );
        }

    }
```

If the existing tests in this class use `anyList()` from a different import (check `org.mockito.ArgumentMatchers.anyList`), reuse the same import. Otherwise add:

```java
import static org.mockito.ArgumentMatchers.anyList;
```

- [ ] **Step 2: Run the documentation test**

```
mvn -pl services/asapp-tasks-service test -Dtest=TaskRestControllerDocumentationIT
```

Expected: `BUILD SUCCESS`. The build generates `target/generated-snippets/get-tasks-by-ids/` with `http-request.adoc`, `http-response.adoc`, `request-headers.adoc`, `request-parameters.adoc`, `curl-request.adoc`, and `response-fields.adoc`.

- [ ] **Step 3: Commit**

```
git add services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java
git commit -m "test(tasks): document GET /api/tasks?ids= endpoint with RestDocs"
```

---

## Task 8: End-to-end integration test

**Agent:** `test-automator`

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/TaskE2EIT.java`

- [ ] **Step 1: Add the E2E test**

Add static imports (only those not already present):

```java
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
```

Add the `GetTasksByIdsResponse` import:

```java
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByIdsResponse;
```

Add a new `@Nested class GetTasksByIds` after the existing `GetTasksByUserId` nested class:

```java
    @Nested
    class GetTasksByIds {

        @Test
        void ReturnsStatusOKAndBodyWithFoundTasks_AllIdsExist() {
            // Given
            var firstTask = createTask();
            var secondTask = createTask();
            var firstExpected = new GetTasksByIdsResponse(firstTask.id(), firstTask.userId(), firstTask.title(), firstTask.description(),
                    firstTask.startDate(), firstTask.endDate());
            var secondExpected = new GetTasksByIdsResponse(secondTask.id(), secondTask.userId(), secondTask.title(), secondTask.description(),
                    secondTask.startDate(), secondTask.endDate());

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(TASKS_GET_ALL_FULL_PATH)
                                                                    .queryParam("ids", firstTask.id() + "," + secondTask.id())
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetTasksByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).containsExactlyInAnyOrder(firstExpected, secondExpected);
        }

        @Test
        void ReturnsStatusOKAndBodyWithFoundTasks_SomeIdsExist() {
            // Given
            var existingTask = createTask();
            var missingId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
            var expected = new GetTasksByIdsResponse(existingTask.id(), existingTask.userId(), existingTask.title(), existingTask.description(),
                    existingTask.startDate(), existingTask.endDate());

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(TASKS_GET_ALL_FULL_PATH)
                                                                    .queryParam("ids", existingTask.id() + "," + missingId)
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectBody(new ParameterizedTypeReference<List<GetTasksByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).containsExactly(expected);
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var someId = UUID.fromString("11111111-1111-4111-8111-111111111111");

            // When & Then
            restTestClient.get()
                          .uri(uriBuilder -> uriBuilder.path(TASKS_GET_ALL_FULL_PATH)
                                                       .queryParam("ids", someId.toString())
                                                       .build())
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized();
        }

    }
```

If a `createTask()` helper does not already exist in `TaskE2EIT`, locate the existing pattern used by `GetTasksByUserId` (or the create-flow tests) and reuse it — the file already persists tasks via `taskRepository.save(aJdbcTask())` or by calling the create endpoint; whichever pattern matches the surrounding tests is the right one. Do not invent a new helper.

- [ ] **Step 2: Run the E2E test**

```
mvn -pl services/asapp-tasks-service verify -Dit.test=TaskE2EIT
```

Expected: `BUILD SUCCESS`. Testcontainers spins up PostgreSQL + Redis; the new `GetTasksByIds` nested tests pass.

- [ ] **Step 3: Commit**

```
git add services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/TaskE2EIT.java
git commit -m "test(tasks): add E2E coverage for GET /api/tasks?ids= endpoint"
```

---

## Task 9: AsciiDoc API guide

**Agent:** `documentation-engineer`

**Files:**
- Modify: `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc`

- [ ] **Step 1: Add a new resource section**

Open `api-guide.adoc`. Insert this section between `[[resources-tasks-get-all]]` and `[[resources-tasks-create]]`:

```adoc
[[resources-tasks-get-by-ids]]
=== Get Tasks by IDs

Retrieves a list of tasks whose identifiers are in the provided `ids` query parameter.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated. The list must contain between 1 and 50 identifiers.

operation::get-tasks-by-ids[snippets='request-parameters,http-request,response-fields,curl-request,http-response']

```

- [ ] **Step 2: Run the doc build to confirm AsciiDoctor consumes the new snippets**

```
mvn -pl services/asapp-tasks-service verify
```

The full verify run regenerates snippets (from Task 7's IT) and builds the AsciiDoc. Expected: `BUILD SUCCESS`. If AsciiDoc emits a warning about missing snippets, re-run `mvn -pl services/asapp-tasks-service verify -Dit.test=TaskRestControllerDocumentationIT` first to populate `target/generated-snippets/get-tasks-by-ids/`.

- [ ] **Step 3: Commit**

```
git add services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc
git commit -m "docs(tasks): document GET /api/tasks?ids= in api-guide"
```

---

## Task 10: Full verify + TODO housekeeping

**Agent:** `architect-reviewer` (Steps 1–2 final diff review), then plain commit for housekeeping

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Full project verify**

```
mvn clean verify
```

Expected: `BUILD SUCCESS` across all modules. This is the gate before opening the PR.

- [ ] **Step 2: Dispatch the review trio in parallel**

Run three review agents in parallel against the full branch diff:

- `code-reviewer` — line-level review of the diff vs `main`.
- `architect-reviewer` — macro/system-level review: layering, port/adapter taxonomy, exception placement, transaction scope.
- `security-auditor` — confirm no auth regression, no new endpoint without bearer requirement, no input-validation gaps beyond what `@NotEmpty` / `@Size` catch.

Address all findings before proceeding to Step 3.

- [ ] **Step 3: Tick the TODO item**

In `TODO.md`, locate the line:

```
    * [ ] Add operation to find tasks by list of ids
```

(In the v0.4.0 → Quick Wins → Functional Improvements block.)

Change it to:

```
    * [X] Add operation to find tasks by list of ids
```

- [ ] **Step 4: Commit the housekeeping**

```
git add TODO.md
git commit -m "docs(todo): mark find tasks by list of ids as done"
```

- [ ] **Step 5: Push and open the PR**

```
git push -u origin <branch>
gh pr create --title "feat(tasks): add batch find tasks by ids query" --body "..."
```

PR body follows the project's commit-msg shape (Conventional Commits + bulleted body summarising the diff per the bulleted-body skill spec).

---

## Self-Review Notes

**Spec coverage:** Each section of the spec maps to a task:
- §4.1 (endpoint + routing) → Task 5
- §4.2 (request validation) → Task 5 (annotations) + Task 6 (tests)
- §4.3 (URL constants) → Task 1
- §4.4 (response DTO) → Task 4
- §4.5 (status codes) → Task 5 (OpenAPI) + Task 6 (tests)
- §4.6 (OpenAPI) → Task 5
- §5.1–§5.3 (use case, service, port) → Task 3 + Task 2
- §5.4 (domain) → no task; intentionally unchanged
- §6.1 (JDBC repo) → no task; uses inherited `findAllById`
- §6.2 (adapter) → Task 2
- §6.3 (database) → no task; PK index already covers `IN`
- §7.1 (service unit tests) → Task 3
- §7.2 (controller IT) → Task 6 (validation-only per existing pattern; happy paths moved to Tasks 7 and 8)
- §7.3 (RestDocs IT) → Task 7
- §7.4 (E2E IT) → Task 8
- §7.5 (no JDBC repo test) → respected; no task
- §7.6 (no Mother changes) → respected; tests use `aTaskBuilder().withTaskId(...).build()`
- §8.1 (api-guide.adoc) → Task 9
- §8.2 (README) → no task; service README is not endpoint-indexed
- §8.3 (TODO.md tick) → Task 10
- §9 (validation) → Task 10 Step 1
- §10 (git workflow) → Task 10 Step 5; commit messages distributed across tasks (one per concern, all green at commit time)

**Placeholder scan:** No "TBD" / "TODO" / "implement later" / "appropriate error handling" placeholders. Each step has a complete code block or exact command.

**Type consistency:**
- Use case method: `getTasksByIds(List<UUID>) : List<Task>` — used identically in Tasks 3, 6, 7, 8.
- Port method: `findByIds(Collection<TaskId>) : Collection<Task>` — used identically in Tasks 2 and 3.
- Adapter method: same signature as the port.
- Mapper method: `toGetTasksByIdsResponse(Task) : GetTasksByIdsResponse` — used identically in Tasks 4 and 7.
- Response record: `GetTasksByIdsResponse(UUID, UUID, String, String, Instant, Instant)` — used identically in Tasks 4, 7, 8.
- URL constant: `TASKS_GET_BY_IDS_PARAM = "ids"` — used identically in Task 5 (`@RequestParam(TASKS_GET_BY_IDS_PARAM)`).
- Controller mapping: existing `@GetMapping(value = TASKS_GET_ALL_PATH, params = "!ids")` and new `@GetMapping(value = TASKS_GET_ALL_PATH, params = "ids")` are consistent across Task 5.

No mismatches detected.