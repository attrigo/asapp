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

package com.bcn.asapp.tasks.infrastructure.config;

import static com.bcn.asapp.tasks.infrastructure.security.JwtValidator.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.tasks.AsappTasksServiceApplication;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class SecurityConfigurationIT {

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class HttpBasicAuthentication {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderIsNotPresent() {
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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsInvalidBearerToken() {
            // When & Then
            var nonBearerToken = "invalid_bearer_token";

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsEmptyBearerToken() {
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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderNotContainsBearerToken() {
            // When & Then
            var bearerToken = "Bearer " + "invalid_bearer_token";

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsExpiredBearerTokenWithWrongSignature() {
            // When & Then
            var bearerToken = "Bearer " + testEncodedTokenBuilder().accessToken()
                                                                   .withSecretKey("M0LBjhuY5Xgk25aRFCTp72EXM2HEnRY7KHAIlNQCxzwsMw7HgQBbdN4Mka94siHP")
                                                                   .build();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsExpiredBearerToken() {
            // When & Then
            var bearerToken = "Bearer " + testEncodedTokenBuilder().accessToken()
                                                                   .expired()
                                                                   .build();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsNotSignedBearerToken() {
            // When & Then
            var bearerToken = "Bearer " + testEncodedTokenBuilder().accessToken()
                                                                   .notSigned()
                                                                   .build();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsBearerTokenWithoutType() {
            // When & Then
            var bearerToken = "Bearer " + testEncodedTokenBuilder().withType("")
                                                                   .notSigned()
                                                                   .build();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsBearerTokenWithInvalidType() {
            // When & Then
            var bearerToken = "Bearer " + testEncodedTokenBuilder().withType("invalid_type")
                                                                   .notSigned()
                                                                   .build();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsRefreshBearerToken() {
            // When & Then
            var bearerToken = "Bearer " + defaultTestEncodedRefreshToken();

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
        void ReturnsStatusUnauthorizedAndEmptyBody_AuthorizationHeaderContainsValidBearerTokenNotExistsInRedis() {
            // When & Then
            var bearerToken = "Bearer " + defaultTestEncodedAccessToken();

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

        @Autowired
        private RedisTemplate<String, String> redisTemplate;

        private final String accessToken = defaultTestEncodedAccessToken();

        private final String bearerToken = "Bearer " + accessToken;

        @BeforeEach
        void beforeEach() {
            redisTemplate.opsForValue()
                         .set(ACCESS_TOKEN_PREFIX + accessToken, "");
        }

        @AfterEach
        void afterEach() {
            redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_AccessActuatorEndpoint_AuthorizationHeaderIsNotPresent() {
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
        void ReturnsStatusOkAndBodyContainsAllActuatorLinks_AccessActuatorEndpoint_AuthorizationHeaderContainsValidBearerToken() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .containsKeys("_links")
                                                                       .node("_links")
                                                                       .isObject()
                                                                       .containsKeys("self", "beans", "caches-cache", "caches", "health", "health-path", "info",
                                                                               "conditions", "shutdown", "configprops", "configprops-prefix", "env",
                                                                               "env-toMatch", "liquibase", "loggers", "loggers-name", "heapdump", "threaddump",
                                                                               "prometheus", "metrics-requiredMetricName", "metrics", "sbom-id", "sbom",
                                                                               "scheduledtasks", "httpexchanges", "mappings");
                         });
        }

        @Test
        void ReturnsStatusOkAndBodyContainsStatusAndGroups_AccessActuatorHealthEndpoint_AuthorizationHeaderIsNotPresent() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator/health")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .containsKeys("status", "groups")
                                                                       .node("groups")
                                                                       .isArray()
                                                                       .contains("liveness", "readiness");
                         });
        }

        @Test
        void ReturnsStatusOkAndBodyContainsStatusAndHealthDetails_AccessActuatorHealthEndpoint_AuthorizationHeaderContainsValidBearerToken() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator/health")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .containsKeys("status", "groups", "components")
                                                                       .node("components")
                                                                       .isObject()
                                                                       .containsKeys("db", "diskSpace", "livenessState", "ping", "readinessState", "ssl");
                         });
        }

        @Test
        void ReturnsStatusOkAndBodyContainsGitBuildJavaOsProcessDetails_AccessActuatorInfoEndpoint_AuthorizationHeaderContainsValidBearerToken() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator/info")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .containsKeys("git", "build", "java", "os", "process");
                         });
        }

        @Test
        void ReturnsStatusOkAndBodyContainsSBOMApplicationId_AccessActuatorSBOMEndpoint_AuthorizationHeaderContainsValidBearerToken() {
            // When & Then
            webTestClient.get()
                         .uri("/actuator/sbom")
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .containsKeys("ids")
                                                                       .node("ids")
                                                                       .isArray();
                         });
        }

    }

    @Nested
    class SwaggerAuthentication {

        @Test
        void ReturnsStatusFoundAndHeaderLocationToSwaggerIndexAndEmptyBody_AccessSwaggerEndpoint_AuthorizationHeaderIsNotPresent() {
            // When & Then
            webTestClient.get()
                         .uri("/swagger-ui.html")
                         .exchange()
                         .expectStatus()
                         .isFound()
                         .expectHeader()
                         .location("/asapp-tasks-service/swagger-ui/index.html")
                         .expectBody()
                         .isEmpty();
        }

        @Test
        void ReturnsStatusOkAndBodyWithContent_AccessSwaggerIndexEndpoint_AuthorizationHeaderContainsValidBearerToken() {
            // When & Then
            webTestClient.get()
                         .uri("/swagger-ui/index.html")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThat(response.getResponseBody()).isNotBlank();
                         });
        }

    }

    @Nested
    class OpenApiAuthentication {

        @Test
        void ReturnsStatusOkAndBodyWithContent_AccessOpenApiEndpoint_AuthorizationHeaderIsNotPresent() {
            // When & Then
            webTestClient.get()
                         .uri("/v3/api-docs")
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectBody(String.class)
                         .consumeWith(response -> {
                             assertThatJson(response.getResponseBody()).isNotNull()
                                                                       .isObject()
                                                                       .isNotEmpty();
                         });
        }

    }

}
