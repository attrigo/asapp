# Declarative HTTP Clients Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hand-written `RestClient`-based `TasksRestClient` with a Spring Boot 4 declarative HTTP interface (`@HttpExchange`), moving the library to a pure contract and relocating wiring + mapping + graceful-degradation into `asapp-users-service`.

**Architecture:** `asapp-rest-clients` becomes a contract-only library exposing the `@HttpExchange` interface `TasksHttpClient` and the `TasksByUserIdResponse` DTO. `asapp-users-service` registers the client as an HTTP service *group* via `@ImportHttpServices`, configures the group's `RestClient` (redirect-disabled factory, `JwtInterceptor`, conditional Spring Cloud load-balancer interceptor) through a `RestClientHttpServiceGroupConfigurer`, and `TasksGatewayAdapter` maps the DTO list to `List<UUID>` while preserving the swallow-and-return-empty fallback.

**Tech Stack:** Java 25, Spring Boot 4.0.5, Spring Framework 7 (`@HttpExchange`, `@ImportHttpServices`, `RestClientHttpServiceGroupConfigurer`), Spring Cloud 2025.1 (LoadBalancer + Eureka), JUnit 5, AssertJ, Mockito, MockRestServiceServer, MockServer (Testcontainers).

**Reference spec:** `docs/superpowers/specs/2026-06-13-declarative-http-clients-design.md`

---

## Key facts established during research

- **API surface (verified against Spring docs):**
  - `@org.springframework.web.service.annotation.HttpExchange` / `@GetExchange` (in `spring-web`).
  - `@org.springframework.web.service.registry.ImportHttpServices` — repeatable, on `@Configuration` classes; attributes `group`, `types`.
  - Base-url property: `spring.http.serviceclient.<group>.base-url`.
  - `org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer` — functional bean; Boot auto-applies it. Lambda shape: `groups -> groups.forEachClient((group, clientBuilder) -> { ... })` where `clientBuilder` is a `RestClient.Builder`.
  - For the contract test: `org.springframework.web.client.support.RestClientAdapter` + `org.springframework.web.service.invoker.HttpServiceProxyFactory` (both in `spring-web`).
- **Load-balancing constraint:** the test profile (`services/asapp-users-service/src/test/resources/application.properties`) sets `eureka.client.enabled=false` and `spring.cloud.loadbalancer.enabled=false`. With LB disabled, no `LoadBalancerInterceptor` bean exists, so the group must call the base-url host directly (this is what `UserE2EIT` relies on — it points the base-url at a MockServer container). The plan therefore injects `ObjectProvider<LoadBalancerInterceptor>` and applies it only when present — reproducing today's `@LoadBalanced`-builder behavior exactly.
- **Standard file header:** every Java/`.java` file in this repo begins with the Apache 2.0 license header. The full header is shown once in Task 2 Step 1; **prepend that identical header to every new Java file in this plan.**

---

## Task 0: Spike — confirm Spring Boot 4 HTTP service client wiring

**Goal:** De-risk the three genuinely version-dependent points before writing production code. No production commit; record findings in the plan's execution notes (and update later steps if reality differs).

- [ ] **Step 1: Confirm the API classes are on the classpath**

Run:
```bash
cd C:/dev/repos/ttrigo/asapp
mvn -q -pl services/asapp-users-service dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
# Verify the key types resolve in the resolved Spring Framework 7 / Boot 4 jars:
unzip -l "$(grep -o '[^;]*spring-web-[0-9][^;]*\.jar' /tmp/cp.txt | head -1)" | grep -E "service/registry/ImportHttpServices|service/annotation/HttpExchange|client/support/RestClientHttpServiceGroupConfigurer|client/support/RestClientAdapter|service/invoker/HttpServiceProxyFactory"
```
Expected: all five class entries are listed. If any package path differs, note the corrected import and adjust the affected steps below.

- [ ] **Step 2: Confirm the `forEachClient` configurer signature**

Open the `RestClientHttpServiceGroupConfigurer` (and its supertype `HttpServiceGroupConfigurer`) Javadoc/sources for the resolved version and confirm the lambda used in Task 4 Step 3 compiles:
```java
groups -> groups.forEachClient((group, clientBuilder) -> { /* clientBuilder is RestClient.Builder */ })
```
Expected: matches. If the entry method is named differently (e.g. a `filterByName(...)` prefix is required), record the exact form — Task 4 Step 3 is the only place it is used.

- [ ] **Step 3: Decide the load-balancing path**

The plan implements **manual conditional LB** (apply `LoadBalancerInterceptor` from an `ObjectProvider` only when present). Spring Cloud 2025.1 advertises *transparent* LB for HTTP service groups; if a quick check shows the group already resolves the `asapp-tasks-service` service-id without the manual interceptor **and** still calls a raw host directly when `spring.cloud.loadbalancer.enabled=false`, you may drop the `ObjectProvider`/`ifAvailable` lines from Task 4 Step 3. Default to the manual path — it is known-correct. Record the decision.

