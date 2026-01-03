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

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTests {

    private final UserId id = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

    private final FirstName firstName = FirstName.of("FirstName");

    private final LastName lastName = LastName.of("LastName");

    private final Email email = Email.of("user@asapp.com");

    private final PhoneNumber phoneNumber = PhoneNumber.of("555 555 555");

    @Nested
    class CreateNewUser {

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // When
            var thrown = catchThrowable(() -> User.create(null, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // When
            var thrown = catchThrowable(() -> User.create(firstName, null, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // When
            var thrown = catchThrowable(() -> User.create(firstName, lastName, null, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // When
            var thrown = catchThrowable(() -> User.create(firstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ReturnsNewUser_ValidParameters() {
            // When
            var actual = User.create(firstName, lastName, email, phoneNumber);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getFirstName()).isEqualTo(firstName);
            assertThat(actual.getLastName()).isEqualTo(lastName);
            assertThat(actual.getEmail()).isEqualTo(email);
            assertThat(actual.getPhoneNumber()).isEqualTo(phoneNumber);
        }

    }

    @Nested
    class CreateReconstitutedUser {

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var thrown = catchThrowable(() -> User.reconstitute(null, firstName, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstName() {
            // When
            var thrown = catchThrowable(() -> User.reconstitute(id, null, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastName() {
            // When
            var thrown = catchThrowable(() -> User.reconstitute(id, firstName, null, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmail() {
            // When
            var thrown = catchThrowable(() -> User.reconstitute(id, firstName, lastName, null, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumber() {
            // When
            var thrown = catchThrowable(() -> User.reconstitute(id, firstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ReturnsReconstitutedUser_ValidParameters() {
            // When
            var actual = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getFirstName()).isEqualTo(firstName);
            assertThat(actual.getLastName()).isEqualTo(lastName);
            assertThat(actual.getEmail()).isEqualTo(email);
            assertThat(actual.getPhoneNumber()).isEqualTo(phoneNumber);
        }

    }

    @Nested
    class UpdateUserData {

        @Test
        void ThrowsIllegalArgumentException_NullFirstNameOnNewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastNameOnNewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmailOnNewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, newLastName, null, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumberOnNewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, newLastName, newEmail, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void UpdatesAllFields_ValidParametersOnNewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            user.update(newFirstName, newLastName, newEmail, newPhoneNumber);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getFirstName()).isEqualTo(newFirstName);
            assertThat(user.getLastName()).isEqualTo(newLastName);
            assertThat(user.getEmail()).isEqualTo(newEmail);
            assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstNameOnReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullLastNameOnReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullEmailOnReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, newLastName, null, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullPhoneNumberOnReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, newLastName, newEmail, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void UpdatesAllFields_ValidParametersOnReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("NewFirstName");
            var newLastName = LastName.of("NewLastName");
            var newEmail = Email.of("new_user@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            user.update(newFirstName, newLastName, newEmail, newPhoneNumber);

            // Then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getFirstName()).isEqualTo(newFirstName);
            assertThat(user.getLastName()).isEqualTo(newLastName);
            assertThat(user.getEmail()).isEqualTo(newEmail);
            assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ReturnsFalse_NullOtherUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);
            var other = "not a user";

            // When
            var actual = user.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsTrue_SameObjectOtherUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(user);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_NewUserAndReconstitutedUser() {
            // Given
            var user1 = User.create(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ReturnsTrue_ThreeReconstitutedUsersSameId() {
            // Given
            var user1 = User.reconstitute(id, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(id, firstName, lastName, email, phoneNumber);
            var user3 = User.reconstitute(id, firstName, lastName, email, phoneNumber);

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
        void ReturnsFalse_ThreeReconstitutedUsersDifferentId() {
            // Given
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
        void ReturnsDifferentHashCode_TwoNewUsers() {
            // Given
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
            var user1 = User.create(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsSameHashCode_TwoReconstitutedUsersSameId() {
            // Given
            var user1 = User.reconstitute(id, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoReconstitutedUsersDifferentId() {
            // Given
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
        void ReturnsNull_NewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ReturnsId_ReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetFirstName {

        @Test
        void ReturnsFirstName_NewUser() {
            // Given
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getFirstName();

            // Then
            assertThat(actual).isEqualTo(firstName);
        }

        @Test
        void ReturnsFirstName_ReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

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
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getLastName();

            // Then
            assertThat(actual).isEqualTo(lastName);
        }

        @Test
        void ReturnsLastName_ReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

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
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getEmail();

            // Then
            assertThat(actual).isEqualTo(email);
        }

        @Test
        void ReturnsEmail_ReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

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
            var user = User.create(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

        @Test
        void ReturnsPhoneNumber_ReconstitutedUser() {
            // Given
            var user = User.reconstitute(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

    }

}
