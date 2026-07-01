# Complete the Authentication Exception Contracts

**Status**: Implemented

## Context

The `asapp-authentication-service` interfaces under-document and in places mis-document the
exceptions they raise, and one framework exception leaks through the application layer.

**Use-case interfaces** (`application/authentication/in/`):

- `AuthenticateUseCase.authenticate` — the credential-mismatch failure is absent from the
  contract. `CredentialsAuthenticatorAdapter` rethrows Spring Security's
  `BadCredentialsException`, a framework type that escapes through the application port.
- `RefreshAuthenticationUseCase.refreshAuthentication` — documents
  `@throws IllegalArgumentException if the refresh token is invalid or blank`, but
  `EncodedToken.of(...)` only ever throws `InvalidEncodedTokenException` (null/blank **and**
  malformed). The documented type and its implied status (400) are both wrong — the real
  exception maps to 401. Also missing `InvalidJwtException`, which `TokenVerifier` declares.
- `RevokeAuthenticationUseCase.revokeAuthentication` — same wrong `IllegalArgumentException`;
  missing `InvalidJwtException`; missing `TokenStoreException` (the use case calls
  `tokenStore.delete`); documents the generic `PersistenceException` where the adapter throws
  the specific `AuthenticationPersistenceException`.

**Output ports** — the typed exception is documented only on the adapter `@Override`, not on
the port interface that owns the contract:

- `JwtAuthenticationRepository` deletes throw `AuthenticationPersistenceException`.
- `TokenStore` `save`/`delete` throw `TokenStoreException`.
- `PasswordService.encode` throws `IllegalArgumentException`.

**Inconsistent 401 shape.** Because `BadCredentialsException` propagates out of the
`DispatcherServlet`, `ExceptionTranslationFilter` routes it to `JwtAuthenticationEntryPoint`,
which returns a **401 with an empty body**. A bad-*format* username, by contrast, throws the
domain `InvalidUsernameException`, which `GlobalExceptionHandler` turns into a **401
ProblemDetail body**. The same endpoint returns two different error shapes depending on which
kind of bad credential it received.

This work completes line 64 of `TODO.md` ("Complete the exception contracts on interfaces")
and its four subtasks.

## Goal

Make each authentication interface declare its full application/domain exception contract,
translate the framework credentials exception into an owned type at the adapter boundary so no
framework exception escapes the application layer, unify the 401 response shape, and record the
"interface owns its exception contract" principle as a project rule.

## Decisions

Three design forks, resolved up front:

1. **Bad-credentials response** → ProblemDetail-body 401 (consistent with the bad-format case),
   not the current empty body.
2. **Contract completeness** → declare only application/domain exceptions a caller can act on
   and that map to distinct responses. Infrastructure-only failures that become a generic 500
   (`DataAccessException` on save, `JwtIssuanceException`) are **not** declared on application
   interfaces — declaring them would import infrastructure types into the application layer
   (violating `infrastructure → application → domain`) and they are already handled globally.
3. **Aggregate scope** → authentication aggregate only.

## Principle: an interface owns its exception contract

Each port and use-case interface declares, in its own Javadoc, every application- or
domain-level typed exception a caller can observe. Adapters translate framework/infrastructure
failures into those declared types at the boundary; a framework type (e.g.
`BadCredentialsException`) never escapes through an application interface. Implementation
`@Override` methods do not restate the inherited contract (consistent with commit `4fe4041d`,
"remove redundant implementation Javadoc inherited from ports") — they document only behavior
*beyond* it, via `{@inheritDoc}`.

