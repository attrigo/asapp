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
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bcn.asapp.uaa.security.authentication.JwtAuthenticationToken;
import com.bcn.asapp.uaa.security.authentication.verifier.JwtVerifier;

/**
 * HTTP filter for JWT-based verification integrated with Spring Security.
 * <p>
 * Intercepts incoming HTTP requests to extract and verify JWTs from the Authorization header.
 * <p>
 * When a valid token is found, sets the authentication in the {@link SecurityContextHolder} for downstream processing.
 * <p>
 * Designed for stateless authentication systems where each request must include a valid JWT in the Authorization header formatted as {@code Bearer <token>}.
 * <p>
 * The created {@link JwtAuthenticationToken} retains the JWT for use in later outgoing authenticated requests that require authentication.
 *
 * @since 0.2.0
 * @see OncePerRequestFilter
 * @see SecurityContextHolder
 * @author ttrigo
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * The JWT verifier used to validate access tokens.
     */
    private final JwtVerifier accessTokenVerifier;

    /**
     * Constructs a new {@code JwtAuthenticationFilter} with the specified JWT verifier.
     *
     * @param accessTokenVerifier the JWT verifier used to validate access tokens and extract user information
     */
    public JwtAuthenticationFilter(JwtVerifier accessTokenVerifier) {
        this.accessTokenVerifier = accessTokenVerifier;
    }

    /**
     * Filters incoming HTTP requests to perform JWT verification.
     * <p>
     * Extracts the JWT from the Authorization header and verifies it using the {@link JwtVerifier}. Upon successful verification, sets the resulting
     * authentication in the {@link SecurityContextHolder}.
     * <p>
     * If token extraction or verification fails, the filter chain proceeds without authentication, allowing other security mechanisms to handle unauthenticated
     * requests.
     *
     * @param request     the incoming {@link HttpServletRequest}
     * @param response    the {@link HttpServletResponse}
     * @param filterChain the {@link FilterChain} to delegate request processing
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> optionalToken = getTokenFromRequest(request);

        if (optionalToken.isPresent()) {
            var jwt = optionalToken.get();

            var authentication = accessTokenVerifier.verify(jwt);

            SecurityContextHolder.getContext()
                                 .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header of the given request.
     *
     * @param request the {@link HttpServletRequest}
     * @return an {@link Optional} containing the JWT if present, otherwise an {@link Optional#empty}
     */
    private Optional<String> getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ") ? Optional.of(bearerToken.substring(7)) : Optional.empty();
    }

}
