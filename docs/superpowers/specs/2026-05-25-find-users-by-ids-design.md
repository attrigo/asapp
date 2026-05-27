# Find users by list of IDs — design spec

**Date**: 2026-05-25
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Functional Improvements → "Add operation to find users by list of ids"
**Service affected**: `asapp-users-service`

## 1. Context

The users service exposes two read operations today: `getUserById` (single, enriched with task IDs via `TasksGateway`) and `getAllUsers` (no enrichment). There is no way to fetch multiple users by an arbitrary list of identifiers in a single request. Clients that hold a set of user IDs must loop and call `GET /api/users/{id}` once per ID, which is N round-trips for what is conceptually one query.

This spec adds a batch-by-IDs read operation, exposed as a query-parameter filter on the existing `/api/users` collection. It is the second of the three sibling TODO items (tasks, users, auth users). The tasks-service feature has already shipped and been polished through manual review (`a2fa5c77` → `55fc26ac` → `5361c949` → `2ec0bad2`); **the current state of `asapp-tasks-service` is the canonical template** for this spec. Conventions and decisions copied from there are deliberate, not coincidental.

## 2. Goals

- Provide a single-request batch fetch keyed by a list of user IDs.
- Stay within REST conventions (idempotent GET, filter-on-collection semantics).
- Preserve the existing `GET /api/users` (`getAllUsers`) behaviour; no breaking change.
- Match the layering, naming, and validation conventions already used in the tasks-service batch endpoint, including the post-review polish.
- Bound the request size predictably so clients get a clean 400, not an opaque 414 URI Too Long.

## 3. Non-goals

- No task-ID enrichment in the response. Decided against because:
  - `TasksGateway` has no batched variant; enriching N users would fan out to N service-to-service calls.
  - The collection endpoint (`getAllUsers`) is the precedent for batch reads and has no enrichment.
- No new RPC-style batch endpoint (`POST /api/users:batchGet` or similar).
- No pagination on the batch response. The 50-element cap (see §4.2) keeps a single response payload acceptable.
- No envelope shape (e.g., `{ found: [...], missing: [...] }`). The response is a plain array, consistent with `getAllUsers`.
- No ordering guarantee. Clients that need order sort client-side by `user_id`.
- No changes to authentication, authorization, or the JWT scope. Any authenticated bearer can call this endpoint, matching the existing read endpoints.
- No batched `TasksGateway.getTaskIdsByUserIds` variant. If a future feature needs enriched batch reads, it gets its own spec.
- The sibling auth-users TODO is out of scope.

## 4. API surface

### 4.1 Endpoint

`GET /api/users?ids={uuid},{uuid},...`

`ids` is a filter on the existing users collection. The collection endpoint serves two distinct contracts disambiguated by Spring's `params` attribute:

```java
@GetMapping(value = USERS_GET_ALL_PATH, params = "!ids")              // existing getAllUsers
@GetMapping(value = USERS_GET_ALL_PATH, params = USERS_IDS_PARAM)     // new getUsersByIds
```

The existing `getAllUsers` interface method gets `params = "!ids"` added to its `@GetMapping`. No other behavioural change to that method.

### 4.2 Request

| Aspect | Rule |
|---|---|
| Parameter name | `ids` |
| Binding | `@RequestParam(USERS_IDS_PARAM) List<UUID> ids` — Spring binds a comma-separated string natively |
| Empty list (`?ids=`) | Rejected with 400 via `@NotEmpty(message = "Users identifiers list must not be empty")` |
| Over-cap (>50 elements) | Rejected with 400 via `@Size(max = 50, message = "Users identifiers list must contain at most 50 elements")` |
| Malformed UUID | Rejected with 400 via Spring's `MethodArgumentTypeMismatchException` chain |
| Duplicates | Accepted; deduplicated server-side at the service layer |
| Class-level annotation | `@Validated` on `UserRestController` (not the API interface — per Spring docs) |
| Annotation order on the param | `@RequestParam → @Parameter → @NotEmpty → @Size` (matches the tasks-service convention) |

The 50 cap fits comfortably in the ~2000-character practical URL budget.

### 4.3 URL constants

Modify `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/users/UserRestAPIURL.java` to mirror `TaskRestAPIURL` structure:

```java
public static final String USERS_GET_BY_IDS_PATH = "";

public static final String USERS_GET_BY_IDS_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_IDS_PATH;

public static final String USERS_IDS_PARAM = "ids";
```

Conventions:

