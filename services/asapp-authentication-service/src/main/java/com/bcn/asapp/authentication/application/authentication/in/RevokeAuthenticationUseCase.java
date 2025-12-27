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

package com.bcn.asapp.authentication.application.authentication.in;

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.PersistenceException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;

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
     * @throws IllegalArgumentException         if the access token is invalid or blank
     * @throws UnexpectedJwtTypeException       if the provided token is not an access token
     * @throws AuthenticationNotFoundException  if the token is not found in active sessions or repository
     * @throws PersistenceException             if authentication deletion fails (after compensation)
     * @throws CompensatingTransactionException if compensating transaction fails
     */
    void revokeAuthentication(String accessToken);

}
