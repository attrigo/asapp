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

package com.bcn.asapp.users.infrastructure.user;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static com.bcn.asapp.users.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.users.testutil.TestFactory.TestUserFactory.defaultTestUser;
import static com.bcn.asapp.users.testutil.TestFactory.TestUserFactory.testUserBuilder;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.users.infrastructure.user.out.UserJdbcRepository;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class UserE2EIT {

    @Container
    static MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
        registry.add("asapp.client.tasks.base-url", mockServerContainer::getEndpoint);
    }

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();
        mockServerClient.reset();

        bearerToken = "Bearer " + defaultTestEncodedAccessToken();
    }

    @Nested
    class GetUserById {

        @Test
        void DoesNotGetUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e");

            webTestClient.get()
                         .uri(USERS_GET_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void GetsUserAndReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsAndTasksServiceFails(CapturedOutput output) {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            mockRequestToGetTasksByUserIdWithServerErrorResponse(userCreated.id());

            // When
            var userIdPath = userCreated.id();

            var response = webTestClient.get()
                                        .uri(USERS_GET_BY_ID_FULL_PATH, userIdPath)
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
            // Assert API response - graceful degradation returns empty list
            var expectedResponse = new GetUserByIdResponse(userCreated.id(), userCreated.firstName(), userCreated.lastName(), userCreated.email(),
                    userCreated.phoneNumber(), Collections.emptyList());
            assertThat(response).isEqualTo(expectedResponse);

            // Assert warning logs appear due to service failure
            assertThat(output.getAll()).contains("Failed to retrieve tasks for user " + userCreated.id())
                                       .contains("Returning empty list");
        }

        @Test
        void DoesNotGetUserAndReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // When & Then
            var userIdPath = UUID.fromString("b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e");

            webTestClient.get()
                         .uri(USERS_GET_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void GetsUserAndReturnsStatusOKAndBodyWithFoundUserWithoutTasks_UserExistsWithoutTasks(CapturedOutput output) {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            mockRequestToGetTasksByUserIdWithOkResponse(userCreated.id(), Collections.emptyList());

            // When
            var userIdPath = userCreated.id();

            var response = webTestClient.get()
                                        .uri(USERS_GET_BY_ID_FULL_PATH, userIdPath)
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
            // Assert API response
            var expectedResponse = new GetUserByIdResponse(userCreated.id(), userCreated.firstName(), userCreated.lastName(), userCreated.email(),
                    userCreated.phoneNumber(), Collections.emptyList());
            assertThat(response).isEqualTo(expectedResponse);

            // Assert NO warning logs appear for a successful empty response
            assertThat(output.getAll()).doesNotContain("Failed to retrieve tasks")
                                       .doesNotContain("Returning empty list");
        }

        @Test
        void GetsUserAndReturnsStatusOKAndBodyWithFoundUserWithTasks_UserExistsWithTasks() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var taskId1 = UUID.fromString("d3e4f5a6-b7c8-4d9e-0f1a-2b3c4d5e6f7a");
            var taskId2 = UUID.fromString("e4f5a6b7-c8d9-4e0f-1a2b-3c4d5e6f7a8b");
            var taskId3 = UUID.fromString("f5a6b7c8-d9e0-4f1a-2b3c-4d5e6f7a8b9c");
            var taskIds = List.of(taskId1, taskId2, taskId3);

            mockRequestToGetTasksByUserIdWithOkResponse(userCreated.id(), taskIds);

            // When
            var userIdPath = userCreated.id();

            var response = webTestClient.get()
                                        .uri(USERS_GET_BY_ID_FULL_PATH, userIdPath)
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
            // Assert API response
            var expectedResponse = new GetUserByIdResponse(userCreated.id(), userCreated.firstName(), userCreated.lastName(), userCreated.email(),
                    userCreated.phoneNumber(), taskIds);
            assertThat(response).isEqualTo(expectedResponse);
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void DoesNotGetUsersAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            webTestClient.get()
                         .uri(USERS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotGetUsersAndReturnsStatusOKAndEmptyBody_ThereAreNotUsers() {
            // When
            var response = webTestClient.get()
                                        .uri(USERS_GET_ALL_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllUsersResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            assertThat(response).isEmpty();
        }

        @Test
        void GetsAllUsersAndReturnsStatusOKAndBodyWithFoundUsers_ThereAreUsers() {
            // Given
            var user1 = testUserBuilder().withEmail("user1@asapp.com")
                                         .build();
            var user2 = testUserBuilder().withEmail("user2@asapp.com")
                                         .build();
            var user3 = testUserBuilder().withEmail("user3@asapp.com")
                                         .build();
            var userCreated1 = userRepository.save(user1);
            var userCreated2 = userRepository.save(user2);
            var userCreated3 = userRepository.save(user3);
            assertThat(userCreated1).isNotNull();
            assertThat(userCreated2).isNotNull();
            assertThat(userCreated3).isNotNull();

            // When
            var response = webTestClient.get()
                                        .uri(USERS_GET_ALL_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBodyList(GetAllUsersResponse.class)
                                        .returnResult()
                                        .getResponseBody();
            // Then
            // Assert API response
            var userResponse1 = new GetAllUsersResponse(userCreated1.id(), userCreated1.firstName(), userCreated1.lastName(), userCreated1.email(),
                    userCreated1.phoneNumber());
            var userResponse2 = new GetAllUsersResponse(userCreated2.id(), userCreated2.firstName(), userCreated2.lastName(), userCreated2.email(),
                    userCreated2.phoneNumber());
            var userResponse3 = new GetAllUsersResponse(userCreated3.id(), userCreated3.firstName(), userCreated3.lastName(), userCreated3.email(),
                    userCreated3.phoneNumber());
            assertThat(response).hasSize(3)
                                .containsExactlyInAnyOrder(userResponse1, userResponse2, userResponse3);
        }

    }

    @Nested
    class CreateUser {

        @Test
        void DoesNotCreateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When
            var createUserRequestBody = new CreateUserRequest("FirstName", "LastName", "user@asapp.com", "555 555 555");

            // When & Then
            webTestClient.post()
                         .uri(USERS_CREATE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(createUserRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void CreatesUserAndReturnsStatusCreatedAndBodyWithUserCreated() {
            // When
            var createUserRequestBody = new CreateUserRequest("FirstName", "LastName", "user@asapp.com", "555 555 555");

            var response = webTestClient.post()
                                        .uri(USERS_CREATE_FULL_PATH)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(createUserRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isCreated()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(CreateUserResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            // Assert API response
            assertThat(response).isNotNull()
                                .extracting(CreateUserResponse::userId)
                                .isNotNull();

            // Assert the user has been created
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.firstName()).isEqualTo(createUserRequestBody.firstName());
                                              assertThat(userEntity.lastName()).isEqualTo(createUserRequestBody.lastName());
                                              assertThat(userEntity.email()).isEqualTo(createUserRequestBody.email());
                                              assertThat(userEntity.phoneNumber()).isEqualTo(createUserRequestBody.phoneNumber());
                                          });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void DoesNotUpdateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("a6b7c8d9-e0f1-4a2b-3c4d-5e6f7a8b9c0d");

            var updateUserRequest = new UpdateUserRequest("NewFirstName", "NewLastName", "new_user@asapp.com", "555-555-555");

            webTestClient.put()
                         .uri(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(updateUserRequest)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotUpdateUserAndReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // When & Then
            var userIdPath = UUID.fromString("a6b7c8d9-e0f1-4a2b-3c4d-5e6f7a8b9c0d");

            var updateUserRequest = new UpdateUserRequest("NewFirstName", "NewLastName", "new_user@asapp.com", "555-555-555");

            webTestClient.put()
                         .uri(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(updateUserRequest)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void UpdatesUserAndReturnsStatusOkAndBodyWithUpdatedUser_UserExists() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userIdPath = userCreated.id();

            var updateUserRequest = new UpdateUserRequest("NewFirstName", "NewLastName", "new_user@asapp.com", "555-555-555");

            var response = webTestClient.put()
                                        .uri(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(updateUserRequest)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UpdateUserResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            // Assert API response
            assertThat(response).isNotNull()
                                .extracting(UpdateUserResponse::userId)
                                .isEqualTo(userCreated.id());

            // Assert the user has been updated
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.firstName()).isEqualTo(updateUserRequest.firstName());
                                              assertThat(userEntity.lastName()).isEqualTo(updateUserRequest.lastName());
                                              assertThat(userEntity.email()).isEqualTo(updateUserRequest.email());
                                              assertThat(userEntity.phoneNumber()).isEqualTo(updateUserRequest.phoneNumber());
                                          });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void DoesNotDeleteUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("c8d9e0f1-a2b3-4c4d-5e6f-7a8b9c0d1e2f");

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DoesNotDeleteUserAndReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // When & Then
            var userIdPath = UUID.fromString("c8d9e0f1-a2b3-4c4d-5e6f-7a8b9c0d1e2f");

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void DeletesUserAndReturnsStatusNoContentAndEmptyBody_UserExists() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userIdPath = userCreated.id();

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNoContent()
                         .expectBody()
                         .isEmpty();

            // Then
            // Assert the user has been deleted
            var optionalActualUser = userRepository.findById(userCreated.id());
            assertThat(optionalActualUser).isEmpty();
        }

        @Test
        void DeletesUserAndReturnsStatusNoContentAndEmptyBody_UserExistsAndHasBeenAuthenticated() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userIdPath = userCreated.id();

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, userIdPath)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNoContent()
                         .expectBody()
                         .isEmpty();

            // Then
            // Assert the user has been deleted
            var optionalActualUser = userRepository.findById(userCreated.id());
            assertThat(optionalActualUser).isEmpty();
        }

    }

    private void mockRequestToGetTasksByUserIdWithOkResponse(UUID userId, List<UUID> taskIds) {
        try {
            var responseBody = taskIds.stream()
                                      .map(taskId -> String.format("{\"task_id\":\"%s\"}", taskId))
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
