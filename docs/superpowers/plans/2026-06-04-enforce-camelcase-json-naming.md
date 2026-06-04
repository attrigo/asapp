# Enforce camelCase JSON Naming Globally — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Flip the HTTP wire format from snake_case to camelCase across all three services, the REST-client lib, and the error-response body, and add a per-service ArchUnit guardrail that fails the build if a renaming `@JsonProperty` is reintroduced.

**Architecture:** Java fields are already camelCase, so removing the snake_case `@JsonProperty` annotations makes Jackson emit camelCase by default. A global `LOWER_CAMEL_CASE` strategy in `central-config` documents the convention (it is not the enforcement — a renaming `@JsonProperty` would override it). The `field_errors` error property is renamed at its `ErrorMessages` constant because `ProblemDetail` extension keys serialize via `@JsonAnyGetter`, which ignores the naming strategy. Enforcement is a reflection-free ArchUnit convention test per service.

**Tech Stack:** Spring Boot 4 / Java 25, Jackson 3, Spring REST Docs (Asciidoctor), JUnit 5, AssertJ, json-unit, ArchUnit (new, test scope).

**Spec:** `docs/superpowers/specs/2026-06-04-enforce-camelcase-json-naming-design.md`

---

## Shared Conventions (read before every task)

### Maven is run by the developer, not the agent

This project's owner runs all `mvn` commands personally. At every step marked
**▶ DEV-RUN**, output the exact command, then **stop and wait** for the
developer to paste back the result. Never invoke `mvn` yourself.

### Format and commit gate

The pre-commit hook validates Spotless formatting, Unix (LF) line endings, and
Conventional-Commit message format — it does **not** run tests. Before every
commit that touches `*.java`:

1. **▶ DEV-RUN:** `mvn spotless:apply` — applies the Eclipse formatter, import
   order (`java|javax,org,com,,com.bcn`), removes unused imports, and inserts
   the Apache license header on new files.
2. Then stage and commit.

### Canonical field-name mapping (the entire rename)

Every change in this plan is one of these exact token substitutions. The
left column is the **old JSON name**; the right is the **new JSON name**, which
already equals the existing camelCase Java field/component name.

| Old JSON (snake_case) | New JSON (camelCase) |
| --------------------- | -------------------- |
| `access_token`        | `accessToken`        |
| `refresh_token`       | `refreshToken`       |
| `user_id`             | `userId`             |
| `task_id`             | `taskId`             |
| `start_date`          | `startDate`          |
| `end_date`            | `endDate`            |
| `first_name`          | `firstName`          |
| `last_name`           | `lastName`           |
| `phone_number`        | `phoneNumber`        |
| `task_ids`            | `taskIds`            |
| `field_errors`        | `fieldErrors`        |

**Do NOT touch database column names** (snake_case in JDBC `@Column`, Liquibase
changelogs, persistence tests, and SQL strings stay snake_case). The rename is
limited to JSON: `@JsonProperty` values, JSON string literals in tests, REST
Docs field paths, and the `field_errors` error property.

### Residual check (run at the end of each service task and at the final task)

**▶ DEV-RUN** (PowerShell or Bash; expects **zero** matches in the listed scope):

```bash
git grep -nE '"(access_token|refresh_token|user_id|task_id|start_date|end_date|first_name|last_name|phone_number|task_ids|field_errors)"' -- "services/<service-dir>/src" "libs/asapp-rest-clients/src"
git grep -nE 'JsonProperty\("[a-z]+_' -- "services/<service-dir>/src/main"
git grep -n 'field_errors' -- "services/<service-dir>/src/docs"
```

Any hit must be a database column reference in a persistence test (correct —
leave it). Hits in DTOs (`**/in/**`), controller tests, or `.adoc` files mean
the rename is incomplete.

---

## Task 1: ArchUnit dependency management + global camelCase config

**Files:**
- Modify: `pom.xml` (parent — `<properties>` and `<dependencyManagement>`)
- Modify: `central-config/application.properties`

POM entries are sorted alphabetically within their comment-delimited group
(`.claude/rules/maven.md`).

- [ ] **Step 1: Add the ArchUnit version property to the parent POM**

