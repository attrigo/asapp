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

package com.bcn.asapp.tasks.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component for verifying JWT tokens.
 * <p>
 * Verifies JWT tokens by validating their signature, expiration, and revocation status.
 * <p>
 * Performs two-step validation: cryptographic verification via {@link JwtDecoder} and revocation check via {@link JwtValidator}.
 * <p>
 * Used by the authentication filter for token validation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class JwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);

    private final JwtDecoder jwtDecoder;

    private final JwtValidator jwtValidator;

    /**
     * Constructs a new {@code JwtVerifier} with required dependencies.
     *
     * @param jwtDecoder   the JWT decoder for decoding and validating tokens
     * @param jwtValidator the JWT validate for checking token revocation status
     */
    public JwtVerifier(JwtDecoder jwtDecoder, JwtValidator jwtValidator) {
        this.jwtDecoder = jwtDecoder;
        this.jwtValidator = jwtValidator;
    }

    /**
     * Verifies an access token.
     * <p>
     * Validates the token's signature, expiration, revocation status, and type, then returns the validated decoded JWT.
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link DecodedJwt} containing the decoded JWT data
     * @throws InvalidJwtException        if the token is invalid, revoked, or verification fails
     * @throws UnexpectedJwtTypeException if the token is not an access token
     */
    public DecodedJwt verifyAccessToken(String accessToken) {
        logger.trace("Verifying access token {}", accessToken);

        try {
            var decodedJwt = jwtDecoder.decode(accessToken);
            if (!decodedJwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedJwt.encodedToken()));
            }

            var isTokenActive = jwtValidator.accessTokenExists(accessToken);
            if (!isTokenActive) {
                // TODO: Use more specific Exception?
                throw new InvalidJwtException(String.format("Access token has been revoked or expired: %s", accessToken));
            }

            return decodedJwt;

        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

}
