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

package com.bcn.asapp.uaa.user.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.security.authentication.revoker.JwtRevoker;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserMapperImpl;
import com.bcn.asapp.uaa.user.UserRepository;

@ExtendWith(SpringExtension.class)
class UserServiceImplTests {

    @Mock
    private PasswordEncoder passwordEncoderMock;

    @Mock
    private UserRepository userRepositoryMock;

    @Spy
    private UserMapperImpl userMapperSpy;

    @Mock
    private JwtRevoker jwtRevokerMock;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID fakeUserId;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private String fakeUserEncodedPassword;

    private Role fakeUserRole;

    @BeforeEach
    void beforeEach() {
        this.fakeUserId = UUID.randomUUID();
        this.fakeUserUsername = "TEST USERNAME";
        this.fakeUserPassword = "TEST PASSWORD";
        this.fakeUserEncodedPassword = "ENCODED TEST PASSWORD";
        this.fakeUserRole = Role.USER;
    }

    @Nested
    class FindById {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN find a user by id THEN does not find the user And returns empty")
        void UserIdNotExists_FindById_DoesNotFindUserAndReturnsEmpty() {
            // Given
            given(userRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When
            var idToFind = fakeUserId;

            var actualUser = userService.findById(idToFind);

            // Then
            assertFalse(actualUser.isPresent());

            then(userRepositoryMock).should(times(1))
                                    .findById(idToFind);
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN find a user by id THEN finds the user And returns the user found")
        void UserIdExists_FindById_FindsUserAndReturnsUserFound() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole);
            given(userRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeUser));

            // When
            var idToFind = fakeUserId;

            var actualUser = userService.findById(idToFind);

            // Then
            var expectedUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            assertTrue(actualUser.isPresent());
            assertEquals(expectedUser, actualUser.get());

