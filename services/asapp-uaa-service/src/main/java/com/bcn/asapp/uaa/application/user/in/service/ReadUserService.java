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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.uaa.application.ApplicationService;
import com.bcn.asapp.uaa.application.user.in.ReadUserUseCase;
import com.bcn.asapp.uaa.application.user.out.UserRepository;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.domain.user.UserId;

/**
 * Application service responsible for orchestrate user retrieval operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class ReadUserService implements ReadUserUseCase {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code ReadUserService} with required dependencies.
     *
     * @param userRepository the user repository
     */
    public ReadUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the user's unique identifier
     * @return an {@link Optional} containing the {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Optional<User> getUserById(UUID id) {
        var userId = UserId.of(id);

        return this.userRepository.findById(userId);
    }

    /**
     * Retrieves all users from the system.
     *
     * @return a {@link List} of all {@link User} entities
     */
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll()
                             .stream()
                             .toList();
    }

}
