# Find tasks by list of IDs — design spec

**Date**: 2026-05-22
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Functional Improvements → "Add operation to find tasks by list of ids"
**Service affected**: `asapp-tasks-service`

## 1. Context

The tasks service exposes three read operations today: `getTaskById`, `getTasksByUserId`, and `getAllTasks`. There is no way to fetch multiple tasks by an arbitrary list of identifiers in a single request. External clients that already hold a set of task IDs (e.g., from another listing endpoint, from a saved set, from a cross-reference in another system) must loop and call `GET /api/tasks/{id}` once per ID, which is N round-trips for what is conceptually one query.

This spec adds a batch-by-IDs read operation, exposed as a query-parameter filter on the existing `/api/tasks` collection. The feature is one of three sibling TODO items (tasks, users, auth users by IDs); each will be designed and implemented independently, but the API and semantics chosen here set the precedent for the other two.

## 2. Goals

- Provide a single-request batch fetch keyed by a list of task IDs.
- Stay within REST conventions (idempotent GET, filter-on-collection semantics).
- Preserve the existing `GET /api/tasks` (`getAllTasks`) behaviour; no breaking change.
- Match the layering, naming, and validation conventions already used in the service (`getTasksByUserId` is the closest sibling and serves as the reference shape).
- Bound the request size predictably so clients get a clean 400, not an opaque 414 URI Too Long.

## 3. Non-goals

- No new RPC-style batch endpoint (`POST /api/tasks:batchGet` or similar). Decided against because the existing slice is REST-flat and the request fits a query string.
- No pagination on the batch response. The 50-element cap (see §4.2) makes a single response payload acceptable.
- No envelope shape (e.g., `{ found: [...], missing: [...] }`). The response is a plain array, consistent with `getAllTasks` and `getTasksByUserId`.
- No ordering guarantee. Clients that need order sort client-side by `task_id`.
- No changes to authentication, authorization, or the JWT scope. Any authenticated bearer can call this endpoint, matching the existing read endpoints.
- The sibling TODO items (users by IDs, auth users by IDs) are out of scope for this spec.

## 4. API surface

### 4.1 Endpoint

`GET /api/tasks?ids={uuid},{uuid},...`

`ids` is a filter on the existing tasks collection. The collection endpoint serves two distinct contracts disambiguated by Spring's `params` attribute:

```java
@GetMapping(value = TASKS_GET_ALL_PATH, params = "!ids")   // existing getAllTasks
@GetMapping(value = TASKS_GET_ALL_PATH, params = "ids")    // new getTasksByIds
```

The existing `getAllTasks` interface method gets `params = "!ids"` added to its `@GetMapping`. No other behavioural change to that method.

### 4.2 Request

| Aspect | Rule |
|---|---|
| Parameter name | `ids` |
| Binding | `@RequestParam List<UUID> ids` — Spring binds a comma-separated string natively |
| Empty list (`?ids=`) | Rejected with 400 via `@NotEmpty` |
| Over-cap (>50 elements) | Rejected with 400 via `@Size(max = 50)` |
| Malformed UUID | Rejected with 400 via Spring's `MethodArgumentTypeMismatchException`, handled by the existing `ResponseEntityExceptionHandler` chain |
| Duplicates | Accepted; deduplicated server-side at the service layer |
| Class-level annotation | `@Validated` on `TaskRestController` — required so method-parameter constraints fire |

The 50 cap fits comfortably in the ~2000-character practical URL budget (50 UUIDs ≈ 1850 chars including separators and the path/host).

### 4.3 URL constants

Add to `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java`:

```java
public static final String TASKS_GET_BY_IDS_PARAM = "ids";
```

No new path constant — the URL is `TASKS_GET_ALL_FULL_PATH + "?ids=..."`. The query-parameter name lives in `TaskRestAPIURL` so tests and clients reference a single source.

### 4.4 Response

New response record (one response per endpoint per project convention):

```java
package com.bcn.asapp.tasks.infrastructure.task.in.response;

public record GetTasksByIdsResponse(
        @JsonProperty("task_id") UUID taskId,
        @JsonProperty("user_id") UUID userId,
        String title,
        String description,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate
) {}
```

Fields match `GetTasksByUserIdResponse` one-for-one. A separate record is required by project rule even when fields overlap.

| Aspect | Rule |
|---|---|
| Body | `List<GetTasksByIdsResponse>` |
| Order | Unspecified; documented in OpenAPI and `api-guide.adoc` |
| Missing IDs | Silently omitted from the response |
| No matches | 200 with `[]` |