            then(userRepositoryMock).should(times(1))
                                    .findById(idToFind);
        }

    }

    @Nested
    class FindAll {

        @Test
        @DisplayName("GIVEN there are not users WHEN find all users THEN does not find any users And returns empty list")
        void ThereAreNotUsers_FindAll_DoesNotFindUsersAndReturnsEmptyList() {
            // Given
            given(userRepositoryMock.findAll()).willReturn(Collections.emptyList());

            // When
            var actualUsers = userService.findAll();

            // Then
            assertTrue(actualUsers.isEmpty());

            then(userRepositoryMock).should(times(1))
                                    .findAll();
        }

        @Test
        @DisplayName("GIVEN there are users WHEN find all users THEN finds users And returns the users found")
        void ThereAreUsers_FindAll_FindsUsersAndReturnsUsersFound() {
            var fakeUserId1 = UUID.randomUUID();
            var fakeUserId2 = UUID.randomUUID();
            var fakeUserId3 = UUID.randomUUID();
            var fakeUserUsername1 = fakeUserUsername + " 1";
            var fakeUserUsername2 = fakeUserUsername + " 2";
            var fakeUserUsername3 = fakeUserUsername + " 3";
            var fakeUserPassword1 = fakeUserPassword + " 1";
            var fakeUserPassword2 = fakeUserPassword + " 2";
            var fakeUserPassword3 = fakeUserPassword + " 3";

            // Given
            var fakeUser1 = new User(fakeUserId1, fakeUserUsername1, fakeUserPassword1, fakeUserRole);
            var fakeUser2 = new User(fakeUserId2, fakeUserUsername2, fakeUserPassword2, fakeUserRole);
            var fakeUser3 = new User(fakeUserId3, fakeUserUsername3, fakeUserPassword3, fakeUserRole);
            var fakeUsers = List.of(fakeUser1, fakeUser2, fakeUser3);
            given(userRepositoryMock.findAll()).willReturn(fakeUsers);

            // When
            var actualUsers = userService.findAll();

            // Then
            var expectedUser1 = new UserDTO(fakeUserId1, fakeUserUsername1, fakeUserPassword1, fakeUserRole.name());
            var expectedUser2 = new UserDTO(fakeUserId2, fakeUserUsername2, fakeUserPassword2, fakeUserRole.name());
            var expectedUser3 = new UserDTO(fakeUserId3, fakeUserUsername3, fakeUserPassword3, fakeUserRole.name());
            var expectedUsers = List.of(expectedUser1, expectedUser2, expectedUser3);

            assertIterableEquals(expectedUsers, actualUsers);

            then(userRepositoryMock).should(times(1))
                                    .findAll();
        }

    }

    @Nested
    class Create {

        @Test
        @DisplayName("GIVEN user id field is not null WHEN create a user THEN creates the user with the encoded password ignoring the given user id And returns the user created with a new id")
        void UserIdFieldIsNotNull_Create_CreatesUserWithEncodedPasswordIgnoringUserIdAndReturnsUserCreated() {
            var anotherFakeUserId = UUID.randomUUID();

            // Given
            given(passwordEncoderMock.encode(any(String.class))).willReturn(fakeUserEncodedPassword);

            var fakeUser = new User(anotherFakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole);
            given(userRepositoryMock.save(any(User.class))).willReturn(fakeUser);

            // When
            var userToCreate = new UserDTO(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.create(userToCreate);

            // Then
            var expectedUser = new UserDTO(anotherFakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole.name());

            assertEquals(expectedUser, actualUser);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepositoryMock).should(times(1))
                                    .save(userArgumentCaptor.capture());
            User userArgument = userArgumentCaptor.getValue();
            assertNull(userArgument.id());
            assertEquals(fakeUserUsername, userArgument.username());
            assertEquals(fakeUserEncodedPassword, userArgument.password());
            assertEquals(fakeUserRole.name(), userArgument.role()
                                                          .name());
        }

        @Test
        @DisplayName("GIVEN user id field is null WHEN create a user THEN creates the user with the encoded password And returns the user created with a new id")
        void UserIdFieldIsNull_Create_CreatesUserWithEncodedPasswordAndReturnsUserCreated() {
            var anotherFakeUserId = UUID.randomUUID();

            // Given
            given(passwordEncoderMock.encode(any(String.class))).willReturn(fakeUserEncodedPassword);

            var fakeUser = new User(anotherFakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole);
            given(userRepositoryMock.save(any(User.class))).willReturn(fakeUser);

            // When
            var userToCreate = new UserDTO(null, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.create(userToCreate);

            // Then
            var expectedUser = new UserDTO(anotherFakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole.name());

            assertEquals(expectedUser, actualUser);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepositoryMock).should(times(1))
                                    .save(userArgumentCaptor.capture());
            User userArgument = userArgumentCaptor.getValue();
            assertNull(userArgument.id());
            assertEquals(fakeUserUsername, userArgument.username());
            assertEquals(fakeUserEncodedPassword, userArgument.password());
            assertEquals(fakeUserRole.name(), userArgument.role()
                                                          .name());
        }

    }

    @Nested
    class UpdateById {

        @Test
        @DisplayName("GIVEN user id does not exists And new user data id field is not null WHEN update a user by id THEN does not update the user And returns empty")
        void UserIdNotExistsAndNewUserDataIdFieldIsNotNull_UpdateById_DoesNotUpdateUserAndReturnsEmpty() {
            // Given
            given(userRepositoryMock.existsById(any(UUID.class))).willReturn(false);

            // When
            var idToUpdate = fakeUserId;
            var userToUpdate = new UserDTO(UUID.randomUUID(), fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.updateById(idToUpdate, userToUpdate);

            // Then
            assertFalse(actualUser.isPresent());

            then(userRepositoryMock).should(never())
                                    .save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN user id does not exists And new user data id field is null WHEN update a user by id THEN does not update the user And returns empty")
        void UserIdNotExistsAndNewUserDataIdFieldIsNull_UpdateById_DoesNotUpdateUserAndReturnsEmpty() {
            // Given
            given(userRepositoryMock.existsById(any(UUID.class))).willReturn(false);

            // When
            var idToUpdate = fakeUserId;
            var userToUpdate = new UserDTO(null, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.updateById(idToUpdate, userToUpdate);

            // Then
            assertFalse(actualUser.isPresent());

            then(userRepositoryMock).should(never())
                                    .save(any(User.class));
        }

        @Test
        @DisplayName("GIVEN user id exists And new user data id field is not null WHEN update a user by id THEN updates all fields of the user except the id And returns the user updated with the new data")
        void UserIdExistsAndNewUserDataIdFieldIsNotNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsUserUpdated() {
            // Given
            given(userRepositoryMock.existsById(any(UUID.class))).willReturn(true);

            given(passwordEncoderMock.encode(any(String.class))).willReturn(fakeUserEncodedPassword);

            var fakeUser = new User(fakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole);
            given(userRepositoryMock.save(any(User.class))).willReturn(fakeUser);

            // When
            var idToUpdate = fakeUserId;
            var userToUpdate = new UserDTO(UUID.randomUUID(), fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.updateById(idToUpdate, userToUpdate);

            // Then
            var expectedUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole.name());

            assertTrue(actualUser.isPresent());
            assertEquals(expectedUser, actualUser.get());

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepositoryMock).should(times(1))
                                    .save(userArgumentCaptor.capture());
            User userArgument = userArgumentCaptor.getValue();
            assertEquals(fakeUserId, userArgument.id());
            assertEquals(fakeUserUsername, userArgument.username());
            assertEquals(fakeUserEncodedPassword, userArgument.password());
            assertEquals(fakeUserRole.name(), userArgument.role()
                                                          .name());
        }

        @Test
        @DisplayName("GIVEN user id exists And new user data id field is null WHEN update a user by id THEN updates all fields of the user except the id And returns the user updated with the new data")
        void UserIdExistsAndNewUserDataIdFieldIsNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsUserUpdated() {
            // Given
            given(userRepositoryMock.existsById(any(UUID.class))).willReturn(true);

            given(passwordEncoderMock.encode(any(String.class))).willReturn(fakeUserEncodedPassword);

            var fakeUser = new User(fakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole);
            given(userRepositoryMock.save(any(User.class))).willReturn(fakeUser);

            // When
            var idToUpdate = fakeUserId;
            var userToUpdate = new UserDTO(null, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var actualUser = userService.updateById(idToUpdate, userToUpdate);

            // Then
            var expectedUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserEncodedPassword, fakeUserRole.name());

            assertTrue(actualUser.isPresent());
            assertEquals(expectedUser, actualUser.get());

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepositoryMock).should(times(1))
                                    .save(userArgumentCaptor.capture());
            User userArgument = userArgumentCaptor.getValue();
            assertEquals(fakeUserId, userArgument.id());
            assertEquals(fakeUserUsername, userArgument.username());
            assertEquals(fakeUserEncodedPassword, userArgument.password());
            assertEquals(fakeUserRole.name(), userArgument.role()
                                                          .name());
        }

    }

    @Nested
    class DeleteById {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN delete a user by id THEN does not delete the user And returns false")
        void UserIdNotExists_DeleteById_DoesNotDeleteUserAndReturnsFalse() {
            // Given
            given(userRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When
            var idToDelete = fakeUserId;

            var actual = userService.deleteById(idToDelete);

            // Then
            assertFalse(actual);

            then(userRepositoryMock).should(never())
                                    .deleteUserById(idToDelete);
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN delete a user by id THEN revokes the user authentication And deletes the user And returns true")
        void UserIdExists_DeleteById_RevokesAuthenticationAndDeletesUserAndReturnsTrue() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole);
            given(userRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeUser));
            willDoNothing().given(jwtRevokerMock)
                           .revokeAuthentication(any(User.class));
            given(userRepositoryMock.deleteUserById(any(UUID.class))).willReturn(1L);

            // When
            var idToDelete = fakeUserId;

            var actual = userService.deleteById(idToDelete);

            // Then
            assertTrue(actual);

            then(userRepositoryMock).should(times(1))
                                    .deleteUserById(idToDelete);
        }

    }

}
