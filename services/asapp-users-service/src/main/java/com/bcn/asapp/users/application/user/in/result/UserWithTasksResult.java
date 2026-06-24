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
 * @param user                  the {@link User} domain entity containing core user information
 * @param taskIds               a {@link List} of task UUIDs associated with the user, or {@code null} when tasks are unavailable
 * @param tasksServiceAvailable whether the task references were successfully retrieved; {@code false} indicates degraded data
 * @since 0.2.0
 * @author attrigo
 */
public record UserWithTasksResult(
        User user,
        List<UUID> taskIds,
        boolean tasksServiceAvailable
) {

    public UserWithTasksResult {
        validateUserIsNotNull(user);
        validateTaskIdsConsistency(taskIds, tasksServiceAvailable);
    }

    /**
     * Creates a result for a user whose tasks were retrieved successfully.
     *
     * @param user    the user entity
     * @param taskIds the retrieved task identifiers (never {@code null}; empty when the user has no tasks)
     * @return an available {@code UserWithTasksResult}
     */
    public static UserWithTasksResult available(User user, List<UUID> taskIds) {
        return new UserWithTasksResult(user, taskIds, true);
    }

    /**
     * Creates a degraded result for a user whose tasks could not be retrieved because tasks-service is unavailable.
     *
     * @param user the user entity
     * @return an unavailable {@code UserWithTasksResult} with {@code null} task identifiers
     */
    public static UserWithTasksResult unavailable(User user) {
        return new UserWithTasksResult(user, null, false);
    }

    private static void validateUserIsNotNull(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
    }

    private static void validateTaskIdsConsistency(List<UUID> taskIds, boolean tasksServiceAvailable) {
        if (tasksServiceAvailable && taskIds == null) {
            throw new IllegalArgumentException("Task IDs list must not be null when tasks are available");
        }
        if (!tasksServiceAvailable && taskIds != null) {
            throw new IllegalArgumentException("Task IDs list must be null when tasks are unavailable");
        }
    }

}
