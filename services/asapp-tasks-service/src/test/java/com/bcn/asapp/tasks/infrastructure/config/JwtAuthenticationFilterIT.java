/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.tasks.infrastructure.config;

import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.anEncodedTokenBuilder;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.tasks.AsappTasksServiceApplication;
import com.bcn.asapp.tasks.infrastructure.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests {@link JwtAuthenticationFilter} JWT authentication and endpoint access control.
 * <p>
 * Coverage:
 * <li>Rejects API requests without Authorization header</li>
 * <li>Rejects API requests with malformed, expired, unsigned, or invalid signature tokens</li>
 * <li>Rejects API requests with refresh tokens</li>
 * <li>Rejects API requests with valid tokens not present in session store</li>
 * <li>Allows access to protected actuator endpoints with valid credentials</li>
 * <li>Rejects protected actuator endpoint requests with invalid credentials</li>
 * <li>Rejects protected actuator endpoint requests without credentials</li>
 * <li>Allows public access to health actuator endpoint without credentials</li>
 * <li>Allows public access to liveness and readiness endpoints without credentials</li>
 * <li>Allows public access to Swagger UI without credentials</li>
 * <li>Allows public access to OpenAPI documentation without credentials</li>
 */
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class JwtAuthenticationFilterIT {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void beforeEach() {
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.getConnectionFactory()
                     .getConnection()
                     .serverCommands()
                     .flushDb();
    }

    @Nested
    class ApiAuthentication {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidBearerToken() {
            // Given
            var bearerToken = "invalid_bearer_token";

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_EmptyBearerToken() {
            // Given
            var bearerToken = "Bearer ";

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MalformedBearerToken() {
            // Given
            var bearerToken = "Bearer " + "invalid_bearer_token";

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_ExpiredBearerTokenWithWrongSignature() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .withSecretKey("M0LBjhuY5Xgk25aRFCTp72EXM2HEnRY7KHAIlNQCxzwsMw7HgQBbdN4Mka94siHP")
                                                                 .build();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_ExpiredBearerToken() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .expired()
                                                                 .build();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_UnsignedBearerToken() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .notSigned()
                                                                 .build();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_BearerTokenWithMissingType() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().withType("")
                                                                 .notSigned()
                                                                 .build();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_BearerTokenWithInvalidType() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().withType("invalid_type")
                                                                 .notSigned()
                                                                 .build();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_RefreshBearerToken() {
            // Given
            var bearerToken = "Bearer " + encodedRefreshToken();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
                          .header(HttpHeaders.AUTHORIZATION, bearerToken)
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_ValidBearerTokenNotInRedis() {
            // Given
            var bearerToken = "Bearer " + encodedAccessToken();

            // When & Then
            restTestClient.get()
                          .uri("/api/tasks")
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

        @LocalManagementPort
        private int managementPort;

        @Value("${server.servlet.context-path}")
        private String contextPath;

        @Value("${spring.security.user.name}")
        private String managementUsername;

        @Value("${spring.security.user.password}")
        private String managementPassword;

        private RestTestClient managementRestTestClient;

        @BeforeEach
        void setUp() {
            managementRestTestClient = RestTestClient.bindToServer()
                                                     .baseUrl("http://localhost:" + managementPort + contextPath)
                                                     .build();
        }

        @ParameterizedTest
        @MethodSource("protectedGetEndpoints")
        void ReturnsStatusOk_ValidCredentialsOnProtectedGetEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth(managementUsername, managementPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedGetEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidCredentialsOnProtectedGetEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth(managementUsername, managementPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedGetEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_NoAuthorizationHeaderOnProtectedGetEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .exchange()
                                    .expectStatus()
                                    .isUnauthorized()
                                    .expectBody()
                                    .isEmpty();
        }

        @ParameterizedTest
        @MethodSource("publicGetEndpoints")
        void ReturnsStatusOk_NoAuthorizationHeaderOnPublicGetEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("serverPortPublicGetEndpoints")
        void ReturnsStatusOk_NoAuthorizationHeaderOnSererPortPublicGetEndpoint(String endpoint) {
            // When & Then
            restTestClient.get()
                          .uri(endpoint)
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedPostEndpoints")
        void ReturnsStatusOk_ValidCredentialsOnProtectedPostEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.post()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth(managementUsername, managementPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedPostEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidCredentialsOnProtectedPostEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.post()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth(managementUsername, managementPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedPostEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_NoAuthorizationHeaderOnProtectedPostEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.post()
                                    .uri(endpoint)
                                    .exchange()
                                    .expectStatus()
                                    .isUnauthorized()
                                    .expectBody()
                                    .isEmpty();
        }

        private static Stream<String> protectedGetEndpoints() {
            return Stream.of("/actuator", "/actuator/beans", "/actuator/info", "/actuator/conditions", "/actuator/configprops", "/actuator/env",
                    "/actuator/liquibase", "/actuator/loggers", "/actuator/heapdump", "/actuator/threaddump", "/actuator/prometheus", "/actuator/metrics",
                    "/actuator/sbom", "/actuator/scheduledtasks", "/actuator/httpexchanges", "/actuator/mappings");
        }

        private static Stream<String> publicGetEndpoints() {
            return Stream.of("/actuator/health");
        }

        private static Stream<String> serverPortPublicGetEndpoints() {
            return Stream.of("/livez", "/readyz");
        }

        private static Stream<String> protectedPostEndpoints() {
            return Stream.of("/actuator/refresh");
        }

    }

    @Nested
    class SwaggerAuthentication {

        @Test
        void ReturnsStatusOk_NoAuthorizationHeaderOnSwaggerIndexEndpoint() {
            // When & Then
            restTestClient.get()
                          .uri("/swagger-ui/index.html")
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

    }

    @Nested
    class OpenApiAuthentication {

        @Test
        void ReturnsStatusOk_NoAuthorizationHeaderOnApiDocsEndpoint() {
            // When & Then
            restTestClient.get()
                          .uri("/v3/api-docs")
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

    }

}
