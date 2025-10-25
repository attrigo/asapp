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

package com.bcn.asapp.tasks.infrastructure.task.in;

import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_DESCRIPTION;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_END_DATE;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_START_DATE;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_TITLE;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_ROOT_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.tasks.testutil.WebMvcTestContext;

@WithMockUser
class TaskControllerIT extends WebMvcTestContext {

    @Nested
    class GetTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_TaskIdPathIsNotPresent() {
            // When & Then
            var requestBuilder = get(TASKS_ROOT_PATH + "/");
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/tasks.")
                                                  .containsEntry("instance", "/api/tasks/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_TaskIdPathIsInvalid() {
            // When & Then
            var taskIdPath = 1L;

            var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, taskIdPath);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/tasks/1");
                   });
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetails_UserIdPathIsNotPresent() {
            // When & Then
            var requestBuilder = get(TASKS_ROOT_PATH + "/user/");
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/tasks/user.")
                                                  .containsEntry("instance", "/api/tasks/user/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetails_UserIdPathIsInvalid() {
            // When & Then
            var idToFind = 1L;

            var requestBuilder = get(TASKS_GET_BY_USER_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/tasks/user/1");
                   });
        }

    }

    @Nested
    class CreateTask {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyIsNotJson() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                             .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Unsupported Media Type")
                                                  .containsEntry("status", 415)
                                                  .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                  .containsEntry("instance", "/api/tasks");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsNotPresent() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to read request")
                                                  .containsEntry("instance", "/api/tasks");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsEmpty() {
            // When & Then
            var requestBody = "{}";

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/tasks");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The user ID must not be null")
                                                  .contains("The title must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createTaskRequest", "field", "userId", "message", "The user ID must not be null"),
                                                          Map.of("entity", "createTaskRequest", "field", "title", "message", "The title must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
            // When & Then
            var requestBody = """
                    {
                    "user_id": "",
                    "title": ""
                    }
                    """;

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/tasks");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The user ID must not be null")
                                                  .contains("The title must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createTaskRequest", "field", "userId", "message", "The user ID must not be null"),
                                                          Map.of("entity", "createTaskRequest", "field", "title", "message", "The title must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_TaskIdPathIsNotPresent() {
            // When & Then
            var requestBody = """
                    {
                    "user_id": "%s",
                    "title": "%s",
                    "description": "%s",
                    "start_date": "%s",
                    "end_date": "%s"
                    }
                    """.formatted(UUID.randomUUID()
                                      .toString(),
                    DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE, DEFAULT_FAKE_END_DATE);

            var requestBuilder = put(TASKS_ROOT_PATH + "/").contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/tasks.")
                                                  .containsEntry("instance", "/api/tasks/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_TaskIdPathIsInvalid() {
            // When & Then
            var taskIdPath = 1L;
            var requestBody = """
                    {
                    "user_id": "%s",
                    "title": "%s",
                    "description": "%s",
                    "start_date": "%s",
                    "end_date": "%s"
                    }
                    """.formatted(UUID.randomUUID()
                                      .toString(),
                    DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE, DEFAULT_FAKE_END_DATE);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/tasks/1");
                   });
        }

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyIsNotJson() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var requestBody = "";

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath).contentType(MediaType.TEXT_PLAIN)
                                                                              .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Unsupported Media Type")
                                                  .containsEntry("status", 415)
                                                  .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                  .containsEntry("instance", "/api/tasks/" + taskIdPath);
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsNotPresent() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var requestBody = "";

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to read request")
                                                  .containsEntry("instance", "/api/tasks/" + taskIdPath);
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsEmpty() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var requestBody = "{}";

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/tasks/" + taskIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The user ID must not be null")
                                                  .contains("The title must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateTaskRequest", "field", "userId", "message", "The user ID must not be null"),
                                                          Map.of("entity", "updateTaskRequest", "field", "title", "message", "The title must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "user_id": "",
                    "title": ""
                    }
                    """;

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/tasks/" + taskIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The user ID must not be null")
                                                  .contains("The title must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateTaskRequest", "field", "userId", "message", "The user ID must not be null"),
                                                          Map.of("entity", "updateTaskRequest", "field", "title", "message", "The title must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_TaskIdPathIsNotPresent() {
            // When & Then
            var requestBuilder = delete(TASKS_ROOT_PATH + "/");
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/tasks.")
                                                  .containsEntry("instance", "/api/tasks/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_TaskIdPathIsInvalid() {
            // When & Then
            var taskIdPath = 1L;

            var requestBuilder = delete(TASKS_DELETE_BY_ID_FULL_PATH, taskIdPath);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/tasks/1");
                   });
        }

    }

}
