/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.attrigo.asapp.authentication.infrastructure.authentication.in;

import org.springframework.web.bind.annotation.RestController;

import com.attrigo.asapp.authentication.application.authentication.in.AuthenticateUseCase;
import com.attrigo.asapp.authentication.application.authentication.in.RefreshAuthenticationUseCase;
import com.attrigo.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.attrigo.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.attrigo.asapp.authentication.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.attrigo.asapp.authentication.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.attrigo.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.attrigo.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.attrigo.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;

/**
 * REST controller implementing authentication endpoints.
 * <p>
 * Handles HTTP requests for authentication operations by delegating to application use cases and mapping between DTOs.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RestController
public class AuthenticationRestController implements AuthenticationApi {

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

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        var authenticateCommand = jwtAuthenticationMapper.toAuthenticateCommand(request);

        var authentication = authenticateUseCase.authenticate(authenticateCommand);

        return jwtAuthenticationMapper.toAuthenticateResponse(authentication);
    }

    @Override
    public RefreshAuthenticationResponse refreshAuthentication(RefreshAuthenticationRequest request) {
        var refreshedAuthentication = refreshAuthenticationUseCase.refreshAuthentication(request.refreshToken());

        return jwtAuthenticationMapper.toRefreshAuthenticationResponse(refreshedAuthentication);
    }

    @Override
    public void revokeAuthentication(RevokeAuthenticationRequest request) {
        revokeAuthenticationUseCase.revokeAuthentication(request.accessToken());
    }

}
