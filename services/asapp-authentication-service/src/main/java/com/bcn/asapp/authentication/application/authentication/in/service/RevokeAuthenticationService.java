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
import com.bcn.asapp.authentication.application.PersistenceException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.TokenStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.InvalidEncodedTokenException;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Application service responsible for orchestrating authentication revocation.
 * <p>
 * Coordinates the complete revocation workflow including token verification and removal from persistent storage and fast-access store.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Verifies access token</li>
 * <li>Fetches authentication from repository</li>
 * <li>Deletes authentication from repository</li>
 * <li>Deletes token pair from fast-access store</li>
 * </ol>
 * <p>
 * Token deactivation occurs after successful repository deletion, ensuring no compensation is needed if repository operations fail.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RevokeAuthenticationService implements RevokeAuthenticationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RevokeAuthenticationService.class);

    private final TokenVerifier tokenVerifier;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final TokenStore tokenStore;

    /**
     * Constructs a new {@code RevokeAuthenticationService} with required dependencies.
     *
     * @param tokenVerifier               the token verifier for verifying access tokens
     * @param jwtAuthenticationRepository the repository for JWT authentications data access
     * @param tokenStore                  the store for fast token lookup and validation
     */
    public RevokeAuthenticationService(TokenVerifier tokenVerifier, JwtAuthenticationRepository jwtAuthenticationRepository, TokenStore tokenStore) {
        this.tokenVerifier = tokenVerifier;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.tokenStore = tokenStore;
    }

    /**
     * Revokes an authentication using a valid access token.
     * <p>
     * Orchestrates the complete revocation workflow: verification and removal from both storage systems.
     * <p>
     * First deletes from fast-access store, then deletes from repository. If repository deletion fails, tokens are restored to fast-access store via
     * compensating transaction.
     *
     * @param accessToken the access token string
     * @throws InvalidEncodedTokenException    if the access token does not conform to JWT format
     * @throws UnexpectedJwtTypeException      if the provided token is not an access token
     * @throws AuthenticationNotFoundException if the token is not found in active sessions or repository
     * @throws PersistenceException            if authentication deletion fails
     */
    @Override
    @Transactional
    public void revokeAuthentication(String accessToken) {
        logger.debug("[REVOKE] Revoking authentication");

        var encodedAccessToken = EncodedToken.of(accessToken);
        verifyAccessToken(encodedAccessToken);

        var authentication = retrieveAuthentication(encodedAccessToken);
        var jwtPairToDelete = authentication.getJwtPair();

        deleteAuthentication(authentication);
        deactivateTokens(jwtPairToDelete);

        logger.debug("[REVOKE] Authentication revoked successfully for authenticationId={}", authentication.getId()
                                                                                                           .value());
    }

    /**
     * Verifies the access token.
     *
     * @param encodedToken the encoded access token to verify
     * @throws UnexpectedJwtTypeException      if the provided token is not an access token
     * @throws AuthenticationNotFoundException if the authentication session is not found in fast-access store
     */
    private void verifyAccessToken(EncodedToken encodedToken) {
        logger.trace("[REVOKE] Step 1/4: Verifying access token");
        tokenVerifier.verifyAccessToken(encodedToken);
    }

    /**
     * Fetches the authentication record from the repository using the access token.
     *
     * @param encodedToken the access token to search for
     * @return the {@link JwtAuthentication} from repository
     * @throws AuthenticationNotFoundException if authentication is not found in repository
     */
    private JwtAuthentication retrieveAuthentication(EncodedToken encodedToken) {
        logger.trace("[REVOKE] Step 2/4: Fetching authentication from repository");
        return jwtAuthenticationRepository.findByAccessToken(encodedToken);
    }

    /**
     * Deactivates the token pair by removing it from the fast-access store.
     *
     * @param jwtPair the JWT pair to deactivate
     */
    private void deactivateTokens(JwtPair jwtPair) {
        logger.trace("[REVOKE] Step 4/4: Deleting token pair from fast-access store");
        tokenStore.delete(jwtPair);
    }

    /**
     * Deletes the authentication from the repository.
     *
     * @param jwtAuthentication the authentication to delete
     */
    private void deleteAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("[REVOKE] Step 3/4: Deleting authentication from repository");
        jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());
    }

}
