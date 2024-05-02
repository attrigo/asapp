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
package com.bcn.asapp.tasks.task;

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.tasks.config.JacksonMapperConfiguration;

@WebMvcTest(TaskRestController.class)
@Import(JacksonMapperConfiguration.class)
class TaskControllerIT {

    public static final String TASKS_EMPTY_ID_PATH = TASKS_ROOT_PATH + "/";

    @Value("${spring.mvc.format.date-time}")
    private String dateTimeFormat;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskServiceMock;

    private UUID fakeTaskId;

    private String fakeTaskTitle;

    private String fakeTaskDescription;

    private LocalDateTime fakeTaskStartDate;

    private String fakeTaskStartDateFormatted;

    @BeforeEach
    void beforeEach() {
        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskStartDate = LocalDateTime.now()
                                              .truncatedTo(ChronoUnit.MILLIS);
        this.fakeTaskTitle = "IT Title";
        this.fakeTaskDescription = "IT Description";
        this.fakeTaskStartDateFormatted = DateTimeFormatter.ofPattern(dateTimeFormat)
                                                           .format(fakeTaskStartDate);
    }

    // getTaskById
    @Test
    @DisplayName("GIVEN id is empty WHEN get a task by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_GetTaskById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN id is not a valid UUID WHEN get a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_GetTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN id does not exists WHEN get a task by id THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_GetTaskById_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
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
    @DisplayName("GIVEN id exists WHEN get a task by id THEN returns HTTP response with status OK And the body with the task found")
    void IdExists_GetTaskById_ReturnsStatusOkAndBodyWithTaskFound() throws Exception {
        // Given
        var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
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
               .andExpect(jsonPath("$.startDateTime", is(fakeTaskStartDateFormatted)));
    }

    // getAllTasks
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
    void ThereAreTasks_GetAllTasks_ReturnsStatusOkAndBodyWithAllTasksFound() throws Exception {
        var fakeTask1Id = UUID.randomUUID();
        var fakeTask2Id = UUID.randomUUID();
        var fakeTask3Id = UUID.randomUUID();

        // Given
        var fakeTask1 = new TaskDTO(fakeTask1Id, fakeTaskTitle + " 1", fakeTaskDescription + " 1", fakeTaskStartDate);
        var fakeTask2 = new TaskDTO(fakeTask2Id, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);
        var fakeTask3 = new TaskDTO(fakeTask3Id, fakeTaskTitle + " 3", fakeTaskDescription + " 3", fakeTaskStartDate);
        var fakeTasks = Arrays.asList(fakeTask1, fakeTask2, fakeTask3);
        given(taskServiceMock.findAll()).willReturn(fakeTasks);

        // When & Then
        var requestBuilder = get(TASKS_GET_ALL_FULL_PATH);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$[0].id", is(fakeTask1Id.toString())))
               .andExpect(jsonPath("$[0].title", is(fakeTaskTitle + " 1")))
               .andExpect(jsonPath("$[0].description", is(fakeTaskDescription + " 1")))
               .andExpect(jsonPath("$[0].startDateTime", is(fakeTaskStartDateFormatted)))
               .andExpect(jsonPath("$[1].id", is(fakeTask2Id.toString())))
               .andExpect(jsonPath("$[1].title", is(fakeTaskTitle + " 2")))
               .andExpect(jsonPath("$[1].description", is(fakeTaskDescription + " 2")))
               .andExpect(jsonPath("$[1].startDateTime", is(fakeTaskStartDateFormatted)))
               .andExpect(jsonPath("$[2].id", is(fakeTask3Id.toString())))
               .andExpect(jsonPath("$[2].title", is(fakeTaskTitle + " 3")))
               .andExpect(jsonPath("$[2].description", is(fakeTaskDescription + " 3")))
               .andExpect(jsonPath("$[2].startDateTime", is(fakeTaskStartDateFormatted)));
    }

    // CreateTask
    @Test
    @DisplayName("GIVEN task is not a valid Json WHEN create a task THEN returns HTTP response with status Unsupported Media Type And the body with the problem details")
    void TaskIsNotJson_CreateTask_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN task is not present WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskIsNotPresent_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN task has not mandatory fields WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskHasNotMandatoryFields_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var taskToCreate = new TaskDTO(null, null, fakeTaskDescription, fakeTaskStartDate);
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
        var taskToCreate = new TaskDTO(null, "", fakeTaskDescription, fakeTaskStartDate);
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
    @DisplayName("GIVEN task's start date has invalid format WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskStartDateHasInvalidFormat_CreateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var taskToCreate = """
                {
                "title": "IT Title",
                "description": "IT Description",
                "startDateTime": "2011-11-11T11:11:11"
                }
                """;
        var taskToCreateAsJson = objectMapper.writeValueAsString(taskToCreate);

        var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                         .content(taskToCreateAsJson);
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
    @DisplayName("GIVEN task is valid WHEN create a task THEN returns HTTP response with status CREATED And the body with the task created")
    void TaskIsValid_CreateTask_ReturnsStatusCreatedAndBodyWithTaskCreated() throws Exception {
        // Given
        var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        given(taskServiceMock.create(any(TaskDTO.class))).willReturn(fakeTask);

        // When & Then
        var taskToCreate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToCreateAsJson = objectMapper.writeValueAsString(taskToCreate);

        var requestBuilder = post(TASKS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                         .content(taskToCreateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isCreated())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.id", is(fakeTaskId.toString())))
               .andExpect(jsonPath("$.title", is(fakeTaskTitle)))
               .andExpect(jsonPath("$.description", is(fakeTaskDescription)))
               .andExpect(jsonPath("$.startDateTime", is(fakeTaskStartDateFormatted)));
    }

    // UpdateTask
    @Test
    @DisplayName("GIVEN id is empty WHEN update a task THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_UpdateTask_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
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
    @DisplayName("GIVEN id is not a valid UUID WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_UpdateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = 1L;
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
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
    @DisplayName("GIVEN task is not a valid Json WHEN update a task THEN returns HTTP response with status Unsupported Media Type And content with the problem details")
    void TaskIsNotJson_UpdateTask_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN task is not present WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskIsNotPresent_UpdateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN task has not mandatory fields WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskHasNotMandatoryFields_UpdateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, null, fakeTaskDescription, fakeTaskStartDate);
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
    @DisplayName("GIVEN task mandatory fields are empty WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskMandatoryFieldsAreEmpty_UpdateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, "", fakeTaskDescription, fakeTaskStartDate);
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
    @DisplayName("GIVEN task's start date has invalid format WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void TaskStartDateHasInvalidFormat_UpdateTask_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = """
                {
                "title": "IT Title",
                "description": "IT Description",
                "startDateTime": "2011-11-11T11:11:11"
                }
                """;
        var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

        var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(taskToUpdateAsJson);
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
    @DisplayName("GIVEN id does not exists WHEN update a task THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_UpdateTask_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
        // Given
        given(taskServiceMock.updateById(any(UUID.class), any(TaskDTO.class))).willReturn(Optional.empty());

        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

        var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(taskToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("GIVEN id exists WHEN update a task THEN returns HTTP response with status OK And the body with the task updated")
    void IdExists_UpdateTask_ReturnsStatusOkAndBodyWithTaskUpdated() throws Exception {
        var anotherFakeTaskStartDate = LocalDateTime.now()
                                                    .truncatedTo(ChronoUnit.MILLIS);
        var anotherFakeTaskStartDateFormatted = DateTimeFormatter.ofPattern(dateTimeFormat)
                                                                 .format(anotherFakeTaskStartDate);

        // Given
        var fakeTask = new TaskDTO(fakeTaskId, fakeTaskTitle + " 2", fakeTaskDescription + " 2", anotherFakeTaskStartDate);
        given(taskServiceMock.updateById(any(UUID.class), any(TaskDTO.class))).willReturn(Optional.of(fakeTask));

        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToUpdateAsJson = objectMapper.writeValueAsString(taskToUpdate);

        var requestBuilder = put(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(taskToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.id", is(fakeTaskId.toString())))
               .andExpect(jsonPath("$.title", is(fakeTaskTitle + " 2")))
               .andExpect(jsonPath("$.description", is(fakeTaskDescription + " 2")))
               .andExpect(jsonPath("$.startDateTime", is(anotherFakeTaskStartDateFormatted)));
    }

    // DeleteTaskById
    @Test
    @DisplayName("GIVEN id is empty WHEN delete a task by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_DeleteTaskById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN id is not a valid UUID WHEN delete a task by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_DeleteTaskById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
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
    @DisplayName("GIVEN id does not exists WHEN delete a task by id THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_DeleteTaskById_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
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
    @DisplayName("GIVEN id exists WHEN delete a task by id THEN returns HTTP response with status NO_CONTENT And the body without content")
    void IdExists_DeleteTaskById_ReturnsStatusNoContentAndBodyWithoutContent() throws Exception {
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
