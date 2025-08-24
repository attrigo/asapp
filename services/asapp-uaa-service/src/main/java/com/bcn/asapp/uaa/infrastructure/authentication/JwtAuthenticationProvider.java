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

package com.bcn.asapp.uaa.infrastructure.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public JwtAuthenticationProvider(JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            // TODO: Should check if authentication JWT belongs to the user in the system (JWT could username could not be modified)
            var isAuthenticated = jwtAuthenticationRepository.existsByAccessToken(jwtAuthentication.getJwt());
            if (isAuthenticated) {
                return JwtAuthenticationToken.authenticated(jwtAuthentication.getName(), jwtAuthentication.getAuthorities(), jwtAuthentication.getJwt());
            }
        }
        // TODO: Should return null (to delegate the authentication to next Provider) or throw an exception to stop the authentication flow
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
