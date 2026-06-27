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

package com.attrigo.asapp.users.application.user.in.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.attrigo.asapp.users.application.ApplicationService;
import com.attrigo.asapp.users.application.user.TasksUnavailableException;
import com.attrigo.asapp.users.application.user.in.ReadUserUseCase;
import com.attrigo.asapp.users.application.user.in.result.UserWithTasksResult;
import com.attrigo.asapp.users.application.user.out.TasksGateway;
import com.attrigo.asapp.users.application.user.out.UserRepository;
import com.attrigo.asapp.users.domain.user.User;
import com.attrigo.asapp.users.domain.user.UserId;

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
     * If tasks-service is unavailable, the result is returned with tasks marked unavailable (no task identifiers), allowing graceful degradation.
     *
     * @param id the user's unique identifier
     * @return an {@link Optional} containing the {@link UserWithTasksResult} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Optional<UserWithTasksResult> getUserById(UUID id) {
        var userId = UserId.of(id);

        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        var user = optionalUser.get();
        var result = enrichUserWithTasks(user);
        return Optional.of(result);
    }

    /**
     * Retrieves users by their unique identifiers.
     * <p>
     * Duplicate identifiers are ignored.
     *
     * @param ids the identifiers of the users to retrieve
     * @return a {@link List} of {@link User} entities found, or an empty list if none match
     * @throws IllegalArgumentException if any id is invalid
     */
    @Override
    public List<User> getUsersByIds(List<UUID> ids) {
        var userIds = ids.stream()
                         .map(UserId::of)
                         .collect(Collectors.toUnmodifiableSet());

        return userRepository.findByIds(userIds)
                             .stream()
                             .toList();
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

    /**
     * Enriches user with task identifiers from tasks-service.
     * <p>
     * Queries the tasks-service via gateway to retrieve associated task IDs and combines with user data.
     * <p>
     * If tasks-service is unavailable, returns the user with tasks marked unavailable (no task identifiers).
     *
     * @param user the user to enrich
     * @return a {@link UserWithTasksResult} containing user and associated task IDs, or a degraded result if tasks-service is unavailable
     */
    private UserWithTasksResult enrichUserWithTasks(User user) {
        try {
            var taskIds = tasksGateway.getTaskIdsByUserId(user.getId());
            return UserWithTasksResult.available(user, taskIds);
        } catch (TasksUnavailableException _) {
            return UserWithTasksResult.unavailable(user);
        }
    }

}
