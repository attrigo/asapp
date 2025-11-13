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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.JwtDecoder;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Database-backed implementation of {@link JwtAuthenticationVerifier} for validating JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, verifying JWT tokens by validating their signature and checking their existence in the database.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DatabaseJwtAuthenticationVerifier implements JwtAuthenticationVerifier {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseJwtAuthenticationVerifier.class);

    private final JwtDecoder jwtDecoder;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DatabaseJwtAuthenticationVerifier} with required dependencies.
     *
     * @param jwtDecoder                  the JWT decoder for decoding and validating tokens
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public DatabaseJwtAuthenticationVerifier(JwtDecoder jwtDecoder, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Verifies an access token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication from the database.
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link JwtAuthentication} associated with the access token
     * @throws InvalidJwtException                if the token is invalid or verification fails
     * @throws UnexpectedJwtTypeException         if the token is not an access token
     * @throws JwtAuthenticationNotFoundException if no authentication is found for the token
     */
    @Override
    public JwtAuthentication verifyAccessToken(EncodedToken accessToken) {
        logger.trace("Verifying access token {}", accessToken);

        try {
            var decodedJwt = jwtDecoder.decode(accessToken.value());

            if (!decodedJwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedJwt.encodedToken()));
            }

            return jwtAuthenticationRepository.findByAccessToken(EncodedToken.of(decodedJwt.encodedToken()))
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      String.format("Jwt authentication not found by access token %s", decodedJwt.encodedToken())));

        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

    /**
     * Verifies a refresh token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication from the database.
     *
     * @param refreshToken the encoded refresh token to verify
     * @return the {@link JwtAuthentication} associated with the refresh token
     * @throws InvalidJwtException                if the token is invalid or verification fails
     * @throws UnexpectedJwtTypeException         if the token is not a refresh token
     * @throws JwtAuthenticationNotFoundException if no authentication is found for the token
     */
    @Override
    public JwtAuthentication verifyRefreshToken(EncodedToken refreshToken) {
        logger.trace("Verifying refresh token {}", refreshToken);

        try {
            var decodedJwt = jwtDecoder.decode(refreshToken.value());

            if (!decodedJwt.isRefreshToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not a refresh token", decodedJwt.encodedToken()));
            }

            return jwtAuthenticationRepository.findByRefreshToken(EncodedToken.of(decodedJwt.encodedToken()))
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      String.format("Jwt authentication not found by refresh token %s", decodedJwt.encodedToken())));

        } catch (Exception e) {
            var message = String.format("Refresh token is not valid: %s", refreshToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

}
