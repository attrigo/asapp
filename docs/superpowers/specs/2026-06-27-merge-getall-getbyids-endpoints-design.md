# Merge `getAll` + `getAllByIds` into one endpoint — design spec

**Date**: 2026-06-27
**Status**: Designed
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Functional Improvements → "Join tasks and users getAll and getAllByIds endpoints into one unique endpoint (avoid swagger collision)"
**Services affected**: `asapp-tasks-service`, `asapp-users-service`

## 1. Context

The find-by-ids feature shipped as a query-parameter filter on the existing collection endpoint, so each service ended up with **two** `GET` handlers mapped to the **same** path, disambiguated only by Spring's `params` attribute:

```java
@GetMapping(value = TASKS_GET_ALL_PATH, params = "!ids")   // getAllTasks
@GetMapping(value = TASKS_GET_ALL_PATH, params = "ids")    // getTasksByIds
```

This works at the Spring MVC routing layer (param presence selects the handler), but **OpenAPI 3 cannot model two operations on a single `(method, path)` pair**. springdoc collapses both `@Operation`s into one path-item in the generated spec, so one silently overwrites the other in Swagger UI — the "swagger collision" the TODO item calls out.

The fix is to collapse each pair into a **single** handler that exposes one OpenAPI operation: the `ids` query parameter becomes optional, absent meaning "all". This is a pure HTTP-surface refactor — no new behaviour, no contract break at the URL level.

The same shape exists in `asapp-tasks-service` (`/api/tasks`) and `asapp-users-service` (`/api/users`); both are fixed identically in this spec.

## 2. Goals

- Expose exactly **one** OpenAPI operation per collection (`GET /api/tasks`, `GET /api/users`), eliminating the collision.
- Preserve the existing HTTP contract at the URL level: `GET /api/tasks` (all) and `GET /api/tasks?ids=…` (filtered) keep working unchanged for every client.
- Keep the change confined to the **infrastructure/in** layer (REST interface, controller, DTOs, mappers) plus the shared URL constants. The application/domain/persistence layers are untouched.
- Leave names accurate after the merge: a handler that lists *or* filters is `getTasks`, not `getAllTasks`.
- Keep both services symmetric so the diff reads identically on each side.

## 3. Non-goals

- **No collapsing of the application layer.** `ReadTaskUseCase`/`ReadUserUseCase` keep both `getAllTasks()`/`getAllUsers()` and `getTasksByIds()`/`getUsersByIds()` as distinct domain reads. The "optional filter" is an HTTP-presentation concern and stays in the controller. Pushing an `Optional<List<UUID>>` into the port would leak presentation semantics into the application and force a broad rewrite of services, repositories, and their unit tests for no functional gain.
- **No change to validation semantics for a *populated* `ids` list** (still 1–50 valid UUIDs → otherwise 400).
- **No change to authentication, authorization, response payloads, or status codes.**
- **No change to `asapp-authentication-service`.** It has `getAllUsers` but no by-ids sibling — therefore no collision. Its `GetAllUsersResponse` is a separate class in its own package, unaffected by the users-service rename. See §9.
- **No JMeter `.jmx` edits.** The merge is URL-transparent; see §8.3.

## 4. API surface (per service; `tasks` shown, `users` identical)

### 4.1 Endpoint — before / after

| | Before | After |
|---|---|---|
| List all | `getAllTasks()` — `@GetMapping(TASKS_GET_ALL_PATH, params = "!ids")` | `getTasks(ids)` — `@GetMapping(TASKS_GET_ALL_PATH)` |
| By IDs | `getTasksByIds(ids)` — `@GetMapping(TASKS_GET_ALL_PATH, params = "ids")` | *(removed — folded into `getTasks`)* |

After the merge there is a single handler:

```java
@GetMapping(value = TASKS_GET_ALL_PATH, produces = "application/json")
@ResponseStatus(HttpStatus.OK)
List<GetTasksResponse> getTasks(
        @RequestParam(name = TASKS_IDS_PARAM, required = false)
        @Parameter(description = "Optional list of task identifiers to filter by; omit to return all tasks")
        @Size(min = 1, max = 50, message = "Tasks identifiers list must contain between 1 and 50 elements") List<UUID> ids);
```

`params = "!ids"` is dropped. The endpoint now matches `GET /api/tasks` regardless of the `ids` parameter, producing one path-item in the OpenAPI spec.

