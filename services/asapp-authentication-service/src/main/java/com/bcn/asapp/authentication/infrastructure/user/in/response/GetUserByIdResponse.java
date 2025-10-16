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

package com.bcn.asapp.authentication.infrastructure.user.in.response;

import java.util.UUID;

/**
 * Response for retrieving a user by its unique identifier.
 *
 * @param userId   the user's unique identifier
 * @param username the user's username in the email format
 * @param password the user's masked password
 * @param role     the user's role
 * @since 0.2.0
 * @author attrigo
 */
public record GetUserByIdResponse(
        UUID userId,
        String username,
        String password,
        String role
) {}
