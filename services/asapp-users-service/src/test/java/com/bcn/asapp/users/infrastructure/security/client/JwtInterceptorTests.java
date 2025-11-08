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

import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.users.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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

import com.bcn.asapp.users.infrastructure.security.DecodedToken;
import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
public class JwtInterceptorTests {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private SecurityContext securityContext;

    private final JwtInterceptor jwtInterceptor = new JwtInterceptor();

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class Intercept {

        @Test
        void ThenThrowsIllegalStateException_GivenNoAuthenticationInSecurityContext() {
            // Given
            given(securityContext.getAuthentication()).willReturn(null);

            var body = new byte[0];

            // When
            var thrown = catchThrowable(() -> jwtInterceptor.intercept(request, body, execution));

            // Then
            assertThat(thrown).isInstanceOf(IllegalStateException.class)
                              .hasMessage("No authentication found in SecurityContext");
        }

        @Test
        void ThenThrowsIllegalStateException_GivenAuthenticationIsNotJwtAuthenticationToken() {
            // Given
            var authentication = new UsernamePasswordAuthenticationToken("username", "password");
            given(securityContext.getAuthentication()).willReturn(authentication);

            var body = new byte[0];

            // When
            var thrown = catchThrowable(() -> jwtInterceptor.intercept(request, body, execution));

            // Then
            assertThat(thrown).isInstanceOf(IllegalStateException.class)
                              .hasMessageStartingWith("Expected JwtAuthenticationToken but found: ")
                              .hasMessageContaining("UsernamePasswordAuthenticationToken");
        }

        @Test
        void ThenAddsAuthorizationHeaderAndExecutesRequest_GivenAuthenticationIsValid() throws IOException {
            // Given
            var encodedToken = defaultTestEncodedAccessToken();
            var claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");
            var decodedToken = new DecodedToken(encodedToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);
            var authentication = JwtAuthenticationToken.authenticated(decodedToken);
            given(securityContext.getAuthentication()).willReturn(authentication);

            var headers = new HttpHeaders();
            given(request.getHeaders()).willReturn(headers);

            var body = new byte[0];

            // When
            jwtInterceptor.intercept(request, body, execution);

            // Then
            assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + decodedToken.encodedToken());

            then(execution).should(times(1))
                           .execute(eq(request), eq(body));
        }

    }

}
