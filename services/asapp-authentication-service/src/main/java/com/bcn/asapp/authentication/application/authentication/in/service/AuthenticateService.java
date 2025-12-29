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
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.in.AuthenticateUseCase;
import com.bcn.asapp.authentication.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.authentication.application.authentication.out.CredentialsAuthenticator;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Application service responsible for orchestrating user authentication.
 * <p>
 * Coordinates the complete authentication workflow including credential validation, token generation, and persistence across multiple storage systems.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Validates user credentials via {@link CredentialsAuthenticator}</li>
 * <li>Generates JWT pair via {@link TokenIssuer}</li>
 * <li>Creates {@link JwtAuthentication} domain aggregate</li>
 * <li>Persists authentication to repository via {@link JwtAuthenticationRepository}</li>
 * <li>Stores tokens in fast-access store via {@link JwtStore}</li>
 * </ol>
 * <p>
 * If fast-access store fails after repository commit, the persisted authentication is deleted to maintain consistency between storage systems.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class AuthenticateService implements AuthenticateUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticateService.class);

    private final CredentialsAuthenticator credentialsAuthenticator;

    private final TokenIssuer tokenIssuer;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code AuthenticateService} with required dependencies.
     *
     * @param credentialsAuthenticator    the credentials authenticator for validating user credentials
     * @param tokenIssuer                 the token issuer for generating JWTs
     * @param jwtAuthenticationRepository the repository for persisting JWT authentications
     * @param jwtStore                    the store for fast token lookup and validation
     */
    public AuthenticateService(CredentialsAuthenticator credentialsAuthenticator, TokenIssuer tokenIssuer,
            JwtAuthenticationRepository jwtAuthenticationRepository, JwtStore jwtStore) {

        this.credentialsAuthenticator = credentialsAuthenticator;
        this.tokenIssuer = tokenIssuer;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtStore = jwtStore;
    }

    /**
     * Authenticates a user based on provided credentials.
     * <p>
     * Orchestrates the complete authentication workflow: credential validation, token generation, and persistence.
     * <p>
     * First saves to repository, then stores in fast-access store. If fast-access store fails, repository is rolled back via compensating transaction to
     * maintain consistency.
     *
     * @param authenticateCommand the {@link AuthenticateCommand} containing user credentials
     * @return the {@link JwtAuthentication} containing access and refresh tokens with persistent ID
     * @throws IllegalArgumentException         if the username or password is invalid
     * @throws CompensatingTransactionException if compensating transaction fails
     */
    @Override
    @Transactional
    public JwtAuthentication authenticate(AuthenticateCommand authenticateCommand) {
        logger.debug("[AUTHENTICATE] Authenticating user with username={}", authenticateCommand.username());

        var userAuthentication = authenticateCredentials(authenticateCommand);

        var jwtPair = generateTokenPair(userAuthentication);
        var jwtAuthentication = createJwtAuthentication(userAuthentication, jwtPair);

        var savedAuthentication = persistAuthentication(jwtAuthentication);
        try {
            activateTokens(savedAuthentication.getJwtPair());

            logger.debug("[AUTHENTICATE] Authentication successful for subject={}", userAuthentication.username()
                                                                                                      .value());

            return savedAuthentication;

        } catch (TokenStoreException e) {
            compensateRepositoryPersistence(savedAuthentication);
            throw e;
        }
    }

    /**
     * Authenticates user credentials and returns the authenticated user information.
     *
     * @param command the authentication command containing username and password
     * @return the authenticated user information
     */
    private UserAuthentication authenticateCredentials(AuthenticateCommand command) {
        logger.trace("[AUTHENTICATE] Step 1/4: Authenticating credentials");
        var username = Username.of(command.username());
        var password = RawPassword.of(command.password());

        return credentialsAuthenticator.authenticate(username, password);
    }

    /**
     * Generates a JWT pair (access and refresh tokens) for the authenticated user.
     *
     * @param userAuthentication the authenticated user information
     * @return the generated JWT pair
     */
    private JwtPair generateTokenPair(UserAuthentication userAuthentication) {
        logger.trace("[AUTHENTICATE] Step 2/4: Generating JWT pair");
        return tokenIssuer.issueTokenPair(userAuthentication);
    }

    /**
     * Creates a JWT authentication domain aggregate from user authentication and token pair.
     *
     * @param userAuthentication the authenticated user information
     * @param jwtPair            the JWT pair
     * @return the JWT authentication aggregate
     */
    private JwtAuthentication createJwtAuthentication(UserAuthentication userAuthentication, JwtPair jwtPair) {
        return JwtAuthentication.unAuthenticated(userAuthentication.userId(), jwtPair);
    }

    /**
     * Persists the JWT authentication to the repository.
     *
     * @param jwtAuthentication the JWT authentication to persist
     * @return the persisted JWT authentication with assigned ID
     */
    private JwtAuthentication persistAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("[AUTHENTICATE] Step 3/4: Persisting authentication to repository");
        return jwtAuthenticationRepository.save(jwtAuthentication);
    }

    /**
     * Stores the JWT pair in fast-access store for fast token validation.
     *
     * @param jwtPair the JWT pair to store
     */
    private void activateTokens(JwtPair jwtPair) {
        logger.trace("[AUTHENTICATE] Step 4/4: Storing token pair in fast-access store");
        jwtStore.save(jwtPair);
    }

    /**
     * Compensates for fast-access store failure by deleting the authentication from repository.
     *
     * @param jwtAuthentication the authentication that was saved to repository
     * @throws CompensatingTransactionException if compensation fails
     */
    private void compensateRepositoryPersistence(JwtAuthentication jwtAuthentication) {
        logger.warn("[AUTHENTICATE] Token activation failed, rolling back authenticationId={} from repository", jwtAuthentication.getId()
                                                                                                                                 .value());

        try {
            jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());

            logger.debug("[AUTHENTICATE] Compensation complete, authentication deleted from repository");

        } catch (Exception e) {
            throw new CompensatingTransactionException("Failed to compensate repository persistence after token activation failure", e);
        }
    }

}
