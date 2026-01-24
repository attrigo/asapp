---
paths:
  - "**/test/**/*.java"
  - "**/*Tests.java"
  - "**/*IT.java"
  - "**/*E2EIT.java"
---

## Running Tests

- All unit tests: `mvn test`
- Single test class: `mvn test -Dtest=UsernameTests`
- Integration tests: `mvn verify`
- With coverage: `mvn clean verify` (includes JaCoCo)
- Skip tests: `mvn install -DskipTests`

---

## Testing Stack

**Frameworks**: JUnit 5, AssertJ, BDDMockito, TestContainers, MockServer

**Quality Tools**: JaCoCo (coverage), PITest (mutation testing)

**Project Test Utilities** (`testutil` package):
- `TestFactory` - Factory methods for creating domain objects and test data
- `TestContainerConfiguration` - Shared TestContainers setup for integration tests
- `WebMvcTestContext` - Configuration for WebMvc test slices
- `JwtAssertions` - Custom assertions for JWT validation (authentication service)

---

## 1. Test Organization & Structure

### 1.1 Test Documentation (Javadoc)

**Pattern**: Short behavioral summary + Coverage section with detailed behaviors

**Rules**:
- Omit @since tags
- Don't reference class names, fields or methods

**Template**:
```java
/**
 * [Summary]
 * <p>
 * Coverage:
 * <li>[Specific behavior 1]</li>
 * <li>[Specific behavior 2]</li>
 */
```

#### Domain Classes

- Use DDD terminology: value objects **"encapsulate"**, aggregates **"protect boundaries"**
- Emphasize immutability, self-validation, domain consistency

#### Application Services

- Focus on orchestration and workflow behavior
- Emphasize failure handling, compensation, transaction boundaries

#### Infrastructure Adapters

- Use hexagonal architecture terminology: **"adapter bridges"**, **"translates"**, **"delegates"**
- Emphasize translation between layers

### 1.2 Annotation Ordering

**Rules**:
1. **Test Context**: `@SpringBootTest`, `@WebMvcTest`, `@DataJdbcTest`, `@Import`
2. **Infrastructure**: `@Testcontainers`, `@Container`, `@AutoConfigureTestDatabase`
3. **Mocking**: `@MockBean`, `@Mock`, `@InjectMocks`, `@Captor`
4. **Test Markers**: `@Test`, `@Nested`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`

### 1.3 Test Class Structure

#### @Nested Classes

@Nested classes provide the "When" context.

**Rules**:
- Group tests by method/behavior using PascalCase matching the method under test (e.g., `Authenticate`, `CreateInactiveUser`)

#### Test Ordering Rules

**Rules**:
- **@Nested class order**: Must strictly follow method declaration order in the class under test
- **Test method order** within @Nested classes:
  1. Failure/Exception cases FIRST (ordered by execution flow: validation → logic → persistence)
  2. Success cases LAST

Tests must remain independent despite ordering.

#### @DisplayName Policy

**DON'T**: Use @DisplayName annotation - use `<Behavior>_<Condition>` pattern instead.

### 1.4 Test Naming Conventions

**Pattern**: `<Behavior>_<Condition>` (e.g., `ReturnsUser_ValidId`, `ThrowsException_NullParameter`)

**Examples**:

| Scenario | Method Name | Pattern Applied |
|----------|-------------|-----------------|
| Repository returns user | `ReturnsUser_ValidId` | Returns + ValidId |
| Repository returns empty | `ReturnsEmpty_UserNotExists` | Returns + NotExists |
| Service throws exception | `ThrowsException_NullParameter` | Throws + Null |
| Validation fails | `ThrowsException_InvalidCredentials` | Throws + Invalid |
| Database operation | `DeletesAuthentication_ValidToken` | Deletes + Valid |
| Multiple conditions | `ReturnsEmpty_UserNotExistsInDatabase` | Add context |

**Rules**:
- Use underscore separator between behavior and condition
- Avoid camelCase, articles ("a", "an", "the"), filler words ("should", "will")
- Use concrete infrastructure names (Redis, PostgreSQL) ONLY in integration tests (`*IT.java`)
- Use abstractions (Store, Repository) in domain/application tests
- Don't use "Given/When/Then" keywords

#### Behavior Pattern Selection

**Examples**: `Returns`, `Throws`, `Creates`, `Deletes`, `Updates`, `Persists`

**DON'T**: Combine actions and returns (e.g., `DeletesAndReturnsUser_` - implies unclear responsibility)

#### Condition Pattern Selection

**Pattern**:

| Pattern | Structure | DO | DON'T |
|---------|-----------|-------|----------|
| **Simple State** | `_<Adjective><Noun>` | `_ValidToken`, `_NonNullToken` | `_TokenValid`, `_TokenNotNull` |
| **Complex Characteristic** | `_<Noun><Characteristic>` | `_TokenRefreshType` | `_RefreshTypeToken` |
| **Existence** | `_<Noun>Exist(s)` | `_UserExists`, `_UsersNotExist` | `_NoUsers` |
| **Adding Context** | `_<Noun><Verb>For<Context>` | `_TasksNotExistForUserId` | `_TasksNotExist` |
| **Multi-State** | `_<Noun><State>In<Location>...` | `_TokenValidInHeaderNotExistsInRedis` | `_ValidTokenNotInRedis` |

**Examples**:

| Distinction | When to Use Each | Examples |
|-------------|------------------|----------|
| **Matches vs Equals** | Pattern/criteria vs exact value | `_PasswordMatchesPattern` vs `_CountEquals10` |
| **Exists vs Found** | Existence check vs search result | `_UserExists` vs `_UserFound` |
| **NotExists vs NotFound** | Existence check vs failed search | `_UserNotExists` vs `_UserNotFound` |
| **Missing vs NotFound** | Required but absent vs search failed | `_MissingAuthorizationHeader` vs `_UserNotFound` |

---

## 2. Test Implementation

### 2.1 Test Method Structure

**Rules**:
- Test methods MUST use explicit AAA comment blocks: `// Given`, `// When`, `// Then` (skip given when no setup is needed)

