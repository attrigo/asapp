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

package com.bcn.asapp.tasks.infrastructure.task;

import static com.bcn.asapp.tasks.testutil.TestDataFaker.EncodedJwtDataFaker.defaultFakeEncodedAccessToken;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_DESCRIPTION;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_END_DATE;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_START_DATE;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.DEFAULT_FAKE_TITLE;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.defaultFakeTask;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.fakeTaskBuilder;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.tasks.AsappTasksServiceApplication;
import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.out.TaskJdbcRepository;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class TaskE2EIT {

    @Autowired
    private TaskJdbcRepository taskRepository;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();

        bearerToken = "Bearer " + defaultFakeEncodedAccessToken();
    }

    @Nested
    class GetTaskById {

        @Test
        void DoesNotGetTaskAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.randomUUID();

            webTestClient.get()
                         .uri(TASKS_GET_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotGetTaskAndReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.randomUUID();

            webTestClient.get()
                         .uri(TASKS_GET_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void GetsTaskAndReturnsStatusOKAndBodyWithFoundTask_TaskExists() {
            // Given
            var task = defaultFakeTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskIdPath = taskCreated.id();

            var response = webTestClient.get()
                                        .uri(TASKS_GET_BY_ID_FULL_PATH, taskIdPath)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(GetTaskByIdResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            // Assert API response
            var expectedResponse = new GetTaskByIdResponse(taskCreated.id(), taskCreated.userId(), taskCreated.title(), taskCreated.description(),
                    taskCreated.startDate(), taskCreated.endDate());
            assertThat(response).isEqualTo(expectedResponse);
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void DoesNotGetTasksAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.randomUUID();

            webTestClient.get()
                         .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotGetTasksAndReturnsStatusOKAndEmptyBody_ThereAreNotTasksWithUserId() {
            // Given
            var userIdPath = UUID.randomUUID();

            // When
            var response = webTestClient.get()
                                        .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userIdPath)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllTasksResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            assertThat(response).isEmpty();
        }

        @Test
        void GetsTasksAndReturnsStatusOKAndBodyWithFoundTasks_ThereAreTasksWithUserId() {
            // Given
            var userId = UUID.randomUUID();

            var firstTask = fakeTaskBuilder().withUserId(userId)
                                             .build();
            var secondTask = fakeTaskBuilder().withUserId(userId)
                                              .build();
            var thirdTask = fakeTaskBuilder().withUserId(userId)
                                             .build();
            var firstTaskCreated = taskRepository.save(firstTask);
            var secondTaskCreated = taskRepository.save(secondTask);
            var thirdTaskCreated = taskRepository.save(thirdTask);
            assertThat(firstTaskCreated).isNotNull();
            assertThat(secondTaskCreated).isNotNull();
            assertThat(thirdTaskCreated).isNotNull();

            // When
            var response = webTestClient.get()
                                        .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userId)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllTasksResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            var firstTaskResponse = new GetAllTasksResponse(firstTaskCreated.id(), firstTaskCreated.userId(), firstTaskCreated.title(),
                    firstTaskCreated.description(), firstTaskCreated.startDate(), firstTaskCreated.endDate());
            var secondTaskResponse = new GetAllTasksResponse(secondTaskCreated.id(), secondTaskCreated.userId(), secondTaskCreated.title(),
                    secondTaskCreated.description(), secondTaskCreated.startDate(), secondTaskCreated.endDate());
            var thirdTaskResponse = new GetAllTasksResponse(thirdTaskCreated.id(), thirdTaskCreated.userId(), thirdTaskCreated.title(),
                    thirdTaskCreated.description(), thirdTaskCreated.startDate(), thirdTaskCreated.endDate());
            assertThat(response).hasSize(3)
                                .contains(firstTaskResponse, secondTaskResponse, thirdTaskResponse);
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void DoesNotGetTasksAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            webTestClient.get()
                         .uri(TASKS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotGetTasksAndReturnsStatusOKAndEmptyBody_ThereAreNotTasks() {
            // When
            var response = webTestClient.get()
                                        .uri(TASKS_GET_ALL_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllTasksResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            assertThat(response).isEmpty();
        }

        @Test
        void GetsAllTasksAndReturnsStatusOKAndBodyWithFoundTasks_ThereAreTasks() {
            // Given
            var firstTask = defaultFakeTask();
            var secondTask = defaultFakeTask();
            var thirdTask = defaultFakeTask();
            var firstTaskCreated = taskRepository.save(firstTask);
            var secondTaskCreated = taskRepository.save(secondTask);
            var thirdTaskCreated = taskRepository.save(thirdTask);
            assertThat(firstTaskCreated).isNotNull();
            assertThat(secondTaskCreated).isNotNull();
            assertThat(thirdTaskCreated).isNotNull();

            // When
            var response = webTestClient.get()
                                        .uri(TASKS_GET_ALL_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllTasksResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            var firstTaskResponse = new GetAllTasksResponse(firstTaskCreated.id(), firstTaskCreated.userId(), firstTaskCreated.title(),
                    firstTaskCreated.description(), firstTaskCreated.startDate(), firstTaskCreated.endDate());
            var secondTaskResponse = new GetAllTasksResponse(secondTaskCreated.id(), secondTaskCreated.userId(), secondTaskCreated.title(),
                    secondTaskCreated.description(), secondTaskCreated.startDate(), secondTaskCreated.endDate());
            var thirdTaskResponse = new GetAllTasksResponse(thirdTaskCreated.id(), thirdTaskCreated.userId(), thirdTaskCreated.title(),
                    thirdTaskCreated.description(), thirdTaskCreated.startDate(), thirdTaskCreated.endDate());
            assertThat(response).hasSize(3)
                                .contains(firstTaskResponse, secondTaskResponse, thirdTaskResponse);
        }

    }

    @Nested
    class CreateTask {

        @Test
        void DoesNotCreateTaskAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            var userId = UUID.randomUUID();
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE,
                    DEFAULT_FAKE_END_DATE);

            // When & Then
            webTestClient.post()
                         .uri(TASKS_CREATE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(createTaskRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void CreatesTaskAndReturnsStatusCreatedAndBodyWithTaskCreated() {
            // When
            var userId = UUID.randomUUID();
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE,
                    DEFAULT_FAKE_END_DATE);

            var response = webTestClient.post()
                                        .uri(TASKS_CREATE_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(createTaskRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isCreated()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(CreateTaskResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            // Assert API response
            assertThat(response).isNotNull()
                                .extracting(CreateTaskResponse::taskId)
                                .isNotNull();

            // Assert task has been created
            var optionalActualTask = taskRepository.findById(response.taskId());
            assertThat(optionalActualTask).isNotEmpty()
                                          .get()
                                          .satisfies(taskEntity -> {
                                              assertThat(taskEntity.id()).isEqualTo(response.taskId());
                                              assertThat(taskEntity.userId()).isEqualTo(userId);
                                              assertThat(taskEntity.title()).isEqualTo(DEFAULT_FAKE_TITLE);
                                              assertThat(taskEntity.description()).isEqualTo(DEFAULT_FAKE_DESCRIPTION);
                                              assertThat(taskEntity.startDate()).isEqualTo(DEFAULT_FAKE_START_DATE);
                                              assertThat(taskEntity.endDate()).isEqualTo(DEFAULT_FAKE_END_DATE);
                                          });
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void DoesNotUpdateTaskAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var newUserId = UUID.randomUUID();
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE,
                    DEFAULT_FAKE_END_DATE);

            webTestClient.put()
                         .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(updateTaskRequest)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotUpdateTaskAndReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.randomUUID();
            var newUserId = UUID.randomUUID();
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), DEFAULT_FAKE_TITLE, DEFAULT_FAKE_DESCRIPTION, DEFAULT_FAKE_START_DATE,
                    DEFAULT_FAKE_END_DATE);

            webTestClient.put()
                         .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(updateTaskRequest)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void UpdatesTaskAndReturnsStatusOkAndBodyWithUpdatedTask_TaskExists() {
            // Given
            var task = defaultFakeTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskIdPath = taskCreated.id();
            var newUserId = UUID.randomUUID();
            var newStartDate = Instant.now()
                                      .truncatedTo(ChronoUnit.SECONDS);
            var newEndDate = newStartDate.plus(1, ChronoUnit.HOURS);
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "new_title", "new_description", newStartDate, newEndDate);

            var response = webTestClient.put()
                                        .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskIdPath)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(updateTaskRequest)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UpdateTaskResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            // Assert API response
            assertThat(response).isNotNull()
                                .extracting(UpdateTaskResponse::taskId)
                                .isEqualTo(taskCreated.id());

            // Assert task has been updated
            var optionalActualTask = taskRepository.findById(response.taskId());
            assertThat(optionalActualTask).isNotEmpty()
                                          .get()
                                          .satisfies(taskEntity -> {
                                              assertThat(taskEntity.id()).isEqualTo(response.taskId());
                                              assertThat(taskEntity.userId()).isEqualTo(UUID.fromString(updateTaskRequest.userId()));
                                              assertThat(taskEntity.title()).isEqualTo(updateTaskRequest.title());
                                              assertThat(taskEntity.description()).isEqualTo(updateTaskRequest.description());
                                              assertThat(taskEntity.startDate()).isEqualTo(updateTaskRequest.startDate());
                                              assertThat(taskEntity.endDate()).isEqualTo(updateTaskRequest.endDate());
                                          });
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void DoesNotDeleteTaskAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.randomUUID();

            webTestClient.delete()
                         .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotDeleteTaskAndReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.randomUUID();

            webTestClient.delete()
                         .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DeletesTaskAndReturnsStatusNoContentAndEmptyBody_TaskExists() {
            // Given
            var task = defaultFakeTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskIdPath = taskCreated.id();

            webTestClient.delete()
                         .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNoContent()
                         .expectBody()
                         .isEmpty();

            // Then
            // Assert task has been deleted
            var optionalActualTask = taskRepository.findById(taskCreated.id());
            assertThat(optionalActualTask).isEmpty();
        }

        @Test
        void DeletesTaskAndReturnsStatusNoContentAndEmptyBody_TaskExistsAndHasBeenAuthenticated() {
            // Given
            var task = defaultFakeTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskIdPath = taskCreated.id();

            webTestClient.delete()
                         .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNoContent()
                         .expectBody()
                         .isEmpty();

            // Then
            // Assert task has been deleted
            var optionalActualTask = taskRepository.findById(taskCreated.id());
            assertThat(optionalActualTask).isEmpty();
        }

    }

}
