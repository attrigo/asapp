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

package com.attrigo.asapp.users.infrastructure.user.out;

import static com.attrigo.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;
import static com.attrigo.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static com.attrigo.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.attrigo.asapp.users.AsappUsersServiceApplication;
import com.attrigo.asapp.users.application.user.TasksUnavailableException;
import com.attrigo.asapp.users.application.user.out.TasksGateway;
import com.attrigo.asapp.users.infrastructure.security.JwtAuthenticationToken;
import com.attrigo.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link TasksGatewayAdapter} fallback behavior end-to-end through the real declarative HTTP client against an embedded MockServer.
 * <p>
 * Setup:
 * <li>Loads the full application context backed by a Testcontainers PostgreSQL instance and an embedded mock server</li>
 * <li>Resets the mock server and seeds an authenticated security context before each test</li>
 * <p>
 * Coverage:
 * <li>Signals tasks unavailable when the Tasks Service responds with a server error (5xx)</li>
 * <li>Signals tasks unavailable when the Tasks Service connection is dropped</li>
 * <li>Propagates client (4xx) errors without masking them as an empty list</li>
 */
@SpringBootTest(classes = AsappUsersServiceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestContainerConfiguration.class)
class TasksGatewayAdapterIT {

    static final ClientAndServer embeddedMockServer = ClientAndServer.startClientAndServer(0);

    static final MockServerClient mockServerClient = embeddedMockServer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.tasks.base-url", () -> "http://localhost:" + embeddedMockServer.getPort());
    }

    @Autowired
    private TasksGateway tasksGateway;

    @BeforeEach
    void beforeEach() {
        mockServerClient.reset();
        seedSecurityContext();
    }

    @AfterAll
    static void afterAll() {
        embeddedMockServer.stop();
    }

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ThrowsTasksUnavailableException_TasksServiceReturnsServerError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());

            mockServerClient.when(request)
                            .respond(response().withStatusCode(500));

            // When
            var actual = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(actual).isInstanceOf(TasksUnavailableException.class)
                              .hasMessage("Tasks Service is unavailable")
                              .hasCauseInstanceOf(HttpServerErrorException.class);
        }

        @Test
        void ThrowsTasksUnavailableException_UnreachableTasksService() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH)
                                   .withPathParameter("id", userId.value()
                                                                  .toString());
            mockServerClient.when(request)
                            .error(error().withDropConnection(true));

            // When
            var actual = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(actual).isInstanceOf(TasksUnavailableException.class)
                              .hasMessage("Tasks Service is unavailable")
                              .hasCauseInstanceOf(ResourceAccessException.class);
        }

        @Test
        void PropagatesError_TasksServiceReturnsClientError() {
            // Given
            var userId = aUser().getId();
            var request = request().withMethod(HttpMethod.GET.name())
                                   .withPath(TASKS_GET_BY_USER_ID_FULL_PATH.replace("{id}", userId.value()
                                                                                                  .toString()));

            mockServerClient.when(request)
                            .respond(response().withStatusCode(400));

            // When
            var thrown = catchThrowable(() -> tasksGateway.getTaskIdsByUserId(userId));

            // Then
            assertThat(thrown).isInstanceOf(HttpClientErrorException.class);
        }

    }

    private static void seedSecurityContext() {
        var decodedJwt = decodedAccessToken();
        var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
        SecurityContextHolder.getContext()
                             .setAuthentication(jwtAuthenticationToken);
    }

}
