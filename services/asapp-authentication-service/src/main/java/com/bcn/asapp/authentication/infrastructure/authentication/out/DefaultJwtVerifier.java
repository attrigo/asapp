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

import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.JwtDecoder;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Default implementation of {@link JwtVerifier} for validating JWT tokens.
 * <p>
 * Bridges the application layer with the infrastructure layer, decoding and verifying JWT tokens and retrieving their associated authentications from the
 * repository.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DefaultJwtVerifier implements JwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJwtVerifier.class);

    private final JwtDecoder jwtDecoder;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code JwtVerifierAdapter} with required dependencies.
     *
     * @param jwtDecoder                  the JWT decoder for decoding and validating tokens
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public DefaultJwtVerifier(JwtDecoder jwtDecoder, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Verifies an access token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication.
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link JwtAuthentication} associated with the access token
     * @throws InvalidJwtException                if the token is invalid or verification fails
     * @throws UnexpectedJwtTypeException         if the token is not an access token
     * @throws JwtAuthenticationNotFoundException if no authentication is found for the token
     */
    @Override
    public final JwtAuthentication verifyAccessToken(EncodedToken accessToken) {
        logger.trace("Verifying access token {}", accessToken);

        try {
            var jwt = jwtDecoder.decode(accessToken);

            if (!jwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not a %s", jwt.encodedTokenValue(), ACCESS_TOKEN));
            }

            return jwtAuthenticationRepository.findByAccessToken(jwt.encodedToken())
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      String.format("Jwt authentication not found by access token %s", jwt.encodedTokenValue())));

        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

    /**
     * Verifies a refresh token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication.
     *
     * @param refreshToken the encoded refresh token to verify
     * @return the {@link JwtAuthentication} associated with the refresh token
     * @throws InvalidJwtException                if the token is invalid or verification fails
     * @throws UnexpectedJwtTypeException         if the token is not a refresh token
     * @throws JwtAuthenticationNotFoundException if no authentication is found for the token
     */
    @Override
    public final JwtAuthentication verifyRefreshToken(EncodedToken refreshToken) {
        logger.trace("Verifying refresh token {}", refreshToken);

        try {
            var jwt = jwtDecoder.decode(refreshToken);

            if (!jwt.isRefreshToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not a %s", jwt.encodedTokenValue(), REFRESH_TOKEN));
            }

            return jwtAuthenticationRepository.findByRefreshToken(jwt.encodedToken())
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      String.format("Jwt authentication not found by refresh token %s", jwt.encodedTokenValue())));

        } catch (Exception e) {
            var message = String.format("Refresh token is not valid: %s", refreshToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

}
