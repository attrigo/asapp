---
paths:
  - "**/test/**/*.java"
---

How test classes and methods are structured, named and written — `*Tests.java` (unit), `*IT.java` (integration), `*E2EIT.java` (end-to-end).

Contents: Test Documentation (Javadoc) · Annotation Ordering · Test Class Structure · Test Naming Conventions · Test Method Structure · Given Block Structure · Then Block Structure · Assertion Patterns · Soft Assertions · Async Waiting · Formatter Guards · Mocking · Parameterized Test Sources · Variable Naming · Test Data & Fixtures

## Test Documentation (Javadoc)

Javadoc shape — summary line, `<p>Setup:` list, then `<p>Coverage:` list of bare `<li>` items (no `<ul>`): see `JwtDecoderTests.java:40-53`.

- Omit @since tags in test classes (`*Tests.java`, `*IT.java`, `*E2EIT.java`)
- Include @since tags in test utility classes (`testutil` package: factories, `TestContainerConfiguration`, `WebMvcTestContext`, assertions)
- Summary line must start with `Tests {@link ClassName}` followed by aspects in plain language (e.g., `Tests {@link Username} validation and value access.`)
- Exception: classes with no single class under test (e.g. E2EIT, endpoint and lockdown ITs, ArchUnit) use `Tests <what>` without `{@link}`
- Summary describes what's tested, not what the class is — use concrete terms (validation, factory methods, equality, compensation), avoid DDD jargon (encapsulates, enforces invariants)
- Coverage list items must not reference class names, fields or methods — describe behaviors instead
- Optional Setup list before Coverage: add when a class has meaningful shared setup — slice and mock wiring alone doesn't qualify; same wording rules as Coverage

## Annotation Ordering

Class-level order: test context (`@SpringBootTest`, `@DataJdbcTest`) → its `@AutoConfigure*` companions → `@Import`

## Test Class Structure

### @Nested classes

- Name each @Nested class after the method under test (`Authenticate`, `GetUsername`); verb-prefix factory methods and record accessors (`CreateInactiveUser` for `inactiveUser()`, `GetValue` for `value()`)
- Classes with no method-level subject (security, lockdown and documentation ITs) group by scenario theme (`ActuatorAuthentication`, `SwaggerExposure`)
- Don't duplicate helper methods across @Nested classes

### Ordering

- **@Nested class order**: Follow method declaration order in source
- **Test method order** within @Nested classes:
  1. Success cases first — simplest scenario, then more complex variations
  2. Failure cases last — ordered by execution flow (validation → logic → persistence)

## Test Naming Conventions

- Name methods `<Behavior>_<Condition>` (`ReturnsUser_ValidId`, `ThrowsException_NullParameter`)
- `<Behavior>` is any present-tense third-person verb — mostly `Returns` and `Throws`
- Don't combine actions and returns (e.g., `DeletesAndReturnsUser_`)
- Avoid camelCase, articles ("a", "an", "the"), filler words ("should", "will")
- Name by layer, not by test tier: abstractions (Store, Repository) in domain and application tests; concrete infrastructure names (Redis, Database, Db, Cache) in infrastructure tests and ITs
- `Missing` when a required input is absent (`_MissingAuthorizationHeader`, never `_NoAuthorizationHeader`); `NotFound` when a lookup returns nothing (`_AccessTokenNotFound`)

| Condition kind | When | Structure | DO | DON'T |
|----------------|------|-----------|-------|----------|
| Quality/State | Standalone adjective describing overall nature: valid, null, expired, empty, missing, present, non-X | `_<Adjective><Noun>` | `_ValidToken`, `_ExpiredToken`, `_MissingRoleClaim`, `_NonStringRoleClaim` | `_TokenValid`, `_RoleClaimMissingInToken` |
| Specific Attribute | Names a specific property/field of the noun and its value | `_<Noun><Property><Value>` | `_TokenRefreshType`, `_UserAdminRole`, `_TokenUseClaimNotAccess` | — |
| Existence | Condition checks presence or absence in a store | `_<Noun>Exist(s)` | `_UserExists`, `_UsersNotExist` | `_NoUsers` |
| Adding Context | Condition needs scoping to a specific context | `_<Noun><Verb>For<Context>` | `_TasksNotExistForUserId` | `_TasksNotExist` |
| Multi-State | Condition involves multiple locations or states | `_<Noun><State>In<Location>...` | `_TokenValidInHeaderNotExistsInRedis` | `_ValidTokenNotInRedis` |
| Compound Existence | Condition combines existence of an entity with absence of a related entity | `_<Noun>ExistsWith/Without<RelatedNoun>` | `_UserExistsWithoutAuthentications` | `_UserExistsAuthenticationsNotExist` |
| Possession | Condition describes what an entity owns or contains | `_<Noun>Has[No]<RelatedNoun>` | `_UserHasTasks`, `_UserHasNoTasks` | `_UserTasksExist` |
| Action/Event | Condition describes something that occurs during execution | `_<Noun><Verb>` | `_DatabaseOperationFails`, `_CacheConnectionFails`, `_TokenGenerationFails` | — |

