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
package com.bcn.asapp.uaa.user;

import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_CREATE_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_DELETE_BY_ID_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_ALL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_BY_ID_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_UPDATE_BY_ID_PATH;

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

import com.bcn.asapp.dto.user.UserDTO;

/**
 * Defines the RESTful API contract for user management operations.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@RequestMapping(USERS_ROOT_PATH)
@Tag(name = "User Operations", description = "REST API contract for managing users")
public interface UserRestAPI {

    /**
     * Gets a user by id.
     * <p>
     * Returns a user with the specified id, or empty if not found.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-OK: User found.</li>
     * <li>400-BAD_REQUEST: Invalid user id format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id the id of the user to get
     * @return a {@link ResponseEntity} wrapping the {@link UserDTO} with the specified id or empty if not found
     */
    @GetMapping(value = USERS_GET_BY_ID_PATH, produces = "application/json")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Gets a user by id", description = "Returns a user with the specified id, or empty if not found")
    @ApiResponse(responseCode = "200", description = "User found", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user id format", content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<UserDTO> getUserById(@PathVariable("id") @Parameter(description = "Id of the user to get") UUID id);

    /**
     * Get all users.
     * <p>
     * Returns a list of all users, or an empty list if no users exist.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: Users found.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * </ul>
     *
     * @return a list of all {@link UserDTO}, or an empty list if no users exist
     */
    @GetMapping(value = USERS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Gets all users", description = "Returns a list of all users, or an empty list if no users exist")
    @ApiResponse(responseCode = "200", description = "Users found", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    List<UserDTO> getAllUsers();

    /**
     * Creates a new user.
     * <p>
     * Creates a new user with a new generated id, any provided id will be ignored.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED: User created successfully.</li>
     * <li>400-BAD_REQUEST: Request body validation failed.</li>
     * </ul>
     *
     * @param user the user data to create
     * @return the created {@link UserDTO} with a generated id
     */
    @PostMapping(value = USERS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a new user", description = "Creates a new user with a new generated id, any provided id will be ignored")
    @ApiResponse(responseCode = "201", description = "User created successfully", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "Request body validation failed", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    UserDTO createUser(@RequestBody @Valid @Parameter(description = "User data to create", required = true) UserDTO user);

    /**
     * Updates an existing user by id.
     * <p>
     * Updates the data of an existing user by id except the id, with the provided new data.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: User updated successfully.</li>
     * <li>400-BAD_REQUEST: Invalid user id format or request body validation failed.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id          the id of the user to update
     * @param newUserData the user data to update
     * @return a {@link ResponseEntity} wrapping the updated {@link UserDTO}, or empty if not found
     */
    @PutMapping(value = USERS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Updates an existing user by id", description = "Updates the data of an existing user by the specified id except the id, with the provided new data")
    @ApiResponse(responseCode = "200", description = "User has been updated", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "Invalid user id format or request body validation failed.", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<UserDTO> updateUserById(@PathVariable("id") @Parameter(description = "Id of the user to update") UUID id,
            @RequestBody @Valid @Parameter(description = "User data to update", required = true) UserDTO newUserData);

    /**
     * Deletes a user by id.
     * <p>
     * Deletes a user by the specified id.
     * <p>
     * If the user is authenticated, its authentication is revoked before the deletion.
     * <p>
     * Response codes:
     * <ul>
     * <li>204-NO_CONTENT: User deleted successfully.</li>
     * <li>400-BAD_REQUEST: Invalid user id format.</li>
     * <li>401-UNAUTHORIZED: Authentication required or failed.</li>
     * <li>404-NOT_FOUND: User not found.</li>
     * </ul>
     *
     * @param id the id of the user to delete
     * @return a {@link ResponseEntity} with no content upon successful deletion
     */
    @DeleteMapping(value = USERS_DELETE_BY_ID_PATH, produces = "application/json")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Deletes a user by id", description = "Deletes a user by the specified id, if the user is authenticated, it is revoked before the deletion")
    @ApiResponse(responseCode = "204", description = "User deleted successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "Invalid user id format", content = { @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "Authentication required or failed", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    ResponseEntity<Void> deleteUserById(@PathVariable("id") @Parameter(description = "Id of the user to delete") UUID id);

}
