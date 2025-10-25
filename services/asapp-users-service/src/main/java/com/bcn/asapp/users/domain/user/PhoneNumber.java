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
 * Represents a phone number.
 * <p>
 * This value object encapsulates a phone number value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the phone number is not blank and conform to the standard phone number structure.
 *
 * @param phoneNumber the phone number
 * @since 0.2.0
 * @author attrigo
 */
public record PhoneNumber(
        String phoneNumber
) {

    /**
     * Regular expression pattern for validating phone number format.
     * <p>
     * Validates standard phone number structure: {@code 666777888, 666 777 888 and 666-777-888}
     */
    public static final String SUPPORTED_PHONE_NUMBER_PATTERN = "^(\\d{3}[- ]?){2}\\d{3}$";

    /**
     * Constructs a new {@code PhoneNumber} instance and validates its integrity.
     *
     * @param phoneNumber the phone number value to validate and store
     * @throws IllegalArgumentException if the phone number is {@code null}, blank, or does not match the phone number pattern
     */
    public PhoneNumber {
        validatePhoneNumberIsNotBlank(phoneNumber);
        validatePhoneNumberPattern(phoneNumber);
    }

    /**
     * Factory method to create a new {@code PhoneNumber} instance.
     *
     * @param phoneNumber the phone number value
     * @return a new {@code PhoneNumber} instance
     * @throws IllegalArgumentException if the phone number is {@code null}, blank, or does not match the phone number pattern
     */
    public static PhoneNumber of(String phoneNumber) {
        return new PhoneNumber(phoneNumber);
    }

    /**
     * Returns the phone number value.
     *
     * @return the phone number {@link String} in phone number format
     */
    public String value() {
        return this.phoneNumber;
    }

    /**
     * Validates that the phone number is not {@code null} or blank.
     *
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if the phone number is {@code null} or blank
     */
    private static void validatePhoneNumberIsNotBlank(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or empty");
        }
    }

    /**
     * Validates that the phone number matches the phone number pattern.
     *
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if the phone number does not conform to the phone number format
     */
    private static void validatePhoneNumberPattern(String phoneNumber) {
        if (!phoneNumber.matches(SUPPORTED_PHONE_NUMBER_PATTERN)) {
            throw new IllegalArgumentException("Phone number must follow one of these pattern: 666777888, 666 777 888 or 666-777-888");
        }
    }

}
