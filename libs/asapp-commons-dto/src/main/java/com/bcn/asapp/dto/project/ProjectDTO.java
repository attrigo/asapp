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
package com.bcn.asapp.dto.project;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import com.bcn.asapp.dto.task.TaskDTO;

/**
 * Represents a project DTO.
 *
 * @author ttrigo
 * @since 0.1.0
 */
public record ProjectDTO(UUID id, @NotBlank(message = "The title of the project is mandatory") String title, String description, Instant startDateTime,
        List<TaskDTO> tasks) {}
