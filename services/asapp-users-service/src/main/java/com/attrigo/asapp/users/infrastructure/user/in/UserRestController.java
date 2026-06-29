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

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import com.attrigo.asapp.users.application.user.in.CreateUserUseCase;
import com.attrigo.asapp.users.application.user.in.DeleteUserUseCase;
import com.attrigo.asapp.users.application.user.in.ReadUserUseCase;
import com.attrigo.asapp.users.application.user.in.UpdateUserUseCase;
import com.attrigo.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUsersResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.attrigo.asapp.users.infrastructure.user.mapper.UserMapper;

/**
 * REST controller implementing user management endpoints.
 * <p>
 * Handles HTTP requests for user operations by delegating to application use cases and mapping between DTOs.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RestController
@Validated
public class UserRestController implements UserApi {

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

    @Override
    public ResponseEntity<GetUserByIdResponse> getUserById(UUID id) {
        return readUserUseCase.getUserById(id)
                              .map(userMapper::toGetUserByIdResponse)
                              .map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound()
                                                             .build());
    }

    @Override
    public List<GetUsersResponse> getUsers(List<UUID> ids) {
        var users = ids == null ? readUserUseCase.getAllUsers() : readUserUseCase.getUsersByIds(ids);

        return users.stream()
                    .map(userMapper::toGetUsersResponse)
                    .toList();
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        var command = userMapper.toCreateUserCommand(request);

        var userCreated = createUserUseCase.createUser(command);

        return userMapper.toCreateUserResponse(userCreated);
    }

    @Override
    public ResponseEntity<UpdateUserResponse> updateUserById(UUID id, UpdateUserRequest request) {
        var command = userMapper.toUpdateUserCommand(id, request);

        return updateUserUseCase.updateUserById(command)
                                .map(userMapper::toUpdateUserResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound()
                                                               .build());
    }

    @Override
    public ResponseEntity<Void> deleteUserById(UUID id) {
        boolean userHasBeenDeleted = deleteUserUseCase.deleteUserById(id);

        return ResponseEntity.status(userHasBeenDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
