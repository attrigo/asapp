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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.bcn.asapp.config.AsappConfigServiceApplication;

/**
 * Tests {@link SecurityConfiguration} HTTP Basic authentication rules enforced by the application.
 * <p>
 * Coverage:
 * <li>Rejects request without credentials</li>
 * <li>Rejects request with invalid credentials</li>
 */
@SpringBootTest(classes = AsappConfigServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SecurityConfigurationIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ReturnsStatusUnauthorizedAndEmptyBody_OnConfigEndpointWithoutCredentials() {
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
    void ReturnsStatusUnauthorizedAndEmptyBody_OnConfigEndpointWithInvalidCredentials() {
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
