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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.authentication.application.ApplicationService;
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
 * <li>Persists authentication to database via {@link JwtAuthenticationRepository}</li>
 * <li>Stores tokens in fast-access store via {@link JwtStore}</li>
 * </ol>
 * <p>
 * If Redis storage fails after database commit, the database record is deleted to maintain consistency between storage systems.
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
     * Fist saves Database, then stores Redis. If Redis fails, database is rolled back via compensating transaction to maintain consistency.
     *
     * @param authenticateCommand the {@link AuthenticateCommand} containing user credentials
     * @return the {@link JwtAuthentication} containing access and refresh tokens with persistent ID
     * @throws IllegalArgumentException if the username or password is invalid
     * @throws BadCredentialsException  if authentication fails or token storage fails
     */
    @Override
    @Transactional
    // TODO: Handle specific exceptions for better error reporting
    public JwtAuthentication authenticate(AuthenticateCommand authenticateCommand) {
        logger.debug("Authenticating user {}", authenticateCommand.username());

        var userAuthentication = authenticateCredentials(authenticateCommand);
        var jwtPair = generateTokenPair(userAuthentication);
        var jwtAuthentication = createJwtAuthentication(userAuthentication, jwtPair);
        var savedAuthentication = persistAuthentication(jwtAuthentication);

        try {
            activateTokens(savedAuthentication.getJwtPair());

            logger.debug("Authentication completed successfully for user {}", userAuthentication.username()
                                                                                                .value());

            return savedAuthentication;

        } catch (Exception e) {
            rollbackAuthentication(savedAuthentication);
            throw new BadCredentialsException("Authentication failed: tokens could not be stored", e);
        }
    }

    /**
     * Authenticates user credentials and returns the authenticated user information.
     *
     * @param command the authentication command containing username and password
     * @return the authenticated user information
     */
    private UserAuthentication authenticateCredentials(AuthenticateCommand command) {
        logger.trace("Step 1: Validating user credentials for username={}", command.username());
        var username = Username.of(command.username());
        var password = RawPassword.of(command.password());
        return credentialsAuthenticator.authenticate(username, password);
    }

    /**
     * Generates a JWT pair (access and refresh tokens) for the authenticated user.
     *
     * @param userAuthentication the authenticated user information
     * @return the generated JWT pair
     * @throws BadCredentialsException if token generation fails
     */
    private JwtPair generateTokenPair(UserAuthentication userAuthentication) {
        logger.trace("Step 2: Generating JWT pair for userId={}", userAuthentication.userId()
                                                                                    .value());
        try {
            return tokenIssuer.issueTokenPair(userAuthentication);

        } catch (Exception e) {
            logger.error("Failed to generate token pair for user {}", userAuthentication.username()
                                                                                        .value(),
                    e);
            throw new BadCredentialsException("Authentication failed: could not generate tokens", e);
        }
    }

    /**
     * Creates a JWT authentication domain aggregate from user authentication and token pair.
     *
     * @param userAuthentication the authenticated user information
     * @param jwtPair            the JWT pair
     * @return the JWT authentication aggregate
     */
    private JwtAuthentication createJwtAuthentication(UserAuthentication userAuthentication, JwtPair jwtPair) {
        logger.trace("Step 3: Creating JWT authentication for userId={}", userAuthentication.userId()
                                                                                            .value());
        return JwtAuthentication.unAuthenticated(userAuthentication.userId(), jwtPair);
    }

    /**
     * Persists the JWT authentication to the database.
     *
     * @param jwtAuthentication the JWT authentication to persist
     * @return the persisted JWT authentication with assigned ID
     * @throws BadCredentialsException if database persistence fails
     */
    private JwtAuthentication persistAuthentication(JwtAuthentication jwtAuthentication) {
        logger.trace("Step 4: Persisting authentication to database");
        try {
            return jwtAuthenticationRepository.save(jwtAuthentication);

        } catch (Exception e) {
            logger.error("Failed to persist authentication to database", e);
            throw new BadCredentialsException("Authentication failed: could not persist to database", e);
        }
    }

    /**
     * Stores the JWT pair in Redis for fast token validation.
     *
     * @param jwtPair the JWT pair to store
     */
    private void activateTokens(JwtPair jwtPair) {
        logger.trace("Step 5: Storing tokens in fast-access store for Redis");
        jwtStore.save(jwtPair);
    }

    /**
     * Compensates for Redis storage failure by deleting the authentication from database.
     *
     * @param jwtAuthentication the authentication that was saved to database
     */
    private void rollbackAuthentication(JwtAuthentication jwtAuthentication) {
        logger.warn("Redis storage failed, rolling back database for authenticationId={}", jwtAuthentication.getId()
                                                                                                            .value());

        try {
            jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());

            logger.info("Compensating transaction completed: authentication deleted from database");

        } catch (Exception e) {
            logger.error("CRITICAL: Compensating transaction failed - orphaned record in database with authenticationId={}", jwtAuthentication.getId()
                                                                                                                                              .value(),
                    e);
        }
    }

}
