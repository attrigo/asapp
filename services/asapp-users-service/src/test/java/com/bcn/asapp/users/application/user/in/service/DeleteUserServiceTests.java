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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Tests {@link DeleteUserService} user deletion and failure propagation.
 * <p>
 * Coverage:
 * <li>Deletion failures propagate without completing workflow</li>
 * <li>Returns false when user does not exist</li>
 * <li>Returns true when user successfully deleted</li>
 */
@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeleteUserService deleteUserService;

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsTrue_UserExists() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var userId = UserId.of(userIdValue);

            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var actual = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(actual).isTrue();

            then(userRepository).should(times(1))
                                .deleteById(userId);
        }

        @Test
        void ReturnsFalse_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var userId = UserId.of(userIdValue);

            given(userRepository.deleteById(userId)).willReturn(false);

            // When
            var actual = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(actual).isFalse();

            then(userRepository).should(times(1))
                                .deleteById(userId);
        }

        @Test
        void ThrowsRuntimeException_UserDeletionFails() {
            // Given
            var userIdValue = UUID.fromString("d4e5f6a7-b8c9-4012-d3e4-f5a6b7c8d9e0");
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .deleteById(userId);

            // When
            var actual = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .deleteById(userId);
        }

    }

}
