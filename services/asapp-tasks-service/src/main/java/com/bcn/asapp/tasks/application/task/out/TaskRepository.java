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

package com.bcn.asapp.tasks.application.task.out;

import java.util.Collection;
import java.util.Optional;

import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.UserId;

/**
 * Repository port for task persistence operations.
 * <p>
 * Defines the contract for storing and retrieving {@link Task} entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TaskRepository {

    /**
     * Finds a task by their unique identifier.
     *
     * @param taskId the task's unique identifier
     * @return an {@link Optional} containing the {@link Task} if found, {@link Optional#empty} otherwise
     */
    Optional<Task> findById(TaskId taskId);

    /**
     * Finds all tasks by their user's unique identifier.
     *
     * @param userId the user's unique identifier
     * @return a {@link Collection} of {@link Task} entities belonging to the user
     */
    Collection<Task> findByUserId(UserId userId);

    /**
     * Retrieves all tasks from the repository.
     *
     * @return a {@link Collection} of all {@link Task} entities
     */
    Collection<Task> findAll();

    /**
     * Saves a task to the repository.
     * <p>
     * If the task is new (without ID), it will be persisted and returned with a generated ID.
     * <p>
     * If the task is reconstructed (with ID), it will be updated.
     *
     * @param task the {@link Task} to save
     * @return the saved {@link Task} with a persistent ID
     */
    Task save(Task task);

    /**
     * Deletes a task by their unique identifier.
     *
     * @param taskId the task's unique identifier
     * @return {@code true} if the task was deleted, {@code false} if not found
     */
    Boolean deleteById(TaskId taskId);

}
