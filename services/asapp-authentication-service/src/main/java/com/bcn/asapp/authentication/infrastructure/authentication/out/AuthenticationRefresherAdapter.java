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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.AuthenticationRefresher;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

/**
 * Adapter implementation of {@link AuthenticationRefresher} for refreshing JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, refreshing JWT tokens and storing the new ones in the repository.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class AuthenticationRefresherAdapter implements AuthenticationRefresher {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationRefresherAdapter.class);

    private final JwtIssuer jwtIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code AuthenticationRefresherAdapter} with required dependencies.
     *
     * @param jwtIssuer                   the JWT issuer for generating tokens
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public AuthenticationRefresherAdapter(JwtIssuer jwtIssuer, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Refreshes a JWT authentication with new tokens.
     * <p>
     * Generates new access and refresh tokens while maintaining the same authentication session, updates the authentication with the new tokens, and persists
     * the changes.
     *
     * @param jwtAuthentication the existing {@link JwtAuthentication} to refresh
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws InvalidJwtAuthenticationException if token generation or persistence fails
     */
    @Override
    public JwtAuthentication refreshAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("Refreshing authentication with id: {}", jwtAuthentication.getId());

        try {
            var currentAccessToken = jwtAuthentication.accessToken();
            var currentRefreshToken = jwtAuthentication.refreshToken();

            var newAccessToken = jwtIssuer.issueAccessToken(currentAccessToken.subject(), currentAccessToken.roleClaim());
            var newRefreshToken = jwtIssuer.issueRefreshToken(currentRefreshToken.subject(), currentRefreshToken.roleClaim());

            jwtAuthentication.updateTokens(newAccessToken, newRefreshToken);

            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (Exception e) {
            var message = String.format("Authentication could not be refreshed due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new InvalidJwtAuthenticationException(message, e);
        }
    }

}
