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

package com.bcn.asapp.authentication.infrastructure.user.out;

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
import org.springframework.dao.DataRetrievalFailureException;

import com.bcn.asapp.authentication.application.user.UserPersistenceException;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.user.mapper.UserMapper;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;

/**
 * Tests {@link UserRepositoryAdapter} delete result mapping and persistence error handling.
 * <p>
 * Coverage:
 * <li>User deletion returns true when user was found and deleted</li>
 * <li>User deletion returns false when user was not found</li>
 * <li>User deletion translates database failures to persistence exception</li>
 */
@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTests {

    @Mock
    private JdbcUserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRepositoryAdapter userRepositoryAdapter;

    @Nested
    class DeleteById {

        @Test
        void ReturnsTrue_UserDeleted() {
            // Given
            var userId = UserId.of(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"));

            given(userRepository.deleteUserById(userId.value())).willReturn(1L);

            // When
            var actual = userRepositoryAdapter.deleteById(userId);

            // Then
            assertThat(actual).isTrue();

            then(userRepository).should(times(1))
                                .deleteUserById(userId.value());
        }

        @Test
        void ReturnsFalse_UserNotFound() {
            // Given
            var userId = UserId.of(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"));

            given(userRepository.deleteUserById(userId.value())).willReturn(0L);

            // When
            var actual = userRepositoryAdapter.deleteById(userId);

            // Then
            assertThat(actual).isFalse();

            then(userRepository).should(times(1))
                                .deleteUserById(userId.value());
        }

        @Test
        void ThrowsUserPersistenceException_DatabaseOperationFails() {
            // Given
            var userId = UserId.of(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"));

            willThrow(new DataRetrievalFailureException("Database error")).given(userRepository)
                                                                          .deleteUserById(userId.value());

            // When
            var actual = catchThrowable(() -> userRepositoryAdapter.deleteById(userId));

            // Then
            assertThat(actual).isInstanceOf(UserPersistenceException.class)
                              .hasMessage("Could not delete user from repository")
                              .hasCauseInstanceOf(DataRetrievalFailureException.class);

            then(userRepository).should(times(1))
                                .deleteUserById(userId.value());
        }

    }

}
