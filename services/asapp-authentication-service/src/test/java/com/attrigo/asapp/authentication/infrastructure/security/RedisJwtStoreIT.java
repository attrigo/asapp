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

import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.ACCESS_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.REFRESH_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtPairMother.aJwtPair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import com.attrigo.asapp.authentication.domain.authentication.JwtPair;
import com.attrigo.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link RedisJwtStore} token storage, existence checks, and time-to-live expiration against Redis.
 * <p>
 * Setup:
 * <li>Loads the full application context backed by a Testcontainers PostgreSQL instance and an embedded Redis</li>
 * <li>Flushes the token store before each test</li>
 * <p>
 * Coverage:
 * <li>Stores tokens in Redis, each with its own supplied time-to-live</li>
 * <li>Verifies token existence checks return correct status</li>
 * <li>Removes stored tokens from Redis</li>
 * <li>Drops tokens from Redis once their time-to-live elapses</li>
 */
@SpringBootTest
@Import(TestContainerConfiguration.class)
class RedisJwtStoreIT {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5L);

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(5L);

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofMinutes(15L);

    private static final Duration EXPIRING_SOON_TTL = Duration.ofSeconds(1L);

    // Seconds the TTL must visibly drop by before re-saving, so the refreshed TTL is unambiguously higher than the decreased one
    private static final long TTL_DROP_SECONDS = 3L;

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
    class Exists {

        @Test
        void ReturnsTrue_AccessTokenExists() {
            // Given
            var jwtPair = createJwtPairInRedis();
            var accessToken = jwtPair.accessToken()
                                     .encodedToken();

            // When
            var actual = redisJwtStore.exists(TokenKey.ofAccessToken(accessToken));

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsTrue_RefreshTokenExists() {
            // Given
            var jwtPair = createJwtPairInRedis();
            var refreshToken = jwtPair.refreshToken()
                                      .encodedToken();

            // When
            var actual = redisJwtStore.exists(TokenKey.ofRefreshToken(refreshToken));

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_AccessTokenNotExists() {
            // Given
            var accessToken = aJwtPair().accessToken()
                                        .encodedToken();

            // When
            var actual = redisJwtStore.exists(TokenKey.ofAccessToken(accessToken));

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_RefreshTokenNotExists() {
            // Given
            var refreshToken = aJwtPair().refreshToken()
                                         .encodedToken();

            // When
            var actual = redisJwtStore.exists(TokenKey.ofRefreshToken(refreshToken));

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
            saveInRedis(jwtPair, TOKEN_TTL);

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
        void StoresJwtPairWithGivenTtl_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            saveInRedis(jwtPair, ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL);

            // Then
            var accessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var refreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);
            // Each token gets its own distinct TTL, so transposing the two durations inside the store fails this test
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenTtl).as("access token TTL").isBetween(ACCESS_TOKEN_TTL.toSeconds() - 5L, ACCESS_TOKEN_TTL.toSeconds());
                softly.assertThat(refreshTokenTtl).as("refresh token TTL").isBetween(REFRESH_TOKEN_TTL.toSeconds() - 5L, REFRESH_TOKEN_TTL.toSeconds());
                // @formatter:on
            });
        }

        @Test
        void OverwritesExistingTokens_StoreSameJwtPair() {
            // Given
            var jwtPair = createJwtPairInRedis();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);
            var initialAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var initialRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // Wait for both TTLs to tick down by several seconds, so a refreshed TTL stays clear of the decreased one even
            // if the re-save and the reads below are delayed by a pause of about a second
            await().atMost(10, TimeUnit.SECONDS)
                   .until(() -> initialAccessTokenTtl - redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS) >= TTL_DROP_SECONDS
                           && initialRefreshTokenTtl - redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS) >= TTL_DROP_SECONDS);
            var decreasedAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var decreasedRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // When
            saveInRedis(jwtPair, TOKEN_TTL);

            // Then
            var accessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var refreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
            var accessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var refreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(accessTokenKeyExists).as("access token key exists").isTrue();
                softly.assertThat(refreshTokenKeyExists).as("refresh token key exists").isTrue();
                softly.assertThat(accessTokenTtl).as("access token TTL").isGreaterThan(decreasedAccessTokenTtl);
                softly.assertThat(refreshTokenTtl).as("refresh token TTL").isGreaterThan(decreasedRefreshTokenTtl);
                // @formatter:on
            });
        }

        @Test
        void DeletesAccessToken_AccessTokenExpiredInRedis() {
            // Given
            var jwtPair = aJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);

            // When
            saveInRedis(jwtPair, EXPIRING_SOON_TTL);

            // Then
            assertThat(redisTemplate.hasKey(accessTokenKey)).isTrue();
            await().atMost(3, TimeUnit.SECONDS)
                   .until(() -> Boolean.FALSE.equals(redisTemplate.hasKey(accessTokenKey)));
        }

        @Test
        void DeletesRefreshToken_RefreshTokenExpiredInRedis() {
            // Given
            var jwtPair = aJwtPair();
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            saveInRedis(jwtPair, EXPIRING_SOON_TTL);

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
            var jwtPair = createJwtPairInRedis();

            // When
            redisJwtStore.delete(TokenKey.of(jwtPair.accessToken()), TokenKey.of(jwtPair.refreshToken()));

            // Then
            assertJwtPairNotExistInRedis(jwtPair);
        }

        @Test
        void CompletesWithoutErrors_JwtPairNotExists() {
            // Given
            var jwtPair = aJwtPair();

            // When
            redisJwtStore.delete(TokenKey.of(jwtPair.accessToken()), TokenKey.of(jwtPair.refreshToken()));

            // Then
            assertJwtPairNotExistInRedis(jwtPair);
        }

    }

    // Test Data Creation Helpers

    private JwtPair createJwtPairInRedis() {
        var jwtPair = aJwtPair();
        saveInRedis(jwtPair, TOKEN_TTL);
        assertThat(redisTemplate.hasKey(buildAccessTokenKey(jwtPair))).isTrue();
        assertThat(redisTemplate.hasKey(buildRefreshTokenKey(jwtPair))).isTrue();
        return jwtPair;
    }

    private void saveInRedis(JwtPair jwtPair, Duration ttl) {
        saveInRedis(jwtPair, ttl, ttl);
    }

    private void saveInRedis(JwtPair jwtPair, Duration accessTtl, Duration refreshTtl) {
        var accessTokenEntry = TokenEntry.of(jwtPair.accessToken(), accessTtl);
        var refreshTokenEntry = TokenEntry.of(jwtPair.refreshToken(), refreshTtl);
        redisJwtStore.save(accessTokenEntry, refreshTokenEntry);
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
