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

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.user.Role.USER;

import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.AuthenticationRefresher;
import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.user.Role;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtIssuer;

@Component
public class AuthenticationRefresherAdapter implements AuthenticationRefresher {

    private final JwtIssuer jwtIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public AuthenticationRefresherAdapter(JwtIssuer jwtIssuer, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    @Override
    public JwtAuthentication refreshAuthentication(JwtAuthentication jwtAuthentication) {
        try {
            var currentAccessToken = jwtAuthentication.getAccessToken();
            var accessTokenSubject = currentAccessToken.subject();
            var accessTokenRole = Role.valueOf(currentAccessToken.getClaim(ROLE_CLAIM_NAME, String.class)
                                                                 .orElse(USER.name()));
            var currentRefreshToken = jwtAuthentication.getRefreshToken();
            var refreshTokenSubject = currentRefreshToken.subject();
            var refreshTokenRole = Role.valueOf(currentRefreshToken.getClaim(ROLE_CLAIM_NAME, String.class)
                                                                   .orElse(USER.name()));

            var accessToken = jwtIssuer.issueAccessToken(accessTokenSubject, accessTokenRole);
            var refreshToken = jwtIssuer.issueRefreshToken(refreshTokenSubject, refreshTokenRole);

            jwtAuthentication.setAccessToken(accessToken);
            jwtAuthentication.setRefreshToken(refreshToken);

            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (DbActionExecutionException e) {
            // TODO: Analyze if this specific exception is needed
            throw new JwtIntegrityViolationException("Authentication could not be refreshed due to: " + e.getMessage(), e);
        }
    }

}
