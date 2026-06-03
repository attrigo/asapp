# Error Handler Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Update (2026-05-30):** Phase A (the `ParameterLocation` origin field) was later reversed — the `location` field was removed from the validation-error record. Phases B–D (sort, constants, fixed detail) shipped and remain valid; the sort is now keyed on `(field, message)`. This plan is retained as a historical record.

**Goal:** Implement the four Error Handler TODOs from Version 0.4.0: deterministic validation-error sort, per-service `ErrorMessages` constants class, fixed `ProblemDetail.detail` values, and `ParameterLocation` enum replacing `entity` on `InvalidRequestParameter`.

**Architecture:** Each of three services (`asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`) owns an identical-shaped `GlobalExceptionHandler` under `infrastructure/error/`. Changes are applied symmetrically across all three services. Handler triplication is preserved; no shared commons handler is introduced. Sequencing favors landing the biggest-blast-radius change first (record shape).

**Tech Stack:** Java 25, Spring Boot 4.0.5, Spring MVC, Jakarta Bean Validation 3.x, JUnit 5, AssertJ, Mockito, MockMvc.

**Spec:** `docs/superpowers/specs/2026-05-28-error-handler-design.md`

**Sequencing:** 4 PRs, each touching all 3 services together:
- PR 1 (Phase A) — Task 4: `ParameterLocation` enum + record refactor
- PR 2 (Phase B) — Task 1: deterministic sort
- PR 3 (Phase C) — Task 2: extract `ErrorMessages` per service
- PR 4 (Phase D) — Task 3: fixed `ProblemDetail.detail` constants

---

## Phase A — ParameterLocation Origin (Task 4)

Starts with the biggest-impact change: replacing `InvalidRequestParameter.entity` with a `ParameterLocation` enum, and populating it correctly for body and method-parameter validation. Tasks-service is implemented first (it has both body validation and constraint-violation paths); users-service mirrors it; authentication-service follows (body-only).

### Task A1: Add `ParameterLocation` enum to tasks-service

**Files:**
- Create: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/ParameterLocation.java`

- [ ] **Step 1: Create the enum file**

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

package com.bcn.asapp.tasks.infrastructure.error;

/**
 * Identifies the HTTP request location of an invalid parameter in validation error responses.
 * <p>
 * Used by {@link InvalidRequestParameter} to disambiguate body fields from path, query, and header parameters.
 *
 * @since 0.4.0
 * @author attrigo
 */
public enum ParameterLocation {

    BODY,
    PATH,
    QUERY,
    HEADER

}
```

- [ ] **Step 2: Verify compilation**

Run: `cd services/asapp-tasks-service && mvn compile -q`

Expected: BUILD SUCCESS.

### Task A2: Refactor `InvalidRequestParameter` record in tasks-service

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/InvalidRequestParameter.java`

- [ ] **Step 1: Replace `entity` with `location`**

Replace the existing record contents with:

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

package com.bcn.asapp.tasks.infrastructure.error;

/**
 * Represents an invalid request parameter in validation errors.
 * <p>
 * Contains the request location (body, path, query, header), the field name, and the validation message.
 *
 * @param location the HTTP request location of the invalid parameter
 * @param field    the field name that failed validation
 * @param message  the validation error message
 * @since 0.4.0
 * @author attrigo
 */
public record InvalidRequestParameter(
        ParameterLocation location,
        String field,
        String message
) {}
```

- [ ] **Step 2: Compile — handler will break, expected**

Run: `cd services/asapp-tasks-service && mvn compile -q`

Expected: BUILD FAILURE with errors on `GlobalExceptionHandler` lines 305 and 325 (both `new InvalidRequestParameter(...)` calls now mismatch). This is the cue for Task A3.

### Task A3: Update tasks-service `GlobalExceptionHandler` body branch

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Update `buildInvalidParameters(List<FieldError>)` to emit `BODY`**

Locate the method at lines 304–311. Replace it with:

