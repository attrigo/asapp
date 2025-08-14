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

package com.bcn.asapp.uaa.auth;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a Data Transfer Object (DTO) for an access token JWT.
 *
 * @param jwt the raw JWT string value of the access token, must not be blank
 * @author ttrigo
 * @since 0.2.0
 */
public record AccessTokenDTO(
        @JsonValue @NotBlank(message = "The access token must not be empty") String jwt
) {}
