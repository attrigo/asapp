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

package com.attrigo.asapp.config.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.attrigo.asapp.config.AsappConfigServiceApplication;

/**
 * Tests Spring Cloud Config Server endpoints exposed by the application.
 * <p>
 * Setup:
 * <li>Loads the full config server context on a random port with an auto-configured REST client</li>
 * <p>
 * Coverage:
 * <li>Config endpoint returns a valid response for a known application and profile</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ConfigServerEndpointsIT {

    @Value("${spring.security.user.name}")
    private String configUsername;

    @Value("${spring.security.user.password}")
    private String configPassword;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ReturnsStatusOkAndBodyContainsNameAndPropertySources_OnConfigEndpoint() {
        // When
        var actual = restTestClient.get()
                                   .uri("/asapp-tasks-service/default")
                                   .headers(h -> h.setBasicAuth(configUsername, configPassword))
                                   .exchange()
                                   .expectStatus()
                                   .isOk()
                                   .expectBody(String.class)
                                   .returnResult()
                                   .getResponseBody();

        // Then
        assertThatJson(actual).node("name")
                              .isEqualTo("asapp-tasks-service");
        assertThatJson(actual).node("propertySources")
                              .isArray()
                              .isNotEmpty();
    }

}
