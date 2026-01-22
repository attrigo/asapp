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

**Testing Frameworks**:
- **BDDMockito**: Use `given()`, `then()` instead of Mockito's `when()`, `verify()`
- **AssertJ**: Use `assertThat()`, `catchThrowable()` instead of JUnit assertions
- **JUnit 5**: Test execution engine with `@Test`, `@Nested`, `@ParameterizedTest`

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

**Principle**: Always use BDDMockito's methods instead of regular Mockito for better alignment with Given-When-Then structure.

**Rationale**:
- **Semantic clarity**: `given()` reads naturally in Given block, `then()` reads naturally in Then block
- **BDD alignment**: Methods match the BDD (Behavior-Driven Development) terminology
- **Readability**: Test intent is clearer when method names match test structure
- **Consistency**: Enforces uniform testing vocabulary across the codebase

**Method Mapping**:

| ❌ Mockito (Don't Use) | ✅ BDDMockito (Use This) | Context |
|------------------------|-------------------------|---------|
| `when(mock.method())` | `given(mock.method())` | Given block - setup |
| `verify(mock)` | `then(mock).should()` | Then block - verification |
| `verify(mock, times(n))` | `then(mock).should(times(n))` | Then block - verification with count |
| `verify(mock, never())` | `then(mock).should(never())` | Then block - negative verification |
| `verifyNoInteractions(mock)` | `then(mock).shouldHaveNoInteractions()` | Then block - no interactions |
| `verifyNoMoreInteractions(mock)` | `then(mock).shouldHaveNoMoreInteractions()` | Then block - no more interactions |

**✅ CORRECT: BDDMockito**
```java
@ExtendWith(MockitoExtension.class)
class ServiceTests {
    @Mock private Repository repo;
    @InjectMocks private Service service;

    @Test
    void ReturnsUser_ValidId() {
        // Given
        var user = User.create(userId, firstName);
        given(repo.findById(userId)).willReturn(Optional.of(user));

        // When
        var result = service.findUser(userId);

        // Then
        assertThat(result).isEqualTo(user);
        then(repo).should(times(1)).findById(userId);
        then(repo).shouldHaveNoMoreInteractions();
    }
}
```

**❌ INCORRECT: Regular Mockito**
```java
@Test
void test() {
    // Given
    when(repo.findById(userId)).thenReturn(Optional.of(user));  // ❌ Doesn't read naturally

    // When
    var result = service.findUser(userId);

    // Then
    verify(repo, times(1)).findById(userId);  // ❌ "verify" doesn't match "Then"
    verifyNoMoreInteractions(repo);  // ❌ Less readable
}
```

**Import Statement**:
```java
import static org.mockito.BDDMockito.*;  // ✅ Use this
// import static org.mockito.Mockito.*;  // ❌ Don't use this
```

### AssertJ (Not JUnit Assertions)

**Principle**: Always use AssertJ's fluent assertions instead of JUnit's traditional assertions for better readability, discoverability, and failure messages.

**Rationale**:
- **Fluent API**: Chainable methods that read like natural language
- **Better failure messages**: AssertJ provides detailed, contextual error messages automatically
- **Type safety**: Compile-time checking for assertion methods based on object type
- **Discoverability**: IDE autocomplete shows all available assertions for the type
- **Rich assertions**: Extensive library covering collections, exceptions, dates, strings, etc.
- **Soft assertions**: Built-in support for collecting multiple assertion failures

**Method Mapping**:

| ❌ JUnit (Don't Use) | ✅ AssertJ (Use This) | Purpose |
|---------------------|----------------------|---------|
| `assertEquals(expected, actual)` | `assertThat(actual).isEqualTo(expected)` | Equality |
| `assertNotEquals(expected, actual)` | `assertThat(actual).isNotEqualTo(expected)` | Inequality |
| `assertTrue(condition)` | `assertThat(condition).isTrue()` | Boolean true |
| `assertFalse(condition)` | `assertThat(condition).isFalse()` | Boolean false |
| `assertNull(object)` | `assertThat(object).isNull()` | Null check |
| `assertNotNull(object)` | `assertThat(object).isNotNull()` | Not null check |
| `assertSame(expected, actual)` | `assertThat(actual).isSameAs(expected)` | Reference equality |
| `assertThrows(Exception.class, () -> ...)` | `catchThrowable(() -> ...)` then `assertThat(throwable).isInstanceOf(...)` | Exception testing |
| `assertArrayEquals(expected, actual)` | `assertThat(actual).containsExactly(expected)` | Array/collection |
| `assertIterableEquals(expected, actual)` | `assertThat(actual).containsExactlyElementsOf(expected)` | Iterable |

**✅ CORRECT: AssertJ**
```java
@Test
void ReturnsUser_ValidId() {
    // Given
    var expectedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
    given(repository.findById(userId)).willReturn(Optional.of(expectedUser));

    // When
    var result = service.findById(userId);

    // Then - Fluent, readable assertions
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(expectedUser);
    assertThat(result.getFirstName()).isEqualTo(firstName);
    assertThat(result.getEmail().value()).contains("@asapp.com");
}
```

**❌ INCORRECT: JUnit Assertions**
```java
@Test
void test() {
    // When
    var result = service.findById(userId);

    // Then - Less readable, poor failure messages
    assertNotNull(result);  // ❌ Failure message: "expected: not null"
    assertEquals(expectedUser, result);  // ❌ Parameters order confusing
    assertEquals(firstName, result.getFirstName());  // ❌ Less fluent
    assertTrue(result.getEmail().value().contains("@asapp.com"));  // ❌ Not chainable
}
```

**Exception Testing**:

**✅ CORRECT: AssertJ catchThrowable**
```java
@Test
void ThrowsException_InvalidId() {
    // Given
    given(repository.findById(userId)).willReturn(Optional.empty());

    // When
    var throwable = catchThrowable(() -> service.findById(userId));

    // Then - Clear, fluent exception assertions
    assertThat(throwable)
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User not found with ID: " + userId.value())
        .hasNoCause();
}
```

**❌ INCORRECT: JUnit assertThrows**
```java
@Test
void test() {
    // When/Then - Less flexible
    var exception = assertThrows(UserNotFoundException.class, () -> {
        service.findById(userId);
    });

    // Then - Separate assertions, less fluent
    assertEquals("User not found with ID: " + userId.value(), exception.getMessage());
}
```

**Collection Assertions**:

**✅ CORRECT: AssertJ**
```java
@Test
void ReturnsAllUsers_MultipleUsersExist() {
    // Given
    var users = List.of(user1, user2, user3);
    given(repository.findAll()).willReturn(users);

    // When
    var result = service.findAll();

    // Then - Rich collection assertions
    assertThat(result)
        .isNotEmpty()
        .hasSize(3)
        .contains(user1, user2, user3)
        .extracting(User::getFirstName)
        .containsExactly("John", "Jane", "Bob");
}
```

**Import Statement**:
```java
import static org.assertj.core.api.Assertions.*;  // ✅ Use this
// import static org.junit.jupiter.api.Assertions.*;  // ❌ Don't use this
```

**Failure Message Examples**:

AssertJ provides superior failure messages:

```
// AssertJ failure message:
Expected: User{id=123, name="John"}
  but was: User{id=123, name="Jane"}

// JUnit failure message:
expected: <User@1a2b3c> but was: <User@4d5e6f>
```

### Given Section Structure

**Principle**: Within the "Given" section, separate data preparation from mock configuration for better readability and maintainability.

**Pattern**: Always follow this two-phase structure:

1. **Data Preparation Phase** - Create all test data first
2. **Mocking Phase** - Configure mocks using the prepared data

**Benefits**:
- **Clarity**: Clear separation between test data and behavior configuration
- **Readability**: Easy to scan and understand test setup at a glance
- **Maintainability**: Changes to test data don't require hunting through mock statements
- **Debugging**: Easier to verify test data values before they're used in mocks

**✅ CORRECT Pattern**:
```java
@Test
void ReturnsAuthentication_ValidCredentials() {
    // Given - Phase 1: Prepare all test data
    var username = Username.of("user@asapp.com");
    var password = RawPassword.of("password");
    var command = new AuthenticateCommand(username.value(), password.value());
    var userAuth = UserAuthentication.authenticated(userId, username, role);
    var accessToken = createJwt(JwtType.ACCESS_TOKEN);
    var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
    var jwtPair = new JwtPair(accessToken, refreshToken);
    var savedAuth = JwtAuthentication.authenticated(authId, userId, jwtPair);

    // Given - Phase 2: Configure mocks using prepared data
    given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
    given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
    given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);
    given(repository.save(any(JwtAuthentication.class))).willReturn(savedAuth);
    given(jwtStore.saveTokenPair(jwtPair)).willReturn(true);

    // When
    var result = authenticateService.authenticate(command);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getJwtPair()).isEqualTo(jwtPair);
}
```

**❌ INCORRECT Pattern** (interleaved):
```java
@Test
void test() {
    // Given - Interleaved data and mocks (harder to read)
    var username = Username.of("user@asapp.com");
    given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);

    var password = RawPassword.of("password");
    var accessToken = createJwt(JwtType.ACCESS_TOKEN);
    given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);

    var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
    // ... mixed pattern continues
}
```

**When to Add Blank Line**:
- Add a blank line between Phase 1 (data) and Phase 2 (mocks) when both phases exist
- Optionally add a comment `// Given - Phase 2: Configure mocks` for extra clarity in complex tests
- Skip the blank line if there's no mocking phase (pure domain tests)

### Expected Values Placement

**Principle**: Expected values used in assertions must be prepared in the Given block, not the Then block. This follows the standard Arrange-Act-Assert (AAA) pattern.

**Rationale**:
- **Single Source of Truth**: All test data is visible in one place
- **Clear Dependencies**: Shows what the test needs before execution
- **Readability**: Separates data preparation from verification logic
- **Standard Practice**: Aligns with industry-standard AAA pattern

**Given Block Contains**:
1. Input data (what goes into the method)
2. Mock setup data (what mocks return)
3. Expected output data (what you'll compare against)

**Then Block Contains**:
- Pure verification logic (assertions only)

**✅ CORRECT: Expected data in Given**
```java
@Test
void ReturnsUser_ValidId() {
    // Given - ALL data preparation (input + expected)
    var userId = UserId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    var firstName = FirstName.of("John");
    var lastName = LastName.of("Doe");
    var email = Email.of("john.doe@asapp.com");
    var phoneNumber = PhoneNumber.of("+1234567890");

    var expectedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

    given(repository.findById(userId)).willReturn(Optional.of(expectedUser));

    // When
    var result = service.findById(userId);

    // Then - Pure assertions
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(expectedUser);
    assertThat(result.getFirstName()).isEqualTo(firstName);
    assertThat(result.getEmail()).isEqualTo(email);
}
```

**❌ INCORRECT: Expected data in Then**
```java
@Test
void ReturnsUser_ValidId() {
    // Given
    var userId = UserId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    var user = User.reconstitute(userId, ...);

    given(repository.findById(userId)).willReturn(Optional.of(user));

    // When
    var result = service.findById(userId);

    // Then - Mixing data preparation with assertions (confusing!)
    var expectedFirstName = FirstName.of("John");
    var expectedEmail = Email.of("john.doe@asapp.com");

    assertThat(result.getFirstName()).isEqualTo(expectedFirstName);
    assertThat(result.getEmail()).isEqualTo(expectedEmail);
}
```

**Exception: Simple Literal Values**

For simple, obvious literals, you can assert directly without creating variables:

```java
@Test
void ReturnsTrue_ValidDeletion() {
    // Given
    var userId = UserId.of(UUID.randomUUID());
    given(repository.deleteById(userId)).willReturn(1);

    // When
    var result = service.delete(userId);

    // Then - Simple literals, no variables needed
    assertThat(result).isTrue();
}
```

**Guidelines**:

| Prepare in Given | Assert Directly in Then |
|------------------|-------------------------|
| ✅ Complex objects (User, Task, JwtPair) | ✅ Simple booleans (`true`, `false`) |
| ✅ Multiple fields to verify | ✅ Simple numbers (0, 1, -1) |
| ✅ Value objects used multiple times | ✅ Null checks |
| ✅ Collections with specific content | ✅ Empty collections |
| ✅ Expected exceptions | ✅ Type checks |

### Soft Assertions

**Principle**: Use soft assertions when verifying multiple related properties of the same result object. Soft assertions collect all failures and report them together, rather than stopping at the first failure.

**Benefits**:
- **See all failures at once**: If 3 out of 6 fields are wrong, you see all 3 failures immediately
- **Faster debugging**: No need to fix one failure and re-run to see the next
- **Better test reports**: Comprehensive failure information in a single test run
- **Clear descriptions**: Using `.as()` makes each failure easy to understand

**When to Use Soft Assertions**:

| ✅ Use Soft Assertions | ❌ Use Hard Assertions |
|------------------------|------------------------|
| Verifying multiple fields/properties of same object | Independent assertions that don't relate to same object |
| Want to see ALL failures at once | Testing sequence of operations where order matters |
| Testing related aspects of same result | Early exit on failure is desired (fail-fast) |
| Complex object with many fields to verify | Single assertion or simple verification |

**✅ CORRECT: Soft assertions for related properties**
```java
@Test
void ReturnsUser_ValidId() {
    // Given
    var userId = UserId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    var firstName = FirstName.of("John");
    var lastName = LastName.of("Doe");
    var email = Email.of("john.doe@asapp.com");
    var phoneNumber = PhoneNumber.of("+1234567890");
    var expectedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

    given(repository.findById(userId)).willReturn(Optional.of(expectedUser));

    // When
    var result = service.findById(userId);

    // Then - Multiple related assertions on same object
    assertSoftly(softly -> {
        softly.assertThat(result).as("user not null").isNotNull();
        softly.assertThat(result.getUserId()).as("user ID").isEqualTo(userId);
        softly.assertThat(result.getFirstName()).as("first name").isEqualTo(firstName);
        softly.assertThat(result.getLastName()).as("last name").isEqualTo(lastName);
        softly.assertThat(result.getEmail()).as("email").isEqualTo(email);
        softly.assertThat(result.getPhoneNumber()).as("phone number").isEqualTo(phoneNumber);
    });
}
```

**❌ INCORRECT: Soft assertions for independent checks**
```java
@Test
void DeletesAuthenticationAndTokens_ValidId() {
    // Given
    given(repository.deleteById(authId)).willReturn(true);
    given(jwtStore.deleteTokens(jwtPair)).willReturn(true);

    // When
    var result = service.delete(authId);

    // Then - Independent assertions (hard assertions more appropriate)
    assertSoftly(softly -> {
        softly.assertThat(result).isTrue();  // If this fails, rest may not make sense
        softly.assertThat(repository).extracting("deleteById").isNotNull();  // Verifying mock interactions
        softly.assertThat(jwtStore).extracting("deleteTokens").isNotNull();  // Different concern
    });
    // Better: Use hard assertions and then() for mock verifications
}
```

**✅ CORRECT: Hard assertions for independent checks**
```java
@Test
void DeletesAuthenticationAndTokens_ValidId() {
    // Given
    given(repository.deleteById(authId)).willReturn(true);
    given(jwtStore.deleteTokens(jwtPair)).willReturn(true);

    // When
    var result = service.delete(authId);

    // Then - Independent assertions (fail-fast appropriate)
    assertThat(result).isTrue();
    then(repository).should(times(1)).deleteById(authId);
    then(jwtStore).should(times(1)).deleteTokens(jwtPair);
}
```

**Best Practices**:
- **Always use `.as()` descriptions**: Makes failure messages clear and distinguishable
- **Use `assertSoftly()` static method**: Automatically calls `assertAll()` at the end
- **Group by object**: One soft assertion block per result object
- **Don't mix concerns**: Keep mock verifications separate from result assertions
- **Null check first**: Put null/existence checks before field verifications

**Pattern**:
```java
// Then - Pattern for soft assertions
assertSoftly(softly -> {
    softly.assertThat(result).as("result object").isNotNull();           // Existence
    softly.assertThat(result.getId()).as("ID").isEqualTo(expectedId);    // Identity
    softly.assertThat(result.getName()).as("name").isEqualTo(expectedName);  // Properties
    softly.assertThat(result.getStatus()).as("status").isEqualTo(expectedStatus);
});

// Then - Mock verifications remain separate (hard assertions)
then(repository).should(times(1)).save(any(User.class));
```

### Then Block Ordering

**Principle**: Then blocks must first assert data/results (state verification), then verify mock interactions (behavior verification). This logical ordering ensures clear failure messages and better test readability.

**Rationale**:
- **Logical flow**: First check if the result is correct, then check if the process was correct
- **Clear failures**: If result is wrong, you know immediately without noise from mock verifications
- **Separation of concerns**: Outcome (what you got) vs. implementation (how you got it)
- **Debugging efficiency**: Result failures vs. interaction failures have different root causes

**Ordering**:
1. **Assert data** - State verification ("What did I get?")
2. **Verify interactions** - Behavior verification ("How did the system get there?")

**✅ CORRECT: Assert first, then verify**
```java
@Test
void ReturnsAuthentication_ValidCredentials() {
    // Given
    var username = Username.of("user@asapp.com");
    var password = RawPassword.of("password");
    var command = new AuthenticateCommand(username.value(), password.value());
    var userAuth = UserAuthentication.authenticated(userId, username, role);
    var accessToken = createJwt(JwtType.ACCESS_TOKEN);
    var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
    var jwtPair = new JwtPair(accessToken, refreshToken);
    var savedAuth = JwtAuthentication.authenticated(authId, userId, jwtPair);

    given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
    given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
    given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);
    given(repository.save(any(JwtAuthentication.class))).willReturn(savedAuth);
    given(jwtStore.saveTokenPair(jwtPair)).willReturn(true);

    // When
    var result = authenticateService.authenticate(command);

    // Then - FIRST: Assert the result (state verification)
    assertSoftly(softly -> {
        softly.assertThat(result).as("authentication result").isNotNull();
        softly.assertThat(result.getUserId()).as("user ID").isEqualTo(userId);
        softly.assertThat(result.getJwtPair()).as("JWT pair").isEqualTo(jwtPair);
        softly.assertThat(result.getJwtPair().accessToken()).as("access token").isEqualTo(accessToken);
        softly.assertThat(result.getJwtPair().refreshToken()).as("refresh token").isEqualTo(refreshToken);
    });

    // Then - SECOND: Verify interactions (behavior verification)
    then(credentialsAuthenticator).should(times(1)).authenticate(username, password);
    then(tokenIssuer).should(times(1)).issueAccessToken(userAuth);
    then(tokenIssuer).should(times(1)).issueRefreshToken(userAuth);
    then(repository).should(times(1)).save(any(JwtAuthentication.class));
    then(jwtStore).should(times(1)).saveTokenPair(jwtPair);
}
```

**❌ INCORRECT: Verifications mixed with assertions**
```java
@Test
void test() {
    // When
    var result = authenticateService.authenticate(command);

    // Then - Mixed order (confusing!)
    assertThat(result).isNotNull();
    then(credentialsAuthenticator).should().authenticate(username, password);  // Too early
    assertThat(result.getUserId()).isEqualTo(userId);
    then(tokenIssuer).should().issueAccessToken(userAuth);  // Interleaved
    assertThat(result.getJwtPair()).isEqualTo(jwtPair);
    then(repository).should().save(any());  // Hard to follow
}
```

**Special Cases**:

**Void methods (no return value)**:
```java
@Test
void DeletesAuthentication_ValidId() {
    // Given
    given(repository.deleteById(authId)).willReturn(true);

    // When
    service.revokeAuthentication(authId);  // void method

    // Then - Only behavior verification (no result to assert)
    then(repository).should(times(1)).deleteById(authId);
    then(jwtStore).should(times(1)).deleteTokens(jwtPair);
}
```

**Verifying order of interactions**:
```java
@Test
void SavesUserThenSendsEmail_ValidInput() {
    // Given
    var user = User.create(userId, firstName, lastName);
    given(repository.save(user)).willReturn(user);

    // When
    service.createAndNotify(user);

    // Then - FIRST: Assert result
    assertThat(service.userExists(userId)).isTrue();

    // Then - SECOND: Verify interactions in order
    var inOrder = inOrder(repository, emailService);
    inOrder.verify(repository).save(user);
    inOrder.verify(emailService).sendWelcomeEmail(user.getEmail());
}
```

**Guidelines**:
- Add a blank line between assertions and verifications for visual separation
- Use `inOrder()` when sequence of interactions matters
- For void methods, skip assertions and go straight to verifications
- Group related verifications together (e.g., all repository calls, then all service calls)

### Test Variable Naming Conventions

**Principle**: Use consistent, descriptive variable names that clearly indicate purpose and intent. Variable names should describe content and purpose, not type or technical implementation.

**Core Conventions**:

| Variable Type | Naming Convention | Example | ❌ Don't Use |
|---------------|-------------------|---------|-------------|
| **Result/Outcome** | `actual` | `var actual = service.execute()` | `result`, `output`, `response`, `sut` |
| **Expected Values** | `expected<Type>` | `expectedUser`, `expectedToken` | `expected`, `exp`, `testUser` |
| **Mock Fields** | Purpose-based, no "Mock" suffix | `@Mock private Repository repository` | `repositoryMock`, `mockRepository` |
| **Commands/DTOs** | `<action>Command` | `authenticateCommand`, `createUserCommand` | `cmd`, `request`, `dto` |
| **Domain Objects** | Domain name | `username`, `password`, `userId`, `jwtPair` | `testUsername`, `mockUser` |
| **Collections** | Plural domain name | `users`, `tasks`, `authentications` | `userList`, `taskArray`, `listOfUsers` |

**Purpose-Describing Names**:

Use descriptive prefixes when multiple instances of the same type exist or when lifecycle/state matters:

**✅ USE descriptive prefixes when**:
- Multiple instances of same type in one test
- Lifecycle or state matters (saved vs unsaved, active vs inactive)
- Purpose differs (input vs output, existing vs updated)

**✅ CORRECT: Multiple instances with clear purposes**
```java
@Test
void UpdatesUser_ValidInput() {
    // Given - Multiple users, different purposes clear from names
    var existingUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
    var updatedUser = User.reconstitute(userId, newFirstName, newLastName, email, phoneNumber);
    var savedUser = User.reconstitute(userId, newFirstName, newLastName, email, phoneNumber);

    given(repository.findById(userId)).willReturn(Optional.of(existingUser));
    given(repository.save(updatedUser)).willReturn(savedUser);

    // When
    var actual = service.update(userId, updateCommand);

    // Then
    assertThat(actual).isEqualTo(savedUser);
}
```

**✅ CORRECT: Lifecycle states**
```java
@Test
void SavesAuthentication_InactiveToActive() {
    // Given - State clear from variable names
    var inactiveUser = User.inactiveUser(username, password, role);  // Before persistence
    var activeUser = User.activeUser(userId, username, role);        // After persistence
    var savedAuthentication = JwtAuthentication.authenticated(authId, userId, jwtPair);

    given(repository.save(any())).willReturn(savedAuthentication);
}
```

**✅ CORRECT: Different purposes**
```java
@Test
void CreatesUser_ValidInput() {
    // Given - Purpose clear from names
    var userToCreate = User.create(username, firstName, lastName);     // Input
    var createdUser = User.reconstitute(userId, firstName, lastName);  // Output

    given(repository.save(userToCreate)).willReturn(createdUser);
}
```

**❌ INCORRECT: Unnecessary prefixes for single instance**
```java
@Test
void ReturnsUser_ValidId() {
    // Given - Only one user, no need for prefix
    var expectedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);  // ❌ Redundant
    var testUser = User.reconstitute(...);  // ❌ "test" prefix unnecessary

    // ✅ Better: Simple, clear
    var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
}
```

**Mock Naming - Critical Rule**:

**❌ DON'T add type suffixes** to mock field names:
```java
@ExtendWith(MockitoExtension.class)
class ServiceTests {
    @Mock private UserRepository repositoryMock;           // ❌ Wrong
    @Mock private CredentialsAuthenticator mockAuthenticator;  // ❌ Wrong
    @Mock private TokenIssuer tokenIssuerMock;             // ❌ Wrong
}
```

**✅ DO use purpose-based names**:
```java
@ExtendWith(MockitoExtension.class)
class ServiceTests {
    @Mock private UserRepository repository;                   // ✅ Correct
    @Mock private CredentialsAuthenticator credentialsAuthenticator;  // ✅ Correct
    @Mock private TokenIssuer tokenIssuer;                     // ✅ Correct

    @InjectMocks private AuthenticateService authenticateService;  // ✅ Not "sut"
}
```

**Rationale**: The `@Mock` annotation already indicates it's a mock. The variable name should describe its purpose/role, not its type.

**Decision Matrix**:

| Scenario | Variable Name | Rationale |
|----------|---------------|-----------|
| **Single instance in test** | `user`, `task`, `token` | Simple, no ambiguity |
| **Multiple instances** | `existingUser`, `updatedUser`, `savedUser` | Clarifies purpose/state |
| **Lifecycle states** | `inactiveUser`, `activeUser` | Shows domain state |
| **Expected for assertions** | `expectedUser`, `expectedToken` | Distinguishes assertion target |
| **Input vs output** | `userToCreate`, `createdUser` | Shows data flow |
| **Mock return values** | `savedAuthentication`, `foundUser` | Shows what mock returns |
| **Method result** | `actual` | Standard convention, pairs with `expected` |

**Pattern Examples**:

```java
@Test
void ReturnsAuthentication_ValidCredentials() {
    // Given - Phase 1: Data preparation
    var username = Username.of("user@asapp.com");              // Single instance, simple name
    var password = RawPassword.of("password");
    var command = new AuthenticateCommand(username.value(), password.value());
    var userAuth = UserAuthentication.authenticated(userId, username, role);
    var accessToken = createJwt(JwtType.ACCESS_TOKEN);
    var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
    var jwtPair = new JwtPair(accessToken, refreshToken);
    var savedAuth = JwtAuthentication.authenticated(authId, userId, jwtPair);  // "saved" shows mock return

    // Given - Phase 2: Mocking (no "Mock" suffix on field names)
    given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
    given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
    given(repository.save(any(JwtAuthentication.class))).willReturn(savedAuth);

    // When - Use "actual" for result
    var actual = authenticateService.authenticate(command);

    // Then
    assertThat(actual).isEqualTo(savedAuth);
}
```

**Variables Used in Multiple Blocks (When and Then)**:

When a variable is declared in Given and used in both When and Then blocks, choose the naming approach based on context:

| Approach | When to Use | Example | Use Case |
|----------|-------------|---------|----------|
| **Simple Names** | Only one instance, purpose clear from test | `userId`, `username`, `token` | Straightforward tests where role is obvious |
| **Purpose-Based Names** | Action/intent needs emphasis | `userIdToDelete`, `tokenToRevoke`, `usernameToValidate` | Action-oriented tests where operation matters |
| **Context-Based Names** | State/condition matters | `existingUserId`, `validToken`, `expiredToken` | State verification or conditional logic |

**✅ CORRECT: Simple name (clear from context)**
```java
@Test
void ReturnsUser_ValidId() {
    // Given
    var userId = UserId.of(UUID.fromString("123..."));  // Simple, only one userId
    var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

    given(repository.findById(userId)).willReturn(Optional.of(user));

    // When - Used as parameter
    var actual = service.findById(userId);

    // Then - Used in verification
    assertThat(actual).isEqualTo(user);
    then(repository).should(times(1)).findById(userId);  // Same variable, clear purpose
}
```

**✅ CORRECT: Purpose-based name (emphasizes action)**
```java
@Test
void DeletesUser_ValidId() {
    // Given
    var userIdToDelete = UserId.of(UUID.fromString("123..."));  // Purpose: deletion

    given(repository.deleteById(userIdToDelete)).willReturn(1);

    // When - Clear deletion intent
    var actual = service.delete(userIdToDelete);

    // Then - Verification matches intent
    assertThat(actual).isTrue();
    then(repository).should(times(1)).deleteById(userIdToDelete);
}
```

**✅ CORRECT: Context-based name (state matters)**
```java
@Test
void ThrowsException_ExpiredToken() {
    // Given
    var expiredToken = testEncodedTokenBuilder().accessToken().expired().build();  // State: expired

    given(tokenDecoder.decode(expiredToken)).willThrow(new TokenExpiredException());

    // When - State is important to the test
    var throwable = catchThrowable(() -> service.validate(expiredToken));

    // Then - Verification references state
    assertThat(throwable).isInstanceOf(TokenExpiredException.class);
    then(tokenDecoder).should(times(1)).decode(expiredToken);
}
```

**Decision Flow**:
1. Is there only one instance and purpose obvious? → **Simple name** (`userId`, `token`)
2. Does the action/operation need emphasis? → **Purpose-based name** (`userIdToDelete`, `tokenToRevoke`)
3. Does the state/condition matter to the test? → **Context-based name** (`existingUserId`, `expiredToken`, `validToken`)

**Adding Context for Single Variables**:

Balance clarity with verbosity - add context when it helps understanding, skip when obvious:

| ✅ ADD Context (Even for Single Variable) | ❌ SKIP Context (Keep Simple) |
|-------------------------------------------|-------------------------------|
| Complex test flows with multiple steps | Simple, straightforward tests |
| Variable represents specific state/lifecycle | Variable usage is obvious from test |
| Helps understand data transformation | Adding prefix makes it redundant |
| Test involves persistence/state changes | Domain name is self-explanatory |
| Improves self-documentation | Context is clear from surrounding code |

**✅ GOOD: Context adds clarity**
```java
@Test
void SavesUser_ValidInput() {
    // Given
    var userToSave = User.create(username, firstName, lastName);  // ✅ Clear: input to save
    var savedUser = User.reconstitute(userId, firstName, lastName);  // ✅ Clear: output from save

    given(repository.save(userToSave)).willReturn(savedUser);

    // When
    var actual = service.create(userToSave);

    // Then
    assertThat(actual).isEqualTo(savedUser);
    assertThat(actual.getUserId()).isNotNull();  // ✅ Context shows this is the persisted version
}
```

**✅ GOOD: Simple name sufficient**
```java
@Test
void ReturnsUser_ValidId() {
    // Given - Only one user, simple query operation
    var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);  // ✅ No prefix needed

    given(repository.findById(userId)).willReturn(Optional.of(user));

    // When
    var actual = service.findById(userId);

    // Then
    assertThat(actual).isEqualTo(user);  // ✅ Context obvious: it's the same user
}
```

**❌ BAD: Unnecessary verbosity**
```java
@Test
void ReturnsUser_ValidId() {
    // Given
    var expectedFoundUserFromRepository = User.reconstitute(...);  // ❌ Too verbose, obvious from context

    given(repository.findById(userId)).willReturn(Optional.of(expectedFoundUserFromRepository));
}
```

**Guidelines**:

1. **When data flows through transformations** → Add context (`userToCreate`, `createdUser`, `updatedUser`)
2. **When state/lifecycle matters** → Add context (`inactiveUser`, `activeUser`, `expiredToken`)
3. **When persistence is involved** → Add context (`savedUser`, `deletedUser`)
4. **When test is simple read operation** → Keep simple (`user`, `task`, `token`)
5. **When context is obvious from test name** → Keep simple

**Self-Documenting Principle**: Variable names should tell a story of the test flow without being verbose. If someone reads the test, they should understand what's happening from the variable names alone.

### Test Data Initialization

**Key Insight**: JUnit 5 creates a **new test class instance for each test method**, so both final fields and `@BeforeEach` provide fresh data per test.

**Decision Criteria**:

| Use Final Fields When... | Use @BeforeEach When... |
|---------------------------|-------------------------|
| ✅ Initialization is **simple** (one-liners, factory methods) | ✅ Initialization is **complex** (multi-step, procedural logic) |
| ✅ **No dependencies on mocks** (self-contained objects) | ✅ Setup **depends on mocks** (mocks must be injected first) |
| ✅ Using **fixed/deterministic** test data | ✅ Need **random/dynamic** data per test (e.g., `UUID.randomUUID()`, `Instant.now()`) |
| ✅ All tests use the **same base data** | ✅ Need **conditional setup** (if/else logic, test-specific variations) |
| ✅ Want **inline visibility** (see data in field declarations) | ✅ Setup logic is **better as a method** (too complex for inline) |
| ✅ Objects are **value objects/immutable** (signals intent) | ✅ Working with **mutable state** that tests will modify |
| ✅ No **helper methods** needed for setup | ✅ Reusing **complex helper methods** for initialization |

**Final Fields Example** (preferred for simple, self-contained data):
```java
@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTests {
    @Mock private CredentialsAuthenticator credentialsAuthenticator;
    @InjectMocks private AuthenticateService authenticateService;

    // ✅ Simple, deterministic, no mock dependencies
    private final UserId userId = UserId.of(UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631"));
    private final Username username = Username.of("user@asapp.com");
    private final RawPassword password = RawPassword.of("TEST@09_password?!");
    private final AuthenticateCommand command = new AuthenticateCommand(username.value(), password.value());

    @Test
    void ReturnsAuthentication_ValidCredentials() {
        // Direct use of final fields
        given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
    }
}
```

**@BeforeEach Example** (for mock-dependent or complex setup):
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private User user;
    private UserId userId;

    @BeforeEach
    void setUp() {
        // ✅ Mock-dependent initialization (mocks are ready now)
        userId = UserId.of(UUID.randomUUID());  // Dynamic per test
        user = userRepository.findById(userId).orElseThrow();

        // Or complex multi-step setup
        var token = createAccessToken();
        validateTokenStructure(token);
        user = extractUserFromToken(token);
    }
}
```

### Test Lifecycle: @BeforeEach and @AfterEach

**Purpose**: Manage test setup and teardown to ensure test isolation and proper resource cleanup.

#### @BeforeEach - Test Initialization

**Primary Goal**: **Test isolation** - ensures each test starts with a fresh state, preventing one test from affecting another.

**✅ When to Use @BeforeEach**:
- **Complex initialization** requiring multiple steps
- **Mock-dependent setup** (mocks must be injected first)
- **Dynamic/random data** per test (`UUID.randomUUID()`, `Instant.now()`)
- **Conditional setup logic** (if/else, loops, test-specific variations)
- **Mutable state** that tests will modify
- **Database cleanup** (clean database BEFORE test, not after!)

**❌ When NOT to Use @BeforeEach** (use final fields instead):
- **Simple initialization** (one-liners, factory methods)
- **No mock dependencies** (self-contained objects)
- **Fixed/deterministic test data** (same data for all tests)
- **Immutable value objects** (signals intent)
- **Inline visibility** preferred (see data in field declarations)

#### @AfterEach - Resource Cleanup

**Primary Goal**: **Resource cleanup** - release external resources after each test.

**✅ When to Use @AfterEach** (CRITICAL for these):
- **File handles/streams** must be released
- **Network connections/sockets** must be closed
- **Temporary files** must be deleted
- **Locks/semaphores** must be released
- **External processes** must be terminated
- **Any resource with explicit close() method**

**❌ When NOT to Use @AfterEach**:
- **Database cleanup** → Use @BeforeEach instead (see Database Cleanup section)
- **Pure in-memory objects** → JVM handles cleanup automatically
- **TestContainers** → Framework handles cleanup
- **Mocked dependencies** → No real resources to clean
- **Spring-managed resources** → Spring handles lifecycle

**Example: Resource Cleanup**
```java
class FileProcessorTests {
    private FileInputStream inputStream;
    private FileOutputStream outputStream;

    @BeforeEach
    void setup() throws IOException {
        inputStream = new FileInputStream("test.txt");
        outputStream = new FileOutputStream("output.txt");
    }

    @AfterEach  // ✅ Release file handles
    void cleanup() throws IOException {
        if (inputStream != null) inputStream.close();
        if (outputStream != null) outputStream.close();
    }

    @Test
    void processFile() {
        // Test with file resources
    }
}
```

#### @AutoClose - Modern Alternative (JUnit 5.9+)

**Purpose**: Automatic resource cleanup without @AfterEach boilerplate.

**When to Use @AutoClose**:
- ✅ Resources with `close()` method (streams, connections)
- ✅ Simpler than @AfterEach for single resources
- ✅ Type-safe (compile-time checking)

**When to Use @AfterEach instead**:
- ✅ Multiple resources to clean
- ✅ Complex cleanup logic
- ✅ Custom cleanup operations (not just `close()`)

**Example: @AutoClose**
```java
class FileProcessorTests {
    @AutoClose  // ✅ Automatic cleanup
    private FileInputStream inputStream = new FileInputStream("test.txt");

    @AutoClose
    private FileOutputStream outputStream = new FileOutputStream("output.txt");

    @Test
    void processFile() {
        // Files automatically closed after test
    }
}
```

#### Database Cleanup: Use @BeforeEach, NOT @AfterEach

**Critical Rule**: Clean database in @BeforeEach, not @AfterEach.

**Why @BeforeEach?**
- ✅ **Guarantees clean state** before every test
- ✅ **Works even if previous test failed** or was stopped during debugging
- ✅ **Easier to debug** (can inspect data after test)
- ✅ **Prevents issues** with @Transactional tests

**Why NOT @AfterEach?**
- ❌ **Skipped if test fails** or debugging stops
- ❌ **Leaves dirty data** after failures
- ❌ **Data disappears**, harder to debug
- ❌ **Can cause silent hangs** with maven surefire and @Transactional

**Pattern: Repository.deleteAll() (Simple schemas)**
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestContainerConfiguration.class)
class UserRepositoryIT {

    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;

    @BeforeEach  // ✅ Clean BEFORE test, not AFTER
    void cleanDatabase() {
        // Delete in order respecting foreign key constraints
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void SavesUser_ValidData() {
        // Test starts with guaranteed clean database
        var user = User.create(username, firstName, lastName);
        var saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
    }
}
```

**Alternative: JdbcTestUtils (Spring utility)**
```java
@SpringBootTest
class UserRepositoryIT {

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Delete in order respecting foreign key constraints
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "tasks", "users", "jwt_authentications");
    }
}
```

**Alternative: Custom DatabaseCleaner (Complex schemas with cascades)**
```java
@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void cleanAll() {
        // Disable foreign key checks temporarily
        jdbcTemplate.execute("SET session_replication_role = replica");  // PostgreSQL

        jdbcTemplate.execute("TRUNCATE TABLE tasks CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE jwt_authentications CASCADE");

        // Re-enable foreign key checks
        jdbcTemplate.execute("SET session_replication_role = DEFAULT");
    }
}

// Usage in tests
@SpringBootTest
class UserRepositoryIT {

    @Autowired private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.cleanAll();
    }
}
```

#### Complete Decision Matrix

| Scenario | Use | Reason |
|----------|-----|--------|
| **Simple immutable data** | Final fields | Clearer, faster, no setup needed |
| **Complex initialization** | @BeforeEach | Multi-step logic, better organization |
| **Mock-dependent setup** | @BeforeEach | Mocks injected first |
| **Dynamic per-test data** | @BeforeEach | Fresh random/time-based data |
| **File handles/streams** | @AfterEach or @AutoClose | Must release resources |
| **Database cleanup** | @BeforeEach | Guarantees clean state even after failures |
| **Network connections** | @AfterEach or @AutoClose | Must close connections |
| **Temporary files** | @AfterEach | Must delete files |
| **Mocked objects** | Nothing | Framework handles lifecycle |
| **TestContainers** | Nothing | Framework handles lifecycle |
| **In-memory objects** | Nothing | JVM garbage collection |
| **Spring-managed beans** | Nothing | Spring handles lifecycle |

### Test Variable Scope

**Principle**: Prefer method-level (local) variables for maximum test isolation and readability. Promote to class-level only when used by most tests.

**Decision Criteria**:

| Use Class-Level Fields When... | Use Method-Level Variables When... |
|--------------------------------|-----------------------------------|
| ✅ **Used by ALL or MOST tests** in the class | ✅ **Used by ONE or FEW tests** (default choice) |
| ✅ **Constant across all tests** (immutable setup data) | ✅ **Varies between tests** (test-specific data) |
| ✅ **Complex to construct** (reduces duplication) | ✅ **Simple to create** (one-liners, factory calls) |
| ✅ **Signals "shared test context"** (documents intent) | ✅ **Emphasizes test independence** (better isolation) |
| ✅ **Reduces visual noise** when repeated everywhere | ✅ **Improves readability** by keeping data near usage |
| ✅ **Part of Given/Arrange setup** (common preconditions) | ✅ **Part of When/Act or Then/Assert** (test-specific) |

**Class-Level Example** (shared constant data):
```java
@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTests {
    @Mock private CredentialsAuthenticator credentialsAuthenticator;
    @InjectMocks private AuthenticateService authenticateService;

    // ✅ Used by ALL tests - promote to class level
    private final UserId userId = UserId.of(UUID.fromString("..."));
    private final Username username = Username.of("user@asapp.com");
    private final RawPassword password = RawPassword.of("TEST@09_password?!");
    private final AuthenticateCommand command = new AuthenticateCommand(username.value(), password.value());

    @Test
    void ReturnsAuthentication_ValidCredentials() {
        // Direct use of class-level fields
        given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
    }
}
```

**Method-Level Example** (test-specific variations):
```java
@Test
void ThrowsException_ExpiredToken() {
    // ✅ Only this test needs expired token - keep local
    var expiredToken = testEncodedTokenBuilder().accessToken().expired().build();

    given(tokenDecoder.decode(expiredToken)).willThrow(new TokenExpiredException());
}

@Test
void SavesUserAndReturnsId_ValidInput() {
    // ✅ Test-specific ID generation - keep local
    var savedAuthentication = JwtAuthentication.authenticated(
        JwtAuthenticationId.of(UUID.randomUUID()),
        userId,
        jwtPair
    );

    given(repository.save(any())).willReturn(savedAuthentication);
}
```

**Nested Class Scoping** (multi-level organization):
```java
class TaskServiceTests {
    // Outer: Shared by ALL nested classes
    @Mock private TaskRepository repository;
    private final UserId userId = UserId.of(UUID.fromString("..."));

    @Nested
    class CreateTask {
        // Nested: Shared by tests in THIS nested class only
        private final Title title = Title.of("Task Title");
        private final Description description = Description.of("Description");

        @Test
        void ReturnsTask_ValidInput() {
            // Method: Specific to THIS test
            var startDate = StartDate.of(Instant.now());
            var result = taskService.create(userId, title, description, startDate, null);
        }
    }
}
```

**Guidelines**:
- Default to method-level variables for maximum clarity
- Promote to class-level when used by 3+ tests (DRY principle)
- Use `final` for class-level fields to signal immutability
- Keep class-level fields at the top for visibility
- Leverage `@Nested` classes for intermediate scopes

### Mock Precision with Specific Variables

**Principle**: Use specific variables instead of `any()` matchers whenever possible for better test precision and documentation.

**Benefits**:
- **Precision**: Tests verify exact values, not just types
- **Documentation**: Makes it explicit what values are being tested
- **Debugging**: Easier to understand test failures
- **Maintenance**: Changes to value objects are caught by tests

**Pattern**:
```java
@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTests {
    @Mock private CredentialsAuthenticator credentialsAuthenticator;
    @Mock private TokenIssuer tokenIssuer;
    @InjectMocks private AuthenticateService authenticateService;

    private final String usernameValue = "user@asapp.com";
    private final String passwordValue = "password";

    @Test
    void ReturnsAuthentication_ValidCredentials() {
        // Given - Create specific variables
        var username = Username.of(usernameValue);
        var password = RawPassword.of(passwordValue);
        var userAuth = UserAuthentication.authenticated(userId, username, role);
        var accessToken = createJwt(JwtType.ACCESS_TOKEN);
        var refreshToken = createJwt(JwtType.REFRESH_TOKEN);

        // ✅ CORRECT: Use specific variables in mocks
        given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
        given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
        given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);

        // When
        var result = authenticateService.authenticate(command);

        // Then - Use same specific variables in verifications
        then(credentialsAuthenticator).should(times(1)).authenticate(username, password);
        then(tokenIssuer).should(times(1)).issueAccessToken(userAuth);
        then(tokenIssuer).should(times(1)).issueRefreshToken(userAuth);
    }
}
```

**Anti-Pattern**:
```java
@Test
void test() {
    // ❌ WRONG: Using any() matchers when specific values are available
    given(credentialsAuthenticator.authenticate(any(Username.class), any(RawPassword.class))).willReturn(userAuth);

    // Verifications also use any() - less precise
    then(credentialsAuthenticator).should(times(1)).authenticate(any(Username.class), any(RawPassword.class));
}
```

**When to Use `any()` Matchers**:
- When the exact value is irrelevant to the test scenario
- In negative assertions: `should(never()).delete(any(JwtPair.class))`
- When testing generic error handling regardless of input
- When the value is truly variable and can't be predetermined

### ArgumentCaptor

**Principle**: Use ArgumentCaptor to capture and verify dynamically generated arguments passed to mocks. Prefer specific variables when possible for better readability.

**Purpose**: ArgumentCaptor is particularly useful when verifying methods that accept dynamically generated arguments that cannot be predetermined.

**✅ When to Use ArgumentCaptor**:
- **Dynamically generated arguments** that vary per test execution
- **Complex object verification** where you need to inspect multiple properties
- **Verification only** (not stubbing) - capture arguments to assert their state
- **Event-driven systems** where listeners receive dynamic values
- **Void methods** where you can't access the argument outside the method

**❌ When NOT to Use ArgumentCaptor** (use specific variables instead):
- **Stubbing operations** (reduces readability, use specific variables)
- **Simple argument matching** (prefer specific variables or `eq()` matcher)
- **Predetermined values** (use specific variables for clarity)
- **When ArgumentMatchers suffice** (e.g., `any()`, `eq()`, custom matchers)

**✅ CORRECT: ArgumentCaptor for dynamic verification**
```java
@Test
void SavesUserWithCorrectData_ValidInput() {
    // Given
    var command = new CreateUserCommand("user@asapp.com", "John", "Doe");
    var captor = ArgumentCaptor.forClass(User.class);

    // When
    service.createUser(command);

    // Then - Capture the dynamically created User object
    then(repository).should(times(1)).save(captor.capture());

    // Then - Verify properties of captured object
    var capturedUser = captor.getValue();
    assertSoftly(softly -> {
        softly.assertThat(capturedUser.getUsername()).as("username").isEqualTo(Username.of("user@asapp.com"));
        softly.assertThat(capturedUser.getFirstName()).as("first name").isEqualTo(FirstName.of("John"));
        softly.assertThat(capturedUser.getLastName()).as("last name").isEqualTo(LastName.of("Doe"));
        softly.assertThat(capturedUser.getId()).as("ID").isNull();  // Not yet persisted
    });
}
```

**✅ CORRECT: @Captor annotation (cleaner)**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock private UserRepository repository;
    @InjectMocks private UserService service;

    @Captor
    private ArgumentCaptor<User> userCaptor;  // ✅ Declared as field with @Captor

    @Test
    void SavesUserWithCorrectData_ValidInput() {
        // Given
        var command = new CreateUserCommand("user@asapp.com", "John", "Doe");

        // When
        service.createUser(command);

        // Then
        then(repository).should(times(1)).save(userCaptor.capture());
        var capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getUsername()).isEqualTo(Username.of("user@asapp.com"));
    }
}
```

**❌ INCORRECT: ArgumentCaptor for stubbing**
```java
@Test
void test() {
    // Given
    var captor = ArgumentCaptor.forClass(User.class);

    // ❌ WRONG: Don't use captor with stubbing
    given(repository.save(captor.capture())).willReturn(savedUser);

    // This reduces readability and is not recommended
}
```

**✅ BETTER: Use specific variables for stubbing**
```java
@Test
void test() {
    // Given
    var userToSave = User.create(username, firstName, lastName);
    var savedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

    // ✅ CORRECT: Use specific variables
    given(repository.save(userToSave)).willReturn(savedUser);
}
```

**Multiple Captures**:
```java
@Test
void SavesMultipleUsers_ValidBatch() {
    // Given
    var captor = ArgumentCaptor.forClass(User.class);

    // When
    service.createBatch(commands);

    // Then - Capture all invocations
    then(repository).should(times(3)).save(captor.capture());

    // Then - Verify all captured values
    var capturedUsers = captor.getAllValues();
    assertThat(capturedUsers).hasSize(3);
    assertThat(capturedUsers).extracting(User::getUsername)
        .containsExactly(username1, username2, username3);
}
```

**Guidelines**:
- **Prefer specific variables** over ArgumentCaptor for better readability
- **Use @Captor annotation** instead of manual `ArgumentCaptor.forClass()` when possible
- **Use only for verification**, never for stubbing
- **Keep capture and assertion close** together for readability
- **Document why** you're using captor (complex verification, dynamic data)

### Mock Verification Modes

**Principle**: Use appropriate verification modes to validate meaningful interactions, not every method call.

**Purpose**: Verification modes specify how many times a mock method should have been called, ranging from never to at least/most certain times.

**Available Modes**:

| Mode | Description | Use Case |
|------|-------------|----------|
| **times(n)** | Exact number of invocations | Precise expectations (e.g., `times(1)`, `times(3)`) |
| **times(1)** | Called exactly once (default, can be omitted) | Most common case |
| **never()** | Alias to `times(0)`, never called | Negative assertions, guard clauses |
| **atLeastOnce()** | Called one or more times | Minimum guarantee |
| **atLeast(n)** | Called at least n times | Minimum invocation boundary |
| **atMost(n)** | Called at most n times | Maximum invocation boundary |

**✅ When to Use Each Mode**:

**times(n) - Exact Count**:
```java
@Test
void CallsRepositorySaveOnce_ValidInput() {
    // When
    service.createUser(command);

    // Then - Exact count: called exactly once
    then(repository).should(times(1)).save(any(User.class));
    // Or omit times(1) since it's the default:
    then(repository).should().save(any(User.class));
}
```

**never() - Negative Assertion**:
```java
@Test
void DoesNotSaveUser_ValidationFails() {
    // Given
    var invalidCommand = new CreateUserCommand("", "John", "Doe");  // Invalid email

    // When
    var throwable = catchThrowable(() -> service.createUser(invalidCommand));

    // Then - Verify save was never called
    assertThat(throwable).isInstanceOf(ValidationException.class);
    then(repository).should(never()).save(any(User.class));
}
```

**atLeastOnce() - Minimum Guarantee**:
```java
@Test
void LogsAtLeastOnce_SuccessfulOperation() {
    // When
    service.processUsers(users);

    // Then - Don't care about exact count, just that it logged
    then(logger).should(atLeastOnce()).info(anyString());
}
```

**atLeast(n) / atMost(n) - Boundaries**:
```java
@Test
void RetriesAtLeast3Times_TransientFailure() {
    // Given
    given(externalService.call()).willThrow(new TransientException());

    // When
    catchThrowable(() -> service.callWithRetry());

    // Then - Verify retry logic (at least 3 attempts)
    then(externalService).should(atLeast(3)).call();
}

