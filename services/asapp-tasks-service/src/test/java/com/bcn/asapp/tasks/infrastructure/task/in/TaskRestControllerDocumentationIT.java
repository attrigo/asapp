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

import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aTask;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.application.task.in.command.UpdateTaskCommand;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByUserIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;
import com.bcn.asapp.tasks.testutil.RestDocsConstrainedFields;
import com.bcn.asapp.tasks.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link TaskRestController} REST API documentation.
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all task endpoints and error responses</li>
 * <li>Documents path parameters, request fields, and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 * <li>Covers error responses for validation failures, unauthorized access, not found, and server errors</li>
 */
@WithMockUser
class TaskRestControllerDocumentationIT extends RestDocsWebMvcTestContext {

    @Nested
    class GetTaskById {

        @Test
        void DocumentsGetTaskById_TaskFound() throws Exception {
            // Given
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();
            var taskUserIdValue = task.getUserId()
                                      .value();
            var taskTitleValue = task.getTitle()
                                     .value();
            var taskDescriptionValue = task.getDescription()
                                           .value();
            var taskStartDateValue = task.getStartDate()
                                         .value();
            var taskEndDateValue = task.getEndDate()
                                       .value();
            var response = new GetTaskByIdResponse(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);

            given(readTaskUseCase.getTaskById(any(UUID.class))).willReturn(Optional.of(task));
            given(taskMapper.toGetTaskByIdResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(TASKS_GET_BY_ID_FULL_PATH, taskIdValue).accept(APPLICATION_JSON)
                                                                       .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-task-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The task's unique identifier")),
                           responseFields(
                                   fieldWithPath("task_id").description("The task's unique identifier"),
                                   fieldWithPath("user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("title").description("The task's title"),
                                   fieldWithPath("description").description("The task's description"),
                                   fieldWithPath("start_date").description("The task's start date in ISO 8601 format"),
                                   fieldWithPath("end_date").description("The task's end date in ISO 8601 format"))
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void DocumentsGetTasksByUserId() throws Exception {
            // Given
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();
            var taskUserIdValue = task.getUserId()
                                      .value();
            var taskTitleValue = task.getTitle()
                                     .value();
            var taskDescriptionValue = task.getDescription()
                                           .value();
            var taskStartDateValue = task.getStartDate()
                                         .value();
            var taskEndDateValue = task.getEndDate()
                                       .value();
            var response = new GetTasksByUserIdResponse(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue,
                    taskEndDateValue);

            given(readTaskUseCase.getTasksByUserId(any(UUID.class))).willReturn(List.of(task));
            given(taskMapper.toGetTasksByUserIdResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(TASKS_GET_BY_USER_ID_FULL_PATH, taskUserIdValue).accept(APPLICATION_JSON)
                                                                                .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-tasks-by-user-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The user's unique identifier")),
                           responseFields(
                                   fieldWithPath("[].task_id").description("The task's unique identifier"),
                                   fieldWithPath("[].user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("[].title").description("The task's title"),
                                   fieldWithPath("[].description").description("The task's description"),
                                   fieldWithPath("[].start_date").description("The task's start date in ISO 8601 format"),
                                   fieldWithPath("[].end_date").description("The task's end date in ISO 8601 format")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void DocumentsGetAllTasks() throws Exception {
            // Given
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();
            var taskUserIdValue = task.getUserId()
                                      .value();
            var taskTitleValue = task.getTitle()
                                     .value();
            var taskDescriptionValue = task.getDescription()
                                           .value();
            var taskStartDateValue = task.getStartDate()
                                         .value();
            var taskEndDateValue = task.getEndDate()
                                       .value();
            var response = new GetAllTasksResponse(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);

            given(readTaskUseCase.getAllTasks()).willReturn(List.of(task));
            given(taskMapper.toGetAllTasksResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(TASKS_GET_ALL_FULL_PATH).accept(APPLICATION_JSON)
                                                        .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-all-tasks",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           responseFields(
                                   fieldWithPath("[].task_id").description("The task's unique identifier"),
                                   fieldWithPath("[].user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("[].title").description("The task's title"),
                                   fieldWithPath("[].description").description("The task's description"),
                                   fieldWithPath("[].start_date").description("The task's start date in ISO 8601 format"),
                                   fieldWithPath("[].end_date").description("The task's end date in ISO 8601 format")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class CreateTask {

        @Test
        void DocumentsCreateTask() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(CreateTaskRequest.class);
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();
            var taskUserIdValue = task.getUserId()
                                      .value();
            var taskTitleValue = task.getTitle()
                                     .value();
            var taskDescriptionValue = task.getDescription()
                                           .value();
            var taskStartDateValue = task.getStartDate()
                                         .value();
            var taskEndDateValue = task.getEndDate()
                                       .value();
            var requestBody = """
                    {
                        "user_id": "%s",
                        "title": "%s",
                        "description": "%s",
                        "start_date": "%s",
                        "end_date": "%s"
                    }
                    """.formatted(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var createTaskCommand = new CreateTaskCommand(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var response = new CreateTaskResponse(taskIdValue);

            given(taskMapper.toCreateTaskCommand(any(CreateTaskRequest.class))).willReturn(createTaskCommand);
            given(createTaskUseCase.createTask(any(CreateTaskCommand.class))).willReturn(task);
            given(taskMapper.toCreateTaskResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(TASKS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content(requestBody)
                                                        .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isCreated())
                   .andDo(
                   // @formatter:off
                       document("create-task",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           requestFields(
                                   fields.withPath("user_id", "userId").description("The task's owner unique identifier"),
                                   fields.withPath("title").description("The task's title"),
                                   fields.withPath("description").description("The task's description").optional(),
                                   fields.withPath("start_date", "startDate").description("The task's start date in ISO 8601 format").optional(),
                                   fields.withPath("end_date", "endDate").description("The task's end date in ISO 8601 format").optional()
                           ),
                           responseFields(fieldWithPath("task_id").description("The created task's unique identifier"))
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void DocumentsUpdateTaskById_TaskFound() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(UpdateTaskRequest.class);
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();
            var taskUserIdValue = task.getUserId()
                                      .value();
            var taskTitleValue = task.getTitle()
                                     .value();
            var taskDescriptionValue = task.getDescription()
                                           .value();
            var taskStartDateValue = task.getStartDate()
                                         .value();
            var taskEndDateValue = task.getEndDate()
                                       .value();
            var requestBody = """
                    {
                        "user_id": "%s",
                        "title": "%s",
                        "description": "%s",
                        "start_date": "%s",
                        "end_date": "%s"
                    }
                    """.formatted(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var updateTaskCommand = new UpdateTaskCommand(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue,
                    taskEndDateValue);
            var response = new UpdateTaskResponse(taskIdValue);

            given(taskMapper.toUpdateTaskCommand(any(UUID.class), any(UpdateTaskRequest.class))).willReturn(updateTaskCommand);
            given(updateTaskUseCase.updateTaskById(any(UpdateTaskCommand.class))).willReturn(Optional.of(task));
            given(taskMapper.toUpdateTaskResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdValue).contentType(APPLICATION_JSON)
                                                                          .content(requestBody)
                                                                          .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("update-task-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The task's unique identifier")),
                           requestFields(
                                   fields.withPath("user_id", "userId").description("The task's owner unique identifier"),
                                   fields.withPath("title").description("The task's title"),
                                   fields.withPath("description").description("The task's description").optional(),
                                   fields.withPath("start_date", "startDate").description("The task's start date in ISO 8601 format").optional(),
                                   fields.withPath("end_date", "endDate").description("The task's end date in ISO 8601 format").optional()
                           ),
                           responseFields(fieldWithPath("task_id").description("The updated task's unique identifier"))
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void DocumentsDeleteTaskById() throws Exception {
            // Given
            var task = aTask();
            var taskIdValue = task.getId()
                                  .value();

            given(deleteTaskUseCase.deleteTaskById(any(UUID.class))).willReturn(true);

            // When & Then
            mockMvc.perform(delete(TASKS_DELETE_BY_ID_FULL_PATH, taskIdValue).header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isNoContent())
                   .andDo(
                   // @formatter:off
                       document("delete-task-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The task's unique identifier")))
                       // @formatter:on
                   );
        }

    }

    @Nested
    class Errors {

        @Test
        void DocumentsValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(post(TASKS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content("{}"))
                   .andExpect(status().isBadRequest())
                   .andDo(
                   // @formatter:off
                       document("error-validation-failure",
                           relaxedResponseFields(
                               fieldWithPath("title").description("Short summary of the problem type"),
                               fieldWithPath("status").description("HTTP status code"),
                               fieldWithPath("detail").description("Human-readable explanation of the problem"),
                               fieldWithPath("errors").description("List of validation errors"),
                               fieldWithPath("errors[].entity").description("Entity that failed validation"),
                               fieldWithPath("errors[].field").description("Field that failed validation"),
                               fieldWithPath("errors[].message").description("Validation error message")
                           )
                       )
                   // @formatter:on
                   );
        }

        @WithAnonymousUser
        @Test
        void DocumentsUnauthorized() throws Exception {
            // Given
            var taskIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            // When & Then
            mockMvc.perform(get(TASKS_GET_BY_ID_FULL_PATH, taskIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isUnauthorized())
                   .andDo(document("error-unauthorized"));
        }

        @Test
        void DocumentsNotFound() throws Exception {
            // Given
            var taskIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            given(readTaskUseCase.getTaskById(any(UUID.class))).willReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(TASKS_GET_BY_ID_FULL_PATH, taskIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isNotFound())
                   .andDo(document("error-not-found"));
        }

        @Test
        void DocumentsInternalServerError() throws Exception {
            // Given
            var taskIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            given(readTaskUseCase.getTaskById(any(UUID.class))).willThrow(new DataRetrievalFailureException("Database error"));

            // When & Then
            mockMvc.perform(get(TASKS_GET_BY_ID_FULL_PATH, taskIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isInternalServerError())
                   .andDo(
                   // @formatter:off
                       document("error-internal-server-error",
                           relaxedResponseFields(
                               fieldWithPath("title").description("Short summary of the problem type"),
                               fieldWithPath("status").description("HTTP status code"),
                               fieldWithPath("detail").description("Human-readable explanation of the problem"),
                               fieldWithPath("error").description("Machine-readable error code")
                           )
                       )
                   // @formatter:on
                   );
        }

    }

}