```java
private List<InvalidRequestParameter> buildInvalidParameters(List<FieldError> fieldErrors) {
    Function<FieldError, InvalidRequestParameter> fieldErrorMapper = fieldError -> new InvalidRequestParameter(ParameterLocation.BODY,
            fieldError.getField(), fieldError.getDefaultMessage());

    return fieldErrors.stream()
                      .map(fieldErrorMapper)
                      .toList();
}
```

Note: `fieldError.getObjectName()` is no longer used.

- [ ] **Step 2: Compile — constraint-violation branch still broken**

Run: `cd services/asapp-tasks-service && mvn compile -q`

Expected: BUILD FAILURE on the constraint-violation branch at line 325 only.

### Task A4: Add `resolveLocation` helper and update constraint-violation branch

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Add the required imports**

In the import block, add:

```java
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.validation.ElementKind;
import jakarta.validation.Path;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
```

- [ ] **Step 2: Replace `buildInvalidParameters(Set<ConstraintViolation<?>>)` to resolve location**

Locate the method at lines 319–328. Replace it with:

```java
private List<InvalidRequestParameter> buildInvalidParameters(Set<ConstraintViolation<?>> violations) {
    return violations.stream()
                     .map(v -> {
                         var path = v.getPropertyPath()
                                     .toString();
                         var field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                         return new InvalidRequestParameter(resolveLocation(v), field, v.getMessage());
                     })
                     .toList();
}

private static ParameterLocation resolveLocation(ConstraintViolation<?> violation) {
    Class<?> rootClass = violation.getRootBeanClass();
    Iterator<Path.Node> nodes = violation.getPropertyPath()
                                         .iterator();

    if (!nodes.hasNext()) {
        return ParameterLocation.QUERY;
    }
    String methodName = nodes.next()
                             .getName();

    if (!nodes.hasNext()) {
        return ParameterLocation.QUERY;
    }
    Path.Node paramNode = nodes.next();
    if (paramNode.getKind() != ElementKind.PARAMETER) {
        return ParameterLocation.QUERY;
    }
    Integer paramIndex = paramNode.as(Path.ParameterNode.class)
                                  .getParameterIndex();

    for (Method method : rootClass.getMethods()) {

        if (!method.getName()
                   .equals(methodName) || paramIndex >= method.getParameterCount()) {
            continue;
        }

        Parameter parameter = method.getParameters()[paramIndex];
        if (parameter.isAnnotationPresent(PathVariable.class)) {
            return ParameterLocation.PATH;
        }
        if (parameter.isAnnotationPresent(RequestHeader.class)) {
            return ParameterLocation.HEADER;
        }
        if (parameter.isAnnotationPresent(RequestParam.class)) {
            return ParameterLocation.QUERY;
        }
        return ParameterLocation.QUERY;
    }

    return ParameterLocation.QUERY;
}
```

- [ ] **Step 3: Add the missing `Iterator` import**

In the import block (java.util group), add:

```java
import java.util.Iterator;
```

- [ ] **Step 4: Verify compilation**

Run: `cd services/asapp-tasks-service && mvn compile -q`

Expected: BUILD SUCCESS.

### Task A5: Update tasks-service unit tests for new record shape

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Identify all `new InvalidRequestParameter(...)` references**

Run from the project root:

```
grep -n "new InvalidRequestParameter" services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java
```

Note each matched line — every constructor call needs the first argument changed from the binding-object string to a `ParameterLocation` enum constant.

- [ ] **Step 2: Update every `new InvalidRequestParameter(...)` call**

For body-validation tests (the ones using `MethodArgumentNotValidException` / `FieldError`), change the first argument from the existing string (e.g., `"createTaskRequest"`) to `ParameterLocation.BODY`. Example before/after:

Before:
```java
new InvalidRequestParameter("createTaskRequest", "title", "must not be empty")
```

After:
```java
new InvalidRequestParameter(ParameterLocation.BODY, "title", "must not be empty")
```

For constraint-violation tests, change to the actual expected location (typically `ParameterLocation.QUERY` if no annotation was specified on the fixture, `ParameterLocation.PATH` for `@PathVariable`-annotated parameter fixtures).

