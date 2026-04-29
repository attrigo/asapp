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

package com.bcn.asapp.tasks.infrastructure.task;

import static com.bcn.asapp.tasks.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aJdbcTask;
import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aTaskBuilder;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.tasks.AsappTasksServiceApplication;
import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskEntity;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskRepository;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests end-to-end task management workflows including CRUD operations and ownership queries.
 * <p>
 * Coverage:
 * <li>Rejects all operations without valid JWT authentication</li>
 * <li>Retrieves task by identifier returning 404 when not found, task when exists</li>
 * <li>Retrieves tasks by user ownership returning empty or collection</li>
 * <li>Retrieves all tasks across users returning empty or collection</li>
 * <li>Creates task persisting to database and returning assigned identifier</li>
 * <li>Updates existing task persisting changes and returning updated data</li>
 * <li>Deletes existing task removing from database</li>
 * <li>Tests complete flow: HTTP → Security → Controller → Service → Repository → Database</li>
 */
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class TaskE2EIT {

    @Autowired
    private JdbcTaskRepository taskRepository;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String encodedAccessToken = encodedAccessToken();

    private final String bearerToken = "Bearer " + encodedAccessToken;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();

        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
    }

    @Nested
    class GetTaskById {

        @Test
        void ReturnsStatusOKAndBodyWithFoundTask_TaskExists() {
            // Given
            var createdTask = createTask();
            var taskId = createdTask.id();
            var response = new GetTaskByIdResponse(createdTask.id(), createdTask.userId(), createdTask.title(), createdTask.description(),
                    createdTask.startDate(), createdTask.endDate());

            // When
            var actual = restTestClient.get()
                                       .uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
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
            assertThat(actual).isEqualTo(response);
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // Given
            var taskId = UUID.fromString("a7f3e5d2-6b9c-4a81-9e3f-2d4b8c7e1a9f");

            // When & Then
            restTestClient.get()
                          .uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isNotFound()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var taskId = UUID.fromString("a7f3e5d2-6b9c-4a81-9e3f-2d4b8c7e1a9f");

            // When & Then
            restTestClient.get()
                          .uri(TASKS_GET_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ReturnsStatusOKAndBodyWithFoundTasks_TasksExistForUserId() {
            // Given
            var userId = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");
            var task1 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var task2 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var task3 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var createdTask1 = createTask(task1);
            var createdTask2 = createTask(task2);
            var createdTask3 = createTask(task3);
            var response1 = new GetAllTasksResponse(createdTask1.id(), createdTask1.userId(), createdTask1.title(), createdTask1.description(),
                    createdTask1.startDate(), createdTask1.endDate());
            var response2 = new GetAllTasksResponse(createdTask2.id(), createdTask2.userId(), createdTask2.title(), createdTask2.description(),
                    createdTask2.startDate(), createdTask2.endDate());
            var response3 = new GetAllTasksResponse(createdTask3.id(), createdTask3.userId(), createdTask3.title(), createdTask3.description(),
                    createdTask3.startDate(), createdTask3.endDate());

            // When
            var actual = restTestClient.get()
                                       .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllTasksResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(response1, response2, response3);
        }

        @Test
        void ReturnsStatusOKAndEmptyBody_TasksNotExistForUserId() {
            // Given
            var userId = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");

            // When
            var actual = restTestClient.get()
                                       .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllTasksResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("c9e2a5f8-4d7b-4c63-9a8e-7b3f2d9c5e1a");

            // When & Then
            restTestClient.get()
                          .uri(TASKS_GET_BY_USER_ID_FULL_PATH, userId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void ReturnsStatusOKAndBodyWithFoundTasks_TasksExist() {
            // Given
            var task1 = aTaskBuilder().withTitle("Title1")
                                      .buildJdbc();
            var task2 = aTaskBuilder().withTitle("Title2")
                                      .buildJdbc();
            var task3 = aTaskBuilder().withTitle("Title3")
                                      .buildJdbc();
            var createdTask1 = createTask(task1);
            var createdTask2 = createTask(task2);
            var createdTask3 = createTask(task3);
            var response1 = new GetAllTasksResponse(createdTask1.id(), createdTask1.userId(), createdTask1.title(), createdTask1.description(),
                    createdTask1.startDate(), createdTask1.endDate());
            var response2 = new GetAllTasksResponse(createdTask2.id(), createdTask2.userId(), createdTask2.title(), createdTask2.description(),
                    createdTask2.startDate(), createdTask2.endDate());
            var response3 = new GetAllTasksResponse(createdTask3.id(), createdTask3.userId(), createdTask3.title(), createdTask3.description(),
                    createdTask3.startDate(), createdTask3.endDate());

            // When
            var actual = restTestClient.get()
                                       .uri(TASKS_GET_ALL_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllTasksResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(response1, response2, response3);
        }

        @Test
        void ReturnsStatusOKAndEmptyBody_TasksNotExist() {
            // When
            var actual = restTestClient.get()
                                       .uri(TASKS_GET_ALL_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllTasksResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            restTestClient.get()
                          .uri(TASKS_GET_ALL_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class CreateTask {

        @Test
        void ReturnsStatusCreatedAndBodyWithTaskCreated_ValidTask() {
            // Given
            var userId = UUID.fromString("f8b4d2e9-7c5a-4f36-8d9b-1e3a7f4c6b8d");
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), "Title", "Description", startDate, endDate);

            // When
            var actual = restTestClient.post()
                                       .uri(TASKS_CREATE_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(createTaskRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isCreated()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(CreateTaskResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull()
                              .extracting(CreateTaskResponse::taskId)
                              .isNotNull();

            // Assert the task has been created
            var createdTask = taskRepository.findById(actual.taskId());
            assertThat(createdTask).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(createdTask.get().id()).as("id").isEqualTo(actual.taskId());
                softly.assertThat(createdTask.get().userId()).as("userId").isEqualTo(UUID.fromString(createTaskRequestBody.userId()));
                softly.assertThat(createdTask.get().title()).as("title").isEqualTo(createTaskRequestBody.title());
                softly.assertThat(createdTask.get().description()).as("description").isEqualTo(createTaskRequestBody.description());
                softly.assertThat(createdTask.get().startDate()).as("startDate").isEqualTo(createTaskRequestBody.startDate());
                softly.assertThat(createdTask.get().endDate()).as("endDate").isEqualTo(createTaskRequestBody.endDate());
                // @formatter:on
            });
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("f8b4d2e9-7c5a-4f36-8d9b-1e3a7f4c6b8d");
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var createTaskRequestBody = new CreateTaskRequest(userId.toString(), "Title", "Description", startDate, endDate);

            // When & Then
            restTestClient.post()
                          .uri(TASKS_CREATE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(createTaskRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class UpdateTaskById {

        @Test
        void ReturnsStatusOkAndBodyWithUpdatedTask_TaskExists() {
            // Given
            var createdTask = createTask();
            var taskId = createdTask.id();
            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "New Title", "New Description", newStartDate, newEndDate);

            // When
            var actual = restTestClient.put()
                                       .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(updateTaskRequest)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(UpdateTaskResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull()
                              .extracting(UpdateTaskResponse::taskId)
                              .isEqualTo(createdTask.id());

            // Assert the task has been updated
            var updatedTask = taskRepository.findById(actual.taskId());
            assertThat(updatedTask).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(updatedTask.get().id()).as("id").isEqualTo(actual.taskId());
                softly.assertThat(updatedTask.get().userId()).as("userId").isEqualTo(UUID.fromString(updateTaskRequest.userId()));
                softly.assertThat(updatedTask.get().title()).as("title").isEqualTo(updateTaskRequest.title());
                softly.assertThat(updatedTask.get().description()).as("description").isEqualTo(updateTaskRequest.description());
                softly.assertThat(updatedTask.get().startDate()).as("startDate").isEqualTo(updateTaskRequest.startDate());
                softly.assertThat(updatedTask.get().endDate()).as("endDate").isEqualTo(updateTaskRequest.endDate());
                // @formatter:on
            });
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // Given
            var taskId = UUID.fromString("b7f3a8d1-4e9c-4118-8f2b-6d9e3a5c7b1f");
            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "New Title", "New Description", newStartDate, newEndDate);

            // When & Then
            restTestClient.put()
                          .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(updateTaskRequest)
                          .exchange()
                          .expectStatus()
                          .isNotFound()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var taskId = UUID.fromString("b7f3a8d1-4e9c-4118-8f2b-6d9e3a5c7b1f");
            var newUserId = UUID.fromString("c4d9e2f8-5a7b-4209-9a3c-8e1f7b4d6a2e");
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var updateTaskRequest = new UpdateTaskRequest(newUserId.toString(), "New Title", "New Description", newStartDate, newEndDate);

            // When & Then
            restTestClient.put()
                          .uri(TASKS_UPDATE_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(updateTaskRequest)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsStatusNoContentAndEmptyBody_TaskExists() {
            // Given
            var createdTask = createTask();
            var taskId = createdTask.id();

            // When
            restTestClient.delete()
                          .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isNoContent()
                          .expectBody()
                          .isEmpty();

            // Then
            // Assert the task has been deleted
            var deletedTask = taskRepository.findById(createdTask.id());
            assertThat(deletedTask).isEmpty();
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_TaskNotExists() {
            // Given
            var taskId = UUID.fromString("a4e7f1c9-8b2d-464d-9e7a-5f3c8d1b6e4a");

            // When & Then
            restTestClient.delete()
                          .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isNotFound()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var taskId = UUID.fromString("a4e7f1c9-8b2d-464d-9e7a-5f3c8d1b6e4a");

            // When & Then
            restTestClient.delete()
                          .uri(TASKS_DELETE_BY_ID_FULL_PATH, taskId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    // Test Data Creation Helpers

    private JdbcTaskEntity createTask() {
        var task = aJdbcTask();
        return createTask(task);
    }

    private JdbcTaskEntity createTask(JdbcTaskEntity task) {
        var createdTask = taskRepository.save(task);
        assertThat(createdTask).isNotNull();
        return createdTask;
    }

}
