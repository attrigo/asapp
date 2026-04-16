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

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.users.application.ApplicationService;
import com.bcn.asapp.users.application.user.in.CreateUserUseCase;
import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserFactory;

/**
 * Application service responsible for orchestrating user creation operations.
 * <p>
 * Coordinates the user creation workflow including domain object creation and persistence to the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Creates {@link User} domain object via {@link UserFactory}</li>
 * <li>Persists user to repository</li>
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
     * @param userRepository the repository for user data access
     */
    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user based on the provided command.
     * <p>
     * Orchestrates the complete user creation workflow: domain object creation and persistence.
     *
     * @param command the {@link CreateUserCommand} containing user registration data
     * @return the created {@link User} with a generated persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid (blank names, invalid email format, etc.)
     */
    @Override
    @Transactional
    public User createUser(CreateUserCommand command) {
        var user = UserFactory.create(command.firstName(), command.lastName(), command.email(), command.phoneNumber());

        return persistUser(user);
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
