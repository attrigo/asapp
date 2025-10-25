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

/**
 * Represents a description.
 * <p>
 * This value object encapsulates a description value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the description is not blank.
 *
 * @param description the description value
 * @since 0.2.0
 * @author attrigo
 */
public record Description(
        String description
) {

    /**
     * Constructs a new {@code Description} instance and validates its integrity.
     *
     * @param description the description value to validate and store
     * @throws IllegalArgumentException if the description is {@code null} or blank
     */
    public Description {
        validateDescriptionIsNotBlank(description);
    }

    /**
     * Factory method to create a new {@code Description} instance.
     *
     * @param description the description value
     * @return a new {@code Description} instance
     * @throws IllegalArgumentException if the description is {@code null} or blank
     */
    public static Description of(String description) {
        return new Description(description);
    }

    /**
     * Factory method to create an optional {@code Description} instance.
     * <p>
     * Returns {@code null} if the provided description is {@code null} or blank, otherwise creates a valid {@code Description}.
     *
     * @param description the description value, may be {@code null} or blank
     * @return a new {@code Description} instance if the value is not {@code null} or blank, {@code null} otherwise
     */
    public static Description ofNullable(String description) {
        return (description == null || description.isBlank()) ? null : new Description(description);
    }

    /**
     * Returns the description value.
     *
     * @return the description {@link String}
     */
    public String value() {
        return this.description;
    }

    /**
     * Validates that the description is not {@code null} or blank.
     *
     * @param description the description to validate
     * @throws IllegalArgumentException if the description is {@code null} or blank
     */
    private static void validateDescriptionIsNotBlank(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be null or empty");
        }
    }

}
