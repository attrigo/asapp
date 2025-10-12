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

package com.bcn.asapp.authentication.infrastructure.authentication.in.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for revoking a JWT authentication.
 * <p>
 * Contains data validation including the access token.
 *
 * @param accessToken the access token string; must not be blank
 * @since 0.2.0
 * @author attrigo
 */
public record RevokeAuthenticationRequest(
        @JsonProperty("access_token") @NotBlank(message = "The access token must not be empty") String accessToken
) {}
