---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

# Ports & Adapters

## Package Structure

```
com.bcn.asapp.<service>/
├── domain/<aggregate>/
├── application/<aggregate>/
│   ├── in/
│   │   ├── UseCase interfaces            # input ports
│   │   ├── command/                      # immutable command records
│   │   ├── result/                       # result records (multi-port aggregation)
│   │   └── service/                      # use case implementations
│   └── out/                              # output port interfaces
└── infrastructure/
    ├── <aggregate>/
    │   ├── in/                           # REST controllers, request/response DTOs
    │   ├── mapper/                       # MapStruct mappers
    │   ├── out/                          # port adapters
    │   └── persistence/                  # JDBC entities, Spring Data repositories
    ├── security/                         # security components (cross-cutting)
    ├── error/                            # error management (cross-cutting)
    └── config/                           # configuration (cross-cutting)
```

**Dependency rule**: `infrastructure → application → domain`. Never reverse.

## Naming

- Port names must be framework-agnostic — no Spring, JWT, or Redis references
- Pure adapters (data/protocol translation only): `<Domain><Port>Adapter` (e.g., `UserRepositoryAdapter`)
- Adapters with logic (e.g., exception handling): descriptive name, no `Adapter` suffix (e.g., `RedisTokenStore`)

## Application Service

- To register services in Spring context always use `@ApplicationService` (a custom meta-annotation); never use `@Service` directly
- `@Transactional` when coordinating multiple ports or using compensation; omit for read-only or single-port operations
- Logger only for critical multi-step orchestrations: `logger.debug()` at entry/exit, `logger.trace()` per step

## Ports & Adapters

**Create an adapter when**: the adaptee can't implement the port directly, external libraries (Spring JDBC, Redis), protocol translation, or type translation needed (e.g., `UserRepositoryAdapter`).
**Implement port directly when**: the adaptee can implement the port directly, internal code with no translation needed (e.g., `JwtIssuer implements TokenIssuer`).
- Cross-cutting infrastructure (e.g., `security/`) can implement ports from any aggregate package.

## Result Objects

Use a `*Result` record (in `application/<aggregate>/in/result/`) when a use case aggregates output from multiple ports, and it cannot be expressed as a single domain object (e.g., `UserWithTasksResult` combines `UserRepository` + `TasksGateway`).

## Compensating Transactions

- Use when coordinating two non-transactional stores (e.g., PostgreSQL + Redis).
- Always in the application service, never in adapters. 
- See `AuthenticateService` for a reference implementation.

## Exception Handling

- Only translate infrastructure exceptions into application exceptions when the application service needs to catch them (e.g., `TokenStoreException`). Everything else propagates to `GlobalExceptionHandler` or Spring Security filters.
- Never wrap `BadCredentialsException` or Spring's `AuthenticationException` subtypes — Spring Security filters run before `DispatcherServlet`; wrapping breaks the filter chain.
- Exception placement: directly in the functional package (e.g., `application/authentication/`).
- Exception hierarchy:

| Type | Extends | Location | Example |
|---|---|---|---|
| Cross-domain base | `RuntimeException` | `application/` | `PersistenceException` |
| Persistence-specific | `PersistenceException` | `application/<aggregate>/` | `AuthenticationPersistenceException` |
| Orchestration base | `RuntimeException` | `application/<aggregate>/` | `AuthenticationException` |
| Orchestration subtype | Orchestration base | `application/<aggregate>/` | `AuthenticationNotFoundException` |
