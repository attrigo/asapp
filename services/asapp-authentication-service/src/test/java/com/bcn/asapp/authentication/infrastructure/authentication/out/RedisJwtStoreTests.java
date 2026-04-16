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

import static com.bcn.asapp.authentication.testutil.fixture.JwtPairMother.aJwtPair;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import com.bcn.asapp.authentication.application.authentication.TokenStoreException;

/**
 * Tests {@link RedisJwtStore} token existence checks and exception translation on Redis operation failures.
 * <p>
 * Coverage:
 * <li>Persists token pair via pipelined store operation</li>
 * <li>Translates Redis failures during token pair storage to store exception</li>
 * <li>Deletes token pair via pipelined delete operation</li>
 * <li>Translates Redis failures during token pair deletion to store exception</li>
 */
@ExtendWith(MockitoExtension.class)
class RedisJwtStoreTests {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private RedisJwtStore redisJwtStore;

    @Nested
    class Save {

        @Test
        void PersistsTokenPair_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();

            // When
            redisJwtStore.save(jwtPair);

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(any(RedisCallback.class));
        }

        @Test
        void ThrowsTokenStoreException_StoreOperationFails() {
            // Given
            var jwtPair = aJwtPair();

            willThrow(new RuntimeException("Redis connection failed")).given(redisTemplate)
                                                                      .executePipelined(any(RedisCallback.class));

            // When
            var actual = catchThrowable(() -> redisJwtStore.save(jwtPair));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not store tokens in fast-access store");
        }

    }

    @Nested
    class Delete {

        @Test
        void DeletesTokenPair_ValidJwtPair() {
            // Given
            var jwtPair = aJwtPair();

            // When
            redisJwtStore.delete(jwtPair);

            // Then
            then(redisTemplate).should(times(1))
                               .executePipelined(any(RedisCallback.class));
        }

        @Test
        void ThrowsTokenStoreException_DeleteOperationFails() {
            // Given
            var jwtPair = aJwtPair();

            willThrow(new RuntimeException("Redis connection failed")).given(redisTemplate)
                                                                      .executePipelined(any(RedisCallback.class));

            // When
            var actual = catchThrowable(() -> redisJwtStore.delete(jwtPair));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not delete tokens from fast-access store");
        }

    }

}
