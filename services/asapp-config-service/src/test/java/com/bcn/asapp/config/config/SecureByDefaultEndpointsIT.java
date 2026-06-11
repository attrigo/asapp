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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.config.AsappConfigServiceApplication;

/**
 * Tests the secure-by-default (prod) posture of the config service endpoint exposure and access control.
 * <p>
 * Coverage:
 * <li>Actuator root exposes only health, info and prometheus links when exposure is narrowed</li>
 * <li>Sensitive actuator endpoints (env, heapdump, shutdown) are not reachable when locked down</li>
 * <li>Protected actuator endpoints require Basic authentication</li>
 * <li>Health endpoint remains publicly accessible without credentials</li>
 * <li>Liveness and readiness probes remain publicly accessible without credentials</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@TestPropertySource(properties = { "management.endpoints.web.exposure.include=health,info,prometheus", "management.endpoint.heapdump.access=none",
        "management.endpoint.shutdown.access=none", "management.info.env.enabled=false" })
class SecureByDefaultEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

    @Nested
    class ActuatorExposure {

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

        @Test
        void BodyContainsOnlyHealthInfoPrometheusLinks_ExposureNarrowed() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator")
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectBody(String.class)
                                    .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                       .node("_links")
                                                                                                       .isObject()
                                                                                                       .containsKeys("self", "health", "health-path", "info",
                                                                                                               "prometheus")
                                                                                                       .doesNotContainKeys("beans", "env", "env-toMatch",
                                                                                                               "heapdump", "threaddump", "shutdown", "loggers",
                                                                                                               "loggers-name", "mappings", "conditions",
                                                                                                               "configprops", "configprops-prefix", "sbom",
                                                                                                               "sbom-id", "metrics",
                                                                                                               "metrics-requiredMetricName", "scheduledtasks"));
        }

        @Test
        void ReturnsStatusNotFound_EnvEndpointNotExposed() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/env")
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isNotFound();
        }

        @Test
        void ReturnsStatusNotFound_HeapdumpEndpointNotExposed() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/heapdump")
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isNotFound();
        }

        @Test
        void ReturnsStatusNotFound_ShutdownEndpointNotExposed() {
            // When & Then
            managementRestTestClient.post()
                                    .uri("/actuator/shutdown")
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isNotFound();
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

        @Test
        void ReturnsStatusUnauthorized_InfoEndpointWithoutCredentials() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/info")
                                    .exchange()
                                    .expectStatus()
                                    .isUnauthorized();
        }

        @Test
        void ReturnsStatusUnauthorized_PrometheusEndpointWithoutCredentials() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/prometheus")
                                    .exchange()
                                    .expectStatus()
                                    .isUnauthorized();
        }

        @Test
        void ReturnsStatusOk_InfoEndpointWithValidCredentials() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/info")
                                    .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

    }

    @Nested
    class Probes {

        @LocalManagementPort
        private int managementPort;

        @Value("${server.servlet.context-path}")
        private String contextPath;

        private RestTestClient managementRestTestClient;

        @BeforeEach
        void setUp() {
            managementRestTestClient = RestTestClient.bindToServer()
                                                     .baseUrl("http://localhost:" + managementPort + contextPath)
                                                     .build();
        }

        @Test
        void ReturnsStatusOk_HealthEndpointPublic() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator/health")
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
        }

        @Test
        void ReturnsStatusOk_LivezProbePublic() {
            // When & Then
            restTestClient.get()
                          .uri("/livez")
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

        @Test
        void ReturnsStatusOk_ReadyzProbePublic() {
            // When & Then
            restTestClient.get()
                          .uri("/readyz")
                          .exchange()
                          .expectStatus()
                          .isOk();
        }

    }

}
