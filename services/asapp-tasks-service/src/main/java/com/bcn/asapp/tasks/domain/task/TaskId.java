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

package com.bcn.asapp.tasks.domain.task;

import java.util.UUID;

/**
 * Represents a unique task identifier.
 * <p>
 * This value object encapsulates a task unique identifier value as {@link UUID}.
 * <p>
 * It enforces structural integrity by ensuring the unique identifier is not {@code null}.
 * <p>
 * Provides type safety and domain clarity for task identification.
 *
 * @param id the unique identifier for a task
 * @since 0.2.0
 * @author attrigo
 */
public record TaskId(
        UUID id
) {

    /**
     * Constructs a new {@code TaskId} instance and validates its integrity.
     *
     * @param id the UUID value to validate and store
     * @throws IllegalArgumentException if the id is {@code null}
     */
    public TaskId {
        validateIdIsNotNull(id);
    }

    /**
     * Factory method to create a new {@code TaskId} instance.
     *
     * @param id the UUID value
     * @return a new {@code TaskId} instance
     * @throws IllegalArgumentException if the id is {@code null}
     */
    public static TaskId of(UUID id) {
        return new TaskId(id);
    }

    /**
     * Returns the task identifier value.
     *
     * @return the {@link UUID} representing the task's unique identifier
     */
    public UUID value() {
        return this.id;
    }

    /**
     * Validates that the id is not {@code null}.
     *
     * @param id the id to validate
     * @throws IllegalArgumentException if the id is {@code null}
     */
    private static void validateIdIsNotNull(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Task ID must not be null");
        }
    }

}
