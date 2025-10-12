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

import java.util.Optional;

import com.bcn.asapp.authentication.application.ApplicationService;
import com.bcn.asapp.authentication.application.user.in.UpdateUserUseCase;
import com.bcn.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Application service responsible for orchestrate user updates operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class UpdateUserService implements UpdateUserUseCase {

    private final PasswordService passwordService;

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code UpdateUserService} with required dependencies.
     *
     * @param passwordService the password encoding service
     * @param userRepository  the user repository
     */
    public UpdateUserService(PasswordService passwordService, UserRepository userRepository) {
        this.passwordService = passwordService;
        this.userRepository = userRepository;
    }

    /**
     * Updates an existing user based on the provided command.
     * <p>
     * Retrieves the user by ID, validates and transforms command data into domain objects, encodes the new password, updates the user, and persists the
     * changes.
     *
     * @param command the {@link UpdateUserCommand} containing user update data
     * @return an {@link Optional} containing the updated {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if username, password, or role is invalid
     */
    @Override
    public Optional<User> updateUserById(UpdateUserCommand command) {
        var userId = UserId.of(command.userId());

        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }
        var currentUser = optionalUser.get();

        var newUsername = Username.of(command.username());
        var newRawPassword = RawPassword.of(command.password());
        var newRole = Role.valueOf(command.role());

        var encodedPassword = passwordService.encode(newRawPassword);

        currentUser.update(newUsername, encodedPassword, newRole);

        var userUpdated = userRepository.save(currentUser);

        return Optional.of(userUpdated);
    }

}
