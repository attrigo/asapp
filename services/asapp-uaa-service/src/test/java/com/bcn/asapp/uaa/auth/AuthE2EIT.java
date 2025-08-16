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

package com.bcn.asapp.uaa.auth;

import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtExpiresAt;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtIssuedAt;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtRole;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtType;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtUsername;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.uaa.AsappUAAServiceApplication;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.testutil.JwtFaker;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true)
class AuthE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private WebTestClient webTestClient;

    private JwtFaker jwtFaker;

    private String fakeUsername;

    private String fakePassword;

    private String fakePasswordBcryptEncoded;

    @BeforeEach
    void beforeEach() {
        accessTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        this.jwtFaker = new JwtFaker();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
        this.fakePasswordBcryptEncoded = "{bcrypt}" + new BCryptPasswordEncoder().encode(fakePassword);
    }

    @Nested
    class Authenticate {

        @Test
        @DisplayName("GIVEN user credentials username does not exists WHEN authenticate a user THEN does not authenticate the user And returns HTTP response with status UNAUTHORIZED And an empty body")
        void UserCredentialsUsernameNotExists_Authenticate_DoesNotAuthenticateAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When & Then
            var userCredentialsToAuthenticate = new UserCredentialsDTO("UserNotExists", fakePassword);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToAuthenticate)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN user credentials password is not valid WHEN authenticate a user THEN does not authenticate the user And returns HTTP response with status UNAUTHORIZED And an empty body")
        void UserCredentialsPasswordIsInvalid_Authenticate_DoesNotAuthenticateReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When & Then
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, "NotValidPassword");

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToAuthenticate)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN user credentials are valid And belongs to USER role WHEN authenticate a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as USER")
        void UserCredentialsAreValidAndBelongsToUserRole_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userCredentialsToAuthenticate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(JwtAuthenticationDTO.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertAuthenticationResponseFields(response, Role.USER);

            assertAuthenticationExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN user credentials are valid And belongs to ADMIN role WHEN authenticate a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as ADMIN")
        void UserCredentialsAreValidAndBelongsToAdminRole_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.ADMIN);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userCredentialsToAuthenticate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(JwtAuthenticationDTO.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertAuthenticationResponseFields(response, Role.ADMIN);

            assertAuthenticationExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN user credentials are valid And belongs to user already authenticated WHEN authenticate a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication")
        void UserCredentialsBelongsToUserAlreadyAuthenticated_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userToBeAuthenticated.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var currentAccessToken = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(currentAccessToken);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userToBeAuthenticated.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var currentRefreshToken = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(currentRefreshToken);

            // When
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

            var response = webTestClient.post()
                                        .uri(AUTH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userCredentialsToAuthenticate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(JwtAuthenticationDTO.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertNotNull(response);
            assertNotEquals(fakeAccessJwt, response.accessToken()
                                                   .jwt());
            assertNotEquals(fakeRefreshJwt, response.refreshToken()
                                                    .jwt());
            assertAuthenticationResponseFields(response, Role.USER);

            var actualAccessToken = accessTokenRepository.findAll();
            assertEquals(1L, actualAccessToken.size());
            assertNotEquals(fakeAccessJwt, actualAccessToken.getFirst()
                                                            .jwt());
            var actualRefreshToken = refreshTokenRepository.findAll();
            assertEquals(1L, actualRefreshToken.size());
            assertNotEquals(fakeRefreshJwt, actualRefreshToken.getFirst()
                                                              .jwt());
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        @DisplayName("GIVEN refresh token is invalid WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void InvalidRefreshToken_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When & Then
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwtInvalid());

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToRefresh)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN refresh token is an access token WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void RefreshTokenIsAccessToken_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When & Then
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToRefresh)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN user of refresh token not exists WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void RefreshTokenUserNotExists_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, "ANOTHER USER", fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When & Then
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToRefresh)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN refresh token belongs to non-authenticated user WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void RefreshTokenBelongsToUserNotAuthenticated_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            // When
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToRefresh)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertAuthenticationNotExists(userToBeAuthenticated);
        }

        @Test
        @DisplayName("GIVEN refresh token has been already refreshed WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void RefreshTokenHasBeenAlreadyRefreshed_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userToBeAuthenticated.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var currentAccessToken = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(currentAccessToken);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userToBeAuthenticated.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var currentrefreshToken = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(currentrefreshToken);

            var firstRefreshTokenResponse = webTestClient.post()
                                                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                         .bodyValue(new RefreshTokenDTO(fakeRefreshJwt))
                                                         .exchange()
                                                         .expectStatus()
                                                         .isOk()
                                                         .expectHeader()
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .expectBody(JwtAuthenticationDTO.class)
                                                         .returnResult()
                                                         .getResponseBody();
            assertNotNull(firstRefreshTokenResponse);

            // When
            var refreshTokenToRefresh = new RefreshTokenDTO(fakeRefreshJwt);

            webTestClient.post()
                         .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToRefresh)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            var actualAccessToken = accessTokenRepository.findAll();
            assertEquals(1L, actualAccessToken.size());
            assertEquals(firstRefreshTokenResponse.accessToken()
                                                  .jwt(),
                    actualAccessToken.getFirst()
                                     .jwt());
            var actualRefreshToken = refreshTokenRepository.findAll();
            assertEquals(1L, actualRefreshToken.size());
            assertEquals(firstRefreshTokenResponse.refreshToken()
                                                  .jwt(),
                    actualRefreshToken.getFirst()
                                      .jwt());
        }

        @Test
        @DisplayName("GIVEN refresh token has been revoked WHEN refresh an authentication THEN does not refresh the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void RefreshTokenHasBeenRevoked_RefreshAuthentication_DoesNotRefreshAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(new AccessTokenDTO(fakeAccessJwt))
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody()
                         .isEmpty();

            // When
            var refreshTokenToBeRefreshed = new RefreshTokenDTO(fakeRefreshJwt);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(refreshTokenToBeRefreshed)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN refresh token is valid WHEN refresh an authentication THEN refreshes the authentication And returns HTTP response with status OK And the body with the refreshed authentication")
        void RefreshTokenIsValid_RefreshAuthentication_RefreshesAuthenticationAndReturnsStatusOkAndBodyWithRefreshedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeAuthenticated = userRepository.save(fakeUser);
            assertNotNull(userToBeAuthenticated);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userToBeAuthenticated.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var currentAccessToken = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(currentAccessToken);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userToBeAuthenticated.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var currentrefreshToken = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(currentrefreshToken);

            // When
            var refreshTokenToRefresh = new RefreshTokenDTO(fakeRefreshJwt);

            var response = webTestClient.post()
                                        .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(refreshTokenToRefresh)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(JwtAuthenticationDTO.class)
                                        .returnResult()
                                        .getResponseBody();

            // Then
            assertNotNull(response);
            assertNotEquals(fakeAccessJwt, response.accessToken()
                                                   .jwt());
            assertNotEquals(fakeRefreshJwt, response.refreshToken()
                                                    .jwt());
            assertAuthenticationResponseFields(response, Role.USER);

            var actualAccessToken = accessTokenRepository.findAll();
            assertEquals(1L, actualAccessToken.size());
            assertNotEquals(fakeAccessJwt, actualAccessToken.getFirst()
                                                            .jwt());
            var actualRefreshToken = refreshTokenRepository.findAll();
            assertEquals(1L, actualRefreshToken.size());
            assertNotEquals(fakeRefreshJwt, actualRefreshToken.getFirst()
                                                              .jwt());
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        @DisplayName("GIVEN access token is invalid WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void InvalidAccessToken_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When & Then
            var accessTokenToBeRevoked = new AccessTokenDTO(jwtFaker.fakeJwtInvalid());

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationExists(userSaved);
        }

        @Test
        @DisplayName("GIVEN access token is a refresh token WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void AccessTokenIsRefreshToken_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When & Then
            var accessTokenToBeRevoked = new AccessTokenDTO(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationExists(userSaved);
        }

        @Test
        @DisplayName("GIVEN user of access token not exists WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void AccessTokenUserNotExists_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, "ANOTHER USER", fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When & Then
            var accessTokenToBeRevoked = new AccessTokenDTO(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            assertAuthenticationExists(userSaved);
        }

        @Test
        @DisplayName("GIVEN access token belongs to non-authenticated user WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void AccessTokenBelongsToUserNotAuthenticated_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            // When
            var accessTokenToBeRevoked = new AccessTokenDTO(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            assertAuthenticationNotExists(userSaved);
        }

        @Test
        @DisplayName("GIVEN access token has been refreshed WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void AccessTokenHasBeenRefreshed_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            var refreshTokenResponse = webTestClient.post()
                                                    .uri(AUTH_REFRESH_TOKEN_FULL_PATH)
                                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                    .bodyValue(new RefreshTokenDTO(fakeRefreshJwt))
                                                    .exchange()
                                                    .expectStatus()
                                                    .isOk()
                                                    .expectHeader()
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .expectBody(JwtAuthenticationDTO.class)
                                                    .returnResult()
                                                    .getResponseBody();
            assertNotNull(refreshTokenResponse);

            // When
            var accessTokenToBeRevoked = new AccessTokenDTO(fakeAccessJwt);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualAccessToken.isPresent());
            assertNotEquals(accessTokenSaved.jwt(), actualAccessToken.get()
                                                                     .jwt());
            var actualRefreshToken = refreshTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualRefreshToken.isPresent());
            assertNotEquals(refreshTokenSaved.jwt(), actualRefreshToken.get()
                                                                       .jwt());
        }

        @Test
        @DisplayName("GIVEN access token has been revoked WHEN revoke an authentication THEN does not revoke the authentication And returns HTTP response with status UNAUTHORIZED And an empty body")
        void AccessTokenHasBeenRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(new AccessTokenDTO(fakeAccessJwt))
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody()
                         .isEmpty();

            // When
            var accessTokenToBeRevoked = new AccessTokenDTO(fakeAccessJwt);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(userSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN access token is valid WHEN revoke an authentication THEN revokes the authentication And returns HTTP response with status OK And an empty body")
        void AccessTokenIsValid_RevokeAuthentication_RevokesAuthenticationAndReturnsStatusOkAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userSaved = userRepository.save(fakeUser);
            assertNotNull(userSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, userSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When
            var accessTokenToBeRevoked = new AccessTokenDTO(fakeAccessJwt);

            webTestClient.post()
                         .uri(AUTH_REVOKE_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(accessTokenToBeRevoked)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody()
                         .isEmpty();

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(userSaved.id());
            assertFalse(actualAccessToken.isPresent());
            var actualRefreshToken = refreshTokenRepository.findByUserId(userSaved.id());
            assertFalse(actualRefreshToken.isPresent());
        }

    }

    private void assertAuthenticationResponseFields(JwtAuthenticationDTO actualAuthentication, Role expectedRole) {
        assertNotNull(actualAuthentication);

        var actualAccessToken = actualAuthentication.accessToken();
        assertNotNull(actualAccessToken);
        assertJwtType(actualAccessToken.jwt(), JwtType.ACCESS_TOKEN);
        assertJwtUsername(actualAccessToken.jwt(), fakeUsername);
        assertJwtRole(actualAccessToken.jwt(), expectedRole);
        assertJwtIssuedAt(actualAccessToken.jwt());
        assertJwtExpiresAt(actualAccessToken.jwt());

        var actualRefreshToken = actualAuthentication.refreshToken();
        assertNotNull(actualRefreshToken);
        assertJwtType(actualRefreshToken.jwt(), JwtType.REFRESH_TOKEN);
        assertJwtUsername(actualRefreshToken.jwt(), fakeUsername);
        assertJwtRole(actualRefreshToken.jwt(), expectedRole);
        assertJwtIssuedAt(actualRefreshToken.jwt());
        assertJwtExpiresAt(actualRefreshToken.jwt());
    }

    private void assertAuthenticationExists(User actualUser) {
        var actualAccessToken = accessTokenRepository.findByUserId(actualUser.id());
        assertTrue(actualAccessToken.isPresent());
        var actualRefreshToken = refreshTokenRepository.findByUserId(actualUser.id());
        assertTrue(actualRefreshToken.isPresent());
    }

    private void assertAuthenticationNotExists(User actualUser) {
        var actualAccessToken = accessTokenRepository.findByUserId(actualUser.id());
        assertFalse(actualAccessToken.isPresent());
        var actualRefreshToken = refreshTokenRepository.findByUserId(actualUser.id());
        assertFalse(actualRefreshToken.isPresent());
    }

}
