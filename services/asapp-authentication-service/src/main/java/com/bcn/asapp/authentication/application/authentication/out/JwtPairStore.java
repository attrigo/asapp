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

import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Port for storing JWT token pairs for fast lookup.
 * <p>
 * Defines the contract for temporarily storing JWT token pairs to enable fast token validation and revocation.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface JwtPairStore {

    /**
     * Stores a JWT token pair in the fast-access store.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to store
     */
    void store(JwtPair jwtPair);

    /**
     * Deletes a JWT token pair from the fast-access store.
     * <p>
     * Removes both access and refresh tokens to invalidate them immediately.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to delete
     */
    void delete(JwtPair jwtPair);

}
