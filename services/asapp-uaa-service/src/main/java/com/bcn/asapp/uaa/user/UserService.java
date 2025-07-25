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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.dto.user.UserDTO;

/**
 * Defines user-related business operations.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public interface UserService {

    /**
     * Finds a user by their id.
     *
     * @param id the id of the user
     * @return an {@link Optional} containing the {@link UserDTO} if found, otherwise an {@code Optional.empty()}
     */
    Optional<UserDTO> findById(UUID id);

    /**
     * Finds all users.
     *
     * @return a list of {@link UserDTO}, or empty list if none found
     */
    List<UserDTO> findAll();

    /**
     * Creates a new user.
     * <p>
     * The user's password is encoded before saving.
     * <p>
     * In case the id of the {@link UserDTO} is present it is ignored, as id generation is handled by the persistence layer.
     *
     * @param user the user data to be created
     * @return the created {@link UserDTO} with a generated id
     */
    UserDTO create(UserDTO user);

    /**
     * Updates an existing user by id.
     * <p>
     * The password of the new user data is encoded before saving.
     * <p>
     * The id of the {@link UserDTO} is not updated.
     *
     * @param id          the id of the user to update
     * @param newUserData the updated user data
     * @return an {@link Optional} containing the updated {@link UserDTO} if the user exists; otherwise an {@code Optional.empty()}
     */
    Optional<UserDTO> updateById(UUID id, UserDTO newUserData);

    /**
     * Deletes a user by id.
     *
     * @param id the id of the user to delete
     * @return {@code true} if the user was deleted; {@code false} otherwise
     */
    Boolean deleteById(UUID id);

}
