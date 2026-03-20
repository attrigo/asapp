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

package com.bcn.asapp.users.infrastructure.security.client;

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenFactory.encodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bcn.asapp.users.infrastructure.security.DecodedJwt;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * Tests {@link JwtInterceptor} JWT propagation to outgoing requests.
 * <p>
 * Coverage:
 * <li>Rejects requests when authentication missing from security context</li>
 * <li>Rejects requests when authentication is not JWT-based</li>
 * <li>Extracts JWT from authenticated security context</li>
 * <li>Adds Authorization Bearer header to outgoing HTTP requests</li>
 * <li>Delegates request execution to chain with enriched headers</li>
 */
@ExtendWith(MockitoExtension.class)
class JwtInterceptorTests {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private SecurityContext securityContext;

    private final JwtInterceptor jwtInterceptor = new JwtInterceptor();

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class Intercept {

        @Test
        void ExecutesRequest_ValidAuthentication() throws IOException {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);
            var headers = new HttpHeaders();
            var body = new byte[0];

            given(securityContext.getAuthentication()).willReturn(jwtAuthenticationToken);
            given(request.getHeaders()).willReturn(headers);

            // When
            jwtInterceptor.intercept(request, body, execution);

            // Then
            assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + encodedAccessToken);

            then(execution).should(times(1))
                           .execute(eq(request), eq(body));
        }

        @Test
        void ThrowsIllegalStateException_AuthenticationMissingInSecurityContext() {
            // Given
            var body = new byte[0];

            given(securityContext.getAuthentication()).willReturn(null);

            // When
            var actual = catchThrowable(() -> jwtInterceptor.intercept(request, body, execution));

            // Then
            assertThat(actual).isInstanceOf(IllegalStateException.class)
                              .hasMessage("No authentication found in SecurityContext");
        }

        @Test
        void ThrowsIllegalStateException_AuthenticationNotJwtAuthenticationToken() {
            // Given
            var authentication = new UsernamePasswordAuthenticationToken("username", "password");
            var body = new byte[0];

            given(securityContext.getAuthentication()).willReturn(authentication);

            // When
            var actual = catchThrowable(() -> jwtInterceptor.intercept(request, body, execution));

            // Then
            assertThat(actual).isInstanceOf(IllegalStateException.class)
                              .hasMessageStartingWith("Expected JwtAuthenticationToken but found: ")
                              .hasMessageContaining("UsernamePasswordAuthenticationToken");
        }

    }

}