In `pom.xml`, under `<!-- Dependencies Versions -->`, add the property. Use the
newest 1.4.x — ArchUnit must parse **Java 25** bytecode, which only recent
releases support. If `1.4.1` does not resolve, pick the newest `1.4.x` on Maven
Central.

```xml
        <!-- Dependencies Versions -->
        <archunit.version>1.4.1</archunit.version>
        <!-- # CVE Dependencies (Overrides SB4 BOM) -->
        <jackson-bom.version>3.1.1</jackson-bom.version>
```

- [ ] **Step 2: Add ArchUnit to parent `<dependencyManagement>`**

In the `<!-- # Test Dependencies --> <!-- ## Org Dependencies -->` block, add
the entry before `pitest-junit5-plugin` (`com.tngtech.archunit` sorts before
`org.pitest`):

```xml
            <!-- # Test Dependencies -->
            <!-- ## Org Dependencies -->
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-junit5-plugin</artifactId>
                <version>${pitest-junit5-plugin.version}</version>
                <scope>test</scope>
            </dependency>
```

- [ ] **Step 3: Add the global Jackson naming-strategy config**

In `central-config/application.properties`, replace the existing Jackson block:

```properties
# Jackson properties
spring.jackson.default-property-inclusion=NON_NULL
```

with:

```properties
# Jackson properties
spring.jackson.default-property-inclusion=NON_NULL
# camelCase is Jackson's default for our camelCase Java fields; this line documents
# the project-wide convention. It is NOT the enforcement mechanism — a renaming
# @JsonProperty would override it. Enforcement lives in each service's ArchUnit
# JsonNamingConventionTest.
spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE
```

- [ ] **Step 4: Verify the reactor still resolves and builds**

**▶ DEV-RUN:** `mvn -DskipTests install`
Expected: `BUILD SUCCESS`. (The new dependency is declared but not yet used;
the config is a no-op while snake_case `@JsonProperty` annotations remain.)

- [ ] **Step 5: Commit**

```bash
git add pom.xml central-config/application.properties
git commit -m "chore: add ArchUnit and global camelCase Jackson naming config"
```

---

## Task 2: REST-clients lib — flip to camelCase

**Files:**
- Modify: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/response/TasksByUserIdResponse.java`
- Test: `libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/tasks/TasksRestClientTests.java`

- [ ] **Step 1: Flip the DTO**

Remove the `@JsonProperty` annotation and its now-unused import.

```java
// before
import com.fasterxml.jackson.annotation.JsonProperty;
...
public record TasksByUserIdResponse(
        @JsonProperty("task_id") UUID taskId
) {}

// after  (import line deleted)
public record TasksByUserIdResponse(
        UUID taskId
) {}
```

- [ ] **Step 2: Update the mocked response JSON in `TasksRestClientTests`**

Change every `"task_id"` JSON key to `"taskId"` (occurrences around lines 82 and
118–124). Example:

```java
// before
            "task_id": "%s"
// after
            "taskId": "%s"
```

- [ ] **Step 3: Format**

**▶ DEV-RUN:** `mvn spotless:apply`

- [ ] **Step 4: Verify the lib**

**▶ DEV-RUN:** `mvn verify -pl libs/asapp-rest-clients -am`
Expected: `BUILD SUCCESS`; `TasksRestClientTests` green (the client now
deserializes `taskId`).

- [ ] **Step 5: Residual check (rest-clients scope)**

**▶ DEV-RUN:** `git grep -n 'task_id' -- "libs/asapp-rest-clients/src"`
Expected: zero matches.

- [ ] **Step 6: Commit**

```bash
git add libs/asapp-rest-clients/src
git commit -m "refactor(rest-clients)!: expect camelCase taskId from tasks service

