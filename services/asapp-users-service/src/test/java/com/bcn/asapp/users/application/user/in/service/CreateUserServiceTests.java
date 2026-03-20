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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.User;

/**
 * Tests {@link CreateUserService} user creation and persistence.
 * <p>
 * Coverage:
 * <li>Persistence failures propagate without completing creation workflow</li>
 * <li>Successful creation persists user with assigned identity</li>
 * <li>Domain constraints validated before persistence</li>
 */
@ExtendWith(MockitoExtension.class)
class CreateUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateUserService createUserService;

    @Nested
    class CreateUser {

        @Test
        void ReturnsCreatedUser_ValidUser() {
            // Given
            var user = aUser();
            var firstName = user.getFirstName();
            var lastName = user.getLastName();
            var email = user.getEmail();
            var phoneNumber = user.getPhoneNumber();
            var command = new CreateUserCommand(firstName.value(), lastName.value(), email.value(), phoneNumber.value());

            given(userRepository.save(any(User.class))).willReturn(user);

            // When
            var actual = createUserService.createUser(command);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("created user").isNotNull();
                softly.assertThat(actual.getId()).as("ID").isNotNull();
                softly.assertThat(actual.getFirstName()).as("first name").isEqualTo(firstName);
                softly.assertThat(actual.getLastName()).as("last name").isEqualTo(lastName);
                softly.assertThat(actual.getEmail()).as("email").isEqualTo(email);
                softly.assertThat(actual.getPhoneNumber()).as("phone number").isEqualTo(phoneNumber);
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

        @Test
        void ThrowsRuntimeException_UserPersistenceFails() {
            // Given
            var firstName = "FirstName";
            var lastName = "LastName";
            var email = "user@asapp.com";
            var phoneNumber = "555 555 555";
            var command = new CreateUserCommand(firstName, lastName, email, phoneNumber);

            willThrow(new RuntimeException("Database connection failed")).given(userRepository)
                                                                         .save(any(User.class));

            // When
            var actual = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

    }

}