### 4.5 Status codes

| Code | Cause |
|---|---|
| 200 | Success (including empty result set) |
| 400 | Empty list, over-cap, or malformed UUID |
| 401 | Missing or invalid bearer token (existing security chain) |
| 500 | Database failure (`DataAccessException` → existing handler) |

All 4xx/5xx responses use the existing RFC 7807 `ProblemDetail` shape; validation failures use the `errors` extension with `InvalidRequestParameter` entries, exactly like the rest of the service.

### 4.6 OpenAPI

Annotations on `TaskRestAPI.getTasksByIds`:

- `@Operation(summary = "Gets tasks by their unique identifiers", description = "Retrieves a list of tasks whose identifiers are in the provided list. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The list must contain between 1 and 50 identifiers. This endpoint requires authentication.")`
- `@ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = { @Content(schema = @Schema(implementation = GetTasksByIdsResponse.class)) })`
- `@ApiResponse(responseCode = "400", description = "Empty, over-cap or malformed ids parameter", content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })`
- `@ApiResponse(responseCode = "401", description = "Authentication required or failed", ...)`
- `@ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", ...)`
- `@Parameter(description = "List of task identifiers (1 to 50 elements, comma-separated)")` on the `ids` parameter.

## 5. Application & domain layers

### 5.1 Use case

Add to `application/task/in/ReadTaskUseCase.java`:

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

Raw `UUID` at the application boundary matches the existing `getTaskById(UUID id)` convention. The service is responsible for wrapping into the domain VO.

### 5.2 Service

Add to `application/task/in/service/ReadTaskService.java`:

```java
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

- `TaskId::of` performs domain validation; any invalid element throws `IllegalArgumentException`, mapped to 400 by `GlobalExceptionHandler.handleIllegalArgumentException`.
- Collecting into a `Set` deduplicates before the database round-trip. Acceptable because response order is unspecified.
- No `@Transactional` (read-only, single-port).

### 5.3 Repository port

Add to `application/task/out/TaskRepository.java`:

```java
/**
 * Finds tasks by their unique identifiers.
 *
 * @param taskIds the collection of task identifiers
 * @return a {@link Collection} of {@link Task} entities found; missing identifiers are silently omitted
 */
Collection<Task> findByIds(Collection<TaskId> taskIds);
```

Domain VO at the port boundary, `Collection` return type — both match the existing shape (`findByUserId`).

### 5.4 Domain

No changes. `Task`, `TaskId`, and `TaskFactory` are untouched. This is a query operation; no new invariants.

## 6. Persistence layer

### 6.1 JDBC repository

`JdbcTaskRepository` extends `ListCrudRepository<JdbcTaskEntity, UUID>`. The inherited `findAllById(Iterable<UUID> ids) : List<JdbcTaskEntity>` produces a `SELECT ... WHERE id IN (?, ?, ...)` query that uses the existing primary-key index. No custom `@Query`, no migration, no changelog.

### 6.2 Repository adapter

Add to `infrastructure/task/out/TaskRepositoryAdapter.java`:

```java
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

Same shape as `findByUserId`: unwrap VO, call JDBC, map back to domain via the existing `TaskMapper.toTask`.

### 6.3 Database

Nothing to change. PK on `tasks.id` covers the `IN` lookup. No foreign-key implications (we query by PK only). No Liquibase changeset.

## 7. Tests

### 7.1 `ReadTaskServiceTests` (unit)

New `@Nested GetTasksByIds`, ordered success-then-failure:

| Test | Scenario |
|---|---|
| `ReturnsTasks_AllIdsExist` | Port returns all requested tasks; service returns the list |
| `ReturnsFoundTasks_SomeIdsExist` | Port returns a strict subset; missing ids silently dropped |
| `ReturnsEmptyList_NoIdsExist` | Port returns empty; service returns `[]` |
| `DedupesIds_DuplicatesInInput` | `[u1, u1, u2]` → service calls the port with a Set of 2 distinct elements (verify with `ArgumentCaptor<Collection<TaskId>>`) |
| `ThrowsException_NullId` | `[u1, null]` → `TaskId.of(null)` throws `IllegalArgumentException` |

### 7.2 `TaskRestControllerIT` (WebMvc)

New `@Nested GetTasksByIds`:

