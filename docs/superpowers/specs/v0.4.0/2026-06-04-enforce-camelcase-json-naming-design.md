# Enforce camelCase JSON Naming Globally — Design

**Date:** 2026-06-04
**Status:** Implemented
**Targets:** `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`, `asapp-rest-clients`, `central-config`

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → JSON Naming)
drives this work:

> JSON Naming — Enforce request/response camelCase globally

The project's **current** HTTP wire format is **snake_case**, applied
deliberately through field-level `@JsonProperty("...")` annotations on
multi-word fields. There is no global `PropertyNamingStrategy`; snake_case
comes purely from those annotations, and Java fields are already camelCase.
Single-word fields (`username`, `password`, `title`, `description`, `email`,
`role`) carry no annotation and are identical in both conventions.

So this task is not merely "enforcement" — it is a deliberate **convention
reversal** from snake_case to camelCase, a **breaking change** to the wire
format. This is acceptable: ASAPP is a solo project with no external API
consumers, and all inter-service contracts live in the same build.

A secondary benefit: validation-error responses already report field names in
camelCase (the Java field name). Flipping request/response bodies to camelCase
removes an existing asymmetry between success and error payloads.

### Relationship to the v0.7.0 ArchUnit note

`TODO.md` line 79 (Version 0.7.0) plans:

> Add an ArchUnit/test asserting every request DTO `@JsonProperty` value
> equals the snake_case of its Java component name

This design implements that assertion **early and inverted to camelCase**.
Line 79 is reworded accordingly (see §5).

## 2. Decisions (from brainstorming)

1. **Direction:** flip the entire request/response JSON contract to camelCase.
2. **Error scope:** the error-response body is included — the only snake_case
   property there, `field_errors`, becomes `fieldErrors`.
3. **Enforcement:** an **ArchUnit guardrail test** (the only mechanism that
   actually blocks regression), **plus** an explicit global Jackson config as
   centralized documentation of intent, **plus** README documentation.

### Why ArchUnit, not config, is the enforcement mechanism

`spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE` reads like "global
enforcement" but is effectively a **no-op** for already-camelCase Java fields,
and a stray `@JsonProperty("snake_case")` would **silently override** it. The
config therefore documents intent but cannot prevent regression. Only a
build-failing test can. The config is still added (one line, declares intent
centrally) but is annotated with an inline comment stating it is documentation,
not the guard.

`field_errors` is a further illustration: it is set via
`ProblemDetail.setProperty(...)`, which Jackson serializes through
`@JsonAnyGetter`. The naming strategy does **not** apply to `@JsonAnyGetter`
map keys, so the rename must happen at the constant — the config could never
have fixed it.

## 3. Affected Files

### 3.1 Production DTOs — remove snake_case `@JsonProperty` (24 files)

Java fields are already camelCase; removing the annotation makes Jackson emit
camelCase by default. Drop the `import com.fasterxml.jackson.annotation.JsonProperty;`
where nothing else in the file uses it.

**`asapp-authentication-service`** (`infrastructure/authentication/in/` and `infrastructure/user/in/`):
- `authentication/in/request/RevokeAuthenticationRequest.java` — `access_token`
- `authentication/in/request/RefreshAuthenticationRequest.java` — `refresh_token`
- `authentication/in/response/AuthenticateResponse.java` — `access_token`, `refresh_token`
- `authentication/in/response/RefreshAuthenticationResponse.java` — `access_token`, `refresh_token`
- `user/in/response/CreateUserResponse.java` — `user_id`
- `user/in/response/UpdateUserResponse.java` — `user_id`
- `user/in/response/GetUserByIdResponse.java` — `user_id`
- `user/in/response/GetAllUsersResponse.java` — `user_id`