Add the import: `import com.bcn.asapp.tasks.infrastructure.error.ParameterLocation;` (or, if already in same package, no import needed).

- [ ] **Step 3: Add a same-field-multiple-violations fixture (will be reused in Phase B)**

In the body-validation test section, add a new test:

```java
@Test
void handleMethodArgumentNotValid_whenFieldHasMultipleViolations_returnsAllViolations() {
    // Given
    FieldError empty = new FieldError("createTaskRequest", "title", "must not be empty");
    FieldError tooShort = new FieldError("createTaskRequest", "title", "size must be between 3 and 50");
    var bindingResult = mock(BindingResult.class);
    given(bindingResult.getFieldErrors()).willReturn(List.of(empty, tooShort));
    var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

    // When
    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

    // Then
    var problemDetail = (ProblemDetail) response.getBody();
    @SuppressWarnings("unchecked")
    var errors = (List<InvalidRequestParameter>) problemDetail.getProperties().get("errors");
    assertThat(errors).hasSize(2)
                      .allMatch(e -> e.location() == ParameterLocation.BODY)
                      .allMatch(e -> "title".equals(e.field()));
}
```

If existing tests already cover the same shape under a different name, skip this step.

- [ ] **Step 4: Run tests**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests -q`

Expected: all tests pass.

### Task A6: Update tasks-service integration tests

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java`

- [ ] **Step 1: Identify JSON assertions on `entity`**

Run:

```
grep -n "errors\[" services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java
grep -n "\.entity" services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java
```

- [ ] **Step 2: Replace every `entity` JSON-path assertion with `location`**

For each match, change assertions like:

Before:
```java
.andExpect(jsonPath("$.errors[0].entity").value("createTaskRequest"))
```

After:
```java
.andExpect(jsonPath("$.errors[0].location").value("BODY"))
```

For any constraint-violation IT (e.g., a `@RequestParam @Size` test on `TaskRestController`), use `"QUERY"` instead of `"BODY"`.

- [ ] **Step 3: Add an IT for path-variable validation failure**

If `TaskRestController` has a `@PathVariable` validated parameter (look for `@Validated` on the controller class), add a test asserting `location = "PATH"`. Skeleton:

```java
@Test
void findById_whenIdIsInvalid_returnsBadRequestWithPathLocation() throws Exception {
    mockMvc.perform(get("/v1/tasks/{id}", "not-a-uuid"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors[0].location").value("PATH"))
           .andExpect(jsonPath("$.errors[0].field").value("id"));
}
```

If no `@PathVariable` is `@Validated` on this controller today, skip adding the test and note this in the PR description; do not invent new controller behavior.

- [ ] **Step 4: Run integration tests**

Run: `cd services/asapp-tasks-service && mvn verify -DskipUTs=false -Dit.test=TaskRestControllerIT -q`

Expected: all tests pass.

### Task A7: Mirror changes to users-service

**Files:**
- Create: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/ParameterLocation.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/InvalidRequestParameter.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java`
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerIT.java`

- [ ] **Step 1: Repeat A1–A6 against users-service paths**

The code is identical to tasks-service except for the package prefix (`com.bcn.asapp.users` instead of `com.bcn.asapp.tasks`). Apply the same edits to the equivalent files. Do NOT skip steps even if the diffs look mechanical — each file has its own copy of the handler.

- [ ] **Step 2: Run users-service tests**

Run: `cd services/asapp-users-service && mvn verify -q`

Expected: BUILD SUCCESS.

### Task A8: Apply changes to authentication-service (body validation only)

