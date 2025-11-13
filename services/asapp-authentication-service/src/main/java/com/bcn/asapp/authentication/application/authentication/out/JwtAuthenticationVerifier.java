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

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;

/**
 * Port for verifying and validating JWT tokens for authentication use cases.
 * <p>
 * Defines the contract for validating JWT tokens and retrieving their associated authentications.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface JwtAuthenticationVerifier {

    /**
     * Verifies an access token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication.
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link JwtAuthentication} associated with the access token
     */
    JwtAuthentication verifyAccessToken(EncodedToken accessToken);

    /**
     * Verifies a refresh token and retrieves its associated authentication.
     * <p>
     * Validates the token's signature, expiration, and type, then returns the corresponding authentication.
     *
     * @param refreshToken the encoded refresh token to verify
     * @return the {@link JwtAuthentication} associated with the refresh token
     */
    JwtAuthentication verifyRefreshToken(EncodedToken refreshToken);

}
