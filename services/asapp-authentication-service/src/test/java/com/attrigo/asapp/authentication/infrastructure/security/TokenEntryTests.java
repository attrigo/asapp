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
import static com.attrigo.asapp.authentication.testutil.fixture.JwtMother.anAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.attrigo.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Tests {@link TokenEntry} construction and validation.
 * <p>
 * Coverage:
 * <li>Validates token key and time-to-live required at construction</li>
 * <li>Rejects a non-positive time-to-live, the store's own constraint</li>
 * <li>Takes the token key from the JWT it is created from</li>
 * <li>Provides immutable access to the token and its time-to-live</li>
 * <li>Exposes the time-to-live in the seconds unit the store expects</li>
 */
class TokenEntryTests {

    private static final String TOKEN_VALUE = "access.token.value";

    private static final Duration TTL = Duration.ofMinutes(5L);

    @Nested
    class CreateTokenEntryWithConstructor {

        @Test
        void ReturnsTokenEntry_ValidParameters() {
            // Given
            var tokenKey = new TokenKey(ACCESS_TOKEN, EncodedToken.of(TOKEN_VALUE));

            // When
            var actual = new TokenEntry(tokenKey, TTL);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("expiring token").isNotNull();
                softly.assertThat(actual.key()).as("token key").isEqualTo(tokenKey);
                softly.assertThat(actual.ttl()).as("time-to-live").isEqualTo(TTL);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullTokenKey() {
            // When
            var actual = catchThrowable(() -> new TokenEntry(null, TTL));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Token key must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTimeToLive() {
            // Given
            var tokenKey = new TokenKey(ACCESS_TOKEN, EncodedToken.of(TOKEN_VALUE));

            // When
            var actual = catchThrowable(() -> new TokenEntry(tokenKey, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Time-to-live must not be null");
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, -1L })
        void ThrowsIllegalArgumentException_NonPositiveTimeToLive(Long ttlSeconds) {
            // Given
            var tokenKey = new TokenKey(ACCESS_TOKEN, EncodedToken.of(TOKEN_VALUE));

            // When
            var actual = catchThrowable(() -> new TokenEntry(tokenKey, Duration.ofSeconds(ttlSeconds)));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Time-to-live must be positive");
        }

    }

    @Nested
    class CreateTokenEntryWithFactoryMethod {

        @Test
        void ReturnsTokenEntry_ValidParameters() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = TokenEntry.of(jwt, TTL);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("expiring token").isNotNull();
                softly.assertThat(actual.key()).as("token key").isEqualTo(TokenKey.of(jwt));
                softly.assertThat(actual.ttl()).as("time-to-live").isEqualTo(TTL);
                // @formatter:on
            });
        }

    }

    @Nested
    class GetTtlSeconds {

        @Test
        void ReturnsTimeToLiveInSeconds_ValidTimeToLive() {
            // Given
            var tokenEntry = new TokenEntry(new TokenKey(ACCESS_TOKEN, EncodedToken.of(TOKEN_VALUE)), TTL);

            // When
            var actual = tokenEntry.ttlSeconds();

            // Then
            assertThat(actual).isEqualTo(300L);
        }

    }

}
