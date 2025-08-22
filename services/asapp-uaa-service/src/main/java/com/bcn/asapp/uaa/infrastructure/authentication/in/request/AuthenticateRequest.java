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

package com.bcn.asapp.uaa.infrastructure.authentication.in.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for authenticating a user.
 * <p>
 * Contains data validation including username and password.
 *
 * @param username the user's username; must not be blank
 * @param password the user's raw password; must not be blank
 * @since 0.2.0
 * @author attrigo
 */
public record AuthenticateRequest(
        @NotBlank(message = "The username must not be empty") String username,
        @NotBlank(message = "The password must not be empty") String password
) {}
