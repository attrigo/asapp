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

package com.bcn.asapp.tasks.application.task.in.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.ReadTaskUseCase;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.UserId;

/**
 * Application service responsible for orchestrate task retrieval operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class ReadTaskService implements ReadTaskUseCase {

    private final TaskRepository taskRepository;

    /**
     * Constructs a new {@code ReadTaskService} with required dependencies.
     *
     * @param taskRepository the task repository
     */
    public ReadTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Retrieves a task by their unique identifier.
     *
     * @param id the task's unique identifier
     * @return an {@link Optional} containing the {@link Task} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Optional<Task> getTaskById(UUID id) {
        var taskId = TaskId.of(id);

        return this.taskRepository.findById(taskId);
    }

    /**
     * Retrieves all tasks for a specific user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of {@link Task} entities belonging to the user
     * @throws IllegalArgumentException if the userId is invalid
     */
    @Override
    public List<Task> getTasksByUserId(UUID userId) {
        var userIdObj = UserId.of(userId);

        return taskRepository.findByUserId(userIdObj)
                             .stream()
                             .toList();
    }

    /**
     * Retrieves all tasks from the system.
     *
     * @return a {@link List} of all {@link Task} entities
     */
    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll()
                             .stream()
                             .toList();
    }

}