- The relative path constant `USERS_GET_BY_IDS_PATH = ""` is added even though it is empty; it groups with the other relative path constants. Insertion point: between `USERS_GET_BY_ID_PATH` and `USERS_GET_ALL_PATH`.
- The full-path constant `USERS_GET_BY_IDS_FULL_PATH` follows the same grouping; insertion point: between `USERS_GET_BY_ID_FULL_PATH` and `USERS_GET_ALL_FULL_PATH`.
- The param constant `USERS_IDS_PARAM` goes at the bottom of the file, before the private constructor. Naming follows `TASKS_IDS_PARAM` (no `GET_BY_` prefix — it is a parameter name shared by any operation, not a path).
- The controller `@GetMapping` continues to reference `USERS_GET_ALL_PATH` (both empty strings resolve to `/api/users`). The new `USERS_GET_BY_IDS_FULL_PATH` is used by tests and external clients to express intent.

### 4.4 Response

New response record (one response per endpoint per project convention):

```java
package com.bcn.asapp.users.infrastructure.user.in.response;

public record GetUsersByIdsResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        @JsonProperty("phone_number") String phoneNumber
) {}
```

Fields match `GetAllUsersResponse` one-for-one. **No `taskIds` field** (see §3 non-goals). A separate record is required by project rule even though fields overlap with `GetAllUsersResponse`.

| Aspect | Rule |
|---|---|
| Body | `List<GetUsersByIdsResponse>` |
| Order | Unspecified; documented in OpenAPI and `api-guide.adoc` |
| Missing IDs | Silently omitted from the response |
| No matches | 200 with `[]` |

Javadoc summary starts with the verb `"Response for retrieving users by a list of unique identifiers."` (not `"Represents the response for..."`).

### 4.5 Status codes

| Code | Cause |
|---|---|
| 200 | Success (including empty result set) |
| 400 | Empty list, over-cap, or malformed UUID |
| 401 | Missing or invalid bearer token (existing security chain) |
| 500 | Database failure (`DataAccessException` → existing handler) |

All 4xx/5xx responses use the existing RFC 7807 `ProblemDetail` shape. `@NotEmpty` / `@Size` violations produce a `ProblemDetail` with `errors[]` entries of shape `{entity: "", field: "ids", message: "..."}` — see §7.

### 4.6 OpenAPI

Annotations on `UserRestAPI.getUsersByIds`, mirroring `TaskRestAPI.getTasksByIds`:

- `@Operation(summary = "Gets users by their unique identifiers", description = "Retrieves a list of users whose identifiers are in the provided list. This endpoint requires authentication. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The list must contain between 1 and 50 identifiers.")`
- `@ApiResponse(responseCode = "200", description = "Users found", content = { @Content(schema = @Schema(implementation = GetUsersByIdsResponse.class)) })`
- `@ApiResponse(responseCode = "400", description = "User identifiers list is empty, exceeds 50 elements, or contains a malformed UUID", content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })`
- `@ApiResponse(responseCode = "401", ...)` and `@ApiResponse(responseCode = "500", ...)` — same shape as the tasks endpoint.
- `@Parameter(description = "List of user identifiers")` on the `ids` parameter (no parenthetical — matches the tasks polish).

## 5. Application & domain layers

### 5.1 Use case

Add to `application/user/in/ReadUserUseCase.java`, **positioned between `getUserById` and `getAllUsers`**:

```java
/**
 * Retrieves users by their unique identifiers.
 *
 * @param ids the list of user identifiers; duplicates are deduped
 * @return a {@link List} of {@link User} entities found; missing ids are silently omitted
 * @throws IllegalArgumentException if any id is invalid
 */
List<User> getUsersByIds(List<UUID> ids);
```

Raw `UUID` at the application boundary matches the existing `getUserById(UUID id)` convention.

### 5.2 Service

Add to `application/user/in/service/ReadUserService.java`, positioned between `getUserById` and `getAllUsers`:

```java
@Override
public List<User> getUsersByIds(List<UUID> ids) {
    var userIds = ids.stream()
                     .map(UserId::of)
                     .collect(Collectors.toUnmodifiableSet());

    return userRepository.findByIds(userIds)
                         .stream()
                         .toList();
}
```

- `UserId.of` performs domain validation; any `null` element throws `IllegalArgumentException("User ID must not be null")`, mapped to 400 by `GlobalExceptionHandler.handleIllegalArgumentException`.
- Collecting into a `Set` deduplicates before the database round-trip. Acceptable because response order is unspecified.
- **No `TasksGateway` interaction.** This is the key departure from `getUserById`.
- No `@Transactional` (read-only, single-port).

### 5.3 Repository port

