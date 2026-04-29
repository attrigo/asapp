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

package com.bcn.asapp.authentication.application.user.in.service;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.aUserBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.anActiveUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Tests {@link UpdateUserService} two-phase fetch-then-persist with optional password re-encoding.
 * <p>
 * Coverage:
 * <li>Retrieval failures propagate without executing update operations</li>
 * <li>Password encoding failures propagate when password update requested</li>
 * <li>Persistence failures propagate without completing update workflow</li>
 * <li>Successful update retrieves user, re-encodes password if provided, updates data, and persists changes</li>
 */
@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTests {

    @Mock
    private PasswordService passwordService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateUserService updateUserService;

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsUpdatedUser_UserExists() {
            // Given
            var existingUser = anActiveUser();
            var existingUserId = existingUser.getId();
            var newUser = aUserBuilder().active()
                                        .withUsername("new_user@asapp.com")
                                        .withRole(ADMIN)
                                        .build();
            var newUsername = newUser.getUsername();
            var newPasswordValue = "newPassword123!";
            var newRole = newUser.getRole();
            var command = new UpdateUserCommand(existingUserId.value(), newUsername.value(), newPasswordValue, newRole.name());
            var newRawPassword = RawPassword.of(newPasswordValue);
            var newEncodedPassword = EncodedPassword.of("{bcrypt}$2a$10$newEncodedPassword");

            var userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            given(userRepository.findById(existingUserId)).willReturn(Optional.of(existingUser));
            given(passwordService.encode(newRawPassword)).willReturn(newEncodedPassword);
            given(userRepository.save(existingUser)).willReturn(newUser);

            // When
            var actual = updateUserService.updateUserById(command);

            // Then
            assertThat(actual).isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().getId()).as("ID").isEqualTo(existingUserId);
                softly.assertThat(actual.get().getUsername()).as("username").isEqualTo(newUsername);
                softly.assertThat(actual.get().getPassword()).as("password").isNull();
                softly.assertThat(actual.get().getRole()).as("role").isEqualTo(newRole);
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(existingUserId);
            then(passwordService).should(times(1))
                                 .encode(newRawPassword);

            // Verifies domain object was mutated before persist (save() returns reconstituted active user without password)
            then(userRepository).should(times(1))
                                .save(userArgumentCaptor.capture());
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(userArgumentCaptor.getValue().getUsername()).as("saved username").isEqualTo(newUsername);
                softly.assertThat(userArgumentCaptor.getValue().getPassword()).as("saved password").isEqualTo(newEncodedPassword);
                softly.assertThat(userArgumentCaptor.getValue().getRole()).as("saved role").isEqualTo(newRole);
                // @formatter:on
            });
        }

        @Test
        void ReturnsEmptyOptional_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var username = "user@asapp.com";
            var password = "TEST@09_password?!";
            var role = "USER";
            var command = new UpdateUserCommand(userIdValue, username, password, role);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var actual = updateUserService.updateUserById(command);

            // Then
            assertThat(actual).isEmpty();

            then(userRepository).should(times(1))
                                .findById(userId);
            then(passwordService).should(never())
                                 .encode(any(RawPassword.class));
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserRetrievalFails() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var username = "user@asapp.com";
            var password = "TEST@09_password?!";
            var role = "USER";
            var command = new UpdateUserCommand(userIdValue, username, password, role);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findById(userId);

            // When
            var actual = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

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
            var existingUser = anActiveUser();
            var existingUserId = existingUser.getId();
            var newUsername = "new_user@asapp.com";
            var newPasswordValue = "newPassword123!";
            var newRole = "ADMIN";
            var command = new UpdateUserCommand(existingUserId.value(), newUsername, newPasswordValue, newRole);
            var rawPassword = RawPassword.of(newPasswordValue);

            given(userRepository.findById(existingUserId)).willReturn(Optional.of(existingUser));
            willThrow(new RuntimeException("Password encoding failed")).given(passwordService)
                                                                       .encode(rawPassword);

            // When
            var actual = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Password encoding failed");

            then(userRepository).should(times(1))
                                .findById(existingUserId);
            then(passwordService).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserPersistenceFails() {
            // Given
            var existingUser = anActiveUser();
            var existingUserId = existingUser.getId();
            var newUsername = "new_user@asapp.com";
            var newPasswordValue = "newPassword123!";
            var newRole = "ADMIN";
            var command = new UpdateUserCommand(existingUserId.value(), newUsername, newPasswordValue, newRole);
            var rawPassword = RawPassword.of(newPasswordValue);
            var encodedPassword = EncodedPassword.of("{bcrypt}$2a$10$newEncodedPassword");

            given(userRepository.findById(existingUserId)).willReturn(Optional.of(existingUser));
            given(passwordService.encode(rawPassword)).willReturn(encodedPassword);
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .save(existingUser);

            // When
            var actual = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(existingUserId);
            then(passwordService).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(times(1))
                                .save(existingUser);
        }

    }

}
