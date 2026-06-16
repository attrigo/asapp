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

import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link TasksGatewayAdapter} circuit breaker behavior through the proxied bean.
 * <p>
 * Coverage:
 * <li>Degrades to an empty list on a server or I/O error</li>
 * <li>Opens the circuit and degrades to an empty list once the failure rate threshold is exceeded</li>
 * <li>Short-circuits to an empty list while the circuit is open</li>
 * <li>Recovers and closes the circuit after calls succeed in the half-open state</li>
 * <li>Propagates client (4xx) errors without opening the circuit</li>
 * <li>Propagates unexpected errors instead of masking them as an empty list</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfiguration.class)
@Testcontainers
class TasksGatewayAdapterIT {

    private static final int MINIMUM_NUMBER_OF_CALLS = 5;

    private static final int PERMITTED_CALLS_IN_HALF_OPEN_STATE = 3;

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
        void ReturnsEmpty_TasksServiceNotResponding() {
            // Given
            var userId = aUser().getId();

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new ResourceAccessException("Connection refused"));

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        void OpensCircuit_FailureRateThresholdExceeded() {
            // Given
            var userId = aUser().getId();

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // When
            for (int i = 0; i < MINIMUM_NUMBER_OF_CALLS; i++) {
                tasksGateway.getTaskIdsByUserId(userId);
            }
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            then(tasksHttpClient).should(times(MINIMUM_NUMBER_OF_CALLS))
                                 .getTasksByUserId(userId.value());
        }

        @Test
        void ReturnsEmpty_CircuitOpen() {
            // Given
            var userId = aUser().getId();

            circuitBreaker.transitionToOpenState();

            // When
            var actual = tasksGateway.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
            then(tasksHttpClient).shouldHaveNoInteractions();
        }

        @Test
        void ClosesCircuit_CircuitHalfOpenAndCallsSucceed() {
            // Given
            var userId = aUser().getId();

            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(List.of());

            circuitBreaker.transitionToOpenState();
            circuitBreaker.transitionToHalfOpenState();

            // When
            for (int i = 0; i < PERMITTED_CALLS_IN_HALF_OPEN_STATE; i++) {
                tasksGateway.getTaskIdsByUserId(userId);
            }

            // Then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        void PropagatesClientError_TasksServiceReturnsClientError() {
            // Given
            var userId = aUser().getId();
            var ignoredErrors = new AtomicInteger();

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
            circuitBreaker.getEventPublisher()
                          .onIgnoredError(_ -> ignoredErrors.incrementAndGet());

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(HttpClientErrorException.class);
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(ignoredErrors).hasValue(1);
        }

        @Test
        void PropagatesError_TasksServiceFailsUnexpectedly() {
            // Given
            var userId = aUser().getId();

            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new RuntimeException("Unexpected error"));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessage("Unexpected error");
        }

    }

}
