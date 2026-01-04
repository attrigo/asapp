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

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeleteUserService deleteUserService;

    private final UUID userIdValue = UUID.randomUUID();

    @Nested
    class DeleteUserById {

        @Test
        void ThrowsRuntimeException_DeleteUserFails() {
            // Given
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .deleteById(userId);

            // When
            var thrown = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .deleteById(userId);
        }

        @Test
        void ReturnsFalse_UserNotExists() {
            // Given
            var userId = UserId.of(userIdValue);

            given(userRepository.deleteById(userId)).willReturn(false);

            // When
            var result = deleteUserService.deleteUserById(userIdValue);

            // Then
            then(userRepository).should(times(1))
                                .deleteById(userId);
            assertThat(result).isFalse();
        }

        @Test
        void ReturnsTrue_UserExists() {
            // Given
            var userId = UserId.of(userIdValue);

            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var result = deleteUserService.deleteUserById(userIdValue);

            // Then
            then(userRepository).should(times(1))
                                .deleteById(userId);
            assertThat(result).isTrue();
        }

    }

}
