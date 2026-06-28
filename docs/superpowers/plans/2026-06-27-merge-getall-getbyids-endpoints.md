# Merge `getAll` + `getAllByIds` into one endpoint — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse each service's two same-path `GET` handlers (`getAllTasks` + `getTasksByIds`, `getAllUsers` + `getUsersByIds`) into a single endpoint with an optional `ids` filter, so OpenAPI exposes one operation per collection and the Swagger collision disappears.

**Architecture:** HTTP-surface-only refactor confined to `infrastructure/in` (REST interface, controller, response DTOs, mappers) plus the shared `asapp-commons-url` constants. The application, domain, and persistence layers — including both `getAll*()` and `getByIds()` use-case methods — are untouched; the controller branches on `ids == null`.

**Tech Stack:** Spring Boot 4 / Java 25, Spring MVC, springdoc-openapi, MapStruct, Spring REST Docs (Asciidoctor), JUnit 6 + AssertJ + json-unit, Testcontainers.

## Global Constraints

- **HTTP-surface-only.** Do not modify `ReadTaskUseCase`/`ReadUserUseCase`, their services, repositories, adapters, or domain types. Both `getAllTasks()`/`getAllUsers()` and `getTasksByIds()`/`getUsersByIds()` remain.
- **Optional param.** `@RequestParam(name = *_IDS_PARAM, required = false) @Size(min = 1, max = 50, message = "<…> identifiers list must contain between 1 and 50 elements") List<UUID> ids`. Drop `@NotEmpty`. Controller branches on `ids == null` only.
- **One response record per endpoint.** After the merge there is one endpoint, hence one record per service: `GetTasksResponse` / `GetUsersResponse`. Delete the by-ids records.
- **Doc parity (REST rule).** Each endpoint's `api-guide.adoc` prose MUST be verbatim-identical to its `@Operation(description = …)`. "This endpoint requires authentication." MUST NOT appear in either.
- **camelCase JSON, no `@JsonProperty`.** Response records keep their existing fields verbatim.
- **`asapp-authentication-service` is out of scope** — it has no by-ids sibling and its `GetAllUsersResponse` is a different class in its own package. Do not touch it.
- **File renames use `git mv`** — never delete-and-recreate.
- **Conventional Commits**, one commit per service. Pre-commit hooks validate formatting, LF line endings, and the commit message.
- **Spec:** `docs/superpowers/specs/2026-06-27-merge-getall-getbyids-endpoints-design.md`.

> **Maven note:** `mvn clean verify` runs the integration tests (`*IT`, `*E2EIT`) and is slow; per the developer's workflow, run the full `verify`/`install` gates with the developer rather than autonomously. There are no new unit tests in this plan (the use-case layer is unchanged).

> **TDD shape for a rename refactor:** Each service task leads with a *new* failing OpenAPI assertion (the regression anchor for "collision resolved"), then applies the production + test edits together. Because the rename deletes symbols the existing tests reference, the module will not compile mid-task — that is expected. `mvn clean verify` is the single green gate at the end of each task.

---

### Task 1: Merge the tasks-service endpoint

