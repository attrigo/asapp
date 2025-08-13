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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import com.bcn.asapp.uaa.testconfig.SecurityTestConfiguration;
import com.bcn.asapp.uaa.testutil.JwtFaker;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityTestConfiguration.class)
@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true)
class SecurityConfigurationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtFaker jwtFaker;

    @Nested
    class HttpBasicAuthentication {

        @Test
        @DisplayName("GIVEN authorization header is not present WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void AuthorizationHeaderIsNotPresent_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator")
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is not a bearer token WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotABearerToken_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var nonBearerToken = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, nonBearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is empty WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsEmptyBearerToken_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer ";

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is invalid WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsInvalid_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtInvalid();

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT signature is invalid WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtSignatureIsInvalid_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtWithInvalidSignature(JwtType.ACCESS_TOKEN);

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT has expired WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtHasExpired_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtExpired(JwtType.ACCESS_TOKEN);

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is not signed WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotSigned_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtNotSigned(JwtType.ACCESS_TOKEN);

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT has not type WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtHasNotType_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtWithoutType();

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT has invalid type WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtHasInvalidType_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwtWithInvalidType();

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is a refresh token WHEN call secured endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsRefreshToken_CallSecuredEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            // When & Then
            var bearerToken = "Bearer " + jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

    }

    @Nested
    class ActuatorAuthentication {

        @Test
        @DisplayName("GIVEN JWT is not present WHEN call actuator endpoint THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_CallActuatorEndpoint_ReturnsStatusUnauthorizedAndEmptyBody() {
            webTestClient.get()
                         .uri("/actuator")
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is present WHEN call actuator endpoint THEN returns HTTP response with status OK And the body with the actuator content")
        void JwtIsPresent_CallActuatorEndpoint_ReturnsStatusOkAndBodyWithContent() {
            // Given
            var fakeUsername = "TEST USERNAME";
            var fakePasswordBcryptEncoded = "{bcrypt}" + new BCryptPasswordEncoder().encode("TEST PASSWORD");

            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var userToBeLogin = userRepository.save(fakeUser);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, userToBeLogin.id(), fakeAccessJwt, Instant.now(), Instant.now());
            accessTokenRepository.save(fakeAccessToken);

            // When & Then
            var bearerToken = "Bearer " + fakeAccessJwt;

            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .value(body -> assertThat(body, not(emptyOrNullString())));

            // Clean
            accessTokenRepository.deleteAll();
            userRepository.deleteAll();
        }

        @Test
        @DisplayName("GIVEN JWT is not present WHEN call actuator health endpoint THEN returns HTTP response with status OK And the body with the actuator content")
        void JwtIsNotPresent_CallActuatorHealthEndpoint_ReturnsStatusOkAndBodyWithContent() {
            webTestClient.get()
                         .uri("/actuator/health")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .value(body -> assertThat(body, not(emptyOrNullString())));
        }

    }

    @Nested
    class SwaggerAuthentication {

        @Test
        @DisplayName("GIVEN JWT is not present WHEN call swagger endpoint THEN returns HTTP response with status Found And a location header pointing to index And empty body")
        void JwtIsNotPresent_CallSwaggerEndpoint_ReturnsStatusFoundAndHeaderLocationToSwaggerIndexAndEmptyBody() {
            webTestClient.get()
                         .uri("/swagger-ui.html")
                         .exchange()
                         .expectStatus()
                         .isFound()
                         .expectHeader()
                         .location("/asapp-uaa-service/swagger-ui/index.html")
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN JWT is not present WHEN call swagger index endpoint THEN returns HTTP response with status OK And the body with the index content")
        void JwtIsNotPresent_CallSwaggerIndexEndpoint_ReturnsStatusOkAndBodyWithContent() {
            webTestClient.get()
                         .uri("/swagger-ui/index.html")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .value(body -> assertThat(body, not(emptyOrNullString())));
        }

    }

    @Nested
    class OpenApiAuthentication {

        @Test
        @DisplayName("GIVEN JWT is not present WHEN call OpenApi endpoint THEN returns HTTP response with status OK And the body with the OpeApi content")
        void JwtIsNotPresent_CallOpenApiEndpoint_ReturnsStatusOkAndBodyWithContent() {
            webTestClient.get()
                         .uri("/v3/api-docs")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .value(body -> assertThat(body, not(emptyOrNullString())));
        }

    }

}
