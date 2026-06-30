/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.attrigo.asapp.authentication.application.authentication.in;

import com.attrigo.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.attrigo.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.attrigo.asapp.authentication.application.authentication.InvalidJwtException;
import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;

/**
 * Use case for revoking authentications in the system.
 * <p>
 * Defines the contract for authentication revocation operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface RevokeAuthenticationUseCase {

    /**
     * Revokes an authentication using a valid access token.
     *
     * @param accessToken the access token string
     * @throws InvalidEncodedTokenException       if the access token is null, blank, or not a valid JWT format
     * @throws InvalidJwtException                if the token is malformed, expired, or fails signature verification
     * @throws UnexpectedJwtTypeException         if the provided token is not an access token
     * @throws AuthenticationNotFoundException    if the token is not found in active sessions or repository
     * @throws AuthenticationPersistenceException if authentication deletion fails
     * @throws TokenStoreException                if token deactivation fails
     */
    void revokeAuthentication(String accessToken);

}
