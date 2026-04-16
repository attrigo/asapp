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

package com.bcn.asapp.tasks.infrastructure.security;

import static com.bcn.asapp.tasks.infrastructure.security.RedisJwtStore.ACCESS_TOKEN_PREFIX;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests {@link RedisJwtStore} token existence checks against Redis.
 * <p>
 * Coverage:
 * <li>Verifies token existence check returns false when token not present in Redis</li>
 * <li>Verifies token existence check returns true when token stored in Redis</li>
 * <li>Tests actual Redis operations with TestContainers</li>
 */
@SpringBootTest
@Import(TestContainerConfiguration.class)
class RedisJwtStoreIT {

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
            var accessToken = createAccessToken();

            // When
            var actual = redisJwtStore.accessTokenExists(accessToken);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_AccessTokenNotExists() {
            // Given
            var encodedAccessToken = encodedAccessToken();

            // When
            var actual = redisJwtStore.accessTokenExists(encodedAccessToken);

            // Then
            assertThat(actual).isFalse();
        }

    }

    // Test Data Creation Helpers

    private String createAccessToken() {
        var encodedAccessToken = encodedAccessToken();
        var accessTokenKey = ACCESS_TOKEN_PREFIX + encodedAccessToken;
        redisTemplate.opsForValue()
                     .set(accessTokenKey, "");
        assertThat(redisTemplate.hasKey(accessTokenKey)).isTrue();
        return encodedAccessToken;
    }

}
