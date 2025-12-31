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
import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.in.RefreshAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Application service responsible for orchestrating authentication refresh.
 * <p>
 * Coordinates the complete token refresh workflow including token verification, generation of new tokens, cleanup of old tokens, and persistence across
 * multiple storage systems.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Verifies refresh token via {@link TokenVerifier}</li>
 * <li>Fetches authentication from repository via {@link JwtAuthenticationRepository}</li>
 * <li>Generates new JWT pair via {@link TokenIssuer}</li>
 * <li>Updates domain aggregate with new tokens</li>
 * <li>Persists updated authentication to repository</li>
 * <li>Deletes old tokens from fast-access store and stores new tokens</li>
 * </ol>
 * <p>
 * If fast-access store operations fail after repository commit, the old authentication state is restored to maintain consistency.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RefreshAuthenticationService implements RefreshAuthenticationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RefreshAuthenticationService.class);

    private final TokenVerifier tokenVerifier;

    private final TokenIssuer tokenIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code RefreshAuthenticationService} with required dependencies.
     *
     * @param tokenVerifier               the token verifier for verifying refresh tokens
     * @param tokenIssuer                 the token issuer for generating new JWTs
     * @param jwtAuthenticationRepository the repository for persisting JWT authentications
     * @param jwtStore                    the store for fast token lookup and validation
     */
    public RefreshAuthenticationService(TokenVerifier tokenVerifier, TokenIssuer tokenIssuer, JwtAuthenticationRepository jwtAuthenticationRepository,
            JwtStore jwtStore) {

        this.tokenVerifier = tokenVerifier;
        this.tokenIssuer = tokenIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtStore = jwtStore;
    }

    /**
     * Refreshes an authentication using a valid refresh token.
     * <p>
     * Orchestrates the complete token refresh workflow: verification, new token generation, and persistence with cleanup.
     * <p>
     * First updates repository, then updates fast-access store. If fast-access store fails, old state is restored via compensating transaction.
     *
     * @param refreshToken the refresh token string
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws IllegalArgumentException         if the refresh token is invalid or blank
     * @throws UnexpectedJwtTypeException       if the provided token is not a refresh token
     * @throws AuthenticationNotFoundException  if the token is not found in active sessions or repository
     * @throws TokenStoreException              if token rotation fails (after compensation)
     * @throws CompensatingTransactionException if compensating transaction fails
     */
    @Override
    @Transactional
    public JwtAuthentication refreshAuthentication(String refreshToken) {
        logger.debug("[REFRESH] Refreshing authentication");

        var encodedRefreshToken = EncodedToken.of(refreshToken);
        verifyRefreshToken(encodedRefreshToken);

        var authentication = retrieveAuthentication(encodedRefreshToken);
        var oldJwtPair = authentication.getJwtPair();

        var newJwtPair = generateNewTokenPair(authentication.refreshToken());
        updateAuthenticationWithNewTokens(authentication, newJwtPair);

        var updatedAuthentication = persistAuthenticationUpdate(authentication);
        try {
            rotateTokens(oldJwtPair, updatedAuthentication.getJwtPair());

            logger.debug("[REFRESH] Authentication refreshed successfully");

            return updatedAuthentication;

        } catch (TokenStoreException e) {
            compensateTokenRotation(oldJwtPair);
            throw e;
        }
    }

    /**
     * Verifies the refresh token.
     *
     * @param encodedToken the encoded refresh token to verify
     * @throws UnexpectedJwtTypeException      if the provided token is not a refresh token
     * @throws AuthenticationNotFoundException if the authentication session is not found in fast-access store
     */
    private void verifyRefreshToken(EncodedToken encodedToken) {
        logger.trace("[REFRESH] Step 1/7: Verifying refresh token");
        tokenVerifier.verifyRefreshToken(encodedToken);
    }

    /**
     * Fetches the authentication record from the repository using the refresh token.
     *
     * @param encodedToken the refresh token to search for
     * @return the JWT authentication from repository
     * @throws AuthenticationNotFoundException if authentication is not found in repository
     */
    private JwtAuthentication retrieveAuthentication(EncodedToken encodedToken) {
        logger.trace("[REFRESH] Step 2/7: Fetching authentication from repository");
        return jwtAuthenticationRepository.findByRefreshToken(encodedToken);
    }

    /**
     * Generates a new JWT pair based on the JWT claims.
     *
     * @param jwt the JWT containing subject and role claims
     * @return the newly generated JWT pair
     */
    private JwtPair generateNewTokenPair(Jwt jwt) {
        logger.trace("[REFRESH] Step 3/7: Generating new JWT pair");
        return tokenIssuer.issueTokenPair(jwt.subject(), jwt.roleClaim());
    }

    /**
     * Updates the authentication aggregate with the new token pair.
     *
     * @param jwtAuthentication the authentication aggregate to update
     * @param jwtPair           the new JWT pair
     */
    private void updateAuthenticationWithNewTokens(JwtAuthentication jwtAuthentication, JwtPair jwtPair) {
        logger.trace("[REFRESH] Step 4/7: Updating authentication with new tokens");
        jwtAuthentication.refreshTokens(jwtPair);
    }

    /**
     * Persists the updated authentication to the repository.
     *
     * @param jwtAuthentication the authentication to persist
     * @return the persisted authentication
     */
    private JwtAuthentication persistAuthenticationUpdate(JwtAuthentication jwtAuthentication) {
        logger.trace("[REFRESH] Step 5/7: Persisting updated authentication to repository");
        return jwtAuthenticationRepository.save(jwtAuthentication);
    }

    /**
     * Rotates tokens by removing old token pair and activating new token pair.
     *
     * @param oldJwtPair the old JWT pair to remove
     * @param newJwtPair the new JWT pair to activate
     */
    private void rotateTokens(JwtPair oldJwtPair, JwtPair newJwtPair) {
        logger.trace("[REFRESH] Step 6/7: Deleting old token pair from fast-access store");
        jwtStore.delete(oldJwtPair);

        logger.trace("[REFRESH] Step 7/7: Storing new token pair in fast-access store");
        jwtStore.save(newJwtPair);
    }

    /**
     * Reactivates previous tokens after a failed rotation.
     *
     * @param jwtPair the previous JWT pair to reactivate
     * @throws CompensatingTransactionException if compensation fails
     */
    private void compensateTokenRotation(JwtPair jwtPair) {
        logger.warn("[REFRESH] Token rotation failed, restoring old tokens in fast-access store");

        try {
            jwtStore.save(jwtPair);

            logger.debug("[REFRESH] Compensation complete, old tokens restored in fast-access store");

        } catch (Exception e) {
            throw new CompensatingTransactionException("Failed to compensate token rotation after token activation failure", e);
        }
    }

}
