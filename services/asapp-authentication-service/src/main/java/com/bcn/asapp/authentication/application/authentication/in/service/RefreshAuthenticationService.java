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
import com.bcn.asapp.authentication.infrastructure.security.DecodedJwt;
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
 * <li>Generates new JWT pair via {@link TokenIssuer}</li>
 * <li>Updates domain aggregate with new tokens</li>
 * <li>Persists updated authentication to database</li>
 * <li>Deletes old tokens from Redis and stores new tokens</li>
 * </ol>
 * <p>
 * If Redis operations fail after database commit, the old authentication state is restored to maintain consistency.
 *
 * @since 0.2.0
 * @author attrigo
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
     * @param tokenIssuer                 the token issuer for generating new JWTs
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
     * First updates database, then updates Redis. If Redis fails, old state is restored via compensating transaction.
     *
     * @param refreshToken the refresh token string
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws IllegalArgumentException           if the refresh token is invalid or blank
     * @throws InvalidJwtException                if the token is invalid, malformed, or expired
     * @throws UnexpectedJwtTypeException         if the provided token is not a refresh token
     * @throws JwtAuthenticationNotFoundException if the token is not found in active sessions or database
     * @throws InvalidJwtAuthenticationException  if refresh fails due to storage errors
     */
    @Override
    @Transactional
    // TODO: Handle specific exceptions for better error reporting
    public JwtAuthentication refreshAuthentication(String refreshToken) {
        logger.debug("Refreshing authentication with refresh token");

        var encodedRefreshToken = EncodedToken.of(refreshToken);
        var decodedToken = decodeToken(encodedRefreshToken);
        verifyTokenType(decodedToken);
        checkTokenInActiveStore(encodedRefreshToken);

        var authentication = retrieveAuthentication(encodedRefreshToken);
        var oldJwtPair = authentication.getJwtPair();

        var newJwtPair = generateNewTokenPair(decodedToken);
        updateAuthenticationWithNewTokens(authentication, newJwtPair);

        var updatedAuthentication = persistAuthenticationUpdate(authentication);

        try {
            rotateTokens(oldJwtPair, updatedAuthentication.getJwtPair());

            logger.debug("Authentication refreshed successfully with new tokens");

            return updatedAuthentication;

        } catch (Exception e) {
            reactivatePreviousTokens(oldJwtPair);
            throw new InvalidJwtAuthenticationException("Refresh failed: tokens could not be updated in Redis", e);
        }
    }

    /**
     * Decodes and validates the refresh token.
     *
     * @param encodedToken the encoded refresh token
     * @return the decoded JWT with claims
     */
    private DecodedJwt decodeToken(EncodedToken encodedToken) {
        logger.trace("Step 1: Decoding and validating refresh token");
        return tokenDecoder.decode(encodedToken);
    }

    /**
     * Verifies that the decoded token is a refresh token.
     *
     * @param decodedToken the decoded JWT to verify
     * @throws UnexpectedJwtTypeException if the token is not a refresh token
     */
    private void verifyTokenType(DecodedJwt decodedToken) {
        logger.trace("Step 2: Verifying token type is refresh token");
        if (!decodedToken.isRefreshToken()) {
            throw new UnexpectedJwtTypeException(String.format("Token %s is not a refresh token", decodedToken.encodedToken()));
        }
    }

    /**
     * Checks if the token exists in the fast-access store (Redis).
     *
     * @param encodedToken the encoded token to check
     * @throws JwtAuthenticationNotFoundException if the token is not found in active sessions
     */
    private void checkTokenInActiveStore(EncodedToken encodedToken) {
        logger.trace("Step 3: Checking token exists in fast-access store");
        var isTokenActive = jwtStore.refreshTokenExists(encodedToken);
        if (!isTokenActive) {
            throw new JwtAuthenticationNotFoundException(
                    String.format("Refresh token not found in active sessions (revoked or expired): %s", encodedToken.token()));
        }
    }

    /**
     * Fetches the authentication record from the database using the refresh token.
     *
     * @param encodedToken the refresh token to search for
     * @return the JWT authentication from database
     * @throws JwtAuthenticationNotFoundException if authentication is not found
     */
    private JwtAuthentication retrieveAuthentication(EncodedToken encodedToken) {
        logger.trace("Step 4: Fetching authentication from database for refresh token");
        return jwtAuthenticationRepository.findByRefreshToken(encodedToken)
                                          .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                  String.format("JWT authentication not found by refresh token %s", encodedToken.token())));
    }

    /**
     * Generates a new JWT pair based on the decoded token claims.
     *
     * @param decodedToken the decoded token containing subject and role claims
     * @return the newly generated JWT pair
     * @throws InvalidJwtAuthenticationException if token generation fails
     */
    private JwtPair generateNewTokenPair(DecodedJwt decodedToken) {
        logger.trace("Step 5: Generating new JWT pair for subject={}, role={}", decodedToken.subject(), decodedToken.roleClaim());
        try {
            var subject = Subject.of(decodedToken.subject());
            var role = Role.valueOf(decodedToken.roleClaim());
            return tokenIssuer.issueTokenPair(subject, role);

        } catch (Exception e) {
            logger.error("Failed to generate token pair for refresh token", e);
            throw new InvalidJwtAuthenticationException("Refresh failed: could not generate tokens", e);
        }
    }

    /**
     * Updates the authentication aggregate with the new token pair.
     *
     * @param jwtAuthentication the authentication aggregate to update
     * @param jwtPair           the new JWT pair
     */
    private void updateAuthenticationWithNewTokens(JwtAuthentication jwtAuthentication, JwtPair jwtPair) {
        logger.trace("Step 6: Updating authentication with new tokens");
        jwtAuthentication.refreshTokens(jwtPair);
    }

    /**
     * Persists the updated authentication to the database.
     *
     * @param jwtAuthentication the authentication to persist
     * @return the persisted authentication
     * @throws InvalidJwtAuthenticationException if database persistence fails
     */
    private JwtAuthentication persistAuthenticationUpdate(JwtAuthentication jwtAuthentication) {
        logger.trace("Step 7: Persisting updated authentication to database");
        try {
            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (Exception e) {
            logger.error("Failed to persist updated authentication to database for refresh token", e);
            throw new InvalidJwtAuthenticationException("Refresh failed: could not persist to database", e);
        }
    }

    /**
     * Rotates tokens by removing old token pair and activating new token pair.
     *
     * @param oldJwtPair the old JWT pair to remove
     * @param newJwtPair the new JWT pair to activate
     */
    private void rotateTokens(JwtPair oldJwtPair, JwtPair newJwtPair) {
        logger.trace("Step 8: Deleting old token pair from fast-access store");
        jwtStore.delete(oldJwtPair);

        logger.trace("Step 9: Storing new token pair in fast-access store");
        jwtStore.save(newJwtPair);
    }

    /**
     * Reactivates previous tokens after a failed rotation.
     *
     * @param jwtPair the previous JWT pair to reactivate
     */
    private void reactivatePreviousTokens(JwtPair jwtPair) {
        logger.warn("Redis update failed, attempting to restore old tokens in Redis");

        try {
            jwtStore.save(jwtPair);

            logger.info("Compensating transaction completed: old tokens restored in Redis");

        } catch (Exception e) {
            logger.error("CRITICAL: Compensating transaction failed - tokens may be inconsistent in Redis", e);
        }
    }

}
