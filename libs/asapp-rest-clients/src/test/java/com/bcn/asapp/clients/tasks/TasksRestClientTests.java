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

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.clients.util.DefaultUriHandler;

/**
 * Tests {@link TasksRestClient} HTTP delegation, error resilience, and JSON deserialization.
 * <p>
 * Coverage:
 * <li>Deserializes single task ID from JSON response array</li>
 * <li>Deserializes multiple task IDs from JSON response array</li>
 * <li>Returns empty collection when no tasks exist for user</li>
 * <li>Rejects null user identifiers with validation exception</li>
 * <li>Returns empty collection when response body is null</li>
 * <li>Returns empty collection when server errors occur (graceful degradation)</li>
 */
class TasksRestClientTests {

    private static final String BASE_URL = "http://localhost:8081/asapp-tasks-service";

    private MockRestServiceServer server;

    private TasksRestClient tasksRestClient;

    @BeforeEach
    void beforeEach() {
        var uriHandler = new DefaultUriHandler(BASE_URL);
        var restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder)
                                      .build();
        var restClient = restClientBuilder.build();
        tasksRestClient = new TasksRestClient(uriHandler, restClient);
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ReturnsSingleTaskId_UserHasOneTask() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var taskId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = userId.toString();
            var responseBody = """
                    [
                        {
                            "task_id": "%s"
                        }
                    ]
                    """.formatted(taskId);

            server.expect(requestToUriTemplate(uri, uriVariables))
                  .andExpect(method(GET))
                  .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(userId);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("result").isNotNull();
                softly.assertThat(actual).as("size").hasSize(1);
                softly.assertThat(actual).as("elements").containsExactly(taskId);
                // @formatter:on
            });

            server.verify();
        }

        @Test
        void ReturnsTaskIds_UserHasMultipleTasks() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var taskId1 = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var taskId2 = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");
            var taskId3 = UUID.fromString("660e8400-e29b-41d4-a716-446655440003");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = userId.toString();
            var responseBody = """
                    [
                        {
                            "task_id": "%s"
                        },
                        {
                            "task_id": "%s"
                        },
                        {
                            "task_id": "%s"
                        }
                    ]
                    """.formatted(taskId1, taskId2, taskId3);

            server.expect(requestToUriTemplate(uri, uriVariables))
                  .andExpect(method(GET))
                  .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(userId);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("result").isNotNull();
                softly.assertThat(actual).as("size").hasSize(3);
                softly.assertThat(actual).as("elements").containsExactly(taskId1, taskId2, taskId3);
                // @formatter:on
            });

            server.verify();
        }

        @Test
        void ReturnsEmptyList_TasksNotExistForUserId() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = userId.toString();
            var responseBody = "[]";

            server.expect(requestToUriTemplate(uri, uriVariables))
                  .andExpect(method(GET))
                  .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();

            server.verify();
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // When
            var actual = catchThrowable(() -> tasksRestClient.getTaskIdsByUserId(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ReturnsEmptyList_NullResponse() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = userId.toString();

            server.expect(requestToUriTemplate(uri, uriVariables))
                  .andExpect(method(GET))
                  .andRespond(withSuccess());

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();

            server.verify();
        }

        @Test
        void ReturnsEmptyList_ServerCallFails() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = userId.toString();

            server.expect(requestToUriTemplate(uri, uriVariables))
                  .andExpect(method(GET))
                  .andRespond(withServerError());

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();

            server.verify();
        }

    }

}
