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

package com.bcn.asapp.uaa.domain.user;

import java.util.UUID;

/**
 * Represents a unique user identifier.
 * <p>
 * This value object wraps a {@link UUID} and ensures it is not {@code null}, providing type safety and domain clarity for user identification.
 *
 * @param id the unique identifier for a user
 * @since 0.2.0
 * @author attrigo
 */
public record UserId(
        UUID id
) {

    /**
     * Constructs a new {@code UserId} instance and validates its integrity.
     *
     * @param id the UUID value to validate and store
     * @throws IllegalArgumentException if the id is {@code null}
     */
    public UserId {
        validateIdIsNotNull(id);
    }

    /**
     * Factory method to create a new {@code UserId} instance.
     *
     * @param id the UUID value
     * @return a new {@code UserId} instance
     * @throws IllegalArgumentException if the id is {@code null}
     */
    public static UserId of(UUID id) {
        return new UserId(id);
    }

    /**
     * Returns the user identifier value.
     *
     * @return the {@link UUID} representing the user's unique identifier
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
            throw new IllegalArgumentException("User ID must not be null");
        }
    }

}
