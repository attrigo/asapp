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

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.bcn.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.bcn.asapp.http.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.AsappUsersServiceApplication;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link RestClientConfiguration} HTTP service group wiring, exercised end-to-end through the real declarative HTTP client against an embedded
 * MockServer.
 * <p>
 * Coverage:
 * <li>Routes outgoing requests through the load balancer to the resolved service instance when load balancing is enabled</li>
 * <li>Returns redirect responses from downstream services as-is without following them</li>
 * <li>Propagates the authenticated caller's JWT as an Authorization Bearer header to downstream requests</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestContainerConfiguration.class)
class RestClientConfigurationIT {

    static final ClientAndServer embeddedMockServer = ClientAndServer.startClientAndServer(0);

    static final MockServerClient mockServerClient = embeddedMockServer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.loadbalancer.enabled", () -> true);
        registry.add("spring.cloud.discovery.client.simple.instances.asapp-tasks-service[0].uri", () -> "http://localhost:" + embeddedMockServer.getPort());
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://asapp-tasks-service");
    }

    @Autowired
    private TasksHttpClient tasksHttpClient;

    @BeforeEach
    void beforeEach() {
        mockServerClient.reset();
        seedSecurityContext();
    }

    @AfterAll
    static void afterAll() {
        embeddedMockServer.stop();
    }

    @Test
    void ResolvesServiceInstance_LoadBalancingEnabled() {
        // Given
        var userId = aUser().getId()
                            .value();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.toString()));

        mockServerClient.when(request)
                        .respond(response().withStatusCode(200)
                                           .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                           .withBody("[]"));

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        mockServerClient.verify(request, exactly(1));
    }

    @Test
    void DoesNotFollowRedirect_ServerReturnsRedirect() {
        // Given
        var userId = aUser().getId()
                            .value();
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.toString()));

        mockServerClient.when(request)
                        .respond(response().withStatusCode(302)
                                           .withHeader("Location", "/redirected"));

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        mockServerClient.verify(request().withPath("/redirected"), exactly(0));
    }

    @Test
    void PropagatesAuthorizationHeader_AuthenticatedSecurityContext() {
        // Given
        var userId = aUser().getId()
                            .value();
        var tasksByUserIdPath = TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.toString());
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(tasksByUserIdPath);

        mockServerClient.when(request)
                        .respond(response().withStatusCode(200)
                                           .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                           .withBody("[]"));

        var encodedToken = seedSecurityContext();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        mockServerClient.verify(request().withMethod(HttpMethod.GET.name())
                                         .withPath(tasksByUserIdPath)
                                         .withHeader("Authorization", "Bearer " + encodedToken),
                exactly(1));
    }

    private static String seedSecurityContext() {
        var decodedJwt = decodedAccessToken();
        var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
        SecurityContextHolder.getContext()
                             .setAuthentication(jwtAuthenticationToken);
        return decodedJwt.encodedToken();
    }

}
