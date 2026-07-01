# Authentication Exception Contracts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every `asapp-authentication-service` port and use-case interface declare its full application/domain exception contract, translate the framework credentials exception into an owned type at the adapter boundary, unify the login 401 response shape, and record the principle as a project rule.

**Architecture:** Hexagonal (Ports & Adapters) + DDD. The only behavioral change is Task 1 (credentials failure now returns a ProblemDetail body via an owned `InvalidCredentialsException` instead of an empty body via the security entry point). Tasks 2–4 are Javadoc/rule changes with no runtime behavior change.

**Tech Stack:** Spring Boot 4.0.5, Java 25, Spring MVC, Spring Security, JUnit 5, AssertJ, BDDMockito, JSONAssert (`assertThatJson`), RestTestClient, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-06-30-authentication-exception-contracts-design.md`

## Global Constraints

- **`@since` on new production types:** use the current module POM version `0.4.0`.
- **Exception placement & hierarchy** (`ports-adapters.md`): orchestration exceptions live in `application/authentication/`; orchestration subtypes extend `AuthenticationException`.
- **No restated `@Override` Javadoc** (`code-style.md`): an overriding method must not repeat the interface contract; document only behavior beyond it via `{@inheritDoc}`, otherwise remove the comment entirely (no bare `{@inheritDoc}`).
- **Removing `@throws`/`{@link}` Javadoc can orphan imports** — the pre-commit hook does not flag them; run `mvn spotless:apply` and delete unused imports.
- **AssertJ only**, `catchThrowable()` for exceptions, BDDMockito syntax, test names `<Behavior>_<Condition>` (`testing-core.md` / `testing-unit.md`).
- **Maven command policy:** fast commands (compile, `*Tests` via surefire, `spotless`) run autonomously; **slow commands that run `*IT` integration tests (`mvn verify`, `mvn install`, `-Pfull`) require asking the developer first.**
- **Commits:** Conventional Commits with a bulleted body; footer line `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`. Pre-commit hooks validate format + LF endings + message.
- **Branch:** work continues on `feat/complete-authentication-exception-contracts` (already created; spec already committed there).

---

### Task 1: Translate the framework credentials exception (behavioral)

Introduce an owned `InvalidCredentialsException`, translate Spring's `BadCredentialsException` at the adapter, map it to a 401 ProblemDetail, and update its contract Javadoc and tests. After this task a wrong-password / unknown-user login returns a ProblemDetail body (was empty body).

**Files:**
- Create: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/InvalidCredentialsException.java`
- Modify: `.../application/authentication/AuthenticationException.java` (add `(String, Throwable)` constructor)
- Modify: `.../application/authentication/out/CredentialsAuthenticator.java` (declare `@throws`, drop stale prose)
- Modify: `.../application/authentication/in/AuthenticateUseCase.java` (add `@throws`)
- Modify: `.../infrastructure/authentication/out/CredentialsAuthenticatorAdapter.java` (narrow catch, throw owned type, fix imports/Javadoc)
- Modify: `.../infrastructure/error/GlobalExceptionHandler.java` (add to `handleInvalidCredentials` group + import + Javadoc)
- Test: `.../infrastructure/authentication/out/CredentialsAuthenticatorAdapterTests.java` (3 tests)
- Test: `.../infrastructure/error/GlobalExceptionHandlerTests.java` (extend `invalidCredentialExceptions` stream)
- Test: `.../infrastructure/authentication/AuthenticationE2EIT.java` (2 tests)

**Interfaces:**
- Produces: `InvalidCredentialsException(String message, Throwable cause)` in package `com.attrigo.asapp.authentication.application.authentication`, extending `AuthenticationException`.
- Produces: new `AuthenticationException(String message, Throwable cause)` constructor (existing `(String)` constructor stays).

