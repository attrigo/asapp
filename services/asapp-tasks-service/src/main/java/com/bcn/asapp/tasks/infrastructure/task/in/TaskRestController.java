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

package com.bcn.asapp.tasks.infrastructure.task.in;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.tasks.application.task.in.CreateTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.DeleteTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.ReadTaskUseCase;
import com.bcn.asapp.tasks.application.task.in.UpdateTaskUseCase;
import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByUserIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.mapper.TaskMapper;

/**
 * REST controller implementing task management endpoints.
 * <p>
 * Handles HTTP requests for task operations by delegating to application use cases and mapping between DTOs.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RestController
public class TaskRestController implements TaskRestAPI {

    private final ReadTaskUseCase readTaskUseCase;

    private final CreateTaskUseCase createTaskUseCase;

    private final UpdateTaskUseCase updateTaskUseCase;

    private final DeleteTaskUseCase deleteTaskUseCase;

    private final TaskMapper taskMapper;

    /**
     * Constructs a new {@code TaskRestController} with required dependencies.
     *
     * @param readTaskUseCase   the use case for reading tasks
     * @param createTaskUseCase the use case for creating tasks
     * @param updateTaskUseCase the use case for updating tasks
     * @param deleteTaskUseCase the use case for deleting tasks
     * @param taskMapper        the mapper for task DTOs
     */
    public TaskRestController(ReadTaskUseCase readTaskUseCase, CreateTaskUseCase createTaskUseCase, UpdateTaskUseCase updateTaskUseCase,
            DeleteTaskUseCase deleteTaskUseCase, TaskMapper taskMapper) {

        this.readTaskUseCase = readTaskUseCase;
        this.createTaskUseCase = createTaskUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.taskMapper = taskMapper;
    }

    /**
     * Gets a task by their unique identifier.
     *
     * @param id the task's unique identifier
     * @return a {@link ResponseEntity} wrapping the {@link GetTaskByIdResponse} if found, otherwise wrapping empty
     */
    @Override
    public ResponseEntity<GetTaskByIdResponse> getTaskById(UUID id) {
        return readTaskUseCase.getTaskById(id)
                              .map(taskMapper::toGetTaskByIdResponse)
                              .map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound()
                                                             .build());
    }

    /**
     * Gets all tasks for a specific user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of {@link GetTasksByUserIdResponse} containing all tasks for the user, or an empty list if no tasks exist
     */
    @Override
    public List<GetTasksByUserIdResponse> getTasksByUserId(UUID userId) {
        return readTaskUseCase.getTasksByUserId(userId)
                              .stream()
                              .map(taskMapper::toGetTasksByUserIdResponse)
                              .toList();
    }

    /**
     * Gets all tasks from the system.
     *
     * @return a {@link List} of {@link GetAllTasksResponse} containing all tasks found, or an empty list if no tasks exist
     */
    @Override
    public List<GetAllTasksResponse> getAllTasks() {
        return readTaskUseCase.getAllTasks()
                              .stream()
                              .map(taskMapper::toGetAllTasksResponse)
                              .toList();
    }

    /**
     * Creates a new task in the system. Response codes:
     *
     * @param request the {@link CreateTaskRequest} containing task data
     * @return the {@link CreateTaskResponse} containing the created task's information
     */
    @Override
    public CreateTaskResponse createTask(CreateTaskRequest request) {
        var command = taskMapper.toCreateTaskCommand(request);

        var taskCreated = createTaskUseCase.createTask(command);

        return taskMapper.toCreateTaskResponse(taskCreated);
    }

    /**
     * Updates an existing task by their unique identifier.
     *
     * @param id      the task's unique identifier
     * @param request the {@link UpdateTaskRequest} containing updated task data
     * @return a {@link ResponseEntity} containing the {@link UpdateTaskResponse} if found, otherwise wrapping empty
     */
    @Override
    public ResponseEntity<UpdateTaskResponse> updateTaskById(UUID id, UpdateTaskRequest request) {
        var command = taskMapper.toUpdateTaskCommand(id, request);

        return updateTaskUseCase.updateTaskById(command)
                                .map(taskMapper::toUpdateTaskResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound()
                                                               .build());
    }

    /**
     * Deletes a task by their unique identifier.
     *
     * @param id the task's unique identifier
     * @return a {@link ResponseEntity} wrapping empty upon successful deletion
     */
    @Override
    public ResponseEntity<Void> deleteTaskById(UUID id) {
        boolean taskHasBeenDeleted = deleteTaskUseCase.deleteTaskById(id);

        return ResponseEntity.status(taskHasBeenDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
