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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtCacheRepository;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.security.util.TokenHasher;

/**
 * Redis implementation of {@link JwtCacheRepository}.
 * <p>
 * Stores JWT tokens in Redis with automatic expiration based on token TTL. Tokens are hashed using SHA-256 before storage for security. Only token existence is
 * tracked - the cache acts as an active token registry.
 *
 * @since 0.3.0
 * @author attrigo
 */
@Component
public class RedisJwtCacheRepository implements JwtCacheRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtCacheRepository.class);

    private static final String ACCESS_TOKEN_PREFIX = "jwt:access:";

    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";

    private static final String ACTIVE_TOKEN_MARKER = "1";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Constructs a new {@code RedisJwtCacheRepository}.
     *
     * @param redisTemplate the Redis template for cache operations
     */
    public RedisJwtCacheRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeTokenPair(Jwt accessToken, Jwt refreshToken, UserId userId) {
        logger.debug("Storing token pair for user {}", userId.value());

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            var accessKey = buildKey(ACCESS_TOKEN_PREFIX, accessToken.encodedTokenValue());
            var accessTtl = calculateTtl(accessToken.expiration()
                                                    .value());

            connection.stringCommands()
                      .setEx(accessKey.getBytes(), accessTtl, ACTIVE_TOKEN_MARKER.getBytes());

            var refreshKey = buildKey(REFRESH_TOKEN_PREFIX, refreshToken.encodedTokenValue());
            var refreshTtl = calculateTtl(refreshToken.expiration()
                                                      .value());

            connection.stringCommands()
                      .setEx(refreshKey.getBytes(), refreshTtl, ACTIVE_TOKEN_MARKER.getBytes());

            return null;
        });

        logger.debug("Token pair stored successfully for user {}", userId.value());
    }

    @Override
    public Boolean tokenExists(String token) {
        logger.trace("Checking if token exists in cache");

        var accessKey = buildKey(ACCESS_TOKEN_PREFIX, token);
        var exists = Boolean.TRUE.equals(redisTemplate.hasKey(accessKey));

        if (!exists) {
            var refreshKey = buildKey(REFRESH_TOKEN_PREFIX, token);
            exists = Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey));
        }

        logger.trace("Token {} in cache", exists ? "found" : "not found");
        return exists;
    }

    @Override
    public void deleteTokenPair(Jwt accessToken, Jwt refreshToken) {
        logger.debug("Deleting token pair from cache");

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            var accessKey = buildKey(ACCESS_TOKEN_PREFIX, accessToken.encodedTokenValue());
            connection.keyCommands()
                      .del(accessKey.getBytes());

            var refreshKey = buildKey(REFRESH_TOKEN_PREFIX, refreshToken.encodedTokenValue());
            connection.keyCommands()
                      .del(refreshKey.getBytes());

            return null;
        });

        logger.debug("Token pair deleted from cache");
    }

    /**
     * Builds Redis key by hashing token with SHA-256.
     *
     * @param prefix the key prefix (access or refresh)
     * @param token  the raw token string
     * @return the Redis key (prefix + hash)
     */
    private String buildKey(String prefix, String token) {
        var hash = TokenHasher.hash(token);
        return prefix + hash;
    }

    /**
     * Calculates TTL in seconds from expiration timestamp.
     *
     * @param expiration the expiration timestamp
     * @return TTL in seconds (minimum 1)
     */
    private long calculateTtl(Instant expiration) {
        var ttl = Duration.between(Instant.now(), expiration)
                          .getSeconds();
        return Math.max(ttl, 1); // Minimum 1 second
    }

}
