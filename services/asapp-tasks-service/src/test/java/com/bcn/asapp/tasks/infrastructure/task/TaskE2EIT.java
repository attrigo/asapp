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

import static com.bcn.asapp.tasks.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestTaskFactory.defaultTestTask;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestTaskFactory.testTaskBuilder;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
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
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskRepository;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class TaskE2EIT {

    @Autowired
    private JdbcTaskRepository taskRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String accessToken = defaultTestEncodedAccessToken();

    private final String bearerToken = "Bearer " + accessToken;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();

        redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + accessToken, "");
    }

    @Nested
    class GetTaskById {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.fromString("a7f3e5d2-6b9c-4a81-9e3f-2d4b8c7e1a9f");

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
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.fromString("a7f3e5d2-6b9c-4a81-9e3f-2d4b8c7e1a9f");

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
        void ReturnsStatusOKAndBodyWithFoundTask_TaskExists() {
            // Given
            var task = defaultTestTask();
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
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");

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
        void ReturnsStatusOKAndEmptyBody_TasksNotExistForUserId() {
            // Given
            var userIdPath = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");

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
        void ReturnsStatusOKAndBodyWithFoundTasks_TasksExistsForUserId() {
            // Given
            var userId = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");

            var task1 = testTaskBuilder().withUserId(userId)
                                         .build();
            var task2 = testTaskBuilder().withUserId(userId)
                                         .build();
            var task3 = testTaskBuilder().withUserId(userId)
                                         .build();
            var taskCreated1 = taskRepository.save(task1);
            var taskCreated2 = taskRepository.save(task2);
            var taskCreated3 = taskRepository.save(task3);
            assertThat(taskCreated1).isNotNull();
            assertThat(taskCreated2).isNotNull();
            assertThat(taskCreated3).isNotNull();

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
            var expectedResponse1 = new GetAllTasksResponse(taskCreated1.id(), taskCreated1.userId(), taskCreated1.title(), taskCreated1.description(),
                    taskCreated1.startDate(), taskCreated1.endDate());
            var expectedResponse2 = new GetAllTasksResponse(taskCreated2.id(), taskCreated2.userId(), taskCreated2.title(), taskCreated2.description(),
                    taskCreated2.startDate(), taskCreated2.endDate());
            var expectedResponse3 = new GetAllTasksResponse(taskCreated3.id(), taskCreated3.userId(), taskCreated3.title(), taskCreated3.description(),
                    taskCreated3.startDate(), taskCreated3.endDate());
            assertThat(response).hasSize(3)
                                .containsExactlyInAnyOrder(expectedResponse1, expectedResponse2, expectedResponse3);
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
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
        void ReturnsStatusOKAndEmptyBody_TasksNotExist() {
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
        void ReturnsStatusOKAndBodyWithFoundTasks_TasksExist() {
            // Given
            var task1 = testTaskBuilder().withTitle("Title1")
                                         .build();
            var task2 = testTaskBuilder().withTitle("Title2")
                                         .build();
            var task3 = testTaskBuilder().withTitle("Title3")
                                         .build();
            var taskCreated1 = taskRepository.save(task1);
            var taskCreated2 = taskRepository.save(task2);
            var taskCreated3 = taskRepository.save(task3);
            assertThat(taskCreated1).isNotNull();
            assertThat(taskCreated2).isNotNull();
            assertThat(taskCreated3).isNotNull();

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
            var expectedResponse1 = new GetAllTasksResponse(taskCreated1.id(), taskCreated1.userId(), taskCreated1.title(), taskCreated1.description(),
                    taskCreated1.startDate(), taskCreated1.endDate());
            var expectedResponse2 = new GetAllTasksResponse(taskCreated2.id(), taskCreated2.userId(), taskCreated2.title(), taskCreated2.description(),
                    taskCreated2.startDate(), taskCreated2.endDate());
            var expectedResponse3 = new GetAllTasksResponse(taskCreated3.id(), taskCreated3.userId(), taskCreated3.title(), taskCreated3.description(),
                    taskCreated3.startDate(), taskCreated3.endDate());
            assertThat(response).hasSize(3)
                                .containsExactlyInAnyOrder(expectedResponse1, expectedResponse2, expectedResponse3);
        }

    }

    @Nested
    class CreateTask {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            var userId = UUID.fromString("f8b4d2e9-7c5a-4f36-8d9b-1e3a7f4c6b8d");
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), "Title", "Description", startDate, endDate);

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
        void ReturnsStatusCreatedAndBodyWithTaskCreated() {
            // When
            var userId = UUID.fromString("f8b4d2e9-7c5a-4f36-8d9b-1e3a7f4c6b8d");
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), "Title", "Description", startDate, endDate);

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

            // Assert the task has been created
            var optionalActualTask = taskRepository.findById(response.taskId());
            assertThat(optionalActualTask).isNotEmpty()
                                          .get()
                                          .satisfies(actualTask -> {
                                              assertThat(actualTask.id()).isEqualTo(response.taskId());
                                              assertThat(actualTask.userId()).isEqualTo(UUID.fromString(createTaskRequestBody.userId()));
                                              assertThat(actualTask.title()).isEqualTo(createTaskRequestBody.title());
                                              assertThat(actualTask.description()).isEqualTo(createTaskRequestBody.description());
                                              assertThat(actualTask.startDate()).isEqualTo(createTaskRequestBody.startDate());
                                              assertThat(actualTask.endDate()).isEqualTo(createTaskRequestBody.endDate());
                                          });
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.fromString("b7f3a8d1-4e9c-4118-8f2b-6d9e3a5c7b1f");

            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "NewTitle", "NewDescription", newStartDate, newEndDate);

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
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.fromString("b7f3a8d1-4e9c-4118-8f2b-6d9e3a5c7b1f");

            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "NewTitle", "NewDescription", newStartDate, newEndDate);

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
        void ReturnsStatusOkAndBodyWithUpdatedTask_TaskExists() {
            // Given
            var task = defaultTestTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskIdPath = taskCreated.id();

            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "NewTitle", "NewDescription", newStartDate, newEndDate);

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

            // Assert the task has been updated
            var optionalActualTask = taskRepository.findById(response.taskId());
            assertThat(optionalActualTask).isNotEmpty()
                                          .get()
                                          .satisfies(actualTask -> {
                                              assertThat(actualTask.id()).isEqualTo(response.taskId());
                                              assertThat(actualTask.userId()).isEqualTo(UUID.fromString(updateTaskRequest.userId()));
                                              assertThat(actualTask.title()).isEqualTo(updateTaskRequest.title());
                                              assertThat(actualTask.description()).isEqualTo(updateTaskRequest.description());
                                              assertThat(actualTask.startDate()).isEqualTo(updateTaskRequest.startDate());
                                              assertThat(actualTask.endDate()).isEqualTo(updateTaskRequest.endDate());
                                          });
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            var taskIdPath = UUID.fromString("a4e7f1c9-8b2d-464d-9e7a-5f3c8d1b6e4a");

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
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // When & Then
            var taskIdPath = UUID.fromString("a4e7f1c9-8b2d-464d-9e7a-5f3c8d1b6e4a");

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
        void ReturnsStatusNoContentAndEmptyBody_TaskExists() {
            // Given
            var task = defaultTestTask();
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
            // Assert the task has been deleted
            var optionalActualTask = taskRepository.findById(taskCreated.id());
            assertThat(optionalActualTask).isEmpty();
        }

    }

}
