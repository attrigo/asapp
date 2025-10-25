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
 * Represents a title.
 * <p>
 * This value object encapsulates a title value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the title is not blank.
 *
 * @param title the title value
 * @since 0.2.0
 * @author attrigo
 */
public record Title(
        String title
) {

    /**
     * Constructs a new {@code Title} instance and validates its integrity.
     *
     * @param title the title value to validate and store
     * @throws IllegalArgumentException if the title is {@code null} or blank
     */
    public Title {
        validateTitleIsNotBlank(title);
    }

    /**
     * Factory method to create a new {@code Title} instance.
     *
     * @param title the title value
     * @return a new {@code Title} instance
     * @throws IllegalArgumentException if the title is {@code null} or blank
     */
    public static Title of(String title) {
        return new Title(title);
    }

    /**
     * Returns the title value.
     *
     * @return the title {@link String}
     */
    public String value() {
        return this.title;
    }

    /**
     * Validates that the title is not {@code null} or blank.
     *
     * @param title the title to validate
     * @throws IllegalArgumentException if the title is {@code null} or blank
     */
    private static void validateTitleIsNotBlank(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }
    }

}
