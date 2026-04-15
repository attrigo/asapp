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

package com.bcn.asapp.authentication.infrastructure.authentication;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenFactory.anEncodedTokenBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenFactory.encodedAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenFactory.encodedRefreshToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aJdbcUser;
import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aUserBuilder;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

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

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests end-to-end authentication workflows including token issuance, refresh, and revocation.
 * <p>
 * Coverage:
 * <li>Authenticates user credentials generating JWT pair persisted to database and activated in Redis</li>
 * <li>Refreshes authentication rotating token pair with old tokens deactivated and new tokens activated</li>
 * <li>Revokes authentication deleting from database and deactivating in Redis</li>
 * <li>Tests complete flow: HTTP → Security → Controller → Service → Repository → Database + Redis</li>
 * <li>Complete refresh flow: old tokens revoked, new tokens generated and stored</li>
 * <li>Complete revoke flow: authentication deleted from DB, tokens removed from Redis</li>
 * <li>Validates tokens are properly formatted JWTs with correct claims</li>
 * <li>Validates tokens exist in both PostgreSQL and Redis after issuance</li>
 * <li>Validates old tokens removed and new tokens added during refresh</li>
 * <li>Validates tokens completely removed after revocation</li>
 */
@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class AuthenticationE2EIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTestClient restTestClient;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();

        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.getConnectionFactory()
                     .getConnection()
                     .serverCommands()
                     .flushDb();
    }

    @Nested
    class Authenticate {

        @Test
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_UserNotAuthenticated() {
            // Given
            var createdUser = createUser();
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(authenticateRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(AuthenticateResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull();
            assertAPIResponse(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(actual.accessToken(), actual.refreshToken(), createdUser);
        }

        @Test
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_AdminUserNotAuthenticated() {
            // Given
            var user = aUserBuilder().withRole(ADMIN)
                                     .buildJdbc();
            var createdUser = createUser(user);
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(authenticateRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(AuthenticateResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull();
            assertAPIResponse(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(actual.accessToken(), actual.refreshToken(), createdUser);
        }

        @Test
        void ReturnsStatusOkAndBodyWithNewGeneratedAuthentication_UserAlreadyAuthenticated() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);
            var accessToken = createdJwtAuthentication.accessToken();
            var refreshToken = createdJwtAuthentication.refreshToken();
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(authenticateRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(AuthenticateResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull();
            assertAPIResponse(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(accessToken.token(), refreshToken.token(), createdUser);
        }

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithGenericMessage_UsernameNotEmailFormat() {
            // Given
            var authenticateRequestBody = new AuthenticateRequest("invalid_username", "TEST@09_password?!");

            // When
            restTestClient.post()
                          .uri(AUTH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(authenticateRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/token");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_UsernameNotExists() {
            // Given
            var authenticateRequestBody = new AuthenticateRequest("user_not_exist@asapp.com", "TEST@09_password?!");

            // When
            restTestClient.post()
                          .uri(AUTH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(authenticateRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_NonMatchingPassword() {
            // Given
            var createdUser = createUser();
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "password_not_match");

            // When
            restTestClient.post()
                          .uri(AUTH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(authenticateRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();

            // Then
            assertAuthenticationNotExist();
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        void ReturnsStatusOkAndBodyWithRefreshedAuthentication_ValidRefreshToken() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);
            var accessToken = createdJwtAuthentication.accessToken();
            var refreshToken = createdJwtAuthentication.refreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken.token());

            // When
            var actual = restTestClient.post()
                                       .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(refreshAuthenticationRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(RefreshAuthenticationResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull();
            assertAPIResponse(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationNotExist(accessToken.token(), refreshToken.token());
        }

        @Test
        void ReturnsStatusOkAndBodyWithRefreshedAuthentication_UserHasSeveralAuthentications() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication2 = createJwtAuthenticationForUser(createdUser);
            var accessToken1 = createdJwtAuthentication1.accessToken();
            var refreshToken1 = createdJwtAuthentication1.refreshToken();
            var accessToken2 = createdJwtAuthentication2.accessToken();
            var refreshToken2 = createdJwtAuthentication2.refreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken1.token());

            // When

            var actual = restTestClient.post()
                                       .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                       .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                       .body(refreshAuthenticationRequestBody)
                                       .exchange()
                                       .expectStatus()
                                       .isOk()
                                       .expectHeader()
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .expectBody(RefreshAuthenticationResponse.class)
                                       .returnResult()
                                       .getResponseBody();

            // Then
            assertThat(actual).isNotNull();
            assertAPIResponse(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationExist(actual.accessToken(), actual.refreshToken(), createdUser);
            assertAuthenticationNotExist(accessToken1.token(), refreshToken1.token());
            assertAuthenticationExist(accessToken2.token(), refreshToken2.token(), createdUser);
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_InvalidRefreshToken() {
            // Given
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest("invalid_refresh_token");

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_AccessTokenJwt() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(encodedAccessToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid token")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_ExpiredRefreshToken() {
            // Given
            var encodedRefreshToken = anEncodedTokenBuilder().refreshToken()
                                                             .expired()
                                                             .build();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(encodedRefreshToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_RefreshTokenSubjectNotExists() {
            // Given
            var encodedRefreshToken = anEncodedTokenBuilder().refreshToken()
                                                             .withSubject("subject_not_exist")
                                                             .build();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(encodedRefreshToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_RefreshTokenNotExists() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(encodedRefreshToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_RefreshTokenExistsInDbButNotInRedis() {
            // Given
            var createdUser = createUser();
            var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(createdUser.id())
                                                               .buildJdbc();
            var createdJwtAuthentication = createJwtAuthenticationInDB(jwtAuthentication);
            var accessToken = createdJwtAuthentication.accessToken();
            var refreshToken = createdJwtAuthentication.refreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationExistInDB(accessToken.token(), refreshToken.token(), createdUser);
            assertAuthenticationNotExistInRedis(accessToken.token(), refreshToken.token());
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_RefreshTokenExistsInRedisButNotInDb() {
            // Given
            var createdUser = createUser();
            var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(createdUser.id())
                                                               .buildJdbc();
            createJwtAuthenticationInRedis(jwtAuthentication);
            var accessToken = jwtAuthentication.accessToken();
            var refreshToken = jwtAuthentication.refreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(refreshAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/refresh");

            // Then
            assertAuthenticationNotExistInDB(accessToken.token(), refreshToken.token());
            assertAuthenticationExistInRedis(accessToken.token(), refreshToken.token());
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void ReturnsStatusNoContentAndEmptyBody_ValidAccessToken() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);
            var accessToken = createdJwtAuthentication.accessToken();
            var refreshToken = createdJwtAuthentication.refreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isNoContent()
                          .expectBody()
                          .isEmpty();

            // Then
            assertAuthenticationNotExist(accessToken.token(), refreshToken.token());
        }

        @Test
        void ReturnsStatusNoContentAndEmptyBody_UserHasSeveralAuthentications() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication2 = createJwtAuthenticationForUser(createdUser);
            var accessToken1 = createdJwtAuthentication1.accessToken();
            var refreshToken1 = createdJwtAuthentication1.refreshToken();
            var accessToken2 = createdJwtAuthentication2.accessToken();
            var refreshToken2 = createdJwtAuthentication2.refreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken1.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isNoContent()
                          .expectBody()
                          .isEmpty();

            // Then
            assertAuthenticationNotExist(accessToken1.token(), refreshToken1.token());
            assertAuthenticationExist(accessToken2.token(), refreshToken2.token(), createdUser);
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_InvalidAccessToken() {
            // Given
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest("invalid_access_token");

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_RefreshTokenJwt() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(encodedRefreshToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid token")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_ExpiredAccessToken() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .expired()
                                                            .build();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(encodedAccessToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_AccessTokenSubjectNotExists() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSubject("subject_not_exist")
                                                            .build();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(encodedAccessToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_AccessTokenNotExists() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(encodedAccessToken);

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExist();
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_AccessTokenExistsInDbButNotInRedis() {
            // Given
            var createdUser = createUser();
            var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(createdUser.id())
                                                               .buildJdbc();
            var createdJwtAuthentication = createJwtAuthenticationInDB(jwtAuthentication);
            var accessToken = createdJwtAuthentication.accessToken();
            var refreshToken = createdJwtAuthentication.refreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationExistInDB(accessToken.token(), refreshToken.token(), createdUser);
            assertAuthenticationNotExistInRedis(accessToken.token(), refreshToken.token());
        }

        @Test
        void ReturnsStatusUnauthorizedWithGenericMessage_AccessTokenExistsInRedisButNotInDb() {
            // Given
            var createdUser = createUser();
            var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(createdUser.id())
                                                               .buildJdbc();
            createJwtAuthenticationInRedis(jwtAuthentication);
            var accessToken = jwtAuthentication.accessToken();
            var refreshToken = jwtAuthentication.refreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken.token());

            // When
            restTestClient.post()
                          .uri(AUTH_REVOKE_FULL_PATH)
                          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .body(revokeAuthenticationRequestBody)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectHeader()
                          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                          .expectBody()
                          .jsonPath("$.title")
                          .isEqualTo("Authentication Failed")
                          .jsonPath("$.status")
                          .isEqualTo(401)
                          .jsonPath("$.detail")
                          .isEqualTo("Invalid credentials")
                          .jsonPath("$.error")
                          .isEqualTo("invalid_grant")
                          .jsonPath("$.instance")
                          .isEqualTo("/asapp-authentication-service/api/auth/revoke");

            // Then
            assertAuthenticationNotExistInDB(accessToken.token(), refreshToken.token());
            assertAuthenticationExistInRedis(accessToken.token(), refreshToken.token());
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
        var createdJwtAuthentication = createJwtAuthenticationInDB(jwtAuthentication);
        createJwtAuthenticationInRedis(jwtAuthentication);
        return createdJwtAuthentication;
    }

    private JdbcJwtAuthenticationEntity createJwtAuthenticationInDB(JdbcJwtAuthenticationEntity jwtAuthentication) {
        var createdJwtAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(createdJwtAuthentication).isNotNull();
        return createdJwtAuthentication;
    }

    private void createJwtAuthenticationInRedis(JdbcJwtAuthenticationEntity jwtAuthentication) {
        var accessToken = jwtAuthentication.accessToken();
        var refreshToken = jwtAuthentication.refreshToken();
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + accessToken.token(), "");
        redisTemplate.opsForValue()
                     .set(REFRESH_TOKEN_PREFIX + refreshToken.token(), "");
        assertThat(redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + accessToken.token())).isTrue();
        assertThat(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + refreshToken.token())).isTrue();
    }

    // Assertions Helpers

    private void assertAPIResponse(String actualAccessToken, String actualRefreshToken, JdbcUserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();

        assertThatJwt(actualAccessToken).isNotNull()
                                        .isAccessToken()
                                        .hasSubject(expectedUser.username())
                                        .hasClaim(ROLE, expectedRoleName, String.class)
                                        .hasIssuedAt()
                                        .hasExpiration();
        assertThatJwt(actualRefreshToken).isNotNull()
                                         .isRefreshToken()
                                         .hasSubject(expectedUser.username())
                                         .hasClaim(ROLE, expectedRoleName, String.class)
                                         .hasIssuedAt()
                                         .hasExpiration();
    }

    private void assertAuthenticationExist(String expectedAccessToken, String expectedRefreshToken, JdbcUserEntity expectedUser) {
        assertAuthenticationExistInDB(expectedAccessToken, expectedRefreshToken, expectedUser);
        assertAuthenticationExistInRedis(expectedAccessToken, expectedRefreshToken);
    }

    private void assertAuthenticationExistInDB(String expectedAccessToken, String expectedRefreshToken, JdbcUserEntity expectedUser) {
        var expectedUsername = expectedUser.username();
        var expectedRole = expectedUser.role();

        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);

        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAuthentication).as("authentication").isNotEmpty();
            softly.assertThat(actualAuthentication).get()
                  .extracting(JdbcJwtAuthenticationEntity::accessToken)
                  .satisfies(actualAccessToken -> softly.assertThat(actualAccessToken.token()).as("access token value").isEqualTo(expectedAccessToken),
                          actualAccessToken -> softly.assertThat(actualAccessToken.type()).as("access token type").isEqualTo(ACCESS_TOKEN.type()),
                          actualAccessToken -> softly.assertThat(actualAccessToken.subject()).as("access token subject").isEqualTo(expectedUsername),
                          actualAccessToken -> softly.assertThat(actualAccessToken.claims().claims().get(TOKEN_USE)).as("access token use claim").isEqualTo(ACCESS_TOKEN_USE),
                          actualAccessToken -> softly.assertThat(actualAccessToken.claims().claims().get(ROLE)).as("access token role claim").isEqualTo(expectedRole),
                          actualAccessToken -> softly.assertThat(actualAccessToken.issued()).as("access token issued").isNotNull(),
                          actualAccessToken -> softly.assertThat(actualAccessToken.expiration()).as("access token expiration").isNotNull());
            softly.assertThat(actualAuthentication).get()
                  .extracting(JdbcJwtAuthenticationEntity::refreshToken)
                  .satisfies(actualRefreshToken -> softly.assertThat(actualRefreshToken.token()).as("refresh token value").isEqualTo(expectedRefreshToken),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.type()).as("refresh token type").isEqualTo(REFRESH_TOKEN.type()),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.subject()).as("refresh token subject").isEqualTo(expectedUsername),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.claims().claims().get(TOKEN_USE)).as("refresh token use claim").isEqualTo(REFRESH_TOKEN_USE),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.claims().claims().get(ROLE)).as("refresh token role claim").isEqualTo(expectedRole),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.issued()).as("refresh token issued").isNotNull(),
                          actualRefreshToken -> softly.assertThat(actualRefreshToken.expiration()).as("refresh token expiration").isNotNull());
            // @formatter:on
        });
    }

    private void assertAuthenticationExistInRedis(String expectedAccessToken, String expectedRefreshToken) {
        // Redis
        var expectedAccessTokenRedisKey = ACCESS_TOKEN_PREFIX + expectedAccessToken;
        var expectedRefreshTokenRedisKey = REFRESH_TOKEN_PREFIX + expectedRefreshToken;
        var actualAccessTokenKeyExists = redisTemplate.hasKey(expectedAccessTokenRedisKey);
        var actualRefreshTokenKeyExists = redisTemplate.hasKey(expectedRefreshTokenRedisKey);
        var actualAccessTokenInRedis = redisTemplate.opsForValue()
                                                    .get(expectedAccessTokenRedisKey);
        var actualRefreshTokenInRedis = redisTemplate.opsForValue()
                                                     .get(expectedRefreshTokenRedisKey);

        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAccessTokenKeyExists).as("access token exists in Redis").isTrue();
            softly.assertThat(actualRefreshTokenKeyExists).as("refresh token exists in Redis").isTrue();
            softly.assertThat(actualAccessTokenInRedis).as("access token value in Redis").isEmpty();
            softly.assertThat(actualRefreshTokenInRedis).as("refresh token value in Redis").isEmpty();
            // @formatter:on
        });
    }

    private void assertAuthenticationNotExist(String expectedAccessToken, String expectedRefreshToken) {
        assertAuthenticationNotExistInDB(expectedAccessToken, expectedRefreshToken);
        assertAuthenticationNotExistInRedis(expectedAccessToken, expectedRefreshToken);
    }

    private void assertAuthenticationNotExistInDB(String expectedAccessToken, String expectedRefreshToken) {
        var actualAuthenticationFromAccessToken = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        var actualAuthenticationFromRefreshToken = jwtAuthenticationRepository.findByRefreshTokenToken(expectedRefreshToken);
        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAuthenticationFromAccessToken).as("authentication from access token").isEmpty();
            softly.assertThat(actualAuthenticationFromRefreshToken).as("authentication from refresh token").isEmpty();
            // @formatter:on
        });
    }

    private void assertAuthenticationNotExistInRedis(String expectedAccessToken, String expectedRefreshToken) {
        // Redis
        var actualAccessTokenKey = ACCESS_TOKEN_PREFIX + expectedAccessToken;
        var actualRefreshTokenKey = REFRESH_TOKEN_PREFIX + expectedRefreshToken;
        var actualAccessTokenKeyExists = redisTemplate.hasKey(actualAccessTokenKey);
        var actualRefreshTokenKeyExists = redisTemplate.hasKey(actualRefreshTokenKey);

        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(actualAccessTokenKeyExists).as("access token exists in Redis").isFalse();
            softly.assertThat(actualRefreshTokenKeyExists).as("refresh token exists in Redis").isFalse();
            // @formatter:on
        });
    }

    private void assertAuthenticationNotExist() {
        // Database
        var actualAuthenticationsInDB = jwtAuthenticationRepository.findAll();

        // Redis
        var actualAuthenticationsInRedis = redisTemplate.keys("jwt:*");

        assertThat(actualAuthenticationsInDB).isEmpty();
        assertThat(actualAuthenticationsInRedis).isEmpty();
    }

}
