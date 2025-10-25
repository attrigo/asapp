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

package com.bcn.asapp.tasks.infrastructure.task.in.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for retrieving a task by its unique identifier.
 *
 * @param taskId      the task's unique identifier
 * @param userId      the task's user unique identifier
 * @param title       the task's title
 * @param description the task's description
 * @param startDate   the task's start date
 * @param endDate     the task's end date
 * @since 0.2.0
 * @author attrigo
 */
public record GetTaskByIdResponse(
        @JsonProperty("task_id") UUID taskId,
        @JsonProperty("user_id") UUID userId,
        String title,
        String description,
        @JsonProperty("start_date") Instant startDate,
        @JsonProperty("end_date") Instant endDate
) {}
