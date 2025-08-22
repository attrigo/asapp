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

package com.bcn.asapp.uaa.infrastructure.authentication.api;

import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.uaa.application.authentication.api.AuthenticateUserUseCase;
import com.bcn.asapp.uaa.application.authentication.api.RefreshAuthenticationUseCase;
import com.bcn.asapp.uaa.application.authentication.api.RevokeAuthenticationUseCase;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.AuthenticateRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.AuthenticateResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RefreshAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RefreshAuthenticationResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RevokeAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.mapper.JwtAuthenticationMapper;

@RestController
public class AuthenticationRestController implements AuthenticationRestAPI {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    private final RefreshAuthenticationUseCase refreshAuthenticationUseCase;

    private final RevokeAuthenticationUseCase revokeAuthenticationUseCase;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    public AuthenticationRestController(AuthenticateUserUseCase authenticateUserUseCase, RefreshAuthenticationUseCase refreshAuthenticationUseCase,
            RevokeAuthenticationUseCase revokeAuthenticationUseCase, JwtAuthenticationMapper jwtAuthenticationMapper) {

        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshAuthenticationUseCase = refreshAuthenticationUseCase;
        this.revokeAuthenticationUseCase = revokeAuthenticationUseCase;
        this.jwtAuthenticationMapper = jwtAuthenticationMapper;
    }

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        var credentialsToAuthenticate = authenticateUserUseCase.authenticate(request.username(), request.password());
        return jwtAuthenticationMapper.toAuthenticateResponse(credentialsToAuthenticate);
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