BREAKING CHANGE: the tasks-service client now reads the 'taskId' JSON field instead of 'task_id'."
```

---

## Task 3: authentication-service — flip to camelCase + ArchUnit guard

**Files:**
- Modify: `services/asapp-authentication-service/pom.xml`
- Create: `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/architecture/JsonNamingConventionTest.java`
- Modify (DTOs, remove `@JsonProperty` + unused import):
  - `.../infrastructure/authentication/in/request/RevokeAuthenticationRequest.java` — `access_token`
  - `.../infrastructure/authentication/in/request/RefreshAuthenticationRequest.java` — `refresh_token`
  - `.../infrastructure/authentication/in/response/AuthenticateResponse.java` — `access_token`, `refresh_token`
  - `.../infrastructure/authentication/in/response/RefreshAuthenticationResponse.java` — `access_token`, `refresh_token`
  - `.../infrastructure/user/in/response/CreateUserResponse.java` — `user_id`
  - `.../infrastructure/user/in/response/UpdateUserResponse.java` — `user_id`
  - `.../infrastructure/user/in/response/GetUserByIdResponse.java` — `user_id`
  - `.../infrastructure/user/in/response/GetAllUsersResponse.java` — `user_id`
- Modify: `.../infrastructure/error/ErrorMessages.java` — `FIELD_ERRORS_PROPERTY`
- Modify (tests): `AuthenticationRestControllerIT`, `UserRestControllerIT`, `AuthenticationRestControllerDocumentationIT`, `UserRestControllerDocumentationIT`, `AuthenticationE2EIT`, `UserE2EIT`
- Modify: `services/asapp-authentication-service/src/docs/asciidoc/api-guide.adoc` — `field_errors` ×3

- [ ] **Step 1: Add ArchUnit to the service POM**

In `services/asapp-authentication-service/pom.xml`, under
`<!-- # Test Dependencies --> <!-- ## Other Dependencies -->`, add the
dependency in alphabetical groupId order (after `com.redis`, before
`net.javacrumbs`):

```xml
        <!-- ## Other Dependencies -->
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Write the ArchUnit guardrail test**

Create `JsonNamingConventionTest.java` in a new `architecture` test package.
(Test class — no `@since`; Spotless adds the Apache license header on apply.)

```java
package com.bcn.asapp.authentication.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Tests that request and response DTOs expose camelCase JSON property names (no {@code @JsonProperty} renaming).
 *
 * @author attrigo
 */
@AnalyzeClasses(packages = "com.bcn.asapp.authentication.infrastructure", importOptions = ImportOption.DoNotIncludeTests.class)
class JsonNamingConventionTest {

    @ArchTest
    static final ArchRule requestResponseDtoFieldsUseCamelCaseJson = fields().that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..in.request..", "..in.response..")
            .should(notRenameViaJsonProperty());

    private static ArchCondition<JavaField> notRenameViaJsonProperty() {
        return new ArchCondition<>("not rename the JSON property (the camelCase Java field name is the wire name)") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.tryGetAnnotationOfType(JsonProperty.class).ifPresent(annotation -> {
                    var jsonName = annotation.value();

                    if (!jsonName.isEmpty() && !jsonName.equals(field.getName())) {
                        var message = String.format(
                                "%s.%s is annotated @JsonProperty(\"%s\"); request/response JSON must be camelCase — remove the annotation or set the value to \"%s\"",
                                field.getOwner().getSimpleName(), field.getName(), jsonName, field.getName());
                        events.add(SimpleConditionEvent.violated(field, message));
                    }
                });
            }
        };
    }

}
```

- [ ] **Step 3: Run the guard and watch it FAIL (red)**

**▶ DEV-RUN:** `mvn test -pl services/asapp-authentication-service -am -Dtest=JsonNamingConventionTest`
Expected: **FAIL** — violations naming `accessToken`, `refreshToken`, `userId`
fields still carrying snake_case `@JsonProperty`. This proves the guard works.

- [ ] **Step 4: Flip the 8 DTOs**

For each DTO listed above, delete the `@JsonProperty("...")` annotation from
each component and delete the now-unused
`import com.fasterxml.jackson.annotation.JsonProperty;`. Leave every other
annotation (`@NotBlank`, etc.) and the component order untouched. Example:

```java
// before
public record AuthenticateResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken
) {}

// after  (JsonProperty import deleted)
public record AuthenticateResponse(
        String accessToken,
        String refreshToken
) {}
```

- [ ] **Step 5: Rename the error property constant**

In `ErrorMessages.java`:

```java
// before
    static final String FIELD_ERRORS_PROPERTY = "field_errors";
