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

package com.bcn.asapp.authentication.domain.user;

/**
 * Represents a username in email format.
 * <p>
 * This value object encapsulates a username value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the username is not blank and conform to the standard email structure.
 *
 * @param username the username value in the email format
 * @since 0.2.0
 * @author attrigo
 */
public record Username(
        String username
) {

    /**
     * Regular expression pattern for validating email format.
     * <p>
     * Validates standard email structure: {@code localpart@domain}
     */
    public static final String SUPPORTED_EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    /**
     * Constructs a new {@code Username} instance and validates its integrity.
     *
     * @param username the username value to validate and store
     * @throws InvalidUsernameException if the username is {@code null}, blank, or does not match the email pattern
     */
    public Username {
        validateUsernameIsNotBlank(username);
        validateUsernamePattern(username);
    }

    /**
     * Factory method to create a new {@code Username} instance.
     *
     * @param username the username value
     * @return a new {@code Username} instance
     * @throws InvalidUsernameException if the username is {@code null}, blank, or does not match the email pattern
     */
    public static Username of(String username) {
        return new Username(username);
    }

    /**
     * Returns the username value.
     *
     * @return the username {@link String} in email format
     */
    public String value() {
        return this.username;
    }

    /**
     * Validates that the username is not {@code null} or blank.
     *
     * @param username the username to validate
     * @throws InvalidUsernameException if the username is {@code null} or blank
     */
    private static void validateUsernameIsNotBlank(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidUsernameException("Username must not be null or empty");
        }
    }

    /**
     * Validates that the username matches the email pattern.
     *
     * @param username the username to validate
     * @throws InvalidUsernameException if the username does not conform to the email format
     */
    private static void validateUsernamePattern(String username) {
        if (!username.matches(SUPPORTED_EMAIL_PATTERN)) {
            throw new InvalidUsernameException("Username must be a valid email address");
        }
    }

}
