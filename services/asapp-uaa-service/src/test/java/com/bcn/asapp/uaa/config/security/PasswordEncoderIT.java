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

package com.bcn.asapp.uaa.config.security;

import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.uaa.AsappUAAServiceApplication;
import com.bcn.asapp.uaa.auth.JwtAuthenticationDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true)
class PasswordEncoderIT {

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

    private String fakeUsername;

    private String fakePassword;

    private String fakePasswordBcryptEncoded;

    @BeforeEach
    void beforeEach() {
        accessTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
        this.fakePasswordBcryptEncoded = "{bcrypt}" + new BCryptPasswordEncoder().encode(fakePassword);
    }

    @Test
    @DisplayName("GIVEN stored user password has Bcrypt encoding WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void StoredUserPasswordHasBcryptEncode_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
        // Given
        var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
        var userToBeAuthenticated = userRepository.save(fakeUser);
        assertNotNull(userToBeAuthenticated);

        // When
        var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

        webTestClient.post()
                     .uri(AUTH_TOKEN_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(userCredentialsToAuthenticate)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(JwtAuthenticationDTO.class);
    }

    @Test
    @DisplayName("GIVEN stored user password has Argon2 encoding WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void StoredUserPasswordHasArgon2Encode_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
        // Given
        var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordArgon2Encoded = "{argon2@SpringSecurity_v5_8}" + argon2Encoder.encode(fakePassword);
        var fakeUser = new User(null, fakeUsername, fakePasswordArgon2Encoded, Role.USER);
        var userToBeAuthenticated = userRepository.save(fakeUser);
        assertNotNull(userToBeAuthenticated);

        // When
        var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

        webTestClient.post()
                     .uri(AUTH_TOKEN_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(userCredentialsToAuthenticate)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(JwtAuthenticationDTO.class);
    }

    @Test
    @DisplayName("GIVEN stored user password has Pbkdf2 encoding WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void StoredUserPasswordHasPbkdf2Encode_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
        // Given
        var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordPbkdf2Encoded = "{pbkdf2@SpringSecurity_v5_8}" + pbkdf2Encoder.encode(fakePassword);
        var fakeUser = new User(null, fakeUsername, fakePasswordPbkdf2Encoded, Role.USER);
        var userToBeAuthenticated = userRepository.save(fakeUser);
        assertNotNull(userToBeAuthenticated);

        // When
        var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

        webTestClient.post()
                     .uri(AUTH_TOKEN_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(userCredentialsToAuthenticate)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(JwtAuthenticationDTO.class);
    }

    @Test
    @DisplayName("GIVEN stored user password has Scrypt encoding WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void StoredUserPasswordHasScryptEncode_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
        // Given
        var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordScryptEncoded = "{scrypt@SpringSecurity_v5_8}" + scryptEncoder.encode(fakePassword);
        var fakeUser = new User(null, fakeUsername, fakePasswordScryptEncoded, Role.USER);
        var userToBeAuthenticated = userRepository.save(fakeUser);
        assertNotNull(userToBeAuthenticated);

        // When
        var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

        webTestClient.post()
                     .uri(AUTH_TOKEN_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(userCredentialsToAuthenticate)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(JwtAuthenticationDTO.class);
    }

    @Test
    @DisplayName("GIVEN stored user password has with noop encoding WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void StoredUserPasswordHasNoopEncode_Authenticate_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
        // Given
        var fakePasswordNoopEncoded = "{noop}TEST PASSWORD";
        var fakeUser = new User(null, fakeUsername, fakePasswordNoopEncoded, Role.USER);
        var userToBeAuthenticated = userRepository.save(fakeUser);
        assertNotNull(userToBeAuthenticated);

        // When
        var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

        webTestClient.post()
                     .uri(AUTH_TOKEN_FULL_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(userCredentialsToAuthenticate)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(JwtAuthenticationDTO.class);
    }

}
