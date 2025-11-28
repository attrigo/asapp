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
import com.bcn.asapp.authentication.application.authentication.out.JwtPairStore;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

/**
 * Default implementation of {@link JwtAuthenticationGranter} for issuing JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer by generating JWT tokens, storing them temporarily for fast validation, and persisting the
 * authentication session for durability.
 * <p>
 * <strong>Token Lifecycle:</strong>
 * <ol>
 * <li>Generates access and refresh tokens via {@link JwtIssuer}</li>
 * <li>Stores token pairs ({@link JwtPairStore}) for fast lookup during validation</li>
 * <li>Persists authentication session ({@link JwtAuthenticationRepository}) for durability</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DefaultJwtAuthenticationGranter implements JwtAuthenticationGranter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJwtAuthenticationGranter.class);

    private final JwtIssuer jwtIssuer;

    private final JwtPairStore jwtPairStore;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DefaultJwtAuthenticationGranter} with required dependencies.
     *
     * @param jwtIssuer                   the JWT issuer for generating tokens
     * @param jwtPairStore                the JWT pair store for token lookup
     * @param jwtAuthenticationRepository the JWT authentication repository for persistent storage
     */
    public DefaultJwtAuthenticationGranter(JwtIssuer jwtIssuer, JwtPairStore jwtPairStore, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtIssuer = jwtIssuer;
        this.jwtPairStore = jwtPairStore;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Grants a JWT authentication for an authenticated user.
     * <p>
     * Generates access and refresh tokens based on the user's authentication information, stores them temporarily for fast validation, and persists the
     * authentication session for durability.
     *
     * @param userAuthentication the {@link UserAuthentication} containing authenticated user data
     * @return the {@link JwtAuthentication} containing generated access and refresh tokens with persistent ID
     * @throws BadCredentialsException if token generation, storage, or persistence fails
     */
    @Override
    public JwtAuthentication grantAuthentication(UserAuthentication userAuthentication) {
        logger.trace("Granting authentication for user {}", userAuthentication.username());

        try {
            var userId = userAuthentication.userId();
            var accessToken = jwtIssuer.issueAccessToken(userAuthentication);
            var refreshToken = jwtIssuer.issueRefreshToken(userAuthentication);

            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            var savedAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);

            // TODO: What if store/delete fails? We have a consistency issue between Redis and DB
            jwtPairStore.save(jwtAuthentication.getJwtPair());

            return savedAuthentication;

        } catch (Exception e) {
            var message = String.format("Authentication could not be granted due to: %s", e.getMessage());
            logger.warn(message, e);
            // TODO: Throw another exception when the errors happens in repository or store operations?
            throw new BadCredentialsException(message, e);
        }
    }

}
