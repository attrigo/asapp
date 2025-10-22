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

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class PhoneNumberTests {

    private final String phoneNumberValue = "555 555 555";

    @Nested
    class CreatePhoneNumberWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNullOrEmpty(String phoneNumber) {
            // When
            var thrown = catchThrowable(() -> new PhoneNumber(phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null or empty");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.PhoneNumberTests#provideInvalidPhoneNumbers")
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNotValid(String phoneNumber) {
            // When
            var thrown = catchThrowable(() -> new PhoneNumber(phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must follow one of these pattern: 666777888, 666 777 888 or 666-777-888");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.PhoneNumberTests#provideValidPhoneNumbers")
        void ThenReturnsPhoneNumber_GivenPhoneNumberIsValid(String phoneNumber) {
            // When
            var actual = new PhoneNumber(phoneNumber);

            // Then
            assertThat(actual.phoneNumber()).isEqualTo(phoneNumber);
        }

    }

    @Nested
    class CreatePhoneNumberWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNullOrEmpty(String phoneNumber) {
            // When
            var thrown = catchThrowable(() -> PhoneNumber.of(phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null or empty");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.PhoneNumberTests#provideInvalidPhoneNumbers")
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNotValid(String phoneNumber) {
            // When
            var thrown = catchThrowable(() -> PhoneNumber.of(phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must follow one of these pattern: 666777888, 666 777 888 or 666-777-888");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.PhoneNumberTests#provideValidPhoneNumbers")
        void ThenReturnsPhoneNumber_GivenPhoneNumberIsValid(String phoneNumber) {
            // When
            var actual = PhoneNumber.of(phoneNumber);

            // Then
            assertThat(actual.phoneNumber()).isEqualTo(phoneNumber);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsPhoneNumberValue_GivenPhoneNumberIsValid() {
            // Given
            var phoneNumber = PhoneNumber.of(phoneNumberValue);

            // When
            var actual = phoneNumber.value();

            // Then
            assertThat(actual).isEqualTo(phoneNumberValue);
        }

    }

    private static Stream<String> provideValidPhoneNumbers() {
        return Stream.of(
                // No separators
                "666777888", "123456789", "999888777",

                // Space separator
                "666 777 888", "123 456 789", "999 888 777",

                // Hyphen separator
                "666-777-888", "123-456-789", "999-888-777",

                // Mixed valid patterns (first separator)
                "666 777888", "666-777888",

                // Mixed valid patterns (second separator)
                "666777 888", "666777-888");
    }

    private static Stream<String> provideInvalidPhoneNumbers() {
        return Stream.of(
                // Wrong separators
                "555.555.555", "555,555,555", "555;555;555", "555/555/555",

                // International format (not supported)
                "+30 555 555 555", "+1-555-555-555", "0034 555 555 555",

                // Wrong length - too many digits
                "555 555 555 555", "5555 5555 5555", "1234567890",

                // Wrong length - too few digits
                "55 55 55", "5 5 5", "555 555", "555", "12345678",

                // Wrong grouping
                "5555-555-55", "55-555-5555", "5555-55-555",

                // Contains letters
                "666ABC888", "666 ABC 888", "aaa bbb ccc",

                // Special characters
                "666*777*888", "666#777#888", "(666)777-888",

                // Leading/trailing spaces (should fail after strip)
                // Removing these as strip() handles them

                // Multiple consecutive separators
                "666--777--888", "666  777  888", "666- -777- -888",

                // Starting/ending with separator
                "-666-777-888", "666-777-888-", " 666 777 888", "666 777 888 ");
    }

}
