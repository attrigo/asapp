---
paths:
  - "**/*.java"
  - "**/*.xml"
  - "**/*.yml"
---

# Code Style

## Imports

Order: `java|javax` → `org` → `com` → *(blank line)* → `com.bcn`

## Annotation Ordering

**Production classes** — strictly in this order:
1. Component role: `@RestController`, `@Entity`, `@ApplicationService`
2. Configuration/routing: `@RequestMapping`, `@Scope`, `@Profile`
3. API docs: `@Tag`, `@Operation`, `@Schema`
4. Security: `@PreAuthorize`, `@Secured`
5. Transaction/caching: `@Transactional`, `@Cacheable`
6. Persistence: `@Table`, `@Id`, `@Column`
7. Serialization: `@JsonProperty`, `@JsonIgnore`
8. Validation: `@NotNull`, `@Size`, `@Valid`
9. Mapping: `@Mapping`, `@InheritInverseConfiguration`
10. Code generation: `@Data`, `@Builder`

**Test classes** — strictly in this order:
1. Test context: `@SpringBootTest`, `@WebMvcTest`, `@DataJdbcTest`, `@Import`
2. Infrastructure: `@Testcontainers`, `@AutoConfigureTestDatabase`
3. Mocking: `@MockitoBean`, `@Mock`, `@InjectMocks`, `@Captor`
4. Test markers: `@Test`, `@Nested`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`

## Javadoc

**Rules**:
- `@since` MUST appear on all public classes and interfaces — use the module's current POM version (e.g., `@since 0.2.0`)
- `@see` ONLY for framework/library classes (Spring, MapStruct) — **never** for internal project classes
- `{@link}` for internal references inside description text only
- Summary line must start with a verb: "Stores…", "Validates…", "Handles…"
- Test classes (`*Tests.java`, `*IT.java`): omit `@since`
- Test utility classes (`testutil` package): include `@since`

## Formatting

- Formatter: Spotless + Eclipse (`asapp_formatter.xml`) — run `mvn spotless:apply` to fix
- Line endings: LF only (never CRLF)
- License header: Apache 2.0 required on every Java file

## Logging

**Rules**:
- `debug` = operation entry/exit and major milestones
- `trace` = individual steps within an operation
- Never log passwords, tokens, or PII — use safe placeholders (e.g., username only)
- Domain layer: no logging
- Controllers: let Spring handle HTTP logging; only log business context if necessary

**Application service pattern**:
```java
logger.debug("Authenticating user {}", command.username());   // entry
logger.trace("Step 1: validating credentials");
logger.trace("Step 2: generating tokens");
logger.trace("Step 3: persisting authentication");
logger.debug("Authentication completed for user {}", command.username());  // exit
```

**Infrastructure adapters**:
```java
logger.trace("Translating domain {} to JDBC entity", user);
logger.warn("Authentication failed: {}", exception.getMessage());  // recoverable failure
```

## Error Logging

Log errors **only** in `GlobalExceptionHandler` — never `logger.error()` inside service methods.

| Exception type | Log level |
|---|---|
| 5xx — `TokenStoreException`, `PersistenceException`, `CompensatingTransactionException` | `logger.error()` |
| 4xx with business impact — `AuthenticationNotFoundException`, `UnexpectedJwtTypeException` | `logger.warn()` |
| Expected flow — validation errors, business rule violations | `logger.debug()` |

```java
// ✅ Log in GlobalExceptionHandler
@ExceptionHandler(PersistenceException.class)
protected ResponseEntity<ProblemDetail> handle(PersistenceException ex) {
    logger.error("Persistence failure: {}", ex.getMessage(), ex);
    ...
}

// ❌ Never in service methods
catch (Exception e) {
    // logger.error("...", e);  ← DON'T
    throw new PersistenceException("...", e);  // just rethrow
}
```
