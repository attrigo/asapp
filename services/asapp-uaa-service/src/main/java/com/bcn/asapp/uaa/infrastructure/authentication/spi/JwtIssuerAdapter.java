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

package com.bcn.asapp.uaa.infrastructure.authentication.spi;

import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.application.authentication.spi.JwtIssuer;
import com.bcn.asapp.uaa.application.user.spi.UserRepository;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.infrastructure.authentication.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.infrastructure.authentication.JwtProvider;

@Component
public class JwtIssuerAdapter implements JwtIssuer {

    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public JwtIssuerAdapter(JwtProvider jwtProvider, UserRepository userRepository, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    public JwtAuthentication issueAuthentication(JwtAuthentication authentication) {
        var user = resolveAuthenticationUser(authentication);

        try {
            var newAccessToken = jwtProvider.generateAccessToken(user);
            var newRefreshToken = jwtProvider.generateRefreshToken(user);

            authentication.setAccessToken(newAccessToken);
            authentication.setRefreshToken(newRefreshToken);

            return jwtAuthenticationRepository.save(authentication);

        } catch (DbActionExecutionException e) {
            throw new JwtIntegrityViolationException("Authentication could not be issued due to: " + e.getMessage(), e);
        }
    }

    private User resolveAuthenticationUser(JwtAuthentication authentication) {
        var authenticationId = authentication.getId()
                                             .id();
        return userRepository.findByAuthenticationId(authenticationId)
                             .orElseThrow(() -> new UsernameNotFoundException("User not exists by authentication id " + authentication.getId()));
    }

}