// after
    static final String FIELD_ERRORS_PROPERTY = "fieldErrors";
```

- [ ] **Step 6: Update controller integration tests**

In `AuthenticationRestControllerIT` and `UserRestControllerIT`, apply the
mapping table to:
- request body JSON literals (e.g. `"refresh_token": "..."` → `"refreshToken": "..."`, `"access_token"`, `"user_id"`);
- json-unit assertions on error responses: `.node("field_errors")` → `.node("fieldErrors")` and `.node("field_errors[0]")` → `.node("fieldErrors[0]")` (and `[1]`, `[2]`, …);
- any response-body assertions on `access_token` / `refresh_token` / `user_id`.

- [ ] **Step 7: Update REST Docs tests**

In `AuthenticationRestControllerDocumentationIT` and
`UserRestControllerDocumentationIT`:
- response descriptors `fieldWithPath("access_token")` / `fieldWithPath("user_id")` (and `[].user_id`, etc.) → camelCase;
- request descriptors using the two-arg constrained helper collapse to one arg, since JSON path now equals the Java property:

```java
// before
fields.withPath("refresh_token", "refreshToken").description("...")
// after
fields.withPath("refreshToken").description("...")
```

- [ ] **Step 8: Update E2E tests**

In `AuthenticationE2EIT` and `UserE2EIT`, apply the mapping table to all JSON
request bodies and response assertions (`access_token`, `refresh_token`,
`user_id`, `field_errors`).

- [ ] **Step 9: Update the API guide**

In `api-guide.adoc`, change the 3 hand-written `field_errors` references to
`fieldErrors`:

```asciidoc
The `fieldErrors` array describes each validation failure.
```
(and the other two prose lines).

- [ ] **Step 10: Residual check (authentication scope)**

**▶ DEV-RUN:**
```bash
git grep -nE '"(access_token|refresh_token|user_id|field_errors)"' -- "services/asapp-authentication-service/src"
git grep -nE 'JsonProperty\("[a-z]+_' -- "services/asapp-authentication-service/src/main"
git grep -n 'field_errors' -- "services/asapp-authentication-service/src/docs"
```
Expected: zero matches outside database-column references in persistence tests.

- [ ] **Step 11: Format**

**▶ DEV-RUN:** `mvn spotless:apply`

- [ ] **Step 12: Verify the service (guard green + ITs green)**

**▶ DEV-RUN:** `mvn verify -pl services/asapp-authentication-service -am`
Expected: `BUILD SUCCESS`; `JsonNamingConventionTest` now PASSES; all ITs /
E2E green.

- [ ] **Step 13: Commit**

```bash
git add services/asapp-authentication-service
git commit -m "refactor(authentication)!: use camelCase request/response JSON naming

Flip access_token, refresh_token, user_id and the field_errors error property
to camelCase, and guard the convention with an ArchUnit JsonNamingConventionTest.

BREAKING CHANGE: authentication-service request/response and validation-error JSON now use camelCase field names (accessToken, refreshToken, userId, fieldErrors)."
```

---

## Task 4: tasks-service — flip to camelCase + ArchUnit guard

**Files:**
- Modify: `services/asapp-tasks-service/pom.xml`
- Create: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/architecture/JsonNamingConventionTest.java`
- Modify (DTOs, remove `@JsonProperty` + unused import):
  - `.../infrastructure/task/in/request/CreateTaskRequest.java` — `user_id`, `start_date`, `end_date`
  - `.../infrastructure/task/in/request/UpdateTaskRequest.java` — `user_id`, `start_date`, `end_date`
  - `.../infrastructure/task/in/response/CreateTaskResponse.java` — `task_id`
  - `.../infrastructure/task/in/response/UpdateTaskResponse.java` — `task_id`
  - `.../infrastructure/task/in/response/GetTaskByIdResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
  - `.../infrastructure/task/in/response/GetAllTasksResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
  - `.../infrastructure/task/in/response/GetTasksByUserIdResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
  - `.../infrastructure/task/in/response/GetTasksByIdsResponse.java` — `task_id`, `user_id`, `start_date`, `end_date`
- Modify: `.../infrastructure/error/ErrorMessages.java` — `FIELD_ERRORS_PROPERTY`
- Modify (tests): `TaskRestControllerIT`, `TaskRestControllerDocumentationIT`, `TaskE2EIT`
- Modify: `services/asapp-tasks-service/src/docs/asciidoc/api-guide.adoc` — `field_errors` ×3

- [ ] **Step 1: Add ArchUnit to the service POM**

Same XML as Task 3 Step 1, in `services/asapp-tasks-service/pom.xml` under
`<!-- ## Other Dependencies -->` (after `com.redis`, before `net.javacrumbs`):

