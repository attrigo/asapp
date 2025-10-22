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
 * Represents a email.
 * <p>
 * This value object encapsulates an email value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the email is not blank and conform to the standard email structure.
 *
 * @param email the email
 * @since 0.2.0
 * @author attrigo
 */
public record Email(
        String email
) {

    /**
     * Regular expression pattern for validating email format.
     * <p>
     * Validates standard email structure: {@code localpart@domain}
     */
    public static final String SUPPORTED_EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    /**
     * Constructs a new {@code Email} instance and validates its integrity.
     *
     * @param email the email value to validate and store
     * @throws IllegalArgumentException if the email is {@code null}, blank, or does not match the email pattern
     */
    public Email {
        validateEmailIsNotBlank(email);
        validateEmailPattern(email);
    }

    /**
     * Factory method to create a new {@code Email} instance.
     *
     * @param email the email value
     * @return a new {@code Email} instance
     * @throws IllegalArgumentException if the email is {@code null}, blank, or does not match the email pattern
     */
    public static Email of(String email) {
        return new Email(email);
    }

    /**
     * Returns the email value.
     *
     * @return the email {@link String} in email format
     */
    public String value() {
        return this.email;
    }

    /**
     * Validates that the email is not {@code null} or blank.
     *
     * @param email the email to validate
     * @throws IllegalArgumentException if the email is {@code null} or blank
     */
    private static void validateEmailIsNotBlank(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
    }

    /**
     * Validates that the email matches the email pattern.
     *
     * @param email the email to validate
     * @throws IllegalArgumentException if the email does not conform to the email format
     */
    private static void validateEmailPattern(String email) {
        if (!email.matches(SUPPORTED_EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email must be a valid email address");
        }
    }

}
