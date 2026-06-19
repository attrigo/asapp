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

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.users.infrastructure.config.TasksHttpClientConfiguration.TASKS_CLIENT_NAME;
import static com.bcn.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link TasksGatewayAdapter} fallback behavior end-to-end through the real declarative HTTP client against an embedded MockServer.
 * <p>
 * Coverage:
 * <li>Degrades to an empty list when the Tasks Service responds with a server error (5xx)</li>
 * <li>Degrades to an empty list when the Tasks Service connection is dropped</li>
 * <li>Propagates client (4xx) errors without masking them as an empty list</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestContainerConfiguration.class)
class TasksGatewayAdapterIT {

    static final ClientAndServer embeddedMockServer = ClientAndServer.startClientAndServer(0);

    static final MockServerClient mockServerClient = embeddedMockServer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("resilience4j.retry.instances.tasks.max-attempts", () -> "3");
    }

    @Autowired
    private TasksGateway tasksGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void beforeEach() {
        mockServerClient.reset();
        circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME)
                              .reset();
        seedSecurityContext();
    }

    @AfterAll
    static void afterAll() {
        embeddedMockServer.stop();
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ReturnsTaskIds_TasksServiceRecoversAfterTransientErrors() {
            // Given
            var userId = aUser().getId();
            var taskId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));

            mockServerClient.when(request, Times.exactly(2))
                            .respond(response().withStatusCode(500));
            mockServerClient.when(request)
                            .respond(response().withStatusCode(200)
                                               .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                               .withBody("[{\"taskId\":\"" + taskId + "\"}]"));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).containsExactly(taskId);
            assertThat(circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME)
                                             .getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            mockServerClient.verify(request, exactly(3));
        }

        @Test
        void ReturnsEmptyList_TasksServiceReturnsServerError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmptyList_TasksServiceUnreachable() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());
            mockServerClient.when(request)
                            .error(error().withDropConnection(true));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void RecordsOneFailurePerCall_ServerErrorsPersist() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME)
                                             .getMetrics()
                                             .getNumberOfFailedCalls()).isEqualTo(1);

            mockServerClient.verify(request, exactly(3));
        }

        @Test
        void PropagatesError_TasksServiceReturnsClientError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));

            mockServerClient.when(request)
                            .respond(response().withStatusCode(400));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(HttpClientErrorException.class);

            mockServerClient.verify(request, exactly(1));
        }

    }

    private static void seedSecurityContext() {
        var decodedJwt = decodedAccessToken();
        var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
        SecurityContextHolder.getContext()
                             .setAuthentication(jwtAuthenticationToken);
    }

}
