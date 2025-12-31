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

package com.bcn.asapp.authentication.application.user.in.service;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import com.bcn.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTests {

    @Mock
    private PasswordService passwordService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateUserService updateUserService;

    private final UUID userIdValue = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final String usernameValue = "user@asapp.com";

    private final String newUsernameValue = "newuser@asapp.com";

    private final String newPasswordValue = "newPassword123";

    private final String newRoleValue = "ADMIN";

    @Nested
    class UpdateUserById {

        @Test
        void ThrowsDataAccessException_FetchUserFails() {
            // Given
            var command = new UpdateUserCommand(userIdValue, newUsernameValue, newPasswordValue, newRoleValue);
            var userId = UserId.of(userIdValue);

            willThrow(new DataAccessException("Database connection failed") {}).given(userRepository)
                                                                               .findById(userId);

            // When
            var thrown = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(never())
                                 .encode(any(RawPassword.class));
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_PasswordEncodingFails() {
            // Given
            var command = new UpdateUserCommand(userIdValue, newUsernameValue, newPasswordValue, newRoleValue);
            var userId = UserId.of(userIdValue);
            var currentUser = User.activeUser(userId, Username.of(usernameValue), USER);
            var newRawPassword = RawPassword.of(newPasswordValue);

            given(userRepository.findById(userId)).willReturn(Optional.of(currentUser));
            willThrow(new RuntimeException("Password encoding failed")).given(passwordService)
                                                                       .encode(newRawPassword);

            // When
            var thrown = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Password encoding failed");

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(times(1))
                                 .encode(newRawPassword);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsDataAccessException_SaveUserFails() {
            // Given
            var command = new UpdateUserCommand(userIdValue, newUsernameValue, newPasswordValue, newRoleValue);
            var userId = UserId.of(userIdValue);
            var currentUser = User.activeUser(userId, Username.of(usernameValue), USER);
            var newRawPassword = RawPassword.of(newPasswordValue);
            var newEncodedPassword = EncodedPassword.of("{bcrypt}$2a$10$newEncodedPassword");

            given(userRepository.findById(userId)).willReturn(Optional.of(currentUser));
            given(passwordService.encode(newRawPassword)).willReturn(newEncodedPassword);
            willThrow(new DataAccessException("Database connection failed") {}).given(userRepository)
                                                                               .save(currentUser);

            // When
            var thrown = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(times(1))
                                 .encode(newRawPassword);
            then(userRepository).should(times(1))
                                .save(currentUser);
        }

        @Test
        void ReturnsEmptyOptional_UserNotExists() {
            // Given
            var command = new UpdateUserCommand(userIdValue, newUsernameValue, newPasswordValue, newRoleValue);
            var userId = UserId.of(userIdValue);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var result = updateUserService.updateUserById(command);

            // Then
            assertThat(result).isEmpty();

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(never())
                                 .encode(any(RawPassword.class));
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ReturnsUpdatedUser_UpdateSucceed() {
            // Given
            var command = new UpdateUserCommand(userIdValue, newUsernameValue, newPasswordValue, newRoleValue);
            var userId = UserId.of(userIdValue);
            var currentUsername = Username.of(usernameValue);
            var currentUser = User.activeUser(userId, currentUsername, USER);

            var newUsername = Username.of(newUsernameValue);
            var newRawPassword = RawPassword.of(newPasswordValue);
            var newEncodedPassword = EncodedPassword.of("{bcrypt}$2a$10$newEncodedPassword");
            var updatedUser = User.activeUser(userId, newUsername, ADMIN);

            given(userRepository.findById(userId)).willReturn(Optional.of(currentUser));
            given(passwordService.encode(newRawPassword)).willReturn(newEncodedPassword);
            given(userRepository.save(currentUser)).willReturn(updatedUser);

            // When
            var result = updateUserService.updateUserById(command);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isNotNull();
            assertThat(result.get()
                             .getId()).isEqualTo(userId);
            assertThat(result.get()
                             .getUsername()).isEqualTo(newUsername);
            assertThat(result.get()
                             .getRole()).isEqualTo(ADMIN);

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(times(1))
                                 .encode(newRawPassword);
            then(userRepository).should(times(1))
                                .save(currentUser);
        }

    }

}
