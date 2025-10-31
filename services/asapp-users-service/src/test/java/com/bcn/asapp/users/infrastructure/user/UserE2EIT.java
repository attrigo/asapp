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
import static com.bcn.asapp.users.testutil.TestDataFaker.EncodedJwtDataFaker.defaultFakeEncodedAccessToken;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_EMAIL;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_FIRST_NAME;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_LAST_NAME;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_PHONE_NUMBER;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.fakeUserBuilder;
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

        bearerToken = "Bearer " + defaultFakeEncodedAccessToken();
    }

    @Nested
    class GetUserById {

        @Test
        void DoesNotGetUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.randomUUID();

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
            var user = defaultFakeUser();
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
            var userIdPath = UUID.randomUUID();

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
            var user = defaultFakeUser();
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var taskId1 = UUID.randomUUID();
            var taskId2 = UUID.randomUUID();
            var taskId3 = UUID.randomUUID();
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
            var firstUser = fakeUserBuilder().withEmail("first.test.user@asapp.com")
                                             .build();
            var secondUser = fakeUserBuilder().withEmail("second.test.user@asapp.com")
                                              .build();
            var thirdUser = fakeUserBuilder().withEmail("third.test.user@asapp.com")
                                             .build();
            var firstUserCreated = userRepository.save(firstUser);
            var secondUserCreated = userRepository.save(secondUser);
            var thirdUserCreated = userRepository.save(thirdUser);
            assertThat(firstUserCreated).isNotNull();
            assertThat(secondUserCreated).isNotNull();
            assertThat(thirdUserCreated).isNotNull();

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
            var firstUserResponse = new GetAllUsersResponse(firstUserCreated.id(), firstUserCreated.firstName(), firstUserCreated.lastName(),
                    firstUserCreated.email(), firstUserCreated.phoneNumber());
            var secondUserResponse = new GetAllUsersResponse(secondUserCreated.id(), secondUserCreated.firstName(), secondUserCreated.lastName(),
                    secondUserCreated.email(), secondUserCreated.phoneNumber());
            var thirdUserResponse = new GetAllUsersResponse(thirdUserCreated.id(), thirdUserCreated.firstName(), thirdUserCreated.lastName(),
                    thirdUserCreated.email(), thirdUserCreated.phoneNumber());
            assertThat(response).hasSize(3)
                                .contains(firstUserResponse, secondUserResponse, thirdUserResponse);
        }

    }

    @Nested
    class CreateUser {

        @Test
        void DoesNotCreateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When
            var createUserRequestBody = new CreateUserRequest(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
            var createUserRequestBody = new CreateUserRequest(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
                                .satisfies(createUserResponse -> {
                                    assertThat(createUserResponse.userId()).isNotNull();
                                    assertThat(createUserResponse.firstName()).isEqualTo(DEFAULT_FAKE_FIRST_NAME);
                                    assertThat(createUserResponse.lastName()).isEqualTo(DEFAULT_FAKE_LAST_NAME);
                                    assertThat(createUserResponse.email()).isEqualTo(DEFAULT_FAKE_EMAIL);
                                    assertThat(createUserResponse.phoneNumber()).isEqualTo(DEFAULT_FAKE_PHONE_NUMBER);
                                });

            // Assert user has been created
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.firstName()).isEqualTo(response.firstName());
                                              assertThat(userEntity.lastName()).isEqualTo(response.lastName());
                                              assertThat(userEntity.email()).isEqualTo(response.email());
                                              assertThat(userEntity.phoneNumber()).isEqualTo(response.phoneNumber());
                                          });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void DoesNotUpdateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var updateUserRequest = new UpdateUserRequest(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
            var userIdPath = UUID.randomUUID();
            var updateUserRequest = new UpdateUserRequest(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userIdPath = userCreated.id();
            var updateUserRequest = new UpdateUserRequest("new_first_name", "new_last_name", "new.test.user@asapp.com", "777777777");

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
            var expectedResponse = new UpdateUserResponse(userCreated.id(), updateUserRequest.firstName(), updateUserRequest.lastName(),
                    updateUserRequest.email(), updateUserRequest.phoneNumber());
            assertThat(response).isNotNull()
                                .isEqualTo(expectedResponse);

            // Assert user has been updated
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.firstName()).isEqualTo(response.firstName());
                                              assertThat(userEntity.lastName()).isEqualTo(response.lastName());
                                              assertThat(userEntity.email()).isEqualTo(response.email());
                                              assertThat(userEntity.phoneNumber()).isEqualTo(response.phoneNumber());
                                          });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void DoesNotDeleteUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.randomUUID();

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
            var userIdPath = UUID.randomUUID();

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
            var user = defaultFakeUser();
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
            // Assert user has been deleted
            var optionalActualUser = userRepository.findById(userCreated.id());
            assertThat(optionalActualUser).isEmpty();
        }

        @Test
        void DeletesUserAndReturnsStatusNoContentAndEmptyBody_UserExistsAndHasBeenAuthenticated() {
            // Given
            var user = defaultFakeUser();
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
            // Assert user has been deleted
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
