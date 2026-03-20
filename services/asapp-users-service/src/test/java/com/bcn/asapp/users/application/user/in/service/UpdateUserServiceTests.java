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

package com.bcn.asapp.users.application.user.in.service;

import static com.bcn.asapp.users.testutil.fixture.UserFactory.aUser;
import static com.bcn.asapp.users.testutil.fixture.UserFactory.aUserBuilder;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.users.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Tests {@link UpdateUserService} two-phase fetch-then-persist with field updates.
 * <p>
 * Coverage:
 * <li>Fetch failures prevent update workflow execution</li>
 * <li>Persistence failures after successful fetch propagate</li>
 * <li>Returns empty when user does not exist (no-op update)</li>
 * <li>Successful update completes fetch, mutation, and persistence phases</li>
 */
@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateUserService updateUserService;

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsUpdatedUser_UserExists() {
            // Given
            var existingUser = aUser();
            var existingUserId = existingUser.getId();
            var newUser = aUserBuilder().withFirstName("New FirstName")
                                        .withLastName("New LastName")
                                        .withEmail("new_user@asapp.com")
                                        .withPhoneNumber("666 666 666")
                                        .build();
            var newFirstName = newUser.getFirstName();
            var newLastName = newUser.getLastName();
            var newEmail = newUser.getEmail();
            var newPhoneNumber = newUser.getPhoneNumber();
            var command = new UpdateUserCommand(existingUserId.value(), newFirstName.value(), newLastName.value(), newEmail.value(), newPhoneNumber.value());

            given(userRepository.findById(existingUserId)).willReturn(Optional.of(existingUser));
            given(userRepository.save(existingUser)).willReturn(existingUser); // Returns same reference so assertions verify the in-place domain mutation

            // When
            var actual = updateUserService.updateUserById(command);

            // Then
            assertThat(actual).as("updated user")
                              .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().getId()).as("ID").isEqualTo(existingUserId);
                softly.assertThat(actual.get().getFirstName()).as("first name").isEqualTo(newFirstName);
                softly.assertThat(actual.get().getLastName()).as("last name").isEqualTo(newLastName);
                softly.assertThat(actual.get().getEmail()).as("email").isEqualTo(newEmail);
                softly.assertThat(actual.get().getPhoneNumber()).as("phone number").isEqualTo(newPhoneNumber);
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(existingUserId);
            then(userRepository).should(times(1))
                                .save(existingUser);
        }

        @Test
        void ReturnsEmpty_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var firstName = "FirstName";
            var lastName = "LastName";
            var email = "user@asapp.com";
            var phoneNumber = "555 555 555";
            var command = new UpdateUserCommand(userIdValue, firstName, lastName, email, phoneNumber);
            var userId = UserId.of(userIdValue);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var actual = updateUserService.updateUserById(command);

            // Then
            assertThat(actual).isEmpty();

            then(userRepository).should(times(1))
                                .findById(userId);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserRetrievalFails() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var firstName = "FirstName";
            var lastName = "LastName";
            var email = "user@asapp.com";
            var phoneNumber = "555 555 555";
            var command = new UpdateUserCommand(userIdValue, firstName, lastName, email, phoneNumber);
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findById(userId);

            // When
            var actual = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserPersistenceFails() {
            // Given
            var existingUser = aUser();
            var existingUserId = existingUser.getId();
            var newFirstName = "New FirstName";
            var newLastName = "New LastName";
            var newEmail = "new_user@asapp.com";
            var newPhoneNumber = "666 666 666";
            var command = new UpdateUserCommand(existingUserId.value(), newFirstName, newLastName, newEmail, newPhoneNumber);

            given(userRepository.findById(existingUserId)).willReturn(Optional.of(existingUser));
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .save(existingUser);

            // When
            var actual = catchThrowable(() -> updateUserService.updateUserById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(existingUserId);
            then(userRepository).should(times(1))
                                .save(existingUser);
        }

    }

}
