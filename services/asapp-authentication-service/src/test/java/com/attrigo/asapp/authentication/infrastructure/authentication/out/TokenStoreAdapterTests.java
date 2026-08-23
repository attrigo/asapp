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

import static com.attrigo.asapp.authentication.testutil.fixture.JwtMother.aJwtBuilder;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtPairMother.aJwtPair;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtPairMother.aJwtPairBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.infrastructure.security.RedisJwtStore;
import com.attrigo.asapp.authentication.infrastructure.security.TokenEntry;
import com.attrigo.asapp.authentication.infrastructure.security.TokenKey;

/**
 * Tests {@link TokenStoreAdapter} token value unwrapping, time-to-live conversion and Redis failure translation.
 * <p>
 * Coverage:
 * <li>Pairs each token with its own time-to-live before storing</li>
 * <li>Derives each token's time-to-live from its expiration timestamp</li>
 * <li>Falls back to a one-second time-to-live for tokens already expired</li>
 * <li>Unwraps encoded token values before deleting from the Redis store</li>
 * <li>Surfaces a null token pair as a null pointer exception during storage</li>
 * <li>Translates Redis failures during storage to a token store exception</li>
 * <li>Surfaces a null token pair as a null pointer exception during deletion</li>
 * <li>Translates Redis failures during deletion to a token store exception</li>
 */
@ExtendWith(MockitoExtension.class)
class TokenStoreAdapterTests {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 300L;

    private static final long REFRESH_TOKEN_TTL_SECONDS = 900L;

    @Mock
    private RedisJwtStore redisJwtStore;

    @InjectMocks
    private TokenStoreAdapter tokenStoreAdapter;

    @Nested
    class Save {

        @Test
        void PersistsTokensWithTtlFromExpiration_ValidJwtPair() {
            // Given
            var now = Instant.now();
            var accessToken = aJwtBuilder().accessToken()
                                           .withExpiration(now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS))
                                           .build();
            var refreshToken = aJwtBuilder().refreshToken()
                                            .withExpiration(now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS))
                                            .build();
            var jwtPair = aJwtPairBuilder().withTokens(accessToken, refreshToken)
                                           .build();
            var accessTokenEntryCaptor = ArgumentCaptor.forClass(TokenEntry.class);
            var refreshTokenEntryCaptor = ArgumentCaptor.forClass(TokenEntry.class);

            // When
            tokenStoreAdapter.save(jwtPair);

            // Then
            then(redisJwtStore).should()
                               .save(accessTokenEntryCaptor.capture(), refreshTokenEntryCaptor.capture());
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenEntryCaptor.getValue().key()).as("access token key").isEqualTo(TokenKey.of(accessToken));
                softly.assertThat(accessTokenEntryCaptor.getValue().ttlSeconds()).as("access token time-to-live").isBetween(ACCESS_TOKEN_TTL_SECONDS - 5L, ACCESS_TOKEN_TTL_SECONDS);
                softly.assertThat(refreshTokenEntryCaptor.getValue().key()).as("refresh token key").isEqualTo(TokenKey.of(refreshToken));
                softly.assertThat(refreshTokenEntryCaptor.getValue().ttlSeconds()).as("refresh token time-to-live").isBetween(REFRESH_TOKEN_TTL_SECONDS - 5L, REFRESH_TOKEN_TTL_SECONDS);
                // @formatter:on
            });
        }

        @Test
        void PersistsTokensWithMinimumTtl_ExpiredJwtPair() {
            // Given
            var jwtPair = aJwtPairBuilder().expired()
                                           .build();
            var accessTokenEntryCaptor = ArgumentCaptor.forClass(TokenEntry.class);
            var refreshTokenEntryCaptor = ArgumentCaptor.forClass(TokenEntry.class);

            // When
            tokenStoreAdapter.save(jwtPair);

            // Then
            then(redisJwtStore).should()
                               .save(accessTokenEntryCaptor.capture(), refreshTokenEntryCaptor.capture());
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenEntryCaptor.getValue().ttl()).as("access token time-to-live").isEqualTo(Duration.ofSeconds(1L));
                softly.assertThat(refreshTokenEntryCaptor.getValue().ttl()).as("refresh token time-to-live").isEqualTo(Duration.ofSeconds(1L));
                // @formatter:on
            });
        }

        @Test
        void ThrowsNullPointerException_NullJwtPair() {
            // When
            var actual = catchThrowable(() -> tokenStoreAdapter.save(null));

            // Then
            assertThat(actual).isInstanceOf(NullPointerException.class)
                              .isNotInstanceOf(TokenStoreException.class);
            then(redisJwtStore).shouldHaveNoInteractions();
        }

        @Test
        void ThrowsTokenStoreException_RedisOperationFails() {
            // Given
            var jwtPair = aJwtPair();

            willThrow(new RuntimeException("Redis connection failed")).given(redisJwtStore)
                                                                      .save(any(), any());

            // When
            var actual = catchThrowable(() -> tokenStoreAdapter.save(jwtPair));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not store tokens in fast-access store");
        }

    }

    @Nested
    class Delete {

        @Test
        void DeletesTokens_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();

            // When
            tokenStoreAdapter.delete(jwtPair);

            // Then
            then(redisJwtStore).should()
                               .delete(TokenKey.of(jwtPair.accessToken()), TokenKey.of(jwtPair.refreshToken()));
        }

        @Test
        void ThrowsNullPointerException_NullJwtPair() {
            // When
            var actual = catchThrowable(() -> tokenStoreAdapter.delete(null));

            // Then
            assertThat(actual).isInstanceOf(NullPointerException.class)
                              .isNotInstanceOf(TokenStoreException.class);
            then(redisJwtStore).shouldHaveNoInteractions();
        }

        @Test
        void ThrowsTokenStoreException_RedisOperationFails() {
            // Given
            var jwtPair = aJwtPair();

            willThrow(new RuntimeException("Redis connection failed")).given(redisJwtStore)
                                                                      .delete(any(), any());

            // When
            var actual = catchThrowable(() -> tokenStoreAdapter.delete(jwtPair));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not delete tokens from fast-access store");
        }

    }

}
