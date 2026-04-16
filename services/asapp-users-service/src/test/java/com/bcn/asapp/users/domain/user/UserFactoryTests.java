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

package com.bcn.asapp.users.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link UserFactory} user creation from raw primitive values.
 * <p>
 * Coverage:
 * <li>Creates new user from primitives with null ID and correct field values</li>
 * <li>Reconstitutes user from primitives with ID and correct field values</li>
 * <li>Rejects null first name, last name, email, or phone number during creation</li>
 * <li>Rejects null ID, null first name, last name, email, or phone number during reconstitution</li>
 */
class UserFactoryTests {

    @Nested
    class Create {

        @Test
        void ReturnsNewUser_ValidParameters() {
            // Given
            var firstName = "FirstName";
            var lastName = "LastName";
            var email = "user@asapp.com";
            var phoneNumber = "555 555 555";
            var expectedFirstName = FirstName.of(firstName);
            var expectedLastName = LastName.of(lastName);
            var expectedEmail = Email.of(email);
            var expectedPhoneNumber = PhoneNumber.of(phoneNumber);

            // When
            var actual = UserFactory.create(firstName, lastName, email, phoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getFirstName()).as("first name").isEqualTo(expectedFirstName);
                softly.assertThat(actual.getLastName()).as("last name").isEqualTo(expectedLastName);
                softly.assertThat(actual.getEmail()).as("email").isEqualTo(expectedEmail);
                softly.assertThat(actual.getPhoneNumber()).as("phone number").isEqualTo(expectedPhoneNumber);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // When
            var actual = catchThrowable(() -> UserFactory.create(null, "LastName", "user@asapp.com", "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // When
            var actual = catchThrowable(() -> UserFactory.create("FirstName", null, "user@asapp.com", "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // When
            var actual = catchThrowable(() -> UserFactory.create("FirstName", "LastName", null, "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // When
            var actual = catchThrowable(() -> UserFactory.create("FirstName", "LastName", "user@asapp.com", null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null or empty");
        }

    }

    @Nested
    class Reconstitute {

        @Test
        void ReturnsReconstitutedUser_ValidParameters() {
            // Given
            var id = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var firstName = "FirstName";
            var lastName = "LastName";
            var email = "user@asapp.com";
            var phoneNumber = "555 555 555";
            var expectedId = UserId.of(id);
            var expectedFirstName = FirstName.of(firstName);
            var expectedLastName = LastName.of(lastName);
            var expectedEmail = Email.of(email);
            var expectedPhoneNumber = PhoneNumber.of(phoneNumber);

            // When
            var actual = UserFactory.reconstitute(id, firstName, lastName, email, phoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(expectedId);
                softly.assertThat(actual.getFirstName()).as("first name").isEqualTo(expectedFirstName);
                softly.assertThat(actual.getLastName()).as("last name").isEqualTo(expectedLastName);
                softly.assertThat(actual.getEmail()).as("email").isEqualTo(expectedEmail);
                softly.assertThat(actual.getPhoneNumber()).as("phone number").isEqualTo(expectedPhoneNumber);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> UserFactory.reconstitute(null, "FirstName", "LastName", "user@asapp.com", "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // Given
            var id = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> UserFactory.reconstitute(id, null, "LastName", "user@asapp.com", "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // Given
            var id = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> UserFactory.reconstitute(id, "FirstName", null, "user@asapp.com", "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // Given
            var id = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> UserFactory.reconstitute(id, "FirstName", "LastName", null, "555 555 555"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // Given
            var id = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> UserFactory.reconstitute(id, "FirstName", "LastName", "user@asapp.com", null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null or empty");
        }

    }

}
