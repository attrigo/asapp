---
paths:
  - "**/*IT.java"
---

Slice, container and external-service-mock wiring for integration tests, plus cleanup and helper-naming conventions.

## Test Slice Selection

- **`@WebMvcTest`** (web layer) — extend `WebMvcTestContext` (security config + `MockMvcTester`), or `RestDocsWebMvcTestContext` (`MockMvc`) for `*ApiDocumentationIT`
- **`@DataJdbcTest`** (repository layer) — use `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` to use real database
- **`@SpringBootTest`** (full application) — when testing web layer, use `webEnvironment = RANDOM_PORT` with `@AutoConfigureRestTestClient`

## TestContainers

- Slice tests needing real infrastructure import `TestContainerConfiguration.class`
- Expose containers as `@Bean` methods with `@ServiceConnection` annotation

## Resource Cleanup

- Clean external stores (database, Redis) in `@BeforeEach`, never in `@AfterEach`
- Reserve `@AfterAll` for external resources needing explicit release (embedded servers, file handles, network connections, locks)
- Assert dependency is not null before cleanup when required (e.g. Redis connection factory)
- Do not use `@Transactional` for automatic test rollback

## Test Data Persistence Helpers

- Always extract data creation in an external resource (DB, Redis, etc.) into a helper method

| Caller needs | Pattern | Example |
|---|---|---|
| Default data | `createXxx()` | `createUser()` |
| Control over the fields | `createXxx(entity)` | `createUser(user)` |
| A specific named state | `createStateXxx()` | `createExpiredAuthentication()` |

- When a helper requires a prerequisite, append `ForYyy(prereq)` to the method name (e.g. `createJwtAuthenticationForUser(user)`, `createExpiredJwtAuthenticationForUser(user)`)
- When a helper persists data into a specific store only, append `InStorage` to the method name (e.g. `createJwtAuthenticationInDB(jwtAuthentication)`, `createJwtAuthenticationInRedis(jwtAuthentication)`)

## Custom Assertion Helpers

- Extract repeated assertion logic into private helpers — don't inline complex multi-field assertions in test bodies
- Limit assertion helper parameters to 3 — if more expected values are needed, assert inline instead
- A helper asserting several stores delegates to the `InStorage` helpers where they exist, and asserts each store inline where they don't
- Exception: the no-arg `assertXxxNotExist()` always asserts inline — it checks the stores are empty, which the token-taking `InStorage` helpers can't express

| Caller needs | Pattern | Example |
|---|---|---|
| Compare an object against expected values | `assertXxx(actual, expected)` | `assertJwtAuthentication(actual, expectedUser)` |
| Check an HTTP response, values vary | `assertXxxResponse(actual, expected)` | `assertAPIResponse(accessToken, refreshToken, user)` |
| Check a fixed HTTP response | `assert<Case>Response(actual)` | `assertMissingTokenUnauthorizedResponse(json)` |
| Check presence or absence across stores | `assertXxxExist(...)` / `assertXxxNotExist(...)` | `assertAuthenticationExist(accessToken, refreshToken, user)` |

- When a helper asserts a specific store only, append `InStorage` to the method name (e.g. `assertAuthenticationExistInDB(...)`, `assertAuthenticationNotExistInRedis(...)`)

## MockServer for External Services

- Mock service-to-service calls only in `@SpringBootTest`
- `*IT`: embedded `mockserver-netty` (`ClientAndServer`)
- `*E2EIT`: Testcontainers `MockServerContainer`
- Wire the base URL with `@DynamicPropertySource` — neither tier supports `@ServiceConnection`
