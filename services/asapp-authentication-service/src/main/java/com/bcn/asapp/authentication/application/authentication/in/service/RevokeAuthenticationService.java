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

package com.bcn.asapp.authentication.application.authentication.in.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.authentication.application.ApplicationService;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenDecoder;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Application service responsible for orchestrating authentication revocation.
 * <p>
 * Coordinates the complete revocation workflow including token validation, verification, and removal from both fast-access store and persistent storage.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Decodes and validates access token via {@link TokenDecoder}</li>
 * <li>Verifies token type is access token</li>
 * <li>Checks token exists in fast-access store via {@link JwtStore}</li>
 * <li>Fetches authentication from database via {@link JwtAuthenticationRepository}</li>
 * <li>Deletes token pair from fast-access store</li>
 * <li>Deletes authentication from database</li>
 * </ol>
 * <p>
 * The entire revocation workflow executes within a single transaction to ensure consistency between database and token store operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RevokeAuthenticationService implements RevokeAuthenticationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RevokeAuthenticationService.class);

    private final TokenDecoder tokenDecoder;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code RevokeAuthenticationService} with required dependencies.
     *
     * @param tokenDecoder                the token decoder for decoding and validating tokens
     * @param jwtAuthenticationRepository the repository for persisting JWT authentications
     * @param jwtStore                    the store for fast token lookup and validation
     */
    public RevokeAuthenticationService(TokenDecoder tokenDecoder, JwtAuthenticationRepository jwtAuthenticationRepository, JwtStore jwtStore) {
        this.tokenDecoder = tokenDecoder;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtStore = jwtStore;
    }

    /**
     * Revokes an authentication using a valid access token.
     * <p>
     * Orchestrates the complete revocation workflow: validation, verification, and removal from both storage systems.
     * <p>
     * The operation is transactional to ensure consistency between database and token store.
     *
     * @param accessToken the access token string
     * @throws IllegalArgumentException           if the access token is invalid or blank
     * @throws InvalidJwtException                if the token is invalid, malformed, or expired
     * @throws UnexpectedJwtTypeException         if the provided token is not an access token
     * @throws JwtAuthenticationNotFoundException if the token is not found in active sessions or database
     * @throws InvalidJwtAuthenticationException  if revocation fails due to storage errors
     */
    @Override
    @Transactional
    // TODO: Handle specific exceptions for better error reporting
    // TODO: Improve transaction management (what if token store operation fails?)
    // TODO: Refactor to simplify method and improve readability
    public void revokeAuthentication(String accessToken) {
        logger.debug("Revoking authentication with access token");

        logger.trace("Step 1: Decoding and validating access token");
        var encodedAccessToken = EncodedToken.of(accessToken);
        var decodedToken = tokenDecoder.decode(encodedAccessToken);

        logger.trace("Step 2: Verifying token type is access token");
        if (!decodedToken.isAccessToken()) {
            throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedToken.encodedToken()));
        }

        logger.trace("Step 3: Checking token exists in fast-access store");
        var isTokenActive = jwtStore.accessTokenExists(encodedAccessToken);
        if (!isTokenActive) {
            throw new JwtAuthenticationNotFoundException(
                    String.format("Access token not found in active sessions (revoked or expired): %s", encodedAccessToken.token()));
        }

        logger.trace("Step 4: Fetching authentication from database for subject={}", decodedToken.subject());
        var authentication = jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)
                                                        .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                                String.format("JWT authentication not found by access token %s", encodedAccessToken.token())));

        assert authentication.getId() != null;
        var authenticationId = authentication.getId()
                                             .value();

        try {
            logger.trace("Step 5: Deleting token pair from fast-access store for authenticationId={}", authenticationId);
            jwtStore.delete(authentication.getJwtPair());

            logger.trace("Step 6: Deleting authentication authenticationId={} from database", authenticationId);
            jwtAuthenticationRepository.deleteById(authentication.getId());

            logger.debug("Authentication revoked successfully with ID {}", authenticationId);

        } catch (Exception e) {
            var message = String.format("Authentication could not be revoked due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new InvalidJwtAuthenticationException(message, e);
        }
    }

}