| Test | Scenario |
|---|---|
| `ReturnsTasks_ValidIdsAllExist` | 200 + JSON array, field-by-field assertion |
| `ReturnsFoundTasks_SomeIdsExist` | 200 + array with only the subset returned by the mocked use case |
| `ReturnsEmptyArray_NoIdsExist` | 200 + `[]` |
| `ReturnsBadRequest_EmptyIds` | `?ids=` triggers `@NotEmpty`; assert `ProblemDetail` shape and message |
| `ReturnsBadRequest_TooManyIds` | 51 UUIDs trigger `@Size(max=50)`; assert message |
| `ReturnsBadRequest_MalformedUuid` | `?ids=not-a-uuid` → 400 via type-mismatch handler |
| `ReturnsUnauthorized_MissingToken` | Same auth check as the other endpoints |
| `RoutesToGetAllTasks_NoIdsParam` | Regression for `params = "!ids"`: hitting `/api/tasks` without `ids` still routes to `getAllTasks` |

### 7.3 `TaskRestControllerDocumentationIT`

One snippet for the success case using `request-parameters` (instead of `path-parameters`), `http-request`, `response-fields`, `curl-request`, `http-response`. Snippet identifier: `get-tasks-by-ids`.

### 7.4 `TaskE2EIT`

One end-to-end case: seed two tasks via the create endpoint or fixture, `GET /api/tasks?ids=<both>`, assert both come back with matching fields. Real PostgreSQL via testcontainers.

### 7.5 No JDBC repository test

`findAllById` is inherited from `ListCrudRepository` and tested upstream by Spring Data. The existing `JdbcTaskRepositoryIT` tests for `findById` and `findAll` already exercise the same connection/SQL path against Postgres. Adding a new case would only re-test Spring Data.

### 7.6 Fixtures

No new methods on `TaskMother`. Tests that need multiple distinct tasks use `aTaskBuilder().withTaskId(...).build()` per instance — this is the existing pattern.

## 8. Documentation

### 8.1 `api-guide.adoc`

Add a new resource section between *Get All Tasks* and *Create Task* in `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc`:

```adoc
[[resources-tasks-get-by-ids]]
=== Get Tasks by IDs

Retrieves a list of tasks whose identifiers are in the provided `ids` query parameter.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated. The list must contain between 1 and 50 identifiers.

operation::get-tasks-by-ids[snippets='request-parameters,http-request,response-fields,curl-request,http-response']
```

The overview tables (HTTP verbs, status codes, RFC 7807 errors) already cover every failure mode of the new endpoint — no edits needed there.

### 8.2 README

No edits expected. The service README describes the service's purpose, not its endpoint inventory. If implementation discovery surfaces an endpoint list that needs syncing, the spec is amended at planning time.

### 8.3 TODO.md

Tick the `Add operation to find tasks by list of ids` item under v0.4.0 → Quick Wins → Functional Improvements as `[X]` once the change lands. Not part of the implementation diff, but called out so it doesn't slip during release housekeeping.

## 9. Validation

- **Build**: `mvn clean install` passes.
- **Unit + integration tests**: `mvn clean verify` passes; the new `@Nested GetTasksByIds` groups in §7.1 and §7.2 cover the new branches.
- **End-to-end**: `TaskE2EIT` adds one case proving the path through Spring, Spring Security, the controller, the service, the adapter, the JDBC repository, and PostgreSQL.
- **OpenAPI**: `OpenApiEndpointsIT` (existing) verifies the new endpoint is exposed in the generated spec.
- **REST Docs**: the new snippet in §7.3 lands `request-parameters` and `response-fields` documentation alongside the rest.
- **Manual smoke (optional)**: run the service stack via `docker-compose up -d`, create two tasks, `GET /api/tasks?ids=<both>` with a valid bearer; assert both come back.

## 10. Git workflow

Single feature branch off `main`, single commit on green CI:

```
feat(tasks): add batch find tasks by ids query

- Add getTasksByIds use case backed by ListCrudRepository.findAllById
- Add GET /api/tasks?ids= filter alongside getAllTasks
- Add validation: NotEmpty, Size(max=50), reject malformed UUIDs
- Add unit, WebMvc, RestDocs, and E2E coverage for the new endpoint
```

PR title and body follow the existing convention (Conventional Commits + bulleted body per the `commit-msg` skill). The TODO.md item is ticked in a follow-up release-housekeeping commit, consistent with the pattern in `c3daa0b4`.

