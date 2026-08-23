# Settle the Driven-Adapter Naming, Implementation, and Placement Conventions

**Status**: Implemented

## Context

`ports-adapters.md` carries three discriminators for driven adapters: an `Adapter` suffix for a wrapper
against a descriptive name for a class that *is* the implementation; a separate adapter when the
collaborator's shape mismatches the port or the collaborator is reused elsewhere, against a direct
implementation otherwise; and a cross-cutting direct implementation living in its own package rather than
the aggregate's `out/`.

All three came out of the 2026-07-22 rule review (findings M3, S8, N3), which fitted the rule to the two
classes that did not match the majority and logged the questions rather than settling them. The code was
never touched, and `TODO.md` has carried the open questions since.

Ten classes implement an output port. Eight follow one shape — a technology class in `persistence/`,
`security/`, or a client lib, wrapped by a `<Port>Adapter` in the aggregate's `out/`:

| Port | Adapter in `out/` | Technology it drives | Where that lives |
|---|---|---|---|
| `UserRepository` (authentication) | `UserRepositoryAdapter` | `JdbcUserRepository` | `user/persistence/` |
| `UserRepository` (users) | `UserRepositoryAdapter` | `JdbcUserRepository` | `user/persistence/` |
| `TaskRepository` | `TaskRepositoryAdapter` | `JdbcTaskRepository` | `task/persistence/` |
| `JwtAuthenticationRepository` | `JwtAuthenticationRepositoryAdapter` | `JdbcJwtAuthenticationRepository` | `authentication/persistence/` |
| `TasksGateway` | `TasksGatewayAdapter` | `TasksHttpClient` | `libs/asapp-http-clients` |
| `CredentialsAuthenticator` | `CredentialsAuthenticatorAdapter` | `AuthenticationManager` | Spring |
| `PasswordService` | `PasswordServiceAdapter` | `PasswordEncoder` | Spring |
| `TokenVerifier` | `TokenVerifierAdapter` | `JwtVerifier` | `infrastructure/security/` |

The two exceptions fuse the technology and the port implementation into one class, and disagree with each
other about where that class belongs:

| Class | Port | Package | Referenced outside its port |
|---|---|---|---|
| `RedisJwtStore` (198 lines, on `RedisTemplate`) | `TokenStore` | `authentication/out/` | Yes — `JwtVerifier` calls `accessTokenExists` / `refreshTokenExists` |
| `JwtIssuer` (226 lines, on Nimbus `MACSigner`) | `TokenIssuer` | `infrastructure/security/` | No |

Against the rule's own test the pair is inverted. The class that is reused, and therefore cross-cutting,
sits inside the aggregate; the class nothing but its port reaches sits in the shared package.

The `JwtVerifier` → `RedisJwtStore` edge is the only place a `security/` class depends on a port
implementation. The two comparable edges — `CustomUserDetailsService` → `JdbcUserRepository` and
`ExpiredJwtCleanupScheduler` → `JdbcJwtAuthenticationRepository` — land on technology classes in
`persistence/`, which is exactly the shape this design generalizes.

One more consequence is already on record: the port-boundary guardrail
(`2026-08-12-port-boundary-design.md`) rejected the declarative form of its output-port rule in part
because `JwtIssuer` implements a port from outside `out/`, and deferred that decision to this task.

## The decision

One shape, no exceptions.

- A class implementing an output port is named `<PortInterfaceName>Adapter` and lives in
  `infrastructure/<aggregate>/out/`.
- The adapter owns the translation — domain types to the technology's, and the technology's failures to
  the port's declared exceptions.
- The technology it drives stays outside `out/`, in its own infrastructure package, and speaks only
  technology types — `infrastructure/security/` for what the request-filter path also needs,
  `<aggregate>/persistence/` for datastore access, the home `architecture.md` already gives it. An adapter
  may equally drive a library or framework bean with no such class in between.

The descriptive-name carve-out and the cross-cutting-lives-outside-`out/` carve-out both go. What is left
is one test with no discriminator to remember: everything the application reaches is an `<Port>Adapter` in
`out/`; the technology behind it stays in its own package.

The convention is documentation only. No ArchUnit rule is added — see *What this does not settle*.

## The rule text

`## Naming` and `## Adapter vs. Direct Implementation` collapse into one section, placed where `## Naming`
sits today:

```markdown
## Driven Adapters

- Name a class implementing an output port `<PortInterfaceName>Adapter` and place it in `infrastructure/<aggregate>/out/` — whatever logic it adds, whatever it is built on
- The adapter owns the translation: domain types to the technology's, and the technology's failures to the port's declared exceptions
- The technology an adapter drives stays outside `out/`, in its own infrastructure package, and speaks only technology types — or the adapter drives a library or framework bean directly
```

