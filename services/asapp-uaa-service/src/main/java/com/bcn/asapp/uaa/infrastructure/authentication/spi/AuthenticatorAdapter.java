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

package com.bcn.asapp.uaa.infrastructure.authentication.spi;

import java.util.Collection;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.Authenticator;
import com.bcn.asapp.uaa.domain.authentication.UsernamePasswordAuthentication;
import com.bcn.asapp.uaa.domain.user.Role;

@Component
public class AuthenticatorAdapter implements Authenticator {

    private final AuthenticationManager authenticationManager;

    public AuthenticatorAdapter(org.springframework.security.authentication.AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public UsernamePasswordAuthentication authenticate(UsernamePasswordAuthentication authenticationRequest) {
        var authenticationTokenRequest = UsernamePasswordAuthenticationToken.unauthenticated(authenticationRequest.username(),
                authenticationRequest.password());

        var authenticationToken = authenticationManager.authenticate(authenticationTokenRequest);
        var roles = buildAuthenticationRoles(authenticationToken);

        return UsernamePasswordAuthentication.authenticated(authenticationToken.getName(), null, roles);
    }

    private Collection<Role> buildAuthenticationRoles(Authentication authenticationToken) {
        return authenticationToken.getAuthorities()
                                  .stream()
                                  .map(authority -> Role.valueOf(authority.getAuthority()))
                                  .toList();
    }

}
