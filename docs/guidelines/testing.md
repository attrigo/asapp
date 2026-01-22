# Testing Strategy

## Quick Reference

**Test Types**:
- `*Tests.java` - Unit tests (domain/application logic)
- `*IT.java` - Integration tests (with database)
- `*E2EIT.java` - End-to-end tests (full context)

**Integration Test Strategies**:

| Type | Speed | Scope | Database | Use For |
|------|-------|-------|----------|---------|
| **@WebMvcTest** | ⚡⚡⚡ ~2-3s | Controllers only | ❌ Mocked | REST API, validation |
| **@DataJdbcTest** | ⚡⚡ ~5-7s | Repositories only | ✅ Real | CRUD, queries |
| **@SpringBootTest** | ⚡ ~10-15s | Full app | ✅ Real | E2E workflows |

**Key Tools**:
- **JaCoCo**: Coverage reports (`mvn verify` → `target/site/jacoco-aggregate/index.html`)
- **PITest**: Mutation testing (`mvn org.pitest:pitest-maven:mutationCoverage`)
- **TestContainers**: PostgreSQL + Redis containers (⚠️ use `static` for singletons!)
- **MockServer**: Mock external services in E2E tests

**CI/CD**: GitHub Actions runs `mvn verify` on push/PR to main

## Key Patterns

### Test Naming

**Pattern**: `<Behavior>_<Condition>` (implicit BDD keywords)

**Rationale**: With `@Nested` classes providing the "When" context, explicit "Then/Given" keywords create unnecessary redundancy and reduce readability.

```java
@Nested
class CreateInactiveUser {  // When = behavior/method under test (PascalCase)

    @Test
    void ReturnsInactiveUser_ValidParameters() {  // Then_Given: <Behavior>_<Condition>
        // Given
        var username = Username.of("user@asapp.com");

        // When
        var user = User.inactiveUser(username, password, role);

        // Then
        assertThat(user.getId()).isNull();
    }
}
```

**Standardization Rules**:

✅ **DO**:
- Start with action/result: `Returns`, `Throws`, `Creates`, `Deletes`, `Updates`
- Use underscore to separate behavior from condition
- Keep condition concise: `ValidInput`, `NullParameter`, `ExpiredToken`
- Nest classes by behavior under test (provides "When" context)
- Omit articles and filler words for scannability

❌ **DON'T**:
- Add explicit "Then/Given/When" keywords (e.g., `ThenReturnsUser_GivenValidInput`)
- Use camelCase without underscores (e.g., `returnsUserValidInput`)
- Include articles like "a", "an", "the" (e.g., `ReturnsTheUser_ValidInput`)
- Include filler words like "should", "will", "can" (e.g., `ShouldReturnUser_ValidInput`)
- Mix naming styles within the same test class

#### Behavior Pattern Selection

Choose the right pattern based on the primary behavior being tested:

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **`Returns<Value>_<Condition>`** | Method returns a value and the return is the primary behavior | `ReturnsUser_UserExists`<br>`ReturnsEmpty_UserNotExists`<br>`ReturnsTrue_SuccessfulDeletion` |
| **`<Action><Object>_<Condition>`** | Method has side effects (void, mutations, deletions) where the action is the behavior | `DeletesAuthentications_RefreshTokenExpired`<br>`DoesNotDeleteAuthentications_NoExpiredTokens`<br>`UpdatesUserStatus_ValidRequest` |
| **`Throws<Exception>_<Condition>`** | Method throws an exception | `ThrowsException_NullParameter`<br>`ThrowsIllegalArgumentException_InvalidId`<br>`ThrowsAuthenticationException_ExpiredToken` |
| **`<Action>And<Returns>_<Condition>`** | Both the action AND return value are equally critical to verify (use sparingly) | `SavesUserAndReturnsId_ValidInput`<br>`SendsEmailAndReturnsConfirmation_ValidEmail`<br>`DeletesTokenAndReturnsTrue_TokenExists` |

**Decision Rule**: Ask "What is the primary behavior I'm asserting?"
- If it's the **return value** → Use `Returns<Value>_`
- If it's a **side effect** (state change, persistence, event) → Use `<Action><Object>_`
- If it's an **exception** → Use `Throws<Exception>_`
- If **both action AND return are equally important** → Use `<Action>And<Returns>_` (rare)

**Why avoid `<Action>And<Returns>_` by default?**
With `@Nested` classes providing the "When" context, the action is already explicit in the class name. Repeating it in the method name creates redundancy:

