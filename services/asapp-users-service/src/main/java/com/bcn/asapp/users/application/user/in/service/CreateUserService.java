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

import org.springframework.transaction.annotation.Transactional;

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
 * Application service responsible for orchestrating user creation operations.
 * <p>
 * Coordinates the user creation workflow including parameter transformation, domain object creation, and persistence to the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Transforms command parameters into domain value objects</li>
 * <li>Creates user domain object via {@link User#create}</li>
 * <li>Persists user to repository via {@link UserRepository#save}</li>
 * </ol>
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
     * Orchestrates the complete user creation workflow: parameter transformation, domain object creation, and persistence.
     *
     * @param command the {@link CreateUserCommand} containing user registration data
     * @return the created {@link User} with a generated persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid (blank names, invalid email format, etc.)
     */
    @Override
    @Transactional
    public User createUser(CreateUserCommand command) {
        var user = mapCommandToDomain(command);

        return persistUser(user);
    }

    /**
     * Creates a user domain object from command data.
     * <p>
     * Maps command parameters into domain value objects and invokes the user factory method.
     *
     * @param command the create user command containing raw data
     * @return the created {@link User} domain object without persistence ID
     * @throws IllegalArgumentException if any value object validation fails
     */
    private User mapCommandToDomain(CreateUserCommand command) {
        var firstName = FirstName.of(command.firstName());
        var lastName = LastName.of(command.lastName());
        var email = Email.of(command.email());
        var phoneNumber = PhoneNumber.of(command.phoneNumber());

        return User.create(firstName, lastName, email, phoneNumber);
    }

    /**
     * Persists user to the repository.
     *
     * @param user the user domain object to persist
     * @return the persisted {@link User} with generated ID
     */
    private User persistUser(User user) {
        return userRepository.save(user);
    }

}
