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

package com.bcn.asapp.clients.tasks;

import java.util.List;
import java.util.UUID;

/**
 * Client interface for interacting with the Tasks Service.
 * <p>
 * Provides methods to retrieve task information from the Tasks Service.
 * <p>
 * This client abstracts the HTTP communication details and provides a clean API for consuming task-related data.
 * <p>
 * Implementations of this interface should handle:
 * <ul>
 * <li>REST API communication</li>
 * <li>JWT token propagation for authentication</li>
 * <li>Error handling and graceful degradation</li>
 * <li>Response mapping</li>
 * </ul>
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TasksClient {

    /**
     * Retrieves all task identifiers associated with a specific user.
     * <p>
     * This method calls the Tasks Service to fetch all tasks belonging to the specified user and returns only their identifiers. This allows consuming services
     * to obtain task references without loading full task details.
     * <p>
     * In case of communication failures or service unavailability, implementations should handle errors gracefully by returning {@code null} or propagating an
     * appropriate exception based on the implementation strategy.
     *
     * @param userId the unique identifier of the user whose task IDs should be retrieved
     * @return a {@link List} of task UUIDs belonging to the user, an empty list if the user has no tasks or {@code null} if an error occurs
     * @throws IllegalArgumentException if userId is {@code null}
     */
    List<UUID> getTaskIdsByUserId(UUID userId);

}
