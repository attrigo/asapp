/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.infrastructure.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.bcn.asapp.authentication.AsappAuthenticationServiceApplication;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests OpenAPI and Swagger UI endpoint content exposed by the application.
 * <p>
 * Coverage:
 * <li>Swagger UI redirect endpoint returns 302 with Location header pointing to Swagger index page</li>
 * <li>Swagger UI index page returns HTML content</li>
 * <li>OpenAPI documentation endpoint returns a specification with the expected API info and security scheme</li>
 * <li>OpenAPI documentation endpoint returns a valid non-empty API specification</li>
 */
@SpringBootTest(classes = AsappAuthenticationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TestContainerConfiguration.class)
class OpenApiEndpointsIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void ReturnsStatusFoundAndHeaderLocationToSwaggerIndexAndEmptyBody_OnSwaggerEndpoint() {
        // When & Then
        webTestClient.get()
                     .uri("/swagger-ui.html")
                     .exchange()
                     .expectStatus()
                     .isFound()
                     .expectHeader()
                     .location("/asapp-authentication-service/swagger-ui/index.html")
                     .expectBody()
                     .isEmpty();
    }

    @Test
    void ReturnsStatusOkAndBodyWithHtmlContent_OnSwaggerIndexEndpoint() {
        // When & Then
        webTestClient.get()
                     .uri("/swagger-ui/index.html")
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(String.class)
                     .consumeWith(response -> {
                         assertThat(response.getResponseBody()).isNotBlank();
                     });
    }

    @Test
    void ReturnsStatusOkAndBodyContainsApiInfoAndSecurityScheme_OnOpenApiDocs() {
        // When & Then
        webTestClient.get()
                     .uri("/v3/api-docs")
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(String.class)
                     .consumeWith(response -> {
                         String body = response.getResponseBody();
                         assertThatJson(body).node("info")
                                             .isObject()
                                             .containsEntry("title", "Authentication Service API")
                                             .containsEntry("description", "Provides authentication operations")
                                             .node("license")
                                             .isObject()
                                             .containsEntry("name", "Apache-2.0");
                         assertThatJson(body).node("components.securitySchemes")
                                             .node("Bearer Authentication")
                                             .isObject()
                                             .containsEntry("type", "http")
                                             .containsEntry("scheme", "bearer")
                                             .containsEntry("bearerFormat", "JWT");
                     });
    }

    @Test
    void ReturnsStatusOkAndBodyWithApiSpec_OnOpenApiDocs() {
        // When & Then
        webTestClient.get()
                     .uri("/v3/api-docs")
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(String.class)
                     .consumeWith(response -> {
                         assertThatJson(response.getResponseBody()).isNotNull()
                                                                   .isObject()
                                                                   .isNotEmpty();
                     });
    }

}