The users-service mirror uses `@Size(min = 1, max = 50, message = "Users identifiers list must contain between 1 and 50 elements")`.

### 4.2 Request / parameter binding

| Aspect | Rule | Change vs. today |
|---|---|---|
| Parameter name | `ids` (`TASKS_IDS_PARAM`) | unchanged |
| Required | `required = false` | **new** — was implicitly required on the by-ids handler |
| Absent (`GET /api/tasks`) | binds to `null` → controller returns all tasks | unchanged behaviour, now same handler |
| Populated (1–50 valid UUIDs) | filtered fetch | unchanged behaviour, now same handler |
| Empty value (`?ids=`) | Spring binds it to an **empty list** `[]`; rejected with 400 via `@Size(min = 1)` | **behaviour preserved** (still 400); the violation message changes — see below |
| Over-cap (>50) | 400 via `@Size(max = 50)` | unchanged behaviour; message changes — see below |
| Malformed UUID | 400 via the existing type-mismatch handler | unchanged |
| Duplicates | deduplicated server-side in the service layer | unchanged |
| `@NotEmpty` | **removed** | incompatible with an optional param — a `null` value (param absent) would fail `@NotEmpty` and break the all-tasks case |
| `@Size(min = 1, max = 50)` | **replaces** `@NotEmpty` + `@Size(max = 50)`; `@Size` skips `null`, so it fires only when the param is present | rejects both empty and over-cap; **null passes through** to mean "all" |

`@Size(min = 1, max = 50)` is the key to keeping the param optional while preserving the empty-`ids` 400:

- `null` (param absent) → `@Size` skips null → controller returns all.
- `[]` (`?ids=`) → size 0 < min 1 → 400.
- 1–50 valid UUIDs → valid → filtered.
- >50 → 400.

Because empty (`@Size`) and malformed (type conversion) are both rejected **before** the handler body runs, a **non-null** `ids` reaching the controller always holds ≥1 valid UUID. The controller therefore branches on `null` alone.

**Validation message change**: a single `@Size(min = 1, max = 50)` produces one message for both the empty and over-cap cases — `"Tasks identifiers list must contain between 1 and 50 elements"` (users: `"Users identifiers list must contain between 1 and 50 elements"`). This replaces today's two distinct messages (`"… must not be empty"` and `"… must contain at most 50 elements"`).

### 4.3 Controller

```java
@Override
public List<GetTasksResponse> getTasks(List<UUID> ids) {
    var tasks = (ids == null) ? readTaskUseCase.getAllTasks() : readTaskUseCase.getTasksByIds(ids);

    return tasks.stream()
                .map(taskMapper::toGetTasksResponse)
                .toList();
}
```

The two former overrides (`getAllTasks`, `getTasksByIds`) collapse into this one. Both use-case methods remain and are called as before.

### 4.4 Response DTO

One record per endpoint (project rule). Since there is now one endpoint, there is one record:

- **Delete** `GetTasksByIdsResponse` / `GetUsersByIdsResponse`.
- **Rename** (`git mv`) `GetAllTasksResponse` → `GetTasksResponse`, `GetAllUsersResponse` → `GetUsersResponse`. Fields are unchanged (the two records were field-identical); only the type name and Javadoc summary change.

`GetTasksResponse` fields (unchanged): `UUID taskId, UUID userId, String title, String description, Instant startDate, Instant endDate`.
`GetUsersResponse` fields (unchanged): `UUID userId, String firstName, String lastName, String email, String phoneNumber`.

Serialization stays camelCase (no `@JsonProperty`), per the global JSON naming convention.

### 4.5 Status codes (unchanged)

| Code | Cause |
|---|---|
| 200 | Success (including empty result set and the all-tasks case) |
| 400 | `ids` present but empty, over-cap, or containing a malformed UUID |
| 401 | Missing or invalid bearer token (existing security chain) |
| 500 | Persistence failure (existing handler) |

### 4.6 OpenAPI

The surviving `@Operation` on `getTasks` must describe **both** behaviours in one description. Proposed wording (and the `api-guide.adoc` prose must match it verbatim — see §8.1):

> "Retrieves tasks from the system. By default returns all registered tasks; when the `ids` query parameter is supplied, returns only the tasks whose identifiers are in the list. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The `ids` list, when present, must contain between 1 and 50 identifiers. If no tasks match, an empty array is returned."

`@ApiResponse` set on `getTasks`:

