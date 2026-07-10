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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles unauthorized access attempts on the JWT filter chains.
 * <p>
 * Renders a generic RFC 7807 {@link ProblemDetail} 401 response when JWT authentication fails or is missing.
 *
 * @since 0.2.0
 * @see AuthenticationEntryPoint
 * @author attrigo
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    static final String AUTHENTICATION_FAILED_TITLE = "Authentication Failed";

    static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";

    static final String INVALID_TOKEN_ERROR = "invalid_token";

    static final String ERROR_PROPERTY = "error";

    static final String BEARER_CHALLENGE = "Bearer";

    static final String BEARER_CHALLENGE_INVALID_TOKEN = "Bearer error=\"invalid_token\"";

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code JwtAuthenticationEntryPoint} with required dependencies.
     *
     * @param objectMapper the Spring-managed Jackson object mapper carrying RFC 7807 ProblemDetail serialization support
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Writes a RFC 7807 {@link ProblemDetail} 401 body as {@code application/problem+json}, whose {@code error} property and {@code WWW-Authenticate} challenge
     * vary depending on whether the request carried a bearer token. The raw exception message is logged but never placed in the response body.
     */
    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException authException)
            throws IOException {

        logger.warn("Unauthorized request to {} {}: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
        problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);

        String authenticateChallenge = BEARER_CHALLENGE;
        if (isBearerTokenPresent(request)) {
            problemDetail.setProperty(ERROR_PROPERTY, INVALID_TOKEN_ERROR);
            authenticateChallenge = BEARER_CHALLENGE_INVALID_TOKEN;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, authenticateChallenge);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }

    /**
     * Determines whether the request carries a bearer token, mirroring the same predicate {@code JwtAuthenticationFilter} uses to extract it.
     *
     * @param request the HTTP request
     * @return {@code true} if the {@code Authorization} header is a non-blank {@code Bearer <token>} value, {@code false} otherwise
     */
    private boolean isBearerTokenPresent(HttpServletRequest request) {
        var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Bearer ");
    }

}
