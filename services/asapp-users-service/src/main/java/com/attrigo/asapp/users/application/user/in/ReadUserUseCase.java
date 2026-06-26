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

package com.attrigo.asapp.users.application.user.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.attrigo.asapp.users.application.user.in.result.UserWithTasksResult;
import com.attrigo.asapp.users.domain.user.User;

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
     * Retrieves a user by their unique identifier, enriched with task references.
     * <p>
     * This method combines user information from the User bounded context with task identifiers from the Task bounded context. It returns a
     * {@link UserWithTasksResult} that contains the user entity along with a list of task IDs associated with that user.
     * <p>
     * If tasks-service is unavailable, the result is returned with the tasks marked unavailable (no task identifiers), allowing graceful degradation.
     *
     * @param id the user's unique identifier
     * @return an {@link Optional} containing the {@link UserWithTasksResult} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    Optional<UserWithTasksResult> getUserById(UUID id);

    /**
     * Retrieves users by their unique identifiers.
     *
     * @param ids the list of user identifiers; duplicates are deduped
     * @return a {@link List} of {@link User} entities found; missing ids are silently omitted
     * @throws IllegalArgumentException if any id is invalid
     */
    List<User> getUsersByIds(List<UUID> ids);

    /**
     * Retrieves all users from the system.
     *
     * @return a {@link List} of all {@link User} entities
     */
    List<User> getAllUsers();

}
