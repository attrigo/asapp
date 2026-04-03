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

package com.bcn.asapp.authentication.domain.authentication;

/**
 * Represents the subject claim of a JWT.
 * <p>
 * This value object encapsulates a subject value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the subject is not blank and conform to the standard email structure.
 *
 * @param subject the subject identifier
 * @since 0.2.0
 * @author attrigo
 */
public record Subject(
        String subject
) {

    /**
     * Constructs a new {@code Subject} instance and validates its integrity.
     *
     * @param subject the subject value to validate and store
     * @throws IllegalArgumentException if the subject is {@code null}, blank, or does not match the email pattern
     */
    public Subject {
        validateSubjectIsNotBlank(subject);
        validateSubjectIsEmail(subject);
    }

    /**
     * Factory method to create a new {@code Subject} instance.
     *
     * @param subject the subject value
     * @return a new {@code Subject} instance
     * @throws IllegalArgumentException if the subject is {@code null}, blank, or does not match the email pattern
     */
    public static Subject of(String subject) {
        return new Subject(subject);
    }

    /**
     * Returns the subject value.
     *
     * @return the subject identifier {@link String}
     */
    public String value() {
        return this.subject;
    }

    /**
     * Validates that the subject is not {@code null} or blank.
     *
     * @param subject the subject to validate
     * @throws IllegalArgumentException if the subject is {@code null} or blank
     */
    private static void validateSubjectIsNotBlank(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject must not be null or empty");
        }
    }

    /**
     * Validates that the subject conforms to the email format.
     *
     * @param subject the subject to validate
     * @throws IllegalArgumentException if the subject does not conform to the email format
     */
    private static void validateSubjectIsEmail(String subject) {
        if (!subject.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
            throw new IllegalArgumentException("Subject must be a valid email address");
        }
    }

}
