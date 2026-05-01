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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
import com.bcn.asapp.tasks.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link TaskRestController} REST API documentation.
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all task endpoints</li>
 * <li>Documents path parameters, request fields, and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 */
@WithMockUser
class TaskRestControllerDocIT extends RestDocsWebMvcTestContext {

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
            mockMvc.perform(get(TASKS_GET_BY_ID_FULL_PATH, taskIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-task-by-id",
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
            mockMvc.perform(get(TASKS_GET_BY_USER_ID_FULL_PATH, taskUserIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-tasks-by-user-id",
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
            mockMvc.perform(get(TASKS_GET_ALL_FULL_PATH).accept(APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-all-tasks",
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
            var createTaskCommand = new CreateTaskCommand(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var requestBody = """
                    {
                        "user_id": "%s",
                        "title": "%s",
                        "description": "%s",
                        "start_date": "%s",
                        "end_date": "%s"
                    }
                    """.formatted(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var response = new CreateTaskResponse(taskIdValue);

            given(taskMapper.toCreateTaskCommand(any(CreateTaskRequest.class))).willReturn(createTaskCommand);
            given(createTaskUseCase.createTask(any(CreateTaskCommand.class))).willReturn(task);
            given(taskMapper.toCreateTaskResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(TASKS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content(requestBody))
                   .andExpect(status().isCreated())
                   .andDo(
                   // @formatter:off
                       document("create-task",
                           requestFields(
                                   fieldWithPath("user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("title").description("The task's title"),
                                   fieldWithPath("description").description("The task's description").optional(),
                                   fieldWithPath("start_date").description("The task's start date in ISO 8601 format").optional(),
                                   fieldWithPath("end_date").description("The task's end date in ISO 8601 format").optional()
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
            var updateTaskCommand = new UpdateTaskCommand(taskIdValue, taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue,
                    taskEndDateValue);
            var requestBody = """
                    {
                        "user_id": "%s",
                        "title": "%s",
                        "description": "%s",
                        "start_date": "%s",
                        "end_date": "%s"
                    }
                    """.formatted(taskUserIdValue, taskTitleValue, taskDescriptionValue, taskStartDateValue, taskEndDateValue);
            var response = new UpdateTaskResponse(taskIdValue);

            given(taskMapper.toUpdateTaskCommand(any(UUID.class), any(UpdateTaskRequest.class))).willReturn(updateTaskCommand);
            given(updateTaskUseCase.updateTaskById(any(UpdateTaskCommand.class))).willReturn(Optional.of(task));
            given(taskMapper.toUpdateTaskResponse(any(Task.class))).willReturn(response);

            // When & Then
            mockMvc.perform(put(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdValue).contentType(APPLICATION_JSON)
                                                                          .content(requestBody))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("update-task-by-id",
                           pathParameters(parameterWithName("id").description("The task's unique identifier")),
                           requestFields(
                                   fieldWithPath("user_id").description("The task's owner unique identifier"),
                                   fieldWithPath("title").description("The task's title"),
                                   fieldWithPath("description").description("The task's description").optional(),
                                   fieldWithPath("start_date").description("The task's start date in ISO 8601 format").optional(),
                                   fieldWithPath("end_date").description("The task's end date in ISO 8601 format").optional()
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
            mockMvc.perform(delete(TASKS_DELETE_BY_ID_FULL_PATH, taskIdValue))
                   .andExpect(status().isNoContent())
                   .andDo(
                   // @formatter:off
                       document("delete-task-by-id",
                       pathParameters(parameterWithName("id").description("The task's unique identifier")))
                       // @formatter:on
                   );
        }

    }

}
