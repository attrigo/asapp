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

package com.bcn.asapp.authentication.infrastructure.config;

import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_RAW_PASSWORD;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.fakeUserBuilder;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.out.JwtAuthenticationJdbcRepository;
import com.bcn.asapp.authentication.infrastructure.user.out.UserJdbcRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class PasswordEncoderIT {

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private JwtAuthenticationJdbcRepository jwtAuthenticationJdbcRepository;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationJdbcRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class AuthenticateWithUserPassword {

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_StoredUserPasswordHasBcryptEncode() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When & Then
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticateResponse.class);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_StoredUserPasswordHasArgon2Encode() {
            // Given
            var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = fakeUserBuilder().withPasswordEncoder("{argon2@SpringSecurity_v5_8}", argon2Encoder)
                                        .build();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When & Then
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticateResponse.class);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_StoredUserPasswordHasPbkdf2Encode() {
            // Given
            var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = fakeUserBuilder().withPasswordEncoder("{pbkdf2@SpringSecurity_v5_8}", pbkdf2Encoder)
                                        .build();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When & Then
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticateResponse.class);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_StoredUserPasswordHasScryptEncode() {
            // Given
            var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = fakeUserBuilder().withPasswordEncoder("{scrypt@SpringSecurity_v5_8}", scryptEncoder)
                                        .build();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When & Then
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticateResponse.class);
        }

        @Test
        void AuthenticatesUserAndReturnsStatusOkAndBodyWithGeneratedAuthentication_StoredUserPasswordHasNoopEncode() {
            // Given
            var noOpEncoder = NoOpPasswordEncoder.getInstance();
            var user = fakeUserBuilder().withPasswordEncoder("{noop}", noOpEncoder)
                                        .build();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When & Then
            var authenticateRequestBody = new AuthenticateRequest(userCreated.username(), DEFAULT_FAKE_RAW_PASSWORD);

            webTestClient.post()
                         .uri(AUTH_TOKEN_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(authenticateRequestBody)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(AuthenticateResponse.class);
        }

    }

}