**Confirmed unaffected (do not change):** `AuthenticationApiDocumentationIT.DocumentsInvalidCredentials` (triggers 401 via a mocked `AuthenticationNotFoundException`, already a ProblemDetail) and `SecurityConfigurationIT` (its empty-body cases are the JWT resource-server filter path and actuator Basic-auth, not the login path).

- [ ] **Step 1: Update the adapter unit test (red)**

In `CredentialsAuthenticatorAdapterTests.java`: add the import
`import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;`
then replace the three failure tests (keep `ReturnsAuthenticatedUser_ValidCredentials` as-is):

```java
        @Test
        void ThrowsInvalidCredentialsException_AuthenticationFails() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");

            willThrow(new BadCredentialsException("Invalid credentials")).given(authenticationManager)
                                                                         .authenticate(any(UsernamePasswordAuthenticationToken.class));

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(InvalidCredentialsException.class)
                              .hasMessage("Invalid credentials")
                              .hasCauseInstanceOf(BadCredentialsException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThrowsInvalidPrincipalException_PrincipalNotCustomUserDetails() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var authorities = AuthorityUtils.createAuthorityList(USER.name());
            var authenticationToken = new UsernamePasswordAuthenticationToken("invalid_principal", null, authorities);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPrincipalException.class)
                              .hasMessage("Authentication principal must contain the ID of the user");

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThrowsRoleNotFoundException_EmptyAuthorities() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");
            var userDetails = new CustomUserDetails(userId, username.value(), password.value(), AuthorityUtils.NO_AUTHORITIES);
            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, AuthorityUtils.NO_AUTHORITIES);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(RoleNotFoundException.class)
                              .hasMessage("Authentication authorities must contain at least one role");

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }
```

Also update the class Javadoc Coverage bullets: replace "Propagates authentication failures from Spring Security" with "Translates Spring Security authentication failures to a domain exception", and "Handles invalid principal type with domain exception" with "Propagates principal and role extraction failures".

- [ ] **Step 2: Run the adapter test — verify it fails to compile / fails**

Run: `mvn -pl services/asapp-authentication-service -am test -Dtest=CredentialsAuthenticatorAdapterTests`
Expected: FAIL — `InvalidCredentialsException` does not exist (compilation error).

- [ ] **Step 3: Create `InvalidCredentialsException`**

Create the file with the standard Apache license header (copy the 15-line header from any sibling exception, e.g. `AuthenticationNotFoundException.java`), then:

