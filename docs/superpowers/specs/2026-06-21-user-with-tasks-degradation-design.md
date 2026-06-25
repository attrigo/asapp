# User-with-Tasks Degradation Behaviour — Design

**Date:** 2026-06-21
**Status:** Implemented
**Targets:** `asapp-users-service` (`TasksGatewayAdapter`, `TasksGateway`, `ReadUserService`, `UserWithTasksResult`, `UserMapper`, `GetUserByIdResponse`, new `TasksUnavailableException`, new `WarningCodes`, plus tests + REST docs)

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Review the user-with-tasks degradation behaviour when tasks-service is unavailable

The previous resilience tasks (circuit breaker, retry, timeouts) left the `tasks`
gateway guarded but with a **silent** degradation policy. Today, when
tasks-service is unavailable:

- `TasksGatewayAdapter.getTaskIdsByUserId` is wrapped by
  `@CircuitBreaker(fallbackMethod = "emptyTasksFallback")` + `@Retry`.
- On a degradable outage (5xx `HttpServerErrorException`, I/O
  `ResourceAccessException`, open-circuit `CallNotPermittedException`) the
  fallback **returns `List.of()`**. 4xx and unexpected errors are rethrown.
- `ReadUserService.enrichUserWithTasks` wraps the (empty) list into
  `UserWithTasksResult(user, taskIds)`.
- The controller maps that to `GetUserByIdResponse`, so the consumer sees
  `200 OK` with `"taskIds": []`.

**The problem:** `"taskIds": []` when tasks-service is down is **indistinguishable
from a user who genuinely has zero tasks**. The degraded state is operator-visible
only (a `WARN` log). This is a correctness smell, and AWS Well-Architected names
it an anti-pattern (§2).

The `TODO` entry frames two decisions, both resolved during brainstorming:

1. **What should `GET /users/{id}` do** — keep silent degrade-to-empty, surface
   "tasks unavailable" to the consumer, or fail the request?
2. **Where does the degrade policy belong** — adapter circuit-breaker fallback
   (current) vs application service catching a typed exception?

---

## 2. Decisions (from brainstorming + research)

1. **Behaviour: treat tasks as a SOFT dependency → partial-success `200`.** The
   user record is available; only the secondary enrichment failed. Return the
   user with `"taskIds": []` and an explicit, optional warning object. The
   `warnings` key is **omitted entirely on the happy path**
   (`@JsonInclude(NON_EMPTY)`). This fixes the silent-empty bug while preserving
   the useful user data (never `503`, never silent `[]`).

   Because `taskIds` is `[]` on degradation (not `null`), an empty `taskIds` is
   intentionally **indistinguishable from a user who genuinely has no tasks**.
   Clients **must** inspect `warnings` to detect degradation.

   **Response shape:**
   ```json
   // tasks-service available (happy path)
   200 OK
   { "userId": "...", "firstName": "Jane", ..., "taskIds": ["..."] }

   // tasks-service unavailable (degraded)
   200 OK
   { "userId": "...", "firstName": "Jane", ..., "taskIds": [],
     "warnings": [
       { "code": "tasks_unavailable", "field": "taskIds",
         "message": "Tasks could not be retrieved and may be incomplete.",
         "retryable": true }
     ] }
   ```

2. **Placement: split the resilience MECHANISM from the degrade POLICY.**
   - The **adapter** keeps the mechanism (circuit breaker + retry + timeout) but,
     on a degradable outage, **translates to a typed `TasksUnavailableException`**
     instead of silently returning `List.of()`. 4xx / unexpected still propagate.
   - The **application service** (`ReadUserService`) owns the policy: it catches
     `TasksUnavailableException` and decides "the read still succeeds, tasks
     marked unavailable."
   - This honours `ports-adapters.md`: *"Only translate infrastructure exceptions
     into application exceptions when the application service needs to catch
     them."* Under decision 1 it genuinely needs to.

3. **Result modelling: explicit semantic flag (brainstorming option A).**
   `UserWithTasksResult(User user, List<UUID> taskIds, boolean tasksAvailable)`.
   The application layer carries only the *semantic* degradation state; the
   infrastructure mapper translates `tasksAvailable == false` into the
   `tasks_unavailable` warning code. The application layer knows no HTTP/JSON
   vocabulary.

4. **Structured warning objects** — each warning is `{ "code": "...", "field":
   "...", "message": "...", "retryable": <bool> }`. This aligns with the
   project's RFC 7807 `ProblemDetail` error style: `ProblemDetail` covers hard
   failures; `WarningDetail` (the same shape applied to soft degradations) covers
   partial successes. Consistent vocabulary across both failure modes.

