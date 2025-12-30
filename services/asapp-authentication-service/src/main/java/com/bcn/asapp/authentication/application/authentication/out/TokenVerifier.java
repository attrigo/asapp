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

package com.bcn.asapp.authentication.application.authentication.out;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.InvalidJwtException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Port for verifying authentication tokens.
 * <p>
 * Defines the contract for performing three-step token verification: cryptographic validation, token type verification, and session validation.
 * <p>
 * Verification is performed in three steps:
 * <ol>
 * <li>Decodes and validates the token's signature and expiration</li>
 * <li>Verifies the token type matches the expected type (access or refresh token)</li>
 * <li>Checks the token exists in the active session store</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TokenVerifier {

    /**
     * Verifies an access token.
     * <p>
     * Performs comprehensive verification including signature validation, expiration check, token type verification, and session validation.
     *
     * @param encodedToken the {@link EncodedToken} to verify
     * @throws InvalidJwtException             if the token is invalid, malformed, expired, or signature verification fails
     * @throws UnexpectedJwtTypeException      if the token is not an access token
     * @throws AuthenticationNotFoundException if the authentication session is not found
     */
    void verifyAccessToken(EncodedToken encodedToken);

    /**
     * Verifies a refresh token.
     * <p>
     * Performs comprehensive verification including signature validation, expiration check, token type verification, and session validation.
     *
     * @param encodedToken the {@link EncodedToken} to verify
     * @throws InvalidJwtException             if the token is invalid, malformed, expired, or signature verification fails
     * @throws UnexpectedJwtTypeException      if the token is not a refresh token
     * @throws AuthenticationNotFoundException if the authentication session is not found
     */
    void verifyRefreshToken(EncodedToken encodedToken);

}
