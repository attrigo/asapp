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

package com.bcn.asapp.users.application.user.in.service;

import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUserBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

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
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Tests {@link ReadUserService} single and collection retrieval with task enrichment.
 * <p>
 * Coverage:
 * <li>Retrieval failures propagate for all query strategies (by ID, all users)</li>
 * <li>Returns empty result when no users match query criteria</li>
 * <li>Returns single user when queried by unique identifier</li>
 * <li>Returns user collection when querying all users</li>
 * <li>Enriches user data with associated task identifiers via external gateway</li>
 * <li>Propagates task gateway failures to the caller</li>
 */
@ExtendWith(MockitoExtension.class)
class ReadUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TasksGateway tasksGateway;

    @InjectMocks
    private ReadUserService readUserService;

    @Nested
    class GetUserById {

        @Test
        void ReturnsUserWithEmptyTaskList_UserHasNoTasks() {
            // Given
            var user = aUser();
            var userId = user.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(tasksGateway.getTaskIdsByUserId(userId)).willReturn(List.of());

            // When
            var actual = readUserService.getUserById(userId.value());

            // Then
            assertThat(actual).as("found user with tasks")
                              .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().user()).as("user").isEqualTo(user);
                softly.assertThat(actual.get().taskIds()).as("task IDs").isEmpty();
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
        }

        @Test
        void ReturnsUserWithTasks_UserHasTasks() {
            // Given
            var user = aUser();
            var userId = user.getId();
            var taskId1 = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789");
            var taskId2 = UUID.fromString("b2c3d4e5-f6a7-4890-bcde-f01234567890");
            var taskIds = List.of(taskId1, taskId2);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(tasksGateway.getTaskIdsByUserId(userId)).willReturn(taskIds);

            // When
            var actual = readUserService.getUserById(userId.value());

            // Then
            assertThat(actual).as("found user with tasks")
                              .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().user()).as("user").isEqualTo(user);
                softly.assertThat(actual.get().taskIds()).as("task IDs").hasSize(2);
                softly.assertThat(actual.get().taskIds()).as("task IDs").contains(taskId1, taskId2);
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
        }

        @Test
        void ReturnsEmptyOptional_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var userId = UserId.of(userIdValue);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var actual = readUserService.getUserById(userIdValue);

            // Then
            assertThat(actual).isEmpty();

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ThrowsRuntimeException_UserRetrievalFails() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findById(userId);

            // When
            var actual = catchThrowable(() -> readUserService.getUserById(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ThrowsRuntimeException_TaskGatewayOperationFails() {
            // Given
            var user = aUser();
            var userId = user.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            willThrow(new RuntimeException("Tasks service unavailable")).given(tasksGateway)
                                                                        .getTaskIdsByUserId(userId);

            // When
            var actual = catchThrowable(() -> readUserService.getUserById(userId.value()));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Tasks service unavailable");

            then(userRepository).should(times(1))
                                .findById(userId);
            then(tasksGateway).should(times(1))
                              .getTaskIdsByUserId(userId);
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void ReturnsUsers_UsersExist() {
            // Given
            var user1 = aUser();
            var user2 = aUserBuilder().withUserId(UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789"))
                                      .withFirstName("FirstName 2")
                                      .withLastName("LastName 2")
                                      .withEmail("user2@asapp.com")
                                      .withPhoneNumber("666 666 666")
                                      .build();
            var users = List.of(user1, user2);

            given(userRepository.findAll()).willReturn(users);

            // When
            var actual = readUserService.getAllUsers();

            // Then
            assertThat(actual).hasSize(2);
            assertThat(actual).contains(user1, user2);

            then(userRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsEmptyList_UsersNotExist() {
            // Given
            given(userRepository.findAll()).willReturn(List.of());

            // When
            var actual = readUserService.getAllUsers();

            // Then
            assertThat(actual).isEmpty();

            then(userRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ThrowsRuntimeException_UsersRetrievalFails() {
            // Given
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .findAll();

            // When
            var actual = catchThrowable(() -> readUserService.getAllUsers());

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .findAll();
        }

    }

}