@Test
void DoesNotExceedRateLimit_BulkOperation() {
    // When
    service.bulkProcess(items);

    // Then - Verify rate limiting (at most 10 calls per second)
    then(rateLimiter).should(atMost(10)).acquire();
}
```

**Guidelines**:

1. **Default to times(1)**: Most common case, can be omitted for brevity
   ```java
   then(repository).should().save(user);  // Equivalent to times(1)
   ```

2. **Use never() for guard clauses**: Verify early exits don't call certain methods
   ```java
   then(repository).should(never()).save(any());
   ```

3. **Use atLeast() for non-critical counts**: Logging, metrics, caching
   ```java
   then(cache).should(atLeast(1)).put(anyString(), any());
   ```

4. **Use atMost() for resource limits**: Rate limiting, batch sizes
   ```java
   then(apiClient).should(atMost(100)).call();
   ```

5. **Avoid over-verification**: Don't verify every interaction, focus on meaningful behaviors
   ```java
   // ❌ Too much verification noise
   then(repository).should(times(1)).findById(userId);
   then(repository).should(times(1)).save(user);
   then(logger).should(times(2)).debug(anyString());
   then(metrics).should(times(1)).increment(anyString());

   // ✅ Verify only critical interactions
   then(repository).should().save(user);
   ```

6. **Combine with never() for exclusive behaviors**:
   ```java
   @Test
   void UpdatesExistingUser_UserExists() {
       // Then
       then(repository).should().update(user);
       then(repository).should(never()).create(any());  // Should update, not create
   }
   ```

**Best Practice**: Verify meaningful interactions that validate business logic, not implementation details.

### Parameterized Tests

**Principle**: Use parameterized tests to run the same test logic with multiple input values, reducing code duplication and improving test coverage.

**Purpose**: Test multiple scenarios (different inputs, edge cases, boundary values) without writing separate test methods for each case.

**When to Use Parameterized Tests**:
- Testing validation logic with multiple invalid inputs
- Testing boundary values (min, max, zero, negative)
- Testing equivalent partitions (similar inputs, same expected outcome)
- Testing multiple valid/invalid formats (emails, phone numbers, dates)

**Three Main Approaches**:

#### 1. @ValueSource - Simple, Single-Argument Tests

**Use For**: Testing a single parameter with simple values (strings, ints, booleans, etc.)

**✅ CORRECT: @ValueSource for validation tests**
```java
@ParameterizedTest
@ValueSource(strings = {"", "   ", "  \t  ", "\n"})
void ThrowsException_BlankUsername(String username) {
    // When
    var throwable = catchThrowable(() -> Username.of(username));

    // Then
    assertThat(throwable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username must not be blank");
}

@ParameterizedTest
@ValueSource(ints = {-1, -10, -100, Integer.MIN_VALUE})
void ThrowsException_NegativeAge(int age) {
    // When
    var throwable = catchThrowable(() -> new User(age));

    // Then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
}
```

**Available Types**: strings, ints, longs, doubles, floats, shorts, bytes, chars, booleans, classes

#### 2. @CsvSource - Multiple Arguments, Compact Format

**Use For**: Testing multiple parameters with expected results in a compact, readable format

**✅ CORRECT: @CsvSource for input-output pairs**
```java
@ParameterizedTest
@CsvSource({
    "user@asapp.com,      true",
    "john.doe@gmail.com,  true",
    "invalid-email,       false",
    "missing@domain,      false",
    "@nodomain.com,       false",
    "no-at-sign.com,      false"
})
void ValidatesEmailFormat_VariousInputs(String email, boolean expectedValid) {
    // When
    var actual = EmailValidator.isValid(email);

    // Then
    assertThat(actual).isEqualTo(expectedValid);
}

@ParameterizedTest
@CsvSource({
    "John,  Doe,   John Doe",
    "Jane,  Smith, Jane Smith",
    "A,     B,     A B",
    "'',    Doe,   ' Doe'"  // Use quotes for empty/special values
})
void FormatsFullName_VariousCombinations(String firstName, String lastName, String expectedFullName) {
    // When
    var actual = NameFormatter.format(firstName, lastName);

    // Then
    assertThat(actual).isEqualTo(expectedFullName);
}
```

**Tips**:
- Use single quotes `' '` for empty strings or strings with spaces
- Separate values with commas
- Each line is one test case

#### 3. @MethodSource - Complex Objects, Programmatic Data

**Use For**: Complex objects, domain value objects, or when data generation logic is needed

**✅ CORRECT: @MethodSource for complex domain objects**
```java
@ParameterizedTest
@MethodSource("invalidUsernameProvider")
void ThrowsException_InvalidUsername(Username username, String expectedMessage) {
    // When
    var throwable = catchThrowable(() -> User.create(username, firstName, lastName));

    // Then
    assertThat(throwable)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining(expectedMessage);
}

// Factory method providing test data
static Stream<Arguments> invalidUsernameProvider() {
    return Stream.of(
        Arguments.of(null, "Username must not be null"),
        Arguments.of(Username.of(""), "Username must not be blank"),
        Arguments.of(Username.of("invalid"), "Username must be valid email"),
        Arguments.of(Username.of("no@domain"), "Username must be valid email")
    );
}
```

**✅ CORRECT: @MethodSource for multiple test scenarios**
```java
@ParameterizedTest
@MethodSource("authenticationScenariosProvider")
void AuthenticatesUser_VariousScenarios(Credentials credentials, boolean shouldSucceed) {
    // Given
    if (shouldSucceed) {
        given(credentialsAuthenticator.authenticate(any(), any())).willReturn(userAuth);
    } else {
        given(credentialsAuthenticator.authenticate(any(), any()))
            .willThrow(new BadCredentialsException("Invalid credentials"));
    }

    // When
    if (shouldSucceed) {
        var actual = service.authenticate(credentials);
        assertThat(actual).isNotNull();
    } else {
        var throwable = catchThrowable(() -> service.authenticate(credentials));
        assertThat(throwable).isInstanceOf(AuthenticationException.class);
    }
}

static Stream<Arguments> authenticationScenariosProvider() {
    return Stream.of(
        Arguments.of(new Credentials("user@asapp.com", "password123"), true),
        Arguments.of(new Credentials("admin@asapp.com", "admin123"), true),
        Arguments.of(new Credentials("invalid@asapp.com", "wrongpass"), false),
        Arguments.of(new Credentials("", "password123"), false)
    );
}
```

**Method Requirements**:
- Must be `static` (unless using `@TestInstance(PER_CLASS)`)
- Must return `Stream`, `Iterable`, `Iterator`, or array
- Use `Arguments.of(...)` to pass multiple parameters

#### Special Annotations

**@NullAndEmptySource**: Combines `@NullSource` and `@EmptySource`
```java
@ParameterizedTest
@NullAndEmptySource
void ThrowsException_NullOrEmptyUsername(String username) {
    var throwable = catchThrowable(() -> Username.of(username));
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
}
```

**@EnumSource**: Test all enum values
```java
@ParameterizedTest
@EnumSource(Role.class)
void CreatesUser_AllRoles(Role role) {
    var user = User.create(username, firstName, lastName, role);
    assertThat(user.getRole()).isEqualTo(role);
}
```

**@CsvFileSource**: Load data from external CSV file
```java
@ParameterizedTest
@CsvFileSource(resources = "/test-users.csv", numLinesToSkip = 1)
void ValidatesUser_FromCsvFile(String username, String email, boolean expectedValid) {
    // Test with data from CSV file
}
```

#### Test Naming with Parameterized Tests

**Use @ParameterizedTest name attribute** for readable test reports:
```java
@ParameterizedTest(name = "[{index}] Username=''{0}'' should be invalid")
@ValueSource(strings = {"", "invalid", "no@domain"})
void ThrowsException_InvalidUsername(String username) {
    // Test output: [1] Username='' should be invalid
    // Test output: [2] Username='invalid' should be invalid
}

@ParameterizedTest(name = "email={0}, valid={1}")
@CsvSource({
    "user@asapp.com, true",
    "invalid-email,  false"
})
void ValidatesEmailFormat(String email, boolean expectedValid) {
    // Test output: email=user@asapp.com, valid=true
}
```

**Placeholders**:
- `{index}` - Test invocation index
- `{0}`, `{1}`, ... - Parameter values
- `{arguments}` - All parameters
- `{displayName}` - Display name

#### Guidelines

1. **Choose the right annotation**:
   - Simple single value → `@ValueSource`
   - Multiple parameters, compact → `@CsvSource`
   - Complex objects, logic → `@MethodSource`

2. **Keep factory methods simple**: Data providers should be "dumb" (no complex logic)

3. **Use descriptive names**: `name` attribute makes test reports readable

4. **Group related scenarios**: One parameterized test per logical validation rule

5. **Don't overuse**: If scenarios require completely different setup/assertions, use separate tests

### @DisplayName Policy

**Policy**: Do NOT use @DisplayName annotation. Method names following the `<Behavior>_<Condition>` pattern are sufficient.

**Rationale**:
- **Your naming convention is clear**: `ReturnsUser_ValidId`, `ThrowsException_NullParameter` are self-documenting
- **Avoids duplication**: Combining long method names with @DisplayName creates redundancy
- **Maintains consistency**: All tests follow the same naming pattern
- **Reduces maintenance**: One less thing to keep in sync with method names
- **IDE-friendly**: Method names are visible everywhere (code, test runners, Git diffs)

**✅ CORRECT: Use descriptive method names only**
```java
@Nested
class Authenticate {

    @Test
    void ThrowsException_NullUsername() {
        // Clear from method name what's being tested
    }

    @Test
    void ReturnsAuthentication_ValidCredentials() {
        // No need for @DisplayName("Returns authentication when valid credentials")
    }
}
```

**❌ INCORRECT: Using @DisplayName creates duplication**
```java
@Nested
class Authenticate {

    @Test
    @DisplayName("Throws exception when username is null")  // ❌ Redundant
    void ThrowsException_NullUsername() {
        // Method name already says this
    }

    @Test
    @DisplayName("Returns authentication when valid credentials are provided")  // ❌ Duplicates method name
    void ReturnsAuthentication_ValidCredentials() {
        // Unnecessary annotation
    }
}
```

**Exception**: If you ever need to use @DisplayName for specific cases (e.g., client requirements, special characters), document the reason as a comment.

### Test Ordering

**Principle**: Organize tests for maximum readability by ordering @Nested classes by method declaration order and test methods by execution flow (failures first, then successes).

**Purpose**: While JUnit doesn't enforce test execution order by default (tests should be independent), **logical organization improves code readability and helps developers understand failure scenarios before happy paths**.

**Two-Level Ordering Strategy**:

#### Level 1: @Nested Class Order

**Rule**: @Nested classes must strictly follow the declaration order of methods in the class under test.

**Rationale**: Mirrors the structure of the production code, making it easy to navigate between implementation and tests.

**✅ CORRECT: @Nested classes follow method order**
```java
// Production code
class AuthenticationService {
    public Authentication authenticate(Credentials creds) { }  // Method 1
    public Authentication refresh(Token token) { }              // Method 2
    public void revoke(AuthId id) { }                          // Method 3
}

// Test class
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Nested
    class Authenticate {  // ✅ First @Nested - matches method 1
        // Tests for authenticate()
    }

    @Nested
    class Refresh {  // ✅ Second @Nested - matches method 2
        // Tests for refresh()
    }

    @Nested
    class Revoke {  // ✅ Third @Nested - matches method 3
        // Tests for revoke()
    }
}
```

**❌ INCORRECT: @Nested classes out of order**
```java
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Nested
    class Revoke {  // ❌ Wrong - should be third, not first
        // Tests for revoke()
    }

    @Nested
    class Authenticate {  // ❌ Wrong - should be first, not second
        // Tests for authenticate()
    }
}
```

#### Level 2: Test Method Order Within @Nested Classes

**Rule**: Within each @Nested class, order test methods as:
1. **Failure/Exception cases FIRST** (negative tests) - ordered by code execution flow
2. **Success cases LAST** (positive tests) - ordered by code execution flow

**Rationale**:
- **Failures first** follows a "guard clauses first" mental model
- Developers see **what can go wrong** before seeing the happy path
- **Execution flow ordering** mirrors how the code validates and processes data

**Code Execution Flow**: The order in which the production code validates inputs, processes data, and produces outputs:
1. Input validation
2. Business logic execution
3. Persistence/side effects
4. Return result

**✅ CORRECT: Failures first (by execution flow), then successes**
```java
@Nested
class Authenticate {

