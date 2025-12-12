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
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;

/**
 * Application service responsible for orchestrating user authentication.
 * <p>
 * Coordinates the complete authentication workflow including credential validation, token generation, and persistence across multiple storage systems.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Validates user credentials via {@link CredentialsAuthenticator}</li>
 * <li>Generates access token via {@link TokenIssuer}</li>
 * <li>Generates refresh token via {@link TokenIssuer}</li>
 * <li>Creates {@link JwtAuthentication} domain aggregate</li>
 * <li>Persists authentication to database via {@link JwtAuthenticationRepository}</li>
 * <li>Stores tokens in fast-access store via {@link JwtStore}</li>
 * </ol>
 * <p>
 * The entire authentication workflow executes within a single transaction to ensure consistency between database and token store operations.
 *
 * @author attrigo
 * @since 0.2.0
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
     * @param tokenIssuer                 the token issuer for generating JWT tokens
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
     * The operation is transactional to ensure consistency between the database and token store.
     *
     * @param authenticateCommand the {@link AuthenticateCommand} containing user credentials
     * @return the {@link JwtAuthentication} containing access and refresh tokens with persistent ID
     * @throws IllegalArgumentException if the username or password is invalid
     * @throws BadCredentialsException  if authentication fails
     * @throws InvalidJwtException      if token generation fails
     */
    @Override
    @Transactional
    // TODO: Handle specific exceptions for better error reporting
    // TODO: Improve transaction management (what if token store operation fails?)
    // TODO: Refactor to simplify method and improve readability
    public JwtAuthentication authenticate(AuthenticateCommand authenticateCommand) {
        logger.debug("Authenticating user {}", authenticateCommand.username());

        logger.trace("Step 1: Validating user credentials for username={}", authenticateCommand.username());
        var username = Username.of(authenticateCommand.username());
        var password = RawPassword.of(authenticateCommand.password());
        var userAuthentication = credentialsAuthenticator.authenticate(username, password);

        try {
            logger.trace("Step 2: Generating access and refresh tokens for userId={}", userAuthentication.userId()
                                                                                                         .value());
            var accessToken = tokenIssuer.issueAccessToken(userAuthentication);
            var refreshToken = tokenIssuer.issueRefreshToken(userAuthentication);
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            logger.trace("Step 3: Creating JWT authentication domain aggregate for userId={}", userAuthentication.userId()
                                                                                                                 .value());
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userAuthentication.userId(), jwtPair);

            logger.trace("Step 4: Persisting authentication to database");
            var savedAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);

            logger.trace("Step 5: Storing tokens in fast-access store for authenticationId={}", savedAuthentication.getId()
                                                                                                                   .value());
            jwtStore.save(savedAuthentication.getJwtPair());

            logger.debug("Authentication completed successfully for user {}", userAuthentication.username()
                                                                                                .value());

            return savedAuthentication;

        } catch (Exception e) {
            var message = String.format("Authentication could not be granted due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new BadCredentialsException(message, e);
        }
    }

}
