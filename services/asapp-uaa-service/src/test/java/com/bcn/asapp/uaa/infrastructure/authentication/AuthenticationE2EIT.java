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

package com.bcn.asapp.uaa.infrastructure.authentication;

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.uaa.domain.user.Role.ADMIN;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.JwtAuthenticationDataFaker.fakeJwtAuthenticationBuilder;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.JwtDataFaker.defaultFakeAccessToken;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.JwtDataFaker.defaultFakeRefreshToken;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.RawJwtDataFaker.fakeRawJwtBuilder;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_RAW_PASSWORD;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static com.bcn.asapp.uaa.testutil.TestDataFaker.UserDataFaker.fakeUserBuilder;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_TOKEN_FULL_PATH;
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

import com.bcn.asapp.uaa.AsappUAAServiceApplication;
import com.bcn.asapp.uaa.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.out.JwtAuthenticationJdbcRepository;
import com.bcn.asapp.uaa.infrastructure.authentication.out.entity.JwtAuthenticationEntity;
import com.bcn.asapp.uaa.infrastructure.authentication.out.entity.JwtEntity;
import com.bcn.asapp.uaa.infrastructure.user.out.UserJdbcRepository;
import com.bcn.asapp.uaa.infrastructure.user.out.entity.UserEntity;
import com.bcn.asapp.uaa.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class AuthenticationE2EIT {

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private JwtAuthenticationJdbcRepository jwtAuthenticationRepository;

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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest("USERNAME_NOT_EXISTS", DEFAULT_FAKE_RAW_PASSWORD);

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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), "PASSWORD_NOT_MATCH");

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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

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
            var user = fakeUserBuilder().withRole(ADMIN.name())
                                        .build();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

            // When
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

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
            var refreshAuthenticationRequestBody = new RefreshAuthenticationRequest("INVALID_REFRESH_TOKEN");

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
            var accessToken = defaultFakeAccessToken().token();
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
            var refreshToken = fakeRawJwtBuilder().refreshToken()
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var refreshToken = fakeRawJwtBuilder().refreshToken()
                                                  .withSubject("SUBJECT_NOT_EXISTS")
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var refreshToken = defaultFakeRefreshToken().token();
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
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
            var refreshToken = firstJwtAuthenticationCreated.refreshToken()
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
            assertAuthenticationHasBeenRefreshed(response.refreshToken(), firstJwtAuthenticationCreated.refreshToken()
                                                                                                       .token());
            assertAuthenticationHasNotBeenRefreshed(secondJwtAuthenticationCreated.refreshToken()
                                                                                  .token());
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody_InvalidAccessToken() {
            // When
            var revokeAuthenticationRequestBody = new RevokeAuthenticationRequest("INVALID_ACCESS_TOKEN");

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
            var refreshToken = defaultFakeRefreshToken().token();
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
            var accessToken = fakeRawJwtBuilder().accessToken()
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var accessToken = fakeRawJwtBuilder().accessToken()
                                                 .withSubject("SUBJECT_NOT_EXISTS")
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var accessToken = defaultFakeAccessToken().token();
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
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
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
            var accessToken = firstJwtAuthenticationCreated.accessToken()
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
            assertAuthenticationHasBeenRevoked(firstJwtAuthenticationCreated.accessToken()
                                                                            .token());
            assertAuthenticationHasNotBeenRevoked(secondJwtAuthenticationCreated.accessToken()
                                                                                .token());
        }

    }

    private void assertAPIResponse(String actualAccessToken, String actualRefreshToken, UserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();

        SoftAssertions.assertSoftly(softAssertions -> {
            assertThatJwt(actualAccessToken).isNotNull()
                                            .isAccessToken()
                                            .hasSubject(expectedUser.username())
                                            .hasClaim(ROLE_CLAIM_NAME, expectedRoleName, String.class)
                                            .hasIssuedAt()
                                            .hasExpiration();
            assertThatJwt(actualRefreshToken).isNotNull()
                                             .isRefreshToken()
                                             .hasSubject(expectedUser.username())
                                             .hasClaim(ROLE_CLAIM_NAME, expectedRoleName, String.class)
                                             .hasIssuedAt()
                                             .hasExpiration();
        });
    }

    private void assertAuthenticationHasBeenCreated(String expectedAccessToken, String expectedRefreshToken, UserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();

        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);

        // @formatter:off
        SoftAssertions.assertSoftly(softAssertions -> {
            assertThat(actualAuthentication).isNotEmpty();

            assertThat(actualAuthentication).get()
                                            .extracting(JwtAuthenticationEntity::accessToken)
                                            .satisfies(jwtEntity -> assertThat(jwtEntity.token()).isEqualTo(expectedAccessToken),
                                                    jwtEntity -> assertThat(jwtEntity.type()).isEqualTo(ACCESS_TOKEN.type()),
                                                    jwtEntity -> assertThat(jwtEntity.subject()).isEqualTo(expectedUser.username()),
                                                    jwtEntity -> assertThat(jwtEntity.claims().claims().get(TOKEN_USE_CLAIM_NAME)).isEqualTo(ACCESS_TOKEN_USE_CLAIM_VALUE),
                                                    jwtEntity -> assertThat(jwtEntity.claims().claims().get(ROLE_CLAIM_NAME)).isEqualTo(expectedRoleName),
                                                    jwtEntity -> assertThat(jwtEntity.issued()).isNotNull(),
                                                    jwtEntity -> assertThat(jwtEntity.expiration()).isNotNull());

            assertThat(actualAuthentication).get()
                                            .extracting(JwtAuthenticationEntity::refreshToken)
                                            .satisfies(jwtEntity -> assertThat(jwtEntity.token()).isEqualTo(expectedRefreshToken),
                                                    jwtEntity -> assertThat(jwtEntity.type()).isEqualTo(REFRESH_TOKEN.type()),
                                                    jwtEntity -> assertThat(jwtEntity.subject()).isEqualTo(expectedUser.username()),
                                                    jwtEntity -> assertThat(jwtEntity.claims().claims().get(TOKEN_USE_CLAIM_NAME)).isEqualTo(REFRESH_TOKEN_USE_CLAIM_VALUE),
                                                    jwtEntity -> assertThat(jwtEntity.claims().claims().get(ROLE_CLAIM_NAME)).isEqualTo(expectedRoleName),
                                                    jwtEntity -> assertThat(jwtEntity.issued()).isNotNull(),
                                                    jwtEntity -> assertThat(jwtEntity.expiration()).isNotNull());
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
                                        .extracting(JwtAuthenticationEntity::refreshToken)
                                        .extracting(JwtEntity::token)
                                        .isEqualTo(expectedRefreshToken);
    }

    private void assertAuthenticationHasBeenRevoked(String expectedAccessToken) {
        var actualAccessToken = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        assertThat(actualAccessToken).isEmpty();
    }

    private void assertAuthenticationHasNotBeenRevoked(String expectedAccessToken) {
        var actualAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expectedAccessToken);
        assertThat(actualAuthentication).get()
                                        .extracting(JwtAuthenticationEntity::accessToken)
                                        .extracting(JwtEntity::token)
                                        .isEqualTo(expectedAccessToken);
    }

    private void assertNoAuthenticationsExists() {
        var actualAuthentication = jwtAuthenticationRepository.findAll();
        assertThat(actualAuthentication).isEmpty();
    }

}
