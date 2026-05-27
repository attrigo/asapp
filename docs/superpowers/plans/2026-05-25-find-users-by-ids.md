# Find Users by IDs — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `GET /api/users?ids=u1,u2,...` batch read operation on `asapp-users-service` that returns users whose IDs are in the provided list, with server-side dedup and a 1–50 size cap.

**Architecture:** Hexagonal slice that mirrors the post-review `asapp-tasks-service` `getTasksByIds` flow end-to-end (commits `a2fa5c77` → `55fc26ac` → `5361c949` → `2ec0bad2`). A new use case method delegates to a new outbound port method, implemented by an adapter that calls Spring Data's inherited `ListCrudRepository.findAllById`. A new query-string filter on the existing `/api/users` collection is routed via Spring `@GetMapping(params = ...)` alongside the existing `getAllUsers`. **No task-ID enrichment** — the response mirrors `GetAllUsersResponse`, not `GetUserByIdResponse`.

**Tech Stack:** Spring Boot 4.0 · Spring MVC · Spring Data JDBC · Spring Security · MapStruct · Spring REST Docs · JUnit 5 · Mockito (BDD) · AssertJ · Testcontainers (PostgreSQL).

**Source spec:** `docs/superpowers/specs/2026-05-25-find-users-by-ids-design.md`

**Canonical reference (mirror conventions verbatim):** the current state of these tasks-service files:

- `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/tasks/TaskRestAPIURL.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/ReadTaskUseCase.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskService.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/application/task/out/TaskRepository.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/out/TaskRepositoryAdapter.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestAPI.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestController.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/in/response/GetTasksByIdsResponse.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/task/mapper/TaskMapper.java`
- `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/application/task/in/service/ReadTaskServiceTests.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java`
- `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/TaskE2EIT.java`
- `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc`

**Agent ownership convention:** Each task is tagged with an `**Agent:**` line. When executing under `subagent-driven-development`, dispatch a fresh subagent of that type. Plain tasks (URL constants, AsciiDoc) can go to `spring-boot-developer` / `documentation-engineer`. Prefer custom subagents from `.claude/agents/` over `general-purpose`.

---

## File Inventory

**Modified:**

- `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/users/UserRestAPIURL.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/ReadUserUseCase.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/out/UserRepository.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/UserRepositoryAdapter.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/UserMapper.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestAPI.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestController.java`
- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`
- `services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/service/ReadUserServiceTests.java`
- `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerIT.java`
- `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java`
- `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java`
- `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc`
- `TODO.md`

**Created:**

- `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUsersByIdsResponse.java`

---

## Task 1: URL constants

**Agent:** `spring-boot-developer`

**Files:**

- Modify: `libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/users/UserRestAPIURL.java`

- [ ] **Step 1: Add the relative path constant**

Insert `USERS_GET_BY_IDS_PATH` after `USERS_GET_BY_ID_PATH` and before `USERS_GET_ALL_PATH`:

```java
    public static final String USERS_GET_BY_IDS_PATH = "";
```

- [ ] **Step 2: Add the full path constant**

Insert `USERS_GET_BY_IDS_FULL_PATH` after `USERS_GET_BY_ID_FULL_PATH` and before `USERS_GET_ALL_FULL_PATH`:

```java
    public static final String USERS_GET_BY_IDS_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_IDS_PATH;
```

- [ ] **Step 3: Add the param constant at the bottom**

Insert `USERS_IDS_PARAM` immediately before the `private UserRestAPIURL() {}` line:

```java
    public static final String USERS_IDS_PARAM = "ids";
```

The final file should match this structure (verify ordering against `TaskRestAPIURL.java`):

```java
public class UserRestAPIURL {

    public static final String USERS_ROOT_PATH = "/api/users";

    public static final String USERS_GET_BY_ID_PATH = "/{id}";

    public static final String USERS_GET_BY_IDS_PATH = "";

    public static final String USERS_GET_ALL_PATH = "";

    public static final String USERS_CREATE_PATH = "";

    public static final String USERS_UPDATE_BY_ID_PATH = "/{id}";

    public static final String USERS_DELETE_BY_ID_PATH = "/{id}";

    public static final String USERS_GET_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_ID_PATH;

    public static final String USERS_GET_BY_IDS_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_IDS_PATH;

    public static final String USERS_GET_ALL_FULL_PATH = USERS_ROOT_PATH + USERS_GET_ALL_PATH;

    public static final String USERS_CREATE_FULL_PATH = USERS_ROOT_PATH + USERS_CREATE_PATH;

    public static final String USERS_UPDATE_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_UPDATE_BY_ID_PATH;

    public static final String USERS_DELETE_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_DELETE_BY_ID_PATH;

    public static final String USERS_IDS_PARAM = "ids";

    private UserRestAPIURL() {}

}
```

- [ ] **Step 4: Run the libs build to confirm it compiles**

```
mvn -pl libs/asapp-commons-url -am compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```
git add libs/asapp-commons-url/src/main/java/com/bcn/asapp/url/users/UserRestAPIURL.java
git commit -m "feat(commons): add ids query-parameter constant for users"
```

---

## Task 2: Repository port + adapter

**Agent:** `spring-boot-developer`

**Files:**

- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/out/UserRepository.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/UserRepositoryAdapter.java`

- [ ] **Step 1: Add `findByIds` to the repository port**

Insert into `UserRepository.java` **between `findById` and `findAll`** (matches method ordering convention from tasks-service: byId → byIds → all):

```java
    /**
     * Finds users by their unique identifiers.
     *
     * @param userIds the collection of user identifiers
     * @return a {@link Collection} of {@link User} entities found; missing identifiers are silently omitted
     */
    Collection<User> findByIds(Collection<UserId> userIds);
```

- [ ] **Step 2: Implement `findByIds` on the adapter**

Insert into `UserRepositoryAdapter.java` **between `findById` and `findAll`**:

```java
    /**
     * Finds users by their unique identifiers.
     *
     * @param userIds the collection of user identifiers
     * @return a {@link Collection} of {@link User} entities found; missing identifiers are silently omitted
     */
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

No new imports needed — `Collection`, `UserId`, `User`, `UserMapper`, `JdbcUserRepository` are already imported.

- [ ] **Step 3: Run the users-service compile to confirm the interface is satisfied**

```
mvn -pl services/asapp-users-service -am compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit (deferred — bundle with Task 3)**

This task's changes only compile because no caller exercises them yet. Defer the commit until the use case + service add their callers in Task 3, so the commit ships a self-contained slice.

---

## Task 3: Use case + service with unit tests (TDD)

**Agent:** `test-automator` for Steps 1–2 (write tests), `spring-boot-developer` for Steps 3–6 (implement & green)

**Files:**

- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/application/user/in/service/ReadUserServiceTests.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/ReadUserUseCase.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/application/user/in/service/ReadUserService.java`

- [ ] **Step 1: Update the class Javadoc coverage list**

In `ReadUserServiceTests.java`, the existing class Javadoc has a Coverage list. Insert three bullets so the final list reads (replace the existing list):

```java
/**
 * Tests {@link ReadUserService} single, collection, and identifier-list retrieval with task enrichment.
 * <p>
 * Coverage:
 * <li>Retrieval failures propagate for all query strategies (by ID, by IDs, all users)</li>
 * <li>Returns empty result when no users match query criteria</li>
 * <li>Returns single user when queried by unique identifier</li>
 * <li>Returns user collection when queried by a list of identifiers</li>
 * <li>Deduplicates duplicate identifiers before querying</li>
 * <li>Throws when the input list contains a null identifier</li>
 * <li>Returns user collection when querying all users</li>
 * <li>Enriches user data with associated task identifiers via external gateway</li>
 * <li>Propagates task gateway failures to the caller</li>
 */
```

- [ ] **Step 2: Add the new imports needed by the tests**

Add to the imports block of `ReadUserServiceTests.java` (alongside existing imports — preserve alphabetical order within each group):

```java
import java.util.ArrayList;
import java.util.Set;
```

- [ ] **Step 3: Write the failing unit tests**

Add a new `@Nested class GetUsersByIds` to `ReadUserServiceTests.java`, **positioned between `GetUserById` and `GetAllUsers`** (matches the method ordering convention):

```java
    @Nested
    class GetUsersByIds {

        @Test
        void ReturnsUsers_AllUsersExist() {
            // Given
            var user1 = aUser();
            var user2 = aUserBuilder().withUserId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withFirstName("FirstName 2")
                                      .withLastName("LastName 2")
                                      .withEmail("user2@asapp.com")
                                      .withPhoneNumber("666 666 666")
                                      .build();
            var userId1 = user1.getId();
            var userId2 = user2.getId();
            var users = List.of(user1, user2);
            var userIds = Set.of(userId1, userId2);
            var userIdValues = List.of(userId1.value(), userId2.value());

            given(userRepository.findByIds(userIds)).willReturn(users);

            // When
            var actual = readUserService.getUsersByIds(userIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(user1, user2);

            then(userRepository).should(times(1))
                                .findByIds(userIds);
        }

        @Test
        void ReturnsFoundUsers_SomeUsersExist() {
            // Given
            var user1 = aUser();
            var user2 = aUserBuilder().withUserId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withFirstName("FirstName 2")
                                      .withLastName("LastName 2")
                                      .withEmail("user2@asapp.com")
                                      .withPhoneNumber("666 666 666")
                                      .build();
            var userId1 = user1.getId();
            var userId2 = user2.getId();
            var userIds = Set.of(userId1, userId2);
            var userIdValues = List.of(userId1.value(), userId2.value());

            given(userRepository.findByIds(userIds)).willReturn(List.of(user1));

            // When
            var actual = readUserService.getUsersByIds(userIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(user1);

            then(userRepository).should(times(1))
                                .findByIds(userIds);
        }

        @Test
        void ReturnsEmptyList_UsersNotExist() {
            // Given
            var user1 = aUser();
            var user2 = aUserBuilder().withUserId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withFirstName("FirstName 2")
                                      .withLastName("LastName 2")
                                      .withEmail("user2@asapp.com")
                                      .withPhoneNumber("666 666 666")
                                      .build();
            var userId1 = user1.getId();
            var userId2 = user2.getId();
            var userIds = Set.of(userId1, userId2);
            var userIdValues = List.of(userId1.value(), userId2.value());

            given(userRepository.findByIds(userIds)).willReturn(List.of());

            // When
            var actual = readUserService.getUsersByIds(userIdValues);

            // Then
            assertThat(actual).isEmpty();

            then(userRepository).should(times(1))
                                .findByIds(userIds);
        }

        @Test
        void ReturnsUsersOnce_DuplicateIds() {
            // Given
            var user1 = aUser();
            var user2 = aUserBuilder().withUserId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withFirstName("FirstName 2")
                                      .withLastName("LastName 2")
                                      .withEmail("user2@asapp.com")
                                      .withPhoneNumber("666 666 666")
                                      .build();
            var userId1 = user1.getId();
            var userId2 = user2.getId();
            var users = List.of(user1, user2);
            var userIds = Set.of(userId1, userId2);
            var userIdValues = List.of(userId1.value(), userId1.value(), userId2.value());

            given(userRepository.findByIds(userIds)).willReturn(users);

            // When
            var actual = readUserService.getUsersByIds(userIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(user1, user2);

            then(userRepository).should(times(1))
                                .findByIds(userIds);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var userIdValue = UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04");
            var userIds = new ArrayList<UUID>();
            userIds.add(userIdValue);
            userIds.add(null);

            // When
            var actual = catchThrowable(() -> readUserService.getUsersByIds(userIds));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

    }
```

