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
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.infrastructure.security.DecodedJwt;
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
 * If database deletion fails after Redis deletion, tokens are restored to Redis to maintain consistency.
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
     * Deletes from Redis first, then database. If database deletion fails, tokens are restored to Redis via compensating transaction.
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
    public void revokeAuthentication(String accessToken) {
        logger.debug("Revoking authentication with access token");

        var encodedAccessToken = EncodedToken.of(accessToken);

        var decodedToken = decodeToken(encodedAccessToken);
        verifyTokenType(decodedToken);
        checkTokenInActiveStore(encodedAccessToken);

        var authentication = retrieveAuthentication(encodedAccessToken);
        var jwtPairToDelete = authentication.getJwtPair();

        deactivateTokens(jwtPairToDelete);

        try {
            deleteAuthentication(authentication);

            logger.debug("Authentication revoked successfully with ID {}", authentication.getId()
                                                                                         .value());

        } catch (Exception e) {
            reactivateTokens(jwtPairToDelete);
            throw new InvalidJwtAuthenticationException("Revocation failed: could not delete from database", e);
        }
    }

    /**
     * Decodes and validates the access token.
     *
     * @param encodedToken the encoded access token
     * @return the decoded JWT with claims
     */
    private DecodedJwt decodeToken(EncodedToken encodedToken) {
        logger.trace("Step 1: Decoding and validating access token");
        return tokenDecoder.decode(encodedToken);
    }

    /**
     * Verifies that the decoded token is an access token.
     *
     * @param decodedToken the decoded JWT to verify
     * @throws UnexpectedJwtTypeException if the token is not an access token
     */
    private void verifyTokenType(DecodedJwt decodedToken) {
        logger.trace("Step 2: Verifying token type is access token");
        if (!decodedToken.isAccessToken()) {
            throw new UnexpectedJwtTypeException(String.format("Token %s is not an access token", decodedToken.encodedToken()));
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
        var isTokenActive = jwtStore.accessTokenExists(encodedToken);
        if (!isTokenActive) {
            throw new JwtAuthenticationNotFoundException(
                    String.format("Access token not found in active sessions (revoked or expired): %s", encodedToken.token()));
        }
    }

    /**
     * Fetches the authentication record from the database using the access token.
     *
     * @param encodedToken the access token to search for
     * @return the JWT authentication from database
     * @throws JwtAuthenticationNotFoundException if authentication is not found
     */
    private JwtAuthentication retrieveAuthentication(EncodedToken encodedToken) {
        logger.trace("Step 4: Fetching authentication from database for subject={}", encodedToken.token());
        return jwtAuthenticationRepository.findByAccessToken(encodedToken)
                                          .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                  String.format("JWT authentication not found by access token %s", encodedToken.token())));
    }

    /**
     * Deactivates the token pair by removing it from the fast-access store.
     *
     * @param jwtPair the JWT pair to deactivate
     * @throws InvalidJwtAuthenticationException if token deactivation fails
     */
    private void deactivateTokens(JwtPair jwtPair) {
        logger.trace("Step 5: Deleting token pair from fast-access store");
        try {
            jwtStore.delete(jwtPair);

        } catch (Exception e) {
            logger.error("Failed to deactivate tokens from fast-access store", e);
            throw new InvalidJwtAuthenticationException("Revocation failed: could not deactivate tokens", e);
        }
    }

    /**
     * Deletes the authentication from the database.
     *
     * @param jwtAuthentication the authentication to delete
     */
    private void deleteAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("Step 6: Deleting authentication authenticationId={} from database", jwtAuthentication.getId()
                                                                                                           .value());
        jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());
    }

    /**
     * Compensates for database deletion failure by restoring tokens to Redis.
     *
     * @param jwtPair the JWT pair to restore
     */
    private void reactivateTokens(JwtPair jwtPair) {
        logger.warn("Database deletion failed, restoring tokens to Redis");

        try {
            jwtStore.save(jwtPair);

            logger.info("Compensating transaction completed: tokens restored to Redis");

        } catch (Exception e) {
            logger.error("CRITICAL: Compensating transaction failed - tokens deleted from Redis but authentication still in database", e);
        }
    }

}
