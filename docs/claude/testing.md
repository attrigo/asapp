# Testing Strategy

## Test Types and Naming
- **Unit tests**: `*Tests.java` - Fast, isolated domain/application logic tests
- **Integration tests**: `*IT.java` - Use TestContainers for PostgreSQL
- **Controller tests**: `*ControllerIT.java` - WebTestClient for HTTP testing
- **E2E tests**: `*E2EIT.java` - Full application context and workflows

## Test Structure and Conventions

### Test Class Organization
- Use `@Nested` classes to group related tests by method
- Nested class names use **PascalCase** describing the method or behavior under test
  - Examples: `CreateInactiveUser`, `Authenticate`, `CheckIsAccessToken`, `GetRoleClaim`

### Test Method Naming
Follow the pattern: `Then<Expected>_Given<Condition>`
- **Then** part: Describes the expected outcome or behavior
- **Given** part: Describes the input condition or scenario
- Examples:
  - `ThenThrowsIllegalArgumentException_GivenUsernameIsNull()`
  - `ThenReturnsTrue_GivenTypeAndTokenUseClaimAreAccessToken()`
  - `ThenAuthenticatesUser_GivenAuthenticationRequestIsValid()`

### Test Body Structure (Given-When-Then)
All tests follow the **Given-When-Then** pattern with comments:

```java
@Test
void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
    // Given
    var user = User.inactiveUser(username, password, role);
    var newPassword = EncodedPassword.of("{noop}new_password");

    // When
    var thrown = catchThrowable(() -> user.update(null, newPassword, role));

    // Then
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                      .hasMessage("Username must not be null");
}
```

### Mocking with BDDMockito
- Always use **BDDMockito** style instead of regular Mockito
- Use `@ExtendWith(MockitoExtension.class)` for tests with mocks
- Use `@Mock` for dependencies and `@InjectMocks` for the class under test
- BDDMockito methods:
  - `given()`: Setup mock behavior (replaces `when()`)
  - `willThrow()`: Setup exception throwing (replaces `doThrow()`)
  - `then()`: Verify interactions (replaces `verify()`)

```java
@ExtendWith(MockitoExtension.class)
class DefaultAuthenticatorTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private DefaultAuthenticator defaultAuthenticator;

    @Test
    void ThenThrowsBadCredentialsException_GivenAuthenticationFails() {
        // Given
        willThrow(new BadCredentialsException("Invalid credentials"))
            .given(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When
        var thrown = catchThrowable(() -> defaultAuthenticator.authenticate(request));

        // Then
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        then(authenticationManager).should(times(1))
                                   .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
```

### Common Patterns
- Use `catchThrowable()` from AssertJ to test exceptions
- Use `assertThat()` from AssertJ for all assertions
- Define test data as class-level constants when reused across multiple tests
- Use `@ParameterizedTest` with `@NullAndEmptySource` for validating null/empty inputs

## Test Data Builders
Located in `testutil/` packages:
- `UserDataFaker`: Generate fake user data
- `JwtDataFaker`: Generate fake JWT tokens
- `TestDataFaker`: Domain-specific test data generators
- `TestContainerConfiguration`: PostgreSQL test container setup

### When to Use Builders vs Inline Test Data

**Use Test Data Builders for:**
- **Complex technical setup**: JWT tokens, HTTP requests, database entities with many fields
- **Infrastructure objects**: Objects that require intricate API calls (e.g., JJWT builder, Spring mocks)
- **Repeated test data**: When the same complex setup appears across multiple tests
- **Rationale**: Builders hide irrelevant technical details and let tests focus on the scenario being tested

**Create Inline for:**
- **Simple data**: Primitives, Strings, simple POJOs with 2-3 fields
- **Domain objects under test**: When the object itself is what you're testing
- **Unique scenarios**: One-off test cases where the specific values matter for understanding
- **Rationale**: Keeps tests self-contained and explicit about what's being tested

**Example:**
```java
// ✅ Use builder for JWT tokens (complex technical setup)
var expiredToken = testEncodedTokenBuilder().accessToken()
                                           .withSecretKey(secretKey)
                                           .expired()
                                           .build();

// ✅ Create inline for simple domain objects
var username = Username.of("user@asapp.com");
var password = EncodedPassword.of("{noop}password");
var role = Role.USER;
```

The key principle: **Hide technical complexity that's irrelevant to the test's intent, but keep domain concepts explicit.**

## Running Specific Tests
```bash
# Run single test class
mvn test -Dtest=UserTests

# Run integration tests only
mvn verify -DskipUnitTests

# Run with coverage
mvn clean verify jacoco:report
```