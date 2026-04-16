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

package com.bcn.asapp.users.infrastructure.config;

import static com.bcn.asapp.users.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests actuator endpoint content exposed by the application.
 * <p>
 * Coverage:
 * <li>Actuator root exposes all expected endpoint links</li>
 * <li>Health endpoint returns status and groups without authentication</li>
 * <li>Health endpoint returns full component details with authentication</li>
 * <li>Info endpoint returns build, git, java, os and process details</li>
 * <li>SBOM endpoint returns application SBOM identifiers</li>
 * <li>Liquibase endpoint returns executed changesets</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class ActuatorEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String encodedAccessToken = encodedAccessToken();

    private final String bearerToken = "Bearer " + encodedAccessToken;

    @BeforeEach
    void beforeEach() {
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + encodedAccessToken);
        redisTemplate.opsForValue()
                     .set(ACCESS_TOKEN_PREFIX + encodedAccessToken, "");
    }

    @Test
    void ReturnsStatusOkAndBodyContainsAllActuatorLinks_OnActuatorEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator")
                      .header(HttpHeaders.AUTHORIZATION, bearerToken)
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
                                                                                                 "env", "env-toMatch", "liquibase", "loggers", "loggers-name",
                                                                                                 "heapdump", "threaddump", "prometheus",
                                                                                                 "metrics-requiredMetricName", "metrics", "sbom-id", "sbom",
                                                                                                 "scheduledtasks", "httpexchanges", "mappings"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsStatusAndGroups_OnHealthEndpointWithoutAuthentication() {
        // When & Then
        restTestClient.get()
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
    void ReturnsStatusOkAndBodyContainsStatusGroupsAndComponents_OnHealthEndpointWithAuthentication() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator/health")
                      .header(HttpHeaders.AUTHORIZATION, bearerToken)
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                         .isObject()
                                                                                         .containsKeys("status", "groups", "components")
                                                                                         .node("components")
                                                                                         .isObject()
                                                                                         .containsKeys("db", "diskSpace", "livenessState", "ping",
                                                                                                 "readinessState", "ssl"));
    }

    @Test
    void ReturnsStatusOkAndBodyContainsGitBuildJavaOsProcessDetails_OnInfoEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator/info")
                      .header(HttpHeaders.AUTHORIZATION, bearerToken)
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
                      .header(HttpHeaders.AUTHORIZATION, bearerToken)
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

    @Test
    void ReturnsStatusOkAndBodyContainsChangesets_OnLiquibaseEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/actuator/liquibase")
                      .header(HttpHeaders.AUTHORIZATION, bearerToken)
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                         .isObject()
                                                                                         .containsKeys("contexts")
                                                                                         .node("contexts")
                                                                                         .isObject()
                                                                                         .isNotEmpty());
    }

}
