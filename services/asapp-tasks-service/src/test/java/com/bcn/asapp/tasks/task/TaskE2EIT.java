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

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_ID_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_ROOT_PATH;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

import com.bcn.asapp.dtos.task.TaskDTO;
import com.bcn.asapp.tasks.AsappTasksServiceApplication;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class TaskE2EIT {

    public static final String TASKS_BY_ID_PATH = TASKS_ROOT_PATH + TASKS_GET_BY_ID_PATH;

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

    private LocalDateTime fakeTaskStartDate;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();

        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskTitle = "IT Title";
        this.fakeTaskDescription = "IT Description";
        this.fakeTaskStartDate = LocalDateTime.now()
                                              .truncatedTo(ChronoUnit.MILLIS);
    }

    // GetTaskById
    @Test
    @DisplayName("GIVEN id does not exists WHEN get a task by id THEN returns HTTP response with status NOT_FOUND And no body")
    void IdNotExists_GetTaskById_ReturnsStatusNotFoundAndNoBody() {
        // When & Then
        var idToFind = fakeTaskId;

        webTestClient.get()
                     .uri(TASKS_BY_ID_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN id exists WHEN get a task by id THEN gets the task with given id And returns HTTP response with status OK And the body with the task found")
    void IdExists_GetTaskById_GetsTaskAndReturnsStatusOKAndBodyWithTaskFound() {
        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToBeFound = taskRepository.save(fakeTask);
        assertNotNull(taskToBeFound);

        // When & Then
        var idToFind = taskToBeFound.id();

        webTestClient.get()
                     .uri(TASKS_BY_ID_PATH, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(TaskDTO.class)
                     .value(task -> assertThat(task.id(), equalTo(idToFind)))
                     .value(task -> assertThat(task.title(), equalTo(fakeTaskTitle)))
                     .value(task -> assertThat(task.description(), equalTo(fakeTaskDescription)))
                     .value(task -> assertThat(task.startDateTime(), equalTo(fakeTaskStartDate)));
    }

    // GetAllTasks
    @Test
    @DisplayName("GIVEN there are not tasks WHEN get all tasks THEN returns HTTP response with status OK And an empty body")
    void ThereAreNotTasks_GetAllTasks_ReturnsStatusOKAndEmptyBody() {
        // When & Then
        webTestClient.get()
                     .uri(TASKS_ROOT_PATH)
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
        // Given
        var fakeTask1 = new Task(null, fakeTaskTitle + " 1", fakeTaskDescription + " 1", fakeTaskStartDate);
        var fakeTask2 = new Task(null, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);
        var fakeTask3 = new Task(null, fakeTaskTitle + " 3", fakeTaskDescription + " 3", fakeTaskStartDate);
        var tasksToBeFound = taskRepository.saveAll(Arrays.asList(fakeTask1, fakeTask2, fakeTask3));
        var taskIdsToBeFound = tasksToBeFound.stream()
                                             .map(Task::id)
                                             .collect(Collectors.toList());
        assertNotNull(taskIdsToBeFound);
        assertEquals(3L, taskIdsToBeFound.size());

        // When & Then
        var expectedTask1 = new TaskDTO(taskIdsToBeFound.get(0), fakeTaskTitle + " 1", fakeTaskDescription + " 1", fakeTaskStartDate);
        var expectedTask2 = new TaskDTO(taskIdsToBeFound.get(1), fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);
        var expectedTask3 = new TaskDTO(taskIdsToBeFound.get(2), fakeTaskTitle + " 3", fakeTaskDescription + " 3", fakeTaskStartDate);
        var expected = Arrays.asList(expectedTask1, expectedTask2, expectedTask3);

        webTestClient.get()
                     .uri(TASKS_ROOT_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(TaskDTO.class)
                     .hasSize(3)
                     .isEqualTo(expected);
    }

    // CreateTask
    @Test
    @DisplayName("GIVEN task has id WHEN create a task THEN creates the task ignoring the given task id And returns HTTP response with status CREATED And the body with the task created")
    void TaskHasId_CreateTask_CreatesTaskIgnoringGivenTaskIdAndReturnsStatusCreatedAndBodyWithTaskCreated() {
        var anotherFakeTaskId = UUID.randomUUID();

        // When & Then
        var taskToCreate = new TaskDTO(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);

        var response = webTestClient.post()
                                    .uri(TASKS_ROOT_PATH)
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
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Task(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = taskRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(expected.get(), actual);
    }

    @Test
    @DisplayName("GIVEN task fields are valid WHEN create a task THEN creates the task And returns HTTP response with status CREATED And the body with the task created")
    void TaskFieldsAreValid_CreateTask_CreatesTaskAndReturnsStatusCreatedAndBodyWithTaskCreated() {
        // When & Then
        var taskToCreate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);

        var response = webTestClient.post()
                                    .uri(TASKS_ROOT_PATH)
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
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Task(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = taskRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(expected.get(), actual);
    }

    // UpdateTask
    @Test
    @DisplayName("GIVEN id does not exists WHEN update a task THEN returns HTTP response with status NOT_FOUND And an empty body")
    void IdNotExists_UpdateTask_ReturnsStatusNotFoundAndEmptyBody() {
        // When & Then
        var idToUpdate = fakeTaskId;
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);

        webTestClient.put()
                     .uri(TASKS_BY_ID_PATH, idToUpdate)
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
    @DisplayName("GIVEN id exists And task has id WHEN update a task THEN updates all fields (except task's id) of the task with the given id with the given task data And returns HTTP response with status OK And the body with the task updated")
    void IdExistsAndTaskHasId_UpdateTask_UpdatesAllTaskFieldsExceptIdAndReturnsStatusOkAndBodyWithTaskUpdated() {
        var anotherFakeTaskStartDate = LocalDateTime.now()
                                                    .truncatedTo(ChronoUnit.MILLIS);

        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToBeUpdated = taskRepository.save(fakeTask);
        assertNotNull(taskToBeUpdated);

        // When & Then
        var idToUpdate = taskToBeUpdated.id();
        var taskToUpdate = new TaskDTO(UUID.randomUUID(), fakeTaskTitle + " 2", fakeTaskDescription + " 2", anotherFakeTaskStartDate);

        var response = webTestClient.put()
                                    .uri(TASKS_BY_ID_PATH, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .value(task -> assertThat(task.id(), equalTo(taskToBeUpdated.id())))
                                    .value(task -> assertThat(task.title(), equalTo(fakeTaskTitle + " 2")))
                                    .value(task -> assertThat(task.description(), equalTo(fakeTaskDescription + " 2")))
                                    .value(task -> assertThat(task.startDateTime(), equalTo(anotherFakeTaskStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Task(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = taskRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(actual, expected.get());
    }

    @Test
    @DisplayName("GIVEN id exists And task is valid WHEN update a task THEN updates all fields of the task with the given id with the given task data And returns HTTP response with status OK And the body with the task updated")
    void IdExistsAndTaskIsValid_UpdateTask_UpdatesAllTaskFieldsAndReturnsStatusOkAndBodyWithTaskUpdated() {
        var anotherFakeTaskStartDate = LocalDateTime.now()
                                                    .truncatedTo(ChronoUnit.MILLIS);

        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToBeUpdated = taskRepository.save(fakeTask);
        assertNotNull(taskToBeUpdated);

        // When & Then
        var idToUpdate = taskToBeUpdated.id();
        var taskToUpdate = new TaskDTO(null, fakeTaskTitle + " 2", fakeTaskDescription + " 2", anotherFakeTaskStartDate);

        var response = webTestClient.put()
                                    .uri(TASKS_BY_ID_PATH, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(taskToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(TaskDTO.class)
                                    .value(task -> assertThat(task.id(), equalTo(taskToBeUpdated.id())))
                                    .value(task -> assertThat(task.title(), equalTo(fakeTaskTitle + " 2")))
                                    .value(task -> assertThat(task.description(), equalTo(fakeTaskDescription + " 2")))
                                    .value(task -> assertThat(task.startDateTime(), equalTo(anotherFakeTaskStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Task(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = taskRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(actual, expected.get());
    }

    // DeleteTaskById
    @Test
    @DisplayName("GIVEN id does not exists WHEN delete a task by id THEN does not delete any task And returns HTTP response with status NOT_FOUND And an empty body")
    void IdNotExists_DeleteTaskById_DoesNotDeleteTaskAndReturnsStatusNotFoundAndEmptyBody() {
        // When & Then
        var idToDelete = UUID.randomUUID();

        webTestClient.delete()
                     .uri(TASKS_BY_ID_PATH, idToDelete)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN id exists WHEN delete a task by id THEN deletes the task with the given id And returns HTTP response with status NO_CONTENT And an empty body")
    void IdExists_DeleteTaskById_DeletesTaskAndReturnsStatusNoContentAndEmptyBody() {
        // Given
        var fakeTask = new Task(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        var taskToBeDeleted = taskRepository.save(fakeTask);
        assertNotNull(taskToBeDeleted);

        // When & Then
        var idToDelete = taskToBeDeleted.id();

        webTestClient.delete()
                     .uri(TASKS_BY_ID_PATH, idToDelete)
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
