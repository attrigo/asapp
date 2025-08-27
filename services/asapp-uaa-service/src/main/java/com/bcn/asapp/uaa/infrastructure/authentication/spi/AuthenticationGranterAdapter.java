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

import com.bcn.asapp.uaa.application.authentication.spi.AuthenticationGranter;
import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.application.user.spi.UserRepository;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.UsernamePasswordAuthentication;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtIssuer;

@Component
public class AuthenticationGranterAdapter implements AuthenticationGranter {

    private final JwtIssuer jwtIssuer;

    private final UserRepository userRepository;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public AuthenticationGranterAdapter(JwtIssuer jwtIssuer, UserRepository userRepository, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.userRepository = userRepository;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    @Override
    public JwtAuthentication grantAuthentication(UsernamePasswordAuthentication usernamePasswordAuthentication) {
        try {
            var user = resolveAuthenticationUser(usernamePasswordAuthentication.username());

            var accessToken = jwtIssuer.issueAccessToken(user);
            var refreshToken = jwtIssuer.issueRefreshToken(user);

            // TODO: Rethink if it is ok to use unAuthenticated
            var jwtAuthentication = JwtAuthentication.unAuthenticated(user.getId(), accessToken, refreshToken);

            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (DbActionExecutionException e) {
            // TODO: Analyze if this specific exception is needed
            throw new JwtIntegrityViolationException("Authentication could not be generated due to: " + e.getMessage(), e);
        }
    }

    private User resolveAuthenticationUser(String username) {
        return userRepository.findByUsername(username)
                             .orElseThrow(() -> new UsernameNotFoundException("User not found by username " + username));
    }

}
