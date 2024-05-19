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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.bcn.asapp.clients.internal.uri.DefaultUriHandler;
import com.bcn.asapp.dto.task.TaskDTO;

class TaskRestClientTests {

    private MockRestServiceServer mockServer;

    private TaskRestClient client;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(restClientBuilder)
                                               .ignoreExpectOrder(true)
                                               .build();
        this.client = new TaskRestClient(restClientBuilder, new DefaultUriHandler(""));

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // getTasksByProjectId
    @Test
    @DisplayName("GIVEN tasks service is down WHEN get task by project id THEN returns status INTERNAL SERVER ERROR")
    void TasksServiceIsDown_GetTasksByProjectId_ReturnsInternalServerError() {
        // Given
        var projectId = UUID.randomUUID();

        mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withServerError().contentType(MediaType.APPLICATION_JSON));

        // When & Then
        try {
            client.getTasksByProjectId(projectId);

            fail("HttpServerErrorException.InternalServerError should be thrown");
        } catch (HttpServerErrorException.InternalServerError e) {
            assertNotNull(e.getMessage());
        }

        mockServer.verify();
    }

    @Test
    @DisplayName("GIVEN id is wrong WHEN get task by project id THEN returns status BAD_REQUEST And the body with problem details")
    void IdIsWrong_GetTasksByProjectId_ReturnsBadRequestAndBodyWithProblemDetails() throws JsonProcessingException {
        // Given
        var expectedProblemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to convert 'id' with value: 'project'");
        var responseBody = objectMapper.writeValueAsString(expectedProblemDetails);

        mockServer.expect(requestTo("/v1/tasks/project/"))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withBadRequest().body(responseBody)
                                              .contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // When & Then
        try {
            client.getTasksByProjectId(null);

            fail("HttpClientErrorException.BadRequest should be thrown");
        } catch (HttpClientErrorException.BadRequest e) {
            var actual = e.getResponseBodyAs(ProblemDetail.class);
            assertEquals(expectedProblemDetails, actual);
        }

        mockServer.verify();
    }

    @Test
    @DisplayName("GIVEN id not exist WHEN get task by project id THEN returns status OK And an empty body")
    void IdNotExist_GetTasksByProjectId_ReturnsStatusOkAndEmptyBody() throws JsonProcessingException {
        // Given
        var expectedTasks = Collections.emptyList();
        var responseBody = objectMapper.writeValueAsString(expectedTasks);

        var projectId = UUID.randomUUID();

        mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // When
        var actual = client.getTasksByProjectId(projectId);

        // Then
        assertNotNull(actual);
        assertTrue(actual.isEmpty());
        assertEquals(expectedTasks, actual);

        mockServer.verify();
    }

    @Test
    @DisplayName("GIVEN id exists WHEN get task by project id THEN returns status OK And the body with task of the given project")
    void IdExist_GetTasksByProjectId_ReturnsStatusOkAndBodyWithProjectsTasks() throws JsonProcessingException {
        // Given
        var expectedTask1 = new TaskDTO(UUID.randomUUID(), "Test Title 1", "Test Description 1", LocalDateTime.now(), UUID.randomUUID());
        var expectedTask2 = new TaskDTO(UUID.randomUUID(), "Test Title 2", "Test Description 2", LocalDateTime.now(), UUID.randomUUID());
        var expectedTasks = List.of(expectedTask1, expectedTask2);
        var responseBody = objectMapper.writeValueAsString(expectedTasks);

        var projectId = UUID.randomUUID();

        mockServer.expect(requestToUriTemplate(TASKS_GET_BY_PROJECT_ID_FULL_PATH, projectId))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // When
        var actual = client.getTasksByProjectId(projectId);

        // Then
        assertNotNull(actual);
        assertEquals(2L, actual.size());
        assertEquals(expectedTasks, actual);

        mockServer.verify();
    }

}
