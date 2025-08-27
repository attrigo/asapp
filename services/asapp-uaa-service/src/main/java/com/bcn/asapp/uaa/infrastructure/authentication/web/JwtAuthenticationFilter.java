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

package com.bcn.asapp.uaa.infrastructure.authentication.web;

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.user.Role.USER;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtAuthenticationToken;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtDecoder;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> optionalBearerToken = getBearerToken(request);

        if (optionalBearerToken.isPresent()) {
            var bearerToken = optionalBearerToken.get();
            var jwt = jwtDecoder.decode(bearerToken);

            var isAuthenticated = jwtAuthenticationRepository.existsByAccessToken(jwt.token());
            if (isAuthenticated) {
                var role = jwt.getClaim(ROLE_CLAIM_NAME, String.class)
                              .orElse(USER.name());
                var authorities = AuthorityUtils.createAuthorityList(role);
                var authentication = JwtAuthenticationToken.authenticated(jwt.subject(), authorities, jwt.token());

                var newContext = SecurityContextHolder.createEmptyContext();
                newContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(newContext);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ") ? Optional.of(bearerToken.substring(7)) : Optional.empty();
    }

}