- `200` — "Tasks retrieved successfully", schema `GetTasksResponse`.
- `400` — "Task identifiers list is empty, exceeds 50 elements, or contains a malformed UUID", schema `ProblemDetail`. *(carried over from the former by-ids handler — invalid `ids` is still possible)*
- `401` — "Authentication required or failed", schema `ProblemDetail`.
- `500` — "An internal error occurred during retrieval", schema `ProblemDetail`.

The users-service mirrors this with "users"/`GetUsersResponse` wording.

### 4.7 URL constants

In `libs/asapp-commons-url` `TaskRestAPIURL` and `UserRestAPIURL`:

- **Remove** `TASKS_GET_BY_IDS_PATH` / `USERS_GET_BY_IDS_PATH` (empty-string relative paths, now unused).
- **Remove** `TASKS_GET_BY_IDS_FULL_PATH` / `USERS_GET_BY_IDS_FULL_PATH` (only referenced by the by-ids tests, which migrate to the `*_GET_ALL_FULL_PATH` constant — see §7).
- **Keep** `*_GET_ALL_PATH`, `*_GET_ALL_FULL_PATH`, and `*_IDS_PARAM`.

## 5. Application, domain & persistence layers

**No changes.** `ReadTaskUseCase`/`ReadUserUseCase` (both `getAll*` and `getByIds`), their services, the repository ports, the JDBC repositories/adapters, and the domain types are all untouched. The merge is entirely above the use-case boundary.

## 6. Mappers

In `TaskMapper` / `UserMapper`:

- **Remove** `toGetTasksByIdsResponse` / `toGetUsersByIdsResponse`.
- **Rename** `toGetAllTasksResponse` → `toGetTasksResponse`, `toGetAllUsersResponse` → `toGetUsersResponse` (return type changes to the renamed record; mapping body unchanged).

## 7. Tests (per service; `tasks` shown, `users` identical)

The use-case/service layer is unchanged, so **`ReadTaskServiceTests` / `ReadUserServiceTests` are not touched**. All test work is at the web tier.

### 7.1 `TaskRestControllerIT` (WebMvc)

Merge the existing `GetAllTasks` and `GetTasksByIds` nested groups into a single `GetTasks` group. Migrate request builders from `TASKS_GET_BY_IDS_FULL_PATH` to `TASKS_GET_ALL_FULL_PATH` (the path string is identical; this just drops the obsolete constant). Coverage to preserve:

| Test | Scenario |
|---|---|
| returns all tasks when `ids` absent | `GET /api/tasks` → use case `getAllTasks()` |
| returns filtered tasks when `ids` present | `GET /api/tasks?ids=…` → use case `getTasksByIds(ids)` |
| 400 when `ids` empty (`?ids=`) | `@Size(min=1)` constraint-violation `ProblemDetail`; `fieldErrors[0].message` = "Tasks identifiers list must contain between 1 and 50 elements" |
| 400 when `ids` over-cap (51) | `@Size(max=50)` constraint-violation `ProblemDetail`; same combined message |
| 400 when `ids` malformed | type-mismatch `ProblemDetail` (unchanged) |
| 401 when token missing | existing auth check |

