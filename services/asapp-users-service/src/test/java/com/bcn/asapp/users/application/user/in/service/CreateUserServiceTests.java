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
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataAccessException;

import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateUserService createUserService;

    // Specific test data
    private final String firstNameValue = "FirstName";

    private final String lastNameValue = "LastName";

    private final String emailValue = "user@asapp.com";

    private final String phoneNumberValue = "555 555 555";

    @Nested
    class CreateUser {

        @Test
        void ThrowsDataAccessException_SaveUserFails() {
            // Given
            var command = new CreateUserCommand(firstNameValue, lastNameValue, emailValue, phoneNumberValue);

            willThrow(new DataAccessException("Database connection failed") {}).given(userRepository)
                                                                               .save(any(User.class));

            // When
            var thrown = catchThrowable(() -> createUserService.createUser(command));

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(userRepository).should(times(1))
                                .save(any(User.class));
        }

        @Test
        void ReturnsCreatedUser_CreationSucceeds() {
            // Given
            var firstName = FirstName.of(firstNameValue);
            var lastName = LastName.of(lastNameValue);
            var email = Email.of(emailValue);
            var phoneNumber = PhoneNumber.of(phoneNumberValue);
            var savedUser = User.reconstitute(UserId.of(UUID.randomUUID()), firstName, lastName, email, phoneNumber);
            var command = new CreateUserCommand(firstNameValue, lastNameValue, emailValue, phoneNumberValue);

            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // When
            var result = createUserService.createUser(command);

            // Then
            then(userRepository).should(times(1))
                                .save(any(User.class));
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(firstName);
            assertThat(result.getLastName()).isEqualTo(lastName);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        }

    }

}
