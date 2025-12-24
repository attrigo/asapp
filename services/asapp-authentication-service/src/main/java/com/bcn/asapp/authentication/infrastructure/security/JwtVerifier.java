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

package com.bcn.asapp.authentication.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Component for verifying JWTs.
 * <p>
 * Verifies JWTs by validating their signature, expiration, and revocation status.
 * <p>
 * Performs three-step validation: cryptographic verification via {@link JwtDecoder}, token type check, and session validation via {@link JwtStore}.
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

    private final JwtStore jwtStore;

    /**
     * Constructs a new {@code JwtVerifier} with required dependencies.
     *
     * @param jwtDecoder the JWT decoder for decoding and validating tokens
     * @param jwtStore   the JWT store for checking token revocation status
     */
    public JwtVerifier(JwtDecoder jwtDecoder, JwtStore jwtStore) {
        this.jwtDecoder = jwtDecoder;
        this.jwtStore = jwtStore;
    }

    /**
     * Verifies an access token.
     * <p>
     * Validates the token's signature, expiration, revocation status, and type, then returns the validated decoded JWT.
     * <p>
     * Performs three verification steps:
     * <ol>
     * <li>Decodes the token and validates its cryptographic signature and expiration</li>
     * <li>Verifies the token type is an access token (not refresh token)</li>
     * <li>Checks the authentication session exists in the active token store</li>
     * </ol>
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link DecodedJwt} containing the decoded JWT data
     * @throws InvalidJwtException             if the token is invalid or verification fails
     * @throws AuthenticationNotFoundException if the authentication session is not found
     * @throws UnexpectedJwtTypeException      if the token is not an access token
     */
    public DecodedJwt verifyAccessToken(String accessToken) {
        logger.debug("Verifying access token {}", accessToken);

        try {
            var encodedToken = EncodedToken.of(accessToken);

            var decodedJwt = decodeToken(encodedToken);
            verifyAccessTokenType(decodedJwt);
            checkAccessTokenInActiveStore(encodedToken);

            logger.debug("Access token {} verified successfully", accessToken);

            return decodedJwt;

        } catch (AuthenticationNotFoundException | UnexpectedJwtTypeException e) {
            throw e;
        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

    /**
     * Decodes and cryptographically validates the JWT token.
     * <p>
     * Verification step 1: Validates the token's signature using the configured signing key and checks the expiration timestamp.
     *
     * @param encodedToken the encoded token to decode
     * @return the {@link DecodedJwt} containing the decoded token data and claims
     * @throws InvalidJwtException if the token signature is invalid or the token is expired
     */
    private DecodedJwt decodeToken(EncodedToken encodedToken) {
        logger.trace("Step 1: Decoding and validating the token");
        return jwtDecoder.decode(encodedToken);
    }

    /**
     * Verifies the token type is an access token.
     * <p>
     * Verification step 2: Checks the token_use claim to ensure this is an access token and not a refresh token.
     *
     * @param decodedJwt the decoded JWT to verify
     * @throws UnexpectedJwtTypeException if the token is not an access token
     */
    private void verifyAccessTokenType(DecodedJwt decodedJwt) {
        logger.trace("Step 2: Verifying token type is access token");
        if (!decodedJwt.isAccessToken()) {
            throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedJwt.encodedToken()));
        }
    }

    /**
     * Checks the authentication session exists in the active token store.
     * <p>
     * Verification step 3: Queries the fast-access store (Redis) to verify the token has not been revoked and the authentication session is still active.
     *
     * @param encodedToken the encoded token to check
     * @throws AuthenticationNotFoundException if the authentication session is not found (token revoked or expired)
     */
    private void checkAccessTokenInActiveStore(EncodedToken encodedToken) {
        logger.trace("Step 3: Checking access token exists in fast-access store");
        var isTokenActive = jwtStore.accessTokenExists(encodedToken);
        if (!isTokenActive) {
            throw new AuthenticationNotFoundException(String.format("Authentication session not found for access token: %s", encodedToken.token()));
        }
    }

}
