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
 * Verifies JWT tokens by validating their signature and expiration.
 * <p>
 * Used by the authentication filter for lightweight token validation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class JwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);

    private final JwtDecoder jwtDecoder;

    /**
     * Constructs a new {@code JwtVerifier} with required dependencies.
     *
     * @param jwtDecoder the JWT decoder for decoding and validating tokens
     */
    public JwtVerifier(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Verifies an access token.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the validated decoded JWT.
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link DecodedJwt} containing the decoded JWT data
     * @throws InvalidJwtException        if the token is invalid or verification fails
     * @throws UnexpectedJwtTypeException if the token is not an access token
     */
    public DecodedJwt verifyAccessToken(String accessToken) {
        logger.trace("Verifying access token {}", accessToken);

        try {
            var decodedJwt = jwtDecoder.decode(accessToken);

            if (!decodedJwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedJwt.encodedToken()));
            }

            return decodedJwt;

        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

}
