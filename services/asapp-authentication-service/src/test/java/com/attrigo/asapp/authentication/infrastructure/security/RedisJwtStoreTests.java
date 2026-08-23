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

import static com.attrigo.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.attrigo.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.ACCESS_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.REFRESH_TOKEN_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import com.attrigo.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Tests {@link RedisJwtStore} token existence checks and pipelined token storage and deletion.
 * <p>
 * Coverage:
 * <li>Reports a token as present when found in the cache</li>
 * <li>Reports a token as absent when not found in the cache</li>
 * <li>Queries each token under its own prefixed key</li>
 * <li>Treats a null cache response as the token being absent</li>
 * <li>Stores both tokens in a single pipelined Redis call</li>
 * <li>Binds each prefixed token key to its own time-to-live</li>
 * <li>Deletes both tokens in a single pipelined Redis call</li>
 * <li>Removes both prefixed token keys</li>
 * <li>Writes and removes the same keys whatever order the tokens arrive in</li>
 */
@ExtendWith(MockitoExtension.class)
class RedisJwtStoreTests {

    private static final String ACCESS_TOKEN_VALUE = "access.token.value";

    private static final String REFRESH_TOKEN_VALUE = "refresh.token.value";

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(5L);

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofMinutes(15L);

    private static final byte[] EMPTY_VALUE = "".getBytes();

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisStringCommands redisStringCommands;

    @Mock
    private RedisKeyCommands redisKeyCommands;

    @InjectMocks
    private RedisJwtStore redisJwtStore;

    @Nested
    class Exists {

        @Test
        void ReturnsTrue_AccessTokenExists() {
            // Given
            given(redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE)).willReturn(true);

            // When
            var actual = redisJwtStore.exists(TokenKey.ofAccessToken(EncodedToken.of(ACCESS_TOKEN_VALUE)));

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_AccessTokenNotExists() {
            // Given
            given(redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE)).willReturn(false);

            // When
            var actual = redisJwtStore.exists(TokenKey.ofAccessToken(EncodedToken.of(ACCESS_TOKEN_VALUE)));

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void QueriesPrefixedTokenKey_AccessToken() {
            // When
            redisJwtStore.exists(TokenKey.ofAccessToken(EncodedToken.of(ACCESS_TOKEN_VALUE)));

            // Then
            then(redisTemplate).should(times(1))
                               .hasKey(ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE);
        }

        @Test
        void QueriesPrefixedTokenKey_RefreshToken() {
            // When
            redisJwtStore.exists(TokenKey.ofRefreshToken(EncodedToken.of(REFRESH_TOKEN_VALUE)));

            // Then
            then(redisTemplate).should(times(1))
                               .hasKey(REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_VALUE);
        }

        @Test
        void ReturnsFalse_CacheReturnsNull() {
            // Given
            given(redisTemplate.hasKey(ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE)).willReturn(null);

            // When
            var actual = redisJwtStore.exists(TokenKey.ofAccessToken(EncodedToken.of(ACCESS_TOKEN_VALUE)));

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class Save {

        @Test
        void PersistsTokens_ValidTokens() {
            // When
            redisJwtStore.save(accessTokenEntry(), refreshTokenEntry());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(any(RedisCallback.class));
        }

        @Test
        void PersistsEachTokenWithItsOwnTtl_DifferentTtls() {
            // Given
            var callbackCaptor = ArgumentCaptor.forClass(RedisCallback.class);

            given(redisConnection.stringCommands()).willReturn(redisStringCommands);

            // When
            redisJwtStore.save(accessTokenEntry(), refreshTokenEntry());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(callbackCaptor.capture());

            callbackCaptor.getValue()
                          .doInRedis(redisConnection);

            then(redisStringCommands).should(times(1))
                                     .setEx((ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE).getBytes(), ACCESS_TOKEN_TTL.toSeconds(), EMPTY_VALUE);
            then(redisStringCommands).should(times(1))
                                     .setEx((REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_VALUE).getBytes(), REFRESH_TOKEN_TTL.toSeconds(), EMPTY_VALUE);
        }

        @Test
        void PersistsEachTokenUnderItsOwnKey_ReversedTokenOrder() {
            // Given
            var callbackCaptor = ArgumentCaptor.forClass(RedisCallback.class);

            given(redisConnection.stringCommands()).willReturn(redisStringCommands);

            // When
            redisJwtStore.save(refreshTokenEntry(), accessTokenEntry());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(callbackCaptor.capture());

            callbackCaptor.getValue()
                          .doInRedis(redisConnection);

            then(redisStringCommands).should(times(1))
                                     .setEx((ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE).getBytes(), ACCESS_TOKEN_TTL.toSeconds(), EMPTY_VALUE);
            then(redisStringCommands).should(times(1))
                                     .setEx((REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_VALUE).getBytes(), REFRESH_TOKEN_TTL.toSeconds(), EMPTY_VALUE);
        }

    }

    @Nested
    class Delete {

        @Test
        void DeletesTokens_ValidTokens() {
            // When
            redisJwtStore.delete(accessTokenKey(), refreshTokenKey());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(any(RedisCallback.class));
        }

        @Test
        void DeletesEachPrefixedTokenKey_ValidTokens() {
            // Given
            var callbackCaptor = ArgumentCaptor.forClass(RedisCallback.class);

            given(redisConnection.keyCommands()).willReturn(redisKeyCommands);

            // When
            redisJwtStore.delete(accessTokenKey(), refreshTokenKey());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(callbackCaptor.capture());

            callbackCaptor.getValue()
                          .doInRedis(redisConnection);

            then(redisKeyCommands).should(times(1))
                                  .del((ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE).getBytes());
            then(redisKeyCommands).should(times(1))
                                  .del((REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_VALUE).getBytes());
        }

        @Test
        void DeletesEachPrefixedTokenKey_ReversedTokenOrder() {
            // Given
            var callbackCaptor = ArgumentCaptor.forClass(RedisCallback.class);

            given(redisConnection.keyCommands()).willReturn(redisKeyCommands);

            // When
            redisJwtStore.delete(refreshTokenKey(), accessTokenKey());

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(callbackCaptor.capture());

            callbackCaptor.getValue()
                          .doInRedis(redisConnection);

            then(redisKeyCommands).should(times(1))
                                  .del((ACCESS_TOKEN_PREFIX + ACCESS_TOKEN_VALUE).getBytes());
            then(redisKeyCommands).should(times(1))
                                  .del((REFRESH_TOKEN_PREFIX + REFRESH_TOKEN_VALUE).getBytes());
        }

    }

    private static TokenEntry accessTokenEntry() {
        return new TokenEntry(accessTokenKey(), ACCESS_TOKEN_TTL);
    }

    private static TokenEntry refreshTokenEntry() {
        return new TokenEntry(refreshTokenKey(), REFRESH_TOKEN_TTL);
    }

    private static TokenKey accessTokenKey() {
        return new TokenKey(ACCESS_TOKEN, EncodedToken.of(ACCESS_TOKEN_VALUE));
    }

    private static TokenKey refreshTokenKey() {
        return new TokenKey(REFRESH_TOKEN, EncodedToken.of(REFRESH_TOKEN_VALUE));
    }

}
