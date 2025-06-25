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
package com.bcn.asapp.uaa.security.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT-based implementation of {@link AuthenticationEntryPoint} that handles unauthorized access attempts by responding with an HTTP 401 Unauthorized status.
 * <p>
 * Typically used in JWT authentication systems to handle requests that lack valid authentication tokens or are otherwise unauthenticated.
 *
 * @since 0.2.0
 * @see AuthenticationEntryPoint
 * @author ttrigo
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    /**
     * Commences an authentication scheme by sending an HTTP 401 Unauthorized response and logging the authentication exception.
     *
     * @param request       the {@link HttpServletRequest} that resulted in an {@code AuthenticationException}
     * @param response      the {@link HttpServletResponse} to which the error response is sent
     * @param authException the {@link AuthenticationException} that triggered this entry point
     * @throws IOException if an I/O error occurs while sending the error response
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        logger.error("Unauthorized error: {}", authException.getMessage());

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

}
