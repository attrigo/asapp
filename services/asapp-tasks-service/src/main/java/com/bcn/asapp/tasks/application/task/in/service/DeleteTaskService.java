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

import com.bcn.asapp.tasks.application.ApplicationService;
import com.bcn.asapp.tasks.application.task.in.DeleteTaskUseCase;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.TaskId;

/**
 * Application service responsible for orchestrate task deletion operations.
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
     * @param taskRepository the task repository
     */
    public DeleteTaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Deletes an existing task by their unique identifier.
     *
     * @param id the task's unique identifier
     * @return {@code true} if the task was deleted, {@code false} if not found
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Boolean deleteTaskById(UUID id) {
        var taskId = TaskId.of(id);

        return taskRepository.deleteById(taskId);
    }

}
