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

package com.bcn.asapp.authentication.infrastructure.user;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.JwtAuthenticationDataFaker.fakeJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_RAW_PASSWORD;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_ROLE;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_USERNAME;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.fakeUserBuilder;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

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

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.infrastructure.authentication.out.JwtAuthenticationJdbcRepository;
import com.bcn.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.out.UserJdbcRepository;
import com.bcn.asapp.authentication.infrastructure.user.out.entity.UserEntity;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class UserE2EIT {

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private JwtAuthenticationJdbcRepository jwtAuthenticationRepository;

    @Autowired
    private WebTestClient webTestClient;

    private UserEntity authenticatedUser;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();

        var user = fakeUserBuilder().withUsername("auth.test.user@asapp.com")
                                    .build();
        authenticatedUser = userRepository.save(user);
        assertThat(authenticatedUser).isNotNull();

        var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(authenticatedUser.id())
                                                              .build();
        var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(jwtAuthenticationCreated).isNotNull();

        bearerToken = "Bearer " + jwtAuthenticationCreated.accessToken()
                                                          .token();
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
        void GetsUserAndReturnsStatusOKAndBodyWithFoundUser_UserExists() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

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
            var expectedResponse = new GetUserByIdResponse(userCreated.id(), userCreated.username(), "*****", userCreated.role());
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
        void GetsAllUsersAndReturnsStatusOKAndBodyWithFoundUsers_ThereAreUsers() {
            // Given
            var firstUser = fakeUserBuilder().withUsername("first.test.username@asapp.com")
                                             .build();
            var secondUser = fakeUserBuilder().withUsername("second.test.username@asapp.com")
                                              .build();
            var thirdUser = fakeUserBuilder().withUsername("third.test.username@asapp.com")
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
            var authUserResponse = new GetAllUsersResponse(authenticatedUser.id(), authenticatedUser.username(), "*****", authenticatedUser.role());
            var firstUserResponse = new GetAllUsersResponse(firstUserCreated.id(), firstUserCreated.username(), "*****", firstUserCreated.role());
            var secondUserResponse = new GetAllUsersResponse(secondUserCreated.id(), secondUserCreated.username(), "*****", secondUserCreated.role());
            var thirdUserResponse = new GetAllUsersResponse(thirdUserCreated.id(), thirdUserCreated.username(), "*****", thirdUserCreated.role());
            assertThat(response).hasSize(4)
                                .contains(authUserResponse, firstUserResponse, secondUserResponse, thirdUserResponse);
        }

    }

    @Nested
    class CreateUser {

        @Test
        void CreatesUserAndReturnsStatusCreatedAndBodyWithUserCreated() {
            // When
            var createUserRequestBody = new CreateUserRequest(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

            var response = webTestClient.post()
                                        .uri(USERS_CREATE_FULL_PATH)
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
                                    assertThat(createUserResponse.username()).isEqualTo(DEFAULT_FAKE_USERNAME);
                                    assertThat(createUserResponse.password()).isEqualTo("*****");
                                    assertThat(createUserResponse.role()).isEqualTo(DEFAULT_FAKE_ROLE.name());
                                });

            // Assert user has been created
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.username()).isEqualTo(response.username());
                                              assertThat(userEntity.password()).isNotNull();
                                              assertThat(userEntity.role()).isEqualTo(response.role());
                                          });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void DoesNotUpdateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var updateUserRequest = new UpdateUserRequest(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

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
            var updateUserRequest = new UpdateUserRequest(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

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
            var updateUserRequest = new UpdateUserRequest("new.test.username@asapp.com", "new_test#Password12", ADMIN.name());

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
            var expectedResponse = new UpdateUserResponse(userCreated.id(), updateUserRequest.username(), "*****", updateUserRequest.role());
            assertThat(response).isNotNull()
                                .isEqualTo(expectedResponse);

            // Assert user has been updated
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(userEntity -> {
                                              assertThat(userEntity.id()).isEqualTo(response.userId());
                                              assertThat(userEntity.username()).isEqualTo(response.username());
                                              assertThat(userEntity.password()).isNotNull()
                                                                               .isNotEqualTo(userCreated.password());
                                              assertThat(userEntity.role()).isEqualTo(response.role());
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
        void RevokesAuthenticationsAndDeletesUserAndReturnsStatusNoContentAndEmptyBody_UserExistsAndHasBeenAuthenticated() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var firstJwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                       .build();
            var secondJwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                        .build();
            var firstJwtAuthenticationCreated = jwtAuthenticationRepository.save(firstJwtAuthentication);
            var secondJwtAuthenticationCreated = jwtAuthenticationRepository.save(secondJwtAuthentication);
            assertThat(firstJwtAuthenticationCreated).isNotNull();
            assertThat(secondJwtAuthenticationCreated).isNotNull();

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
            // Assert authentications have been revoked
            var optionalActualAuthentication = jwtAuthenticationRepository.findById(firstJwtAuthenticationCreated.id());
            assertThat(optionalActualAuthentication).isEmpty();
            optionalActualAuthentication = jwtAuthenticationRepository.findById(secondJwtAuthenticationCreated.id());
            assertThat(optionalActualAuthentication).isEmpty();

            // Assert user has been deleted
            var optionalActualUser = userRepository.findById(userCreated.id());
            assertThat(optionalActualUser).isEmpty();
        }

    }

}
