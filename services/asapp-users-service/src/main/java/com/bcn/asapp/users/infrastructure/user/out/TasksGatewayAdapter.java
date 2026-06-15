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

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating to the declarative {@link TasksHttpClient} and mapping task responses to their
 * identifiers.
 * <p>
 * The call is guarded by a Resilience4j circuit breaker (instance {@code tasks}): repeated I/O or server errors open the circuit and fast-fail, and the breaker
 * recovers automatically once the Tasks Service is healthy again. On a server (5xx) error, an I/O failure, or while the circuit is open, the
 * {@code emptyTasksFallback} method logs a warning and returns an empty list, preventing cascading failures so the user lookup still succeeds. Client (4xx)
 * errors and unexpected failures do not open the circuit and propagate to the caller instead of being masked as an empty list.
 *
 * @since 0.2.0
 * @see CircuitBreaker
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
     * When the downstream service is unavailable (5xx or I/O error) or the circuit is open, the {@code tasks} circuit breaker fallback returns an empty list
     * instead; client (4xx) errors and unexpected failures propagate to the caller.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, or an empty list if the user has no tasks, the response body is null, or the downstream
     *         service is unavailable (5xx/I/O) or its circuit is open
     */
    @Override
    @CircuitBreaker(name = "tasks", fallbackMethod = "emptyTasksFallback")
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
     * Returns an empty task id list when the Tasks Service is unavailable or the circuit is open.
     * <p>
     * Invoked reflectively by Resilience4j as the {@code tasks} circuit breaker fallback. Only server (5xx) errors ({@link HttpServerErrorException}), I/O
     * failures ({@link ResourceAccessException}) and the open-circuit {@link CallNotPermittedException} are degraded to an empty list; client (4xx) errors and
     * any unexpected failure are rethrown so callers and the error handler can surface them.
     *
     * @param userId the user's unique identifier
     * @param cause  the failure that triggered the fallback
     * @return an empty {@link List} when the downstream service is unavailable or the circuit is open
     * @throws Throwable the original failure when it is not a recoverable downstream outage (e.g. a 4xx client error or a bug)
     */
    private List<UUID> emptyTasksFallback(UserId userId, Throwable cause) throws Throwable {
        if (cause instanceof HttpServerErrorException || cause instanceof ResourceAccessException || cause instanceof CallNotPermittedException) {
            var className = cause.getClass()
                                 .getSimpleName();
            var message = cause.getMessage();
            logger.warn("Failed to retrieve tasks for user {}: {} - {}. Returning empty list.", userId.value(), className, message);
            return List.of();
        }

        throw cause;
    }

}
