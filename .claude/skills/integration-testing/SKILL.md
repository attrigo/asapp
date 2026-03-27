---
name: integration-testing
description: Write integration tests for repository, controller, and E2E layers. Trigger on new IT class, test slice, TestContainers, WebMvcTest, SpringBootTest, WebTestClient, MockMvcTester, E2E test, controller IT, repository IT.
---

# Integration Testing

## Quick Reference

| Layer | Annotation | Test Client | Containers | Suffix |
|---|---|---|---|---|
| Repository | `@DataJdbcTest` | Repository (autowired) | PostgreSQL (+ Redis if needed) | `*IT.java` |
| Controller | `@WebMvcTest` | `MockMvcTester` | None | `*IT.java` |
| E2E | `@SpringBootTest` | `WebTestClient` | PostgreSQL + Redis | `*E2EIT.java` |

## Core Workflow

### 1. Choose Test Slice

**Repository IT** -- tests JDBC queries against real PostgreSQL:
```java
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class JdbcTaskRepositoryIT {
```

**Controller IT** -- tests request validation/error responses with mocked services:
```java
class TaskRestControllerIT extends WebMvcTestContext {
```

**E2E IT** -- tests full HTTP flow with real containers:
```java
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class TaskE2EIT {
```

### 2. Set Up TestContainerConfiguration

Place in `testutil` package. Use singleton pattern (static fields) so containers start once per test run, not per class.

```java
@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestContainerConfiguration {

    static {
        System.setProperty("api.version", "1.44");
    }

    @Container
    public static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.7"));

    @Container
    public static final RedisContainer redisContainer =
            new RedisContainer(DockerImageName.parse("redis:8.4.0-alpine"));

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return postgreSQLContainer;
    }

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {
        return redisContainer;
    }
}
```

### 3. Set Up WebMvcTestContext (Controller ITs Only)

Place in `testutil` package. All controller IT classes extend this base to share context.

```java
@WebMvcTest(controllers = { TaskRestController.class })
@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class })
public class WebMvcTestContext {

    @Autowired
    protected MockMvcTester mockMvc;

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @MockitoBean
    private ReadTaskService readTaskServiceMock;
    // ... all service + mapper mocks
}
```

### 4. Write @BeforeEach Cleanup

Clean resources BEFORE each test. Delete child entities first (FK constraints).

```java
// Repository IT
@BeforeEach
void beforeEach() {
    taskRepository.deleteAll();
}

// E2E IT (with Redis)
@BeforeEach
void beforeEach() {
    jwtAuthenticationRepository.deleteAll();
    userRepository.deleteAll();

    assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
    redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
}
```

### 5. Write Test Data Creation Helpers

Extract all external-resource creation into private helpers. Assert creation succeeded.

```java
private JdbcTaskEntity createTask() {
    var task = aJdbcTask();
    return createTask(task);
}

private JdbcTaskEntity createTask(JdbcTaskEntity task) {
    var createdTask = taskRepository.save(task);
    assertThat(createdTask).isNotNull();
    return createdTask;
}
```

For multi-store seeding (DB + Redis), use `ForYyy` suffix:
```java
private JdbcJwtAuthenticationEntity createJwtAuthenticationForUser(JdbcUserEntity user) {
    var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id()).buildJdbc();
    var created = jwtAuthenticationRepository.save(jwtAuthentication);
    assertThat(created).isNotNull();

    redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + created.accessToken().token(), "");
    redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + created.refreshToken().token(), "");
    return created;
}
```

### 6. Write Tests per Layer

See `references/test-layer-examples.md` for complete patterns per layer.

## Common Pitfalls

- **DO NOT** use `@Transactional` on integration tests for automatic rollback -- use explicit `@BeforeEach` cleanup
- **DO NOT** create a new `WebMvcTestContext` per controller -- one shared base per service reuses the Spring context
- **DO NOT** forget `@AutoConfigureTestDatabase(replace = NONE)` on `@DataJdbcTest` -- without it Spring replaces the datasource with an embedded DB
- **DO NOT** omit `@WithMockUser` on controller ITs for secured endpoints (tasks service) -- requests will get 401
- **DO NOT** forget the static initializer `System.setProperty("api.version", "1.44")` in TestContainerConfiguration -- required for Docker Engine 29.x compatibility
- **DO NOT** import `JacksonAutoConfiguration` in `@DataJdbcTest` unless the entity uses JSON columns -- the auth service needs it, the tasks service does not
- **DO NOT** seed the bearer token in Redis without first deleting any stale key from a prior test -- call `redisTemplate.delete()` then `opsForValue().set()` in `@BeforeEach`
