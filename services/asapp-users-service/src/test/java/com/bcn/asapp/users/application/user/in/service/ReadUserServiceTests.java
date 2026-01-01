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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class ReadUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TasksGateway tasksGateway;

    @InjectMocks
    private ReadUserService readUserService;

    // Specific test data
    private final UUID userIdValue = UUID.randomUUID();

    private final String firstNameValue = "FirstName";

    private final String lastNameValue = "LastName";

    private final String emailValue = "user@asapp.com";

    private final String phoneNumberValue = "555 555 555";

    @Nested
    class GetUserById {

        @Test
        void ThrowsRuntimeException_FetchUserFails() {
            // Given
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findById(userId);

            // When
            var thrown = catchThrowable(() -> readUserService.getUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ReturnsEmpty_UserNotExists() {
            // Given
            var userId = UserId.of(userIdValue);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var result = readUserService.getUserById(userIdValue);

            // Then
            then(userRepository).should(times(1))
                                .findById(userId);
            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsUserWithTasks_UserExists() {
            // Given
            var userId = UserId.of(userIdValue);
            var firstName = FirstName.of(firstNameValue);
            var lastName = LastName.of(lastNameValue);
            var email = Email.of(emailValue);
            var phoneNumber = PhoneNumber.of(phoneNumberValue);

            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            var taskId1 = UUID.randomUUID();
            var taskId2 = UUID.randomUUID();
            var taskIds = List.of(taskId1, taskId2);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(tasksGateway.getTaskIdsByUserId(userId)).willReturn(taskIds);

            // When
            var result = readUserService.getUserById(userIdValue);

            // Then
            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
            assertThat(result).isPresent();
            assertThat(result.get()
                             .user()).isEqualTo(user);
            assertThat(result.get()
                             .taskIds()).hasSize(2);
            assertThat(result.get()
                             .taskIds()).contains(taskId1, taskId2);
        }

        @Test
        void ReturnsUserWithEmptyTaskList_TasksGatewayFails() {
            // Given
            var userId = UserId.of(userIdValue);
            var firstName = FirstName.of(firstNameValue);
            var lastName = LastName.of(lastNameValue);
            var email = Email.of(emailValue);
            var phoneNumber = PhoneNumber.of(phoneNumberValue);

            var user = User.reconstitute(userId, firstName, lastName, email, phoneNumber);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(tasksGateway.getTaskIdsByUserId(userId)).willReturn(List.of());

            // When
            var result = readUserService.getUserById(userIdValue);

            // Then
            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
            assertThat(result).isPresent();
            assertThat(result.get()
                             .user()).isEqualTo(user);
            assertThat(result.get()
                             .taskIds()).isEmpty();
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void ThrowsRuntimeException_FetchUsersFails() {
            // Given
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findAll();

            // When
            var thrown = catchThrowable(() -> readUserService.getAllUsers());

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsEmptyList_UsersNotExists() {
            // Given
            given(userRepository.findAll()).willReturn(List.of());

            // When
            var result = readUserService.getAllUsers();

            // Then
            then(userRepository).should(times(1))
                                .findAll();
            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsUsers_UsersExists() {
            // Given
            var userId1 = UserId.of(UUID.randomUUID());
            var userId2 = UserId.of(UUID.randomUUID());

            var user1 = User.reconstitute(userId1, FirstName.of("FirstName1"), LastName.of("LastName1"), Email.of("user1@asapp.com"),
                    PhoneNumber.of("666666666"));

            var user2 = User.reconstitute(userId2, FirstName.of("FirstName2"), LastName.of("LastName2"), Email.of("user2@asapp.com"),
                    PhoneNumber.of("777777777"));

            Collection<User> users = List.of(user1, user2);

            given(userRepository.findAll()).willReturn(users);

            // When
            var result = readUserService.getAllUsers();

            // Then
            then(userRepository).should(times(1))
                                .findAll();
            assertThat(result).hasSize(2);
            assertThat(result).contains(user1, user2);
        }

    }

}
