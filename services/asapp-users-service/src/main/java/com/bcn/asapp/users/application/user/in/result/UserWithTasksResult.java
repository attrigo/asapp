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

package com.bcn.asapp.users.application.user.in.result;

import java.util.List;
import java.util.UUID;

import com.bcn.asapp.users.domain.user.User;

/**
 * Result object containing user information enriched with task references.
 * <p>
 * Represents the result of a query that combines user data from the User bounded context with task identifiers from the Task bounded context. It serves as a
 * read model that aggregates information from these sources while maintaining loose coupling between bounded contexts.
 *
 * @param user    the {@link User} domain entity containing core user information
 * @param taskIds a {@link List} of task UUIDs associated with the user, or an empty list if the user has no tasks
 * @since 0.2.0
 * @author attrigo
 */
public record UserWithTasksResult(
        User user,
        List<UUID> taskIds
) {

    /**
     * Constructs a new {@code UserWithTasksResult} with validation.
     *
     * @param user    the user entity
     * @param taskIds the list of task identifiers
     * @throws IllegalArgumentException if user is {@code null} or taskIds is {@code null}
     */
    public UserWithTasksResult {
        validateUserIsNotNull(user);
        validateTaskIdsIsNotNull(taskIds);
    }

    /**
     * Validates that the user is not {@code null}.
     *
     * @param user the user to validate
     * @throws IllegalArgumentException if the user is {@code null}
     */
    private static void validateUserIsNotNull(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
    }

    /**
     * Validates that the task IDs list is not {@code null}.
     *
     * @param taskIds the task IDs list to validate
     * @throws IllegalArgumentException if the task IDs list is {@code null}
     */
    private static void validateTaskIdsIsNotNull(List<UUID> taskIds) {
        if (taskIds == null) {
            throw new IllegalArgumentException("Task IDs list must not be null");
        }
    }

}
