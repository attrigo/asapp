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

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.DeleteTaskUseCase;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.TaskId;

/**
 * Application service responsible for orchestrating task deletion operations.
 * <p>
 * Coordinates the task deletion workflow including identifier transformation and removal from the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Transforms UUID into domain value object {@link TaskId}</li>
 * <li>Deletes task from repository</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class DeleteTaskService implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;

    /**
     * Constructs a new {@code DeleteTaskService} with required dependencies.
     *
     * @param taskRepository the repository for task data access
     */
    public DeleteTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Deletes an existing task by its unique identifier.
     * <p>
     * Orchestrates the task deletion workflow: identifier transformation and deletion.
     *
     * @param id the task's unique identifier as UUID
     * @return {@code true} if the task was successfully deleted, {@code false} if task was not found
     * @throws IllegalArgumentException if the id is null or invalid
     */
    @Override
    @Transactional
    public Boolean deleteTaskById(UUID id) {
        var taskId = TaskId.of(id);

        return deleteTask(taskId);
    }

    /**
     * Deletes the task from the repository by identifier.
     *
     * @param taskId the task's unique identifier
     * @return {@code true} if deleted, {@code false} if not found
     */
    private Boolean deleteTask(TaskId taskId) {
        return taskRepository.deleteById(taskId);
    }

}
