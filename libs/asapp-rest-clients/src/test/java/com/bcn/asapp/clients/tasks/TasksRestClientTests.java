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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.clients.util.DefaultUriHandler;

class TasksRestClientTests {

    private static final String BASE_URL = "http://localhost:8081/asapp-tasks-service";

    private static final UUID USER_ID = UUID.randomUUID();

    private static final UUID TASK_ID_1 = UUID.randomUUID();

    private static final UUID TASK_ID_2 = UUID.randomUUID();

    private static final UUID TASK_ID_3 = UUID.randomUUID();

    private MockRestServiceServer mockServer;

    private TasksRestClient tasksRestClient;

    @BeforeEach
    void beforeEach() {
        var uriHandler = new DefaultUriHandler(BASE_URL);
        var restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder)
                                          .build();

        var restClient = restClientBuilder.build();

        tasksRestClient = new TasksRestClient(uriHandler, restClient);
    }

    @AfterEach
    void afterEach() {
        mockServer.verify();
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ThenReturnsEmptyList_GivenServerError() {
            // Given
            var expectedUri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = USER_ID.toString();

            mockServer.expect(requestToUriTemplate(expectedUri, uriVariables))
                      .andExpect(method(GET))
                      .andRespond(withServerError());

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(USER_ID);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNull() {
            // When
            var thrown = catchThrowable(() -> tasksRestClient.getTaskIdsByUserId(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenReturnsEmptyList_GivenResponseIsNull() {
            // Given
            var expectedUri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = USER_ID.toString();

            mockServer.expect(requestToUriTemplate(expectedUri, uriVariables))
                      .andExpect(method(GET))
                      .andRespond(withSuccess());

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(USER_ID);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();
        }

        @Test
        void ThenReturnsEmptyList_GivenUserHasNoTasks() {
            // Given
            var expectedUri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = USER_ID.toString();

            var responseBody = "[]";

            mockServer.expect(requestToUriTemplate(expectedUri, uriVariables))
                      .andExpect(method(GET))
                      .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(USER_ID);

            // Then
            assertThat(actual).isNotNull()
                              .isEmpty();
        }

        @Test
        void ThenReturnsSingleTaskId_GivenUserHasOneTask() {
            // Given
            var expectedUri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = USER_ID.toString();

            var responseBody = """
                    [
                        {
                            "task_id": "%s"
                        }
                    ]
                    """.formatted(TASK_ID_1);

            mockServer.expect(requestToUriTemplate(expectedUri, uriVariables))
                      .andExpect(method(GET))
                      .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(USER_ID);

            // Then
            assertThat(actual).isNotNull()
                              .hasSize(1)
                              .containsExactly(TASK_ID_1);
        }

        @Test
        void ThenReturnsListOfTaskIds_GivenUserHasMultipleTasks() {
            // Given
            var expectedUri = BASE_URL + TASKS_GET_BY_USER_ID_FULL_PATH;
            var uriVariables = USER_ID.toString();

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
                    """.formatted(TASK_ID_1, TASK_ID_2, TASK_ID_3);

            mockServer.expect(requestToUriTemplate(expectedUri, uriVariables))
                      .andExpect(method(GET))
                      .andRespond(withSuccess(responseBody, APPLICATION_JSON));

            // When
            var actual = tasksRestClient.getTaskIdsByUserId(USER_ID);

            // Then
            assertThat(actual).isNotNull()
                              .hasSize(3)
                              .containsExactly(TASK_ID_1, TASK_ID_2, TASK_ID_3);
        }

    }

}
