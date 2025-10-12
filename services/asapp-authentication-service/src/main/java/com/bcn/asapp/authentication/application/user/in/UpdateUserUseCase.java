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

package com.bcn.asapp.authentication.application.user.in;

import java.util.Optional;

import com.bcn.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.authentication.domain.user.User;

/**
 * Use case for updating an existing user in the system.
 * <p>
 * Defines the contract for update user modification operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface UpdateUserUseCase {

    /**
     * Updates an existing user based on the provided command.
     *
     * @param command the {@link UpdateUserCommand} containing user update data
     * @return an {@link Optional} containing the updated {@link User} if found, {@link Optional#empty} otherwise
     * @throws IllegalArgumentException if username, password, or role is invalid
     */
    Optional<User> updateUserById(UpdateUserCommand command);

}
