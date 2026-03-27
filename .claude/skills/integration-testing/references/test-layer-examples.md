# Integration Test Layer Examples

## Repository IT (@DataJdbcTest)

Annotation stack:
```java
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
```

Add `JacksonAutoConfiguration.class` to `@Import` only when entities use JSON columns (e.g., auth service `JdbcJwtAuthenticationEntity`).

Full example:
```java
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class JdbcTaskRepositoryIT {

    @Autowired
    private JdbcTaskRepository taskRepository;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();
    }

    @Nested
    class FindByUserId {

        @Test
        void ReturnsFoundTasks_TasksExistForUserId() {
            // Given
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");
            var task1 = aTaskBuilder().withUserId(userId).buildJdbc();
            var task2 = aTaskBuilder().withUserId(userId).buildJdbc();
            var createdTask1 = createTask(task1);
            var createdTask2 = createTask(task2);

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).hasSize(2)
                              .containsExactlyInAnyOrder(createdTask1, createdTask2);
        }

        @Test
        void ReturnsEmptyList_TasksNotExistForUserId() {
            // Given
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }
    }

    private JdbcTaskEntity createTask(JdbcTaskEntity task) {
        var createdTask = taskRepository.save(task);
        assertThat(createdTask).isNotNull();
        return createdTask;
    }
}
```

## Controller IT (@WebMvcTest)

Extend `WebMvcTestContext`. Use `MockMvcTester` (AssertJ-based). Use `@WithMockUser` for secured endpoints.

Assertion pattern for RFC 7807 Problem Details:
```java
@WithMockUser
class TaskRestControllerIT extends WebMvcTestContext {

    @Nested
    class CreateTask {

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(TASKS_CREATE_FULL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                               .containsEntry("type", "about:blank")
                               .containsEntry("title", "Bad Request")
                               .containsEntry("status", 400)
                               .containsEntry("instance", "/api/tasks");
                       assertThatJson(jsonContent).inPath("detail")
                               .asString()
                               .contains("The user ID must not be empty",
                                         "The title must not be empty");
                       //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                               .isArray()
                               .containsOnly(
                                       Map.of("entity", "createTaskRequest",
                                              "field", "userId",
                                              "message", "The user ID must not be empty"),
                                       Map.of("entity", "createTaskRequest",
                                              "field", "title",
                                              "message", "The title must not be empty")
                               );
                       //@formatter:on
                   });
        }
    }
}
```

Standard validation test cases to cover per endpoint:
1. `_NonJsonRequestBody` -- POST/PUT with `TEXT_PLAIN` returns 415
2. `_MissingRequestBody` -- POST/PUT with empty string returns 400
3. `_EmptyRequestBody` -- POST/PUT with `{}` returns 400 with field errors
4. `_EmptyMandatoryFields` -- POST/PUT with blank values returns 400
5. `_MissingId` / `_InvalidId` -- GET/PUT/DELETE with bad path param

## E2E IT (@SpringBootTest)

Annotation stack:
```java
@SpringBootTest(classes = AsappTasksServiceApplication.class,
                webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
```

JWT authentication setup -- generate a valid token and seed it in Redis:
```java
private final String encodedAccessToken = encodedAccessToken();
private final String bearerToken = "Bearer " + encodedAccessToken;

@BeforeEach
void beforeEach() {
    taskRepository.deleteAll();

    assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
    redisTemplate.opsForValue().set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
}
```

WebTestClient patterns:

**GET returning body:**
```java
var actual = webTestClient.get()
        .uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(GetTaskByIdResponse.class)
        .returnResult()
        .getResponseBody();

assertThat(actual).isEqualTo(expectedResponse);
```

**POST with body + DB verification:**
```java
var actual = webTestClient.post()
        .uri(TASKS_CREATE_FULL_PATH)
        .header(HttpHeaders.AUTHORIZATION, bearerToken)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .bodyValue(createTaskRequestBody)
        .exchange()
        .expectStatus().isCreated()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(CreateTaskResponse.class)
        .returnResult()
        .getResponseBody();

assertThat(actual).isNotNull()
                  .extracting(CreateTaskResponse::taskId)
                  .isNotNull();

var createdTask = taskRepository.findById(actual.taskId());
assertThat(createdTask).as("task optional").isPresent();
assertSoftly(softly -> {
    // @formatter:off
    softly.assertThat(createdTask.get().id()).as("id").isEqualTo(actual.taskId());
    softly.assertThat(createdTask.get().title()).as("title").isEqualTo(request.title());
    // @formatter:on
});
```

**Unauthorized test (omit Authorization header):**
```java
webTestClient.get()
        .uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody().isEmpty();
```

Standard E2E test cases per CRUD endpoint:
1. Success case -- returns expected status and body, verify DB state
2. Not-found case -- returns 404 with empty body
3. Unauthorized case -- omit `Authorization` header, returns 401

## Custom Assertion Helpers (E2E)

For multi-store verification, create private assertion helpers:
```java
private void assertAuthenticationNotExist(JdbcJwtAuthenticationEntity expected) {
    var id = expected.id();
    var accessToken = expected.accessToken().token();
    var refreshToken = expected.refreshToken().token();

    var actualAuth = jwtAuthenticationRepository.findById(id);
    var actualAccessExists = redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + accessToken);
    var actualRefreshExists = redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + refreshToken);

    assertSoftly(softly -> {
        // @formatter:off
        softly.assertThat(actualAuth).as("authentication in database").isEmpty();
        softly.assertThat(actualAccessExists).as("access token exists in Redis").isFalse();
        softly.assertThat(actualRefreshExists).as("refresh token exists in Redis").isFalse();
        // @formatter:on
    });
}
```

## Test Data Factories

Use existing factories from `testutil.fixture` package with builder pattern:

```java
// Domain object with defaults
var task = aTask();

// JDBC entity with defaults
var jdbcTask = aJdbcTask();

// Customized via builder
var task = aTaskBuilder().withUserId(userId).withTitle("Custom").build();
var jdbcTask = aTaskBuilder().withUserId(userId).buildJdbc();

// JWT tokens
var accessToken = encodedAccessToken();
var expiredToken = anEncodedTokenBuilder().accessToken().expired().build();
```