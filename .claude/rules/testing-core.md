---
paths:
  - "**/test/**/*.java"
  - "**/*Tests.java"
  - "**/*IT.java"
  - "**/*E2EIT.java"
---

Suffixes: `*Tests.java` (unit), `*IT.java` (integration), `*E2EIT.java` (end-to-end)

## 1. Test Organization & Structure

### 1.1 Test Documentation (Javadoc)

- Omit @since tags in test classes (`*Tests.java`, `*IT.java`, `*E2EIT.java`)
- Include @since tags in test utility classes (`testutil` package: factories, `TestContainerConfiguration`, `WebMvcTestContext`, assertions)
- Summary line MUST start with `Tests {@link ClassName}` followed by aspects in plain language (e.g., `Tests {@link Username} validation and value access.`)
- Exception: E2EIT classes use `Tests <what>` without `{@link}` (no single class under test)
- Summary describes what's tested, not what the class is — use concrete terms (validation, factory methods, equality, compensation), avoid DDD jargon (encapsulates, protects boundaries)
- Coverage list items MUST NOT reference class names, fields or methods — describe behaviors instead

### 1.2 Annotation Ordering

1. **Test Context**: `@SpringBootTest`, `@WebMvcTest`, `@DataJdbcTest`, `@Import`
2. **Infrastructure**: `@Testcontainers`, `@Container`, `@AutoConfigureTestDatabase`
3. **Mocking**: `@MockitoBean`, `@Mock`, `@InjectMocks`, `@Captor`
4. **Test Markers**: `@Test`, `@Nested`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`

### 1.3 Test Class Structure

#### @Nested Classes

- @Nested classes provide the "When" context
- Group tests by method/behavior using PascalCase matching the method under test (e.g., `Authenticate`, `CreateInactiveUser`)

#### Test Ordering Rules

- **@Nested class order**: Follow method declaration order in source
- **Test method order** within @Nested classes:
  1. Success cases FIRST — simplest scenario first, more complex variations after
  2. Failure cases LAST — ordered by execution flow (validation → logic → persistence)


#### @DisplayName Policy

**DON'T**: Use @DisplayName annotation - use `<Behavior>_<Condition>` pattern instead.

### 1.4 Test Naming Conventions

**Pattern**: `<Behavior>_<Condition>` (e.g., `ReturnsUser_ValidId`, `ThrowsException_NullParameter`)

**Behavior verbs**: `Returns`, `Throws`, `Creates`, `Deletes`, `Updates`, `Persists`

**DON'T**: Combine actions and returns (e.g., `DeletesAndReturnsUser_` - implies unclear responsibility)

- Use underscore separator between behavior and condition
- Avoid camelCase, articles ("a", "an", "the"), filler words ("should", "will")
- Use concrete infrastructure names (Redis, PostgreSQL) ONLY in integration tests (`*IT.java`)
- Use abstractions (Store, Repository) in domain/application tests
- Don't use "Given/When/Then" keywords

#### Condition Pattern Selection

| Pattern | When | Structure | DO | DON'T |
|---------|------|-----------|-------|----------|
| **Quality/State** | Standalone adjective describing overall nature: valid, null, expired, empty, missing, present, non-X | `_<Adjective><Noun>` | `_ValidToken`, `_ExpiredToken`, `_MissingRoleClaim`, `_NonStringRoleClaim` | `_TokenValid`, `_RoleClaimMissingInToken` |
| **Specific Attribute** | Names a specific property/field of the noun and its value | `_<Noun><Property><Value>` | `_TokenRefreshType`, `_UserAdminRole`, `_TokenUseClaimNotAccess` | `_RefreshTypeToken` |
| **Existence** | Condition checks presence or absence | `_<Noun>Exist(s)` | `_UserExists`, `_UsersNotExist` | `_NoUsers` |
| **Adding Context** | Condition needs scoping to a specific context | `_<Noun><Verb>For<Context>` | `_TasksNotExistForUserId` | `_TasksNotExist` |
| **Multi-State** | Condition involves multiple locations or states | `_<Noun><State>In<Location>...` | `_TokenValidInHeaderNotExistsInRedis` | `_ValidTokenNotInRedis` |
| **Compound Existence** | Condition combines existence of an entity with absence of a related entity | `_<Noun>ExistsWith/Without<RelatedNoun>` | `_UserExistsWithoutAuthentications` | `_UserExistsAuthenticationsNotExist` |
| **Possession** | Condition describes what an entity owns or contains | `_<Noun>Has[No]<RelatedNoun>` | `_UserHasTasks`, `_UserHasNoTasks` | `_UserTasksExist` |
| **Action/Event** | Condition describes something that occurs during execution | `_<Noun><Verb>` | `_DatabaseOperationFails`, `_CacheConnectionFails`, `_TokenGenerationFails` | `_FailedDatabaseOperation`, `_DatabaseOperationFailure` |

#### Condition Word Distinctions

| Distinction | When to Use Each | Examples |
|-------------|------------------|----------|
| **Matches vs Equals** | Pattern/criteria vs exact value | `_PasswordMatchesPattern` vs `_CountEquals10` |
| **Exists vs Found** | Existence check vs search result | `_UserExists` vs `_UserFound` |
| **NotExists vs NotFound** | Existence check vs failed search | `_UserNotExists` vs `_UserNotFound` |
| **Missing vs NotFound** | Required but absent vs search failed | `_MissingAuthorizationHeader` vs `_UserNotFound` |

## 2. Test Implementation

### 2.1 Test Method Structure

- Test methods MUST use explicit AAA comment blocks: `// Given`, `// When`, `// Then`
- Skip `// Given` when no setup is needed
- Use `// When & Then` when the assertion is fluently chained to the action (e.g., MockMvc, RestTestClient)

