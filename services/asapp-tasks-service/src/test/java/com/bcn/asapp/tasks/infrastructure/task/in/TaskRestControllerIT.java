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

package com.bcn.asapp.tasks.infrastructure.task.in;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_ROOT_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.tasks.testutil.WebMvcTestContext;

/**
 * Tests {@link TaskRestController} request validation and error responses.
 * <p>
 * Coverage:
 * <li>Validates path parameter format (UUID required for task and user IDs)</li>
 * <li>Validates request content type (JSON required for POST/PUT operations)</li>
 * <li>Validates request body presence and structure</li>
 * <li>Validates mandatory field constraints (user ID, title)</li>
 * <li>Returns RFC 7807 Problem Details for all validation failures</li>
 * <li>Tests all HTTP endpoints (GET by ID, GET by user, POST, PUT, DELETE)</li>
 */
@WithMockUser
class TaskRestControllerIT extends WebMvcTestContext {

    @Nested
    class GetTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingTaskId() {
            // Given
            var requestBuilder = get(TASKS_ROOT_PATH + "/");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.NOT_FOUND)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Not Found")
                                                                .containsEntry("status", 404)
                                                                .containsEntry("detail", "No static resource api/tasks.")
                                                                .containsEntry("instance", "/api/tasks/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidTaskId() {
            // Given
            var taskId = 1L;
            var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, taskId);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                                .containsEntry("instance", "/api/tasks/1"));
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingUserId() {
            // Given
            var requestBuilder = get(TASKS_ROOT_PATH + "/user/");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.NOT_FOUND)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Not Found")
                                                                .containsEntry("status", 404)
                                                                .containsEntry("detail", "No static resource api/tasks/user.")
                                                                .containsEntry("instance", "/api/tasks/user/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId() {
            // Given
            var userId = 1L;
            var requestBuilder = get(TASKS_GET_BY_USER_ID_FULL_PATH, userId);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                                .containsEntry("instance", "/api/tasks/user/1"));
        }

    }

    @Nested
    class CreateTask {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                             .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Unsupported Media Type")
                                                                .containsEntry("status", 415)
                                                                .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                                .containsEntry("instance", "/api/tasks"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to read request")
                                                                .containsEntry("instance", "/api/tasks"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("instance", "/api/tasks");
                             assertThatJson(json).inPath("detail")
                                                 .asString()
                                                 .contains("The user ID must not be empty")
                                                 .contains("The title must not be empty");
                         //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createTaskRequest", "field", "userId", "message", "The user ID must not be empty"),
                                                  Map.of("entity", "createTaskRequest", "field", "title", "message", "The title must not be empty")
                                          );
                       //@formatter:on
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var requestBody = """
                    {
                    "user_id": "",
                    "title": ""
                    }
                    """;
            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("instance", "/api/tasks");
                             assertThatJson(json).inPath("detail")
                                                 .asString()
                                                 .contains("The user ID must not be empty")
                                                 .contains("The title must not be empty");
                         //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createTaskRequest", "field", "userId", "message", "The user ID must not be empty"),
                                                  Map.of("entity", "createTaskRequest", "field", "title", "message", "The title must not be empty")
                                          );
                       //@formatter:on
                         });
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingTaskId() {
            // Given
            var newUserId = UUID.fromString("c2f7d9e3-5a8b-46cf-9d4e-8a2f7b3c5e1d");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var requestBody = """
                    {
                    "user_id": "%s",
                    "title": "%s",
                    "description": "%s",
                    "start_date": "%s",
                    "end_date": "%s"
                    }
                    """.formatted(newUserId, "New Title", "New Description", newStartDate, newEndDate);
            var requestBuilder = put(TASKS_ROOT_PATH + "/").contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.NOT_FOUND)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Not Found")
                                                                .containsEntry("status", 404)
                                                                .containsEntry("detail", "No static resource api/tasks.")
                                                                .containsEntry("instance", "/api/tasks/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidTaskId() {
            // Given
            var taskId = 1L;
            var newUserId = UUID.fromString("c2f7d9e3-5a8b-46cf-9d4e-8a2f7b3c5e1d");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var requestBody = """
                    {
                    "user_id": "%s",
                    "title": "%s",
                    "description": "%s",
                    "start_date": "%s",
                    "end_date": "%s"
                    }
                    """.formatted(newUserId, "New Title", "New Description", newStartDate, newEndDate);
            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                                .containsEntry("instance", "/api/tasks/1"));
        }

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var taskId = UUID.fromString("e3a8c5d1-7f9b-482b-9f6a-2d8e5b7c9f3a");
            var requestBody = "";
            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskId).contentType(MediaType.TEXT_PLAIN)
                                                                          .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Unsupported Media Type")
                                                                .containsEntry("status", 415)
                                                                .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                                .containsEntry("instance", "/api/tasks/" + taskId));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var taskId = UUID.fromString("e3a8c5d1-7f9b-482b-9f6a-2d8e5b7c9f3a");
            var requestBody = "";
            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to read request")
                                                                .containsEntry("instance", "/api/tasks/" + taskId));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var taskId = UUID.fromString("e3a8c5d1-7f9b-482b-9f6a-2d8e5b7c9f3a");
            var requestBody = "{}";
            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("instance", "/api/tasks/" + taskId);
                             assertThatJson(json).inPath("detail")
                                                 .asString()
                                                 .contains("The user ID must not be empty")
                                                 .contains("The title must not be empty");
                         //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateTaskRequest", "field", "userId", "message", "The user ID must not be empty"),
                                                  Map.of("entity", "updateTaskRequest", "field", "title", "message", "The title must not be empty")
                                          );
                       //@formatter:on
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var taskId = UUID.fromString("e3a8c5d1-7f9b-482b-9f6a-2d8e5b7c9f3a");
            var requestBody = """
                    {
                    "user_id": "",
                    "title": ""
                    }
                    """;
            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("instance", "/api/tasks/" + taskId);
                             assertThatJson(json).inPath("detail")
                                                 .asString()
                                                 .contains("The user ID must not be empty")
                                                 .contains("The title must not be empty");
                         //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateTaskRequest", "field", "userId", "message", "The user ID must not be empty"),
                                                  Map.of("entity", "updateTaskRequest", "field", "title", "message", "The title must not be empty")
                                          );
                       //@formatter:on
                         });
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingTaskId() {
            // Given
            var requestBuilder = delete(TASKS_ROOT_PATH + "/");

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.NOT_FOUND)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Not Found")
                                                                .containsEntry("status", 404)
                                                                .containsEntry("detail", "No static resource api/tasks.")
                                                                .containsEntry("instance", "/api/tasks/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidTaskId() {
            // Given
            var taskId = 1L;
            var requestBuilder = delete(TASKS_DELETE_BY_ID_FULL_PATH, taskId);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                                .containsEntry("instance", "/api/tasks/1"));
        }

    }

}