**Files:**
- Modify: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskRestAPIURL.java`
- Rename: `services/asapp-tasks-service/.../infrastructure/task/in/response/GetAllTasksResponse.java` → `GetTasksResponse.java`
- Delete: `services/asapp-tasks-service/.../infrastructure/task/in/response/GetTasksByIdsResponse.java`
- Modify: `services/asapp-tasks-service/.../infrastructure/task/mapper/TaskMapper.java`
- Modify: `services/asapp-tasks-service/.../infrastructure/task/in/TaskRestAPI.java`
- Modify: `services/asapp-tasks-service/.../infrastructure/task/in/TaskRestController.java`
- Test: `services/asapp-tasks-service/.../infrastructure/config/OpenApiEndpointsIT.java`
- Test: `services/asapp-tasks-service/.../infrastructure/task/in/TaskRestControllerIT.java`
- Test: `services/asapp-tasks-service/.../infrastructure/task/in/TaskRestControllerDocumentationIT.java`
- Test: `services/asapp-tasks-service/.../infrastructure/task/TaskE2EIT.java`
- Modify: `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc`
- Modify: `services/asapp-tasks-service/README.md`

**Interfaces:**
- Consumes: `ReadTaskUseCase.getAllTasks() : List<Task>` and `ReadTaskUseCase.getTasksByIds(List<UUID>) : List<Task>` (both unchanged).
- Produces: REST interface method `getTasks(List<UUID> ids) : List<GetTasksResponse>`; mapper `TaskMapper.toGetTasksResponse(Task) : GetTasksResponse`; OpenAPI operationId `getTasks` on `GET /api/tasks`.

- [ ] **Step 1: Write the failing OpenAPI regression test**

Add this method to `OpenApiEndpointsIT` (after `ReturnsStatusOkAndBodyWithApiSpec_OnOpenApiDocs`):

```java
    @Test
    void ReturnsSingleGetTasksOperationWithIdsQueryParam_OnOpenApiDocs() {
        // When & Then
        restTestClient.get()
                      .uri("/v3/api-docs")
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> {
                          var body = response.getResponseBody();
                          assertThatJson(body).node("paths./api/tasks.get.operationId")
                                              .isEqualTo("getTasks");
                          assertThatJson(body).node("paths./api/tasks.get.parameters")
                                              .isArray()
                                              .hasSize(1);
                          assertThatJson(body).node("paths./api/tasks.get.parameters[0].name")
                                              .isEqualTo("ids");
                          assertThatJson(body).node("paths./api/tasks.get.parameters[0].in")
                                              .isEqualTo("query");
                      });
    }
```

Add `Single merged GET /api/tasks operation exposed with an optional ids query parameter` to the class Javadoc Coverage list.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd services/asapp-tasks-service && mvn -q test -Dtest=OpenApiEndpointsIT#ReturnsSingleGetTasksOperationWithIdsQueryParam_OnOpenApiDocs`
Expected: FAIL — current code exposes two collided handlers, so `paths./api/tasks.get.operationId` is `getAllTasks` or `getTasksByIds`, not `getTasks`.

- [ ] **Step 3: Remove the unused by-ids URL constants**

In `TaskRestAPIURL.java`, delete these two lines:

```java
    public static final String TASKS_GET_BY_IDS_PATH = "";
```
```java
    public static final String TASKS_GET_BY_IDS_FULL_PATH = TASKS_ROOT_PATH + TASKS_GET_BY_IDS_PATH;
```

Keep `TASKS_GET_ALL_PATH`, `TASKS_GET_ALL_FULL_PATH`, and `TASKS_IDS_PARAM`.

- [ ] **Step 4: Rename the response record and delete the by-ids record**

Rename the file with `git mv`:

```bash
git mv services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/response/GetAllTasksResponse.java \
       services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/response/GetTasksResponse.java
```

In the renamed file, change the type name and Javadoc summary; fields stay identical:

```java
/**
 * Response for retrieving tasks.
 *
 * @param taskId      the task's unique identifier
 * @param userId      the task's user unique identifier
 * @param title       the task's title
 * @param description the task's description
 * @param startDate   the task's start date
 * @param endDate     the task's end date
 * @since 0.2.0
 * @author attrigo
 */
public record GetTasksResponse(
        UUID taskId,
        UUID userId,
        String title,
        String description,
        Instant startDate,
        Instant endDate
) {}
```

Delete the by-ids record:

```bash
git rm services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/response/GetTasksByIdsResponse.java
```

- [ ] **Step 5: Update `TaskMapper`**

Remove the import of `GetTasksByIdsResponse`; change the import `GetAllTasksResponse` → `GetTasksResponse`. Delete the `toGetTasksByIdsResponse` method. Rename `toGetAllTasksResponse` to `toGetTasksResponse`:

```java
    /**
     * Maps a domain {@link Task} to a {@link GetTasksResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link GetTasksResponse}
     */
    @Mapping(target = "taskId", source = "id")
    GetTasksResponse toGetTasksResponse(Task task);
```

- [ ] **Step 6: Merge the REST interface method in `TaskRestAPI`**

Fix imports: remove `import ...response.GetAllTasksResponse;`, remove `import ...response.GetTasksByIdsResponse;`, add `import ...response.GetTasksResponse;`, remove `import jakarta.validation.constraints.NotEmpty;` (keep `@Size`).

Delete the entire `getTasksByIds` method (its Javadoc + `@GetMapping(params = TASKS_IDS_PARAM)` + `@Operation` + `@ApiResponse`s + signature).

