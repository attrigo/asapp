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

import com.bcn.asapp.uaa.auth.AuthRestAPI;
import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.LoginRequestDTO;
import com.bcn.asapp.uaa.auth.LoginResponseDTO;

/**
 * Standard implementation of {@link AuthRestAPI}.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@RestController
public class AuthRestController implements AuthRestAPI {

    private final AuthService authService;

    /**
     * Main constructor.
     *
     * @param authService the services that brings authentication operations.
     */
    public AuthRestController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        var loginResponse = authService.login(loginRequest);

        return ResponseEntity.ok(loginResponse);
    }

}
