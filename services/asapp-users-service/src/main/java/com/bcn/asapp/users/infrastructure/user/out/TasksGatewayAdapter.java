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

import static com.bcn.asapp.users.infrastructure.config.TasksHttpClientConfiguration.TASKS_CLIENT_NAME;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.users.application.user.TasksUnavailableException;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating to the declarative {@link TasksHttpClient} and mapping task responses to their
 * identifiers.
 * <p>
 * Outbound calls are guarded by Resilience4j (circuit breaker + retry).
 *
 * @since 0.2.0
 * @see CircuitBreaker
 * @see Retry
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
     * <p>
     * The call is guarded by a Resilience4j circuit breaker and retry mechanism:
     * <ul>
     * <li>Circuit breaker: repeated failures open the circuit and fast-fail (a call that exhausts its retries counts as a single failure) recovering
     * automatically once the Tasks Service is healthy again.</li>
     * <li>Retry: transient server (5xx) and I/O failures are retried with exponential backoff so a momentary blip recovers transparently; client (4xx) errors
     * are not retried.</li>
     * <li>Degradation: outages (5xx, I/O failures, and open-circuit) are translated into a {@link TasksUnavailableException} so the caller can decide how to
     * degrade, while client and unexpected errors propagate — see {@code tasksUnavailableFallback} for the exact classification.</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, or an empty list if the user has no tasks or the response body is null
     */
    @Override
    @CircuitBreaker(name = TASKS_CLIENT_NAME, fallbackMethod = "tasksUnavailableFallback")
    @Retry(name = TASKS_CLIENT_NAME)
    public List<UUID> getTaskIdsByUserId(UserId userId) {
        var tasks = tasksHttpClient.getTasksByUserId(userId.value());

        if (tasks == null) {
            logger.warn("Received null response body from Tasks Service for user {}. Returning empty list.", userId.value());
            return List.of();
        }

        return tasks.stream()
                    .map(TasksByUserIdResponse::taskId)
                    .toList();
    }

    /**
     * Translates a tasks-service outage into a {@link TasksUnavailableException}, or rethrows non-outage failures.
     * <p>
     * Invoked reflectively by Resilience4j as the {@code tasks} circuit breaker fallback:
     * <ul>
     * <li>Server (5xx) errors ({@link HttpServerErrorException}), I/O failures ({@link ResourceAccessException}), and the open-circuit
     * {@link CallNotPermittedException} are translated into a {@link TasksUnavailableException} so the application service can degrade gracefully.</li>
     * <li>Client (4xx) errors and any unexpected failure are rethrown so callers and the error handler can surface them.</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @param cause  the failure that triggered the fallback
     * @return never returns normally for an outage
     * @throws TasksUnavailableException when the downstream service is unavailable or the circuit is open
     * @throws Throwable                 the original failure when it is not a recoverable downstream outage (e.g. a 4xx client error or a bug)
     */
    private List<UUID> tasksUnavailableFallback(UserId userId, Throwable cause) throws Throwable {
        if (cause instanceof HttpServerErrorException || cause instanceof ResourceAccessException || cause instanceof CallNotPermittedException) {
            var className = cause.getClass()
                                 .getSimpleName();
            var message = cause.getMessage();
            logger.warn("Tasks Service unavailable for user {}: {} - {}.", userId.value(), className, message);
            throw new TasksUnavailableException("Tasks Service is unavailable", cause);
        }

        throw cause;
    }

}
