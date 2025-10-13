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
 * Represents an encoded (hashed) password.
 * <p>
 * This value object encapsulates an encoded password value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the encoded password is not blank and follow a specific password encoder format, which includes an algorithm
 * identifier prefix (e.g., {@code {bcrypt}}, {@code {argon2}}).
 *
 * @param password the encoded password value with algorithm prefix
 * @since 0.2.0
 * @author attrigo
 */
public record EncodedPassword(
        String password
) {

    /**
     * Regular expression pattern for validating prefixed password encoders.
     * <p>
     * Expects format: {@code {encoderId}encodedPasswordHash}
     */
    private static final String SUPPORTED_PREFIXED_ENCODER_PATTERN = "^\\{([^}]+)}.*$";

    /**
     * Constructs a new {@code EncodedPassword} instance and validates its integrity.
     *
     * @param password the encoded password value to validate and store
     * @throws IllegalArgumentException if the password is {@code null}, blank, or does not match the required pattern
     */
    public EncodedPassword {
        validatePasswordIsNotBlank(password);
        validatePasswordPattern(password);
    }

    /**
     * Factory method to create a new {@code EncodedPassword} instance.
     *
     * @param password the encoded password value
     * @return a new {@code EncodedPassword} instance
     * @throws IllegalArgumentException if the password is {@code null}, blank, or does not match the required pattern
     */
    public static EncodedPassword of(String password) {
        return new EncodedPassword(password);
    }

    /**
     * Returns the encoded password value.
     *
     * @return the encoded password {@link String} with algorithm prefix
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
            throw new IllegalArgumentException("Encoded password must not be null or empty");
        }
    }

    /**
     * Validates that the password matches the prefixed encoder pattern.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password does not start with an encoding algorithm prefix
     */
    private static void validatePasswordPattern(String password) {
        if (!password.matches(SUPPORTED_PREFIXED_ENCODER_PATTERN)) {
            throw new IllegalArgumentException("Encoded password must start with an encoding id like {bcrypt}");
        }
    }

}
