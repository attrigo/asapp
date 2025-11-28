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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtPairStore;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Redis-based adapter implementation of {@link JwtPairStore}.
 * <p>
 * Stores JWT token pairs in Redis with automatic expiration based on token TTL (time-to-live). Tokens are stored as Redis keys with empty values, as only their
 * presence is validated during token lookup operations.
 *
 * @since 0.2.0
 * @see RedisTemplate
 * @author attrigo
 */
@Component
public class RedisJwtPairStore implements JwtPairStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtPairStore.class);

    /**
     * Redis key prefix for access tokens.
     * <p>
     * Used to namespace access tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    private static final String ACCESS_TOKEN_PREFIX = "jwt:access:";

    /**
     * Redis key prefix for refresh tokens.
     * <p>
     * Used to namespace refresh tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Constructs a new {@code RedisJwtPairStore} with required dependencies.
     *
     * @param redisTemplate the Spring Data Redis template for executing Redis operations
     */
    public RedisJwtPairStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Saves a JWT token pair in Redis.
     * <p>
     * Both access and refresh tokens are stored atomically using Redis pipelining with automatic expiration calculated from the token's expiration timestamp.
     * Tokens are stored with empty values as only their presence needs to be validated.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>SETEX {@code jwt:access:<token>} with TTL from access token expiration</li>
     * <li>SETEX {@code jwt:refresh:<token>} with TTL from refresh token expiration</li>
     * </ul>
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to save
     * @throws RuntimeException if the Redis operation fails
     */
    @Override
    public void save(JwtPair jwtPair) {
        logger.trace("Saving JWT pair");

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands redisStringCommands = connection.stringCommands();

            var accessToken = jwtPair.accessToken();
            var refreshToken = jwtPair.refreshToken();

            var accessKey = ACCESS_TOKEN_PREFIX + accessToken.encodedTokenValue();
            var accessTtl = calculateTtl(accessToken.expiration());
            redisStringCommands.setEx(accessKey.getBytes(), accessTtl, "".getBytes());

            var refreshKey = REFRESH_TOKEN_PREFIX + refreshToken.encodedTokenValue();
            var refreshTtl = calculateTtl(refreshToken.expiration());
            redisStringCommands.setEx(refreshKey.getBytes(), refreshTtl, "".getBytes());

            return null;
        });

        logger.trace("JWT pair stored successfully");
    }

    /**
     * Deletes a JWT token pair from Redis.
     * <p>
     * Both access and refresh tokens are removed atomically using Redis pipelining, effectively invalidating them immediately.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>DEL {@code jwt:access:<token>}</li>
     * <li>DEL {@code jwt:refresh:<token>}</li>
     * </ul>
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to delete
     * @throws RuntimeException if the Redis operation fails
     */
    @Override
    public void delete(JwtPair jwtPair) {
        logger.trace("Deleting JWT pair");

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisKeyCommands redisKeyCommands = connection.keyCommands();

            var accessToken = jwtPair.accessToken();
            var refreshToken = jwtPair.refreshToken();

            var accessKey = ACCESS_TOKEN_PREFIX + accessToken.encodedTokenValue();
            redisKeyCommands.del(accessKey.getBytes());

            var refreshKey = REFRESH_TOKEN_PREFIX + refreshToken.encodedTokenValue();
            redisKeyCommands.del(refreshKey.getBytes());

            return null;
        });

        logger.trace("JWT pair deleted successfully");
    }

    /**
     * Calculates the time-to-live (TTL) in seconds for a token based on its expiration.
     * <p>
     * The TTL is calculated as the duration between now and the token's expiration timestamp. If the calculated TTL is less than 1 second, it returns 1 to
     * ensure the token is stored for at least a minimal duration.
     *
     * @param expiration the token's expiration timestamp
     * @return the TTL in seconds, minimum value of 1
     */
    private long calculateTtl(Expiration expiration) {
        var ttl = Duration.between(Instant.now(), expiration.value())
                          .getSeconds();
        return Math.max(ttl, 1);
    }

}
