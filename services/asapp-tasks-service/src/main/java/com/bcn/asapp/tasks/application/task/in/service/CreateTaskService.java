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

import org.springframework.transaction.annotation.Transactional;

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
 * Application service responsible for orchestrating task creation operations.
 * <p>
 * Coordinates the task creation workflow including command transformation, domain object creation, and persistence to the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Transforms command parameters into domain value objects</li>
 * <li>Creates task domain object via {@link Task#create}</li>
 * <li>Persists task to repository via {@link TaskRepository#save}</li>
 * </ol>
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
     * Orchestrates the complete task creation workflow: parameter transformation, domain object creation, and persistence.
     *
     * @param command the {@link CreateTaskCommand} containing task registration data
     * @return the created {@link Task} with a generated persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid (blank title, invalid dates, etc.)
     */
    @Override
    @Transactional
    public Task createTask(CreateTaskCommand command) {
        var task = mapCommandToDomain(command);

        return persistTask(task);
    }

    /**
     * Creates a task domain object from command data.
     * <p>
     * Maps command parameters into domain value objects and invokes the task factory method.
     *
     * @param command the create task command containing raw data
     * @return the created {@link Task} domain object without persistence ID
     * @throws IllegalArgumentException if any value object validation fails
     */
    private Task mapCommandToDomain(CreateTaskCommand command) {
        var userId = UserId.of(command.userId());
        var title = Title.of(command.title());
        var description = Description.ofNullable(command.description());
        var startDate = StartDate.ofNullable(command.startDate());
        var endDate = EndDate.ofNullable(command.endDate());

        return Task.create(userId, title, description, startDate, endDate);
    }

    /**
     * Persists task to the repository.
     *
     * @param task the task domain object to persist
     * @return the persisted {@link Task} with generated ID
     */
    private Task persistTask(Task task) {
        return taskRepository.save(task);
    }

}
