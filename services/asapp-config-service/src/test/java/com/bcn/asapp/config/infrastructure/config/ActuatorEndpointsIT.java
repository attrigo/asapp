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

package com.bcn.asapp.config.infrastructure.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.config.AsappConfigServiceApplication;

/**
 * Tests actuator endpoint content exposed by the application.
 * <p>
 * Coverage:
 * <li>Actuator root exposes all expected endpoint links</li>
 * <li>Health endpoint returns status</li>
 * <li>Info endpoint returns build, git, java, os and process details</li>
 * <li>SBOM endpoint returns application SBOM identifiers</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ActuatorEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ReturnsStatusOkAndBodyContainsAllActuatorLinks_OnActuatorEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator")
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                         .isObject()
                                                                                         .containsKeys("_links")
                                                                                         .node("_links")
                                                                                         .isObject()
                                                                                         .containsKeys("self", "beans", "health", "health-path", "info",
                                                                                                 "conditions", "shutdown", "configprops", "configprops-prefix",
                                                                                                 "env", "env-toMatch", "loggers", "loggers-name", "heapdump",
                                                                                                 "threaddump", "metrics-requiredMetricName", "metrics",
                                                                                                 "sbom-id", "sbom", "scheduledtasks", "mappings"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsStatus_OnHealthEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator/health")
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
        restTestClient.get()
                      .uri("/actuator/info")
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
        restTestClient.get()
                      .uri("/actuator/sbom")
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
