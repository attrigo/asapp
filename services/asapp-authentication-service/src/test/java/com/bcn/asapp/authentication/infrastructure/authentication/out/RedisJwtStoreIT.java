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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.JwtPairFactory.aJwtPair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link RedisJwtStore} token activation, existence checks, and TTL expiration against Redis.
 * <p>
 * Coverage:
 * <li>Activates token pairs in Redis with calculated TTL from expiration timestamps</li>
 * <li>Verifies token existence checks return correct status</li>
 * <li>Deactivates token pairs removing them from Redis</li>
 * <li>Tests actual Redis operations with TestContainers</li>
 */
@SpringBootTest
@Import(TestContainerConfiguration.class)
class RedisJwtStoreIT {

    private static final long EXPIRING_SOON_SECONDS = 5L;

    @Autowired
    private RedisJwtStore redisJwtStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void beforeEach() {
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        redisTemplate.getConnectionFactory()
                     .getConnection()
                     .serverCommands()
                     .flushDb();
    }

    @Nested
    class AccessTokenExists {

        @Test
        void ReturnsTrue_AccessTokenExists() {
            // Given
            var jwtPair = createJwtPair();
            var accessToken = jwtPair.accessToken()
                                     .encodedToken();

            // When
            var actual = redisJwtStore.accessTokenExists(accessToken);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_AccessTokenNotExists() {
            // Given
            var accessToken = aJwtPair().accessToken()
                                        .encodedToken();

            // When
            var actual = redisJwtStore.accessTokenExists(accessToken);

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class RefreshTokenExists {

        @Test
        void ReturnsTrue_RefreshTokenExists() {
            // Given
            var jwtPair = createJwtPair();
            var refreshToken = jwtPair.refreshToken()
                                      .encodedToken();

            // When
            var actual = redisJwtStore.refreshTokenExists(refreshToken);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_RefreshTokenNotExists() {
            // Given
            var refreshToken = aJwtPair().refreshToken()
                                         .encodedToken();

            // When
            var actual = redisJwtStore.refreshTokenExists(refreshToken);

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class Save {

        @Test
        void StoresJwtPair_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            var accessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var refreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
            var accessTokenKeyValue = redisTemplate.opsForValue()
                                                   .get(accessTokenKey);
            var refreshTokenKeyValue = redisTemplate.opsForValue()
                                                    .get(refreshTokenKey);
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenKeyExists).as("access token key exists").isTrue();
                softly.assertThat(refreshTokenKeyExists).as("refresh token key exists").isTrue();
                softly.assertThat(accessTokenKeyValue).as("access token key value").isEmpty();
                softly.assertThat(refreshTokenKeyValue).as("refresh token key value").isEmpty();
                // @formatter:on
            });
        }

        @Test
        void StoresJwtPairWithCorrectTtl_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            var accessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var refreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);
            // TTL range explanation:
            // - Maximum (300s): Token issued now expires in 5 minutes (300 seconds)
            // - Minimum (25s): Token issued 4.5 minutes ago has ~30 seconds remaining minus execution overhead
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenTtl).as("access token TTL").isBetween(25L, 300L);
                softly.assertThat(refreshTokenTtl).as("refresh token TTL").isBetween(25L, 300L);
                // @formatter:on
            });
        }

        @Test
        void StoresJwtPairWithMinimumTtl_TokenExpiringSoon() {
            // Given
            var now = Instant.now();
            var expiration = now.plusSeconds(EXPIRING_SOON_SECONDS);
            var jwtPair = aJwtAuthenticationBuilder().withAccessTokenExpiration(expiration)
                                                     .withRefreshTokenExpiration(expiration)
                                                     .build()
                                                     .getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            var accessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var refreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);
            // Verify Math.max(ttl, 1), tokens expiring very soon get minimum TTL of 1 second
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenTtl).as("access token TTL").isGreaterThanOrEqualTo(1L);
                softly.assertThat(refreshTokenTtl).as("refresh token TTL").isGreaterThanOrEqualTo(1L);
                // @formatter:on
            });
        }

        @Test
        void OverwritesExistingTokens_StoreSameJwtPair() {
            // Given
            var jwtPair = createJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);
            var initialAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var initialRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // Wait to allow TTL to decrease naturally over time
            // This ensures that when we save the same JWT pair again, the TTLs are updated to new values rather than remaining unchanged.
            await().atMost(5, TimeUnit.SECONDS)
                   .until(() -> redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS) < initialAccessTokenTtl);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            var accessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var refreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
            var accessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var refreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenKeyExists).as("access token key exists").isTrue();
                softly.assertThat(refreshTokenKeyExists).as("refresh token key exists").isTrue();
                softly.assertThat(accessTokenTtl).as("access token TTL").isLessThan(initialAccessTokenTtl);
                softly.assertThat(refreshTokenTtl).as("refresh token TTL").isLessThan(initialRefreshTokenTtl);
                softly.assertThat(accessTokenTtl).as("access token TTL positive").isGreaterThan(0L);
                softly.assertThat(refreshTokenTtl).as("refresh token TTL positive").isGreaterThan(0L);
                // @formatter:on
            });
        }

        @Test
        void DeletesAccessToken_AccessTokenExpiredInRedis() {
            // Given
            var now = Instant.now();
            var expiration = now.plusSeconds(1L);
            var jwtPair = aJwtAuthenticationBuilder().withAccessTokenExpiration(expiration)
                                                     .withRefreshTokenExpiration(expiration)
                                                     .build()
                                                     .getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            assertThat(redisTemplate.hasKey(accessTokenKey)).isTrue();
            await().atMost(3, TimeUnit.SECONDS)
                   .until(() -> Boolean.FALSE.equals(redisTemplate.hasKey(accessTokenKey)));
        }

        @Test
        void DeletesRefreshToken_RefreshTokenExpiredInRedis() {
            // Given
            var now = Instant.now();
            var expiration = now.plusSeconds(1L);
            var jwtPair = aJwtAuthenticationBuilder().withAccessTokenExpiration(expiration)
                                                     .withRefreshTokenExpiration(expiration)
                                                     .build()
                                                     .getJwtPair();
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtStore.save(jwtPair);

            // Then
            assertThat(redisTemplate.hasKey(refreshTokenKey)).isTrue();
            await().atMost(3, TimeUnit.SECONDS)
                   .until(() -> Boolean.FALSE.equals(redisTemplate.hasKey(refreshTokenKey)));
        }

    }

    @Nested
    class Delete {

        @Test
        void DeletesJwtPair_JwtPairExists() {
            // Given
            var jwtPair = createJwtPair();

            // When
            redisJwtStore.delete(jwtPair);

            // Then
            assertJwtPairNotExistInRedis(jwtPair);
        }

        @Test
        void CompletesWithoutErrors_JwtPairNotExists() {
            // Given
            var jwtPair = aJwtPair();

            // When
            redisJwtStore.delete(jwtPair);

            // Then
            assertJwtPairNotExistInRedis(jwtPair);
        }

    }

    // Test Data Creation Helpers

    private JwtPair createJwtPair() {
        var jwtPair = aJwtPair();
        redisJwtStore.save(jwtPair);
        assertThat(redisTemplate.hasKey(buildAccessTokenKey(jwtPair))).isTrue();
        assertThat(redisTemplate.hasKey(buildRefreshTokenKey(jwtPair))).isTrue();
        return jwtPair;
    }

    // Assertions Helpers

    private void assertJwtPairNotExistInRedis(JwtPair jwtPair) {
        var accessTokenKey = buildAccessTokenKey(jwtPair);
        var refreshTokenKey = buildRefreshTokenKey(jwtPair);
        var accessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
        var refreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
        assertSoftly(softly -> {
            // @formatter:off
            softly.assertThat(accessTokenKeyExists).as("access token key exists").isFalse();
            softly.assertThat(refreshTokenKeyExists).as("refresh token key exists").isFalse();
            // @formatter:on
        });
    }

    private String buildAccessTokenKey(JwtPair jwtPair) {
        return ACCESS_TOKEN_PREFIX + jwtPair.accessToken()
                                            .encodedTokenValue();
    }

    private String buildRefreshTokenKey(JwtPair jwtPair) {
        return REFRESH_TOKEN_PREFIX + jwtPair.refreshToken()
                                             .encodedTokenValue();
    }

}
