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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.uaa.AsappUAAServiceApplication;

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

    private String fakePasswordEncoded;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();

        this.fakeUsername = "IT username";
        this.fakePassword = "IT password";
        this.fakePasswordEncoded = "$2a$12$e0cBX1d5lUSzBE2YulhdlOXIaRISzQzubIN2X5xcpzF2zzAqHevy2";
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
            var fakeUser = new User(null, fakeUsername, fakePassword, Role.USER);
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
        @DisplayName("GIVEN user credentials exists as USER And credentials are valid WHEN login a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as USER")
        void UserCredentialsExistsAsUserAndCredentialsAreValid_Login_AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordEncoded, Role.USER);
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
        @DisplayName("GIVEN user credentials exists as ADMIN And credentials are valid WHEN login a user THEN authenticates the user And returns HTTP response with status OK And the body with the generated authentication as ADMIN")
        void UserCredentialsExistsAsAdminAndCredentialsAreValid_Login_AuthenticatesTheUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordEncoded, Role.ADMIN);
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

    }

}
