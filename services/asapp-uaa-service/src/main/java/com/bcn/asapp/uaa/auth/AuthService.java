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

/**
 * Service interface defining user authentication and JWT management operations.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public interface AuthService {

    /**
     * Authenticates a user based on the provided credentials.
     *
     * @param userCredentials the user credentials to authenticate
     * @return a {@link JwtAuthenticationDTO} containing authentication tokens upon success
     */
    JwtAuthenticationDTO authenticate(UserCredentialsDTO userCredentials);

    /**
     * Refreshes JWT authentication tokens using the provided refresh token.
     *
     * @param refreshToken the refresh token used to obtain new authentication tokens
     * @return a new {@link JwtAuthenticationDTO} containing refreshed tokens
     */
    JwtAuthenticationDTO refreshToken(RefreshTokenDTO refreshToken);

    /**
     * Revokes the JWT authentication for a user by invalidating both access and refresh tokens using the provided access token.
     *
     * @param accessToken the access token used to invalidate the JWT authentication
     */
    void revokeAuthentication(AccessTokenDTO accessToken);

}
