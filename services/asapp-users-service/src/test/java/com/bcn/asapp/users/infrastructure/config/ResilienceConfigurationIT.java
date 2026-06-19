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

package com.bcn.asapp.users.infrastructure.config;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.users.infrastructure.config.TasksHttpClientConfiguration.TASKS_CLIENT_NAME;
import static com.bcn.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
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
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests the tasks circuit breaker and retry wiring driven by the configured resilience properties, exercised end-to-end through the real declarative HTTP
 * client against an embedded MockServer.
 * <p>
 * Coverage:
 * <li>Keeps the circuit closed while failures stay below the minimum number of calls</li>
 * <li>Opens the circuit once server errors exceed the failure-rate threshold, issuing no further downstream calls</li>
 * <li>Keeps the circuit closed when client (4xx) errors occur, as the breaker ignores them and does not retry them</li>
 * <li>Closes the circuit once the downstream service is healthy again</li>
 * <li>Retries transient server errors (5xx) until the Tasks Service recovers</li>
 * <li>Records a single circuit breaker failure when a call exhausts all retries</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestContainerConfiguration.class)
class ResilienceConfigurationIT {

    private static final int MINIMUM_NUMBER_OF_CALLS = 5;

    private static final int MAX_ATTEMPTS = 3;

    static final ClientAndServer embeddedMockServer = ClientAndServer.startClientAndServer(0);

    static final MockServerClient mockServerClient = embeddedMockServer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state", () -> "1s");
        registry.add("resilience4j.retry.instances.tasks.max-attempts", () -> String.valueOf(MAX_ATTEMPTS));
    }

    @Autowired
    private TasksGateway tasksGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void beforeEach() {
        mockServerClient.reset();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(TASKS_CLIENT_NAME);
        circuitBreaker.reset();
        seedSecurityContext();
    }

    @AfterAll
    static void afterAll() {
        embeddedMockServer.stop();
    }

    @Test
    void KeepsCircuitClosed_FailuresBelowMinimumCalls() {
        // Given
        var userId = aUser().getId();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                              .toString()));

        mockServerClient.when(request)
                        .respond(response().withStatusCode(500));

        // When
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS - 1)
                 .forEach(_ -> tasksGateway.getTaskIdsByUserId(userId));

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        mockServerClient.verify(request, exactly((MINIMUM_NUMBER_OF_CALLS - 1) * MAX_ATTEMPTS));
    }

    @Test
    void OpensCircuit_ServerErrorsExceedFailureThreshold() {
        // Given
        var userId = aUser().getId();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                              .toString()));

        openCircuit(userId, request);

        // When
        var actual = tasksGateway.getTaskIdsByUserId(userId);

        // Then
        assertThat(actual).isEmpty();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        mockServerClient.verify(request, exactly(MINIMUM_NUMBER_OF_CALLS * MAX_ATTEMPTS));
    }

    @Test
    void KeepsCircuitClosed_ClientErrorsExceedFailureThreshold() {
        // Given
        var userId = aUser().getId();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                              .toString()));

        mockServerClient.when(request)
                        .respond(response().withStatusCode(400));

        // When
        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS)
                 .forEach(_ -> assertThat(catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId))).isInstanceOf(HttpClientErrorException.class));

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        mockServerClient.verify(request, exactly(MINIMUM_NUMBER_OF_CALLS));
    }

    @Test
    void ClosesCircuit_DownstreamServiceRecovers() {
        // Given
        var userId = aUser().getId();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                              .toString()));

        openCircuit(userId, request);

        mockServerClient.when(request)
                        .respond(response().withStatusCode(200)
                                           .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                           .withBody("[]"));

        // When & Then
        // half-opens after the wait-duration, no call needed
        await().atMost(Duration.ofSeconds(3))
               .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);
        // trial successes close it; poll in-thread for the ThreadLocal SecurityContext
        await().pollInSameThread()
               .atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   tasksGateway.getTaskIdsByUserId(userId);
                   assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
               });
    }

    @Test
    void RetriesCall_TransientDownstreamServerErrors() {
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

        mockServerClient.verify(request, exactly(3));
    }

    @Test
    void RetriesInsideCircuitBreaker_ServerErrorsPersist() {
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
        assertThat(circuitBreaker.getMetrics()
                                 .getNumberOfFailedCalls()).isEqualTo(1);

        mockServerClient.verify(request, exactly(3));
    }

    private void openCircuit(UserId userId, HttpRequest request) {
        mockServerClient.when(request, Times.exactly(MINIMUM_NUMBER_OF_CALLS * MAX_ATTEMPTS))
                        .respond(response().withStatusCode(500));

        IntStream.range(0, MINIMUM_NUMBER_OF_CALLS)
                 .forEach(_ -> tasksGateway.getTaskIdsByUserId(userId));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private static void seedSecurityContext() {
        var decodedJwt = decodedAccessToken();
        var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
        SecurityContextHolder.getContext()
                             .setAuthentication(jwtAuthenticationToken);
    }

}
