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

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.users.application.ApplicationService;
import com.bcn.asapp.users.application.user.in.UpdateUserUseCase;
import com.bcn.asapp.users.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Application service responsible for orchestrating user update operations.
 * <p>
 * Coordinates the user update workflow including user retrieval, parameter transformation, domain object update, and persistence.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Retrieves existing user from repository by ID</li>
 * <li>Returns empty if user not found</li>
 * <li>Transforms command parameters into domain value objects</li>
 * <li>Updates user domain object via {@link User#update}</li>
 * <li>Persists updated user to repository</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code UpdateUserService} with required dependencies.
     *
     * @param userRepository the user repository
     */
    public UpdateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Updates an existing user based on the provided command.
     * <p>
     * Orchestrates the complete user update workflow: retrieval, validation, transformation, domain update, and persistence.
     *
     * @param command the {@link UpdateUserCommand} containing user update data
     * @return an {@link Optional} containing the updated {@link User} if found, {@link Optional#empty()} if user does not exist
     * @throws IllegalArgumentException if any data within the command is invalid (blank names, invalid email format, invalid user ID, etc.)
     */
    @Override
    @Transactional
    public Optional<User> updateUserById(UpdateUserCommand command) {
        var userId = UserId.of(command.userId());

        var optionalUser = retrieveUser(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        var user = optionalUser.get();
        updateUserDomain(user, command);

        var updatedUser = persistUser(user);
        return Optional.of(updatedUser);
    }

    /**
     * Retrieves user from repository by identifier.
     *
     * @param userId the user identifier
     * @return an {@link Optional} containing the user if found
     */
    private Optional<User> retrieveUser(UserId userId) {
        return userRepository.findById(userId);
    }

    /**
     * Updates user domain object with new values from command.
     * <p>
     * Transforms command parameters into value objects and delegates to domain update method.
     *
     * @param user    the user domain object to update
     * @param command the command containing new values
     * @throws IllegalArgumentException if any value object validation fails
     */
    private void updateUserDomain(User user, UpdateUserCommand command) {
        var newFirstName = FirstName.of(command.firstName());
        var newLastName = LastName.of(command.lastName());
        var newEmail = Email.of(command.email());
        var newPhoneNumber = PhoneNumber.of(command.phoneNumber());

        user.update(newFirstName, newLastName, newEmail, newPhoneNumber);
    }

    /**
     * Persists updated user to repository.
     *
     * @param user the user domain object to persist
     * @return the persisted {@link User}
     */
    private User persistUser(User user) {
        return userRepository.save(user);
    }

}