Replace the `getAllTasks` method (Javadoc + annotations + signature) with:

```java
    /**
     * Gets tasks, optionally filtered by their unique identifiers.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Tasks retrieved successfully.</li>
     * <li>400-BAD_REQUEST: ids is empty, exceeds 50 elements, or contains a malformed UUID.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param ids the optional list of task identifiers to filter by (1 to 50 elements); when {@code null} all tasks are returned
     * @return a {@link List} of {@link GetTasksResponse} containing the tasks found, or an empty list if none match; missing identifiers are silently omitted
     */
    @GetMapping(value = TASKS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets tasks", description = "Retrieves tasks from the system. By default returns all registered tasks; when the `ids` query parameter is supplied, returns only the tasks whose identifiers are in the list. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The `ids` list, when present, must contain between 1 and 50 identifiers. If no tasks match, an empty array is returned.")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = {
            @Content(schema = @Schema(implementation = GetTasksResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Task identifiers list is empty, exceeds 50 elements, or contains a malformed UUID", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetTasksResponse> getTasks(
            @RequestParam(name = TASKS_IDS_PARAM, required = false) @Parameter(description = "Optional list of task identifiers to filter by; omit to return all tasks") @Size(min = 1, max = 50, message = "Tasks identifiers list must contain between 1 and 50 elements") List<UUID> ids);
```

- [ ] **Step 7: Merge the controller method in `TaskRestController`**

Fix imports: remove `import ...response.GetAllTasksResponse;` and `import ...response.GetTasksByIdsResponse;`, add `import ...response.GetTasksResponse;`. Delete the `getTasksByIds` override. Replace the `getAllTasks` override with:

```java
    /**
     * Gets tasks, optionally filtered by their unique identifiers.
     *
     * @param ids the optional list of task identifiers to filter by; when {@code null} all tasks are returned
     * @return a {@link List} of {@link GetTasksResponse} containing the tasks found, or an empty list if none match; missing identifiers are silently omitted
     */
    @Override
    public List<GetTasksResponse> getTasks(List<UUID> ids) {
        var tasks = ids == null ? readTaskUseCase.getAllTasks() : readTaskUseCase.getTasksByIds(ids);

        return tasks.stream()
                    .map(taskMapper::toGetTasksResponse)
                    .toList();
    }
```

- [ ] **Step 8: Migrate `TaskRestControllerIT`**

This file holds only the by-ids validation failures (happy paths live in the Documentation/E2E ITs). Apply:

1. Swap the import `TASKS_GET_BY_IDS_FULL_PATH` → `TASKS_GET_ALL_FULL_PATH`.
2. Rename `class GetTasksByIds {` → `class GetTasks {`.
3. Replace every `get(TASKS_GET_BY_IDS_FULL_PATH)` with `get(TASKS_GET_ALL_FULL_PATH)` (3 occurrences, in `…_MissingTasksIds`, `…_ExceedTasksIds`, `…_InvalidTaskId`).
4. In `…_MissingTasksIds`, change the asserted message `"Tasks identifiers list must not be empty"` → `"Tasks identifiers list must contain between 1 and 50 elements"`.
5. In `…_ExceedTasksIds`, change `"Tasks identifiers list must contain at most 50 elements"` → `"Tasks identifiers list must contain between 1 and 50 elements"`.

The `…_InvalidTaskId` detail (`"Failed to convert 'ids' with value: '1'"`) is unchanged. Test method names are kept; only the nesting class and the two messages change.

- [ ] **Step 9: Migrate `TaskRestControllerDocumentationIT`**

1. Rename `class GetTasksByIds {` → `class GetTasks {`.
2. In that class's success test, rename the snippet `document("get-tasks-by-ids", …)` → `document("get-tasks", …)`. Keep its `queryParameters(...)` and `responseFields(...)`; set the `ids` parameter description to `"Optional list of task identifiers to filter by; omit to return all tasks"`.
3. Delete the entire `class GetAllTasks { … }` block (its `document("get-all-tasks", …)` success test is now redundant — the merged `get-tasks` snippet documents the same endpoint with its optional `ids` param).
4. In the errors section, replace `get(TASKS_GET_BY_IDS_FULL_PATH)` with `get(TASKS_GET_ALL_FULL_PATH)` in the `document("error-request-param-validation-failure", …)` test (the `?ids=` empty case still returns 400 via `@Size(min=1)`), and swap its import accordingly.

