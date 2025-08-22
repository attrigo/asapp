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

package com.bcn.asapp.uaa.domain.authentication;

/**
 * Represents the subject claim of a JWT token.
 * <p>
 * This value object wraps the subject identifier (typically a user identifier) and ensures it is not blank, providing type safety for JWT claims.
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
     * @throws IllegalArgumentException if the subject is {@code null} or blank
     */
    public Subject {
        validateSubjectIsNotBlank(subject);
    }

    /**
     * Factory method to create a new {@code Subject} instance.
     *
     * @param subject the subject value
     * @return a new {@code Subject} instance
     * @throws IllegalArgumentException if the subject is {@code null} or blank
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

}