This refines the existing `ports-adapters.md` guidance ("Only translate infrastructure
exceptions when the application service needs to catch them"): translation also happens purely
to keep the port contract honest, even when no service catches the result and it propagates
straight to `GlobalExceptionHandler`.

## Change 1 — Translate the credentials exception (subtask 3)

- **New exception** `InvalidCredentialsException` in `application/authentication/`, extending
  `AuthenticationException` (the orchestration tier, alongside `AuthenticationNotFoundException`
  and `UnexpectedJwtTypeException`). "Domain exception" in the TODO means an owned type rather
  than the framework's; the established hierarchy places authentication-failure types at the
  application tier.
- **`AuthenticationException`** gains a `(String message, Throwable cause)` constructor so the
  new subtype can preserve the framework cause for server-side logs (the public response stays
  generic). Existing `(String)` constructor and subtypes are unaffected.
- **`CredentialsAuthenticatorAdapter`** narrows its catch from `Exception` to Spring's
  `org.springframework.security.core.AuthenticationException` and throws
  `InvalidCredentialsException(message, cause)`. The port `CredentialsAuthenticator` now
  declares `@throws InvalidCredentialsException`, replacing the prose that deferred handling to
  "the security mechanism".
- **`GlobalExceptionHandler`** adds `InvalidCredentialsException` to the existing
  `handleInvalidCredentials(...)` `@ExceptionHandler` group → 401 ProblemDetail with the current
  `INVALID_CREDENTIALS_DETAIL` ("Invalid credentials") and `INVALID_GRANT_ERROR`. The generic
  message keeps the no-user-enumeration property (unknown-user and wrong-password are
  indistinguishable).

**Behavior change (intended):** wrong-password / unknown-user login moves from empty-body 401
to ProblemDetail-body 401, matching the bad-format case.

**Behavior change (minor, flagged):** narrowing the catch means the
`InvalidPrincipalException` / `RoleNotFoundException` raised during principal/role extraction no
longer get masked as a 401 — they propagate as a generic 500. These indicate server
misconfiguration (the principal is not a `CustomUserDetails`, or carries no authority) and are
unreachable on the normal path given `CustomUserDetailsService`, so a 500 is the more correct
outcome. No new handler is added for them.

## Change 2 — Use-case interface corrections (subtask 1)

| Interface | Correction |
|---|---|
| `AuthenticateUseCase` | Add `@throws InvalidCredentialsException`. Keep `InvalidUsernameException`, `InvalidPasswordException`, `TokenStoreException`. |
| `RefreshAuthenticationUseCase` | Replace `IllegalArgumentException` → `InvalidEncodedTokenException`; add `InvalidJwtException`. Keep `UnexpectedJwtTypeException`, `AuthenticationNotFoundException`, `TokenStoreException`, `CompensatingTransactionException`. |
| `RevokeAuthenticationUseCase` | Replace `IllegalArgumentException` → `InvalidEncodedTokenException`; add `InvalidJwtException` and `TokenStoreException`; specialize `PersistenceException` → `AuthenticationPersistenceException`. Keep `UnexpectedJwtTypeException`, `AuthenticationNotFoundException`. |

The refresh/revoke service `@Override`s currently carry `@throws InvalidEncodedTokenException`.
Once the interface owns that contract, those impl `@throws` become redundant restatements and
are removed. Removing `@throws`/`{@link}` Javadoc can orphan imports (the pre-commit hook does
not flag them); run `mvn spotless:apply` and remove any now-unused imports.

## Change 3 — Output-port doc additions (subtask 2)

Lift the typed-exception `@throws` from the adapter `@Override` onto the port interface; then
remove the now-redundant impl `@throws`:

| Port | Method(s) | Declares |
|---|---|---|
| `JwtAuthenticationRepository` | `deleteById`, `deleteAllByUserId` | `AuthenticationPersistenceException` |
| `TokenStore` | `save`, `delete` | `TokenStoreException` |
| `PasswordService` | `encode` | `IllegalArgumentException` |

`save` on the repositories (which can raise `DataAccessException`) and the `TokenIssuer`
contract (`JwtIssuanceException`) are deliberately left undeclared per decision 2 —
infrastructure-only, generic-500, not referencable from the application layer.

## Change 4 — Record the rule (subtask 4)

Update the "## Exception Handling" section of `.claude/rules/ports-adapters.md`:

1. Add the "an interface owns its exception contract" principle (one line, policy only — no
   restating of inferable Javadoc mechanics, per the project's rule-authoring convention).
2. Refine the blanket *"Never wrap `BadCredentialsException` or Spring's `AuthenticationException`
   subtypes"* line so it is scoped, not absolute:
   - **Inside a Security filter** (the `JwtAuthenticationFilter` resource-server path) — do not
     wrap; `ExceptionTranslationFilter` + the `AuthenticationEntryPoint` must see the framework
     type. Wrapping there breaks the filter chain.
   - **Inside a use-case-invoked adapter** (the login path, where `AuthenticationManager` runs
     under the controller, after the `DispatcherServlet`) — translate to an owned application
     exception so `GlobalExceptionHandler` owns the response and the port contract stays honest.

**Permission note:** edits under `.claude/` are denied by the auto-mode classifier. The rule
change will be surfaced for the developer to approve/apply rather than written silently.

## Testing impact

- `AuthenticationE2EIT` — the two bad-credentials cases
  (`ReturnsStatusUnauthorizedAndEmptyBody_UsernameNotExists`,
  `...EmptyBody_NonMatchingPassword`) change from empty body to ProblemDetail body; rename to
  `ReturnsStatusUnauthorizedAndBodyWithGenericMessage_*` and assert the body (title
  "Authentication Failed", status 401, detail "Invalid credentials", error "invalid_grant"),
  matching the existing format-error case.
- `CredentialsAuthenticatorAdapterTests` — assert `InvalidCredentialsException` instead of
  `BadCredentialsException`.
- `GlobalExceptionHandlerTests` — add an `InvalidCredentialsException` → 401 case.
- These are integration/E2E (slow) tests; per the project's Maven-permissions convention the
  developer is asked before the slow suites run.

## Verification

- `mvn clean install` — compiles; Javadoc generates (inheritance resolves removed impl
  comments). Run `-Pfull` for the javadoc + style-check profile.
- `mvn spotless:apply` — run after Javadoc edits to surface orphaned imports.
- `mvn clean verify` — exercises the updated E2E/integration assertions (slow tier; confirm
  first).

## Out of scope / non-goals

- User aggregate (`UserRepository`, `CreateUserUseCase`, `UpdateUserUseCase`, etc.).
- Wrapping infrastructure-only failures (`DataAccessException` on save, `JwtIssuanceException`)
  in new application exceptions to make them declarable.
- The `TokenIssuer` contract — `JwtIssuanceException` stays infrastructure-only.
- Any change to the resource-server `JwtAuthenticationFilter` path or its empty-body 401.

## Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-30-authentication-exception-contracts.md`)
were written before implementation, and the core change shipped substantially as designed: the
credentials-exception translation in Change 1 (`InvalidCredentialsException`,
`CredentialsAuthenticatorAdapter`, `GlobalExceptionHandler`'s `handleInvalidCredentials` group) and
the use-case/port `@throws` corrections in Changes 2 and 3 for `TokenStore` and `PasswordService`
landed as written. The canonical implementation is the current state of the real artifacts on the
branch — the authentication-service exception classes, use-case/port interfaces,
`GlobalExceptionHandler`, `CredentialsAuthenticatorAdapter`, and their tests — not this document.

Notable deltas:

- **Persistence exception wrappers removed (reverses Change 3's `JwtAuthenticationRepository` →
  `AuthenticationPersistenceException` row and Change 2's Revoke row that specialized
  `PersistenceException` → `AuthenticationPersistenceException`).** During manual review these
  three types — `application/PersistenceException`, `application/authentication/AuthenticationPersistenceException`,
  and `application/user/UserPersistenceException` — were found to be dead code: they existed only
  so a since-removed compensation saga could catch them, and `GlobalExceptionHandler` never
  handled them, so a DB delete failure surfaced as a raw 500. All three classes were deleted;
  `JwtAuthenticationRepositoryAdapter` and `UserRepositoryAdapter` no longer wrap
  `DataAccessException`; the persistence `@throws` was dropped from `JwtAuthenticationRepository`,
  `RevokeAuthenticationUseCase`, and `DeleteUserUseCase`. DB delete failures now reach
  `GlobalExceptionHandler` for a proper RFC-7807 500. (The other two Change 3 rows —
  `TokenStore` → `TokenStoreException` and `PasswordService` → `IllegalArgumentException` —
  shipped as designed.)
- **Scope extended beyond the authentication aggregate (softens Decision 3, which fixed scope to
  authentication only and listed the user aggregate as out of scope).** The dead persistence types
  spanned both aggregates, so the cleanup necessarily touched the user aggregate too:
  `UserPersistenceException` deleted, `UserRepositoryAdapter` de-wrapped, and the persistence
  contract dropped from `DeleteUserUseCase` / `DeleteUserService`, along with their tests.
- **Transaction Javadoc corrected across the use-case services (new work, not in this spec).** The
  class- and method-level Javadoc on `AuthenticateService`, `RefreshAuthenticationService`,
  `RevokeAuthenticationService`, and `DeleteUserService` had overstated atomicity ("no partial
  state") even though the Redis token store is not enlisted in the JDBC transaction; it was
  standardized to state that repository operations run in a transaction and the fast-access store
  is updated last, outside it. `AuthenticateService`'s class Javadoc also still described a
  compensation removed with the saga, which was fixed.
- **`handleInvalidCredentials` wording broadened (extends Change 1).** Beyond adding
  `InvalidCredentialsException` to the handler group, the handler's log message and Javadoc in
  `GlobalExceptionHandler` were rewritten so they describe credential *mismatch*, not just bad
  *format* — the Javadoc is now a per-exception bullet list of trigger conditions.
- **Exception-hierarchy table updated for the deletions (extends Change 4, same
  `.claude/rules/ports-adapters.md` file).** The table's Persistence-specific tier was removed
  because no instances remain, and its "Cross-domain base" row was relabeled "Cross-domain" and
  repointed at `CompensatingTransactionException`.

For future authentication exception-contract edits, treat the real artifacts — the interfaces,
adapters, `GlobalExceptionHandler`, and their tests — as the template; this spec is preserved as a
record of the original design intent.