- [ ] **Step 10: Migrate `TaskE2EIT`**

Merge `class GetTasksByIds` and `class GetAllTasks` into one `class GetTasks`. Swap the import `TASKS_GET_BY_IDS_FULL_PATH` → `TASKS_GET_ALL_FULL_PATH` and replace every `path(TASKS_GET_BY_IDS_FULL_PATH)` with `path(TASKS_GET_ALL_FULL_PATH)` (5 occurrences). Port the existing bodies unchanged except the constant, renaming methods to avoid collisions, to this inventory (success first, failure last per the testing rules):

| Source | New method name |
|---|---|
| `GetAllTasks.ReturnsStatusOKAndBodyWithFoundTasks_TasksExist` (no `ids` param) | `ReturnsStatusOKAndBodyWithAllTasks_NoIdsFilter` |
| `GetTasksByIds.ReturnsStatusOKAndBodyWithFoundTasks_AllTasksExist` | `ReturnsStatusOKAndBodyWithFoundTasks_AllIdsExist` |
| `GetTasksByIds.ReturnsStatusOKAndBodyWithFoundTasks_SomeTasksExist` | `ReturnsStatusOKAndBodyWithFoundTasks_SomeIdsExist` |
| `GetTasksByIds.ReturnsStatusOKAndBodyWithFoundTasksOnce_DuplicateIds` | `ReturnsStatusOKAndBodyWithFoundTasksOnce_DuplicateIds` |
| `GetAllTasks.ReturnsStatusOKAndEmptyBody_TasksNotExist` (empty DB, no `ids`) | `ReturnsStatusOKAndEmptyBody_NoIdsFilterAndTasksNotExist` |
| `GetTasksByIds.ReturnsStatusOKAndEmptyBody_TasksNotExist` (unknown `ids`) | `ReturnsStatusOKAndEmptyBody_IdsNotExist` |
| `GetTasksByIds.ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` | `ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` |

Drop `GetAllTasks.ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` (duplicate of the one kept). Update the class Javadoc Coverage list to describe the merged unfiltered/filtered scenarios behaviorally.

- [ ] **Step 11: Merge the `api-guide.adoc` sections**

Delete the `[[resources-tasks-get-by-ids]] === Get Tasks by IDs` block (heading + prose + `operation::get-tasks-by-ids[…]`). Replace the `[[resources-tasks-get-all]] === Get All Tasks` block with:

```adoc
[[resources-tasks-get]]
=== Get Tasks

Retrieves tasks from the system. By default returns all registered tasks; when the `ids` query parameter is supplied, returns only the tasks whose identifiers are in the list.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated server-side. The `ids` list, when present, must contain between 1 and 50 identifiers.
If no tasks match, an empty array is returned.

operation::get-tasks[snippets='query-parameters,http-request,response-fields,curl-request,http-response']
```

The prose joins to the same text as the Step 6 `@Operation(description)` — keep them byte-for-byte equal.

- [ ] **Step 12: Reword the README**

In `services/asapp-tasks-service/README.md`:

- Line ~33 bullet: `- **Get All Tasks**: List all tasks in system` → `- **Get Tasks**: List all tasks, optionally filtered by a list of IDs`
- Endpoint table row: `| GET    | `/api/tasks`           | Get all tasks        | ✅             |` → `| GET    | `/api/tasks`           | Get tasks (optionally by IDs) | ✅             |`

- [ ] **Step 13: Run the full verification gate (green)**

Run: `cd services/asapp-tasks-service && mvn clean verify`
Expected: BUILD SUCCESS. Specifically: the new `OpenApiEndpointsIT` assertion passes; `TaskRestControllerIT` `GetTasks` failures assert the combined message; the Asciidoctor build resolves `operation::get-tasks` (proving the `get-tasks` snippet was generated and the `get-tasks-by-ids`/`get-all-tasks` references are gone).

- [ ] **Step 14: Commit**