```java
package com.attrigo.asapp.authentication.application.authentication;

/**
 * Exception thrown when user credentials are invalid.
 * <p>
 * Indicates that authentication failed because the supplied username and password did not match a known active user.
 *
 * @since 0.4.0
 * @author attrigo
 */
public class InvalidCredentialsException extends AuthenticationException {

    /**
     * Constructs a new {@code InvalidCredentialsException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of the authentication failure
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

- [ ] **Step 4: Add the `(String, Throwable)` constructor to `AuthenticationException`**

In `AuthenticationException.java`, add below the existing `(String)` constructor:

```java
    /**
     * Constructs a new {@code AuthenticationException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
```

- [ ] **Step 5: Translate in `CredentialsAuthenticatorAdapter`**

Imports: remove `import org.springframework.security.authentication.BadCredentialsException;`; add
`import org.springframework.security.core.AuthenticationException;` and
`import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;`.

Replace the `authenticate` override (delete its Javadoc block — the port now owns the contract):

```java
    @Override
    public UserAuthentication authenticate(Username username, RawPassword password) {
        logger.debug("[CREDENTIALS_AUTH] Authenticating credentials with username={}", username);

        try {
            var authenticationToken = authenticateUsernamePassword(username, password);

            return buildUserAuthentication(authenticationToken);
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid credentials", e);
        }
    }
```

Update the private `authenticateUsernamePassword` Javadoc `@throws` line to:

```java
     * @throws AuthenticationException if the credentials are invalid
```

(`AuthenticationException` here resolves to the imported Spring type.)

- [ ] **Step 6: Run the adapter test — verify it passes**

Run: `mvn -pl services/asapp-authentication-service -am test -Dtest=CredentialsAuthenticatorAdapterTests`
Expected: PASS (4 tests).

- [ ] **Step 7: Extend the handler unit test (red)**

In `GlobalExceptionHandlerTests.java`, add `InvalidCredentialsException` to the existing parameterized source so the case is covered:

```java
        private static Stream<RuntimeException> invalidCredentialExceptions() {
            return Stream.of(new InvalidCredentialsException("Invalid credentials", new RuntimeException("bad credentials")),
                    new InvalidUsernameException("Username must be a valid email address"),
                    new InvalidPasswordException("Raw password must be between 8 and 64 characters"),
                    new InvalidEncodedTokenException("Encoded token must be a valid JWT format"));
        }
```

Add `import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;`.

- [ ] **Step 8: Run the handler test**

Run: `mvn -pl services/asapp-authentication-service -am test -Dtest=GlobalExceptionHandlerTests`
Expected: PASS once `InvalidCredentialsException` exists — the parameterized test invokes `handleInvalidCredentials(...)` directly, so it is green even before registration. This unit test asserts the response shape but does NOT prove Spring routes the exception to the handler; that routing is proven end-to-end by the E2E test in Step 14. Register the mapping in Step 9 so routing is correct at runtime.

- [ ] **Step 9: Register the handler mapping**

In `GlobalExceptionHandler.java` add `import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;` and prepend it to the existing group:

```java
    @ExceptionHandler({ InvalidCredentialsException.class, InvalidUsernameException.class, InvalidPasswordException.class, InvalidEncodedTokenException.class })
    protected ResponseEntity<ProblemDetail> handleInvalidCredentials(RuntimeException ex) {
```

Update that handler's Javadoc: change the summary's first sentence to "Handles invalid-credential exceptions (bad format or credential mismatch)." and update the `@param ex` enumeration to include `{@link InvalidCredentialsException}`.

- [ ] **Step 10: Run the handler test — verify it passes**

Run: `mvn -pl services/asapp-authentication-service -am test -Dtest=GlobalExceptionHandlerTests`
Expected: PASS.

- [ ] **Step 11: Update the port and use-case Javadoc**

In `CredentialsAuthenticator.java`: add `import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;`; remove the two stale paragraphs about "infrastructure layer / security mechanism" from the type and method Javadoc; add to the method:

```java
     * @throws InvalidCredentialsException if the credentials are invalid
```

In `AuthenticateUseCase.java`: add `import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;` and add the `@throws` line (place after the existing credential-format throws):

```java
     * @throws InvalidCredentialsException if the username and password do not match a known user
```

- [ ] **Step 12: Update the two E2E login-failure tests**

In `AuthenticationE2EIT.java`, replace `ReturnsStatusUnauthorizedAndEmptyBody_UsernameNotExists` and `ReturnsStatusUnauthorizedAndEmptyBody_NonMatchingPassword` (mirroring the sibling `ReturnsStatusUnauthorizedAndBodyWithGenericMessage_UsernameNotEmailFormat`):

```java
        @Test
        void ReturnsStatusUnauthorizedAndBodyWithGenericMessage_UsernameNotExists() {
            // Given
            var authenticateRequestBody = new AuthenticateRequest("user_not_exist@asapp.com", "TEST@09_password?!");

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(authenticateRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isObject()
                                  .containsEntry("title", "Authentication Failed")
                                  .containsEntry("status", 401)
                                  .containsEntry("detail", "Invalid credentials")
                                  .containsEntry("error", "invalid_grant")
                                  .containsEntry("instance", "/asapp-authentication-service/api/auth/token");

            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithGenericMessage_NonMatchingPassword() {
            // Given
            var createdUser = createUser();
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "password_not_match");

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(authenticateRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isObject()
                                  .containsEntry("title", "Authentication Failed")
                                  .containsEntry("status", 401)
                                  .containsEntry("detail", "Invalid credentials")
                                  .containsEntry("error", "invalid_grant")
                                  .containsEntry("instance", "/asapp-authentication-service/api/auth/token");

            assertAuthenticationNotExist();
        }
```

- [ ] **Step 13: Format and compile**

Run: `mvn spotless:apply` then `mvn -pl services/asapp-authentication-service -am test-compile`
Expected: BUILD SUCCESS, no unused-import errors.

- [ ] **Step 14: Slow verification — request approval first**

This runs the `*IT`/`*E2EIT` suite (Testcontainers, slow). Per the Maven policy, ask the developer before running.
Run: `mvn -pl services/asapp-authentication-service verify`
Expected: PASS, including the two updated `AuthenticationE2EIT` cases. If approval is deferred, note that the E2E assertions are verified at the final gate and proceed to commit (the fast adapter + handler unit tests already gate the behavior).

- [ ] **Step 15: Commit**

```bash
git add services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/InvalidCredentialsException.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/AuthenticationException.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/out/CredentialsAuthenticator.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/AuthenticateUseCase.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/CredentialsAuthenticatorAdapter.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/error/GlobalExceptionHandler.java \
        services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/CredentialsAuthenticatorAdapterTests.java \
        services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/error/GlobalExceptionHandlerTests.java \
        services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/AuthenticationE2EIT.java
git commit -m "fix(authentication): translate framework credentials exception to a domain exception

- Add InvalidCredentialsException (application tier) and an
  AuthenticationException (message, cause) constructor
- Translate Spring's AuthenticationException in the credentials adapter
  and map it to a 401 ProblemDetail in the global handler
- Declare the owned exception on the CredentialsAuthenticator port and
  the AuthenticateUseCase, and assert the body in the affected tests

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Correct the refresh and revoke use-case contracts (Javadoc)

No behavior change. Fix the wrong `IllegalArgumentException` (the real type is `InvalidEncodedTokenException`), add the missing thrown types, and remove the now-redundant `@throws` from the implementations.

**Files:**
- Modify: `.../application/authentication/in/RefreshAuthenticationUseCase.java`
- Modify: `.../application/authentication/in/RevokeAuthenticationUseCase.java`
- Modify: `.../application/authentication/in/service/RefreshAuthenticationService.java`
- Modify: `.../application/authentication/in/service/RevokeAuthenticationService.java`

**Interfaces:**
- Consumes (already exist): `InvalidEncodedTokenException` (`domain.authentication`), `InvalidJwtException`, `TokenStoreException`, `AuthenticationPersistenceException`, `AuthenticationNotFoundException`, `UnexpectedJwtTypeException`, `CompensatingTransactionException` (all `application.authentication`).

- [ ] **Step 1: Fix `RefreshAuthenticationUseCase` contract**

Add imports:
`import com.attrigo.asapp.authentication.application.authentication.InvalidJwtException;`
`import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;`
Replace the method `@throws` block with:

```java
     * @throws InvalidEncodedTokenException     if the refresh token is null, blank, or not a valid JWT format
     * @throws InvalidJwtException              if the token is malformed, expired, or fails signature verification
     * @throws UnexpectedJwtTypeException       if the provided token is not a refresh token
     * @throws AuthenticationNotFoundException  if the token is not found in active sessions or repository
     * @throws TokenStoreException              if token rotation fails (after compensation)
     * @throws CompensatingTransactionException if compensating transaction fails
```

- [ ] **Step 2: Fix `RevokeAuthenticationUseCase` contract**

Replace the import `com.attrigo.asapp.authentication.application.PersistenceException` with:
`import com.attrigo.asapp.authentication.application.authentication.AuthenticationPersistenceException;`
`import com.attrigo.asapp.authentication.application.authentication.InvalidJwtException;`
`import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;`
`import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;`
Replace the method `@throws` block with:

```java
     * @throws InvalidEncodedTokenException        if the access token is null, blank, or not a valid JWT format
     * @throws InvalidJwtException                 if the token is malformed, expired, or fails signature verification
     * @throws UnexpectedJwtTypeException          if the provided token is not an access token
     * @throws AuthenticationNotFoundException     if the token is not found in active sessions or repository
     * @throws AuthenticationPersistenceException  if authentication deletion fails
     * @throws TokenStoreException                 if token deactivation fails
```

- [ ] **Step 3: Remove the redundant `@throws` from `RefreshAuthenticationService`**

Delete the `import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;` line. In the `refreshAuthentication` override Javadoc, delete only the `@throws InvalidEncodedTokenException ...` line (and the blank `*` separator before it), keeping the `{@inheritDoc}` and the "First updates repository…" note:

```java
    /**
     * {@inheritDoc}
     * <p>
     * First updates repository, then updates fast-access store. If fast-access store fails, old state is restored via compensating transaction.
     */
