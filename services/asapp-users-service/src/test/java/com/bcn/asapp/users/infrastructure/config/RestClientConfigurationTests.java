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

import static com.bcn.asapp.users.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.sun.net.httpserver.HttpServer;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * Tests {@link RestClientConfiguration} HTTP service group configuration.
 * <p>
 * Coverage:
 * <li>Load balancer client is invoked for each outgoing request when load balancing is enabled</li>
 * <li>Redirect responses from downstream services are returned as-is without being followed</li>
 * <li>JWT from the authenticated security context is propagated as Authorization Bearer header to downstream requests</li>
 */
@SpringJUnitConfig(RestClientConfigurationTests.TestApp.class)
@TestPropertySource(properties = { "eureka.client.enabled=false", "spring.cloud.config.enabled=false",
        "spring.http.serviceclient.tasks.base-url=http://asapp-tasks-service" })
class RestClientConfigurationTests {

    /**
     * Minimal context: imports {@link RestClientConfiguration} plus the HTTP service client auto-configurations and the Spring Cloud load-balancer beans needed
     * to exercise the group configurer, without component-scanning the production configuration package.
     */
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class, HttpServiceClientPropertiesAutoConfiguration.class,
            HttpServiceClientAutoConfiguration.class })
    @Import({ RestClientConfiguration.class, TasksHttpClientConfiguration.class })
    static class TestApp {

        @Bean
        LoadBalancerClient loadBalancerClient() {
            return Mockito.mock(LoadBalancerClient.class);
        }

        @Bean
        LoadBalancerInterceptor loadBalancerInterceptor(LoadBalancerClient loadBalancerClient) {
            return new LoadBalancerInterceptor(loadBalancerClient);
        }

    }

    @Autowired
    private TasksHttpClient tasksHttpClient;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private HttpServer server;

    private int port;

    private String encodedToken;

    @BeforeEach
    void beforeEach() throws IOException {
        var decodedJwt = decodedAccessToken();
        encodedToken = decodedJwt.encodedToken();
        SecurityContextHolder.getContext()
                             .setAuthentication(JwtAuthenticationToken.authenticated(decodedJwt));

        // Port 0 lets the OS assign an available ephemeral port, avoiding conflicts between test runs.
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress()
                     .getPort();

        Mockito.reset(loadBalancerClient);
        // Delegate each load-balanced call to the real HTTP execution against the in-process server.
        given(loadBalancerClient.execute(anyString(), any(LoadBalancerRequest.class))).willAnswer(inv -> {
            LoadBalancerRequest<?> request = inv.getArgument(1);
            return request.apply(new DefaultServiceInstance("", "", "localhost", port, false));
        });
        given(loadBalancerClient.reconstructURI(any(), any())).willAnswer(inv -> {
            URI original = inv.getArgument(1, URI.class);
            String rewritten = original.getScheme() + "://localhost:" + port + original.getPath();
            return URI.create(rewritten);
        });
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        server.stop(0);
    }

    @Test
    void InvokesLoadBalancerClient_OnRequest() throws IOException {
        // Given
        var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        server.createContext("/api/tasks/user/" + userId, exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        then(loadBalancerClient).should()
                                .execute(eq("asapp-tasks-service"), any(LoadBalancerRequest.class));
    }

    @Test
    void PropagatesAuthorizationHeader_ValidSecurityContext() throws IOException {
        // Given
        var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var authorizationHeader = new AtomicReference<String>();

        server.createContext("/api/tasks/user/" + userId, exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders()
                                            .getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        assertThat(authorizationHeader.get()).isEqualTo("Bearer " + encodedToken);
    }

    @Test
    void ReturnsResponseWithoutFollowingRedirect_ServerReturnsRedirect() throws IOException {
        // Given
        var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var redirectCalled = new AtomicBoolean();

        server.createContext("/api/tasks/user/" + userId, exchange -> {
            exchange.getResponseHeaders()
                    .set("Location", "/redirected");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirected", exchange -> {
            redirectCalled.set(true);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        tasksHttpClient.getTasksByUserId(userId);

        // Then
        assertThat(redirectCalled.get()).isFalse();
    }

}
