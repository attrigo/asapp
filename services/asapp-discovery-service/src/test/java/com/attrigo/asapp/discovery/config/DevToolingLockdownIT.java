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

package com.attrigo.asapp.discovery.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.attrigo.asapp.discovery.AsappDiscoveryServiceApplication;

/**
 * Tests that Actuator exposure is locked down without the dev profile.
 * <p>
 * Setup:
 * <li>Loads the full discovery server context on a random port with narrowed actuator exposure and builds a REST client targeting the management port before
 * each test</li>
 * <p>
 * Coverage:
 * <li>Actuator root exposes only health, info, prometheus and sbom links when exposure is narrowed</li>
 */
@SpringBootTest(classes = AsappDiscoveryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @formatter:off
@TestPropertySource(properties = {
        "management.endpoint.heapdump.access=none",
        "management.endpoint.shutdown.access=none",
        "management.endpoints.web.exposure.include=health,info,prometheus,sbom",
        "management.info.env.enabled=false" })
// @formatter:on
class DevToolingLockdownIT {

    @LocalManagementPort
    private int managementPort;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${spring.security.user.name}")
    private String discoveryUsername;

    @Value("${spring.security.user.password}")
    private String discoveryPassword;

    private RestTestClient managementRestTestClient;

    @BeforeEach
    void setUp() {
        managementRestTestClient = RestTestClient.bindToServer()
                                                 .baseUrl("http://localhost:" + managementPort + contextPath)
                                                 .build();
    }

    @Nested
    class ActuatorExposure {

        @Test
        void ReturnsStatusOkAndBodyContainsOnlyHealthInfoPrometheusSbomLinks_OnActuatorEndpoint() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator")
                                    .headers(h -> h.setBasicAuth(discoveryUsername, discoveryPassword))
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectBody(String.class)
                                    .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                       .node("_links")
                                                                                                       .isObject()
                                                                                                       .containsOnlyKeys("self", "health", "health-path",
                                                                                                               "info", "prometheus", "sbom", "sbom-id"));
        }

    }

}
