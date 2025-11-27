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

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRefresher;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtPairStore;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

/**
 * Default implementation of {@link JwtAuthenticationRefresher} for refreshing JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, refreshing JWT tokens and storing the new ones in the repository.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DefaultJwtAuthenticationRefresher implements JwtAuthenticationRefresher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJwtAuthenticationRefresher.class);

    private final JwtIssuer jwtIssuer;

    private final JwtPairStore jwtPairStore;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DefaultJwtAuthenticationRefresher} with required dependencies.
     *
     * @param jwtIssuer                   the JWT issuer for generating tokens
     * @param jwtPairStore                the JWT pair store for token lookup
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public DefaultJwtAuthenticationRefresher(JwtIssuer jwtIssuer, JwtPairStore jwtPairStore, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.jwtPairStore = jwtPairStore;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Refreshes a JWT authentication with new tokens.
     * <p>
     * Generates new access and refresh tokens while maintaining the same authentication session, updates the authentication with the new tokens, and persists
     * the changes.
     * <p>
     * Database is persisted first to ensure durability. If database persistence fails, Redis remains unchanged with valid old tokens. Redis updates happen last
     * because they are fast and atomic (via pipelining), minimizing the window of inconsistency.
     *
     * @param jwtAuthentication the existing {@link JwtAuthentication} to refresh
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws InvalidJwtAuthenticationException if token generation or persistence fails
     */
    @Override
    public JwtAuthentication refreshAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("Refreshing authentication with id {}", jwtAuthentication.getId());

        try {
            var oldJwtPair = jwtAuthentication.getJwtPair();
            var oldAccessToken = oldJwtPair.accessToken();
            var oldRefreshToken = oldJwtPair.refreshToken();

            var newAccessToken = jwtIssuer.issueAccessToken(oldAccessToken.subject(), oldAccessToken.roleClaim());
            var newRefreshToken = jwtIssuer.issueRefreshToken(oldRefreshToken.subject(), oldRefreshToken.roleClaim());

            jwtAuthentication.updateTokens(newAccessToken, newRefreshToken);

            var savedAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);

            // TODO: What if store/delete fails? We have a consistency issue between Redis and DB
            jwtPairStore.delete(oldJwtPair);
            jwtPairStore.store(savedAuthentication.getJwtPair());

            return savedAuthentication;

        } catch (Exception e) {
            var message = String.format("Authentication could not be refreshed due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new InvalidJwtAuthenticationException(message, e);
        }
    }

}