    // ❌ FAILURES FIRST - Ordered by execution flow in production code

    @Test
    void ThrowsException_NullUsername() {  // Step 1: Input validation
        // Test null username validation
    }

    @Test
    void ThrowsException_EmptyPassword() {  // Step 1: Input validation
        // Test empty password validation
    }

    @Test
    void ThrowsException_InvalidCredentials() {  // Step 2: Authentication logic
        // Test authentication failure
    }

    @Test
    void ThrowsException_TokenGenerationFails() {  // Step 3: Token generation
        // Test token generation failure
    }

    @Test
    void ThrowsException_PersistenceFails() {  // Step 4: Persistence
        // Test database save failure
    }

    @Test
    void ThrowsException_TokenActivationFails() {  // Step 5: Token activation
        // Test Redis activation failure
    }

    // ✅ SUCCESSES LAST - Ordered by execution flow

    @Test
    void ReturnsAuthentication_ValidCredentials() {  // Happy path - full flow
        // Test successful authentication with all steps
    }
}
```

**❌ INCORRECT: Mixed order or successes first**
```java
@Nested
class Authenticate {

    @Test
    void ReturnsAuthentication_ValidCredentials() {  // ❌ Success first - wrong order
        // Happy path
    }

    @Test
    void ThrowsException_TokenGenerationFails() {  // ❌ Failures mixed - wrong order
        // Token generation failure
    }

