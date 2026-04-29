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

package com.bcn.asapp.authentication.infrastructure.user;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationMother.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.aJdbcUser;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.aUserBuilder;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

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

/**
 * Tests end-to-end user management workflows including CRUD operations and cascading cleanup.
 * <p>
 * Coverage:
 * <li>Rejects all operations without valid JWT authentication</li>
 * <li>Creates user with password encoding and persistence to database</li>
 * <li>Retrieves user by identifier returning 404 when not found, user when exists</li>
 * <li>Retrieves all users returning empty or collection</li>
 * <li>Updates existing user persisting changes with password re-encoding when provided</li>
 * <li>Deletes existing user cascading to authentication records and deactivating tokens</li>
 * <li>Tests complete flow: HTTP → Security → Controller → Service → Repository → Database</li>
 * <li>Complete delete user flow: user removed from DB, associated tokens cleaned up</li>
 * <li>Validates authentication tokens removed from PostgreSQL and Redis after user deletion</li>
 * <li>Validates JWT authentication required for all endpoints</li>
 * <li>Validates password never returned in responses</li>
 */
@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class UserE2EIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTestClient restTestClient;

    private final String encodedAccessToken = encodedAccessToken();

    private final String bearerToken = "Bearer " + encodedAccessToken;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();

        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
    }

    @Nested
    class GetUserById {

        @Test
        void ReturnsStatusOKAndBodyWithFoundUser_UserExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var response = new GetUserByIdResponse(createdUser.id(), createdUser.username(), "*****", createdUser.role());

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
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("b9e4d3c2-5c7f-4ba0-c3e9-8f4d6b2a0c5e");

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
            var userId = UUID.fromString("b9e4d3c2-5c7f-4ba0-c3e9-8f4d6b2a0c5e");

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
            var user1 = aUserBuilder().withUsername("user1@asapp.com")
                                      .buildJdbc();
            var user2 = aUserBuilder().withUsername("user2@asapp.com")
                                      .buildJdbc();
            var user3 = aUserBuilder().withUsername("user3@asapp.com")
                                      .buildJdbc();
            var createdUser1 = createUser(user1);
            var createdUser2 = createUser(user2);
            var createdUser3 = createUser(user3);
            var response1 = new GetAllUsersResponse(createdUser1.id(), createdUser1.username(), "*****", createdUser1.role());
            var response2 = new GetAllUsersResponse(createdUser2.id(), createdUser2.username(), "*****", createdUser2.role());
            var response3 = new GetAllUsersResponse(createdUser3.id(), createdUser3.username(), "*****", createdUser3.role());

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
            var createUserRequestBody = new CreateUserRequest("user@asapp.com", "TEST@09_password?!", USER.name());

            // When
            var actual = restTestClient.post()
                                       .uri(USERS_CREATE_FULL_PATH)
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
            assertThat(createdUser).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(createdUser.get().id()).as("id").isEqualTo(actual.userId());
                softly.assertThat(createdUser.get().username()).as("username").isEqualTo(createUserRequestBody.username());
                softly.assertThat(createdUser.get().password()).as("password").isNotNull();
                softly.assertThat(createdUser.get().role()).as("role").isEqualTo(createUserRequestBody.role());
                // @formatter:on
            });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsStatusOkAndBodyWithUpdatedUser_UserExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "newPassword123!", ADMIN.name());

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
            assertThat(updatedUser).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(updatedUser.get().id()).as("id").isEqualTo(actual.userId());
                softly.assertThat(updatedUser.get().username()).as("username").isEqualTo(updateUserRequest.username());
                softly.assertThat(updatedUser.get().password()).as("password").isNotNull().isNotEqualTo(createdUser.password());
                softly.assertThat(updatedUser.get().role()).as("role").isEqualTo(updateUserRequest.role());
                // @formatter:on
            });
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("d1a6f5e4-7e9a-4dc2-e5fb-0a6f8d4c2e7a");
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "newPassword123!", ADMIN.name());

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
            var userId = UUID.fromString("d1a6f5e4-7e9a-4dc2-e5fb-0a6f8d4c2e7a");
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "newPassword123!", ADMIN.name());

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
            var actualUser = userRepository.findById(createdUser.id());
            assertThat(actualUser).isEmpty();
        }

        @Test
        void ReturnsStatusNoContentAndEmptyBody_UserWithAuthenticationsExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var jwtAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var jwtAuthentication2 = createJwtAuthenticationForUser(createdUser);

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
            // Assert the authentications have been revoked
            assertAuthenticationNotExist(jwtAuthentication1);
            assertAuthenticationNotExist(jwtAuthentication2);

            // Assert the user has been deleted
            var deletedUser = userRepository.findById(createdUser.id());
            assertThat(deletedUser).isEmpty();
        }

        @Test
        void ReturnsStatusNotFoundAndEmptyBody_UserNotExists() {
            // Given
            var userId = UUID.fromString("f3c8b7a6-9ebc-4fe4-a7bd-2c8b0f6e4a9c");

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
            var userId = UUID.fromString("f3c8b7a6-9ebc-4fe4-a7bd-2c8b0f6e4a9c");

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

    private JdbcJwtAuthenticationEntity createJwtAuthenticationForUser(JdbcUserEntity user) {
        var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id())
                                                           .buildJdbc();
        var createdJwtAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(createdJwtAuthentication).isNotNull();

        var accessToken = createdJwtAuthentication.accessToken();
        var refreshToken = createdJwtAuthentication.refreshToken();
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + accessToken.token(), "");
        redisTemplate.opsForValue()
                     .set(REFRESH_TOKEN_PREFIX + refreshToken.token(), "");
        assertThat(redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + accessToken.token())).isTrue();
        assertThat(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + refreshToken.token())).isTrue();

        return createdJwtAuthentication;
    }

    // Assertions Helpers

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

        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAuthentication).as("authentication in database").isEmpty();
            softly.assertThat(actualAccessTokenKeyExists).as("access token exists in Redis").isFalse();
            softly.assertThat(actualRefreshTokenKeyExists).as("refresh token exists in Redis").isFalse();
            // @formatter:on
        });
    }

}
