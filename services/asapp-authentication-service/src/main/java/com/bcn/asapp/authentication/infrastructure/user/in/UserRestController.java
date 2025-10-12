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

package com.bcn.asapp.authentication.infrastructure.user.in;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.authentication.application.user.in.CreateUserUseCase;
import com.bcn.asapp.authentication.application.user.in.DeleteUserUseCase;
import com.bcn.asapp.authentication.application.user.in.ReadUserUseCase;
import com.bcn.asapp.authentication.application.user.in.UpdateUserUseCase;
import com.bcn.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.mapper.UserMapper;

/**
 * REST controller implementing user management endpoints.
 * <p>
 * Handles HTTP requests for user operations by delegating to application use cases and mapping between DTOs.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RestController
public class UserRestController implements UserRestAPI {

    private final ReadUserUseCase readUserUseCase;

    private final CreateUserUseCase createUserUseCase;

    private final UpdateUserUseCase updateUserUseCase;

    private final DeleteUserUseCase deleteUserUseCase;

    private final UserMapper userMapper;

    /**
     * Constructs a new {@code UserRestController} with required dependencies.
     *
     * @param readUserUseCase   the use case for reading users
     * @param createUserUseCase the use case for creating users
     * @param updateUserUseCase the use case for updating users
     * @param deleteUserUseCase the use case for deleting users
     * @param userMapper        the mapper for user DTOs
     */
    public UserRestController(ReadUserUseCase readUserUseCase, CreateUserUseCase createUserUseCase, UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase, UserMapper userMapper) {

        this.readUserUseCase = readUserUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.userMapper = userMapper;
    }

    /**
     * Gets a user by their unique identifier.
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping the {@link GetUserByIdResponse} if found, otherwise wrapping empty
     */
    @Override
    public ResponseEntity<GetUserByIdResponse> getUserById(UUID id) {
        return readUserUseCase.getUserById(id)
                              .map(userMapper::toGetUserByIdResponse)
                              .map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound()
                                                             .build());
    }

    /**
     * Gets all users from the system.
     *
     * @return a {@link List} of {@link GetAllUsersResponse} containing all users found, or an empty list if no users exist
     */
    @Override
    public List<GetAllUsersResponse> getAllUsers() {
        return readUserUseCase.getAllUsers()
                              .stream()
                              .map(userMapper::toGetAllUsersResponse)
                              .toList();
    }

    /**
     * Creates a new user in the system. Response codes:
     *
     * @param request the {@link CreateUserRequest} containing user data
     * @return the {@link CreateUserResponse} containing the created user's information
     */
    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        var command = userMapper.toCreateUserCommand(request);

        var userCreated = createUserUseCase.createUser(command);

        return userMapper.toCreateUserResponse(userCreated);
    }

    /**
     * Updates an existing user by their unique identifier.
     *
     * @param id      the user's unique identifier
     * @param request the {@link UpdateUserRequest} containing updated user data
     * @return a {@link ResponseEntity} containing the {@link UpdateUserResponse} if found, otherwise wrapping empty
     */
    @Override
    public ResponseEntity<UpdateUserResponse> updateUserById(UUID id, UpdateUserRequest request) {
        var command = userMapper.toUpdateUserCommand(id, request);

        return updateUserUseCase.updateUserById(command)
                                .map(userMapper::toUpdateUserResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound()
                                                               .build());
    }

    /**
     * Deletes a user by their unique identifier.
     * <p>
     * If the user has active authentications, they will be revoked before deletion.
     *
     * @param id the user's unique identifier
     * @return a {@link ResponseEntity} wrapping empty upon successful deletion
     */
    @Override
    public ResponseEntity<Void> deleteUserById(UUID id) {
        boolean userHasBeenDeleted = deleteUserUseCase.deleteUserById(id);

        return ResponseEntity.status(userHasBeenDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
