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
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestUser;
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
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtEntity;
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
    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class Authenticate {

        @Test
        void DoesNotAuthenticateAndReturnsStatusUnauthorizedAndEmptyBody_UsernameNotExists() {
            // Given
            var user = defaultTestUser();
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
            var user = defaultTestUser();
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
            var user = defaultTestUser();
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
            assertAuthenticationHasBeenCreated(response.accessToken(), response.refreshToken(), userCreated);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_AdminUserNotAuthenticated() {
            // Given
            var user = testUserBuilder().withRole(ADMIN.name())
                                        .build();
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
            assertAuthenticationHasBeenCreated(response.accessToken(), response.refreshToken(), userCreated);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithNewGeneratedAuthentication_UserAuthenticated() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

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
            assertAuthenticationHasBeenCreated(response.accessToken(), response.refreshToken(), userCreated);
            assertThereAreSeveralAuthentications();
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
            var user = defaultTestUser();
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
            var user = defaultTestUser();
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
        void RefreshesAuthenticationAndReturnsStatusOkAndBodyWithRefreshedAuthentication_RefreshTokenBelongsToUserAuthenticated() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

            // When
            var refreshToken = jwtAuthenticationCreated.refreshToken()
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
            assertAuthenticationHasBeenCreated(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationHasBeenRefreshed(response.refreshToken(), jwtAuthenticationCreated.refreshToken()
                                                                                                  .token());
        }

        @Test
        void RefreshesAuthenticationAndReturnsStatusOkAndBodyWithRefreshedAuthentication_RefreshTokenBelongsToUserHasSeveralAuthentications() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication1 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .build();
            var jwtAuthentication2 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .build();
            var jwtAuthenticationCreated1 = jwtAuthenticationRepository.save(jwtAuthentication1);
            var jwtAuthenticationCreated2 = jwtAuthenticationRepository.save(jwtAuthentication2);
            assertThat(jwtAuthenticationCreated1).isNotNull();
            assertThat(jwtAuthenticationCreated2).isNotNull();

            // When
            var refreshToken = jwtAuthenticationCreated1.refreshToken()
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
            assertAuthenticationHasBeenCreated(response.accessToken(), response.refreshToken(), userCreated);
            assertAuthenticationHasBeenRefreshed(response.refreshToken(), jwtAuthenticationCreated1.refreshToken()
                                                                                                   .token());
            assertAuthenticationHasNotBeenRefreshed(jwtAuthenticationCreated2.refreshToken()
                                                                             .token());
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
            var user = defaultTestUser();
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
            var user = defaultTestUser();
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
        void RevokesAuthenticationAndReturnsStatusOkAndEmptyBody_AccessTokenBelongsToUserAuthenticated() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

            // When
            var accessToken = jwtAuthenticationCreated.accessToken()
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
            assertAuthenticationHasBeenRevoked(jwtAuthenticationCreated.accessToken()
                                                                       .token());
        }

        @Test
        void RevokesAuthenticationAndReturnsStatusOkAndEmptyBody_AccessTokenBelongsToUserHasSeveralAuthentications() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication1 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .build();
            var jwtAuthentication2 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .build();
            var jwtAuthenticationCreated1 = jwtAuthenticationRepository.save(jwtAuthentication1);
            var jwtAuthenticationCreated2 = jwtAuthenticationRepository.save(jwtAuthentication2);
            assertThat(jwtAuthenticationCreated1).isNotNull();
            assertThat(jwtAuthenticationCreated2).isNotNull();

            // When
            var accessToken = jwtAuthenticationCreated1.accessToken()
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
            assertAuthenticationHasBeenRevoked(jwtAuthenticationCreated1.accessToken()
                                                                        .token());
            assertAuthenticationHasNotBeenRevoked(jwtAuthenticationCreated2.accessToken()
                                                                           .token());
        }

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

    private void assertAuthenticationHasBeenCreated(String expectedAccessToken, String expectedRefreshToken, JdbcUserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();

        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);

        // @formatter:off
        SoftAssertions.assertSoftly(softAssertions -> {
            assertThat(actualAuthentication).isNotEmpty();

            assertThat(actualAuthentication).get()
                                            .extracting(JdbcJwtAuthenticationEntity::accessToken)
                                            .satisfies(actualAccessToken -> assertThat(actualAccessToken.token()).isEqualTo(expectedAccessToken),
                                                    actualAccessToken -> assertThat(actualAccessToken.type()).isEqualTo(ACCESS_TOKEN.type()),
                                                    actualAccessToken -> assertThat(actualAccessToken.subject()).isEqualTo(expectedUser.username()),
                                                    actualAccessToken -> assertThat(actualAccessToken.claims().claims().get(TOKEN_USE)).isEqualTo(ACCESS_TOKEN_USE),
                                                    actualAccessToken -> assertThat(actualAccessToken.claims().claims().get(ROLE)).isEqualTo(expectedRoleName),
                                                    actualAccessToken -> assertThat(actualAccessToken.issued()).isNotNull(),
                                                    actualAccessToken -> assertThat(actualAccessToken.expiration()).isNotNull());

            assertThat(actualAuthentication).get()
                                            .extracting(JdbcJwtAuthenticationEntity::refreshToken)
                                            .satisfies(actualRefreshToken -> assertThat(actualRefreshToken.token()).isEqualTo(expectedRefreshToken),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.type()).isEqualTo(REFRESH_TOKEN.type()),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.subject()).isEqualTo(expectedUser.username()),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.claims().claims().get(TOKEN_USE)).isEqualTo(REFRESH_TOKEN_USE),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.claims().claims().get(ROLE)).isEqualTo(expectedRoleName),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.issued()).isNotNull(),
                                                    actualRefreshToken -> assertThat(actualRefreshToken.expiration()).isNotNull());
        });
        // @formatter:on
    }

    private void assertThereAreSeveralAuthentications() {
        var actualAuthentication = jwtAuthenticationRepository.findAll();

        SoftAssertions.assertSoftly(softAssertions -> {
            assertThat(actualAuthentication).isNotEmpty();
            assertThat(actualAuthentication).hasSizeGreaterThan(1);
        });
    }

    private void assertAuthenticationHasBeenRefreshed(String actualRefreshToken, String expectedRefreshToken) {
        assertThat(actualRefreshToken).asString()
                                      .isNotEqualTo(expectedRefreshToken);
        var previousAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(expectedRefreshToken);
        assertThat(previousAuthentication).isEmpty();
    }

    private void assertAuthenticationHasNotBeenRefreshed(String expectedRefreshToken) {
        var actualAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(expectedRefreshToken);
        assertThat(actualAuthentication).get()
                                        .extracting(JdbcJwtAuthenticationEntity::refreshToken)
                                        .extracting(JdbcJwtEntity::token)
                                        .isEqualTo(expectedRefreshToken);
    }

    private void assertAuthenticationHasBeenRevoked(String expectedAccessToken) {
        var actualAccessToken = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        assertThat(actualAccessToken).isEmpty();
    }

    private void assertAuthenticationHasNotBeenRevoked(String expectedAccessToken) {
        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        assertThat(actualAuthentication).get()
                                        .extracting(JdbcJwtAuthenticationEntity::accessToken)
                                        .extracting(JdbcJwtEntity::token)
                                        .isEqualTo(expectedAccessToken);
    }

    private void assertNoAuthenticationsExists() {
        var actualAuthentication = jwtAuthenticationRepository.findAll();
        assertThat(actualAuthentication).isEmpty();
    }

}