```xml
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Write the ArchUnit guardrail test**

Create `JsonNamingConventionTest.java` identical to Task 3 Step 2 except the
package and the analyzed root package use `tasks`:

```java
package com.bcn.asapp.tasks.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Tests that request and response DTOs expose camelCase JSON property names (no {@code @JsonProperty} renaming).
 *
 * @author attrigo
 */
@AnalyzeClasses(packages = "com.bcn.asapp.tasks.infrastructure", importOptions = ImportOption.DoNotIncludeTests.class)
class JsonNamingConventionTest {

    @ArchTest
    static final ArchRule requestResponseDtoFieldsUseCamelCaseJson = fields().that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..in.request..", "..in.response..")
            .should(notRenameViaJsonProperty());

    private static ArchCondition<JavaField> notRenameViaJsonProperty() {
        return new ArchCondition<>("not rename the JSON property (the camelCase Java field name is the wire name)") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.tryGetAnnotationOfType(JsonProperty.class).ifPresent(annotation -> {
                    var jsonName = annotation.value();

                    if (!jsonName.isEmpty() && !jsonName.equals(field.getName())) {
                        var message = String.format(
                                "%s.%s is annotated @JsonProperty(\"%s\"); request/response JSON must be camelCase — remove the annotation or set the value to \"%s\"",
                                field.getOwner().getSimpleName(), field.getName(), jsonName, field.getName());
                        events.add(SimpleConditionEvent.violated(field, message));
                    }
                });
            }
        };
    }

}
```

- [ ] **Step 3: Run the guard and watch it FAIL (red)**

**▶ DEV-RUN:** `mvn test -pl services/asapp-tasks-service -am -Dtest=JsonNamingConventionTest`
Expected: **FAIL** — violations on `taskId`, `userId`, `startDate`, `endDate`.

- [ ] **Step 4: Flip the 8 DTOs**

Delete each `@JsonProperty("...")` and the unused import. Example:

```java
// before
public record GetTaskByIdResponse(
        @JsonProperty("task_id") UUID taskId,
        @JsonProperty("user_id") UUID userId,
        String title,
        String description,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate
) {}

