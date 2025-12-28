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
import com.bcn.asapp.authentication.application.PersistenceException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Application service responsible for orchestrating authentication revocation.
 * <p>
 * Coordinates the complete revocation workflow including token verification and removal from both fast-access store and persistent storage.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Verifies access token via {@link TokenVerifier}</li>
 * <li>Fetches authentication from repository via {@link JwtAuthenticationRepository}</li>
 * <li>Deletes token pair from fast-access store</li>
 * <li>Deletes authentication from repository</li>
 * </ol>
 * <p>
 * If repository deletion fails after fast-access store deletion, tokens are restored to fast-access store to maintain consistency.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RevokeAuthenticationService implements RevokeAuthenticationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RevokeAuthenticationService.class);

    private final TokenVerifier tokenVerifier;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code RevokeAuthenticationService} with required dependencies.
     *
     * @param tokenVerifier               the token verifier for verifying access tokens
     * @param jwtAuthenticationRepository the repository for persisting JWT authentications
     * @param jwtStore                    the store for fast token lookup and validation
     */
    public RevokeAuthenticationService(TokenVerifier tokenVerifier, JwtAuthenticationRepository jwtAuthenticationRepository, JwtStore jwtStore) {
        this.tokenVerifier = tokenVerifier;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtStore = jwtStore;
    }

    /**
     * Revokes an authentication using a valid access token.
     * <p>
     * Orchestrates the complete revocation workflow: verification and removal from both storage systems.
     * <p>
     * Deletes from fast-access store first, then repository. If repository deletion fails, tokens are restored to fast-access store via compensating
     * transaction.
     *
     * @param accessToken the access token string
     * @throws IllegalArgumentException         if the access token is invalid or blank
     * @throws UnexpectedJwtTypeException       if the provided token is not an access token
     * @throws AuthenticationNotFoundException  if the token is not found in active sessions or repository
     * @throws PersistenceException             if authentication deletion fails (after compensation)
     * @throws CompensatingTransactionException if compensating transaction fails
     */
    @Override
    @Transactional
    public void revokeAuthentication(String accessToken) {
        logger.debug("Revoking authentication with access token");

        var encodedAccessToken = EncodedToken.of(accessToken);
        verifyAccessToken(encodedAccessToken);

        var authentication = retrieveAuthentication(encodedAccessToken);
        var jwtPairToDelete = authentication.getJwtPair();

        deactivateTokens(jwtPairToDelete);
        try {
            deleteAuthentication(authentication);

            logger.debug("Authentication revoked successfully with ID {}", authentication.getId()
                                                                                         .value());

        } catch (PersistenceException e) {
            compensateTokenDeactivation(jwtPairToDelete);
            throw e;
        }
    }

    /**
     * Verifies the access token.
     *
     * @param encodedToken the encoded access token to verify
     * @throws UnexpectedJwtTypeException if the provided token is not an access token
     */
    private void verifyAccessToken(EncodedToken encodedToken) {
        logger.trace("Step 1: Verifying access token");
        tokenVerifier.verifyAccessToken(encodedToken);
    }

    /**
     * Fetches the authentication record from the repository using the access token.
     *
     * @param encodedToken the access token to search for
     * @return the JWT authentication from repository
     * @throws AuthenticationNotFoundException if authentication is not found
     */
    private JwtAuthentication retrieveAuthentication(EncodedToken encodedToken) {
        logger.trace("Step 2: Fetching authentication from repository for subject={}", encodedToken.token());
        return jwtAuthenticationRepository.findByAccessToken(encodedToken)
                                          .orElseThrow(() -> new AuthenticationNotFoundException(
                                                  String.format("Authentication not found by access token %s", encodedToken.token())));
    }

    /**
     * Deactivates the token pair by removing it from the fast-access store.
     *
     * @param jwtPair the JWT pair to deactivate
     */
    private void deactivateTokens(JwtPair jwtPair) {
        logger.trace("Step 3: Deleting token pair from fast-access store");
        jwtStore.delete(jwtPair);
    }

    /**
     * Deletes the authentication from the repository.
     *
     * @param jwtAuthentication the authentication to delete
     */
    private void deleteAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("Step 4: Deleting authentication authenticationId={} from repository", jwtAuthentication.getId()
                                                                                                             .value());
        jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());
    }

    /**
     * Compensates for repository deletion failure by restoring tokens to fast-access store.
     *
     * @param jwtPair the JWT pair to restore
     * @throws CompensatingTransactionException if compensation fails
     */
    private void compensateTokenDeactivation(JwtPair jwtPair) {
        logger.warn("Restoring tokens to fast-access store");

        try {
            jwtStore.save(jwtPair);

            logger.debug("Compensating transaction completed: tokens restored to fast-access store");

        } catch (Exception e) {
            throw new CompensatingTransactionException("Failed to compensate token deactivation after repository deletion failure", e);
        }
    }

}
