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

package com.bcn.asapp.uaa.infrastructure.authentication.out;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.out.AuthenticationGranter;
import com.bcn.asapp.uaa.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.UserAuthentication;
import com.bcn.asapp.uaa.infrastructure.security.JwtIssuer;

/**
 * Adapter implementation of {@link AuthenticationGranter} for issuing JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, issuing JWT tokens and storing them in the repository.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class AuthenticationGranterAdapter implements AuthenticationGranter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationGranterAdapter.class);

    private final JwtIssuer jwtIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code AuthenticationGranterAdapter} with required dependencies.
     *
     * @param jwtIssuer                   the JWT issuer for generating tokens
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public AuthenticationGranterAdapter(JwtIssuer jwtIssuer, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Grants a JWT authentication for an authenticated user.
     * <p>
     * Generates access and refresh tokens based on the user's authentication information and persists the authentication session.
     *
     * @param userAuthentication the {@link UserAuthentication} containing authenticated user data
     * @return the {@link JwtAuthentication} containing generated access and refresh tokens
     * @throws BadCredentialsException if token generation or persistence fails
     */
    @Override
    public JwtAuthentication grantAuthentication(UserAuthentication userAuthentication) {
        logger.trace("Granting authentication for user {}", userAuthentication.username());

        try {
            var userId = userAuthentication.userId();
            var accessToken = jwtIssuer.issueAccessToken(userAuthentication);
            var refreshToken = jwtIssuer.issueRefreshToken(userAuthentication);

            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (Exception e) {
            var message = String.format("Authentication could not be granted due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new BadCredentialsException(message, e);
        }
    }

}
