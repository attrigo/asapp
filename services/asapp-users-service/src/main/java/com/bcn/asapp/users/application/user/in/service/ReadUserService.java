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

package com.bcn.asapp.users.application.user.in.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.users.application.ApplicationService;
import com.bcn.asapp.users.application.user.in.ReadUserUseCase;
import com.bcn.asapp.users.application.user.in.result.UserWithTasksResult;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Application service responsible for orchestrating user retrieval operations.
 * <p>
 * Coordinates user query operations including single user retrieval and bulk user listing.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class ReadUserService implements ReadUserUseCase {

    private final UserRepository userRepository;

    private final TasksGateway tasksGateway;

    /**
     * Constructs a new {@code ReadUserService} with required dependencies.
     *
     * @param userRepository the repository for user data access
     * @param tasksGateway   the task gateway for accessing task information from tasks-service
     */
    public ReadUserService(UserRepository userRepository, TasksGateway tasksGateway) {
        this.userRepository = userRepository;
        this.tasksGateway = tasksGateway;
    }

    /**
     * Retrieves a user by their unique identifier, enriched with task references.
     * <p>
     * Orchestrates data retrieval from multiple sources: retrieves user from repository and enriches with task IDs from tasks-service.
     * <p>
     * <strong>Orchestration Flow:</strong>
     * <ol>
     * <li>Retrieves user from repository by ID</li>
     * <li>Returns empty if user not found</li>
     * <li>Enriches user with task IDs from tasks-service via gateway</li>
     * <li>Wraps result in {@link UserWithTasksResult}</li>
     * </ol>
     * <p>
     * If task retrieval fails or the tasks-service is unavailable, the result will contain the user with an empty task list, allowing graceful degradation.
     *
     * @param id the user's unique identifier
     * @return an {@link Optional} containing the {@link UserWithTasksResult} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Optional<UserWithTasksResult> getUserById(UUID id) {
        var userId = UserId.of(id);

        var optionalUser = retrieveUser(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        var user = optionalUser.get();
        var result = enrichUserWithTasks(user);
        return Optional.of(result);
    }

    /**
     * Retrieves user from repository by identifier.
     *
     * @param userId the user identifier
     * @return an {@link Optional} containing the {@link User} if found, {@link Optional#empty} otherwise
     */
    private Optional<User> retrieveUser(UserId userId) {
        return userRepository.findById(userId);
    }

    /**
     * Enriches user with task identifiers from tasks-service.
     * <p>
     * Queries the tasks-service via gateway to retrieve associated task IDs and combines with user data.
     * <p>
     * If task retrieval fails, returns user with empty task list (graceful degradation).
     *
     * @param user the user to enrich
     * @return a {@link UserWithTasksResult} containing user and associated task IDs
     */
    private UserWithTasksResult enrichUserWithTasks(User user) {
        var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
        return new UserWithTasksResult(user, taskIds);
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