// after  (JsonProperty import deleted)
public record GetTaskByIdResponse(
        UUID taskId,
        UUID userId,
        String title,
        String description,
        Instant startDate,
        Instant endDate
) {}
```

- [ ] **Step 5: Rename the error property constant**

In `ErrorMessages.java`: `FIELD_ERRORS_PROPERTY = "field_errors"` → `"fieldErrors"`.

- [ ] **Step 6: Update `TaskRestControllerIT`**

Apply the mapping table to request body literals (`"user_id"`, `"start_date"`,
`"end_date"`) and error assertions `.node("field_errors")` /
`.node("field_errors[0]")` → `fieldErrors`.

- [ ] **Step 7: Update `TaskRestControllerDocumentationIT`**

Response descriptors `fieldWithPath("task_id")` / `fieldWithPath("[].user_id")`
(and `start_date`, `end_date`) → camelCase. Request descriptors collapse the
two-arg helper to one arg:

```java
// before
fields.withPath("user_id", "userId").description("The task's owner unique identifier"),
fields.withPath("start_date", "startDate").description("...").optional(),
fields.withPath("end_date", "endDate").description("...").optional()
// after
fields.withPath("userId").description("The task's owner unique identifier"),
fields.withPath("startDate").description("...").optional(),
fields.withPath("endDate").description("...").optional()
```

- [ ] **Step 8: Update `TaskE2EIT`**

Apply the mapping table to all JSON request bodies and response assertions
(`task_id`, `user_id`, `start_date`, `end_date`, `field_errors`).

- [ ] **Step 9: Update the API guide**

In `api-guide.adoc`, change the 3 `field_errors` references to `fieldErrors`.

- [ ] **Step 10: Residual check (tasks scope)**

**▶ DEV-RUN:**
```bash
git grep -nE '"(task_id|user_id|start_date|end_date|field_errors)"' -- "services/asapp-tasks-service/src"
git grep -nE 'JsonProperty\("[a-z]+_' -- "services/asapp-tasks-service/src/main"
git grep -n 'field_errors' -- "services/asapp-tasks-service/src/docs"
```
Expected: zero matches outside database-column references in persistence tests.

- [ ] **Step 11: Format**

**▶ DEV-RUN:** `mvn spotless:apply`

- [ ] **Step 12: Verify the service**

**▶ DEV-RUN:** `mvn verify -pl services/asapp-tasks-service -am`
Expected: `BUILD SUCCESS`; guard PASSES; ITs / E2E green.

- [ ] **Step 13: Commit**

```bash
git add services/asapp-tasks-service
git commit -m "refactor(tasks)!: use camelCase request/response JSON naming

Flip task_id, user_id, start_date, end_date and the field_errors error property
to camelCase, and guard the convention with an ArchUnit JsonNamingConventionTest.

BREAKING CHANGE: tasks-service request/response and validation-error JSON now use camelCase field names (taskId, userId, startDate, endDate, fieldErrors)."
```

---

## Task 5: users-service — flip to camelCase + ArchUnit guard

**Files:**
- Modify: `services/asapp-users-service/pom.xml`
- Create: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/architecture/JsonNamingConventionTest.java`
- Modify (DTOs, remove `@JsonProperty` + unused import):
  - `.../infrastructure/user/in/request/CreateUserRequest.java` — `first_name`, `last_name`, `phone_number`
  - `.../infrastructure/user/in/request/UpdateUserRequest.java` — `first_name`, `last_name`, `phone_number`
  - `.../infrastructure/user/in/response/CreateUserResponse.java` — `user_id`
  - `.../infrastructure/user/in/response/UpdateUserResponse.java` — `user_id`
  - `.../infrastructure/user/in/response/GetUserByIdResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`, `task_ids`
  - `.../infrastructure/user/in/response/GetAllUsersResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`
  - `.../infrastructure/user/in/response/GetUsersByIdsResponse.java` — `user_id`, `first_name`, `last_name`, `phone_number`
- Modify: `.../infrastructure/error/ErrorMessages.java` — `FIELD_ERRORS_PROPERTY`
- Modify (tests): `UserRestControllerIT`, `UserRestControllerDocumentationIT`, `UserE2EIT`
- Modify: `services/asapp-users-service/src/docs/asciidoc/api-guide.adoc` — `field_errors` ×3

Note: `UserRestControllerDocumentationIT` and `UserE2EIT` also reference
`task_ids` (the response field) and may mock the tasks-service response with
`task_id` via the REST client — flip both.

- [ ] **Step 1: Add ArchUnit to the service POM**

Same XML as Task 3 Step 1, in `services/asapp-users-service/pom.xml` under
`<!-- ## Other Dependencies -->`.

- [ ] **Step 2: Write the ArchUnit guardrail test**

Create `JsonNamingConventionTest.java` identical to Task 4 Step 2 except the
package and analyzed root use `users`:
- `package com.bcn.asapp.users.architecture;`
- `@AnalyzeClasses(packages = "com.bcn.asapp.users.infrastructure", importOptions = ImportOption.DoNotIncludeTests.class)`

(All other lines, including imports and the `notRenameViaJsonProperty()`
condition, are identical to Task 4 Step 2.)

- [ ] **Step 3: Run the guard and watch it FAIL (red)**

