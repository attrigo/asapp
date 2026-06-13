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

import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;

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

        @Test
        void ReturnsEmptyList_UserHasNoTasks() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var uri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var responseBody = "[]";

            server.expect(requestToUriTemplate(uri, userId.toString()))
                  .andExpect(method(GET))
                  .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksHttpClient.getTasksByUserId(userId);

            // Then
            assertThat(actual).isEmpty();

            server.verify();
        }

    }

}