### Spike findings (recorded 2026-06-13)

- **Step 1 ✅** Resolved `spring-web 7.0.6` (Spring Framework 7). All five class entries present with the exact package paths the plan imports: `web.service.registry.ImportHttpServices`, `web.service.annotation.HttpExchange`/`GetExchange`, `web.client.support.RestClientHttpServiceGroupConfigurer`, `web.client.support.RestClientAdapter`, `web.service.invoker.HttpServiceProxyFactory`. No import corrections needed.
- **Step 2 ✅** `RestClientHttpServiceGroupConfigurer extends HttpServiceGroupConfigurer<RestClient.Builder>`. `Groups.forEachClient(ClientCallback<CB>)` exists; `ClientCallback.withClient(HttpServiceGroup, CB)` is the 2-arg target. The sibling overload `forEachClient(InitializingClientCallback)` uses a 1-arg `initClient(HttpServiceGroup)`, so the plan's `(group, clientBuilder) -> {…}` lambda is unambiguous. Task 4 lambda compiles as-written.
- **Step 3 ✅** Manual conditional LB path chosen (default). No change to Task 4.
- **Sequencing fix (applies to Task 1 → Task 2):** Task 1 Step 4 as written runs `test` only, leaving the local `.m2` copy of `asapp-rest-clients` without `TasksHttpClient`; Task 2/3 run `-pl services/asapp-users-service` **without** `-am`, so they resolve the library from `.m2`. Task 1's verification must therefore `install` the library (runs the contract test *and* publishes the new interface) so Task 2 can compile. (Task 4 Step 4 already uses `-am`; Task 5 reinstalls; Task 6 rebuilds the full reactor.)
- **DTO import (applies to Task 1 Step 1):** `TasksByUserIdResponse` lives in `com.bcn.asapp.clients.tasks.response`; the contract test in package `com.bcn.asapp.clients.tasks` must add `import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;` (omitted from the plan snippet).

---

## Task 1: Library — add the declarative `TasksHttpClient` interface

**Files:**
- Create: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksHttpClient.java`
- Create (test): `libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/tasks/TasksHttpClientTests.java`

> Old classes (`TasksClient`, `TasksRestClient`, etc.) stay in place for now so every module keeps compiling; they are removed in Task 5 after the consumer no longer references them.

- [ ] **Step 1: Write the failing contract test**

Create `TasksHttpClientTests.java` (prepend the standard license header from Task 2 Step 1):

```java
package com.bcn.asapp.clients.tasks;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Tests {@link TasksHttpClient} HTTP request mapping and JSON response deserialization.
 * <p>
 * Coverage:
 * <li>Issues a GET to the tasks-by-user-id path with the user id expanded into the URI template</li>
 * <li>Deserializes the JSON response array into task response records</li>
 */
class TasksHttpClientTests {

    private static final String BASE_URL = "http://localhost:8081/asapp-tasks-service";

    private MockRestServiceServer server;

    private TasksHttpClient tasksHttpClient;

    @BeforeEach
    void beforeEach() {
        var restClientBuilder = RestClient.builder()
                                          .baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(restClientBuilder)
                                      .build();
        var adapter = RestClientAdapter.create(restClientBuilder.build());
        tasksHttpClient = HttpServiceProxyFactory.builderFor(adapter)
                                                 .build()
                                                 .createClient(TasksHttpClient.class);
    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ReturnsTaskResponses_UserHasTasks() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var taskId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var responseBody = """
                    [
                        {
                            "taskId": "%s"
                        }
                    ]
                    """.formatted(taskId);

            server.expect(requestToUriTemplate(uri, userId.toString()))
                  .andExpect(method(GET))
                  .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksHttpClient.getTasksByUserId(userId);

            // Then
            assertThat(actual).extracting(TasksByUserIdResponse::taskId)
                              .containsExactly(taskId);

            server.verify();
        }

    }

}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `mvn -q -pl libs/asapp-rest-clients test -Dtest=TasksHttpClientTests`
Expected: FAIL — compilation error, `TasksHttpClient` does not exist.

- [ ] **Step 3: Create the declarative interface**

Create `TasksHttpClient.java` (prepend the standard license header):