```bash
git add libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskRestAPIURL.java \
        services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task \
        services/asapp-tasks-service/src/test/java/com/attrigo/asapp/tasks/infrastructure \
        services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc \
        services/asapp-tasks-service/README.md
git commit -m "refactor(tasks): merge get-all and get-by-ids into one endpoint

- Fold getTasksByIds into getTasks with an optional ids filter so OpenAPI exposes a single GET /api/tasks operation
- Validate ids with @Size(min=1, max=50); drop @NotEmpty and the params=ids/!ids split
- Rename GetAllTasksResponse to GetTasksResponse and remove GetTasksByIdsResponse
- Remove the unused TASKS_GET_BY_IDS path constants
- Merge the api-guide section, reword the README row, and consolidate the web-tier tests
- Assert the single merged operation in OpenApiEndpointsIT

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Merge the users-service endpoint

Mirror of Task 1 for `asapp-users-service`. Field names differ (`userId, firstName, lastName, email, phoneNumber`); there is **no** `getUsersByUserId` sibling.

**Files:**
- Modify: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserRestAPIURL.java`
- Rename: `services/asapp-users-service/.../infrastructure/user/in/response/GetAllUsersResponse.java` → `GetUsersResponse.java`
- Delete: `services/asapp-users-service/.../infrastructure/user/in/response/GetUsersByIdsResponse.java`
- Modify: `services/asapp-users-service/.../infrastructure/user/mapper/UserMapper.java`
- Modify: `services/asapp-users-service/.../infrastructure/user/in/UserRestAPI.java`
- Modify: `services/asapp-users-service/.../infrastructure/user/in/UserRestController.java`
- Test: `services/asapp-users-service/.../infrastructure/config/OpenApiEndpointsIT.java`
- Test: `services/asapp-users-service/.../infrastructure/user/in/UserRestControllerIT.java`
- Test: `services/asapp-users-service/.../infrastructure/user/in/UserRestControllerDocumentationIT.java`
- Test: `services/asapp-users-service/.../infrastructure/user/UserE2EIT.java`
- Modify: `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc`
- Modify: `services/asapp-users-service/README.md`

**Interfaces:**
- Consumes: `ReadUserUseCase.getAllUsers() : List<User>` and `ReadUserUseCase.getUsersByIds(List<UUID>) : List<User>` (both unchanged).
- Produces: REST interface method `getUsers(List<UUID> ids) : List<GetUsersResponse>`; mapper `UserMapper.toGetUsersResponse(User) : GetUsersResponse`; OpenAPI operationId `getUsers` on `GET /api/users`.

- [ ] **Step 1: Write the failing OpenAPI regression test**

Add to `OpenApiEndpointsIT`:

```java
    @Test
    void ReturnsSingleGetUsersOperationWithIdsQueryParam_OnOpenApiDocs() {
        // When & Then
        restTestClient.get()
                      .uri("/v3/api-docs")
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> {
                          var body = response.getResponseBody();
                          assertThatJson(body).node("paths./api/users.get.operationId")
                                              .isEqualTo("getUsers");
                          assertThatJson(body).node("paths./api/users.get.parameters")
                                              .isArray()
                                              .hasSize(1);
                          assertThatJson(body).node("paths./api/users.get.parameters[0].name")
                                              .isEqualTo("ids");
                          assertThatJson(body).node("paths./api/users.get.parameters[0].in")
                                              .isEqualTo("query");
                      });
    }
```

Add `Single merged GET /api/users operation exposed with an optional ids query parameter` to the class Javadoc Coverage list.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd services/asapp-users-service && mvn -q test -Dtest=OpenApiEndpointsIT#ReturnsSingleGetUsersOperationWithIdsQueryParam_OnOpenApiDocs`
Expected: FAIL — operationId is `getAllUsers` or `getUsersByIds`, not `getUsers`.

- [ ] **Step 3: Remove the unused by-ids URL constants**

In `UserRestAPIURL.java`, delete:

```java
    public static final String USERS_GET_BY_IDS_PATH = "";
```
```java
    public static final String USERS_GET_BY_IDS_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_IDS_PATH;
```

Keep `USERS_GET_ALL_PATH`, `USERS_GET_ALL_FULL_PATH`, `USERS_IDS_PARAM`.

- [ ] **Step 4: Rename the response record and delete the by-ids record**

```bash
git mv services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/response/GetAllUsersResponse.java \
       services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/response/GetUsersResponse.java
```

In the renamed file:

