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

class EncodedPasswordTests {

    private final String passwordValue = "{noop}password";

    @Nested
    class CreateEncodedPasswordWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNullOrEmpty(String password) {
            // When
            var thrown = catchThrowable(() -> new EncodedPassword(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNotEncoded() {
            // When
            var thrown = catchThrowable(() -> new EncodedPassword("not_encoded_password"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must start with an encoding id like {bcrypt}");
        }

        @Test
        void ThenReturnsEncodedPassword_GivenPasswordIsValid() {
            // When
            var actual = new EncodedPassword(passwordValue);

            // Then
            assertThat(actual.password()).isEqualTo(passwordValue);
        }

    }

    @Nested
    class CreateEncodedPasswordWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNullOrEmpty(String password) {
            // When
            var thrown = catchThrowable(() -> EncodedPassword.of(password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNotEncoded() {
            // When
            var thrown = catchThrowable(() -> EncodedPassword.of("not_encoded_password"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded password must start with an encoding id like {bcrypt}");
        }

        @Test
        void ThenReturnsEncodedPassword_GivenPasswordIsValid() {
            // When
            var actual = EncodedPassword.of(passwordValue);

            // Then
            assertThat(actual.password()).isEqualTo(passwordValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsPasswordValue_GivenPasswordIsValid() {
            // Given
            var password = EncodedPassword.of(passwordValue);

            // When
            var actual = password.value();

            // Then
            assertThat(actual).isEqualTo(passwordValue);
        }

    }

}
