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

package com.bcn.asapp.users.infrastructure.user;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static com.bcn.asapp.users.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aJdbcUser;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUserBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mockserver.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests end-to-end user management workflows including CRUD operations and task enrichment.
 * <p>
 * Coverage:
 * <li>Rejects all operations without valid JWT authentication</li>
 * <li>Retrieves user by identifier returning 404 when not found, user when exists</li>
 * <li>Retrieves user with task enrichment via external gateway (graceful degradation on failure)</li>
 * <li>Retrieves all users returning empty or collection</li>
 * <li>Creates user persisting to database and returning assigned identifier</li>
 * <li>Updates existing user persisting changes and returning updated data</li>
 * <li>Deletes existing user removing from database</li>
 * <li>Tests complete flow: HTTP → Security → Controller → Service → Repository → Database</li>
 * <li>Tests external service integration using MockServer for task service calls</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class UserE2EIT {

    @Container
    static final MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
        registry.add("asapp.client.tasks.base-url", mockServerContainer::getEndpoint);
    }

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String encodedAccessToken = encodedAccessToken();

    private final String bearerToken = "Bearer " + encodedAccessToken;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();
        mockServerClient.reset();

        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
    }

    @Nested
    class GetUserById {

        @Test
        void ReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsWithoutTasks(CapturedOutput output) {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), Collections.emptyList());

            mockRequestToGetTasksByUserIdWithOkResponse(userId, Collections.emptyList());

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(GetUserByIdResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEqualTo(response);

            // Assert no warning logs appear for a successful empty response
            assertThat(output.getAll()).doesNotContain("Failed to retrieve tasks")
                                       .doesNotContain("Returning empty list");
        }

        @Test
        void ReturnsStatusOKAndBodyWithFoundUserWithTasks_UserExistsWithTasks() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var taskId1 = UUID.fromString("d3e4f5a6-b7c8-4d9e-0f1a-2b3c4d5e6f7a");
            var taskId2 = UUID.fromString("e4f5a6b7-c8d9-4e0f-1a2b-3c4d5e6f7a8b");
            var taskId3 = UUID.fromString("f5a6b7c8-d9e0-4f1a-2b3c-4d5e6f7a8b9c");
            var taskIds = List.of(taskId1, taskId2, taskId3);
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), taskIds);

            mockRequestToGetTasksByUserIdWithOkResponse(userId, taskIds);

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(GetUserByIdResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEqualTo(response);
        }

        @Test
        void ReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsAndTasksServiceFails(CapturedOutput output) {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.firstName(), createdUser.lastName(), createdUser.email(),
                    createdUser.phoneNumber(), Collections.emptyList());

            mockRequestToGetTasksByUserIdWithServerErrorResponse(userId);

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(GetUserByIdResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEqualTo(response);

            // Assert warning logs appear due to service failure
            assertThat(output.getAll()).contains("Failed to retrieve tasks for user " + createdUser.id())
                                       .contains("Returning empty list");
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e");

            // When & Then
            restTestClient.get()
                          .uri(USERS_GET_BY_ID_FULL_PATH, userId)
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
            var userId = UUID.fromString("b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e");

            // When & Then
            restTestClient.get()
                          .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void ReturnsStatusOKAndBodyWithFoundUsers_UsersExist() {
            // Given
            var user1 = aUserBuilder().withEmail("user1@asapp.com")
                                      .buildJdbc();
            var user2 = aUserBuilder().withEmail("user2@asapp.com")
                                      .buildJdbc();
            var user3 = aUserBuilder().withEmail("user3@asapp.com")
                                      .buildJdbc();
            var createdUser1 = createUser(user1);
            var createdUser2 = createUser(user2);
            var createdUser3 = createUser(user3);
            var response1 = new GetAllUsersResponse(createdUser1.id(), createdUser1.firstName(), createdUser1.lastName(), createdUser1.email(),
                    createdUser1.phoneNumber());
            var response2 = new GetAllUsersResponse(createdUser2.id(), createdUser2.firstName(), createdUser2.lastName(), createdUser2.email(),
                    createdUser2.phoneNumber());
            var response3 = new GetAllUsersResponse(createdUser3.id(), createdUser3.firstName(), createdUser3.lastName(), createdUser3.email(),
                    createdUser3.phoneNumber());

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_ALL_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllUsersResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(response1, response2, response3);
        }

        @Test
        void ReturnsStatusOKAndEmptyBody_UsersNotExist() {
            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_ALL_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(new ParameterizedTypeReference<List<GetAllUsersResponse>>() {})
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            restTestClient.get()
                          .uri(USERS_GET_ALL_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class CreateUser {

        @Test
        void ReturnsStatusCreatedAndBodyWithUserCreated_ValidUser() {
            // Given
            var createUserRequestBody = new CreateUserRequest("FirstName", "LastName", "user@asapp.com", "555 555 555");

            // When
            var actual = restTestClient.post()
                                       .uri(USERS_CREATE_FULL_PATH)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(createUserRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isCreated()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(CreateUserResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull()
                              .extracting(CreateUserResponse::userId)
                              .isNotNull();

            // Assert the user has been created
            var createdUser = userRepository.findById(actual.userId());
            assertThat(createdUser).as("user optional")
                                   .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(createdUser.get().id()).as("id").isEqualTo(actual.userId());
                softly.assertThat(createdUser.get().firstName()).as("firstName").isEqualTo(createUserRequestBody.firstName());
                softly.assertThat(createdUser.get().lastName()).as("lastName").isEqualTo(createUserRequestBody.lastName());
                softly.assertThat(createdUser.get().email()).as("email").isEqualTo(createUserRequestBody.email());
                softly.assertThat(createdUser.get().phoneNumber()).as("phoneNumber") .isEqualTo(createUserRequestBody.phoneNumber());
                // @formatter:on
            });
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var createUserRequestBody = new CreateUserRequest("FirstName", "LastName", "user@asapp.com", "555 555 555");

            // When & Then
            restTestClient.post()
                          .uri(USERS_CREATE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(createUserRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsStatusOkAndBodyWithUpdatedUser_UserExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var updateUserRequest = new UpdateUserRequest("New FirstName", "New LastName", "new_user@asapp.com", "666 666 666");

            // When
            var actual = restTestClient.put()
                                       .uri(USERS_UPDATE_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(updateUserRequest)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(UpdateUserResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull()
                              .extracting(UpdateUserResponse::userId)
                              .isEqualTo(createdUser.id());

            // Assert the user has been updated
            var updatedUser = userRepository.findById(actual.userId());
            assertThat(updatedUser).as("user optional")
                                   .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(updatedUser.get().id()).as("id").isEqualTo(actual.userId());
                softly.assertThat(updatedUser.get().firstName()).as("firstName").isEqualTo(updateUserRequest.firstName());
                softly.assertThat(updatedUser.get().lastName()).as("lastName").isEqualTo(updateUserRequest.lastName());
                softly.assertThat(updatedUser.get().email()).as("email").isEqualTo(updateUserRequest.email());
                softly.assertThat(updatedUser.get().phoneNumber()).as("phoneNumber").isEqualTo(updateUserRequest.phoneNumber());
                // @formatter:on
            });
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("a6b7c8d9-e0f1-4a2b-3c4d-5e6f7a8b9c0d");
            var updateUserRequest = new UpdateUserRequest("New FirstName", "New LastName", "new_user@asapp.com", "666 666 666");

            // When & Then
            restTestClient.put()
                          .uri(USERS_UPDATE_BY_ID_FULL_PATH, userId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(updateUserRequest)
                          .exchange()
                          .expectStatus()
                          .isNotFound()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("a6b7c8d9-e0f1-4a2b-3c4d-5e6f7a8b9c0d");
            var updateUserRequest = new UpdateUserRequest("New FirstName", "New LastName", "new_user@asapp.com", "666 666 666");

            // When & Then
            restTestClient.put()
                          .uri(USERS_UPDATE_BY_ID_FULL_PATH, userId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(updateUserRequest)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsStatusNoContentAndEmptyBody_UserExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();

            // When
            restTestClient.delete()
                          .uri(USERS_DELETE_BY_ID_FULL_PATH, userId)
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isNoContent()
                          .expectBody()
                          .isEmpty();

            // Then
            // Assert the user has been deleted
            var deletedUser = userRepository.findById(createdUser.id());
            assertThat(deletedUser).isEmpty();
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("c8d9e0f1-a2b3-4c4d-5e6f-7a8b9c0d1e2f");

            // When & Then
            restTestClient.delete()
                          .uri(USERS_DELETE_BY_ID_FULL_PATH, userId)
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
            var userId = UUID.fromString("c8d9e0f1-a2b3-4c4d-5e6f-7a8b9c0d1e2f");

            // When & Then
            restTestClient.delete()
                          .uri(USERS_DELETE_BY_ID_FULL_PATH, userId)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

    }

    // Test Data Creation Helpers

    private JdbcUserEntity createUser() {
        var user = aJdbcUser();
        return createUser(user);
    }

    private JdbcUserEntity createUser(JdbcUserEntity user) {
        var createdUser = userRepository.save(user);
        assertThat(createdUser).isNotNull();
        return createdUser;
    }

    // Mock Helpers

    private void mockRequestToGetTasksByUserIdWithOkResponse(UUID userId, List<UUID> taskIds) {
        try {
            var responseBody = taskIds.stream()
                                      .map(taskId -> "{\"task_id\":\"%s\"}".formatted(taskId))
                                      .toList();
            var jsonResponse = "[" + String.join(",", responseBody) + "]";

            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.toString());
            var times = once();
            mockServerClient.when(request, times)
                            .respond(response().withStatusCode(200)
                                               .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                               .withBody(jsonResponse));
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock tasks service response", e);
        }
    }

    private void mockRequestToGetTasksByUserIdWithServerErrorResponse(UUID userId) {
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                               .withPathParameter("id", userId.toString());
        var times = once();
        mockServerClient.when(request, times)
                        .respond(response().withStatusCode(500)
                                           .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
    }

}
