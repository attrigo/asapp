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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Infrastructure component for storing JWTs in Redis for fast lookup.
 * <p>
 * Stores tokens as Redis keys with empty values and a time-to-live supplied by the caller, as only their presence is validated during token lookup operations.
 * <p>
 * Each token resolves its own key, so the order a pair is handed to a pipelined operation does not affect what is written or removed.
 *
 * @since 0.2.0
 * @see RedisTemplate
 * @author attrigo
 */
@Component
public class RedisJwtStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtStore.class);

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
     * Checks if a token exists in Redis.
     * <p>
     * Resolves in O(1) without retrieving the token value.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>EXISTS {@code jwt:<type>:<token>}</li>
     * </ul>
     *
     * @param tokenKey the key of the token to check
     * @return {@code true} if the token exists in Redis, {@code false} otherwise, with a {@code null} cache response treated as absent
     */
    public boolean exists(TokenKey tokenKey) {
        logger.trace("[REDIS_JWT_STORE] Checking token existence");
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey.value()));
    }

    /**
     * Stores the given tokens in Redis, each under its own key and time-to-live.
     * <p>
     * All tokens are stored atomically using Redis pipelining, with empty values as only their presence needs to be validated.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>SETEX {@code jwt:<type>:<token>} with the given token time-to-live, once per entry</li>
     * </ul>
     *
     * @param tokenEntries the tokens to store, each with the time-to-live to store it under; storing none is a no-op
     */
    public void save(TokenEntry... tokenEntries) {
        logger.trace("[REDIS_JWT_STORE] Saving JWT pair");
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands redisStringCommands = connection.stringCommands();

            for (var tokenEntry : tokenEntries) {
                var tokenKeyValue = tokenEntry.key()
                                              .value();
                redisStringCommands.setEx(tokenKeyValue.getBytes(), tokenEntry.ttlSeconds(), "".getBytes());
            }

            return null;
        });
    }

    /**
     * Deletes the given tokens from Redis.
     * <p>
     * Performed atomically using Redis pipelining.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>DEL {@code jwt:<type>:<token>}, once per token</li>
     * </ul>
     *
     * @param tokenKeys the keys of the tokens to delete; deleting none is a no-op
     */
    public void delete(TokenKey... tokenKeys) {
        logger.trace("[REDIS_JWT_STORE] Deleting JWT pair");
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisKeyCommands redisKeyCommands = connection.keyCommands();

            for (var tokenKey : tokenKeys) {
                var tokenKeyValue = tokenKey.value();
                redisKeyCommands.del(tokenKeyValue.getBytes());
            }

            return null;
        });
    }

}
