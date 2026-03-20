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

import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aJdbcUser;
import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aUserBuilder;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link DelegatingPasswordEncoder} multi-format password authentication.
 * <p>
 * Coverage:
 * <li>Authenticates users with noop-encoded passwords</li>
 * <li>Authenticates users with bcrypt-encoded passwords</li>
 * <li>Authenticates users with argon2-encoded passwords</li>
 * <li>Authenticates users with scrypt-encoded passwords</li>
 * <li>Authenticates users with pbkdf2-encoded passwords</li>
 * <li>Returns valid JWT token pair for all encoding types</li>
 */
@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class PasswordEncoderIT {

    @Autowired
    private JdbcUserRepository userRepository;

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

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
    class AuthenticateWithUserPassword {

        @Test
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_BcryptEncodedStoredUserPassword() {
            // Given
            var createdUser = createUser();
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When & Then
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
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_Argon2EncodedStoredUserPassword() {
            // Given
            var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = aUserBuilder().withPasswordEncoder("{argon2@SpringSecurity_v5_8}", argon2Encoder)
                                     .buildJdbc();
            var createdUser = createUser(user);
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When & Then
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
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_Pbkdf2EncodedStoredUserPassword() {
            // Given
            var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = aUserBuilder().withPasswordEncoder("{pbkdf2@SpringSecurity_v5_8}", pbkdf2Encoder)
                                     .buildJdbc();
            var createdUser = createUser(user);
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When & Then
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
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_ScryptEncodedStoredUserPassword() {
            // Given
            var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
            var user = aUserBuilder().withPasswordEncoder("{scrypt@SpringSecurity_v5_8}", scryptEncoder)
                                     .buildJdbc();
            var createdUser = createUser(user);
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When & Then
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
        void ReturnsStatusOkAndBodyWithGeneratedAuthentication_NoopEncodedStoredUserPassword() {
            // Given
            var noOpEncoder = NoOpPasswordEncoder.getInstance();
            var user = aUserBuilder().withPasswordEncoder("{noop}", noOpEncoder)
                                     .buildJdbc();
            var createdUser = createUser(user);
            var authenticateRequestBody = new AuthenticateRequest(createdUser.username(), "TEST@09_password?!");

            // When & Then
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

}
