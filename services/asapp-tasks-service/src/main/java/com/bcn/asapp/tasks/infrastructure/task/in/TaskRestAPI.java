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

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_CREATE_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_DELETE_BY_ID_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_ALL_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_ID_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_ROOT_PATH;
import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_UPDATE_BY_ID_PATH;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByUserIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;

/**
 * REST API contract for task management operations.
 * <p>
 * Defines the HTTP endpoints for CRUD operations on tasks, including OpenAPI documentation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RequestMapping(TASKS_ROOT_PATH)
@Tag(name = "Task Operations", description = "REST API contract for managing tasks")
@SecurityRequirement(name = "Bearer Authentication")
public interface TaskRestAPI {

    /**
     * Gets a task by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Task found.</li>
     * <li>400-BAD_REQUEST: Invalid task identifier format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: Task not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param id the task's unique identifier
     * @return a {@link ResponseEntity} wrapping the {@link GetTaskByIdResponse} if found, otherwise wrapping empty
     */
    @GetMapping(value = TASKS_GET_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Gets a task by their unique identifier", description = "Retrieves detailed information about a specific task by their unique identifier. This endpoint requires authentication.")
    @ApiResponse(responseCode = "200", description = "Task found", content = { @Content(schema = @Schema(implementation = GetTaskByIdResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid task identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "Task not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<GetTaskByIdResponse> getTaskById(@PathVariable("id") @Parameter(description = "Identifier of the task to get") UUID id);

    /**
     * Gets all tasks for a specific user by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Tasks found for the user.</li>
     * <li>400-BAD_REQUEST: Invalid user identifier format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of {@link GetTasksByUserIdResponse} containing all tasks for the user, or an empty list if no tasks exist
     */
    @GetMapping(value = TASKS_GET_BY_USER_ID_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets all tasks for a specific user by their unique identifier", description = "Retrieves a list of all tasks belonging to a specific user identified by their unique identifier. This endpoint requires authentication. If no tasks exist for the user, an empty array is returned.")
    @ApiResponse(responseCode = "200", description = "Tasks found for the user", content = {
            @Content(schema = @Schema(implementation = GetTasksByUserIdResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetTasksByUserIdResponse> getTasksByUserId(@PathVariable("id") @Parameter(description = "Identifier of the user whose tasks to retrieve") UUID userId);

    /**
     * Gets all tasks.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Tasks retrieved successfully.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @return a {@link List} of {@link GetAllTasksResponse} containing all tasks found, or an empty list if no tasks exist
     */
    @GetMapping(value = TASKS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets all tasks", description = "Retrieves a list of all registered tasks in the system. This endpoint requires authentication. If no tasks exist, an empty array is returned.")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = {
            @Content(schema = @Schema(implementation = GetAllTasksResponse.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetAllTasksResponse> getAllTasks();

    /**
     * Creates a new task.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED: Task created successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during task creation.</li>
     * </ul>
     *
     * @param request the {@link CreateTaskRequest} containing task data
     * @return the {@link CreateTaskResponse} containing the created task's identifier
     */
    @PostMapping(value = TASKS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new task", description = "Creates a new task in the system with the provided task information. This endpoint requires authentication. Returns the task identifier. Use GET endpoint to retrieve full task details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Task creation request containing all necessary task information", required = true, content = @Content(schema = @Schema(implementation = CreateTaskRequest.class)))
    @ApiResponse(responseCode = "201", description = "Task created successfully", content = {
            @Content(schema = @Schema(implementation = CreateTaskResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during task creation", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    CreateTaskResponse createTask(@RequestBody @Valid CreateTaskRequest request);

    /**
     * Updates an existing task by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Task updated successfully.</li>
     * <li>400-BAD_REQUEST: The task identifier format is invalid or the request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: Task not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during task update.</li>
     * </ul>
     *
     * @param id      the task's unique identifier
     * @param request the {@link UpdateTaskRequest} containing updated task data
     * @return a {@link ResponseEntity} wrapping the {@link UpdateTaskResponse} with the task identifier if found, otherwise wrapping empty
     */
    @PutMapping(value = TASKS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    @Operation(summary = "Updates an existing task by their unique identifier", description = "Updates the information of an existing task identified by their unique identifier. This endpoint requires authentication. Only the fields provided in the request will be updated. Returns the task identifier. Use GET endpoint to retrieve full task details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Task update request containing the task information to be modified", required = true, content = @Content(schema = @Schema(implementation = UpdateTaskRequest.class)))
    @ApiResponse(responseCode = "200", description = "Task updated successfully", content = {
            @Content(schema = @Schema(implementation = UpdateTaskResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The task identifier format is invalid or the request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "Task not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during task update", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<UpdateTaskResponse> updateTaskById(@PathVariable("id") @Parameter(description = "Identifier of the task to update") UUID id,
            @RequestBody @Valid UpdateTaskRequest request);

    /**
     * Deletes a task by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>204-NO_CONTENT: Task deleted successfully.</li>
     * <li>400-BAD_REQUEST: Invalid task identifier format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: Task not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during task deletion.</li>
     * </ul>
     *
     * @param id the task's unique identifier
     * @return a {@link ResponseEntity} wrapping empty upon successful deletion
     */
    @DeleteMapping(value = TASKS_DELETE_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Deletes a task by their unique identifier", description = "Removes a task from the system by their unique identifier. This endpoint requires authentication. This operation cannot be undone.")
    @ApiResponse(responseCode = "204", description = "Task deleted successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "Invalid task identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "Task not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during task deletion", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<Void> deleteTaskById(@PathVariable("id") @Parameter(description = "Identifier of the task to delete") UUID id);

}