Add to `application/user/out/UserRepository.java`, positioned between `findById` and `findAll`:

```java
/**
 * Finds users by their unique identifiers.
 *
 * @param userIds the collection of user identifiers
 * @return a {@link Collection} of {@link User} entities found; missing identifiers are silently omitted
 */
Collection<User> findByIds(Collection<UserId> userIds);
```

Domain VO at the port boundary, `Collection` return type — matches the existing shape.

### 5.4 Domain

No changes. `User`, `UserId`, `UserFactory`, and the value objects are untouched. This is a query operation; no new invariants.

## 6. Persistence layer

### 6.1 JDBC repository

`JdbcUserRepository extends ListCrudRepository<JdbcUserEntity, UUID>`. The inherited `findAllById(Iterable<UUID> ids) : List<JdbcUserEntity>` produces a `SELECT ... WHERE id IN (?, ?, ...)` query that uses the existing primary-key index. No custom `@Query`, no migration, no changelog.

### 6.2 Repository adapter

Add to `infrastructure/user/out/UserRepositoryAdapter.java`, positioned between `findById` and `findAll`:

```java
@Override
public Collection<User> findByIds(Collection<UserId> userIds) {
    var ids = userIds.stream()
                     .map(UserId::value)
                     .toList();

    return userRepository.findAllById(ids)
                         .stream()
                         .map(userMapper::toUser)
                         .toList();
}
```

Unwrap VO via `.value()`, call inherited JDBC method, map back to domain via the existing `UserMapper.toUser`.

### 6.3 Database

Nothing to change. PK on `users.id` covers the `IN` lookup. No foreign-key implications (we query by PK only). No Liquibase changeset.

## 7. Error handling

The users-service `GlobalExceptionHandler` does **not yet** define a `ConstraintViolationException` handler — it has had no `@Validated` query-parameter endpoint until now. Add it, mirroring the tasks-service handler post-`2ec0bad2`:

```java
@ExceptionHandler(ConstraintViolationException.class)
protected ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
    log.warn("Constraint violation: {}", ex.getMessage());

    var invalidParameters = buildInvalidParameters(ex.getConstraintViolations());

    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    problemDetail.setTitle("Bad Request");
    problemDetail.setProperty("errors", invalidParameters);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                         .body(problemDetail);
}

private List<InvalidRequestParameter> buildInvalidParameters(Set<ConstraintViolation<?>> violations) {
    return violations.stream()
                     .map(v -> {
                         var path = v.getPropertyPath().toString();
                         var field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                         return new InvalidRequestParameter("", field, v.getMessage());
                     })
                     .toList();
}
```

Conventions (post-review):

- `detail` is `ex.getLocalizedMessage()` (the raw violation, e.g. `getUsersByIds.ids: Users identifiers list must not be empty`), not a fixed string.
- `InvalidRequestParameter.entity` is `""` (empty), not `"request"` — distinguishes query-param violations from request-body field errors which carry the DTO name.
- Field extraction uses `indexOf('.')`, **not** `lastIndexOf('.')` — this preserves nested paths like `body.address.zipCode` for future endpoints (per `2ec0bad2`). For the flat `ids` parameter, `getUsersByIds.ids` collapses to `ids`.

Imports required (not yet present in the users-service handler):

```java
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
```

## 8. Tests

### 8.1 `ReadUserServiceTests` (unit)

New `@Nested GetUsersByIds`, **placed between `GetUserById` and `GetAllUsers`** (matches method ordering). The class Javadoc Coverage list gains:

- `Returns user collection when queried by a list of identifiers`
- `Deduplicates duplicate identifiers before querying`
- `Throws when the input list contains a null identifier`

Tests (success-then-failure, names per `<Behavior>_<Condition>`):

| Test | Scenario |
|---|---|
| `ReturnsUsers_AllUsersExist` | Port returns all requested users; service returns the list |
| `ReturnsFoundUsers_SomeUsersExist` | Port returns a strict subset; missing ids silently dropped |
| `ReturnsEmptyList_UsersNotExist` | Port returns empty; service returns `[]` |
| `ReturnsUsersOnce_DuplicateIds` | `[u1, u1, u2]` → service calls the port with a Set of 2 distinct elements; verify with `findByIds(Set.of(...))` directly — **no `ArgumentCaptor`** |
| `ThrowsIllegalArgumentException_NullId` | `[u1, null]` → `UserId.of(null)` throws `IllegalArgumentException` with message `"User ID must not be null"` |

No `TasksGateway` interaction is expected; tests must not stub or verify it.

