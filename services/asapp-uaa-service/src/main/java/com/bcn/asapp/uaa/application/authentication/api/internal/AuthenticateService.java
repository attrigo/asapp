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

package com.bcn.asapp.uaa.application.authentication.api.internal;

import com.bcn.asapp.uaa.application.ApplicationService;
import com.bcn.asapp.uaa.application.authentication.api.AuthenticateUseCase;
import com.bcn.asapp.uaa.application.authentication.spi.AuthenticationProvider;
import com.bcn.asapp.uaa.application.authentication.spi.AuthenticatorManagerPort;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.UsernamePasswordAuthentication;

@ApplicationService
public class AuthenticateService implements AuthenticateUseCase {

    private final AuthenticatorManagerPort authenticatorManagerPort;

    private final AuthenticationProvider authenticationProvider;

    public AuthenticateService(AuthenticatorManagerPort authenticatorManagerPort, AuthenticationProvider authenticationProvider) {
        this.authenticatorManagerPort = authenticatorManagerPort;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public JwtAuthentication authenticate(UsernamePasswordAuthentication authenticationRequest) {
        var authentication = authenticatorManagerPort.authenticate(authenticationRequest);

        return authenticationProvider.generateAuthentication(authentication);
    }

}
