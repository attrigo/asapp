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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link TasksGatewayAdapter} circuit breaker behavior through the proxied bean.
 * <p>
 * Coverage:
 * <li>Opens the circuit and stops calling the Tasks Service once the failure rate threshold is exceeded</li>
 * <li>Degrades to an empty list while the circuit is open</li>
 * <li>Does not open the circuit on client (4xx) errors</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfiguration.class)
@Testcontainers
// @formatter:off
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.tasks.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.tasks.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.tasks.minimum-number-of-calls=5",
        "resilience4j.circuitbreaker.instances.tasks.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.tasks.wait-duration-in-open-state=10s",
        "resilience4j.circuitbreaker.instances.tasks.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.tasks.ignore-exceptions=org.springframework.web.client.HttpClientErrorException" })
// @formatter:on
class TasksGatewayAdapterIT {

    @MockitoBean
    private TasksHttpClient tasksHttpClient;

    @Autowired
    private TasksGateway tasksGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void beforeEach() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("tasks");
        circuitBreaker.reset();
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void OpensCircuitAndStopsCallingTasksService_FailureRateThresholdExceeded() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // When
            for (int i = 0; i < 5; i++) {
                assertThat(tasksGateway.getTaskIdsByUserId(userId)).isEmpty();
            }
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            then(tasksHttpClient).should(times(5))
                                 .getTasksByUserId(userId.value());
        }

        @Test
        void KeepsCircuitClosed_TasksServiceReturnsClientError() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(circuitBreaker.getMetrics()
                                     .getNumberOfFailedCalls()).isZero();
        }

    }

}
