---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

Conventions for the application and infrastructure layers — ports, adapters, application services, and the cross-cutting concerns at their boundaries.

## Naming

- Port names must not leak implementation/framework details (Spring, Redis, specific libraries) — use domain vocabulary
- Name a port adapter `<PortInterfaceName>Adapter` (e.g., `UserRepositoryAdapter`), whatever logic it adds
- Use a descriptive name when the class is the real implementation rather than a wrapper over something that already does the job (e.g., `RedisJwtStore`, `JwtIssuer`)

## Application Service

- To register services in the Spring context always use `@ApplicationService` (a custom marker annotation, kept Spring-free and registered by an `@ComponentScan` filter in the infrastructure layer); never use `@Service` directly
- `@Transactional` on state-changing (command) use cases; omit for read-only queries
- Use logging only for critical multi-step orchestrations

## Adapter vs. Direct Implementation

- **Create a separate adapter when** the collaborator's shape doesn't match the port (type/protocol translation, e.g., `UserRepositoryAdapter` over a JDBC repository) or the collaborator is reused outside this port (e.g., `TokenVerifierAdapter` over the shared `JwtVerifier`)
- **Implement the port directly otherwise** — one class fulfilling the port, even when built on a low-level library (e.g., `RedisJwtStore` on Redis, `JwtIssuer implements TokenIssuer`)
- A cross-cutting concern that directly implements a port lives in its own package (e.g., `security/`), not the aggregate's `out/` (e.g., `JwtIssuer implements TokenIssuer` in `security/`)

## Result Objects

- Use a `*Result` record (in `application/<aggregate>/in/result/`) when a use case aggregates output from multiple ports, and it cannot be expressed as a single domain object (e.g., `UserWithTasksResult` combines `UserRepository` + `TasksGateway`)

## Compensating Transactions

- Use when a use case coordinates a transactional store with a non-transactional one that can't roll back with the DB transaction (e.g. PostgreSQL + Redis)
- Prefer ordering the single non-transactional write last so a failure rolls back the DB transaction, avoiding compensation; compensate only for a multi-step non-transactional mutation (e.g. token rotation)
- Always in the application service, never in adapters

## Logging

- `debug` = operation entry/exit and major milestones
- `trace` = individual steps within an operation
- Never log passwords, tokens, or PII — log safe placeholders only (e.g., username)
- Let Spring handle HTTP logging; log business context in Controllers only if necessary
- In `GlobalExceptionHandler` use `logger.warn(...)` for 4xx errors (client mistakes); `logger.error(..., ex)` for 5xx errors (include stack trace)

## Exception Handling

- An interface owns its exception contract: each port and use-case interface declares every application/domain exception its callers can observe
- Translate a framework exception to an application type at the adapter only when the service must catch it (e.g. `TokenStoreException` for compensation) or the interface contract requires an owned type (e.g. `InvalidCredentialsException` on login); otherwise let it propagate
- Wrapping Spring `AuthenticationException`/`BadCredentialsException` is path-dependent: never inside a Security filter (JWT resource-server path); do inside a use-case-invoked adapter (login path)
- Exception hierarchy:

| Type | Extends | Location | Example |
|---|---|---|---|
| Cross-domain | `RuntimeException` | `application/` | `CompensatingTransactionException` |
| Orchestration base | `RuntimeException` | `application/<aggregate>/` | `AuthenticationException` |
| Orchestration subtype | Orchestration base | `application/<aggregate>/` | `AuthenticationNotFoundException` |
| Port/gateway failure | `RuntimeException` | `application/<aggregate>/` | `TokenStoreException`, `TasksUnavailableException` |

## Resilience

- The adapter owns the resilience mechanism and translates a downstream outage into a typed gateway exception (e.g. `TasksUnavailableException`)
- Whether that outage degrades the response or fails the request is the application service's policy, never the adapter's

## Security

- Read the current user by narrowing the authentication with `instanceof JwtAuthenticationToken`, failing (`IllegalStateException`) on mismatch
- When reading the current user, never plain-cast the authentication and never use `UserDetails` or `UsernamePasswordAuthenticationToken` (both are legitimate on the login and actuator paths)
- `JwtAuthenticationFilter` has already validated the JWT via Redis and stored it as a `JwtAuthenticationToken` in the security context
