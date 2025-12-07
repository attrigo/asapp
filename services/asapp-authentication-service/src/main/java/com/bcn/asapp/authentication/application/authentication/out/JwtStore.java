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
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Port for storing JWT tokens for fast lookup.
 * <p>
 * Defines the contract for temporarily storing JWT tokens to enable fast token validation and revocation.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface JwtStore {

    /**
     * Checks if an access token exists in the fast-access store.
     * <p>
     * Used to verify if an access token is still valid and has not been revoked.
     *
     * @param accessToken the {@link EncodedToken} representing the access token to check
     * @return {@code true} if the access token exists in the store, {@code false} otherwise
     */
    Boolean accessTokenExists(EncodedToken accessToken);

    /**
     * Checks if a refresh token exists in the fast-access store.
     * <p>
     * Used to verify if a refresh token is still valid and has not been revoked.
     *
     * @param refreshToken the {@link EncodedToken} representing the refresh token to check
     * @return {@code true} if the refresh token exists in the store, {@code false} otherwise
     */
    Boolean refreshTokenExists(EncodedToken refreshToken);

    /**
     * Saves a JWT token pair in the fast-access store.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to save
     */
    void save(JwtPair jwtPair);

    /**
     * Deletes a JWT token pair from the fast-access store.
     * <p>
     * Removes both access and refresh tokens to invalidate them immediately.
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to delete
     */
    void delete(JwtPair jwtPair);

}
