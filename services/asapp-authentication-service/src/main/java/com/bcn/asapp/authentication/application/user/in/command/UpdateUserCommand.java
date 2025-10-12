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

package com.bcn.asapp.authentication.application.user.in.command;

import java.util.UUID;

/**
 * Command to update an existing user in the system.
 * <p>
 * Encapsulates the data required to modify a user’s account.
 *
 * @param userId   the unique identifier of the user to update
 * @param username the updated username in email format
 * @param password the updated raw password
 * @param role     the updated role as a string
 * @since 0.2.0
 * @author attrigo
 */
public record UpdateUserCommand(
        UUID userId,
        String username,
        String password,
        String role
) {}
