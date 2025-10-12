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

package com.bcn.asapp.authentication.infrastructure.user.in.request;

import static com.bcn.asapp.authentication.domain.user.RawPassword.MAXIMUM_PASSWORD_LENGTH;
import static com.bcn.asapp.authentication.domain.user.RawPassword.MINIMUM_PASSWORD_LENGTH;
import static com.bcn.asapp.authentication.domain.user.Username.SUPPORTED_EMAIL_PATTERN;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for updating an existing user.
 * <p>
 * Contains data validation including username, password, and role.
 *
 * @param username the user's updated username in the email format; must not be blank and must be a valid email address
 * @param password the user's updated raw password; must not be blank and must be between 8 and 64 characters
 * @param role     the user's updated role as a string; must be a valid Role
 * @since 0.2.0
 * @author attrigo
 */
public record UpdateUserRequest(
        @NotBlank(message = "The username must not be empty") @Email(regexp = SUPPORTED_EMAIL_PATTERN, message = "The username must be a valid email address") String username,
        @NotBlank(message = "The password must not be empty") @Size(min = MINIMUM_PASSWORD_LENGTH, max = MAXIMUM_PASSWORD_LENGTH, message = "The password must be between 8 and 64 characters") String password,
        @ValidRole(message = "The role must be a valid Role") String role
) {}
