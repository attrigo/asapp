---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

Conventions for the application and infrastructure layers — ports, adapters, application services, and the cross-cutting concerns at their boundaries.

## Driven Adapters

- Name a class implementing an output port `<PortInterfaceName>Adapter` and place it in `infrastructure/<aggregate>/out/` — whatever logic it adds, whatever it is built on
- The adapter owns the translation: domain types to the technology's, and the technology's failures to the port's declared exceptions
- The technology an adapter drives stays outside `out/`, in its own infrastructure package — or the adapter drives a library or framework bean directly

## Application Service

- To register services in the Spring context always use `@ApplicationService` (a custom marker annotation, kept Spring-free and registered by an `@ComponentScan` filter in the infrastructure layer); never use `@Service` directly
- `@Transactional` on state-changing (command) use cases; omit for read-only queries — **including single-write commands**
- Keep remote calls and CPU-bound work (e.g. password hashing) out of the transaction; where unavoidable, note it in the method Javadoc
- Use logging only for critical multi-step orchestrations

## Result Objects

- Use a `*Result` record when a use case aggregates output from multiple ports, and it cannot be expressed as a single domain object (e.g., `UserWithTasksResult` combines `UserRepository` + `TasksGateway`)

## Compensating Transactions

- Prefer ordering the single non-transactional write last so a failure rolls back the DB transaction, avoiding compensation; compensate only for a multi-step non-transactional mutation (e.g. token rotation)
- Always in the application service, never in adapters

## Logging

- Prefix messages with an `[UPPER_SNAKE]` context tag
- `debug` = operation entry/exit and major milestones, `trace` = `Step N/M:` individual steps
- Log safe placeholders only (e.g., username)
- Let Spring handle HTTP logging; log business context in Controllers only if necessary

## Exception Handling

- Declare every application or domain exception a caller can receive on the port or use-case interface, as Javadoc `@throws` — never a `throws` clause (all are unchecked)
- Translate a framework exception to an application type at the adapter only when the service must catch it or the interface contract requires an owned type; otherwise let it propagate
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
- When reading the current user, never use `UserDetails` or `UsernamePasswordAuthenticationToken` (both are legitimate on the login and actuator paths)