- [ ] **Step 4: Run unit tests, expect compile failure**

```
mvn -pl services/asapp-users-service test -Dtest=ReadUserServiceTests
```

Expected: `BUILD FAILURE` — compile error: `cannot find symbol method getUsersByIds(List<UUID>)` on `ReadUserService` (or `ReadUserUseCase`).

- [ ] **Step 5: Add `getUsersByIds` to the use case interface**

Insert into `ReadUserUseCase.java` **between `getUserById` and `getAllUsers`**:

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

- [ ] **Step 6: Implement the use case in `ReadUserService`**

Add the following import to `ReadUserService.java`:

```java
import java.util.stream.Collectors;
```

Insert **between `getUserById` and `getAllUsers`**:

```java
    /**
     * Retrieves users by their unique identifiers.
     *
     * @param ids the list of user identifiers; duplicates are deduped
     * @return a {@link List} of {@link User} entities found; missing ids are silently omitted
     * @throws IllegalArgumentException if any id is invalid
     */
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

Note: no `TasksGateway` interaction. This is the key difference from `getUserById`.

- [ ] **Step 7: Run unit tests, expect all green**

```
mvn -pl services/asapp-users-service test -Dtest=ReadUserServiceTests
```

Expected: `BUILD SUCCESS`. All `GetUsersByIds` tests pass; all previously passing tests still pass.

- [ ] **Step 8: Commit (bundles Tasks 2 + 3)**

Task 1 already committed the URL constants standalone, so this commit only ships the application slice + tests:

```
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/application services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out services/asapp-users-service/src/test/java/com/bcn/asapp/users/application
git commit -m "feat(users): add application slice for find users by ids

- Add findByIds outbound port and JDBC adapter using findAllById
- Add getUsersByIds use case with server-side dedup via Set
- Add unit tests covering happy path, partial misses, dedupe, empty, null"
```

---

## Task 4: Response DTO + mapper

**Agent:** `spring-boot-developer`

**Files:**

- Create: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUsersByIdsResponse.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/UserMapper.java`

- [ ] **Step 1: Create the response record**

Create `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUsersByIdsResponse.java`:

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

package com.bcn.asapp.users.infrastructure.user.in.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for retrieving users by a list of unique identifiers.
 *
 * @param userId      the user's unique identifier
 * @param firstName   the user's first name
 * @param lastName    the user's last name
 * @param email       the user's email
 * @param phoneNumber the user's phone number
 * @since 0.4.0
 * @author attrigo
 */
public record GetUsersByIdsResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        @JsonProperty("phone_number") String phoneNumber
) {}
```

Field set mirrors `GetAllUsersResponse` exactly (no `taskIds`). `@since 0.4.0` matches the current users-service POM version.

- [ ] **Step 2: Add the mapper method**

Add the `GetUsersByIdsResponse` import to `UserMapper.java`:

```java
import com.bcn.asapp.users.infrastructure.user.in.response.GetUsersByIdsResponse;
```

Insert into `UserMapper.java` **between `toGetUserByIdResponse` and `toGetAllUsersResponse`**:

```java
    /**
     * Maps a domain {@link User} to a {@link GetUsersByIdsResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link GetUsersByIdsResponse}
     */
    @Mapping(target = "userId", source = "id")
    GetUsersByIdsResponse toGetUsersByIdsResponse(User user);
```

- [ ] **Step 3: Compile to verify MapStruct generation**

```
mvn -pl services/asapp-users-service compile
```

Expected: `BUILD SUCCESS`. MapStruct generates `UserMapperImpl.toGetUsersByIdsResponse` in `target/generated-sources`.

- [ ] **Step 4: Commit**

```
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/response/GetUsersByIdsResponse.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/mapper/UserMapper.java
git commit -m "feat(users): add GetUsersByIdsResponse DTO and mapper method"
```

---

## Task 5: ConstraintViolationException handler

**Agent:** `spring-boot-developer`

**Files:**

- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`

Adds the handler that turns `@Validated` constraint violations on `@RequestParam` arguments into RFC 7807 `ProblemDetail` responses. The users-service handler does not have this yet (its tasks-service counterpart was added when the tasks batch endpoint shipped). Without this, Task 7's WebMvc tests would fall back to Spring's default constraint-violation handling and our assertions would not match.

- [ ] **Step 1: Add the new imports**

Add these to `GlobalExceptionHandler.java` (preserve alphabetical order within groups):

```java
import java.util.Set;
```

```java
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
```

```java
import org.springframework.web.bind.annotation.ExceptionHandler;
```

- [ ] **Step 2: Add the handler method**

