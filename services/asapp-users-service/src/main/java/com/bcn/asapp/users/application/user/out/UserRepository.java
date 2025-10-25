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

package com.bcn.asapp.users.application.user.out;

import java.util.Collection;
import java.util.Optional;

import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Repository port for user persistence operations.
 * <p>
 * Defines the contract for storing and retrieving {@link User} entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface UserRepository {

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return an {@link Optional} containing the {@link User} if found, {@link Optional#empty} otherwise
     */
    Optional<User> findById(UserId userId);

    /**
     * Retrieves all users from the repository.
     *
     * @return a {@link Collection} of all {@link User} entities
     */
    Collection<User> findAll();

    /**
     * Saves a user to the repository.
     * <p>
     * If the user is new (without ID), it will be persisted and returned with a generated ID.
     * <p>
     * If the user is reconstructed (with ID), it will be updated.
     *
     * @param user the {@link User} to save
     * @return the saved {@link User} with a persistent ID
     */
    User save(User user);

    /**
     * Deletes a user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return {@code true} if the user was deleted, {@code false} if not found
     */
    Boolean deleteById(UserId userId);

}
