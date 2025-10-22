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

class EmailTests {

    private final String emailValue = "user@asapp.com";

    @Nested
    class CreateEmailWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenEmailIsNullOrEmpty(String email) {
            // When
            var thrown = catchThrowable(() -> new Email(email));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null or empty");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.EmailTests#provideInvalidEmails")
        void ThenThrowsIllegalArgumentException_GivenEmailIsNotValid(String email) {
            // When
            var thrown = catchThrowable(() -> new Email(email));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must be a valid email address");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.EmailTests#provideValidEmails")
        void ThenReturnsEmail_GivenEmailIsValid(String email) {
            // When
            var actual = new Email(email);

            // Then
            assertThat(actual.email()).isEqualTo(email);
        }

    }

    @Nested
    class CreateEmailWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenEmailIsNullOrEmpty(String email) {
            // When
            var thrown = catchThrowable(() -> Email.of(email));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null or empty");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.EmailTests#provideInvalidEmails")
        void ThenThrowsIllegalArgumentException_GivenEmailIsNotValid(String email) {
            // When
            var thrown = catchThrowable(() -> Email.of(email));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must be a valid email address");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.users.domain.user.EmailTests#provideValidEmails")
        void ThenReturnsEmail_GivenEmailIsValid(String email) {
            // When
            var actual = Email.of(email);

            // Then
            assertThat(actual.email()).isEqualTo(email);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsEmailValue_GivenEmailIsValid() {
            // Given
            var email = Email.of(emailValue);

            // When
            var actual = email.value();

            // Then
            assertThat(actual).isEqualTo(emailValue);
        }

    }

    private static Stream<String> provideValidEmails() {
        return Stream.of(
                // Standard formats
                "user@example.com", "user@domain.co.uk", "firstname.lastname@example.com",

                // Numbers in local part
                "user123@example.com", "123user@example.com", "user.123@example.com",

                // Underscore
                "user_name@example.com", "first_last@example.com", "_user@example.com",

                // Hyphen in local part and domain
                "user-name@example.com", "user@my-domain.com", "user@sub-domain.example.com",

                // Plus sign (common for email aliases)
                "user+tag@example.com", "user+filter@example.com",

                // Dots
                "u.s.e.r@example.com", "user@sub.domain.example.com",

                // Special characters allowed by your regex
                "user!name@example.com", "user#name@example.com", "user$name@example.com", "user%name@example.com", "user&name@example.com",
                "user'name@example.com", "user*name@example.com", "user/name@example.com", "user=name@example.com", "user?name@example.com",
                "user`name@example.com", "user{name@example.com", "user|name@example.com", "user}name@example.com", "user~name@example.com",
                "user^name@example.com",

                // Short formats
                "a@b.co", "x@y.io",

                // Numeric domains
                "user@123.456.789.012", "user@domain123.com",

                // Mixed case (though should normalize to lowercase)
                "User@Example.Com", "ADMIN@DOMAIN.COM");
    }

    private static Stream<String> provideInvalidEmails() {
        return Stream.of(
                // Missing @ symbol
                "userexample.com", "user.example.com",

                // Missing local part
                "@example.com",

                // Missing domain
                "user@",

                // Multiple @ symbols
                "user@@example.com", "user@domain@example.com",

                // Invalid characters in local part
                "user name@example.com", // space
                "user\"name@example.com", // quotes (outside quotes context)
                "user(name)@example.com", // parentheses
                "user,name@example.com", // comma
                "user:name@example.com", // colon
                "user;name@example.com", // semicolon
                "user<name>@example.com", // angle brackets
                "user[name]@example.com", // square brackets
                "user\\name@example.com", // backslash

                // Invalid domain formats
                "user@", // empty domain

                // Special edge cases
                "user@", "@", "user", "example.com");
    }

}
