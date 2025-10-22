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

import com.bcn.asapp.users.application.ApplicationService;
import com.bcn.asapp.users.application.user.in.CreateUserUseCase;
import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;

/**
 * Application service responsible for orchestrate user creation operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code CreateUserService} with required dependencies.
     *
     * @param userRepository the user repository
     */
    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user based on the provided command.
     * <p>
     * Validates and transforms command data into domain objects, creates an inactive user, and persists it to the repository.
     *
     * @param command the {@link CreateUserCommand} containing user registration data
     * @return the created {@link User} with a persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    @Override
    public User createUser(CreateUserCommand command) {
        var firstName = FirstName.of(command.firstName());
        var lastName = LastName.of(command.lastName());
        var email = Email.of(command.email());
        var phoneNumber = PhoneNumber.of(command.phoneNumber());

        var newUser = User.newUser(firstName, lastName, email, phoneNumber);

        return userRepository.save(newUser);
    }

}
