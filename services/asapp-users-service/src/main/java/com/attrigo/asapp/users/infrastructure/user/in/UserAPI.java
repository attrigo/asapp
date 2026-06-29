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

package com.attrigo.asapp.users.infrastructure.user.in;

import static com.attrigo.asapp.url.users.UserAPIURL.USERS_CREATE_PATH;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_DELETE_BY_ID_PATH;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_GET_ALL_PATH;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_GET_BY_ID_PATH;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_IDS_PARAM;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_ROOT_PATH;
import static com.attrigo.asapp.url.users.UserAPIURL.USERS_UPDATE_BY_ID_PATH;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.attrigo.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUsersResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.UpdateUserResponse;

/**
 * API contract for user management operations.
 * <p>
 * Serves as the service's inbound HTTP entry point, implemented by the controller.
 * <p>
 * Defines the HTTP endpoints for CRUD operations on users, including OpenAPI documentation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RequestMapping(USERS_ROOT_PATH)
@Tag(name = "User Operations", description = "API contract for managing users")
@SecurityRequirement(name = "Bearer Authentication")
public interface UserAPI {

    /**
     * Gets a user by their unique identifier, enriched with task references.
     * <p>
     * This endpoint retrieves user information along with a list of task identifiers associated with the user.
     * <p>
     * If tasks-service is unavailable, the endpoint still returns 200 OK with the user, an empty task list, and a {@code warnings} array describing the
     * degradation.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: User found (with or without tasks).</li>
     * <li>400-BAD_REQUEST: Invalid user identifier format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping the {@link GetUserByIdResponse} if found, otherwise wrapping empty
     */
    @GetMapping(value = USERS_GET_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Gets a user by their unique identifier with task references", description = "Retrieves detailed information about a specific user by their unique identifier, including a list of associated task identifiers. If tasks-service is unavailable, the request still succeeds with an empty `taskIds` and a `task_ids_unavailable` warning in the `warnings` array. Because an empty `taskIds` is indistinguishable from a user who genuinely has no tasks, clients must inspect `warnings` to detect degradation.")
    @ApiResponse(responseCode = "200", description = "User found (with or without tasks); includes a warnings array when task data could not be retrieved", content = {
            @Content(schema = @Schema(implementation = GetUserByIdResponse.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<GetUserByIdResponse> getUserById(@PathVariable @Parameter(description = "Identifier of the user to get") UUID id);

    /**
     * Gets users, optionally filtered by their unique identifiers.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Users retrieved successfully.</li>
     * <li>400-BAD_REQUEST: ids is present but empty, exceeds 50 elements, or contains a malformed UUID.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during retrieval.</li>
     * </ul>
     *
     * @param ids the identifiers of the users
     * @return a {@link List} of {@link GetUsersResponse} with the matching users, or an empty list if none match
     */
    @GetMapping(value = USERS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets users, optionally filtered by their unique identifiers", description = "Retrieves users from the system. By default returns all users; when the `ids` query parameter is supplied, returns only the users whose identifiers are in the list. Duplicate identifiers are ignored. Returns an empty array if no users match.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = {
            @Content(schema = @Schema(implementation = GetUsersResponse.class)) })
    @ApiResponse(responseCode = "400", description = "User identifiers list is present but empty, exceeds 50 elements, or contains a malformed UUID", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during retrieval", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    List<GetUsersResponse> getUsers(
            @RequestParam(name = USERS_IDS_PARAM, required = false) @Parameter(description = "Optional list of user identifiers to filter by; omit to return all users") @Size(min = 1, max = 50, message = "Users identifiers list must contain between 1 and 50 elements") List<UUID> ids);

    /**
     * Creates a new user.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED: User created successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during user creation.</li>
     * </ul>
     *
     * @param request the {@link CreateUserRequest} containing user data
     * @return the {@link CreateUserResponse} containing the created user's identifier
     */
    @PostMapping(value = USERS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new user", description = "Creates a new user in the system with the provided user information. Returns the user identifier. Use the GET endpoint to retrieve full user details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User creation request containing all necessary user information", required = true, content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
    @ApiResponse(responseCode = "201", description = "User created successfully", content = {
            @Content(schema = @Schema(implementation = CreateUserResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during user creation", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    CreateUserResponse createUser(@RequestBody @Valid CreateUserRequest request);

    /**
     * Updates an existing user by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: User updated successfully.</li>
     * <li>400-BAD_REQUEST: The user identifier format is invalid or the request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during user update.</li>
     * </ul>
     *
     * @param id      the user's unique identifier
     * @param request the {@link UpdateUserRequest} containing updated user data
     * @return a {@link ResponseEntity} wrapping the {@link UpdateUserResponse} with the user identifier if found, otherwise wrapping empty
     */
    @PutMapping(value = USERS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    @Operation(summary = "Updates an existing user by their unique identifier", description = "Updates the information of an existing user identified by their unique identifier. Only the fields provided in the request will be updated. Returns the user identifier. Use the GET endpoint to retrieve full user details.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User update request containing the user information to be modified", required = true, content = @Content(schema = @Schema(implementation = UpdateUserRequest.class)))
    @ApiResponse(responseCode = "200", description = "User updated successfully", content = {
            @Content(schema = @Schema(implementation = UpdateUserResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The user identifier format is invalid or the request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during user update", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<UpdateUserResponse> updateUserById(@PathVariable @Parameter(description = "Identifier of the user to update") UUID id,
            @RequestBody @Valid UpdateUserRequest request);

    /**
     * Deletes a user by their unique identifier.
     * <p>
     * Response codes:
     * <ul>
     * <li>204-NO_CONTENT: User deleted successfully.</li>
     * <li>400-BAD_REQUEST: Invalid user identifier format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * <li>500-INTERNAL_SERVER_ERROR: An internal error occurred during user deletion.</li>
     * </ul>
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping empty upon successful deletion
     */
    @DeleteMapping(value = USERS_DELETE_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Deletes a user by their unique identifier", description = "Removes a user from the system by their unique identifier. This operation cannot be undone.")
    @ApiResponse(responseCode = "204", description = "User deleted successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "Invalid user identifier format", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @ApiResponse(responseCode = "500", description = "An internal error occurred during user deletion", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    ResponseEntity<Void> deleteUserById(@PathVariable @Parameter(description = "Identifier of the user to delete") UUID id);

}
