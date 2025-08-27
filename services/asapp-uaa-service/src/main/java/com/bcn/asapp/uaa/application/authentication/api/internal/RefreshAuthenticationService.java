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
import com.bcn.asapp.uaa.application.authentication.api.RefreshAuthenticationUseCase;
import com.bcn.asapp.uaa.application.authentication.spi.AuthenticationRefresher;
import com.bcn.asapp.uaa.application.authentication.spi.JwtVerifier;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

@ApplicationService
public class RefreshAuthenticationService implements RefreshAuthenticationUseCase {

    private final JwtVerifier jwtVerifier;

    private final AuthenticationRefresher authenticationRefresher;

    public RefreshAuthenticationService(JwtVerifier jwtVerifier, AuthenticationRefresher authenticationRefresher) {
        this.jwtVerifier = jwtVerifier;
        this.authenticationRefresher = authenticationRefresher;
    }

    @Override
    public JwtAuthentication refreshAuthentication(String refreshToken) {
        var authentication = jwtVerifier.verifyRefreshToken(refreshToken);

        return authenticationRefresher.refreshAuthentication(authentication);

    }

}
