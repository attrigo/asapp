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

import java.util.List;
import java.util.UUID;

import com.bcn.asapp.users.domain.user.UserId;

/**
 * Gateway port for accessing task information from tasks-service.
 * <p>
 * Defines the contract for retrieving task-related data that exists outside the User bounded context.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TasksGateway {

    /**
     * Retrieves all task identifiers associated with a specific user.
     * <p>
     * This method queries the tasks-service to obtain references to all tasks belonging to the specified user. Only task identifiers are returned, maintaining
     * loose coupling between bounded contexts.
     * <p>
     * The implementation should handle communication failures gracefully, either by returning an empty list or propagating an appropriate exception based on
     * the desired error handling strategy.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, or an empty list if the user has no tasks or if retrieval fails
     * @throws IllegalArgumentException if userId is {@code null}
     */
    List<UUID> getTaskIdsByUserId(UserId userId);

}
