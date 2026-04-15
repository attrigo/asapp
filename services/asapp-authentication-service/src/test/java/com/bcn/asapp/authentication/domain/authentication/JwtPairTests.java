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

package com.bcn.asapp.authentication.domain.authentication;

import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aRefreshToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.anAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JwtPair} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null access token</li>
 * <li>Rejects null refresh token</li>
 * <li>Accepts valid token pair through constructor and factory method</li>
 */
class JwtPairTests {

    @Nested
    class CreateJwtPairWithConstructor {

        @Test
        void ReturnsJwtPair_ValidAccessTokenAndRefreshToken() {
            // Given
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();

            // When
            var actual = new JwtPair(accessToken, refreshToken);

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        void ThrowsIllegalArgumentException_NullAccessToken() {
            // Given
            var refreshToken = aRefreshToken();

            // When
            var actual = catchThrowable(() -> new JwtPair(null, refreshToken));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullRefreshToken() {
            // Given
            var accessToken = anAccessToken();

            // When
            var actual = catchThrowable(() -> new JwtPair(accessToken, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

    }

    @Nested
    class CreateJwtPairWithFactoryMethod {

        @Test
        void ReturnsJwtPair_ValidAccessTokenAndRefreshToken() {
            // Given
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();

            // When
            var actual = JwtPair.of(accessToken, refreshToken);

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        void ThrowsIllegalArgumentException_NullAccessToken() {
            // Given
            var refreshToken = aRefreshToken();

            // When
            var actual = catchThrowable(() -> JwtPair.of(null, refreshToken));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullRefreshToken() {
            // Given
            var accessToken = anAccessToken();

            // When
            var actual = catchThrowable(() -> JwtPair.of(accessToken, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

    }

}