### 8.2 `UserRestControllerIT` (WebMvc) — validation-only

New `@Nested GetUsersByIds`. **No happy-path tests** — those live in `UserRestControllerDocumentationIT` and `UserE2EIT` per the project pattern. The controller-IT updates the class Javadoc Coverage list to mention "GET by IDs".

| Test | Scenario | Asserted detail |
|---|---|---|
| `ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingUsersIds` | `?ids=` triggers `@NotEmpty` | `title=Bad Request`, `status=400`, `detail="getUsersByIds.ids: Users identifiers list must not be empty"`, `instance="/api/users"`, `errors[]` contains exactly `{entity:"", field:"ids", message:"Users identifiers list must not be empty"}` |
| `ReturnsStatusBadRequestAndBodyWithProblemDetail_ExceedUsersIds` | 51 UUIDs trigger `@Size(max=50)` | same shape, message `"Users identifiers list must contain at most 50 elements"` |
| `ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId` | `?ids=1` triggers `MethodArgumentTypeMismatchException` | `title=Bad Request`, `status=400`, `detail="Failed to convert 'ids' with value: '1'"`, `instance="/api/users"`. No `errors[]` array — type-conversion failures don't produce one. |

All three use `get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, ...)` for the request builder.

### 8.3 `UserRestControllerDocumentationIT`

Two additions:

1. New `@Nested GetUsersByIds` (between `GetUserById` and `GetAllUsers`) with `DocumentsGetUsersByIds`:
   - Stubs `readUserUseCase.getUsersByIds(anyList())` and `userMapper.toGetUsersByIdsResponse(any(User.class))`.
   - Builds the request with `get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, ...)`.
   - Documents `requestHeaders`, `queryParameters(parameterWithName("ids").description("Comma-separated list of user identifiers (1 to 50 elements)"))`, and `responseFields` for the 5 user fields.
   - Snippet identifier: `get-users-by-ids`.

2. New test in the existing `Errors` nested class: `DocumentsRequestParamValidationFailure`:
   - Fires `get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, "")` to produce a constraint violation.
   - Documents the `error-request-param-validation-failure` snippet with the same `relaxedResponseFields` shape as the tasks-service snippet (title, status, detail, errors, errors[].entity, errors[].field, errors[].message).

### 8.4 `UserE2EIT`

New `@Nested GetUsersByIds`, **placed between `GetUserById` and `GetAllUsers`**, mirroring the tasks E2E coverage after `55fc26ac`:

| Test | Scenario |
|---|---|
| `ReturnsStatusOKAndBodyWithFoundUsers_AllIdsExist` | Seed two users; `GET /api/users?ids=<both>`; assert both come back |
| `ReturnsStatusOKAndBodyWithFoundUsers_SomeIdsExist` | One existing id + one missing id; only the existing user is returned |
| `ReturnsStatusOKAndBodyWithDistinctUsers_DuplicateIds` | `?ids=<id>,<id>,<id>` → exactly one user returned |
| `ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` | Same auth check as the other endpoints |

Uses the existing helper pattern in `UserE2EIT` (typically `createUser`-style or `userRepository.save(...)`); the test author selects the helper that the surrounding tests already use — no new fixture method.

### 8.5 No JDBC repository test

`findAllById` is inherited from `ListCrudRepository` and exercised by Spring Data itself. The existing `JdbcUserRepositoryIT` tests for `findById` and `findAll` already cover the same connection/SQL path against Postgres. A new case would only re-test Spring Data.

### 8.6 Fixtures

No new methods on `UserMother`. Tests that need multiple distinct users use `aUserBuilder().withUserId(...).build()` per instance — the existing pattern.

## 9. Documentation

### 9.1 `api-guide.adoc`

Add the resource section to `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc` between `[[resources-users-get-by-id]]` and `[[resources-users-get-all]]`:

```adoc
[[resources-users-get-by-ids]]
=== Get Users by IDs

Retrieves a list of users whose identifiers are in the provided `ids` query parameter.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated. The list must contain between 1 and 50 identifiers.

operation::get-users-by-ids[snippets='query-parameters,http-request,response-fields,curl-request,http-response']
```

Add a new error subsection between `[[users-errors-400-path]]` and `[[users-errors-400-body]]`:

```adoc
[[users-errors-400-param]]
==== Validation Failure - Request Parameter (400)

Returned when a query parameter fails validation (empty, out of range, or
malformed). `errors[].entity` is empty; `errors[].field` is the parameter name.

operation::error-request-param-validation-failure[snippets='http-response,response-fields']
```

