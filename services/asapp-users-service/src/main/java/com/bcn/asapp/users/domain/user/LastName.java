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

package com.bcn.asapp.users.domain.user;

/**
 * Represents a last name.
 * <p>
 * This value object encapsulates a last name value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the last name is not blank.
 *
 * @param lastName the last name value
 * @since 0.2.0
 * @author attrigo
 */
public record LastName(
        String lastName
) {

    /**
     * Constructs a new {@code LastName} instance and validates its integrity.
     *
     * @param lastName the last name value to validate and store
     * @throws IllegalArgumentException if the last name is {@code null} or blank
     */
    public LastName {
        validateLastNameIsNotBlank(lastName);
    }

    /**
     * Factory method to create a new {@code LastName} instance.
     *
     * @param lastName the last name value
     * @return a new {@code LastName} instance
     * @throws IllegalArgumentException if the last name is {@code null} or blank
     */
    public static LastName of(String lastName) {
        return new LastName(lastName);
    }

    /**
     * Returns the last name value.
     *
     * @return the last name {@link String}
     */
    public String value() {
        return this.lastName;
    }

    /**
     * Validates that the last name is not {@code null} or blank.
     *
     * @param lastName the last name to validate
     * @throws IllegalArgumentException if the last name is {@code null} or blank
     */
    private static void validateLastNameIsNotBlank(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name must not be null or empty");
        }
    }

}
