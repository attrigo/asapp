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

package com.bcn.asapp.uaa.auth.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.uaa.auth.AccessTokenDTO;
import com.bcn.asapp.uaa.auth.AuthRestAPI;
import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.JwtAuthenticationDTO;
import com.bcn.asapp.uaa.auth.RefreshTokenDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;

/**
 * REST controller implementation of the {@link AuthRestAPI} responsible for handling user authentication requests.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@RestController
public class AuthRestController implements AuthRestAPI {

    /**
     * Service responsible for authentication logic and token management.
     */
    private final AuthService authService;

    /**
     * Constructs a new {@code AuthRestController} with the specified authentication service.
     *
     * @param authService the authentication service responsible for authentication and token management
     */
    public AuthRestController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JwtAuthenticationDTO> authenticate(UserCredentialsDTO userCredentials) {
        var authentication = authService.authenticate(userCredentials);
        return ResponseEntity.ok(authentication);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JwtAuthenticationDTO> refreshAuthentication(RefreshTokenDTO refreshToken) {
        var authentication = authService.refreshAuthentication(refreshToken);
        return ResponseEntity.ok(authentication);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> revokeAuthentication(AccessTokenDTO accessToken) {
        authService.revokeAuthentication(accessToken);
        return ResponseEntity.ok()
                             .build();
    }

}