The overview tables (HTTP verbs, status codes) already cover every status code of the new endpoint — no edits there.

### 9.2 README

No edits expected. The service README describes the service's purpose, not its endpoint inventory.

### 9.3 TODO.md

Tick the `Add operation to find users by list of ids` item under v0.4.0 → Quick Wins → Functional Improvements as `[X]` once the change lands. A separate housekeeping commit, mirroring the tasks-service pattern.

## 10. Validation

- **Build**: `mvn clean install` passes.
- **Unit + integration tests**: `mvn clean verify` passes; the new `@Nested GetUsersByIds` groups in §8.1, §8.2, §8.3, §8.4 cover the new branches.
- **End-to-end**: `UserE2EIT` adds the four cases above proving the path through Spring, Spring Security, the controller, the service, the adapter, the JDBC repository, and PostgreSQL.
- **OpenAPI**: `OpenApiEndpointsIT` (existing) verifies the new endpoint is exposed in the generated spec.
- **REST Docs**: the new snippets in §8.3 land `query-parameters` and `response-fields` documentation, plus the `error-request-param-validation-failure` snippet for the api-guide error section.
- **Manual smoke (optional)**: run the service stack via `docker-compose up -d`, create two users, `GET /api/users?ids=<both>` with a valid bearer; assert both come back.

## 11. Git workflow

Single feature branch off `main`. Commits are sliced per concern (matching the tasks-service implementation cadence), each green at commit time:

1. `feat(commons): add ids query-parameter constant for users`
2. `feat(users): add application slice for find users by ids`
3. `feat(users): add GetUsersByIdsResponse DTO and mapper method`
4. `feat(users): expose GET /api/users?ids= batch find endpoint` (controller + validation IT)
5. `feat(users): handle constraint violations in GlobalExceptionHandler`
6. `test(users): document GET /api/users?ids= endpoint with RestDocs`
7. `test(users): add E2E coverage for GET /api/users?ids= endpoint`
8. `docs(users): document GET /api/users?ids= in api-guide`
9. `docs(todo): mark find users by list of ids as done`

Before opening the PR: dispatch `code-reviewer`, `architect-reviewer`, and `security-auditor` in parallel against the branch diff. Address all findings.

PR title and body follow Conventional Commits + bulleted body per the `commit-msg` skill.

## 12. Post-implementation notes

This spec was written before implementation. The slice shipped in `efad4f5e` (application slice, planned via `docs/superpowers/plans/2026-05-25-find-users-by-ids.md`) and was completed across `90621d0e` → `95d95add`. Manual review produced follow-ups in `910dd6fe`, `e3c19195`, `50b7452b`, and a final refactor pass in `a0d05b8c`. The sections above describe the original design intent; **the canonical implementation is the current state of the code**, not this document. Notable deltas:

- **`ReadUserServiceTests` (unit)**: The spec's §8.1 table listed five tests. A sixth was added during implementation (`910dd6fe`): `ThrowsRuntimeException_UsersRetrievalFails` — mirrors the failure-propagation coverage already present in `GetUserById` and `GetAllUsers`. The mock was subsequently refined to use a concrete `Set<UserId>` rather than `any()` (`e3c19195`).

- **`UserE2EIT`**: Spec §8.4 listed four tests; five were implemented. The extra case is `ReturnsStatusOKAndEmptyBody_UsersNotExist` (both IDs unknown → 200 with `[]`), which closes a gap left by the spec. Test name deltas: `AllIdsExist` → `AllUsersExist`, `SomeIdsExist` → `SomeUsersExist`, `WithDistinctUsers_DuplicateIds` → `WithFoundUsersOnce_DuplicateIds`.

- **`UserMapper` import order**: An out-of-order import for `GetUsersByIdsResponse` was caught post-commit and corrected in a dedicated fix commit (`50b7452b`).

- **Manual review refactoring (`a0d05b8c`)**: `retrieveUser(UserId)` — a one-liner private wrapper — was inlined directly into `getUserById`, removing unnecessary indirection. `enrichUserWithTasks` was moved to the end of the class, after all public read methods, for consistent helper placement. Test UUIDs in `GetUsersByIds` were aligned with the spec-defined value (`57cfe2c8-...`). Variable names in `ThrowsRuntimeException_UsersRetrievalFails` were renamed to follow the `userIdValue` / `userId` / `userIds` / `userIdValues` convention. Static imports in `UserRestControllerIT` were sorted alphabetically.

**For the next sibling feature**, treat the current code as the template, not this spec body. The spec is preserved as a record of original design intent and the reasoning behind key decisions.