## 11. Post-implementation notes

This spec was written before implementation. The slice shipped in `a2fa5c77` (initial implementation, planned via `docs/superpowers/plans/2026-05-22-find-tasks-by-ids.md`) and was then refined by manual review in `55fc26ac`, with follow-ups in `5361c949` and `2ec0bad2`. The sections above describe the original design intent; **the canonical implementation is the current state of the code**, not this document. Notable deltas:

- **URL constants** (`TaskRestAPIURL`):
  - Param constant renamed `TASKS_GET_BY_IDS_PARAM` → `TASKS_IDS_PARAM` (no `GET_BY_` prefix — it's a param name, not a path).
  - Added `TASKS_GET_BY_IDS_PATH = ""` and `TASKS_GET_BY_IDS_FULL_PATH` for use by tests and external clients (the controller `@GetMapping` still references `TASKS_GET_ALL_PATH`).
  - Constants regrouped: relative paths → full paths → `*_PARAM` constants at the bottom.

- **Method ordering**: `getTasksByIds` placed between `getTaskById` and `getTasksByUserId` in the use case, service, repository port, repository adapter, REST API interface, REST controller, and mapper. Final order: byId → byIds → byUserId → all.

- **`@Validated` placement**: on `TaskRestController` (the `@RestController` class), not on the `TaskRestAPI` interface — per Spring's documented convention.

- **OpenAPI + validation wording**:
  - 200 description: `"Tasks found"` (not "Tasks retrieved successfully").
  - 400 description names the exact failure modes.
  - `@NotEmpty` message: `"Tasks identifiers list must not be empty"`.
  - `@Size` message: `"Tasks identifiers list must contain at most 50 elements"`.
  - `@Parameter` description shortened to `"List of task identifiers"` (no parenthetical).

- **Request-param annotation order**: `@RequestParam → @Parameter → @NotEmpty → @Size`.

- **Response DTO Javadoc** starts with the verb `"Response for…"` (not "Represents the response for…").

- **`GlobalExceptionHandler.handleConstraintViolationException`**:
  - `detail` uses `ex.getLocalizedMessage()` (full violation, e.g. `getTasksByIds.ids: …`), not a fixed string.
  - `InvalidRequestParameter.entity` is `""` (empty), not `"request"`.
  - Field extraction uses `indexOf('.')` (not `lastIndexOf('.')`) so nested paths like `body.address.zipCode` are preserved (`2ec0bad2`).
  - The mapping lambda is inlined (no `Function` variable).

- **`TaskRestControllerIT` (WebMvc)** — validation-only after review:
  - All happy-path tests removed; they live in `TaskRestControllerDocumentationIT` and `TaskE2EIT`.
  - Failure tests carry full `detail` / `instance` / `errors[]` assertions, not just title+status.
  - The `RoutesToGetAllTasks_IdsAbsent` regression test was removed.

- **`ReadTaskServiceTests` (unit)**:
  - `@Nested GetTasksByIds` placed between `GetTasksByUserId` and the existing `GetTaskById` ordering — final order: byId → byIds → byUserId → all.
  - Test renames per `<Behavior>_<Condition>`: `ReturnsTasks_AllTasksExist`, `ReturnsFoundTasks_SomeTasksExist`, `ReturnsEmptyList_TasksNotExist`, `ReturnsTasksOnce_DuplicateIds`, `ThrowsIllegalArgumentException_NullId`.
  - Dedupe test uses 2 distinct IDs with one duplicated, no `ArgumentCaptor` — verifies `findByIds(Set.of(...))` directly.
  - Null-id test asserts `.hasMessage("Task ID must not be null")` (the actual VO exception message).
  - Class Javadoc Coverage list expanded for the dedupe + null-id scenarios.

- **Documentation**:
  - New api-guide subsection `[[tasks-errors-400-param]]` for query-parameter validation failures (split from the body-validation section in `d19e56aa`).
  - New RestDocs snippet `error-request-param-validation-failure` in `TaskRestControllerDocumentationIT` (added in `55fc26ac`).
  - Get-tasks-by-ids snippet list uses `query-parameters` (not `request-parameters`).

- **`TaskE2EIT`**: additional coverage for duplicate-ids and missing-ids cases (`55fc26ac`); body assertions strengthened in `5361c949`.

**For the next sibling feature**, treat the current code as the template, not this spec body. The Spec is preserved as a record of the original design intent and the reasoning that led to it.