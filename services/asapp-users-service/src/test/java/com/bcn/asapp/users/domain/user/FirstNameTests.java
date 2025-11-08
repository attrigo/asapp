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

class FirstNameTests {

    private final String firstNameValue = "FirstName";

    @Nested
    class CreateFirstNameWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNullOrEmpty(String firstName) {
            // When
            var thrown = catchThrowable(() -> new FirstName(firstName));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

        @Test
        void ThenReturnsFirstName_GivenFirstNameIsValid() {
            // When
            var actual = new FirstName(firstNameValue);

            // Then
            assertThat(actual.firstName()).isEqualTo(firstNameValue);
        }

    }

    @Nested
    class CreateFirstNameWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNullOrEmpty(String firstName) {
            // When
            var thrown = catchThrowable(() -> FirstName.of(firstName));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

        @Test
        void ThenReturnsFirstName_GivenFirstNameIsValid() {
            // When
            var actual = FirstName.of(firstNameValue);

            // Then
            assertThat(actual.firstName()).isEqualTo(firstNameValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsFirstNameValue_GivenFirstNameIsValid() {
            // Given
            var firstName = FirstName.of(firstNameValue);

            // When
            var actual = firstName.value();

            // Then
            assertThat(actual).isEqualTo(firstNameValue);
        }

    }

}
