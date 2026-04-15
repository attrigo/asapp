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

package com.bcn.asapp.authentication.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link RawPassword} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank password values</li>
 * <li>Validates password length must be between 8 and 64 characters</li>
 * <li>Accepts passwords at boundary lengths (8 and 64 characters)</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped password value</li>
 */
class RawPasswordTests {

    @Nested
    class CreateRawPasswordWithConstructor {

        @Test
        void ReturnsRawPassword_ValidPassword() {
            // Given
            var password = "TEST@09_password?!";

            // When
            var actual = new RawPassword(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        /**
         * <li>"12345678" = 8 chars - minimum valid
         * <li>"1234567890123456789012345678901234567890123456789012345678901234" = 64 chars - maximum valid
         */
        @ParameterizedTest
        @ValueSource(strings = { "12345678", "1234567890123456789012345678901234567890123456789012345678901234" })
        void ReturnsRawPassword_PasswordAtBoundary(String password) {
            // When
            var actual = new RawPassword(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsInvalidPasswordException_NullOrBlankPassword(String password) {
            // When
            var actual = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPasswordException.class)
                              .hasMessage("Raw password must not be null or empty");
        }

        /**
         * <li>"1234" = 4 chars - too short
         * <li>"1234567" = 7 chars - just below a minimum
         */
        @ParameterizedTest
        @ValueSource(strings = { "1234", "1234567" })
        void ThrowsInvalidPasswordException_PasswordTooShort(String password) {
            // When
            var actual = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPasswordException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

        /**
         * <li>"12345678901234567890123456789012345678901234567890123456789012345" = 65 chars - just above maximum
         * <li>"123456789012345678901234567890123456789012345678901234567890123456789" = 69 chars - too long
         */
        @ParameterizedTest
        @ValueSource(strings = { "12345678901234567890123456789012345678901234567890123456789012345",
                "123456789012345678901234567890123456789012345678901234567890123456789" })
        void ThrowsInvalidPasswordException_PasswordTooLong(String password) {
            // When
            var actual = catchThrowable(() -> new RawPassword(password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPasswordException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

    }

    @Nested
    class CreateRawPasswordWithFactoryMethod {

        @Test
        void ReturnsRawPassword_ValidPassword() {
            // Given
            var password = "TEST@09_password?!";

            // When
            var actual = RawPassword.of(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsInvalidPasswordException_NullOrBlankPassword(String password) {
            // When
            var actual = catchThrowable(() -> RawPassword.of(password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPasswordException.class)
                              .hasMessage("Raw password must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "pass", "long_password_123456789123456789123456789123456789123456789123456879" })
        void ThrowsInvalidPasswordException_InvalidPasswordLength(String password) {
            // When
            var actual = catchThrowable(() -> RawPassword.of(password));

            // Then
            assertThat(actual).isInstanceOf(InvalidPasswordException.class)
                              .hasMessage("Raw password must be between 8 and 64 characters");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsPasswordValue_ValidPassword() {
            // Given
            var passwordValue = "TEST@09_password?!";
            var password = RawPassword.of(passwordValue);

            // When
            var actual = password.value();

            // Then
            assertThat(actual).isEqualTo(passwordValue);
        }

    }

}
