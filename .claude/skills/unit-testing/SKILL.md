---
name: unit-testing
description: >
  Write unit tests for domain and application service classes. Activates on "write unit test",
  "add test", "test this class", "unit test", "create tests for". Not for integration, E2E,
  controller, or repository tests.
---

## Quick Reference

| Aspect              | Convention                                                                  |
|---------------------|-----------------------------------------------------------------------------|
| File suffix         | `*Tests.java`                                                               |
| Framework           | JUnit 5 + AssertJ + BDDMockito                                             |
| Class annotation    | None (domain) or `@ExtendWith(MockitoExtension.class)` (application)        |
| Test path           | `src/test/java/` mirrors `src/main/java/` package structure                 |
| Nested classes      | PascalCase matching method under test (e.g., `CreateNewTask`, `Authenticate`) |
| Test naming         | `<Behavior>_<Condition>` (e.g., `ReturnsTask_ValidParameters`)              |
| Assertion library   | AssertJ only -- never JUnit assertions                                      |
| Exception testing   | `catchThrowable()` + `.isInstanceOf().hasMessage()`                         |
| Multi-field asserts | `assertSoftly()` with `.as()` on each assertion + `@formatter:off/on`       |
| Mock style          | BDDMockito (`given`, `willThrow`, `then`) -- never plain Mockito            |
| Result variable     | Always `actual`                                                             |
| Mutation testing    | PITest with 100% mutation threshold, targets `domain.*` + `application.*.in.service.*` |

## Core Workflow

### 1. Determine Test Type by Layer

**Domain tests** (`domain/**/*Tests.java`) -- NO mocks, NO Spring:
- Value objects: test constructor, `of()` factory, `value()` accessor, validation rejects
- Aggregates: test factory methods (`create`, `reconstitute`), behavior methods, equality, hashCode

**Application service tests** (`application/**/in/service/*Tests.java`) -- mocked dependencies:
- Annotate class with `@ExtendWith(MockitoExtension.class)`
- Declare ports as `@Mock` fields, service as `@InjectMocks`
- Test success path first, then failure paths ordered by execution flow

### 2. Structure Every Test with AAA Comments

```java
@Test
void ReturnsCreatedTask_ValidUser() {
    // Given
    var task = aTask();
    var command = new CreateTaskCommand(task.getUserId().value(), task.getTitle().value(),
            task.getDescription().value(), task.getStartDate().value(), task.getEndDate().value());

    given(taskRepository.save(any(Task.class))).willReturn(task);

    // When
    var actual = createTaskService.createTask(command);

    // Then
    assertSoftly(softly -> {
        // @formatter:off
        softly.assertThat(actual).as("created task").isNotNull();
        softly.assertThat(actual.getId()).as("ID").isNotNull();
        softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(task.getUserId());
        // @formatter:on
    });

    then(taskRepository).should(times(1))
                         .save(any(Task.class));
}
```

### 3. Write Value Object Tests

Cover three groups per value object -- each as a `@Nested` class:

```java
class TitleTests {

    @Nested
    class CreateTitleWithConstructor {
        @Test
        void ReturnsTitle_ValidTitle() { /* ... */ }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankTitle(String title) { /* ... */ }
    }

    @Nested
    class CreateTitleWithFactoryMethod { /* same shape as constructor tests */ }

    @Nested
    class GetValue {
        @Test
        void ReturnsTitleValue_ValidTitle() { /* ... */ }
    }
}
```

For UUID-based IDs (e.g., `TaskId`), use `@Test` for null check instead of `@ParameterizedTest`.

### 4. Write Aggregate Tests

Cover all lifecycle states. Each factory method and behavior method gets a `@Nested` class:

- **Factory methods**: success with all fields, success with each nullable field as null, then validation failures
- **Behavior methods**: success on each state, nullable variations, then validation failures per state
- **Equality**: reflexive, transitive, null, wrong type, cross-state, different IDs
- **HashCode**: same ID returns same hash, new entities differ, cross-state differs, different IDs differ
- **Getters**: one test per state per field

### 5. Write Application Service Tests

```java
@ExtendWith(MockitoExtension.class)
class CreateTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CreateTaskService createTaskService;

    @Nested
    class CreateTask {
        @Test
        void ReturnsCreatedTask_ValidUser() { /* success path */ }

        @Test
        void ThrowsRuntimeException_TaskPersistenceFails() { /* failure path */ }
    }
}
```

**Given block two-phase pattern**: (1) create test data, (2) configure mocks.
**Then block order**: (1) assert results, (2) verify mock interactions.

Use test factories for success paths (`aTask()`, `aUser()`). Use inline primitives for failure paths.

### 6. Use Test Data Factories

Factories live in `testutil/fixture/` and follow Object Mother + Builder pattern:

```java
// Quick default aggregate
var task = aTask();

// Customized via builder -- wither methods accept primitives
var task = aTaskBuilder().withUserId(UUID.fromString("..."))
                         .withTitle("Custom Title")
                         .build();
```

Constants live in `TestFactoryConstants` (package-private) -- one per service.

### 7. Run Mutation Testing

```bash
# Single service
cd services/<service-name>
mvn org.pitest:pitest-maven:mutationCoverage

# Must achieve 100% mutation kill rate
```

PITest targets per service (configured in service `pom.xml`):

```xml
<targetClasses>
    <param>com.bcn.asapp.<service>.domain.*</param>
    <param>com.bcn.asapp.<service>.application.*.in.service.*</param>
</targetClasses>
```

## Common Pitfalls

- **Using `@Service` in tests**: Application services use `@ApplicationService`, but tests use `@ExtendWith(MockitoExtension.class)` -- never load Spring context for unit tests
- **Mixing Mockito and BDDMockito**: Always use `given`/`willThrow`/`then`, never `when`/`thenReturn`/`verify`
- **Using `any()` when specific values are available**: Prefer specific variables for mock precision; use `any()` only for values the test cannot predict (e.g., `any(Task.class)` when the service creates the object internally)
- **Forgetting `assertSoftly` for 3+ properties**: Groups of 3+ assertions on the same result must use `assertSoftly` with `.as()` labels
- **Skipping `@formatter:off/on`**: Always wrap `assertSoftly` blocks with formatter directives
- **Mocks in domain tests**: Domain layer must be infrastructure-agnostic -- never mock
- **Placing test data in class-level fields**: Keep test data inline in methods; class-level fields only for infrastructure config
- **Missing compensation test coverage**: When a service has compensating transactions (e.g., `AuthenticateService`), test both compensation-success and compensation-failure paths