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

class UsernameTests {

    private final String usernameValue = "username@asapp.com";

    @Nested
    class CreateUsernameWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNullOrEmpty(String username) {
            // When
            var thrown = catchThrowable(() -> new Username(username));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNotEmail() {
            // When
            var thrown = catchThrowable(() -> new Username("username_not_email"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must be a valid email address");
        }

        @Test
        void ThenReturnsUsername_GivenUsernameIsValid() {
            // When
            var actual = new Username(usernameValue);

            // Then
            assertThat(actual.username()).isEqualTo(usernameValue);
        }

    }

    @Nested
    class CreateUsernameWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNullOrEmpty(String username) {
            // When
            var thrown = catchThrowable(() -> Username.of(username));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNotEmail() {
            // When
            var thrown = catchThrowable(() -> Username.of("username_not_email"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must be a valid email address");
        }

        @Test
        void ThenReturnsUsername_GivenUsernameIsValid() {
            // When
            var actual = Username.of(usernameValue);

            // Then
            assertThat(actual.username()).isEqualTo(usernameValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsUsernameValue_GivenUsernameIsValid() {
            // Given
            var username = Username.of(usernameValue);

            // When
            var actual = username.value();

            // Then
            assertThat(actual).isEqualTo(usernameValue);
        }

    }

}
