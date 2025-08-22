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

package com.bcn.asapp.uaa.application.authentication.in.service;

import com.bcn.asapp.uaa.application.ApplicationService;
import com.bcn.asapp.uaa.application.authentication.in.RefreshAuthenticationUseCase;
import com.bcn.asapp.uaa.application.authentication.out.AuthenticationRefresher;
import com.bcn.asapp.uaa.application.authentication.out.JwtVerifier;
import com.bcn.asapp.uaa.domain.authentication.EncodedToken;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

/**
 * Application service responsible for orchestrating authentication refresh.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RefreshAuthenticationService implements RefreshAuthenticationUseCase {

    private final JwtVerifier jwtVerifier;

    private final AuthenticationRefresher authenticationRefresher;

    /**
     * Constructs a new {@code RefreshAuthenticationService} with required dependencies.
     *
     * @param jwtVerifier             the JWT verifier for validating refresh tokens
     * @param authenticationRefresher the authentication refresher for generating new tokens
     */
    public RefreshAuthenticationService(JwtVerifier jwtVerifier, AuthenticationRefresher authenticationRefresher) {
        this.jwtVerifier = jwtVerifier;
        this.authenticationRefresher = authenticationRefresher;
    }

    /**
     * Refreshes an authentication using a valid refresh token.
     * <p>
     * Validates the refresh token, verifies it, and generates a new JWT authentication with fresh access and refresh tokens.
     *
     * @param refreshToken the refresh token string
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws IllegalArgumentException if the refresh token is invalid or blank
     */
    @Override
    public JwtAuthentication refreshAuthentication(String refreshToken) {
        var rawRefreshToken = EncodedToken.of(refreshToken);

        var authentication = jwtVerifier.verifyRefreshToken(rawRefreshToken);

        return authenticationRefresher.refreshAuthentication(authentication);

    }

}