Insert **inside the `// 400 BAD REQUEST - Validation Errors` section, after `handleIllegalArgumentException`** (so the order is: `handleMethodArgumentNotValid` → `handleIllegalArgumentException` → `handleConstraintViolationException`):

```java
    /**
     * Handles constraint violations from {@code @Validated} request parameter constraints.
     * <p>
     * Thrown by the Bean Validation framework when method-level parameter constraints (e.g., {@code @NotEmpty}, {@code @Size} on {@code @RequestParam}) are
     * violated. Returns a 400 Bad Request response.
     *
     * @param ex the {@link ConstraintViolationException}
     * @return a {@link ResponseEntity} containing the error details
     */
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
```

- [ ] **Step 3: Add the `buildInvalidParameters` overload**

Insert at the bottom of the file, **after the existing `buildInvalidParameters(List<FieldError>)` helper**:

```java
    /**
     * Builds a list of invalid parameter details from constraint violations.
     *
     * @param violations the set of {@link ConstraintViolation} from bean validation
     * @return a {@link List} of {@link InvalidRequestParameter} containing error details
     */
    private List<InvalidRequestParameter> buildInvalidParameters(Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                         .map(v -> {
                             var path = v.getPropertyPath()
                                         .toString();
                             var field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                             return new InvalidRequestParameter("", field, v.getMessage());
                         })
                         .toList();
    }
```

Conventions (verbatim from tasks-service post-`2ec0bad2`):

- `detail = ex.getLocalizedMessage()` — gives `"getUsersByIds.ids: <message>"` shape.
- `InvalidRequestParameter.entity = ""` (empty) — distinguishes query-param violations from request-body field errors.
- Field extraction uses `indexOf('.')` (first dot), **not** `lastIndexOf('.')`. For a flat parameter like `getUsersByIds.ids`, the result is `ids`; for nested paths like `body.address.zipCode`, the result is `address.zipCode`. This was the explicit fix in commit `2ec0bad2`.

- [ ] **Step 4: Compile**

```
mvn -pl services/asapp-users-service compile
```

Expected: `BUILD SUCCESS`. (No existing tests reference the new handler — they continue to pass; the new handler is dead code until Task 6 wires `@Validated`.)

- [ ] **Step 5: Commit (deferred — bundle with Task 7)**

The handler is added together with its first caller (the `@Validated` controller in Task 6) and the IT tests that exercise both (Task 7), so the bundled commit ships a meaningful slice.

---

## Task 6: REST API + controller method (compile-green, no tests yet)

**Agent:** `api-designer` for Step 1 (interface annotations), `spring-boot-developer` for Steps 2–4 (controller impl)

**Files:**

- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestAPI.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestController.java`

- [ ] **Step 1a: Add `params = "!ids"` to the existing `getAllUsers` mapping**

Locate this method in `UserRestAPI.java` (currently around lines 102–123) and change only the `@GetMapping` annotation to include `params = "!ids"`:

```java
    @GetMapping(value = USERS_GET_ALL_PATH, params = "!ids", produces = "application/json")
```

Everything else about `getAllUsers` stays unchanged.

- [ ] **Step 1b: Add imports to `UserRestAPI.java`**

Add the static import:

```java
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_IDS_PARAM;
```

Add the validation imports:

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
```

Add the response-DTO import:

```java
import com.bcn.asapp.users.infrastructure.user.in.response.GetUsersByIdsResponse;
```

Add the Spring web import:

```java
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 1c: Add the `getUsersByIds` interface method**

Insert in `UserRestAPI.java` **between `getUserById` and `getAllUsers`**:

```java
    /**
     * Gets users by their unique identifiers.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Users found.</li>
     * <li>400-BAD_REQUEST: ids is empty, exceeds 50 elements, or contains a malformed UUID.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param ids the list of user identifiers (1 to 50 elements)
     * @return a {@link List} of {@link GetUsersByIdsResponse} containing the users found; missing identifiers are silently omitted
     */
    @GetMapping(value = USERS_GET_ALL_PATH, params = USERS_IDS_PARAM, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets users by their unique identifiers", description = "Retrieves a list of users whose identifiers are in the provided list. This endpoint requires authentication. Missing identifiers are silently omitted; the response order is not guaranteed. Duplicate identifiers are deduplicated server-side. The list must contain between 1 and 50 identifiers.")
    @ApiResponse(responseCode = "200", description = "Users found", content = { @Content(schema = @Schema(implementation = GetUsersByIdsResponse.class)) })
    @ApiResponse(responseCode = "400", description = "User identifiers list is empty, exceeds 50 elements, or contains a malformed UUID", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetUsersByIdsResponse> getUsersByIds(
            @RequestParam(USERS_IDS_PARAM) @Parameter(description = "List of user identifiers") @NotEmpty(message = "Users identifiers list must not be empty") @Size(max = 50, message = "Users identifiers list must contain at most 50 elements") List<UUID> ids);
```

Annotation order on the param is **`@RequestParam → @Parameter → @NotEmpty → @Size`** — verbatim from `TaskRestAPI.getTasksByIds`.

- [ ] **Step 2: Add `@Validated` to `UserRestController`**

Open `UserRestController.java`. Add the import:

```java
import org.springframework.validation.annotation.Validated;
```

Add `@Validated` directly under `@RestController` (the project rule places it in the "Component role" annotation group):

```java
@RestController
@Validated
public class UserRestController implements UserRestAPI {
```

- [ ] **Step 3: Implement `getUsersByIds` in `UserRestController`**

Add the `GetUsersByIdsResponse` import:

```java
import com.bcn.asapp.users.infrastructure.user.in.response.GetUsersByIdsResponse;
```

Insert **between `getUserById` and `getAllUsers`**:

```java
    /**
     * Gets users by their unique identifiers.
     *
     * @param ids the list of user identifiers
     * @return a {@link List} of {@link GetUsersByIdsResponse} containing the users found; missing identifiers are silently omitted
     */
    @Override
    public List<GetUsersByIdsResponse> getUsersByIds(List<UUID> ids) {
        return readUserUseCase.getUsersByIds(ids)
                              .stream()
                              .map(userMapper::toGetUsersByIdsResponse)
                              .toList();
    }
```

- [ ] **Step 4: Compile**

```
mvn -pl services/asapp-users-service compile
```

Expected: `BUILD SUCCESS`. (No tests yet for this layer — Task 7 adds them.)

- [ ] **Step 5: Commit (deferred — bundle with Task 7)**

The controller wiring lands together with its WebMvc tests and the handler so the commit ships a tested slice.

---

## Task 7: Validation WebMvc tests (TDD)

**Agent:** `test-automator`

**Files:**

- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerIT.java`

- [ ] **Step 1: Update the class Javadoc coverage list**

Add `"Validates request parameter constraints (ids non-empty, ids size cap, ids malformed UUID)"` and update the endpoint coverage line to include "GET by IDs":

```java
/**
 * Tests {@link UserRestController} request validation and error responses.
 * <p>
 * Coverage:
 * <li>Validates path parameter format (UUID required for user IDs)</li>
 * <li>Validates request parameter constraints (ids non-empty, ids size cap, ids malformed UUID)</li>
 * <li>Validates request content type (JSON required for POST/PUT operations)</li>
 * <li>Validates request body presence and structure</li>
 * <li>Validates mandatory field constraints (first name, last name, email, phone number)</li>
 * <li>Validates email format and phone number format patterns</li>
 * <li>Returns RFC 7807 Problem Details for all validation failures</li>
 * <li>Tests all HTTP endpoints (GET by ID, GET by IDs, POST, PUT, DELETE)</li>
 */
```

- [ ] **Step 2: Add new imports to `UserRestControllerIT.java`**

Add the static imports (only those not already present):

```java
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_IDS_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_IDS_PARAM;
```

Add the JDK import:

```java
import java.util.stream.IntStream;
```

- [ ] **Step 3: Write the WebMvc validation tests**

Add a new `@Nested class GetUsersByIds` **between `GetUserById` and `CreateUser`** (matches method order: byId → byIds → all → create → update → delete; `getAllUsers` has no WebMvc validation IT, so byIds slots right after byId):

```java
    @Nested
    class GetUsersByIds {

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingUsersIds() {
            // Given
            var requestBuilder = get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, "");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "getUsersByIds.ids: Users identifiers list must not be empty")
                                                 .containsEntry("instance", "/api/users");
                         //@formatter:off
                             assertThatJson(json).inPath("errors")
                                                 .isArray()
                                                 .containsOnly(
                                                         Map.of("entity", "", "field", "ids", "message", "Users identifiers list must not be empty")
                                                 );
                             //@formatter:on
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_ExceedUsersIds() {
            // Given
            var userIds = IntStream.range(0, 51)
                                   .mapToObj(_ -> UUID.randomUUID())
                                   .map(UUID::toString)
                                   .reduce((a, b) -> a + "," + b)
                                   .orElseThrow();
            var requestBuilder = get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, userIds);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "getUsersByIds.ids: Users identifiers list must contain at most 50 elements")
                                                 .containsEntry("instance", "/api/users");
                         //@formatter:off
                             assertThatJson(json).inPath("errors")
                                                 .isArray()
                                                 .containsOnly(
                                                         Map.of("entity", "", "field", "ids", "message", "Users identifiers list must contain at most 50 elements")
                                                 );
                             //@formatter:on
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId() {
            // Given
            var userIds = "1";
            var requestBuilder = get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, userIds);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to convert 'ids' with value: '1'")
                                                                .containsEntry("instance", "/api/users"));
        }

    }
```

- [ ] **Step 4: Run tests, expect green**

```
mvn -pl services/asapp-users-service test -Dtest=UserRestControllerIT
```

Expected: `BUILD SUCCESS`. All three new tests in `GetUsersByIds` pass; all previously passing tests still pass.

If a test fails with a 500 status or a missing `errors[]` array, confirm Task 5's handler is in place (`ConstraintViolationException` must be caught by `GlobalExceptionHandler.handleConstraintViolationException`, not the default fallback).

- [ ] **Step 5: Commit (bundles Tasks 5 + 6 + 7)**

```
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestAPI.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/in/UserRestController.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerIT.java
git commit -m "feat(users): expose GET /api/users?ids= batch find endpoint

- Add getUsersByIds method to UserRestAPI with OpenAPI annotations
- Add params=\"!ids\" to getAllUsers to disambiguate the route
- Add @Validated on UserRestController to enable param-level validation
- Add handleConstraintViolationException in GlobalExceptionHandler
- Add WebMvc tests covering empty, too-many, and malformed UUID"
```

---

## Task 8: RestDocs documentation test

**Agent:** `documentation-engineer`

**Files:**

- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java`

Adds the success snippet for the new endpoint and the request-parameter validation snippet for the api-guide error section.

- [ ] **Step 1: Add the new imports**

Add the static imports (only those not already present):

```java
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_IDS_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_IDS_PARAM;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
```

Add the response-DTO import:

```java
import com.bcn.asapp.users.infrastructure.user.in.response.GetUsersByIdsResponse;
```

- [ ] **Step 2: Add the new `@Nested class GetUsersByIds`**

Insert **between `GetUserById` and `GetAllUsers`**:

```java
    @Nested
    class GetUsersByIds {

        @Test
        void DocumentsGetUsersByIds() throws Exception {
            // Given
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var firstNameValue = user.getFirstName()
                                     .value();
            var lastNameValue = user.getLastName()
                                    .value();
            var emailValue = user.getEmail()
                                 .value();
            var phoneNumberValue = user.getPhoneNumber()
                                       .value();
            var response = new GetUsersByIdsResponse(userIdValue, firstNameValue, lastNameValue, emailValue, phoneNumberValue);

            given(readUserUseCase.getUsersByIds(anyList())).willReturn(List.of(user));
            given(userMapper.toGetUsersByIdsResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, userIdValue.toString())
                                                           .accept(APPLICATION_JSON)
                                                           .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-users-by-ids",
                               requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                               queryParameters(parameterWithName("ids").description("Comma-separated list of user identifiers (1 to 50 elements)")),
                               responseFields(
                                       fieldWithPath("[].user_id").description("The user's unique identifier"),
                                       fieldWithPath("[].first_name").description("The user's first name"),
                                       fieldWithPath("[].last_name").description("The user's last name"),
                                       fieldWithPath("[].email").description("The user's email address"),
                                       fieldWithPath("[].phone_number").description("The user's phone number"))
                       )
                   // @formatter:on
                   );
        }

    }
```

- [ ] **Step 3: Add the request-parameter validation error snippet test**

Inside the existing `@Nested class Errors`, insert `DocumentsRequestParamValidationFailure` **between `DocumentsPathVariableValidationFailure` and `DocumentsRequestBodyValidationFailure`**:

```java
        @Test
        void DocumentsRequestParamValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(get(USERS_GET_BY_IDS_FULL_PATH).param(USERS_IDS_PARAM, ""))
                   .andExpect(status().isBadRequest())
                   .andDo(
                   // @formatter:off
                       document("error-request-param-validation-failure",
                           relaxedResponseFields(
                               fieldWithPath("title").description("Short summary of the problem type"),
                               fieldWithPath("status").description("HTTP status code"),
                               fieldWithPath("detail").description("Human-readable explanation of the problem"),
                               fieldWithPath("errors").description("List of validation errors"),
                               fieldWithPath("errors[].entity").description("Always empty for request-parameter violations"),
                               fieldWithPath("errors[].field").description("Name of the request parameter that failed validation"),
                               fieldWithPath("errors[].message").description("Validation error message")
                           )
                       )
                       // @formatter:on
                   );
        }