```java
package com.bcn.asapp.clients.tasks;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;

/**
 * Declarative HTTP client for the Tasks Service.
 * <p>
 * Defines the HTTP contract for retrieving task data from the Tasks Service as a Spring {@link HttpExchange} interface. A client proxy is created and configured
 * by the consuming service, which owns base-url resolution, authentication propagation, and load balancing.
 *
 * @since 0.4.0
 * @see HttpExchange
 * @author attrigo
 */
@HttpExchange
public interface TasksHttpClient {

    /**
     * Retrieves all tasks associated with a specific user.
     *
     * @param id the unique identifier of the user whose tasks should be retrieved
     * @return a {@link List} of task responses belonging to the user; an empty list if the user has no tasks
     */
    @GetExchange(TASKS_GET_BY_USER_ID_FULL_PATH)
    List<TasksByUserIdResponse> getTasksByUserId(@PathVariable("id") UUID id);

}
```

- [ ] **Step 4: Run the contract test to verify it passes**

Run: `mvn -q -pl libs/asapp-rest-clients test -Dtest=TasksHttpClientTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksHttpClient.java \
        libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/tasks/TasksHttpClientTests.java
git commit -m "feat(http-clients): add declarative TasksHttpClient interface"
```
(The pre-commit hook runs Spotless + commit-message validation; if Spotless reformats, `git add -u` and re-commit.)

---

## Task 2: Consumer — rewrite `TasksGatewayAdapter` to use the proxy (mapping + fallback)

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java`
- Create (test): `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterTests.java`

> The `TasksHttpClient` bean does not exist in the context until Task 4 registers the group. This task only changes the adapter + its **Mockito** unit test (no Spring context), so it compiles and the unit test passes on its own. Module-wide context tests (`UserE2EIT`, `RestClientConfigurationTests`) are green again after Task 4. Run only the targeted unit test in this task.

- [ ] **Step 1: Write the failing adapter unit test**

Create `TasksGatewayAdapterTests.java` with the standard license header (shown here in full once — reuse verbatim for all new Java files):

```java
/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.users.infrastructure.user.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Tests {@link TasksGatewayAdapter} task id mapping and graceful degradation.
 * <p>
 * Coverage:
 * <li>Maps task responses to their task ids</li>
 * <li>Returns an empty list when the user has no tasks</li>
 * <li>Returns an empty list when the client yields a null response body</li>
 * <li>Returns an empty list when the Tasks Service call fails</li>
 */
@ExtendWith(MockitoExtension.class)
class TasksGatewayAdapterTests {

    @Mock
    private TasksHttpClient tasksHttpClient;

    @InjectMocks
    private TasksGatewayAdapter tasksGatewayAdapter;

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ReturnsTaskIds_UserHasTasks() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var taskId1 = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var taskId2 = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(List.of(new TasksByUserIdResponse(taskId1), new TasksByUserIdResponse(taskId2)));

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).containsExactly(taskId1, taskId2);
        }

        @Test
        void ReturnsEmptyList_UserHasNoTasks() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(List.of());

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmptyList_NullResponse() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(null);

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmptyList_TasksServiceFails() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new RestClientException("connection refused"));

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

    }

}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=TasksGatewayAdapterTests`
Expected: FAIL — `TasksGatewayAdapter` still has a single-arg `TasksClient` constructor; `@InjectMocks` cannot inject a `TasksHttpClient`, and `getTaskIdsByUserId` delegates differently. (Compilation or assertion failure.)

- [ ] **Step 3: Rewrite the adapter**

Replace the body of `TasksGatewayAdapter.java` with:

```java
package com.bcn.asapp.users.infrastructure.user.out;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating to the declarative {@link TasksHttpClient}, mapping task responses to their
 * identifiers, and degrading gracefully when the Tasks Service is unavailable.
 * <p>
 * When communication with the Tasks Service fails (network errors, service unavailability, or invalid responses), this adapter logs a warning and returns an
 * empty list, preventing cascading failures so the user lookup still succeeds.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class TasksGatewayAdapter implements TasksGateway {

    private static final Logger logger = LoggerFactory.getLogger(TasksGatewayAdapter.class);

    private final TasksHttpClient tasksHttpClient;

    /**
     * Constructs a new {@code TasksGatewayAdapter} with required dependencies.
     *
     * @param tasksHttpClient the declarative HTTP client for communicating with the tasks-service
     */
    public TasksGatewayAdapter(TasksHttpClient tasksHttpClient) {
        this.tasksHttpClient = tasksHttpClient;
    }

    /**
     * Retrieves all task identifiers associated with a specific user by delegating to the tasks-service client.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, otherwise an empty list if the user has no tasks or the retrieval fails
     */
    @Override
    public List<UUID> getTaskIdsByUserId(UserId userId) {
        try {
            var tasks = tasksHttpClient.getTasksByUserId(userId.value());

            if (tasks == null) {
                logger.warn("Received null response body from Tasks Service for user {}. Returning empty list.", userId.value());
                return List.of();
            }

            return tasks.stream()
                        .map(TasksByUserIdResponse::taskId)
                        .toList();

        } catch (RestClientException e) {
            logger.warn("Failed to retrieve tasks for user {}: {}. Returning empty list.", userId.value(), e.getMessage());
            return List.of();
        }
    }

}
```

> Note: the previous null-`userId` `IllegalArgumentException` guard is intentionally dropped — `UserId` already guarantees a non-null value (`UserId` constructor validates non-null). The `UserE2EIT` log assertions (`"Failed to retrieve tasks for user " + id` and `"Returning empty list"`) still match the warning text above.

- [ ] **Step 4: Run the adapter unit test to verify it passes**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=TasksGatewayAdapterTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/out/TasksGatewayAdapterTests.java
git commit -m "refactor(http-clients): map and degrade in TasksGatewayAdapter via proxy"
```