```java
// ❌ REDUNDANT:
@Nested
class FindUserById {
    @Test
    void FindsUserAndReturnsUser_UserExists() { }  // "FindsUser" duplicates class name
}

// ✅ BETTER:
@Nested
class FindUserById {  // Action is clear from class name
    @Test
    void ReturnsUser_UserExists() { }  // Focus on outcome
}

// ✅ VALID use of <Action>And<Returns>_:
@Nested
class CreateUser {
    @Test
    void SavesUserAndReturnsId_ValidInput() {
        // Test verifies BOTH:
        // 1. User was persisted to database (action)
        // 2. Generated ID was returned (return value)
        // Both are essential to this test's purpose
    }
}
```

#### Condition Pattern Selection

**Verb Choice Reference**:

When naming test methods, choose verbs carefully based on their semantic meaning:

| Verb | Use For | Example Context | Test Name Example |
|------|---------|-----------------|-------------------|
| **Exists** | Boolean state (singular) | Checking if single entity exists | `ReturnsTrue_UserExists` |
| **Exist** | Boolean state (plural) | Checking if multiple entities exist | `ReturnsTrue_UsersExist` |
| **NotExists** | Negative Boolean state | Entity does not exist (existence check) | `ThrowsException_UserNotExists` |
| **Found** | Search/lookup result | Result of find/search operation | `ReturnsUser_UserFound` |
| **NotFound** | Failed search/lookup | Search returned empty/null | `ThrowsException_UserNotFound` |
| **Has** | Entity property/possession | Single entity possessing attribute | `ReturnsTrue_UserHasRole` |
| **Contains** | Collection membership | Collection containing elements | `ReturnsTrue_ListContainsUser` |
| **Includes** | Collection membership (synonym) | Alternative to Contains | `ReturnsTrue_ResultIncludesData` |
| **Matches** | Pattern/rule comparison | Regex, criteria, or pattern match | `ReturnsTrue_PasswordMatchesPattern` |
| **Equals** | Exact value comparison | Value equality (often primitives) | `ReturnsTrue_CountEquals10` |
| **IsValid** | Boolean validation check | Validation result (predicate) | `ReturnsTrue_TokenIsValid` |
| **Valid** | State description | Describing valid state | `ReturnsUser_ValidCredentials` |
| **IsEmpty** | Boolean empty check | Collection/string emptiness (predicate) | `ReturnsTrue_ListIsEmpty` |
| **Empty** | State description | Describing empty state | `ReturnsEmptyList_NoResults` |
| **IsNull** | Null check | Checking for null value | `ReturnsTrue_UserIsNull` |
| **Null** | State description | Describing null state | `ThrowsException_NullParameter` |
| **Expired** | Past/completed state | Something has already expired | `ReturnsFalse_TokenExpired` |
| **Expires** | Future/expiring state | Something will expire (rare) | `ReturnsTrue_TokenExpiresSoon` |
| **Enabled** | Active configuration state | Feature/setting is on | `ReturnsTrue_FeatureEnabled` |
| **Disabled** | Inactive configuration state | Feature/setting is off | `ReturnsFalse_FeatureDisabled` |
| **Active** | Live entity state | Entity is currently active | `ReturnsUser_UserActive` |
| **Inactive** | Dormant entity state | Entity is not active | `ThrowsException_UserInactive` |
| **Missing** | Expected but absent | Required data not present | `ThrowsException_RequiredFieldMissing` |
| **Present** | Existence with emphasis | Emphasizes presence | `ReturnsTrue_RefreshTokenPresent` |

**Key Semantic Distinctions**:

| Distinction | When to Use Each | Examples |
|-------------|------------------|----------|
| **Matches vs Equals** | Pattern/criteria vs exact value | `_PasswordMatchesPattern` (regex) vs `_CountEquals10` (exact value) |
| **Exists vs Found** | Existence query vs search result | `_UserExists` (existence check) vs `_UserFound` (search operation) |
| **NotExists vs NotFound** | Existence check vs failed search | `_UserNotExists` (existence check) vs `_UserNotFound` (search failed) |
| **Missing vs NotFound** | Required but absent vs search failed | `_MissingAuthorizationHeader` (required) vs `_UserNotFound` (search) |
| **Expired vs Expires** | Past state vs future state | `_ExpiredToken` (already past) vs `_TokenExpiresSoon` (will expire) |

**Condition Naming Patterns**:

Choose the right pattern based on what the condition describes. Each pattern optimizes readability by placing key information where it's most scannable.

