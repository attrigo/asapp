# Rewrite E2EIT to Assert JSON Instead of Java DTOs — Design

**Date:** 2026-06-27
**Status:** Implemented
**Targets:** `asapp-tasks-service`, `asapp-users-service`, `asapp-authentication-service`

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Test) drives this work:

> Rewrite E2EIT to assert Json instead of Java DTOs

The four `*E2EIT` classes assert HTTP **success** response bodies by deserializing
them back into the same Java response DTO and comparing objects:

```java
var actual = restTestClient.get().uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
        ...
        .expectBody(GetTaskByIdResponse.class)
        .returnResult()
        .getResponseBody();
assertThat(actual).isEqualTo(new GetTaskByIdResponse(...));
```

This tests the JSON↔DTO **round-trip**, not the wire contract clients actually
receive. A wrong `@JsonProperty` name, an extra or missing field, or a
serialization surprise (null inclusion, date format) round-trips cleanly through
the DTO and the test still passes. For an end-to-end test — whose whole purpose
is the externally observable contract — this is the wrong thing to assert.

The project already enforces camelCase JSON naming globally
(`2026-06-04-enforce-camelcase-json-naming-design.md`) and asserts the real wire
JSON in the controller integration tests. This task brings the E2E tier in line.

## 2. Decisions (from brainstorming)

1. **Mechanism: JsonUnit's fluent `assertThatJson(...)` chain API — used uniformly
   for every body (success and error).** `json-unit-assertj` (5.1.1) is already a
   declared test dependency in all three services and `assertThatJson` is already
   used in 22 test files. The body is captured as a `String` and asserted with the
   navigational chain (`.isObject()`, `.isArray()`, `.node(...)`,
   `.containsOnlyKeys(...)`, `.containsEntry(...)`, `.satisfiesExactlyInAnyOrder(...)`).
   - **No full-document `isEqualTo("""…json…""")` and no embedded JSON snippets
     (`json("{…}")`).** The rewrite must not assert against any JSON-document
     string literal. Expected values are passed as plain value literals
     (`createdTask.id().toString()`, `"Title1"`), never as JSON text.
2. **Strict field set for success bodies.** Object assertions pin the exact field
   set with `.containsOnlyKeys(...)` so an extra or missing field fails — preserving
   the full-contract strength the old DTO `isEqualTo` had (the point of asserting
   the wire JSON). Collection assertions are strict by construction
   (`.satisfiesExactlyInAnyOrder` requires an exact one-to-one match of elements).
3. **Unordered collections via `.satisfiesExactlyInAnyOrder(...)`.** The collection
   endpoints (`get-by-ids`, `get-by-user`, `get-all`) have no `ORDER BY`, so element
   order is not guaranteed. `.satisfiesExactlyInAnyOrder(consumer, consumer, …)` over
   the array elements asserts an exact, order-independent one-to-one match; each
   element is asserted with a nested `assertThatJson(element).isObject()…` chain — no
   JSON strings, no order assumption. (Replaces the old `containsExactlyInAnyOrder`
   over DTOs.)
4. **Request bodies stay as typed DTOs** (`.body(new CreateTaskRequest(...))`). The
   task targets *assertions*; the request wire contract is already covered by the
   controller ITs. Out of scope.
5. **Error bodies migrate** from `.jsonPath(...)` chains to `assertThatJson(...)`.
   Error bodies use **partial** `.isObject().containsEntry(...)` (no
   `containsOnlyKeys`), matching the controller-IT convention — RFC 7807
   `ProblemDetail` carries fields (e.g. `type`) the tests intentionally do not pin.

## 3. Affected Files

All changes are test-only; no production code is touched.

- `services/asapp-tasks-service/src/test/java/.../infrastructure/task/TaskE2EIT.java`
- `services/asapp-users-service/src/test/java/.../infrastructure/user/UserE2EIT.java`
- `services/asapp-authentication-service/src/test/java/.../infrastructure/user/UserE2EIT.java`
- `services/asapp-authentication-service/src/test/java/.../infrastructure/authentication/AuthenticationE2EIT.java`

No POM changes — `json-unit-assertj` is already on the test classpath in every
target service.

## 4. Assertion conventions

### 4.1 Body capture

Capture the body as a `String` in the `// When` block, assert in `// Then`,
keeping the project's `actual` naming and AAA structure:

