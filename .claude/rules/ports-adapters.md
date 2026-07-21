---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

## Naming

- Port names must be framework-agnostic — no Spring, JWT, or Redis references
- Pure adapters (data/protocol translation only): `<Domain><Port>Adapter` (e.g., `UserRepositoryAdapter`)
- Adapters with logic (e.g., exception handling): descriptive name, no `Adapter` suffix (e.g., `RedisJwtStore`)

## Application Service

- To register services in Spring context always use `@ApplicationService` (a custom meta-annotation); never use `@Service` directly
- Constructor injection only — never field injection
- `@Transactional` when coordinating multiple ports or using compensation; omit for read-only or single-port operations
- Use logging only for critical multi-step orchestrations

## Adapter vs. Direct Implementation

- **Create an adapter when** external libraries (Spring JDBC, Redis), protocol translation, or type translation needed (e.g., `UserRepositoryAdapter`)
- **Implement port directly when** the adaptee can implement the port directly, internal code with no translation needed (e.g., `JwtIssuer implements TokenIssuer`)
- Cross-cutting infrastructure (e.g., `security/`) can implement ports from any aggregate package

## Result Objects

- Use a `*Result` record (in `application/<aggregate>/in/result/`) when a use case aggregates output from multiple ports, and it cannot be expressed as a single domain object (e.g., `UserWithTasksResult` combines `UserRepository` + `TasksGateway`)

## Compensating Transactions

- Use when coordinating two non-transactional stores (e.g., PostgreSQL + Redis)
- Always in the application service, never in adapters

## Logging

- `debug` = operation entry/exit and major milestones
- `trace` = individual steps within an operation
- Never log passwords, tokens, or PII, use safe placeholders (e.g., username only)
- Let Spring handle HTTP logging; log business context in Controllers only if necessary
- In `GlobalExceptionHandler` use `logger.warn(...)` for 4xx errors (client mistakes); `logger.error(..., ex)` for 5xx errors (include stack trace)

## Exception Handling

- An interface owns its exception contract: each port and use-case interface declares every application/domain exception its callers can observe
- Translate a framework exception to an application type at the adapter only when the service must catch it (e.g. `TokenStoreException` for compensation) or the interface contract requires an owned type (e.g. `InvalidCredentialsException` on login); otherwise let it propagate
- Wrapping Spring `AuthenticationException`/`BadCredentialsException` is path-dependent: never inside a Security filter (JWT resource-server path); do inside a use-case-invoked adapter (login path)
- Exception placement: directly in the functional package (e.g., `application/authentication/`)
- Exception hierarchy:

| Type | Extends | Location | Example |
|---|---|---|---|
| Cross-domain | `RuntimeException` | `application/` | `CompensatingTransactionException` |
| Orchestration base | `RuntimeException` | `application/<aggregate>/` | `AuthenticationException` |
| Orchestration subtype | Orchestration base | `application/<aggregate>/` | `AuthenticationNotFoundException` |

## Resilience

- The adapter owns the resilience mechanism and translates a downstream outage into a typed gateway exception (e.g. `TasksUnavailableException`)
- Whether that outage degrades the response or fails the request is the application service's policy, never the adapter's

## Security

- Always cast to `JwtAuthenticationToken` when reading the current user — never use `UserDetails` or `UsernamePasswordAuthenticationToken`
- `JwtAuthenticationFilter` has already validated the JWT via Redis and stored it as a `JwtAuthenticationToken` in the security context
