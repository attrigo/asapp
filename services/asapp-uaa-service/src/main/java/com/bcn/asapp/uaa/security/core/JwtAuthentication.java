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
package com.bcn.asapp.uaa.security.core;

import jakarta.validation.constraints.NotNull;

/**
 * Represents the authentication details containing both an access token and a refresh token.
 * <p>
 * Encapsulates the tokens required for user authentication and refresh token operations.
 *
 * @param accessToken  the {@link AccessToken} used for authenticating the user and granting access to resources
 * @param refreshToken the {@link RefreshToken} used for obtaining a new access token when the current one expires
 * @since 0.2.0
 * @author ttrigo
 */
public record JwtAuthentication(@NotNull AccessToken accessToken, @NotNull RefreshToken refreshToken) {}