| Pattern | When to Use | Structure | ✅ DO | ❌ DON'T |
|---------|-------------|-----------|-------|----------|
| **Simple State** | Object state with adjectives (valid, null, empty) | `_<Adjective><Noun>` /<br>`_Non<Adjective><Noun>` | `_ValidToken`<br>`_NonNullToken`<br>`_EmptyList` | `_TokenValid`<br>`_TokenNotNull`<br>`_ListEmpty` |
| **Complex Characteristic** | Specific type/characteristic (token type, format) | `_<Noun><Characteristic>` /<br>`_<Noun>Not<Characteristic>` | `_TokenRefreshType`<br>`_TokenNotRefreshType`<br>`_RequestBodyJson` | `_RefreshTypeToken`<br>`_NotRefreshTypeToken`<br>`_JsonRequestBody` |
| **Existence** | Entity exists/not exists | `_<Noun>Exist(s)` /<br>`_<Noun>NotExist(s)` | `_UserExists`<br>`_UsersNotExist`<br>`_AuthenticationsNotExist` | `_NoUsers`<br>`_NoAuthentications` |
| **Adding Context** | Preserve important context (location, scope, relationship) for ANY verb | `_<Noun><Verb>For<Context>`<br>`_<Noun><Verb>In<Location>`<br>`_<Noun><Verb>Of<Owner>` | `_TasksNotExistForUserId`<br>`_AuthenticationMissingInSecurityContext`<br>`_TokenExpiredForUser`<br>`_UserInactiveInOrganization` | `_TasksNotExist` (loses context)<br>`_MissingAuthentication` (loses context)<br>`_ExpiredToken` (loses context)<br>`_InactiveUser` (loses context) |
| **Multi-State Context** | Tests validating multiple states across different locations | `_<Noun><State1>In<Location1><State2>In<Location2>` | `_BearerTokenValidInAuthorizationHeaderNotExistsInRedis`<br>`_UserActiveInDatabaseInactiveInCache` | `_ValidTokenNotInRedis` (loses first location)<br>`_TokenValidButNotExistsInRedis` (inconsistent structure) |

**Important Considerations**:

1. **Infrastructure References in Test Names**: Only use concrete infrastructure names (Redis, PostgreSQL, etc.) in **infrastructure layer test classes** (e.g., `*IT.java`, `*RepositoryIT.java`). In application and domain test classes, always use abstractions:
   - ✅ Infrastructure tests: `_AccessTokenExpiredInRedis`, `_UserNotFoundInDatabase`
   - ✅ Application tests: `_AccessTokenExpiredInStore`, `_UserNotFoundInRepository`
   - ❌ Application tests: `_AccessTokenExpiredInRedis` (leaks infrastructure details)

### Test Class Javadoc

**Pattern**: Short behavioral summary + Coverage section with detailed behaviors

**Purpose**: Document what the test class verifies without coupling to implementation details

**General Guidelines**:

✅ **DO**:
- Start with "Verifies" followed by the behavior being tested (for test classes)
- Keep summary concise and high-level (avoid listing specific details)
- Use Coverage section for detailed list of behaviors
- Focus on **what** is verified, not **how** or **with what tools**
- Omit @since tags in test classes
- Write behavior descriptions that remain valid when implementation changes

❌ **DON'T**:
- Reference class names in summary
- Duplicate information between summary and Coverage section
- List specific fields, methods, or technical details in summary
- Use phrases like "tests the X class" or "unit tests for Y"

---

#### Domain Classes

**Guidelines**:
- Use DDD terminology: value objects **"encapsulate"**, aggregates **"protect boundaries"**
- Emphasize immutability, self-validation, and domain consistency
- Focus on what the domain concept represents, not validation mechanics

**Structure**:
```java
/**
 * Verifies [DomainConcept] value object encapsulates [concept] with [characteristics].
 * <p>
 * Coverage:
 * <li>[Specific behavior 1]</li>
 * <li>[Specific behavior 2]</li>
 */
```

**Examples**:

Value object:
```java
/**
 * Verifies Username value object encapsulates user identification with self-validation and immutability.
 * <p>
 * Coverage:
 * <li>Rejects null or empty username values</li>
 * <li>Validates username must be valid email address format</li>
 * <li>Creates Username with constructor and factory method for valid inputs</li>
 * <li>Provides access to wrapped username value</li>
 */
class UsernameTests {
```

