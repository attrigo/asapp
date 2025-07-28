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
package com.bcn.asapp.dto.user;

import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.validation.constraints.NotBlank;

import com.bcn.asapp.dto.utils.MaskSensibleStringSerializer;

/**
 * Represents a Data Transfer Object (DTO) for a user.
 *
 * @param username the username of the user, must not be blank
 * @param password the password of the user, must not be blank and is masked during serialization
 * @param role     the role assigned to the user, must not be blank
 *
 * @author ttrigo
 * @since 0.2.0
 */
public record UserDTO(UUID id, @NotBlank(message = "The username must not be empty") String username,
        @NotBlank(message = "The password must not be empty") @JsonSerialize(using = MaskSensibleStringSerializer.class) String password,
        @NotBlank(message = "The role must not be empty") String role) {}
