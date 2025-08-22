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
import com.bcn.asapp.uaa.application.authentication.in.AuthenticateUseCase;
import com.bcn.asapp.uaa.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.uaa.application.authentication.out.AuthenticationGranter;
import com.bcn.asapp.uaa.application.authentication.out.Authenticator;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.UserAuthentication;
import com.bcn.asapp.uaa.domain.user.RawPassword;
import com.bcn.asapp.uaa.domain.user.Username;

/**
 * Application service responsible for orchestrate user authentication.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class AuthenticateService implements AuthenticateUseCase {

    private final Authenticator authenticator;

    private final AuthenticationGranter authenticationGranter;

    /**
     * Constructs a new {@code AuthenticateService} with required dependencies.
     *
     * @param authenticator         the authenticator for validating credentials
     * @param authenticationGranter the authentication granter for generating JWT tokens
     */
    public AuthenticateService(Authenticator authenticator, AuthenticationGranter authenticationGranter) {
        this.authenticator = authenticator;
        this.authenticationGranter = authenticationGranter;
    }

    /**
     * Authenticates a user based on the provided credentials.
     * <p>
     * Validates command data, creates an unauthenticated user authentication request, authenticates the user, and grants a JWT authentication with tokens.
     *
     * @param authenticateCommand the {@link AuthenticateCommand} containing user credentials
     * @return the {@link JwtAuthentication} containing access and refresh tokens
     * @throws IllegalArgumentException if the username or password is invalid
     */
    @Override
    public JwtAuthentication authenticate(AuthenticateCommand authenticateCommand) {
        var username = Username.of(authenticateCommand.username());
        var password = RawPassword.of(authenticateCommand.password());
        var userAuthenticationRequest = UserAuthentication.unAuthenticated(username, password);

        var authentication = authenticator.authenticate(userAuthenticationRequest);

        return authenticationGranter.grantAuthentication(authentication);
    }

}
