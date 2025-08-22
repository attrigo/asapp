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
import com.bcn.asapp.uaa.application.authentication.api.RevokeAuthenticationUseCase;
import com.bcn.asapp.uaa.application.authentication.spi.JwtRevoker;
import com.bcn.asapp.uaa.application.authentication.spi.JwtVerifier;

@ApplicationService
public class RevokeAuthenticationService implements RevokeAuthenticationUseCase {

    private final JwtVerifier jwtVerifier;

    private final JwtRevoker jwtRevoker;

    public RevokeAuthenticationService(JwtVerifier jwtVerifier, JwtRevoker jwtRevoker) {
        this.jwtVerifier = jwtVerifier;
        this.jwtRevoker = jwtRevoker;
    }

    @Override
    public void revokeAuthentication(String accessToken) {
        var authentication = jwtVerifier.verifyAccessToken(accessToken);

        jwtRevoker.revokeAuthentication(authentication);
    }

}
