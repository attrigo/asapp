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

import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtPairStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.infrastructure.authentication.out.RedisJwtPairStore.REFRESH_TOKEN_PREFIX;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.defaultTestDomainJwtAuthentication;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@SpringBootTest
@Import(TestContainerConfiguration.class)
class RedisJwtPairStoreIT {

    private static final long EXPIRING_SOON_SECONDS = 5L;

    private static final long OVERWRITE_WAIT_MILLIS = 2000L;

    @Autowired
    private RedisJwtPairStore redisJwtPairStore;

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
    class Save {

        @Test
        void StoresJwtPairInRedis_JwtPairIsValid() {
            // Given
            var jwtPair = defaultTestDomainJwtAuthentication().getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtPairStore.save(jwtPair);

            // Then
            var actualAccessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var actualRefreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
            var actualAccessTokenKeyValue = redisTemplate.opsForValue()
                                                         .get(accessTokenKey);
            var actualRefreshTokenKeyValue = redisTemplate.opsForValue()
                                                          .get(refreshTokenKey);
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenKeyExists).isTrue();
                assertThat(actualRefreshTokenKeyExists).isTrue();
                assertThat(actualAccessTokenKeyValue).isEmpty();
                assertThat(actualRefreshTokenKeyValue).isEmpty();
            });
        }

        @Test
        void StoresJwtPairWithCorrectTtl_JwtPairIsValid() {
            // Given
            var jwtPair = defaultTestDomainJwtAuthentication().getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtPairStore.save(jwtPair);

            // Then
            var actualAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var actualRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // TTL range explanation:
            // - Maximum (300s): Token issued now expires in 5 minutes (300 seconds)
            // - Minimum (25s): Token issued 4.5 minutes ago has ~30 seconds remaining minus execution overhead
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenTtl).isBetween(25L, 300L);
                assertThat(actualRefreshTokenTtl).isBetween(25L, 300L);
            });
        }

        @Test
        void StoresJwtPairWithMinimumTtl_TokenIsExpiringSoon() {
            // Given
            var now = java.time.Instant.now();
            var expiringSoon = now.plusSeconds(EXPIRING_SOON_SECONDS);
            var jwtPair = testJwtAuthenticationBuilder().withAccessTokenExpiration(expiringSoon)
                                                        .withRefreshTokenExpiration(expiringSoon)
                                                        .buildDomainEntity()
                                                        .getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            // When
            redisJwtPairStore.save(jwtPair);

            // Then
            var actualAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var actualRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // Verify Math.max(ttl, 1), tokens expiring very soon get minimum TTL of 1 second
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenTtl).isGreaterThanOrEqualTo(1L);
                assertThat(actualRefreshTokenTtl).isGreaterThanOrEqualTo(1L);
            });
        }

        @Test
        void OverwritesExistingTokens_StoreSameJwtPair() throws InterruptedException {
            // Given
            var jwtPair = defaultTestDomainJwtAuthentication().getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            redisJwtPairStore.save(jwtPair);
            assertThat(redisTemplate.hasKey(accessTokenKey)).isTrue();
            assertThat(redisTemplate.hasKey(refreshTokenKey)).isTrue();
            var initialAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var initialRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            // Wait to allow TTL to decrease naturally over time
            // (necessary to verify that re-saving recalculates TTL correctly based on expiration timestamp)
            Thread.sleep(OVERWRITE_WAIT_MILLIS);

            // When
            redisJwtPairStore.save(jwtPair);

            // Then
            var actualAccessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var actualRefreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);
            var actualAccessTokenTtl = redisTemplate.getExpire(accessTokenKey, TimeUnit.SECONDS);
            var actualRefreshTokenTtl = redisTemplate.getExpire(refreshTokenKey, TimeUnit.SECONDS);

            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenKeyExists).isTrue();
                assertThat(actualRefreshTokenKeyExists).isTrue();
                assertThat(actualAccessTokenTtl).isLessThanOrEqualTo(initialAccessTokenTtl);
                assertThat(actualRefreshTokenTtl).isLessThanOrEqualTo(initialRefreshTokenTtl);
                assertThat(actualAccessTokenTtl).isGreaterThan(0L);
                assertThat(actualRefreshTokenTtl).isGreaterThan(0L);
            });
        }

    }

    @Nested
    class Delete {

        @Test
        void DeletesJwtPairFromRedis_JwtPairExists() {
            // Given
            var jwtPair = defaultTestDomainJwtAuthentication().getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            redisJwtPairStore.save(jwtPair);
            assertThat(redisTemplate.hasKey(accessTokenKey)).isTrue();
            assertThat(redisTemplate.hasKey(refreshTokenKey)).isTrue();

            // When
            redisJwtPairStore.delete(jwtPair);

            // Then
            var actualAccessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var actualRefreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);

            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenKeyExists).isFalse();
                assertThat(actualRefreshTokenKeyExists).isFalse();
            });
        }

        @Test
        void DoesNotThrowException_DeletingNonExistentJwtPair() {
            // Given
            var jwtPair = defaultTestDomainJwtAuthentication().getJwtPair();
            var accessTokenKey = buildAccessTokenKey(jwtPair);
            var refreshTokenKey = buildRefreshTokenKey(jwtPair);

            assertThat(redisTemplate.hasKey(accessTokenKey)).isFalse();
            assertThat(redisTemplate.hasKey(refreshTokenKey)).isFalse();

            // When
            redisJwtPairStore.delete(jwtPair);

            // Then
            var actualAccessTokenKeyExists = redisTemplate.hasKey(accessTokenKey);
            var actualRefreshTokenKeyExists = redisTemplate.hasKey(refreshTokenKey);

            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualAccessTokenKeyExists).isFalse();
                assertThat(actualRefreshTokenKeyExists).isFalse();
            });
        }

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