The empty- and over-cap-`ids` assertions both change to expect the single combined `@Size(min = 1, max = 50)` message (replacing today's two distinct `@NotEmpty`/`@Size` messages). Both still produce the same `fieldErrors[0].field = "ids"`, `detail = "Request validation failed"`, `error = "invalid_request"` shape as today.

### 7.2 `TaskRestControllerDocumentationIT`

Consolidate the `get-all-tasks` and `get-tasks-by-ids` snippet-generating tests into a single `get-tasks` operation that documents the optional `query-parameters`, `http-request`, `response-fields`, `curl-request`, and `http-response`. Preserve the invalid-`ids` `400` validation snippet (the request-parameter validation-failure snippet) — it still documents a real failure mode of the merged endpoint.

### 7.3 `TaskE2EIT`

Merge the all-tasks and by-ids end-to-end cases to hit the single endpoint with and without `ids` against real PostgreSQL. Migrate `TASKS_GET_BY_IDS_FULL_PATH` references to `TASKS_GET_ALL_FULL_PATH`.

### 7.4 OpenAPI exposure

The existing OpenAPI-endpoints integration test must now see a single `GET /api/tasks` operation. Confirm it asserts the merged shape (one operation, no duplicate path-item).

## 8. Documentation

### 8.1 `api-guide.adoc` (both services)

Replace the two sections (`=== Get Tasks by IDs` and `=== Get All Tasks`) with one:

```adoc
[[resources-tasks-get]]
=== Get Tasks

<prose verbatim-identical to the merged @Operation(description) from §4.6>

operation::get-tasks[snippets='query-parameters,http-request,response-fields,curl-request,http-response']
```

Per the REST rule, the adoc prose MUST equal the `@Operation` description word-for-word. The users-service guide is updated the same way (`=== Get Users`, `operation::get-users`).

### 8.2 Service READMEs (light reword)

`services/asapp-tasks-service/README.md` and `services/asapp-users-service/README.md` currently list only `GET /api/tasks | Get all tasks` (the by-ids variant was never documented). Reword the endpoint-table description and the "Operations" prose bullet to acknowledge the optional filter, kept high-level — e.g. **"Get tasks, optionally filtered by `ids`"**. No mechanism detail (cap, validation) in the README. No other README changes:

- `authentication-service` README — out of scope, stays accurate.
- Root `README.md`, `asapp-commons-url`, `asapp-http-clients`, `tools/jmeter` READMEs — high-level only, no per-endpoint/by-ids detail → unchanged.

### 8.3 JMeter load tests (`tools/jmeter/*.jmx`)

**No edits expected.** The merge is transparent at the URL level: every existing sampler keeps hitting a valid URL on the single merged endpoint —

- `asapp-regression.jmx`: "Get Users By Ids" (`/api/users?ids=…`), "Get All Users" (`/api/users`), "Get Tasks By Ids" (`/api/tasks?ids=…`), "Get All Tasks" (`/api/tasks`).
- `asapp-stress.jmx`: "Get Tasks By Ids" (`/api/tasks` + `ids` arg), "Get All Users/Tasks".

Sampler names still accurately describe the two request variants, so no rename is needed. **Action: re-run both `.jmx` after the change to confirm green; no file changes anticipated.**

### 8.4 `TODO.md`

Tick the "Join tasks and users getAll and getAllByIds endpoints…" item under v0.4.0 once the change lands (release-housekeeping, not part of the implementation diff).

## 9. Scope boundary — authentication-service

`asapp-authentication-service` exposes `getAllUsers` (`GET /api/users`) but **no `getUsersByIds`**, no `params = "!ids"`, and no `*_GET_BY_IDS_*` constant usage — so it has no collision and needs no change. Its `GetAllUsersResponse` lives in `com.attrigo.asapp.authentication.infrastructure.user.in.response`, distinct from the users-service class being renamed, so the rename does not touch it. Verified by grep: the only `*_GET_BY_IDS_FULL_PATH` references are in the tasks-service and users-service test trees.

## 10. Validation

- **Build**: `mvn clean install` passes for both services and `asapp-commons-url`.
- **Tests**: `mvn clean verify` passes; the consolidated `GetTasks`/`GetUsers` web-tier groups (§7) cover all branches.
- **OpenAPI**: the generated spec exposes exactly one `GET /api/tasks` and one `GET /api/users` operation (collision resolved).
- **REST Docs**: the merged `get-tasks`/`get-users` snippets render in `api-guide.adoc`.
- **Load tests**: `asapp-regression.jmx` and `asapp-stress.jmx` re-run green with no edits.
- **Manual smoke (optional)**: `docker-compose up -d`, then `GET /api/tasks` and `GET /api/tasks?ids=<a,b>` with a valid bearer; both return 200 with the expected bodies.

## 11. Git workflow

Single feature branch off `main`. Conventional Commits with a bulleted body per the `commit-msg` skill, e.g.:

```
refactor(api): merge get-all and get-by-ids into one endpoint

- Fold getTasksByIds/getUsersByIds into getTasks/getUsers with an optional ids filter
- Drop the params="!ids"/"ids" split so OpenAPI exposes a single GET operation
- Rename GetAllTasksResponse/GetAllUsersResponse to GetTasksResponse/GetUsersResponse and remove the by-ids records
- Remove the now-unused TASKS_GET_BY_IDS/USERS_GET_BY_IDS path constants
- Merge the api-guide sections and reword the service README endpoint rows
- Consolidate the web-tier tests onto the single endpoint
```

The TODO.md tick lands in a follow-up release-housekeeping commit, consistent with the existing pattern.
