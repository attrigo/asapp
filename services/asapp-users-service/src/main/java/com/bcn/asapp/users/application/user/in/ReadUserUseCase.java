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

package com.bcn.asapp.users.application.user.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.users.domain.user.User;

/**
 * Use case for retrieving user information from the system.
 * <p>
 * Defines the contract for user retrieval operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface ReadUserUseCase {

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the user's unique identifier
     * @return an {@link Optional} containing the {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    Optional<User> getUserById(UUID id);

    /**
     * Retrieves all users from the system.
     *
     * @return a {@link List} of all {@link User} entities
     */
    List<User> getAllUsers();

}
