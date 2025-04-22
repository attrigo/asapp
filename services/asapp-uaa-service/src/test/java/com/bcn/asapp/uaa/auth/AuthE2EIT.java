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

import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtAuthorities;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtUsername;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_LOGIN_FULL_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

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

@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebTestClient webTestClient;

    private String fakeUsername;

    private String fakePassword;

    private String fakePasswordBcryptEncoded;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
        this.fakePasswordBcryptEncoded = "{bcrypt}" + new BCryptPasswordEncoder().encode(fakePassword);
    }

    @Nested
    class Login {

        @Test
        @DisplayName("GIVEN user credentials username does not exists WHEN login a user THEN does not authenticate the user And returns HTTP response with status UNAUTHORIZED And an empty body")
        void UserCredentialsUsernameNotExists_Login_DoesNotAuthenticateAndReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody();

        }

        @Test
        @DisplayName("GIVEN user credentials password is not valid WHEN login a user THEN does not authenticate the user And returns HTTP response with status UNAUTHORIZED And an empty body")
        void UserCredentialsPasswordIsNotValid_Login_DoesNotAuthenticateReturnsStatusUnauthorizedAndEmptyBody() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, "NotValidPassword");

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN user credentials with USER role are valid WHEN login a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as USER")
        void UserCredentialsWithUserRoleValid_Login_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

        @Test
        @DisplayName("GIVEN user credentials with ADMIN role are valid WHEN login a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as ADMIN")
        void UserCredentialsWithAdminRoleAreValid_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.ADMIN);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.ADMIN));
        }

        @Test
        @DisplayName("GIVEN stored username password has Bcrypt encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void StoredUserPasswordHasBcryptEncode_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

        @Test
        @DisplayName("GIVEN stored username password has Argon2 encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void StoredUserPasswordHasArgon2Encode_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var fakePasswordArgon2Encoded = "{argon2@SpringSecurity_v5_8}" + argon2Encoder.encode(fakePassword);
            var fakeUser = new User(null, fakeUsername, fakePasswordArgon2Encoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

        @Test
        @DisplayName("GIVEN stored username password has Pbkdf2 encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void StoredUserPasswordHasPbkdf2Encode_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var fakePasswordPbkdf2Encoded = "{pbkdf2@SpringSecurity_v5_8}" + pbkdf2Encoder.encode(fakePassword);
            var fakeUser = new User(null, fakeUsername, fakePasswordPbkdf2Encoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

        @Test
        @DisplayName("GIVEN stored username password has Scrypt encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void StoredUserPasswordHasScryptEncode_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
            var fakePasswordScryptEncoded = "{scrypt@SpringSecurity_v5_8}" + scryptEncoder.encode(fakePassword);
            var fakeUser = new User(null, fakeUsername, fakePasswordScryptEncoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

        @Test
        @DisplayName("GIVEN stored username password has with noop encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void StoredUserPasswordHasNoopEncode_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakePasswordNoopEncoded = "{noop}TEST PASSWORD";
            var fakeUser = new User(null, fakeUsername, fakePasswordNoopEncoded, Role.USER);
            userRepository.save(fakeUser);

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            webTestClient.post()
                         .uri(AUTH_LOGIN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userCredentialsToLogin)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticationDTO.class)
                         .value(authentication -> assertThat(authentication.jwt(), notNullValue()))
                         .value(authentication -> assertJwtUsername(authentication.jwt(), fakeUsername))
                         .value(authentication -> assertJwtAuthorities(authentication.jwt(), Role.USER));
        }

    }

}
