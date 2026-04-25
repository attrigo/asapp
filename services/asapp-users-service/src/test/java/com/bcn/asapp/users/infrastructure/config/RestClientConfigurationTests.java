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

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import com.bcn.asapp.users.infrastructure.security.DecodedJwt;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * Tests {@link RestClientConfiguration} HTTP client configuration.
 * <p>
 * Coverage:
 * <li>Redirect responses from downstream services are returned as-is without being followed</li>
 */
class RestClientConfigurationTests {

    @Nested
    class RestClientCustomizer {

        private HttpServer server;

        private int port;

        @BeforeEach
        void beforeEach() throws IOException {
            var encodedToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);
            SecurityContextHolder.getContext()
                                 .setAuthentication(JwtAuthenticationToken.authenticated(decodedJwt));

            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress()
                         .getPort();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
            server.stop(0);
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

            var restClientBuilder = RestClient.builder();
            new RestClientConfiguration().restClientCustomizer()
                                         .customize(restClientBuilder);
            var restClient = restClientBuilder.build();

            // When
            var actual = restClient.get()
                                   .uri("http://localhost:" + port + "/original")
                                   .exchange((_, response) -> response.getStatusCode());

            // Then
            assertThat(actual).isEqualTo(HttpStatus.FOUND);
            assertThat(redirectCalled.get()).isFalse();
        }

    }

}
