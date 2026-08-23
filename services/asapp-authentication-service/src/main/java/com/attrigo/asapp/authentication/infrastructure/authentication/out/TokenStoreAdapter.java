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

package com.attrigo.asapp.authentication.infrastructure.authentication.out;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.application.authentication.out.TokenStore;
import com.attrigo.asapp.authentication.domain.authentication.Expiration;
import com.attrigo.asapp.authentication.domain.authentication.JwtPair;
import com.attrigo.asapp.authentication.infrastructure.security.RedisJwtStore;
import com.attrigo.asapp.authentication.infrastructure.security.TokenEntry;
import com.attrigo.asapp.authentication.infrastructure.security.TokenKey;

/**
 * Adapter implementation of {@link TokenStore} for Redis-backed token storage.
 * <p>
 * Bridges the application layer with the infrastructure layer by mapping domain tokens to {@link RedisJwtStore} entries and translating its failures to
 * {@link TokenStoreException}.
 * <p>
 * This adapter performs type translation by converting each token's {@link Expiration} timestamp into the time-to-live the store expects, flooring it at one
 * second so a token on the verge of expiry is still stored.
 *
 * @since 0.5.0
 * @author attrigo
 */
@Component
public class TokenStoreAdapter implements TokenStore {

    private static final long MINIMUM_TTL_SECONDS = 1L;

    private final RedisJwtStore redisJwtStore;

    /**
     * Constructs a new {@code TokenStoreAdapter} with required dependencies.
     *
     * @param redisJwtStore the Redis JWT store for executing token store operations
     */
    public TokenStoreAdapter(RedisJwtStore redisJwtStore) {
        this.redisJwtStore = redisJwtStore;
    }

    @Override
    public void save(JwtPair jwtPair) {
        var accessToken = jwtPair.accessToken();
        var refreshToken = jwtPair.refreshToken();

        var accessTokenTtl = calculateTtl(accessToken.expiration());
        var refreshTokenTtl = calculateTtl(refreshToken.expiration());

        var accessTokenEntry = TokenEntry.of(accessToken, accessTokenTtl);
        var refreshTokenEntry = TokenEntry.of(refreshToken, refreshTokenTtl);

        try {
            redisJwtStore.save(accessTokenEntry, refreshTokenEntry);
        } catch (Exception e) {
            throw new TokenStoreException("Could not store tokens in fast-access store", e);
        }
    }

    @Override
    public void delete(JwtPair jwtPair) {
        var accessTokenKey = TokenKey.of(jwtPair.accessToken());
        var refreshTokenKey = TokenKey.of(jwtPair.refreshToken());

        try {
            redisJwtStore.delete(accessTokenKey, refreshTokenKey);
        } catch (Exception e) {
            throw new TokenStoreException("Could not delete tokens from fast-access store", e);
        }
    }

    /**
     * Calculates the time-to-live for a token based on its expiration.
     * <p>
     * The time-to-live is the duration between now and the token's expiration timestamp, floored at one second so a token expiring imminently is still stored.
     *
     * @param expiration the token's expiration timestamp
     * @return the time-to-live, at least one second
     */
    private Duration calculateTtl(Expiration expiration) {
        var ttlSeconds = Duration.between(Instant.now(), expiration.value())
                                 .toSeconds();
        return Duration.ofSeconds(Math.max(ttlSeconds, MINIMUM_TTL_SECONDS));
    }

}
