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

package com.bcn.asapp.authentication.domain.user;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTests {

    private final UserId id = UserId.of(UUID.randomUUID());

    private final Username username = Username.of("user@asapp.com");

    private final EncodedPassword password = EncodedPassword.of("{noop}pass");

    private final Role role = USER;

    @Nested
    class CreateInactiveUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.inactiveUser(null, password, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNull() {
            // When
            var thrown = catchThrowable(() -> User.inactiveUser(username, null, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNull() {
            // When
            var thrown = catchThrowable(() -> User.inactiveUser(username, password, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenReturnsInactiveUser_GivenParametersAreValid() {
            // When
            var actual = User.inactiveUser(username, password, role);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getUsername()).isEqualTo(username);
            assertThat(actual.getPassword()).isEqualTo(password);
            assertThat(actual.getRole()).isEqualTo(role);
        }

    }

    @Nested
    class CreateActiveUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> User.activeUser(null, username, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> User.activeUser(id, null, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNull() {
            // When
            var thrown = catchThrowable(() -> User.activeUser(id, username, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenReturnsActiveUser_GivenParametersAreValid() {
            // When
            var actual = User.activeUser(id, username, role);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getUsername()).isEqualTo(username);
            assertThat(actual.getPassword()).isNull();
            assertThat(actual.getRole()).isEqualTo(role);
        }

    }

    @Nested
    class UpdateUserData {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNullOnInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            var newPassword = EncodedPassword.of("{noop}new_pass");
            var newRole = ADMIN;

            // When
            var thrown = catchThrowable(() -> user.update(null, newPassword, newRole));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenUpdatesUsernameAndRole_GivenPasswordIsNullOnInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newRole = ADMIN;

            // When
            user.update(newUsername, null, newRole);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getUsername()).isEqualTo(newUsername);
            assertThat(user.getPassword()).isEqualTo(password);
            assertThat(user.getRole()).isEqualTo(newRole);
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNullOnInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newPassword = EncodedPassword.of("{noop}new_pass");

            // When
            var thrown = catchThrowable(() -> user.update(newUsername, newPassword, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenUpdatesAllFields_GivenParametersAreValidOnInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newPassword = EncodedPassword.of("{noop}new_pass");
            var newRole = ADMIN;

            // When
            user.update(newUsername, newPassword, newRole);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getUsername()).isEqualTo(newUsername);
            assertThat(user.getPassword()).isEqualTo(password);
            assertThat(user.getRole()).isEqualTo(newRole);
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNullOnActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            var newPassword = EncodedPassword.of("{noop}new_pass");
            var newRole = ADMIN;

            // When
            var thrown = catchThrowable(() -> user.update(null, newPassword, newRole));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNullOnActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newRole = ADMIN;

            // When
            var thrown = catchThrowable(() -> user.update(newUsername, null, newRole));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNullOnActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newPassword = EncodedPassword.of("{noop}new_pass");

            // When
            var thrown = catchThrowable(() -> user.update(newUsername, newPassword, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenUpdatesUsernameAndRole_GivenParametersAreValidOnActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            var newUsername = Username.of("new_user@asapp.com");
            var newPassword = EncodedPassword.of("{noop}new_pass");
            var newRole = ADMIN;

            // When
            user.update(newUsername, newPassword, newRole);

            // Then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getUsername()).isEqualTo(newUsername);
            assertThat(user.getPassword()).isEqualTo(newPassword);
            assertThat(user.getRole()).isEqualTo(newRole);
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ThenReturnsFalse_GivenOtherUserIsNull() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenOtherClassIsNotUser() {
            // Given
            var user = User.activeUser(id, username, role);
            var other = "not a user";

            // When
            var actual = user.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenOtherUserIsSameObject() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.equals(user);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsTrue_GivenThreeInactiveUsersWithSameUsername() {
            // Given
            var user1 = User.inactiveUser(Username.of("user@asapp.com"), password, USER);
            var user2 = User.inactiveUser(Username.of("user@asapp.com"), password, USER);
            var user3 = User.inactiveUser(Username.of("user@asapp.com"), password, USER);

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
        void ThenReturnsFalse_GivenThreeInactiveUsersWithDifferentUsername() {
            // Given
            var user1 = User.inactiveUser(Username.of("user1@asapp.com"), password, USER);
            var user2 = User.inactiveUser(Username.of("user2@asapp.com"), password, USER);
            var user3 = User.inactiveUser(Username.of("user3@asapp.com"), password, USER);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user3);
            var actual3 = user1.equals(user3);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
            assertThat(actual3).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenInactiveUserAndActiveUser() {
            // Given
            var user1 = User.inactiveUser(Username.of("inactive.user@asapp.com"), password, USER);
            var user2 = User.activeUser(id, Username.of("active.user@asapp.com"), USER);

            // When
            var actual1 = user1.equals(user2);
            var actual2 = user2.equals(user1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenThreeActiveUsersWithSameId() {
            // Given
            var user1 = User.activeUser(id, Username.of("user1@asapp.com"), USER);
            var user2 = User.activeUser(id, Username.of("user2@asapp.com"), USER);
            var user3 = User.activeUser(id, Username.of("user3@asapp.com"), USER);

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
        void ThenReturnsFalse_GivenThreeActiveUsersWithDifferentId() {
            // Given
            var user1 = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);
            var user2 = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);
            var user3 = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);

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
        void ThenReturnsSameHashCode_GivenTwoInactiveUsersWithSameUsername() {
            // Given
            var user1 = User.inactiveUser(Username.of("user@asapp.com"), password, USER);
            var user2 = User.inactiveUser(Username.of("user@asapp.com"), password, USER);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoInactiveUsersWithDifferentUsername() {
            // Given
            var user1 = User.inactiveUser(Username.of("user1@asapp.com"), password, USER);
            var user2 = User.inactiveUser(Username.of("user2@asapp.com"), password, USER);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenInactiveUserAndActiveUser() {
            // Given
            var user1 = User.inactiveUser(Username.of("inactive.user@asapp.com"), password, USER);
            var user2 = User.activeUser(id, Username.of("active.user@asapp.com"), USER);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsSameHashCode_GivenTwoActiveUsersWithSameId() {
            // Given
            var user1 = User.activeUser(id, Username.of("user1@asapp.com"), USER);
            var user2 = User.activeUser(id, Username.of("user2@asapp.com"), USER);

            // When
            var actual1 = user1.hashCode();
            var actual2 = user2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoActiveUsersWithDifferentId() {
            // Given
            var user1 = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);
            var user2 = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);

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
        void ThenReturnsNull_GivenInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsId_GivenActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetUsername {

        @Test
        void ThenReturnsUsername_GivenInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            // When
            var actual = user.getUsername();

            // Then
            assertThat(actual).isEqualTo(username);
        }

        @Test
        void ThenReturnsUsername_GivenActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.getUsername();

            // Then
            assertThat(actual).isEqualTo(username);
        }

    }

    @Nested
    class GetPassword {

        @Test
        void ThenReturnsPassword_GivenInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            // When
            var actual = user.getPassword();

            // Then
            assertThat(actual).isEqualTo(password);
        }

        @Test
        void ThenReturnsNull_GivenActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.getPassword();

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetRole {

        @Test
        void ThenReturnsRole_GivenInactiveUser() {
            // Given
            var user = User.inactiveUser(username, password, role);

            // When
            var actual = user.getRole();

            // Then
            assertThat(actual).isEqualTo(role);
        }

        @Test
        void ThenReturnsRole_GivenActiveUser() {
            // Given
            var user = User.activeUser(id, username, role);

            // When
            var actual = user.getRole();

            // Then
            assertThat(actual).isEqualTo(role);
        }

    }

}
