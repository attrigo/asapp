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

package com.bcn.asapp.clients.client.task;

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_PROJECT_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_ROOT_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.bcn.asapp.clients.internal.uri.DefaultUriHandler;
import com.bcn.asapp.dto.task.TaskDTO;

class TaskRestClientTests {

    public static final String TASKS_GET_BY_PROJECT_ID_PATH_WITHOUT_ID = TASKS_ROOT_PATH + "/project/";

    private MockRestServiceServer mockServer;

    private TaskRestClient client;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(restClientBuilder)
                                               .ignoreExpectOrder(true)
                                               .build();
        this.client = new TaskRestClient(restClientBuilder, new DefaultUriHandler(""));

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Nested
    class GetTasksByProjectId {

        @Test
        @DisplayName("GIVEN tasks service is not available WHEN get task by project id THEN returns null")
        void TasksServiceIsNotAvailable_GetTasksByProjectId_ReturnsNull() {
            // Given
            var projectId = UUID.randomUUID();

            mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                      .andExpect(method(HttpMethod.GET))
                      .andRespond(withServerError().contentType(MediaType.APPLICATION_JSON));

            // When
            var actualTasks = client.getTasksByProjectId(projectId);

            // Then
            assertNull(actualTasks);

            mockServer.verify();
        }

        @Test
        @DisplayName("GIVEN id is not valid WHEN get task by project id THEN returns null")
        void IdIsNotValid_GetTasksByProjectId_ReturnsNull() throws JsonProcessingException {
            // Given
            var expectedProblemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to convert 'id' with value: 'project'");
            var responseBody = objectMapper.writeValueAsString(expectedProblemDetails);

            mockServer.expect(requestTo(TASKS_GET_BY_PROJECT_ID_PATH_WITHOUT_ID))
                      .andExpect(method(HttpMethod.GET))
                      .andRespond(withBadRequest().body(responseBody)
                                                  .contentType(MediaType.APPLICATION_PROBLEM_JSON));

            // When
            var actualTasks = client.getTasksByProjectId(null);

            // Then
            assertNull(actualTasks);

            mockServer.verify();
        }

        @Test
        @DisplayName("GIVEN id not exist WHEN get task by project id THEN returns an empty list of tasks")
        void IdNotExist_GetTasksByProjectId_ReturnsEmptyList() throws JsonProcessingException {
            // Given
            var expectedTasks = Collections.emptyList();
            var responseBody = objectMapper.writeValueAsString(expectedTasks);

            var projectId = UUID.randomUUID();

            mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                      .andExpect(method(HttpMethod.GET))
                      .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            // When
            var actualTasks = client.getTasksByProjectId(projectId);

            // Then
            assertNotNull(actualTasks);
            assertTrue(actualTasks.isEmpty());
            assertEquals(expectedTasks, actualTasks);

            mockServer.verify();
        }

        @Test
        @DisplayName("GIVEN id exists WHEN get task by project id THEN returns the tasks of the project")
        void IdExist_GetTasksByProjectId_ReturnsProjectTasks() throws JsonProcessingException {
            // Given
            var expectedTask1 = new TaskDTO(UUID.randomUUID(), "Test Title 1", "Test Description 1", Instant.now(), UUID.randomUUID());
            var expectedTask2 = new TaskDTO(UUID.randomUUID(), "Test Title 2", "Test Description 2", Instant.now(), UUID.randomUUID());
            var expectedTasks = List.of(expectedTask1, expectedTask2);
            var responseBody = objectMapper.writeValueAsString(expectedTasks);

            var projectId = UUID.randomUUID();

            mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                      .andExpect(method(HttpMethod.GET))
                      .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            // When
            var actualTasks = client.getTasksByProjectId(projectId);

            // Then
            assertNotNull(actualTasks);
            assertEquals(2L, actualTasks.size());
            assertEquals(expectedTasks, actualTasks);

            mockServer.verify();
        }

    }

}