### 2.2 Assertion Patterns

#### AssertJ Tool

**Rules**:
- Use AssertJ assertions exclusively, NOT JUnit assertions

#### Given Section Structure

**Rules**:
- Given block MUST follow two-phase pattern:
  1. Phase 1: Create all test data FIRST
  2. Phase 2: Configure all mocks using prepared data
- Separate phases with blank line for readability

#### Expected Values Placement

**Rules**:
- Expected values MUST be prepared in Given block, not Then block
- Exception: Simple literals (true, false, null, 0, 1) can be asserted directly

#### Soft Assertions

**Rules**:
- When using `assertSoftly()`, `.as()` descriptions are MANDATORY for each assertion

#### Then Block Ordering

**Rules**:
- Then block order MUST be: (1) Assert data/results first, (2) Verify mock interactions second
- Use `inOrder()` when interaction sequence matters

---

## 3. Test Code Conventions

### 3.1 Variable Naming

**Pattern**:

| Variable Type | Required Name | Prohibited |
|---------------|---------------|-------------|
| Method result | `actual` | `result`, `output`, `sut` |
| Expected values | `expected<Type>` | `expected`, `exp` |
| Mock fields | No "Mock" suffix | `repositoryMock`, `mockRepository` |
| Domain objects | Domain name | `testUsername`, `testUser` |
| Collections | Plural domain name | `userList`, `listOfUsers` |

**Rules**:
- NEVER add "Mock" or "mockXxx" suffixes to mock field names (the @Mock annotation already indicates it's a mock)

### 3.2 Test Data & Fixtures

**Scope Priority**:

- **Inline test data**: Create test data directly in test methods for visibility and self-containment
- **Class-level data**: Only for infrastructure config (secrets, base URLs, timeout values) that are NOT the test subject

**DON'T**:  Use class-level fields for test subject values, domain objects, or data being validated

**Pattern Priority**:

| Pattern                           | When to Use                                   | How                                                       | Scope                              |
|-----------------------------------|-----------------------------------------------|-----------------------------------------------------------|------------------------------------|
| **TestFactory Aggregates**        | Need full aggregate for test                  | `testUserBuilder()`, `testJwtAuthenticationBuilder()`    | Method local                       |
| **TestFactory Nested Entities**   | Need reusable nested entity without aggregate | `aJwt()`, `aUser()`, `aTask()`                            | Method local                       |
| **Extract from Aggregates**       | Need several components from same aggregate   | `jwtAuth.accessToken()`, `user.getUsername()`            | Method local                       |
| **Inline Value Objects**          | Need one-off simple value objects             | `Username.of()`, `UserId.of()`                           | Method local                       |
| **Helper Methods**                | Need to test constructor/factory validation   | Create as needed                                          | Nested class (or class if shared) |

**TestFactory Builders**:
- `buildDomainEntity()` - Domain layer tests (returns domain objects)
- `buildJdbcEntity()` - Integration tests (returns JDBC entities)
- Customization: `.withUserId(uuid).withUsername("custom")`

**DON'T**: Duplicate helper methods across @Nested classes

### 3.3 Custom Assertions

**JWT Validation** (authentication service only): Use `assertThatJwt(token)` for fluent JWT assertions

### 3.4 Test Lifecycle

#### @BeforeEach - Test Initialization

**Rules**: Complex initialization, mock-dependent setup, dynamic/random data, conditional setup logic, mutable state, **database cleanup** (clean BEFORE test)

**DON'T**: Test data creation (use patterns in 3.2), Simple initialization, immutable value objects

#### @AfterEach - Resource Cleanup

**Rules**: Use ONLY for external resources requiring explicit cleanup (file handles, network connections, locks)

**DON'T**: Database cleanup (use @BeforeEach instead), pure in-memory objects, TestContainers, mocked dependencies
