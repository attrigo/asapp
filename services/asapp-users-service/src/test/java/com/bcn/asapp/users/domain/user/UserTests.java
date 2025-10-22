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

    private final UserId id = UserId.of(UUID.randomUUID());

    private final FirstName firstName = FirstName.of("John");

    private final LastName lastName = LastName.of("Doe");

    private final Email email = Email.of("user@asapp.com");

    private final PhoneNumber phoneNumber = PhoneNumber.of("555 555 555");

    @Nested
    class CreateNewUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.newUser(null, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenLastNameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.newUser(firstName, null, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenEmailIsNull() {
            // When
            var thrown = catchThrowable(() -> User.newUser(firstName, lastName, null, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNull() {
            // When
            var thrown = catchThrowable(() -> User.newUser(firstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ThenReturnsInactiveUser_GivenParametersAreValid() {
            // When
            var actual = User.newUser(firstName, lastName, email, phoneNumber);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getFirstName()).isEqualTo(firstName);
            assertThat(actual.getLastName()).isEqualTo(lastName);
            assertThat(actual.getEmail()).isEqualTo(email);
            assertThat(actual.getPhoneNumber()).isEqualTo(phoneNumber);
        }

    }

    @Nested
    class CreateReconstructedUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> User.reconstructedUser(null, firstName, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.reconstructedUser(id, null, lastName, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenLastNameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.reconstructedUser(id, firstName, null, email, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenEmailIsNull() {
            // When
            var thrown = catchThrowable(() -> User.reconstructedUser(id, firstName, lastName, null, phoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNull() {
            // When
            var thrown = catchThrowable(() -> User.reconstructedUser(id, firstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ThenReturnsInactiveUser_GivenParametersAreValid() {
            // When
            var actual = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

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
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNullOnNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenLastNameIsNullOnNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newEmail = Email.of("new_email@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenEmailIsNullOnNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, lastName, null, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNullOnNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ThenUpdatesAllFields_GivenParametersAreValidOnNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");
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
        void ThenThrowsIllegalArgumentException_GivenFirstNameIsNullOnReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(null, newLastName, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("First name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenLastNameIsNullOnReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newEmail = Email.of("new_email@asapp.com");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, null, newEmail, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Last name must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenEmailIsNullOnReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newPhoneNumber = PhoneNumber.of("555-555-555");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, lastName, null, newPhoneNumber));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Email must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPhoneNumberIsNullOnReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");

            // When
            var thrown = catchThrowable(() -> user.update(newFirstName, lastName, email, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Phone number must not be null");
        }

        @Test
        void ThenUpdatesAllFields_GivenParametersAreValidOnReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            var newFirstName = FirstName.of("new_first_name");
            var newLastName = LastName.of("new_last_name");
            var newEmail = Email.of("new_email@asapp.com");
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
        void ThenReturnsFalse_GivenOtherUserIsNull() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenOtherClassIsNotUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);
            var other = "not a user";

            // When
            var actual = user.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenOtherUserIsSameObject() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.equals(user);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenNewUserAndReconstructedUser() {
            // Given
            var user1 = User.newUser(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenThreeReconstructedUsersWithSameId() {
            // Given
            var user1 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);
            var user3 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

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
        void ThenReturnsFalse_GivenThreeReconstructedUsersWithDifferentId() {
            // Given
            var user1 = User.reconstructedUser(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);
            var user3 = User.reconstructedUser(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);

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
        void ThenReturnsDifferentHashCode_GivenTwoNewUsers() {
            // Given
            var user1 = User.newUser(firstName, lastName, email, phoneNumber);
            var user2 = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenNewUserAndReconstructedUser() {
            // Given
            var user1 = User.newUser(firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsSameHashCode_GivenTwoReconstructedUsersWithSameId() {
            // Given
            var user1 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoReconstructedUsersWithDifferentId() {
            // Given
            var user1 = User.reconstructedUser(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);
            var user2 = User.reconstructedUser(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);

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
        void ThenReturnsNull_GivenNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsId_GivenReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetFirstName {

        @Test
        void ThenReturnsFirstName_GivenNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getFirstName();

            // Then
            assertThat(actual).isEqualTo(firstName);
        }

        @Test
        void ThenReturnsFirstName_GivenReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getFirstName();

            // Then
            assertThat(actual).isEqualTo(firstName);
        }

    }

    @Nested
    class GetLastName {

        @Test
        void ThenReturnsLastName_GivenNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getLastName();

            // Then
            assertThat(actual).isEqualTo(lastName);
        }

        @Test
        void ThenReturnsLastName_GivenReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getLastName();

            // Then
            assertThat(actual).isEqualTo(lastName);
        }

    }

    @Nested
    class GetEmail {

        @Test
        void ThenReturnsEmail_GivenNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getEmail();

            // Then
            assertThat(actual).isEqualTo(email);
        }

        @Test
        void ThenReturnsEmail_GivenReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getEmail();

            // Then
            assertThat(actual).isEqualTo(email);
        }

    }

    @Nested
    class GetPhoneNumber {

        @Test
        void ThenReturnsPhoneNumber_GivenNewUser() {
            // Given
            var user = User.newUser(firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

        @Test
        void ThenReturnsPhoneNumber_GivenReconstructedUser() {
            // Given
            var user = User.reconstructedUser(id, firstName, lastName, email, phoneNumber);

            // When
            var actual = user.getPhoneNumber();

            // Then
            assertThat(actual).isEqualTo(phoneNumber);
        }

    }

}
