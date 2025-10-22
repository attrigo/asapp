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

package com.bcn.asapp.users.infrastructure.user.in.response;

import java.util.UUID;

/**
 * Response for retrieving a user by its unique identifier.
 *
 * @param userId      the user's unique identifier
 * @param firstName   the user's first name
 * @param lastName    the user's last name
 * @param email       the user's email
 * @param phoneNumber the user's phone number
 * @since 0.2.0
 * @author attrigo
 */
public record GetUserByIdResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {}
