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

package com.bcn.asapp.authentication.infrastructure.security.web;

import static com.bcn.asapp.authentication.infrastructure.config.SecurityConfiguration.API_WHITELIST_POST_URLS;
import static com.bcn.asapp.authentication.infrastructure.config.SecurityConfiguration.API_WHITELIST_URLS;
import static com.bcn.asapp.authentication.infrastructure.config.SecurityConfiguration.MANAGEMENT_WHITELIST_URLS;
import static com.bcn.asapp.authentication.infrastructure.config.SecurityConfiguration.ROOT_WHITELIST_URLS;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.authentication.infrastructure.security.JwtVerifier;

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
 * @author attrigo
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtVerifier jwtVerifier;

    private final Set<RequestMatcher> excludedMatchers;

    /**
     * Constructs a new {@code JwtAuthenticationFilter} with required dependencies.
     *
     * @param jwtVerifier the JWT verifier for validating tokens
     */
    public JwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;

        this.excludedMatchers = buildExcludedMatchers();
    }

    /**
     * Determines whether this filter should not process the current request.
     * <p>
     * Returns {@code true} for allowlisted endpoints that do not require authentication.
     *
     * @param request the HTTP request
     * @return {@code true} if the filter should be skipped, {@code false} otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return excludedMatchers.stream()
                               .anyMatch(matcher -> matcher.matches(request));
    }

    /**
     * Performs JWT authentication for each request.
     * <p>
     * Extracts the Bearer token from the Authorization header, verifies it, and establishes the security context if valid.
     * <p>
     * If no token is present or validation fails, the request continues without authentication.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> optionalBearerToken = getBearerToken(request);

        if (optionalBearerToken.isEmpty()) {
            logger.warn("Bearer token not found");
            filterChain.doFilter(request, response);
            return;
        }

        var bearerToken = optionalBearerToken.get();
        try {
            var decodedJwt = jwtVerifier.verifyAccessToken(bearerToken);

            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);

            var newContext = SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(jwtAuthenticationToken);
            SecurityContextHolder.setContext(newContext);

        } catch (AuthenticationNotFoundException e) {
            logger.warn("Authentication session not found for token: {}", bearerToken);
        } catch (UnexpectedJwtTypeException e) {
            logger.warn("Invalid token type for bearer token: {}", bearerToken);
        } catch (InvalidJwtException e) {
            logger.warn("Invalid bearer token: {}", bearerToken, e);
        } catch (Exception e) {
            logger.warn("Error authenticating the bearer token: {}", bearerToken, e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Builds a set of request matchers to allowlisted endpoints.
     *
     * @return a {@link Set} of {@link RequestMatcher} instances for excluded paths
     */
    private Set<RequestMatcher> buildExcludedMatchers() {
        Set<RequestMatcher> excludedMatchers = Stream.of(API_WHITELIST_URLS, ROOT_WHITELIST_URLS, MANAGEMENT_WHITELIST_URLS)
                                                     .flatMap(Arrays::stream)
                                                     .map(AntPathRequestMatcher::new)
                                                     .collect(Collectors.toSet());
        Stream.of(API_WHITELIST_POST_URLS)
              .map(url -> new AntPathRequestMatcher(url, HttpMethod.POST.name()))
              .forEach(excludedMatchers::add);
        return excludedMatchers;
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * @param request the HTTP request
     * @return an {@link Optional} containing the token if present, {@link Optional#empty()} otherwise
     */
    private Optional<String> getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ") ? Optional.of(bearerToken.substring(7)) : Optional.empty();
    }

}
