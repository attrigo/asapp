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

package com.attrigo.asapp.users.application.user.in.service;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.attrigo.asapp.users.application.ApplicationService;
import com.attrigo.asapp.users.application.user.in.DeleteUserUseCase;
import com.attrigo.asapp.users.application.user.out.UserRepository;
import com.attrigo.asapp.users.domain.user.UserId;

/**
 * Application service responsible for orchestrating user deletion operations.
 * <p>
 * Coordinates the user deletion workflow including identifier transformation and removal from the repository.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Transforms UUID into domain value object {@link UserId}</li>
 * <li>Deletes user from repository</li>
 * </ol>
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class DeleteUserService implements DeleteUserUseCase {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code DeleteUserService} with required dependencies.
     *
     * @param userRepository the repository for user data access
     */
    public DeleteUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Boolean deleteUserById(UUID id) {
        var userId = UserId.of(id);

        return deleteUser(userId);
    }

    /**
     * Deletes the user from the repository by identifier.
     *
     * @param userId the user's unique identifier
     * @return {@code true} if deleted, {@code false} if not found
     */
    private Boolean deleteUser(UserId userId) {
        return userRepository.deleteById(userId);
    }

}