## Test Method Structure

- Skip `// Given` when no setup is needed
- Use `// When & Then` only when the assertion chains directly onto the action

## Given Block Structure

- Expected values involving computation or transformation (`.of()`, `.value()`, `.fromString()`) must be prepared in Given block, not Then block
- Inline a value used only once; name it only when referenced more than once or its construction is non-trivial

## Then Block Structure

- Then block order: (1) Assert data/results first, (2) Verify mock interactions second — captor assertions come last, after the verification that captures
- Use `inOrder()` when interaction sequence matters

## Assertion Patterns

- Use AssertJ assertions exclusively, not JUnit assertions
- Use `catchThrowable()` for exception testing, not `assertThatThrownBy()` or `assertThrows()`
- Chain `catchThrowable()` with `.isInstanceOf(Type.class).hasMessage()` — use `hasMessageContaining()` only when the message includes dynamic values
- Reserve `extracting()` for projecting fields from collections; assert single-object fields with direct getters

## Soft Assertions

- Use `assertSoftly()` when asserting 3 or more properties on the same result, or when grouping sibling results of one operation — navigating into nested properties still counts as asserting on the same root
- Keep the result and any `captor.getValue()` assertions in separate `assertSoftly` blocks; sibling results of one operation may share one
- A description is mandatory for each assertion — `.as()` or `describedAs()` must be placed before the assertion method (silently ignored if placed after)
- `.as()` descriptions must use a concise human-readable noun phrase naming the domain concept asserted; never generic labels or camelCase identifiers

## Async Waiting

- Use Awaitility (`await()`) instead of `Thread.sleep()` for async waiting (available transitively via the Boot test starters)

## Formatter Guards

Use `// @formatter:off/on` only for:

- `assertSoftly` blocks — keeps each soft assertion on one line
- a `.containsEntry(...)` whose argument is a method chain like `createdUser.id().toString()` — without the guard the formatter breaks the chain across lines
- `satisfiesExactly…` lambdas — guard just inside `(` when there are multiple lambdas or a method-chain argument; leave a single lambda with simple arguments inline
- RestDocs `document(...)` descriptor lists — keeps one field descriptor per line
- `@TestPropertySource` property arrays — keeps one property per line
- `@ArchTest` rule declarations — one guard per rule; without it the field name sets the chain's indentation

## Mocking

- Domain tests (`**/domain/**/*Tests.java`) never use mocks
- Use BDDMockito syntax exclusively
- Use specific variables instead of `any()` matchers; reserve `any()` only when the exact value is irrelevant or testing generic error handling

## Parameterized Test Sources

- Source selection: `@ValueSource` for inline literals, `@MethodSource` for computed, non-primitive or multi-parameter sets, `@EnumSource` for enum exhaustiveness, `@NullAndEmptySource` for null and blank inputs — stack it on `@ValueSource` when both apply. Never `@CsvSource`

## Variable Naming

| Variable Type | Required Name | Prohibited |
|---------------|---------------|-------------|
| Method result | `actual` | `result`, `output`, `sut` |
| All other test data | Domain name | `expectedToken`, `testUser`, `userList` |
| Mock fields | Domain name, no `Mock` affix | `repositoryMock`, `mockRepository` |

- When multiple variables of the same type play different roles, use prefixes to disambiguate. When only one exists, use plain domain names
- Suffix the raw primitive `Value` when it sits beside its value object (`userIdValue` / `userId`)
- When a test has multiple result variables (e.g., primary SUT result + a verification query in `// Then` to assert side effects), use `actual` for the primary SUT result and a domain name for the secondary (e.g., `createdUser`, `updatedUser`)

## Test Data & Fixtures

### Values

- Use fixed values by default; dynamic values only as factory builder defaults (`testing-factories.md`)
- Only specify values relevant to what you're testing; let factories provide defaults for unrelated fields

### Where to define data

- Create test data inline in test methods; reserve class-level fields for infrastructure config (secrets, base URLs, timeout values) — never the test subject, a domain object, or data under validation

### How to create data

Choose the simplest pattern that works (ordered by priority):

| Pattern                       | When to Use                                                  |
|-------------------------------|--------------------------------------------------------------|
| Inline Value Objects          | One-off simple value (userId, username)                      |
| Extract from Aggregates       | Need one or two fields from existing object                  |
| Object Mother Nested Entities | Need reusable component of an aggregate (JWT, encoded token) |
| Object Mother Aggregates      | Need full aggregate with related entities (User, Task)       |

- Patterns are composable. When extracting from aggregates, create the aggregate itself using the simplest sufficient pattern
