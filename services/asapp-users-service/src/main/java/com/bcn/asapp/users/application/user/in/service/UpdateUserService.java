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
 * Application service responsible for orchestrate user updates operations.
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
     * Retrieves the user by ID, validates and transforms command data into domain objects, updates the user, and persists the changes.
     *
     * @param command the {@link UpdateUserCommand} containing user update data
     * @return an {@link Optional} containing the updated {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    @Override
    public Optional<User> updateUserById(UpdateUserCommand command) {
        var userId = UserId.of(command.userId());

        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }
        var currentUser = optionalUser.get();

        var newFirstName = FirstName.of(command.firstName());
        var newLastName = LastName.of(command.lastName());
        var newEmail = Email.of(command.email());
        var newPhoneNumber = PhoneNumber.of(command.phoneNumber());

        currentUser.update(newFirstName, newLastName, newEmail, newPhoneNumber);

        var userUpdated = userRepository.save(currentUser);

        return Optional.of(userUpdated);
    }

}