Three bullets replace five. The `Adapter vs. Direct Implementation` heading disappears with the choice it
named.

`architecture.md` needs no change — its package tree already labels `out/` as *port adapters*, which
becomes exact rather than approximate.

## Applying it to the authentication service

The sibling subtask lands these; they are recorded here so the convention is checked against real code
rather than stated in the abstract.

| Today | After |
|---|---|
| `security/JwtIssuer implements TokenIssuer` | `authentication/out/TokenIssuerAdapter` — moved and renamed, body unchanged, `@RefreshScope` and the two `@Value` expirations travel with it |
| `authentication/out/RedisJwtStore implements TokenStore` | `security/RedisJwtStore` — a plain Redis component implementing no port |
| — | `authentication/out/TokenStoreAdapter implements TokenStore` |
| `security/JwtVerifier` → `authentication/out/RedisJwtStore` | `security/JwtVerifier` → `security/RedisJwtStore` |

Nothing else moves. The other eight adapters, the tasks service, and the users service are untouched.

### Splitting the Redis store

`RedisJwtStore` keeps the Redis key schema (`jwt:access_token:`, `jwt:refresh_token:`), the pipelined
`setEx` and `del` calls, and its `[REDIS_JWT_STORE]` trace logs. Its signatures drop to technology types:

| Now | After |
|---|---|
| `Boolean accessTokenExists(EncodedToken)` | `Boolean accessTokenExists(String)` |
| `Boolean refreshTokenExists(EncodedToken)` | `Boolean refreshTokenExists(String)` |
| `void save(JwtPair)` | `void save(String accessToken, Duration accessTtl, String refreshToken, Duration refreshTtl)` |
| `void delete(JwtPair)` | `void delete(String accessToken, String refreshToken)` |

The two `exists` signatures then match the tasks and users copies of the same class, which already take a
`String`.

`TokenStoreAdapter` picks up everything the store sheds: unwrapping `JwtPair` and `EncodedToken`, the
`calculateTtl` conversion from `Expiration` to a duration with its one-second floor, and the two
`catch (Exception)` blocks that raise `TokenStoreException`. The `exists` methods stay unwrapped, as they
are today.

That makes the adapter the opposite of a pass-through: it holds the only logic in the pair that is about
the domain rather than about Redis. `TokenStoreException` stays in `application/authentication/`, still
matching the rule's port/gateway-failure tier, and is now thrown from `out/` like every other port failure.

### Why placement follows sharing

Issuing tokens is an authentication use case with one caller, so `TokenIssuerAdapter` belongs to the
aggregate. Decoding, verifying, and session lookup serve the request-filter path in all three services, so
they stay in `security/`. Sharing, not the JWT family name, decides — which is why `JwtIssuer` leaves a
package it only ever sat in by association.

After the move, no `security/` class depends on a port implementation. The two remaining inbound edges
from `security/` reach `persistence/` technology classes, matching the pattern everywhere else.

## Tests

No behavior changes, so the existing suites are the regression net; the end-to-end tests cover the
authentication flows end to end.

| File | Change |
|---|---|
| `security/JwtIssuerTests` | → `authentication/out/TokenIssuerAdapterTests` (`git mv`, class and references renamed) |
| `authentication/out/RedisJwtStoreTests` | → `security/RedisJwtStoreTests` (`git mv`), assertions follow the `String`/`Duration` signatures |
| `authentication/out/RedisJwtStoreIT` | → `security/RedisJwtStoreIT` (`git mv`), same |
| `security/JwtVerifierTests` | import only — the mock type keeps its name |
| `authentication/out/TokenStoreAdapterTests` | new — TTL conversion, value unwrapping, and both `TokenStoreException` translations |

`TokenVerifierAdapter` and `PasswordServiceAdapter` have no unit tests today, being pure delegation. The
new `TokenStoreAdapter` is not in that category and gets one.

## Commits

One for this subtask beyond the design commit that carries this file — `docs(architecture)`, the
`ports-adapters.md` rewrite. The refactor belongs to the sibling subtask, *Refactor the mismatched
adapters to match the settled conventions*, and lands after it.

Until then the rule describes a state two classes do not yet meet. That window is deliberate: the TODO
splits settling the convention from aligning the code precisely so each is its own outcome, and the rule
is what the refactor will be measured against.

## What this does not settle

No ArchUnit rule. The convention is now mechanically checkable — an output-port implementation resides in
`..infrastructure..out` and has a name ending in `Adapter` — but enforcement was deliberately left out, in
line with the rest of `ports-adapters.md`, which is unenforced throughout.

The custom `ArchCondition` in `PortBoundaryRulesTests` stays. Once `JwtIssuer` moves, the declarative form
that spec rejected becomes accurate, but it is strictly looser: it would admit any class in an `out/`
package referencing any output port, where the condition admits only an implementation of that port.
Swapping it is a separate call, not a consequence of this one.

