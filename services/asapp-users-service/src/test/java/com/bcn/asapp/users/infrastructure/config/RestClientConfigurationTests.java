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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRestClientBuilderBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * Tests {@link RestClientConfiguration} HTTP client configuration.
 * <p>
 * Coverage:
 * <li>Load balancer client is invoked for each outgoing request (protects against accidental {@code @LoadBalanced} removal)</li>
 * <li>Redirect responses from downstream services are returned as-is without being followed</li>
 * <li>JWT from the authenticated security context is propagated as {@code Authorization: Bearer} header to downstream requests</li>
 */
@SpringJUnitConfig
@SuppressWarnings("unchecked")
class RestClientConfigurationTests {

    /**
     * Provides the minimal Spring Cloud load-balancer beans needed to test {@link RestClientConfiguration} in isolation.
     */
    @Configuration
    @Import(RestClientConfiguration.class)
    static class TestConfiguration {

        @Bean
        RestClientBuilderConfigurer restClientBuilderConfigurer() {
            return new RestClientBuilderConfigurer();
        }

        @Bean
        LoadBalancerClient loadBalancerClient() {
            return Mockito.mock(LoadBalancerClient.class);
        }

        @Bean
        LoadBalancerInterceptor loadBalancerInterceptor(LoadBalancerClient loadBalancerClient) {
            return new LoadBalancerInterceptor(loadBalancerClient);
        }

        @Bean
        static LoadBalancerRestClientBuilderBeanPostProcessor<LoadBalancerInterceptor> loadBalancerBeanPostProcessor(
                ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor, ApplicationContext ctx) {
            return new LoadBalancerRestClientBuilderBeanPostProcessor<>(loadBalancerInterceptor, ctx);
        }

    }

    @Autowired
    private RestClient restClient;

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
        // Delegate each load-balanced call to the real HTTP execution by applying the request against a
        // synthetic service instance pointing at the in-process test server, so the actual HttpServer is reached.
        given(loadBalancerClient.execute(anyString(), any(LoadBalancerRequest.class))).willAnswer(inv -> {
            LoadBalancerRequest<?> request = inv.getArgument(1);
            return request.apply(new DefaultServiceInstance("", "", "localhost", port, false));
        });
        // The URI already targets localhost, so return it unchanged — no host/port rewrite needed.
        given(loadBalancerClient.reconstructURI(any(), any())).willAnswer(inv -> inv.getArgument(1, URI.class));
    }

    @AfterEach
    void afterEach() {
        // Prevents the seeded JWT from leaking to subsequent tests via the thread-local SecurityContextHolder.
        SecurityContextHolder.clearContext();
        server.stop(0);
    }

    @Test
    void InvokesLoadBalancerClient_OnRequest() throws IOException {
        // Given
        server.createContext("/some-path", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        restClient.get()
                  .uri("http://localhost:" + port + "/some-path")
                  .exchange((_, response) -> response.getStatusCode());

        // Then
        then(loadBalancerClient).should()
                                .execute(anyString(), any(LoadBalancerRequest.class));
    }

    @Test
    void ReturnsStatusFound_ServerReturnsRedirect() {
        // Given
        var redirectCalled = new AtomicBoolean();
        server.createContext("/original", exchange -> {
            exchange.getResponseHeaders()
                    .set("Location", "http://localhost:" + port + "/redirected");
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
        var actual = restClient.get()
                               .uri("http://localhost:" + port + "/original")
                               .exchange((_, response) -> response.getStatusCode());

        // Then
        assertThat(actual).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectCalled.get()).isFalse();
    }

    @Test
    void PropagatesAuthorizationHeader_ValidSecurityContext() {
        // Given
        var authorizationHeader = new AtomicReference<String>();
        server.createContext("/some-path", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders()
                                            .getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        // When
        restClient.get()
                  .uri("http://localhost:" + port + "/some-path")
                  .exchange((_, response) -> response.getStatusCode());

        // Then
        assertThat(authorizationHeader.get()).isEqualTo("Bearer " + encodedToken);
    }

}
