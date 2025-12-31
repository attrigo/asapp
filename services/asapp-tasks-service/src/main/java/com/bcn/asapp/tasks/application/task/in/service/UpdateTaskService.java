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

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.UpdateTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.command.UpdateTaskCommand;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;

/**
 * Application service responsible for orchestrating task update operations.
 * <p>
 * Coordinates the task update workflow including task retrieval, parameter transformation, domain object update, and persistence.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Retrieves existing task from repository by ID</li>
 * <li>Returns empty if task not found</li>
 * <li>Transforms command parameters into domain value objects</li>
 * <li>Updates task domain object via {@link Task#update}</li>
 * <li>Persists updated task to repository</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class UpdateTaskService implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;

    /**
     * Constructs a new {@code UpdateTaskService} with required dependencies.
     *
     * @param taskRepository the repository for task persistence operations
     */
    public UpdateTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Updates an existing task based on the provided command.
     * <p>
     * Orchestrates the complete task update workflow: retrieval, validation, transformation, domain update, and persistence.
     *
     * @param command the {@link UpdateTaskCommand} containing task update data
     * @return an {@link Optional} containing the updated {@link Task} if found, {@link Optional#empty()} if task does not exist
     * @throws IllegalArgumentException if any data within the command is invalid (blank title, invalid task ID, invalid dates, etc.)
     */
    @Override
    @Transactional
    public Optional<Task> updateTaskById(UpdateTaskCommand command) {
        var taskId = TaskId.of(command.taskId());

        var optionalTask = retrieveTask(taskId);
        if (optionalTask.isEmpty()) {
            return Optional.empty();
        }

        var task = optionalTask.get();
        updateTaskDomain(task, command);

        var updatedTask = persistTask(task);
        return Optional.of(updatedTask);
    }

    /**
     * Retrieves task from repository by identifier.
     *
     * @param taskId the task identifier
     * @return an {@link Optional} containing the task if found
     */
    private Optional<Task> retrieveTask(TaskId taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * Updates task domain object with new values from command.
     * <p>
     * Transforms command parameters into value objects and delegates to domain update method.
     *
     * @param task    the task domain object to update
     * @param command the command containing new values
     * @throws IllegalArgumentException if any value object validation fails
     */
    private void updateTaskDomain(Task task, UpdateTaskCommand command) {
        var newUserId = UserId.of(command.userId());
        var newTitle = Title.of(command.title());
        var newDescription = Description.ofNullable(command.description());
        var newStartDate = StartDate.ofNullable(command.startDate());
        var newEndDate = EndDate.ofNullable(command.endDate());

        task.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);
    }

    /**
     * Persists updated task to repository.
     *
     * @param task the task domain object to persist
     * @return the persisted {@link Task}
     */
    private Task persistTask(Task task) {
        return taskRepository.save(task);
    }

}
