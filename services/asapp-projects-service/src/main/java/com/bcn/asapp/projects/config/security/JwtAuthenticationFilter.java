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
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP filter for JWT-based authentication in Spring Security.
 * <p>
 * This filter intercepts incoming HTTP requests to check for a valid JWT in the Authorization header.
 * <p>
 * If a valid token is found, it extracts the username and authorities (roles) and sets the authentication context for the request.
 * <p>
 * The filter is designed for stateless authentication systems where each request must include a valid JWT in the Authorization header with the format:
 * {@code Bearer <token>}.
 * <p>
 * The {@link JwtAuthenticationToken} created by this filter preserves the JWT, making it available for later outgoing requests that require authentication.
 *
 * @author ttrigo
 * @see OncePerRequestFilter
 * @since 0.2.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Main constructor.
     *
     * @param jwtTokenProvider the JWT provider used to validate the token and extract user information.
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Processes the incoming request for JWT authentication.
     * <p>
     * Extracts and validates the JWT from the Authorization header using the {@link JwtTokenProvider}, then creates and sets a new
     * {@link JwtAuthenticationToken} in the {@link SecurityContextHolder} containing the authenticated user details and the original token.
     * <p>
     * If any step fails, the filter chain continues without setting authentication, allowing other security mechanisms to handle unauthenticated requests
     * appropriately.
     *
     * @param request     the incoming HTTP request.
     * @param response    the HTTP response.
     * @param filterChain the filter chain for request processing.
     * @throws ServletException if a servlet error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> optionalToken = getTokenFromRequest(request);

        if (optionalToken.isPresent() && jwtTokenProvider.validateToken(optionalToken.get())) {
            var token = optionalToken.get();
            var optionalUsername = jwtTokenProvider.getUsername(token);
            var authorities = jwtTokenProvider.getAuthorities(token);

            if (optionalUsername.isPresent() && CollectionUtils.isNotEmpty(authorities)) {
                var username = optionalUsername.get();
                var grantedAuthorities = authorities.stream()
                                                    .map(SimpleGrantedAuthority::new)
                                                    .toList();

                var authenticationToken = new JwtAuthenticationToken(username, null, grantedAuthorities, token);

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext()
                                     .setAuthentication(authenticationToken);
            }

        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the Authorization header of the request.
     *
     * @param request the HTTP request.
     * @return an {@link Optional} containing the JWT if present, otherwise an empty {@link Optional}.
     */
    private Optional<String> getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ") ? Optional.of(bearerToken.substring(7)) : Optional.empty();
    }

}
