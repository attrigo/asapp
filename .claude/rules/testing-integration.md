---
paths:
  - "**/*IT.java"
  - "**/*E2EIT.java"
---

# Integration Test Patterns

Infrastructure-specific patterns for integration tests with real database, containers, and external services

## 1. Test Slice Selection

### 1.1 @WebMvcTest (Web Layer)

**Rules**:
- Extend `WebMvcTestContext` (provides security config and MockMvc)
- Mock service layer dependencies with `@MockitoBean`

**DON'T**: Load database or full application context

### 1.2 @DataJdbcTest (Repository Layer)

**Rules**:
- Import `TestContainerConfiguration.class` for TestContainers setup
- Use `@AutoConfigureTestDatabase(replace = NONE)` to use real database

**DON'T**: Load web layer or full application context

### 1.3 @SpringBootTest (Full Application)

**Rules**:
- Import `TestContainerConfiguration.class` for TestContainers setup
- When testing web layer: use `webEnvironment = RANDOM_PORT` with `@AutoConfigureWebTestClient`

## 2. TestContainers

**Rules**:
- Use `static` fields for TestContainers (singleton pattern - starts once, reused for all tests)
- Expose containers as `@Bean` methods with `@ServiceConnection` annotation (enables Spring Boot 3.1+ auto-configuration)

## 3. Test Patterns

### 3.1 Resource Cleanup

**Rules**:
- Clean resources in `@BeforeEach`, NOT `@AfterEach` (guarantees clean state even if previous test failed)
- Delete in order respecting foreign key constraints
- Assert dependency is not null before cleanup when required (e.g. Redis connection factory)
- Do NOT use `@Transactional` for automatic test rollback — use explicit `@BeforeEach` cleanup

**Example**:
```java
@BeforeEach
void beforeEach() {
    assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

    taskRepository.deleteAll();  // Child first
    userRepository.deleteAll();  // Parent last
}
```

### 3.2 Test Data Persistence Helpers

**Rules**:
- Always extract data creation in an external resource (DB, Redis, etc.) into a helper method
- Assert data was created successfully whenever possible

**Naming conventions**:

| Condition                             | Pattern | Example |
|---------------------------------------|---|---|
| Default data                          | `createXxx()` | `createUser()` |
| Specific data variant (always same shape), ≥3 usages | `createStateXxx()` | `createExpiredAuthentication()` |
| Specific data variant (always same shape), <3 usages | `createXxx(entity)` | `createAuthentication(entity)` |

- When a helper requires a prerequisite, append `ForYyy(prereq)` to the method name (e.g. `createJwtAuthenticationForUser(user)`, `createExpiredJwtAuthenticationForUser(user)`)
- When a helper persists data to a specific store only, append `InStorage` to the method name (e.g. `createJwtAuthenticationInDB(jwtAuthentication)`, `createJwtAuthenticationInRedis(jwtAuthentication)`) — use only when tests need to seed a single store
- Setting a foreign key (e.g. `.withUserId()`) does not make data "custom" — always use row 1

### 3.3 Custom Assertion Helpers

**Rules**:
- Extract repeated assertion logic into private helpers — don't inline complex multi-field assertions in test bodies
- Limit assertion helper parameters to 3 — if more expected values are needed, assert inline instead
- Multi-store facades (`assertXxxExist`, `assertXxxNotExist`) must delegate to storage-specific helpers — allows partial-store tests to reuse them

**Naming conventions**:

| Condition | Pattern | Example |
|---|---|---|
| Full domain object fields | `assertXxx(actual, expected)` | `assertJwtAuthentication(actual, expectedUser)` |
| HTTP response body | `assertXxxResponse(actual, expected)` | `assertAPIResponse(accessToken, refreshToken, user)` |
| Positive existence (all stores) | `assertXxxExist(params, context)` | `assertAuthenticationExist(accessToken, refreshToken, user)` |
| Negative existence (all stores) | `assertXxxNotExist(params)` | `assertAuthenticationNotExist(accessToken, refreshToken)` |
| No-arg negative existence | `assertXxxNotExist()` | `assertAuthenticationNotExist()` |
| Storage-specific | append `InStorage` | `assertAuthenticationExistInDB(...)`, `assertAuthenticationNotExistInRedis(...)` |

### 3.4 MockServer for External Services

**Rules**:
- Use `MockServerContainer` with `@DynamicPropertySource` for external service mocking
- Only use in `@SpringBootTest` tests for service-to-service calls