Aggregate:
```java
/**
 * Verifies User aggregate root protects identity boundaries across persistence states and maintains consistency.
 * <p>
 * Coverage:
 * <li>Creates inactive user with username, password, and role, null ID</li>
 * <li>Creates active user with ID, username, and role, null password</li>
 * <li>Updates user data (username, password, role) on both states</li>
 * <li>Validates username, password, and role required for inactive state</li>
 * <li>Validates ID, username, and role required for active state</li>
 * <li>Implements identity-based equality using ID for active users, username for inactive users</li>
 */
class UserTests {
```

---

#### Application Services

**Guidelines**:
- Focus on orchestration and workflow behavior
- Emphasize failure handling, compensation, and transaction boundaries
- Describe the business flow being tested

**Structure**:
```java
/**
 * Verifies [workflow/operation] orchestration.
 * <p>
 * Coverage:
 * <li>[Workflow step 1]</li>
 * <li>[Failure scenario 1]</li>
 * <li>[Compensation behavior]</li>
 */
```

**Example**:
```java
/**
 * Verifies authentication workflow orchestration.
 * <p>
 * Coverage:
 * <li>Credential validation failures propagate without executing downstream steps</li>
 * <li>Token generation failures prevent persistence and activation</li>
 * <li>Persistence failures prevent token activation</li>
 * <li>Token activation failures trigger compensation to delete persisted authentication</li>
 * <li>Compensation failures throw CompensatingTransactionException</li>
 * <li>Successful authentication completes all orchestration steps</li>
 */
@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTests {
```

---

#### Infrastructure Adapters

**Guidelines**:
- Use hexagonal architecture terminology: **"adapter bridges"**, **"translates"**, **"delegates"**
- Emphasize the translation between layers (domain ↔ infrastructure)
- Focus on integration with external frameworks/systems

**Structure**:
```java
/**
 * Verifies adapter bridges [operation] between [layer1] and [layer2/framework].
 * <p>
 * Coverage:
 * <li>[Delegation behavior]</li>
 * <li>[Translation behavior]</li>
 * <li>[Error handling]</li>
 */
```

**Example**:
```java
/**
 * Verifies adapter bridges credential authentication between application layer and Spring Security framework.
 * <p>
 * Coverage:
 * <li>Delegates authentication to Spring Security AuthenticationManager</li>
 * <li>Translates CustomUserDetails to UserAuthentication domain object</li>
 * <li>Extracts user ID, username, and role from authenticated principal</li>
 * <li>Propagates BadCredentialsException from Spring Security</li>
 * <li>Handles invalid principal types gracefully</li>
 * <li>Handles missing role in principal</li>
 */
@ExtendWith(MockitoExtension.class)
class CredentialsAuthenticatorAdapterTests {
```

---

#### Utility Classes

**Guidelines**:
- Start with **"Provides"** or **"Configures"**
- **No Coverage section** (they're helpers, not tests)
- Keep it simple and concise

**Structure**:
```java
/**
 * Provides/Configures [capability/resource] for [purpose].
 */
```

**Examples**:

Test data factory:
```java
/**
 * Provides test data builders for domain and infrastructure entities with fluent API.
 */
public class TestFactory {
```

Container configuration:
```java
/**
 * Configures TestContainers for integration testing with singleton lifecycle.
 * <p>
 * Creates containers for following services:
 * <li>PostgreSQL</li>
 * <li>Redis</li>
 * <p>
 * This configuration class ensures containers start once per test execution, rather than once per test class, reducing startup overhead. This is achieved using
 * a singleton pattern by declaring containers as {@code static final} fields.
 * <p>
 * Spring Boot's {@code @ServiceConnection} auto-configures connection properties.
 */
@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestContainerConfiguration {
```

### BDDMockito (Not Regular Mockito)
```java
@ExtendWith(MockitoExtension.class)
class ServiceTests {
    @Mock private Repository repo;
    @InjectMocks private Service service;

    @Test
    void test() {
        // Given
        given(repo.find()).willReturn(user);  // NOT when()

        // When
        service.execute();

        // Then
        then(repo).should().find();  // NOT verify()
    }
}
```

### Integration Test Patterns

**@WebMvcTest** (Extend WebMvcTestContext):
```java
class ControllerTests extends WebMvcTestContext {
    // Mocks inherited, mockMvc available
}
```

**@DataJdbcTest** (Repository tests):
```java
@DataJdbcTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestContainerConfiguration.class)
class RepositoryIT { }
```

**@SpringBootTest** (E2E tests):
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestContainerConfiguration.class)
@Testcontainers
class E2EIT {
    @Container
    static MockServerContainer mockServer = ...;  // static = singleton!
}
```

### Test Data Builders

**Use Builders for**: Complex objects (JWT tokens, HTTP requests, entities with many fields)
**Use Inline for**: Simple data (primitives, 2-3 field objects, domain objects under test)

```java
// Builder for complex JWT
var token = testEncodedTokenBuilder().accessToken().expired().build();

