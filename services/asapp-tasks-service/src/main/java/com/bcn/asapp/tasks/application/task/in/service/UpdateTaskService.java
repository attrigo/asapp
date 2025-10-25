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
 * Application service responsible for orchestrate task updates operations.
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
     * @param taskRepository the task repository
     */
    public UpdateTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Updates an existing task based on the provided command.
     * <p>
     * Retrieves the task by ID, validates and transforms command data into domain objects, updates the task, and persists the changes.
     *
     * @param command the {@link UpdateTaskCommand} containing task update data
     * @return an {@link Optional} containing the updated {@link Task} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    @Override
    public Optional<Task> updateTaskById(UpdateTaskCommand command) {
        var taskId = TaskId.of(command.taskId());

        var optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) {
            return Optional.empty();
        }
        var currentTask = optionalTask.get();

        var newUserId = UserId.of(command.userId());
        var newTitle = Title.of(command.title());
        var newDescription = Description.ofNullable(command.description());
        var newStartDate = StartDate.ofNullable(command.startDate());
        var newEndDate = EndDate.ofNullable(command.endDate());

        currentTask.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);

        var taskUpdated = taskRepository.save(currentTask);

        return Optional.of(taskUpdated);
    }

}
