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
package com.bcn.asapp.uaa.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) encapsulating JWT authentication tokens.
 * <p>
 * Contains both an access token and a refresh token used in authentication flows.
 *
 * @param accessToken  the access token DTO, must not be {@literal null}
 * @param refreshToken the refresh token DTO, must not be {@literal null}
 * @author ttrigo
 * @since 0.2.0
 */
public record JwtAuthenticationDTO(@NotNull(message = "The access token is mandatory") @JsonProperty("access_token") AccessTokenDTO accessToken,
        @NotNull(message = "The refresh token is mandatory") @JsonProperty("refresh_token") RefreshTokenDTO refreshToken) {}
