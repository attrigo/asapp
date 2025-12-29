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
import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Application service responsible for orchestrating user update operations.
 * <p>
 * Coordinates the complete user update workflow including retrieval, password encoding, user update, and persistence.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Retrieves the user from the repository by ID</li>
 * <li>Encodes raw password using {@link PasswordService}</li>
 * <li>Updates the user with new credentials</li>
 * <li>Persists the updated user to the repository</li>
 * </ol>
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
     * Orchestrates the complete user update workflow: retrieval, password encoding, user update, and persistence.
     *
     * @param command the {@link UpdateUserCommand} containing user update data
     * @return an {@link Optional} containing the updated {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    @Override
    public Optional<User> updateUserById(UpdateUserCommand command) {
        var userId = UserId.of(command.userId());

        var optionalUser = retrieveUser(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        var currentUser = optionalUser.get();
        var newUsername = Username.of(command.username());
        var newRawPassword = RawPassword.of(command.password());
        var newRole = Role.valueOf(command.role());

        var encodedPassword = encodePassword(newRawPassword);

        updateUserData(currentUser, newUsername, encodedPassword, newRole);

        var updatedUser = persistUser(currentUser);
        return Optional.of(updatedUser);
    }

    /**
     * Retrieves user from repository by ID.
     *
     * @param userId the user's unique identifier
     * @return Optional containing the user if found
     */
    private Optional<User> retrieveUser(UserId userId) {
        return userRepository.findById(userId);
    }

    /**
     * Encodes raw password using password service.
     *
     * @param rawPassword the raw password
     * @return encoded password
     */
    private EncodedPassword encodePassword(RawPassword rawPassword) {
        return passwordService.encode(rawPassword);
    }

    /**
     * Updates user with new data.
     *
     * @param user            the user to update
     * @param username        the new username
     * @param encodedPassword the new encoded password
     * @param role            the new role
     */
    private void updateUserData(User user, Username username, EncodedPassword encodedPassword, Role role) {
        user.update(username, encodedPassword, role);
    }

    /**
     * Persists user to repository.
     *
     * @param user the user to persist
     * @return the persisted user
     */
    private User persistUser(User user) {
        return userRepository.save(user);
    }

}
