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

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bcn.asapp.users.infrastructure.security.JwtAuthenticationToken;

/**
 * HTTP request interceptor for adding JWT authentication headers to outgoing requests.
 * <p>
 * Intercepts outgoing HTTP client requests to automatically inject the JWT Bearer token from the current security context into the Authorization header.
 * <p>
 * This interceptor is intended for use with outgoing HTTP clients to propagate authentication context to downstream services.
 *
 * @since 0.2.0
 * @see ClientHttpRequestInterceptor
 * @see SecurityContextHolder
 * @author attrigo
 */
public class JwtInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Intercepts the HTTP request to inject JWT authentication headers.
     * <p>
     * This method performs the following steps:
     * <ol>
     * <li>Retrieves the authentication from the {@link SecurityContextHolder}</li>
     * <li>Validates that authentication is present</li>
     * <li>Validates that authentication is a {@link JwtAuthenticationToken}</li>
     * <li>Extracts and validates the JWT token</li>
     * <li>Adds the token as a Bearer token in the Authorization header</li>
     * <li>Proceeds with the request execution</li>
     * </ol>
     * <p>
     * Injects the token with format: {@code Authorization: Bearer <token>}
     * <p>
     * These validations ensure fail-fast behavior for programming errors during development and testing.
     *
     * @param request   the HTTP request being intercepted
     * @param body      the request body
     * @param execution the request execution chain
     * @return the HTTP response after execution
     * @throws IOException           if an I/O error occurs during request execution
     * @throws IllegalStateException if authentication is not present in the {@link SecurityContextHolder} or is not a {@link JwtAuthenticationToken}
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var token = extractTokenFromAuthentication();

        var bearerToken = "Bearer " + token;
        request.getHeaders()
               .add(HttpHeaders.AUTHORIZATION, bearerToken);

        return execution.execute(request, body);
    }

    /**
     * Extracts the JWT token from the current authentication context.
     * <p>
     * Retrieves the authentication from the {@link SecurityContextHolder}, validates it is a {@link JwtAuthenticationToken}, and extracts the encoded JWT
     * token.
     *
     * @return the encoded JWT token
     * @throws IllegalStateException if no authentication is found in the security context or if the authentication is not a {@link JwtAuthenticationToken}
     */
    private String extractTokenFromAuthentication() {
        var authentication = SecurityContextHolder.getContext()
                                                  .getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("No authentication found in SecurityContext");
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("Expected JwtAuthenticationToken but found: " + authentication.getClass()
                                                                                                          .getName());
        }

        return jwtAuthentication.getJwt();
    }

}
