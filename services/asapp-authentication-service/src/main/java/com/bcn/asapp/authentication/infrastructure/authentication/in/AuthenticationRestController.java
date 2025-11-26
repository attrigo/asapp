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

package com.bcn.asapp.authentication.infrastructure.authentication.in;

import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.authentication.application.authentication.in.AuthenticateUseCase;
import com.bcn.asapp.authentication.application.authentication.in.RefreshAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;

/**
 * REST controller implementing authentication endpoints.
 * <p>
 * Handles HTTP requests for authentication operations by delegating to application use cases and mapping between DTOs.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RestController
public class AuthenticationRestController implements AuthenticationRestAPI {

    private final AuthenticateUseCase authenticateUseCase;

    private final RefreshAuthenticationUseCase refreshAuthenticationUseCase;

    private final RevokeAuthenticationUseCase revokeAuthenticationUseCase;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    /**
     * Constructs a new {@code AuthenticationRestController} with required dependencies.
     *
     * @param authenticateUseCase          the use case for authenticating users
     * @param refreshAuthenticationUseCase the use case for refreshing authentications
     * @param revokeAuthenticationUseCase  the use case for revoking authentications
     * @param jwtAuthenticationMapper      the mapper for authentication DTOs
     */
    public AuthenticationRestController(AuthenticateUseCase authenticateUseCase, RefreshAuthenticationUseCase refreshAuthenticationUseCase,
            RevokeAuthenticationUseCase revokeAuthenticationUseCase, JwtAuthenticationMapper jwtAuthenticationMapper) {

        this.authenticateUseCase = authenticateUseCase;
        this.refreshAuthenticationUseCase = refreshAuthenticationUseCase;
        this.revokeAuthenticationUseCase = revokeAuthenticationUseCase;
        this.jwtAuthenticationMapper = jwtAuthenticationMapper;
    }

    /**
     * Authenticates a user with the given credentials and provides a new JWT authentication.
     * <p>
     * If the user is already authenticated, new JWT authentication (access and refresh tokens) are generated to override the existing ones.
     *
     * @param request the {@link AuthenticateRequest} containing user credentials
     * @return the {@link AuthenticateResponse} containing access and refresh tokens
     */
    // TODO: Update Javadocs and OpenAPI docs, is it really the JwtAuthentication overridden if it exists?
    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        var authenticateCommand = jwtAuthenticationMapper.toAuthenticateCommand(request);

        var authentication = authenticateUseCase.authenticate(authenticateCommand);

        return jwtAuthenticationMapper.toAuthenticateResponse(authentication);
    }

    /**
     * Refreshes a JWT authentication using a refresh token.
     *
     * @param request the {@link RefreshAuthenticationRequest} containing the refresh token
     * @return the {@link RefreshAuthenticationResponse} containing new access and refresh tokens
     */
    @Override
    public RefreshAuthenticationResponse refreshAuthentication(RefreshAuthenticationRequest request) {
        var refreshedAuthentication = refreshAuthenticationUseCase.refreshAuthentication(request.refreshToken());

        return jwtAuthenticationMapper.toRefreshAuthenticationResponse(refreshedAuthentication);
    }

    /**
     * Revokes a JWT authentication using an access token.
     * <p>
     * Invalidates the JWT authentication, effectively logging out the user.
     *
     * @param request the {@link RevokeAuthenticationRequest} containing the access token
     */
    @Override
    public void revokeAuthentication(RevokeAuthenticationRequest request) {
        revokeAuthenticationUseCase.revokeAuthentication(request.accessToken());
    }

}