### Research backing (verified 2026-06)

- **AWS Well-Architected REL05-BP01** (hard vs soft dependencies): treating a
  dependency as soft and degrading is "as much a business discussion as a
  technical one." Named **anti-pattern**: *"Serving no data on errors or when
  only one out of multiple dependencies is unavailable and partial results can
  still be returned."* Canonical example mirrors ours — an aggregating page where
  one upstream fails: show everything else, not an error page.
  <https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/rel_mitigate_interaction_failure_graceful_degradation.html>
- **Silent `200 + []` is itself an anti-pattern:** it "delegates all detection of
  success/failure to client code — caching tools cache the error, monitoring
  won't know." Whatever we return, the degraded state must be visible.
  (Speakeasy API design / errors; oneuptime cascading-failures.)
- **Classify each dependency critical vs non-critical:** non-critical → degrade to
  a neutral value on 5xx/timeout; critical → fail. Tasks is non-critical here.
- **"Success with Warnings" 200 pattern:** signal partial outcomes with a
  warnings/metadata collection present only when there are warnings; describe the
  **data**, not the infrastructure (don't name the downstream service — avoids
  topology leak, consistent with `GlobalExceptionHandler`'s "no internal
  disclosure" stance). (API Catalyst — mixed success/failure in 200 responses.)
- **Hexagonal:** the resilience *mechanism* is an adapter concern; the
  degrade-vs-fail *policy* is a business decision and belongs in the application
  service. (Thoughtworks — hexagonal architecture.)

---

## 3. Detailed Design

### 3.1 New exception — `TasksUnavailableException`

- Location: `application/user/` (the aggregate package). This is the **first**
  application-layer exception in users-service.
- `extends RuntimeException` (orchestration exception per the `ports-adapters.md`
  hierarchy table; no base needed for a single exception).
- Constructor takes a message + `Throwable` cause. `@since 0.4.0`.
- Thrown by the infrastructure adapter, caught by the application service —
  dependency direction `infrastructure → application` is allowed.

### 3.2 Adapter — `TasksGatewayAdapter`

Happy path and the resilience annotations are unchanged. The fallback changes
from *degrade-to-empty* to *translate-or-rethrow*:

```java
@Override
@CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "tasksUnavailableFallback")
@Retry(name = TASKS_CLIENT_NAME)
public List<UUID> getTaskIdsByUserId(UserId userId) {
    var tasks = tasksHttpClient.getTasksByUserId(userId.value());
    if (tasks == null) {                 // valid "no tasks" — still a success
        return List.of();
    }
    return tasks.stream().map(TasksByUserIdResponse::taskId).toList();
}

private List<UUID> tasksUnavailableFallback(UserId userId, Throwable cause) throws Throwable {
    if (cause instanceof HttpServerErrorException
            || cause instanceof ResourceAccessException
            || cause instanceof CallNotPermittedException) {
        logger.warn("Tasks Service unavailable for user {}: {} - {}.",
                userId.value(), cause.getClass().getSimpleName(), cause.getMessage());
        throw new TasksUnavailableException("Tasks Service is unavailable", cause);
    }
    throw cause;   // 4xx + unexpected propagate unchanged
}
```

- The fallback **no longer returns an empty list** for outages; it throws the
  typed exception. The empty-list-for-outage behaviour moves out of the adapter
  entirely — the *policy* now lives in the application service.
- Null body (a valid "no tasks" response) stays an inline success, exactly as
  today.
- Method renamed `emptyTasksFallback` → `tasksUnavailableFallback` to reflect the
  new semantics. Class/method Javadoc updated.

### 3.3 Port — `TasksGateway`

Update the contract Javadoc: `getTaskIdsByUserId` now **throws
`TasksUnavailableException`** when tasks-service is unavailable (was: "returns an
empty list ... if retrieval fails"). It still returns an empty list for a genuine
no-tasks response.

### 3.4 Application service — `ReadUserService`

```java
private UserWithTasksResult enrichUserWithTasks(User user) {
    try {
        var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
        return UserWithTasksResult.available(user, taskIds);
    } catch (TasksUnavailableException ex) {
        logger.warn("Tasks unavailable for user {}; returning degraded result.", user.getId().value());
        return UserWithTasksResult.unavailable(user);
    }
}
```

Update the `getUserById` / `enrichUserWithTasks` Javadoc to describe the degraded
result (tasks marked unavailable) rather than "empty task list". `ReadUserUseCase`
Javadoc updated to match.

### 3.5 Result object — `UserWithTasksResult`

```java
public record UserWithTasksResult(User user, List<UUID> taskIds, boolean tasksAvailable) {

    public UserWithTasksResult {
        validateUserIsNotNull(user);
        // invariant: available ⇒ taskIds non-null;  unavailable ⇒ taskIds empty list
        if (tasksAvailable && taskIds == null) {
            throw new IllegalArgumentException("Task IDs list must not be null when tasks are available");
        }
        if (!tasksAvailable && taskIds != null && !taskIds.isEmpty()) {
            throw new IllegalArgumentException("Task IDs list must be empty when tasks are unavailable");
        }
    }

    public static UserWithTasksResult available(User user, List<UUID> taskIds) {
        return new UserWithTasksResult(user, taskIds, true);
    }

    public static UserWithTasksResult unavailable(User user) {
        return new UserWithTasksResult(user, List.of(), false);
    }
}
```

Factory methods are the intended construction path; the canonical constructor
enforces the invariant. Javadoc updated for the new component and factories.

### 3.6 Response DTO — `GetUserByIdResponse`

```java
public record GetUserByIdResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<UUID> taskIds,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<WarningDetail> warnings) {}
```

- `warnings` is omitted from the JSON when null/empty (`NON_EMPTY`) → clean happy
  path. (`@JsonInclude` is an inclusion policy, not a rename, so it does not
  violate the `rest.md` "no `@JsonProperty` renaming" rule.)
- `taskIds` is `[]` when degraded (not `null`); clients must inspect `warnings`
  to distinguish degradation from a genuine no-tasks user. Javadoc updated.
- Each `WarningDetail` carries `code`, `field`, `message`, and `retryable`,
  mirroring the `ProblemDetail` style used for hard failures.

### 3.7 Mapper — `UserMapper` (+ `WarningCodes`)

- `taskIds` continues to map 1:1 (empty list flows through to the response).
- `warnings` is derived from `tasksAvailable` via a `default` helper that
  constructs the structured `WarningDetail` object.
- The warning **code constant lives in a dedicated package-private holder**,
  `infrastructure/user/mapper/WarningCodes`, co-located with its only consumer
  (the mapper) — mirroring `ErrorMessages` sitting with `GlobalExceptionHandler`,
  and consistent with the project's per-vocabulary constant holders
  (`JwtClaimNames`, `JwtTypeNames`, …). Promote to a shared/public location only
  if a second consumer ever appears (YAGNI).

```java
// infrastructure/user/mapper/WarningCodes.java
final class WarningCodes {
    static final String TASKS_UNAVAILABLE = "tasks_unavailable";
    private WarningCodes() {}
}

// UserMapper
@Mapping(target = "userId", source = "user.id")
... (existing user field mappings) ...
@Mapping(target = "taskIds", source = "taskIds")
@Mapping(target = "warnings", source = "tasksAvailable")
GetUserByIdResponse toGetUserByIdResponse(UserWithTasksResult result);

default List<WarningDetail> toWarnings(boolean tasksAvailable) {
    if (tasksAvailable) {
        return List.of();
    }
    return List.of(new WarningDetail(
            WarningCodes.TASKS_UNAVAILABLE,
            "taskIds",
            "Tasks could not be retrieved and may be incomplete.",
            true));
}
```

The application layer carries only the semantic `tasksAvailable` flag; the
structured warning object is infrastructure-only vocabulary. See §9 for the
MapStruct boolean→`List<WarningDetail>` resolution open question.

### 3.8 `GlobalExceptionHandler` — no change

`TasksUnavailableException` is always caught by `ReadUserService` (the only caller
of the gateway; `getUsersByIds`/`getAllUsers` don't touch it), so it never reaches
the handler. No new `@ExceptionHandler` is needed. The existing `503` mapping
(Redis) is unrelated.

---

## 4. Testing strategy

- **`TasksGatewayAdapterIT`** (embedded MockServer): the two
  `ReturnsEmptyList_*` cases become
  `ThrowsTasksUnavailableException_TasksServiceReturnsServerError` and
  `ThrowsTasksUnavailableException_TasksServiceUnreachable`
  (`catchThrowable(...).isInstanceOf(TasksUnavailableException.class)`). The 4xx
  `PropagatesError_TasksServiceReturnsClientError` case stays.
- **`ReadUserServiceTests`** (Mockito): add — gateway returns ids → result
  `available` with ids and `tasksAvailable == true`; gateway throws
  `TasksUnavailableException` → result `unavailable` (`taskIds == []`,
  `tasksAvailable == false`).
- **`UserWithTasksResult` tests:** invariants — `available` requires non-null
  taskIds; `unavailable` yields empty `taskIds`; null user throws. (Add a
  `UserWithTasksResultTests` if none exists.)
- **`UserRestControllerIT`** (`@WebMvcTest`): happy path → `taskIds` populated,
  **no `warnings` key**; degraded path (stub `ReadUserUseCase` → `unavailable`
  result) → `taskIds: []` + `warnings: [{ "code": "tasks_unavailable",
  "field": "taskIds", "message": "Tasks could not be retrieved and may be
  incomplete.", "retryable": true }]`.
- **`UserRestControllerDocumentationIT` + `api-guide.adoc`:** document the new
  `warnings` field and partial-success semantics.
- **Verify:** `mvn clean verify`. ITs are slow — confirm with the developer before
  running them, per project convention.

---

## 5. Documentation

- **`api-guide.adoc`** (`src/docs/asciidoc`): update the *get user by id* section —
  new `warnings` field, `taskIds` empty (not null) on degradation, partial-success note.
- **`services/asapp-users-service/README.md`:** update the resilience/degradation
  description → now surfaces a structured `tasks_unavailable` warning with
  `taskIds: []`; user lookup still succeeds. Clients must inspect `warnings` to
  distinguish degradation from a genuine no-tasks user.
- **Javadoc:** `TasksGatewayAdapter` (class + fallback), `TasksGateway` port,
  `ReadUserService` / `ReadUserUseCase`, `UserWithTasksResult`,
  `GetUserByIdResponse`.
- **`TODO.md`:** tick line 33 and drop its two decision sub-bullets (decisions
  recorded here).
- **`.claude/rules/`** (gated — text **prepared** for the developer to apply; not
  edited directly per the auto-mode permission gate). Two complementary additions:
  - **`rest.md`** — a new *"Partial Success / Degraded Responses"* section
    documenting the response-shape convention: soft-dependency outage → `200` with
    the primary data + an optional `warnings` array of structured `WarningDetail`
    objects (omitted on the happy path via `@JsonInclude(NON_EMPTY)`); the degraded
    field uses its natural empty value (`[]`), with the warning as the degradation
    signal; the code names the missing data, never internal services.
  - **`ports-adapters.md`** (or `development-patterns.md`) — a note that the
    degrade-vs-fail policy belongs in the application service, reached via a typed
    gateway exception (`TasksUnavailableException`), while the adapter owns only
    the resilience mechanism.

---

## 6. Out of scope

- Library rename `asapp-rest-clients` → `asapp-http-clients` (TODO line 37).
- Parametrize resilience configs in the docker profile (TODO line 36).
- Changing the circuit-breaker / retry / timeout mechanism or its thresholds —
  unchanged; only the fallback's *outcome* changes (empty list → typed
  exception).
- Applying the pattern to other endpoints — none other call the gateway.
- Promoting `WarningDetail` to a shared library location — single consumer rule (YAGNI); revisit if a second consumer appears.

---

## 7. Acceptance criteria

- `GET /users/{id}` with tasks-service down → `200 OK`, `taskIds: []`,
  `warnings: [{ "code": "tasks_unavailable", "field": "taskIds", "message":
  "Tasks could not be retrieved and may be incomplete.", "retryable": true }]`.
- Happy path → `taskIds` populated, **no `warnings` key** in the JSON.
- Genuine no-tasks user → `200 OK`, `taskIds: []`, no `warnings`. Because
  `taskIds` is `[]` in both cases, clients **must** inspect `warnings` to detect
  degradation.
- 4xx and unexpected errors from tasks-service still propagate (unchanged).
- Degrade policy lives in `ReadUserService` via `TasksUnavailableException`; the
  adapter only translates the outage. Circuit breaker / retry / timeout mechanism
  and thresholds unchanged.
- Tests + docs updated per §4–§5; `mvn clean verify` is green.

---

## 8. Sequencing

1. Add `TasksUnavailableException` (`application/user/`).
2. Adapter: rename fallback, throw the typed exception on outages, update Javadoc.
3. Port `TasksGateway`: update contract Javadoc.
4. `UserWithTasksResult`: add `tasksAvailable` + factory methods + invariant.
5. `ReadUserService`: catch the exception → degraded result; update Javadoc.
6. `GetUserByIdResponse`: add `warnings` (`List<WarningDetail>`, `@JsonInclude(NON_EMPTY)`); `taskIds` always non-null (empty list on degradation).
7. `UserMapper`: add the package-private `WarningCodes` holder; derive `warnings`
   from `tasksAvailable`.
8. Tests (§4).
9. Verify (`mvn clean verify`; confirm before slow ITs).
10. Docs (§5); prepare the `.claude/rules` text for the developer.
11. `TODO.md`: tick line 33.

---

## 9. Open questions for the plan

- **MapStruct boolean→`List<WarningDetail>`:** confirm `toWarnings(boolean)` is
  resolved for `@Mapping(target = "warnings", source = "tasksAvailable")` by
  type; if ambiguous, fall back to `expression = "java(...)"` or a `@Named`
  qualifier.
- **`@JsonInclude` on a record component:** confirm Jackson honours
  component-level `@JsonInclude(NON_EMPTY)` (vs. needing it on the accessor /
  type); verify the happy path truly omits the key in a serialization test.
- Whether to add a dedicated `UserMapper` unit test for the warning derivation or
  cover it solely through `UserRestControllerIT`.
- `.claude/rules` placement — `ports-adapters.md` vs `development-patterns.md`.

---

## 10. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-21-user-with-tasks-degradation.md`)
were written before implementation. The core change shipped substantially as designed —
tasks are now a soft dependency: the adapter translates a downstream outage into a typed
`TasksUnavailableException`, `ReadUserService` catches it and returns a degraded result, and
`GET /users/{id}` answers `200 OK` with `taskIds: []` plus an optional `warnings` array that
is omitted on the happy path. The external response contract of §2.1 / §7 holds exactly, as
`UserE2EIT` confirms.

Beyond the wire contract, the canonical implementation is the current state of the source on
this branch — `TasksGatewayAdapter`, `TasksUnavailableException`, `ReadUserService`,
`UserWithTasksResult`, `UserMapper`, `WarningDetail`, and `GetUserByIdResponse`, plus their
tests — not this document.

Notable deltas:

- **Warning catalog moved from a `WarningCodes` string holder into a `WarningDetail.Reason`
  enum (reverses §3.7 and the `WarningCodes` mention in the Targets line / §8).** The design
  put a package-private `infrastructure/user/mapper/WarningCodes` holding only the code
  string, with the mapper assembling the `WarningDetail` from inline literals. The
  implementation instead catalogs the whole warning — code, field, message, retryable — in a
  `Reason` enum nested in the `WarningDetail` response record
  (`infrastructure/user/in/response/WarningDetail.java`) exposing a `toDetail()` factory;
  `WarningCodes` was never created. `UserMapper.toWarningDetails(boolean)` now returns
  `WarningDetail.Reason.TASK_IDS_UNAVAILABLE.toDetail()`. This keeps the full warning
  vocabulary in one typed place beside the DTO it builds, rather than a bare constant plus
  scattered literals.

- **Warning code renamed `tasks_unavailable` → `task_ids_unavailable` (revises §2.1, §3.6–§3.7,
  §4, §7).** The code now names the missing data, not the downstream service, per the `rest.md`
  partial-success convention ("the code names the missing data, never internal services"). It
  is defined on `WarningDetail.Reason.TASK_IDS_UNAVAILABLE`.

- **Availability flag renamed `tasksAvailable` → `tasksServiceAvailable` (revises §2.3, §3.5,
  §3.7).** `UserWithTasksResult` and the `UserMapper`
  `@Mapping(target = "warnings", source = "tasksServiceAvailable")` use the longer name, which
  states plainly that the flag tracks the service's reachability.

- **Degraded `taskIds` is modeled as `null` in the application result and coalesced to `[]`
  only at the mapper, not `[]` end-to-end (reverses §3.5, revises §3.7's "maps 1:1").**
  `UserWithTasksResult.unavailable(user)` now stores `null`, and the invariant requires
  `taskIds` be `null` when unavailable (was: empty list). The null→`[]` translation is an
  infrastructure decision made by the `@Named("toTaskIdsUUID")` helper in `UserMapper`; the
  application layer represents "no data" as absence. The wire contract is unchanged — the
  degraded response is still `taskIds: []` plus the warning (asserted in `UserE2EIT`).

For future degradation / partial-success edits, treat the source on this branch —
`WarningDetail` (and its `Reason` enum), `UserMapper`, `UserWithTasksResult`, and the
`rest.md` "Partial Success / Degraded Responses" convention — as the template; this spec is
preserved as a record of the original design intent.