**▶ DEV-RUN:** `mvn test -pl services/asapp-users-service -am -Dtest=JsonNamingConventionTest`
Expected: **FAIL** — violations on `userId`, `firstName`, `lastName`,
`phoneNumber`, `taskIds`.

- [ ] **Step 4: Flip the 7 DTOs**

Delete each `@JsonProperty("...")` and the unused import. Example:

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

// after  (JsonProperty import deleted)
public record GetUserByIdResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<UUID> taskIds
) {}
```

- [ ] **Step 5: Rename the error property constant**

In `ErrorMessages.java`: `FIELD_ERRORS_PROPERTY = "field_errors"` → `"fieldErrors"`.

- [ ] **Step 6: Update `UserRestControllerIT`**

Apply the mapping table to request body literals (`"first_name"`, `"last_name"`,
`"phone_number"`) and error assertions `.node("field_errors")` /
`.node("field_errors[n]")` → `fieldErrors`.

- [ ] **Step 7: Update `UserRestControllerDocumentationIT`**

Response descriptors (`user_id`, `first_name`, `last_name`, `phone_number`,
`task_ids`, and `[].`-prefixed variants) → camelCase. Request descriptors
collapse the two-arg helper to one arg:

```java
// before
fields.withPath("first_name", "firstName").description("...")
fields.withPath("phone_number", "phoneNumber").description("...")
// after
fields.withPath("firstName").description("...")
fields.withPath("phoneNumber").description("...")
```

- [ ] **Step 8: Update `UserE2EIT`**

Apply the mapping table to all JSON request bodies and response assertions,
**including** any mocked tasks-service response that returns `task_id`
(→ `taskId`) and the `task_ids` response field (→ `taskIds`).

- [ ] **Step 9: Update the API guide**

In `api-guide.adoc`, change the 3 `field_errors` references to `fieldErrors`.

- [ ] **Step 10: Residual check (users scope)**

**▶ DEV-RUN:**
```bash
git grep -nE '"(user_id|first_name|last_name|phone_number|task_ids|task_id|field_errors)"' -- "services/asapp-users-service/src"
git grep -nE 'JsonProperty\("[a-z]+_' -- "services/asapp-users-service/src/main"
git grep -n 'field_errors' -- "services/asapp-users-service/src/docs"
```
Expected: zero matches outside database-column references in persistence tests.

- [ ] **Step 11: Format**

**▶ DEV-RUN:** `mvn spotless:apply`

- [ ] **Step 12: Verify the service**

**▶ DEV-RUN:** `mvn verify -pl services/asapp-users-service -am`
Expected: `BUILD SUCCESS`; guard PASSES; ITs / E2E green (the users→tasks
client path deserializes `taskId`).

- [ ] **Step 13: Commit**

```bash
git add services/asapp-users-service
git commit -m "refactor(users)!: use camelCase request/response JSON naming

Flip user_id, first_name, last_name, phone_number, task_ids and the field_errors
error property to camelCase, and guard the convention with an ArchUnit
JsonNamingConventionTest.

BREAKING CHANGE: users-service request/response and validation-error JSON now use camelCase field names (userId, firstName, lastName, phoneNumber, taskIds, fieldErrors)."
```

---

## Task 6: Cross-cutting documentation

**Files:**
- Modify: `.claude/rules/rest.md`
- Modify: `README.md` (root)
- Modify: `TODO.md`

- [ ] **Step 1: Update the project rule (`.claude/rules/rest.md`)**

In the "Request / Response DTOs" section, replace:

```markdown
- Request and response fields use snake_case serialization names
- Use `@JsonProperty("field_name")` for multi-word field names — single-word fields need no annotation
```

with:

```markdown
- Request and response fields use camelCase serialization names (Jackson's default — Java fields are already camelCase)
- Do not use `@JsonProperty` to rename fields; this is enforced per service by `JsonNamingConventionTest` (ArchUnit)
```

In the "Error Response Format" section, change the `field_errors` property name
to `fieldErrors`:

```markdown
- Validation errors extend `ProblemDetail` with a `fieldErrors` property containing a list of `RequestValidationError(field, message)`
```

- [ ] **Step 2: Fix the README example workflow + add the convention note**

In `README.md`, in the "Example Workflow" block, apply the mapping table:

```bash
  | jq -r '.accessToken')
