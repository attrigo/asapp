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
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestJdbcUser;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.testUserBuilder;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
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

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class UserE2EIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private WebTestClient webTestClient;

    private final String accessToken = defaultTestEncodedAccessToken();

    private final String bearerToken = "Bearer " + accessToken;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();

        redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + accessToken, "");
    }

    @Nested
    class GetUserById {

        @Test
        void DoesNotGetUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("b9e4d3c2-5c7f-4ba0-c3e9-8f4d6b2a0c5e");

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
            var userIdPath = UUID.fromString("b9e4d3c2-5c7f-4ba0-c3e9-8f4d6b2a0c5e");

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
            var user = defaultTestJdbcUser();
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
            var user1 = testUserBuilder().withUsername("user1@asapp.com")
                                         .buildJdbcEntity();
            var user2 = testUserBuilder().withUsername("user2@asapp.com")
                                         .buildJdbcEntity();
            var user3 = testUserBuilder().withUsername("user3@asapp.com")
                                         .buildJdbcEntity();
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
            var expectedResponse1 = new GetAllUsersResponse(userCreated1.id(), userCreated1.username(), "*****", userCreated1.role());
            var expectedResponse2 = new GetAllUsersResponse(userCreated2.id(), userCreated2.username(), "*****", userCreated2.role());
            var expectedResponse3 = new GetAllUsersResponse(userCreated3.id(), userCreated3.username(), "*****", userCreated3.role());
            assertThat(response).hasSize(3)
                                .containsExactlyInAnyOrder(expectedResponse1, expectedResponse2, expectedResponse3);
        }

    }

    @Nested
    class CreateUser {

        @Test
        void CreatesUserAndReturnsStatusCreatedAndBodyWithUserCreated() {
            // When
            var createUserRequestBody = new CreateUserRequest("user@asapp.com", "TEST@09_password?!", USER.name());

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
                                .extracting(CreateUserResponse::userId)
                                .isNotNull();

            // Assert the user has been created
            var optionalActualUser = userRepository.findById(response.userId());
            assertThat(optionalActualUser).isNotEmpty()
                                          .get()
                                          .satisfies(actualUser -> {
                                              assertThat(actualUser.id()).isEqualTo(response.userId());
                                              assertThat(actualUser.username()).isEqualTo(createUserRequestBody.username());
                                              assertThat(actualUser.password()).isNotNull();
                                              assertThat(actualUser.role()).isEqualTo(createUserRequestBody.role());
                                          });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void DoesNotUpdateUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("d1a6f5e4-7e9a-4dc2-e5fb-0a6f8d4c2e7a");
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "new_test#Password12", ADMIN.name());

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
            var userIdPath = UUID.fromString("d1a6f5e4-7e9a-4dc2-e5fb-0a6f8d4c2e7a");
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "new_test#Password12", ADMIN.name());

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
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userIdPath = userCreated.id();
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "new_test#Password12", ADMIN.name());

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
                                          .satisfies(actualUser -> {
                                              assertThat(actualUser.id()).isEqualTo(response.userId());
                                              assertThat(actualUser.username()).isEqualTo(updateUserRequest.username());
                                              assertThat(actualUser.password()).isNotNull()
                                                                               .isNotEqualTo(userCreated.password());
                                              assertThat(actualUser.role()).isEqualTo(updateUserRequest.role());
                                          });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void DoesNotDeleteUserAndReturnsStatusUnauthorizedAndEmptyBody_RequestNotHasAuthorizationHeader() {
            // When & Then
            var userIdPath = UUID.fromString("f3c8b7a6-9ebc-4fe4-a7bd-2c8b0f6e4a9c");

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
            var userIdPath = UUID.fromString("f3c8b7a6-9ebc-4fe4-a7bd-2c8b0f6e4a9c");

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
            var user = defaultTestJdbcUser();
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
            var actualUser = userRepository.findById(userCreated.id());
            assertThat(actualUser).isEmpty();
        }

        @Test
        void RevokesAuthenticationsAndDeletesUserAndReturnsStatusNoContentAndEmptyBody_UserExistsAndHasBeenAuthenticated() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication1 = createJwtAuthentication(userCreated);
            var jwtAuthentication2 = createJwtAuthentication(userCreated);

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
            // Assert the authentications have been revoked
            assertAuthenticationNotExist(jwtAuthentication1);
            assertAuthenticationNotExist(jwtAuthentication2);

            // Assert the user has been deleted
            var actualUser = userRepository.findById(userCreated.id());
            assertThat(actualUser).isEmpty();
        }

    }

    private JdbcJwtAuthenticationEntity createJwtAuthentication(JdbcUserEntity user) {
        var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(user.id())
                                                              .buildJdbcEntity();
        var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(jwtAuthenticationCreated).isNotNull();

        var accessToken = jwtAuthenticationCreated.accessToken();
        var refreshToken = jwtAuthenticationCreated.refreshToken();

        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + accessToken.token(), "");
        redisTemplate.opsForValue()
                     .set(REFRESH_TOKEN_PREFIX + refreshToken.token(), "");

        return jwtAuthenticationCreated;
    }

    private void assertAuthenticationNotExist(JdbcJwtAuthenticationEntity expectedJwtAuthentication) {
        var jwtAuthenticationId = expectedJwtAuthentication.id();
        var accessToken = expectedJwtAuthentication.accessToken()
                                                   .token();
        var refreshToken = expectedJwtAuthentication.refreshToken()
                                                    .token();

        // Database
        var actualAuthentication = jwtAuthenticationRepository.findById(jwtAuthenticationId);

        // Redis
        var actualAccessTokenKey = ACCESS_TOKEN_PREFIX + accessToken;
        var actualRefreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        var actualAccessTokenKeyExists = redisTemplate.hasKey(actualAccessTokenKey);
        var actualRefreshTokenKeyExists = redisTemplate.hasKey(actualRefreshTokenKey);

        SoftAssertions.assertSoftly(softAssertions -> {
            // Database
            assertThat(actualAuthentication).isEmpty();

            // Redis
            assertThat(actualAccessTokenKeyExists).isFalse();
            assertThat(actualRefreshTokenKeyExists).isFalse();
        });
    }

}