    @Test
    void ThrowsException_NullUsername() {  // ❌ Out of execution order
        // Null username
    }
}
```

#### Complete Example

```java
// Production code with methods in this order:
class UserService {
    public User create(CreateUserCommand cmd) { }     // Method 1
    public User update(UserId id, UpdateUserCommand cmd) { }  // Method 2
    public User findById(UserId id) { }               // Method 3
    public boolean delete(UserId id) { }              // Method 4
}

// Test class following ordering principles:
@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Nested
    class Create {  // ✅ First @Nested - matches method 1

        // ❌ Failures first (by execution flow)
        @Test
        void ThrowsException_NullCommand() {  // Input validation
            var throwable = catchThrowable(() -> userService.create(null));
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void ThrowsException_InvalidEmail() {  // Input validation
            var command = new CreateUserCommand("invalid-email", "John", "Doe");
            var throwable = catchThrowable(() -> userService.create(command));
            assertThat(throwable).isInstanceOf(ValidationException.class);
        }

        @Test
        void ThrowsException_UsernameAlreadyExists() {  // Business rule validation
            given(userRepository.existsByUsername(username)).willReturn(true);
            var throwable = catchThrowable(() -> userService.create(command));
            assertThat(throwable).isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        void ThrowsException_PersistenceFails() {  // Persistence
            given(userRepository.save(any())).willThrow(new DataAccessException("DB error"));
            var throwable = catchThrowable(() -> userService.create(command));
            assertThat(throwable).isInstanceOf(PersistenceException.class);
        }

        // ✅ Successes last
        @Test
        void ReturnsUser_ValidCommand() {  // Happy path
            var userToCreate = User.create(username, firstName, lastName);
            var savedUser = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            given(userRepository.save(userToCreate)).willReturn(savedUser);

            var actual = userService.create(command);

            assertThat(actual).isEqualTo(savedUser);
        }
    }

    @Nested
    class Update {  // ✅ Second @Nested - matches method 2

        // ❌ Failures first
        @Test
        void ThrowsException_UserNotFound() {  // Lookup failure
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            var throwable = catchThrowable(() -> userService.update(userId, updateCommand));
            assertThat(throwable).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void ThrowsException_InvalidEmail() {  // Input validation
            var throwable = catchThrowable(() -> userService.update(userId, invalidCommand));
            assertThat(throwable).isInstanceOf(ValidationException.class);
        }

        // ✅ Successes last
        @Test
        void ReturnsUpdatedUser_ValidCommand() {
            var existingUser = User.reconstitute(userId, oldFirstName, oldLastName, oldEmail, phoneNumber);
            var updatedUser = User.reconstitute(userId, newFirstName, newLastName, newEmail, phoneNumber);

            given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));
            given(userRepository.save(any())).willReturn(updatedUser);

            var actual = userService.update(userId, updateCommand);

            assertThat(actual).isEqualTo(updatedUser);
        }
    }

    @Nested
    class FindById {  // ✅ Third @Nested - matches method 3

        // ❌ Failures first
        @Test
        void ThrowsException_NullId() {
            var throwable = catchThrowable(() -> userService.findById(null));
            assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void ThrowsException_UserNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            var throwable = catchThrowable(() -> userService.findById(userId));
            assertThat(throwable).isInstanceOf(UserNotFoundException.class);
        }

        // ✅ Successes last
        @Test
        void ReturnsUser_UserExists() {
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            var actual = userService.findById(userId);

            assertThat(actual).isEqualTo(user);
        }
    }

    @Nested
    class Delete {  // ✅ Fourth @Nested - matches method 4

        // ❌ Failures first
        @Test
        void ThrowsException_UserNotFound() {
            given(userRepository.deleteById(userId)).willReturn(0);
            var throwable = catchThrowable(() -> userService.delete(userId));
            assertThat(throwable).isInstanceOf(UserNotFoundException.class);
        }

        // ✅ Successes last
        @Test
        void ReturnsTrue_SuccessfulDeletion() {
            given(userRepository.deleteById(userId)).willReturn(1);

            var actual = userService.delete(userId);

            assertThat(actual).isTrue();
        }
    }
}
```

#### Guidelines Summary

1. **@Nested class order**: Strictly follows method declaration order in production code
2. **Test method order within @Nested**:
   - **First**: Failure/exception cases (ordered by code execution flow)
   - **Last**: Success cases (ordered by code execution flow)
3. **Code execution flow**: Input validation → Business logic → Persistence → Return
4. **Purpose**: Readability and code navigation, not execution dependency
5. **Independence**: Tests must still be independent (order is for organization, not dependency)

**Note on JUnit Execution Order**:
- JUnit doesn't enforce test execution order by default (tests run in deterministic but non-obvious order)
- Tests **must be independent** - one test should not depend on another's execution
- This ordering is for **code organization and readability**, not execution dependency
- If you need to enforce execution order, use `@TestMethodOrder(OrderAnnotation.class)` and `@Order(n)` annotations

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
- Use BDDMockito (`given()`, `then()`) instead of Mockito (`when()`, `verify()`) - see [BDDMockito section](#bddmockito-not-regular-mockito)
- Use AssertJ (`assertThat()`, `catchThrowable()`) instead of JUnit assertions - see [AssertJ section](#assertj-not-junit-assertions)
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
