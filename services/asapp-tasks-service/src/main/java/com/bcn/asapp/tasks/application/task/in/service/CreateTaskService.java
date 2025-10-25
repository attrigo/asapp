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

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.CreateTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;

/**
 * Application service responsible for orchestrate task creation operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class CreateTaskService implements CreateTaskUseCase {

    private final TaskRepository taskRepository;

    /**
     * Constructs a new {@code CreateTaskService} with required dependencies.
     *
     * @param taskRepository the task repository
     */
    public CreateTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Creates a new task based on the provided command.
     * <p>
     * Validates and transforms command data into domain objects, creates a new task, and persists it to the repository.
     *
     * @param command the {@link CreateTaskCommand} containing task data
     * @return the created {@link Task} with a persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    @Override
    public Task createTask(CreateTaskCommand command) {
        var userId = UserId.of(command.userId());
        var title = Title.of(command.title());
        var description = Description.ofNullable(command.description());
        var startDate = StartDate.ofNullable(command.startDate());
        var endDate = EndDate.ofNullable(command.endDate());

        var newTask = Task.newTask(userId, title, description, startDate, endDate);

        return taskRepository.save(newTask);
    }

}
