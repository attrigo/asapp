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

package com.bcn.asapp.users.infrastructure.user.in.request;

import static com.bcn.asapp.users.domain.user.Email.SUPPORTED_EMAIL_PATTERN;
import static com.bcn.asapp.users.domain.user.PhoneNumber.SUPPORTED_PHONE_NUMBER_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for updating an existing user.
 * <p>
 * Contains data validation including first name, last name, email and phone number.
 *
 * @param firstName   the user's first name; must not be blank
 * @param lastName    the user's last name; must not be blank
 * @param email       the user's email; must not be blank and must be a valid email address
 * @param phoneNumber the user's phone number; must not be blank and must be a valid phone number
 * @since 0.2.0
 * @author attrigo
 */
public record UpdateUserRequest(
        @JsonProperty("first_name") @NotBlank(message = "The first name must not be empty") String firstName,
        @JsonProperty("last_name") @NotBlank(message = "The last name must not be empty") String lastName,
        @NotBlank(message = "The email must not be empty") @Email(regexp = SUPPORTED_EMAIL_PATTERN, message = "The email must be a valid email address") String email,
        @JsonProperty("phone_number") @NotBlank(message = "The phone number must not be empty") @Pattern(regexp = SUPPORTED_PHONE_NUMBER_PATTERN, message = "The phone number must be a valid phone number") String phoneNumber
) {}
