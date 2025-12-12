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
import com.bcn.asapp.authentication.application.authentication.in.RefreshAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenDecoder;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Application service responsible for orchestrating authentication refresh.
 * <p>
 * Coordinates the complete token refresh workflow including token validation, verification, generation of new tokens, cleanup of old tokens, and persistence
 * across multiple storage systems.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Decodes and validates refresh token via {@link TokenDecoder}</li>
 * <li>Verifies token type is refresh token</li>
 * <li>Checks token exists in fast-access store via {@link JwtStore}</li>
 * <li>Fetches authentication from database via {@link JwtAuthenticationRepository}</li>
 * <li>Generates new access token via {@link TokenIssuer}</li>
 * <li>Generates new refresh token via {@link TokenIssuer}</li>
 * <li>Updates domain aggregate with new tokens</li>
 * <li>Persists updated authentication to database (overrides existing record)</li>
 * <li>Deletes old tokens from fast-access store</li>
 * <li>Stores new tokens in fast-access store</li>
 * </ol>
 * <p>
 * The entire refresh workflow executes within a single transaction.
 * <p>
 * Database save operation updates the existing authentication record with new tokens.
 * <p>
 * Old tokens are explicitly deleted from Redis to prevent reuse, ensuring immediate invalidation of the previous session.
 *
 * @author attrigo
 * @since 0.2.0
 */
@ApplicationService
public class RefreshAuthenticationService implements RefreshAuthenticationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RefreshAuthenticationService.class);

    private final TokenDecoder tokenDecoder;

    private final TokenIssuer tokenIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code RefreshAuthenticationService} with required dependencies.
     *
     * @param tokenDecoder                the token decoder for decoding and validating tokens
     * @param tokenIssuer                 the token issuer for generating new JWT tokens
     * @param jwtAuthenticationRepository the repository for persisting JWT authentications
     * @param jwtStore                    the store for fast token lookup and validation
     */
    public RefreshAuthenticationService(TokenDecoder tokenDecoder, TokenIssuer tokenIssuer, JwtAuthenticationRepository jwtAuthenticationRepository,
            JwtStore jwtStore) {

        this.tokenDecoder = tokenDecoder;
        this.tokenIssuer = tokenIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtStore = jwtStore;
    }

    /**
     * Refreshes an authentication using a valid refresh token.
     * <p>
     * Orchestrates the complete token refresh workflow: validation, verification, new token generation, and persistence with cleanup.
     * <p>
     * The operation is transactional to ensure consistency between database and token store.
     *
     * @param refreshToken the refresh token string
     * @return the {@link JwtAuthentication} containing new access and refresh tokens with updated persistence
     * @throws IllegalArgumentException           if the refresh token is invalid or blank
     * @throws InvalidJwtException                if the token is invalid, malformed, or expired
     * @throws UnexpectedJwtTypeException         if the provided token is not a refresh token
     * @throws JwtAuthenticationNotFoundException if the token is not found in active sessions or database
     */
    @Override
    @Transactional
    // TODO: Handle specific exceptions for better error reporting
    // TODO: Improve transaction management (what if token store operation fails?)
    // TODO: Refactor to simplify method and improve readability
    public JwtAuthentication refreshAuthentication(String refreshToken) {
        logger.debug("Refreshing authentication with refresh token");

        logger.trace("Step 1: Decoding and validating refresh token");
        var encodedRefreshToken = EncodedToken.of(refreshToken);
        var decodedToken = tokenDecoder.decode(encodedRefreshToken);

        logger.trace("Step 2: Verifying token type is refresh token");
        if (!decodedToken.isRefreshToken()) {
            throw new UnexpectedJwtTypeException(String.format("JWT %s is not a refresh token", decodedToken.encodedToken()));
        }

        logger.trace("Step 3: Checking token exists in fast-access store");
        var isTokenActive = jwtStore.refreshTokenExists(encodedRefreshToken);
        if (!isTokenActive) {
            throw new JwtAuthenticationNotFoundException(
                    String.format("Refresh token not found in active sessions (revoked or expired): %s", encodedRefreshToken.token()));
        }

        logger.trace("Step 4: Fetching authentication from database for subject={}", decodedToken.subject());
        var authentication = jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)
                                                        .orElseThrow(() -> new JwtAuthenticationNotFoundException(String.format(
                                                                "JWT authentication not found by refresh token %s", encodedRefreshToken.token())));

        assert authentication.getId() != null;
        var authenticationId = authentication.getId()
                                             .value();
        var oldJwtPair = authentication.getJwtPair();

        try {
            logger.trace("Step 5: Generating new tokens for subject={}, role={}", decodedToken.subject(), decodedToken.roleClaim());
            var subject = Subject.of(decodedToken.subject());
            var role = Role.valueOf(decodedToken.roleClaim());
            var newAccessToken = tokenIssuer.issueAccessToken(subject, role);
            var newRefreshToken = tokenIssuer.issueRefreshToken(subject, role);
            var jwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            logger.trace("Step 6: Updating domain aggregate authenticationId={} with new tokens", authenticationId);
            authentication.refreshTokens(jwtPair);

            logger.trace("Step 7: Persisting updated authentication authenticationId={}", authenticationId);
            var savedAuthentication = jwtAuthenticationRepository.save(authentication);

            logger.trace("Step 8: Deleting old token pair from fast-access store");
            jwtStore.delete(oldJwtPair);
            logger.trace("Step 9: Storing new token pair in fast-access store");
            jwtStore.save(savedAuthentication.getJwtPair());

            logger.debug("Authentication refreshed successfully with new tokens");

            return savedAuthentication;

        } catch (Exception e) {
            var message = String.format("Authentication could not be refreshed due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new InvalidJwtAuthenticationException(message, e);
        }
    }

}
