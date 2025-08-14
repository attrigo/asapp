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

package com.bcn.asapp.uaa.user.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.user.UserRestAPI;
import com.bcn.asapp.uaa.user.UserService;

/**
 * REST controller implementation of the {@link UserRestAPI} responsible for handling user management requests.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@RestController
public class UserRestController implements UserRestAPI {

    /**
     * Service responsible for user business logic.
     */
    private final UserService userService;

    /**
     * Constructs a new {@code UserRestController} with the specified user service.
     *
     * @param userService the service responsible for user business logic
     */
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UserDTO> getUserById(UUID id) {
        return userService.findById(id)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound()
                                                         .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserDTO> getAllUsers() {
        return userService.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDTO createUser(UserDTO user) {
        return userService.create(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UserDTO> updateUserById(UUID id, UserDTO newUserData) {
        return userService.updateById(id, newUserData)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound()
                                                         .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> deleteUserById(UUID id) {
        boolean userHasBeenDeleted = userService.deleteById(id);
        return ResponseEntity.status(userHasBeenDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
