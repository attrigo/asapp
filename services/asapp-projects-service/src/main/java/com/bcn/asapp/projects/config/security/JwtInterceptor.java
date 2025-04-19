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
package com.bcn.asapp.projects.config.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * HTTP request interceptor that adds JWT authentication headers to outgoing requests.
 * <p>
 * This interceptor implements Spring's {@link ClientHttpRequestInterceptor} to automatically add Bearer token authentication headers to HTTP requests. It
 * retrieves the JWT from the current security context and adds it as a Bearer token in the Authorization header.
 * <p>
 * The interceptor expects a {@link JwtAuthenticationToken} to be present in the Spring Security context. The JWT from this token is added as a Bearer token in
 * the format: {@code Authorization: Bearer <jwt-token>}
 *
 * @author ttrigo
 * @since 0.2.0
 * @see ClientHttpRequestInterceptor
 */
public class JwtInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Intercepts the HTTP request to add JWT authentication headers.
     *
     * @param request   the HTTP request being processed.
     * @param body      the body of the request.
     * @param execution the request execution chain.
     * @return the response from the request execution.
     * @throws IOException if an I/O error occurs during request execution.
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                                                                           .getAuthentication();
        var jwt = authentication.getJwt();
        var bearerToken = "Bearer " + jwt;
        request.getHeaders()
               .add(HttpHeaders.AUTHORIZATION, bearerToken);

        return execution.execute(request, body);
    }

}