```

- [ ] **Step 4: Run the documentation tests**

```
mvn -pl services/asapp-users-service test -Dtest=UserRestControllerDocumentationIT
```

Expected: `BUILD SUCCESS`. The build generates `target/generated-snippets/get-users-by-ids/` (with `http-request.adoc`, `http-response.adoc`, `request-headers.adoc`, `query-parameters.adoc`, `curl-request.adoc`, `response-fields.adoc`) and `target/generated-snippets/error-request-param-validation-failure/` (with `http-response.adoc`, `response-fields.adoc`).

- [ ] **Step 5: Commit**

```
git add services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java
git commit -m "test(users): document GET /api/users?ids= endpoint with RestDocs"
```

---

## Task 9: End-to-end integration test

**Agent:** `test-automator`

**Files:**

- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java`

- [ ] **Step 1: Update the class Javadoc coverage list**

Add the bullet `<li>Retrieves users by identifier list, omitting unknown ids and deduplicating</li>` between the existing single-user retrieval line and the "Retrieves all users" line.

- [ ] **Step 2: Add the new imports**

Add the static imports:

```java
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_IDS_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_IDS_PARAM;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUserBuilder;
```

(`aUserBuilder` and several others may already be imported — verify and skip duplicates.)

Add the response-DTO import:

```java
import com.bcn.asapp.users.infrastructure.user.in.response.GetUsersByIdsResponse;
```

- [ ] **Step 3: Add the new `@Nested class GetUsersByIds`**

Insert **between `GetUserById` and `GetAllUsers`**:

```java
    @Nested
    class GetUsersByIds {

        @Test
        void ReturnsStatusOKAndBodyWithFoundUsers_AllUsersExist() {
            // Given
            var user1 = aUserBuilder().withEmail("user1@asapp.com")
                                      .buildJdbc();
            var user2 = aUserBuilder().withEmail("user2@asapp.com")
                                      .buildJdbc();
            var createdUser1 = createUser(user1);
            var createdUser2 = createUser(user2);
            var userId1 = createdUser1.id();
            var userId2 = createdUser2.id();
            var response1 = new GetUsersByIdsResponse(userId1, createdUser1.firstName(), createdUser1.lastName(), createdUser1.email(),
                    createdUser1.phoneNumber());
            var response2 = new GetUsersByIdsResponse(userId2, createdUser2.firstName(), createdUser2.lastName(), createdUser2.email(),
                    createdUser2.phoneNumber());

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(USERS_GET_BY_IDS_FULL_PATH)
                                                                    .queryParam(USERS_IDS_PARAM, userId1 + "," + userId2)
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetUsersByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).containsExactlyInAnyOrder(response1, response2);
        }

        @Test
        void ReturnsStatusOKAndBodyWithFoundUsers_SomeUsersExist() {
            // Given
            var createdUser = createUser();
            var userId1 = createdUser.id();
            var userId2 = UUID.fromString("b344ecdf-d5bf-4e1f-84d9-c3a023dc0414");
            var response = new GetUsersByIdsResponse(userId1, createdUser.firstName(), createdUser.lastName(), createdUser.email(), createdUser.phoneNumber());

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(USERS_GET_BY_IDS_FULL_PATH)
                                                                    .queryParam(USERS_IDS_PARAM, userId1 + "," + userId2)
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetUsersByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).containsExactlyInAnyOrder(response);
        }

        @Test
        void ReturnsStatusOKAndEmptyBody_UsersNotExist() {
            // Given
            var userId1 = UUID.fromString("b344ecdf-d5bf-4e1f-84d9-c3a023dc0414");
            var userId2 = UUID.fromString("68699b10-b665-4378-baea-a44b4be287f9");

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(USERS_GET_BY_IDS_FULL_PATH)
                                                                    .queryParam(USERS_IDS_PARAM, userId1 + "," + userId2)
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetUsersByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsStatusOKAndBodyWithFoundUsersOnce_DuplicateIds() {
            // Given
            var user1 = aUserBuilder().withEmail("user1@asapp.com")
                                      .buildJdbc();
            var user2 = aUserBuilder().withEmail("user2@asapp.com")
                                      .buildJdbc();
            var createdUser1 = createUser(user1);
            var createdUser2 = createUser(user2);
            var userId1 = createdUser1.id();
            var userId2 = createdUser2.id();
            var response1 = new GetUsersByIdsResponse(userId1, createdUser1.firstName(), createdUser1.lastName(), createdUser1.email(),
                    createdUser1.phoneNumber());
            var response2 = new GetUsersByIdsResponse(userId2, createdUser2.firstName(), createdUser2.lastName(), createdUser2.email(),
                    createdUser2.phoneNumber());

            // When
            var actual = restTestClient.get()
                                       .uri(uriBuilder -> uriBuilder.path(USERS_GET_BY_IDS_FULL_PATH)
                                                                    .queryParam(USERS_IDS_PARAM, userId1 + "," + userId1 + "," + userId2)
                                                                    .build())
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetUsersByIdsResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).containsExactlyInAnyOrder(response1, response2);
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("b344ecdf-d5bf-4e1f-84d9-c3a023dc0414");

            // When & Then
            restTestClient.get()
                          .uri(uriBuilder -> uriBuilder.path(USERS_GET_BY_IDS_FULL_PATH)
                                                       .queryParam(USERS_IDS_PARAM, userId)
                                                       .build())
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }
```

Reuses the existing `createUser()` / `createUser(JdbcUserEntity)` helpers already present in `UserE2EIT`. No new helpers needed. Two distinct users are built with `aUserBuilder().withEmail(...).buildJdbc()` to avoid email collisions in the database.

- [ ] **Step 4: Run the E2E tests**

```
mvn -pl services/asapp-users-service verify -Dit.test=UserE2EIT
```

Expected: `BUILD SUCCESS`. Testcontainers spins up PostgreSQL + Redis + MockServer; the new `GetUsersByIds` nested tests pass; all existing tests still pass.

- [ ] **Step 5: Commit**

```
git add services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java
git commit -m "test(users): add E2E coverage for GET /api/users?ids= endpoint"
```

---

## Task 10: AsciiDoc API guide

**Agent:** `documentation-engineer`

**Files:**

- Modify: `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc`

- [ ] **Step 1: Add the new resource section**

Insert **between `[[resources-users-get-by-id]]` and `[[resources-users-get-all]]`**:

```adoc
[[resources-users-get-by-ids]]
=== Get Users by IDs

Retrieves a list of users whose identifiers are in the provided `ids` query parameter.
Missing identifiers are silently omitted; the response order is not guaranteed.
Duplicate identifiers are deduplicated. The list must contain between 1 and 50 identifiers.

operation::get-users-by-ids[snippets='query-parameters,http-request,response-fields,curl-request,http-response']

```

(Note the trailing blank line; preserve the spacing between resource sections.)

- [ ] **Step 2: Add the new error subsection**

Insert **between `[[users-errors-400-path]]` and `[[users-errors-400-body]]`**:

```adoc
[[users-errors-400-param]]
==== Validation Failure - Request Parameter (400)

Returned when a query parameter fails validation (empty, out of range, or
malformed). `errors[].entity` is empty; `errors[].field` is the parameter name.

operation::error-request-param-validation-failure[snippets='http-response,response-fields']

```

- [ ] **Step 3: Run the full module verify to build the AsciiDoc**

```
mvn -pl services/asapp-users-service verify
```