...
  }' | jq -r '.userId')
...
    "userId": "'$USER_ID'",
    "title": "Complete documentation",
    "description": "Update all README files",
    "startDate": "2025-01-15T09:00:00Z",
    "endDate": "2025-01-20T17:00:00Z"
```

Add one line in the "Reference" section (after "API Endpoints") stating the
convention:

```markdown
All request and response bodies use **camelCase** JSON field names.
```

- [ ] **Step 3: Update `TODO.md`**

- Line 23: mark done — `        * [X] Enforce request/response camelCase globally`
- Line 79 (v0.7.0): reword the snake_case assertion to camelCase and note it is
  realized by `JsonNamingConventionTest`:

```markdown
    * Add an ArchUnit/test asserting every request DTO `@JsonProperty` value equals the camelCase of its Java component name (already enforced per service by `JsonNamingConventionTest`; fold into the Modulith ArchUnit suite)
```

- [ ] **Step 4: Commit**

```bash
git add .claude/rules/rest.md README.md TODO.md
git commit -m "docs: document camelCase JSON naming convention

Flip the REST DTO rule, README examples, and the v0.7.0 ArchUnit note from
snake_case to camelCase; mark the JSON naming TODO done."
```

---

## Task 7: Full reactor verification

No code changes — this is the acceptance gate, run only after Tasks 1–6 are
committed (a full-reactor verify mid-sequence would show users-service red until
Task 5).

- [ ] **Step 1: Repo-wide residual check**

**▶ DEV-RUN:**
```bash
git grep -nE '"(access_token|refresh_token|user_id|task_id|start_date|end_date|first_name|last_name|phone_number|task_ids|field_errors)"' -- "services/*/src" "libs/*/src"
git grep -nE 'JsonProperty\("[a-z]+_' -- "services/*/src/main" "libs/*/src/main"
git grep -n 'field_errors' -- "services/*/src/docs"
```
Expected: zero matches except database-column references in persistence tests
(verify each remaining hit is a DB column, not a JSON field).

- [ ] **Step 2: Full build with formatting, docs, coverage**

**▶ DEV-RUN:** `mvn clean verify -Pfull`
Expected: `BUILD SUCCESS` across all 10 modules — Spotless check passes, all
three `JsonNamingConventionTest` suites pass, every IT / E2E green, REST Docs
(`api-guide.html`) regenerated with camelCase fields.

- [ ] **Step 3: Done**

All work is committed across Tasks 1–6; no further commit needed. Update the
spec status to `Implemented` if you keep that convention (optional).

---

## Self-Review

**Spec coverage:**
- §2 flip 24 DTOs → Tasks 2 (1), 3 (8), 4 (8), 5 (7). ✓
- §2 `field_errors → fieldErrors` (ErrorMessages ×3) → Tasks 3/4/5 Step 5. ✓
- §2 global config → Task 1 Step 3. ✓
- §3 ArchUnit dependency + 3 guard tests → Task 1 Steps 1–2, Tasks 3/4/5 Steps 1–2. ✓
- §3.5 tests (RestControllerIT, DocumentationIT, E2EIT, TasksRestClientTests) → Task 2 Step 2; Tasks 3/4/5 Steps 6–8. ✓
- §3.6 docs (api-guide.adoc ×3, rest.md, README, TODO) → Tasks 3/4/5 Step 9; Task 6. ✓
- §5 TODO line 23 + line 79 → Task 6 Step 3. ✓
- §9 acceptance (mvn verify, guard fails on regression) → Task 7. ✓

**Placeholder scan:** No TBD/TODO; every code step shows full content; the
ArchUnit test is repeated verbatim per service (not "similar to Task N") so
tasks can be executed out of order.

**Type consistency:** `JsonNamingConventionTest`, `notRenameViaJsonProperty()`,
`requestResponseDtoFieldsUseCamelCaseJson`, and `FIELD_ERRORS_PROPERTY` are used
identically across all tasks. The `RestDocsConstrainedFields.withPath(String)`
single-arg overload (verified to exist) is the collapse target for every
two-arg call.
