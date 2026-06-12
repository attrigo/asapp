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

package com.bcn.asapp.discovery.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.discovery.AsappDiscoveryServiceApplication;

/**
 * Tests the secure-by-default (prod) Actuator endpoint exposure.
 * <p>
 * Coverage:
 * <li>Actuator root exposes only health, info and prometheus links when exposure is narrowed</li>
 */
@SpringBootTest(classes = AsappDiscoveryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "management.endpoints.web.exposure.include=health,info,prometheus", "management.endpoint.heapdump.access=none",
        "management.endpoint.shutdown.access=none", "management.info.env.enabled=false" })
class SecureByDefaultEndpointsIT {

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
        void BodyContainsOnlyHealthInfoPrometheusLinks_ExposureNarrowed() {
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
                                                                                                               "info", "prometheus"));
        }

    }

}
