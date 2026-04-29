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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.discovery.AsappDiscoveryServiceApplication;

/**
 * Tests Eureka server endpoints exposed by the application.
 * <p>
 * Coverage:
 * <li>Eureka apps endpoint returns a valid response with an applications registry when valid credentials are provided</li>
 */
@SpringBootTest(classes = AsappDiscoveryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EurekaServerEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

    @Value("${spring.security.user.name}")
    private String discoveryUsername;

    @Value("${spring.security.user.password}")
    private String discoveryPassword;

    @Test
    void ReturnsStatusOkAndBodyContainsApplications_ValidCredentialsOnEurekaAppsEndpoint() {
        // When & Then
        restTestClient.get()
                      .uri("/eureka/apps")
                      .accept(MediaType.APPLICATION_JSON)
                      .headers(h -> h.setBasicAuth(discoveryUsername, discoveryPassword))
                      .exchange()
                      .expectStatus()
                      .isOk()
                      .expectBody(String.class)
                      .consumeWith(response -> assertThatJson(response.getResponseBody()).isNotNull()
                                                                                         .node("applications")
                                                                                         .isPresent());
    }

}
