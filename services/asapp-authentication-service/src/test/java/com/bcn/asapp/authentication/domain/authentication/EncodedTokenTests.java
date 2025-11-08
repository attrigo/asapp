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

package com.bcn.asapp.authentication.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class EncodedTokenTests {

    private final String tokenValue = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYWRtaW4ifQ.c2lnbmF0dXJlVGVzdA";

    @Nested
    class CreateEncodedTokenWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTokenIsNullOrEmpty(String token) {
            // When
            var thrown = catchThrowable(() -> new EncodedToken(token));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null or empty");
        }

        @Test
        void ThenReturnsEncodedToken_GivenTokenIsValid() {
            // When
            var actual = new EncodedToken(tokenValue);

            // Then
            assertThat(actual.token()).isEqualTo(tokenValue);
        }

    }

    @Nested
    class CreateEncodedTokenWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTokenIsNullOrEmpty(String token) {
            // When
            var thrown = catchThrowable(() -> EncodedToken.of(token));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null or empty");
        }

        @Test
        void ThenReturnsEncodedToken_GivenTokenIsValid() {
            // When
            var actual = EncodedToken.of(tokenValue);

            // Then
            assertThat(actual.token()).isEqualTo(tokenValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsEncodedTokenValue_GivenTokenIsValid() {
            // Given
            var encodedToken = EncodedToken.of(tokenValue);

            // When
            var actual = encodedToken.value();

            // Then
            assertThat(actual).isEqualTo(tokenValue);
        }

    }

}
