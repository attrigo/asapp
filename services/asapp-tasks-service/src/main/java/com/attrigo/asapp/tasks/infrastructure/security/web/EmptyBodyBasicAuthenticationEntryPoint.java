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

package com.attrigo.asapp.tasks.infrastructure.security.web;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Renders an empty-body 401 response with an HTTP Basic authentication challenge.
 * <p>
 * Signals that credentials are required or invalid by writing only the {@code WWW-Authenticate: Basic} challenge header and the 401 status, leaving the
 * response body empty.
 *
 * @since 0.4.0
 * @see AuthenticationEntryPoint
 * @author attrigo
 */
public class EmptyBodyBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String BASIC_CHALLENGE = "Basic realm=\"actuator\"";

    /**
     * Constructs a new {@code EmptyBodyBasicAuthenticationEntryPoint}.
     */
    public EmptyBodyBasicAuthenticationEntryPoint() {}

    /**
     * {@inheritDoc}
     * <p>
     * Writes the {@code WWW-Authenticate: Basic} challenge header and a 401 status directly on the response, without a body. Deliberately avoids
     * {@link HttpServletResponse#sendError} so the servlet container does not re-dispatch to {@code /error}, where another entry point would otherwise render a
     * ProblemDetail body.
     */
    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException authException) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC_CHALLENGE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
    }

}
