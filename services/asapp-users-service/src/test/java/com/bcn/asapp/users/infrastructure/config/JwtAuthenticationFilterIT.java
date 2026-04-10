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

package com.bcn.asapp.users.infrastructure.config;

import static com.bcn.asapp.users.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenFactory.anEncodedTokenBuilder;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenFactory.encodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenFactory.encodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.infrastructure.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link JwtAuthenticationFilter} JWT authentication and endpoint access control.
 * <p>
 * Coverage:
 * <li>Rejects requests without Authorization header</li>
 * <li>Rejects malformed, expired, unsigned, or invalid signature tokens</li>
 * <li>Rejects refresh tokens for access token endpoints</li>
 * <li>Rejects valid tokens not present in session store</li>
 * <li>Grants access to protected endpoints with valid authenticated token</li>
 * <li>Restricts access to protected endpoints without authentication</li>
 * <li>Allows public access to health, liveness, readiness, Swagger UI and OpenAPI documentation endpoints</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class JwtAuthenticationFilterIT {

    @Autowired
    private WebTestClient webTestClient;

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
    class HttpBasicAuthentication {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_MissingAuthorizationHeader() {
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
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidBearerToken() {
            // Given
            var bearerToken = "invalid_bearer_token";

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_EmptyBearerToken() {
            // Given
            var bearerToken = "Bearer ";

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_MalformedBearerToken() {
            // Given
            var bearerToken = "Bearer " + "invalid_bearer_token";

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_ExpiredBearerTokenWithWrongSignature() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .withSecretKey("M0LBjhuY5Xgk25aRFCTp72EXM2HEnRY7KHAIlNQCxzwsMw7HgQBbdN4Mka94siHP")
                                                                 .build();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_ExpiredBearerToken() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .expired()
                                                                 .build();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_UnsignedBearerToken() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().accessToken()
                                                                 .notSigned()
                                                                 .build();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_BearerTokenWithMissingType() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().withType("")
                                                                 .notSigned()
                                                                 .build();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_BearerTokenWithInvalidType() {
            // Given
            var bearerToken = "Bearer " + anEncodedTokenBuilder().withType("invalid_type")
                                                                 .notSigned()
                                                                 .build();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_RefreshBearerToken() {
            // Given
            var bearerToken = "Bearer " + encodedRefreshToken();

            // When & Then
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
        void ReturnsStatusUnauthorizedAndEmptyBody_ValidBearerTokenNotInRedis() {
            // Given
            var bearerToken = "Bearer " + encodedAccessToken();

            // When & Then
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

        private final String encodedAccessToken = encodedAccessToken();

        private final String bearerToken = "Bearer " + encodedAccessToken;

        @BeforeEach
        void beforeEach() {
            redisTemplate.opsForValue()
                         .set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
        }

        @ParameterizedTest
        @MethodSource("protectedEndpoints")
        void ReturnsStatusOk_AuthorizationHeaderOnEndpoint(String endpoint) {
            // When & Then
            webTestClient.get()
                         .uri(endpoint)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_NoAuthorizationHeaderOnEndpoint(String endpoint) {
            // When & Then
            webTestClient.get()
                         .uri(endpoint)
                         .exchange()
                         .expectStatus()
                         .isUnauthorized()
                         .expectBody()
                         .isEmpty();
        }

        @ParameterizedTest
        @MethodSource("publicEndpoints")
        void ReturnsStatusOk_NoAuthorizationHeaderOnEndpoint(String endpoint) {
            // When & Then
            webTestClient.get()
                         .uri(endpoint)
                         .exchange()
                         .expectStatus()
                         .isOk();
        }

        private static Stream<String> protectedEndpoints() {
            return Stream.of("/actuator", "/actuator/beans", "/actuator/info", "/actuator/conditions", "/actuator/configprops", "/actuator/env",
                    "/actuator/liquibase", "/actuator/loggers", "/actuator/heapdump", "/actuator/threaddump", "/actuator/prometheus", "/actuator/metrics",
                    "/actuator/sbom", "/actuator/scheduledtasks", "/actuator/httpexchanges", "/actuator/mappings");
        }

        private static Stream<String> publicEndpoints() {
            return Stream.of("/actuator/health", "/livez", "/readyz");
        }

    }

    @Nested
    class SwaggerAuthentication {

        @Test
        void ReturnsStatusOk_NoAuthorizationHeaderOnSwaggerIndexEndpoint() {
            // When & Then
            webTestClient.get()
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
            webTestClient.get()
                         .uri("/v3/api-docs")
                         .exchange()
                         .expectStatus()
                         .isOk();
        }

    }

}