The full verify regenerates the RestDocs snippets (from Task 8's IT) and builds the AsciiDoc. Expected: `BUILD SUCCESS`.

If AsciiDoctor emits a warning about a missing snippet (`get-users-by-ids` or `error-request-param-validation-failure`), re-run `mvn -pl services/asapp-users-service verify -Dit.test=UserRestControllerDocumentationIT` first to repopulate `target/generated-snippets/`.

- [ ] **Step 4: Commit**

```
git add services/asapp-users-service/src/docs/asciidoc/api-guide.adoc
git commit -m "docs(users): document GET /api/users?ids= in api-guide"
```

---

## Task 11: Full verify + review trio + TODO housekeeping

**Agent:** `architect-reviewer` for Step 2 (final architecture review), `code-reviewer` and `security-auditor` in parallel for Step 2, then plain commit for housekeeping.

**Files:**

- Modify: `TODO.md`

- [ ] **Step 1: Full project verify**

```
mvn clean verify
```

Expected: `BUILD SUCCESS` across all modules. This is the gate before opening the PR.

- [ ] **Step 2: Dispatch the review trio in parallel**

Run three review agents in parallel against the full branch diff (single message, multiple `Agent` tool uses):

- `code-reviewer` — line-level review of the diff vs `main`.
- `architect-reviewer` — macro/system-level review: layering, port/adapter taxonomy, exception placement, transaction scope, drift from the spec.
- `security-auditor` — confirm no auth regression, no new endpoint without bearer requirement, no input-validation gaps beyond what `@NotEmpty` / `@Size` catch.

Address all findings before proceeding to Step 3.

- [ ] **Step 3: Tick the TODO item**

In `TODO.md`, locate the line (currently in the v0.4.0 → Quick Wins → Functional Improvements block):

```
    * [ ] Add operation to find users by list of ids
```

Change it to:

```
    * [X] Add operation to find users by list of ids
```

- [ ] **Step 4: Commit the housekeeping**

```
git add TODO.md
git commit -m "docs(todo): mark find users by list of ids as done"
```

- [ ] **Step 5: Push and open the PR**

```
git push -u origin find-by-list-ids
gh pr create --title "feat(users): add batch find users by ids query" --body "..."
```

PR body follows the project's commit-msg shape (Conventional Commits + bulleted body per the bulleted-body skill spec).

---

## Self-Review Notes

**Spec coverage:** Each section of the spec maps to a task:

- §4.1 (endpoint + routing) → Task 6
- §4.2 (request validation) → Task 6 (annotations) + Task 7 (tests)
- §4.3 (URL constants) → Task 1
- §4.4 (response DTO) → Task 4
- §4.5 (status codes) → Task 6 (OpenAPI) + Task 7 (tests)
- §4.6 (OpenAPI) → Task 6
- §5.1–§5.3 (use case, service, port) → Tasks 2, 3
- §5.4 (domain) → no task; intentionally unchanged
- §6.1 (JDBC repo) → no task; uses inherited `findAllById`
- §6.2 (adapter) → Task 2
- §6.3 (database) → no task; PK index already covers `IN`
- §7 (error handling — new `ConstraintViolationException` handler) → Task 5
- §8.1 (service unit tests) → Task 3
- §8.2 (controller IT, validation-only) → Task 7
- §8.3 (RestDocs IT, success + request-param error snippet) → Task 8
- §8.4 (E2E IT) → Task 9
- §8.5 (no JDBC repo test) → respected; no task
- §8.6 (no Mother changes) → respected; tests use `aUserBuilder().withUserId(...).build()` / `.withEmail(...).buildJdbc()`
- §9.1 (api-guide.adoc resource + error sections) → Task 10
- §9.2 (README) → no task; service README is not endpoint-indexed
- §9.3 (TODO.md tick) → Task 11
- §10 (validation) → Task 11 Step 1
- §11 (git workflow) → commits distributed across tasks (one per concern, all green at commit time); PR opened in Task 11 Step 5

**Placeholder scan:** No "TBD" / "TODO" / "implement later" / "appropriate error handling" placeholders. Each step has a complete code block or exact command. The single `"..."` is in the PR body template (Step 5 of Task 11) — engineer composes the PR description from the branch diff at PR time.

**Type consistency:**

- Use case method: `getUsersByIds(List<UUID>) : List<User>` — used identically in Tasks 3, 6.
- Port method: `findByIds(Collection<UserId>) : Collection<User>` — used identically in Tasks 2, 3.
- Adapter method: same signature as the port.
- Mapper method: `toGetUsersByIdsResponse(User) : GetUsersByIdsResponse` — used identically in Tasks 4, 8.
- Response record: `GetUsersByIdsResponse(UUID, String, String, String, String)` — used identically in Tasks 4, 8, 9.
- URL constants: `USERS_GET_BY_IDS_FULL_PATH`, `USERS_IDS_PARAM` — used identically across Tasks 6 (controller mapping), 7 (WebMvc IT), 8 (RestDocs IT), 9 (E2E IT).
- Controller mapping: existing `@GetMapping(value = USERS_GET_ALL_PATH, params = "!ids")` and new `@GetMapping(value = USERS_GET_ALL_PATH, params = USERS_IDS_PARAM)` — both reference `USERS_GET_ALL_PATH`, consistent with the tasks-service reference.
- Method ordering in every file with multiple read methods: byId → **byIds** → all.
- Snippet identifiers: `get-users-by-ids` (Task 8 Step 2), `error-request-param-validation-failure` (Task 8 Step 3), referenced in Task 10 Step 1 and Step 2 respectively.
- Validation messages: `"Users identifiers list must not be empty"` and `"Users identifiers list must contain at most 50 elements"` — used identically in Task 6 (annotations) and Task 7 (assertions).
- VO factory error message: `"User ID must not be null"` — verified against `services/asapp-users-service/src/main/java/com/bcn/asapp/users/domain/user/UserId.java` and asserted in Task 3 Step 3.

No mismatches detected.