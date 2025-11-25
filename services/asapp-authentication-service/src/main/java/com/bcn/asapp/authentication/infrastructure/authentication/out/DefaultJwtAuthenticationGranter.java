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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationGranter;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtCacheRepository;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

/**
 * Default implementation of {@link JwtAuthenticationGranter} for issuing JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, issuing JWT tokens, storing them in the cache layer for fast validation, and persisting them in
 * the repository for long-term storage.
 * <p>
 * <strong>Token Lifecycle:</strong>
 * <ol>
 * <li>Generates access and refresh tokens via {@link JwtIssuer}</li>
 * <li>Stores tokens in cache ({@link JwtCacheRepository}) for fast lookup</li>
 * <li>Persists tokens in database ({@link JwtAuthenticationRepository}) for durability</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DefaultJwtAuthenticationGranter implements JwtAuthenticationGranter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJwtAuthenticationGranter.class);

    private final JwtIssuer jwtIssuer;

    private final JwtCacheRepository jwtCacheRepository;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DefaultJwtAuthenticationGranter} with required dependencies.
     *
     * @param jwtIssuer                   the JWT issuer for generating tokens
     * @param jwtCacheRepository          the JWT cache repository for fast token lookups
     * @param jwtAuthenticationRepository the JWT authentication repository for persistent storage
     */
    public DefaultJwtAuthenticationGranter(JwtIssuer jwtIssuer, JwtCacheRepository jwtCacheRepository,
            JwtAuthenticationRepository jwtAuthenticationRepository) {

        this.jwtIssuer = jwtIssuer;
        this.jwtCacheRepository = jwtCacheRepository;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Grants a JWT authentication for an authenticated user.
     * <p>
     * Generates access and refresh tokens based on the user's authentication information, stores them in the cache layer for fast validation, and persists the
     * authentication session in the database for durability.
     * <p>
     * <strong>Process:</strong>
     * <ol>
     * <li>Issues access and refresh tokens using {@link JwtIssuer}</li>
     * <li>Caches the token pair in Redis for O(1) validation lookups</li>
     * <li>Persists the authentication in the database</li>
     * </ol>
     *
     * @param userAuthentication the {@link UserAuthentication} containing authenticated user data
     * @return the {@link JwtAuthentication} containing generated access and refresh tokens with persistent ID
     * @throws BadCredentialsException if token generation, caching, or persistence fails
     */
    @Override
    public JwtAuthentication grantAuthentication(UserAuthentication userAuthentication) {
        logger.trace("Granting authentication for user {}", userAuthentication.username());

        try {
            var userId = userAuthentication.userId();
            var accessToken = jwtIssuer.issueAccessToken(userAuthentication);
            var refreshToken = jwtIssuer.issueRefreshToken(userAuthentication);

            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            jwtCacheRepository.storeJwtPair(jwtAuthentication);

            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (Exception e) {
            var message = String.format("Authentication could not be granted due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new BadCredentialsException(message, e);
        }
    }

}
