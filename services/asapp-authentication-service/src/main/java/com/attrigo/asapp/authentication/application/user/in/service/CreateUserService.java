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

package com.attrigo.asapp.authentication.application.user.in.service;

import com.attrigo.asapp.authentication.application.ApplicationService;
import com.attrigo.asapp.authentication.application.user.in.CreateUserUseCase;
import com.attrigo.asapp.authentication.application.user.in.command.CreateUserCommand;
import com.attrigo.asapp.authentication.application.user.out.UserRepository;
import com.attrigo.asapp.authentication.domain.user.EncodedPassword;
import com.attrigo.asapp.authentication.domain.user.PasswordService;
import com.attrigo.asapp.authentication.domain.user.RawPassword;
import com.attrigo.asapp.authentication.domain.user.Role;
import com.attrigo.asapp.authentication.domain.user.User;
import com.attrigo.asapp.authentication.domain.user.Username;

/**
 * Application service responsible for orchestrating user creation operations.
 * <p>
 * Coordinates the user creation workflow including command transformation, password encoding, user instantiation, and persistence.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Encodes raw password</li>
 * <li>Creates an inactive {@link User}</li>
 * <li>Persists user to repository via</li>
 * </ol>
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
     * @param passwordEncoder the password encoding service for securing user passwords
     * @param userRepository  the repository for user data access
     */
    public CreateUserService(PasswordService passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserCommand command) {
        var username = Username.of(command.username());
        var rawPassword = RawPassword.of(command.password());
        var role = Role.valueOf(command.role());

        var encodedPassword = encodePassword(rawPassword);
        var newUser = createInactiveUser(username, encodedPassword, role);

        return persistUser(newUser);
    }

    /**
     * Encodes raw password using password service.
     *
     * @param rawPassword the raw password
     * @return the {@link EncodedPassword}
     */
    private EncodedPassword encodePassword(RawPassword rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Creates an inactive user with provided credentials.
     *
     * @param username        the username
     * @param encodedPassword the encoded password
     * @param role            the user role
     * @return the inactive {@link User}
     */
    private User createInactiveUser(Username username, EncodedPassword encodedPassword, Role role) {
        return User.inactiveUser(username, encodedPassword, role);
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
