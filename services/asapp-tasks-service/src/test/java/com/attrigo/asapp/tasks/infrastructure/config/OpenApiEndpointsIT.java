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

package com.attrigo.asapp.tasks.infrastructure.config;

import static com.attrigo.asapp.url.tasks.TaskApiUrl.TASKS_GET_BY_ID_FULL_PATH;
import static com.attrigo.asapp.url.tasks.TaskApiUrl.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.attrigo.asapp.url.tasks.TaskApiUrl.TASKS_GET_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.attrigo.asapp.tasks.AsappTasksServiceApplication;
import com.attrigo.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests OpenAPI and Swagger UI endpoint content exposed by the application.
 * <p>
 * Setup:
 * <li>Loads the full application context backed by a Testcontainers PostgreSQL instance and an embedded Redis</li>
 * <p>
 * Coverage:
 * <li>Swagger UI index page returns HTML content</li>
 * <li>OpenAPI documentation endpoint returns a specification with the expected API info and security scheme</li>
 * <li>OpenAPI documentation endpoint exposes only the service's business operation paths and methods</li>
 */
@SpringBootTest(classes = AsappTasksServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestContainerConfiguration.class)
class OpenApiEndpointsIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void ReturnsStatusOkAndBodyWithHtmlContent_OnSwaggerIndexEndpoint() {
        // When
        var actual = restTestClient.get()
                                   .uri("/swagger-ui/index.html")
                                   .exchange()
                                   .expectStatus()
                                   .isOk()
                                   .expectBody(String.class)
                                   .returnResult()
                                   .getResponseBody();

        // Then
        assertThat(actual).isNotBlank();
    }

    @Test
    void ReturnsStatusOkAndBodyContainsApiInfoAndSecurityScheme_OnOpenApiDocs() {
        // When
        var actual = restTestClient.get()
                                   .uri("/v3/api-docs")
                                   .exchange()
                                   .expectStatus()
                                   .isOk()
                                   .expectBody(String.class)
                                   .returnResult()
                                   .getResponseBody();

        // Then
        assertThatJson(actual).node("info")
                              .isObject()
                              .containsEntry("title", "Tasks Service API")
                              .containsEntry("description", "Provides tasks operations")
                              .node("license")
                              .isObject()
                              .containsEntry("name", "Apache-2.0");
        assertThatJson(actual).node("components.securitySchemes")
                              .node("Bearer Authentication")
                              .isObject()
                              .containsEntry("type", "http")
                              .containsEntry("scheme", "bearer")
                              .containsEntry("bearerFormat", "JWT");
    }

    @Test
    void ReturnsStatusOkAndBodyExposesOnlyBusinessOperations_OnOpenApiDocs() {
        // When
        var actual = restTestClient.get()
                                   .uri("/v3/api-docs")
                                   .exchange()
                                   .expectStatus()
                                   .isOk()
                                   .expectBody(String.class)
                                   .returnResult()
                                   .getResponseBody();

        // Then
        assertThatJson(actual).node("paths")
                              .isObject()
                              .containsOnlyKeys(TASKS_GET_FULL_PATH, TASKS_GET_BY_ID_FULL_PATH, TASKS_GET_BY_USER_ID_FULL_PATH);
        assertThatJson(actual).node("paths")
                              .node(TASKS_GET_FULL_PATH)
                              .isObject()
                              .containsOnlyKeys("get", "post");
        assertThatJson(actual).node("paths")
                              .node(TASKS_GET_BY_ID_FULL_PATH)
                              .isObject()
                              .containsOnlyKeys("get", "put", "delete");
        assertThatJson(actual).node("paths")
                              .node(TASKS_GET_BY_USER_ID_FULL_PATH)
                              .isObject()
                              .containsOnlyKeys("get");
    }

}
