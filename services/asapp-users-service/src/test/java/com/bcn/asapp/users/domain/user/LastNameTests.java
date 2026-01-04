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

package com.bcn.asapp.users.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class LastNameTests {

    private final String lastNameValue = "LastName";

    @Nested
    class CreateLastNameWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptyLastName(String lastName) {
            // When
            var thrown = catchThrowable(() -> new LastName(lastName));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

        @Test
        void ReturnsLastName_ValidLastName() {
            // When
            var actual = new LastName(lastNameValue);

            // Then
            assertThat(actual.lastName()).isEqualTo(lastNameValue);
        }

    }

    @Nested
    class CreateLastNameWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptyLastName(String lastName) {
            // When
            var thrown = catchThrowable(() -> LastName.of(lastName));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

        @Test
        void ReturnsLastName_ValidLastName() {
            // When
            var actual = LastName.of(lastNameValue);

            // Then
            assertThat(actual.lastName()).isEqualTo(lastNameValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsLastNameValue_ValidLastName() {
            // Given
            var lastName = LastName.of(lastNameValue);

            // When
            var actual = lastName.value();

            // Then
            assertThat(actual).isEqualTo(lastNameValue);
        }

    }

}
