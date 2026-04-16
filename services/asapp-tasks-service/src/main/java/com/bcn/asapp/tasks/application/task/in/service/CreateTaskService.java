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

package com.bcn.asapp.tasks.application.task.in.service;

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.CreateTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskFactory;

/**
 * Application service responsible for orchestrating task creation operations.
 * <p>
 * Coordinates the task creation workflow including domain object creation and persistence to the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Creates {@link Task} domain object via {@link TaskFactory}</li>
 * <li>Persists task to repository</li>
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
     * @param taskRepository the repository for task data access
     */
    public CreateTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Creates a new task based on the provided command.
     * <p>
     * Orchestrates the complete task creation workflow: domain object creation and persistence.
     *
     * @param command the {@link CreateTaskCommand} containing task registration data
     * @return the created {@link Task} with a generated persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid (blank title, invalid dates, etc.)
     */
    @Override
    @Transactional
    public Task createTask(CreateTaskCommand command) {
        var task = TaskFactory.create(command.userId(), command.title(), command.description(), command.startDate(), command.endDate());

        return persistTask(task);
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
