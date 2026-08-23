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

package com.attrigo.asapp.authentication.infrastructure.security;

import java.time.Duration;

import org.springframework.util.Assert;

import com.attrigo.asapp.authentication.domain.authentication.Jwt;

/**
 * Represents a token store entry: a token and the time-to-live it must be stored under.
 * <p>
 * Binds each token to its own time-to-live so a pair of tokens cannot be stored under each other's lifetime.
 *
 * @param key the key the token is stored under
 * @param ttl the time-to-live the token must be stored under
 * @since 0.5.0
 * @author attrigo
 */
public record TokenEntry(
        TokenKey key,
        Duration ttl
) {

    /**
     * Constructs a new {@code TokenEntry} instance and validates its integrity.
     * <p>
     * Validates that all required fields are present and that the time-to-live is positive, as the store rejects a non-positive one.
     *
     * @param key the key the token is stored under
     * @param ttl the time-to-live the token must be stored under
     * @throws IllegalArgumentException if any validation fails
     */
    public TokenEntry {
        Assert.notNull(key, "Token key must not be null");
        Assert.notNull(ttl, "Time-to-live must not be null");
        Assert.isTrue(ttl.isPositive(), "Time-to-live must be positive");
    }

    /**
     * Factory method to create a new {@code TokenEntry} instance from a JWT.
     *
     * @param jwt the JWT to store
     * @param ttl the time-to-live the token must be stored under
     * @return a new {@code TokenEntry} instance
     * @throws IllegalArgumentException if any validation fails
     */
    public static TokenEntry of(Jwt jwt, Duration ttl) {
        return new TokenEntry(TokenKey.of(jwt), ttl);
    }

    /**
     * Returns the time-to-live in seconds, the unit the store expects.
     *
     * @return the time-to-live in seconds
     */
    public Long ttlSeconds() {
        return this.ttl.toSeconds();
    }

}
