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

package com.bcn.asapp.uaa.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RawPasswordTests {

    private final String passwordValue = "password";

    @Nested
    class CreateRawPasswordWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNullOrEmpty(String password) {
            // When
            var thrown = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Raw password must not be null or empty");
        }

        /**
         * <li>"1234" = 4 chars - too short
         * <li>"1234567" = 7 chars - just below a minimum
         */
        @ParameterizedTest
        @ValueSource(strings = { "1234", "1234567" })
        void ThenThrowsIllegalArgumentException_GivenPasswordIsTooShort(String password) {
            // When
            var thrown = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

        /**
         * <li>"12345678901234567890123456789012345678901234567890123456789012345" = 65 chars - just above maximum
         * <li>"123456789012345678901234567890123456789012345678901234567890123456789" = 69 chars - too long
         */
        @ParameterizedTest
        @ValueSource(strings = { "12345678901234567890123456789012345678901234567890123456789012345",
                "123456789012345678901234567890123456789012345678901234567890123456789" })
        void ThenThrowsIllegalArgumentException_GivenPasswordIsTooLong(String password) {
            // When
            var thrown = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

        /**
         * <li>"12345678" = 8 chars - minimum valid
         * <li>"1234567890123456789012345678901234567890123456789012345678901234" = 64 chars - maximum valid
         */
        @ParameterizedTest
        @ValueSource(strings = { "12345678", "1234567890123456789012345678901234567890123456789012345678901234" })
        void ThenReturnsRawPassword_GivenPasswordIsAtBoundary(String password) {
            // When
            var actual = new RawPassword(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        @Test
        void ThenReturnsRawPassword_GivenPasswordIsValid() {
            // When
            var actual = new RawPassword(passwordValue);

            // Then
            assertThat(actual.password()).isEqualTo(passwordValue);
        }

    }

    @Nested
    class CreateRawPasswordWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNullOrEmpty(String password) {
            // When
            var thrown = catchThrowable(() -> RawPassword.of(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Raw password must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "pass", "long_password_123456789123456789123456789123456789123456789123456879" })
        void ThenThrowsIllegalArgumentException_GivenPasswordLengthIsNotValid(String password) {
            // When
            var thrown = catchThrowable(() -> RawPassword.of(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

        @Test
        void ThenReturnsRawPassword_GivenPasswordIsValid() {
            // When
            var actual = RawPassword.of(passwordValue);

            // Then
            assertThat(actual.password()).isEqualTo(passwordValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsPasswordValue_GivenPasswordIsValid() {
            // Given
            var password = RawPassword.of(passwordValue);

            // When
            var actual = password.value();

            // Then
            assertThat(actual).isEqualTo(passwordValue);
        }

    }

}
