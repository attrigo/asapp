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

package com.attrigo.asapp.authentication.application.authentication.out;

import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.domain.authentication.JwtPair;

/**
 * Port for storing JWTs for fast lookup.
 * <p>
 * Defines the contract for temporarily storing JWTs to enable fast token validation and revocation.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TokenStore {

    /**
     * Saves a JWT pair in the fast-access store.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to save
     * @throws TokenStoreException if the store operation fails
     */
    void save(JwtPair jwtPair);

    /**
     * Deletes a JWT pair from the fast-access store.
     * <p>
     * Removes both access and refresh tokens to invalidate them immediately.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to delete
     * @throws TokenStoreException if the store operation fails
     */
    void delete(JwtPair jwtPair);

}
