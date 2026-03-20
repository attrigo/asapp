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
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link LastName} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank last name values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped last name value</li>
 */
class LastNameTests {

    @Nested
    class CreateLastNameWithConstructor {

        @Test
        void ReturnsLastName_ValidLastName() {
            // Given
            var lastName = "LastName";

            // When
            var actual = new LastName(lastName);

            // Then
            assertThat(actual.lastName()).isEqualTo(lastName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankLastName(String lastName) {
            // When
            var actual = catchThrowable(() -> new LastName(lastName));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

    }

    @Nested
    class CreateLastNameWithFactoryMethod {

        @Test
        void ReturnsLastName_ValidLastName() {
            // Given
            var lastName = "LastName";

            // When
            var actual = LastName.of(lastName);

            // Then
            assertThat(actual.lastName()).isEqualTo(lastName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankLastName(String lastName) {
            // When
            var actual = catchThrowable(() -> LastName.of(lastName));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsLastNameValue_ValidLastName() {
            // Given
            var lastNameValue = "LastName";
            var lastName = LastName.of(lastNameValue);

            // When
            var actual = lastName.value();

            // Then
            assertThat(actual).isEqualTo(lastNameValue);
        }

    }

}