---

## Task 3: Consumer — rename the base-url property in all environments

**Files:**
- Modify: `services/asapp-users-service/src/main/resources/application.properties:41`
- Modify: `services/asapp-users-service/src/main/resources/application-docker.properties:50`
- Modify: `services/asapp-users-service/src/test/resources/application.properties` (the `asapp.client.tasks.base-url` line)
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java:108`

> Renaming the property also deactivates the library's old `TasksClientConfiguration` (it is gated on `@ConditionalOnProperty(name = "asapp.client.tasks.base-url")`, which no longer resolves), preventing a duplicate/competing `TasksClient` bean before Task 5 deletes it.

- [ ] **Step 1: Rename in main properties**

In `application.properties`, change:
```properties
asapp.client.tasks.base-url=http://asapp-tasks-service/asapp-tasks-service
```
to:
```properties
spring.http.serviceclient.tasks.base-url=http://asapp-tasks-service/asapp-tasks-service
```

- [ ] **Step 2: Rename in docker properties**

In `application-docker.properties`, change:
```properties
asapp.client.tasks.base-url=${ASAPP_CLIENT_TASKS_BASE_URL}
```
to:
```properties
spring.http.serviceclient.tasks.base-url=${ASAPP_CLIENT_TASKS_BASE_URL}
```
(The `ASAPP_CLIENT_TASKS_BASE_URL` env var in `docker-compose.yaml` is unchanged.)

- [ ] **Step 3: Rename in test properties**

In `src/test/resources/application.properties`, change:
```properties
asapp.client.tasks.base-url=http://localhost:8081/asapp-tasks-service
```
to:
```properties
spring.http.serviceclient.tasks.base-url=http://localhost:8081/asapp-tasks-service
```

- [ ] **Step 4: Rename in the E2E `@DynamicPropertySource`**

In `UserE2EIT.java`, change line 108:
```java
registry.add("asapp.client.tasks.base-url", mockServerContainer::getEndpoint);
```
to:
```java
registry.add("spring.http.serviceclient.tasks.base-url", mockServerContainer::getEndpoint);
```

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service/src/main/resources/application.properties \
        services/asapp-users-service/src/main/resources/application-docker.properties \
        services/asapp-users-service/src/test/resources/application.properties \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/user/UserE2EIT.java
git commit -m "refactor(http-clients): rename tasks client base-url to serviceclient group property"
```

---

## Task 4: Consumer — register and configure the `tasks` HTTP service group

**Files:**
- Modify: `services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/RestClientConfiguration.java`
- Modify: `services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/RestClientConfigurationTests.java`

- [ ] **Step 1: Replace `RestClientConfiguration`**

Replace the whole class body with the following. It **adds** `@ImportHttpServices` + the group configurer, **removes** the `@LoadBalanced` builder and the standalone `restClient` bean, and **keeps** the `@Primary` plain builder and the Eureka factory supplier.

