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
package com.bcn.asapp.tasks.task.internal;

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_PROJECT_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_ROOT_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.tasks.task.TaskService;

@WebMvcTest(TaskRestController.class)
class TaskControllerIT {

    public static final String TASKS_EMPTY_ID_PATH = TASKS_ROOT_PATH + "/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskServiceMock;

    private UUID fakeTaskId;

    private String fakeTaskTitle;

    private String fakeTaskDescription;

    private Instant fakeTaskStartDate;

    private UUID fakeProjectId;

    @BeforeEach
    void beforeEach() {
        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskStartDate = Instant.now()
                                        .truncatedTo(ChronoUnit.MILLIS);
        this.fakeTaskTitle = "IT Title";
        this.fakeTaskDescription = "IT Description";
        this.fakeProjectId = UUID.randomUUID();
    }

    @Nested
    class GetTaskById {

        @Test
        @DisplayName("GIVEN task id is empty WHEN get a task by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void TaskIdIsEmpty_GetTaskById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = get(TASKS_EMPTY_ID_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", is("No static resource v1/tasks.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/")));
        }

        @Test
        @DisplayName("GIVEN task id is not a valid UUID WHEN get a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskIdIsNotUUID_GetTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToFound = 1L;

            var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, idToFound);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToFound)));
        }

        @Test
        @DisplayName("GIVEN task id does not exists WHEN get a task by id THEN returns HTTP response with status NOT_FOUND And without body")
        void TaskIdNotExists_GetTaskById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(taskServiceMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When & Then
            var idToFind = fakeTaskId;

            var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN task id exists WHEN get a task by id THEN returns HTTP response with status OK And the body with the task found")
        void TaskIdExists_GetTaskById_ReturnsStatusOkAndBodyWithTaskFound() throws Exception {
            // Given
            var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskServiceMock.findById(any(UUID.class))).willReturn(Optional.of(fakeTask));

            // When & Then
            var idToFind = fakeTaskId;

            var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeTaskId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeTaskTitle)))
                   .andExpect(jsonPath("$.description", is(fakeTaskDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$.projectId", is(fakeProjectId.toString())));
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        @DisplayName("GIVEN there are not tasks WHEN get all tasks THEN returns HTTP response with status OK And an empty body")
        void ThereAreNotTasks_GetAllTasks_ReturnsStatusOkAndEmptyBody() throws Exception {
            // Given
            given(taskServiceMock.findAll()).willReturn(Collections.emptyList());

            // When & Then
            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GIVEN there are tasks WHEN get all tasks THEN returns HTTP response with status OK And the body with all tasks found")
        void ThereAreTasks_GetAllTasks_ReturnsStatusOkAndBodyWithTasksFound() throws Exception {
            var fakeTaskId1 = UUID.randomUUID();
            var fakeTaskId2 = UUID.randomUUID();
            var fakeTaskId3 = UUID.randomUUID();
            var fakeTaskTitle1 = fakeTaskTitle + " 1";
            var fakeTaskTitle2 = fakeTaskTitle + " 2";
            var fakeTaskTitle3 = fakeTaskTitle + " 3";
            var fakeTaskDesc1 = fakeTaskDescription + " 1";
            var fakeTaskDesc2 = fakeTaskDescription + " 2";
            var fakeTaskDesc3 = fakeTaskDescription + " 3";

            // Given
            var fakeTask1 = new TaskDTO(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var fakeTask2 = new TaskDTO(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var fakeTask3 = new TaskDTO(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var fakeTasks = List.of(fakeTask1, fakeTask2, fakeTask3);
            given(taskServiceMock.findAll()).willReturn(fakeTasks);

            // When & Then
            var requestBuilder = get(TASKS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$[0].id", is(fakeTaskId1.toString())))
                   .andExpect(jsonPath("$[0].title", is(fakeTaskTitle1)))
                   .andExpect(jsonPath("$[0].description", is(fakeTaskDesc1)))
                   .andExpect(jsonPath("$[0].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[0].projectId", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$[1].id", is(fakeTaskId2.toString())))
                   .andExpect(jsonPath("$[1].title", is(fakeTaskTitle2)))
                   .andExpect(jsonPath("$[1].description", is(fakeTaskDesc2)))
                   .andExpect(jsonPath("$[1].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[1].projectId", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$[2].id", is(fakeTaskId3.toString())))
                   .andExpect(jsonPath("$[2].title", is(fakeTaskTitle3)))
                   .andExpect(jsonPath("$[2].description", is(fakeTaskDesc3)))
                   .andExpect(jsonPath("$[2].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[2].projectId", is(fakeProjectId.toString())));
        }

    }

    @Nested
    class GetTasksByProjectId {

        @Test
        @DisplayName("GIVEN project id is empty WHEN get tasks by project id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void ProjectIdIsEmpty_GetTasksByProjectId_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = get(TASKS_ROOT_PATH + "/project/");
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", is("No static resource v1/tasks/project.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/project/")));
        }

        @Test
        @DisplayName("GIVEN project id not a valid UUID WHEN get tasks by project id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectIdIsNotUUID_GetTasksByProjectId_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToFind = 1L;

            var requestBuilder = get(TASKS_GET_BY_PROJECT_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/project/" + idToFind)));
        }

        @Test
        @DisplayName("GIVEN there are not tasks with project id WHEN get tasks by project id THEN returns HTTP response with status OK And an empty body")
        void ThereAreNotTasksWithProjectId_GetTasksByProjectId_ReturnsStatusOkAndEmptyBody() throws Exception {
            // Given
            given(taskServiceMock.findByProjectId(any(UUID.class))).willReturn(Collections.emptyList());

            // When & Then
            var idToFind = UUID.randomUUID();

            var requestBuilder = get(TASKS_GET_BY_PROJECT_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GIVEN there are tasks with project id WHEN get tasks by project id THEN returns HTTP response with status OK And the body with the tasks found")
        void ThereAreTasksWithProjectId_GetTasksByProjectId_ReturnsStatusOkAndBodyWithTasksFound() throws Exception {
            var fakeTaskId1 = UUID.randomUUID();
            var fakeTaskId2 = UUID.randomUUID();
            var fakeTaskId3 = UUID.randomUUID();
            var fakeTaskTitle1 = fakeTaskTitle + " 1";
            var fakeTaskTitle2 = fakeTaskTitle + " 2";
            var fakeTaskTitle3 = fakeTaskTitle + " 3";
            var fakeTaskDesc1 = fakeTaskDescription + " 1";
            var fakeTaskDesc2 = fakeTaskDescription + " 2";
            var fakeTaskDesc3 = fakeTaskDescription + " 3";

            // Given
            var fakeTask1 = new TaskDTO(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var fakeTask2 = new TaskDTO(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var fakeTask3 = new TaskDTO(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var fakeTasks = List.of(fakeTask1, fakeTask2, fakeTask3);
            given(taskServiceMock.findByProjectId(any(UUID.class))).willReturn(fakeTasks);

            // When & Then
            var idToFind = fakeProjectId;

            var requestBuilder = get(TASKS_GET_BY_PROJECT_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$[0].id", is(fakeTaskId1.toString())))
                   .andExpect(jsonPath("$[0].title", is(fakeTaskTitle1)))
                   .andExpect(jsonPath("$[0].description", is(fakeTaskDesc1)))
                   .andExpect(jsonPath("$[0].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[0].projectId", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$[1].id", is(fakeTaskId2.toString())))
                   .andExpect(jsonPath("$[1].title", is(fakeTaskTitle2)))
                   .andExpect(jsonPath("$[1].description", is(fakeTaskDesc2)))
                   .andExpect(jsonPath("$[1].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[1].projectId", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$[2].id", is(fakeTaskId3.toString())))
                   .andExpect(jsonPath("$[2].title", is(fakeTaskTitle3)))
                   .andExpect(jsonPath("$[2].description", is(fakeTaskDesc3)))
                   .andExpect(jsonPath("$[2].startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$[2].projectId", is(fakeProjectId.toString())));
        }

    }

    @Nested
    class CreateTask {

        @Test
        @DisplayName("GIVEN task fields are not a valid Json WHEN create a task THEN returns HTTP response with status Unsupported Media Type And the body with the problem details")
        void TaskFieldsAreNotJson_CreateTask_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = "";

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                             .content(taskToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")));
        }

        @Test
        @DisplayName("GIVEN task fields are not present WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskFieldsAreNotPresent_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = "";

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")));
        }

        @Test
        @DisplayName("GIVEN task mandatory fields are not present WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskMandatoryFieldsAreNotPresent_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = new TaskDTO(null, null, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToCreateAsJson = objectMapper.writeValueAsString(taskToCreate);

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the task is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")))
                   .andExpect(jsonPath("$.errors[0].entity", is("taskDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the task is mandatory")));
        }

        @Test
        @DisplayName("GIVEN task mandatory fields are empty WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskMandatoryFieldsAreEmpty_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = new TaskDTO(null, "", fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToCreateAsJson = objectMapper.writeValueAsString(taskToCreate);

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the task is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")))
                   .andExpect(jsonPath("$.errors[0].entity", is("taskDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the task is mandatory")));
        }

        @Test
        @DisplayName("GIVEN task start date field has invalid format WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskStartDateFieldHasInvalidFormat_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11",
                    "projectId: "ec3b3120-a8ca-4b09-966a-3376b077bc8d"
                    }
                    """;

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")));
        }

        @Test
        @DisplayName("GIVEN task project id field is not a valid UUID WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskProjectIdFieldIsNotUUID_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToCreate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11.111Z",
                    "projectId": 1
                    }
                    """;

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks")));
        }

        @Test
        @DisplayName("GIVEN task fields are valid WHEN create a task THEN returns HTTP response with status CREATED And the body with the task created")
        void TaskFieldsAreValid_CreateTask_ReturnsStatusCreatedAndBodyWithTaskCreated() throws Exception {
            // Given
            var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskServiceMock.create(any(TaskDTO.class))).willReturn(fakeTask);

            // When & Then
            var taskToCreate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToCreateAsJson = objectMapper.writeValueAsString(taskToCreate);

            var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(taskToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isCreated())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeTaskId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeTaskTitle)))
                   .andExpect(jsonPath("$.description", is(fakeTaskDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$.projectId", is(fakeProjectId.toString())));
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        @DisplayName("GIVEN task id is empty WHEN update a task by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void TaskIdIsEmpty_UpdateTaskById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_EMPTY_ID_PATH).contentType(MediaType.APPLICATION_JSON)
                                                         .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", is("No static resource v1/tasks.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/")));
        }

        @Test
        @DisplayName("GIVEN task id is not a valid UUID WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskIdIsNotUUID_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = 1L;
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN task fields are not a valid Json WHEN update a task by id THEN returns HTTP response with status Unsupported Media Type And content with the problem details")
        void NewTaskDataFieldsAreNotJson_UpdateTaskById_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = "";

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.TEXT_PLAIN)
                                                                              .content(taskToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new task data fields are not present WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewTaskDataFieldsAreNotPresent_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = "";

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new task data mandatory fields are not present WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewTaskDataMandatoryFieldsAreNotPresent_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, null, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the task is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)))
                   .andExpect(jsonPath("$.errors[0].entity", is("taskDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the task is mandatory")));
        }

        @Test
        @DisplayName("GIVEN new task data mandatory fields are empty WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewTaskDataMandatoryFieldsAreEmpty_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, "", fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the task is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)))
                   .andExpect(jsonPath("$.errors[0].entity", is("taskDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the task is mandatory")));
        }

        @Test
        @DisplayName("GIVEN new task data start date field has invalid format WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewTaskDataStartDateFieldHasInvalidFormat_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11",
                    "projectId: "ec3b3120-a8ca-4b09-966a-3376b077bc8d"
                    }
                    """;

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new task data project id field is not a valid UUID WHEN update a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewTaskDataProjectIdFieldIsNotUUID_UpdateTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11.111Z",
                    "projectId": 1
                    }
                    """;

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN task id does not exists WHEN update a task by id THEN returns HTTP response with status NOT_FOUND And without body")
        void TaskIdNotExists_UpdateTaskById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(taskServiceMock.updateById(any(UUID.class), any(TaskDTO.class))).willReturn(Optional.empty());

            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN task id exists WHEN update a task by id THEN returns HTTP response with status OK And the body with the task updated")
        void TaskIdExists_UpdateTaskById_ReturnsStatusOkAndBodyWithTaskUpdated() throws Exception {
            // Given
            var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskServiceMock.updateById(any(UUID.class), any(TaskDTO.class))).willReturn(Optional.of(fakeTask));

            // When & Then
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

            var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(taskToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeTaskId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeTaskTitle)))
                   .andExpect(jsonPath("$.description", is(fakeTaskDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeTaskStartDate.toString())))
                   .andExpect(jsonPath("$.projectId", is(fakeProjectId.toString())));
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        @DisplayName("GIVEN task id is empty WHEN delete a task by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void TaskIdIsEmpty_DeleteTaskById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = delete(TASKS_EMPTY_ID_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", is("No static resource v1/tasks.")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/")));
        }

        @Test
        @DisplayName("GIVEN task id is not a valid UUID WHEN delete a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void TaskIdIsNotUUID_DeleteTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToDelete = 1L;

            var requestBuilder = delete(TASKS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is("/v1/tasks/" + idToDelete)));
        }

        @Test
        @DisplayName("GIVEN task id does not exists WHEN delete a task by id THEN returns HTTP response with status NOT_FOUND And without body")
        void TaskIdNotExists_DeleteTaskById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(taskServiceMock.deleteById(any(UUID.class))).willReturn(false);

            // When
            var idToDelete = fakeTaskId;

            var requestBuilder = delete(TASKS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN task id exists WHEN delete a task by id THEN returns HTTP response with status NO_CONTENT And without body")
        void TaskIdExists_DeleteTaskById_ReturnsStatusNoContentAndWithoutBody() throws Exception {
            // Given
            given(taskServiceMock.deleteById(any(UUID.class))).willReturn(true);

            // When
            var idToDelete = fakeTaskId;

            var requestBuilder = delete(TASKS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNoContent())
                   .andExpect(jsonPath("$").doesNotExist());
        }

    }

}