`PasswordService` keeps its declaration site. It is the only port declared in a domain package
(`domain/user/`) rather than `application/<aggregate>/out/`, and the port-boundary spec left the shape to
this task. Its adapter satisfies the convention as written — `PasswordServiceAdapter` in
`infrastructure/user/out/`, driving a framework bean — so nothing here needs it to move. Whether a domain
service port should be declared in the domain at all is a port-declaration question, and stays open.

The tasks and users services keep their `RedisJwtStore` exactly as it is: in `security/`, implementing no
port, reached only by `JwtVerifier`. This design brings the authentication copy toward them, not the
reverse.

The technology classes keep their descriptive names. `RedisJwtStore` and `JwtVerifier` say what they are
built on, which is the point of a technology class; the convention constrains only the classes that
implement ports.

One bullet is under review rather than settled. Bullet 3 asks the technology an adapter drives to speak
only technology types, which would make `JwtVerifier` non-conformant for taking `EncodedToken`. That
reading is too strict: the infrastructure layer is allowed to depend on the domain, which is the inward
direction the architecture requires — `LayerDependencyRulesTests` permits it explicitly, and all eight
already-conformant adapters rely on it. What the bullet is really protecting is portability: the tasks and
users services run their own `RedisJwtStore` against their own domains, so a store speaking
`authentication.domain.Expiration` could not hold the same shape across the three. That argues for keeping
`RedisJwtStore` technology-typed as a deliberate choice, not for a rule binding every technology class.
Narrowing bullet 3 to the classes shared across services, or dropping it, is its own task.

One gap survives that reading. `TokenIssuerAdapter` throws `JwtIssuanceException`, an infrastructure
exception, while `TokenIssuer` declares none on either method — so the failure crosses the port boundary
untranslated, which bullet 2 does not allow whatever types the collaborators speak. Whether the port
should declare it, or the adapter should raise a port-tier exception instead, is a follow-up.

## Verification

- `ports-adapters.md` holds the three bullets under `## Driven Adapters`; `## Adapter vs. Direct
  Implementation` is gone and its frontmatter glob is unchanged.
- No other rule, agent, or skill restates adapter naming or placement — `.claude/` mentions `Adapter` only
  as a design concept, never as a naming or placement constraint.
- Each of the ten port implementations in the table above is either already conformant or listed in
  *Applying it to the authentication service*.

## Post-implementation notes

The canonical implementation is `ports-adapters.md`'s `## Driven Adapters` bullets, the authentication
service's `out/` adapters, and the three `PortBoundaryRulesTests`, not this document.

Notable deltas:

- **Rule bullet 3 narrowed to placement only (revises "The rule text").** The "speaks only
  technology types" clause was over-strict; `ports-adapters.md`'s `## Driven Adapters` holds the
  shipped wording.

- **The store speaks value objects, not positional parameters (revises "Splitting the Redis
  store").** A transposable four-argument `save` was unsafe; `TokenKey` and `TokenEntry` now carry
  the key and TTL.

- **`TokenStore`'s existence checks were deleted, not kept (revises "Applying it to the
  authentication service").** Review proved both unreachable; `JwtVerifier` reaches Redis directly,
  so the port keeps only `save` and `delete`.

- **Issuance failure crosses the port and was renamed (revises "What this does not settle").** The
  parked gap closed in-branch; `TokenIssuer` declares `TokenIssuanceException`, now under
  `application/authentication/`.

- **ArchUnit pins adapter placement in all three services (revises "What this does not settle").**
  Once every implementation sat in `out/`, `outputPortsAreImplementedOnlyInDrivenAdapterPackages`
  became true and cheap to enforce.

- **`JwtPair` gained slot-versus-type validation (revises "Tests").** Data-derived delete keys let a
  mistyped pair silently no-op logout; `JwtPair` now rejects it.

- **The adapter reports its own defects as defects (revises "Applying it to the authentication
  service").** A wide `try` masked translation bugs as outages; `TokenStoreAdapter` now wraps only
  the store call.

- **Existence returns a null-safe primitive (revises "Splitting the Redis store").** A `null` from
  `hasKey` misreported a bad token as an outage; `RedisJwtStore#exists` uses `Boolean.TRUE.equals`.

- **The TTL invariant moved into the entry (revises "Applying it to the authentication service").**
  `TokenEntry`'s compact constructor asserts a positive TTL; `TokenStoreAdapter` keeps the
  one-second floor.

- **Two Redis-store weaknesses ship logged, not fixed (revises "Applying it to the authentication
  service").** Raw-token keys and non-atomic writes predate the branch and span services; `TODO.md`
  carries both.
