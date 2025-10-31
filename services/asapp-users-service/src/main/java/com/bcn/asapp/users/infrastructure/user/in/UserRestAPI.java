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

package com.bcn.asapp.users.infrastructure.user.in;

import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_CREATE_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_DELETE_BY_ID_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_ALL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_PATH;

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

import com.bcn.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.UpdateUserResponse;

/**
 * REST API contract for user management operations.
 * <p>
 * Defines the HTTP endpoints for CRUD operations on users, including OpenAPI documentation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RequestMapping(USERS_ROOT_PATH)
@Tag(name = "User Operations", description = "REST API contract for managing users")
@SecurityRequirement(name = "Bearer Authentication")
public interface UserRestAPI {

    /**
     * Gets a user by their unique identifier, enriched with task references.
     * <p>
     * This endpoint retrieves user information along with a list of task identifiers associated with the user.
     * <p>
     * If task retrieval fails, the response will contain the user with an empty task list (graceful degradation).
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: User found (with or without tasks).</li>
     * <li>400-BAD_REQUEST: Invalid user id format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping the {@link GetUserByIdResponse} if found, otherwise wrapping empty
     */
    @GetMapping(value = USERS_GET_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Gets a user by their unique identifier with task references", description = "Retrieves detailed information about a specific user by their unique identifier, including a list of associated task IDs. This endpoint requires authentication.")
    @ApiResponse(responseCode = "200", description = "User found (with or without tasks)", content = {
            @Content(schema = @Schema(implementation = GetUserByIdResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<GetUserByIdResponse> getUserById(@PathVariable("id") @Parameter(description = "Identifier of the user to get") UUID id);

    /**
     * Gets all users.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Users found.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * </ul>
     *
     * @return a {@link List} of {@link GetAllUsersResponse} containing all users found, or an empty list if no users exist
     */
    @GetMapping(value = USERS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets all users", description = "Retrieves a list of all registered users in the system. This endpoint requires authentication. If no users exist, an empty array is returned.")
    @ApiResponse(responseCode = "200", description = "Users found", content = { @Content(schema = @Schema(implementation = GetAllUsersResponse.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    List<GetAllUsersResponse> getAllUsers();

    /**
     * Creates a new user.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED: User created successfully.</li>
     * <li>400-BAD_REQUEST: Request body validation failed.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * </ul>
     *
     * @param request the {@link CreateUserRequest} containing user data
     * @return the {@link CreateUserResponse} containing the created user's identifier
     */
    @PostMapping(value = USERS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new user", description = "Creates a new user in the system with the provided user information. This endpoint requires authentication. Returns the user identifier. Use GET endpoint to retrieve full user details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User creation request containing all necessary user information", required = true, content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
    @ApiResponse(responseCode = "201", description = "User created successfully", content = {
            @Content(schema = @Schema(implementation = CreateUserResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Request body validation failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request);

    /**
     * Updates an existing user by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: User updated successfully.</li>
     * <li>400-BAD_REQUEST: Invalid request id format or request body validation failed.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id      the user's unique identifier
     * @param request the {@link UpdateUserRequest} containing updated user data
     * @return a {@link ResponseEntity} wrapping the {@link UpdateUserResponse} with the user identifier if found, otherwise wrapping empty
     */
    @PutMapping(value = USERS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    @Operation(summary = "Updates an existing user by their unique identifier", description = "Updates the information of an existing user identified by their unique identifier. This endpoint requires authentication. Only the fields provided in the request will be updated. Returns the user identifier. Use GET endpoint to retrieve full user details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User update request containing the user information to be modified", required = true, content = @Content(schema = @Schema(implementation = UpdateUserRequest.class)))
    @ApiResponse(responseCode = "200", description = "User updated successfully", content = {
            @Content(schema = @Schema(implementation = UpdateUserResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format or request body validation failed.", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<UpdateUserResponse> updateUserById(@PathVariable("id") @Parameter(description = "Identifier of the request to update") UUID id,
            @RequestBody @Valid UpdateUserRequest request);

    /**
     * Deletes a user by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>204-NO_CONTENT: User deleted successfully.</li>
     * <li>400-BAD_REQUEST: Invalid user id format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping empty upon successful deletion
     */
    @DeleteMapping(value = USERS_DELETE_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Deletes a user by their unique identifier", description = "Removes a user from the system by their unique identifier. This endpoint requires authentication. This operation cannot be undone.")
    @ApiResponse(responseCode = "204", description = "User deleted successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<Void> deleteUserById(@PathVariable("id") @Parameter(description = "Identifier of the user to delete") UUID id);

}
