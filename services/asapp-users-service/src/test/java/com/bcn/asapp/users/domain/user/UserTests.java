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
 * Tests {@link User} persistence states, profile updates, and identity equality.
 * <p>
 * Coverage:
 * <li>Creates new user with profile data (first name, last name, email, phone), null ID</li>
 * <li>Creates reconstituted user with ID and complete profile data</li>
 * <li>Updates profile data on both creation and reconstitution states</li>
 * <li>Validates all profile fields required for both states</li>
 * <li>Validates ID required only for reconstituted state</li>
 * <li>Implements identity-based equality using ID for reconstituted users, unique hash for new users</li>
 */
class UserTests {

    @Nested
    class CreateNewUser {

        @Test
        void ReturnsNewUser_ValidParameters() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = User.create(firstName, lastName, email, phoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getFirstName()).as("first name").isEqualTo(firstName);
                softly.assertThat(actual.getLastName()).as("last name").isEqualTo(lastName);
                softly.assertThat(actual.getEmail()).as("email").isEqualTo(email);
                softly.assertThat(actual.getPhoneNumber()).as("phone number").isEqualTo(phoneNumber);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // Given
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.create(null, lastName, email, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // Given
            var firstName = FirstName.of("FirstName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.create(firstName, null, email, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.create(firstName, lastName, null, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");

            // When
            var actual = catchThrowable(() -> User.create(firstName, lastName, email, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

    }

    @Nested
    class CreateReconstitutedUser {

        @Test
        void ReturnsReconstitutedUser_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(userId);
                softly.assertThat(actual.getFirstName()).as("first name").isEqualTo(firstName);
                softly.assertThat(actual.getLastName()).as("last name").isEqualTo(lastName);
                softly.assertThat(actual.getEmail()).as("email").isEqualTo(email);
                softly.assertThat(actual.getPhoneNumber()).as("phone number").isEqualTo(phoneNumber);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.reconstitute(null, firstName, lastName, email, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.reconstitute(userId, null, lastName, email, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.reconstitute(userId, firstName, null, email, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var phoneNumber = PhoneNumber.of("555 555 555");

            // When
            var actual = catchThrowable(() -> User.reconstitute(userId, firstName, lastName, null, phoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");

            // When
            var actual = catchThrowable(() -> User.reconstitute(userId, firstName, lastName, email, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

    }

    @Nested
    class UpdateUserData {

        @Test
        void UpdatesAllFields_ValidParametersOnNewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            user.update(newFirstName, newLastName, newEmail, newPhoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(user.getId()).as("ID").isNull();
                softly.assertThat(user.getFirstName()).as("first name").isEqualTo(newFirstName);
                softly.assertThat(user.getLastName()).as("last name").isEqualTo(newLastName);
                softly.assertThat(user.getEmail()).as("email").isEqualTo(newEmail);
                softly.assertThat(user.getPhoneNumber()).as("phone number").isEqualTo(newPhoneNumber);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_ValidParametersOnReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            user.update(newFirstName, newLastName, newEmail, newPhoneNumber);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(user.getId()).as("ID").isEqualTo(userId);
                softly.assertThat(user.getFirstName()).as("first name").isEqualTo(newFirstName);
                softly.assertThat(user.getLastName()).as("last name").isEqualTo(newLastName);
                softly.assertThat(user.getEmail()).as("email").isEqualTo(newEmail);
                softly.assertThat(user.getPhoneNumber()).as("phone number").isEqualTo(newPhoneNumber);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstNameOnNewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastNameOnNewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmailOnNewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, newLastName, null, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumberOnNewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, newLastName, newEmail, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstNameOnReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastNameOnReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmailOnReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, newLastName, null, newPhoneNumber));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumberOnReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");

            // When
            var actual = catchThrowable(() -> user.update(newFirstName, newLastName, newEmail, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ReturnsTrue_SameObjectOtherUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(user);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsTrue_ThreeReconstitutedUsersSameId() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user1 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
            var user3 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user3);
            var actual3 = user1.equals(user3);

            // Then
            assertThat(actual1).isTrue();
            assertThat(actual2).isTrue();
            assertThat(actual3).isTrue();
        }

        @Test
        void ReturnsFalse_NullOtherUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
            var other = "not a user";

            // When
            var actual = user.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_NewUserAndReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user1 = User.create(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ReturnsFalse_ThreeReconstitutedUsersDifferentId() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var userId1 = UserId.of(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"));
            var userId2 = UserId.of(UUID.fromString("8f7e3d2a-5c4b-4e9f-9a1e-3b2c1d0e5f6a"));
            var userId3 = UserId.of(UUID.fromString("3f8d2a1b-6c5e-4f7d-9a8b-2c1e0d9f8e7c"));
            var user1 = User.reconstitute(userId1, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId2, firstName, lastName, email, phoneNumber);
            var user3 = User.reconstitute(userId3, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user3);
            var actual3 = user1.equals(user3);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
            assertThat(actual3).isFalse();
        }

    }

    @Nested
    class HashCode {

        @Test
        void ReturnsSameHashCode_TwoReconstitutedUsersSameId() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user1 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoNewUsers() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user1 = User.create(firstName, lastName, email, phoneNumber);
            var user2 = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_NewUserAndReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user1 = User.create(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoReconstitutedUsersDifferentId() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var userId1 = UserId.of(UUID.fromString("7c9e4a2f-3d1b-4e8c-9f5a-6b8d2c3e1f4a"));
            var userId2 = UserId.of(UUID.fromString("5a6b7c8d-9e0f-4a1b-2c3d-4e5f6a7b8c9d"));
            var user1 = User.reconstitute(userId1, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(userId2, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

    }

    @Nested
    class GetId {

        @Test
        void ReturnsId_ReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ReturnsNull_NewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetFirstName {

        @Test
        void ReturnsFirstName_NewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getFirstName();

            // Then
            assertThat(actual).isEqualTo(firstName);
        }

        @Test
        void ReturnsFirstName_ReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getFirstName();

            // Then
            assertThat(actual).isEqualTo(firstName);
        }

    }

    @Nested
    class GetLastName {

        @Test
        void ReturnsLastName_NewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getLastName();

            // Then
            assertThat(actual).isEqualTo(lastName);
        }

        @Test
        void ReturnsLastName_ReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getLastName();

            // Then
            assertThat(actual).isEqualTo(lastName);
        }

    }

    @Nested
    class GetEmail {

        @Test
        void ReturnsEmail_NewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getEmail();

            // Then
            assertThat(actual).isEqualTo(email);
        }

        @Test
        void ReturnsEmail_ReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getEmail();

            // Then
            assertThat(actual).isEqualTo(email);
        }

    }

    @Nested
    class GetPhoneNumber {

        @Test
        void ReturnsPhoneNumber_NewUser() {
            // Given
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

        @Test
        void ReturnsPhoneNumber_ReconstitutedUser() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var firstName = FirstName.of("FirstName");
            var lastName = LastName.of("LastName");
            var email = Email.of("user@asapp.com");
            var phoneNumber = PhoneNumber.of("555 555 555");
            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

    }

}
