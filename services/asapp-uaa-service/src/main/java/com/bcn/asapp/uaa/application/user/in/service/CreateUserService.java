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

package com.bcn.asapp.uaa.application.user.in.service;

import com.bcn.asapp.uaa.application.ApplicationService;
import com.bcn.asapp.uaa.application.user.in.CreateUserUseCase;
import com.bcn.asapp.uaa.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.uaa.application.user.out.UserRepository;
import com.bcn.asapp.uaa.domain.user.PasswordService;
import com.bcn.asapp.uaa.domain.user.RawPassword;
import com.bcn.asapp.uaa.domain.user.Role;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.domain.user.Username;

/**
 * Application service responsible for orchestrate user creation operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class CreateUserService implements CreateUserUseCase {

    private final PasswordService passwordEncoder;

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code CreateUserService} with required dependencies.
     *
     * @param passwordEncoder the password encoding service
     * @param userRepository  the user repository
     */
    public CreateUserService(PasswordService passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user based on the provided command.
     * <p>
     * Validates and transforms command data into domain objects, encodes the password, creates an inactive user, and persists it to the repository.
     *
     * @param command the {@link CreateUserCommand} containing user registration data
     * @return the created {@link User} with a persistent ID
     * @throws IllegalArgumentException if username, password, or role is invalid
     */
    @Override
    public User createUser(CreateUserCommand command) {
        var username = Username.of(command.username());
        var password = RawPassword.of(command.password());
        var role = Role.valueOf(command.role());

        var encodedPassword = passwordEncoder.encode(password);

        var newUser = User.inactiveUser(username, encodedPassword, role);

        return userRepository.save(newUser);
    }

}
