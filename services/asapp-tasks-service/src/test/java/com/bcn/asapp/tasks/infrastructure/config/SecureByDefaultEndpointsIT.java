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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.tasks.AsappTasksServiceApplication;
import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests the secure-by-default (prod) endpoint exposure and availability.
 * <p>
 * Coverage:
 * <li>Actuator root exposes only health, info, prometheus and sbom links when exposure is narrowed</li>
 * <li>Swagger UI endpoint is not reachable when springdoc is disabled</li>
 * <li>OpenAPI documentation endpoint is not reachable when springdoc is disabled</li>
 */
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
@TestPropertySource(properties = { "management.endpoints.web.exposure.include=health,info,prometheus,sbom", "management.endpoint.heapdump.access=none",
        "management.endpoint.shutdown.access=none", "management.info.env.enabled=false", "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false" })
class SecureByDefaultEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

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
    void beforeEach() {
        managementRestTestClient = RestTestClient.bindToServer()
                                                 .baseUrl("http://localhost:" + managementPort + contextPath)
                                                 .build();
    }

    @Nested
    class Swagger {

        @Test
        void ReturnsStatusNotFound_SwaggerDisabled() {
            // When & Then
            restTestClient.get()
                          .uri("/swagger-ui/index.html")
                          .exchange()
                          .expectStatus()
                          .isNotFound();
        }

        @Test
        void ReturnsStatusNotFound_ApiDocsDisabled() {
            // When & Then
            restTestClient.get()
                          .uri("/v3/api-docs")
                          .exchange()
                          .expectStatus()
                          .isNotFound();
        }

    }

    @Nested
    class ActuatorExposure {

        @Test
        void BodyContainsOnlyHealthInfoPrometheusLinks_ExposureNarrowed() {
            // When & Then
            managementRestTestClient.get()
                                    .uri("/actuator")
                                    .headers(h -> h.setBasicAuth(managementUsername, managementPassword))
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