**Files:**
- Create: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/ParameterLocation.java`
- Modify: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/InvalidRequestParameter.java`
- Modify: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java`
- Modify (likely two files): `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/authentication/in/AuthenticationRestControllerIT.java` and `.../infrastructure/user/in/UserRestControllerIT.java`

- [ ] **Step 1: Apply A1, A2, and the body-only portion of A3 (the `buildInvalidParameters(List<FieldError>)` change)**

Do NOT add the `resolveLocation` helper or the constraint-violation overload — authentication-service does not handle `ConstraintViolationException`.

- [ ] **Step 2: Apply A5 to `GlobalExceptionHandlerTests.java`**

All `InvalidRequestParameter` constructor calls in authentication tests are body validations; use `ParameterLocation.BODY` for every one.

- [ ] **Step 3: Apply A6 to both authentication IT files**

Update every `entity` JSON path to `location` with value `"BODY"`. No constraint-violation IT exists here, so the `PATH` IT step is skipped.

- [ ] **Step 4: Build authentication-service**

Run: `cd services/asapp-authentication-service && mvn verify -q`

Expected: BUILD SUCCESS.

### Task A9: Update `.claude/rules/rest.md`

**Files:**
- Modify: `.claude/rules/rest.md`

- [ ] **Step 1: Replace the `InvalidRequestParameter` shape line**

Find the bullet that reads:

```
- Validation errors extend `ProblemDetail` with an `errors` property containing a list of `InvalidRequestParameter(entity, field, message)`
```

Replace with:

```
- Validation errors extend `ProblemDetail` with an `errors` property containing a list of `InvalidRequestParameter(location, field, message)` where `location` is a `ParameterLocation` enum (`BODY`, `PATH`, `QUERY`, `HEADER`)
```

- [ ] **Step 2: Confirm no other rule files reference the old shape**

Run:

```
grep -rn "InvalidRequestParameter(entity" .claude/
```

Expected: no matches.

### Task A10: Final Phase-A build and commit

- [ ] **Step 1: Full multi-module build**

Run: `mvn clean verify -q`

Expected: BUILD SUCCESS across all 10 modules.

- [ ] **Step 2: Stage Phase-A files only**

Stage each modified/created file explicitly (avoid `git add .`):

```
git add services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/ParameterLocation.java services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/InvalidRequestParameter.java services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/authentication/in/AuthenticationRestControllerIT.java services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/user/in/UserRestControllerIT.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/ParameterLocation.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/InvalidRequestParameter.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/task/in/TaskRestControllerIT.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/ParameterLocation.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/InvalidRequestParameter.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/in/UserRestControllerIT.java .claude/rules/rest.md
```

- [ ] **Step 3: Commit**

```
git commit -m "feat(error-handler)!: replace InvalidRequestParameter.entity with ParameterLocation

- Add ParameterLocation enum (BODY, PATH, QUERY, HEADER) per service
- Replace InvalidRequestParameter.entity with a ParameterLocation field
- Resolve location for ConstraintViolation via controller-parameter annotations
- Update unit and integration tests to assert on the new location field
- Update .claude/rules/rest.md to reflect the new record shape

