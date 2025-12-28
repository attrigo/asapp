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

import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Redis-based adapter implementation of {@link JwtStore}.
 * <p>
 * Bridges the application layer with the infrastructure layer by storing JWTs in Redis with automatic expiration based on token TTL (time-to-live). Tokens are
 * stored as Redis keys with empty values, as only their presence is validated during token lookup operations.
 *
 * @since 0.2.0
 * @see RedisTemplate
 * @author attrigo
 */
@Component
public class RedisJwtStore implements JwtStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtStore.class);

    /**
     * Redis key prefix for access tokens.
     * <p>
     * Used to namespace access tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    public static final String ACCESS_TOKEN_PREFIX = "jwt:access_token:";

    /**
     * Redis key prefix for refresh tokens.
     * <p>
     * Used to namespace refresh tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    public static final String REFRESH_TOKEN_PREFIX = "jwt:refresh_token:";

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
     * @param accessToken the {@link EncodedToken} representing the access token to check
     * @return {@code true} if the access token exists in Redis, {@code false} otherwise
     */
    @Override
    public Boolean accessTokenExists(EncodedToken accessToken) {
        logger.trace("[REDIS_JWT_STORE] Checking access token existence");
        var key = ACCESS_TOKEN_PREFIX + accessToken.value();
        return redisTemplate.hasKey(key);
    }

    /**
     * Checks if a refresh token exists in Redis.
     * <p>
     * Verifies token presence without retrieving its value, providing fast O(1) lookup to determine if a refresh token is still valid and has not been revoked.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>EXISTS {@code jwt:refresh_token:<token>}</li>
     * </ul>
     *
     * @param refreshToken the {@link EncodedToken} representing the refresh token to check
     * @return {@code true} if the refresh token exists in Redis, {@code false} otherwise
     */
    @Override
    public Boolean refreshTokenExists(EncodedToken refreshToken) {
        logger.trace("[REDIS_JWT_STORE] Checking refresh token existence");
        var key = REFRESH_TOKEN_PREFIX + refreshToken.value();
        return redisTemplate.hasKey(key);
    }

    /**
     * Saves a JWT pair in Redis.
     * <p>
     * Both access and refresh tokens are stored atomically using Redis pipelining with automatic expiration calculated from the token's expiration timestamp.
     * Tokens are stored with empty values as only their presence needs to be validated.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>SETEX {@code jwt:access_token:<token>} with TTL from access token expiration</li>
     * <li>SETEX {@code jwt:refresh_token:<token>} with TTL from refresh token expiration</li>
     * </ul>
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to save
     * @throws TokenStoreException if the Redis operation fails
     */
    @Override
    public void save(JwtPair jwtPair) {
        logger.trace("[REDIS_JWT_STORE] Saving JWT pair");
        try {
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

        } catch (Exception e) {
            throw new TokenStoreException("Could not store tokens in fast-access store", e);
        }
    }

    /**
     * Deletes a JWT pair from Redis.
     * <p>
     * Both access and refresh tokens are removed atomically using Redis pipelining, effectively invalidating them immediately.
     * <p>
     * <strong>Redis Operations:</strong>
     * <ul>
     * <li>DEL {@code jwt:access_token:<token>}</li>
     * <li>DEL {@code jwt:refresh_token:<token>}</li>
     * </ul>
     *
     * @param jwtPair the {@link JwtPair} containing the token pair to delete
     * @throws TokenStoreException if the Redis operation fails
     */
    @Override
    public void delete(JwtPair jwtPair) {
        logger.trace("[REDIS_JWT_STORE] Deleting JWT pair");
        try {
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

        } catch (Exception e) {
            throw new TokenStoreException("Could not delete tokens from fast-access store", e);
        }
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
