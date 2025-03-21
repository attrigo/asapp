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
package com.bcn.asapp.tasks.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.dto.task.TaskDTO;

/**
 * Defines the task business operations.
 *
 * @author ttrigo
 * @since 0.1.0
 */
public interface TaskService {

    /**
     * Finds a task by the given id.
     *
     * @param id the id of the task to be found, must not be {@literal null}.
     * @return {@link Optional} containing the task found, or an empty {@link Optional} if the given id has not been found.
     */
    Optional<TaskDTO> findById(UUID id);

    /**
     * Finds all tasks.
     *
     * @return a list with all tasks, or an empty list if none found.
     */
    List<TaskDTO> findAll();

    /**
     * Finds the tasks that belongs to the given project id.
     *
     * @param projectId the id of the project.
     * @return a list with the tasks found, or an empty list if none found.
     */
    List<TaskDTO> findByProjectId(UUID projectId);

    /**
     * Creates the given task.
     * <p>
     * Always creates the task with a new id, therefore in cases where the id of the given task is present it is ignored.
     *
     * @param task the task to be created, must be a valid task.
     * @return the created task.
     */
    TaskDTO create(TaskDTO task);

    /**
     * Updates the task by the given id with the given new data.
     * <p>
     * The id of the task is not updated, so in cases where the given new task has id it is ignored.
     *
     * @param id          the id of the task to be updated, must not be {@literal null}.
     * @param newTaskData the new task data, must be a valid task.
     * @return {@link Optional} containing the task updated with the new data, or an empty {@link Optional} if the given id has not been found.
     */
    Optional<TaskDTO> updateById(UUID id, TaskDTO newTaskData);

    /**
     * Deletes a task by the given id.
     *
     * @param id the id of the task to be deleted, must not be {@literal null}.
     * @return true if the given id exists, otherwise false.
     */
    Boolean deleteById(UUID id);

}
