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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link EncodedToken} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank token values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped token value</li>
 */
class EncodedTokenTests {

    @Nested
    class CreateEncodedTokenWithConstructor {

        @Test
        void ReturnsEncodedToken_ValidToken() {
            // Given
            var token = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYWRtaW4ifQ.c2lnbmF0dXJlVGVzdA";

            // When
            var actual = new EncodedToken(token);

            // Then
            assertThat(actual.token()).isEqualTo(token);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsInvalidEncodedTokenException_NullOrBlankToken(String token) {
            // When
            var actual = catchThrowable(() -> new EncodedToken(token));

            // Then
            assertThat(actual).isInstanceOf(InvalidEncodedTokenException.class)
                              .hasMessage("Encoded token must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "notajwt", "only.two", "too.many.dots.here", ".empty.header", "empty..sig" })
        void ThrowsInvalidEncodedTokenException_InvalidJwtFormat(String token) {
            // When
            var actual = catchThrowable(() -> new EncodedToken(token));

            // Then
            assertThat(actual).isInstanceOf(InvalidEncodedTokenException.class)
                              .hasMessage("Encoded token must be a valid JWT format");
        }

    }

    @Nested
    class CreateEncodedTokenWithFactoryMethod {

        @Test
        void ReturnsEncodedToken_ValidToken() {
            // Given
            var token = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYWRtaW4ifQ.c2lnbmF0dXJlVGVzdA";

            // When
            var actual = EncodedToken.of(token);

            // Then
            assertThat(actual.token()).isEqualTo(token);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsInvalidEncodedTokenException_NullOrBlankToken(String token) {
            // When
            var actual = catchThrowable(() -> EncodedToken.of(token));

            // Then
            assertThat(actual).isInstanceOf(InvalidEncodedTokenException.class)
                              .hasMessage("Encoded token must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "notajwt", "only.two", "too.many.dots.here", ".empty.header", "empty..sig" })
        void ThrowsInvalidEncodedTokenException_InvalidJwtFormat(String token) {
            // When
            var actual = catchThrowable(() -> EncodedToken.of(token));

            // Then
            assertThat(actual).isInstanceOf(InvalidEncodedTokenException.class)
                              .hasMessage("Encoded token must be a valid JWT format");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsEncodedTokenValue_ValidToken() {
            // Given
            var tokenValue = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYWRtaW4ifQ.c2lnbmF0dXJlVGVzdA";
            var encodedToken = EncodedToken.of(tokenValue);

            // When
            var actual = encodedToken.value();

            // Then
            assertThat(actual).isEqualTo(tokenValue);
        }

    }

}
