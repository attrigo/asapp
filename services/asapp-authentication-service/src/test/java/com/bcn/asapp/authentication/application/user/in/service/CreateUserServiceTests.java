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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTests {

    @Mock
    private PasswordService passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateUserService createUserService;

    private final String usernameValue = "user@asapp.com";

    private final String passwordValue = "password123";

    private final String roleValue = "USER";

    @Nested
    class CreateUser {

        @Test
        void ThrowsRuntimeException_PasswordEncodingFails() {
            // Given
            var command = new CreateUserCommand(usernameValue, passwordValue, roleValue);
            var rawPassword = RawPassword.of(passwordValue);

            willThrow(new RuntimeException("Password encoding failed")).given(passwordEncoder)
                                                                       .encode(rawPassword);

            // When
            var thrown = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Password encoding failed");

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_SaveUserFails() {
            // Given
            var command = new CreateUserCommand(usernameValue, passwordValue, roleValue);
            var rawPassword = RawPassword.of(passwordValue);
            var encodedPassword = EncodedPassword.of("{bcrypt}$2a$10$encodedPassword");

            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .save(any(User.class));

            // When
            var thrown = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

        @Test
        void ReturnsCreatedUser_ValidUser() {
            // Given
            var command = new CreateUserCommand(usernameValue, passwordValue, roleValue);
            var username = Username.of(usernameValue);
            var rawPassword = RawPassword.of(passwordValue);
            var encodedPassword = EncodedPassword.of("{bcrypt}$2a$10$encodedPassword");
            var savedUser = User.activeUser(UserId.of(UUID.randomUUID()), username, USER);

            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // When
            var result = createUserService.createUser(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getRole()).isEqualTo(USER);

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

    }

}
