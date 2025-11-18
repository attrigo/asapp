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

package com.bcn.asapp.tasks.infrastructure.task.out;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.UserId;
import com.bcn.asapp.tasks.infrastructure.task.mapper.TaskMapper;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskRepository;

/**
 * Adapter implementation of {@link TaskRepository} for JDBC persistence.
 * <p>
 * Bridges the application layer with the infrastructure layer by translating domain operations to JDBC repository calls and mapping between domain entities and
 * database entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class TaskRepositoryAdapter implements TaskRepository {

    private final JdbcTaskRepository taskRepository;

    private final TaskMapper taskMapper;

    /**
     * Constructs a new {@code TaskRepositoryAdapter} with required dependencies.
     *
     * @param taskRepository the Spring Data JDBC repository
     * @param taskMapper     the mapper for converting between domain and database entities
     */
    public TaskRepositoryAdapter(JdbcTaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    /**
     * Finds a task by their unique identifier.
     *
     * @param taskId the task's unique identifier
     * @return an {@link Optional} containing the {@link Task} if found, {@link Optional#empty} otherwise
     */
    @Override
    public Optional<Task> findById(TaskId taskId) {
        return taskRepository.findById(taskId.value())
                             .map(taskMapper::toTask);
    }

    /**
     * Finds all tasks by their user's unique identifier.
     *
     * @param userId the user's unique identifier
     * @return a {@link Collection} of {@link Task} entities belonging to the user
     */
    @Override
    public Collection<Task> findByUserId(UserId userId) {
        return taskRepository.findByUserId(userId.value())
                             .stream()
                             .map(taskMapper::toTask)
                             .toList();
    }

    /**
     * Retrieves all tasks from the repository.
     *
     * @return a {@link Collection} of all {@link Task} entities
     */
    @Override
    public Collection<Task> findAll() {
        return taskRepository.findAll()
                             .stream()
                             .map(taskMapper::toTask)
                             .toList();
    }

    /**
     * Saves a task to the repository.
     * <p>
     * If the task is new (without ID), it will be persisted and returned with a generated ID.
     * <p>
     * If the task is reconstituted (with ID), it will be updated.
     *
     * @param task the {@link Task} to save
     * @return the saved {@link Task} with a persistent ID
     */
    @Override
    public Task save(Task task) {
        var taskToSave = taskMapper.toJdbcTaskEntity(task);

        var taskSaved = taskRepository.save(taskToSave);

        return taskMapper.toTask(taskSaved);
    }

    /**
     * Deletes a task by their unique identifier.
     *
     * @param taskId the task's unique identifier
     * @return {@code true} if the task was deleted, {@code false} if not found
     */
    @Override
    public Boolean deleteById(TaskId taskId) {
        return taskRepository.deleteTaskById(taskId.value()) > 0;
    }

}
