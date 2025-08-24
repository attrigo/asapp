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

package com.bcn.asapp.uaa.infrastructure.user.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.uaa.application.user.api.CreateUserUseCase;
import com.bcn.asapp.uaa.application.user.api.DeleteUserUseCase;
import com.bcn.asapp.uaa.application.user.api.ReadUserUseCase;
import com.bcn.asapp.uaa.application.user.api.UpdateUserUseCase;
import com.bcn.asapp.uaa.domain.user.UserId;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.CreateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.CreateUserResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.GetAllUsersResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.GetUserByIdResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.UpdateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.UpdateUserResponse;
import com.bcn.asapp.uaa.infrastructure.user.mapper.UserMapper;

/**
 * REST controller implementation of the {@link UserRestAPI} responsible for handling user management requests.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@RestController
public class UserRestController implements UserRestAPI {

    private final ReadUserUseCase readUserUseCase;

    private final CreateUserUseCase createUserUseCase;

    private final UpdateUserUseCase updateUserUseCase;

    private final DeleteUserUseCase deleteUserUseCase;

    private final UserMapper userMapper;

    public UserRestController(ReadUserUseCase readUserUseCase, CreateUserUseCase createUserUseCase, UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase, UserMapper userMapper) {

        this.readUserUseCase = readUserUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.userMapper = userMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<GetUserByIdResponse> getUserById(UUID id) {
        return readUserUseCase.getUserById(new UserId(id))
                              .map(userMapper::toGetUserByIdResponse)
                              .map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound()
                                                             .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GetAllUsersResponse> getAllUsers() {
        return readUserUseCase.getAllUsers()
                              .stream()
                              .map(userMapper::toGetAllUsersResponse)
                              .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        var userToCreate = userMapper.toUser(request);
        var userCreated = createUserUseCase.createUser(userToCreate);
        return userMapper.toCreateUserResponse(userCreated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UpdateUserResponse> updateUserById(UUID id, UpdateUserRequest request) {
        var userIdToUpdate = new UserId(id);
        var userToUpdate = userMapper.toUser(request);
        return updateUserUseCase.updateUserById(userIdToUpdate, userToUpdate)
                                .map(userMapper::toUpdateUserResponse)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound()
                                                               .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> deleteUserById(UUID id) {
        boolean userHasBeenDeleted = deleteUserUseCase.deleteUserById(new UserId(id));
        return ResponseEntity.status(userHasBeenDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
