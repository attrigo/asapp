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

package com.bcn.asapp.tasks.infrastructure.task.out.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a task in the database.
 * <p>
 * Maps to the {@code tasks} table and provides persistence representation for task domain entities.
 *
 * @param id          the task's unique identifier
 * @param userId      the task's user id; must not be {@code null}
 * @param title       the task's title; must not be blank
 * @param description the task's description
 * @param startDate   the task's start date
 * @param endDate     the task's end date
 * @since 0.2.0
 * @author attrigo
 */
@Table("tasks")
public record TaskEntity(
        @Id UUID id,
        @Column("user_id") @NotNull UUID userId,
        @NotBlank String title,
        String description,
        Instant startDate,
        Instant endDate
) {}
