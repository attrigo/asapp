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
- **TestContainers**: PostgreSQL containers (⚠️ use `static` for singletons!)
- **MockServer**: Mock external services in E2E tests

**CI/CD**: GitHub Actions runs `mvn verify` on push/PR to main

## Key Patterns

### Test Naming
```java
@Nested
class CreateInactiveUser {  // PascalCase: method/behavior under test

    @Test
    void ThenReturnsUser_GivenValidParameters() {  // Then<Expected>_Given<Condition>
        // Given
        var username = Username.of("user@asapp.com");

        // When
        var user = User.inactiveUser(username, password, role);

        // Then
        assertThat(user.getId()).isNull();
    }
}
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
- Examples: `CreateInactiveUser`, `Authenticate`, `GetRoleClaim`

**Method Names**: `Then<Expected>_Given<Condition>`
- Examples: `ThenThrowsException_GivenNull()`, `ThenReturnsTrue_GivenValid()`

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
