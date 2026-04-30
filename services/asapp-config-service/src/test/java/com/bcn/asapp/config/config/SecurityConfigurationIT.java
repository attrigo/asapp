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

package com.bcn.asapp.config.config;

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
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.config.AsappConfigServiceApplication;

/**
 * Tests {@link SecurityConfiguration} HTTP Basic authentication rules enforced by the application.
 * <p>
 * Coverage:
 * <li>Rejects config endpoint requests without credentials</li>
 * <li>Rejects config endpoint requests with invalid credentials</li>
 * <li>Allows access to protected actuator endpoints with valid credentials</li>
 * <li>Rejects protected actuator endpoint requests with invalid credentials</li>
 * <li>Rejects protected actuator endpoint requests without credentials</li>
 * <li>Allows public access to health actuator endpoint without credentials</li>
 * <li>Allows public access to liveness and readiness endpoints without credentials</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SecurityConfigurationIT {

    @Autowired
    private RestTestClient restTestClient;

    @Nested
    class ConfigEndpointsAuthentication {

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_NoAuthorizationHeaderOnConfigEndpoint() {
            // When & Then
            restTestClient.get()
                          .uri("/asapp-tasks-service/default")
                          .exchange()
                          .expectStatus()
                          .isUnauthorized()
                          .expectBody()
                          .isEmpty();
        }

        @Test
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidCredentialsOnConfigEndpoint() {
            // When & Then
            restTestClient.get()
                          .uri("/asapp-tasks-service/default")
                          .headers(h -> h.setBasicAuth("wrong-user", "wrong-password"))
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
        private String configUsername;

        @Value("${spring.security.user.password}")
        private String configPassword;

        private RestTestClient managementRestTestClient;

        @BeforeEach
        void setUp() {
            managementRestTestClient = RestTestClient.bindToServer()
                                                     .baseUrl("http://localhost:" + managementPort + contextPath)
                                                     .build();
        }

        @ParameterizedTest
        @MethodSource("protectedEndpoints")
        void ReturnsStatusOk_ValidCredentialsOnProtectedEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("protectedEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_InvalidCredentialsOnProtectedEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .headers(h -> h.setBasicAuth("wrong-user", "wrong-password"))
                                    .exchange()
                                    .expectStatus()
                                    .isUnauthorized()
                                    .expectBody()
                                    .isEmpty();
        }

        @ParameterizedTest
        @MethodSource("protectedEndpoints")
        void ReturnsStatusUnauthorizedAndEmptyBody_NoAuthorizationHeaderOnProtectedEndpoint(String endpoint) {
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
        @MethodSource("publicEndpoints")
        void ReturnsStatusOk_NoAuthorizationHeaderOnPublicEndpoint(String endpoint) {
            // When & Then
            managementRestTestClient.get()
                                    .uri(endpoint)
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @ParameterizedTest
        @MethodSource("serverPortPublicEndpoints")
        void ReturnsStatusOk_NoAuthorizationHeaderOnServerPortPublicEndpoint(String endpoint) {
            // When & Then
            restTestClient.get()
                          .uri(endpoint)
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

        private static Stream<String> protectedEndpoints() {
            return Stream.of("/actuator", "/actuator/beans", "/actuator/info", "/actuator/conditions", "/actuator/configprops", "/actuator/env",
                    "/actuator/loggers", "/actuator/heapdump", "/actuator/threaddump", "/actuator/prometheus", "/actuator/metrics", "/actuator/sbom",
                    "/actuator/scheduledtasks", "/actuator/mappings");
        }

        private static Stream<String> publicEndpoints() {
            return Stream.of("/actuator/health");
        }

        private static Stream<String> serverPortPublicEndpoints() {
            return Stream.of("/livez", "/readyz");
        }

    }

}