```java
// When
var actual = restTestClient.get().uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
```

Status and `Content-Type` assertions stay in the fluent chain exactly as today.

### 4.2 Single-object success bodies

Strict object assertion — pin the field set, then assert each value. Values are
plain literals (UUID / `Instant` via `.toString()`), never JSON text:

```java
// Then
assertThatJson(actual).isObject()
        .containsOnlyKeys("taskId", "userId", "title", "description", "startDate", "endDate")
        .containsEntry("taskId", createdTask.id().toString())
        .containsEntry("userId", createdTask.userId().toString())
        .containsEntry("title", createdTask.title())
        .containsEntry("description", createdTask.description())
        .containsEntry("startDate", createdTask.startDate().toString())
        .containsEntry("endDate", createdTask.endDate().toString());
```

### 4.3 Collection success bodies (unordered, string-free)

`.isArray()` then `.satisfiesExactlyInAnyOrder(...)` — one consumer per expected
element, exact one-to-one match, order-independent. Each element is asserted with a
nested `assertThatJson(element)` chain (strict per element via `containsOnlyKeys`):

```java
// Then
assertThatJson(actual).isArray()
        .satisfiesExactlyInAnyOrder(
                task -> assertThatJson(task).isObject()
                        .containsOnlyKeys("taskId", "userId", "title", "description", "startDate", "endDate")
                        .containsEntry("taskId", createdTask1.id().toString())
                        .containsEntry("userId", createdTask1.userId().toString())
                        .containsEntry("title", createdTask1.title())
                        .containsEntry("description", createdTask1.description())
                        .containsEntry("startDate", createdTask1.startDate().toString())
                        .containsEntry("endDate", createdTask1.endDate().toString()),
                task -> assertThatJson(task).isObject()
                        .containsOnlyKeys("taskId", "userId", "title", "description", "startDate", "endDate")
                        .containsEntry("taskId", createdTask2.id().toString())
                        // … remaining fields …
        );
```

`.satisfiesExactlyInAnyOrder(...)` is an AssertJ iterable assertion; JsonUnit's
`isArray()` returns a list assert that extends it, and `assertThatJson(element)`
accepts the parsed element. **Implementation must confirm this composition compiles
and runs** (element type accepted by `assertThatJson`); the documented fallback, if
it does not, is positional `node("[i]").isObject()...` plus a `.hasSize(n)` count —
accepting that it relies on a stable element order.

### 4.4 Empty collection bodies

```java
assertThatJson(actual).isArray().isEmpty();
```

### 4.5 Create / Update success bodies (single-id payload)

The body is a single-key object, e.g. `{"taskId":"<uuid>"}`.

- **Create (generated id):** pin the shape, then extract the id to drive the
  existing DB verification. The extraction (`UUID.fromString(JsonPath.read(...))`)
  validates the value is a real UUID — no JSON-string assertion needed:

  ```java
  // Then
  assertThatJson(actual).isObject().containsOnlyKeys("taskId");

  var taskId = UUID.fromString(JsonPath.read(actual, "$.taskId"));
  var createdTask = taskRepository.findById(taskId);
  assertThat(createdTask).isPresent();
  assertSoftly(softly -> { ... });   // unchanged DB-side checks, keyed off taskId
  ```

- **Update (known id):** the id equals the seeded entity's, so assert it directly:

  ```java
  assertThatJson(actual).isObject()
          .containsOnlyKeys("taskId")
          .containsEntry("taskId", createdTask.id().toString());
  ```

