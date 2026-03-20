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

package com.bcn.asapp.authentication.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link EncodedPassword} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank encoded password values</li>
 * <li>Validates password must start with encoding identifier like {bcrypt}</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped encoded password value</li>
 */
class EncodedPasswordTests {

    @Nested
    class CreateEncodedPasswordWithConstructor {

        @Test
        void ReturnsEncodedPassword_ValidPassword() {
            // Given
            var password = "{noop}password";

            // When
            var actual = new EncodedPassword(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankPassword(String password) {
            // When
            var actual = catchThrowable(() -> new EncodedPassword(password));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_PasswordNotEncoded() {
            // When
            var actual = catchThrowable(() -> new EncodedPassword("password_not_encoded"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must start with an encoding id like {bcrypt}");
        }

    }

    @Nested
    class CreateEncodedPasswordWithFactoryMethod {

        @Test
        void ReturnsEncodedPassword_ValidPassword() {
            // Given
            var password = "{noop}password";

            // When
            var actual = EncodedPassword.of(password);

            // Then
            assertThat(actual.password()).isEqualTo(password);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankPassword(String password) {
            // When
            var actual = catchThrowable(() -> EncodedPassword.of(password));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_PasswordNotEncoded() {
            // When
            var actual = catchThrowable(() -> EncodedPassword.of("password_not_encoded"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must start with an encoding id like {bcrypt}");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsPasswordValue_ValidPassword() {
            // Given
            var passwordValue = "{noop}password";
            var password = EncodedPassword.of(passwordValue);

            // When
            var actual = password.value();

            // Then
            assertThat(actual).isEqualTo(passwordValue);
        }

    }

}
