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

import com.bcn.asapp.uaa.application.authentication.api.AuthenticateUseCase;
import com.bcn.asapp.uaa.application.authentication.api.RefreshAuthenticationUseCase;
import com.bcn.asapp.uaa.application.authentication.api.RevokeAuthenticationUseCase;
import com.bcn.asapp.uaa.domain.authentication.UsernamePasswordAuthentication;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.AuthenticateRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.AuthenticateResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RefreshAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RefreshAuthenticationResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RevokeAuthenticationRequest;
import com.bcn.asapp.uaa.infrastructure.authentication.mapper.JwtAuthenticationMapper;

@RestController
public class AuthenticationRestController implements AuthenticationRestAPI {

    private final AuthenticateUseCase authenticateUseCase;

    private final RefreshAuthenticationUseCase refreshAuthenticationUseCase;

    private final RevokeAuthenticationUseCase revokeAuthenticationUseCase;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    public AuthenticationRestController(AuthenticateUseCase authenticateUseCase, RefreshAuthenticationUseCase refreshAuthenticationUseCase,
            RevokeAuthenticationUseCase revokeAuthenticationUseCase, JwtAuthenticationMapper jwtAuthenticationMapper) {

        this.authenticateUseCase = authenticateUseCase;
        this.refreshAuthenticationUseCase = refreshAuthenticationUseCase;
        this.revokeAuthenticationUseCase = revokeAuthenticationUseCase;
        this.jwtAuthenticationMapper = jwtAuthenticationMapper;
    }

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        var authenticationRequest = UsernamePasswordAuthentication.unAuthenticated(request.username(), request.password());
        var authentication = authenticateUseCase.authenticate(authenticationRequest);
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