```java
package com.bcn.asapp.users.infrastructure.config;

import java.net.http.HttpClient;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configuration class for HTTP REST clients and declarative HTTP service groups.
 * <p>
 * Registers the {@code tasks} HTTP service group (the {@link TasksHttpClient} proxy) via {@link ImportHttpServices} and configures its underlying
 * {@link RestClient} through a {@link RestClientHttpServiceGroupConfigurer}: a redirect-disabled JDK request factory, a {@link JwtInterceptor} for bearer-token
 * propagation, and — when Spring Cloud LoadBalancer is enabled — the {@link LoadBalancerInterceptor} for Eureka-based service-name resolution.
 * <p>
 * A {@link Primary} plain {@link RestClient.Builder} is kept for Eureka and any unqualified injection point, and a
 * {@link DefaultEurekaClientHttpRequestFactorySupplier} gives Eureka its own isolated HTTP factory.
 *
 * @since 0.2.0
 * @see ImportHttpServices
 * @see RestClientHttpServiceGroupConfigurer
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "tasks", types = TasksHttpClient.class)
public class RestClientConfiguration {

    /**
     * Creates a plain {@link RestClient.Builder} bean used by Eureka and any unqualified injection point.
     *
     * @param configurer Boot's auto-configured {@link RestClientBuilderConfigurer}
     * @return a plain {@link RestClient.Builder} carrying Boot defaults only
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    /**
     * Configures the {@code tasks} HTTP service group's {@link RestClient}.
     * <p>
     * Applies a redirect-disabled JDK request factory and the {@link JwtInterceptor}. When a {@link LoadBalancerInterceptor} bean is present (Spring Cloud
     * LoadBalancer enabled), it is also applied so service-id hosts resolve through Eureka; when absent (e.g. tests with load balancing disabled), the
     * configured base-url host is called directly.
     *
     * @param loadBalancerInterceptor provider for the optional Spring Cloud load-balancer interceptor
     * @return the group configurer for RestClient-backed HTTP services
     */
    @Bean
    RestClientHttpServiceGroupConfigurer tasksHttpServiceGroupConfigurer(ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor) {
        var httpClient = HttpClient.newBuilder()
                                   .followRedirects(HttpClient.Redirect.NEVER)
                                   .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return groups -> groups.forEachClient((group, clientBuilder) -> {
            clientBuilder.requestFactory(requestFactory)
                         .requestInterceptor(new JwtInterceptor());
            loadBalancerInterceptor.ifAvailable(clientBuilder::requestInterceptor);
        });
    }

    /**
     * Provides Eureka with its own isolated {@link org.springframework.http.client.ClientHttpRequestFactory}, preventing it from picking up any application
     * {@link RestClient.Builder} beans.
     *
     * @return the default Eureka HTTP request factory supplier
     */
    @Bean
    DefaultEurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier() {
        return new DefaultEurekaClientHttpRequestFactorySupplier(new TimeoutProperties(), Set.of());
    }

}
```

> Spike confirmation point: if Task 0 Step 2 found a different `forEachClient` form, adjust only the `return groups -> ...` lambda. If Task 0 Step 3 chose transparent LB, delete the `ObjectProvider<LoadBalancerInterceptor>` parameter and the `loadBalancerInterceptor.ifAvailable(...)` line.

- [ ] **Step 2: Rewrite `RestClientConfigurationTests` as a context-slice test**

The old test autowired the now-removed `restClient` bean. Replace it with a slice that boots the `tasks` group against an in-process HTTP server and asserts the three guarantees: JWT propagation, redirect-not-followed, and load-balancer invocation when enabled. Replace the whole file with:

```java
/* standard license header */

package com.bcn.asapp.users.infrastructure.config;

import static com.bcn.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import com.sun.net.httpserver.HttpServer;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * Tests {@link RestClientConfiguration} HTTP service group configuration.
 * <p>
 * Coverage:
 * <li>Load balancer client is invoked for each outgoing request when load balancing is enabled</li>
 * <li>Redirect responses from downstream services are returned as-is without being followed</li>
 * <li>JWT from the authenticated security context is propagated as {@code Authorization: Bearer} header to downstream requests</li>
 */
@SpringBootTest(classes = RestClientConfigurationTests.TestApp.class)
@TestPropertySource(properties = { "eureka.client.enabled=false", "spring.cloud.config.enabled=false",
        "spring.http.serviceclient.tasks.base-url=http://asapp-tasks-service" })
class RestClientConfigurationTests {

    /**
     * Minimal Boot application importing {@link RestClientConfiguration} plus the Spring Cloud load-balancer beans needed to exercise the group configurer.
     */
    @org.springframework.boot.autoconfigure.SpringBootApplication
    @org.springframework.context.annotation.Import(RestClientConfiguration.class)
    static class TestApp {

        @Bean
        LoadBalancerClient loadBalancerClient() {
            return Mockito.mock(LoadBalancerClient.class);
        }

        @Bean
        LoadBalancerInterceptor loadBalancerInterceptor(LoadBalancerClient loadBalancerClient) {
            return new LoadBalancerInterceptor(loadBalancerClient);
        }

    }

    @Autowired
    private TasksHttpClient tasksHttpClient;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private HttpServer server;

    private int port;

    private String encodedToken;

    private final UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void beforeEach() throws IOException {
        var decodedJwt = decodedAccessToken();
        encodedToken = decodedJwt.encodedToken();
        SecurityContextHolder.getContext()
                             .setAuthentication(JwtAuthenticationToken.authenticated(decodedJwt));

        // Port 0 lets the OS assign an available ephemeral port, avoiding conflicts between test runs.
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress()
                     .getPort();

        Mockito.reset(loadBalancerClient);
        // Delegate each load-balanced call to the real HTTP execution against the in-process server.
        given(loadBalancerClient.execute(anyString(), any(LoadBalancerRequest.class))).willAnswer(inv -> {
            LoadBalancerRequest<?> request = inv.getArgument(1);
            return request.apply(new DefaultServiceInstance("", "", "localhost", port, false));
        });
        given(loadBalancerClient.reconstructURI(any(), any())).willAnswer(inv -> inv.getArgument(1, URI.class));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        server.stop(0);
    }

    @Test
    void InvokesLoadBalancerClient_OnRequest() {
        // Given
        server.createContext("/asapp-tasks-service/api/tasks/user/" + userId, exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        then(loadBalancerClient).should()
                                .execute(anyString(), any(LoadBalancerRequest.class));
    }

    @Test
    void PropagatesAuthorizationHeader_ValidSecurityContext() {
        // Given
        var authorizationHeader = new AtomicReference<String>();
        server.createContext("/asapp-tasks-service/api/tasks/user/" + userId, exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders()
                                            .getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        assertThat(authorizationHeader.get()).isEqualTo("Bearer " + encodedToken);
    }

    @Test
    void ReturnsResponseWithoutFollowingRedirect_ServerReturnsRedirect() {
        // Given
        var redirectCalled = new AtomicBoolean();
        server.createContext("/asapp-tasks-service/api/tasks/user/" + userId, exchange -> {
            exchange.getResponseHeaders()
                    .set("Location", "/asapp-tasks-service/redirected");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/asapp-tasks-service/redirected", exchange -> {
            redirectCalled.set(true);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        assertThat(redirectCalled.get()).isFalse();
    }

}
```

