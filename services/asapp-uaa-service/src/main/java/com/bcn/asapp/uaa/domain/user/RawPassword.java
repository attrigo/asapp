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

/**
 * Represents a raw (unhashed) password.
 * <p>
 * This value object enforces password validity by ensuring it is not blank and that its length is within defined boundaries.
 *
 * @param password the raw password value
 * @since 0.2.0
 * @author attrigo
 */
public record RawPassword(
        String password
) {

    /**
     * The minimum allowed password length.
     */
    public static final int MINIMUM_PASSWORD_LENGTH = 8;

    /**
     * The maximum allowed password length.
     */
    public static final int MAXIMUM_PASSWORD_LENGTH = 64;

    /**
     * Constructs a new {@code RawPassword} instance and validates its integrity.
     *
     * @param password the raw password value to validate and store
     * @throws IllegalArgumentException if the password is {@code null}, blank, or outside the valid length range
     */
    public RawPassword {
        validatePasswordIsNotBlank(password);
        validatePasswordLength(password);
    }

    /**
     * Factory method to create a new {@code RawPassword} instance.
     *
     * @param password the raw password value
     * @return a new {@code RawPassword} instance
     * @throws IllegalArgumentException if the password is {@code null}, blank, or outside the valid length range
     */
    public static RawPassword of(String password) {
        return new RawPassword(password);
    }

    /**
     * Returns the raw password value.
     *
     * @return the raw, unencrypted password {@link String}
     */
    public String value() {
        return this.password;
    }

    /**
     * Validates that the password is not {@code null} or blank.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password is {@code null} or blank
     */
    private static void validatePasswordIsNotBlank(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Raw password must not be null or empty");
        }
    }

    /**
     * Validates that the password length is within acceptable bounds.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password length is outside the valid range
     */
    private static void validatePasswordLength(String password) {
        if (password.length() < MINIMUM_PASSWORD_LENGTH || password.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Raw password must be between 8 and 64 characters");
        }
    }

}
