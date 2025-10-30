/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.clients.tasks;

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.clients.util.UriHandler;

/**
 * REST-based implementation of {@link TasksClient}.
 * <p>
 * This implementation communicates with the Tasks Service via HTTP REST calls.
 * <p>
 * It uses Spring's {@link RestClient} for making HTTP requests.
 * <p>
 * When communication with the Tasks Service fails, this implementation logs a warning and returns {@code null} to allow graceful degradation. This prevents
 * cascading failures when the Tasks Service is temporarily unavailable.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class TasksRestClient implements TasksClient {

    private static final Logger logger = LoggerFactory.getLogger(TasksRestClient.class);

    private final UriHandler tasksServiceUriHandler;

    private final RestClient taskClient;

    /**
     * Constructs a new {@code TasksServiceRestClient} with required dependencies.
     *
     * @param tasksServiceUriHandler the URI handler for building service endpoint URIs
     * @param taskClient             the configured REST client for making HTTP requests
     */
    public TasksRestClient(UriHandler tasksServiceUriHandler, RestClient taskClient) {
        this.tasksServiceUriHandler = tasksServiceUriHandler;
        this.taskClient = taskClient;
    }

    /**
     * Retrieves all task identifiers for a specific user by calling the Tasks Service REST API.
     * <p>
     * Makes a GET request and extracts the task IDs from the response.
     * <p>
     * If the request fails due to network issues, service unavailability, or any other {@link RestClientException}, this method logs a warning and returns
     * {@code null} to enable graceful degradation.
     *
     * @param userId the unique identifier of the user whose task IDs should be retrieved
     * @return a {@link List} of task UUIDs belonging to the user, an empty list if the user has no tasks or {@code null} if an error occurs
     * @throws IllegalArgumentException if userId is {@code null}
     */
    @Override
    public List<UUID> getTaskIdsByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        try {
            var uri = this.tasksServiceUriHandler.newInstance()
                                                 .path(TASKS_GET_BY_USER_ID_FULL_PATH)
                                                 .build(userId);

            List<TasksByUserIdResponse> tasks = this.taskClient.get()
                                                               .uri(uri)
                                                               .retrieve()
                                                               .body(new ParameterizedTypeReference<>() {});

            if (tasks == null) {
                return null;
            }

            return tasks.stream()
                        .map(TasksByUserIdResponse::taskId)
                        .toList();

        } catch (RestClientException e) {
            logger.warn("Failed to retrieve tasks for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

}
