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

import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTypeTests {

    @Nested
    class GetType {

        @Test
        void ThenReturnsAtJwt_GivenAccessToken() {
            // When
            var actual = ACCESS_TOKEN.type();

            // Then
            assertThat(actual).isEqualTo("at+jwt");
        }

        @Test
        void ThenReturnsRtJwt_GivenRefreshToken() {
            // When
            var actual = REFRESH_TOKEN.type();

            // Then
            assertThat(actual).isEqualTo("rt+jwt");
        }

        @ParameterizedTest
        @EnumSource(JwtType.class)
        void ThenReturnsNonNull_GivenAllJwtTypes(JwtType jwtType) {
            // When
            var actual = jwtType.type();

            // Then
            assertThat(actual).isNotNull()
                              .isNotEmpty();
        }

    }

    @Nested
    class OfType {

        @Test
        void ThenReturnsAccessToken_GivenAtJwt() {
            // When
            var actual = JwtType.ofType("at+jwt");

            // Then
            assertThat(actual).isEqualTo(ACCESS_TOKEN);
        }

        @Test
        void ThenReturnsRefreshToken_GivenRtJwt() {
            // When
            var actual = JwtType.ofType("rt+jwt");

            // Then
            assertThat(actual).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTypeIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtType.ofType(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Invalid JWT type: null");
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = { "invalid", "jwt", "AT+JWT", "RT+JWT", "access_token", "refresh_token", "at jwt", "rt jwt" })
        void ThrowsIllegalArgumentException_TypeIsInvalid(String invalidType) {
            // When
            var thrown = catchThrowable(() -> JwtType.ofType(invalidType));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageStartingWith("Invalid JWT type: ")
                              .hasMessageContaining(invalidType);
        }

    }

}
