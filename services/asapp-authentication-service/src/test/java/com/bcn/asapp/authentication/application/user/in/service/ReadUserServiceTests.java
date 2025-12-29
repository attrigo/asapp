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

import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

@ExtendWith(MockitoExtension.class)
class ReadUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadUserService readUserService;

    private final UUID userIdValue = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final String usernameValue = "user@asapp.com";

    @Nested
    class GetUserById {

        @Test
        void ThrowsDataAccessException_RepositoryFetchFails() {
            // Given
            var userId = UserId.of(userIdValue);

            willThrow(new DataAccessException("Database connection failed") {}).given(userRepository)
                                                                               .findById(userId);

            // When
            var thrown = catchThrowable(() -> readUserService.getUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ReturnsEmptyOptional_UserNotFound() {
            // Given
            var userId = UserId.of(userIdValue);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When
            var result = readUserService.getUserById(userIdValue);

            // Then
            assertThat(result).isEmpty();

            then(userRepository).should(times(1))
                                .findById(userId);
        }

        @Test
        void ReturnsUser_UserExists() {
            // Given
            var userId = UserId.of(userIdValue);
            var username = Username.of(usernameValue);
            var user = User.activeUser(userId, username, USER);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            var result = readUserService.getUserById(userIdValue);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isNotNull();
            assertThat(result.get()
                             .getId()).isEqualTo(userId);
            assertThat(result.get()
                             .getUsername()).isEqualTo(username);
            assertThat(result.get()
                             .getRole()).isEqualTo(USER);

            then(userRepository).should(times(1))
                                .findById(userId);
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void ThrowsDataAccessException_RepositoryFetchFails() {
            // Given
            willThrow(new DataAccessException("Database connection failed") {}).given(userRepository)
                                                                               .findAll();

            // When
            var thrown = catchThrowable(() -> readUserService.getAllUsers());

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsEmptyList_NoUsersExist() {
            // Given
            given(userRepository.findAll()).willReturn(Collections.emptyList());

            // When
            var result = readUserService.getAllUsers();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            then(userRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsListOfUsers_UsersExist() {
            // Given
            var user1 = User.activeUser(UserId.of(UUID.randomUUID()), Username.of("user1@asapp.com"), USER);
            var user2 = User.activeUser(UserId.of(UUID.randomUUID()), Username.of("user2@asapp.com"), USER);
            var users = List.of(user1, user2);

            given(userRepository.findAll()).willReturn(users);

            // When
            var result = readUserService.getAllUsers();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(user1, user2);

            then(userRepository).should(times(1))
                                .findAll();
        }

    }

}