// Inline for simple domain
var username = Username.of("user@asapp.com");
```

## Details

### Test Structure Conventions

**@Nested Classes**:
- Group tests by method or behavior
- Use PascalCase names
- Examples: `CreateInactiveUser`, `Authenticate`, `DeleteAllByRefreshTokenExpiredBefore`

**Method Names**: `<Behavior>_<Condition>`
- Format: Start with action/result, followed by context after underscore
- Examples:
  - `ThrowsException_NullParameter()` - Exception scenario
  - `ReturnsTrue_ValidInput()` - Success scenario
  - `DeletesExpiredAuthentications_RefreshTokenExpired()` - Action with context
  - `DoesNotDeleteAuthentications_NoExpiredTokens()` - Negative scenario

**Body Structure**: Always use Given-When-Then with comments

**Common Patterns**:
- Use `catchThrowable()` for exceptions (AssertJ)
- Use `assertThat()` for all assertions (AssertJ)
- Use `@ParameterizedTest` with `@NullAndEmptySource` for null/empty validation
- Define test data as class-level constants when reused

### WebMvcTestContext

**Purpose**: Loads only web layer (controllers + security), **not** services/repositories/database

**Performance**: 5-7x faster than @SpringBootTest (~2-3s vs ~10-15s)

**Location**: `testutil/WebMvcTestContext.java`

**Usage**: Extend this class in controller tests to inherit mocked dependencies and `mockMvc`

### TestContainers Performance

**⚠️ CRITICAL**: Always use `static` containers for singleton pattern:

```java
✅ CORRECT:
@Container
public static PostgreSQLContainer<?> container = ...;  // Starts once

❌ WRONG:
@Container
public PostgreSQLContainer<?> container = ...;  // Starts per test class!
```

**Impact**: Static = ~5s once vs Non-static = ~5s × number of test classes

**Redis Container** (same singleton pattern):

```java
✅ CORRECT (Spring Boot 3.1+):
@Container
@ServiceConnection  // Auto-configures Redis connection properties
public static GenericContainer<?> redis =
    new GenericContainer<>("redis:8.4.0-alpine")
        .withExposedPorts(6379);  // Starts once

// No DynamicPropertySource needed - Spring Boot handles it automatically!
```

**Legacy approach** (pre-3.1):
```java
@Container
public static GenericContainer<?> redis =
    new GenericContainer<>("redis:8.4.0-alpine")
        .withExposedPorts(6379);

@DynamicPropertySource  // Manual configuration (not needed in 3.1+)
static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
}
```

### Code Coverage (JaCoCo)

**Reports**:
- Unit: `target/site/jacoco-ut/index.html`
- Integration: `target/site/jacoco-it/index.html`
- Aggregate: `target/site/jacoco-aggregate/index.html`

**Commands**:
```bash
mvn clean verify                 # Run all tests with coverage
mvn verify -DskipUnitTests       # Integration tests only
```

**Metrics**: Line, branch, method, class, complexity coverage

### Mutation Testing (PITest)

**Purpose**: Validates test quality by introducing code mutations

**Command**:
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

**Report**: `target/pit-reports/<timestamp>/index.html`

**Results**:
- Killed ✅ = Test caught mutation (good)
- Survived ❌ = Test missed mutation (weak test)

### MockServer for E2E Tests

**Setup**:
```java
@Container
static MockServerContainer mockServer =
    new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

@DynamicPropertySource
static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("asapp.client.tasks.base-url", mockServer::getEndpoint);
}
```

**Usage**: Mock external service responses in E2E tests

**Example**: See `UserE2EIT.java` for mocking Tasks service

### Running Tests

```bash
# Single test class
mvn test -Dtest=UserTests

# Single method
mvn test -Dtest=UserTests#ThenReturnsUser_GivenUserExists

# Integration tests only
mvn verify -DskipUnitTests

# With coverage
mvn clean verify

# With mutation testing
mvn org.pitest:pitest-maven:mutationCoverage
```

### GitHub Actions CI

**Workflow**: `.github/workflows/ci.yml`

**Triggers**: Push to main, PRs to main

**Command**: `mvn verify` (compiles, tests, coverage)

**Cache**: Maven dependencies cached for faster builds
