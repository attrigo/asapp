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

import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Repository port for JWT token caching operations.
 * <p>
 * Defines the contract for storing and checking JWT token existence in a distributed cache. The cache acts as an active token registry - only non-revoked
 * tokens exist in the cache.
 *
 * @since 0.3.0
 * @author attrigo
 */
public interface JwtCacheRepository {

    /**
     * Stores both access and refresh tokens in the cache atomically.
     * <p>
     * TTL is calculated from token expiration claims.
     *
     * @param accessToken  the access token to store
     * @param refreshToken the refresh token to store
     * @param userId       the user ID associated with the tokens
     */
    void storeTokenPair(Jwt accessToken, Jwt refreshToken, UserId userId);

    /**
     * Checks if a token exists in the cache.
     * <p>
     * Token is hashed before lookup. Returns true if token is active (not revoked/expired).
     *
     * @param token the raw token string
     * @return true if token exists in cache, false otherwise
     */
    Boolean tokenExists(String token);

    /**
     * Deletes both access and refresh tokens from the cache atomically.
     *
     * @param accessToken  the access token to delete
     * @param refreshToken the refresh token to delete
     */
    void deleteTokenPair(Jwt accessToken, Jwt refreshToken);

}