### 2.2 Given Block Structure

- Given block: create all test data before configuring mocks that depend on it
- Order Given block variables by where they are consumed: When variables first, stub-only second, assertion-only last
- Expected values involving computation or transformation (`.of()`, `.value()`, `.fromString()`) MUST be prepared in Given block, not Then block
- Inline values used in only one place (Given or Then blocks). Declare a named variable in Given block only when referenced in multiple places, the construction is non-trivial, or a descriptive name adds clarity.

### 2.3 Assertion Patterns

- Use AssertJ assertions exclusively, NOT JUnit assertions
- Use `catchThrowable()` for exception testing, NOT `assertThatThrownBy()` or `assertThrows()`
- Chain `catchThrowable()` with `.isInstanceOf(Type.class).hasMessage()` — use `hasMessageContaining()` only when the message includes dynamic values
- Use `assertSoftly()` when asserting 3 or more properties on the same result — navigating into nested properties still counts as asserting on the same root
- Assertions on different root variables (e.g., `actual` vs `captor.getValue()`) are separate groups
- When using `assertSoftly()`, `.as()` descriptions are MANDATORY for each assertion — `.as()` MUST be placed BEFORE the assertion method (silently ignored if placed after)
- `.as()` descriptions MUST use human-readable concise noun phrase naming the domain concept being asserted (AssertJ convention), avoid generic labels or camelCase identifiers
- Use `// @formatter:off/on` around multi-line assertion blocks to preserve alignment
- Use Awaitility (`await()`) instead of `Thread.sleep()` for async waiting (available via `spring-boot-starter-test`)

### 2.4 Then Block Structure

- Then block order: (1) Assert data/results first, (2) Verify mock interactions second
- Use `inOrder()` when interaction sequence matters

## 3. Test Code Conventions

### 3.1 Variable Naming

**Pattern**:

| Variable Type | Required Name | Prohibited |
|---------------|---------------|-------------|
| Method result | `actual` | `result`, `output`, `sut` |
| All other test data | Domain name | `expectedToken`, `testUser`, `userList` |
| Mock fields | No "Mock" suffix | `repositoryMock`, `mockRepository` |

- NEVER add "Mock" or "mockXxx" suffixes to mock field names
- When multiple variables of the same type play different roles, use prefixes to disambiguate. When only one exists, use plain domain names
- When a test has multiple result variables (e.g., primary SUT result + a verification query in `// Then` to assert side effects), use `actual` for the primary SUT result and a domain name for the secondary (e.g., `createdUser`, `updatedUser`)

### 3.2 Test Data & Fixtures

#### 3.2.1 Value Strategy

- Use fixed values by default; random values ONLY for property-based testing or integration tests with data variety (use fixed seed or document strategy)
- Only specify values relevant to what you're testing; let factories provide defaults for unrelated fields

#### 3.2.2 Scope Strategy (Where to Define Data)

- **Inline test data**: Create test data directly in test methods for visibility and self-containment
- **Class-level data**: Only for infrastructure config (secrets, base URLs, timeout values) that are NOT the test subject

**DON'T**:
- Use class-level fields for test subject values, domain objects, or data being validated
- Duplicate helper methods across @Nested classes

#### 3.2.3 Pattern Selection (How to Create Data)

**Choose the simplest pattern that works (ordered by priority)**:

| Pattern                           | When to Use                                   |
|-----------------------------------|-----------------------------------------------|
| **Inline Value Objects**          | One-off simple value (userId, username)      |
| **Extract from Aggregates**       | Need one or two fields from existing object  |
| **Object Mother Nested Entities** | Need reusable complex object (JWT, User)     |
| **Object Mother Aggregates**      | Need full aggregate with related entities    |
| **Helper Methods**                | Testing constructor/validation edge cases    |

- Patterns are composable. When extracting from aggregates, create the aggregate itself using the simplest sufficient pattern

### 3.3 Test Lifecycle

#### @BeforeEach

- Complex initialization, mock-dependent setup, dynamic/random data, conditional setup logic, mutable state, database cleanup (clean before test)

#### @AfterEach

- Use ONLY for external resources requiring explicit cleanup (file handles, network connections, locks). Database cleanup goes in @BeforeEach instead
