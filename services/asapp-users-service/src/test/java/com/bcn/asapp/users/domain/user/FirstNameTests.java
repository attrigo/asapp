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
 * Tests {@link FirstName} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank first name values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped first name value</li>
 */
class FirstNameTests {

    @Nested
    class CreateFirstNameWithConstructor {

        @Test
        void ReturnsFirstName_ValidFirstName() {
            // Given
            var firstName = "FirstName";

            // When
            var actual = new FirstName(firstName);

            // Then
            assertThat(actual.firstName()).isEqualTo(firstName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankFirstName(String firstName) {
            // When
            var actual = catchThrowable(() -> new FirstName(firstName));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

    }

    @Nested
    class CreateFirstNameWithFactoryMethod {

        @Test
        void ReturnsFirstName_ValidFirstName() {
            // Given
            var firstName = "FirstName";

            // When
            var actual = FirstName.of(firstName);

            // Then
            assertThat(actual.firstName()).isEqualTo(firstName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankFirstName(String firstName) {
            // When
            var actual = catchThrowable(() -> FirstName.of(firstName));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsFirstNameValue_ValidFirstName() {
            // Given
            var firstNameValue = "FirstName";
            var firstName = FirstName.of(firstNameValue);

            // When
            var actual = firstName.value();

            // Then
            assertThat(actual).isEqualTo(firstNameValue);
        }

    }

}