```

- [ ] **Step 4: Remove the redundant `@throws` from `RevokeAuthenticationService`**

Delete the `import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;` line. The `revokeAuthentication` override Javadoc has no content beyond `{@inheritDoc}` + the redundant `@throws`, so remove the entire Javadoc block (leave `@Override` and `@Transactional`):

```java
    @Override
    @Transactional
    public void revokeAuthentication(String accessToken) {
```

- [ ] **Step 5: Format, compile, and re-run the affected unit tests**

Run: `mvn spotless:apply`
Run: `mvn -pl services/asapp-authentication-service -am test -Dtest=RefreshAuthenticationServiceTests,RevokeAuthenticationServiceTests`
Expected: BUILD SUCCESS; no unused-import warnings; tests PASS (no behavior changed).

- [ ] **Step 6: Commit**

```bash
git add services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/RefreshAuthenticationUseCase.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/RevokeAuthenticationUseCase.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/service/RefreshAuthenticationService.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/service/RevokeAuthenticationService.java
git commit -m "docs(authentication): correct refresh and revoke use-case exception contracts

- Replace the wrong IllegalArgumentException with InvalidEncodedTokenException
- Declare the missing InvalidJwtException on both use cases and
  TokenStoreException on revoke; specialize to AuthenticationPersistenceException
- Drop the now-redundant @throws from the service implementations

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Document the output-port contracts (Javadoc)

No behavior change. Lift the typed-exception `@throws` from the adapter `@Override`s onto the ports that own the contract, then remove the redundant impl Javadoc.

**Files:**
- Modify: `.../application/authentication/out/JwtAuthenticationRepository.java`
- Modify: `.../infrastructure/authentication/out/JwtAuthenticationRepositoryAdapter.java`
- Modify: `.../application/authentication/out/TokenStore.java`
- Modify: `.../infrastructure/authentication/out/RedisJwtStore.java`
- Modify: `.../domain/user/PasswordService.java`
- Modify: `.../infrastructure/user/out/PasswordServiceAdapter.java`

- [ ] **Step 1: Declare on `JwtAuthenticationRepository`**

Add `import com.attrigo.asapp.authentication.application.authentication.AuthenticationPersistenceException;`. Add to the `deleteById` Javadoc:

```java
     * @throws AuthenticationPersistenceException if the persistence operation fails
```

Add the same `@throws` line to `deleteAllByUserId`.

- [ ] **Step 2: Remove redundant Javadoc from `JwtAuthenticationRepositoryAdapter`**

Delete the entire `{@inheritDoc}` + `@throws AuthenticationPersistenceException` Javadoc blocks above `deleteById` and `deleteAllByUserId` (keep `@Override`). The `AuthenticationPersistenceException` import stays (used by `throw new AuthenticationPersistenceException(...)`).

- [ ] **Step 3: Declare on `TokenStore`**

Add `import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;`. Add to the `save` Javadoc:

```java
     * @throws TokenStoreException if the store operation fails
```

Add the same `@throws` line to `delete`.

- [ ] **Step 4: Remove redundant `@throws` from `RedisJwtStore`**

In the `save` and `delete` override Javadoc, delete only the `@throws TokenStoreException ...` line (and the blank `*` separator before it), keeping `{@inheritDoc}` and the "Redis Operations" notes. The `TokenStoreException` import stays (used by `throw new TokenStoreException(...)`).

- [ ] **Step 5: Declare on `PasswordService` and clean its adapter**

In `PasswordService.java`, add to the `encode` Javadoc:

```java
     * @throws IllegalArgumentException if the raw password is invalid
```

In `PasswordServiceAdapter.java`, delete the entire `{@inheritDoc}` + `@throws IllegalArgumentException` Javadoc block above `encode` (keep `@Override`). No import change (`IllegalArgumentException` is `java.lang`).

- [ ] **Step 6: Format and compile**

Run: `mvn spotless:apply` then `mvn -pl services/asapp-authentication-service -am test-compile`
Expected: BUILD SUCCESS; no unused-import warnings.

- [ ] **Step 7: Commit**

```bash
git add services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/out/JwtAuthenticationRepository.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/JwtAuthenticationRepositoryAdapter.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/out/TokenStore.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/RedisJwtStore.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/domain/user/PasswordService.java \
        services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/out/PasswordServiceAdapter.java
git commit -m "docs(authentication): document persistence, token-store and password port exceptions

- Declare AuthenticationPersistenceException on the JWT repository deletes
- Declare TokenStoreException on the token store save and delete
- Declare IllegalArgumentException on the password service encode
- Remove the now-redundant @throws from the corresponding adapters

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Record the exception-contract rule (developer-applied)

`.claude/` writes are denied by the auto-mode permission gate. **Do not attempt to write the file silently** — propose the exact edit and ask the developer to apply (or approve) it.

**Files:**
- Modify: `.claude/rules/ports-adapters.md` (the `## Exception Handling` section)

- [ ] **Step 1: Present the exact edit to the developer**

Replace the first two bullets of `## Exception Handling` with these three:

```markdown
- An interface owns its exception contract: each port and use-case interface declares every application/domain exception its callers can observe; adapters translate framework/infrastructure failures into those declared types at the boundary, so no framework type escapes through an application interface
- Translate a framework/infrastructure exception into an application exception at the adapter when the application service must catch it (e.g. `TokenStoreException` for compensation) or when the interface contract requires an owned type (e.g. `InvalidCredentialsException` on the login path); otherwise let it propagate to `GlobalExceptionHandler` or the Spring Security filters
- Wrapping `BadCredentialsException` / Spring `AuthenticationException` subtypes is path-dependent: inside a Security filter (the JWT resource-server path) never wrap — `ExceptionTranslationFilter` and the entry point must see the framework type; inside a use-case-invoked adapter (the login path, run under the controller) translate to an owned application exception so `GlobalExceptionHandler` owns the response
```

Leave the "Exception placement" bullet and the hierarchy table unchanged.

- [ ] **Step 2: Confirm applied, then commit**

After the developer applies the edit, commit:

```bash
git add .claude/rules/ports-adapters.md
git commit -m "docs(rules): record that interfaces own their exception contract

- State the interface-owns-its-exception-contract principle
- Scope the BadCredentialsException wrapping ban to the Security filter
  path and allow translation on the login use-case path

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Final verification (whole task, slow — request approval)

Per the Maven policy, ask the developer before running the integration-test tiers.

- [ ] Run `mvn clean verify` — full unit + integration + E2E suite passes.
- [ ] Run `mvn clean install -Pfull` — coverage, Javadoc generation (inheritance resolves the removed impl comments), sources, and style check all pass.
- [ ] Tick line 64's four subtasks in `TODO.md` (handled by the close-task flow, not this plan).