BREAKING CHANGE: error response 'errors[].entity' is replaced by 'errors[].location' carrying the enum name (BODY/PATH/QUERY/HEADER); clients reading 'entity' must migrate."
```

---

## Phase B — Deterministic Sort (Task 1)

Validation errors now carry `location`, so the sort comparator is well-defined. Per spec, sort by `(location, field, message)` immediately before assigning to the `errors` extension. This phase is small and TDD-friendly.

### Task B1: Add failing sort test in tasks-service

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Add the sort-order test**

Append the test below to the existing class. It builds two body field errors and two constraint violations in a deliberately scrambled order, and asserts the response orders them by `(location, field, message)`.

```java
@Test
void handleMethodArgumentNotValid_whenMixedFieldsOutOfOrder_sortsErrors() {
    // Given
    FieldError usernameSize = new FieldError("createTaskRequest", "username", "size must be between 3 and 30");
    FieldError usernameEmpty = new FieldError("createTaskRequest", "username", "must not be empty");
    FieldError passwordEmpty = new FieldError("createTaskRequest", "password", "must not be empty");
    var bindingResult = mock(BindingResult.class);
    given(bindingResult.getFieldErrors()).willReturn(List.of(usernameSize, usernameEmpty, passwordEmpty));
    var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

    // When
    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

    // Then
    var problemDetail = (ProblemDetail) response.getBody();
    @SuppressWarnings("unchecked")
    var errors = (List<InvalidRequestParameter>) problemDetail.getProperties().get("errors");
    assertThat(errors).containsExactly(
            new InvalidRequestParameter(ParameterLocation.BODY, "password", "must not be empty"),
            new InvalidRequestParameter(ParameterLocation.BODY, "username", "must not be empty"),
            new InvalidRequestParameter(ParameterLocation.BODY, "username", "size must be between 3 and 30")
    );
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests#handleMethodArgumentNotValid_whenMixedFieldsOutOfOrder_sortsErrors -q`

Expected: FAIL — actual order is the insertion order (passwordEmpty, usernameSize, usernameEmpty would all come from the list as-is, so `usernameSize` arrives before `usernameEmpty` in the response).

### Task B2: Implement sort in tasks-service handler

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Add the `Comparator` import**

In the `java.util` import group, add:

```java
import java.util.Comparator;
```

- [ ] **Step 2: Add a private static `SORT_ORDER` constant**

Below the existing `private static final String` constants block (above the `// 400 BAD REQUEST` section), add:

```java
private static final Comparator<InvalidRequestParameter> SORT_ORDER = Comparator.comparing(InvalidRequestParameter::location)
                                                                                .thenComparing(InvalidRequestParameter::field)
                                                                                .thenComparing(InvalidRequestParameter::message);
```

- [ ] **Step 3: Apply `.sorted(SORT_ORDER)` in both `buildInvalidParameters` overloads**

Body overload — change the terminal `.toList()` chain to:

```java
return fieldErrors.stream()
                  .map(fieldErrorMapper)
                  .sorted(SORT_ORDER)
                  .toList();
```

Constraint-violation overload — change to:

```java
return violations.stream()
                 .map(v -> {
                     var path = v.getPropertyPath()
                                 .toString();
                     var field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                     return new InvalidRequestParameter(resolveLocation(v), field, v.getMessage());
                 })
                 .sorted(SORT_ORDER)
                 .toList();
```

- [ ] **Step 4: Re-run the failing test**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests#handleMethodArgumentNotValid_whenMixedFieldsOutOfOrder_sortsErrors -q`

Expected: PASS.

- [ ] **Step 5: Run the entire test class to catch regressions**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests -q`

Expected: all tests pass. If a prior test asserted on a specific order that now changes (e.g., the body-validation IT in Phase A), update its expected order to match the new comparator.

### Task B3: Mirror sort changes to users-service and authentication-service

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java`
- Modify: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Apply Task B1 + B2 to users-service**

Same code as tasks-service.

- [ ] **Step 2: Apply Task B1 + B2 to authentication-service**

Skip the constraint-violation `.sorted(SORT_ORDER)` line — authentication-service handler has no constraint-violation overload.

- [ ] **Step 3: Full build**

Run: `mvn clean verify -q`

Expected: BUILD SUCCESS.

### Task B4: Commit Phase B

- [ ] **Step 1: Stage Phase-B files only**

```
git add services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java
```

If any Phase-A integration tests were adjusted in Step 5 of Task B2, include those files as well.

- [ ] **Step 2: Commit**

```
git commit -m "feat(error-handler): sort validation errors deterministically

- Add a Comparator<InvalidRequestParameter> SORT_ORDER per handler keyed on (location, field, message)
- Apply the comparator in both buildInvalidParameters overloads so the 'errors' array is stable
- Cover same-field-multiple-violations and mixed-field ordering in unit tests"
```

---

## Phase C — Per-Service `ErrorMessages` Class (Task 2)

Mechanical extraction: each service gets one new `ErrorMessages.java` next to its handler holding every title/detail/code currently defined either as a `private static final` inside the handler or inline at a use site.

### Task C1: Create `ErrorMessages` for authentication-service

**Files:**
- Create: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/ErrorMessages.java`

- [ ] **Step 1: Write the constants file**

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

package com.bcn.asapp.authentication.infrastructure.error;

/**
 * Centralizes error titles, fixed detail messages, and machine-readable error codes used by
 * {@link GlobalExceptionHandler}.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class ErrorMessages {

    // Titles
    static final String BAD_REQUEST_TITLE = "Bad Request";

    static final String INVALID_ARGUMENT_TITLE = "Invalid Argument";

    static final String AUTHENTICATION_FAILED_TITLE = "Authentication Failed";

    static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";

    static final String SERVICE_UNAVAILABLE_TITLE = "Service Unavailable";

    // Fixed details (Task 3 will swap dynamic detail call sites to these)
    static final String VALIDATION_FAILED_DETAIL = "Request validation failed";

    static final String INVALID_ARGUMENT_DETAIL = "Invalid argument provided";

    static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";

    static final String INVALID_TOKEN_DETAIL = "Invalid token";

    static final String INTERNAL_ERROR_DETAIL = "An internal error occurred";

    static final String SERVICE_UNAVAILABLE_DETAIL = "Service temporarily unavailable";

    // Error codes
    static final String ERROR_PROPERTY = "error";

    static final String INVALID_GRANT_ERROR = "invalid_grant";

    static final String SERVER_ERROR = "server_error";

    static final String TEMPORARILY_UNAVAILABLE_ERROR = "temporarily_unavailable";

    private ErrorMessages() {
    }

}
```

### Task C2: Migrate authentication-service handler to `ErrorMessages`

**Files:**
- Modify: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Delete the in-class constant block**

Remove lines 72–90 (the ten `private static final String` declarations).

- [ ] **Step 2: Add static import for clarity (optional but recommended)**

At the top of the imports block, add:

```java
import static com.bcn.asapp.authentication.infrastructure.error.ErrorMessages.*;
```

- [ ] **Step 3: Replace inline strings at all use sites**

| Line (pre-edit) | Before                              | After                          |
| --------------- | ----------------------------------- | ------------------------------ |
| 118             | `problemDetail.setTitle("Bad Request");`   | `problemDetail.setTitle(BAD_REQUEST_TITLE);` |
| 137             | `problemDetail.setTitle("Invalid Argument");` | `problemDetail.setTitle(INVALID_ARGUMENT_TITLE);` |
| 204             | `..., "Invalid token");`            | `..., INVALID_TOKEN_DETAIL);`  |

All other references (`AUTHENTICATION_FAILED_TITLE`, `INVALID_CREDENTIALS_DETAIL`, etc.) already used identifiers and now resolve via the static import.

- [ ] **Step 4: Compile and run tests**

Run: `cd services/asapp-authentication-service && mvn verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 5: Verify no inline title/detail/code strings remain in the handler**

Run:

```
grep -nE '"(Bad Request|Invalid Argument|Invalid token|Invalid credentials|Authentication Failed|Internal Server Error|Service Unavailable|invalid_grant|server_error|temporarily_unavailable|An internal error occurred|Service temporarily unavailable|Request validation failed|Invalid argument provided)"' services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java
```

Expected: zero matches.

### Task C3: Create `ErrorMessages` and migrate tasks-service handler

**Files:**
- Create: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/ErrorMessages.java`
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Copy the C1 `ErrorMessages` template verbatim, changing the package to `com.bcn.asapp.tasks.infrastructure.error`**

All 14 constants from C1 apply to tasks-service: the handler has body validation (`BAD_REQUEST_TITLE`, `VALIDATION_FAILED_DETAIL`), domain exceptions (`INVALID_ARGUMENT_*`), JWT handlers (`AUTHENTICATION_FAILED_TITLE`, `INVALID_CREDENTIALS_DETAIL`, `INVALID_TOKEN_DETAIL`, `INVALID_GRANT_ERROR`, `ERROR_PROPERTY`), `DataAccessException` (`INTERNAL_*`, `SERVER_ERROR`), and Redis (`SERVICE_UNAVAILABLE_*`, `TEMPORARILY_UNAVAILABLE_ERROR`).

- [ ] **Step 2: Repeat C2 against the tasks-service handler**

| Line (pre-edit) | Before                              | After                          |
| --------------- | ----------------------------------- | ------------------------------ |
| 110             | `problemDetail.setTitle("Bad Request");`   | `problemDetail.setTitle(BAD_REQUEST_TITLE);` |
| 129             | `problemDetail.setTitle("Invalid Argument");` | `problemDetail.setTitle(INVALID_ARGUMENT_TITLE);` |
| 151             | `problemDetail.setTitle("Bad Request");`   | `problemDetail.setTitle(BAD_REQUEST_TITLE);` |
| 198             | `..., "Invalid token");`            | `..., INVALID_TOKEN_DETAIL);`  |

Delete the in-class `private static final String` block (lines 64–82) and add the static import.

- [ ] **Step 3: Build and verify**

Run: `cd services/asapp-tasks-service && mvn verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 4: Grep for residual inline strings**

Same grep as C2 Step 5, against the tasks-service handler.

Expected: zero matches.

### Task C4: Create `ErrorMessages` and migrate users-service handler

**Files:**
- Create: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/ErrorMessages.java`
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Repeat C3 against users-service paths**

Same template, package `com.bcn.asapp.users.infrastructure.error`. Same line-by-line replacement table (users-service handler has the same structure as tasks-service).

- [ ] **Step 2: Build**

Run: `cd services/asapp-users-service && mvn verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 3: Grep for residual inline strings**

Expected: zero matches.

### Task C5: Final Phase-C build and commit

- [ ] **Step 1: Full multi-module build**

Run: `mvn clean verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Stage and commit**

```
git add services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/ErrorMessages.java services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/ErrorMessages.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/ErrorMessages.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java

git commit -m "refactor(error-handler): extract titles, details, and codes into per-service ErrorMessages

- Add ErrorMessages constants class in each service's infrastructure/error package
- Move every static error string out of GlobalExceptionHandler into ErrorMessages
- Inline the three remaining literals ('Bad Request', 'Invalid Argument', 'Invalid token')"
```

---

## Phase D — Fixed `ProblemDetail.detail` Constants (Task 3)

With `ErrorMessages` in place, the three dynamic-detail call sites swap to fixed constants. TDD-friendly because the assertion is just `assertThat(problemDetail.getDetail()).isEqualTo(ErrorMessages.VALIDATION_FAILED_DETAIL)`.

### Task D1: Add failing tests in tasks-service

**Files:**
- Modify: `services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Add three tests asserting fixed detail values**

Append to the test class:

```java
@Test
void handleMethodArgumentNotValid_setsFixedDetail() {
    // Given
    var bindingResult = mock(BindingResult.class);
    given(bindingResult.getFieldErrors()).willReturn(List.of());
    var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

    // When
    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

    // Then
    var problemDetail = (ProblemDetail) response.getBody();
    assertThat(problemDetail.getDetail()).isEqualTo(ErrorMessages.VALIDATION_FAILED_DETAIL);
}

@Test
void handleConstraintViolationException_setsFixedDetail() {
    // Given
    ConstraintViolationException ex = new ConstraintViolationException("ignored", Set.of());

    // When
    ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(ex);

    // Then
    assertThat(response.getBody().getDetail()).isEqualTo(ErrorMessages.VALIDATION_FAILED_DETAIL);
}

@Test
void handleIllegalArgumentException_setsFixedDetail() {
    // Given
    var ex = new IllegalArgumentException("any dynamic message");

    // When
    ResponseEntity<ProblemDetail> response = handler.handleIllegalArgumentException(ex);

    // Then
    assertThat(response.getBody().getDetail()).isEqualTo(ErrorMessages.INVALID_ARGUMENT_DETAIL);
}
```

- [ ] **Step 2: Run the three new tests**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests#handleMethodArgumentNotValid_setsFixedDetail+GlobalExceptionHandlerTests#handleConstraintViolationException_setsFixedDetail+GlobalExceptionHandlerTests#handleIllegalArgumentException_setsFixedDetail -q`

Expected: all three FAIL because `detail` currently echoes the exception message.

### Task D2: Replace dynamic detail with constants in tasks-service handler

**Files:**
- Modify: `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java`

- [ ] **Step 1: Edit the three call sites**

In `handleMethodArgumentNotValid` (line ~109):

```java
var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_DETAIL);
```

In `handleConstraintViolationException` (line ~150):

```java
var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_DETAIL);
```

In `handleIllegalArgumentException` (line ~128):

```java
var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_ARGUMENT_DETAIL);
```

(`VALIDATION_FAILED_DETAIL` and `INVALID_ARGUMENT_DETAIL` are resolved via the `import static ... ErrorMessages.*` added in Phase C.)

- [ ] **Step 2: Re-run the three tests**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests#handleMethodArgumentNotValid_setsFixedDetail+GlobalExceptionHandlerTests#handleConstraintViolationException_setsFixedDetail+GlobalExceptionHandlerTests#handleIllegalArgumentException_setsFixedDetail -q`

Expected: all three PASS.

- [ ] **Step 3: Run the full test class**

Run: `cd services/asapp-tasks-service && mvn -pl . test -Dtest=GlobalExceptionHandlerTests -q`

Expected: all tests pass. Any pre-existing test that previously asserted `detail` equals an exception message must be updated to assert the new fixed constant.

### Task D3: Mirror to users-service and authentication-service

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java`
- Modify: `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java`
- Modify: `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Apply D1 + D2 to users-service**

Identical to tasks-service.

- [ ] **Step 2: Apply D1 + D2 to authentication-service**

Skip the `handleConstraintViolationException` test and edit — the handler doesn't have that method. Only two test methods and two call-site edits apply.

- [ ] **Step 3: Update integration tests asserting on `detail`**

Search for IT assertions on `$.detail`:

```
grep -rn "\$\.detail" services/*/src/test/java/
```

For any test that previously expected an exception-message echo, update the expectation to the new fixed constant.

### Task D4: Final Phase-D build and commit

- [ ] **Step 1: Full multi-module build**

Run: `mvn clean verify -q`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Stage and commit**

```
git add services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandler.java services/asapp-tasks-service/src/test/java/com/bcn/asapp/tasks/infrastructure/error/GlobalExceptionHandlerTests.java services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandler.java services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/error/GlobalExceptionHandlerTests.java

git commit -m "refactor(error-handler): use fixed ProblemDetail.detail values

- Replace ex.getLocalizedMessage() with VALIDATION_FAILED_DETAIL in body and constraint-violation handlers
- Replace ex.getMessage() with INVALID_ARGUMENT_DETAIL in handleIllegalArgumentException
- Keep raw exception messages in server-side logs only (warn/error), never surfaced to clients"
```

---

## Phase E — Wrap-up

### Task E1: Update `TODO.md`

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Mark the four Error Handler items complete**

Change lines 17–20 from `[ ]` to `[X]`:

```
* Error Handler
    * [X] Return validation errors in a deterministic, sorted order
    * [X] Move all remaining inline error strings (titles, details, codes) into constants
    * [X] Use only fixed messages for `ProblemDetail.detail` (never exception messages)
    * [X] Make `InvalidRequestParameter` correctly represent the origin of validation errors for non-body parameters (path/query)
```

- [ ] **Step 2: Commit the TODO update separately**

```
git add TODO.md
git commit -m "docs: mark Error Handler tasks complete in TODO"
```

### Task E2: Sanity grep across the codebase

- [ ] **Step 1: Confirm no `InvalidRequestParameter(<string-literal>, ...)` survives**

Run:

```
grep -rn 'new InvalidRequestParameter("' services/
```

Expected: zero matches — all constructor first-arguments are now `ParameterLocation.*` enum constants.

- [ ] **Step 2: Confirm no exception-message echo remains in any handler**

Run:

```
grep -nE "forStatusAndDetail\([^,]+,\s*(ex\.|exception\.)" services/*/src/main/java/com/bcn/asapp/*/infrastructure/error/GlobalExceptionHandler.java
```

Expected: zero matches.

### Task E3: Optional — push the branch and open PRs

This plan was scoped to land as four commits on the `improve-error-handler` branch. Pushing and opening PRs is outside the plan; do that when ready.
