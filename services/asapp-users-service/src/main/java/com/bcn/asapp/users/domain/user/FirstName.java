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
 * Represents a first name.
 * <p>
 * This value object encapsulates a first name value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the first name is not blank.
 *
 * @param firstName the first name value
 * @since 0.2.0
 * @author attrigo
 */
public record FirstName(
        String firstName
) {

    /**
     * Constructs a new {@code FirstName} instance and validates its integrity.
     *
     * @param firstName the first name value to validate and store
     * @throws IllegalArgumentException if the first name is {@code null} or blank
     */
    public FirstName {
        validateFirstNameIsNotBlank(firstName);
    }

    /**
     * Factory method to create a new {@code FirstName} instance.
     *
     * @param firstName the first name value
     * @return a new {@code FirstName} instance
     * @throws IllegalArgumentException if the first name is {@code null} or blank
     */
    public static FirstName of(String firstName) {
        return new FirstName(firstName);
    }

    /**
     * Returns the first name value.
     *
     * @return the first name {@link String}
     */
    public String value() {
        return this.firstName;
    }

    /**
     * Validates that the first name is not {@code null} or blank.
     *
     * @param firstName the first name to validate
     * @throws IllegalArgumentException if the first name is {@code null} or blank
     */
    private static void validateFirstNameIsNotBlank(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name must not be null or empty");
        }
    }

}
