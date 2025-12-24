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

package com.bcn.asapp.authentication.application.user.in.service;

import java.util.UUID;

import com.bcn.asapp.authentication.application.ApplicationService;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.user.in.DeleteUserUseCase;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Application service responsible for orchestrate user deletion operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class DeleteUserService implements DeleteUserUseCase {

    private final JwtStore jwtStore;

    private final UserRepository userRepository;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DeleteUserService} with required dependencies.
     *
     * @param jwtStore                    the JWT store for fast-access store operations
     * @param userRepository              the user repository
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public DeleteUserService(JwtStore jwtStore, UserRepository userRepository, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtStore = jwtStore;
        this.userRepository = userRepository;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Deletes an existing user by their unique identifier.
     * <p>
     * Orchestrates the complete user deletion process:
     * <ol>
     * <li>Finds all JWT authentications for the user</li>
     * <li>Deletes each JWT pair from fast-access store (revokes tokens)</li>
     * <li>Deletes all JWT authentications from the repository</li>
     * <li>Deletes the user from the repository</li>
     * </ol>
     *
     * @param id the user's unique identifier
     * @return {@code true} if the user was deleted, {@code false} if not found
     * @throws IllegalArgumentException if the id is invalid
     */
    @Override
    public Boolean deleteUserById(UUID id) {
        var userId = UserId.of(id);

        var jwtAuthentications = jwtAuthenticationRepository.findAllByUserId(userId);
        jwtAuthentications.forEach(jwtAuthentication -> jwtStore.delete(jwtAuthentication.getJwtPair()));

        jwtAuthenticationRepository.deleteAllByUserId(userId);

        return userRepository.deleteById(userId);
    }

}