> Spike confirmation point: this slice depends on Boot auto-configuring the HTTP service client registry/proxy factory for `@ImportHttpServices` and applying the group configurer. Task 0 confirms a `@SpringBootApplication`-based slice is sufficient; if Boot needs an explicit auto-config import (e.g. an `@ImportAutoConfiguration` for the HTTP service client auto-configuration), add it to `TestApp` — that is the only adjustment this test should need. Keep the three behavior assertions intact.
>
> The load-balancer host (`asapp-tasks-service`) is resolved by the mocked `LoadBalancerClient` to `localhost:port`; the request path becomes `/asapp-tasks-service/api/tasks/user/{id}` because the base-url carries the `/asapp-tasks-service` context path and the exchange path is `/api/tasks/user/{id}`.

- [ ] **Step 3: Run the configuration tests**

Run: `mvn -q -pl services/asapp-users-service test -Dtest=RestClientConfigurationTests`
Expected: PASS (3 tests). If a missing auto-config surfaces (no `TasksHttpClient` bean), apply the spike-note adjustment and re-run.

- [ ] **Step 4: Build the whole users-service (unit tests only)**

Run: `mvn -q -pl services/asapp-users-service -am test`
Expected: PASS — full module compiles and all surefire (`*Tests`) tests pass. (Integration tests `*IT`/`*E2EIT` run under failsafe in Task 6.)

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/RestClientConfiguration.java \
        services/asapp-users-service/src/test/java/com/bcn/asapp/users/infrastructure/config/RestClientConfigurationTests.java
git commit -m "feat(http-clients): register and configure the tasks HTTP service group"
```

---

## Task 5: Library — delete the obsolete REST client machinery

**Files:**
- Delete: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksClient.java`
- Delete: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksRestClient.java`
- Delete: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksClientConfiguration.java`
- Delete: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/util/UriHandler.java`
- Delete: `libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/util/DefaultUriHandler.java`
- Delete: `libs/asapp-rest-clients/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Delete: `libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/tasks/TasksRestClientTests.java`
- Delete: `libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/util/DefaultUriHandlerTests.java`
- Modify: `libs/asapp-rest-clients/pom.xml`

- [ ] **Step 1: Delete the obsolete classes, tests, and the autoconfig import file**

```bash
cd C:/dev/repos/ttrigo/asapp
git rm libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksClient.java \
       libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksRestClient.java \
       libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/tasks/TasksClientConfiguration.java \
       libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/util/UriHandler.java \
       libs/asapp-rest-clients/src/main/java/com/bcn/asapp/clients/util/DefaultUriHandler.java \
       libs/asapp-rest-clients/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
       libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/tasks/TasksRestClientTests.java \
       libs/asapp-rest-clients/src/test/java/com/bcn/asapp/clients/util/DefaultUriHandlerTests.java
```
(The `util` package is now empty; `git rm` of its files removes them. Leave the directory — Git does not track empty dirs.)

- [ ] **Step 2: Slim the library `pom.xml`**

