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
import com.bcn.asapp.authentication.application.authentication.InvalidJwtException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Infrastructure component responsible for orchestrating JWT token verification through a 3-step validation process.
 * <p>
 * Provides the infrastructure capability to verify JWTs using {@link JwtDecoder} and {@link JwtStore}.
 * <p>
 * Performs three-step validation: cryptographic verification via {@link JwtDecoder}, token type check, and session validation via {@link JwtStore}.
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
     * @param encodedToken the {@link EncodedToken} to verify
     * @return the {@link DecodedJwt} containing the decoded JWT data
     * @throws InvalidJwtException             if the token is invalid or verification fails
     * @throws AuthenticationNotFoundException if the authentication session is not found
     * @throws UnexpectedJwtTypeException      if the token is not an access token
     */
    public DecodedJwt verifyAccessToken(EncodedToken encodedToken) {
        logger.debug("[JWT_VERIFIER] Verifying access token");

        try {
            var decodedJwt = decodeToken(encodedToken);
            verifyAccessTokenType(decodedJwt);
            checkAccessTokenInActiveStore(encodedToken);

            return decodedJwt;

        } catch (UnexpectedJwtTypeException | AuthenticationNotFoundException e) {
            throw e;
        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", encodedToken.token());
            throw new InvalidJwtException(message, e);
        }
    }

    /**
     * Verifies a refresh token.
     * <p>
     * Validates the token's signature, expiration, revocation status, and type, then returns the validated decoded JWT.
     * <p>
     * Performs three verification steps:
     * <ol>
     * <li>Decodes the token and validates its cryptographic signature and expiration</li>
     * <li>Verifies the token type is a refresh token (not access token)</li>
     * <li>Checks the authentication session exists in the active token store</li>
     * </ol>
     *
     * @param encodedToken the {@link EncodedToken} to verify
     * @return the {@link DecodedJwt} containing the decoded JWT data
     * @throws InvalidJwtException             if the token is invalid or verification fails
     * @throws AuthenticationNotFoundException if the authentication session is not found
     * @throws UnexpectedJwtTypeException      if the token is not a refresh token
     */
    public DecodedJwt verifyRefreshToken(EncodedToken encodedToken) {
        logger.debug("[JWT_VERIFIER] Verifying refresh token");

        try {
            var decodedJwt = decodeToken(encodedToken);
            verifyRefreshTokenType(decodedJwt);
            checkRefreshTokenInActiveStore(encodedToken);

            return decodedJwt;

        } catch (UnexpectedJwtTypeException | AuthenticationNotFoundException e) {
            throw e;
        } catch (Exception e) {
            var message = String.format("Refresh token is not valid: %s", encodedToken.token());
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
        logger.trace("[JWT_VERIFIER] Step 1/3: Decoding and validating token");
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
        logger.trace("[JWT_VERIFIER] Step 2/3: Verifying token type is ACCESS");
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
        logger.trace("[JWT_VERIFIER] Step 3/3: Checking access token exists in store");
        var isTokenActive = jwtStore.accessTokenExists(encodedToken);
        if (!isTokenActive) {
            throw new AuthenticationNotFoundException(String.format("Authentication session not found in store for access token: %s", encodedToken.token()));
        }
    }

    /**
     * Verifies the token type is a refresh token.
     * <p>
     * Verification step 2: Checks the token_use claim to ensure this is a refresh token and not an access token.
     *
     * @param decodedJwt the decoded JWT to verify
     * @throws UnexpectedJwtTypeException if the token is not a refresh token
     */
    private void verifyRefreshTokenType(DecodedJwt decodedJwt) {
        logger.trace("[JWT_VERIFIER] Step 2/3: Verifying token type is REFRESH");
        if (!decodedJwt.isRefreshToken()) {
            throw new UnexpectedJwtTypeException(String.format("JWT %s is not a refresh token", decodedJwt.encodedToken()));
        }
    }

    /**
     * Checks the refresh token exists in the active token store.
     * <p>
     * Verification step 3: Queries the fast-access store (Redis) to verify the token has not been revoked and the authentication session is still active.
     *
     * @param encodedToken the encoded token to check
     * @throws AuthenticationNotFoundException if the authentication session is not found (token revoked or expired)
     */
    private void checkRefreshTokenInActiveStore(EncodedToken encodedToken) {
        logger.trace("[JWT_VERIFIER] Step 3/3: Checking refresh token exists in store");
        var isTokenActive = jwtStore.refreshTokenExists(encodedToken);
        if (!isTokenActive) {
            throw new AuthenticationNotFoundException(String.format("Authentication session not found in store for refresh token: %s", encodedToken.token()));
        }
    }

}
