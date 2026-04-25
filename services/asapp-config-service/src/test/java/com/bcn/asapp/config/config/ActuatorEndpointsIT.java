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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.config.AsappConfigServiceApplication;

/**
 * Tests actuator endpoint content exposed by the application.
 * <p>
 * Coverage:
 * <li>Actuator root exposes all expected endpoint links</li>
 * <li>Health endpoint returns status and groups without authentication</li>
 * <li>Health endpoint returns status with authentication</li>
 * <li>Info endpoint returns build, git, java, os and process details</li>
 * <li>SBOM endpoint returns application SBOM identifiers</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorEndpointsIT {

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
    void ReturnsStatusOkAndBodyContainsAllActuatorLinks_OnActuatorEndpoint() {
        // When & Then
        managementRestTestClient.get()
                                .uri("/actuator")
                                .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(String.class)
                                .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                   .isObject()
                                                                                                   .containsKeys("_links")
                                                                                                   .node("_links")
                                                                                                   .isObject()
                                                                                                   .containsKeys("self", "beans", "health", "health-path",
                                                                                                           "info", "conditions", "shutdown", "configprops",
                                                                                                           "configprops-prefix", "env", "env-toMatch",
                                                                                                           "loggers", "loggers-name", "heapdump", "threaddump",
                                                                                                           "metrics-requiredMetricName", "metrics", "sbom-id",
                                                                                                           "sbom", "scheduledtasks", "mappings"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsStatusAndGroups_OnHealthEndpointWithoutCredentials() {
        // When & Then
        managementRestTestClient.get()
                                .uri("/actuator/health")
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(String.class)
                                .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                   .isObject()
                                                                                                   .containsKeys("status", "groups")
                                                                                                   .node("groups")
                                                                                                   .isArray()
                                                                                                   .contains("liveness", "readiness"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsStatus_OnHealthEndpointWithCredentials() {
        // When & Then
        managementRestTestClient.get()
                                .uri("/actuator/health")
                                .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(String.class)
                                .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                   .isObject()
                                                                                                   .containsKeys("status"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsGitBuildJavaOsProcessDetails_OnInfoEndpoint() {
        // When & Then
        managementRestTestClient.get()
                                .uri("/actuator/info")
                                .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(String.class)
                                .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                   .isObject()
                                                                                                   .containsKeys("git", "build", "java", "os", "process"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsSBOMIds_OnSBOMEndpoint() {
        // When & Then
        managementRestTestClient.get()
                                .uri("/actuator/sbom")
                                .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                .exchange()
                                .expectStatus()
                                .isOk()
                                .expectBody(String.class)
                                .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                                   .isObject()
                                                                                                   .containsKeys("ids")
                                                                                                   .node("ids")
                                                                                                   .isArray());
    }

}
