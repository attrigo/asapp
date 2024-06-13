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
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_PROJECT_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.tasks.AsappTasksServiceApplication;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class TaskE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WebTestClient webTestClient;

    private UUID fakeTaskId;

    private String fakeTaskTitle;

    private String fakeTaskDescription;

    private Instant fakeTaskStartDate;

    private UUID fakeProjectId;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();

        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskTitle = "E2E IT Title";
        this.fakeTaskDescription = "E2E IT Description";
        this.fakeTaskStartDate = Instant.now()
                                        .truncatedTo(ChronoUnit.MILLIS);
        this.fakeProjectId = UUID.randomUUID();
    }

    // GetTaskById
    @Test
    @DisplayName("GIVEN task id does not exists WHEN get a task by id THEN does not get the task And returns HTTP response with status NOT_FOUND And without body")
    void TaskIdNotExists_GetTaskById_DoesNotGetTheTaskAndReturnsStatusNotFoundAndWithoutBody() {
        // When & Then
        var idToFind = fakeTaskId;

        webTestClient.get()
                     .uri(TASKS_GET_BY_ID_FULL_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN task id exists WHEN get a task by id THEN gets the task And returns HTTP response with status OK And the body with the task found")
    void TaskIdExists_GetTaskById_GetsTaskAndReturnsStatusOKAndBodyWithTaskFound() {
        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
        var taskToBeFound = taskRepository.save(fakeTask);
        assertNotNull(taskToBeFound);

        // When & Then
        var idToFind = taskToBeFound.id();

        var expectedTask = new TaskDTO(idToFind, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

        webTestClient.get()
                     .uri(TASKS_GET_BY_ID_FULL_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(TaskDTO.class)
                     .isEqualTo(expectedTask);
    }

    // GetAllTasks
    @Test
    @DisplayName("GIVEN there are not tasks WHEN get all tasks THEN does not find any tasks And returns HTTP response with status OK And an empty body")
    void ThereAreNotTasks_GetAllTasks_DoesNotFindTasksAndReturnsStatusOKAndEmptyBody() {
        // When & Then
        webTestClient.get()
                     .uri(TASKS_GET_ALL_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(TaskDTO.class)
                     .hasSize(0);
    }

    @Test
    @DisplayName("GIVEN there are tasks WHEN get all tasks THEN gets all tasks And returns HTTP response with status OK And the body with the tasks found")
    void ThereAreTasks_GetAllTasks_GetsAllTasksAndReturnsStatusOKAndBodyWithTasksFound() {
        var fakeTaskTitle1 = fakeTaskTitle + " 1";
        var fakeTaskTitle2 = fakeTaskTitle + " 2";
        var fakeTaskTitle3 = fakeTaskTitle + " 3";
        var fakeTaskDesc1 = fakeTaskDescription + " 1";
        var fakeTaskDesc2 = fakeTaskDescription + " 2";
        var fakeTaskDesc3 = fakeTaskDescription + " 3";

        // Given
        var fakeTask1 = new Task(null, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
        var fakeTask2 = new Task(null, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
        var fakeTask3 = new Task(null, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
        var tasksToBeFound = taskRepository.saveAll(List.of(fakeTask1, fakeTask2, fakeTask3));
        var taskIdsToBeFound = tasksToBeFound.stream()
                                             .map(Task::id)
                                             .collect(Collectors.toList());
        assertNotNull(taskIdsToBeFound);
        assertEquals(3L, taskIdsToBeFound.size());

        // When & Then
        var expectedTask1 = new TaskDTO(taskIdsToBeFound.getFirst(), fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
        var expectedTask2 = new TaskDTO(taskIdsToBeFound.get(1), fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
        var expectedTask3 = new TaskDTO(taskIdsToBeFound.getLast(), fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
        var expectedTasks = List.of(expectedTask1, expectedTask2, expectedTask3);

        webTestClient.get()
                     .uri(TASKS_GET_ALL_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(TaskDTO.class)
                     .hasSize(3)
                     .isEqualTo(expectedTasks);
    }

    // getByProjectId
    @Test
    @DisplayName("GIVEN there are not tasks with project id WHEN get tasks by project id THEN does not find any tasks And returns HTTP response with status OK And and empty body")
    void ThereAreNotTasksWithProjectId_GetTasksByProjectId_DoesNotFindTasksAndReturnsStatusOkAndEmptyBody() {
        // When & Then
        var idToFind = UUID.randomUUID();

        webTestClient.get()
                     .uri(TASKS_GET_BY_PROJECT_ID_FULL_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(TaskDTO.class)
                     .hasSize(0);
    }

    @Test
    @DisplayName("GIVEN there are tasks with project id WHEN get tasks by project id THEN gets tasks And returns HTTP response with status OK And the body with the tasks found")
    void ThereAreTasksWithProjectId_GetTasksByProjectId_GetsTasksAndReturnsStatusOKAndBodyWithTaskFound() {
        var fakeTaskTitle1 = fakeTaskTitle + " 1";
        var fakeTaskTitle2 = fakeTaskTitle + " 2";
        var fakeTaskTitle3 = fakeTaskTitle + " 3";
        var fakeTaskDesc1 = fakeTaskDescription + " 1";
        var fakeTaskDesc2 = fakeTaskDescription + " 2";
        var fakeTaskDesc3 = fakeTaskDescription + " 3";

        // Given
        var taskToBeFound1 = new Task(null, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
        var taskToBeFound2 = new Task(null, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
        var taskToBeFound3 = new Task(null, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
        var tasksToBeFound = taskRepository.saveAll(List.of(taskToBeFound1, taskToBeFound2, taskToBeFound3));
        var taskIdsToBeFound = tasksToBeFound.stream()
                                             .map(Task::id)
                                             .collect(Collectors.toList());
        assertNotNull(taskIdsToBeFound);
        assertEquals(3L, taskIdsToBeFound.size());

        // When & Then
        var expectedTask1 = new TaskDTO(taskIdsToBeFound.getFirst(), fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
        var expectedTask2 = new TaskDTO(taskIdsToBeFound.get(1), fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
        var expectedTask3 = new TaskDTO(taskIdsToBeFound.getLast(), fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
        var expectedTasks = List.of(expectedTask1, expectedTask2, expectedTask3);

        var idToFind = fakeProjectId;

        webTestClient.get()
                     .uri(TASKS_GET_BY_PROJECT_ID_FULL_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(TaskDTO.class)
                     .hasSize(3)
                     .isEqualTo(expectedTasks);
    }

    // CreateTask
    @Test
    @DisplayName("GIVEN task has id field WHEN create a task THEN creates the task ignoring the given task id And returns HTTP response with status CREATED And the body with the task created")
    void TaskHasIdField_CreateTask_CreatesTaskIgnoringTaskIdAndReturnsStatusCreatedAndBodyWithTaskCreated() {
        var anotherFakeTaskId = UUID.randomUUID();

        // When & Then
        var taskToCreate = new TaskDTO(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

        var response = webTestClient.post()
                                    .uri(TASKS_CREATE_FULL_PATH)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToCreate)
                                    .exchange()
                                    .expectStatus()
                                    .isCreated()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .value(task -> assertThat(task.id(), allOf(not(anotherFakeTaskId), notNullValue())))
                                    .value(task -> assertThat(task.title(), equalTo(fakeTaskTitle)))
                                    .value(task -> assertThat(task.description(), equalTo(fakeTaskDescription)))
                                    .value(task -> assertThat(task.startDateTime(), equalTo(fakeTaskStartDate)))
                                    .value(task -> assertThat(task.projectId(), equalTo(fakeProjectId)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);
        var expectedTask = new Task(response.id(), response.title(), response.description(), response.startDateTime(), response.projectId());
        var actualTask = taskRepository.findById(response.id());
        assertTrue(actualTask.isPresent());
        assertEquals(expectedTask, actualTask.get());
    }

    @Test
    @DisplayName("GIVEN task fields are valid WHEN create a task THEN creates the task And returns HTTP response with status CREATED And the body with the task created")
    void TaskFieldsAreValid_CreateTask_CreatesTaskAndReturnsStatusCreatedAndBodyWithTaskCreated() {
        // When & Then
        var taskToCreate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

        var response = webTestClient.post()
                                    .uri(TASKS_CREATE_FULL_PATH)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToCreate)
                                    .exchange()
                                    .expectStatus()
                                    .isCreated()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .value(task -> assertThat(task.id(), notNullValue()))
                                    .value(task -> assertThat(task.title(), equalTo(fakeTaskTitle)))
                                    .value(task -> assertThat(task.description(), equalTo(fakeTaskDescription)))
                                    .value(task -> assertThat(task.startDateTime(), equalTo(fakeTaskStartDate)))
                                    .value(task -> assertThat(task.projectId(), equalTo(fakeProjectId)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);
        var expectedTask = new Task(response.id(), response.title(), response.description(), response.startDateTime(), fakeProjectId);
        var actualTask = taskRepository.findById(response.id());
        assertTrue(actualTask.isPresent());
        assertEquals(expectedTask, actualTask.get());
    }

    // UpdateTaskById
    @Test
    @DisplayName("GIVEN task id does not exists WHEN update a task by id THEN does not update the task And returns HTTP response with status NOT_FOUND And without body")
    void TaskIdNotExists_UpdateTaskById_DoesNotUpdateTheTaskAndReturnsStatusNotFoundAndWithoutBody() {
        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

        webTestClient.put()
                     .uri(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(taskToUpdate)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN task id exists And new task data has id field WHEN update a task by id THEN updates all task fields except the id And returns HTTP response with status OK And the body with the task updated")
    void TaskIdExistsAndNewTaskDataHasIdField_UpdateTaskById_UpdatesAllTaskFieldsExceptIdAndReturnsStatusOkAndBodyWithTaskUpdated() {
        var anotherFakeTaskTitle = fakeTaskTitle + " 2";
        var anotherFakeTaskDesc = fakeTaskDescription + " 2";
        var anotherFakeTaskStartDate = Instant.now()
                                              .truncatedTo(ChronoUnit.MILLIS);
        var anotherFakeTaskProjectId = UUID.randomUUID();

        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
        var taskToBeUpdated = taskRepository.save(fakeTask);
        assertNotNull(taskToBeUpdated);

        // When & Then
        var idToUpdate = taskToBeUpdated.id();
        var taskToUpdate = new TaskDTO(UUID.randomUUID(), anotherFakeTaskTitle, anotherFakeTaskDesc, anotherFakeTaskStartDate, anotherFakeTaskProjectId);

        var expectedTaskDTO = new TaskDTO(idToUpdate, anotherFakeTaskTitle, anotherFakeTaskDesc, anotherFakeTaskStartDate, anotherFakeTaskProjectId);

        var response = webTestClient.put()
                                    .uri(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .isEqualTo(expectedTaskDTO)
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);
        var expectedTask = new Task(response.id(), response.title(), response.description(), response.startDateTime(), response.projectId());
        var actualTask = taskRepository.findById(response.id());
        assertTrue(actualTask.isPresent());
        assertEquals(expectedTask, actualTask.get());
    }

    @Test
    @DisplayName("GIVEN task id exists And new task data fields are valid WHEN update a task THEN updates all task fields And returns HTTP response with status OK And the body with the task updated")
    void TaskIdExistsAndNewTaskDataFieldsAreValid_UpdateTask_UpdatesAllTaskFieldsAndReturnsStatusOkAndBodyWithTaskUpdated() {
        var anotherFakeTaskTitle = fakeTaskTitle + " 2";
        var anotherFakeTaskDesc = fakeTaskDescription + " 2";
        var anotherFakeTaskStartDate = Instant.now()
                                              .truncatedTo(ChronoUnit.MILLIS);
        var anotherFakeTaskProjectId = UUID.randomUUID();

        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
        var taskToBeUpdated = taskRepository.save(fakeTask);
        assertNotNull(taskToBeUpdated);

        // When & Then
        var idToUpdate = taskToBeUpdated.id();
        var taskToUpdate = new TaskDTO(null, anotherFakeTaskTitle, anotherFakeTaskDesc, anotherFakeTaskStartDate, anotherFakeTaskProjectId);

        var expectedTaskDTO = new TaskDTO(idToUpdate, anotherFakeTaskTitle, anotherFakeTaskDesc, anotherFakeTaskStartDate, anotherFakeTaskProjectId);

        var response = webTestClient.put()
                                    .uri(TASKS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .isEqualTo(expectedTaskDTO)
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);
        var expectedTask = new Task(response.id(), response.title(), response.description(), response.startDateTime(), response.projectId());
        var actualTask = taskRepository.findById(response.id());
        assertTrue(actualTask.isPresent());
        assertEquals(expectedTask, actualTask.get());
    }

    // DeleteTaskById
    @Test
    @DisplayName("GIVEN task id does not exists WHEN delete a task by id THEN does not delete the task And returns HTTP response with status NOT_FOUND And without body")
    void TaskIdNotExists_DeleteTaskById_DoesNotDeleteTaskAndReturnsStatusNotFoundAndWithoutBody() {
        // When & Then
        var idToDelete = UUID.randomUUID();

        webTestClient.delete()
                     .uri(TASKS_DELETE_BY_ID_FULL_PATH, idToDelete)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN task id exists WHEN delete a task by id THEN deletes the task And returns HTTP response with status NO_CONTENT And without body")
    void TaskIdExists_DeleteTaskById_DeletesTaskAndReturnsStatusNoContentAndWithoutBody() {
        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
        var taskToBeDeleted = taskRepository.save(fakeTask);
        assertNotNull(taskToBeDeleted);

        // When & Then
        var idToDelete = taskToBeDeleted.id();

        webTestClient.delete()
                     .uri(TASKS_DELETE_BY_ID_FULL_PATH, idToDelete)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNoContent()
                     .expectBody()
                     .isEmpty();

        assertFalse(taskRepository.findById(taskToBeDeleted.id())
                                  .isPresent());
    }

}
