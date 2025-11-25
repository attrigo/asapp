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

import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;

/**
 * Repository port for JWT token caching operations.
 * <p>
 * Defines the contract for storing JWT token pairs in a cache layer to enable fast token validation and revocation capabilities. This port abstracts the
 * caching mechanism from the application core, allowing different cache implementations (Redis, Memcached, etc.) without affecting business logic.
 * <p>
 * Implementations should ensure that tokens are stored with appropriate TTL (time-to-live) values matching the token expiration times to prevent stale data.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface JwtCacheRepository {

    /**
     * Stores a JWT authentication token pair in the cache.
     * <p>
     * Both access and refresh tokens from the authentication object should be cached with their respective expiration times. This enables fast lookup for token
     * validation and revocation operations without querying the primary database.
     *
     * @param jwtAuthentication the {@link JwtAuthentication} containing the token pair to cache
     * @throws RuntimeException if the cache storage operation fails
     */
    void storeJwtPair(JwtAuthentication jwtAuthentication);

}