```java
/**
 * Response for retrieving users.
 *
 * @param userId      the user's unique identifier
 * @param firstName   the user's first name
 * @param lastName    the user's last name
 * @param email       the user's email
 * @param phoneNumber the user's phone number
 * @since 0.2.0
 * @author attrigo
 */
public record GetUsersResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {}
```

(Use the renamed record's exact existing fields/order/Javadoc param names — adjust the snippet above only if they differ.)

Delete the by-ids record:

```bash
git rm services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/response/GetUsersByIdsResponse.java
```

- [ ] **Step 5: Update `UserMapper`**

Remove the import of `GetUsersByIdsResponse`; change `GetAllUsersResponse` → `GetUsersResponse`. Delete `toGetUsersByIdsResponse`. Rename `toGetAllUsersResponse`:

```java
    /**
     * Maps a domain {@link User} to a {@link GetUsersResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link GetUsersResponse}
     */
    @Mapping(target = "userId", source = "id")
    GetUsersResponse toGetUsersResponse(User user);
```

- [ ] **Step 6: Merge the REST interface method in `UserRestAPI`**

Fix imports: remove `GetAllUsersResponse` and `GetUsersByIdsResponse`, add `GetUsersResponse`, remove `import jakarta.validation.constraints.NotEmpty;` (keep `@Size`). Delete the `getUsersByIds` method. Replace `getAllUsers` with:

```java
    /**
     * Gets users, optionally filtered by their unique identifiers.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Users retrieved successfully.</li>
     * <li>400-BAD_REQUEST: ids is empty, exceeds 50 elements, or contains a malformed UUID.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param ids the optional list of user identifiers to filter by (1 to 50 elements); when {@code null} all users are returned
     * @return a {@link List} of {@link GetUsersResponse} containing the users found, or an empty list if none match; missing identifiers are silently omitted
     */
    @GetMapping(value = USERS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets users", description = "Retrieves users from the system. By default returns all registered users; when the `ids` query parameter is supplied, returns only the users whose identifiers are in the list. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The `ids` list, when present, must contain between 1 and 50 identifiers. If no users match, an empty array is returned.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = {
            @Content(schema = @Schema(implementation = GetUsersResponse.class)) })
    @ApiResponse(responseCode = "400", description = "User identifiers list is empty, exceeds 50 elements, or contains a malformed UUID", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetUsersResponse> getUsers(
            @RequestParam(name = USERS_IDS_PARAM, required = false) @Parameter(description = "Optional list of user identifiers to filter by; omit to return all users") @Size(min = 1, max = 50, message = "Users identifiers list must contain between 1 and 50 elements") List<UUID> ids);
```

- [ ] **Step 7: Merge the controller method in `UserRestController`**

Fix imports (remove `GetAllUsersResponse`, `GetUsersByIdsResponse`; add `GetUsersResponse`). Delete the `getUsersByIds` override. Replace `getAllUsers` with:

```java
    /**
     * Gets users, optionally filtered by their unique identifiers.
     *
     * @param ids the optional list of user identifiers to filter by; when {@code null} all users are returned
     * @return a {@link List} of {@link GetUsersResponse} containing the users found, or an empty list if none match; missing identifiers are silently omitted
     */
    @Override
    public List<GetUsersResponse> getUsers(List<UUID> ids) {
        var users = ids == null ? readUserUseCase.getAllUsers() : readUserUseCase.getUsersByIds(ids);

        return users.stream()
                    .map(userMapper::toGetUsersResponse)
                    .toList();
    }
```

- [ ] **Step 8: Migrate `UserRestControllerIT`**

1. Swap import `USERS_GET_BY_IDS_FULL_PATH` → `USERS_GET_ALL_FULL_PATH`.
2. Rename `class GetUsersByIds {` → `class GetUsers {`.
3. Replace every `get(USERS_GET_BY_IDS_FULL_PATH)` with `get(USERS_GET_ALL_FULL_PATH)` (3 occurrences: `…_MissingUsersIds`, `…_ExceedUsersIds`, `…_InvalidUserId`).
4. In `…_MissingUsersIds`, change `"Users identifiers list must not be empty"` → `"Users identifiers list must contain between 1 and 50 elements"`.
5. In `…_ExceedUsersIds`, change `"Users identifiers list must contain at most 50 elements"` → `"Users identifiers list must contain between 1 and 50 elements"`.

- [ ] **Step 9: Migrate `UserRestControllerDocumentationIT`**

1. Rename `class GetUsersByIds {` → `class GetUsers {`.
2. Rename the snippet `document("get-users-by-ids", …)` → `document("get-users", …)`; keep `queryParameters(...)`/`responseFields(...)`; set the `ids` parameter description to `"Optional list of user identifiers to filter by; omit to return all users"`.
3. Delete the entire `class GetAllUsers { … }` block (its `document("get-all-users", …)` test).
4. In the errors section, replace `get(USERS_GET_BY_IDS_FULL_PATH)` with `get(USERS_GET_ALL_FULL_PATH)` in `document("error-request-param-validation-failure", …)` and swap its import.

- [ ] **Step 10: Migrate `UserE2EIT`**

Merge `class GetUsersByIds` and `class GetAllUsers` into one `class GetUsers`. Swap import `USERS_GET_BY_IDS_FULL_PATH` → `USERS_GET_ALL_FULL_PATH` and replace every `path(USERS_GET_BY_IDS_FULL_PATH)` with `path(USERS_GET_ALL_FULL_PATH)` (5 occurrences). Port bodies unchanged except the constant, renaming to avoid collisions:

| Source | New method name |
|---|---|
| `GetAllUsers.ReturnsStatusOKAndBodyWithFoundUsers_UsersExist` (no `ids`) | `ReturnsStatusOKAndBodyWithAllUsers_NoIdsFilter` |
| `GetUsersByIds.ReturnsStatusOKAndBodyWithFoundUsers_AllUsersExist` | `ReturnsStatusOKAndBodyWithFoundUsers_AllIdsExist` |
| `GetUsersByIds.ReturnsStatusOKAndBodyWithFoundUsers_SomeUsersExist` | `ReturnsStatusOKAndBodyWithFoundUsers_SomeIdsExist` |
| `GetUsersByIds.ReturnsStatusOKAndBodyWithFoundUsersOnce_DuplicateIds` | `ReturnsStatusOKAndBodyWithFoundUsersOnce_DuplicateIds` |
| `GetAllUsers.ReturnsStatusOKAndEmptyBody_UsersNotExist` (empty DB) | `ReturnsStatusOKAndEmptyBody_NoIdsFilterAndUsersNotExist` |
| `GetUsersByIds.ReturnsStatusOKAndEmptyBody_UsersNotExist` (unknown `ids`) | `ReturnsStatusOKAndEmptyBody_IdsNotExist` |
| `GetUsersByIds.ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` | `ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader` |

Drop `GetAllUsers.ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader`. Update the class Javadoc Coverage list behaviorally.

- [ ] **Step 11: Merge the `api-guide.adoc` sections**

Delete the `[[resources-users-get-by-ids]] === Get Users by IDs` block. Replace the `[[resources-users-get-all]] === Get All Users` block with:

```adoc
[[resources-users-get]]
=== Get Users

Retrieves users from the system. By default returns all registered users; when the `ids` query parameter is supplied, returns only the users whose identifiers are in the list.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated server-side. The `ids` list, when present, must contain between 1 and 50 identifiers.
If no users match, an empty array is returned.

operation::get-users[snippets='query-parameters,http-request,response-fields,curl-request,http-response']
```

Keep this prose byte-for-byte equal to the Step 6 `@Operation(description)`.

- [ ] **Step 12: Reword the README**

In `services/asapp-users-service/README.md`:

- Line ~32 bullet: `- **Get All Users**: List all user profiles without tasks` → `- **Get Users**: List all user profiles (without tasks), optionally filtered by a list of IDs`
- Endpoint table row: `| GET    | `/api/users`      | Get all users               | ✅             |` → `| GET    | `/api/users`      | Get users (optionally by IDs) | ✅             |`

- [ ] **Step 13: Run the full verification gate (green)**

Run: `cd services/asapp-users-service && mvn clean verify`
Expected: BUILD SUCCESS — `OpenApiEndpointsIT` passes, the `GetUsers` failures assert the combined message, and Asciidoctor resolves `operation::get-users`.

- [ ] **Step 14: Commit**

```bash
git add libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserRestAPIURL.java \
        services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user \
        services/asapp-users-service/src/test/java/com/attrigo/asapp/users/infrastructure \
        services/asapp-users-service/src/docs/asciidoc/api-guide.adoc \
        services/asapp-users-service/README.md
git commit -m "refactor(users): merge get-all and get-by-ids into one endpoint

- Fold getUsersByIds into getUsers with an optional ids filter so OpenAPI exposes a single GET /api/users operation
- Validate ids with @Size(min=1, max=50); drop @NotEmpty and the params=ids/!ids split
- Rename GetAllUsersResponse to GetUsersResponse and remove GetUsersByIdsResponse
- Remove the unused USERS_GET_BY_IDS path constants
- Merge the api-guide section, reword the README row, and consolidate the web-tier tests
- Assert the single merged operation in OpenApiEndpointsIT

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Full-stack verification (developer-run)

**Files:** none (verification only).

- [ ] **Step 1: Reactor build**

Run: `mvn clean install`
Expected: BUILD SUCCESS across all 10 modules (confirms `asapp-commons-url` consumers compile after the constant removals — `asapp-authentication-service` included, proving it was untouched and unaffected).

- [ ] **Step 2: Confirm the merged Swagger UI by eye (optional)**

Run the stack (`docker-compose up -d`), open `http://localhost:8081/asapp-tasks-service/swagger-ui/index.html` and `http://localhost:8082/asapp-users-service/swagger-ui/index.html`. Expected: a single `GET /api/tasks` and `GET /api/users` operation, each with an optional `ids` query parameter — no duplicate/overwritten entry.

- [ ] **Step 3: Re-run the JMeter load tests (no edits expected)**

With the stack up, run both plans (developer-run):

```bash
cd tools/jmeter
./run-regression.sh
./run-stress.sh
```

Expected: all samplers green, including "Get Tasks/Users By Ids" (`/api/...?ids=…`) and "Get All Tasks/Users" (`/api/...`) — the URLs are unchanged by the merge, so no `.jmx` edits are required. If any sampler fails, that is a real regression to investigate, not a JMeter edit.

- [ ] **Step 4: Tick the TODO item**

In `TODO.md`, mark the v0.4.0 → Quick Wins → Functional Improvements item `Join tasks and users getAll and getAllByIds endpoints into one unique endpoint (avoid swagger collision)` as done (`[X]`). Commit as release housekeeping (separate from the two refactor commits).

---

## Self-Review

**Spec coverage** (spec → task):
- §4.1–4.3 single endpoint + controller branch → T1/T2 Steps 6–7. ✓
- §4.2 `@Size(min=1,max=50)`, drop `@NotEmpty`, optional → T1/T2 Step 6; empty-`ids` 400 preserved & re-asserted → Step 8. ✓
- §4.4 DTO rename + delete by-ids record → Step 4. ✓
- §4.6 merged `@Operation` + `400` ApiResponse → Step 6. ✓
- §4.7 remove by-ids constants → Step 3. ✓
- §5 no app/domain/persistence change → enforced by Global Constraints; no task touches them. ✓
- §6 mapper rename/delete → Step 5. ✓
- §7.1 ControllerIT consolidation + message change → Step 8; §7.2 DocumentationIT snippet merge → Step 9; §7.3 E2EIT merge → Step 10; §7.4 OpenAPI single operation → Step 1 + Step 13. ✓
- §8.1 adoc merge → Step 11; §8.2 README reword → Step 12; §8.3 JMeter verify-only → T3 Step 3; §8.4 TODO tick → T3 Step 4. ✓
- §9 auth-service untouched → verified by T3 Step 1 reactor build. ✓
- §10 validation gate → T1/T2 Step 13, T3 Step 1. ✓

**Placeholder scan:** No TBD/TODO/"handle edge cases"/"similar to Task N". Production code, the OpenAPI test, adoc, and README rows are shown in full; test-file migrations are exact mechanical edits referencing real symbol names. ✓

**Type consistency:** `getTasks(List<UUID>) : List<GetTasksResponse>` / `getUsers(List<UUID>) : List<GetUsersResponse>` used consistently across interface, controller, mapper (`toGetTasksResponse`/`toGetUsersResponse`), and the OpenAPI operationId assertions (`getTasks`/`getUsers`). Constant names (`*_GET_ALL_FULL_PATH`, `*_IDS_PARAM`) match `asapp-commons-url`. ✓
