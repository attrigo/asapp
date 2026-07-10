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

package com.attrigo.asapp.authentication.infrastructure.user;

import static com.attrigo.asapp.authentication.domain.user.Role.ADMIN;
import static com.attrigo.asapp.authentication.domain.user.Role.USER;
import static com.attrigo.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.REFRESH_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtAuthenticationMother.aJwtAuthenticationBuilder;
import static com.attrigo.asapp.authentication.testutil.fixture.UserMother.aJdbcUser;
import static com.attrigo.asapp.authentication.testutil.fixture.UserMother.aUserBuilder;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_CREATE_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_DELETE_BY_ID_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_GET_ALL_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_GET_BY_ID_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_UPDATE_BY_ID_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.jayway.jsonpath.JsonPath;

import com.attrigo.asapp.authentication.AsappAuthenticationServiceApplication;
import com.attrigo.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.attrigo.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.attrigo.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.attrigo.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.attrigo.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.attrigo.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.attrigo.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests end-to-end user management workflows including CRUD operations and cascading cleanup.
 * <p>
 * Setup:
 * <li>Loads the full application context backed by a Testcontainers PostgreSQL instance and an embedded Redis</li>
 * <li>Clears the authentication and user tables, and seeds a valid access token in the store before each test</li>
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

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        var encodedAccessToken = encodedAccessToken();
        bearerToken = "Bearer " + encodedAccessToken;

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
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            // @formatter:off
            assertThatJson(actual).isObject()
                                  .containsOnlyKeys("userId", "username", "password", "role")
                                  .containsEntry("userId", createdUser.id().toString())
                                  .containsEntry("username", createdUser.username())
                                  .containsEntry("password", "*****")
                                  .containsEntry("role", createdUser.role());
            // @formatter:on
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
        void ReturnsStatusUnauthorizedAndProblemDetail_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("b9e4d3c2-5c7f-4ba0-c3e9-8f4d6b2a0c5e");

            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectHeader()
                                       .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                                       .expectHeader()
                                       .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertMissingTokenUnauthorizedResponse(actual);
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
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isArray()
                                  .satisfiesExactlyInAnyOrder(
                                  // @formatter:off
                                      user -> assertThatJson(user).isObject()
                                                                         .containsOnlyKeys("userId", "username", "password", "role")
                                                                         .containsEntry("userId", createdUser1.id().toString())
                                                                         .containsEntry("username", createdUser1.username())
                                                                         .containsEntry("password", "*****")
                                                                         .containsEntry("role", createdUser1.role()),
                                      user -> assertThatJson(user).isObject()
                                                                         .containsOnlyKeys("userId", "username", "password", "role")
                                                                         .containsEntry("userId", createdUser2.id().toString())
                                                                         .containsEntry("username", createdUser2.username())
                                                                         .containsEntry("password", "*****")
                                                                         .containsEntry("role", createdUser2.role()),
                                      user -> assertThatJson(user).isObject()
                                                                         .containsOnlyKeys("userId", "username", "password", "role")
                                                                         .containsEntry("userId", createdUser3.id().toString())
                                                                         .containsEntry("username", createdUser3.username())
                                                                         .containsEntry("password", "*****")
                                                                         .containsEntry("role", createdUser3.role())
                                  // @formatter:on
                                  );
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
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isArray()
                                  .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndProblemDetail_MissingAuthorizationHeader() {
            // When
            var actual = restTestClient.get()
                                       .uri(USERS_GET_ALL_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectHeader()
                                       .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                                       .expectHeader()
                                       .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertMissingTokenUnauthorizedResponse(actual);
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
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isObject()
                                  .containsOnlyKeys("userId");

            // Assert the user has been created
            var actualUserId = UUID.fromString(JsonPath.read(actual, "$.userId"));
            var createdUser = userRepository.findById(actualUserId);
            assertThat(createdUser).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(createdUser.get().id()).as("id").isEqualTo(actualUserId);
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
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThatJson(actual).isObject()
                                  .containsOnlyKeys("userId")
                                  .containsEntry("userId", userId.toString());

            // Assert the user has been updated
            var updatedUser = userRepository.findById(userId);
            assertThat(updatedUser).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(updatedUser.get().id()).as("id").isEqualTo(userId);
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
        void ReturnsStatusUnauthorizedAndProblemDetail_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("d1a6f5e4-7e9a-4dc2-e5fb-0a6f8d4c2e7a");
            var updateUserRequest = new UpdateUserRequest("new_user@asapp.com", "newPassword123!", ADMIN.name());

            // When
            var actual = restTestClient.put()
                                       .uri(USERS_UPDATE_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(updateUserRequest)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectHeader()
                                       .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                                       .expectHeader()
                                       .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertMissingTokenUnauthorizedResponse(actual);
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
        void ReturnsStatusUnauthorizedAndProblemDetail_MissingAuthorizationHeader() {
            // Given
            var userId = UUID.fromString("f3c8b7a6-9ebc-4fe4-a7bd-2c8b0f6e4a9c");

            // When
            var actual = restTestClient.delete()
                                       .uri(USERS_DELETE_BY_ID_FULL_PATH, userId)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .exchange()
                                       .expectStatus()
                                       .isUnauthorized()
                                       .expectHeader()
                                       .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                                       .expectHeader()
                                       .valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                       .expectBody(String.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertMissingTokenUnauthorizedResponse(actual);
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

    private void assertMissingTokenUnauthorizedResponse(String actual) {
        assertThatJson(actual).isObject()
                              .containsEntry("title", "Authentication Failed")
                              .containsEntry("status", 401)
                              .containsEntry("detail", "Invalid credentials")
                              .doesNotContainKey("error");
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

        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAuthentication).as("authentication in database").isEmpty();
            softly.assertThat(actualAccessTokenKeyExists).as("access token exists in Redis").isFalse();
            softly.assertThat(actualRefreshTokenKeyExists).as("refresh token exists in Redis").isFalse();
            // @formatter:on
        });
    }

}
