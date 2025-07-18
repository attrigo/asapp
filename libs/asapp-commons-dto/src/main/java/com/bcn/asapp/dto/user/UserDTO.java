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

public record UserDTO(UUID id, @NotBlank(message = "The username of the user is mandatory") String username,
        @NotBlank(message = "The password of the user is mandatory") @JsonSerialize(using = MaskSensibleStringSerializer.class) String password,
        @NotBlank(message = "The role of the user is mandatory") String role) {}
