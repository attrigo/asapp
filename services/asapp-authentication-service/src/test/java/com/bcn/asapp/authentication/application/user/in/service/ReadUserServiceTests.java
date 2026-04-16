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
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.aUserBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.anActiveUser;
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

import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Tests {@link ReadUserService} single and collection retrieval with failure propagation.
 * <p>
 * Coverage:
 * <li>Retrieval failures propagate for all query strategies (by ID, all users)</li>
 * <li>Returns empty result when no users match query criteria</li>
 * <li>Returns single user when queried by unique identifier</li>
 * <li>Returns user collection when querying all users</li>
 */
@ExtendWith(MockitoExtension.class)
class ReadUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadUserService readUserService;

    @Nested
    class GetUserById {

        @Test
        void ReturnsUser_UserExists() {
            // Given
            var user = anActiveUser();
            var userId = user.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            var actual = readUserService.getUserById(userId.value());

            // Then
            assertThat(actual).as("found user")
                              .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().getId()).as("ID").isEqualTo(userId);
                softly.assertThat(actual.get().getUsername()).as("username").isEqualTo(user.getUsername());
                softly.assertThat(actual.get().getPassword()).as("password").isNull();
                softly.assertThat(actual.get().getRole()).as("role").isEqualTo(USER);
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ReturnsEmptyOptional_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
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
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
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

    }

    @Nested
    class GetAllUsers {

        @Test
        void ReturnsUsers_UsersExist() {
            // Given
            var user1 = anActiveUser();
            var user2 = aUserBuilder().active()
                                      .withUserId(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"))
                                      .withUsername("user2@asapp.com")
                                      .withRole(ADMIN)
                                      .build();
            var users = List.of(user1, user2);

            given(userRepository.findAll()).willReturn(users);

            // When
            var actual = readUserService.getAllUsers();

            // Then
            assertThat(actual).hasSize(2);
            assertThat(actual).containsExactly(user1, user2);

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
