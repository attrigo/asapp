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
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Duration;
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

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link TasksGatewayAdapter} resilience behavior end-to-end through the real declarative HTTP client against an embedded MockServer.
 * <p>
 * Coverage:
 * <li>Degrades to an empty list when the Tasks Service responds with a server error (5xx)</li>
 * <li>Degrades to an empty list when the Tasks Service connection is dropped</li>
 * <li>Propagates client (4xx) errors without masking them as an empty list</li>
 * <li>Short-circuits to an empty list once the failure-rate threshold is exceeded, issuing no further downstream calls</li>
 * <li>Recovers to returning task identifiers once the Tasks Service is healthy again</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestContainerConfiguration.class)
class TasksGatewayAdapterIT {

    private static final int MINIMUM_NUMBER_OF_CALLS = 5;

    static final ClientAndServer embeddedMockServer = ClientAndServer.startClientAndServer(0);

    static final MockServerClient mockServerClient = embeddedMockServer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state", () -> "1s");
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
        void ReturnsEmptyList_FailureRateThresholdExceeded() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            for (int i = 0; i < MINIMUM_NUMBER_OF_CALLS; i++) {
                tasksGateway.getTaskIdsByUserId(userId);
            }

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();

            mockServerClient.verify(request, exactly(MINIMUM_NUMBER_OF_CALLS));
        }

        @Test
        void ReturnsTaskIds_TasksServiceRecovers() {
            // Given
            var userId = aUser().getId();
            var taskId = UUID.fromString("a1b2c3d4-e5f6-4789-a012-b3c4d5e6f7a8");
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));
            var responseBody = "[{\"taskId\":\"%s\"}]".formatted(taskId);

            mockServerClient.when(request, Times.exactly(MINIMUM_NUMBER_OF_CALLS))
                            .respond(response().withStatusCode(500));
            for (int i = 0; i < MINIMUM_NUMBER_OF_CALLS; i++) {
                tasksGateway.getTaskIdsByUserId(userId);
            }

            mockServerClient.when(request)
                            .respond(response().withStatusCode(200)
                                               .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                               .withBody(responseBody));
            tasksGateway.getTaskIdsByUserId(userId);

            // When & Then
            // poll on the test thread so the seeded SecurityContext (a ThreadLocal) reaches the JWT interceptor
            await().pollInSameThread()
                   .atMost(Duration.ofSeconds(5))
                   .untilAsserted(() -> assertThat(tasksGateway.getTaskIdsByUserId(userId)).containsExactly(taskId));
        }

        @Test
        void PropagatesError_TasksServiceReturnsClientError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request)
                            .respond(response().withStatusCode(400));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(HttpClientErrorException.class);
        }

    }

    private static void seedSecurityContext() {
        var decodedJwt = decodedAccessToken();
        var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
        SecurityContextHolder.getContext()
                             .setAuthentication(jwtAuthenticationToken);
    }

}
