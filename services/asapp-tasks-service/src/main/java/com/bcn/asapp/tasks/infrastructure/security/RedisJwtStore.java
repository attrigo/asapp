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

package com.bcn.asapp.tasks.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Infrastructure component responsible for checking JWT session validity through Redis existence.
 * <p>
 * Provides fast O(1) lookup to check token existence in Redis, enabling quick validation of whether tokens have been revoked or expired from the fast-access
 * store.
 * <p>
 * <strong>Redis Key Pattern:</strong>
 * <ul>
 * <li>Access tokens: {@code jwt:access_token:<token_value>}</li>
 * </ul>
 *
 * @since 0.2.0
 * @see RedisTemplate
 * @author attrigo
 */
@Component
public class RedisJwtStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtStore.class);

    /**
     * Redis key prefix for access tokens.
     * <p>
     * Used to namespace access tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    public static final String ACCESS_TOKEN_PREFIX = "jwt:access_token:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Constructs a new {@code RedisJwtStore} with required dependencies.
     *
     * @param redisTemplate the Spring Data Redis template for executing Redis operations
     */
    public RedisJwtStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if an access token exists in Redis.
     * <p>
     * Verifies token presence without retrieving its value, providing fast O(1) lookup to determine if an access token is still valid and has not been revoked.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>EXISTS {@code jwt:access_token:<token>}</li>
     * </ul>
     *
     * @param accessToken the access token string to check
     * @return {@code true} if the access token exists in Redis, {@code false} otherwise
     */
    public Boolean accessTokenExists(String accessToken) {
        logger.trace("[JWT_STORE] Checking if access token exists in Redis");
        var key = ACCESS_TOKEN_PREFIX + accessToken;
        return redisTemplate.hasKey(key);
    }

}
