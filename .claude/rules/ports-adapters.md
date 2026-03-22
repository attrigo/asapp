---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

# Application & Infrastructure Patterns

## Package Structure

```
com.bcn.asapp.<service>/
├── domain/<aggregate>/               # Pure Java — no framework annotations
├── application/<aggregate>/
│   ├── in/
│   │   ├── <Verb><Domain>UseCase.java   # Input port interface
│   │   ├── command/                     # <Verb><Domain>Command records
│   │   └── service/                     # @ApplicationService implementations
│   └── out/
│       └── <Domain>Repository.java      # Output port interface
└── infrastructure/
    ├── <aggregate>/in/               # REST controllers, DTOs, API interfaces
    ├── <aggregate>/out/              # Repository adapters, JPA entities
    ├── security/                     # JWT components (cross-cutting)
    └── config/                       # Spring configuration
```

**Dependency rule**: `infrastructure → application → domain`. Never reverse.
Application layer must never import from `infrastructure.*`.

## Application Service

```java
@ApplicationService  // NOT Spring's @Service — marks this as a use case implementation
public class AuthenticateService implements AuthenticateUseCase {
    // Orchestrate: validate → call ports → coordinate → return
}
```

**Rules**:
- Use `@ApplicationService`, never Spring's `@Service`
- Naming: `<Verb><Domain>UseCase` for interfaces, `<Verb><Domain>Service` for implementations
- Application layer naming must be framework-agnostic — no Spring, JWT, or Redis references in port names
- Own `@Transactional` boundaries here, not in adapters
- `logger.debug()` at operation entry/exit; `logger.trace()` for each internal step

## Ports & Adapters Rules

**When to create an adapter**:
- ✅ Third-party code you can't modify (Spring Security, JDBC, Redis)
- ✅ Protocol translation needed (domain operation → Redis commands / SQL)
- ✅ Type translation needed (domain objects ↔ JPA entities)

**When NOT to create an adapter** (proxy anti-pattern):
- ❌ You own the adaptee and it already works with domain types
- ❌ No translation needed — make it implement the port directly instead (e.g., `JwtIssuer implements TokenIssuer`)

**Port abstraction rule**: Ports represent atomic technical capabilities, not workflows.
- ✅ `TokenIssuer`, `CredentialsAuthenticator`, `TokenDecoder`
- ❌ `JwtAuthenticationGranter` (sounds like a use case, not a capability)

**Cross-cutting infrastructure** (e.g., `security/`) can implement ports from any aggregate package — no requirement to co-locate port implementations with aggregates.

## Exception Handling

**Translation rule**: Only translate infrastructure exceptions to application exceptions when the application layer needs to catch them for a business decision (compensation / saga pattern). Otherwise let them propagate to `GlobalExceptionHandler`.

| Application catches? | For business logic? | Translate in adapter? | Example |
|---|---|---|---|
| ✅ Yes | ✅ Yes (compensation) | ✅ Yes | `TokenStoreException` — Redis failure needing token rollback |
| ❌ No | N/A | ❌ No | `JwtException` → `GlobalExceptionHandler` |
| ❌ No | N/A | ❌ No | `DataAccessException` → `GlobalExceptionHandler` |
| ❌ No | N/A | ❌ No | `BadCredentialsException` → Spring Security filter |

**Spring Security exceptions**: NEVER wrap `BadCredentialsException` or other `AuthenticationException` subtypes in custom application exceptions. Spring Security filters run before `DispatcherServlet` — wrapping breaks the filter chain and disables built-in security behaviours (e.g., user enumeration prevention).

**Exception placement**: Directly in the functional package (e.g., `application/authentication/`). No `exception/` subdirectory.

**Exception hierarchy**:
- Generic cross-domain: `PersistenceException`, `CompensatingTransactionException` → in `application/`
- Domain-specific: extend the generic parent (e.g., `AuthenticationPersistenceException extends PersistenceException`) → in `application/<aggregate>/`
- Application service catches the generic parent; polymorphism handles subclasses automatically
