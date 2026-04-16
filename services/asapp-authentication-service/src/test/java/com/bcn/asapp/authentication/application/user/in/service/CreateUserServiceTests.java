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

import static com.bcn.asapp.authentication.domain.user.Role.USER;
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

/**
 * Tests {@link CreateUserService} password encoding and user persistence.
 * <p>
 * Coverage:
 * <li>Password encoding failures propagate without completing creation workflow</li>
 * <li>Persistence failures propagate without completing creation workflow</li>
 * <li>Successful creation encodes password, persists user, and returns assigned identity</li>
 */
@ExtendWith(MockitoExtension.class)
class CreateUserServiceTests {

    @Mock
    private PasswordService passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateUserService createUserService;

    @Nested
    class CreateUser {

        @Test
        void ReturnsCreatedUser_ValidUser() {
            // Given
            var user = anActiveUser();
            var username = user.getUsername();
            var passwordValue = "TEST@09_password?!";
            var role = user.getRole();
            var command = new CreateUserCommand(username.value(), passwordValue, role.name());
            var rawPassword = RawPassword.of(passwordValue);
            var encodedPassword = EncodedPassword.of("{bcrypt}$2a$10$encodedPassword");

            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(user);

            // When
            var actual = createUserService.createUser(command);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("created user").isNotNull();
                softly.assertThat(actual.getId()).as("ID").isNotNull();
                softly.assertThat(actual.getUsername()).as("username").isEqualTo(username);
                softly.assertThat(actual.getPassword()).as("password").isNull();
                softly.assertThat(actual.getRole()).as("role").isEqualTo(USER);
                // @formatter:on
            });

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_PasswordEncodingFails() {
            // Given
            var username = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var role = "USER";
            var command = new CreateUserCommand(username, passwordValue, role);
            var rawPassword = RawPassword.of(passwordValue);

            willThrow(new RuntimeException("Password encoding failed")).given(passwordEncoder)
                                                                       .encode(rawPassword);

            // When
            var actual = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Password encoding failed");

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(never())
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserPersistenceFails() {
            // Given
            var username = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var role = "USER";
            var command = new CreateUserCommand(username, passwordValue, role);
            var rawPassword = RawPassword.of(passwordValue);
            var encodedPassword = EncodedPassword.of("{bcrypt}$2a$10$encodedPassword");

            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .save(any(User.class));

            // When
            var actual = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(passwordEncoder).should(times(1))
                                 .encode(rawPassword);
            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

    }

}