`JsonPath` is Jayway `com.jayway.jsonpath.JsonPath` (already on the test classpath —
it backs Spring's `.jsonPath(...)`).

### 4.6 Error bodies (partial)

Migrate the `.jsonPath(...)` chains to a partial object assertion — same field set
checked today, no `containsOnlyKeys` (ProblemDetail carries unpinned fields):

```java
// before
.expectBody()
.jsonPath("$.title").isEqualTo("Authentication Failed")
.jsonPath("$.status").isEqualTo(401)
.jsonPath("$.detail").isEqualTo("Invalid credentials")
.jsonPath("$.error").isEqualTo("invalid_grant")
.jsonPath("$.instance").isEqualTo("/asapp-authentication-service/api/auth/token");

// after
var actual = ...expectBody(String.class).returnResult().getResponseBody();
assertThatJson(actual).isObject()
        .containsEntry("title", "Authentication Failed")
        .containsEntry("status", 401)
        .containsEntry("detail", "Invalid credentials")
        .containsEntry("error", "invalid_grant")
        .containsEntry("instance", "/asapp-authentication-service/api/auth/token");
```

The `.expectHeader().contentType(APPLICATION_PROBLEM_JSON)` assertion stays in the
chain. Where an error test also asserts persistence state (e.g.
`assertAuthenticationNotExist()`), that follows unchanged.

### 4.7 Nested arrays and nullable fields (users-service)

`GetUserById` returns `taskIds` (array of UUID strings) and `warnings` (absent or
present depending on degradation):

- **`taskIds`:** navigate and assert as a value array, e.g.
  `.node("taskIds").isArray().containsExactlyInAnyOrder(taskId1.toString(), taskId2.toString(), …)`
  — the elements are plain UUID-string literals, not JSON snippets. Empty case:
  `.node("taskIds").isArray().isEmpty()`.
- **`warnings`:** its presence/shape on the wire must be read from the real body
  (see §6). `containsOnlyKeys(...)` for `GetUserById` must list exactly the keys the
  body actually emits — include `warnings` only in the case where it is present.

## 5. Per-file change inventory

- **`TaskE2EIT`** — the single-object GET body → §4.2; every collection body across
  get-by-ids, get-by-user, and get-all → §4.3 (empty variants → §4.4); create →
  §4.5 (generated id), update → §4.5 (known id). Unauthorized / not-found /
  no-content cases (`.expectBody().isEmpty()`) unchanged. (The working-dir
  start that used `isEqualTo("""…""")` is replaced by the chain form above.)
- **`users/UserE2EIT`** — same shapes as `TaskE2EIT`, **plus** the `GetUserById`
  variants carrying `taskIds` and the partial-success `warnings` field per §4.7.
  MockServer setup and the `CapturedOutput` log assertions unchanged.
- **`authentication/UserE2EIT`** — single-object GET → §4.2; get-all → §4.3 (empty →
  §4.4); create → §4.5, update → §4.5. The password-masking value (`"*****"`) is
  asserted as a normal `containsEntry("password", "*****")`.
- **`AuthenticationE2EIT`** — token issuance/refresh **success** bodies stay on
  `assertAPIResponse(...)` (it decodes the JWT and asserts claims — not a DTO
  round-trip, out of scope). The ~14 **error** bodies migrate to §4.6.

## 6. Risks / watch-points

These are the wire-format truths the DTO round-trip currently hides. During
implementation, assert the **actual** serialized body (capture and inspect it),
never an assumed shape — surfacing these is the point of the task:

- **Exact field names.** Use the names the body actually emits (e.g. the task id
  field is `taskId`, confirmed from the serialized body — not the DTO component
  name). `containsOnlyKeys(...)` will fail loudly if a name is wrong.
- **Nullable fields.** `GetUserByIdResponse.warnings` is `null` in the success
  path. If Jackson omits nulls (`spring.jackson.default-property-inclusion=NON_NULL`),
  the field is **absent** on the wire and must not appear in `containsOnlyKeys`.
- **`WarningDetail` shape.** The partial-success warning object's fields must be
  read from the real body, not guessed.
- **`Instant` format.** Expected to serialize as ISO-8601 (`2025-01-01T11:00:00Z`,
  matching `Instant.toString()`); confirm it is not epoch millis.
- **`satisfiesExactlyInAnyOrder` composition.** Confirm `assertThatJson(element)`
  accepts the element type yielded by JsonUnit's `isArray()` list assert (§4.3);
  fall back to positional navigation only if it does not.

## 7. Testing strategy

This is a like-for-like assertion swap — **no new test scenarios** are added (per
the project's test-complexity-vs-value guidance). Each rewritten test must still
exercise the same scenario and still pass.

Verification: `mvn clean verify` for the three target services. These are `*E2EIT`
(Failsafe, Testcontainers — slow); run is gated on developer approval per the
Maven-permissions convention.

## 8. Out of scope

- Request-body conversion (stays typed DTOs).
- Persistence / Redis verification assertions (unchanged).
- The JWT-decoding `assertAPIResponse(...)` success helpers in `AuthenticationE2EIT`.
- Any new test cases, endpoints, or production-code changes.
- Empty-body cases (`401`/`404`/`204` → `.expectBody().isEmpty()`).

## 9. Acceptance criteria

A change set satisfying this design must:

- Assert every `*E2EIT` response body (success **and** error) with the JsonUnit
  `assertThatJson(...)` chain API over a captured `String` body — **no**
  `.expectBody(SomeResponse.class)` deserialization for assertions, and **no**
  JSON-document string literals (`isEqualTo("""…""")` / `json("{…}")`) anywhere.
- Pin the exact field set of every success object with `.containsOnlyKeys(...)`.
- Assert collection bodies with `.isArray().satisfiesExactlyInAnyOrder(...)`
  (order-independent, exact match); empty collections with `.isArray().isEmpty()`.
  No `containsExactlyInAnyOrder` over DTOs remaining.
- Assert create/update bodies as single-key objects (`containsOnlyKeys("…Id")`),
  keying the DB verification off the id extracted from the JSON (generated case) or
  asserting it directly (known case).
- Migrate all `AuthenticationE2EIT` error bodies to
  `assertThatJson(...).isObject().containsEntry(...)` (no `.jsonPath(...)` on error
  bodies remaining).
- Remove now-unused response-DTO imports, `ParameterizedTypeReference`, and
  body-side `assertThat`/`containsExactlyInAnyOrder` where orphaned; keep
  request-DTO imports and DB-side `assertThat`.
- Leave request bodies, persistence assertions, and `assertAPIResponse(...)`
  unchanged.
- Pass `mvn clean verify` for all three services (run by the developer).

## 10. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-27-e2eit-json-assertions.md`)
were written before implementation. The change shipped **as designed** — a
like-for-like assertion swap with no new scenarios. Every response body across the
four `*E2EIT` classes (success **and** error) is now captured as a `String` and
asserted with the JsonUnit `assertThatJson(...)` chain; no `.expectBody(SomeResponse.class)`
deserialization, `.jsonPath(...)` on error bodies, or JSON-document string literals
remain. The acceptance criteria (§9) are met, confirmed by the developer's
`mvn clean verify` on the three target modules. Notable points:

- **The `satisfiesExactlyInAnyOrder` composition risk (§4.3, §6) resolved positively.**
  `.isArray().satisfiesExactlyInAnyOrder(el -> assertThatJson(el)…)` compiled and ran
  in all three services; the documented positional `node("[i]")` fallback was **not
  needed** (zero usages). Form C is used for every collection body — 5 occurrences in
  `TaskE2EIT`, 5 in users `UserE2EIT`, 1 in authentication `UserE2EIT`.
- **Wire-format watch-points (§6) confirmed against the real body.** The task id field
  is `taskId`; `Instant` serializes as ISO-8601 (`Instant.toString()`); `taskIds` is
  always present (`[]` when empty); `warnings` is absent on the success path
  (`@JsonInclude(NON_EMPTY)`) and present only under degradation, with shape
  `code`/`field`/`message`/`retryable`. `containsOnlyKeys(...)` lists `warnings` only
  in the degradation case.
- **Manual-adjustment pass deltas** (the two `manual adjustements` commits, folded into
  this task's squashed commit):
  - `@formatter:off`/`@formatter:on` placement was tuned to wrap each full
    multi-statement `assertThatJson` block (including the separate `.node("taskIds")` /
    `.node("warnings")` statements), and removed from the short error-body chains in
    `AuthenticationE2EIT` that the formatter keeps clean on their own.
  - Collection single-element cases (`…SomeTasksExist` / `…SomeUsersExist`) and the task
    update body were re-keyed to the seeded entity's actual id (`createdTask.id()` /
    `createdUser.id()` / the extracted `taskId`) rather than the request-id literal, so
    the assertion and its DB verification key off the same value.
  - Inline `uri(uriBuilder -> …)` lambdas were extracted to a named
    `Function<UriBuilder, URI>` local for readability (matching the project's E2EIT URI
    convention).

**For future `*E2EIT` assertion work**, treat the current four test classes as the
template: capture the body as `String`, assert with `assertThatJson(...)` — strict
`containsOnlyKeys(...)` for success objects, `satisfiesExactlyInAnyOrder(...)` for
collections, partial `containsEntry(...)` for `ProblemDetail` error bodies — never a
DTO round-trip or a JSON string literal.