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

@Tag(name = "Users operations", description = "Defines the RESTful API for handling user operations")
@RequestMapping(USERS_ROOT_PATH)
public interface UserRestAPI {

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Gets a user by id", description = "Returns the user found, or empty if the given id has not been found")
    @ApiResponse(responseCode = "200", description = "User has been found", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The JWT authentication could not be authenticated", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @GetMapping(value = USERS_GET_BY_ID_PATH, produces = "application/json")
    ResponseEntity<UserDTO> getUserById(@Parameter(description = "Id of the user to get") @PathVariable("id") UUID id);

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Gets all users", description = "Returns all users, or an empty list if there aren't users")
    @ApiResponse(responseCode = "200", description = "Users found", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "401", description = "The JWT authentication could not be authenticated", content = { @Content })
    @GetMapping(value = USERS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    List<UserDTO> getAllUsers();

    @Operation(summary = "Creates a user", description = "Creates the given user ignoring the id field, the resultant user always has a new id")
    @ApiResponse(responseCode = "201", description = "User has been created", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @PostMapping(value = USERS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    UserDTO createUser(@Valid @RequestBody UserDTO user);

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Updates a user", description = "Updates all fields of the user except the id, with the given new data")
    @ApiResponse(responseCode = "200", description = "User has been updated", content = { @Content(schema = @Schema(implementation = UserDTO.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The JWT authentication could not be authenticated", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @PutMapping(value = USERS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    ResponseEntity<UserDTO> updateUserById(@Parameter(description = "Identifier of the user to update") @PathVariable("id") UUID id,
            @Valid @RequestBody UserDTO newUserData);

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Deletes a user by id", description = "Deletes a user by id")
    @ApiResponse(responseCode = "204", description = "User has been deleted", content = { @Content })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The JWT authentication could not be authenticated", content = { @Content })
    @ApiResponse(responseCode = "404", description = "User not found", content = { @Content })
    @DeleteMapping(value = USERS_DELETE_BY_ID_PATH, produces = "application/json")
    ResponseEntity<Void> deleteUserById(@Parameter(description = "Id of the user to delete") @PathVariable("id") UUID id);

}