In `libs/asapp-rest-clients/pom.xml`, remove the now-unused compile dependencies — `org.springframework:spring-context`, `org.springframework.boot:spring-boot-autoconfigure`, and `org.slf4j:slf4j-api`. Keep `org.springframework:spring-web` (provides `@HttpExchange`/`@GetExchange`/`@PathVariable` and, for tests, `RestClientAdapter`/`HttpServiceProxyFactory`), `com.bcn.asapp:asapp-commons-url`, and `com.fasterxml.jackson.core:jackson-annotations`. Keep all test dependencies (`spring-boot-starter-test`, `jackson-databind`).

The resulting `<dependencies>` block:

```xml
    <dependencies>
        <!-- # Compile Dependencies-->
        <!-- ## ASAPP Dependencies -->
        <dependency>
            <groupId>com.bcn.asapp</groupId>
            <artifactId>asapp-commons-url</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- ## Spring Dependencies -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- ## Other Dependencies -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>

        <!-- # Test Dependencies -->
        <!-- ## Spring Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- ## Other Dependencies -->
        <dependency>
            <groupId>tools.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <scope>test</scope>
            <version>${jackson-databind.version}</version>
        </dependency>
    </dependencies>
```

- [ ] **Step 3: Build the library**

Run: `mvn -q -pl libs/asapp-rest-clients clean install`
Expected: BUILD SUCCESS — only `TasksHttpClient`, `TasksByUserIdResponse`, and `TasksHttpClientTests` remain; the contract test passes.

- [ ] **Step 4: Commit**

```bash
git add libs/asapp-rest-clients/pom.xml
git commit -m "refactor(http-clients)!: remove hand-written RestClient implementation"
```
(`git rm` already staged the deletions. The `!` marks the breaking change: the library no longer ships `TasksClient`, the URI utilities, the auto-configuration, or the `asapp.client.tasks.base-url` property.)

---

## Task 6: Verify the full build and integration tests

**Files:** none (verification only).

- [ ] **Step 1: Full reactor build with integration tests**

Run: `mvn clean verify`
Expected: BUILD SUCCESS across all modules. In particular `UserE2EIT` passes — including:
- `ReturnsStatusOKAndBodyWithFoundUserWithTasks_UserExistsWithTasks` (proxy maps task ids),
- `ReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsAndTasksServiceFails` (graceful degradation: 500 → empty list, warning logged),
- `ReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsWithoutTasks` (no warning on success).

> This step runs integration tests (Testcontainers + MockServer) and is slow. Per project convention, confirm with the developer before running.

- [ ] **Step 2: If the build is green, proceed to docs (Task 7).** If red, fix forward — the most likely failure is the HTTP service client slice context in Task 4 Step 2 (apply the spike note) or the group configurer lambda form (Task 0 Step 2).

---

## Task 7: Documentation updates

**Files:**
- Modify: `libs/asapp-rest-clients/README.md`
- Modify: `README.md` (root)
- Modify: `services/asapp-users-service/README.md`
- Modify: `CLAUDE.md` (root)

- [ ] **Step 1: Rewrite `libs/asapp-rest-clients/README.md`**

Apply these concrete edits:
- **Overview** (line ~13): replace "reusable REST client infrastructure and service-specific clients" with "declarative HTTP client contracts (Spring `@HttpExchange` interfaces) and response DTOs for inter-service communication".
- **Key Features** (lines ~18-21): replace the four bullets with:
  ```
  - ✅ Declarative `@HttpExchange` client interfaces
  - ✅ Type-safe response models
  - ✅ Wiring-free contracts — base URL, auth, and load balancing are owned by the consuming service
  ```
- **Usage step 2** (lines ~44-54): change the property to `spring.http.serviceclient.tasks.base-url` for both local and Docker, with values `http://localhost:8081/asapp-tasks-service` and `http://asapp-tasks-service/asapp-tasks-service` respectively.
- **Usage step 3** (lines ~56-73): replace the injection example with registration + injection of the declarative client:
  ```java
  @Configuration(proxyBeanMethods = false)
  @ImportHttpServices(group = "tasks", types = TasksHttpClient.class)
  public class HttpClientsConfiguration { }

  @Component
  public class MyAdapter {

      private final TasksHttpClient tasksHttpClient;

      public MyAdapter(TasksHttpClient tasksHttpClient) {
          this.tasksHttpClient = tasksHttpClient;
      }

      public List<TasksByUserIdResponse> myMethod(UUID userId) {
          return tasksHttpClient.getTasksByUserId(userId);
      }
  }
  ```
- **Reference → Clients** (line ~119): change to `com.bcn.asapp.clients.tasks.TasksHttpClient`.
- **External Resources** (line ~152): replace the Spring RestClient link with `- [Spring HTTP Interfaces](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)`.

- [ ] **Step 2: Edit root `README.md`**

Line ~303: change `│   └── asapp-rest-clients/                  # REST client infrastructure` to `│   └── asapp-rest-clients/                  # Declarative HTTP client contracts`.

- [ ] **Step 3: Edit `services/asapp-users-service/README.md`**

