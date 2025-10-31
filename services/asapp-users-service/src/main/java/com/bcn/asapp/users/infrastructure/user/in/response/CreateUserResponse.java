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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for a successfully created user.
 *
 * @param userId the user's unique identifier
 * @since 0.2.0
 * @author attrigo
 */
public record CreateUserResponse(
        @JsonProperty("user_id") UUID userId
) {}
