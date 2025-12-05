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

package com.bcn.asapp.authentication.infrastructure.authentication;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtPairStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtPairStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestJdbcUser;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.testUserBuilder;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

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

@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class AuthenticationE2EIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private WebTestClient webTestClient;

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
        void DoesNotAuthenticateAndReturnsStatusUnauthorizedAndEmptyBody_UsernameNotExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest("not_exists_username", "TEST@09_password?!");

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotAuthenticateReturnsStatusUnauthorizedAndEmptyBody_PasswordNotMatch() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), "not_match_password");

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_UserNotAuthenticated() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), "TEST@09_password?!");

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(authenticateRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(AuthenticateResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertThat(response).isNotNull();
            assertAPIResponse(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(response.accessToken(), response.refreshToken(), userCreated);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_AdminUserNotAuthenticated() {
            // Given
            var user = testUserBuilder().withRole(ADMIN.name())
                                        .buildJdbcEntity();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), "TEST@09_password?!");

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(authenticateRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(AuthenticateResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertThat(response).isNotNull();
            assertAPIResponse(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(response.accessToken(), response.refreshToken(), userCreated);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithNewGeneratedAuthentication_UserAlreadyAuthenticated() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var previousJwtAuthentication = createJwtAuthentication(userCreated);
            var previousAccessToken = previousJwtAuthentication.accessToken();
            var previousRefreshToken = previousJwtAuthentication.refreshToken();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), "TEST@09_password?!");

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(authenticateRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(AuthenticateResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertThat(response).isNotNull();
            assertAPIResponse(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(previousAccessToken.token(), previousRefreshToken.token(), userCreated);
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        void DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_InvalidRefreshToken() {
            // When
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest("invalid_refresh_token");

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_JwtIsAccessToken() {
            // When
            var accessToken = defaultTestEncodedAccessToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_RefreshTokenHasExpired() {
            // When
            var refreshToken = testEncodedTokenBuilder().refreshToken()
                                                        .expired()
                                                        .build();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken);

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_RefreshTokenSubjectNotExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var refreshToken = testEncodedTokenBuilder().refreshToken()
                                                        .withSubject("not_exists_subject")
                                                        .build();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken);

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_RefreshTokenNotExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var refreshToken = defaultTestEncodedRefreshToken();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken);

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void RefreshesAuthenticationAndReturnsStatusOkAndBodyWithRefreshedAuthentication_RefreshTokenBelongsToUserAlreadyAuthenticated() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var previousJwtAuthentication = createJwtAuthentication(userCreated);
            var previousAccessToken = previousJwtAuthentication.accessToken();
            var previousRefreshToken = previousJwtAuthentication.refreshToken();

            // When
            var refreshToken = previousJwtAuthentication.refreshToken()
                                                        .token();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken);

            var response = webTestClient.post()
                                        .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(refreshAuthenticationRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(RefreshAuthenticationResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertThat(response).isNotNull();
            assertAPIResponse(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationNotExist(previousAccessToken.token(), previousRefreshToken.token());
        }

        @Test
        void RefreshesAuthenticationAndReturnsStatusOkAndBodyWithRefreshedAuthentication_RefreshTokenBelongsToUserHasSeveralAuthentications() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var previousJwtAuthentication1 = createJwtAuthentication(userCreated);
            var previousJwtAuthentication2 = createJwtAuthentication(userCreated);
            var previousAccessToken1 = previousJwtAuthentication1.accessToken();
            var previousRefreshToken1 = previousJwtAuthentication1.refreshToken();
            var previousAccessToken2 = previousJwtAuthentication2.accessToken();
            var previousRefreshToken2 = previousJwtAuthentication2.refreshToken();

            // When
            var refreshToken = previousJwtAuthentication1.refreshToken()
                                                         .token();
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest(refreshToken);

            var response = webTestClient.post()
                                        .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(refreshAuthenticationRequestBody)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(RefreshAuthenticationResponse.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertThat(response).isNotNull();
            assertAPIResponse(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationExists(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationNotExist(previousAccessToken1.token(), previousRefreshToken1.token());
            assertAuthenticationExists(previousAccessToken2.token(), previousRefreshToken2.token(), userCreated);
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_InvalidAccessToken() {
            // When
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest("invalid_access_token");

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_JwtIsRefreshToken() {
            // When
            var refreshToken = defaultTestEncodedRefreshToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(refreshToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_AccessTokenHasExpired() {
            // When
            var accessToken = testEncodedTokenBuilder().accessToken()
                                                       .expired()
                                                       .build();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_AccessTokenSubjectNotExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var accessToken = testEncodedTokenBuilder().accessToken()
                                                       .withSubject("not_exists_subject")
                                                       .build();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_AccessTokenNotExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var accessToken = defaultTestEncodedAccessToken();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertNoAuthenticationsExists();
        }

        @Test
        void RevokesAuthenticationAndReturnsStatusOkAndEmptyBody_AccessTokenBelongsToUserAlreadyAuthenticated() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var previousJwtAuthentication = createJwtAuthentication(userCreated);
            var previousAccessToken = previousJwtAuthentication.accessToken();
            var previousRefreshToken = previousJwtAuthentication.refreshToken();

            // When
            var accessToken = previousJwtAuthentication.accessToken()
                                                       .token();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody()
                         .isEmpty();

            // Then

            assertAuthenticationNotExist(previousAccessToken.token(), previousRefreshToken.token());
        }

        @Test
        void RevokesAuthenticationAndReturnsStatusOkAndEmptyBody_AccessTokenBelongsToUserHasSeveralAuthentications() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var previousJwtAuthentication1 = createJwtAuthentication(userCreated);
            var previousJwtAuthentication2 = createJwtAuthentication(userCreated);
            var previousAccessToken1 = previousJwtAuthentication1.accessToken();
            var previousRefreshToken1 = previousJwtAuthentication1.refreshToken();
            var previousAccessToken2 = previousJwtAuthentication2.accessToken();
            var previousRefreshToken2 = previousJwtAuthentication2.refreshToken();

            // When
            var accessToken = previousJwtAuthentication1.accessToken()
                                                        .token();
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest(accessToken);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(revokeAuthenticationRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody()
                         .isEmpty();

            // Then

            assertAuthenticationNotExist(previousAccessToken1.token(), previousRefreshToken1.token());
            assertAuthenticationExists(previousAccessToken2.token(), previousRefreshToken2.token(), userCreated);
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

    private void assertAPIResponse(String actualAccessToken, String actualRefreshToken, JdbcUserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();

        SoftAssertions.assertSoftly(softAssertions -> {
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
        });
    }

    private void assertAuthenticationExists(String expectedAccessToken, String expectedRefreshToken, JdbcUserEntity expectedUser) {
        var expectedUsername = expectedUser.username();
        var expectedRole = expectedUser.role();

        // Database
        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);

        // Redis
        var expectedAccessTokenRedisKey = ACCESS_TOKEN_PREFIX + expectedAccessToken;
        var expectedRefreshTokenRedisKey = REFRESH_TOKEN_PREFIX + expectedRefreshToken;
        var actualAccessTokenKeyExists = redisTemplate.hasKey(expectedAccessTokenRedisKey);
        var actualRefreshTokenKeyExists = redisTemplate.hasKey(expectedRefreshTokenRedisKey);
        var actualAccessTokenInRedis = redisTemplate.opsForValue()
                                                    .get(expectedAccessTokenRedisKey);
        var actualRefreshTokenInRedis = redisTemplate.opsForValue()
                                                     .get(expectedRefreshTokenRedisKey);

        // @formatter:off
        SoftAssertions.assertSoftly(softAssertions -> {
            // Database
            assertThat(actualAuthentication).isNotEmpty();
            assertThat(actualAuthentication).get()
                                            .extracting(JdbcJwtAuthenticationEntity::accessToken)
                                            .satisfies(actualAccessToken -> assertThat(actualAccessToken.token()).isEqualTo(expectedAccessToken),
                                                    actualAccessToken -> assertThat(actualAccessToken.type()).isEqualTo(ACCESS_TOKEN.type()),
                                                    actualAccessToken -> assertThat(actualAccessToken.subject()).isEqualTo(expectedUsername),
                                                    actualAccessToken -> assertThat(actualAccessToken.claims().claims().get(TOKEN_USE)).isEqualTo(ACCESS_TOKEN_USE),
                                                    actualAccessToken -> assertThat(actualAccessToken.claims().claims().get(ROLE)).isEqualTo(expectedRole),
                                                    actualAccessToken -> assertThat(actualAccessToken.issued()).isNotNull(),
                                                    actualAccessToken -> assertThat(actualAccessToken.expiration()).isNotNull());
            assertThat(actualAuthentication).get()
                                            .extracting(JdbcJwtAuthenticationEntity::refreshToken)
                                            .satisfies(actualRefreshToken -> assertThat(actualRefreshToken.token()).isEqualTo(expectedRefreshToken),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.type()).isEqualTo(REFRESH_TOKEN.type()),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.subject()).isEqualTo(expectedUsername),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.claims().claims().get(TOKEN_USE)).isEqualTo(REFRESH_TOKEN_USE),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.claims().claims().get(ROLE)).isEqualTo(expectedRole),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.issued()).isNotNull(),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.expiration()).isNotNull());

            // Redis
            assertThat(actualAccessTokenKeyExists).isTrue();
            assertThat(actualRefreshTokenKeyExists).isTrue();
            assertThat(actualAccessTokenInRedis).isEmpty();
            assertThat(actualRefreshTokenInRedis).isEmpty();
        });
        // @formatter:on
    }

    private void assertAuthenticationNotExist(String expectedAccessToken, String expectedRefreshToken) {
        // Database
        var actualAuthenticationFromAccessToken = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        var actualAuthenticationFromRefreshToken = jwtAuthenticationRepository.findByRefreshTokenToken(expectedRefreshToken);

        // Redis
        var actualAccessTokenKey = ACCESS_TOKEN_PREFIX + expectedAccessToken;
        var actualRefreshTokenKey = REFRESH_TOKEN_PREFIX + expectedRefreshToken;
        var actualAccessTokenKeyExists = redisTemplate.hasKey(actualAccessTokenKey);
        var actualRefreshTokenKeyExists = redisTemplate.hasKey(actualRefreshTokenKey);

        SoftAssertions.assertSoftly(softAssertions -> {
            // Database
            assertThat(actualAuthenticationFromAccessToken).isEmpty();
            assertThat(actualAuthenticationFromRefreshToken).isEmpty();

            // Redis
            assertThat(actualAccessTokenKeyExists).isFalse();
            assertThat(actualRefreshTokenKeyExists).isFalse();
        });
    }

    private void assertNoAuthenticationsExists() {
        // Database
        var actualAuthenticationsInDB = jwtAuthenticationRepository.findAll();

        // Redis
        var actualAuthenticationsInRedis = redisTemplate.keys("jwt:*");

        SoftAssertions.assertSoftly(softAssertions -> {
            assertThat(actualAuthenticationsInDB).isEmpty();
            assertThat(actualAuthenticationsInRedis).isEmpty();
        });
    }

}
