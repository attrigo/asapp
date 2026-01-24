---
paths:
  - "**/*IT.java"
  - "**/*E2EIT.java"
---

# Integration Test Patterns

Infrastructure-specific patterns for integration tests with real database, containers, and external services.

---

## 1. Test Slice Selection

### 1.1 Strategy Comparison

**Pattern**: Choose test slice based on scope and speed requirements

| Type | Scope | Startup Time | Database |
|------|-------|--------------|----------|
| @WebMvcTest | Web layer | ~2-3s | Mocked |
| @DataJdbcTest | Repository layer | ~5-7s | Real (TestContainers) |
| @SpringBootTest | Full application | ~10-15s | Real (TestContainers) |

### 1.2 @WebMvcTest (Web Layer)

**Rules**:
- Extend `WebMvcTestContext` (provides security config and MockMvc)
- Mock service layer dependencies with `@MockBean`

**DON'T**: Load database or full application context

### 1.3 @DataJdbcTest (Repository Layer)

**Rules**:
- Import `TestContainerConfiguration.class` for TestContainers setup
- Use `@AutoConfigureTestDatabase(replace = NONE)` to use real database

**DON'T**: Load web layer or full application context

### 1.4 @SpringBootTest (Full Application)

**Rules**:
- Import `TestContainerConfiguration.class` for TestContainers setup
- Use `@AutoConfigureTestDatabase(replace = NONE)` to use real database
- When testing web layer: use `webEnvironment = RANDOM_PORT` with `@AutoConfigureWebTestClient`

---

## 2. TestContainers

**Rules**:
- Use `static` fields for TestContainers (singleton pattern - starts once, reused for all tests)
- Expose containers as `@Bean` methods with `@ServiceConnection` annotation (enables Spring Boot 3.1+ auto-configuration)

---

## 3. Test Patterns

### 3.1 Resource Cleanup

**Rules**:
- Clean resources in `@BeforeEach`, NOT `@AfterEach` (guarantees clean state even if previous test failed)
- Delete in order respecting foreign key constraints
- Assert dependency is not null before cleanup when required (e.g. Redis connection factory)

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

### 3.2 MockServer for External Services

**Rules**:
- Use `MockServerContainer` with `@DynamicPropertySource` for external service mocking
- Only use in `@SpringBootTest` tests for service-to-service calls