- Line ~204: change `- **REST Clients**: Spring RestClient` to `- **REST Clients**: Spring HTTP Interfaces (@HttpExchange)`.
- Line ~361: change `- \`asapp-rest-clients\` - Tasks service client` to `- \`asapp-rest-clients\` - Tasks service HTTP client contract`.

- [ ] **Step 4: Edit root `CLAUDE.md`**

Line 7: change ``- `asapp-rest-clients` (service-to-service HTTP)`` to ``- `asapp-rest-clients` (declarative HTTP client interfaces + DTOs)``.

- [ ] **Step 5: Commit**

```bash
git add libs/asapp-rest-clients/README.md README.md services/asapp-users-service/README.md CLAUDE.md
git commit -m "docs(http-clients): document declarative HTTP client contracts"
```

---

## Task 8: Prepare the `.claude/rules` update (developer applies)

**Files:**
- Modify: `.claude/rules/development-patterns.md` — **the developer must apply this; writes under `.claude/` are blocked by the auto-mode classifier.**

- [ ] **Step 1: Hand the developer the exact replacement for the "Service-to-Service Calls" section**

Current:
```markdown
## Service-to-Service Calls

- Use the injected `RestClient` bean directly — never construct a new `RestClient` or add an `Authorization` header manually
- `JwtInterceptor` automatically propagates the current request's JWT to all outgoing service calls
```
Replacement:
```markdown
## Service-to-Service Calls

- Call other services through an injected declarative `@HttpExchange` client proxy (e.g. `TasksHttpClient`) — never construct a `RestClient` or add an `Authorization` header manually
- Register client interfaces as an HTTP service group with `@ImportHttpServices`; configure the group's `RestClient` (base URL, auth, load balancing) in a `RestClientHttpServiceGroupConfigurer`
- `JwtInterceptor`, applied via the group configurer, automatically propagates the current request's JWT to all outgoing service calls
- Map responses and handle graceful degradation in the port adapter, not in the client interface
```

- [ ] **Step 2: Confirm with the developer it has been applied before closing the task.**

---

## Task 9: Mark the TODO item done

**Files:**
- Modify: `TODO.md:29`

- [ ] **Step 1: Tick the TODO entry**

Change line 29 from `        * [ ] Refactor REST clients by declarative HTTP clients` to `        * [X] Refactor REST clients by declarative HTTP clients`.

- [ ] **Step 2: Commit**

```bash
git add TODO.md
git commit -m "docs(http-clients): mark declarative HTTP clients task done"
```
> Stage only the line-29 change; leave any other unrelated working-tree edits to `TODO.md` untouched (use `git add -p TODO.md` if needed).

---

## Self-review

**Spec coverage:**
- §2.1 pure-contract library → Task 1 (add interface) + Task 5 (delete machinery + slim pom). ✓
- §2.2 fallback in adapter → Task 2 (`ReturnsEmptyList_*` tests + try/catch). ✓
- §2.3 names `TasksHttpClient` / group `tasks` → Tasks 1, 4. ✓
- §3.1 interface → Task 1 Step 3. ✓
- §3.2 deletions → Task 5 Step 1. ✓
- §3.3 pom slimming → Task 5 Step 2. ✓
- §3.4 registration + group config + shrink `RestClientConfiguration` → Task 4 Step 1. ✓
- §3.5 adapter mapping + dropped null guard → Task 2 Step 3. ✓
- §4 property migration (3 prop files + E2E) → Task 3. ✓
- §5 LB/JWT wiring + spike → Task 0 + Task 4 (conditional `ObjectProvider`). ✓
- §6 testing (contract test, adapter test, config test, full verify) → Tasks 1, 2, 4, 6. ✓
- §7 docs + `.claude` rule → Tasks 7, 8. ✓
- §8 sequencing → task order. ✓ §10 acceptance → Task 6 + Task 9. ✓

**Placeholder scan:** no TBD/“handle errors”/“write tests for the above”; every code step shows complete code. The license header is shown in full once (Task 2 Step 1) and explicitly reused — not a placeholder. ✓

**Type consistency:** `TasksHttpClient.getTasksByUserId(UUID)` returns `List<TasksByUserIdResponse>` — used identically in Task 1 (test + interface), Task 2 (adapter + test), Task 4 (config + test). `TasksByUserIdResponse(taskId)` single-component record matches existing source. `UserId.of(UUID)` / `userId.value()` match the domain VO. Property key `spring.http.serviceclient.tasks.base-url` identical across Tasks 3, 4, 7. ✓

**Known residual risk (spike-gated):** the exact `forEachClient` lambda form (Task 4 Step 1) and the minimal test context for the HTTP service group (Task 4 Step 2) are confirmed in Task 0; both have a single, localized adjustment point called out inline.