**`asapp-tasks-service`** (`infrastructure/task/in/`):
- `request/CreateTaskRequest.java` — `user_id`, `start_date`, `end_date`
- `request/UpdateTaskRequest.java` — `user_id`, `start_date`, `end_date`
- `response/CreateTaskResponse.java` — `task_id`
- `response/UpdateTaskResponse.java` — `task_id`
- `response/GetTaskByIdResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
- `response/GetAllTasksResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
- `response/GetTasksByUserIdResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
- `response/GetTasksByIdsResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`

**`asapp-users-service`** (`infrastructure/user/in/`):
- `request/CreateUserRequest.java` — `first_name`, `last_name`, `phone_number`
- `request/UpdateUserRequest.java` — `first_name`, `last_name`, `phone_number`
- `response/CreateUserResponse.java` — `user_id`
- `response/UpdateUserResponse.java` — `user_id`
- `response/GetUserByIdResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`, `task_ids`
- `response/GetAllUsersResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`
- `response/GetUsersByIdsResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`

**`asapp-rest-clients`** (lib):
- `clients/tasks/response/TasksByUserIdResponse.java` — `task_id` → field `taskId`

### 3.2 Error property rename (3 files)

- `services/*/src/main/java/**/infrastructure/error/ErrorMessages.java` ×3:
  `FIELD_ERRORS_PROPERTY = "field_errors"` → `"fieldErrors"`.

No change needed in `GlobalExceptionHandler.java` — it references the constant.

### 3.3 Global config (1 file)

- `central-config/application.properties` — add under the existing Jackson block:

  ```properties
  # camelCase is Jackson's default for our camelCase Java fields; this line
  # documents the project-wide convention. It is NOT the enforcement
  # mechanism — a renaming @JsonProperty would override it. Enforcement lives
  # in the per-service ArchUnit JsonNamingConventionTest.
  spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE
  ```

### 3.4 ArchUnit guardrail (new tests + POM changes)

- Parent `pom.xml` — add `archunit-junit5` version to `dependencyManagement`.
- 3× service `pom.xml` — add `com.tngtech.archunit:archunit-junit5` (test scope).
- 3× new test class `JsonNamingConventionTest` (one per service), placed in a
  new `architecture` test package, e.g.
  `com.bcn.asapp.<service>.architecture.JsonNamingConventionTest`.

The rest-clients lib DTO is flipped manually; **no** lib-level ArchUnit test
(single DTO, minimal test infrastructure — adding ArchUnit there is
over-engineering for one record).

### 3.5 Tests to update

- `services/*/src/test/java/**/*RestControllerIT.java` — request body JSON and
  response/error JSON assertions (`refresh_token`, `access_token`, `user_id`,
  `task_id`, `start_date`, `end_date`, `first_name`, `last_name`,
  `phone_number`, `task_ids`, `field_errors`, `field_errors[n]`) → camelCase.
- `services/*/src/test/java/**/*RestControllerDocumentationIT.java` — Spring
  REST Docs `fieldWithPath("...")` descriptors → camelCase (these regenerate
  the `.adoc` snippets).
- `libs/asapp-rest-clients/src/test/java/**/TasksRestClientTests.java` — mocked
  response JSON `task_id` → `taskId`.
- Any `*E2EIT.java` and test-data / `*Mother` factories that embed JSON string
  literals with snake_case field names (verify during implementation).

### 3.6 Docs to update

- 3× `services/*/src/docs/asciidoc/api-guide.adoc` — snake_case field
  references → camelCase. Auto-generated snippet tables follow from §3.5;
  hand-written field references edited directly.
- `.claude/rules/rest.md` — see §4.4.
- Root `README.md` — see §4.5.
- `TODO.md` — see §5.

## 4. Detailed Design

### 4.1 DTO flip

Mechanical. For each DTO in §3.1, delete the `@JsonProperty("...")` annotation
from every record component, leaving the camelCase Java component name to drive
serialization. Remove the now-orphaned `JsonProperty` import. Preserve all
other annotations and their ordering per `.claude/rules/code-style.md`
(validation annotations such as `@NotBlank`, `@Pattern` stay exactly as they
are, in their existing positions).

Example (`GetUserByIdResponse`, users-service):

```java
// before
public record GetUserByIdResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("task_ids") List<UUID> taskIds
) {}

// after
public record GetUserByIdResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<UUID> taskIds
) {}
```

### 4.2 Error property rename

In each service's `ErrorMessages`:

```java
// before
static final String FIELD_ERRORS_PROPERTY = "field_errors";
// after
static final String FIELD_ERRORS_PROPERTY = "fieldErrors";
```

The emitted JSON key changes from `field_errors` to `fieldErrors`. The
`RequestValidationError` record (`field`, `message`) is already camelCase and
unchanged; `error`, `critical`, and the RFC 7807 standard fields are
single-word and unchanged.

### 4.3 ArchUnit rule

Each `JsonNamingConventionTest` imports the DTO packages and asserts that no
DTO field renames itself via `@JsonProperty`. Because the field name is already
camelCase, "the `@JsonProperty` value equals the field name (or is absent /
empty)" is exactly "the JSON name is camelCase, with no renaming."

Rule shape (illustrative — exact API finalized in the plan):

```java
@AnalyzeClasses(packages = "com.bcn.asapp.users.infrastructure.user.in")
class JsonNamingConventionTest {

    @ArchTest
    static final ArchRule dtoFieldsMustNotRenameJsonProperty =
            fields().that().areDeclaredInClassesThat()
                    .resideInAnyPackage("..in.request..", "..in.response..")
                    .should(notRenameViaJsonProperty());

    // custom ArchCondition: if a field is annotated with @JsonProperty,
    // its value() must be empty OR equal to the field's name. Records
    // propagate @JsonProperty (target FIELD) to the backing field, so
    // ArchUnit reads it from JavaField.getAnnotationOfType(JsonProperty.class).
    // Failure message names the field and the offending value.
}
```

Notes for the plan:
- `@JsonProperty` for non-naming purposes (e.g. `required`, `access`, `index`)
  remains legal as long as its `value()` is empty or matches the field name.
- Scope the imported packages to each service's `..in.request` / `..in.response`
  (per §3.1 package layout). Authentication-service spans two aggregates
  (`authentication`, `user`) — import both.
- Keep each test self-contained per service (no shared test module exists;
  three near-identical copies match the existing per-service
  `JacksonConfigurationIT` pattern).

### 4.4 Rule file update (`.claude/rules/rest.md`)

The "Request / Response DTOs" section currently states:

> - Request and response fields use snake_case serialization names
> - Use `@JsonProperty("field_name")` for multi-word field names — single-word
>   fields need no annotation

Replace with camelCase guidance, e.g.:

> - Request and response fields use **camelCase** serialization names (Jackson
>   default — Java fields are already camelCase)
> - Do **not** use `@JsonProperty` to rename fields; this is enforced by the
>   per-service `JsonNamingConventionTest` (ArchUnit)

The "Error Response Format" section's `field_errors` mention becomes
`fieldErrors`.

### 4.5 README update (root `README.md`)

- **Example Workflow** curl snippets (mandatory — currently emit/parse
  snake_case):
  - `jq -r '.access_token'` → `.accessToken`
  - `jq -r '.user_id'` → `.userId`
  - `"user_id": "..."` → `"userId": "..."`
  - `"start_date"` / `"end_date"` → `"startDate"` / `"endDate"`
- Add a one-line statement of the convention (camelCase for all request/response
  bodies) in a sensible spot (Reference / API section).

The existing `docs/guidelines/` reference cleanup is a **separate** TODO
(line 34) and is not touched here. Service READMEs are checked during
implementation and updated only if they contain snake_case JSON examples.

## 5. TODO.md update

- Line 23: mark `Enforce request/response camelCase globally` as done (`[X]`).
- Line 79 (v0.7.0): reword from "equals the snake_case of its Java component
  name" to "equals the camelCase of its Java component name," and note it is
  partially realized by this task's `JsonNamingConventionTest` (the v0.7.0 item
  may fold it into the Modulith/ArchUnit suite).

## 6. Testing Strategy

- **ArchUnit (`JsonNamingConventionTest` ×3):** the regression guard. A
  reintroduced renaming `@JsonProperty` fails the build.
- **Integration (`*RestControllerIT` ×3):** prove the live wire format is
  camelCase for both success and error responses. These are the primary
  behavioral proof of the flip.
- **REST Docs (`*RestControllerDocumentationIT` ×3):** regenerate camelCase
  snippets; keep `api-guide.adoc` accurate.
- **REST client (`TasksRestClientTests`):** prove the client deserializes the
  new `taskId` field from a camelCase mocked response.
- `JacksonConfigurationIT` ×3: left as-is (ArchUnit already covers DTO naming;
  adding a strategy assertion here is optional and not required).

## 7. Sequencing

A single coordinated change (one PR) is appropriate — the rest-clients lib and
inter-service contracts share the build, so a staged rollout buys nothing.
Suggested internal order:

1. Flip the 24 DTOs + rest-client DTO; rename `FIELD_ERRORS_PROPERTY`.
2. Add the global config line.
3. Add ArchUnit dependency + the three `JsonNamingConventionTest` classes
   (these now pass against the flipped DTOs and would have failed before).
4. Update integration / documentation / rest-client tests.
5. Update docs: `api-guide.adoc` ×3, `.claude/rules/rest.md`, root `README.md`,
   `TODO.md`.

Commit convention: this is a breaking API change — use a Conventional Commits
`feat(...)!:` (or `refactor(...)!:`) subject with a `BREAKING CHANGE:` footer,
matching the recent `feat(error-handler)!:` precedent.

## 8. Out of Scope

- **JWT claims** (`token_use`, `iat`, `exp`, `sub`, `typ`, `role`) — these are
  JWT/RFC conventions set via Nimbus, not Jackson DTO serialization; `token_use`
  stays snake_case.
- The rest of the RFC 7807 error contract (`error`, `critical`, `field`,
  `message`, standard `ProblemDetail` fields) — already single-word/camelCase.
- A lib-level ArchUnit test for `asapp-rest-clients`.
- The `docs/guidelines/` README-reference cleanup (separate TODO line 34).
- Any change to DTO field *types*, validation rules, endpoints, or persistence.

## 9. Acceptance Criteria

A change set satisfying this design must:

- Emit and accept **camelCase** for every request/response field across the
  three services (e.g. `accessToken`, `refreshToken`, `userId`, `taskId`,
  `startDate`, `endDate`, `firstName`, `lastName`, `phoneNumber`, `taskIds`);
  verified by `*RestControllerIT`.
- Emit the validation extension property as `fieldErrors` (not `field_errors`)
  in all three services.
- Contain **zero** renaming `@JsonProperty` annotations in any `..in.request` /
  `..in.response` DTO (search confirms; ArchUnit enforces).
- Have `asapp-rest-clients` deserialize the tasks-service response via `taskId`;
  verified by `TasksRestClientTests`.
- Pass the three `JsonNamingConventionTest` ArchUnit suites, and have them fail
  if a snake_case `@JsonProperty` is reintroduced.
- Keep `central-config/application.properties`, `.claude/rules/rest.md`, the
  three `api-guide.adoc` files, the root `README.md`, and `TODO.md` accurate and
  consistent with camelCase.
- Pass `mvn clean verify` across all modules (run by the developer, not the
  agent).

## 10. Post-implementation notes

This spec was written before implementation. The work was built from the plan
(`docs/superpowers/plans/2026-06-04-enforce-camelcase-json-naming.md`) across
commits `e41446a9`–`cbb2eaa3`, then refined through a manual review pass
(commits `d15ec1e7`–`1987941d`). The plan implementation shipped the design
substantially as written — the 24 DTOs were flipped, `field_errors` became
`fieldErrors`, the global config line and the three ArchUnit guards landed, and
all tests/docs were realigned to camelCase. The manual review made no
behavioral reversals; it polished naming, placement, and documentation. As with
the error-handler spec, **the canonical implementation is the current state of
the code**, not this document. Notable manual-review deltas:

- **ArchUnit guard renamed and its condition reframed.**
  `JsonNamingConventionTest` → `JsonNamingConventionTests` in all three services
  (matching the project's `*Tests` unit-suite naming convention — so the
  `JsonNamingConventionTest` name used throughout §3.4, §4.3, §6 and §9 is now
  plural). The custom condition `notRenameViaJsonProperty()` was renamed to
  `haveJsonNameMatchingFieldName()` (positive framing), and its violation
  message is now built from `JavaField.getFullName()` via `String.formatted(...)`
  instead of `getOwner().getSimpleName()` + the field name with `String.format`.
  Behavior is unchanged: a renaming `@JsonProperty` still fails the build.

- **ArchUnit conventions documented in `.claude/rules/architecture.md`.** A new
  "Architecture tests" section was added stating that ArchUnit fitness functions
  live in `<service>.architecture` (test scope), grouped by concern, are never
  mirrored into the package they scan, and drive their scope via
  `@AnalyzeClasses(packages = ...)`. The plan only created the test classes; the
  rule documentation was added during review, making `architecture.md` a second
  binding contract for this kind of test alongside `rest.md`.

- **`RestDocsConstrainedFields.withPath` collapsed to a single overload.** The
  plan collapsed every two-arg `withPath(jsonPath, javaProperty)` call site to
  the single-arg form (JSON path now equals the Java property) but left the
  two-arg overload in place. The manual review deleted the now-unused two-arg
  overload (and its Javadoc) from all three services' test util, leaving only
  `withPath(String path)`.

- **Global Jackson config trimmed and reordered.** The four-line
  "documentation, not enforcement" comment that §3.3 and the plan added above
  `spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE` was removed, and the
  property was reordered before `spring.jackson.default-property-inclusion=NON_NULL`.
  The "config documents intent, ArchUnit enforces" rationale (§2 decision 3,
  §3.3) now lives only in this spec and the rule files — not as an inline
  comment in `central-config/application.properties`.

- **ArchUnit reclassified in the parent BOM.** The plan placed `archunit-junit5`
  under `<!-- ## Org Dependencies -->` (before `pitest-junit5-plugin`). The
  manual review moved it to a new `<!-- ## Other Dependencies -->` group in both
  the `<properties>` version block and `<dependencyManagement>`, since
  `com.tngtech.archunit` is a third-party (not `org.*`) dependency — aligning the
  parent POM with the per-service POMs, which already grouped it under "Other".

- **`.claude/rules/rest.md` rule statements trimmed.** The two camelCase DTO
  bullets landed (per plan Task 6) carrying their parenthetical rationale
  ("Jackson's default — Java fields are already camelCase"; "enforced per service
  by `JsonNamingConventionTest` (ArchUnit)"). The manual review trimmed both to
  terse, prescriptive rule statements ("Request and response fields use camelCase
  serialization names"; "Do not use `@JsonProperty` to rename fields"), keeping
  the rule file directive rather than explanatory.

**For future JSON-naming or architecture-test edits**, treat the current code —
the three `JsonNamingConventionTests` classes, the flipped `..in.request` /
`..in.response` DTOs, and `central-config/application.properties` — as the
template, and `.claude/rules/rest.md` plus `.claude/rules/architecture.md` as the
binding contracts. This spec is preserved as a record of the original design
intent and the reasoning behind the shipped shape.
